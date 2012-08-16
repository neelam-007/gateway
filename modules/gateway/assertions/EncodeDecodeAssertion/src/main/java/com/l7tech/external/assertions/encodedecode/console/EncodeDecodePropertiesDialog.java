package com.l7tech.external.assertions.encodedecode.console;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.console.util.VariablePrefixUtil;
import com.l7tech.external.assertions.encodedecode.EncodeDecodeAssertion;
import com.l7tech.gui.NumberField;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.ValidationUtils;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

/**
 * Properties dialog for Encode / Decode assertion.
 */
public class EncodeDecodePropertiesDialog extends AssertionPropertiesOkCancelSupport<EncodeDecodeAssertion> {

    //- PUBLIC

    public EncodeDecodePropertiesDialog( final Window parent,
                                         final EncodeDecodeAssertion assertion ) {
        super( EncodeDecodeAssertion.class, parent, assertion, true );
        initComponents();
        setData(assertion);
    }

    @Override
    public EncodeDecodeAssertion getData( final EncodeDecodeAssertion assertion ) throws ValidationException {
        validateData();
        assertion.setTransformType( (EncodeDecodeAssertion.TransformType) encodeDecodeComboBox.getSelectedItem() );
        assertion.setSourceVariableName( VariablePrefixUtil.fixVariableName( sourceVariableTextField.getText() ) );
        assertion.setTargetVariableName( VariablePrefixUtil.fixVariableName( targetVariableTextField.getVariable() ) );
        assertion.setTargetDataType( (DataType) dataTypeComboBox.getSelectedItem() );
        assertion.setTargetContentType( nullIfEmpty(contentTypeTextField) );
        assertion.setCharacterEncoding( nullIfEmpty(encodingTextField) );
        assertion.setStrict( strictCheckBox.isSelected() );
        assertion.setLineBreakInterval( multipleLinesCheckBox.isSelected()&&lineBreakEveryTextField.isEnabled() ? Integer.parseInt(lineBreakEveryTextField.getText()): 0 );

        return assertion;
    }

    @Override
    public void setData( final EncodeDecodeAssertion assertion ) {
        if ( assertion.getTransformType() != null ) encodeDecodeComboBox.setSelectedItem( assertion.getTransformType() );
        setText( sourceVariableTextField, assertion.getSourceVariableName() );
        targetVariableTextField.setVariable(assertion.getTargetVariableName() );
        targetVariableTextField.setAssertion(assertion,getPreviousAssertion());
        if ( assertion.getTargetDataType() != null ) {
            dataTypeComboBox.setSelectedItem( assertion.getTargetDataType() );
        } else {
            dataTypeComboBox.setSelectedItem( DataType.STRING );
        }
        setText( contentTypeTextField, assertion.getTargetContentType() );
        setText( encodingTextField, assertion.getCharacterEncoding() );
        strictCheckBox.setSelected( assertion.isStrict() );
        multipleLinesCheckBox.setSelected( assertion.getLineBreakInterval() > 0 );
        setText( lineBreakEveryTextField, assertion.getLineBreakInterval()<=0 ? "76" : Integer.toString(assertion.getLineBreakInterval()) );
        enableDisableComponents();
    }

    //- PROTECTED

    @Override
    protected JPanel createPropertyPanel() {
        return mainPanel;
    }

        @Override
    protected void initComponents() {
        super.initComponents();

        encodeDecodeComboBox.setModel( new DefaultComboBoxModel( EncodeDecodeAssertion.TransformType.values()));
        encodeDecodeComboBox.setRenderer( new TextListCellRenderer<EncodeDecodeAssertion.TransformType>( new Functions.Unary<String,EncodeDecodeAssertion.TransformType>(){
            @Override
            public String call( final EncodeDecodeAssertion.TransformType transformType ) {
                try {
                    return bundle.getString( "transform." + transformType + ".label" );
                } catch ( MissingResourceException mre ) {
                    return transformType.toString();    
                }
            }
        } ) );

        dataTypeComboBox.setModel( new DefaultComboBoxModel( new DataType[]{ DataType.STRING, DataType.MESSAGE, DataType.CERTIFICATE } ) );
        dataTypeComboBox.setRenderer( new TextListCellRenderer<DataType>( new Functions.Unary<String,DataType>(){
            @Override
            public String call( final DataType dataType ) {
                return dataType.getName();
            }
        } ) );

        targetVariableTextField = new TargetVariablePanel();
        targetVariablePanel.setLayout(new BorderLayout());
        targetVariablePanel.add(targetVariableTextField, BorderLayout.CENTER);
        targetVariableTextField.addChangeListener(new ChangeListener(){
            @Override
            public void stateChanged(ChangeEvent e) {
                getOkButton().setEnabled(targetVariableTextField .isEntryValid());
            }
        });


        contentTypeTextField.setText( ContentTypeHeader.XML_DEFAULT.getFullValue() );
        encodingTextField.setText( ContentTypeHeader.XML_DEFAULT.getEncoding().name() );   
        lineBreakEveryTextField.setDocument( new NumberField() );

        final RunOnChangeListener enableDisableListener = new RunOnChangeListener(){
            @Override
            public void run() {
                enableDisableComponents();
            }
        };

        dataTypeComboBox.addActionListener( enableDisableListener );
        encodeDecodeComboBox.addActionListener( enableDisableListener );
        multipleLinesCheckBox.addActionListener( enableDisableListener );
        sourceVariableTextField.addActionListener( enableDisableListener );
        targetVariableTextField.addChangeListener( enableDisableListener );
        lineBreakEveryTextField.getDocument().addDocumentListener( enableDisableListener );
        contentTypeTextField.getDocument().addDocumentListener( enableDisableListener );
        encodingTextField.getDocument().addDocumentListener( enableDisableListener );
    }

