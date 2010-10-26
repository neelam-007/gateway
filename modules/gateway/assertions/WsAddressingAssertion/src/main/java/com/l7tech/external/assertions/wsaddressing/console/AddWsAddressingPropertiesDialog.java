package com.l7tech.external.assertions.wsaddressing.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.util.TopComponents;
import com.l7tech.external.assertions.wsaddressing.AddWsAddressingAssertion;
import com.l7tech.util.SoapConstants;

import javax.swing.*;
import java.awt.*;
import java.util.UUID;

public class AddWsAddressingPropertiesDialog extends AssertionPropertiesOkCancelSupport<AddWsAddressingAssertion> {

    /**
     * Create a new dialog with the given owner and data.
     *
     * @param owner The owner for the dialog
     * @param assertion The assertion data
     */
    public AddWsAddressingPropertiesDialog(final Window owner,
                                        final AddWsAddressingAssertion assertion)  {
        super(AddWsAddressingAssertion.class, owner, assertion, true);
        initComponents();
        setData(assertion);
    }

    @Override
    public void setData(AddWsAddressingAssertion assertion) {

        final String action = assertion.getAction();
        if(!AddWsAddressingAssertion.ACTION_AUTOMATIC.equals(action)){
            actionComboBox.getEditor().setItem(action);
        } else {
            actionComboBox.setSelectedItem(AddWsAddressingAssertion.ACTION_AUTOMATIC);
        }

        final String msgId = assertion.getMessageId();
        if(msgId == null){
            messageIdTextField.setText(UUID.randomUUID().toString());
        } else {
            messageIdTextField.setText(msgId);
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
        }

        signMessageAddressingPropertiesCheckBox.setSelected(assertion.isSignMessageProperties());
    }

    @Override
    public AddWsAddressingAssertion getData(AddWsAddressingAssertion assertion) {
        final Object actionObj = actionComboBox.getEditor().getItem();

        if(actionObj == null){
            throw new ValidationException("No action selected");
        }

        assertion.setAction(actionObj.toString().trim());
        assertion.setMessageId(messageIdTextField.getText().trim());
        assertion.setDestination(toComboBox.getEditor().getItem().toString().trim());
        assertion.setSourceEndpoint(fromComboBox.getEditor().getItem().toString().trim());
        assertion.setReplyEndpoint(replyToAddress.getEditor().getItem().toString().trim());
        assertion.setFaultEndpoint(faultToComboBox.getEditor().getItem().toString().trim());
        assertion.setRelatesToMessageId(relatesToMessageIdTextField.getText().trim());
        final String wsaNamespaceUri = namespaceComboBox.getEditor().getItem().toString().trim();
        if(wsaNamespaceUri.isEmpty()){
            throw new ValidationException("WS-Addressing Namespace is required.");
        }
        assertion.setWsaNamespaceUri(wsaNamespaceUri);
        assertion.setSignMessageProperties(signMessageAddressingPropertiesCheckBox.isSelected());
        
        return assertion;
    }

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

        actionComboBox.setModel(new DefaultComboBoxModel(new Object[]{AddWsAddressingAssertion.ACTION_AUTOMATIC}));
        actionComboBox.setSelectedIndex(-1);
        toComboBox.setModel(new DefaultComboBoxModel(new Object[]{SoapConstants.WSA_ANONYMOUS_ADDRESS}));
        toComboBox.setSelectedIndex(-1);

        final Object[] addressItems = {SoapConstants.WSA_ANONYMOUS_ADDRESS, SoapConstants.WSA_NO_ADDRESS};        
        fromComboBox.setModel(new DefaultComboBoxModel(addressItems));
        fromComboBox.setSelectedIndex(-1);
        replyToAddress.setModel(new DefaultComboBoxModel(addressItems));
        replyToAddress.setSelectedIndex(-1);
        faultToComboBox.setModel(new DefaultComboBoxModel(addressItems));
        faultToComboBox.setSelectedIndex(-1);

        namespaceComboBox.setModel(new DefaultComboBoxModel(
                new Object[]{SoapConstants.WSA_NAMESPACE, SoapConstants.WSA_NAMESPACE2, SoapConstants.WSA_NAMESPACE_10}));
        namespaceComboBox.setSelectedIndex(-1);
    }

    // - PRIVATE
    private JPanel contentPane;
    private JComboBox namespaceComboBox;
    private JCheckBox signMessageAddressingPropertiesCheckBox;
    private JComboBox actionComboBox;
    private JTextField messageIdTextField;
    private JComboBox toComboBox;
    private JComboBox fromComboBox;
    private JComboBox replyToAddress;
    private JComboBox faultToComboBox;
    private JTextField relatesToMessageIdTextField;

}
