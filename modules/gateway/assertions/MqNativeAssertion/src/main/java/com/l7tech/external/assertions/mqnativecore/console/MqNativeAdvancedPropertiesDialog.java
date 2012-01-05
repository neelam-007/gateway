package com.l7tech.external.assertions.mqnativecore.console;

import com.l7tech.external.assertions.mqnativecore.MqNativeConstants;
import com.l7tech.gui.util.RunOnChangeListener;

import javax.swing.*;
import java.awt.event.*;
import java.util.List;

public class MqNativeAdvancedPropertiesDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField valueTextField;
    private JComboBox nameComboBox;
    private boolean canceled;
    private MqNativeAdvancedProperty prop;
    final private List<MqNativeAdvancedProperty> currList;

    public MqNativeAdvancedPropertiesDialog(MqNativeAdvancedProperty property, List<MqNativeAdvancedProperty> currList) {
        this.prop = property;
        this.currList = currList;
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        RunOnChangeListener changeListener = new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                checkFieldsForText();
            }
        });
        ((JTextField)nameComboBox.getEditor().getEditorComponent()).getDocument().addDocumentListener(changeListener);
        valueTextField.getDocument().addDocumentListener(changeListener);

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

// call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

// call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        if(this.prop!=null) {
            nameComboBox.setModel(new DefaultComboBoxModel(new String[]{property.getName()}));
            nameComboBox.setSelectedIndex(0);
            nameComboBox.setEnabled(false);
            nameComboBox.setEditable(false);
            valueTextField.setText(prop.getValue());
            valueTextField.setCaretPosition(0);
        } else {
            valueTextField.setText("");
//            if (descriptors == null || descriptors.isEmpty()) {
//                nameComboBox.setModel(new DefaultComboBoxModel());
//                nameComboBox.setEnabled(true);
//                nameComboBox.setEditable(true);
//            } else
            {
                nameComboBox.setModel(new DefaultComboBoxModel(MqNativeConstants.MQ_PROPERTIES));
                nameComboBox.setEnabled(true);
                nameComboBox.setEditable(true);
                ItemListener itemListener = new ItemListener() {
                    public void itemStateChanged(ItemEvent e) {
                        String name =  (String) nameComboBox.getSelectedItem();
                        // Get and set description
                        String description = getDescription(name);
                        valueTextField.setText(description);
                        valueTextField.setCaretPosition(0);
                    }
                };
                nameComboBox.addItemListener(itemListener);
                nameComboBox.setSelectedIndex(0);
                itemListener.itemStateChanged(null); // init desc
            }
        }
//        if(this.prop!=null){
//            nameTextField.setText(prop.getName());
//            valueTextField.setText(prop.getValue());
//        }

        checkFieldsForText();
    }

    private String getDescription(String name) {
        for(MqNativeAdvancedProperty property: currList){
            if(property.getName().equals(name))
                return property.getValue();
        }
        return "";
    }

    private void checkFieldsForText(){
        if(String.valueOf(nameComboBox.getSelectedItem()).trim().length()>0 && valueTextField.getText().trim().length()>0){
            buttonOK.setEnabled(true);
        }
        else{
            buttonOK.setEnabled(false);
        }
    }

    private void onOK() {
        if(prop==null){
            prop = new MqNativeAdvancedProperty(String.valueOf(nameComboBox.getSelectedItem()), valueTextField.getText());
        } else {
            prop.setName(String.valueOf(nameComboBox.getSelectedItem()));
            prop.setValue(valueTextField.getText());
        }
        dispose();
    }

    private void onCancel() {
        canceled = true;
        dispose();
    }

    public boolean isCanceled(){
        return canceled;
    }

    public MqNativeAdvancedProperty getTheProperty(){
        return prop;
    }

}
