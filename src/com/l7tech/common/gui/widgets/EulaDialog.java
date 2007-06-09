package com.l7tech.common.gui.widgets;

import com.l7tech.common.License;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.ResourceUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
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

    private boolean confirmed = false;

    /**
     * Create a clickwrap dialog that displays the appropriate EULA for the specified license.
     *
     * @param owner     Frame that owns this dialog.  required
     * @param license   the License that is being installed.  required
     * @param resourcePrefix    prefix to use to find resources named in the license, ie "com/l7tech/console/resources/eula".  Required
     * @param defaultResource   full path of default eula resource to use if one not named in the license, ie "com/l7tech/console/resources/eula-default.txt".  Required.
     * @param encoding   name of character encoding to use when reading a eula resource.  Required.
     * @throws IOException  if there is a problem reading the eula resource
     */
    public EulaDialog(Frame owner, License license, String resourcePrefix, String defaultResource, String encoding)
            throws IOException
    {
        super(owner);
        initialize(license, resourcePrefix, defaultResource, encoding);
    }

    /**
     * Create a clickwrap dialog that displays the appropriate EULA for the specified license.
     *
     * @param owner     Dialog that owns this dialog.  required
     * @param license   the License that is being installed.  required
     * @param resourcePrefix    prefix to use to find resources named in the license, ie "com/l7tech/console/resources/eula".  Required
     * @param defaultResource   full path of default eula resource to use if one not named in the license, ie "com/l7tech/console/resources/eula-default.txt".  Required.
     * @param encoding   name of character encoding to use when reading a eula resource.  Required.
     * @throws IOException  if there is a problem reading the eula resource
     */
    public EulaDialog(Dialog owner, License license, String resourcePrefix, String defaultResource, String encoding)
            throws IOException
    {
        super(owner);
        initialize(license, resourcePrefix, defaultResource, encoding);
    }

    /**
     * @param resourcePath path of eula text to load, ie "com/l7tech/console/resources/clickwrap.txt".  Required.
     * @param encoding name of encoding to use when reading the above text file.  Required.
     * @return the eula string from the specified path, or null if the resource was not found.
     * @throws java.io.IOException if there was a problem reading the file
     *
     */
    private String getEulaResource(String resourcePath, String encoding) throws IOException {
        InputStream eulaStream = null;
        try {
            eulaStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
            if (eulaStream == null)
                return null;
            byte[] eulaBytes = HexUtils.slurpStream(eulaStream);
            // Replace "smart" quotes with smart quotes
            HexUtils.replaceBytes(
                    eulaBytes,
                    new int[] { 0x82, 0x84, 0x91, 0x92, 0x93, 0x94, 0x8b, 0x9b, 0x96, 0x97 },
                    new int[] { ',',  ',',  '\'', '\'', '\'', '\'',  '<',  '>',  '-',  '-' });
            return new String(eulaBytes, encoding);
        } finally {
            ResourceUtils.closeQuietly(eulaStream);
        }
    }

    private String findAgreementText(License license, String resourcePrefix, String defaultResource, String encoding) throws IOException {
        // Check for custom eula text
        String text = license.getEulaText();
        if (text != null && text.trim().length() > 0)
            return text;

        // Check for non-default eula identifier
        String id = license.getEulaIdentifier();
        if (id != null) {
            String path = resourcePrefix + id + ".txt";
            text = getEulaResource(path, encoding);
            if (text != null && text.trim().length() > 0)
                return text;
            logger.warning("Specified eulaid not found (showing default): " + path);
        }

        text = getEulaResource(defaultResource, encoding);
        if (text != null && text.trim().length() > 0)
            return text;

        return "Unable to find " + defaultResource;
    }

    private void initialize(License license, String resourcePrefix, String defaultResource, String encoding) throws IOException {
        String agreementText = findAgreementText(license, resourcePrefix, defaultResource, encoding);

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
        //Utilities.attachDefaultContextMenu(licenseText);
    }

    /** @return true only if the dialog has been displayed and dismissed via the "I Agree" button. */
    public boolean isConfirmed() {
        return confirmed;
    }
}
