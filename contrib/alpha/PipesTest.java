import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import uk.co.uwcs.choob.modules.Modules;
import uk.co.uwcs.choob.support.IRCInterface;
import uk.co.uwcs.choob.support.events.Message;

class Pipes
{
	final Modules mods;
	private final IRCInterface irc;

	public Pipes(final Modules mods, final IRCInterface irc)
	{
		this.mods = mods;
		this.irc = irc;
	}

	static String firstToken(String s)
	{
		final int ind = s.indexOf(' ');
		if (ind == -1)
			return s;
		return s.substring(0, ind); 
	}
	
	public void commandEval(final Message mes) throws Exception
	{
		irc.sendContextReply(mes, eval(mods.util.getParamString(mes), new Execulator()
				{
					@Override
					public String exec(String s, String stdin) throws Exception
					{
						final String[] qq = s.trim().split(" ", 2);
						String cmd = qq[0].trim();
						String arg = qq.length > 1 ? qq[1] : "";

						if ("sed".equals(cmd))
							return (String)mods.plugin.callAPI("MiscUtils", "Sed", arg, stdin);

						if ("xargs".equals(cmd))
						{
							final String[] rr = arg.split(" ", 2);
							cmd = rr[0];
							arg = (rr.length > 1 ? rr[1] : "") + stdin;
						}
						String[] cmds = cmd.split("\\.", 2);

						return (String) mods.plugin.callGeneric(cmds[0], "command", cmds[1], arg);
					}
				}
			)
		);
	}

	static class ParseException extends Exception
	{
		private StringIterator pos;

		/**
		 * @param pos Location the error occoured.
		 */
		public ParseException(String string, StringIterator pos)
		{
			super(string);
			this.pos = pos;
		}
		
		@Override
		public String toString() {
			return super.toString() + " before " + pos + "$$";
		}
	}

	/** Keep track of the position of ourselves in a String. */
	static class StringIterator
	{
		private int i;
		final private char[] c;

		StringIterator (String s)
		{
			c = s.toCharArray();
		}
		
		char get() throws ParseException
		{
			if (i < c.length)
				return c[i++];
			throw new ParseException("Unexpected end", this);
		}
		
		char peek() throws ParseException
		{
			if (i < c.length)
				return c[i];
			throw new ParseException("Peek past end", this);
		}
		
		boolean hasMore()
		{
			return i < c.length;
		}
		
		@Override
		public String toString()
		{
			return new String(c, i, c.length - i);
		}
		
		public int length()
		{
			return c.length - i;
		}
	}

	/** @param si Positioned just after the ( in a valid $(; will terminate just after the ). 
	 * @throws Exception iff comes from Execulator.  */
	private static String eval(final StringIterator si, Execulator e) throws Exception
	{
		StringBuilder sb = new StringBuilder(si.length());
		boolean dquote = false, squote = false, bslash = false;
		String stdin = "";

		while (si.hasMore())
		{
			final char c = si.get();
			if (!squote && !dquote && !bslash && '$' == c && '(' == si.peek())
			{
				si.get();
				sb.append(eval(si, e));
			}
			else if (!squote && !dquote && !bslash && ')' == c)
			{
				if (bslash || dquote || squote)
					throw new ParseException("Unexpected end of expression", si);
				return e.exec(sb.toString(), stdin);
			}
			else if ('"' == c)
				if (squote || bslash)
				{
					sb.append(c);
					bslash = false;
				}
				else
					dquote = !dquote;
			else if ('\'' == c)
				if (bslash)
				{
					sb.append(c);
					bslash = false;
				}
				else
					squote = !squote;
			else if ('\\' == c)
				if (bslash)
				{
					sb.append(c);
					bslash = false;
				}
				else if (squote)
					sb.append(c);
				else
					bslash = true;
			else if ('|' == c)
			{
				if (!squote && !dquote && !bslash)
				{
					stdin = e.exec(sb.toString(), stdin);
					sb.setLength(0);
				}
			}
			else
			{
				if (bslash)
					throw new ParseException("illegal escape sequence: " + c, si);
				sb.append(c);
			}
		}
		throw new ParseException("Expected )", si);
	}
	
