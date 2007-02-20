package com.l7tech.console.panels;

import com.japisoft.xmlpad.PopupModel;
import com.japisoft.xmlpad.UIAccessibility;
import com.japisoft.xmlpad.XMLContainer;
import com.japisoft.xmlpad.action.ActionModel;
import com.japisoft.xmlpad.editor.XMLEditor;
import com.l7tech.common.gui.FilterDocument;
import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.Wsdl;
import com.l7tech.console.action.Actions;
import com.l7tech.console.action.CreateServiceWsdlAction;
import com.l7tech.console.util.TopComponents;
import com.l7tech.service.PublishedService;
import com.l7tech.service.ServiceDocument;

import org.w3c.dom.Document;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.HashSet;
import java.util.Set;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SSM Dialog for editing properties of a PublishedService object.
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jan 22, 2007<br/>
 */
public class ServicePropertiesDialog extends JDialog {
    private PublishedService subject;
    private XMLEditor editor;
    private final Logger logger = Logger.getLogger(ServicePropertiesDialog.class.getName());
    private Collection<ServiceDocument> newWsdlDocuments;
    private Document newWSDL = null;
    private String newWSDLUrl = null;
    private boolean wasoked = false;
    private static final String STD_PORT = ":8080";
    private static final String STD_PORT_DISPLAYED = ":[port]";
    private JPanel mainPanel;
    private JTabbedPane tabbedPane1;
    private JTextField nameField;
    private JRadioButton noURIRadio;
    private JRadioButton customURIRadio;
    private JTextField uriField;
    private JLabel routingURL;
    private JCheckBox getCheck;
    private JCheckBox putCheck;
    private JCheckBox postCheck;
    private JCheckBox deleteCheck;
    private JPanel wsdlPanel;
    private JButton resetWSDLButton;
    private JButton helpButton;
    private JButton okButton;
    private JButton cancelButton;
    private JRadioButton enableRadio;
    private JRadioButton diableRadio;
    private JButton editWSDLButton;
    private String ssgURL;

    public ServicePropertiesDialog(Frame owner, PublishedService svc) {
        super(owner, true);
        subject = svc;
        initialize();
        DialogDisplayer.suppressSheetDisplay(this); // incompatible with xmlpad
    }

