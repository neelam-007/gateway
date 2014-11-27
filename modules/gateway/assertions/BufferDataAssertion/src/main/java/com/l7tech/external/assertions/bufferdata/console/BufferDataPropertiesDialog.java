package com.l7tech.external.assertions.bufferdata.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.console.util.ClusterPropertyCrud;
import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.bufferdata.BufferDataAssertion;
import com.l7tech.gui.FilterDocument;
import com.l7tech.gui.MaxLengthDocument;
import com.l7tech.gui.NumberField;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.util.TimeUnit;
import static com.l7tech.external.assertions.bufferdata.BufferDataAssertion.StorageUnit;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ResourceBundle;

public class BufferDataPropertiesDialog extends AssertionPropertiesOkCancelSupport<BufferDataAssertion> {
    private JPanel contentPane;
    private JTextField bufferNameTextField;
    private JTextField maximumBufferTimeTextField;
    private JTextField maximumBufferSizeTextField;
    private JTextField variablePrefixTextField;
    private JTextArea explanatoryText;
    private JScrollPane explanatoryScrollPane;
    private JPanel targetVariablePanelPanel;
    private JComboBox<TimeUnit> timeUnitComboBox;
    private JComboBox<StorageUnit> storageUnitComboBox;
    private JLabel targetVariablePanelLabel;
    private TargetVariablePanel variableNameField;

    static final StorageUnit[] STORAGE_UNITS = BufferDataAssertion.getStorageUnits();
    static final TimeUnit[] TIME_UNITS = BufferDataAssertion.getTimeUnits();

    private static ResourceBundle bundle = ResourceBundle.getBundle( "com.l7tech.external.assertions.bufferdata.console.resources.BufferDataPropertiesDialog" );

    public BufferDataPropertiesDialog( Frame owner, final Assertion assertion ) {
        super( BufferDataAssertion.class, owner, assertion, true );
        initComponents();

        variableNameField.setAssertion( assertion, null );
        addPropertyChangeListener( "policyPosition", new PropertyChangeListener() {
            @Override
            public void propertyChange( PropertyChangeEvent evt ) {
                PolicyPosition pos = getPolicyPosition();
                if ( pos != null )
                    variableNameField.setAssertion( assertion, pos.getPreviousAssertion() );
            }
        } );
    }

    @Override
    public void setData( BufferDataAssertion a ) {
        bufferNameTextField.setText( a.getBufferName() );
        variableNameField.setVariable( a.getNewDataVarName() );

        long maxSizeBytes = a.getMaxSizeBytes();
        StorageUnit storageUnit = BufferDataAssertion.findBestStorageUnit( maxSizeBytes );
        storageUnitComboBox.setSelectedItem( storageUnit );
        maxSizeBytes /= storageUnit.getBytesPerUnit();
        maximumBufferSizeTextField.setText( Long.toString( maxSizeBytes ) );

        long maxAgeMillis = a.getMaxAgeMillis();
        TimeUnit timeUnit = BufferDataAssertion.findBestTimeUnit( maxAgeMillis );
        timeUnitComboBox.setSelectedItem( timeUnit );
        maxAgeMillis /= timeUnit.getMultiplier();

        maximumBufferTimeTextField.setText( Long.toString( maxAgeMillis ) );
        variablePrefixTextField.setText( a.getVariablePrefix() );
    }

