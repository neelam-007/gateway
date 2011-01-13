package com.l7tech.console.panels;

import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.policy.assertion.xmlsec.BuildRstSoapRequest;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.util.Functions;
import com.l7tech.util.SoapConstants;
import com.l7tech.util.TimeUnit;
import com.l7tech.util.ValidationUtils;
import com.l7tech.xml.soap.SoapVersion;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;

/**
 *
 */
public class BuildRstSoapRequestPropertiesDialog extends AssertionPropertiesOkCancelSupport<BuildRstSoapRequest> {

    //- PUBLIC

    /**
     * Create a new dialog with the given owner and data.
     *
     * @param owner     The owner for the dialog
     * @param assertion The assertion data
     */
    public BuildRstSoapRequestPropertiesDialog( final Window owner,
                                                final BuildRstSoapRequest assertion ) {
        super(BuildRstSoapRequest.class, owner, assertion, true);
        initComponents();
        setData(assertion);
    }

    @Override
    public BuildRstSoapRequest getData( final BuildRstSoapRequest assertion ) throws ValidationException {
        assertion.setTokenType( (SecurityTokenType)tokenTypeComboBox.getSelectedItem() );
        assertion.setSoapVersion( (SoapVersion)soapVersionComboBox.getSelectedItem() );
        assertion.setWsTrustNamespace( (String)wsTrustNsComboBox.getSelectedItem() );
        assertion.setIssuerAddress( getNonEmpty( issuerTextField ) );
        assertion.setAppliesToAddress( getNonEmpty( appliesToTextField ) );
        assertion.setKeySize( (Integer)keySizeComboBox.getSelectedItem() > 0 ? (Integer)keySizeComboBox.getSelectedItem() : null );
        if ( tokenLifetimeCheckBox.isSelected() ) {
            if ( useSystemDefaultCheckBox.isSelected() ) {
                assertion.setLifetime( BuildRstSoapRequest.SYSTEM_LIFETIME );
                assertion.setLifetimeTimeUnit( null );
            } else {
                assertion.setLifetimeTimeUnit( (TimeUnit)lifetimeTimeUnitComboBox.getSelectedItem() );
                assertion.setLifetime( (long)(((Double)tokenLifetimeTextField.getValue()) * assertion.getLifetimeTimeUnit().getMultiplier()) );
            }
        } else {
            assertion.setLifetime( null );
            assertion.setLifetimeTimeUnit( null );
        }
        assertion.setIncludeClientEntropy( includeClientEntropyCheckBox.isSelected() );
        assertion.setVariablePrefix( varPrefixTextField.getVariable() );

        return assertion;
    }

    @Override
    public void setData( final BuildRstSoapRequest assertion ) {
        selectIfNotNull( tokenTypeComboBox, assertion.getTokenType() );
        selectIfNotNull( soapVersionComboBox, assertion.getSoapVersion() );
        selectIfNotNull( wsTrustNsComboBox, assertion.getWsTrustNamespace() );
        setIfNotNull( issuerTextField, assertion.getIssuerAddress() );
        setIfNotNull( appliesToTextField, assertion.getAppliesToAddress() );
        selectIfNotNull( keySizeComboBox, assertion.getKeySize() );
        lifetimeTimeUnitComboBox.setSelectedItem( TimeUnit.HOURS );
        tokenLifetimeTextField.setValue( 2D );
        if ( assertion.getLifetime() != null ) {
            tokenLifetimeCheckBox.setSelected( true );
            if ( BuildRstSoapRequest.SYSTEM_LIFETIME == assertion.getLifetime() ) {
                useSystemDefaultCheckBox.setSelected( true );
            } else {
                useSystemDefaultCheckBox.setSelected( false );
                selectIfNotNull( lifetimeTimeUnitComboBox, assertion.getLifetimeTimeUnit() );
                tokenLifetimeTextField.setValue( ((double)assertion.getLifetime()) / ((TimeUnit)lifetimeTimeUnitComboBox.getSelectedItem()).getMultiplier() );
            }
        } else {
            tokenLifetimeCheckBox.setSelected( false );
            useSystemDefaultCheckBox.setSelected( true );
        }
        oldTimeUnit = (TimeUnit)lifetimeTimeUnitComboBox.getSelectedItem();
        includeClientEntropyCheckBox.setSelected( assertion.isIncludeClientEntropy() );

        varPrefixTextField.setAssertion( assertion );
        varPrefixTextField.setSuffixes( BuildRstSoapRequest.getVariableSuffixes() );
        varPrefixTextField.setVariable( assertion.getVariablePrefix() );

        enableOrDisableComponents();
    }

    //- PROTECTED

    @Override
    protected JPanel createPropertyPanel() {
        return mainPanel;
    }

