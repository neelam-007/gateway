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
        setTitle("Extract Schema from WSDL");
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

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// !!! IMPORTANT !!!
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * !!! IMPORTANT !!!
     * DO NOT edit this method OR call it in your code!
     */
    private void $$$setupUI$$$() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayoutManager(4, 1, new Insets(8, 8, 8, 8), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(panel1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null));
        final JLabel label1 = new JLabel();
        label1.setText("Select the WSDL Schema to Extract:");
        panel1.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        schemaselector = new JComboBox();
        panel1.add(schemaselector, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        xmlpanel = new JPanel();
        mainPanel.add(xmlpanel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(panel2, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null));
        allradio = new JRadioButton();
        allradio.setText("Import Entire Schema");
        panel2.add(allradio, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        requestradio = new JRadioButton();
        requestradio.setText("Import Request-Specific Elements Only");
        panel2.add(requestradio, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        responseradio = new JRadioButton();
        responseradio.setText("Import Response-Specific Elements Only");
        panel2.add(responseradio, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(panel3, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        cancelbutton = new JButton();
        cancelbutton.setText("Cancel");
        panel3.add(cancelbutton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        okbutton = new JButton();
        okbutton.setText("OK");
        panel3.add(okbutton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        final Spacer spacer1 = new Spacer();
        panel3.add(spacer1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null));
    }
}
