package com.l7tech.external.assertions.wsaddressing.console;

import com.l7tech.console.panels.AssertionPropertiesEditorSupport;
import com.l7tech.console.panels.TargetMessagePanel;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.external.assertions.wsaddressing.WsAddressingAssertion;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * Properties dialog for WS-Addressing assertion.
 */
public class WsAddressingPropertiesDialog extends AssertionPropertiesEditorSupport<WsAddressingAssertion> {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JCheckBox requireSignatureCheckBox;
    private JCheckBox wsAddressing10CheckBox;
    private JCheckBox wsAddressing082004CheckBox;
    private JPanel variablePrefixTextFieldPanel;
    private TargetVariablePanel variablePrefixTextField;
    private JCheckBox otherNamespaceCheckBox;
    private JTextField otherNamespaceTextField;
    private JPanel targetMessagePanelHolder;
    private TargetMessagePanel targetMessagePanel = new TargetMessagePanel();
    private WsAddressingAssertion assertion;

    private boolean targetMessageOk;
    private boolean ok;
    private final RunOnChangeListener changeListener = new RunOnChangeListener(new Runnable() {
        @Override
        public void run() {
            configureView();
        }
    });

    /**
     * Create a new dialog with the given owner and data.
     *
     * @param owner The owner for the dialog
     * @param assertion The assertion data
     */
    public WsAddressingPropertiesDialog(final Window owner,
                                        final WsAddressingAssertion assertion)  {
        super(owner, assertion);
        init();
        setData(assertion);
    }

    @Override
    public void setData(final WsAddressingAssertion assertion) {
        initData(assertion);
    }

    @Override
    public WsAddressingAssertion getData(final WsAddressingAssertion assertion) {
        saveData(assertion);
        return assertion;
    }

    /**
     * Did the dialog exit with "Ok"
     *
     * @return True if exited with success
     */
    @Override
    public boolean isConfirmed() {
        return ok;
    }

    @Override
    protected void configureView() {
        otherNamespaceTextField.setEnabled(otherNamespaceCheckBox.isSelected());
        final String other = otherNamespaceTextField.getText().trim();
        boolean ok;
        if (otherNamespaceCheckBox.isSelected()) {
            ok = (other != null && other.length() > 0);
        } else {
            ok = wsAddressing10CheckBox.isSelected() || wsAddressing082004CheckBox.isSelected();
        }
        ok &= targetMessageOk;
        ok &= !isReadOnly();
        ok &= variablePrefixTextField.isEntryValid();
        buttonOK.setEnabled(ok);
    }

    /**
     * Initialize UI
     */
    private void init() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(contentPane);
        getRootPane().setDefaultButton(buttonOK);

        targetMessagePanelHolder.add( targetMessagePanel );
        targetMessagePanel.addPropertyChangeListener("valid", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                targetMessageOk = Boolean.TRUE.equals(evt.getNewValue());
                configureView();
            }
        });

        variablePrefixTextField = new TargetVariablePanel();
        variablePrefixTextFieldPanel.setLayout(new BorderLayout());
        variablePrefixTextFieldPanel.add(variablePrefixTextField, BorderLayout.CENTER);
        variablePrefixTextField.addChangeListener(new ChangeListener(){
            @Override
            public void stateChanged(ChangeEvent e) {
               configureView();
            }
        });

        buttonOK.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        wsAddressing10CheckBox.addActionListener(changeListener);
        wsAddressing082004CheckBox.addActionListener(changeListener);
        otherNamespaceCheckBox.addActionListener(changeListener);
        otherNamespaceCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (otherNamespaceCheckBox.isSelected()) {
                    otherNamespaceTextField.requestFocusInWindow();
                }
            }
        });
        otherNamespaceTextField.getDocument().addDocumentListener(changeListener);

        Utilities.setEscKeyStrokeDisposes(this);
    }

    /**
     * Handle OK
     */
    private void onOK() {
        dispose();
        ok = true;
    }

    /**
     * Handle Cancel
     */
    private void onCancel() {
        dispose();
    }

    /**
     * Initialize form from data
     */
    private void initData(final WsAddressingAssertion assertion) {
        this.assertion = assertion;
        targetMessageOk = true; // Since three radio buttons are grounded, the Request radio button is selected by default.
        targetMessagePanel.setModel(assertion, getPreviousAssertion());
        requireSignatureCheckBox.setSelected(assertion.isRequireSignature());
        wsAddressing10CheckBox.setSelected(assertion.isEnableWsAddressing10());
        wsAddressing082004CheckBox.setSelected(assertion.isEnableWsAddressing200408());
        final String other = assertion.getEnableOtherNamespace();
        if (other != null) {
            otherNamespaceCheckBox.setSelected(true);
            otherNamespaceTextField.setText(other);
        }
        variablePrefixTextField.setAcceptEmpty(true);
        variablePrefixTextField.setVariable(assertion.getVariablePrefix()==null?"":assertion.getVariablePrefix());
        variablePrefixTextField.setAssertion(assertion,getPreviousAssertion());
        variablePrefixTextField.setSuffixes(assertion.getVariableSuffixes());
    }

    /**
     * Update data from form
     */
    private void saveData(final WsAddressingAssertion assertion) {
        assertion.setRequireSignature(requireSignatureCheckBox.isSelected());
        assertion.setEnableWsAddressing10(wsAddressing10CheckBox.isSelected());
        assertion.setEnableWsAddressing200408(wsAddressing082004CheckBox.isSelected());

        final String other = otherNamespaceCheckBox.isSelected() ? otherNamespaceTextField.getText() : null;
        if (other != null && other.length() > 0) {
            assertion.setEnableOtherNamespace(other);
        } else {
            assertion.setEnableOtherNamespace(null);
        }

        if ( variablePrefixTextField.getVariable().trim().length() > 0 ) {
            assertion.setVariablePrefix(variablePrefixTextField.getVariable());
        } else {
            assertion.setVariablePrefix(null);
        }

        targetMessagePanel.updateModel(assertion);
    }

}
