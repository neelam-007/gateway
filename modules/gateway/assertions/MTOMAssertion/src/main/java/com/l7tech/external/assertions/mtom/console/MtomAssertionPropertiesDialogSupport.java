package com.l7tech.external.assertions.mtom.console;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.MessageTargetableSupport;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.assertion.MessageTargetable;
import com.l7tech.policy.assertion.XpathBasedAssertion;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.variable.PolicyVariableUtils;
import com.l7tech.policy.variable.DataType;
import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.XpathBasedAssertionPropertiesDialog;
import com.l7tech.console.util.VariablePrefixUtil;
import com.l7tech.util.Functions;
import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.xml.xpath.XpathUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;

import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

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

    protected ComboBoxModel buildMessageSourceComboBoxModel( final Assertion assertion ) {
        final DefaultComboBoxModel comboBoxModel = new DefaultComboBoxModel();

        comboBoxModel.addElement( new MessageTargetableSupport( TargetMessageType.REQUEST ) );
        comboBoxModel.addElement( new MessageTargetableSupport( TargetMessageType.RESPONSE ) );

        final Map<String, VariableMetadata> predecessorVariables =
                (assertion.getParent() != null) ? PolicyVariableUtils.getVariablesSetByPredecessors( assertion ) :
                (getPreviousAssertion() != null)? PolicyVariableUtils.getVariablesSetByPredecessorsAndSelf( getPreviousAssertion() ) :
                Collections.<String, VariableMetadata>emptyMap();

        final SortedSet<String> predecessorVariableNames = new TreeSet<String>(predecessorVariables.keySet());
        for (String variableName: predecessorVariableNames) {
            if (predecessorVariables.get(variableName).getType() == DataType.MESSAGE) {
                final MessageTargetableSupport item = new MessageTargetableSupport( TargetMessageType.OTHER );
                item.setOtherTargetMessageVariable( variableName );
                comboBoxModel.addElement( item );
            }
        }

        return comboBoxModel;
    }

    protected ComboBoxModel buildMessageTargetComboBoxModel() {
        final DefaultComboBoxModel comboBoxModel = new DefaultComboBoxModel();
        comboBoxModel.addElement( null );
        comboBoxModel.addElement( new MessageTargetableSupport( TargetMessageType.REQUEST ) );
        comboBoxModel.addElement( new MessageTargetableSupport( TargetMessageType.RESPONSE ) );
        comboBoxModel.addElement( new MessageTargetableSupport( TargetMessageType.OTHER ) );
        return comboBoxModel;
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

    protected Functions.Unary<String,MessageTargetable> getMessageNameFunction( final String defaultName,
                                                                                final String variableName ) {
        return new Functions.Unary<String,MessageTargetable>(){
            @Override
            public String call( final MessageTargetable messageTargetable ) {
                if ( messageTargetable == null ) {
                    return defaultName;
                } else if ( variableName != null && messageTargetable.getTarget()== TargetMessageType.OTHER ) {
                    return variableName;
                } else {
                    return messageTargetable.getTargetName();
                }
            }
        };
    }

    protected static void editXpath( final Window parent,
                                     final String titlePrefix,
                                     final XpathExpression xpathExpression,
                                     final Functions.UnaryVoid<XpathExpression> callback ) {
        final XpathBasedAssertion holder = new XpathBasedAssertion(){};
        holder.setXpathExpression( xpathExpression != null ? xpathExpression : XpathExpression.soapBodyXpathValue() );
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
                    callback.call( pruneNamespaces(holder.getXpathExpression(), ape.getRequiredNamespaces()) );
            }
        });
    }

    //- PRIVATE

    private static XpathExpression pruneNamespaces( final XpathExpression expression, final Map<String,String> required ) {
        XpathUtil.removeNamespaces( expression.getExpression(), expression.getNamespaces(), required.keySet() );
        return expression;    
    }

}
