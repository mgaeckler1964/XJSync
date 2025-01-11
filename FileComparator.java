import java.util.*;

/**
 * Title:        XJ Sync
 * Description:  Ordnerabgleich für Mac/Linux/Windows
 * Copyright:    Copyright (c) 2003
 * Company:      CRESD GmbH
 * @author Martin Gäckler
 * @version 1.0
 */

class FileComparator implements Comparator
{
	public static final int SORT_NAME       = 0;
	public static final int SORT_LEFT_SIZE  = 1;
	public static final int SORT_LEFT_DATE  = 2;
	public static final int SORT_OPERATION  = 3;
	public static final int SORT_RIGHT_SIZE = 4;
	public static final int SORT_RIGHT_DATE = 5;

	int sortOrder;

	FileComparator( int newSortOrder )
	{
		sortOrder = newSortOrder;
	}
	private int compareOperation( String oper1, String oper2 )
	{
		// check for nulls
		if( oper1 == null && oper2 != null )
			return -1;
		if( oper2 == null && oper1 != null )
			return 1;
		if( oper1 == null && oper2 == null )
			return 0;

		// check equals
		if( oper1.equals( oper2 ) )
			return 0;

		// error operations allways first
		if( oper1.equals( "Err" ) )
			return -1;
		if( oper2.equals( "Err" ) )
			return 1;

		// no operations allways last
		if( oper1.equals( "===" ) )
			return 1;
		if( oper2.equals( "===" ) )
			return -1;

		// all other cases normal compare
		return oper1.compareTo(oper2);

	}

	/**
	 * compare zwo file entries
	 *
	 * @param fileEntry1 Object first file entry
	 * @param fileEntry2 Object second file entry
	 * @return int -1, 0, +1
	 */
	public int compare( Object fileEntry1, Object fileEntry2 )
	{
		int		sortResult = 0;

		if( fileEntry1 == null && fileEntry2 != null )
			sortResult = 1;
		else if( fileEntry1 != null && fileEntry2 == null )
			sortResult = -1;
		else if( fileEntry1 == null && fileEntry2 == null )
			sortResult = 0;
		else if( !(fileEntry1 instanceof FileEntry && fileEntry2 instanceof FileEntry ) )
			throw new ClassCastException();
		else
		{
			FileEntry	fe1 = (FileEntry)fileEntry1;
			FileEntry	fe2 = (FileEntry)fileEntry2;

			long    sort1Value = 0, sort2Value = 0;

			switch( sortOrder )
			{
				case SORT_LEFT_SIZE:
					sort1Value = fe1.getLeftSize();
					sort2Value = fe2.getLeftSize();
					break;
				case SORT_LEFT_DATE:
					sort1Value = fe1.getLeftModified();
					sort2Value = fe2.getLeftModified();
					break;
				case SORT_OPERATION:
					sortResult = compareOperation(
						fe1.getOperation(), fe2.getOperation()
					);
					break;
				case SORT_RIGHT_SIZE:
					sort1Value = fe1.getRightSize();
					sort2Value = fe2.getRightSize();
					break;
				case SORT_RIGHT_DATE:
					sort1Value = fe1.getRightModified();
					sort2Value = fe2.getRightModified();
					break;
			}

			if( sortResult == 0 )
			{
				if( sort1Value < sort2Value )
					sortResult = -1;
				else  if( sort1Value > sort2Value )
					sortResult = 1;
			}

			if( sortResult == 0 )
				sortResult = fe1.getFilepath().toLowerCase().compareTo(fe2.getFilepath().toLowerCase());
		}

		return sortResult;
	}
}

