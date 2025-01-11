import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Title:        XJ Sync
 * Description:  Ordnerabgleich f¸r Mac/Linux/Windows
 * Copyright:    Copyright (c) 2003
 * Company:      CRESD GmbH
 * @author Martin G‰ckler
 * @version 1.0
 */

public class SyncStatus extends JFrame
{
	public boolean cancelFlag = false;

	JPanel jPanel1 = new JPanel();
	JPanel jPanel2 = new JPanel();
	JButton jButton1 = new JButton();
	JTextArea statusText = new JTextArea();
	BorderLayout borderLayout1 = new BorderLayout();
	GridBagLayout gridBagLayout1 = new GridBagLayout();
	JProgressBar mainProgressBar = new JProgressBar();
	JProgressBar fileProgressBar = new JProgressBar();
	BorderLayout borderLayout2 = new BorderLayout();

	boolean finished = false;
	JProgressBar dataProgressBar = new JProgressBar();

	long curCopyData, curCopyFile;
	int	dataDivisor, fileDivisor;


	/**
	 * SyncStatus Create a new status window
	 *
	 * @param bounds Rectangle the bounds of the new window
	 */
	public SyncStatus( Rectangle bounds, String dbFileName )
	{
		try
		{
			jbInit();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		setBounds( bounds );
		if( dbFileName.length() > 0 )
			setTitle( "XJ Sync Status - " + dbFileName );
	}
	private void jbInit() throws Exception
	{
		this.setTitle("XJSync Status");
		this.getContentPane().setLayout(borderLayout1);
		jButton1.setText("Abbrechen");
		jButton1.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				jButton1_actionPerformed(e);
			}
		});
		statusText.setWrapStyleWord(true);
		statusText.setText("status");
		statusText.setEnabled(false);
		statusText.setAutoscrolls(false);
		statusText.setDisabledTextColor(Color.black);
		statusText.setEditable(false);
		jPanel1.setLayout(borderLayout2);
		jPanel1.setBackground(Color.white);
		jPanel2.setLayout(gridBagLayout1);
		mainProgressBar.setMaximum(0);
		fileProgressBar.setMaximum(0);
		dataProgressBar.setMaximum(0);
		this.getContentPane().add(jPanel1,  BorderLayout.CENTER);
		this.getContentPane().add(jPanel2,  BorderLayout.SOUTH);
		jPanel1.add(statusText, java.awt.BorderLayout.CENTER);
		jPanel2.add(jButton1, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0
			, GridBagConstraints.CENTER, GridBagConstraints.NONE,
			new Insets(0, 5, 5, 5), 0, 0));
		jPanel2.add(mainProgressBar,
					new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0
										   , GridBagConstraints.CENTER,
										   GridBagConstraints.HORIZONTAL,
										   new Insets(5, 5, 0, 5), 0, 0));
		jPanel2.add(dataProgressBar,
					new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
										   , GridBagConstraints.CENTER,
										   GridBagConstraints.HORIZONTAL,
										   new Insets(5, 5, 0, 5), 0, 0));
		jPanel2.add(fileProgressBar,
					new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
										   , GridBagConstraints.CENTER,
										   GridBagConstraints.HORIZONTAL,
										   new Insets(5, 5, 5, 5), 0, 0));
		setDefaultCloseOperation( WindowConstants.DO_NOTHING_ON_CLOSE );
	}
	void jButton1_actionPerformed(ActionEvent e)
	{
		if( !finished )
			cancelFlag = true;
		else
			hide();
	}

	/**
	 * setStatusText
	 * changes the text of the status window
	 *
	 * @param newText String the new text
	 */
	public void setStatusText( final String newText )
	{
		EventQueue.invokeLater(
			new Runnable()
			{
				public void run()
				{
					statusText.setText( newText );
				}
			}
		);
	}

	/**
	 * setMaxProgress
	 * changes the maximum value of the main progress bar
	 *
	 * @param maxVal int new maximum value
	 */
	public void setMaxProgress( final int maxVal )
	{
		EventQueue.invokeLater(
			new Runnable()
			{
				public void run()
				{
					mainProgressBar.setMaximum( maxVal );
				}
			}
		);
	}

	/**
	 * setMaxDataProgress
	 * changes the maximum value of the data progress bar
	 *
	 * @param maxVal int new maximum value
	 */
	public void setMaxDataProgress( long maxVal )
	{
		curCopyData = 0;
		dataDivisor = 1;
		while( maxVal > 2147483647 )
		{
			maxVal /= 2;
			dataDivisor *= 2;
		}
		final long tmp = maxVal;
		EventQueue.invokeLater(
			new Runnable()
			{
				public void run()
				{
					dataProgressBar.setMaximum( (int)tmp );
				}
			}
		);
	}

	/**
	 * setMaxFileProgress
	 * changes the maximum value of the secondary progress bar
	 *
	 * @param maxVal int new maximum value
	 */
	public void setMaxFileProgress( long maxVal )
	{
		curCopyFile = 0;
		fileDivisor = 1;
		while( maxVal > 2147483647 )
		{
			maxVal /= 2;
			fileDivisor *= 2;
		}
		final long tmp = maxVal;
		EventQueue.invokeLater(
			new Runnable()
			{
				public void run()
				{
					fileProgressBar.setMaximum( (int)tmp );
				}
			}
		);
	}

	/**
	 * setCurrentProgress
	 * changes the current value of the main progress bar
	 *
	 * @param value int new value
	 */
	public void setCurrentProgress( final int value )
	{
		EventQueue.invokeLater(
			new Runnable()
			{
				public void run()
				{
					mainProgressBar.setValue(value);
				}
			}
		);
	}

	/**
	 * setCurrentDataProgress
	 * changes the current value of the data progress bar
	 *
	 * @param value int new value
	 */
	public void setCurrentDataProgress( final long value )
	{
		curCopyData = value;
		EventQueue.invokeLater(
			new Runnable()
			{
				public void run()
				{
					dataProgressBar.setValue((int)(value/dataDivisor));
				}
			}
		);
	}

	/**
	 * setCurrentFileProgress
	 * changes the current value of the secondary progress bar
	 *
	 * @param value int new value
	 */
	public void setCurrentFileProgress( final long value )
	{
		curCopyFile = value;
		EventQueue.invokeLater(
			new Runnable()
			{
				public void run()
				{
					fileProgressBar.setValue((int)(value/fileDivisor));
				}
			}
		);
	}

	/**
	 * incrementCurrentProgress
	 * increments the current value of the main progress bar
	 *
	 * @param incValue int the number to add to the old value
	 */
	public void incrementCurrentProgress( final int incValue )
	{
		EventQueue.invokeLater( new Runnable()
			{
				public void run()
				{
					mainProgressBar.setValue(mainProgressBar.getValue()+incValue);
				}
			}
		);
	}

	/**
	 * incrementCurrentDataProgress
	 * increments the current value of the data progress bar
	 *
	 * @param incValue int the number to add to the old value
	 */
	public void incrementCurrentDataProgress( final long incValue )
	{
		curCopyData += incValue;
		final int tmp = (int)(curCopyData / dataDivisor);
		EventQueue.invokeLater( new Runnable()
			{
				public void run()
				{
					dataProgressBar.setValue( tmp );
				}
			}
		);
	}

	/**
	 * incrementCurrentFileProgress
	 * increments the current value of the secondary progress bar
	 *
	 * @param incValue int the number to add to the old value
	 */
	public void incrementCurrentFileProgress( long incValue )
	{
		curCopyFile += incValue;
		final int tmp = (int)(curCopyFile / fileDivisor);
		EventQueue.invokeLater( new Runnable()
			{
				public void run()
				{
					fileProgressBar.setValue(tmp);
				}
			}
		);
	}

	/**
	 * setFinished
	 * tell the status window, that the thread has finished
	 */
	public void setFinished()
	{
		finished = true;
		mainProgressBar.setValue(mainProgressBar.getMaximum());
		dataProgressBar.setValue(dataProgressBar.getMaximum());
		fileProgressBar.setValue(fileProgressBar.getMaximum());
		jButton1.setText("Schlieﬂen");
	}
}
