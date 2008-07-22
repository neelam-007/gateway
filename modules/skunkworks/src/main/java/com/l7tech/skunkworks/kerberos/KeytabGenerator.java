package com.l7tech.skunkworks.kerberos;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.File;
import javax.swing.*;

import com.l7tech.gui.util.Utilities;
import com.l7tech.kerberos.Keytab;
import com.l7tech.kerberos.KerberosException;

/**
 * Utility for creation of Kerberos keytab files.
 *
 * <p>This can be used instead of the windows "ktpass" utility. But note that
 * you will still need to map the SPN to a domain account using "setspn".</p>
 *
 * @author Steve Jones
 */
public class KeytabGenerator extends JDialog {

    //- PUBLIC

    public static void main(String[] args) {
        KeytabGenerator keytabGen = new KeytabGenerator((JFrame)null);
        keytabGen.setVisible(true);
    }

    public KeytabGenerator(JDialog parent) {
        super(parent, TITLE, true);
        init();
    }

    public KeytabGenerator(JFrame parent) {
        super(parent, TITLE, true);
        init();
    }

    //- PRIVATE

    private static final String TITLE = "Keytab Generator v0.1";

    private JTextField hostTextField;
    private JTextField realmTextField;
    private JCheckBox DESOnlyCheckBox;
    private JTextField serviceTextField;
    private JPasswordField passwordPasswordField;
    private JButton saveAsButton;
    private JPanel mainPanel;

    private void init() {
        setContentPane(mainPanel);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        saveAsButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser();
                chooser.setDialogTitle("Save keytab as ...");
                chooser.setDialogType(JFileChooser.SAVE_DIALOG);
                final String suggestedName = "kerberos.keytab";
                chooser.setSelectedFile(new File(suggestedName));
                chooser.setMultiSelectionEnabled(false);
                int r = chooser.showDialog(KeytabGenerator.this, "Save");
                if(r == JFileChooser.APPROVE_OPTION) {
                    File file = chooser.getSelectedFile();
                    if(file!=null) {
                        StringBuffer spnBuffer = new StringBuffer();
                        spnBuffer.append(serviceTextField.getText());
                        spnBuffer.append("/" );
                        spnBuffer.append(hostTextField.getText().toLowerCase());
                        spnBuffer.append("@");
                        spnBuffer.append(realmTextField.getText().toUpperCase());

                        String spn = spnBuffer.toString();

                        try {
                            Keytab keytab = new Keytab(file,
                                    spn,
                                    new String(passwordPasswordField.getPassword()),
                                    DESOnlyCheckBox.isSelected());

                            JOptionPane.showMessageDialog(KeytabGenerator.this,
                                    "Saved keytab with SPN " + spn,
                                    "Keytab saved",
                                    JOptionPane.INFORMATION_MESSAGE);
                        }
                        catch(KerberosException ke) {
                            ke.printStackTrace();
                            JOptionPane.showMessageDialog(KeytabGenerator.this,
                                    "Error saving keytab: " + ke.getMessage(),
                                    "Error saving keytab",
                                    JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            }
        });

        pack();
        Utilities.centerOnScreen(this);
    }
}
