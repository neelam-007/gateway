package com.l7tech.console.panels;

import java.util.logging.Logger;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.*;
import javax.swing.*;

import org.w3c.dom.Document;

import com.l7tech.common.xml.Wsdl;
import com.l7tech.common.gui.util.Utilities;

/**
 * Dialog for selection of a WSDL from a URL (UDDI) or File.
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class SelectWsdlDialog extends JDialog {

    //- PUBLIC

    /**
     * Create a WSDL selection dialog (modal).
     *
     * @param parent the parent
     * @param title the title to use
     */
    public SelectWsdlDialog(JDialog parent, String title) {
        super(parent, title, true);
        owner = parent;
        initComponents();
    }

    /**
     * Create a WSDL selection dialog (modal).
     *
     * @param parent the parent
     * @param title the title to use
     */
    public SelectWsdlDialog(JFrame parent, String title) {
        super(parent, title, true);
        initComponents();
    }

    /**
     * Get the parsed Wsdl.
     *
     * <p>If this is null the user cancelled the dialog.</p>
     *
     * @return The Wsdl or null.
     */
    public Wsdl getWsdl() {
        return wsdl;
    }

    /**
     * Get the wsdl Document object.
     *
     * <p>If the Wsdl is not null this will be a valid Document.</p>
     *
     * @return The Document or null
     */
    public Document getWsdlDocument() {
        return wsdlLocationPanel.getWsdlDocument();
    }

    /**
     * Get the location from which the Wsdl was loaded.
     *
     * <p>Note that this should NOT be used as the base URI for the WSDL unless
     * it is an http (or https) url.</p>
     *
     * @return The Wsdl.
     */
    public String getWsdlUrl() {
        return wsdlLocationPanel.getWsdlUrl();
    }

    /**
     * Set the WSDL location.
     *
     * @param urlOrPath
     */
    public void setWsdlUrl(String urlOrPath) {
        wsdlLocationPanel.setWsdlUrl(urlOrPath);
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(SelectWsdlDialog.class.getName());

    private JDialog owner;
    private WsdlLocationPanel wsdlLocationPanel;
    private Wsdl wsdl;

    private JButton okButton;
    private JButton cancelButton;
    private JPanel mainPanel;
    private JPanel controlPanel;

    private void initComponents() {
        Utilities.setEscKeyStrokeDisposes(this);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        wsdlLocationPanel = new WsdlLocationPanel(this, logger);
        controlPanel.setLayout(new BorderLayout());
        controlPanel.add(wsdlLocationPanel, BorderLayout.CENTER);
        add(mainPanel);

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doOk();
            }
        });
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doCancel();
            }
        });

        pack();
    }

    private void doOk() {
        wsdl = wsdlLocationPanel.getWsdl();

        if (wsdl != null) {
            this.setVisible(false);
        }
    }

    private void doCancel() {
        this.setVisible(false);
    }
}
