package com.l7tech.console.panels;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.WsdlSchemaAnalizer;
import org.syntax.jedit.JEditTextArea;
import org.syntax.jedit.SyntaxDocument;
import org.syntax.jedit.tokenmarker.XMLTokenMarker;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

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
    /*public static void main(String[] args) throws Exception {
        Document wsdl = WsdlSchemaAnalizer.getWsdlSample();
        SelectWsdlSchemaDialog me = new SelectWsdlSchemaDialog(null, wsdl);
        me.pack();
        Utilities.centerOnScreen(me);
        me.show();
        me.dispose();
        String result = me.getOkedSchema();
        System.out.println("Result:\n" + result);
    }*/

    public SelectWsdlSchemaDialog(JDialog parent, Document wsdl) {
        super(parent, true);
        anal = new WsdlSchemaAnalizer(wsdl);
        anal.splitInputOutputs();
        initialize();
    }

    private void initialize() {
        Container p = getContentPane();
        p.setLayout(new BorderLayout());
        p.add(mainPanel, BorderLayout.CENTER);
        setTitle("Extract Schema from Service WSDL.");
        // create the xml control for the recipient panel
        xmlTextArea = new JEditTextArea();
        xmlTextArea.setDocument(new SyntaxDocument());
        xmlTextArea.setEditable(false);
        xmlTextArea.setTokenMarker(new XMLTokenMarker());
        xmlpanel.add(xmlTextArea);
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
        if (node != null) {
            try {
                xmlTextArea.setText(XmlUtil.nodeToFormattedString(node));
            } catch (IOException e) {
                // todo
            }
            xmlTextArea.setCaretPosition(0);
        } else {
            xmlTextArea.setText("");
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
        allradio.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                setSchema();
            }
        });
        requestradio.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                setSchema();
            }
        });
        responseradio.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                setSchema();
            }
        });
        schemaselector.addActionListener(new ActionListener(){
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
    private JEditTextArea xmlTextArea;
    private WsdlSchemaAnalizer anal;
    private String okedSchema = null;

}
