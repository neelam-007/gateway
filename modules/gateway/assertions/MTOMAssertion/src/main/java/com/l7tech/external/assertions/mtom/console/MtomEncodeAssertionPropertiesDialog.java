package com.l7tech.external.assertions.mtom.console;

import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.external.assertions.mtom.MtomEncodeAssertion;
import com.l7tech.gui.NumberField;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.policy.assertion.MessageTargetable;
import com.l7tech.policy.assertion.MessageTargetableSupport;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.util.Functions;
import com.l7tech.util.ValidationUtils;
import com.l7tech.xml.xpath.XpathExpression;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;

/**
 *
 */
public class MtomEncodeAssertionPropertiesDialog extends MtomAssertionPropertiesDialogSupport<MtomEncodeAssertion> {

    //- PUBLIC

    public MtomEncodeAssertionPropertiesDialog( final Window parent,
                                                final MtomEncodeAssertion assertion ) {
        super(parent, MtomEncodeAssertion.class, bundle.getString("dialog.title"));
        initComponents();
        setData(assertion);
    }

    @Override
    public MtomEncodeAssertion getData( final MtomEncodeAssertion assertion ) throws ValidationException {
        MessageTargetable source = (MessageTargetable) messageSourceComboBox.getSelectedItem();
        assertion.setTarget( source.getTarget() );
        assertion.setOtherTargetMessageVariable( source.getOtherTargetMessageVariable() );
        assertion.setOutputTarget( getOutputTarget( messageTargetComboBox, messageTargetVariableNameTextField ) );

        assertion.setOptimizationThreshold( Integer.parseInt(optimizationThresholdTextField.getText()) * 1024 );
        assertion.setAlwaysEncode( alwaysEncodeCheckBox.isSelected() );
        assertion.setFailIfNotFound( failIfNotFoundCheckBox.isSelected() );
        assertion.setXpathExpressions( toArray(optimizationXPathsList.getModel()) );

        return assertion;
    }

    @Override
    public void setData( final MtomEncodeAssertion assertion ) {
        messageSourceComboBox.setModel( buildMessageSourceComboBoxModel(assertion) );
        messageSourceComboBox.setSelectedItem( new MessageTargetableSupport(assertion) );
        selectOutputTarget( assertion.getOutputTarget(), messageTargetComboBox, messageTargetVariableNameTextField );

        optimizationThresholdTextField.setText( Integer.toString(assertion.getOptimizationThreshold() / 1024) );
        alwaysEncodeCheckBox.setSelected( assertion.isAlwaysEncode() );
        failIfNotFoundCheckBox.setSelected( assertion.isFailIfNotFound() );
        messageTargetVariableNameTextField.setAssertion(assertion,getPreviousAssertion());
        DefaultListModel model = (DefaultListModel) optimizationXPathsList.getModel();
        model.clear();
        if ( assertion.getXpathExpressions() != null ) {
            for ( XpathExpression expression : assertion.getXpathExpressions() ) {
                final XpathExpression forEdit = new XpathExpression();
                forEdit.setExpression( expression.getExpression() );
                forEdit.setNamespaces( expression.getNamespaces() );                
                model.addElement( forEdit );
            }
        }

        enableAndDisableControls();
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
        messageTargetComboBox.setModel( buildMessageTargetComboBoxModel(true) );

        messageTargetVariableNameTextField = new TargetVariablePanel();
        messageTargetVariableNamePanel.setLayout(new BorderLayout());
        messageTargetVariableNamePanel.add(messageTargetVariableNameTextField, BorderLayout.CENTER);

        addButton.addActionListener( new ActionListener(){
            @Override
            public void actionPerformed( final ActionEvent e ) {
                addXPath();
            }
        } );

        editButton.addActionListener( new ActionListener(){
            @Override
            public void actionPerformed( final ActionEvent e ) {
                editXPath();
            }
        } );

        removeButton.addActionListener( new ActionListener(){
            @Override
            public void actionPerformed( final ActionEvent e ) {
                removeXPath();
            }
        } );

        final RunOnChangeListener enableDisableListener = new RunOnChangeListener( new Runnable(){
            @Override
            public void run() {
                enableAndDisableControls();
            }
        } );

        messageSourceComboBox.addActionListener( enableDisableListener );
        messageTargetComboBox.addActionListener( enableDisableListener );
        messageTargetVariableNameTextField.addChangeListener( enableDisableListener );

        optimizationThresholdTextField.setDocument( new NumberField(4) );
        optimizationThresholdTextField.getDocument().addDocumentListener( enableDisableListener );

        optimizationXPathsList.setModel( new DefaultListModel() );
        optimizationXPathsList.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
        optimizationXPathsList.addListSelectionListener( enableDisableListener );
        optimizationXPathsList.setCellRenderer( new TextListCellRenderer<XpathExpression>( new Functions.Unary<String,XpathExpression>(){
            @Override
            public String call( final XpathExpression xpathExpression ) {
                return xpathExpression.getExpression() == null ? "" : xpathExpression.getExpression();
            }
        } ) );
        Utilities.setDoubleClickAction( optimizationXPathsList, editButton );
    }


