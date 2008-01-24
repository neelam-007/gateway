package com.l7tech.external.assertions.wsaddressing.console;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.Frame;
import java.awt.Dialog;
import java.util.ResourceBundle;
import javax.swing.*;

import com.l7tech.external.assertions.wsaddressing.WsAddressingAssertion;
import com.l7tech.console.panels.AssertionPropertiesEditorSupport;
import com.l7tech.common.gui.util.Utilities;

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

    private boolean ok;

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
        buttonOK.setEnabled( !isReadOnly() );
    }

    /**
     * Initialize UI
     */
    private void init() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(contentPane);
        getRootPane().setDefaultButton(buttonOK);
        
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
        requireSignatureCheckBox.setSelected(assertion.isRequireSignature());
        wsAddressing10CheckBox.setSelected(assertion.isEnableWsAddressing10());
        wsAddressing082004CheckBox.setSelected(assertion.isEnableWsAddressing200408());
        if ( assertion.getVariablePrefix() != null ) {
            variablePrefixTextField.setText(assertion.getVariablePrefix());
        }
    }

    /**
     * Update data from form
     */
    private void saveData(final WsAddressingAssertion assertion) {
        assertion.setRequireSignature(requireSignatureCheckBox.isSelected());
        assertion.setEnableWsAddressing10(wsAddressing10CheckBox.isSelected());
        assertion.setEnableWsAddressing200408(wsAddressing082004CheckBox.isSelected());
        if ( variablePrefixTextField.getText().trim().length() > 0 ) {
            assertion.setVariablePrefix(variablePrefixTextField.getText().trim());
        } else {
            assertion.setVariablePrefix(null);            
        }
    }
}
