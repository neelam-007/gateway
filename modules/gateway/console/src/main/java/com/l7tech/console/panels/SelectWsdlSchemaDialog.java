package com.l7tech.console.panels;

import com.l7tech.console.util.XMLContainerFactory;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.common.io.XmlUtil;

import com.japisoft.xmlpad.XMLContainer;
import org.dom4j.DocumentException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXParseException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.List;

/**
 * A dialog that allows to select a schema from a wsdl and specify which portion of it to import.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Sep 17, 2004<br/>
 */
public class SelectWsdlSchemaDialog extends JDialog {
    private XMLContainer xmlContainer;
    public SelectWsdlSchemaDialog(JDialog parent, List<Element> fullSchemas, List<Element> inputSchemas, List<Element> outputSchemas)
      throws DocumentException, IOException, SAXParseException {
        super(parent, true);
        this.fullSchemas = fullSchemas;
        this.inputSchemas = inputSchemas;
        this.outputSchemas = outputSchemas;
        initialize();
        DialogDisplayer.suppressSheetDisplay(this); // incompatible with xmlpad
    }

    private void initializeXmlContainer() {
        assert(xmlContainer == null);
        xmlContainer = XMLContainerFactory.createXmlContainer(true);
        xmlContainer.setEditable(false);
    }

    private void initialize() throws DocumentException, IOException, SAXParseException {
        Container p = getContentPane();
        p.setLayout(new BorderLayout());
        p.add(mainPanel, BorderLayout.CENTER);
        setTitle("Extract Schema from WSDL");
        // create the xml control for the recipient panel
        initializeXmlContainer();
        xmlpanel.setLayout(new BorderLayout());
        JScrollPane jScrollPane = new JScrollPane(xmlContainer.getView());
        jScrollPane.setPreferredSize(new Dimension(600, 400));
        xmlpanel.add(jScrollPane, BorderLayout.CENTER);
        ButtonGroup bg = new ButtonGroup();
        bg.add(allradio);
        bg.add(requestradio);
        bg.add(responseradio);
        setInitialData();
        setEventHandlers();
    }

    private void setInitialData() {
        DefaultComboBoxModel model = (DefaultComboBoxModel)schemaselector.getModel();
        if ( fullSchemas == null || fullSchemas.size() < 1 ) {
            model.addElement("No schema present in the wsdl.");
            okbutton.setEnabled(false);
            allradio.setEnabled(false);
            requestradio.setEnabled(false);
            responseradio.setEnabled(false);
            schemaselector.setEnabled(false);
        } else {
            String first = null;
            for(Element schema : fullSchemas) {
                String tns = schema.getAttribute("targetNamespace");
                if (tns == null || tns.length() < 1) {
                    tns = "Undefined targetNamespace";
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
            node = fullSchemas.get(schemaIndex);
        } else if (requestradio.isSelected()) {
            node = inputSchemas.get(schemaIndex);
        } else if (responseradio.isSelected()) {
            node = outputSchemas.get(schemaIndex);
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
            @Override
            public void actionPerformed(ActionEvent e) {
                ok();
            }
        });
        cancelbutton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cancel();
            }
        });
        allradio.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setSchema();
            }
        });
        requestradio.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setSchema();
            }
        });
        responseradio.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setSchema();
            }
        });
        schemaselector.addActionListener(new ActionListener() {
            @Override
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
                okedSchemaNode = node;
            } catch (IOException e) {
                // todo
            }
        }
        SelectWsdlSchemaDialog.this.dispose();
    }

    @Override
    public void dispose() {
        xmlContainer.dispose();
        super.dispose();
    }

    public String getOkedSchema() {
        return okedSchema;
    }

    public Node getOkedSchemaNode() {
        return okedSchemaNode;
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
    private List<Element> fullSchemas;
    private List<Element> inputSchemas;
    private List<Element> outputSchemas;
    private String okedSchema;
    private Node okedSchemaNode;

}
