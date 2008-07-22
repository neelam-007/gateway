package com.l7tech.console.panels;

import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.wsdl.WsdlSchemaAnalizer;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.console.util.TopComponents;

import com.japisoft.xmlpad.XMLContainer;
import com.japisoft.xmlpad.UIAccessibility;
import com.japisoft.xmlpad.PopupModel;
import com.japisoft.xmlpad.action.ActionModel;
import org.dom4j.DocumentException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXParseException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

/**
 * A dialog that allows to select a schema from a wsdl and specify which portion of it to import.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Sep 17, 2004<br/>
 * $Id$<br/>
 */
public class SelectWsdlSchemaDialog extends JDialog {
    private XMLContainer xmlContainer;
    public SelectWsdlSchemaDialog(JDialog parent, Document wsdl)
      throws DocumentException, IOException, SAXParseException {
        super(parent, true);
        anal = new WsdlSchemaAnalizer(wsdl);
        anal.splitInputOutputs();
        initialize();
        DialogDisplayer.suppressSheetDisplay(this); // incompatible with xmlpad
    }

    private void initializeXmlContainer() {
        assert(xmlContainer == null);
        xmlContainer = new XMLContainer(true);
        final UIAccessibility uiAccessibility = xmlContainer.getUIAccessibility();
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

        if (TopComponents.getInstance().isApplet()) {
            // Search action tries to get the class loader
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.INSERT_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.SEARCH_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.COMMENT_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.PARSE_ACTION));
        }

        boolean lastWasSeparator = true; // remove trailing separator
        for (int i=popupModel.size()-1; i>=0; i--) {
            boolean isSeparator = popupModel.isSeparator(i);
            if (isSeparator && (i==0 || lastWasSeparator)) {
                popupModel.removeSeparator(i);
            } else {
                lastWasSeparator = isSeparator;
            }
        }
    }

    private void initialize() throws DocumentException, IOException, SAXParseException {
        Container p = getContentPane();
        p.setLayout(new BorderLayout());
        p.add(mainPanel, BorderLayout.CENTER);
        setTitle("Extract Schema from WSDL");
        // create the xml control for the recipient panel
        initializeXmlContainer();
        xmlpanel.setLayout(new BorderLayout());
        xmlpanel.add(xmlContainer.getView(), BorderLayout.CENTER);
        ButtonGroup bg = new ButtonGroup();
        bg.add(allradio);
        bg.add(requestradio);
        bg.add(responseradio);
        setInitialData();
        setEventHandlers();
    }

    private void setInitialData() {
        Element[] schemas = anal.getFullSchemas();
        DefaultComboBoxModel model = (DefaultComboBoxModel)schemaselector.getModel();
        if (schemas == null || schemas.length < 1) {
            model.addElement("No schema present in the wsdl.");
            okbutton.setEnabled(false);
            allradio.setEnabled(false);
            requestradio.setEnabled(false);
            responseradio.setEnabled(false);
            schemaselector.setEnabled(false);
        } else {
            String first = null;
            for (int i = 0; i < schemas.length; i++) {
                Element schema = schemas[i];
                String tns = schema.getAttribute("targetNamespace");
                if (tns == null || tns.length() < 1) {
                    tns = "Undefined targetNamespace " + i;
                }
                model.addElement(tns);
                if (first == null) first = tns;
            }

            model.setSelectedItem(first);
            allradio.setSelected(true);
            setSchema();
        }
    }

    private Node getCurrentSchemaNode() {
        // decide which schema to use based on selected index
        int schemaIndex = schemaselector.getSelectedIndex();
        Node node = null;
        if (allradio.isSelected()) {
            node = anal.getFullSchemas()[schemaIndex];
        } else if (requestradio.isSelected()) {
            node = anal.getInputSchemas()[schemaIndex];
        } else if (responseradio.isSelected()) {
            node = anal.getOutputSchemas()[schemaIndex];
        }
        return node;
    }

    private void setSchema() {
        Node node = getCurrentSchemaNode();
        try {
            if (node != null) {
                xmlContainer.getAccessibility().setText( XmlUtil.nodeToFormattedString(node));
            } else {
                xmlContainer.getAccessibility().setText(XmlUtil.XML_VERSION);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void setEventHandlers() {
        okbutton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ok();
            }
        });
        cancelbutton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cancel();
            }
        });
        allradio.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setSchema();
            }
        });
        requestradio.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setSchema();
            }
        });
        responseradio.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setSchema();
            }
        });
        schemaselector.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setSchema();
            }
        });
    }

    private void ok() {
        Node node = getCurrentSchemaNode();
        if (node != null) {
            try {
                okedSchema = XmlUtil.nodeToFormattedString(node);
            } catch (IOException e) {
                // todo
            }
        }
        SelectWsdlSchemaDialog.this.dispose();
    }

    public void dispose() {
        xmlContainer.dispose();
        super.dispose();
    }

    public String getOkedSchema() {
        return okedSchema;
    }

    private void cancel() {
        SelectWsdlSchemaDialog.this.dispose();
    }

    private JPanel mainPanel;
    private JButton okbutton;
    private JButton cancelbutton;
    private JRadioButton allradio;
    private JRadioButton requestradio;
    private JRadioButton responseradio;
    private JComboBox schemaselector;
    private JPanel xmlpanel;
    private WsdlSchemaAnalizer anal;
    private String okedSchema = null;

}
