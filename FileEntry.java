/**
 * Title:        XJ Sync
 * Description:  Ordnerabgleich für Mac/Linux/Windows
 * Copyright:    Copyright (c) 2003
 * Company:      CRESD GmbH
 * @author Martin Gäckler
 * @version 1.0
 */

import java.io.*;

/**
 * <p>Contains information for an entry in the database</p>
 */
class FileEntry
{
	private boolean		isDeleted = false;
	private boolean		isDirectory = false;
	private String		fileName = "";
	private String		filePath = "";
	private FileData	leftData = null;
	private String		operation = "nix";
	private FileData	rightData = null;

	public long copySize = 0;
	public long copyTime = 0;

	public static final int SYNC_LEFT_RIGHT    = 0;
	public static final int SYNC_RIGHT_LEFT    = 1;
	public static final int SYNC_BOTH          = 2;

	public static final int SYNC_DELETE_PASS	= 0;
	public static final int SYNC_FILE_PASS		= 1;
	public static final int SYNC_DIR_PASS		= 2;

	public FileEntry( String theNewPath, String fileName, boolean theNewDirectoryFlag  )
	{
		filePath = theNewPath;
		this.fileName = fileName;
		isDirectory = theNewDirectoryFlag;
	}
	public void setFilePath( String newFilePath )
	{
		File	theFile = new File(newFilePath);

		filePath = newFilePath;
		fileName = theFile.getName();
	}
	public void setLeftFsData( String name, long size, long modified )
	{
		if( leftData == null )
			leftData = new FileData();
		leftData.setFsData( name, size, modified );
	}
	public void setLeftDbData( long size, long modified )
	{
		if( leftData == null )
			leftData = new FileData();
		leftData.setDbData( size, modified );
	}

	public void setRightFsData( String name, long size, long modified )
	{
		if( rightData == null )
			rightData = new FileData();
		rightData.setFsData( name, size, modified );
	}
	public void setRightDbData( long size, long modified )
	{
		if( rightData == null )
			rightData = new FileData();
		rightData.setDbData( size, modified );
	}

	public Object getData( int index )
	{
		switch( index )
		{
			case 0:
			{
				if( leftData != null
				&& rightData != null
				&& !leftData.getFsName().equals(rightData.getFsName()) )
					return filePath + " ( "+leftData.getFsName()+" / "+rightData.getFsName() +" )";
				else
					return filePath;
			}
			case 1:
			{
				if( leftData != null )
					return new Long( leftData.getFsSize() );
				else
					return "";
			}
			case 2:
			{
				if( leftData != null )
					return "   " + leftData.getModifiedDateStr();
				else
					return "";
			}
			case 3:
			{
				return "   " + operation;
			}
			case 4:
			{
				if( rightData != null )
					return new Long( rightData.getFsSize() );
				else
					return "";
			}
			case 5:
			{
				if( rightData != null )
					return "   " + rightData.getModifiedDateStr();
				else
					return "";
			}

		}
		return "";
	}
	public String getFilepath()
	{
		return filePath;
	}
	public String getFilename()
	{
		return fileName;
	}
	public long getLeftSize()
	{
		if( leftData != null )
			return leftData.getFsSize();
		else
			return 0;
	}
	public long getLeftDbSize()
	{
		if( leftData != null )
			return leftData.getDbSize();
		else
			return 0;
	}
	public long getLeftModified()
	{
		if( leftData != null )
			return leftData.getFsModified();
		else
			return 0;
	}
	public long getLeftDbModified()
	{
		if( leftData != null )
			return leftData.getDbModified();
		else
			return 0;
	}
	public String getOperation()
	{
		return operation;
	}
	public long getRightSize()
	{
		if( rightData != null )
			return rightData.getFsSize();
		else
			return 0;
	}
	public long getRightDbSize()
	{
		if( rightData != null )
			return rightData.getDbSize();
		else
			return 0;
	}
	public long getRightModified()
	{
		if( rightData != null )
			return rightData.getFsModified();
		else
			return 0;
	}
	public long getRightDbModified()
	{
		if( rightData != null )
			return rightData.getDbModified();
		else
			return 0;
	}
	public boolean getDeleted()
	{
		return isDeleted;
	}

	public boolean hasChanged()
	{
		if( operation.equals( "===" )
		||  operation.equals( "???" )
		||  operation.equals( "Err" )
		||  operation.equals( "<=>" ) )
			return false;
		else
			return true;
	}

