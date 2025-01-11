import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Title:        XJ Sync
 * Description:  Ordnerabgleich für Mac/Linux/Windows
 * Copyright:    Copyright (c) 2003
 * Company:      CRESD GmbH
 * @author Martin Gäckler
 * @version 1.0
 */

public class XJsyncFrame_AboutBox extends JDialog implements ActionListener
{
	JPanel panel1 = new JPanel();
	JPanel panel2 = new JPanel();
	BorderLayout borderLayout1 = new BorderLayout();
	BorderLayout borderLayout2 = new BorderLayout();
	JButton button1 = new JButton();
	JPanel insetsPanel3 = new JPanel();
	JLabel label4 = new JLabel();
	JLabel label3 = new JLabel();
	JLabel label2 = new JLabel();
	JLabel label1 = new JLabel();
	JLabel imageLabel = new JLabel();
	GridLayout gridLayout1 = new GridLayout();
	JLabel label5 = new JLabel();

	public XJsyncFrame_AboutBox(Frame parent)
	{
		super(parent);
		enableEvents(AWTEvent.WINDOW_EVENT_MASK);
		try
		{
			jbInit();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		pack();
	}
	/**Component initialization*/
	private void jbInit() throws Exception
	{
		//imageLabel.setIcon(new ImageIcon(XJsyncFrame_AboutBox.class.getResource("[Your Image]")));
		this.setModal(true);
		this.setResizable(false);
		this.setTitle("Über XJ Sync");
		panel1.setLayout(borderLayout1);
		panel2.setLayout(borderLayout2);
		button1.setHorizontalTextPosition(SwingConstants.CENTER);
		button1.setText("Ok");
		button1.addActionListener(this);
		label4.setText("CRESD GmbH");
		label3.setText("Copyright (c) 2003-2011");
		label2.setText("Version 1.2 .2 - 24.9.2011");
		label1.setText("XJ Sync = Ordnerabgleich für Mac/Linux/Windows   ");
		imageLabel.setIcon(new ImageIcon(XJsyncFrame_AboutBox.class.getResource("syncLogo.gif")));
		imageLabel.setVerticalAlignment(SwingConstants.TOP);
		imageLabel.setVerticalTextPosition(SwingConstants.TOP);
		insetsPanel3.setLayout(gridLayout1);
		gridLayout1.setRows(5);
		label5.setText("http://www.cresd.de/");
		this.getContentPane().add(panel1, BorderLayout.CENTER);
		panel2.add(insetsPanel3,  BorderLayout.CENTER);
		insetsPanel3.add(label1, null);
		insetsPanel3.add(label2, null);
		insetsPanel3.add(label3, null);
		insetsPanel3.add(label4, null);
		insetsPanel3.add(label5, null);
		panel2.add(imageLabel,  BorderLayout.WEST);
		panel1.add(button1,  BorderLayout.SOUTH);
		panel1.add(panel2, BorderLayout.CENTER);
	}
  /**Overridden so we can exit when window is closed*/
  protected void processWindowEvent(WindowEvent e) {
	if (e.getID() == WindowEvent.WINDOW_CLOSING) {
	  cancel();
	}
	super.processWindowEvent(e);
  }
  /**Close the dialog*/
  void cancel() {
	dispose();
  }
  /**Close the dialog on a button event*/
  public void actionPerformed(ActionEvent e) {
	if (e.getSource() == button1) {
	  cancel();
	}
  }
}
