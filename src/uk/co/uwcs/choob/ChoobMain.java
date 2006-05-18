/*
 * ChoobMain.java
 *
 * Created on June 1, 2005, 2:20 AM
 */

package uk.co.uwcs.choob;

/**
 * Main class in the Choob project, simply creates a Choob instance.
 */
public final class ChoobMain
{
	public static void main(String[] args)
	{
		try
		{
			new Choob();
		}
		catch (Throwable t)
		{
			System.err.println("Fatal error in Choob, exiting.");
			System.exit(1);
		}

	}
}