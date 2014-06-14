package com.l7tech.console.panels;

import com.l7tech.console.action.ManageHttpConfigurationAction;
import com.l7tech.gui.widgets.ValidatedPanel;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.gui.widgets.TextEntryPanel;
import com.l7tech.policy.variable.Syntax;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author alex
 */
public class UrlPanel extends ValidatedPanel<String> {

    //- PUBLIC

    /**
     * Create a URL panel that allows empty URLs.
     */
    public UrlPanel() {
        this("URL:", null, true);
    }

    /**
     * Create a URL panel that does not permit empty URLs.
     *
     * @param label The label to use (required)
     * @param initialValue The initial value (optional)
     */
    public UrlPanel( final String label,
                     final String initialValue ) {
        this(label, initialValue, false);
    }

    /**
     * Create a URL panel.
     *
     * @param label The label to use (required)
     * @param initialValue The initial value (optional)
     * @param emptyUrlAllowed True to allow empty URLs
     */
    public UrlPanel( final String label,
                     final String initialValue,
                     final boolean emptyUrlAllowed ) {
        super( PROPERTY_NAME );
        this.urlEntryPanel = new UrlEntryPanel( label, initialValue, emptyUrlAllowed );
        this.manageHttpOptionsButton = new JButton( new ManageHttpConfigurationAction( this ) );
        manageHttpOptionsButton.setText( "HTTP Options" );
        manageHttpOptionsButton.setIcon( null );
        init();
    }

    public String getText() {
        return urlEntryPanel.getText();
    }

    public void setText( final String s ) {
        urlEntryPanel.setText( s );
    }

    @Override
    public String getPropertyName() {
        return urlEntryPanel.getPropertyName();
    }

    @Override
    public void setPropertyName( final String propertyName ) {
        super.setPropertyName( propertyName );
        urlEntryPanel.setPropertyName( propertyName );
    }

    @Override
    public boolean isSyntaxOk() {
        return urlEntryPanel.isSyntaxOk();
    }

    public boolean isEmptyUrlAllowed() {
        return urlEntryPanel.emptyUrlAllowed;
    }

    public void setEmptyUrlAllowed(boolean emptyUrlAllowed) {
        this.urlEntryPanel.emptyUrlAllowed = emptyUrlAllowed;
    }

    public boolean isShowManageHttpOptions() {
        return manageHttpOptionsButton.isVisible();
    }

    public void setShowManageHttpOptions( final boolean showManageHttpOptions ) {
        manageHttpOptionsButton.setVisible( showManageHttpOptions );
    }

    @Override
    public void focusFirstComponent() {
        urlEntryPanel.focusFirstComponent();
    }

    public String getSemanticError( String url ) {
        return urlEntryPanel.getSemanticError( url );
    }

    public String getSyntaxError( String url ) {
        return urlEntryPanel.getSyntaxError( url );
    }

    public static class UrlEntryPanel extends TextEntryPanel {
        private boolean emptyUrlAllowed;

        public UrlEntryPanel( final String label,
                              final String initialValue,
                              final boolean emptyUrlAllowed ) {
            super(label, PROPERTY_NAME, initialValue);
            this.emptyUrlAllowed = emptyUrlAllowed;
        }

        @Override
        public String getModel() {
            return super.getModel();
        }

        @Override
        protected String getSemanticError( final String model ) {
            if (model == null || model.length() == 0) return null;
            // if the URL contains context variable, you just can't check semantic
            String[] tmp = Syntax.getReferencedNames(model, false);
            if (tmp != null && tmp.length > 0) {
                return null;
            }
            try {
                URL url = new URL(model);
                //noinspection ResultOfMethodCallIgnored
                InetAddress.getByName(url.getHost());
                return null;
            } catch (SecurityException se) {
                // if we are not permitted to resolve the address don't show
                // this as an error
                return null;
            } catch (Exception e) {
                return ExceptionUtils.getMessage(e);
            }
        }

        @Override
        protected String getSyntaxError( final String model ) {
            if (model == null || model.trim().length() == 0) {
                if (emptyUrlAllowed) return null;
                else return "empty URL";
            }
            // if the URL contains context variable, you just can't check syntax
            String[] tmp = Syntax.getReferencedNames(model, false);
            if (tmp != null && tmp.length > 0) {
                return null;
            }
            try {
                URL url = new URL(model);
                if (url.getHost() == null || url.getHost().length() == 0) {
                    return "no host";
                } else if ( model.contains( " " ) ) {
                    return "' ' must be url encoded";
                } else if ( model.contains( "\t" ) ) {
                    return "tab must be url encoded";
                } else {
                    return null;
                }
            } catch (MalformedURLException e) {
                return ExceptionUtils.getMessage(e);
            }
        }
    }

    //- PROTECTED

    @Override
    protected void initComponents() {
        add( urlEntryPanel, BorderLayout.CENTER );

        final JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout( new BoxLayout(buttonPanel, BoxLayout.Y_AXIS) );
        buttonPanel.add( manageHttpOptionsButton );
        add( buttonPanel, BorderLayout.EAST );

        // proxy property events from the text panel
        urlEntryPanel.addPropertyChangeListener( new PropertyChangeListener(){
            @Override
            public void propertyChange( final PropertyChangeEvent evt ) {
                if ( evt != null &&
                        ( getPropertyName().equals(evt.getPropertyName()) || "ok".equals(evt.getPropertyName()) ) ) {
                    UrlPanel.this.firePropertyChange(
                            evt.getPropertyName(),
                            evt.getOldValue(),
                            evt.getNewValue() );
                }
            }
        } );
    }

    @Override
    protected String getModel() {
        return urlEntryPanel.getModel();
    }

    @Override
    protected void doUpdateModel() {
    }

    //- PRIVATE

    private static final String PROPERTY_NAME = "url";

    private final UrlEntryPanel urlEntryPanel;
    private final JButton manageHttpOptionsButton;

}
