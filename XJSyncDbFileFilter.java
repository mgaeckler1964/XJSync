import java.io.File;

import javax.swing.filechooser.FileFilter;

/**
 * Title:        XJ Sync
 * Description:  Ordnerabgleich für Mac/Linux/Windows
 * Copyright:    Copyright (c) 2003
 * Company:      CRESD GmbH
 * @author Martin Gäckler
 * @version 1.0
 */

class XJSyncDbFileFilter extends FileFilter
{
	private String getExtension( File theFile )
	{
		if( theFile != null )
		{
			String filename = theFile.getName();
			int i = filename.lastIndexOf('.');
			if( i>0 && i<filename.length()-1 )
			{
				return filename.substring(i+1).toLowerCase();
			};
		}
		return null;
	}
	public String getDescription()
	{
		return "XJ Sync Datenbanken ( *.xjSyncDB, *.xsd )";
	}
	public boolean accept( File theFile )
	{
		if( theFile != null )
		{
			if( theFile.isDirectory())
			{
				return true;
			}
			String extension = getExtension( theFile );
			if( extension != null
			&& (extension.equals("xjsyncdb") || extension.equals("xsd")) )
			{
				return true;
			};
		}
		return false;
	}
}
