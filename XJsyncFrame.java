import java.io.*;
import java.util.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;

import com.gaklib.*;

/**
 * Title:        XJ Sync
 * Description:  Ordnerabgleich für Mac/Linux/Windows
 * Copyright:    Copyright (c) 2003
 * Company:      CRESD GmbH
 * @author Martin Gäckler
 * @version 1.0
 */

class XJsyncProperties extends Properties
{
	static final String propertiesFileName = ".cresd.xjsync.properties";
	static final String dbDirPropertyName = "cresd.xjsync.dbDirectory";
	static final String leftDirPropertyName = "cresd.xjsync.leftDirectory";
	static final String rightDirPropertyName = "cresd.xjsync.rightDirectory";
	static final String winXposPropertyName = "cresd.xjsync.winXpos";
	static final String winYposPropertyName = "cresd.xjsync.winYpos";
	static final String winWidthPropertyName = "cresd.xjsync.winWidth";
	static final String winHeightPropertyName = "cresd.xjsync.winHeight";
	static final String syncXposPropertyName = "cresd.xjsync.syncXpos";
	static final String syncYposPropertyName = "cresd.xjsync.syncYpos";
	static final String syncWidthPropertyName = "cresd.xjsync.syncWidth";
	static final String syncHeightPropertyName = "cresd.xjsync.syncHeight";
	static final String nameWidthPropertyName = "cresd.xjsync.nameWidth";


	String propertiesFilePath;

	public XJsyncProperties()
	{
		propertiesFilePath = System.getProperty("user.home");
		propertiesFilePath += File.separatorChar;
		propertiesFilePath += propertiesFileName;
		File thePropFile = new File(propertiesFilePath);

		try
		{
			if (thePropFile.exists())
				load(new FileInputStream(propertiesFilePath));
		}
		catch( Exception e )
		{
			e.printStackTrace();
		}
	}
	public int getProperty( String propertyName, int defaultVal )
	{
		Integer	value = new Integer( defaultVal );
		String	strValue = getProperty( propertyName );
		if( strValue != null )
			value = Integer.valueOf( strValue );

		return value.intValue();
	}

	public Object setProperty( String propertyName, String propertyValue )
	{
		Object oldValue = super.setProperty(propertyName, propertyValue );
		try
		{
			store( new FileOutputStream(propertiesFilePath), "xjsync");
		}
		catch( Exception e )
		{
			e.printStackTrace();
		}

		return oldValue;
	}

	public Object setProperty( String propertyName, int propertyValue )
	{
		String	strPropValue = Integer.toString(propertyValue);

		return setProperty( propertyName, strPropValue );
	}
	public Rectangle getSyncBounds()
	{
		int		x, y, width, height;

		x		= getProperty( syncXposPropertyName, 0 );
		y		= getProperty( syncYposPropertyName, 0 );
		width	= getProperty( syncWidthPropertyName, 600 );
		height	= getProperty( syncHeightPropertyName, 200 );

		return new Rectangle( x, y, width, height );
	}

	public void setSyncBounds( Rectangle bounds )
	{
		setProperty( syncXposPropertyName, bounds.x );
		setProperty( syncYposPropertyName, bounds.y );
		setProperty( syncWidthPropertyName, bounds.width );
		setProperty( syncHeightPropertyName, bounds.height );
	}
}

abstract class xjSyncThread extends Thread
{
	XJsyncFrame			parent;
	FileTableModel		fileData;

	boolean				hideStatus;
	SyncStatus			statWin;
	XJsyncProperties	theProperties;

	xjSyncThread(XJsyncProperties theProperties, XJsyncFrame parent, FileTableModel fileData, boolean hideStatus)
	{
		this.theProperties = theProperties;
		this.parent = parent;
		this.fileData = fileData;
		this.hideStatus = hideStatus;
		statWin = new SyncStatus(
			theProperties.getSyncBounds(), fileData.getXjSyncDBfile()
		);
	}

	abstract void syncRun() throws IOException;
	public void run()
	{
		try
		{
			if( fileData.lock() )
			{
				statWin.setIconImage(parent.getIconImage());
				statWin.show();

				try
				{
					syncRun();

					theProperties.setSyncBounds(statWin.getBounds());
				}
				finally
				{
					if( hideStatus )
						statWin.hide();
					else
						statWin.setFinished();
				}
			}
			else
			{
				MessageBox	msgBox = new MessageBox(
					"XJ Sync", "Es läuft bereits ein Thread", MessageBox.OK_BUTTON
				);
				msgBox.show();
			}
		}
		catch( Throwable e )
		{
			MessageBox	msgBox = new MessageBox(
				"XJ Sync", e, MessageBox.OK_BUTTON
			);
			msgBox.show();
			e.printStackTrace();
		}
		finally
		{
			fileData.freeLock();
		}
	}
}

