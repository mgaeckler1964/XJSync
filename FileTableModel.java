import java.io.*;
import java.util.*;
import java.text.NumberFormat;

import javax.swing.table.AbstractTableModel;

import com.gaklib.Lock;
import com.gaklib.MessageBox;

/**
 * Title:        XJ Sync
 * Description:  Ordnerabgleich für Mac/Linux/Windows
 * Copyright:    Copyright (c) 2003
 * Company:      CRESD GmbH
 * @author Martin Gäckler
 * @version 1.0
 */

class FileTableModel extends AbstractTableModel
{
	private int		m_numChangedFiles = 0;
	private String	m_xjSyncDBfile = "";
	private String	m_leftDirectory = null;
	private String	m_rightDirectory = null;
	private boolean	m_caseSensitive = false;
	private String	m_filter = null;
	private long	m_startClock = 0;
	private Lock	m_lock = new Lock();

	private final String[] m_columnNames =
	{
		"Name",
		"L-Größe", "L-Datum",
		"Operation",
		"R-Größe", "R-Datum"
	};
	private FileVector	m_data = null;
	private FileVector	m_filtered = null;
	private int			m_sortOrder = FileComparator.SORT_NAME;
	private int			m_syncMode  = FileEntry.SYNC_BOTH;

	public synchronized boolean lock()
	{
		return m_lock.tryLock();
	}
	public synchronized boolean freeLock()
	{
		return m_lock.freeLock();
	}
	public int getNumChangedFiles()
	{
		return m_numChangedFiles;
	}
	public int getColumnCount()
	{
		return m_columnNames.length;
	}

	public int getRowCount( boolean exclDeleted )
	{
		if( m_data == null )
		{
			return 0;
		}
		else
		{
			return m_data.getNumEntries(exclDeleted);
		}
	}

	public int getRowCount()
	{
		return getRowCount( false );
	}

	public String getColumnName(int col)
	{
		return m_columnNames[col];
	}

	public Object getValueAt(int row, int col)
	{
		FileEntry   theFileEntry;
		if( m_data == null )
		{
			return null;
		}
		else
		{
			theFileEntry = (FileEntry)m_data.elementAt( row );
			return theFileEntry.getData(col);
		}
	}

	public Class getColumnClass(int c)
	{
		return getValueAt(0, c).getClass();
	}

	public void setCaseSensitive( boolean newCaseSensitive )
	{
		m_caseSensitive = newCaseSensitive;
	}

	public boolean getCaseSensitive( )
	{
		return m_caseSensitive;
	}

