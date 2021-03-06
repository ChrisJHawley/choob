package uk.co.uwcs.choob.support;

import java.security.BasicPermission;
import java.util.List;

/**
 * Haxxy hack of a permission to enable us to get a stack trace of plugins.
 * @author bucko
 */

public final class ChoobSpecialStackPermission extends BasicPermission
{
	private static final long serialVersionUID = -559099153149660237L;
	private final List<String> haxList;
	private List<String> startList;
	public ChoobSpecialStackPermission(final List<String> haxList)
	{
		super("HAX");
		this.haxList = haxList;
		this.startList = null;
	}

	public void add(final String pluginName)
	{
		// Insert item at start to ensure stack is correctly ordered.
		haxList.add(0, pluginName);
	}

	public void root(final List<String> list)
	{
		startList = list;
	}

	public void patch()
	{
		if (startList != null)
			haxList.addAll(startList);
	}
}
