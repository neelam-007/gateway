package com.l7tech.console.panels;

import com.l7tech.console.policy.SsmPolicyVariableUtils;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.util.Functions;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Collections;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Support class for AssertionPropertiesEditor implementations.
 *
 * @author steve
 */
public abstract class AssertionPropertiesEditorSupport<AT extends Assertion> extends JDialog implements AssertionPropertiesEditor<AT> {

    //- PUBLIC

    /**
     * @deprecated use {@link AssertionPropertiesEditorSupport#AssertionPropertiesEditorSupport(Window, String, ModalityType) AssertionPropertiesEditorSupport}
     */
    @Deprecated
    public AssertionPropertiesEditorSupport( Frame owner, String title, boolean modal ) {
        super( owner, title, modal ? AssertionPropertiesOkCancelSupport.DEFAULT_MODALITY_TYPE : ModalityType.MODELESS );
        init();
    }

    /**
     * @deprecated use {@link AssertionPropertiesEditorSupport#AssertionPropertiesEditorSupport(Window, String, ModalityType) AssertionPropertiesEditorSupport}
     */
    @Deprecated
    public AssertionPropertiesEditorSupport( Dialog owner, String title, boolean modal ) {
        super( owner, title, modal ? AssertionPropertiesOkCancelSupport.DEFAULT_MODALITY_TYPE : ModalityType.MODELESS );
        init();
    }

    /**
     * Create a modal dialog.
     *
     * @param owner The owning window
     * @param title The title for the dialog
     */
    public AssertionPropertiesEditorSupport( Window owner, String title ) {
        super( owner, title, AssertionPropertiesEditorSupport.DEFAULT_MODALITY_TYPE );
        init();
    }

    public AssertionPropertiesEditorSupport( Window owner, Assertion assertion) {
        super( owner, assertion.meta().get(AssertionMetadata.PROPERTIES_ACTION_NAME).toString()
                , AssertionPropertiesEditorSupport.DEFAULT_MODALITY_TYPE );
        init();
    }

    public AssertionPropertiesEditorSupport( Window owner, String title, ModalityType modalityType ) {
        super( owner, title, modalityType );
        init();
    }

    public AssertionPropertiesEditorSupport( Window owner, Assertion assertion, ModalityType modalityType ) {
        super( owner, assertion.meta().get(AssertionMetadata.PROPERTIES_ACTION_NAME).toString() , modalityType );
        init();
    }

    @Override
    public Object getParameter( final String name ) {
        Object value = null;

        if ( PARAM_READONLY.equals( name )) {
            value = readOnly;          
        }

        return value;
    }

    @Override
    public void setParameter( final String name, Object value ) {
        if ( PARAM_READONLY.equals( name ) && value instanceof Boolean ) {
            readOnly = (Boolean) value;          
        }
    }

    @Override
    public JDialog getDialog() {
        return this;
    }

    /**
     * The policy position is set when the assertion about to be added to a policy.
     *
     * <p>The policy position can be used to extract contextual information from a
     * policy (such as variables that are set before this assertion)</p>
     *
     * @param parentAssertion The assertion that will be the parent of this assertion.
     * @param insertPosition The index that will be used to add this assertion to the parent
     */
    public void setPolicyPosition( final Assertion parentAssertion, final int insertPosition ) {
        this.parentAssertion = parentAssertion;
        this.insertPosition = insertPosition;
    }

    //- PROTECTED

    protected AssertionPropertiesEditorSupport() {
    }

    /**
     * Is this a read only view.
     *
     * @return true if read only
     */
    protected boolean isReadOnly() {
        return readOnly;
    }

    /**
     * Can be overridden to configure the view prior to display.
     *
     * @see #isReadOnly 
     */
    protected void configureView(){}

    /**
     * Get the assertion prior to this assertion in the policy.
     *
     * <p>This will only be available when adding an assertion to the policy.</p>
     *
     * @return The previous assertion (null if there is none)
     */
    @SuppressWarnings({ "unchecked" })
    protected Assertion getPreviousAssertion() {
        Assertion assertion = null;

        if ( parentAssertion instanceof CompositeAssertion ) {
            CompositeAssertion compositeAssertion = (CompositeAssertion) parentAssertion;
            java.util.List<Assertion> children = compositeAssertion.getChildren();
            //children should never be null
            if(children == null || children.isEmpty()) return parentAssertion;

            if (insertPosition == 0) return parentAssertion;
            else if(children.size() > (insertPosition-1)) return children.get(insertPosition-1);
            else return children.get(children.size() - 1);
        }

        return assertion;
    }

    protected ComboBoxModel buildMessageSourceComboBoxModel( final Assertion assertion ) {
        final DefaultComboBoxModel comboBoxModel = new DefaultComboBoxModel();

        comboBoxModel.addElement( new MessageTargetableSupport( TargetMessageType.REQUEST ) );
        comboBoxModel.addElement( new MessageTargetableSupport( TargetMessageType.RESPONSE ) );

        final Map<String, VariableMetadata> predecessorVariables =
                (assertion.getParent() != null) ? SsmPolicyVariableUtils.getVariablesSetByPredecessors( assertion ) :
                (getPreviousAssertion() != null)? SsmPolicyVariableUtils.getVariablesSetByPredecessorsAndSelf( getPreviousAssertion() ) :
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

    protected Functions.Unary<String, MessageTargetable> getMessageNameFunction( final String defaultName,
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

    //- PRIVATE

    private boolean readOnly = false;
    private Assertion parentAssertion;
    private int insertPosition;

    private void init() {
        this.setDefaultCloseOperation( JDialog.DISPOSE_ON_CLOSE );
        this.addWindowListener( new WindowAdapter(){
            @Override
            public void windowOpened( final WindowEvent e ) {
                configureView();
            }
        } );
    }
}
