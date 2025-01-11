import java.util.*;


/**
 * Title:        XJ Sync
 * Description:  Ordnerabgleich für Mac/Linux/Windows
 * Copyright:    Copyright (c) 2003
 * Company:      CRESD GmbH
 * @author Martin Gäckler
 * @version 1.0
 */

class FileVector extends Vector
{
	FileVector()
	{
		super( 256, 256 );
	}
	public void sort( Comparator comp )
	{
		if( elementData != null && elementData.length > 1 )
			Arrays.sort( elementData, comp );
	}

	public FileEntry findFileFast( String filePath, boolean caseSensitive )
	{
		int			min, max, i, compareVal;
		FileEntry	fileFound = null;
		String		nameToCompare;

		if( !caseSensitive )
			nameToCompare = filePath.toLowerCase();
		else
			nameToCompare = filePath;

		min = 0;
		max = size()-1;
		compareVal = 1;

		while( min <= max && compareVal != 0 )
		{
			i = (min + max) / 2;

			fileFound =  (FileEntry)elementAt(i);
			if( caseSensitive )
				compareVal = fileFound.getFilepath().compareTo(nameToCompare);
			else
				compareVal = fileFound.getFilepath().toLowerCase().compareTo(nameToCompare);
			if( compareVal < 0 )
				min = i+1;
			else if( compareVal > 0 )
				max = i-1;
		}

		if( compareVal == 0 )
		{
			fileFound.setFilePath( filePath );
			return fileFound;
		}
		return null;
	}

	/**
	 * findFile
	 * Searches the database (FileVector) for a specific entry. If caseSensitive is
	 * set to true, the search is case sensitive.
	 * returns a FileEntry found or null if there is none
	 *
	 * @param filePath String
	 * @param caseSensitive boolean
	 * @return FileEntry
	 */
	public FileEntry findFile( String filePath, boolean caseSensitive )
	{
		int			i, max = size();
		FileEntry	fileFound;
		String		foundName;

		if( !caseSensitive )
			filePath = filePath.toLowerCase();

		for( i=0; i<max; i++ )
		{
			fileFound = (FileEntry)elementAt(i);
			foundName = fileFound.getFilepath();
			if( !caseSensitive )
				foundName = foundName.toLowerCase();
			if( foundName.equals( filePath ) )
				return fileFound;
		}

		return null;
	}

	public void merge( FileVector source )
	{
		int		i, max;
		Object	entry;

		max = source.size();
		for( i=0; i<max; i++ )
			addElement( source.elementAt(i) );
	}

	public int getNumEntries( boolean exclDeleted )
	{
		FileEntry	theFileEntry;
		int			row, deletedCount, numEntries = size();

		if( exclDeleted )
		{
			deletedCount = 0;
			for( row=0; row<numEntries; row++ )
			{
				theFileEntry = (FileEntry) elementAt(row);
				if( theFileEntry.getDeleted() )
					deletedCount++;
			}
			numEntries -= deletedCount;
		}

		return numEntries;
	}
};
