package com.l7tech.console.panels.polback;

import com.l7tech.console.util.Registry;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.*;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.ObjectNotFoundException;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.objectmodel.polback.PolicyBackedService;
import com.l7tech.objectmodel.polback.PolicyBackedServiceOperation;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PolicyBackedServicePropertiesDialog extends JDialog {
    private static final Logger logger = Logger.getLogger( PolicyBackedServicePropertiesDialog.class.getName() );

    private JPanel contentPane;
    private JButton okButton;
    private JButton cancelButton;
    private JComboBox<String> templateComboBox;
    private JTextField nameField;
    private JTable operationsTable;
    private JButton assignButton;

    private SimpleTableModel<OperationRow> operationsTableModel;
    private final InputValidator inputValidator;

    private boolean confirmed = false;

    public PolicyBackedServicePropertiesDialog( Window owner, PolicyBackedService bean ) {
        super( owner, ModalityType.APPLICATION_MODAL );
        setTitle( "Policy Backed Service Properties" );
        setContentPane( contentPane );
        setModal( true );
        getRootPane().setDefaultButton( okButton );
        setDefaultCloseOperation( DISPOSE_ON_CLOSE );
        Utilities.setEscKeyStrokeDisposes( this );

        inputValidator = new InputValidator( this, getTitle() );
        inputValidator.attachToButton( okButton, new ActionListener() {
            @Override
            public void actionPerformed( ActionEvent e ) {
                confirmed = true;
                dispose();
            }
        } );
        inputValidator.addRule( new InputValidator.ComponentValidationRule( operationsTable ) {
            @Override
            public String getValidationError() {
                OperationRow missingAssignment = Functions.grepFirst( operationsTableModel.getRows(), new Functions.Unary<Boolean, OperationRow>() {
                    @Override
                    public Boolean call( OperationRow row ) {
                        return row.concreteOperation == null;
                    }
                } );

                return missingAssignment == null
                        ? null
                        : "No implementaion assigned for operation: " + missingAssignment.getOperationName();
            }
        });

        RunOnChangeListener disposeListener = new RunOnChangeListener( new Runnable() {
            @Override
            public void run() {
                dispose();
            }
        } );
        cancelButton.addActionListener( disposeListener );

        populateTemplatesComboBox();
        setData( bean );

        templateComboBox.setEnabled( bean.isUnsaved() );

        templateComboBox.setRenderer( new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent( JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus ) {

                if ( value instanceof PolicyBackedService ) {
                    PolicyBackedService tbs = (PolicyBackedService) value;
                    value = tbs.getName();
                }

                return super.getListCellRendererComponent( list, value, index, isSelected, cellHasFocus );
            }
        });

        templateComboBox.addActionListener( new RunOnChangeListener( new Runnable() {
            @Override
            public void run() {
                populateOperationsTable( (String) templateComboBox.getSelectedItem() );
            }
        } ) );

        assignButton.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed( ActionEvent e ) {
                doAssign();
            }
        } );

        Utilities.setDoubleClickAction( operationsTable, assignButton );

    }

    private void doAssign() {
        final OperationRow row = getSelectedOperationRow();
        if ( row == null )
            return;

        final String serviceInterfaceName = row.templateEncass.getProperty( EncapsulatedAssertionConfig.PROP_SERVICE_INTERFACE );
        if ( serviceInterfaceName == null ) {
            logger.warning( "template encass config " + row.templateEncass.getName() + " lacks serviceInterface property" );
            return;
        }

        final String operationName = row.templateEncass.getProperty( EncapsulatedAssertionConfig.PROP_SERVICE_METHOD );
        if ( operationName == null ) {
            logger.warning( "template encass config " + row.templateEncass.getName() + " lacks serviceMethod property" );
            return;
        }

        // Find possible assignments
        final Collection<Policy> concreteCandidates;
        try {
            concreteCandidates = Registry.getDefault().getPolicyAdmin().findPoliciesByTypeTagAndSubTag( PolicyType.POLICY_BACKED_OPERATION, serviceInterfaceName, operationName );
        } catch ( FindException e ) {
            showError( "Unable to load candidate encapsulated assertion configurations", e );
            return;
        }

        if ( concreteCandidates.isEmpty() ) {
            // TODO offer to create one on the spot
            showError( "There are currently no policies of type Policy-Backed Operation with the correct tag and operation.  Please create one first.", null );
            return;
        }

        Collection<Pair<Policy,String>> items = new ArrayList<>();
        for ( Policy policy : concreteCandidates ) {
            items.add( new Pair<Policy,String>( policy, policy.getName() ) {
                @Override
                public String toString() {
                    return right;
                }
            } );
        }

        DialogDisplayer.showInputDialog( this,
                "Select implementation policy:",
                "Select Implementation",
                JOptionPane.INFORMATION_MESSAGE,
                null,
                items.toArray(),
                null,
                new DialogDisplayer.InputListener() {
                    @Override
                    public void reportResult( Object option ) {
                        if ( option != null ) {
                            Pair item = (Pair) option;
                            Policy policy = (Policy) item.left;

                            if ( row.concreteOperation == null ) {
                                row.concreteOperation = new PolicyBackedServiceOperation();
                                row.concreteOperation.setName( row.templateEncass.getName() );
                            }
                            row.concreteOperation.setPolicyGoid( policy.getGoid() );
                            row.concretePolicy = policy;
                            operationsTableModel.fireTableDataChanged(); // TODO update only affected row
                        }
                    }
                }
        );
    }

    private OperationRow getSelectedOperationRow() {
        return operationsTableModel.getRowObject( operationsTable.convertRowIndexToModel( operationsTable.getSelectedRow() ) );
    }

    private void populateTemplatesComboBox() {
        Collection<String> templates = Registry.getDefault().getPolicyBackedServiceAdmin().findAllTemplateInterfaceNames();
        templateComboBox.setModel( new DefaultComboBoxModel<>( templates.toArray( new String[templates.size()] ) ));
    }

    private void error( String msg, Throwable t ) {
        if ( t != null )
            msg = msg + ": " + ExceptionUtils.getMessage( t );
        logger.log( Level.WARNING, msg, t );
        DialogDisplayer.showMessageDialog( this, msg, "Error", JOptionPane.ERROR_MESSAGE, null );
    }

    public void setData( PolicyBackedService in ) {
        nameField.setText( in.getName() );
        templateComboBox.setSelectedItem( in.getServiceInterfaceName() );

        populateOperationsTable( in.getServiceInterfaceName() );
        populateOperationsTableConcreteAssignments( in );
    }

    private void populateOperationsTable( String templateInterfaceName ) {
        operationsTableModel = TableUtil.configureTable( operationsTable,
                TableUtil.column( "Operation", 100, 200, 9999, Functions.propertyTransform( OperationRow.class, "operationName" ) ),
                TableUtil.column( "Encapsulated Assertion", 100, 200, 9999, Functions.propertyTransform( OperationRow.class, "implementationName" ) ) );

        if ( templateInterfaceName != null ) {
            final Collection<EncapsulatedAssertionConfig> templateEacs;
            try {
                templateEacs = Registry.getDefault().getPolicyBackedServiceAdmin().getInterfaceDescription( templateInterfaceName );
            } catch ( ObjectNotFoundException e ) {
                showError( "Unable to obtain description of service interface: " + templateInterfaceName, e );
                return;
            }

            java.util.List<OperationRow> rows = new ArrayList<>();
            for ( EncapsulatedAssertionConfig templateEac : templateEacs ) {
                OperationRow row = new OperationRow( templateEac );
                rows.add( row );
            }

            operationsTableModel.setRows( rows );
        }
    }

    private void populateOperationsTableConcreteAssignments( @Nullable PolicyBackedService concreteService ) {
        boolean modified = false;
        java.util.List<OperationRow> rows = operationsTableModel.getRows();
        for ( OperationRow row : rows ) {
            PolicyBackedServiceOperation concreteOperation = findConcreteOperation( concreteService, row.templateEncass );
            if ( row.concreteOperation != concreteOperation ) {
                row.concreteOperation = concreteOperation;
                row.concretePolicy = resolvePolicy( concreteOperation.getPolicyGoid() );
                modified = true;
            }
        }
        if ( modified ) {
            operationsTableModel.fireTableDataChanged();
        }
    }

    private Policy resolvePolicy( Goid policyGoid ) {
        if ( policyGoid == null )
            return null;

        try {
            return Registry.getDefault().getPolicyAdmin().findPolicyByPrimaryKey( policyGoid );
        } catch ( Exception e ) {
            logger.log( Level.WARNING, "Unable to access policy " + policyGoid + ": " + ExceptionUtils.getMessage( e ), ExceptionUtils.getDebugException( e ) );
            return new Policy( PolicyType.POLICY_BACKED_OPERATION, "<Not accessible: " + policyGoid + ">", null, false);
        }
    }

    private PolicyBackedServiceOperation findConcreteOperation( @Nullable PolicyBackedService concreteService, final EncapsulatedAssertionConfig templateEncass ) {
        if ( concreteService == null )
            return null;

        final String methodName = templateEncass.getProperty( EncapsulatedAssertionConfig.PROP_SERVICE_METHOD );
        if ( methodName == null )
            return null;

        return Functions.grepFirst( concreteService.getOperations(), new Functions.Unary<Boolean, PolicyBackedServiceOperation>() {
            @Override
            public Boolean call( PolicyBackedServiceOperation concreteOperation ) {
                return methodName.equals( concreteOperation.getName() );
            }
        } );
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public void getData( PolicyBackedService out ) {
        out.setName( nameField.getText() );
        out.setServiceInterfaceName( (String) templateComboBox.getSelectedItem() );

        Set<PolicyBackedServiceOperation> operations = new HashSet<>( );
        List<OperationRow> rows = operationsTableModel.getRows();
        for ( OperationRow row : rows ) {
            if ( row.concreteOperation != null && row.concreteOperation.getPolicyGoid() != null ) {
                row.concreteOperation.setPolicyBackedService( out );
                operations.add( row.concreteOperation );
            }
        }

        out.setOperations( operations );
    }

    public static class OperationRow {
        @NotNull
        private final EncapsulatedAssertionConfig templateEncass;

        private PolicyBackedServiceOperation concreteOperation;

        private Policy concretePolicy;

        private OperationRow( @NotNull EncapsulatedAssertionConfig templateEncass ) {
            this.templateEncass = templateEncass;
        }

        @SuppressWarnings( "UnusedDeclaration" )
        public String getOperationName() {
            return templateEncass.getName();
        }

        @SuppressWarnings( "UnusedDeclaration" )
        public String getImplementationName() {
            return concretePolicy == null
                    ? "<unassigned>"
                    : concretePolicy.getName();
        }
    }

    /**
     * Displays an error message to the user.
     * @param message the error message to show the user.
     * @param e the Throwable which caused the error or null if you do not want to show the exception to the user.
     */
    private void showError(@NotNull final String message, @Nullable final Throwable e) {
        String error = message;
        if (e != null) {
            error = error + ": " + ExceptionUtils.getMessage(e);
        }
        DialogDisplayer.showMessageDialog(this, error, "Error", JOptionPane.ERROR_MESSAGE, null);
    }
}
