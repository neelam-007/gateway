package com.l7tech.console.panels;

import com.l7tech.policy.assertion.Assertion;

import javax.swing.JDialog;
import java.awt.*;

/**
 * Support class for AssertionPropertiesEditor implementations.
 *
 * @author steve
 */
public abstract class AssertionPropertiesEditorSupport<AT extends Assertion> extends JDialog implements AssertionPropertiesEditor<AT> {

    //- PUBLIC

    public AssertionPropertiesEditorSupport( Frame owner, String title, boolean modal ) {
        super( owner, title, modal );
    }

    public AssertionPropertiesEditorSupport( Dialog owner, String title, boolean modal ) {
        super( owner, title, modal );
    }

    public AssertionPropertiesEditorSupport( Window owner, String title ) {
        super( owner, title, AssertionPropertiesEditorSupport.DEFAULT_MODALITY_TYPE );
    }

    public AssertionPropertiesEditorSupport( Window owner, String title, ModalityType modalityType ) {
        super( owner, title, modalityType );
    }

    protected AssertionPropertiesEditorSupport() {
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

    //- PROTECTED

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

    //- PRIVATE

    private boolean readOnly = false;
}