/*
	Load database file and sync the directories immediately and then terminate
*/
class synchronizeDB extends xjSyncThread
{
	String				dbFileName;

	synchronizeDB( XJsyncProperties theProperties, XJsyncFrame parent, FileTableModel fileData, String dbFileName )
	{
		super( theProperties, parent, fileData, true );

		this.dbFileName = dbFileName;

	}
	void syncRun() throws IOException
	{
		fileData.loadXSyncDB(dbFileName, statWin);
		fileData.synchronizeFiles(statWin);
		System.exit(0);
	}
}

/*
	sync the directories loaded in the table
*/
class synchronizeFiles extends xjSyncThread
{
	synchronizeFiles( XJsyncProperties theProperties, XJsyncFrame parent, FileTableModel fileData )
	{
		super( theProperties, parent, fileData, false );
	}

	void syncRun() throws IOException
	{
		fileData.synchronizeFiles(statWin);
		parent.fileDataScrollTop();
	}
}

class loadXSyncDB extends xjSyncThread
{
	String				dbFileName;

	loadXSyncDB( XJsyncProperties theProperties, XJsyncFrame parent, FileTableModel fileData, String dbFileName )
	{
		super( theProperties, parent, fileData, true );
		statWin.setTitle( "XJ Sync Status - " + dbFileName );
		this.dbFileName = dbFileName;
	}

	void syncRun() throws IOException
	{
		try
		{
			fileData.loadXSyncDB(dbFileName, statWin);
		}
		finally
		{
			parent.fileDataLoaded();
			parent.fileDataScrollTop();
		}
	}
}

class loadFileData extends xjSyncThread
{
	String				directory;
	boolean				leftFlag;

	loadFileData( XJsyncProperties theProperties, XJsyncFrame parent, FileTableModel fileData, String directory, boolean leftFlag )
	{
		super( theProperties, parent, fileData, true );

		this.directory = directory;
		this.leftFlag = leftFlag;
	}

	void syncRun() throws IOException
	{
		fileData.loadFileData( directory, leftFlag, statWin );
		parent.fileDataScrollTop();
	}
}

/**
 * <p>Description: Display the application window</p>
 */
public class XJsyncFrame extends JFrame
{
	ImageIcon exitIcon = new ImageIcon(XJsyncFrame.class.getResource("doorshut.gif"));
	ImageIcon openIcon = new ImageIcon(XJsyncFrame.class.getResource("openFile.gif"));
	ImageIcon reloadIcon = new ImageIcon(XJsyncFrame.class.getResource("reloadFile.gif"));
	ImageIcon saveIcon = new ImageIcon(XJsyncFrame.class.getResource("saveFile.gif"));
	ImageIcon saveAsIcon = new ImageIcon(XJsyncFrame.class.getResource("saveFileAs.gif"));
	ImageIcon leftSyncIcon = new ImageIcon(XJsyncFrame.class.getResource("sync_left.gif"));
	ImageIcon rightSyncIcon = new ImageIcon(XJsyncFrame.class.getResource("sync_right.gif"));
	ImageIcon syncIcon = new ImageIcon(XJsyncFrame.class.getResource("sync.gif"));
	ImageIcon windowIcon = new ImageIcon(XJsyncFrame.class.getResource("syncWindow.gif"));

	JMenuBar jMenuBar = new JMenuBar();

	JMenu jMenuFile = new JMenu();
	JMenuItem jMenuOpenDB = new JMenuItem();
	JMenuItem jMenuReloadDB = new JMenuItem();
	JMenuItem jMenuSaveDB = new JMenuItem();
	JMenuItem jMenuSaveAsDB = new JMenuItem();
	JMenuItem jMenuFileExit = new JMenuItem();

	JMenu jMenuOperation = new JMenu();
	JMenuItem jMenuOperationCopyLeftRight = new JMenuItem();
	JMenuItem jMenuOperationCopyRightLeft = new JMenuItem();
	JMenuItem jMenuOperationDeleteLeft = new JMenuItem();
	JMenuItem jMenuOperationDeleteRight = new JMenuItem();
	JMenuItem jMenuOperationDeleteBoth = new JMenuItem();
	JMenuItem jMenuOperationRenameLeft = new JMenuItem();
	JMenuItem jMenuOperationRenameRight = new JMenuItem();
	JMenuItem jMenuOperationCalc = new JMenuItem();
	JMenuItem jMenuOperationFilter = new JMenuItem();

