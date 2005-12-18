//import uk.co.uwcs.choob.*;
import uk.co.uwcs.choob.modules.*;
import uk.co.uwcs.choob.support.*;
import uk.co.uwcs.choob.support.events.*;
import java.util.*;
//import java.security.*;
import java.util.regex.*;

public class UserTypeCheckResult
{
	boolean hasChecked;
	boolean isBot;
	boolean isAway;
	boolean isOperator;
	boolean isRegistered;
	boolean isSecure;
	long timestamp;
}

public class UserTypeCheck
{
	private Modules mods;
	private IRCInterface irc;
	private Map<String,UserTypeCheckResult> userChecks;
	
	/* The time between checks for expired cache items. */
	private final int USER_DATA_INTERVAL = 30000; // 30 seconds
	/* the time for which an entry is cached. */
	private final int USER_DATA_TIMEOUT = USER_DATA_INTERVAL * 10; // 5 minutes
	
	private final int USER_TYPE_FLAG_BOT         = 1;
	private final int USER_TYPE_FLAG_AWAY        = 2;
	private final int USER_TYPE_FLAG_IRCOP       = 3;
	private final int USER_TYPE_FLAG_REGISTERED  = 4;
	private final int USER_TYPE_FLAG_SECURE      = 5;
	private final int USER_TYPE_FLAG__MIN = USER_TYPE_FLAG_BOT;
	private final int USER_TYPE_FLAG__MAX = USER_TYPE_FLAG_SECURE;
	
	private final int USER_TYPE_RV_ERROR = -1;
	private final int USER_TYPE_RV_NO    =  0;
	private final int USER_TYPE_RV_YES   =  1;
	
	private final Pattern SplitWhoisLine = Pattern.compile("^([^ ]+) ([^ ]+) (.*)$");
	
	public UserTypeCheck(Modules mods, IRCInterface irc)
	{
		this.mods = mods;
		this.irc = irc;
		
		userChecks = new HashMap<String,UserTypeCheckResult>();
		
		mods.interval.callBack(null, 1);
	}
	
	/* This is never called. WTF? */
	public void destroy(Modules mods)
	{
		// Notify all threads blocked on user queries. This allows anything
		// that was asking about a user to continue (and fail) rather than be
		// stuck for all eternity.
		synchronized(userChecks)
		{
			Iterator<String> user = userChecks.keySet().iterator();
			while(user.hasNext()) {
				UserTypeCheckResult entry = userChecks.get(user.next());
				//synchronized(entry)
				//{
					entry.notifyAll();
				//}
			}
		}
	}
	
	public synchronized void interval(Object parameter, Modules mods, IRCInterface irc)
	{
		synchronized(userChecks)
		{
			Iterator<String> user = userChecks.keySet().iterator();
			String nick;
			while(user.hasNext()) {
				// We want to remove any items that have expired, but leave
				// those still in progress.
				nick = user.next();
				UserTypeCheckResult entry = userChecks.get(nick);
				if (entry.hasChecked && (System.currentTimeMillis() > entry.timestamp + USER_DATA_TIMEOUT)) {
					System.out.println("Removed user check for " + nick + ".");
					userChecks.remove(nick);
					// Restart iterator, otherwise it gets all touchy.
					user = userChecks.keySet().iterator();
				}
			}
		}
		
		mods.interval.callBack(null, USER_DATA_INTERVAL);
	}
	
	public String[] helpCommandCheck = {
		"Displays some cached status information about a user.",
		"[<nickname>]",
		"<nickname> is an optional nick to check"
	};
	public void commandCheck(Message mes)
	{
		String nick = mods.util.getParamString(mes);
		if (nick.length() == 0)
			nick = mes.getNick();
		
		int Bot = apiStatus(nick, "bot");
		int Away = apiStatus(nick, "away");
		int IRCop = apiStatus(nick, "ircop");
		int Reg = apiStatus(nick, "registered");
		int SSL = apiStatus(nick, "secure");
		
		irc.sendContextReply(mes, "Status for " + nick +
				": bot = " + mapRVToString(Bot) +
				"; away = " + mapRVToString(Away) +
				"; ircop = " + mapRVToString(IRCop) +
				"; registered = " + mapRVToString(Reg) +
				"; secure = " + mapRVToString(SSL) +
				".");
	}
	
