package com.l7tech.external.assertions.messagecontext.console;

import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.MaxLengthDocument;
import com.l7tech.gateway.common.mapping.MessageContextMapping;
import com.l7tech.util.ValidationUtils;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.util.logging.Logger;


/**
 * @Copyright: Layer 7 Tech. Inc.
 * @Author: ghuang
 * @Date: Aug 7, 2008
 */
public class ConfigureMessageContextMappingDialog extends JDialog {
    private static final Logger logger = Logger.getLogger(ConfigureMessageContextMappingDialog.class.getName());
    private static final String charsAllowedForKey = ValidationUtils.ALPHA_NUMERIC + ",.'!()_-;:\"?/\\";
    private JPanel mainPanel;
    private JButton okButton;
    private JButton cancelButton;
    private JComboBox typeComboBox;
    private JTextField keyTextField;
    private JTextField valueTextField;
    private JLabel valueStatusLabel;
    private JLabel keyStatusLabel;

    private MessageContextMapping mapping;
    private boolean wasOKed;

    public ConfigureMessageContextMappingDialog(Frame parent, MessageContextMapping mapping) {
        super(parent, "Configure Message Context Mapping", true);
        this.mapping = mapping;
        init();
    }

    public ConfigureMessageContextMappingDialog(Dialog parent, MessageContextMapping mapping) {
        super(parent, "Configure Message Context Mapping", true);
        this.mapping = mapping;
        init();
    }

    public boolean wasOKed() {
        return wasOKed;
    }

    public MessageContextMapping getMapping() {
        return mapping;
    }

    private void init() {
        typeComboBox.addItem("Select One of Mapping Types:");
        for (String item: MessageContextMapping.MAPPING_TYPES) {
            typeComboBox.addItem(item);
        }
        typeComboBox.addItemListener(new MappingItemListener());

        keyTextField.setDocument(new MaxLengthDocument(128));
        valueTextField.setDocument(new MaxLengthDocument(255));
        keyTextField.getDocument().addDocumentListener(new RunOnChangeListener(new Runnable() {
            public void run() {
                enableOrDisableOkButton();
            }
        }));
        valueTextField.getDocument().addDocumentListener(new RunOnChangeListener(new Runnable() {
            public void run() {
                enableOrDisableOkButton();
            }
        }));

        enableOrDisableOkButton();

        if (mapping == null) {
            // Case 1: Add a new mapping
            keyTextField.setEditable(false);
            valueTextField.setEditable(false);
            keyTextField.setText(null);
            valueTextField.setText(null);
            keyStatusLabel.setVisible(false);
        } else {
            // Case 2: Edit an existing mapping
            if (mapping.getMappingType().equals(MessageContextMapping.MAPPING_TYPES[MessageContextMapping.CUSTOM_MAPPING_TYPE_IDX])) {
                keyTextField.setEditable(true);
                valueTextField.setEditable(true);
            } else {
                keyTextField.setEditable(false);
                valueTextField.setEditable(false);
            }
            typeComboBox.setSelectedItem(mapping.getMappingType());
            keyTextField.setText(mapping.getKey());
            valueTextField.setText(mapping.getValue());
        }

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ok();
                dispose();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        add(mainPanel);
        pack();
        Utilities.centerOnParentWindow(this);
    }

    private class MappingItemListener implements ItemListener {
        public void itemStateChanged(ItemEvent e) {
            int idx = typeComboBox.getSelectedIndex();

            // Case 1: the user selects the first item - the greeting message from the combo box.
            if (idx == 0) {
                keyTextField.setEditable(false);
                valueTextField.setEditable(false);

                keyTextField.setText(null);
                valueTextField.setText(null);

                enableOrDisableOkButton();
                keyStatusLabel.setVisible(false);
                return;
            }

            // Case 2: the user choosed one of mapping types from the combo box.
            String mappingTypeCurrentlyChosen = (String)typeComboBox.getSelectedItem();
            String customMappingType = MessageContextMapping.MAPPING_TYPES[MessageContextMapping.CUSTOM_MAPPING_TYPE_IDX];

            if (mappingTypeCurrentlyChosen.equals(customMappingType)) {
                keyTextField.setEditable(true);
                valueTextField.setEditable(true);

                if (mapping == null || (! mapping.getMappingType().equals(customMappingType))) {
                    keyTextField.setText(null);
                    valueTextField.setText(null);
                } else {
                    keyTextField.setText(mapping.getKey());
                    valueTextField.setText(mapping.getValue());
                }
            } else {
                keyTextField.setEditable(false);
                valueTextField.setEditable(false);

                if (idx > MessageContextMapping.DEFAULT_KEYS.length) {
                    logger.warning("Invalid standard message context mapping type: " + mappingTypeCurrentlyChosen);
                    return;
                }
                keyTextField.setText(MessageContextMapping.DEFAULT_KEYS[idx-1]);
                valueTextField.setText(MessageContextMapping.DEFAULT_VALUE);
            }

            enableOrDisableOkButton();
        }
    }

    private void ok() {
        if (mapping == null) {
            mapping = new MessageContextMapping();
        }
        mapping.setMappingType((String)typeComboBox.getSelectedItem());
        mapping.setKey(keyTextField.getText());
        mapping.setValue(valueTextField.getText());
        wasOKed = true;
    }

    private void enableOrDisableOkButton() {
        boolean okButtonEnabled;
        boolean keyStatusLabelVisible;
        boolean valueStatusLabelVisible;
        String keyStr = keyTextField.getText();
        String valueStr = valueTextField.getText();
        String[] varables = Syntax.getReferencedNames(valueStr!=null?valueStr:"");

        if (keyStr == null || keyStr.trim().equals("")) {
            keyStatusLabel.setText("This key is empty.");
            keyStatusLabelVisible = true;
            okButtonEnabled = false;
        } else if (! ValidationUtils.isValidCharacters(keyStr, charsAllowedForKey)) {
            keyStatusLabel.setText("This key has invalid characters.");
            keyStatusLabelVisible = true;
            okButtonEnabled = false;
        } else {
            keyStatusLabelVisible = false;
            okButtonEnabled = true;
        }

        String warningMsg = null;
        for (String name: varables) {
            warningMsg = VariableMetadata.validateName(name);
            if (warningMsg != null) {
                valueStatusLabel.setText(warningMsg);
                break;
            }
        }
        valueStatusLabelVisible = (warningMsg != null);
        okButtonEnabled = okButtonEnabled && (warningMsg == null);

        keyStatusLabel.setVisible(keyStatusLabelVisible);
        valueStatusLabel.setVisible(valueStatusLabelVisible);
        okButton.setEnabled(okButtonEnabled);
    }
}