	static String eval(String s, Execulator e) throws Exception
	{
		final StringIterator si = new StringIterator(s + ")");
		final String res = eval(si, e);
		// XXX #testEnd
		if (si.hasMore() && (si.get() != ')' || si.hasMore()))
			throw new ParseException("Trailing characters", si);
		return res;
	}
	
	static String eval(String s) throws Exception
	{
		return eval(s, SysoExeculator);
	}
	
	static interface Execulator
	{
		String exec(String s, String stdin) throws Exception;
	}
	
	private final static Execulator SysoExeculator = new Execulator()
	{

		@Override
		public String exec(String s, String stdin) 
		{
			System.out.println(s + " with stdin of: " + stdin);
			return s;
		}
	};
}

public class PipesTest
{
	@Test
	public void testExeculator() throws Exception
	{
		assertEquals("NOOOMaNOOOM789MOOONbMOOON", Pipes.eval("a$(789)b", new Pipes.Execulator()
		{
				@Override
				public String exec(String s, String stdin) {
					return "NOOOM" + s + stdin + "MOOON"; 
				}
		}));
	}

	@Test
	public void testTrivialDollars() throws Exception
	{
		assertEquals("6", Pipes.eval("$(6)"));
		assertEquals("56", Pipes.eval("5$(6)"));
		assertEquals("67", Pipes.eval("$(6)7"));
		assertEquals("567", Pipes.eval("5$(6)7"));
	}
	
	@Test(expected = Pipes.ParseException.class)
	public void testStart() throws Exception
	{
		Pipes.eval("$(");
	}

	// Fails, XXX above.
	@Test(expected = Pipes.ParseException.class)
	public void testEnd() throws Exception
	{
		Pipes.eval(")");
	}
	
	private static final String DQUOTED = "\"pony\"";

	@Test
	public void testEvalQuotes() throws Exception
	{
		assertEquals("pony", Pipes.eval(DQUOTED));
		assertEquals("pony pony", Pipes.eval(DQUOTED + " " + DQUOTED));
		assertEquals("\"pony\"", Pipes.eval("'" + DQUOTED + "'"));
		assertEquals("\"pony\" \"pony\"", Pipes.eval("'" + DQUOTED + "' '" + DQUOTED + "'"));
		assertEquals("pony", Pipes.eval("'pony'"));
		assertEquals("pony pony", Pipes.eval("'pony' 'pony'"));
		assertEquals("pony \"pony", Pipes.eval("'pony \"pony'"));
		assertEquals("pony \"\"pony", Pipes.eval("'pony \"\"pony'"));
	}

	@Test
	public void testEvalQuotesSlash() throws Exception
	{
		assertEquals("po\\\"ny", Pipes.eval("'po\\\"ny'"));
		assertEquals("po\"ny", Pipes.eval("\"po\\\"ny\""));
		assertEquals("'", Pipes.eval("\\'"));
		assertEquals("pony'pony", Pipes.eval("'pony'\\''pony'"));
		assertEquals("pony\\pony", Pipes.eval("\"pony\\\\pony\""));
	}

	@Test
	public void testEvalEnd() throws Exception
	{
		assertThrowsParse("'");
		assertThrowsParse("\"");
		assertThrowsParse("\\");
		assertThrowsParse("\\q");
	}

	@Test
	public void testEvalDollars() throws Exception
	{
		assertEquals("pony", Pipes.eval("$('pony')"));
		assertEquals("ls() qq", Pipes.eval("$('ls()' 'qq')"));
		assertEquals("ls() qq", Pipes.eval("$('ls()' qq)"));

		assertEquals("ls$() qq", Pipes.eval("$('ls$()' 'qq')"));
		assertEquals("ls$() qq", Pipes.eval("$('ls$()' qq)"));

		assertEquals("57", Pipes.eval("$(5)7"));

	}

	private static void assertThrowsParse(String string) throws Exception
	{
		try
		{
			Pipes.eval(string);
			fail("Didn't throw");
		}
		catch (Pipes.ParseException e)
		{
			// expected
		}
	}
}