	private void calculateOperations( SyncStatus statWin )
	{
		int			i;
		FileEntry	fileFound;

		if( statWin != null )
		{
			statWin.setMaxProgress(m_data.size());
		}
		m_numChangedFiles = 0;
		for( i=0; i<m_data.size(); i++ )
		{
			if( statWin != null )
			{
				statWin.setCurrentProgress(i);
			}
			fileFound = (FileEntry)m_data.elementAt(i);
			if( fileFound.calcOperation( m_syncMode ) )
			{
				m_numChangedFiles++;
			}
		}
	}
	private void loadFileData(
		String		subDirectory,
		SyncStatus	statWin,
		boolean		initStatWin,
		boolean		incrStatWin
	)
	{
		boolean		isDirectory;
		String		leftPath = new String( m_leftDirectory );
		String		rightPath = new String( m_rightDirectory );

		if( subDirectory != null && !subDirectory.equals("") )
		{
			leftPath += File.separatorChar;
			rightPath += File.separatorChar;

			leftPath += subDirectory;
			rightPath += subDirectory;

			if( m_data.size() > 0 )
			{
				statWin.setStatusText(
					"Lade FS " + m_data.size() + ": " +
					( (System.currentTimeMillis() - m_startClock) / m_data.size()) +
					" " +
					subDirectory
					);
			}
		}

		File		leftDirFile = new File( leftPath );
		File		rightDirFile = new File( rightPath );

		File		leftFiles[] = leftDirFile.listFiles();
		File		rightFiles[] = rightDirFile.listFiles();

		int			leftMax = ( leftFiles != null ) ? leftFiles.length : 0;
		int			rightMax = ( rightFiles != null ) ? rightFiles.length : 0;

		FileVector	dirData = new FileVector();

		FileEntry	theFile;
		String		theFilePath;
		int			dirLength, i;

		if( initStatWin )
		{
			statWin.setMaxProgress( leftMax + rightMax );
			m_startClock = System.currentTimeMillis();
		}

		// handle the left directory
		dirLength = m_leftDirectory.length()+1;
		for( i=0; i<leftMax && !statWin.cancelFlag; i++ )
		{
			theFilePath = leftFiles[i].getAbsolutePath();
			theFilePath = theFilePath.substring(dirLength);

			if( leftFiles[i].isDirectory() )
			{
				isDirectory = true;
				theFilePath += File.separatorChar;
				loadFileData(
					theFilePath,
					statWin, false, incrStatWin
				);
			}
			else
			{
				isDirectory = false;
			}
			theFile = new FileEntry(
				theFilePath, leftFiles[i].getName(), isDirectory
			);
			theFile.setLeftFsData(
				leftFiles[i].getName(),
				leftFiles[i].length(),
				leftFiles[i].lastModified()
			);
			dirData.addElement( theFile );
			if( initStatWin || incrStatWin )
			{
				statWin.incrementCurrentProgress(1);
			}
		}

		dirLength = m_rightDirectory.length()+1;
		for( i=0; i<rightMax && !statWin.cancelFlag; i++ )
		{
			theFilePath = rightFiles[i].getAbsolutePath();
			theFilePath = theFilePath.substring(dirLength);

			if( rightFiles[i].isDirectory() )
			{
				theFilePath += File.separatorChar;
				isDirectory = true;
			}
			else
				isDirectory = false;

			theFile = dirData.findFile( theFilePath, m_caseSensitive );
			if( theFile != null )
			{
				theFile.setRightFsData(
					rightFiles[i].getName(),
					rightFiles[i].length(),
					rightFiles[i].lastModified()
				);
			}
			else
			{
				theFile = new FileEntry( theFilePath, rightFiles[i].getName(), isDirectory );
				theFile.setRightFsData(
					rightFiles[i].getName(),
					rightFiles[i].length(),
					rightFiles[i].lastModified()
				);
				dirData.addElement( theFile );
				if( initStatWin || incrStatWin )
					statWin.incrementCurrentProgress(1);

				if( rightFiles[i].isDirectory() )
				{
					loadFileData(
						theFilePath,
						statWin, false, incrStatWin
					);
				}
			}
		}

		m_data.merge(dirData);
	}
	public void loadFileData( String directory, boolean left, SyncStatus statWin )
	{
		if( left )
		{
			m_leftDirectory = new String(directory);
		}
		else
		{
			m_rightDirectory = new String(directory);
		}
		if( m_leftDirectory != null && !m_leftDirectory.equals( "" )
		&&  m_rightDirectory != null && !m_rightDirectory.equals( "" ) )
		{
			m_filtered = new FileVector();
			m_data = new FileVector();
			loadFileData( "", statWin, true, false );
			calculateOperations( statWin );

			m_data.sort( new FileComparator(m_sortOrder) );
			fireTableDataChanged();
		}
	}

	public void saveXSyncDB() throws IOException
	{
		int			row;
		FileEntry	theFileEntry;

		if( m_xjSyncDBfile == null || m_xjSyncDBfile.equals( "" ) )
		{
			return;
		}
		DataOutputStream theFile = new DataOutputStream(
			new FileOutputStream( m_xjSyncDBfile )
		);

		theFile.writeBytes( "xjSyncDB 01.02.00\n" );

		theFile.writeBytes(String.valueOf(m_sortOrder));
		theFile.writeBytes( "\n" );

		theFile.writeBytes(String.valueOf(m_syncMode));
		theFile.writeBytes( "\n" );

		if( m_leftDirectory != null )
		{
			theFile.writeBytes(m_leftDirectory);
		}
		theFile.writeBytes( "\n" );

		if( m_rightDirectory != null )
		{
			theFile.writeBytes(m_rightDirectory);
		}
		theFile.writeBytes( "\n" );

		if( m_filter != null )
		{
			theFile.writeBytes(m_filter);
		}
		theFile.writeBytes( "\n" );

		theFile.writeBytes( m_caseSensitive ? "true\n" : "false\n" );

		theFile.writeBytes(String.valueOf(getRowCount( true )));
		theFile.writeBytes( "\n" );

		if( m_syncMode == FileEntry.SYNC_BOTH )
		{
			for( row=0; row<getRowCount(); row++ )
			{
				theFileEntry = (FileEntry)m_data.elementAt( row );

				if( !theFileEntry.getDeleted() )
				{
					theFile.writeBytes(theFileEntry.getFilepath());
					theFile.writeBytes("\n");

					theFile.writeBytes(String.valueOf(theFileEntry.getLeftDbSize()));
					theFile.writeBytes("\n");

					theFile.writeBytes(String.valueOf(theFileEntry.getLeftDbModified()));
					theFile.writeBytes("\n");

					theFile.writeBytes(theFileEntry.getOperation());
					theFile.writeBytes("\n");

					theFile.writeBytes(String.valueOf(theFileEntry.getRightDbSize()));
					theFile.writeBytes("\n");

					theFile.writeBytes(String.valueOf(theFileEntry.getRightDbModified()));
					theFile.writeBytes("\n");
				}
			}
		}

		theFile.close();
	}

