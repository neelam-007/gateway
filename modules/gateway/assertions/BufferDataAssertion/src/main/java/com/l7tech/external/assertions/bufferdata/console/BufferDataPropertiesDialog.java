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

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

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
    private TargetVariablePanel variableNameField;

    enum StorageUnit {
        BYTES( 1, "bytes" ),
        KILOBYTES( 1024, "kilobytes" ),
        MEGABYTES( 1024 * 1024, "megabytes" );

        private final int bytesPerUnit;
        private final String displayName;

        StorageUnit( int bytesPerUnit, String displayName ) {
            this.bytesPerUnit = bytesPerUnit;
            this.displayName = displayName;
        }

        public int getBytesPerUnit() {
            return bytesPerUnit;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    static final StorageUnit[] STORAGE_UNITS = new StorageUnit[] { StorageUnit.BYTES, StorageUnit.KILOBYTES, StorageUnit.MEGABYTES };
    static final TimeUnit[] TIME_UNITS = new TimeUnit[] { TimeUnit.MILLIS, TimeUnit.SECONDS, TimeUnit.MINUTES, TimeUnit.HOURS };

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
        StorageUnit storageUnit = selectBestStorageUnit( maxSizeBytes );
        maxSizeBytes /= storageUnit.getBytesPerUnit();
        maximumBufferSizeTextField.setText( Long.toString( maxSizeBytes ) );

        long maxAgeMillis = a.getMaxAgeMillis();
        TimeUnit timeUnit = selectBestTimeUnit( maxAgeMillis );
        maxAgeMillis /= timeUnit.getMultiplier();

        maximumBufferTimeTextField.setText( Long.toString( maxAgeMillis ) );
        variablePrefixTextField.setText( a.getVariablePrefix() );
    }

    private TimeUnit selectBestTimeUnit( long t ) {
        TimeUnit ret = TimeUnit.MILLIS;

        java.util.List<TimeUnit> units = new ArrayList<>( Arrays.asList( TIME_UNITS ) );
        Collections.reverse( units );
        for ( TimeUnit unit : units ) {
            if ( t % unit.getMultiplier() == 0 ) {
                ret = unit;
                break;
            }
        }

        timeUnitComboBox.setSelectedItem( ret );
        return ret;
    }

    private StorageUnit selectBestStorageUnit( long s ) {
        StorageUnit ret = StorageUnit.BYTES;

        java.util.List<StorageUnit> units = new ArrayList<>( Arrays.asList( STORAGE_UNITS ) );
        Collections.reverse( units );
        for ( StorageUnit unit : units ) {
            if ( s % unit.getBytesPerUnit() == 0 ) {
                ret = unit;
                break;
            }
        }

        storageUnitComboBox.setSelectedItem( ret );
        return ret;
    }

    @Override
    public BufferDataAssertion getData( BufferDataAssertion a ) throws ValidationException {
        String bufferName = bufferNameTextField.getText();
        if ( bufferName.trim().length() < 1 )
            throw new ValidationException( "A buffer name is required." );
        if ( bufferName.length() > BufferDataAssertion.MAX_BUFFER_NAME_LENGTH )
            throw new ValidationException( "Buffer name is too long." );
        a.setBufferName( bufferName );
        String err = variableNameField.getErrorMessage();
        if ( err != null )
            throw new ValidationException( err );
        a.setNewDataVarName( variableNameField.getVariable() );
        a.setExtractIfFull( true );

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
        variableNameField.setValueWillBeRead( true );
        variableNameField.setValueWillBeWritten( false );
        targetVariablePanelPanel.setLayout( new BorderLayout() );
        targetVariablePanelPanel.add( variableNameField, BorderLayout.CENTER );

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