	/*
	 * Checks the status of the nickname and returns the status of the specified
	 * flag. This bblocks the caller if a client-server check is needed for this
	 * user (which is then cached). The block times out after 10 seconds, at
	 * which point the error value is returned (-1).
	 *
	 * @param nick The nickname to check the status of.
	 * @param flag The flag to check. May be one of: "bot" (user is marked as a bot),
	 *        "away" (marked away), "ircop" (IRC operator/network service),
	 *        "registered" (registered and identified with NickServ) or
	 *        "secure" (using a secure [SSL] connection).
	 * @return 1 meaning the flag is true/set for the user, 0 if it is false/not
	 *         set and -1 if an error occurs.
	 */
	public int apiStatus(String nick, String flag)
	{
		String nickl = nick.toLowerCase();
		String flagl = flag.toLowerCase();
		if (flagl.equals("bot"))
			return getStatus(nickl, USER_TYPE_FLAG_BOT);
		if (flagl.equals("away"))
			return getStatus(nickl, USER_TYPE_FLAG_AWAY);
		if (flagl.equals("ircop"))
			return getStatus(nickl, USER_TYPE_FLAG_IRCOP);
		if (flagl.equals("registered"))
			return getStatus(nickl, USER_TYPE_FLAG_REGISTERED);
		if (flagl.equals("secure"))
			return getStatus(nickl, USER_TYPE_FLAG_SECURE);
		return USER_TYPE_RV_ERROR;
	}
	
	public void onServerResponse(ServerResponse ev)
	{
		// ^([^ ]+) ([^ ]+) (.*)$
		Matcher sp = SplitWhoisLine.matcher(ev.getResponse());
		if (!sp.matches())
			return;
		
		String nickl = sp.group(2).toLowerCase();
		
		UserTypeCheckResult userData;
		synchronized(userChecks) {
			userData = userChecks.get(nickl);
		}
		if (userData == null) {
			return;
		}
		
		synchronized(userData) {
			if (ev.getCode() == 335) {
				userData.isBot = true;
			}
			if (ev.getCode() == 301) {
				userData.isAway = true;
			}
			if (ev.getCode() == 313) {
				userData.isOperator = true;
			}
			if (ev.getCode() == 307) {
				userData.isRegistered = true;
			}
			if (ev.getCode() == 671) {
				userData.isSecure = true;
			}
			if (ev.getCode() == 318) {
				userData.timestamp = System.currentTimeMillis();
				userData.hasChecked = true;
				System.out.println("Check for " + nickl +
						 ": bot("   + (new Boolean(userData.isBot)).toString() + 
						"); away(" + (new Boolean(userData.isAway)).toString() + 
						"); ircop(" + (new Boolean(userData.isOperator)).toString() + 
						"); reg("   + (new Boolean(userData.isRegistered)).toString() + 
						"); ssl("   + (new Boolean(userData.isSecure)).toString() + 
						").");
				userData.notifyAll();
			}
		}
	}
	
	private int getStatus(String nick, int flag)
	{
		UserTypeCheckResult userData = getUserData(nick);
		if ((userData == null) || !userData.hasChecked) {
			System.out.println("Check for " + nick + " FAILED!");
			return USER_TYPE_RV_ERROR;
		}
		
		switch(flag) {
			case USER_TYPE_FLAG_BOT:
				return mapBooleanToCheckRV(userData.isBot);
			
			case USER_TYPE_FLAG_AWAY:
				return mapBooleanToCheckRV(userData.isAway);
			
			case USER_TYPE_FLAG_IRCOP:
				return mapBooleanToCheckRV(userData.isOperator);
			
			case USER_TYPE_FLAG_REGISTERED:
				return mapBooleanToCheckRV(userData.isRegistered);
			
			case USER_TYPE_FLAG_SECURE:
				return mapBooleanToCheckRV(userData.isSecure);
		}
		return USER_TYPE_RV_ERROR;
	}
	
	private UserTypeCheckResult getUserData(String nick)
	{
		UserTypeCheckResult data;
		synchronized(userChecks) {
			data = userChecks.get(nick);
			
			if (data != null) {
				synchronized(data) {
					if (!data.hasChecked) {
						return null;
					}
					return data;
				}
			}
			
			// Create new data thing and send query off.
			data = new UserTypeCheckResult();
			data.hasChecked = false;
			userChecks.put(nick, data);
		}
		irc.sendRawLine("WHOIS " + nick);
		
		synchronized(data) {
			try {
				data.wait(10000);
			} catch (InterruptedException e) {
				return null;
			}
		}
		return data;
	}
	
	private int mapBooleanToCheckRV(boolean in)
	{
		if (in)
			return USER_TYPE_RV_YES;
		return USER_TYPE_RV_NO;
	}
	
	private String mapRVToString(int rv)
	{
		switch (rv) {
			case USER_TYPE_RV_YES: return "yes";
			case USER_TYPE_RV_NO: return "no";
			default: return "error";
		}
	}
}