	public void saveXSyncDB( String dbFileName ) throws IOException
	{
		int slashPos	= dbFileName.lastIndexOf( File.separatorChar );
		int dotPos		= dbFileName.lastIndexOf( '.' );

		if( dotPos <= slashPos )
			dbFileName += ".xjSyncDb";

		m_xjSyncDBfile = dbFileName;
		saveXSyncDB();
	}

	public void loadXSyncDB( String dbFileName, SyncStatus statWin ) throws IOException
	{
		int			i,j;
		String		versionStr, sortOrderStr, syncModeStr, sizeStr;
		int			dataSize;
		String		entryName;
		String		leftSizeStr, leftDateStr;
		long		leftSize, leftDate;
		String		operation;
		String		rightSizeStr, rightDateStr;
		long		rightSize, rightDate;
		FileEntry	theFileEntry;

		m_xjSyncDBfile = dbFileName;
		BufferedReader theFile = new BufferedReader(
			new FileReader(dbFileName)
		);

		// read the header
		versionStr = theFile.readLine();

		sortOrderStr = theFile.readLine();
		m_sortOrder = Integer.parseInt( sortOrderStr );

		syncModeStr = theFile.readLine();
		m_syncMode = Integer.parseInt( syncModeStr );

		m_leftDirectory = theFile.readLine();
		m_rightDirectory = theFile.readLine();

		if( versionStr.equals("xjSyncDB 01.01.00")
		||  versionStr.equals("xjSyncDB 01.02.00") )
		{
			m_filter = theFile.readLine();
		}
		else
		{
			m_filter = null;
		}
		if( versionStr.equals("xjSyncDB 01.02.00") )
		{
			m_caseSensitive = theFile.readLine().equals("true");
		}
		else
		{
			m_caseSensitive = false;
		}
		sizeStr = theFile.readLine();
		dataSize = Integer.parseInt(sizeStr);

		if( m_syncMode == FileEntry.SYNC_BOTH )
		{
			statWin.setMaxProgress(dataSize * 2);
		}
		else
		{
			statWin.setMaxProgress(dataSize);
		}
		m_startClock = System.currentTimeMillis();

		m_filtered = new FileVector();
		m_data = new FileVector();
		m_data.ensureCapacity( dataSize );

		loadFileData( "", statWin, false, true );

		if( m_syncMode == FileEntry.SYNC_BOTH )
		{
			m_data.sort( new FileComparator(FileComparator.SORT_NAME) );

			statWin.setMaxProgress( dataSize + m_data.size() );
			try
			{
				while (theFile.ready() && !statWin.cancelFlag) {
					statWin.incrementCurrentProgress(1);
					entryName = theFile.readLine();
					statWin.setStatusText("Lade DB: " + entryName);
					leftSizeStr = theFile.readLine();
					leftDateStr = theFile.readLine();
					operation = theFile.readLine();
					rightSizeStr = theFile.readLine();
					rightDateStr = theFile.readLine();

					theFileEntry = m_data.findFileFast(entryName, m_caseSensitive);
					if (theFileEntry != null)
					{
						if (!leftSizeStr.equals("") && !leftDateStr.equals(""))
						{
							leftSize = Long.parseLong(leftSizeStr);
							leftDate = Long.parseLong(leftDateStr);
							if (leftDate > 0)
							{
								theFileEntry.setLeftDbData(leftSize, leftDate);
							}
							else
							{
								theFileEntry.setLeftDbData(
									theFileEntry.getLeftSize(),
									theFileEntry.getLeftModified()
								);
							}
						}
						if (!rightSizeStr.equals("") && !rightDateStr.equals("")) {
							rightSize = Long.parseLong(rightSizeStr);
							rightDate = Long.parseLong(rightDateStr);
							if (rightDate > 0)
							{
								theFileEntry.setRightDbData(
									rightSize,
									rightDate
								);
							}
							else
							{
								theFileEntry.setRightDbData(
									theFileEntry.getRightSize(),
									theFileEntry.getRightModified()
								);
							}
						}
					}
				}
			}
			catch( Throwable e )
			{
				MessageBox	msgBox = new MessageBox(
					"Error Loading Database", e, MessageBox.OK_BUTTON
				);
				msgBox.show();
			}
		}

		theFile.close();

		calculateOperations( statWin );

		setFilter( m_filter );
	}