    @Override
    public BufferDataAssertion getData( BufferDataAssertion a ) throws ValidationException {
        String bufferName = bufferNameTextField.getText();
        if ( bufferName.trim().length() < 1 )
            throw new ValidationException( bundle.getString( "error.bufferName.required" ) );
        if ( bufferName.length() > BufferDataAssertion.MAX_BUFFER_NAME_LENGTH )
            throw new ValidationException( bundle.getString( "error.bufferName.tooLong" ) );
        a.setBufferName( bufferName );
        String err = variableNameField.getErrorMessage();
        if ( err != null )
            throw new ValidationException( err );
        a.setNewDataVarName( variableNameField.getVariable() );

        TimeUnit timeUnit = (TimeUnit) timeUnitComboBox.getSelectedItem();
        long timeUnitMultiplier = timeUnit == null ? 1 : timeUnit.getMultiplier();
        a.setMaxAgeMillis( parseLong( "Maximum Buffer Time", maximumBufferTimeTextField.getText(), timeUnitMultiplier, Long.MAX_VALUE, "milliseconds" ) );

        StorageUnit storageUnit = (StorageUnit) storageUnitComboBox.getSelectedItem();
        long storageUnitMultiplier = storageUnit == null ? 1 : storageUnit.getBytesPerUnit();
        a.setMaxSizeBytes( parseLong( "Maximum Buffer Size", maximumBufferSizeTextField.getText(), storageUnitMultiplier, getConfiguredGlobalBufferSizeLimit(), "bytes" ) );

        String prefix = variablePrefixTextField.getText();
        if ( !VariableMetadata.isNameValid( prefix, false ) )
            throw new ValidationException( "Invalid prefix" );
        a.setVariablePrefix( prefix );
        return a;
    }

    long parseLong( String label, String str, long multiplier, long limit, String unitLabel ) {
        try {
            long val = Long.parseLong( str );
            if ( val < 0 )
                throw new ValidationException( "Value of " + label + " must be nonnegative" );
            val *= multiplier;
            if ( val > limit )
                throw new ValidationException( "Value of " + label + " exceeds limit of " + limit + " " + unitLabel );

            return val;

        } catch ( NumberFormatException nfe ) {
            throw new ValidationException( "Value of " + label + " is not a valid number" );
        }
    }

    @Override
    protected JPanel createPropertyPanel() {
        Color bg = new JLabel().getBackground();
        Color fg = new JLabel().getForeground();
        explanatoryText.setBackground( bg );
        explanatoryText.setEditable( false );
        explanatoryText.setEnabled( false );
        explanatoryText.setForeground( fg );
        explanatoryScrollPane.setBorder( null );
        explanatoryScrollPane.setBackground( bg );
        explanatoryScrollPane.setOpaque( false );

        bufferNameTextField.setDocument( new MaxLengthDocument( BufferDataAssertion.MAX_BUFFER_NAME_LENGTH ) );

        variableNameField = new TargetVariablePanel();
        variableNameField.setToolTipText( targetVariablePanelLabel.getToolTipText() );
        variableNameField.setValueWillBeRead( true );
        variableNameField.setValueWillBeWritten( false );
        targetVariablePanelPanel.setLayout( new BorderLayout() );
        targetVariablePanelPanel.add( variableNameField, BorderLayout.CENTER );
        targetVariablePanelLabel.setLabelFor( variableNameField );

        maximumBufferSizeTextField.setDocument( new NumberField() );
        maximumBufferTimeTextField.setDocument( new NumberField() );

        variablePrefixTextField.setDocument( new FilterDocument( 200, new FilterDocument.Filter() {
            @Override
            public boolean accept( String s ) {
                return s != null && VariableMetadata.isNameValid( s );
            }
        } ) );

        storageUnitComboBox.setModel( new DefaultComboBoxModel<>( STORAGE_UNITS ) );
        timeUnitComboBox.setModel( new DefaultComboBoxModel<>( TIME_UNITS ) );

        return contentPane;
    }

    static long getConfiguredGlobalBufferSizeLimit() {
        long ret = BufferDataAssertion.DEFAULT_MAX_BUFFER_SIZE;

        Registry reg = Registry.getDefault();
        if ( reg.isAdminContextPresent() ) {
            try {
                String value = ClusterPropertyCrud.getClusterProperty( BufferDataAssertion.CLUSTER_PROP_MAX_BUFFER_SIZE );
                if ( value != null && value.trim().length() > 0 )
                    ret = Long.parseLong( value );
            } catch ( NumberFormatException | FindException nfe ) {
                /* FALLTHROUGH and use default value */
            }
        }

        return ret;
    }
}
