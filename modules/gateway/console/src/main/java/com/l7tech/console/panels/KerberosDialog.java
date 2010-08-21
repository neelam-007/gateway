package com.l7tech.console.panels;

import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.util.Registry;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimerTask;
import java.util.logging.Level;
import java.io.File;
import java.io.IOException;
import javax.swing.filechooser.FileFilter;

import com.l7tech.console.util.TopComponents;
import com.l7tech.console.action.SecureAction;
import com.l7tech.gateway.common.admin.KerberosAdmin;
import com.l7tech.gateway.common.security.rbac.AttemptedUpdateAny;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.FileChooserUtil;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.kerberos.KerberosException;
import com.l7tech.kerberos.Keytab;
import com.l7tech.util.Background;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.IOUtils;
import com.l7tech.objectmodel.EntityType;

/**
 * Dialog for displaying Kerberos configuration information.
 *
 * @author Steve Jones
 */
public class KerberosDialog extends JDialog {

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
    private JLabel configKdcLabel;
    private JLabel configRealmLabel;
    private JLabel versionLabel;
    private JLabel dateLabel;
    private JLabel principalNameLabel;
    private JLabel realmLabel;
    private JPanel keytabPanel;
    private JLabel validLabel;
    private JLabel summaryLabel;
    private JLabel encryptionTypesLabel;
    private JLabel errorMessageLabel;
    private JButton uploadKeytab;
    private JButton deleteKeytabButton;

    private boolean validKeytab = false;

    private void init() {
        setTitle("Kerberos Configuration");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        Utilities.setEscKeyStrokeDisposes(this);
        setResizable(false);
        add(mainPanel);

        closeButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
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

        initData();
        if (validKeytab) testConfiguration();

        pack();
        Utilities.centerOnScreen(this);
    }

    private void initData() {
        KerberosAdmin kerberosAdmin = Registry.getDefault().getKerberosAdmin();

        if (kerberosAdmin != null) {
            Map config = kerberosAdmin.getConfiguration();
            if (config != null) {
                String kdc = (String) config.get("kdc");
                if (kdc != null) configKdcLabel.setText(kdc);

                String realm = (String) config.get("realm");
                if (realm != null) configRealmLabel.setText(realm);
            }

            Keytab keytab = null;
            boolean keytabInvalid = false;
            try {
                keytab = kerberosAdmin.getKeytab();
            }
            catch(KerberosException e) {
                keytabInvalid = true;
            }

            if ( keytab != null || keytabInvalid ) {
                 deleteKeytabButton.setEnabled( uploadKeytab.isEnabled() );
            }

            if ( keytab == null ) {
                validKeytab = false;

                validLabel.setText("No");
                if ( keytabInvalid ) {
                    summaryLabel.setText("Keytab file is invalid.");
                }
                else {
                    summaryLabel.setText("Keytab file not present.");
                }
                keytabPanel.setEnabled(false);

                versionLabel.setText("");
                dateLabel.setText("");
                principalNameLabel.setText("");
                realmLabel.setText("");
                encryptionTypesLabel.setText("");
            }
            else {
                validKeytab = true;

                validLabel.setText(" - ");
                summaryLabel.setText("Checking configuration ...");

                versionLabel.setText(Long.toString(keytab.getKeyVersionNumber()));
                dateLabel.setText(keytab.getKeyTimestamp() == 0 ?
                        "<Not Available>" :
                        new SimpleDateFormat("yyyy/MM/dd").format(new Date(keytab.getKeyTimestamp())));
                principalNameLabel.setText(formatName(keytab.getKeyName()));
                realmLabel.setText(keytab.getKeyRealm());

                StringBuffer encTypeStringBuffer = new StringBuffer();
                String[] keyTypes = keytab.getKeyTypes();
                for (int e=0; e<keyTypes.length; e++) {
                    if (e > 0) encTypeStringBuffer.append(", ");
                    encTypeStringBuffer.append(keyTypes[e]);
                }
                encryptionTypesLabel.setText(encTypeStringBuffer.toString());
            }
        }
        else {
            validLabel.setText("No");
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
                            boolean valid = false;
                            String errorMessageText = null;

                            try {
                                kerberosAdmin.getPrincipal();
                                valid = true;
                            }
                            catch(KerberosException e) {
                                // bug 5603: truncate the error message to a maximum length of 150 chars.
                                if (e.getMessage() != null && e.getMessage().length() > 150) {
                                    errorMessageText = new StringBuffer(e.getMessage().substring(0, 150)).append("...").toString();
                                } else {
                                    errorMessageText = e.getMessage();
                                }
                            }

                            final boolean wasValid = valid;
                            final String errorMessage = errorMessageText;

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
                                        errorMessageLabel.setVisible( true );
                                        errorMessageLabel.setText( errorMessage );
                                    }

                                    KerberosDialog.this.pack();
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
        versionLabel.setText("");
        dateLabel.setText("");
        principalNameLabel.setText("");
        realmLabel.setText("");
        encryptionTypesLabel.setText("");
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
                            if (validKeytab) testConfiguration();
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
            Keytab keytab = new Keytab( keytabFile );

            DialogDisplayer.showConfirmDialog(
                    KerberosDialog.this,
                    "Load keytab for principal:\n\t" + formatName(keytab.getKeyName()),
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
        } catch ( IOException ioe ) {
            DialogDisplayer.showMessageDialog(
                    KerberosDialog.this,
                    "Error reading keytab:\n\t" + ExceptionUtils.getMessage(ioe),
                    "Keytab Error",
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
            showUpdating();

            Background.scheduleOneShot( new TimerTask(){
                @Override
                public void run() {
                    SwingUtilities.invokeLater( new Runnable() {
                        @Override
                        public void run() {
                            initData();
                            if (validKeytab) testConfiguration(); // deletion could have failed
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
            465, 180,
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