	public String getXjSyncDBfile()
	{
		return m_xjSyncDBfile;
	}
	public String getLeftDirectory()
	{
		return m_leftDirectory;
	}
	public String getRightDirectory()
	{
		return m_rightDirectory;
	}

	public void resort( int newSortOrder )
	{
		m_sortOrder = newSortOrder;
		m_data.sort( new FileComparator(m_sortOrder) );
		fireTableDataChanged();
	}
	public int getSortOrder( )
	{
		return m_sortOrder;
	}
	public void recalculateOperation( int newSyncMode )
	{
		m_syncMode = newSyncMode;
		calculateOperations( null );
		if( m_sortOrder == FileComparator.SORT_OPERATION )
		{
			m_data.sort(new FileComparator(m_sortOrder));
		}
		fireTableDataChanged();
	}
	public int getSyncMode()
	{
		return m_syncMode;
	}

	public void synchronizeFiles( SyncStatus statWin ) throws IOException
	{
		boolean			renamed;
		long			copySize = 0, copyTime = 0, maxCopyData = 0, tmp;
		NumberFormat	form = NumberFormat.getInstance();
		int				progress, row, maxRows, renameRow;
		String			statusText, newName, lowerName, newPath;
		FileEntry		theFileEntry, theRenameEntry;

		m_numChangedFiles = 0;
		maxRows = getRowCount();

		m_data.sort( new FileComparator( FileComparator.SORT_NAME ) );
		maxCopyData = progress = 0;
		statWin.setMaxProgress(maxRows*3);
		for( row=maxRows-1; row>=0 && !statWin.cancelFlag; row--, progress++ )
		{
			statWin.setCurrentProgress( progress );
			theFileEntry = (FileEntry)m_data.elementAt( row );
			if( theFileEntry.getOperation().equals("<--") )
				maxCopyData += theFileEntry.getRightSize();
			else if( theFileEntry.getOperation().equals("-->") )
				maxCopyData += theFileEntry.getLeftSize();

			statWin.setStatusText("Bearbeite: " + theFileEntry.getFilepath());
			theFileEntry.synchronizeFile(
				statWin, m_leftDirectory, m_rightDirectory, FileEntry.SYNC_DELETE_PASS
			);
		}
		statWin.setCurrentDataProgress( 0 );
		statWin.setMaxDataProgress( maxCopyData );
		for( row=0; row<maxRows && !statWin.cancelFlag; row++, progress++ )
		{
			statWin.setCurrentProgress( progress );
			theFileEntry = (FileEntry)m_data.elementAt( row );

			statusText = "Kopiere: " + theFileEntry.getFilepath();
			statusText += "\r\n" + form.format(copySize) + " bytes / " + form.format(copyTime) + " ms";
			if( copyTime > 1000 )
			{
				statusText += " = " +
					form.format( (int) (copySize / (copyTime / 1000))) +
					" bytes/sec.";
			}
			statusText += " " + form.format( maxCopyData-copySize ) + "/" + form.format(maxCopyData);
			statWin.setStatusText( statusText );
			theFileEntry.synchronizeFile(
				statWin, m_leftDirectory, m_rightDirectory, FileEntry.SYNC_FILE_PASS
			);
			copySize += theFileEntry.copySize;
			copyTime += theFileEntry.copyTime;
			statWin.setCurrentDataProgress( copySize );
		}
		for( row=maxRows-1; row>=0 && !statWin.cancelFlag; row--, progress++ )
		{
			statWin.setCurrentProgress( progress );
			theFileEntry = (FileEntry)m_data.elementAt( row );

			statWin.setStatusText("Bearbeite: " + theFileEntry.getFilepath());
			renamed = theFileEntry.synchronizeFile(
				statWin, m_leftDirectory, m_rightDirectory, FileEntry.SYNC_DIR_PASS
			);
			if( !m_caseSensitive && renamed )
			{
				newName = theFileEntry.getFilepath();
				lowerName = newName.toLowerCase();
				for( renameRow = row+1; renameRow<maxRows; renameRow++ )
				{
					theRenameEntry = (FileEntry)m_data.elementAt( renameRow );
					if( theRenameEntry.getFilepath().toLowerCase().startsWith( lowerName )
					&& !theRenameEntry.getFilepath().startsWith( newName ) )
					{
						newPath = newName + theRenameEntry.getFilepath().substring(newName.length());
						theRenameEntry.setFilePath(newPath);
					}
					else
					{
						break;
					}
				}
			}
		}
		statusText = "Fertig";
		statusText += "\r\n" + form.format(copySize) + " bytes / " + form.format(copyTime) + " ms";
		if( copyTime > 1000 )
		{
			statusText += " = " + form.format((int)(copySize/(copyTime/1000))) + " bytes/sec.";
		}
		statWin.setStatusText( statusText );
		if( m_sortOrder != FileComparator.SORT_NAME )
		{
			m_data.sort(new FileComparator(m_sortOrder));
		}
		fireTableDataChanged();

		saveXSyncDB();
	}

