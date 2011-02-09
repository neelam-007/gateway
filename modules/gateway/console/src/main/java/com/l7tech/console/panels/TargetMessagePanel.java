/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import com.l7tech.console.util.VariablePrefixUtil;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.MessageTargetable;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.variable.VariableMetadata;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/** @author alex */
public class TargetMessagePanel extends JPanel {
    private JPanel mainPanel;
    private JRadioButton requestRadioButton;
    private JRadioButton responseRadioButton;
    private JRadioButton otherRadioButton;
    private JPanel otherMessageVariablePanel;
    private JTextField otherMessageVariableTextField;
    private TargetVariablePanel otherMessageVariableTargetVariable;
    private boolean allowNonMessageVariables = false;
    private boolean modifyByGateway = false;
    private JPanel requestExtraPanel;
    private JPanel responseExtraPanel;
    private JPanel otherExtraPanel;

    private JComponent requestExtra;
    private JComponent responseExtra;
    private JComponent otherExtra;

    private final RunOnChangeListener listener = new RunOnChangeListener(new Runnable() {
        @Override
        public void run() {
            enableDisable();
            final boolean valid = isValidTarget();
            firePropertyChange("valid", null, valid);
            if (valid) {
                final ActionEvent event = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "updated", 0);
                for (ActionListener actionListener : listenerList.getListeners(ActionListener.class)) {
                    actionListener.actionPerformed(event);
                }
            }
        }
    });

    private void enableDisable() {
        setOtherMessageVariableEnabled(otherRadioButton.isSelected());
        if (requestExtra != null) requestExtra.setEnabled(requestRadioButton.isSelected());
        if (responseExtra != null) responseExtra.setEnabled(responseRadioButton.isSelected());
        if (otherExtra != null) otherExtra.setEnabled(otherRadioButton.isSelected());
    }

    public TargetMessagePanel() {
        this("Target Message");
    }

    public TargetMessagePanel(String title) {
        setTitle(title);
        initComponents();
    }

    public void setAllowNonMessageVariables(boolean allowNonMessageVariables) {
        if (allowNonMessageVariables != this.allowNonMessageVariables) {
            otherRadioButton.setText(allowNonMessageVariables
                    ? "Other Context Variable:"
                    : "Other Message Variable");
            this.allowNonMessageVariables = allowNonMessageVariables;
        }
    }

    public void setTitle(String title) {
        Border border = getBorder();
        if (border instanceof TitledBorder) {
            TitledBorder titledBorder = (TitledBorder)border;
            titledBorder.setTitle(title);
        }
    }

    public String getTitle() {
        Border border = getBorder();
        if (border instanceof TitledBorder) {
            TitledBorder titledBorder = (TitledBorder)border;
            return titledBorder.getTitle();
        }
        throw new IllegalStateException();
    }

    /**
     * Configure the GUI controls to reflect the settings of the specified model object.
     *
     * @param model the object whose values are to be read to configure the GUI controls.  Required.
     */
    public void setModel(MessageTargetable model , final Assertion prevAssertion) {
        setModel(model, prevAssertion,model.isTargetModifiedByGateway());
    }
    public void setModel(MessageTargetable model , final Assertion prevAssertion, boolean isMessageTargetableModifiedByGateway) {
    
        modifyByGateway = isMessageTargetableModifiedByGateway ;
        
        if(modifyByGateway){
            otherMessageVariablePanel.add(otherMessageVariableTargetVariable, BorderLayout.CENTER);
            otherMessageVariableTargetVariable.addChangeListener(listener);
            Assertion ass = (Assertion)model;
            otherMessageVariableTargetVariable.setAssertion( ass , prevAssertion );

        }
        else{
            otherMessageVariablePanel.add(otherMessageVariableTextField, BorderLayout.CENTER);
            otherMessageVariableTextField.getDocument().addDocumentListener(listener);
        }

        switch(model.getTarget()) {
            case REQUEST:
                requestRadioButton.setSelected(true);
                break;
            case RESPONSE:
                responseRadioButton.setSelected(true);
                break;
            case OTHER:
                otherRadioButton.setSelected(true);
                setOtherMessageVariable(model.getOtherTargetMessageVariable());
                break;
            default:
                throw new IllegalArgumentException();
        }
        enableDisable();        
    }

    /**
     * Check whether the view currently contains valid information.
     *
     * @return true if {@link #updateModel} would succeed if called now; false if it would throw InvalidModelException
     */
    public boolean isValidTarget() {
        return null == check();
    }

    /**
     * Check validity of view contents.
     *
     * @return null if OK, otherwise the error message.
     */
    public String check() {
        if (!otherRadioButton.isSelected())
            return null;
        if (modifyByGateway){
            return otherMessageVariableTargetVariable.getErrorMessage();
        }
        else{
            return VariableMetadata.validateName(otherMessageVariableTextField.getText());
        }

    }

    protected void initComponents() {
        // create components
        otherMessageVariableTextField = new JTextField();
        otherMessageVariableTargetVariable = new TargetVariablePanel();
        otherMessageVariablePanel.setLayout(new BorderLayout());          

        requestRadioButton.addActionListener(listener);
        responseRadioButton.addActionListener(listener);
        otherRadioButton.addActionListener(listener);
        otherMessageVariableTextField.getDocument().addDocumentListener(listener);
        Utilities.attachDefaultContextMenu(otherMessageVariableTextField);

        enableDisable();
        this.setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
    }

    @Override
    public void setBorder(Border border) {
        if (mainPanel != null) mainPanel.setBorder(border);
    }

    @Override
    public Border getBorder() {
        return mainPanel == null ? null : mainPanel.getBorder();
    }

    private String getVariableName() {
        return VariablePrefixUtil.fixVariableName(getOtherMessageVariable());
    }

    private void setExtra(JPanel panel, JComponent extra) {
        panel.removeAll();
        panel.setLayout(new BorderLayout());
        if (extra != null)
            panel.add(extra, BorderLayout.CENTER);
        enableDisable();
    }

    public JComponent getRequestExtra() {
        return requestExtra;
    }

    /**
     * Set an extra component that will appear next to the Request radio button.
     *
     * @param requestExtra An extra component to display to the right of the Request radio button, or null.
     */
    public void setRequestExtra(JComponent requestExtra) {
        this.requestExtra = requestExtra;
        setExtra(requestExtraPanel, requestExtra);
    }

    public JComponent getResponseExtra() {
        return responseExtra;
    }

    /**
     * Set an extra component that will appear next to the Response radio button.
     *
     * @param responseExtra An extra component to display to the right of the Response radio button, or null.
     */
    public void setResponseExtra(JComponent responseExtra) {
        this.responseExtra = responseExtra;
        setExtra(responseExtraPanel, responseExtra);
    }

    public JComponent getOtherExtra() {
        return otherExtra;
    }

    /**
     * Set an extra component that will appear next to the "Other message variable" radio button.
     *
     * @param otherExtra An extra component to display to the right of the "Other message variable" radio button, or null.
     */
    public void setOtherExtra(JComponent otherExtra) {
        this.otherExtra = otherExtra;
        setExtra(otherExtraPanel, otherExtra);
    }

    /**
     * Update the specified model object to correspond to the values the user has set
     * by manipulating the GUI controls.
     *
     * @param model the object to update.  Required.
     */
    public void updateModel(MessageTargetable model) {
        final TargetMessageType type;
        final String var;
        if (requestRadioButton.isSelected()) {
            type = TargetMessageType.REQUEST;
            var = null;
        } else if (responseRadioButton.isSelected()) {
            type = TargetMessageType.RESPONSE;
            var = null;
        } else if (otherRadioButton.isSelected()) {
            type = TargetMessageType.OTHER;
            var = getVariableName();
        } else {
            throw new IllegalStateException();
        }

        model.setTarget(type);
        model.setOtherTargetMessageVariable(var);
    }

    public void addDocumentListener( final DocumentListener documentListener ) {
        otherMessageVariableTextField.getDocument().addDocumentListener( documentListener );

        if ( documentListener instanceof ChangeListener){
            ChangeListener cl = (ChangeListener) documentListener;
            otherMessageVariableTargetVariable.addChangeListener(cl);
        }
        
        if ( documentListener instanceof ActionListener ) {
            ActionListener al = (ActionListener) documentListener;
            requestRadioButton.addActionListener(al);
            responseRadioButton.addActionListener(al);
            otherRadioButton.addActionListener(al);
        }
    }

    public void addActionListener(ActionListener l) {
        listenerList.add(ActionListener.class, l);
    }

    public void removeActionListener(ActionListener l) {
        listenerList.remove(ActionListener.class, l);
    }

    private void setOtherMessageVariable(String text){
        if(modifyByGateway){
           otherMessageVariableTargetVariable.setVariable(text);
        }
        else{
            otherMessageVariableTextField.setText(text);
        }
    }

    private String getOtherMessageVariable(){
        if(modifyByGateway){
           return otherMessageVariableTargetVariable.getVariable();
        }
        else{
           return otherMessageVariableTextField.getText();
        }
    }

    private void setOtherMessageVariableEnabled(boolean enabled){
        if(modifyByGateway){
           otherMessageVariableTargetVariable.setEnabled(enabled);
        }
        else{
            otherMessageVariableTextField.setEnabled(enabled);
        }
    }

}
