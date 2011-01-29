package com.l7tech.external.assertions.wsaddressing.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.console.util.TopComponents;
import com.l7tech.external.assertions.wsaddressing.AddWsAddressingAssertion;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.util.SoapConstants;
import com.l7tech.util.ValidationUtils;

import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ResourceBundle;

/**
 *  Properties dialog for Add WS-Addressing assertion.
 */
public class AddWsAddressingPropertiesDialog extends AssertionPropertiesOkCancelSupport<AddWsAddressingAssertion> {

    //- PUBLIC
    
    /**
     * Create a new dialog with the given owner and data.
     *
     * @param owner     The owner for the dialog
     * @param assertion The assertion data
     */
    public AddWsAddressingPropertiesDialog(final Window owner,
                                           final AddWsAddressingAssertion assertion) {
        super(AddWsAddressingAssertion.class, owner, assertion, true);
        initComponents();
        setData(assertion);
    }

    @Override
    public void setData(AddWsAddressingAssertion assertion) {
        final String action = assertion.getAction();
        if(AddWsAddressingAssertion.WSA_ACTIONS.contains(action)){
            actionComboBox.setSelectedItem(action);
        } else {
            actionComboBox.getEditor().setItem(action);
        }

        final String msgId = assertion.getMessageId();
        if(AddWsAddressingAssertion.MESSAGE_ID_AUTOMATIC.equals(msgId)){
            messageIdComboBox.setSelectedItem(msgId);
        } else {
            messageIdComboBox.getEditor().setItem(msgId);
        }

        final String destination = assertion.getDestination();
        if(destination != null){
            toComboBox.getEditor().setItem(destination);
        }

        final String from = assertion.getSourceEndpoint();
        if(from != null){
            fromComboBox.getEditor().setItem(from);
        }

        final String reply = assertion.getReplyEndpoint();
        if(reply != null){
            replyToAddress.getEditor().setItem(reply);
        }

        final String fault = assertion.getFaultEndpoint();
        if(fault != null){
            faultToComboBox.getEditor().setItem(fault);
        }

        final String relatesMsgId = assertion.getRelatesToMessageId();
        if(relatesMsgId != null){
            relatesToMessageIdTextField.setText(relatesMsgId);
        }

        final String namespace = assertion.getWsaNamespaceUri();
        if(namespace != null){
            namespaceComboBox.getEditor().setItem(namespace);
        } else {
            namespaceComboBox.setSelectedItem(AddWsAddressingAssertion.DEFAULT_NAMESPACE);
        }

        targetVariablePanel.setVariable(assertion.getVariablePrefix());
        targetVariablePanel.setAssertion(assertion);
    }

    @Override
    public AddWsAddressingAssertion getData(AddWsAddressingAssertion assertion) {
        //validate
        validateAssertionConfig();

        final String action = actionComboBox.getEditor().getItem().toString().trim();

        assertion.setAction((action.isEmpty())? null : action);

        final String msgId = messageIdComboBox.getEditor().getItem().toString().trim();
        assertion.setMessageId((msgId.isEmpty())? null : msgId);

        final String dest = toComboBox.getEditor().getItem().toString().trim();
        assertion.setDestination((dest.isEmpty())? null : dest);

        final String from = fromComboBox.getEditor().getItem().toString().trim();
        assertion.setSourceEndpoint((from.isEmpty())? null : from);

        final String replyTo = replyToAddress.getEditor().getItem().toString().trim();
        assertion.setReplyEndpoint((replyTo.isEmpty())? null : replyTo);

        final String fault = faultToComboBox.getEditor().getItem().toString().trim();
        assertion.setFaultEndpoint((fault.isEmpty())? null : fault);

        final String relatesTo = relatesToMessageIdTextField.getText().trim();
        assertion.setRelatesToMessageId((relatesTo.isEmpty())? null : relatesTo);

        final String wsaNamespaceUri = namespaceComboBox.getEditor().getItem().toString().trim();
        assertion.setWsaNamespaceUri((wsaNamespaceUri.isEmpty())? null : wsaNamespaceUri);

        final String variablePrefix = targetVariablePanel.getVariable();
        assertion.setVariablePrefix((variablePrefix.isEmpty())? AddWsAddressingAssertion.VARIABLE_PREFIX : variablePrefix);

        return assertion;
    }

    //- PROTECTED
    
    @Override
    protected JPanel createPropertyPanel() {
        return contentPane;
    }