	public void copyLeftRight( int selectedItems[] )
	{
		int			row;
		FileEntry	theFileEntry;

		for( row=0; row<selectedItems.length; row++ )
		{
			theFileEntry = (FileEntry)m_data.elementAt(selectedItems[row]);
			if( theFileEntry.hasChanged() )
			{
				m_numChangedFiles--;
			}
			if( theFileEntry.copyLeftRight() )
			{
				m_numChangedFiles++;
			}
		}
		if( m_sortOrder == FileComparator.SORT_OPERATION )
		{
			m_data.sort(new FileComparator(m_sortOrder));
		}
		fireTableDataChanged();

	}
	public void copyRightLeft( int selectedItems[] )
	{
		int			row;
		FileEntry	theFileEntry;

		for( row=0; row<selectedItems.length; row++ )
		{
			theFileEntry = (FileEntry)m_data.elementAt(selectedItems[row]);
			if( theFileEntry.hasChanged() )
			{
				m_numChangedFiles--;
			}
			if( theFileEntry.copyRightLeft() )
			{
				m_numChangedFiles++;
			}
		}
		if( m_sortOrder == FileComparator.SORT_OPERATION )
		{
			m_data.sort( new FileComparator(m_sortOrder) );
		}
		fireTableDataChanged();

	}
	public void deleteRight( int selectedItems[] )
	{
		int			row;
		FileEntry	theFileEntry;

		for( row=0; row<selectedItems.length; row++ )
		{
			theFileEntry = (FileEntry)m_data.elementAt(selectedItems[row]);
			if( theFileEntry.hasChanged() )
			{
				m_numChangedFiles--;
			}
			if( theFileEntry.deleteRight() )
			{
				m_numChangedFiles++;
			}
		}
		if( m_sortOrder == FileComparator.SORT_OPERATION )
		{
			m_data.sort(new FileComparator(m_sortOrder));
		}
		fireTableDataChanged();

	}
	public void deleteLeft( int selectedItems[] )
	{
		int			row;
		FileEntry	theFileEntry;

		for( row=0; row<selectedItems.length; row++ )
		{
			theFileEntry = (FileEntry)m_data.elementAt(selectedItems[row]);
			if( theFileEntry.hasChanged() )
			{
				m_numChangedFiles--;
			}
			if( theFileEntry.deleteLeft() )
			{
				m_numChangedFiles++;
			}
		}
		if( m_sortOrder == FileComparator.SORT_OPERATION )
		{
			m_data.sort(new FileComparator(m_sortOrder));
		}
		fireTableDataChanged();
	}
	public void deleteBoth( int selectedItems[] )
	{
		int			row;
		FileEntry	theFileEntry;

		for( row=0; row<selectedItems.length; row++ )
		{
			theFileEntry = (FileEntry)m_data.elementAt(selectedItems[row]);
			if( theFileEntry.hasChanged() )
			{
				m_numChangedFiles--;
			}
			if( theFileEntry.deleteBoth() )
			{
				m_numChangedFiles++;
			}
		}
		if( m_sortOrder == FileComparator.SORT_OPERATION )
		{
			m_data.sort(new FileComparator(m_sortOrder));
		}
		fireTableDataChanged();
	}
	public void renameLeft( int selectedItems[] )
	{
		int			row;
		FileEntry	theFileEntry;

		for( row=0; row<selectedItems.length; row++ )
		{
			theFileEntry = (FileEntry)m_data.elementAt(selectedItems[row]);
			if( theFileEntry.hasChanged() )
			{
				m_numChangedFiles--;
			}
			if( theFileEntry.renameLeft() )
			{
				m_numChangedFiles++;
			}
		}
		if( m_sortOrder == FileComparator.SORT_OPERATION )
		{
			m_data.sort(new FileComparator(m_sortOrder));
		}
		fireTableDataChanged();
	}
	public void renameRight( int selectedItems[] )
	{
		int			row;
		FileEntry	theFileEntry;

		for( row=0; row<selectedItems.length; row++ )
		{
			theFileEntry = (FileEntry)m_data.elementAt(selectedItems[row]);
			if( theFileEntry.hasChanged() )
			{
				m_numChangedFiles--;
			}
			if( theFileEntry.renameRight() )
			{
				m_numChangedFiles++;
			}
		}
		if( m_sortOrder == FileComparator.SORT_OPERATION )
		{
			m_data.sort(new FileComparator(m_sortOrder));
		}
		fireTableDataChanged();
	}
	public void calcOperation( int selectedItems[] )
	{
		int			row;
		FileEntry	theFileEntry;

		for( row=0; row<selectedItems.length; row++ )
		{
			theFileEntry = (FileEntry)m_data.elementAt(selectedItems[row]);
			if( theFileEntry.hasChanged() )
			{
				m_numChangedFiles--;
			}
			if( theFileEntry.calcOperation( m_syncMode ) )
			{
				m_numChangedFiles++;
			}
		}
		if( m_sortOrder == FileComparator.SORT_OPERATION )
		{
			m_data.sort(new FileComparator(m_sortOrder));
		}
		fireTableDataChanged();
	}

