package com.l7tech.console.panels;

import com.l7tech.console.action.Actions;
import com.l7tech.console.tree.policy.SchemaValidationTreeNode;
import com.l7tech.policy.assertion.xml.SchemaValidation;
import com.l7tech.service.PublishedService;
import org.syntax.jedit.JEditTextArea;
import org.syntax.jedit.SyntaxDocument;
import org.syntax.jedit.tokenmarker.XMLTokenMarker;
import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.logging.Level;


/**
 * A dialog to view / configure the properties of a schema validation assertion
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Feb 6, 2004<br/>
 * $Id$<br/>
 *
 */
public class SchemaValidationPropertiesDialog extends JDialog {

    public SchemaValidationPropertiesDialog(Frame owner, SchemaValidationTreeNode node, PublishedService service) {
        super(owner, false);
        setTitle("Schema validation properties");
        subject = node;
        this.service = service;
        initialize();
    }

    private void initialize() {
        // create controls
        allocControls();

        // do layout stuff
        Container contents = getContentPane();
        contents.setLayout(new BorderLayout(0,0));
        contents.add(constructCentralPanel(), BorderLayout.CENTER);
        contents.add(constructBottomButtonsPanel(), BorderLayout.SOUTH);

        // create callbacks
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ok();
            }
        });
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cancel();
            }
        });
        helpButton.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Actions.invokeHelp(SchemaValidationPropertiesDialog.this);
            }
        });
        readFromWsdlButton.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                readFromWsdl();
            }
        });
        resolveButton.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                readFromUrl();
            }
        });
    }

    private void ok() {
        // validate the contents of the xml control
        String contents = wsdlTextArea.getText();
        if (!docIsSchema(contents)) {
            displayError("No xml document specified or invalid schema.");
            return;
        }
        // save new schema
        subject.getAssertion().setSchema(contents);
        // exit
        SchemaValidationPropertiesDialog.this.dispose();
    }

    private boolean docIsSchema(String str) {
        if (str != null || str.length() < 1) return false;
        Document doc = stringToDoc(str);
        if (doc == null) return false;
        Element rootEl = doc.getDocumentElement();
        if (!SchemaValidation.TOP_SCHEMA_ELNAME.equals(rootEl.getNodeName())) {
            log.log(Level.WARNING, "document is not schema (top element is not " +
                                    SchemaValidation.TOP_SCHEMA_ELNAME + ")");
            return false;
        }
        if (!SchemaValidation.W3C_XML_SCHEMA.equals(rootEl.getNamespaceURI())) {
            log.log(Level.WARNING, "document is not schema (namespace is not + " +
                                    SchemaValidation.W3C_XML_SCHEMA + ")");
            return false;
        }
        return true;
    }

    private Document stringToDoc(String str) {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        Document doc = null;
        InputSource source = new InputSource(new ByteArrayInputStream(str.getBytes()));
        try {
            doc = dbf.newDocumentBuilder().parse(source);
        } catch (SAXException e) {
            log.log(Level.WARNING, "cannot parse doc", e);
            return null;
        } catch (IOException e) {
            log.log(Level.WARNING, "cannot parse doc", e);
            return null;
        } catch (ParserConfigurationException e) {
            log.log(Level.WARNING, "cannot parse doc", e);
            return null;
        }
        return doc;
    }

    private void cancel() {
        SchemaValidationPropertiesDialog.this.dispose();
    }

    private void readFromWsdl() {
        if (service == null) {
            displayError("No access to wsdl.");
            return;
        }
        String wsdl = null;
        try {
            wsdl = service.getWsdlXml();
        } catch (IOException e) {
            displayError("No access to wsdl.");
            return;
        }

        Document wsdlDoc = stringToDoc(wsdl);
        if (wsdlDoc == null) {
            displayError("WSDL not set.");
            return;
        }

        SchemaValidation tmp = new SchemaValidation();
        try {
            tmp.assignSchemaFromWsdl(wsdlDoc);
        } catch (IllegalArgumentException e) {
            displayError("WSDL not set.");
            return;
        } catch (IOException e) {
            displayError("WSDL not set.");
            return;
        }
        wsdlTextArea.setText(tmp.getSchema());
        okButton.setEnabled(true);
    }

    private void readFromUrl() {
        // todo
    }

    private void displayError(String msg) {
        // todo, an error dbox
        System.err.println(msg);
    }

    /**
     * the central panel contains the populate from schema button, the load from schema panel
     * and the schema xml display
     */
    private JPanel constructCentralPanel() {

        // panel that contains the read from wsdl and the read from url
        JPanel toppanel = new JPanel();
        toppanel.setLayout(new BoxLayout(toppanel, BoxLayout.Y_AXIS));
        toppanel.add(constructLoadFromWsdlPanel());
        toppanel.add(constructLoadFromUrlPanel());

        // panel that contains the xml display
        JPanel centerpanel = new JPanel();
        centerpanel.setLayout(new BorderLayout());
        centerpanel.add(constructXmlDisplayPanel(), BorderLayout.CENTER);

        JPanel output = new JPanel();
        output.setLayout(new BorderLayout());

        output.add(toppanel, BorderLayout.NORTH);
        output.add(centerpanel, BorderLayout.CENTER);

        return output;
    }

    private JPanel constructXmlDisplayPanel() {
        JPanel xmldisplayPanel = new JPanel();
        xmldisplayPanel.setLayout(new BorderLayout(0, CONTROL_SPACING));
        JLabel schemaTitle = new JLabel("Schema to validate against:");
        xmldisplayPanel.add(schemaTitle, BorderLayout.NORTH);

        if (subject != null && subject.getAssertion() != null && subject.getAssertion().getSchema() != null) {
            wsdlTextArea.setText(subject.getAssertion().getSchema());
        } else {
            okButton.setEnabled(false);
        }
        wsdlTextArea.setCaretPosition(0);
        xmldisplayPanel.add(wsdlTextArea, BorderLayout.CENTER);

        // wrap this with border settings
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        constraints.insets = new Insets(0, BORDER_PADDING, 0, BORDER_PADDING);
        JPanel bordered = new JPanel();
        bordered.setLayout(new GridBagLayout());
        bordered.add(xmldisplayPanel, constraints);

        return bordered;
    }

    private JPanel constructLoadFromWsdlPanel() {
        // align to the left
        JPanel loadfromwsdlpanel = new JPanel();
        loadfromwsdlpanel.setLayout(new BorderLayout());
        loadfromwsdlpanel.add(readFromWsdlButton, BorderLayout.WEST);

        // add border
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        constraints.insets = new Insets(BORDER_PADDING, BORDER_PADDING, 0, BORDER_PADDING);
        JPanel bordered = new JPanel();
        bordered.setLayout(new GridBagLayout());
        bordered.add(loadfromwsdlpanel, constraints);

        return bordered;
    }

    private JPanel constructLoadFromUrlPanel() {
        // make controls
        JPanel loadFromUrlPanel = new JPanel();
        loadFromUrlPanel.setLayout(new BorderLayout(CONTROL_SPACING, 0));
        JLabel loadfromurllabel = new JLabel("Load schema from URL:");
        loadFromUrlPanel.add(loadfromurllabel, BorderLayout.WEST);
        loadFromUrlPanel.add(urlTxtFld, BorderLayout.CENTER);
        loadFromUrlPanel.add(resolveButton, BorderLayout.EAST);

        // wrap this with border settings
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        constraints.insets = new Insets(BORDER_PADDING, BORDER_PADDING, BORDER_PADDING, BORDER_PADDING);
        JPanel bordered = new JPanel();
        bordered.setLayout(new GridBagLayout());
        bordered.add(loadFromUrlPanel, constraints);

        return bordered;
    }

    private JPanel constructBottomButtonsPanel() {
        // construct the bottom panel and wrap it with a border
        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new FlowLayout(FlowLayout.TRAILING, CONTROL_SPACING, 0));
        buttonsPanel.add(helpButton);
        buttonsPanel.add(okButton);
        buttonsPanel.add(cancelButton);

        //  make this panel align to the right
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BorderLayout());
        rightPanel.add(buttonsPanel, BorderLayout.EAST);

        // wrap this with border settings
        JPanel output = new JPanel();
        output.setLayout(new FlowLayout(FlowLayout.TRAILING, BORDER_PADDING, BORDER_PADDING));
        output.add(rightPanel);

        return output;
    }

    private void allocControls() {
        // construct buttons
        helpButton = new JButton();
        helpButton.setText("Help");
        okButton = new JButton();
        okButton.setText("Ok");
        cancelButton = new JButton();
        cancelButton.setText("Cancel");
        readFromWsdlButton = new JButton();
        readFromWsdlButton.setText("Populate schema from WSDL");
        urlTxtFld = new JTextField();
        resolveButton = new JButton();
        resolveButton.setText("Resolve");
        wsdlTextArea = new JEditTextArea();
        wsdlTextArea.setDocument(new SyntaxDocument());
        wsdlTextArea.setEditable(false);
        wsdlTextArea.setTokenMarker(new XMLTokenMarker());
    }

    // show the dialog for test purposes
    public static void main(String[] args) {
        SchemaValidationPropertiesDialog dlg = new SchemaValidationPropertiesDialog(null, null, null);
        dlg.pack();
        dlg.show();
    }

    private JButton helpButton;
    private JButton okButton;
    private JButton cancelButton;
    private JButton readFromWsdlButton;
    private JButton resolveButton;
    private JTextField urlTxtFld;
    private JEditTextArea wsdlTextArea;

    private SchemaValidationTreeNode subject;
    private PublishedService service;

    private final Logger log = Logger.getLogger(getClass().getName());

    private static int BORDER_PADDING = 20;
    private static int CONTROL_SPACING = 5;
}