    @Override
    protected void initComponents() {
        super.initComponents();

        // Components and layout
        varPrefixTextField = new TargetVariablePanel();
        varPrefixPanel.setLayout(new BorderLayout());
        varPrefixPanel.add(varPrefixTextField, BorderLayout.CENTER);

        // Models
        tokenTypeComboBox.setModel( new DefaultComboBoxModel( new Object[]{
                SecurityTokenType.SAML2_ASSERTION,
                SecurityTokenType.SAML_ASSERTION,
                SecurityTokenType.WSSC_CONTEXT,
        } ) );
        tokenTypeComboBox.setRenderer( new TextListCellRenderer<SecurityTokenType>( new Functions.Unary<String,SecurityTokenType>(){
            @Override
            public String call( final SecurityTokenType securityTokenType ) {
                return securityTokenType.getName();
            }
        } ) );

        soapVersionComboBox.setModel( new DefaultComboBoxModel( new Object[]{
                SoapVersion.SOAP_1_2,
                SoapVersion.SOAP_1_1,
        } ) );
        soapVersionComboBox.setRenderer( new TextListCellRenderer<SoapVersion>( new Functions.Unary<String,SoapVersion>(){
            @Override
            public String call( final SoapVersion soapVersion ) {
                return soapVersion.getVersionNumber();
            }
        } ) );

        wsTrustNsComboBox.setModel( new DefaultComboBoxModel( new Object[]{
                SoapConstants.WST_NAMESPACE3,
                SoapConstants.WST_NAMESPACE2,
                SoapConstants.WST_NAMESPACE,
        } ) );
        wsTrustNsComboBox.setRenderer( TextListCellRenderer.<Object>basicComboBoxRenderer() );

        keySizeComboBox.setModel( new DefaultComboBoxModel( new Object[]{
                   0, // use 0 instead of null since null causes UI keyboard navigation issues
                 128,
                 256,
                 512,
                1024,
        } ) );
        keySizeComboBox.setRenderer( new TextListCellRenderer<Integer>( new Functions.Unary<String,Integer>(){
            @Override
            public String call( final Integer size ) {
                return size != 0 ? Integer.toString( size ) : "<Not Included>";
            }
        }) );
        
        lifetimeTimeUnitComboBox.setModel( new DefaultComboBoxModel(TimeUnit.ALL) );

        tokenLifetimeTextField.setFormatterFactory(new JFormattedTextField.AbstractFormatterFactory() {
            @Override
            public JFormattedTextField.AbstractFormatter getFormatter(JFormattedTextField tf) {
                final NumberFormatter numberFormatter = new NumberFormatter(new DecimalFormat("0.####"));
                numberFormatter.setValueClass(Double.class);
                numberFormatter.setMinimum(0D);
                return numberFormatter;
            }
        });

        // Actions
        final RunOnChangeListener enableDisableListener = new RunOnChangeListener(){
            @Override
            protected void run() {
                enableOrDisableComponents();
            }
        };
        tokenLifetimeTextField.getDocument().addDocumentListener( enableDisableListener );
        lifetimeTimeUnitComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed( ActionEvent e) {
                final TimeUnit newTimeUnit = (TimeUnit)lifetimeTimeUnitComboBox.getSelectedItem();
                final Double time = (Double)tokenLifetimeTextField.getValue();
                if ( newTimeUnit != oldTimeUnit ) {
                    final long oldMillis = (long)(oldTimeUnit.getMultiplier() * time);
                    tokenLifetimeTextField.setValue( ((double)oldMillis) / newTimeUnit.getMultiplier() );
                }
                enableOrDisableComponents();
                oldTimeUnit = newTimeUnit;
            }
        });
        tokenLifetimeCheckBox.addActionListener( enableDisableListener );
        useSystemDefaultCheckBox.addActionListener( enableDisableListener );
        varPrefixTextField.addChangeListener( enableDisableListener );

        // Defaults
        oldTimeUnit = TimeUnit.HOURS;
        tokenLifetimeTextField.setValue( 2D );
        lifetimeTimeUnitComboBox.setSelectedItem( oldTimeUnit );

        enableOrDisableComponents();
    }

    //- PRIVATE

    private static final long MILLIS_100_YEARS = TimeUnit.DAYS.toMillis( 100 * 365 );

    private JPanel mainPanel;
    private JComboBox tokenTypeComboBox;
    private JComboBox soapVersionComboBox;
    private JComboBox wsTrustNsComboBox;
    private JTextField issuerTextField;
    private JTextField appliesToTextField;
    private JComboBox keySizeComboBox;
    private JCheckBox tokenLifetimeCheckBox;
    private JFormattedTextField tokenLifetimeTextField;
    private JComboBox lifetimeTimeUnitComboBox;
    private JCheckBox useSystemDefaultCheckBox;
    private JCheckBox includeClientEntropyCheckBox;
    private JPanel varPrefixPanel;

    private TargetVariablePanel varPrefixTextField;
    private TimeUnit oldTimeUnit;

    private void selectIfNotNull( final JComboBox component,
                                  final Object value ) {
        if ( value != null ) {
            component.setSelectedItem( value );
        }
    }

    private void setIfNotNull( final JTextComponent component,
                               final String value ) {
        if ( value != null ) {
            component.setText( value );
            component.setCaretPosition( 0 );
        }
    }

    private String getNonEmpty( final JTextComponent component ) {
        String value = null;

        if ( component.isEnabled() && component.getText()!=null && !component.getText().trim().isEmpty() ) {
            value = component.getText().trim();
        }

        return value;
    }

    private void enableOrDisableComponents() {
        if ( tokenLifetimeCheckBox.isSelected() ) {
            useSystemDefaultCheckBox.setEnabled( true );
            if ( useSystemDefaultCheckBox.isSelected() ) {
                tokenLifetimeTextField.setEnabled( false );
                lifetimeTimeUnitComboBox.setEnabled( false );
            } else {
                tokenLifetimeTextField.setEnabled( true );
                lifetimeTimeUnitComboBox.setEnabled( true );
            }
        } else {
            useSystemDefaultCheckBox.setEnabled( false );
            tokenLifetimeTextField.setEnabled( false );
            lifetimeTimeUnitComboBox.setEnabled( false );
        }
        
        int multiplier = ((TimeUnit) lifetimeTimeUnitComboBox.getSelectedItem()).getMultiplier();
        boolean validLifetime = !tokenLifetimeCheckBox.isSelected() || useSystemDefaultCheckBox.isSelected() ||
            ValidationUtils.isValidDouble(tokenLifetimeTextField.getText().trim(), false,
                0, false,
                MILLIS_100_YEARS / multiplier, true);

        getOkButton().setEnabled( !isReadOnly() && validLifetime && varPrefixTextField.isEntryValid() );
    }

}