	private boolean compareDates( long date1, long date2 )
	{
		boolean changed = false;

		long diff = Math.abs( date1 - date2 );

		if( diff > 2000
		&& (diff < 3598000 || diff > 3602000) )
			changed = true;

		return changed;
	}
	/**
	 * calcOperation calculates the automatic operation depending on the syncMode
	 *
	 * @param syncMode int
	 * @return boolean
	 */
	public boolean calcOperation( int syncMode )
	{
		if( syncMode == SYNC_BOTH )
		{
			boolean	leftNew = false, rightNew = false;
			boolean	leftChanged = false, rightChanged = false;
			boolean	leftDeleted = false, rightDeleted = false;
			boolean	leftUnchanged = false, rightUnchanged = false;
			boolean	leftRenamed = false, rightRenamed = false;

			if( leftData != null )
			{
				if( leftData.getDbModified() == 0 && leftData.getFsModified() != 0 )
					leftNew = true;
				else if( leftData.getDbModified() != 0 && leftData.getFsModified() == 0 )
					leftDeleted = true;
				else if( compareDates( leftData.getFsModified(), leftData.getDbModified() )	)
					leftChanged = true;
				else
					leftUnchanged = true;

				if( !leftData.getFsName().equals(fileName) )
					leftRenamed = true;
			}
			if( rightData != null )
			{
				if( rightData.getDbModified() == 0 && rightData.getFsModified() != 0 )
					rightNew = true;
				else if( rightData.getDbModified() != 0 && rightData.getFsModified() == 0 )
					rightDeleted = true;
				else if( compareDates( rightData.getFsModified(), rightData.getDbModified() ) )
					rightChanged = true;
				else
					rightUnchanged = true;

				if( !rightData.getFsName().equals(fileName) )
					rightRenamed = true;
			}

			if( rightUnchanged && leftData == null )
				operation = "Err";
			else if( leftUnchanged && rightData == null )
				operation = "Err";
			else if( leftChanged && rightUnchanged )
				operation = "-->";
			else if( rightChanged && leftUnchanged )
				operation = "<--";
			else if( leftDeleted && rightUnchanged )
				operation = "->X";
			else if( rightDeleted && leftUnchanged )
				operation = "X<-";
			else if( leftNew && rightData == null )
				operation = "-->";
			else if( rightNew && leftData == null )
				operation = "<--";
			else if( leftUnchanged && rightUnchanged
			&& !compareDates(leftData.getFsModified(), rightData.getFsModified()) )
				operation = "===";
			else if( leftNew && rightNew
			&& !compareDates(leftData.getFsModified(), rightData.getFsModified()) )
				operation = "===";
			else if( leftNew && rightNew && leftData.getFsModified() < rightData.getFsModified() )
				operation = "<--";
			else if( leftNew && rightNew && leftData.getFsModified() > rightData.getFsModified() )
				operation = "-->";
			else if( isDirectory && leftData.getFsModified() < rightData.getFsModified() )
				operation = "<--";
			else if( isDirectory && leftData.getFsModified() > rightData.getFsModified() )
				operation = "-->";
			else if( !compareDates(leftData.getFsModified(), rightData.getFsModified()) )
			{
				operation = "===";
			}
			else
				operation = "???";
			if( operation.equals("===") )
			{
				if (leftRenamed && !rightRenamed)
					operation = "N->";
				else if (!leftRenamed && rightRenamed)
					operation = "<-N";
				else
				{
					leftData.setModified( leftData.getFsModified() );
					rightData.setModified( rightData.getFsModified() );
				}
			}
		}
		else if( syncMode == SYNC_LEFT_RIGHT )
		{
			if( leftData != null && rightData != null
			&& (Math.abs(leftData.getFsModified() - rightData.getFsModified()) == 3600000			// 1 time zone
				|| Math.abs(leftData.getFsModified() - rightData.getFsModified()) <= 2000 ))		// FAT
				operation = "===";
			else if( leftData == null || leftData.getFsModified() == 0 )
				operation = "->X";
			else
				operation = "-->";
		}
		else if( syncMode == SYNC_RIGHT_LEFT )
		{
			if( leftData != null && rightData != null
			&& (Math.abs(leftData.getFsModified() - rightData.getFsModified()) == 3600000			// 1 time zone
				|| Math.abs(leftData.getFsModified() - rightData.getFsModified()) <= 2000 ))		// FAT
				operation = "===";
			else if( rightData == null || rightData.getFsModified() == 0 )
				operation = "X<-";
			else
				operation = "<--";
		}
		else
			operation = "???";

		return hasChanged();
	}

