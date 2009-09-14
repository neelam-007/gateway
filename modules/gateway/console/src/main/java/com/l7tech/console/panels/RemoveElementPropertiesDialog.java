package com.l7tech.console.panels;

import com.l7tech.policy.assertion.xml.RemoveElement;
import com.l7tech.gui.MaxLengthDocument;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.console.util.VariablePrefixUtil;

import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.util.*;

/**
 * Assertion property dialog for RemoveElement assertion.
 */
public class RemoveElementPropertiesDialog extends AssertionPropertiesEditorSupport<RemoveElement> {

    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField _nodeSetVar;
    private RemoveElement removeAssertion;
    private boolean confirmed = false;

    public RemoveElementPropertiesDialog( final Window owner, final RemoveElement assertion ) {
        super(owner, assertion);
        setData(assertion);
        initialize();
    }

    private void initialize() {
        setContentPane(contentPane);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getRootPane().setDefaultButton(buttonOK);

        _nodeSetVar.setDocument(new MaxLengthDocument(128));
        _nodeSetVar.getDocument().addDocumentListener(new RunOnChangeListener(new Runnable(){
            @Override
            public void run() {
                updateEnabledState();
            }
        }));

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

        Utilities.setEscKeyStrokeDisposes(this);
        updateView();
        updateEnabledState();
    }

    private void updateView() {
        _nodeSetVar.setText(removeAssertion.getElementFromVariable());
    }

    @Override
    public boolean isConfirmed() {
        return confirmed;
    }

    @Override
    public void setData( final RemoveElement assertion ) {
        removeAssertion = assertion;
        updateView();
    }

    @Override
    public RemoveElement getData( final RemoveElement assertion ) {
        return removeAssertion;
    }

    private void onOK() {
        confirmed = true;
        removeAssertion.setElementFromVariable(VariablePrefixUtil.fixVariableName(_nodeSetVar.getText()));
        dispose();
    }

    private void onCancel() {
        confirmed = false;
        dispose();
    }

    private void updateEnabledState() {
        String variableName = _nodeSetVar.getText();
        if ( variableName != null ) {
            variableName = variableName.trim();
        } else {
            variableName = "";
        }
        buttonOK.setEnabled( !isReadOnly() && variableName.length() > 0 );
    }
}
