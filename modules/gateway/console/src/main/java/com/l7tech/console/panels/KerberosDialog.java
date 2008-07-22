package com.l7tech.console.panels;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.Date;
import java.util.Map;
import java.util.TimerTask;
import java.util.logging.Level;
import java.text.SimpleDateFormat;
import javax.swing.*;

import com.l7tech.console.util.Registry;
import com.l7tech.console.logging.ErrorManager;
import com.l7tech.gateway.common.admin.KerberosAdmin;
import com.l7tech.gui.util.Utilities;
import com.l7tech.kerberos.KerberosException;
import com.l7tech.kerberos.Keytab;
import com.l7tech.util.Background;

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

    @Override
    public void setVisible( final boolean visible ) {
        if ( visible ) {
            testConfiguration();
        }

        super.setVisible( visible );
    }

    //- PRIVATE

    private JPanel mainPanel;
    private JButton okButton;
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

    private boolean validKeytab = false;

    private void init() {
        setTitle("Kerberos Configuration");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        Utilities.setAlwaysOnTop(this, true);
        setResizable(false);
        add(mainPanel);

        okButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                KerberosDialog.this.dispose();
            }
        });

        initData();

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

            if ( keytab == null ) {
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
                                errorMessageText = e.getMessage();
                            }

                            final boolean wasValid = valid;
                            final String errorMessage = errorMessageText;

                            SwingUtilities.invokeLater( new Runnable() {
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
}
