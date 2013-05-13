package com.l7tech.console.panels;

import com.japisoft.xmlpad.XMLContainer;
import com.l7tech.console.util.SecurityZoneWidget;
import com.l7tech.console.util.XMLContainerFactory;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.service.SampleMessage;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;

import com.l7tech.objectmodel.EntityType;
import org.w3c.dom.*;

/**
 * Used to edit {@link com.l7tech.gateway.common.service.SampleMessage}s.
 */
public class SampleMessageDialog extends JDialog {
    private final SampleMessage message;
    private final boolean allowOperationChange;
    private boolean ok = false;

    private JTextField operationNameField;
    private JPanel xmlEditorPanel;
    private JButton okButton;
    private JButton cancelButton;
    private JPanel mainPanel;
    private JTextField nameField;
    private SecurityZoneWidget zoneControl;
    private XMLContainer xmlContainer;
    private Map<String, String> namespaces;

    private static final String TITLE = "Sample Message";

    public SampleMessageDialog(Window owner, SampleMessage message, boolean allowOperationChange, Map<String, String> namespaces) throws HeadlessException {
        super(owner, TITLE, SampleMessageDialog.DEFAULT_MODALITY_TYPE);
        this.message = message;
        this.allowOperationChange = allowOperationChange;
        this.namespaces = namespaces;
        init();
        DialogDisplayer.suppressSheetDisplay(this); // incompatible with xmlpad
    }

    private void init() {
        xmlContainer = XMLContainerFactory.createXmlContainer(true);

        xmlEditorPanel.setLayout(new BorderLayout());
        xmlEditorPanel.add(xmlContainer.getView(), BorderLayout.CENTER);

        operationNameField.setEnabled(allowOperationChange);
        String opname = message.getOperationName();
        if (!allowOperationChange && (opname == null || opname.length() == 0)) {
            opname = "<all operations>";
        }
        operationNameField.setText(opname);

        xmlContainer.getAccessibility().setText(message.getXml());
        nameField.setText(message.getName());
        Utilities.setMaxLength(nameField.getDocument(), 128);

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                message.setName(nameField.getText());
                message.setSecurityZone(zoneControl.getSelectedZone());
                if (allowOperationChange) message.setOperationName(operationNameField.getText());
                String text = xmlContainer.getAccessibility().getText();
                try {
                    Document doc = XmlUtil.stringToDocument(text);
                    Map<String, String> prefixMap = getPrefixesMap(doc);
                    HashSet<String> keys = new HashSet<String>(prefixMap.size() + namespaces.size());
                    keys.addAll(namespaces.keySet());
                    keys.addAll(prefixMap.keySet());

                    for(String key : keys) {
                        if(namespaces.containsKey(key) && !prefixMap.containsKey(key)) {
                            continue;
                        } else if(!namespaces.containsKey(key) && prefixMap.containsKey(key)) {
                            namespaces.put(key, prefixMap.get(key));
                        } else if(namespaces.containsKey(key) && prefixMap.containsKey(key)) {
                            if(!namespaces.get(key).equals(prefixMap.get(key))) {
                                JOptionPane.showMessageDialog(
                                        SampleMessageDialog.this,
                                        "The prefix \"" + key + "\" does not match the prefix in the namespace table.",
                                        "Invalid Prefix",
                                        JOptionPane.ERROR_MESSAGE);
                                return;
                            }
                        }
                    }

                    message.setXml(text);
                    ok = true;
                    dispose();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(
                            SampleMessageDialog.this,
                            "The XML is not valid: " + ex.toString(),
                            "Invalid XML",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        DocumentListener docListener = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                enableButtons();
            }

            public void removeUpdate(DocumentEvent e) {
                enableButtons();
            }

            public void changedUpdate(DocumentEvent e) {
                enableButtons();
            }
        };

        operationNameField.getDocument().addDocumentListener(docListener);
        xmlContainer.getDocument().addDocumentListener(docListener);
        nameField.getDocument().addDocumentListener(docListener);

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ok = false;
                dispose();
            }
        });

        add(mainPanel);
        zoneControl.configure(EntityType.SAMPLE_MESSAGE,
                message.getOid() == SampleMessage.DEFAULT_OID ? OperationType.CREATE : OperationType.UPDATE,
                message.getSecurityZone());
        enableButtons();
    }

    private void enableButtons() {
        boolean ok = nameField.getText().length() > 0;
        ok = ok && xmlContainer.getAccessibility().getText().length() > 0;
        if (allowOperationChange) ok = ok && operationNameField.getText().length() > 0;
        okButton.setEnabled(ok);
    }

    public void dispose() {
        xmlContainer.dispose();
        super.dispose();
    }

    public SampleMessage getMessage() {
        return message;
    }

    public boolean isOk() {
        return ok;
    }

    private Map<String, String> getPrefixesMap(Document doc) {
        Map<String, String> prefixesMap = new HashMap<String, String>();
        addPrefixesToMap(doc.getDocumentElement(), prefixesMap);

        return prefixesMap;
    }

    private void addPrefixesToMap(Element el, Map<String, String> prefixesMap) {
        NamedNodeMap attributes = el.getAttributes();
        for(int i = 0;i < attributes.getLength();i++) {
            Attr attribute = (Attr)attributes.item(i);
            if(attribute.getName().startsWith("xmlns:")) {
                prefixesMap.put(attribute.getName().substring(6), attribute.getValue());
            }
        }

        NodeList children = el.getChildNodes();
        for(int i = 0;i < children.getLength();i++) {
            if(children.item(i) instanceof Element) {
                Element child = (Element)children.item(i);
                addPrefixesToMap(child, prefixesMap);
            }
        }
    }
}