    //- PRIVATE

    private static final ResourceBundle bundle = ResourceBundle.getBundle( EncodeDecodePropertiesDialog.class.getName());

    private JPanel mainPanel;
    private JComboBox encodeDecodeComboBox;
    private JTextField sourceVariableTextField;
    private TargetVariablePanel targetVariableTextField;
    private JPanel targetVariablePanel;
    private JComboBox dataTypeComboBox;
    private JTextField contentTypeTextField;
    private JTextField encodingTextField;
    private JCheckBox strictCheckBox;
    private JTextField lineBreakEveryTextField;
    private JCheckBox multipleLinesCheckBox;
    private JLabel lineBreakLabel1;
    private JLabel lineBreakLabel2;

    private void setText( final JTextComponent textComponent, final String text ) {
        if ( text != null ) {
            textComponent.setText( text );
            textComponent.setCaretPosition( 0 );
        }
    }

    private String nullIfEmpty( final JTextComponent textComponent ) {
        String value = null;
        final String text = textComponent.getText();
        if ( textComponent.isEnabled() && text != null && !text.trim().isEmpty() ) {
            value = text.trim();            
        }
        return value;
    }

    private void validateData() {
        // validation
        String message = VariableMetadata.validateName( VariablePrefixUtil.fixVariableName( sourceVariableTextField.getText() ) );

        if ( message == null && lineBreakEveryTextField.isEnabled() && !ValidationUtils.isValidInteger( lineBreakEveryTextField.getText(), false, 0, Integer.MAX_VALUE ) ) {
            message = "Invalid line break value, must be a non-negative integer.";
        }

        if ( message == null && contentTypeTextField.isEnabled() ) {
            try {
                final ContentTypeHeader contentType = ContentTypeHeader.parseValue( contentTypeTextField.getText() );
                final String charsetName = contentType.getParam("charset");
                if ( charsetName != null ) {
                    try {
                        final Charset charset = Charset.forName( charsetName );
                        if ( !charset.canEncode() ) {
                            message = "Invalid content type charset '"+charsetName+"'";
                        }
                    } catch ( IllegalArgumentException iae ) {
                        message = "Invalid content type charset '"+charsetName+"'";
                    }
                }
            } catch ( IOException e ) {
                message = "Invalid content type '"+ ExceptionUtils.getMessage( e )+"'";
            }
        }

        if ( message == null && encodingTextField.isEnabled() ) {
            try {
                final Charset charset = Charset.forName( encodingTextField.getText() );
                if ( !charset.canEncode() ) {
                    message = "Invalid encoding '"+encodingTextField.getText()+"'";
                }
            } catch ( IllegalArgumentException iae ) {
                message = "Invalid encoding '"+encodingTextField.getText()+"'";
            }
        }

        if ( message != null ) {
            throw new ValidationException( message, "Invalid Property", null );
        }
    }

    private void enableDisableComponents() {
        boolean enableAny = !isReadOnly() && targetVariableTextField.isEntryValid();

        final EncodeDecodeAssertion.TransformType transformType = (EncodeDecodeAssertion.TransformType) encodeDecodeComboBox.getSelectedItem();
        DataType targetDataType = (DataType) dataTypeComboBox.getSelectedItem();
        if ( transformType.isBinaryOutput() ) {
            dataTypeComboBox.setModel( new DefaultComboBoxModel( new DataType[]{ DataType.STRING, DataType.MESSAGE, DataType.CERTIFICATE } ) );
        } else {
            dataTypeComboBox.setModel( new DefaultComboBoxModel( new DataType[]{ DataType.STRING, DataType.MESSAGE } ) );
        }
        dataTypeComboBox.setSelectedItem( targetDataType );
        if ( dataTypeComboBox.getSelectedItem() != targetDataType ) {
            dataTypeComboBox.setSelectedItem( DataType.MESSAGE );
        }

        boolean enableEncoding = false;
        boolean enableContentType = false;
        if ( targetDataType == DataType.STRING ) {
            enableEncoding = enableAny && transformType.isBinaryOutput();
        } else if ( targetDataType == DataType.MESSAGE ) {
            enableContentType = enableAny;
        }
        if ( transformType.encodingRequired() || transformType.isBinaryInput() ) {
            enableEncoding = enableAny;    
        }
        contentTypeTextField.setEnabled(  enableContentType );
        encodingTextField.setEnabled( enableEncoding );

        strictCheckBox.setEnabled( transformType.isStrictSupported() && enableAny );

        boolean enableOutputFormat = enableAny && !transformType.isBinaryOutput();
        multipleLinesCheckBox.setEnabled( enableOutputFormat );
        boolean enableLineBreak = multipleLinesCheckBox.isEnabled() && multipleLinesCheckBox.isSelected();
        lineBreakLabel1.setEnabled( enableLineBreak );
        lineBreakEveryTextField.setEnabled( enableLineBreak );
        lineBreakLabel2.setEnabled( enableLineBreak );

        getOkButton().setEnabled( enableAny );
    }
}
