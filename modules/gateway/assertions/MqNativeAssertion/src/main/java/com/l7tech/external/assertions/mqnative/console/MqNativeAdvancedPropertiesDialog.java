package com.l7tech.external.assertions.mqnative.console;

import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.util.MutablePair;
import com.l7tech.util.Pair;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Map;

import static com.l7tech.gui.util.Utilities.comboBoxModel;

public class MqNativeAdvancedPropertiesDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField valueTextField;
    private JComboBox nameComboBox;
    private boolean canceled;
    private MutablePair<String, String> targetProp;

    public MqNativeAdvancedPropertiesDialog(final Window parent,
                                            final MutablePair<String, String> targetProp,
                                            final java.util.List<String> selectableProperties,
                                            final Map<String, String> properties) {
        super( parent, DEFAULT_MODALITY_TYPE );
        this.targetProp = targetProp;
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
                onOk();
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

        if(this.targetProp !=null) {
            nameComboBox.setModel(new DefaultComboBoxModel(new String[]{targetProp.left}));
            nameComboBox.setSelectedIndex(0);
            nameComboBox.setEnabled(false);
            nameComboBox.setEditable(false);
            valueTextField.setText(this.targetProp.getValue());
            valueTextField.setCaretPosition(0);
        } else {
            valueTextField.setText("");
            nameComboBox.setModel(comboBoxModel(selectableProperties));
            nameComboBox.setEnabled(true);
            nameComboBox.setEditable(true);
            ItemListener itemListener = new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    String name =  (String) nameComboBox.getSelectedItem();
                    // Get and set property value
                    valueTextField.setText(properties.get(name));
                    valueTextField.setCaretPosition(0);
                }
            };
            nameComboBox.addItemListener(itemListener);
            nameComboBox.setSelectedIndex(0);
            itemListener.itemStateChanged(null); // init desc
        }

        checkFieldsForText();
        Utilities.setMinimumSize( this );
    }

    private void checkFieldsForText(){
        if(String.valueOf(nameComboBox.getSelectedItem()).trim().length()>0){
            buttonOK.setEnabled(true);
        }
        else{
            buttonOK.setEnabled(false);
        }
    }

    private void onOk() {
        if(targetProp == null){
            targetProp = new MutablePair<String, String>("", "");
        }

        targetProp.left = String.valueOf(nameComboBox.getSelectedItem());
        targetProp.right = valueTextField.getText();

        dispose();
    }

    private void onCancel() {
        canceled = true;
        dispose();
    }

    public boolean isCanceled(){
        return canceled;
    }

    public Pair<String, String> getTheProperty(){
        return targetProp.asPair();
    }
}