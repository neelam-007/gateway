package com.l7tech.console.panels;

import com.l7tech.gateway.common.License;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.WrappingLabel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Click wrap dialog for SSM.
 */
public class EulaDialog extends JDialog {
    protected static final Logger logger = Logger.getLogger(EulaDialog.class.getName());

    private JPanel rootPanel;
    private JButton okButton;
    private JButton cancelButton;
    private JScrollPane licenseScrollPane;
    private JPanel licensePanel;

    private boolean noEula = false;
    private boolean confirmed = false;

    /**
     * Create a clickwrap dialog that displays the appropriate EULA for the specified license.
     *
     * @param owner     Frame that owns this dialog.  required
     * @param license   the License that is being installed.  required
     * @throws IOException  if there is a problem reading the eula resource
     */
    public EulaDialog(Frame owner, License license)
            throws IOException
    {
        super(owner);
        initialize(license);
    }

    /**
     * Create a clickwrap dialog that displays the appropriate EULA for the specified license.
     *
     * @param owner     Dialog that owns this dialog.  required
     * @param license   the License that is being installed.  required
     * @throws IOException  if there is a problem reading the eula resource
     */
    public EulaDialog(Dialog owner, License license)
            throws IOException
    {
        super(owner);
        initialize(license);
    }

    /**
     * @return the EULA text, or null if one wasn't found.
     * @param license the license to examine.  required
     */
    private String findAgreementText(License license) {
        // Check for custom eula text
        String text = license.getEulaText();
        if (text != null && text.trim().length() > 0)
            return text;
        noEula = true;
        logger.fine("No EULA found in license.");
        return "No EULA found.";
    }


    public void setVisible(boolean b) {
        // Don't bother showing dialog if there's no eula
        if (b && noEula) {
            super.setVisible(false);
            dispose();
            return;
        }
        super.setVisible(b);
    }

    private void initialize(License license) throws IOException {
        String agreementText = findAgreementText(license);

        setContentPane(rootPanel);
        setModal(true);
        setTitle("License Agreement");

        licenseScrollPane.getHorizontalScrollBar().setUnitIncrement(8);
        licenseScrollPane.getVerticalScrollBar().setUnitIncrement(10);

        WrappingLabel licenseText = new WrappingLabel(agreementText);
        licenseText.setContextMenuAutoSelectAll(false);
        licenseText.setContextMenuEnabled(true);
        licensePanel.add(licenseText, BorderLayout.CENTER);
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                confirmed = true;
                EulaDialog.this.dispose();
            }
        });
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                EulaDialog.this.dispose();
            }
        });
        Utilities.equalizeButtonSizes(new JButton[] { okButton, cancelButton });
    }

    /** @return true if the license contained a EULA for display. */
    public boolean isEulaPresent() {
        return !noEula;
    }

    /** @return true only if the dialog has been displayed and dismissed via the "I Agree" button. */
    public boolean isConfirmed() {
        return confirmed;
    }
}
