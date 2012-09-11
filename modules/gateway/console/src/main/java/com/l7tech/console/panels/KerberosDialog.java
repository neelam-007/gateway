package com.l7tech.console.panels;

import com.l7tech.console.action.SecureAction;
import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.admin.KerberosAdmin;
import com.l7tech.gateway.common.security.rbac.AttemptedUpdateAny;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.FileChooserUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.kerberos.KerberosConstants;
import com.l7tech.kerberos.KerberosException;
import com.l7tech.kerberos.KerberosUtils;
import com.l7tech.kerberos.KeyTabEntryInfo;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.util.Background;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.IOUtils;
import sun.security.krb5.internal.ktab.KeyTab;
import sun.security.krb5.internal.ktab.KeyTabEntry;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.TimerTask;
import java.util.logging.Level;

/**
 * Dialog for displaying Kerberos configuration information.
 *
 * @author Steve Jones
 */
public class KerberosDialog extends JDialog {
    private static final ResourceBundle resources = ResourceBundle.getBundle(KerberosDialog.class.getName());
    private static final int MAX_TABLE_COLUMN_NUM = 6;

    //- PUBLIC

    public KerberosDialog(Dialog parent) {
        super(parent, true);
        init();
    }

    public KerberosDialog(Frame parent) {
        super(parent, true);
        init();
    }

    //- PRIVATE

    private JPanel mainPanel;
    private JButton closeButton;
    private JPanel keytabPanel;
    private JLabel validLabel;
    private JLabel summaryLabel;
    private JLabel errorMessageLabel;
    private JButton uploadKeytab;
    private JButton deleteKeytabButton;
    private JTable keytabTable;
    private JButton validateButton;
    private JCheckBox performValidateCheckBox;

    private boolean validKeytab = false;
    private List<KeyTabEntryInfo> keyTabEntries = new ArrayList<KeyTabEntryInfo>();

