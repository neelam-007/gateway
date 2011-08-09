/**
 * Copyright (C) 2010 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.cluster.ClusterPropertyDescriptor;
import com.l7tech.gui.util.*;
import com.l7tech.objectmodel.FindException;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.EventListener;
import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * User: wlui
 *
 */

public class ByteLimitPanel extends JPanel {
    private JCheckBox setMaxCheckbox;
    private JRadioButton bytesRadioButton;
    private JTextField bytesTextBox;
    private JRadioButton unlimitedRadioButton;
    private JLabel warningLabel;
    private JPanel rootPanel;
    private InputValidator validator;
    private static final Logger logger = Logger.getLogger(ByteLimitPanel.class.getName());

    private static ResourceBundle resources = ResourceBundle.getBundle(ByteLimitPanel.class.getName());
    private final ImageIcon BLANK_ICON = new ImageIcon(ImageCache.getInstance().getIcon("com/l7tech/console/resources/Transparent16.png"));
    private final ImageIcon WARNING_ICON = new ImageIcon(ImageCache.getInstance().getIcon("com/l7tech/console/resources/Warning16.png"));
    private long DEFAULT_MAX_BYTES = 2621440;
    private boolean allowContextVars = false;
    private InputValidator.ValidationRule textBoxRule = null;


    public ByteLimitPanel() {
        initComponents();
    }

    protected void initComponents() {

        Utilities.attachDefaultContextMenu(bytesTextBox);
        Utilities.enableGrayOnDisabled(bytesTextBox);

        setMaxCheckbox.addActionListener(new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                enableDisableComponents();
                notifyListeners();
            }
        }));
        bytesRadioButton.addActionListener(new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                enableDisableComponents();
                notifyListeners();
            }
        }));
        unlimitedRadioButton.addActionListener(new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                enableDisableComponents();
                notifyListeners();
            }
        }));

        validator = new InputValidator(this,setMaxCheckbox.getText());


        textBoxRule = validator.buildTextFieldNumberRangeValidationRule(resources.getString("max.bytes"),bytesTextBox,1,Integer.MAX_VALUE, false);
        validator.addRule(textBoxRule);
        validator.constrainTextFieldToBeNonEmpty(resources.getString("max.bytes"),bytesTextBox,null);

        enableDisableComponents();

        bytesTextBox.getDocument().addDocumentListener(new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                notifyListeners();
            }
        }));

        this.setLayout(new BorderLayout());
        add(rootPanel, BorderLayout.CENTER);
    }


    public void setLabelText(String text){
        setMaxCheckbox.setText(text);
    }

    public void setAllowContextVars(boolean allowContextVars){
        if(this.allowContextVars == allowContextVars)
            return;
        this.allowContextVars = allowContextVars;
        validator.removeRule(textBoxRule);
        if(!allowContextVars){
            textBoxRule = validator.buildTextFieldNumberRangeValidationRule(resources.getString("max.bytes"),bytesTextBox,1,Integer.MAX_VALUE, false);
            validator.addRule(textBoxRule);
        }
    }
    /**
     *
     * @param strValue  Context variable or string of a Long
     * @param defaultValue default value of the property, used when value not selected
     */
    public void setValue(String strValue, long defaultValue){
        if(strValue== null || strValue.isEmpty()) {
            setMaxCheckbox.setSelected(false);
            bytesRadioButton.setSelected(true);
            bytesTextBox.setText(Long.toString(defaultValue));
        }else{
            setMaxCheckbox.setSelected(true);
            try{
                long value = Long.parseLong(strValue);
                if(value>0){
                    bytesRadioButton.setSelected(true);
                    bytesTextBox.setText(Long.toString(value));
                }
                else{
                    unlimitedRadioButton.setSelected(true);
                }
            }
            catch (NumberFormatException ex){
                bytesRadioButton.setSelected(true);
                bytesTextBox.setText(strValue);
            }
        }
        enableDisableComponents();
    }

    /**
     *
     * @return "0" for unlimited, null for not selected
     */
    public String getValue() throws NumberFormatException{
        if(setMaxCheckbox.isSelected()){
            if(bytesRadioButton.isSelected()){
                return bytesTextBox.getText();
            }
            else{
                return "0";
            }
        }else{
            return null;
        }
    }


    public boolean isSelected() {
        return setMaxCheckbox.isSelected();
    }

    public void setSelected(boolean selected){
        setMaxCheckbox.setSelected(selected);
        enableDisableComponents();
    }

    private void enableDisableComponents() {
        boolean isChecked = setMaxCheckbox.isSelected();
        bytesRadioButton.setEnabled(isChecked );
        bytesTextBox.setEnabled(isChecked && bytesRadioButton.isSelected());
        unlimitedRadioButton.setEnabled(isChecked );
        warningLabel.setIcon((isChecked && unlimitedRadioButton.isSelected()) ? WARNING_ICON : BLANK_ICON);
        warningLabel.setText((isChecked && unlimitedRadioButton.isSelected()) ? resources.getString("warning.label") : "");
    }

    public String validateFields() {
        if(setMaxCheckbox.isSelected() && bytesRadioButton.isSelected())
            return validator.validate();
        return null;
    }

    public void addChangeListener(ChangeListener l) {
        listenerList.add(ChangeListener.class, l);
    }  /**
     * Remove a listener to changes of the panel's validity.
     * <p/>
     * The default is a simple implementation that supports a single
     * listener.
     *
     * @param l the listener to remove
     */
    public void removeChangeListener(ChangeListener l) {
        listenerList.remove(ChangeListener.class, l);
    }

    /**
     * notify listeners of the state change
     */
    protected void notifyListeners() {
        ChangeEvent event = new ChangeEvent(this);
        EventListener[] listeners = listenerList.getListeners(ChangeListener.class);
        for (EventListener listener : listeners) {
            ((ChangeListener) listener).stateChanged(event);
        }
    }
}

