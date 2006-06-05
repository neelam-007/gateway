package com.l7tech.console.panels;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.Date;
import java.util.Map;
import java.text.SimpleDateFormat;
import java.rmi.RemoteException;
import javax.swing.*;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.security.kerberos.KerberosAdmin;
import com.l7tech.common.security.kerberos.Keytab;
import com.l7tech.common.security.kerberos.KerberosConfigException;
import com.l7tech.common.security.kerberos.KerberosException;
import com.l7tech.console.util.Registry;

/**
 * Dialog for displaying Kerberos configuration information.
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class KerberosDialog extends JDialog {

    //- PUBLIC

    public KerberosDialog(Dialog parent) throws RemoteException {
        super(parent, true);
        init();
    }

    public KerberosDialog(Frame parent) throws RemoteException {
        super(parent, true);
        init();
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

    private void init() throws RemoteException {
        setTitle("Kerberos Configuration");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setAlwaysOnTop(true);
        setResizable(false);
        add(mainPanel);

        okButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                KerberosDialog.this.setVisible(false);
            }
        });

        initData();

        pack();
        Utilities.centerOnScreen(this);
    }

    private void initData() throws RemoteException {
        KerberosAdmin kerberosAdmin = Registry.getDefault().getKerberosAdmin();

        if (kerberosAdmin != null) {
            Map config = kerberosAdmin.getConfiguration();
            if (config != null) {
                String kdc = (String) config.get("kdc");
                if (kdc != null) configKdcLabel.setText(kdc);

                String realm = (String) config.get("realm");
                if (realm != null) configRealmLabel.setText(realm);
            }

            boolean valid = false;
            try {
                kerberosAdmin.getPrincipal();
                valid = true;
            }
            catch(Exception e) {
                // ignore
            }

            Keytab keytab = null;
            boolean keytabInvalid = false;
            try {
                keytab = kerberosAdmin.getKeytab();
            }
            catch(KerberosException e) {
                keytabInvalid = true;
            }

            if (keytab == null) {
                validLabel.setText("No");
                if (keytabInvalid) {
                    summaryLabel.setText("Keytab file is invalid.");
                }
                else {
                    summaryLabel.setText("Keytab file not present.");
                }
                keytabPanel.setEnabled(false);
            }
            else {
                if (valid) {
                    validLabel.setText("Yes");
                    summaryLabel.setText("Authentication successful.");
                }
                else {
                    validLabel.setText("No");
                    summaryLabel.setText("Authentication failed.");
                }

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
            for (int n=0; n<names.length; n++) {
                nameBuffer.append(names[n]);
                nameBuffer.append(' ');
            }
            principal = nameBuffer.toString();
        }

        return principal;
    }
}
