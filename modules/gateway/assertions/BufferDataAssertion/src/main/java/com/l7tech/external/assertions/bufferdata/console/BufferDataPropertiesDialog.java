package com.l7tech.external.assertions.bufferdata.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.util.ClusterPropertyCrud;
import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.bufferdata.BufferDataAssertion;
import com.l7tech.gui.NumberField;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.Assertion;

import javax.swing.*;
import java.awt.*;

public class BufferDataPropertiesDialog extends AssertionPropertiesOkCancelSupport<BufferDataAssertion> {
    private JPanel contentPane;
    private JTextField bufferNameTextField;
    private JTextField appendDataFromVariableTextField;
    private JTextField maximumBufferTimeTextField;
    private JTextField maximumBufferSizeTextField;
    private JTextField variablePrefixTextField;
    private JTextArea explanatoryText;
    private JScrollPane explanatoryScrollPane;

    public BufferDataPropertiesDialog( Frame owner, Assertion assertion ) {
        super( BufferDataAssertion.class, owner, assertion, true );
        initComponents();
    }

    @Override
    public void setData( BufferDataAssertion a ) {
        bufferNameTextField.setText( a.getBufferName() );
        appendDataFromVariableTextField.setText( a.getNewDataVarName() );
        maximumBufferSizeTextField.setText( Long.toString( a.getMaxSizeBytes() ) );
        maximumBufferTimeTextField.setText( Long.toString( a.getMaxAgeMillis() ) );
        variablePrefixTextField.setText( a.getVariablePrefix() );
    }

    @Override
    public BufferDataAssertion getData( BufferDataAssertion a ) throws ValidationException {
        a.setBufferName( bufferNameTextField.getText() );
        a.setNewDataVarName( appendDataFromVariableTextField.getText() );
        a.setExtractIfFull( true );
        a.setMaxAgeMillis( parseLong( "Maximum Buffer Time", maximumBufferTimeTextField.getText(), Long.MAX_VALUE ) );
        a.setMaxSizeBytes( parseLong( "Maximum Buffer Size", maximumBufferSizeTextField.getText(), getConfiguredGlobalBufferSizeLimit() ) );
        a.setVariablePrefix( variablePrefixTextField.getText() );
        return a;
    }

    long parseLong( String label, String str, long limit ) {
        try {
            long val = Long.parseLong( str );
            if ( val < 0 )
                throw new ValidationException( "Value of " + label + " must be nonnegative" );
            if ( val > limit )
                throw new ValidationException( "Value of " + label + " exceeds limit of " + limit );

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

        maximumBufferSizeTextField.setDocument( new NumberField() );
        maximumBufferTimeTextField.setDocument( new NumberField() );

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
