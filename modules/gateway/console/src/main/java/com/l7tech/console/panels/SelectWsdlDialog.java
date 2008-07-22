package com.l7tech.console.panels;

import java.util.logging.Logger;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.*;
import javax.swing.*;

import org.w3c.dom.Document;

import com.l7tech.wsdl.Wsdl;
import com.l7tech.gui.util.Utilities;

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
    public SelectWsdlDialog(Dialog parent, String title) {
        super(parent, title, true);
        initComponents();
    }

    /**
     * Create a WSDL selection dialog (modal).
     *
     * @param parent the parent
     * @param title the title to use
     */
    public SelectWsdlDialog(Frame parent, String title) {
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

    /**
     * Get the URI from which the Wsdl at the index was loaded.
     *
     * <p>Note that this should NOT be used as the base URI for the WSDL unless
     * it is an http (or https) url.</p>
     *
     * @return The Wsdl URI.
     */
    public String getWsdlUri(int index) {
        return wsdlLocationPanel.getWsdlUri(index);
    }

    /**
     * Get the content of the Wsdl at the index.
     *
     * @return The Wsdl XML.
     */
    public String getWsdlContent(int index) {
        return wsdlLocationPanel.getWsdlContent(index);
    }

    /**
     * Get the number of WSDL documents retrieved.
     *
     * <p>0 is the main document, the rest are imports.</p>
     *
     * @return The number of WSDLs
     */
    public int getWsdlCount() {
        return wsdlLocationPanel.getWsdlCount();
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(SelectWsdlDialog.class.getName());

    private WsdlLocationPanel wsdlLocationPanel;
    private Wsdl wsdl;

    private JButton okButton;
    private JButton cancelButton;
    private JPanel mainPanel;
    private JPanel controlPanel;

    private void initComponents() {
        Utilities.setEscKeyStrokeDisposes(this);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        wsdlLocationPanel = new WsdlLocationPanel(this, logger, true, SearchWsdlDialog.uddiEnabled());
        controlPanel.setLayout(new BorderLayout());
        controlPanel.add(wsdlLocationPanel, BorderLayout.CENTER);
        add(mainPanel);

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                wsdl = wsdlLocationPanel.getWsdl();

                if (wsdl != null) {
                    dispose();
                }
            }
        });
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        pack();
    }
}
