package com.l7tech.console.panels;

import com.l7tech.policy.assertion.MessageTargetable;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.console.util.VariablePrefixUtil;

import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.util.*;

public class AssertionMessageTargetSelector extends JDialog {
    private static final ResourceBundle bundle = ResourceBundle.getBundle(AssertionMessageTargetSelector.class.getName());

    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JRadioButton _requestRadioButton;
    private JRadioButton _responseRadioButton;
    private JRadioButton _otherContextVariableRadioButton;
    private JTextField _contextVarName;

    private MessageTargetable messageTargetable;
    private boolean readonly;
    private boolean wasOKed = false;

    public AssertionMessageTargetSelector( final Window owner,
                                           final MessageTargetable messageTargetable,
                                           final boolean readonly ) {
        super(owner, AssertionMessageTargetSelector.DEFAULT_MODALITY_TYPE);
        this.messageTargetable = messageTargetable;
        this.readonly = readonly;
        initialize();
    }

    private void initialize() {
        setContentPane(contentPane);
        setTitle(bundle.getString("dialog.title"));
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        RunOnChangeListener validator = new RunOnChangeListener( new Runnable() {
            @Override
            public void run() {
                doValidate();
            }
        });
        _requestRadioButton.addActionListener(validator);
        _responseRadioButton.addActionListener(validator);
        _otherContextVariableRadioButton.addActionListener(validator);
        _contextVarName.getDocument().addDocumentListener(validator);

        buttonOK.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });
        buttonOK.setEnabled(!readonly);

        buttonCancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        Utilities.setEscKeyStrokeDisposes(this);

        updateView();
        doValidate();
    }

    private void updateView() {
        TargetMessageType target = messageTargetable.getTarget();
        if (TargetMessageType.REQUEST == target) {
            _requestRadioButton.setSelected(true);
        } else if (TargetMessageType.RESPONSE == target) {
            _responseRadioButton.setSelected(true);
        } else {
            _otherContextVariableRadioButton.setSelected(true);
        }
        _contextVarName.setText(messageTargetable.getOtherTargetMessageVariable());
        _contextVarName.setEditable(_otherContextVariableRadioButton.isSelected());
    }

    private void onOK() {
        if (_requestRadioButton.isSelected()) {
            messageTargetable.setTarget(TargetMessageType.REQUEST);
            messageTargetable.setOtherTargetMessageVariable(null);
        } else if (_responseRadioButton.isSelected()) {
            messageTargetable.setTarget(TargetMessageType.RESPONSE);
            messageTargetable.setOtherTargetMessageVariable(null);
        } else {
            messageTargetable.setTarget(TargetMessageType.OTHER);
            messageTargetable.setOtherTargetMessageVariable(VariablePrefixUtil.fixVariableName(_contextVarName.getText()));
        }
        wasOKed = true;
        dispose();
    }

    public boolean hasAssertionChanged() {
        return wasOKed;
    }
    
    private void onCancel() {
        dispose();
    }

    private void doValidate() {
        if ( !readonly ) {
            _contextVarName.setEditable(_otherContextVariableRadioButton.isSelected());
            if ( _otherContextVariableRadioButton.isSelected() &&
                 ( _contextVarName.getText()==null ||
                  _contextVarName.getText().trim().length() == 0 ) ) {
                buttonOK.setEnabled( false );
            } else {
                buttonOK.setEnabled( true );
            }
        }
    }
}
