package com.l7tech.console.panels;

import com.japisoft.xmlpad.PopupModel;
import com.japisoft.xmlpad.UIAccessibility;
import com.japisoft.xmlpad.XMLContainer;
import com.japisoft.xmlpad.editor.XMLEditor;
import com.japisoft.xmlpad.action.ActionModel;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.Wsdl;
import com.l7tech.console.action.Actions;
import com.l7tech.console.event.PolicyEvent;
import com.l7tech.console.event.PolicyListener;
import com.l7tech.console.tree.policy.SchemaValidationTreeNode;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.xml.SchemaValidation;
import com.l7tech.service.PublishedService;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.dom4j.DocumentException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import javax.wsdl.Binding;
import javax.wsdl.WSDLException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * A dialog to view / configure the properties of a schema validation assertion.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Feb 6, 2004<br/>
 */
public class SchemaValidationPropertiesDialog extends JDialog {
    /**
     * modless construction
     */
    public SchemaValidationPropertiesDialog(Frame owner, SchemaValidationTreeNode node, PublishedService service)    {
        super(owner, false);
        if (node == null || node.getAssertion() == null) {
            throw new IllegalArgumentException("Schema Validation Node == null");
        }
        subject = node.getAssertion();
        this.service = service;
        initialize();
    }

    /**
     * modal construction
     */
    public SchemaValidationPropertiesDialog(Frame owner, SchemaValidation assertion, PublishedService service)      {
        super(owner, true);
        if (assertion == null) {
            throw new IllegalArgumentException("Schema Validation == null");
        }
        subject = assertion;
        this.service = service;
        initialize();
    }

