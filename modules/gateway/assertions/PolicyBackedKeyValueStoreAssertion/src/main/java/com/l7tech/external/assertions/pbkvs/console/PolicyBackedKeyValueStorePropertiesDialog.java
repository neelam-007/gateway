package com.l7tech.external.assertions.pbkvs.console;

import com.l7tech.console.action.ManagePolicyBackedServicesAction;
import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.pbkvs.PolicyBackedKeyValueStoreAssertion;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.polback.KeyValueStore;
import com.l7tech.objectmodel.polback.PolicyBackedService;
import com.l7tech.util.ExceptionUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;

public class PolicyBackedKeyValueStorePropertiesDialog extends AssertionPropertiesOkCancelSupport<PolicyBackedKeyValueStoreAssertion> {
    private JPanel contentPane;
    private JComboBox<PolicyBackedService> pbsComboBox;
    private JButton manageButton;
    private JRadioButton rbGet;
    private JRadioButton rbPut;
    private JTextField keyField;
    private JTextField targetVariableField;
    private JTextField valueField;

    public PolicyBackedKeyValueStorePropertiesDialog( Frame owner, PolicyBackedKeyValueStoreAssertion assertion ) {
        super( PolicyBackedKeyValueStoreAssertion.class, owner, assertion, true );
        initComponents();
    }

    @Override
    public void setData( PolicyBackedKeyValueStoreAssertion assertion ) {
        boolean get = PolicyBackedKeyValueStoreAssertion.OPERATION_GET.equals( assertion.getOperation() );
        rbGet.setSelected( get );
        rbPut.setSelected( !get );
        keyField.setText( assertion.getKey() );
        valueField.setText( assertion.getValue() );
        targetVariableField.setText( assertion.getTargetVariableName() );
        selectService( assertion.getPolicyBackedServiceGoid() );
        enableOrDisableComponents();
    }

    private void selectService( Goid pbsGoid ) {
        int items = pbsComboBox.getItemCount();
        for ( int i = 0; i < items; ++i ) {
            PolicyBackedService pbs = pbsComboBox.getItemAt( i );
            if ( pbs != null && Goid.equals( pbsGoid, pbs.getGoid() ) ) {
                pbsComboBox.setSelectedItem( pbs );
                return;
            }
        }
        pbsComboBox.setSelectedItem( null );
    }

    @Override
    public PolicyBackedKeyValueStoreAssertion getData( PolicyBackedKeyValueStoreAssertion assertion ) throws ValidationException {
        PolicyBackedService pbs = (PolicyBackedService) pbsComboBox.getSelectedItem();
        if ( pbs == null )
            throw new ValidationException( "A policy-backed service instance must be selected." );
        assertion.setPolicyBackedServiceGoid( pbs.getGoid() );
        assertion.setPolicyBackedServiceName( pbs.getName() );
        String op = rbGet.isSelected() ? PolicyBackedKeyValueStoreAssertion.OPERATION_GET : PolicyBackedKeyValueStoreAssertion.OPERATION_PUT;
        assertion.setOperation( op );
        assertion.setKey( keyField.getText() );
        assertion.setValue( valueField.getText() );
        assertion.setTargetVariableName( targetVariableField.getText() );

        return assertion;
    }

    @Override
    protected JPanel createPropertyPanel() {
        final RunOnChangeListener updater = new RunOnChangeListener( new Runnable() {
            @Override
            public void run() {
                enableOrDisableComponents();
            }
        } );

        rbGet.addActionListener( updater );
        rbPut.addActionListener( updater );
        Utilities.enableGrayOnDisabled( valueField, targetVariableField );

        try {
            Collection<PolicyBackedService> services = Registry.getDefault().getPolicyBackedServiceAdmin().findAllForInterface( KeyValueStore.class.getName() );
            pbsComboBox.setModel( new DefaultComboBoxModel<>( services.toArray( new PolicyBackedService[services.size()] ) ) );
        } catch ( FindException e ) {
            showError( "Unable to load policy backed services", e );
        }

        manageButton.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed( ActionEvent e ) {
                new ManagePolicyBackedServicesAction( new Runnable() {
                    @Override
                    public void run() {
                        populatePolicyBackedServicesComboBox();
                        enableOrDisableComponents();
                    }
                } ).actionPerformed( null );
            }
        } );

        return contentPane;
    }

    private void populatePolicyBackedServicesComboBox() {
        PolicyBackedService selectedPbs = (PolicyBackedService) pbsComboBox.getSelectedItem();
        Goid selectedGoid =  selectedPbs == null ? null : selectedPbs.getGoid();

        try {
            Collection<PolicyBackedService> services = Registry.getDefault().getPolicyBackedServiceAdmin().findAllForInterface( KeyValueStore.class.getName() );
            pbsComboBox.setModel( new DefaultComboBoxModel<>( services.toArray( new PolicyBackedService[services.size()] ) ) );
        } catch ( FindException e ) {
            showError( "Unable to load policy backed services", e );
        }

        selectService( selectedGoid );
    }

    private void enableOrDisableComponents() {
        boolean get = rbGet.isSelected();
        boolean put = !get;
        targetVariableField.setEnabled( get );
        valueField.setEnabled( put );
    }

    private void showError( String mess, Throwable t ) {
        if ( t != null )
            mess = mess + ": " + ExceptionUtils.getMessage( t );
        JOptionPane.showMessageDialog( getOwner(), mess, "Error", JOptionPane.ERROR_MESSAGE );
    }
}
