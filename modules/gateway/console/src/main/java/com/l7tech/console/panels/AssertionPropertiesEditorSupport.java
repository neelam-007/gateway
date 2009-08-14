package com.l7tech.console.panels;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;

import javax.swing.JDialog;
import java.awt.*;

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
    }

    /**
     * @deprecated use {@link AssertionPropertiesEditorSupport#AssertionPropertiesEditorSupport(Window, String, ModalityType) AssertionPropertiesEditorSupport}
     */
    @Deprecated
    public AssertionPropertiesEditorSupport( Dialog owner, String title, boolean modal ) {
        super( owner, title, modal ? AssertionPropertiesOkCancelSupport.DEFAULT_MODALITY_TYPE : ModalityType.MODELESS );
    }

    /**
     * Create a modal dialog.
     *
     * @param owner The owning window
     * @param title The title for the dialog
     */
    public AssertionPropertiesEditorSupport( Window owner, String title ) {
        super( owner, title, AssertionPropertiesEditorSupport.DEFAULT_MODALITY_TYPE );
    }

    public AssertionPropertiesEditorSupport( Window owner, String title, ModalityType modalityType ) {
        super( owner, title, modalityType );
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

    @Override
    public void setVisible( boolean b ) {
        configureView();
        super.setVisible( b );
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
            java.util.List<Assertion> children = (java.util.List<Assertion>) compositeAssertion.getChildren();
            //children should never be null
            if(children.isEmpty()) return assertion;

            if(children.size() > (insertPosition-1) ) return children.get(insertPosition-1);
            else return children.get(children.size() - 1);
        }

        return assertion;
    }

    //- PRIVATE

    private boolean readOnly = false;
    private Assertion parentAssertion;
    private int insertPosition;
}
