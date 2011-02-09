package com.l7tech.console.panels;

import com.l7tech.console.util.VariablePrefixUtil;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.MessageTargetable;
import com.l7tech.policy.assertion.TargetMessageType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;

public class AssertionMessageTargetSelector extends JDialog {
    private static final ResourceBundle bundle = ResourceBundle.getBundle(AssertionMessageTargetSelector.class.getName());

    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JRadioButton _requestRadioButton;
    private JRadioButton _responseRadioButton;
    private JRadioButton _otherContextVariableRadioButton;
    private JPanel _contextVarNamePanel;
    private JTextField _contextVarTextField;
    private TargetVariablePanel _contextVarTargetVariable;

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


        _contextVarTargetVariable = new TargetVariablePanel();
        _contextVarNamePanel.setLayout(new BorderLayout());
        if(messageTargetable.isTargetModifiedByGateway()){
            _contextVarNamePanel.add(_contextVarTargetVariable, BorderLayout.CENTER);
            _contextVarTargetVariable.addChangeListener(validator);
        }
        else{
            _contextVarTextField = new JTextField();
            _contextVarNamePanel.add(_contextVarTextField, BorderLayout.CENTER);
            _contextVarTextField.getDocument().addDocumentListener(validator);
        }


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
        setContextVar(messageTargetable.getOtherTargetMessageVariable());
        setContextVarEnabled(_otherContextVariableRadioButton.isSelected());
        if ( messageTargetable instanceof Assertion ) {
            Assertion ass = (Assertion) messageTargetable;
            if(messageTargetable.isTargetModifiedByGateway()) _contextVarTargetVariable.setAssertion(ass,null);//assertion already created
        }
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
            messageTargetable.setOtherTargetMessageVariable(VariablePrefixUtil.fixVariableName(getContextVar()));
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
            setContextVarEnabled(_otherContextVariableRadioButton.isSelected());
            if ( _otherContextVariableRadioButton.isSelected()){
                if(messageTargetable.isTargetModifiedByGateway()){
                    buttonOK.setEnabled( _contextVarTargetVariable.isEntryValid());
                }
                else{
                   boolean notValid = getContextVar()==null ||
                                   getContextVar().trim().length() == 0 ;
                    buttonOK.setEnabled( !notValid);
                }
            } else {
                buttonOK.setEnabled( true );
            }
        }
    }

    private void setContextVar(String text){
        if(messageTargetable.isTargetModifiedByGateway()){
           _contextVarTargetVariable.setVariable(text);
        }
        else{
            _contextVarTextField.setText(text);
        }
    }

    private String getContextVar(){
        if(messageTargetable.isTargetModifiedByGateway()){
           return _contextVarTargetVariable.getVariable();
        }
        else{
           return _contextVarTextField.getText();
        }
    }

    private void setContextVarEnabled(boolean enabled){
        if(messageTargetable.isTargetModifiedByGateway()){
           _contextVarTargetVariable.setEnabled(enabled);
        }
        else{
            _contextVarTextField.setEnabled(enabled);
        }
    }
}
