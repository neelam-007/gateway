package com.l7tech.external.assertions.mtom.console;

import com.l7tech.external.assertions.mtom.MtomDecodeAssertion;
import com.l7tech.policy.assertion.MessageTargetable;
import com.l7tech.policy.assertion.MessageTargetableSupport;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.console.util.VariablePrefixUtil;

import javax.swing.*;
import java.util.ResourceBundle;
import java.awt.*;

/**
 *
 */
public class MtomDecodeAssertionPropertiesDialog extends MtomAssertionPropertiesDialogSupport<MtomDecodeAssertion> {

    //- PUBLIC

    public MtomDecodeAssertionPropertiesDialog( final Window parent,
                                                final MtomDecodeAssertion assertion ) {
        super( parent, MtomDecodeAssertion.class, bundle.getString("dialog.title") );
        initComponents();
        setData(assertion);
    }

    @Override
    public MtomDecodeAssertion getData( final MtomDecodeAssertion assertion ) throws ValidationException {
        MessageTargetable source = (MessageTargetable) messageSourceComboBox.getSelectedItem();
        assertion.setTarget( source.getTarget() );
        assertion.setOtherTargetMessageVariable( source.getOtherTargetMessageVariable() );
        assertion.setOutputTarget( getOutputTarget( messageTargetComboBox, messageTargetVariableNameTextField ) );

        assertion.setRequireEncoded( requireEncodedCheckBox.isSelected() );
        assertion.setRemovePackaging( removePackagingCheckBox.isSelected() );

        return assertion;
    }

    @Override
    public void setData( final MtomDecodeAssertion assertion ) {
        messageSourceComboBox.setModel( buildMessageSourceComboBoxModel(assertion) );
        messageSourceComboBox.setSelectedItem( new MessageTargetableSupport(assertion) );
        selectOutputTarget( assertion.getOutputTarget(), messageTargetComboBox, messageTargetVariableNameTextField );

        requireEncodedCheckBox.setSelected( assertion.isRequireEncoded() );
        removePackagingCheckBox.setSelected( assertion.isRemovePackaging() );
    }

    //- PROTECTED

    @Override
    protected JPanel createPropertyPanel() {
        return mainPanel;
    }

        @Override
    protected void initComponents() {
        super.initComponents();

        final String defaultName = bundle.getString( "target.default" );
        final String variableName = bundle.getString( "target.messageVariable" );
        messageSourceComboBox.setRenderer( new TextListCellRenderer<MessageTargetable>( getMessageNameFunction(defaultName, null), null, false ) );
        messageTargetComboBox.setRenderer( new TextListCellRenderer<MessageTargetable>( getMessageNameFunction(defaultName, variableName), null, true ) );
        messageTargetComboBox.setModel( buildMessageTargetComboBoxModel() );

        final RunOnChangeListener enableDisableListener = new RunOnChangeListener( new Runnable(){
            @Override
            public void run() {
                enableAndDisableControls();
            }
        } );

        messageSourceComboBox.addActionListener( enableDisableListener );
        messageTargetComboBox.addActionListener( enableDisableListener );
        messageTargetVariableNameTextField.getDocument().addDocumentListener( enableDisableListener );
    }

    //- PRIVATE

    private static final ResourceBundle bundle = ResourceBundle.getBundle( MtomDecodeAssertionPropertiesDialog.class.getName());

    private JPanel mainPanel;
    private JCheckBox requireEncodedCheckBox;
    private JComboBox messageSourceComboBox;
    private JComboBox messageTargetComboBox;
    private JTextField messageTargetVariableNameTextField;
    private JCheckBox removePackagingCheckBox;

    private void enableAndDisableControls() {
        boolean validSource = messageSourceComboBox.getSelectedItem() != null;
        boolean validMessage = messageTargetComboBox.getSelectedItem()==null ||
                ((MessageTargetable)messageTargetComboBox.getSelectedItem()).getTarget()!=TargetMessageType.OTHER ||
                VariableMetadata.isNameValid(VariablePrefixUtil.fixVariableName(messageTargetVariableNameTextField.getText()));

        messageTargetVariableNameTextField.setEnabled(
                messageTargetComboBox.getSelectedItem()!=null &&
                ((MessageTargetable)messageTargetComboBox.getSelectedItem()).getTarget()==TargetMessageType.OTHER );

        getOkButton().setEnabled( validSource && validMessage && !isReadOnly() );
    }

}
