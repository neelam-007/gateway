package com.l7tech.external.assertions.api3scale.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.console.util.VariablePrefixUtil;
import com.l7tech.external.assertions.api3scale.Api3ScaleAuthorizeAssertion;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.util.ResourceBundle;

public class Api3ScaleAuthorizePropertiesDialog extends AssertionPropertiesOkCancelSupport<Api3ScaleAuthorizeAssertion> {

    public Api3ScaleAuthorizePropertiesDialog(final Window parent, final Api3ScaleAuthorizeAssertion assertion) {
        super(Api3ScaleAuthorizeAssertion.class, parent, assertion, true);
        initComponents();
    }

    @Override
        protected void initComponents() {
        super.initComponents();

        targetVariablePanel.addChangeListener(new ChangeListener(){
            @Override
            public void stateChanged(ChangeEvent e) {
            getOkButton().setEnabled(targetVariablePanel.isEntryValid());
            }
        });
        targetVariablePanel.setAcceptEmpty(true);

       }

    @Override
    public Api3ScaleAuthorizeAssertion getData(Api3ScaleAuthorizeAssertion assertion) throws ValidationException {

        assertion.setPrivateKey(privateKeyTextField.getText());
        assertion.setServer(serverTextField.getText());
        assertion.setOutputPrefix(VariablePrefixUtil.fixVariableName(targetVariablePanel.getVariable()));

        return assertion;
    }

    @Override
    public void setData(Api3ScaleAuthorizeAssertion assertion) {
        final String privateKey = assertion.getPrivateKey();
        if(privateKey != null && !privateKey.trim().isEmpty()){
            privateKeyTextField.setText(privateKey);
        }

        final String server = assertion.getServer();
        if(server != null && !server.trim().isEmpty()){
            serverTextField.setText(server);
        }

        final String prefix = assertion.getOutputPrefix();
        if (prefix != null) { //don't test for empty, it's ok
            targetVariablePanel.setVariable(prefix);
        }
        targetVariablePanel.setAssertion(assertion,getPreviousAssertion());

        String[] suffixes = Api3ScaleAuthorizeAssertion.VARIABLE_SUFFIXES.toArray( new String[Api3ScaleAuthorizeAssertion.VARIABLE_SUFFIXES.size()] );
        targetVariablePanel.setSuffixes(suffixes);
    }

    @Override
    protected JPanel createPropertyPanel() {
        return propertyPanel;
    }


    private String getPropertyValue(String propKey){
        String propertyName = resourceBundle.getString(propKey);
        if(propertyName.charAt(propertyName.length() - 1) == ':'){
            propertyName = propertyName.substring(0, propertyName.length() - 1);
        }
        return propertyName;
    }

    private JPanel propertyPanel;
    private JTextField privateKeyTextField;
    private TargetVariablePanel targetVariablePanel;
    private JTextField serverTextField;
    private ResourceBundle resourceBundle = ResourceBundle.getBundle(Api3ScaleAuthorizePropertiesDialog.class.getName());
}