    private void init() {
        setTitle("Kerberos Configuration");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        Utilities.setEscKeyStrokeDisposes(this);
        setResizable(false);
        add(mainPanel);

        validateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doValidate();
            }
        });

        closeButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                KerberosAdmin kerberosAdmin = Registry.getDefault().getKerberosAdmin();
                try {
                    kerberosAdmin.setKeytabValidate(performValidateCheckBox.isSelected());
                } catch (KerberosException e1) {
                    ErrorManager.getDefault().notify(Level.WARNING, e1, "Error persist Perform KDC Validation status." );
                }
                KerberosDialog.this.dispose();
            }
        });

        String label = uploadKeytab.getText();
        uploadKeytab.setAction(new SecureAction(new AttemptedUpdateAny(EntityType.CLUSTER_PROPERTY)){
            @Override
            protected void performAction() {
                loadKeytab();
            }
        });
        uploadKeytab.setText(label);

        label = deleteKeytabButton.getText();
        deleteKeytabButton.setAction(new SecureAction(new AttemptedUpdateAny(EntityType.CLUSTER_PROPERTY)){
            @Override
            protected void performAction() {
                perhapsDeleteKeytab();
            }
        });
        deleteKeytabButton.setEnabled(false);
        deleteKeytabButton.setText(label);
        validateButton.setEnabled(false);

        initKeyTabTable();
        initData();
        if (validKeytab && performValidateCheckBox.isSelected()) testConfiguration();

        pack();
        Utilities.centerOnScreen(this);
    }

    private void initData() {
        KerberosAdmin kerberosAdmin = Registry.getDefault().getKerberosAdmin();

        if (kerberosAdmin != null) {
            boolean keytabInvalid = false;
            try {
                keyTabEntries = kerberosAdmin.getKeyTabEntryInfos();
            }
            catch(KerberosException e) {
                keytabInvalid = true;
            }

            if ( keyTabEntries.size() != 0 || keytabInvalid ) {
                 deleteKeytabButton.setEnabled( uploadKeytab.isEnabled() );
                 validateButton.setEnabled(uploadKeytab.isEnabled());
            }

            if ( keyTabEntries.size() == 0 ) {
                validKeytab = false;

                validLabel.setText("No");
                if ( keytabInvalid ) {
                    summaryLabel.setText("Keytab file is invalid.");
                }
                else {
                    summaryLabel.setText("Keytab file not present.");
                }
                keytabPanel.setEnabled(false);
            }
            else {
                validKeytab = true;

                validLabel.setText(" - ");
                summaryLabel.setText("");
                errorMessageLabel.setText("");
                errorMessageLabel.setVisible( false );


                ((KeyTabTableModel)keytabTable.getModel()).fireTableDataChanged();

            }

            try {
                if (kerberosAdmin.getKeytabValidate()) {
                    performValidateCheckBox.setSelected(true);
                } else {
                    performValidateCheckBox.setSelected(false);
                }
            } catch (KerberosException e) {
                performValidateCheckBox.setSelected(true);
            }
        }
        else {
            validLabel.setText("No");
        }
    }
    private void initKeyTabTable() {

        keytabTable.setModel(new KeyTabTableModel());

    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
    }

    private class KeyTabTableModel extends AbstractTableModel {


        @Override
        public int getColumnCount() {
            return MAX_TABLE_COLUMN_NUM;
        }

        @Override
        public int getRowCount() {
            return keyTabEntries.size();
        }

        @Override
        public String getColumnName(int col) {
            switch (col) {
                case 0:
                    return resources.getString("table.column.kdc");
                case 1:
                    return resources.getString("table.column.realm");
                case 2:
                    return resources.getString("table.column.principalName");
                case 3:
                    return resources.getString("table.column.date");
                case 4:
                    return resources.getString("table.column.version");
                case 5:
                    return resources.getString("table.column.encryption");
                default:
                    throw new IndexOutOfBoundsException("Out of the maximum column number, " + MAX_TABLE_COLUMN_NUM + ".");
            }
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            KeyTabEntryInfo keyTabEntry = keyTabEntries.get(rowIndex);

            switch (columnIndex) {
                case 0:
                    KerberosAdmin kerberosAdmin = Registry.getDefault().getKerberosAdmin();
                    if (kerberosAdmin != null) {
                        return keyTabEntry.getKdc();
                    }
                case 1:
                    return keyTabEntry.getRealm();
                case 2:
                    return keyTabEntry.getPrincipalName();
                case 3:
                    return keyTabEntry.getDate() == null ? "<Not Avaliable>" :
                            new SimpleDateFormat("yyyy/MM/dd").format(keyTabEntry.getDate());
                case 4:
                    return keyTabEntry.getVersion();
                case 5:
                    try {
                        return KerberosConstants.ETYPE_NAMES[keyTabEntry.getEType()];
                    } catch( ArrayIndexOutOfBoundsException aioobe ) {
                        return "unknown [" + keyTabEntry.getEType() + "]";
                    }
                default:
                    throw new IndexOutOfBoundsException("Out of the maximum column number, " + MAX_TABLE_COLUMN_NUM + ".");
            }
        }
    }

    private void testConfiguration() {
        validLabel.setText(" - ");
        summaryLabel.setText("Checking configuration ...");
        errorMessageLabel.setText("");
        errorMessageLabel.setVisible( false );
        
        if ( validKeytab ) {
            Background.scheduleOneShot( new TimerTask(){
                @Override
                public void run() {
                    try {
                        KerberosAdmin kerberosAdmin = Registry.getDefault().getKerberosAdmin();
                        if ( kerberosAdmin != null ) {
                            boolean valid = true;
                            StringBuilder errorMessageText = new StringBuilder();

                            for (int i = 0; i < keyTabEntries.size(); i++) {
                                KeyTabEntryInfo keyTabEntryInfo =  keyTabEntries.get(i);
                                try {
                                    kerberosAdmin.validatePrincipal(keyTabEntryInfo.getPrincipalName());
                                }
                                catch(KerberosException e) {
                                    valid = false;
                                    errorMessageText.append(keyTabEntryInfo.getPrincipalName() + " : " + e.getMessage() + "\n\t");
                                }
                            }

                            final boolean wasValid = valid;
                            final StringBuilder errorMessage = new StringBuilder();
                            // bug 5603: truncate the error message to a maximum length of 150 chars.
                            if (errorMessageText.length() > 150 ) {
                                errorMessage.append(errorMessageText.substring(0, 150) + "...");
                            } else {
                                errorMessage.append(errorMessageText.toString());
                            }

                            SwingUtilities.invokeLater( new Runnable() {
                                @Override
                                public void run() {
                                    if ( wasValid ) {
                                        validLabel.setText("Yes");
                                        summaryLabel.setText("Authentication successful.");
                                    }
                                    else {
                                        validLabel.setText("No");
                                        summaryLabel.setText("Authentication failed.");
                                        errorMessageLabel.setVisible(true);
                                        errorMessageLabel.setText( errorMessage.toString() );
                                    }

                                    // use DialogDisplayer so that applet resizes correctly - bug 11050
                                    DialogDisplayer.pack(KerberosDialog.this);
                                }
                            } );
                        }
                    } catch (Exception e) {
                        ErrorManager.getDefault().notify( Level.WARNING, e, "Error while checking kerberos configuratino." );
                    }
                }
            }, 100 );
        }
    }

    private String formatName(String[] names) {
        String principal;

        if (names.length == 1) {
            principal = names[0];
        }
        else if (names.length == 2) {
            principal = names[0] + "/" + names[1];
        }
        else {
            StringBuffer nameBuffer = new StringBuffer();
            for( String name1 : names ) {
                nameBuffer.append( name1 );
                nameBuffer.append( ' ' );
            }
            principal = nameBuffer.toString();
        }

        return principal;
    }

    private void showUpdating() {
        validLabel.setText(" - ");
        summaryLabel.setText("Updating configuration ...");
        errorMessageLabel.setText("");
        errorMessageLabel.setVisible( false );
        keyTabEntries = new ArrayList<KeyTabEntryInfo>();
        ((KeyTabTableModel)keytabTable.getModel()).fireTableDataChanged();
    }

    private void doValidate() {
        showUpdating();
        Background.scheduleOneShot( new TimerTask(){
            @Override
            public void run() {
                SwingUtilities.invokeLater( new Runnable() {
                    @Override
                    public void run() {
                        initData();
                        if (validKeytab) testConfiguration();
                    }
                });
            }
        }, 1500 );
    }

    private void doUpload(  final File keytabFile  ) {
        try {
            byte[] fileData = IOUtils.slurpFile(keytabFile);

            KerberosAdmin kerberosAdmin = Registry.getDefault().getKerberosAdmin();
            kerberosAdmin.installKeytab( fileData );

            showUpdating();

            Background.scheduleOneShot( new TimerTask(){
                @Override
                public void run() {
                    SwingUtilities.invokeLater( new Runnable() {
                        @Override
                        public void run() {
                            initData();
                            if (validKeytab && performValidateCheckBox.isSelected()) testConfiguration();
                        }
                    });
                }
            }, 1500 );
        } catch ( IOException ioe ) {
            DialogDisplayer.showMessageDialog(
                    KerberosDialog.this,
                    "Error reading keytab:\n\t" + ExceptionUtils.getMessage(ioe),
                    "Keytab Error",
                    JOptionPane.WARNING_MESSAGE,
                    null );
        } catch ( KerberosException ome ) {
            DialogDisplayer.showMessageDialog(
                    KerberosDialog.this,
                    "Error saving keytab:\n\t" + ExceptionUtils.getMessage(ome),
                    "Error Saving Keytab",
                    JOptionPane.WARNING_MESSAGE,
                    null );
        }
    }

    private void loadKeytab( final File keytabFile ) {
        try {
            KerberosUtils.validateKeyTab(keytabFile);
            KeyTab keyTab = KeyTab.getInstance(keytabFile);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < keyTab.getEntries().length; i++) {
                KeyTabEntry entry = keyTab.getEntries()[i];
                sb.append(entry.getService().getNameString() + "\n\t");
            }

            DialogDisplayer.showConfirmDialog(
                    KerberosDialog.this,
                    "Load keytab for principal:\n\t" + sb.toString(),
                    "Confirm Keytab Update",
                    JOptionPane.OK_CANCEL_OPTION,
                    new DialogDisplayer.OptionListener(){
                        @Override
                        public void reportResult(int option) {
                            if ( option == JOptionPane.OK_OPTION ) {
                                doUpload( keytabFile );
                            }
                        }
                    } );
        } catch ( KerberosException ke ) {
            DialogDisplayer.showMessageDialog(
                    KerberosDialog.this,
                    "Invalid kerberos keytab:\n\t" + ExceptionUtils.getMessage(ke),
                    "Invalid Keytab",
                    JOptionPane.WARNING_MESSAGE,
                    null );
        }
    }

    private void loadKeytab() {
        FileChooserUtil.doWithJFileChooser( new FileChooserUtil.FileChooserUser(){
            @Override
            public void useFileChooser( final JFileChooser fc ) {
                fc.setDialogTitle("Select Keytab");
                fc.setDialogType(JFileChooser.OPEN_DIALOG);
                FileFilter fileFilter = new FileFilter() {
                    @Override
                    public boolean accept(File f) {
                        return  f.isDirectory() ||
                                f.getName().toLowerCase().endsWith(".keytab");
                    }
                    @Override
                    public String getDescription() {
                        return "(*.keytab) Kerberos Keytab.";
                    }
                };
                fc.addChoosableFileFilter(fileFilter);
                fc.setMultiSelectionEnabled(false);
                int r = fc.showDialog(KerberosDialog.this, "Open");
                if(r == JFileChooser.APPROVE_OPTION) {
                    File file = fc.getSelectedFile();
                    if( file != null ) {
                        if( file.canRead() ) {
                            loadKeytab( file );
                        }
                        else {
                            DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(), null,
                                    "File not accessible: '" + file.getAbsolutePath() + "'.", null);
                        }
                    }
                }
            }
        } );
    }

    private void deleteKeytab() {
        try {
            KerberosAdmin kerberosAdmin = Registry.getDefault().getKerberosAdmin();
            kerberosAdmin.deleteKeytab();

            deleteKeytabButton.setEnabled( false );
            validateButton.setEnabled(false);
            showUpdating();

            Background.scheduleOneShot( new TimerTask(){
                @Override
                public void run() {
                    SwingUtilities.invokeLater( new Runnable() {
                        @Override
                        public void run() {
                            initData();
                            if (validKeytab && performValidateCheckBox.isSelected()) testConfiguration(); // deletion could have failed
                        }
                    });
                }
            }, 1500 );
        } catch ( KerberosException ome ) {
            DialogDisplayer.showMessageDialog(
                    KerberosDialog.this,
                    "Error deleting keytab:\n\t" + ExceptionUtils.getMessage(ome),
                    "Error Deleting Keytab",
                    JOptionPane.WARNING_MESSAGE,
                    null );
        }
    }

    private void perhapsDeleteKeytab() {
        DialogDisplayer.showSafeConfirmDialog(
            this,
             "<html><center>This will delete the keytab and cannot be undone.</center><p>" +
             "<center>Really delete the keytab?</center></html>",
            "Confirm Keytab Deletion",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.WARNING_MESSAGE,
            new DialogDisplayer.OptionListener() {
                @Override
                public void reportResult(int option) {
                    if ( option == JOptionPane.OK_OPTION ) {
                        deleteKeytab();
                    }
                }
            }
        );
    }
}
