package com.l7tech.external.assertions.wsaddressing.console;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.util.RunOnChangeListener;
import com.l7tech.common.gui.util.TextComponentPauseListenerManager;
import com.l7tech.common.gui.util.PauseListener;
import com.l7tech.common.gui.widgets.TargetMessagePanel;
import com.l7tech.console.panels.AssertionPropertiesEditorSupport;
import com.l7tech.console.util.VariablePrefixUtil;
import com.l7tech.external.assertions.wsaddressing.WsAddressingAssertion;
import com.l7tech.policy.variable.PolicyVariableUtils;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

/**
 * Properties dialog for WS-Addressing assertion.
 */
public class WsAddressingPropertiesDialog extends AssertionPropertiesEditorSupport<WsAddressingAssertion> {
    private static final ResourceBundle resources;

    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JCheckBox requireSignatureCheckBox;
    private JCheckBox wsAddressing10CheckBox;
    private JCheckBox wsAddressing082004CheckBox;
    private JTextField variablePrefixTextField;
    private JCheckBox otherNamespaceCheckBox;
    private JTextField otherNamespaceTextField;
    private TargetMessagePanel targetMessagePanel;
    private JLabel varPrefixStatusLabel;
    private WsAddressingAssertion assertion;

    private boolean targetMessageOk;
    private boolean ok;
    private final RunOnChangeListener changeListener = new RunOnChangeListener(new Runnable() {
        public void run() {
            configureView();
        }
    });

    static {
        resources = ResourceBundle.getBundle("com/l7tech/external/assertions/wsaddressing/console/resources/WsAddressingPropertiesDialog");
    }

    /**
     * Create a new dialog with the given owner and data.
     *
     * @param owner The owner for the dialog
     * @param assertion The assertion data
     */
    public WsAddressingPropertiesDialog(final Frame owner,
                                        final WsAddressingAssertion assertion) {
        super(owner, resources.getString("title.text"), true);
        init();
        setData(assertion);
    }

    /**
     * Create a new dialog with the given owner and data.
     *
     * @param owner The owner for the dialog
     * @param assertion The assertion data
     */
    public WsAddressingPropertiesDialog(final Dialog owner,
                                        final WsAddressingAssertion assertion)  {
        super(owner, resources.getString("title.text"), true);
        init();
        setData(assertion);
    }

    public void setData(final WsAddressingAssertion assertion) {
        initData(assertion);
    }

    public WsAddressingAssertion getData(final WsAddressingAssertion assertion) {
        saveData(assertion);
        return assertion;
    }

    /**
     * Did the dialog exit with "Ok"
     *
     * @return True if exited with success
     */
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
        ok &= validateVariablePrefix();
        buttonOK.setEnabled(ok);
    }

    /**
     * Initialize UI
     */
    private void init() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(contentPane);
        getRootPane().setDefaultButton(buttonOK);

        targetMessagePanel.addPropertyChangeListener("valid", new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                targetMessageOk = Boolean.TRUE.equals(evt.getNewValue());
                configureView();
            }
        });

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

        wsAddressing10CheckBox.addActionListener(changeListener);
        wsAddressing082004CheckBox.addActionListener(changeListener);
        otherNamespaceCheckBox.addActionListener(changeListener);
        otherNamespaceCheckBox.addActionListener(new ActionListener() {
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
     * @see {@link com.l7tech.console.util.VariablePrefixUtil#validateVariablePrefix}
     */
    private boolean validateVariablePrefix() {
        return VariablePrefixUtil.validateVariablePrefix(
            variablePrefixTextField.getText(),
            PolicyVariableUtils.getVariablesSetByPredecessors(assertion).keySet(),
            assertion.getVariableSuffixes(),
            varPrefixStatusLabel);
    }

    /**
     * @see {@link com.l7tech.console.util.VariablePrefixUtil#clearVariablePrefixStatus}
     */
    private void clearVariablePrefixStatus() {
        VariablePrefixUtil.clearVariablePrefixStatus(varPrefixStatusLabel);
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
        targetMessagePanel.setModel(assertion);
        requireSignatureCheckBox.setSelected(assertion.isRequireSignature());
        wsAddressing10CheckBox.setSelected(assertion.isEnableWsAddressing10());
        wsAddressing082004CheckBox.setSelected(assertion.isEnableWsAddressing200408());
        final String other = assertion.getEnableOtherNamespace();
        if (other != null) {
            otherNamespaceCheckBox.setSelected(true);
            otherNamespaceTextField.setText(other);
        }
        clearVariablePrefixStatus();
        variablePrefixTextField.setText(assertion.getVariablePrefix());
        validateVariablePrefix();
        TextComponentPauseListenerManager.registerPauseListener(
            variablePrefixTextField,
            new PauseListener() {
                public void textEntryPaused(JTextComponent component, long msecs) {
                    configureView();
                }

                public void textEntryResumed(JTextComponent component) {
                    clearVariablePrefixStatus();
                }
            },
            500
        );
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

        if ( variablePrefixTextField.getText().trim().length() > 0 ) {
            assertion.setVariablePrefix(variablePrefixTextField.getText().trim());
        } else {
            assertion.setVariablePrefix(null);
        }

        targetMessagePanel.updateModel(assertion);
    }
}
