package com.l7tech.console.panels;

import com.l7tech.console.event.WizardAdapter;
import com.l7tech.console.event.WizardEvent;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.resources.ResourceEntryHeader;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.exporter.GlobalResourceReference;
import com.l7tech.util.TextUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wizard step panel for resolving global resources.
 *
 * <p>Currently this is only used for DTDs, XML Schemas use the existing
 * "legacy" panel.</p>
 */
public class ResolveGlobalResourcePanel extends WizardStepPanel {
    private static final Logger logger = Logger.getLogger( ResolveGlobalResourcePanel.class.getName());
    private JPanel mainPanel;
    private GlobalResourceReference foreignRef;
    private JButton addGlobalResourceButton;
    private JRadioButton asIsRadio;
    private JRadioButton removeRadio;
    private JTextField detailsTextField;
    private JTextField systemIdTextField;
    private JLabel referenceValidityLabel;

    public ResolveGlobalResourcePanel( final WizardStepPanel next,
                                       final GlobalResourceReference foreignRef ) {
        super(next);
        this.foreignRef = foreignRef;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        add(mainPanel);
        systemIdTextField.setText(foreignRef.getSystemIdentifier());
        systemIdTextField.setCaretPosition( 0 );
        detailsTextField.setText(getDetails(foreignRef, Integer.MAX_VALUE));
        detailsTextField.setCaretPosition( 0 );
        addGlobalResourceButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed( ActionEvent e) {
                onCreateGlobalResource();
            }
        });
        removeRadio.setSelected(true);
    }

    @Override
    protected void setOwner( final JDialog owner ) {
        super.setOwner( owner );

        if ( owner instanceof Wizard ) {
            ((Wizard)owner).addWizardListener( new WizardAdapter(){
                @Override
                public void wizardSelectionChanged( final WizardEvent e ) {
                    if ( e.getSource() == ResolveGlobalResourcePanel.this ) {
                        // in case the DTD was already added
                        // in a preceding step
                        checkReferenceValidity();
                    }
                }
            } );
        }        
    }

    @Override
    public boolean onNextButton() {
        if ( removeRadio.isSelected() ) {
            foreignRef.setLocalizeDelete();
        }
        return true;
    }

    @Override
    public String getDescription() {
        return describe( 1024 );
    }

    @Override
    public String getStepLabel() {
        return describe( 40 );
    }

    @Override
    public boolean canFinish() {
        return !hasNextPanel();
    }

    private void onCreateGlobalResource() {
        final GlobalResourcesDialog dlg = new GlobalResourcesDialog(SwingUtilities.getWindowAncestor(this));
        DialogDisplayer.display(dlg, new Runnable() {
            @Override
            public void run() {
                checkReferenceValidity();
            }
        });
    }

    private void checkReferenceValidity() {
        final Registry reg = Registry.getDefault();
        if ( reg == null || reg.getResourceAdmin() == null ) {
            logger.warning("No access to registry. Cannot check fix.");
            return;
        }

        boolean fixed;
        try {
            fixed = foreignRef.getSystemIdentifier()!=null &&
                    reg.getResourceAdmin().findResourceHeaderByUriAndType(foreignRef.getSystemIdentifier(), foreignRef.getType()) != null;
            if ( !fixed ) {
                final Collection<ResourceEntryHeader> headers =
                        reg.getResourceAdmin().findResourceHeadersByKeyAndType(foreignRef.getResourceKey1(), foreignRef.getType());
                if ( headers.size() == 1 ) {
                    fixed = true;
                }
            }
        } catch ( FindException e) {
            logger.log( Level.SEVERE, "cannot check fix", e);
            throw new RuntimeException(e);
        }

        if ( fixed ) {
            if ( removeRadio.isSelected() ) {
                asIsRadio.setSelected( true );
            }
            referenceValidityLabel.setText( "(Valid reference)" );
        } else {
            referenceValidityLabel.setText( "(Invalid reference)" );
        }
    }

    private String describe( final int detailLengthRestriction ) {
        String ref =  TextUtils.truncStringMiddleExact( foreignRef.getSystemIdentifier(), detailLengthRestriction );
        if (ref == null) {
            ref = getDetails(foreignRef, detailLengthRestriction);
        }
        return "Unresolved global resource reference " + ref;
    }

    private String getDetails( final GlobalResourceReference reference,
                               final int maxLength ) {
        String detail = "";
        if ( reference.getResourceKey1() != null ) {
            switch ( reference.getType() ) {
                case XML_SCHEMA:
                    detail = "TNS: " + TextUtils.truncStringMiddleExact(reference.getResourceKey1(), maxLength-5);
                    break;
                case DTD:
                    detail = "Public ID: " + TextUtils.truncStringMiddleExact(reference.getResourceKey1(), maxLength-11);
                    break;
            }
        }
        return detail;
    }
}
