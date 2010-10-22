package com.l7tech.external.assertions.mtom.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.XpathBasedAssertionPropertiesDialog;
import com.l7tech.console.util.VariablePrefixUtil;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.annotation.ProcessesResponse;
import com.l7tech.util.Functions;
import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.xml.xpath.XpathUtil;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public abstract class MtomAssertionPropertiesDialogSupport<T extends Assertion> extends AssertionPropertiesOkCancelSupport<T> {

    //- PROTECTED

    protected MtomAssertionPropertiesDialogSupport( final Window parent,
                                                    final Class<T> assertionClass,
                                                    final String title ) {
        super(assertionClass, parent, title, true);
    }

    protected void selectOutputTarget( final MessageTargetable target,
                                       final JComboBox messageTargetComboBox,
                                       final JTextField messageTargetVariableNameTextField ) {
        if ( target == null ) {
            messageTargetComboBox.setSelectedItem( null );
            messageTargetVariableNameTextField.setText( "" );
        } else if ( target.getTarget() == TargetMessageType.OTHER ){
            messageTargetComboBox.setSelectedItem( new MessageTargetableSupport(TargetMessageType.OTHER) );
            messageTargetVariableNameTextField.setText( target.getTargetName() );
        } else {
            messageTargetComboBox.setSelectedItem( new MessageTargetableSupport(target) );
            messageTargetVariableNameTextField.setText( "" );
        }        
    }

    protected MessageTargetableSupport getOutputTarget( final JComboBox messageTargetComboBox,
                                                        final JTextField messageTargetVariableNameTextField ) {
        final MessageTargetableSupport target =
                new MessageTargetableSupport((MessageTargetable) messageTargetComboBox.getSelectedItem());

        if ( target.getTarget()==TargetMessageType.OTHER ) {
            target.setOtherTargetMessageVariable(
                    VariablePrefixUtil.fixVariableName( messageTargetVariableNameTextField.getText()) );
        }

        return target.getTarget()==null ? null : target;
    }

    protected static void editXpath( final Window parent,
                                     final String titlePrefix,
                                     final TargetMessageType targetMessageType,
                                     final XpathExpression xpathExpression,
                                     final Functions.UnaryVoid<XpathExpression> callback ) {
        final XpathBasedAssertion holder = targetMessageType==TargetMessageType.RESPONSE ?
                new ResponseXpathBasedAssertion() : new XpathBasedAssertion(){};
        holder.setXpathExpression( xpathExpression != null ? xpathExpression : holder.createDefaultXpathExpression(true, null) );
        final XpathBasedAssertionPropertiesDialog ape = new XpathBasedAssertionPropertiesDialog(parent, holder);
        JDialog dlg = ape.getDialog();
        dlg.setTitle(titlePrefix + " - XPath Expression");
        dlg.pack();
        dlg.setModal(true);
        Utilities.centerOnParentWindow(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            @Override
            public void run() {
                if (!ape.isConfirmed())
                    return;
                if (callback != null)
                    callback.call( pruneNamespaces(holder.getXpathExpression(), new HashMap<String,String>()) );
            }
        });
    }

    //- PRIVATE

    private static XpathExpression pruneNamespaces( final XpathExpression expression, final Map<String,String> required ) {
        XpathUtil.removeNamespaces( expression.getExpression(), expression.getNamespaces(), required.keySet() );
        return expression;    
    }

    @ProcessesResponse
    private static final class ResponseXpathBasedAssertion extends XpathBasedAssertion {}
}