    @Override
    protected void initComponents() {
        super.initComponents();

        if(TopComponents.getInstance().isApplet()){
            contentPane.setPreferredSize(new Dimension(510, 470));
        }

        actionComboBox.setModel(new DefaultComboBoxModel(
                AddWsAddressingAssertion.WSA_ACTIONS.toArray(new Object[AddWsAddressingAssertion.WSA_ACTIONS.size()])));

        actionComboBox.setSelectedIndex(-1);
        messageIdComboBox.setModel(new DefaultComboBoxModel(new Object[]{
                AddWsAddressingAssertion.MESSAGE_ID_AUTOMATIC}));
        messageIdComboBox.setSelectedIndex(-1);

        toComboBox.setModel(new DefaultComboBoxModel(new Object[]{SoapConstants.WSA_ANONYMOUS_ADDRESS}));
        toComboBox.setSelectedIndex(-1);

        final Object[] addressItems = {SoapConstants.WSA_ANONYMOUS_ADDRESS, SoapConstants.WSA_NO_ADDRESS};        
        fromComboBox.setModel(new DefaultComboBoxModel(addressItems));
        fromComboBox.setSelectedIndex(-1);
        replyToAddress.setModel(new DefaultComboBoxModel(addressItems));
        replyToAddress.setSelectedIndex(-1);
        faultToComboBox.setModel(new DefaultComboBoxModel(addressItems));
        faultToComboBox.setSelectedIndex(-1);

        namespaceComboBox.setModel(new DefaultComboBoxModel(AddWsAddressingAssertion.WSA_NAMESPACES.toArray(
                                new Object[AddWsAddressingAssertion.WSA_NAMESPACES.size()])));
        namespaceComboBox.setSelectedIndex(-1);

        targetVariablePanel = new TargetVariablePanel();
        targetVariablePanel.setSuffixes(AddWsAddressingAssertion.VARIABLE_SUFFIXES.toArray(
                new String[AddWsAddressingAssertion.VARIABLE_SUFFIXES.size()]));

        targetVariablePanelHolder.setLayout(new BorderLayout());
        targetVariablePanelHolder.add(targetVariablePanel, BorderLayout.CENTER);
    }

    // - PRIVATE
    
    private JPanel contentPane;
    private JComboBox namespaceComboBox;
    private JComboBox actionComboBox;
    private JComboBox toComboBox;
    private JComboBox fromComboBox;
    private JComboBox replyToAddress;
    private JComboBox faultToComboBox;
    private JTextField relatesToMessageIdTextField;
    private JComboBox messageIdComboBox;
    private JPanel targetVariablePanelHolder;
    private TargetVariablePanel targetVariablePanel;
    private static final ResourceBundle resources = ResourceBundle.getBundle( AddWsAddressingPropertiesDialog.class.getName() );

    private void validateAssertionConfig() throws ValidationException{
        final String action = actionComboBox.getEditor().getItem().toString().trim();
        if(action == null || action.isEmpty()) {
            throw new ValidationException(resources.getString("actionLabel") + " is required.");
        }

        //validate all URIs for those which don't reference context variables.

        if(!AddWsAddressingAssertion.WSA_ACTIONS.contains(action)) {
            validateUriIfNoVariableReferenced(resources.getString("actionLabel"), action, false);
        }

        final String wsaNamespaceUri = namespaceComboBox.getEditor().getItem().toString().trim();
        if(wsaNamespaceUri == null || wsaNamespaceUri.isEmpty()){
            throw new ValidationException(resources.getString("WsAddressingNamespace") + " is required.");
        }

        if(!AddWsAddressingAssertion.WSA_NAMESPACES.contains(wsaNamespaceUri)){
            validateUriIfNoVariableReferenced(resources.getString("WsAddressingNamespace"), wsaNamespaceUri, true);            
        }

        final String msgId = messageIdComboBox.getEditor().getItem().toString().trim();
        if(!AddWsAddressingAssertion.MESSAGE_ID_AUTOMATIC.equals(msgId)){
            validateUriIfNoVariableReferenced(resources.getString("messageIdLabel"), msgId, false);
        }

        final String dest = toComboBox.getEditor().getItem().toString().trim();
        validateUriIfNoVariableReferenced(resources.getString("toLabel"), dest, false);

        final String from = fromComboBox.getEditor().getItem().toString().trim();
        validateUriIfNoVariableReferenced(resources.getString("fromLabel"), from, false);

        final String replyTo = replyToAddress.getEditor().getItem().toString().trim();
        validateUriIfNoVariableReferenced(resources.getString("replyToAddress"), replyTo, false);

        final String fault = faultToComboBox.getEditor().getItem().toString().trim();
        validateUriIfNoVariableReferenced(resources.getString("faultToAddress"), fault, false);

        final String relatesTo = relatesToMessageIdTextField.getText().trim();
        validateUriIfNoVariableReferenced(resources.getString("relatesToPanel") + " " +
                resources.getString("relatesToMessageId"), relatesTo, false);

        final String prefix = targetVariablePanel.getVariable();
        if(prefix.isEmpty()){
            throw new ValidationException("A variable prefix is required.");
        }
    }

    private void validateUriIfNoVariableReferenced(String propertyName, String propertyValue, boolean validateIsAbsolute)
            throws ValidationException{
        if(propertyValue == null || propertyValue.isEmpty()) return;

        if (Syntax.getReferencedNamesIndexedVarsNotOmitted(propertyValue).length <= 0) {
            if (!ValidationUtils.isValidUri(propertyValue)) {
                throw new ValidationException(propertyName + " is not a valid URI.");
            }

            if(validateIsAbsolute){
                try {
                    if(!new URI(propertyValue).isAbsolute()){
                        throw new ValidationException(propertyName + " is not an absolute URI.");
                    }
                } catch (URISyntaxException e) {
                    //can't happen
                }
            }
        }
    }
}
