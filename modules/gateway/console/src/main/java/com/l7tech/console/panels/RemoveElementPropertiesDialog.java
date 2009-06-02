package com.l7tech.console.panels;

import com.l7tech.policy.assertion.xml.RemoveElement;
import com.l7tech.gui.MaxLengthDocument;
import com.l7tech.gui.util.Utilities;

import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.util.*;

/**
 * Assertion property dialog for RemoveElement assertion.
 */
public class RemoveElementPropertiesDialog extends AssertionPropertiesEditorSupport<RemoveElement> {

    private static final ResourceBundle bundle = ResourceBundle.getBundle( RemoveElementPropertiesDialog.class.getName() );

    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField _nodeSetVar;
    private RemoveElement removeAssertion;
    private boolean confirmed = false;

    public RemoveElementPropertiesDialog( final Window owner, final RemoveElement assertion ) {
        super(owner, bundle.getString("dialog.title"));
        setData(assertion);
        initialize();
    }

    private void initialize() {
        setContentPane(contentPane);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getRootPane().setDefaultButton(buttonOK);

        _nodeSetVar.setDocument(new MaxLengthDocument(128));

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
    }

    private void updateView() {
        _nodeSetVar.setText(removeAssertion.getElementFromVariable());
    }

    @Override
    public boolean isConfirmed() {
        return confirmed;
    }

    @Override
    public void setData(RemoveElement assertion) {
        removeAssertion = assertion;
        updateView();
    }

    @Override
    public RemoveElement getData(RemoveElement assertion) {
        return removeAssertion;
    }

    private void onOK() {
        confirmed = true;
        removeAssertion.setElementFromVariable(_nodeSetVar.getText());
        dispose();
    }

    private void onCancel() {
        confirmed = false;
        dispose();
    }
}