    private void initialize() {
        setContentPane(mainPanel);
        setTitle("Published Service Properties");

        nameField.setDocument(new FilterDocument(254, new FilterDocument.Filter() {
                                                             public boolean accept(String str) {
                                                                 return str != null;
                                                             }
                                                         }));
        uriField.setDocument(new FilterDocument(127, null));

        // set initial data
        nameField.setText(subject.getName());
        if (subject.isDisabled()) {
            diableRadio.setSelected(true);
        } else {
            enableRadio.setSelected(true);
        }
        ssgURL = TopComponents.getInstance().ssgURL();
        if (!ssgURL.startsWith("http://")) {
            ssgURL = "http://" + ssgURL;
        }
        int pos = ssgURL.lastIndexOf(':');
        if (pos > 4) {
            ssgURL = ssgURL.substring(0, pos);

            ssgURL = ssgURL + STD_PORT;
        } else {
            if (ssgURL.endsWith("/") || ssgURL.endsWith("\\")) {
                ssgURL = ssgURL.substring(0, ssgURL.length()-1) + STD_PORT;
            } else {
                ssgURL = ssgURL + STD_PORT;
            }
        }
        String existinguri = subject.getRoutingUri();
        if (existinguri == null) {
            noURIRadio.setSelected(true);
            customURIRadio.setSelected(false);
            uriField.setEnabled(false);
        } else {
            noURIRadio.setSelected(false);
            customURIRadio.setSelected(true);
            uriField.setText(existinguri);
        }
        ActionListener toggleurifield = new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                if (noURIRadio.isSelected()) {
                    uriField.setEnabled(false);
                } else {
                    uriField.setEnabled(true);
                }
            }
        };
        noURIRadio.addActionListener(toggleurifield);
        customURIRadio.addActionListener(toggleurifield);
        Set<String> methods = subject.getHttpMethods();
        if (methods.contains("GET")) {
            getCheck.setSelected(true);
        }
        if (methods.contains("PUT")) {
            putCheck.setSelected(true);
        }
        if (methods.contains("POST")) {
            postCheck.setSelected(true);
        }
        if (methods.contains("DELETE")) {
            deleteCheck.setSelected(true);
        }

        if (!subject.isSoap()) {
            tabbedPane1.setEnabledAt(2, false);
            noURIRadio.setEnabled(false);
            customURIRadio.setSelected(true);
        } else {
            XMLContainer xmlContainer = new XMLContainer(true);
            final UIAccessibility uiAccessibility = xmlContainer.getUIAccessibility();
            editor = uiAccessibility.getEditor();
            editor.setText(subject.getWsdlXml());
            Action reformatAction = ActionModel.getActionByName(ActionModel.FORMAT_ACTION);
            reformatAction.actionPerformed(null);
            uiAccessibility.setTreeAvailable(false);
            uiAccessibility.setTreeToolBarAvailable(false);
            xmlContainer.setEditable(false);
            uiAccessibility.setToolBarAvailable(false);
            xmlContainer.setStatusBarAvailable(false);
            PopupModel popupModel = xmlContainer.getPopupModel();
            // remove the unwanted actions
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.PARSE_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.FORMAT_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.LOAD_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.SAVE_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.SAVEAS_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.NEW_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.INSERT_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.COMMENT_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.TREE_SELECTNODE_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.TREE_COMMENTNODE_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.TREE_COPYNODE_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.TREE_CUTNODE_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.TREE_EDITNODE_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.TREE_CLEANHISTORY_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.TREE_ADDHISTORY_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.TREE_PREVIOUS_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.TREE_NEXT_ACTION));
            boolean lastWasSeparator = true; // remove trailing separator
            for (int i=popupModel.size()-1; i>=0; i--) {
                boolean isSeparator = popupModel.isSeparator(i);
                if (isSeparator && (i==0 || lastWasSeparator)) {
                    popupModel.removeSeparator(i);
                } else {
                    lastWasSeparator = isSeparator;
                }
            }
            wsdlPanel.setLayout(new BorderLayout());
            wsdlPanel.add(xmlContainer.getView(), BorderLayout.CENTER);
        }
        // event handlers
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
                help();
            }
        });

        uriField.addKeyListener(new KeyListener() {
            public void keyPressed(KeyEvent e) {}
            public void keyReleased(KeyEvent e) {
                updateURL();
            }
            public void keyTyped(KeyEvent e) {}
        });

        noURIRadio.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent actionEvent) {
                updateURL();
            }
        });
        customURIRadio.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent actionEvent) {
                updateURL();
            }
        });

        resetWSDLButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent actionEvent) {
                resetWSDL();
            }
        });

        editWSDLButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                editWsdl();
            }
        });

        Utilities.setEscAction(this, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                cancel();
            }
        });

        Utilities.setEnterAction(this, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                ok();
            }
        });

        updateURL();
    }

    private void help() {
        Actions.invokeHelp(this);
    }

    private void cancel() {
        this.dispose();
    }

    public boolean wasOKed() {
        return wasoked;
    }

    public Collection<ServiceDocument> getServiceDocuments() {
        Collection<ServiceDocument> docs = null;
        if (newWsdlDocuments != null)
            docs = Collections.unmodifiableCollection(newWsdlDocuments);        
        return docs;
    }

    private void ok() {

        // validate the name
        String name = nameField.getText();
        if (name == null || name.length() < 1) {
            JOptionPane.showMessageDialog(this, "The service must be given a name");
            return;
        }
        // validate the uri
        String newURI = null;
        if (customURIRadio.isSelected()) {
            newURI = uriField.getText();
            // remove leading '/'s
            if (newURI != null) {
                newURI = newURI.trim();
                while (newURI.startsWith("//")) {
                    newURI = newURI.substring(1);
                }
                if (!newURI.startsWith("/")) newURI = "/" + newURI;
                uriField.setText(newURI);
            }
            try {
                new URL(ssgURL + newURI);
            } catch (MalformedURLException e) {
                JOptionPane.showMessageDialog(this, ssgURL + newURI + " is not a valid URL");
                return;
            }
        }

        if (!subject.isSoap()) {
            if (newURI == null || newURI.length() <= 0 || newURI.equals("/")) { // non-soap service cannot have null routing uri
                JOptionPane.showMessageDialog(this, "Cannot set empty uri on non-soap service");
                return;
            } else if (newURI.startsWith(SecureSpanConstants.SSG_RESERVEDURI_PREFIX)) {
                JOptionPane.showMessageDialog(this, "URI cannot start with " + SecureSpanConstants.SSG_RESERVEDURI_PREFIX);
                return;
            }
        } else {
            if (newURI != null && newURI.length() > 0 && newURI.startsWith(SecureSpanConstants.SSG_RESERVEDURI_PREFIX)) {
                JOptionPane.showMessageDialog(this, "URI cannot start with " + SecureSpanConstants.SSG_RESERVEDURI_PREFIX);
                return;
            }
        }

        if (!getCheck.isSelected() && !putCheck.isSelected() && !postCheck.isSelected() && !deleteCheck.isSelected()) {
            int res = JOptionPane.showConfirmDialog(this, "Because no HTTP methods are selected, this service will " +
                                                          "not be accessible through HTTP. Are you sure you want to " +
                                                          "do this?", "Warning", JOptionPane.YES_NO_OPTION);
            if (res != JOptionPane.YES_OPTION) {
                return;
            }
        }

        // set the new data into the edited subject
        subject.setName(name);
        subject.setDisabled(!enableRadio.isSelected());
        if (newURI != null && (newURI.length() < 1 || newURI.equals("/"))) newURI = null;
        subject.setRoutingUri(newURI);
        Set<String> methods = new HashSet<String>();
        if (getCheck.isSelected()) {
            methods.add("GET");
        }
        if (putCheck.isSelected()) {
            methods.add("PUT");
        }
        if (postCheck.isSelected()) {
            methods.add("POST");
        }
        if (deleteCheck.isSelected()) {
            methods.add("DELETE");
        }
        subject.setHttpMethods(methods);

        if (newWSDL != null) {
            try {
                subject.setWsdlUrl(newWSDLUrl.startsWith("http") ? newWSDLUrl : null);
                subject.setWsdlXml(XmlUtil.nodeToString(newWSDL));
            } catch (MalformedURLException e) {
                throw new RuntimeException("Invalid URL", e);
            } catch (RemoteException e) {
                throw new RuntimeException("Error Changing WSDL. Consult log for more information.", e);
            } catch (IOException e) {
                throw new RuntimeException("Error Changing WSDL. Consult log for more information.", e);
            }
        }

        wasoked = true;
        cancel();
    }

    private void updateURL() {
        String currentValue = null;
        if (customURIRadio.isSelected()) {
            currentValue = uriField.getText();
        }
        String urlvalue;
        if (currentValue != null) {
            currentValue = currentValue.trim();
            String cvWithoutSlashes = currentValue.replace("/", "");
            if (cvWithoutSlashes.length() <= 0) {
                currentValue = null;
            }
        }
        if (currentValue == null || currentValue.length() < 1) {
            urlvalue = ssgURL + "/ssg/soap";
        } else {
            if (currentValue.startsWith("/")) {
                urlvalue = ssgURL + currentValue;
            } else {
                urlvalue = ssgURL + "/" + currentValue;
                if (currentValue.length() > 0) {
                    uriField.setText("/" + currentValue);
                }
            }
        }

        String tmp = urlvalue.replace(STD_PORT, STD_PORT_DISPLAYED);
        tmp = tmp.replace("http://", "http(s)://");
        routingURL.setText("<html><p><a href=\"" + urlvalue + "\">" + tmp + "</a></p></html>");
    }

    private void editWsdl() {
        CreateServiceWsdlAction action = new CreateServiceWsdlAction();
        try {
            Document dom = XmlUtil.parse(new StringReader(subject.getWsdlXml()), false);
            action.setWsdl(Wsdl.newInstance("", dom).getDefinition());
            action.actionPerformed(null);
        } catch (Exception e) {
            logger.log(Level.WARNING, "cannot display new wsdl ", e);
        }

    }

    private void resetWSDL() {
        String existingURL = subject.getWsdlUrl();
        if (existingURL == null) existingURL = "";

        final SelectWsdlDialog rwd = new SelectWsdlDialog(this, "Reset WSDL");
        rwd.setWsdlUrl(existingURL);
        rwd.pack();
        Utilities.centerOnScreen(rwd);
        DialogDisplayer.display(rwd, new Runnable() {
            public void run() {
                Wsdl wsdl = rwd.getWsdl();
                if (wsdl != null) {
                    newWSDL = rwd.getWsdlDocument();
                    newWSDLUrl = rwd.getWsdlUrl();

                    newWsdlDocuments = new ArrayList();
                    for (int i=1; i<rwd.getWsdlCount(); i++) {
                        ServiceDocument sd = new ServiceDocument();
                        sd.setUri(rwd.getWsdlUri(i));
                        sd.setContentType("text/xml");
                        sd.setType("WSDL-IMPORT");
                        sd.setContents(rwd.getWsdlContent(i));
                        newWsdlDocuments.add(sd);
                    }

                    // display new xml in xml display
                    try {
                        editor.setText(XmlUtil.nodeToFormattedString(newWSDL));
                    } catch (IOException e) {
                        logger.log(Level.WARNING, "cannot display new wsdl ", e);
                    }
                }
            }
        });
    }
}
