package com.l7tech.external.assertions.bufferdata.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.external.assertions.bufferdata.BufferDataAssertion;
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
        a.setMaxAgeMillis( Long.parseLong( maximumBufferTimeTextField.getText() ) );
        a.setMaxSizeBytes( Long.parseLong( maximumBufferSizeTextField.getText()) );
        a.setVariablePrefix( variablePrefixTextField.getText() );
        return a;
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
        return contentPane;
    }
}
