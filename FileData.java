/**
 * Title:        XJ Sync
 * Description:  Ordnerabgleich für Mac/Linux/Windows
 * Copyright:    Copyright (c) 2003
 * Company:      CRESD GmbH
 * @author Martin Gäckler
 * @version 1.0
 */

import java.util.Date;
import java.text.DateFormat;

/**
 * <p>Description: Contains information about one file in the database and/or filesystem</p>
 *
 */
class FileData
{
	private long	fsSize		= 0;
	private long	fsModified	= 0;
	private String	fsName		= "";

	private long	dbSize		= 0;
	private long	dbModified	= 0;


	/**
	 * setModified sets the modification date of the entry
	 *
	 * @param modified long
	 */
	public void setModified( long modified )
	{
		fsModified = dbModified = modified / 1000;
	}
	public void setSize( long size )
	{
		fsSize = dbSize = size;
	}
	public void setFsData( String name, long size, long modified )
	{
		fsName = name;
		fsSize = size;
		fsModified = modified / 1000;
	}
	public void setFsName( String name )
	{
		fsName = name;
	}

	/**
	 * setDbData sets the date and size values for the database
	 *
	 * @param size long
	 * @param modified long
	 */
	public void setDbData( long size, long modified )
	{
		dbSize = size;
		dbModified = modified / 1000;
	}
	public String getFsName()
	{
		return fsName;
	}
	public long getFsSize()
	{
		return fsSize;
	}
	public long getFsModified()
	{
		return fsModified * 1000;
	}

	public long getDbSize()
	{
		return dbSize;
	}
	public long getDbModified()
	{
		return dbModified * 1000;
	}

	public String getModifiedDateStr()
	{
		String	modDateStr = "";

		if( fsModified > 0 )
		{
			Date		theDate = new Date( fsModified * 1000);
			DateFormat	df = DateFormat.getInstance();
			modDateStr = df.format(theDate);
		}
		else
			modDateStr = "-";

		if( dbModified > 0 )
		{
			if( dbModified != fsModified )
			{
				Date theDate = new Date(dbModified * 1000);
				DateFormat df = DateFormat.getInstance();
				modDateStr += " (" + df.format(theDate) + ")";
			}
		}
		else
			modDateStr += " (-)";

		return modDateStr;
	}
}