    //- PRIVATE

    private static final ResourceBundle bundle = ResourceBundle.getBundle(MtomEncodeAssertionPropertiesDialog.class.getName());

    private JPanel mainPanel;
    private JCheckBox alwaysEncodeCheckBox;
    private JTextField optimizationThresholdTextField;
    private JComboBox messageSourceComboBox;
    private JComboBox messageTargetComboBox;
    private JPanel messageTargetVariableNamePanel;
    private TargetVariablePanel messageTargetVariableNameTextField;
    private JCheckBox failIfNotFoundCheckBox;
    private JList optimizationXPathsList;
    private JButton addButton;
    private JButton editButton;
    private JButton removeButton;

    private void enableAndDisableControls() {
        
        messageTargetVariableNameTextField.setEnabled(
                messageTargetComboBox.getSelectedItem()!=null &&
                ((MessageTargetable)messageTargetComboBox.getSelectedItem()).getTarget()==TargetMessageType.OTHER );

        boolean validSource = messageSourceComboBox.getSelectedItem() != null;
        boolean validThreshold = ValidationUtils.isValidInteger( optimizationThresholdTextField.getText(), false, 0, 9999 );
        boolean validMessage = messageTargetComboBox.getSelectedItem()==null ||
                ((MessageTargetable)messageTargetComboBox.getSelectedItem()).getTarget()!=TargetMessageType.OTHER ||
                messageTargetVariableNameTextField.isEntryValid();


        getOkButton().setEnabled( validSource && validThreshold && validMessage && !isReadOnly() );

        boolean itemSelected = optimizationXPathsList.getSelectedValue()!=null;
        addButton.setEnabled( !isReadOnly() );
        editButton.setEnabled( itemSelected );
        removeButton.setEnabled( !isReadOnly() && itemSelected );
    }

    private void addXPath() {
        editXpath(
                this,
                getTitle(),
                ((MessageTargetable) messageSourceComboBox.getSelectedItem()).getTarget(),
                null,
                new Functions.UnaryVoid<XpathExpression>(){
                    @Override
                    public void call( final XpathExpression xpathExpression ) {
                        ((DefaultListModel)optimizationXPathsList.getModel()).addElement( xpathExpression );
                    }
                } );
    }

    private void editXPath() {
        final XpathExpression expression = (XpathExpression) optimizationXPathsList.getSelectedValue();
        editXpath(
                this,
                getTitle(),
                ((MessageTargetable) messageSourceComboBox.getSelectedItem()).getTarget(),
                expression, 
                new Functions.UnaryVoid<XpathExpression>(){
                    @Override
                    public void call( final XpathExpression xpathExpression ) {
                        expression.setExpression( xpathExpression.getExpression() );
                        expression.setNamespaces( xpathExpression.getNamespaces() );
                        ((DefaultListModel)optimizationXPathsList.getModel()).setElementAt(
                                expression,
                                optimizationXPathsList.getSelectedIndex() );
                    }
                }  );
    }

    private void removeXPath() {
        ((DefaultListModel)optimizationXPathsList.getModel()).remove( optimizationXPathsList.getSelectedIndex() );
    }

    private XpathExpression[] toArray( final ListModel model ) {
        XpathExpression[] expressions = null;

        if ( model.getSize() > 0 ) {
            expressions = new XpathExpression[ model.getSize() ];

            for ( int i=0; i<expressions.length; i++ ) {
                expressions[i] = (XpathExpression) model.getElementAt( i );
            }
        }

        return expressions;
    }
}