    private void initialize() {

        initResources();
        setTitle(resources.getString("window.title"));

        // create controls
        allocControls();

        // do layout stuff
        Container contents = getContentPane();
        contents.setLayout(new BorderLayout(0, 0));
        contents.add(constructCentralPanel(), BorderLayout.CENTER);
        contents.add(constructBottomButtonsPanel(), BorderLayout.SOUTH);
        Actions.setEscKeyStrokeDisposes(this);

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
        helpButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Actions.invokeHelp(SchemaValidationPropertiesDialog.this);
            }
        });
        readFromWsdlButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                readFromWsdl();
            }
        });

        readFromWsdlButton.setEnabled(wsdlExtractSupported());
        readFromWsdlButton.setToolTipText("Extract schema from WSDL; available for 'document/literal' style services");
        resolveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                readFromUrl();
            }
        });

        loadFromFile.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                readFromFile();
            }
        });
    }

    /**
     * Determine whether wsdl extracting is supported. This is supported for 'document'
     * style services only.
     * Traverse all the soap bindings, and if all the bindings are of style 'document'
     * returns true.
     *
     * @return true if 'document' stle supported, false otherwise
     */
    private boolean wsdlExtractSupported() {
        if (!service.isSoap()) return false;
        String wsdlXml = service.getWsdlXml();
        if (wsdlXml == null) return false;

        try {
            Wsdl wsdl = Wsdl.newInstance(null, new StringReader(wsdlXml));
            wsdl.setShowBindings(Wsdl.SOAP_BINDINGS);
            Collection bindings = wsdl.getBindings();
            if (bindings.isEmpty()) return false;

            for (Iterator iterator = bindings.iterator(); iterator.hasNext();) {
                Binding binding = (Binding)iterator.next();
                if (!Wsdl.STYLE_DOCUMENT.equals(wsdl.getBindingStyle(binding))) {
                    return false;
                }
            }
            return true;
        } catch (WSDLException e) {
            log.log(Level.WARNING, "Wsdl parsing error", e);
        }
        return false;
    }


    private void ok() {
        // validate the contents of the xml control
        String contents = uiAccessibility.getEditor().getText();
        if (!docIsSchema(contents)) {
            displayError(resources.getString("error.notschema"), null);
            return;
        }
        // save new schema
        subject.setSchema(contents);
        fireEventAssertionChanged(subject);
        // exit
        SchemaValidationPropertiesDialog.this.dispose();
    }

    /**
     * notfy the listeners
     *
     * @param a the assertion
     */
    private void fireEventAssertionChanged(final Assertion a) {
        SwingUtilities.invokeLater(
          new Runnable() {
              public void run() {
                  int[] indices = new int[a.getParent().getChildren().indexOf(a)];
                  PolicyEvent event = new
                    PolicyEvent(this, new AssertionPath(a.getPath()), indices, new Assertion[]{a});
                  EventListener[] listeners = listenerList.getListeners(PolicyListener.class);
                  for (int i = 0; i < listeners.length; i++) {
                      ((PolicyListener)listeners[i]).assertionsChanged(event);
                  }
              }
          });
    }

    /**
     * add the PolicyListener
     *
     * @param listener the PolicyListener
     */
    public void addPolicyListener(PolicyListener listener) {
        listenerList.add(PolicyListener.class, listener);
    }

    /**
     * remove the the PolicyListener
     *
     * @param listener the PolicyListener
     */
    public void removePolicyListener(PolicyListener listener) {
        listenerList.remove(PolicyListener.class, listener);
    }

    private boolean docIsSchema(String str) {
        if (str == null || str.length() < 1) {
            log.warning("empty doc");
            return false;
        }
        Document doc = stringToDoc(str);
        if (doc == null) return false;
        return docIsSchema(doc);
    }

    private boolean docIsSchema(Document doc) {
        Element rootEl = doc.getDocumentElement();

        if (!SchemaValidation.TOP_SCHEMA_ELNAME.equals(rootEl.getLocalName())) {
            log.log(Level.WARNING, "document is not schema (top element " + rootEl.getLocalName() +
              " is not " + SchemaValidation.TOP_SCHEMA_ELNAME + ")");
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
        Document doc = null;
        try {
            doc = XmlUtil.stringToDocument(str);
        } catch (SAXException e) {
            log.log(Level.WARNING, "cannot parse doc", e);
            return null;
        } catch (IOException e) {
            log.log(Level.WARNING, "cannot parse doc", e);
            return null;
        }
        return doc;
    }

    private String reformatxml(String input) {
        Document doc = stringToDoc(input);
        try {
            return doc2String(doc);
        } catch (IOException e) {
            log.log(Level.INFO, "reformat could not serialize", e);
            return null;
        }
    }

    private void cancel() {
        SchemaValidationPropertiesDialog.this.dispose();
    }

    private void readFromWsdl() {
        if (service == null) {
            displayError(resources.getString("error.noaccesstowsdl"), null);
            return;
        }
        String wsdl = null;
        wsdl = service.getWsdlXml();

        Document wsdlDoc = stringToDoc(wsdl);
        if (wsdlDoc == null) {
            displayError(resources.getString("error.nowsdl"), null);
            return;
        }

        SelectWsdlSchemaDialog schemafromwsdlchooser = null;
        try {
            schemafromwsdlchooser = new SelectWsdlSchemaDialog(this, wsdlDoc);
        } catch (DocumentException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (SAXParseException e) {
            throw new RuntimeException(e);
        }
        schemafromwsdlchooser.pack();
        Utilities.centerOnScreen(schemafromwsdlchooser);
        schemafromwsdlchooser.setVisible(true);
        schemafromwsdlchooser.dispose();
        String result = schemafromwsdlchooser.getOkedSchema();
        if (result != null) {
            uiAccessibility.getEditor().setText(result);
        }
    }

    private void readFromFile() {
        JFileChooser dlg = Utilities.createJFileChooser();

        if (JFileChooser.APPROVE_OPTION != dlg.showOpenDialog(this)) {
            return;
        }
        FileInputStream fis = null;
        String filename = dlg.getSelectedFile().getAbsolutePath();
        try {
            fis = new FileInputStream(dlg.getSelectedFile());
        } catch (FileNotFoundException e) {
            log.log(Level.FINE, "cannot open file" + filename, e);
            return;
        }

        // try to get document
        Document doc = null;
        try {
            doc = XmlUtil.parse(fis);
        } catch (SAXException e) {
            displayError(resources.getString("error.noxmlaturl") + " " + filename, null);
            log.log(Level.FINE, "cannot parse " + filename, e);
            return;
        } catch (IOException e) {
            displayError(resources.getString("error.noxmlaturl") + " " + filename, null);
            log.log(Level.FINE, "cannot parse " + filename, e);
            return;
        }
        // check if it's a schema
        if (docIsSchema(doc)) {
            // set the new schema
            String printedSchema = null;
            try {
                printedSchema = doc2String(doc);
            } catch (IOException e) {
                String msg = "error serializing document";
                displayError(msg, null);
                log.log(Level.FINE, msg, e);
                return;
            }
            uiAccessibility.getEditor().setText(printedSchema);
            //okButton.setEnabled(true);
        } else {
            displayError(resources.getString("error.urlnoschema") + " " + filename, null);
        }
    }

    private void readFromUrl() {
        // get url
        String urlstr = urlTxtFld.getText();
        if (urlstr == null || urlstr.length() < 1) {
            displayError(resources.getString("error.nourl"), null);
            return;
        }
        // compose input source
        URL url = null;
        try {
            url = new URL(urlstr);
        } catch (MalformedURLException e) {
            displayError(urlstr + " " + resources.getString("error.badurl"), null);
            log.log(Level.FINE, "malformed url", e);
            return;
        }
        // try to get document
        InputStream is = null;
        try {
            is = url.openStream();
        } catch (IOException e) {
            displayError(resources.getString("error.urlnocontent") + " " + urlstr, null);
            return;
        }

        Document doc = null;
        try {
            doc = XmlUtil.parse(is);
        } catch (SAXException e) {
            displayError(resources.getString("error.noxmlaturl") + " " + urlstr, null);
            log.log(Level.FINE, "cannot parse " + urlstr, e);
            return;
        } catch (IOException e) {
            displayError(resources.getString("error.noxmlaturl") + " " + urlstr, null);
            log.log(Level.FINE, "cannot parse " + urlstr, e);
            return;
        }
        // check if it's a schema
        if (docIsSchema(doc)) {
            // set the new schema
            String printedSchema = null;
            try {
                printedSchema = doc2String(doc);
            } catch (IOException e) {
                String msg = "error serializing document";
                displayError(msg, null);
                log.log(Level.FINE, msg, e);
                return;
            }
            uiAccessibility.getEditor().setText(printedSchema);
            //okButton.setEnabled(true);
        } else {
            displayError(resources.getString("error.urlnoschema") + " " + urlstr, null);
        }
    }

    private String doc2String(Document doc) throws IOException {
        final StringWriter sw = new StringWriter(512);
        XMLSerializer xmlSerializer = new XMLSerializer();
        xmlSerializer.setOutputCharStream(sw);
        OutputFormat of = new OutputFormat();
        of.setIndent(4);
        xmlSerializer.setOutputFormat(of);
        xmlSerializer.serialize(doc);
        return sw.toString();
    }

    private void displayError(String msg, String title) {
        if (title == null) title = resources.getString("error.window.title");
        JOptionPane.showMessageDialog(this, msg, title, JOptionPane.ERROR_MESSAGE);
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
        toppanel.add(loadFromFilePanel());

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

    private JPanel loadFromFilePanel() {
        JPanel blah = new JPanel();
        blah.setLayout(new BorderLayout());
        blah.add(loadFromFile, BorderLayout.WEST);

        // wrap this with border settings
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        constraints.insets = new Insets(BORDER_PADDING, BORDER_PADDING, BORDER_PADDING, BORDER_PADDING);
        JPanel bordered = new JPanel();
        bordered.setLayout(new GridBagLayout());
        bordered.add(blah, constraints);

        return bordered;
    }

    private JPanel constructXmlDisplayPanel() {
        JPanel xmldisplayPanel = new JPanel();
        xmldisplayPanel.setLayout(new BorderLayout(0, CONTROL_SPACING));
        JLabel schemaTitle = new JLabel(resources.getString("xmldisplayPanel.name"));
        xmldisplayPanel.add(schemaTitle, BorderLayout.NORTH);

        xmldisplayPanel.add(xmlContainer.getView(), BorderLayout.CENTER);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (subject.getSchema() != null) {
                    XMLEditor editor = uiAccessibility.getEditor();
                    editor.setText(reformatxml(subject.getSchema()));
                    editor.setLineNumber(1);
                }
            }
        });


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
        JLabel loadfromurllabel = new JLabel(resources.getString("loadFromUrlPanel.name"));
        loadFromUrlPanel.add(loadfromurllabel, BorderLayout.WEST);
        loadFromUrlPanel.add(urlTxtFld, BorderLayout.CENTER);
        loadFromUrlPanel.add(resolveButton, BorderLayout.EAST);

        // wrap this with border settings
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        constraints.insets = new Insets(BORDER_PADDING, BORDER_PADDING, 0, BORDER_PADDING);
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
        helpButton.setText(resources.getString("helpButton.name"));
        okButton = new JButton();
        okButton.setText(resources.getString("okButton.name"));
        cancelButton = new JButton();
        cancelButton.setText(resources.getString("cancelButton.name"));
        readFromWsdlButton = new JButton();
        readFromWsdlButton.setText(resources.getString("readFromWsdlButton.name"));
        urlTxtFld = new JTextField();
        resolveButton = new JButton();
        resolveButton.setText(resources.getString("resolveButton.name"));
        // configure xml editing widget
        xmlContainer = new XMLContainer(true);
        uiAccessibility = xmlContainer.getUIAccessibility();
        uiAccessibility.setTreeAvailable(false);
        uiAccessibility.setToolBarAvailable(false);
        xmlContainer.setStatusBarAvailable(false);
        PopupModel popupModel = xmlContainer.getPopupModel();
        // remove the unwanted actions
        popupModel.removeAction(ActionModel.getActionByName(ActionModel.LOAD_ACTION));
        popupModel.removeAction(ActionModel.getActionByName(ActionModel.SAVE_ACTION));
        popupModel.removeAction(ActionModel.getActionByName(ActionModel.SAVEAS_ACTION));
        popupModel.removeAction(ActionModel.getActionByName(ActionModel.NEW_ACTION));

        loadFromFile = new JButton();
        loadFromFile.setText(resources.getString("loadFromFile.name"));
    }

    private void initResources() {
        Locale locale = Locale.getDefault();
        resources = ResourceBundle.getBundle("com.l7tech.console.resources.SchemaValidationPropertiesDialog", locale);
    }

    public static void main(String[] args) throws DocumentException, IOException, SAXParseException {
        SchemaValidationPropertiesDialog me = new SchemaValidationPropertiesDialog(null, new SchemaValidation(), null);
        me.pack();
        me.setVisible(true);
        System.exit(0);
    }

    private JButton helpButton;
    private JButton okButton;
    private JButton cancelButton;
    private JButton readFromWsdlButton;
    private JButton resolveButton;
    private JButton loadFromFile;
    private JTextField urlTxtFld;

    private XMLContainer xmlContainer;
    private UIAccessibility uiAccessibility;

    private ResourceBundle resources;

    private SchemaValidation subject;
    private PublishedService service;

    private final Logger log = Logger.getLogger(getClass().getName());
    private final EventListenerList listenerList = new EventListenerList();

    private static int BORDER_PADDING = 20;
    private static int CONTROL_SPACING = 5;
}