	public void setFilter( String filter )
	{
		int				i, row, colonPos;
		FileEntry		theFileEntry;
		String			tmpFilter, regExp, allFilters = filter;
		m_filter = filter;

		if( m_filtered != null )
		{
			for( row=m_filtered.getNumEntries(true)-1; row>=0; row-- )
			{
				theFileEntry = (FileEntry)m_filtered.elementAt( row );
				m_data.add( theFileEntry );
				if( theFileEntry.hasChanged() )
				{
					m_numChangedFiles++;
				}
			}
		}
		m_filtered = new FileVector();

		while( allFilters != null && !allFilters.equals( "" ) )
		{
			regExp = "";
			colonPos = allFilters.indexOf( ';' );
			if( colonPos >= 0 )
			{
				tmpFilter = allFilters.substring(0, colonPos );
				allFilters = allFilters.substring( colonPos+1 );
			}
			else
			{
				tmpFilter = allFilters;
				allFilters = "";
			}
			for( i=0; i<tmpFilter.length(); i++ )
			{
				char	c = tmpFilter.charAt(i);
				if( c == '*' )
					regExp += ".+";
				else if( c == '.' || c=='\\' )
				{
					regExp += '\\';
					regExp += c;
				}
				else
					regExp += c;
			}

			regExp = regExp.toLowerCase();

			for (row = getRowCount() - 1; row >= 0; row--)
			{
				theFileEntry = (FileEntry) m_data.elementAt(row);

				if (theFileEntry.getFilename().toLowerCase().matches(regExp) )
				{
					m_data.remove(row);
					m_filtered.add(theFileEntry);
					if( theFileEntry.hasChanged() )
					{
						m_numChangedFiles--;
					}
				}
			}
		}
		m_data.sort( new FileComparator(m_sortOrder) );
		fireTableDataChanged();
	}
	public String getFilter()
	{
		return m_filter;
	}
}
