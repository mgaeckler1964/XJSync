import javax.swing.UIManager;
import java.awt.*;
import com.gaklib.MessageBox;

/**
 * Title:        XJ Sync
 * Description:  Ordnerabgleich für Mac/Linux/Windows
 * Copyright:    Copyright (c) 2003
 * Company:      CRESD GmbH
 * @author Martin Gäckler
 * @version 1.0
 */

public class XJsyncApp
{
	boolean packFrame = false;

	static final int MODE_LOAD_DB		= 1;
	static final int MODE_LOAD_LEFT		= 2;
	static final int MODE_LOAD_RIGHT	= 3;
	static final int MODE_EXECUTE_DB	= 4;

	int mode = MODE_LOAD_DB;

	/**
	 * Construct the application
	 *
	 * @param args String[] - commandline argument
	 */
	public XJsyncApp(String[] args)
	{
		int			i;
		String		arg;
		XJsyncFrame	frame = new XJsyncFrame();
		//Validate frames that have preset sizes
		//Pack frames that have useful preferred size info, e.g. from their layout
		if (packFrame)
		{
			frame.pack();
		}
		else
		{
			frame.validate();
		}

		frame.setVisible(true);
		for( i=0; i<args.length; i++ )
		{
			arg = args[i];

			while( arg.length() > 0
			&&  arg.charAt(arg.length()-1) == '\\'
			&&  i<args.length-1)
			{
				arg = arg.substring(0, arg.length()-1);
				arg += ' ';
				arg += args[++i];
			}

			if( arg.length() > 0 )
			{
				if( arg.equals( "-e" ) )
					mode = MODE_EXECUTE_DB;
				else if( arg.equals( "-l" ) )
					mode = MODE_LOAD_DB;
				else if( arg.charAt(0) != '-' )
				{
					if( mode == MODE_LOAD_DB )
						frame.loadXSyncDB( arg );
					else if( mode == MODE_EXECUTE_DB )
						frame.syncDB( arg );

				}
			}
		}
	}

	/**Main method*/
	public static void main(String[] args)
	{
		/*
		this code is not required it can be handled by the java application stub

		boolean isMacOS = System.getProperty("mrj.version") != null;
		if( isMacOS )
		{
			System.setProperty( "apple.laf.useScreenMenuBar", "true" );
			System.setProperty( "com.apple.mrj.application.apple.menu.about.name", "XJ Sync" );
		}
		*/
		try
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		new XJsyncApp( args );
	}
}
