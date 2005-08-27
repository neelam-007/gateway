package com.l7tech.console.panels;

import com.japisoft.xmlpad.PopupModel;
import com.japisoft.xmlpad.UIAccessibility;
import com.japisoft.xmlpad.XMLContainer;
import com.japisoft.xmlpad.action.ActionModel;
import com.japisoft.xmlpad.editor.XMLEditor;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.Wsdl;
import com.l7tech.console.action.Actions;
import com.l7tech.console.event.PolicyEvent;
import com.l7tech.console.event.PolicyListener;
import com.l7tech.console.tree.policy.SchemaValidationTreeNode;
import com.l7tech.console.util.Registry;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.xml.SchemaValidation;
import com.l7tech.service.PublishedService;
import com.l7tech.objectmodel.ObjectModelException;
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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.rmi.RemoteException;


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
    public SchemaValidationPropertiesDialog(Frame owner, SchemaValidationTreeNode node, PublishedService service) {
        super(owner, false);
        if (node == null || node.getAssertion() == null) {
            throw new IllegalArgumentException("Schema Validation Node == null");
        }
        schemaValidationAssertion = node.getAssertion();
        this.service = service;
        initialize();
    }

    /**
     * modal construction
     */
    public SchemaValidationPropertiesDialog(Frame owner, SchemaValidation assertion, PublishedService service) {
        super(owner, true);
        if (assertion == null) {
            throw new IllegalArgumentException("Schema Validation == null");
        }
        schemaValidationAssertion = assertion;
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
        analyzeWsdl(wsdlXml);
        return wsdlBindingSoapUseIsLiteral;
    }

    private boolean wsdlSchemaAppliesToArguments() {
        if (!service.isSoap()) return false;
        String wsdlXml = service.getWsdlXml();
        if (wsdlXml == null) return false;
        analyzeWsdl(wsdlXml);
        return wsdlBindingSoapUseIsLiteral && !wsdlBindingStyleIsDocument;
    }

    /**
     * Determine what kind of service is defined in the wsdl (doc/literal, rpc/encoded, rpc/literal)
     */
    private void analyzeWsdl(String wsdlXml) {
        try {
            Wsdl wsdl = Wsdl.newInstance(null, new StringReader(wsdlXml));
            wsdl.setShowBindings(Wsdl.SOAP_BINDINGS);
            Collection bindings = wsdl.getBindings();
            if (bindings.isEmpty()) return;

            try {
                wsdlBindingStyleIsDocument = true;
                for (Iterator iterator = bindings.iterator(); iterator.hasNext();) {
                    Binding binding = (Binding)iterator.next();
                    if (!Wsdl.STYLE_DOCUMENT.equals(wsdl.getBindingStyle(binding))) {
                        wsdlBindingStyleIsDocument = false;
                        break;
                    }
                }
                wsdlBindingSoapUseIsLiteral = true;
                for (Iterator iterator = bindings.iterator(); iterator.hasNext();) {
                    Binding binding = (Binding)iterator.next();
                    if (!Wsdl.USE_LITERAL.equals(wsdl.getSoapUse(binding))) {
                        wsdlBindingSoapUseIsLiteral = false;
                        break;
                    }
                }
            } catch (WSDLException e) {
                log.log(Level.WARNING, "Could not determine soap use", e);
            }
            return;
        } catch (WSDLException e) {
            log.log(Level.WARNING, "Wsdl parsing error", e);
        }
        return ;
    }

    /**
     * return true if somethign is unresolved
     */
    private boolean checkForUnresolvedImports(Document schemaDoc) {
        Element schemael = schemaDoc.getDocumentElement();
        List listofimports = XmlUtil.findChildElementsByName(schemael, schemael.getNamespaceURI(), "import");
        if (listofimports.isEmpty()) return false;
        ArrayList unresolvedImportsList = new ArrayList();
        Registry reg = Registry.getDefault();
        if (reg == null || reg.getSchemaAdmin() == null) {
            throw new RuntimeException("No access to registry. Cannot check for unresolved imports.");
        }
        for (Iterator iterator = listofimports.iterator(); iterator.hasNext();) {
            Element importEl = (Element) iterator.next();
            String importns = importEl.getAttribute("namespace");
            String importloc = importEl.getAttribute("schemaLocation");
            try {
                if (importloc == null || reg.getSchemaAdmin().findByName(importloc).isEmpty()) {
                    if (importns == null || reg.getSchemaAdmin().findByTNS(importns).isEmpty()) {
                        if (importloc != null) {
                            unresolvedImportsList.add(importloc);
                        } else {
                            unresolvedImportsList.add(importns);
                        }
                    }
                }
            } catch (ObjectModelException e) {
                throw new RuntimeException("Error trying to look for import schema in global schema");
            }  catch (RemoteException e) {
                throw new RuntimeException("Error trying to look for import schema in global schema");
            }
        }
        if (!unresolvedImportsList.isEmpty()) {
            StringBuffer msg = new StringBuffer("The assertion cannot be saved because the schema\n" +
                                                "contains the following unresolved imported schemas:\n");
            for (Iterator iterator = unresolvedImportsList.iterator(); iterator.hasNext();) {
                msg.append(iterator.next());
                msg.append("\n");
            }
            msg.append("Would you like to import those unresolved schemas now?");
            if (JOptionPane.showConfirmDialog(this, msg, "Unresolved Imports", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                GlobalSchemaDialog globalSchemaManager = new GlobalSchemaDialog(this);
                globalSchemaManager.pack();
                Utilities.centerOnScreen(globalSchemaManager);
                globalSchemaManager.show();
            }
            return true;
        }
        return false;
    }

    private void ok() {
        // check that whatever is captured is an xml document and a schema
        String contents = uiAccessibility.getEditor().getText();
        if (contents == null || contents.length() < 1) {
            log.warning("empty doc");
            displayError(resources.getString("error.notschema"), null);
            return;
        }
        Document doc = stringToDoc(contents);
        if (doc == null) {
            displayError(resources.getString("error.notschema"), null);
            return;
        }
        if (!docIsSchema(doc)) {
            displayError(resources.getString("error.notschema"), null);
            return;
        }

        // before saving, make sure all imports are resolveable
        if (checkForUnresolvedImports(doc)) {
            return;
        }

        // save new schema
        schemaValidationAssertion.setSchema(contents);
        schemaValidationAssertion.setApplyToArguments(appliesToMessageArguments.isSelected());
        fireEventAssertionChanged(schemaValidationAssertion);
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
            final XMLEditor editor = uiAccessibility.getEditor();
            editor.setText(result);
            editor.setLineNumber(1);
        }
        if (wsdlSchemaAppliesToArguments()) {
            checkSetAppliesToArgs();
        }
    }

    private void checkSetAppliesToArgs() {
        if (!appliesToMessageArguments.isSelected()) {
        String msg = "The WSDL style seems to indicate that the\n" +
                     "schema validation should be applied to the body\n" +
                     "'arguments' rather than the entire body. Would you\n" +
                     "like to change the setting accordingly?";
        int res = JOptionPane.showConfirmDialog(this, msg,
                                                "Schema Applies To", JOptionPane.YES_NO_OPTION,
                                                JOptionPane.QUESTION_MESSAGE);
            if (res == JOptionPane.YES_OPTION) {
                appliesToMessageArguments.setSelected(true);
            }
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

        // wrap this with border settings
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(BORDER_PADDING, BORDER_PADDING, BORDER_PADDING, BORDER_PADDING);
        constraints.anchor = GridBagConstraints.NORTHWEST;

        JPanel bordered = new JPanel();
        bordered.setLayout(new GridBagLayout());
        bordered.add(loadFromFile, constraints);

        constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.VERTICAL;
        constraints.weightx = 1.0;
        constraints.gridx++;
        constraints.insets = new Insets(BORDER_PADDING, BORDER_PADDING, BORDER_PADDING, BORDER_PADDING);
        bordered.add(Box.createGlue(), constraints);

        constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridy++;
        constraints.weighty = 1.0;
        constraints.gridheight = 2;
        constraints.insets = new Insets(BORDER_PADDING, BORDER_PADDING, BORDER_PADDING, BORDER_PADDING);
        JPanel radioPanel = new JPanel();
        radioPanel.setBorder(BorderFactory.createTitledBorder("Schema Applies To:"));
        radioPanel.setLayout(new GridLayout(2,1));
        radioPanel.add(appliesToEntireMessageMessage);
        radioPanel.add(appliesToMessageArguments);
        if (schemaValidationAssertion.isApplyToArguments()) {
            appliesToMessageArguments.setSelected(true);
        } else {
            appliesToEntireMessageMessage.setSelected(true);
        }
        bordered.add(radioPanel, constraints);

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
                if (schemaValidationAssertion.getSchema() != null) {
                    XMLEditor editor = uiAccessibility.getEditor();
                    editor.setText(reformatxml(schemaValidationAssertion.getSchema()));
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
        popupModel.removeAction(ActionModel.getActionByName(ActionModel.TREE_SELECTNODE_ACTION));
        popupModel.removeAction(ActionModel.getActionByName(ActionModel.TREE_COMMENTNODE_ACTION));
        popupModel.removeAction(ActionModel.getActionByName(ActionModel.TREE_COPYNODE_ACTION));
        popupModel.removeAction(ActionModel.getActionByName(ActionModel.TREE_CUTNODE_ACTION));
        popupModel.removeAction(ActionModel.getActionByName(ActionModel.TREE_EDITNODE_ACTION));
        popupModel.removeAction(ActionModel.getActionByName(ActionModel.TREE_CLEANHISTORY_ACTION));
        popupModel.removeAction(ActionModel.getActionByName(ActionModel.TREE_ADDHISTORY_ACTION));
        popupModel.removeAction(ActionModel.getActionByName(ActionModel.TREE_PREVIOUS_ACTION));
        popupModel.removeAction(ActionModel.getActionByName(ActionModel.TREE_NEXT_ACTION));

        loadFromFile = new JButton();
        loadFromFile.setText(resources.getString("loadFromFile.name"));
        appliesToEntireMessageMessage = new JRadioButton("Entire Message Body");
        appliesToMessageArguments = new JRadioButton("Message Arguments");
        ButtonGroup bg = new ButtonGroup();
        bg.add(appliesToEntireMessageMessage);
        bg.add(appliesToMessageArguments);
    }

    private void initResources() {
        Locale locale = Locale.getDefault();
        resources = ResourceBundle.getBundle("com.l7tech.console.resources.SchemaValidationPropertiesDialog", locale);
    }

    private JButton helpButton;
    private JButton okButton;
    private JButton cancelButton;
    private JButton readFromWsdlButton;
    private JButton resolveButton;
    private JButton loadFromFile;
    private JRadioButton appliesToEntireMessageMessage;
    private JRadioButton appliesToMessageArguments;
    private JTextField urlTxtFld;

    private XMLContainer xmlContainer;
    private UIAccessibility uiAccessibility;

    private ResourceBundle resources;

    private SchemaValidation schemaValidationAssertion;
    private PublishedService service;

    private final Logger log = Logger.getLogger(getClass().getName());
    private final EventListenerList listenerList = new EventListenerList();
    // cached values
    private boolean wsdlBindingStyleIsDocument;
    private boolean wsdlBindingSoapUseIsLiteral;

    private static int BORDER_PADDING = 20;
    private static int CONTROL_SPACING = 5;
}
