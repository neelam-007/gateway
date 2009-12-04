package com.l7tech.external.assertions.mtom.console;

import com.l7tech.gui.widgets.ValidatedPanel;
import com.l7tech.gui.NumberField;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.external.assertions.mtom.MtomValidateAssertion;
import static com.l7tech.external.assertions.mtom.console.MtomAssertionPropertiesDialogSupport.editXpath;
import com.l7tech.util.Functions;
import com.l7tech.util.ValidationUtils;
import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.policy.assertion.TargetMessageType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.ResourceBundle;

/**
 *
 */
class ValidationRulePanel extends ValidatedPanel<MtomValidateAssertion.ValidationRule> {

    //- PUBLIC

    @Override
    public void focusFirstComponent() {
        itemCountTextField.requestFocus();
    }    

    //- PACKAGE

    ValidationRulePanel( final MtomValidateAssertion.ValidationRule rule, final TargetMessageType target ) {
        this.rule = rule;
        this.target = target;
        if ( rule.getXpathExpression() != null ) {
            expression = new XpathExpression();
            expression.setExpression( rule.getXpathExpression().getExpression() );
            expression.setNamespaces( rule.getXpathExpression().getNamespaces() );
        }
        setStatusLabel( statusLabel );
        init();
        checkSyntax();
    }

    //- PROTECTED

    @Override
    protected MtomValidateAssertion.ValidationRule getModel() {
        return updateFromView( new MtomValidateAssertion.ValidationRule() );
    }

    @Override
    protected void initComponents() {
        itemCountTextField.setDocument( new NumberField(7) );
        itemSizeTextField.setDocument( new NumberField(16) );

        final RunOnChangeListener listener = new RunOnChangeListener(){
            @Override
            public void run() {
                checkSyntax();
            }
        };
        itemCountTextField.getDocument().addDocumentListener( listener );
        itemSizeTextField.getDocument().addDocumentListener( listener );

        editButton.addActionListener( new ActionListener(){
            @Override
            public void actionPerformed( final ActionEvent e ) {
                doEditXPath();
            }
        } );

        xpathTextField.setText( rule.getXpathExpression()!=null ? rule.getXpathExpression().getExpression() : "" );
        xpathTextField.setCaretPosition( 0 );
        itemCountTextField.setText( Integer.toString(rule.getCount()) );
        itemSizeTextField.setText( Long.toString(rule.getSize() / 1024L) );

        add( mainPanel, BorderLayout.CENTER );
    }

    @Override
    protected void doUpdateModel() {
        updateFromView( rule );
    }

    @Override
    protected String getSyntaxError( final MtomValidateAssertion.ValidationRule model ) {
        if ( model.getXpathExpression() == null ) return bundle.getString( "error.xpath" );
        if ( model.getCount() < 0 ) return bundle.getString( "error.count" );
        if ( model.getSize() < 0 ) return bundle.getString( "error.size" );
        return null;
    }

    //- PRIVATE

    private static final ResourceBundle bundle = ResourceBundle.getBundle( MtomValidateAssertionPropertiesDialog.class.getName());

    private JPanel mainPanel;
    private JTextField xpathTextField;
    private JTextField itemCountTextField;
    private JTextField itemSizeTextField;
    private JButton editButton;
    private JLabel statusLabel;

    private final MtomValidateAssertion.ValidationRule rule;
    private final TargetMessageType target;
    private XpathExpression expression;

    private void doEditXPath() {
        editXpath(
                SwingUtilities.getWindowAncestor(this),
                bundle.getString( "dialog.title" ),
                target,
                expression,
                new Functions.UnaryVoid<XpathExpression>(){
            @Override
            public void call( final XpathExpression xpathExpression ) {
                expression = xpathExpression;
                xpathTextField.setText( xpathExpression.getExpression() );
                xpathTextField.setCaretPosition( 0 );
                checkSyntax();
            }
        } );
    }

    private MtomValidateAssertion.ValidationRule updateFromView( final MtomValidateAssertion.ValidationRule rule ) {
        rule.setXpathExpression( expression );

        rule.setCount( -1 );
        if ( ValidationUtils.isValidInteger( itemCountTextField.getText().trim(), false, 0, Integer.MAX_VALUE ) ) {
            rule.setCount( Integer.parseInt( itemCountTextField.getText().trim() ) );
        }

        rule.setSize( -1 );
        if ( ValidationUtils.isValidLong( itemSizeTextField.getText().trim(), false, 0, Long.MAX_VALUE ) ) {
            rule.setSize( Long.parseLong( itemSizeTextField.getText().trim() ) * 1024L );
        }

        return rule;
    }
}
