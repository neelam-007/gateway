package com.l7tech.external.assertions.samlpassertion.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.console.util.VariablePrefixUtil;
import com.l7tech.external.assertions.samlpassertion.SamlStatus;
import com.l7tech.external.assertions.samlpassertion.SamlVersion;
import com.l7tech.external.assertions.samlpassertion.SetSamlStatusAssertion;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.util.Functions;

import javax.swing.*;
import java.awt.*;

/**
 * Properties dialog for Set SAML Status assertion.
 */
public class SetSamlStatusPropertiesDialog extends AssertionPropertiesOkCancelSupport<SetSamlStatusAssertion> {

    //- PUBLIC

    public SetSamlStatusPropertiesDialog( final Window parent,
                                         final SetSamlStatusAssertion assertion ) {
        super( SetSamlStatusAssertion.class, parent, assertion, true );
        initComponents();
        setData(assertion);
    }

    @Override
    public SetSamlStatusAssertion getData( final SetSamlStatusAssertion assertion ) throws ValidationException {
        assertion.setSamlStatus( (SamlStatus)statusComboBox.getSelectedItem() );
        assertion.setVariableName( VariablePrefixUtil.fixVariableName( variableNameTextField.getVariable() ) );
        return assertion;
    }

    @Override
    public void setData( final SetSamlStatusAssertion assertion ) {
        if ( assertion.getSamlStatus() != null ) {
            versionComboBox.setSelectedItem( SamlVersion.valueOf(assertion.getSamlStatus().getSamlVersion()) );
            statusComboBox.setSelectedItem( assertion.getSamlStatus() );
        }
        if ( assertion.getVariableName() != null ) {
            variableNameTextField.setVariable(assertion.getVariableName());
        }
        variableNameTextField.setAssertion(assertion,getPreviousAssertion());

    }

    //- PROTECTED

    @Override
    protected JPanel createPropertyPanel() {
        return mainPanel;
    }

    @Override
    protected void initComponents() {
        super.initComponents();

        versionComboBox.setModel( new DefaultComboBoxModel( new SamlVersion[] { SamlVersion.SAML1_1, SamlVersion.SAML2 } ) );

        statusComboBox.setModel( new DefaultComboBoxModel( SamlStatus.getSaml1xStatuses().toArray() ) );
        statusComboBox.setRenderer( new TextListCellRenderer<SamlStatus>( new Functions.Unary<String,SamlStatus>(){
            @Override
            public String call( final SamlStatus dataType ) {
                return dataType.getValue();
            }
        } ) );

        variableNameTextField = new TargetVariablePanel();
        variableNamePanel.setLayout(new BorderLayout());
        variableNamePanel.add(variableNameTextField, BorderLayout.CENTER);


        final RunOnChangeListener enableDisableListener = new RunOnChangeListener(){
            @Override
            public void run() {
                enableDisableComponents();
            }
        };

        versionComboBox.addActionListener( enableDisableListener );
        variableNameTextField.addChangeListener(enableDisableListener);
    }

    //- PRIVATE

    private JPanel mainPanel;
    private JComboBox versionComboBox;
    private JComboBox statusComboBox;
    private JPanel variableNamePanel;
    private TargetVariablePanel variableNameTextField;

    private void enableDisableComponents() {
        boolean enableAny = !isReadOnly() && variableNameTextField.isEntryValid();

        if ( ((SamlVersion)versionComboBox.getSelectedItem()).getVersionInt() != ((SamlStatus)statusComboBox.getSelectedItem()).getSamlVersion() ) {
            if ( SamlVersion.SAML1_1.equals(versionComboBox.getSelectedItem()) ) {
                statusComboBox.setModel( new DefaultComboBoxModel( SamlStatus.getSaml1xStatuses().toArray() ) );
            } else {
                statusComboBox.setModel( new DefaultComboBoxModel( SamlStatus.getSaml2xStatuses().toArray() ) );
            }
        }

        getOkButton().setEnabled( enableAny );
    }
}