	JMenu jMenuSort = new JMenu();
	JRadioButtonMenuItem jMenuSortName = new JRadioButtonMenuItem();
	JRadioButtonMenuItem jMenuSortLeftSize = new JRadioButtonMenuItem();
	JRadioButtonMenuItem jMenuSortLeftDate = new JRadioButtonMenuItem();
	JRadioButtonMenuItem jMenuSortOperation = new JRadioButtonMenuItem();
	JRadioButtonMenuItem jMenuSortRightSize = new JRadioButtonMenuItem();
	JRadioButtonMenuItem jMenuSortRightDate = new JRadioButtonMenuItem();

	JMenu jMenuDirection = new JMenu();
	JRadioButtonMenuItem jMenuLeftRight = new JRadioButtonMenuItem();
	JRadioButtonMenuItem jMenuRightLeft = new JRadioButtonMenuItem();
	JRadioButtonMenuItem jMenuSynchronize = new JRadioButtonMenuItem();

	JMenu jMenuHelp = new JMenu();
	JMenuItem jMenuHelpAbout = new JMenuItem();

	JToolBar jToolBar = new JToolBar();
	JButton exitButton = new JButton();
	JButton openButton = new JButton();
	JButton saveButton = new JButton();
	JButton saveAsButton = new JButton();
	JButton syncButton = new JButton();

	JLabel StatusBar = new JLabel();
	JPanel contentPane;

	FileTableModel fileData = new FileTableModel();
	JScrollPane jScrollPane = new JScrollPane();
	JTable syncTable = new JTable();
	JPanel jMainPanel = new JPanel();
	BorderLayout borderLayout1 = new BorderLayout();
	BorderLayout borderLayout2 = new BorderLayout();
	JPanel jDirectoryPanel = new JPanel();
	JTextField leftTextField = new JTextField();
	JButton leftDirOpenButton = new JButton();
	JTextField rightTextField = new JTextField();
	JButton rightDirOpenButton = new JButton();
	JLabel jLeftDirLabel = new JLabel();
	JLabel jrightDirLabel = new JLabel();
	GridBagLayout gridBagLayout1 = new GridBagLayout();

	static XJsyncProperties theProperties = new XJsyncProperties();;

	ButtonGroup sortGroup = new ButtonGroup();
	ButtonGroup directionGroup = new ButtonGroup();
	JButton reloadButton = new JButton();
	JCheckBox jCheckBoxCaseSensitive = new JCheckBox();
	/**Construct the frame*/
	public XJsyncFrame()
	{
		enableEvents(AWTEvent.WINDOW_EVENT_MASK);
		try
		{
			TableColumn	col;

			jbInit();
			syncTable.setModel(fileData);

			sortGroup.add(jMenuSortName);
			sortGroup.add(jMenuSortLeftSize);
			sortGroup.add(jMenuSortLeftDate);
			sortGroup.add(jMenuSortOperation);
			sortGroup.add(jMenuSortRightSize);
			sortGroup.add(jMenuSortRightDate);

			directionGroup.add( jMenuLeftRight );
			directionGroup.add( jMenuRightLeft );
			directionGroup.add( jMenuSynchronize );

			setIconImage( windowIcon.getImage() );

			int	x = theProperties.getProperty(XJsyncProperties.winXposPropertyName, 50 );
			int	y = theProperties.getProperty(XJsyncProperties.winYposPropertyName, 50 );
			int	width = theProperties.getProperty(XJsyncProperties.winWidthPropertyName, 600 );
			int	height = theProperties.getProperty(XJsyncProperties.winHeightPropertyName, 400 );
			this.setBounds( x, y, width, height );

			width = theProperties.getProperty(XJsyncProperties.nameWidthPropertyName, 300 );
			col = syncTable.getColumn("Name");
			col.setPreferredWidth(width);
			col.setWidth(width);

			col = syncTable.getColumn("L-Größe");
			col.setPreferredWidth( 80 );
			col.setWidth( 80 );
			col = syncTable.getColumn("L-Datum");
			col.setPreferredWidth( 180 );
			col.setWidth( 180 );
			col = syncTable.getColumn("Operation");
			col.setPreferredWidth( 80 );
			col.setWidth( 80 );
			col = syncTable.getColumn("R-Größe");
			col.setPreferredWidth( 80 );
			col.setWidth( 80 );
			col = syncTable.getColumn("R-Datum");
			col.setPreferredWidth( 180 );
			col.setWidth( 180 );

			syncTable.doLayout();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	/**Component initialization*/
	private void jbInit() throws Exception
	{
		//setIconImage(Toolkit.getDefaultToolkit().createImage(XJsyncFrame.class.getResource("[Your Icon]")));
		contentPane = (JPanel) this.getContentPane();
		contentPane.setLayout(borderLayout1);
		this.setTitle("XJ Sync");
		StatusBar.setText(" ");

		jMenuFile.setText("Datei");

		jMenuFileExit.setIcon(exitIcon);
		jMenuFileExit.setText("Ende");
		jMenuFileExit.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				jMenuFileExit_actionPerformed(e);
			}
		});

