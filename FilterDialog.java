import java.awt.*;
import javax.swing.*;
import com.borland.jbcl.layout.XYLayout;
import com.borland.jbcl.layout.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * <p>Title: XJ Sync</p>
 *
 * <p>Description: Ordnerabgleich für Mac/Linux/Windows</p>
 *
 * <p>Copyright: Copyright (c) 2003</p>
 *
 * <p>Company: CRESD GmbH</p>
 *
 * @author Martin Gäckler
 * @version 1.0
 */
public class FilterDialog
	extends JDialog {
	JPanel panel1 = new JPanel();
	JLabel jLabel1 = new JLabel();
	JTextField jTextFieldFilter = new JTextField();
	JButton jButtonOK = new JButton();
	JButton jButtonCancel = new JButton();
	FileTableModel	fileData;
	GridBagLayout gridBagLayout1 = new GridBagLayout();

	public FilterDialog(Frame owner, String title, boolean modal) {
		super(owner, title, modal);
		try {
			setDefaultCloseOperation(DISPOSE_ON_CLOSE);
			jbInit();
			pack();
		}
		catch (Exception exception) {
			exception.printStackTrace();
		}
	}
	public void show( FileTableModel fileData )
	{
		this.fileData = fileData;
		jTextFieldFilter.setText( fileData.getFilter() );
		super.show();
	}

	public FilterDialog() {
		this(new Frame(), "FilterDialog", false);
	}

	private void jbInit() throws Exception {
		panel1.setLayout(gridBagLayout1);
		jLabel1.setText("Exclude");
		jTextFieldFilter.setText("*.*");
		jButtonOK.setText("OK");
		jButtonOK.addActionListener(new FilterDialog_jButtonOK_actionAdapter(this));
		jButtonCancel.setText("Cancel");
		jButtonCancel.addActionListener(new
			FilterDialog_jButtonCancel_actionAdapter(this));
		panel1.setMinimumSize(new Dimension(458, 30));
		panel1.setToolTipText("");
		panel1.add(jTextFieldFilter,
				   new GridBagConstraints(1, 0, 2, 1, 1.0, 0.0
										  , GridBagConstraints.NORTHWEST,
										  GridBagConstraints.HORIZONTAL,
										  new Insets(5, 5, 5, 5), 383, 0));
		panel1.add(jLabel1, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
			, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
			new Insets(5, 5, 5, 5), 0, 0));
		panel1.add(jButtonOK, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
			, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
			new Insets(5, 5, 5, 5), 0, 0));
		panel1.add(jButtonCancel, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0
			, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
			new Insets(5, 5, 5, 5), 0, 0));
		this.getContentPane().add(panel1, java.awt.BorderLayout.NORTH);

	}

	public void jButtonOK_actionPerformed(ActionEvent e)
	{
		fileData.setFilter( jTextFieldFilter.getText() );
		hide();
	}

	public void jButtonCancel_actionPerformed(ActionEvent e) {
		hide();
	}
}

class FilterDialog_jButtonOK_actionAdapter
	implements ActionListener {
	private FilterDialog adaptee;
	FilterDialog_jButtonOK_actionAdapter(FilterDialog adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jButtonOK_actionPerformed(e);
	}
}

class FilterDialog_jButtonCancel_actionAdapter
	implements ActionListener {
	private FilterDialog adaptee;
	FilterDialog_jButtonCancel_actionAdapter(FilterDialog adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jButtonCancel_actionPerformed(e);
	}
}
