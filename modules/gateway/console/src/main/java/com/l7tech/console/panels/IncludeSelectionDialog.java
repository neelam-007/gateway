package com.l7tech.console.panels;

import com.l7tech.policy.PolicyHeader;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.Include;
import com.l7tech.util.Resolver;
import com.l7tech.util.ResolvingComparator;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.admin.LicenseRuntimeException;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.*;

/**
 * JDialog for selection of a Policy Include Fragment.
 *
 * <p>When OK'd the dialog will allow access to a PolicyHeader, if cancelled
 * the header will be null.</p>
 */
public class IncludeSelectionDialog extends JDialog {

    //- PUBLIC

    /**
     * Create a modal dialog with the given parent.
     *
     * @param parent The parent window.
     */
    public IncludeSelectionDialog( final Window parent  ) {
        super( parent, ModalityType.DOCUMENT_MODAL );
        setTitle(new Include().meta().get(AssertionMetadata.PROPERTIES_ACTION_NAME).toString());
        setContentPane(contentPane);
        getRootPane().setDefaultButton(buttonOK);
        Utilities.setEscKeyStrokeDisposes(this);

        DefaultListModel model = new DefaultListModel();
        for ( PolicyHeader header : loadPolicyFragments() ) {
            model.addElement( header );
        }
        policyFragmentList.setModel( model );
        policyFragmentList.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );

        buttonOK.addActionListener( new ActionListener(){
            @Override
            public void actionPerformed( final ActionEvent actionEvent ) {
                selectedHeader = (PolicyHeader) policyFragmentList.getSelectedValue();
                IncludeSelectionDialog.this.dispose();
            }
        } );
        buttonCancel.addActionListener( new ActionListener(){
            @Override
            public void actionPerformed( final ActionEvent actionEvent ) {
                IncludeSelectionDialog.this.dispose();
            }
        } );

        updateEnabledStates();
        policyFragmentList.addListSelectionListener( new ListSelectionListener() {
            @Override
            public void valueChanged( final ListSelectionEvent listSelectionEvent ) {
                updateEnabledStates();
            }
        } );

        pack();
        Utilities.setDoubleClickAction( policyFragmentList, buttonOK );
        Utilities.centerOnParentWindow( this );
    }

    /**
     * Check if any policy include assertions are available.
     *
     * <p>This can be used to avoid display of an empty list.</p>
     *
     * @return true if fragments are available for selection.
     */
    public boolean includeFragmentsAvailable() {
        return policyFragmentList.getModel().getSize() > 0;
    }

    /**
     * Get the policy fragment that was selected.
     *
     * @return The selected policy header (or null)
     */
    public PolicyHeader getSelectedPolicyFragment(){
        return selectedHeader;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( IncludeSelectionDialog.class.getName() );

    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JList policyFragmentList;
    private PolicyHeader selectedHeader = null;

    private void updateEnabledStates() {
        buttonOK.setEnabled( policyFragmentList.getSelectedValue()!=null );
    }

    private Collection<PolicyHeader> loadPolicyFragments() {
        List<PolicyHeader> headers = Collections.emptyList();

        try {
            headers = new ArrayList<PolicyHeader>(Registry.getDefault().getPolicyAdmin().findPolicyHeadersByType(PolicyType.INCLUDE_FRAGMENT));
            Resolver<PolicyHeader, String> resolver = new Resolver<PolicyHeader, String>(){
                @Override
                public String resolve( final PolicyHeader key ) {
                    return key.getName().toLowerCase();
                }
            };
            //noinspection unchecked
            Comparator<PolicyHeader> comp = new ResolvingComparator(resolver, false);
            Collections.sort( headers, comp );
        } catch ( LicenseRuntimeException e ) {
            logger.info("Can't load policies at this time");
        } catch ( ObjectModelException ome ) {
            logger.log( Level.WARNING, "Error loading policy fragments.", ome);
            DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(),
                                             "An unexpected error occurred when loading Policy Fragments.",
                                             "Error Loading Policy Fragments",
                                             JOptionPane.WARNING_MESSAGE,
                                             null );
        }

        return headers;
    }

}