		jMenuHelp.setText("Hilfe");
		jMenuHelpAbout.setText("Über XJ Sync...");
		jMenuHelpAbout.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				jMenuHelpAbout_actionPerformed(e);
			}
		});
		openButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				openButton_actionPerformed(e);
			}
		});
		openButton.setToolTipText("Öffnen...");
		openButton.setIcon(openIcon);
		saveAsButton.setText("...");
		saveAsButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				saveAsButton_actionPerformed(e);
			}
		});
		jMenuOpenDB.setIcon(openIcon);
		jMenuOpenDB.setText("Datenbank öffnen");
		jMenuOpenDB.addActionListener(new java.awt.event.ActionListener()
		{
		  public void actionPerformed(ActionEvent e)
		  {
			openButton_actionPerformed(e);
		  }
		});
		jMenuReloadDB.setIcon(reloadIcon);
		jMenuReloadDB.setText("Datenbank neu laden");
		jMenuReloadDB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				jMenuReloadDB_actionPerformed(e);
			}
		});
		saveAsButton.setToolTipText("Speichern unter...");
		saveAsButton.setIcon(saveAsIcon);
		jMainPanel.setLayout(borderLayout2);
		leftDirOpenButton.setToolTipText("Verzeichniss wählen");
		leftDirOpenButton.setIcon(openIcon);
		leftDirOpenButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				leftDirOpenButton_actionPerformed(e);
			}
		});
		rightDirOpenButton.setToolTipText("Verzeichniss wählen");
		rightDirOpenButton.setIcon(openIcon);
		rightDirOpenButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				rightDirOpenButton_actionPerformed(e);
			}
		});
		jLeftDirLabel.setHorizontalAlignment(SwingConstants.LEFT);
		jLeftDirLabel.setHorizontalTextPosition(SwingConstants.LEFT);
		jLeftDirLabel.setText("Links");
		jrightDirLabel.setText("Rechts");
		jDirectoryPanel.setLayout(gridBagLayout1);
		saveButton.setToolTipText("Speichern");
		saveButton.setIcon(saveIcon);
		saveButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				saveButton_actionPerformed(e);
			}
		});
		jMenuSort.setText("Sortierung");
		jMenuSortName.setSelected(true);
		jMenuSortName.setText("Name");
		jMenuSortName.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				jMenuSortName_actionPerformed(e);
			}
		});
		jMenuSortLeftSize.setText("Linke Größe");
		jMenuSortLeftSize.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				jMenuSortLeftSize_actionPerformed(e);
			}
		});
		jMenuSortLeftDate.setText("Linkes Datum");
		jMenuSortLeftDate.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				jMenuSortLeftDate_actionPerformed(e);
			}
		});
		jMenuSortOperation.setText("Operation");
		jMenuSortOperation.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				jMenuSortOperation_actionPerformed(e);
			}
		});
		jMenuSortRightSize.setText("Rechte Größe");
		jMenuSortRightSize.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				jMenuSortRightSize_actionPerformed(e);
			}
		});
		jMenuSortRightDate.setText("Rechtes Datum");
		jMenuSortRightDate.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				jMenuSortRightDate_actionPerformed(e);
			}
		});
		jMenuDirection.setText("Quelle/Ziel");
		jMenuLeftRight.setText("Von Links nach Rechts");
		jMenuLeftRight.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				jMenuLeftRight_actionPerformed(e);
			}
		});
		jMenuRightLeft.setText("Von Rechts nach Links");
		jMenuRightLeft.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				jMenuRightLeft_actionPerformed(e);
			}
		});
		jMenuSynchronize.setText("Beide Seiten synchronisieren");
		jMenuSynchronize.setSelected(true);
		jMenuSynchronize.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				jMenuSynchronize_actionPerformed(e);
			}
		});
		syncButton.setToolTipText("Abgleichen");
		syncButton.setIcon(syncIcon);
		syncButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				syncButton_actionPerformed(e);
			}
		});
		jMenuOperation.setText("Operation");
		jMenuOperationCopyLeftRight.setActionCommand("Kopieren von Links nach Rechts");
		jMenuOperationCopyLeftRight.setText("Kopieren von Links nach Rechts");
		jMenuOperationCopyLeftRight.addActionListener(new java.awt.event.ActionListener()
		{
		  public void actionPerformed(ActionEvent e)
		  {
			jMenuOperationCopyLeftRight_actionPerformed(e);
		  }
		});
		jMenuOperationCopyRightLeft.setText("Kopieren von Rechts nach Links");
		jMenuOperationCopyRightLeft.addActionListener(new java.awt.event.ActionListener()
		{
		  public void actionPerformed(ActionEvent e)
		  {
			jMenuOperationCopyRightLeft_actionPerformed(e);
		  }
		});
		jMenuOperationDeleteLeft.setText("Links Löschen");
		jMenuOperationDeleteLeft.addActionListener(new java.awt.event.ActionListener()
		{
		  public void actionPerformed(ActionEvent e)
		  {
			jMenuOperationDeleteLeft_actionPerformed(e);
		  }
		});
		jMenuOperationDeleteRight.setText("Rechts löschen");
		jMenuOperationDeleteRight.addActionListener(new java.awt.event.ActionListener()
		{
		  public void actionPerformed(ActionEvent e)
		  {
			jMenuOperationDeleteRight_actionPerformed(e);
		  }
		});
		jMenuOperationCalc.setText("Neu berechnen");
		jMenuOperationCalc.addActionListener(new java.awt.event.ActionListener()
		{
		  public void actionPerformed(ActionEvent e)
		  {
			jMenuOperationCalc_actionPerformed(e);
		  }
		});

		jMenuOperationFilter.setText("Filter...");
		jMenuOperationFilter.addActionListener(new java.awt.event.ActionListener()
		{
		  public void actionPerformed(ActionEvent e)
		  {
			jMenuOperationFilter_actionPerformed(e);
		  }
		});
		leftTextField.setEditable(false);
		rightTextField.setEditable(false);
		syncTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		jMenuOperationDeleteBoth.setText("Beide löschen");
		jMenuOperationDeleteBoth.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				jMenuOperationDeleteBoth_actionPerformed(e);
			}
		});
		jMenuOperationRenameLeft.setText("Links umbenennen");
		jMenuOperationRenameLeft.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				jMenuOperationRenameLeft_actionPerformed(e);
			}
		});
		jMenuOperationRenameRight.setText("Rechts umbenennen");
		jMenuOperationRenameRight.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				jMenuOperationRenameRight_actionPerformed(e);
			}
		});
		exitButton.setMaximumSize(new Dimension(29, 29));
		exitButton.setMinimumSize(new Dimension(29, 29));
		exitButton.setPreferredSize(new Dimension(29, 29));
		exitButton.setToolTipText("Beenden");
		exitButton.setActionCommand("");
		exitButton.setIcon(exitIcon);
		exitButton.setText("");
		exitButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				jMenuFileExit_actionPerformed(e);
			}
		});
		jMenuSaveDB.setIcon(saveIcon);
		jMenuSaveDB.setText("Datenbank Speichern");
		jMenuSaveDB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				saveButton_actionPerformed(e);
			}
		});
		jMenuSaveAsDB.setIcon(saveAsIcon);
		jMenuSaveAsDB.setText("Datenbank Speichern unter...");
		jMenuSaveAsDB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				saveAsButton_actionPerformed(e);
			}
		});
		jCheckBoxCaseSensitive.setText("Groß/Klein");
		jCheckBoxCaseSensitive.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				jCheckBoxCaseSensitive_actionPerformed(e);
			}
		});
		jMenuFile.add(jMenuOpenDB);
		jMenuFile.add(jMenuReloadDB);
		jMenuFile.add(jMenuSaveDB);
		jMenuFile.add(jMenuSaveAsDB);
		jMenuFile.addSeparator();
		jMenuFile.add(jMenuFileExit);
		jMenuBar.add(jMenuFile);

		jMenuOperation.add(jMenuOperationCopyLeftRight);
		jMenuOperation.add(jMenuOperationCopyRightLeft);
		jMenuOperation.add(jMenuOperationDeleteLeft);
		jMenuOperation.add(jMenuOperationDeleteRight);
		jMenuOperation.add(jMenuOperationDeleteBoth);
		jMenuOperation.add(jMenuOperationRenameLeft);
		jMenuOperation.add(jMenuOperationRenameRight);
		jMenuOperation.add(jMenuOperationCalc);
		jMenuOperation.addSeparator();
		jMenuOperation.add(jMenuOperationFilter);
		jMenuBar.add(jMenuOperation);

		jMenuSort.add(jMenuSortName);
		jMenuSort.add(jMenuSortLeftSize);
		jMenuSort.add(jMenuSortLeftDate);
		jMenuSort.add(jMenuSortOperation);
		jMenuSort.add(jMenuSortRightSize);
		jMenuSort.add(jMenuSortRightDate);
		jMenuBar.add(jMenuSort);

		jMenuDirection.add(jMenuSynchronize);
		jMenuDirection.add(jMenuLeftRight);
		jMenuDirection.add(jMenuRightLeft);
		jMenuBar.add(jMenuDirection);

		jMenuHelp.add(jMenuHelpAbout);
		jMenuBar.add(jMenuHelp);

		this.setJMenuBar(jMenuBar);
		contentPane.add(StatusBar, BorderLayout.SOUTH);

		jToolBar.add(exitButton);
		jToolBar.add(openButton);

		reloadButton.setToolTipText("Neu laden");
		reloadButton.setIcon(reloadIcon);
		reloadButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				jMenuReloadDB_actionPerformed(e);
			}
		});
		jToolBar.add(reloadButton);
		jToolBar.add(saveButton);
		jToolBar.add(saveAsButton);
		jToolBar.add(syncButton);

		contentPane.add(jMainPanel, BorderLayout.CENTER);
		jMainPanel.add(jScrollPane, BorderLayout.CENTER);
		jMainPanel.add(jDirectoryPanel, BorderLayout.NORTH);
		contentPane.add(jToolBar, java.awt.BorderLayout.NORTH);
		jScrollPane.getViewport().add(syncTable, null);
		jDirectoryPanel.add(jLeftDirLabel,
							new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
			, GridBagConstraints.WEST, GridBagConstraints.NONE,
			new Insets(0, 0, 0, 0), 0, 0));
		jDirectoryPanel.add(leftTextField,
							new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0
			, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
			new Insets(5, 0, 5, 0), 0, 0));
		jDirectoryPanel.add(leftDirOpenButton,
							new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0
			, GridBagConstraints.CENTER, GridBagConstraints.NONE,
			new Insets(5, 0, 5, 0), 0, 0));
		jDirectoryPanel.add(jrightDirLabel,
							new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0
			, GridBagConstraints.EAST, GridBagConstraints.NONE,
			new Insets(0, 0, 0, 0), 0, 0));
		jDirectoryPanel.add(rightTextField,
							new GridBagConstraints(4, 0, 1, 1, 1.0, 0.0
			, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL,
			new Insets(5, 0, 5, 0), 0, 0));
		jDirectoryPanel.add(rightDirOpenButton,
							new GridBagConstraints(5, 0, 1, 1, 0.0, 0.0
			, GridBagConstraints.EAST, GridBagConstraints.NONE,
			new Insets(0, 0, 0, 0), 0, 0));
		jDirectoryPanel.add(jCheckBoxCaseSensitive,
							new GridBagConstraints(6, 0, 1, 1, 0.0, 0.0
			, GridBagConstraints.CENTER, GridBagConstraints.NONE,
			new Insets(0, 0, 0, 0), 0, 0));
	}

	/**Help | About action performed*/
	public void jMenuHelpAbout_actionPerformed(ActionEvent e)
	{
		XJsyncFrame_AboutBox dlg = new XJsyncFrame_AboutBox(this);
		Dimension dlgSize = dlg.getPreferredSize();
		Dimension frmSize = getSize();
		Point loc = getLocation();
		dlg.setLocation((frmSize.width - dlgSize.width) / 2 + loc.x, (frmSize.height - dlgSize.height) / 2 + loc.y);
		//dlg.setModal(true);
		dlg.show();
	}
	/**Overridden so we can exit when window is closed*/
	protected void processWindowEvent(WindowEvent e)
	{
		super.processWindowEvent(e);
		if (e.getID() == WindowEvent.WINDOW_CLOSING)
		{
			jMenuFileExit_actionPerformed(null);
		}
	}

	private String selectDirectory( String propertyName )
	{
		int				returnVal;

		JFileChooser	dirChooser	= new JFileChooser();
		String			lastDir		= theProperties.getProperty( propertyName );

		dirChooser.setDialogTitle( "Wähle Verzeichnis aus" );
		dirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

		if( lastDir != null )
			dirChooser.setCurrentDirectory( new File( lastDir ) );

		returnVal = dirChooser.showOpenDialog(this);

		if( returnVal == JFileChooser.APPROVE_OPTION )
		{
			theProperties.setProperty( propertyName, dirChooser.getCurrentDirectory().getAbsolutePath() );
			return dirChooser.getSelectedFile().getAbsolutePath();
		}
		else
			return null;
	}
	private String selectSaveAs( String propertyName )
	{
		int				returnVal;
		String			lastDir		= theProperties.getProperty( propertyName );
		JFileChooser	dbChooser	= new JFileChooser();

		dbChooser.setDialogTitle( "Wähle XJ Sync Datenbank aus" );
		dbChooser.addChoosableFileFilter( new XJSyncDbFileFilter() );
		dbChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

		if( lastDir != null )
			dbChooser.setCurrentDirectory( new File( lastDir ) );

		returnVal = dbChooser.showSaveDialog(this);

		if( returnVal == JFileChooser.APPROVE_OPTION )
		{
			theProperties.setProperty( propertyName, dbChooser.getCurrentDirectory().getAbsolutePath() );
			return dbChooser.getSelectedFile().getAbsolutePath();
		}
		else
			return null;
	}
	private String selectOpen( String propertyName )
	{
		int				returnVal;
		String			lastDir		= theProperties.getProperty( propertyName );
		JFileChooser	dbChooser	= new JFileChooser();

		dbChooser.setDialogTitle( "Wähle XJ Sync Datenbank aus" );
		dbChooser.addChoosableFileFilter( new XJSyncDbFileFilter() );
		dbChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

		if( lastDir != null )
			dbChooser.setCurrentDirectory( new File( lastDir ) );

		returnVal = dbChooser.showOpenDialog(this);

		if( returnVal == JFileChooser.APPROVE_OPTION )
		{
			theProperties.setProperty( propertyName, dbChooser.getCurrentDirectory().getAbsolutePath() );
			return dbChooser.getSelectedFile().getAbsolutePath();
		}
		else
			return null;
	}
	public void fileDataScrollTop()
	{
		jScrollPane.getViewport().setViewPosition( new Point(0,0) );
	}
	public void fileDataLoaded()
	{
		int	syncMode;

		leftTextField.setText(fileData.getLeftDirectory());
		rightTextField.setText(fileData.getRightDirectory());
		jCheckBoxCaseSensitive.setSelected( fileData.getCaseSensitive() );
		syncMode = fileData.getSyncMode();
		if( syncMode == FileEntry.SYNC_BOTH )
		{
			jMenuSynchronize.setSelected(true);
			syncButton.setIcon(syncIcon);
		}
		else if( syncMode == FileEntry.SYNC_LEFT_RIGHT )
		{
			jMenuLeftRight.setSelected(true);
			syncButton.setIcon(rightSyncIcon);
		}
		else if( syncMode == FileEntry.SYNC_RIGHT_LEFT )
		{
			jMenuRightLeft.setSelected( true );
			syncButton.setIcon(leftSyncIcon);
		}

		int	sortOrder = fileData.getSortOrder();
		if( sortOrder == FileComparator.SORT_NAME )
			jMenuSortName.setSelected( true );
		else if( sortOrder == FileComparator.SORT_LEFT_SIZE )
			jMenuSortLeftSize.setSelected( true );
		else if( sortOrder == FileComparator.SORT_LEFT_DATE )
			jMenuSortLeftDate.setSelected( true );
		else if( sortOrder == FileComparator.SORT_OPERATION )
			jMenuSortOperation.setSelected( true );
		else if( sortOrder == FileComparator.SORT_RIGHT_SIZE )
			jMenuSortRightSize.setSelected( true );
		else if( sortOrder == FileComparator.SORT_RIGHT_DATE )
			jMenuSortRightDate.setSelected( true );
	}
	public void loadXSyncDB( String xjSyncDb )
	{
		if( xjSyncDb != null )
		{
			this.setTitle("XJ Sync - " + xjSyncDb );

			new loadXSyncDB( theProperties, this, fileData, xjSyncDb ).start();
			StatusBar.setText(xjSyncDb);
		}
		else
			this.setTitle("XJ Sync" );
	}

	void rightDirOpenButton_actionPerformed(ActionEvent e)
	{
		String  rightDirectory = selectDirectory( XJsyncProperties.rightDirPropertyName );

		if( rightDirectory != null )
		{
			new loadFileData( theProperties, this, fileData, rightDirectory, false ).start();
			rightTextField.setText( rightDirectory );
		}
	}

	void jMenuSortName_actionPerformed(ActionEvent e)
	{
		fileData.resort(FileComparator.SORT_NAME);
	}

	void jMenuSortLeftSize_actionPerformed(ActionEvent e)
	{
		fileData.resort(FileComparator.SORT_LEFT_SIZE);
	}

	void jMenuSortLeftDate_actionPerformed(ActionEvent e)
	{
		fileData.resort(FileComparator.SORT_LEFT_DATE);
	}

	void jMenuSortOperation_actionPerformed(ActionEvent e)
	{
		fileData.resort(FileComparator.SORT_OPERATION);
	}

	void jMenuSortRightSize_actionPerformed(ActionEvent e)
	{
		fileData.resort(FileComparator.SORT_RIGHT_SIZE);
	}

	void jMenuSortRightDate_actionPerformed(ActionEvent e)
	{
		fileData.resort(FileComparator.SORT_RIGHT_DATE);
	}

	void jMenuSynchronize_actionPerformed(ActionEvent e)
	{
		fileData.recalculateOperation(FileEntry.SYNC_BOTH);
		syncButton.setIcon(syncIcon);
	}

	void jMenuLeftRight_actionPerformed(ActionEvent e)
	{
		fileData.recalculateOperation(FileEntry.SYNC_LEFT_RIGHT);
		syncButton.setIcon(rightSyncIcon);
	}

	void jMenuRightLeft_actionPerformed(ActionEvent e)
	{
		fileData.recalculateOperation(FileEntry.SYNC_RIGHT_LEFT);
		syncButton.setIcon(leftSyncIcon);
	}

	public void syncDB( String dbFileName )
	{
		new synchronizeDB( theProperties, this, fileData, dbFileName ).start();
	}
	void syncButton_actionPerformed(ActionEvent event)
	{
		MessageBox  msgBox = new MessageBox(
			"XJ Sync",
			"Wollen Sie " +
			fileData.getNumChangedFiles() +
			" geänderte Dateien/Ordner abgleichen?",
			MessageBox.OK_BUTTON|MessageBox.CANCEL_BUTTON
		);
		msgBox.show();
		if( msgBox.getReturnButton() == MessageBox.OK_BUTTON )
			new synchronizeFiles( theProperties, this, fileData ).start();
	}

	void leftDirOpenButton_actionPerformed(ActionEvent e)
	{
		String	leftDirectory = selectDirectory( XJsyncProperties.leftDirPropertyName );

		if( leftDirectory != null )
		{
			new loadFileData( theProperties, this, fileData, leftDirectory, true ).start();
			leftTextField.setText( leftDirectory );
		}
	}


	// Menu and speed button handlers
	void openButton_actionPerformed(ActionEvent event)
	{
		loadXSyncDB( selectOpen( XJsyncProperties.dbDirPropertyName ) );
	}

	public void jMenuReloadDB_actionPerformed(ActionEvent e) {
		String xjSyncDB = fileData.getXjSyncDBfile();
		if( xjSyncDB != null && !xjSyncDB.equals(""))
			loadXSyncDB( xjSyncDB );
	}
	void saveButton_actionPerformed(ActionEvent event)
	{
		if( fileData.getXjSyncDBfile() != null )
		{
			try
			{
				fileData.saveXSyncDB();
			}
			catch( Exception e )
			{
				e.printStackTrace();
			}
		}
		else
			saveAsButton_actionPerformed(event);
	}
	void saveAsButton_actionPerformed(ActionEvent event)
	{
		String xjSyncDb = selectSaveAs( XJsyncProperties.dbDirPropertyName );

		if( xjSyncDb != null )
		{
			try
			{
				fileData.saveXSyncDB( xjSyncDb );
				StatusBar.setText(xjSyncDb);
			}
			catch( Exception e )
			{
				e.printStackTrace();
			}
		}
	}

	/**File | Exit action performed*/
	public void jMenuFileExit_actionPerformed(ActionEvent e)
	{
		Dimension size = this.getSize();
		Point position = this.getLocation();

		theProperties.setProperty( XJsyncProperties.winXposPropertyName, position.x );
		theProperties.setProperty( XJsyncProperties.winYposPropertyName, position.y );

		theProperties.setProperty( XJsyncProperties.winWidthPropertyName, size.width );
		theProperties.setProperty( XJsyncProperties.winHeightPropertyName, size.height );

		TableColumn firstCol = syncTable.getColumn("Name");
		theProperties.setProperty( XJsyncProperties.nameWidthPropertyName, firstCol.getWidth() );

		System.exit(0);
	}

	void jMenuOperationCopyLeftRight_actionPerformed(ActionEvent e)
	{
		fileData.copyLeftRight( syncTable.getSelectedRows() );
	}

	void jMenuOperationCopyRightLeft_actionPerformed(ActionEvent e)
	{
		fileData.copyRightLeft( syncTable.getSelectedRows() );
	}

	void jMenuOperationDeleteLeft_actionPerformed(ActionEvent e)
	{
		fileData.deleteLeft( syncTable.getSelectedRows() );
	}

	void jMenuOperationDeleteRight_actionPerformed(ActionEvent e)
	{
		fileData.deleteRight( syncTable.getSelectedRows() );
	}

	void jMenuOperationCalc_actionPerformed(ActionEvent e)
	{
		fileData.calcOperation(syncTable.getSelectedRows());
	}

	void jMenuOperationFilter_actionPerformed(ActionEvent e)
	{
		FilterDialog filterDialog = new FilterDialog();

		filterDialog.show( fileData );
	}

	public void jMenuOperationDeleteBoth_actionPerformed(ActionEvent e)
	{
		fileData.deleteBoth( syncTable.getSelectedRows() );
	}

	public void jMenuOperationRenameLeft_actionPerformed(ActionEvent e)
	{
		fileData.renameLeft( syncTable.getSelectedRows() );
	}

	public void jMenuOperationRenameRight_actionPerformed(ActionEvent e)
	{
		fileData.renameRight( syncTable.getSelectedRows() );
	}

	public void jCheckBoxCaseSensitive_actionPerformed(ActionEvent e)
	{
		fileData.setCaseSensitive(jCheckBoxCaseSensitive.isSelected());
	}
}