	private boolean renameFile( File source, File destination, File parent, FileData entry )
	{
		boolean	successCode;
		File newDestFile = new File( parent, source.getName() );
		successCode = destination.renameTo(newDestFile);

		if( successCode )
			entry.setFsName( source.getName() );

		return successCode;
	}

	private boolean copyFile( SyncStatus statWin, File source, File destination, FileData entry )
	{
		boolean successCode = true;

		statWin.setCurrentFileProgress(0);
		statWin.setMaxFileProgress(source.length());
		copyTime = System.currentTimeMillis();
		try
		{
			File				parent;
			FileInputStream		sourceStream;
			FileOutputStream	destStream;

			byte				buffer[] = new byte[32768];
			int					dataRead;

			parent = destination.getParentFile();
			parent.mkdirs();
			copySize = 0;
			sourceStream = new FileInputStream( source );
			destStream = new FileOutputStream( destination );

			while( (dataRead = sourceStream.read(buffer)) > 0 )
			{
				copySize += dataRead;
				destStream.write( buffer, 0, dataRead );
				statWin.incrementCurrentDataProgress(dataRead);
				statWin.incrementCurrentFileProgress(dataRead);
			}

			destStream.close();
			sourceStream.close();

			if( !source.getName().equals( destination.getName()) )
			{
				renameFile( source, destination, parent, entry );
			}
		}
		catch( Exception e )
		{
			e.printStackTrace();
			successCode = false;
		}
		copyTime = System.currentTimeMillis()-copyTime;

		return successCode;
	}
	public boolean synchronizeFile( SyncStatus statWin, String leftDirectory, String rightDirectory, int pass )
	{
		boolean	successCode = false;
		boolean	skiped = true;
		boolean renamed = false;
		long	modified;
		long	fileSize;
		File	leftFile, rightFile;
		String	tmpFileName = new String( filePath );

		String  leftFileName = new String( leftDirectory );
		String  rightFileName = new String( rightDirectory );

		leftFileName += File.separatorChar;
		rightFileName += File.separatorChar;

		/*
			remove trailing separator of directories
		*/
		if( tmpFileName.lastIndexOf(File.separatorChar) == tmpFileName.length()-1 )
			tmpFileName = tmpFileName.substring(0, tmpFileName.length()-1);

		/*
			remove the filename portion from the path
		*/
		int	sepPos = tmpFileName.lastIndexOf( File.separatorChar );
		if( sepPos >= 0 )
			tmpFileName = tmpFileName.substring(0,sepPos+1);
		else
			tmpFileName = "";

		leftFileName += tmpFileName;
		leftFileName += leftData != null && leftData.getFsModified()>0
				? leftData.getFsName()
				: rightData.getFsName();
		rightFileName += tmpFileName;
		rightFileName += rightData != null && rightData.getFsModified()>0
				? rightData.getFsName()
				: leftData.getFsName();

		leftFile = new File( leftFileName );
		rightFile = new File( rightFileName );

		if( operation.equals( "-->" ) && pass != SYNC_DELETE_PASS )
		{
			if( !leftFile.isDirectory() )
			{
				if( pass==SYNC_FILE_PASS )
				{
					skiped = false;

					successCode = copyFile(statWin, leftFile, rightFile, rightData );
					if( successCode )
						filePath = tmpFileName + leftData.getFsName();
				}
			}
			else if( pass==SYNC_DIR_PASS )
			{
				if (!rightFile.exists())
					successCode = rightFile.mkdirs();
				else if( leftData != null && rightData != null && !leftData.getFsName().equals(rightData.getFsName()) )
				{
					successCode = renameFile( leftFile, rightFile, rightFile.getParentFile(), rightData );
					filePath = tmpFileName + leftData.getFsName();
					filePath += File.separatorChar;
					if( successCode )
						renamed = true;
				}
				else
					successCode = true;
				skiped = false;
			}

			if( !skiped )
			{
				modified = leftFile.lastModified();
				fileSize = leftFile.length();

				if (successCode)
					successCode = rightFile.setLastModified( modified );

				if (successCode)
				{
					leftData = new FileData();
					leftData.setModified( modified );
					leftData.setSize( fileSize );

					modified = rightFile.lastModified();

					rightData = new FileData();
					rightData.setModified( modified );
					rightData.setSize( fileSize );

					leftData.setFsName(leftFile.getName());
					rightData.setFsName(leftFile.getName());
				}
			}
		}
		else if( operation.equals("<--") && pass != SYNC_DELETE_PASS )
		{
			if( !rightFile.isDirectory() )
			{
				if( pass==SYNC_FILE_PASS )
				{
					skiped = false;

					successCode = copyFile( statWin, rightFile, leftFile, leftData );
					if( successCode )
						filePath = tmpFileName + rightData.getFsName();
				}
			}
			else if( pass==SYNC_DIR_PASS )
			{
				if (!leftFile.exists())
					successCode = leftFile.mkdirs();
				else if( leftData != null && rightData != null && !leftData.getFsName().equals(rightData.getFsName()) )
				{
					successCode = renameFile( rightFile, leftFile, leftFile.getParentFile(), leftData );
					filePath = tmpFileName + rightData.getFsName();
					filePath += File.separatorChar;
					if( successCode )
						renamed = true;
				}
				else
					successCode = true;

				skiped = false;
			}

			if( !skiped )
			{
				modified = rightFile.lastModified();
				fileSize = rightFile.length();

				if( successCode )
					successCode = leftFile.setLastModified( modified );

				if( successCode )
				{
					rightData = new FileData();
					rightData.setModified( modified );
					rightData.setSize( fileSize );

					modified = leftFile.lastModified();

					leftData = new FileData();
					leftData.setModified( modified );
					leftData.setSize( fileSize );

					leftData.setFsName(rightFile.getName());
					rightData.setFsName(rightFile.getName());
				}
			}
		}
		else if( operation.equals("<-N") && pass==SYNC_DIR_PASS )
		{
			skiped = false;

			successCode = renameFile( rightFile, leftFile, leftFile.getParentFile(), leftData );
			filePath = tmpFileName + rightData.getFsName();
			if( isDirectory )
				filePath += File.separatorChar;
			if( successCode )
				renamed = true;
		}
		else if( operation.equals("N->") && pass==SYNC_DIR_PASS )
		{
			skiped = false;

			successCode = renameFile( leftFile, rightFile, rightFile.getParentFile(), rightData );
			filePath = tmpFileName + leftData.getFsName();
			if( isDirectory )
				filePath += File.separatorChar;
			if( successCode )
				renamed = true;
		}
		else if( operation.equals( "X<-" ) && pass==SYNC_DELETE_PASS )
		{
			skiped = false;
			successCode = leftFile.delete();
			if( successCode )
				isDeleted = true;
		}
		else if( operation.equals( "->X" ) && pass==SYNC_DELETE_PASS )
		{
			skiped = false;
			successCode = rightFile.delete();
			if( successCode )
				isDeleted = true;
		}
		else if( operation.equals( "X<->X" ) && pass==SYNC_DELETE_PASS )
		{
			skiped = false;
			successCode = rightFile.delete();
			if( successCode )
				successCode = leftFile.delete();
			if( successCode )
				isDeleted = true;
		}
		if( !skiped )
		{
			if (successCode)
				operation = "<=>";
			else
				operation = "Err";
		}

		return renamed;
	}

	public boolean copyLeftRight()
	{
		if( leftData != null )
			operation = "-->";
		return hasChanged();
	}
	public boolean copyRightLeft()
	{
		if( rightData != null )
			operation = "<--";
		return hasChanged();
	}
	public boolean deleteRight()
	{
		if( rightData != null )
			operation = "->X";
		return hasChanged();
	}
	public boolean deleteLeft()
	{
		if( leftData != null )
			operation = "X<-";
		return hasChanged();
	}
	public boolean deleteBoth()
	{
		if( leftData != null && rightData != null )
			operation = "X<->X";
		return hasChanged();
	}
	public boolean renameLeft()
	{
		if( leftData != null )
			operation = "<-N";
		return hasChanged();
	}
	public boolean renameRight()
	{
		if( leftData != null )
			operation = "N->";
		return hasChanged();
	}
}
