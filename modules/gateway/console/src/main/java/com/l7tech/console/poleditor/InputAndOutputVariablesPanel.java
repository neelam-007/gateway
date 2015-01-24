package com.l7tech.console.poleditor;

import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionArgumentDescriptor;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionResultDescriptor;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;

/**
 * A panel that displays encapsulated assertion inputs and outputs.
 */
public class InputAndOutputVariablesPanel extends JPanel {
    private JTable outputsTable;
    private JTable inputsTable;
    private JPanel mainPanel;
    private JSplitPane splitPane;
    private JLabel interfaceTitleLabel;

    private SimpleTableModel<EncapsulatedAssertionArgumentDescriptor> inputsTableModel;
    private SimpleTableModel<EncapsulatedAssertionResultDescriptor> outputsTableModel;
    private Collection<EncapsulatedAssertionArgumentDescriptor> inputs;
    private Collection<EncapsulatedAssertionResultDescriptor> outputs;
    private String interfaceTitle = null;

    public InputAndOutputVariablesPanel() {
        this( null, null );
    }

    public InputAndOutputVariablesPanel( @Nullable Collection<EncapsulatedAssertionArgumentDescriptor> inputs, @Nullable Collection<EncapsulatedAssertionResultDescriptor> outputs ) {
        setLayout( new BorderLayout() );
        removeAll();
        add( mainPanel, BorderLayout.CENTER );
        Utilities.deuglifySplitPane( splitPane );
        setInputs( inputs );
        setOutputs( outputs );
        setInterfaceTitle( null );
        Color bg = new JPanel().getBackground();
        inputsTable.setBackground( bg );
        outputsTable.setBackground( bg );
    }

    public void setInputs( @Nullable Collection<EncapsulatedAssertionArgumentDescriptor> inputs ) {
        if ( this.inputs != inputs ) {
            Collection<EncapsulatedAssertionArgumentDescriptor> oldValue = this.inputs;
            this.inputs = inputs;
            this.inputsTableModel = TableUtil.configureTable( inputsTable,
                    TableUtil.column( "Name", 50, 100, 99999, Functions.propertyTransform( EncapsulatedAssertionArgumentDescriptor.class, "argumentName" ) ),
                    TableUtil.column( "Type", 50, 100, 99999, INPUT_DATA_TYPE_FINDER )
            );
            if ( inputs != null ) {
                inputsTableModel.setRows( new ArrayList<>( inputs ) );
            }
            firePropertyChange( "inputs", oldValue, inputs );
        }
    }

    public Collection<EncapsulatedAssertionArgumentDescriptor> getInputs() {
        return inputs;
    }

    public void setOutputs( @Nullable Collection<EncapsulatedAssertionResultDescriptor> outputs ) {
        if ( this.outputs != outputs ) {
            Collection<EncapsulatedAssertionResultDescriptor> oldValue = this.outputs;
            this.outputs = outputs;
            this.outputsTableModel = TableUtil.configureTable( outputsTable,
                    TableUtil.column( "Name", 50, 100, 99999, Functions.propertyTransform( EncapsulatedAssertionResultDescriptor.class, "resultName" ) ),
                    TableUtil.column( "Type", 50, 100, 99999, RESULT_DATA_TYPE_FINDER )
            );
            if ( outputs != null ) {
                outputsTableModel.setRows( new ArrayList<>( outputs ) );
            }
            firePropertyChange( "outputs", oldValue, outputs );
        }
    }

    public Collection<EncapsulatedAssertionResultDescriptor> getOutputs() {
        return outputs;
    }

    public boolean isEmpty() {
        return ( inputs == null || inputs.isEmpty() ) && ( outputs == null || outputs.isEmpty() );
    }

    public void setInterfaceTitle( String interfaceTitle ) {
        this.interfaceTitle = interfaceTitle;
        interfaceTitleLabel.setText( interfaceTitle != null ? interfaceTitle : "" );
        interfaceTitleLabel.setVisible( interfaceTitle != null );
    }

    public String getInterfaceTitle() {
        return interfaceTitle;
    }

    private static final Functions.Unary<Object, EncapsulatedAssertionResultDescriptor> RESULT_DATA_TYPE_FINDER = new Functions.Unary<Object, EncapsulatedAssertionResultDescriptor>() {
        @Override
        public Object call( EncapsulatedAssertionResultDescriptor result ) {
            return result.dataType();
        }
    };

    public static final Functions.Unary<Object, EncapsulatedAssertionArgumentDescriptor> INPUT_DATA_TYPE_FINDER = new Functions.Unary<Object, EncapsulatedAssertionArgumentDescriptor>() {
        @Override
        public Object call( EncapsulatedAssertionArgumentDescriptor input ) {
            return input.dataType();
        }
    };
}
