package com.l7tech.console.panels;

import com.l7tech.gateway.common.resources.ResourceEntryHeader;
import com.l7tech.gateway.common.resources.ResourceType;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.policy.exporter.ExternalSchemaReference;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.console.util.Registry;
import com.l7tech.objectmodel.FindException;
import com.l7tech.util.Functions;
import com.l7tech.util.TextUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * This wizard panel allows to administrator to take action on an unresolved external schema
 * referred to in the imported policy.
 *
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Oct 20, 2005<br/>
 */
public class ResolveExternalSchemaReferencePanel extends WizardStepPanel {
    private static final Logger logger = Logger.getLogger(ResolveExternalSchemaReferencePanel.class.getName());
    private JPanel mainPanel;
    private ExternalSchemaReference foreignRef;
    private JButton addSchemaButton;
    private JRadioButton asIsRadio;
    private JRadioButton removeRadio;
    private JRadioButton manualSelectionRadioButton;
    private JComboBox schemaSelectionComboBox;
    private JTextField tnsField;
    private JTextField nameField;
    private JLabel referenceValidityLabel;

    public ResolveExternalSchemaReferencePanel(WizardStepPanel next, ExternalSchemaReference foreignRef) {
        super(next);
        this.foreignRef = foreignRef;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        add(mainPanel);
        nameField.setText(foreignRef.getName());
        nameField.setCaretPosition( 0 );
        tnsField.setText(foreignRef.getTns());
        tnsField.setCaretPosition( 0 );
        schemaSelectionComboBox.setRenderer( new TextListCellRenderer<String>( truncatingAccessor(0, 80), truncatingAccessor(80, 4096), false ) );
        addSchemaButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCreateSchema();
            }
        });
        removeRadio.setSelected(true);
        ActionListener enableListener = new ActionListener(){
            @Override
            public void actionPerformed( final ActionEvent e ) {
                enableAndDisableComponents();
            }
        };
        asIsRadio.addActionListener( enableListener );
        removeRadio.addActionListener( enableListener );
        manualSelectionRadioButton.addActionListener( enableListener );
        loadSchemas();
        enableAndDisableComponents();
    }

    @Override
    public boolean onNextButton() {
        if (removeRadio.isSelected()) {
            foreignRef.setLocalizeDelete();
        } else if (manualSelectionRadioButton.isSelected() && !foreignRef.getName().equals(schemaSelectionComboBox.getSelectedItem())) {
            foreignRef.setLocalizeReplace((String)schemaSelectionComboBox.getSelectedItem());
        }
        return true;
    }

    private void onCreateSchema() {
        // show global schemas
        // get wizard
        Component component = this.getParent();
        while (component != null) {
            if (component instanceof Wizard) break;
            component = component.getParent();
        }
        GlobalResourcesDialog dlg = new GlobalResourcesDialog((Wizard)component);
        DialogDisplayer.display(dlg, new Runnable() {
            @Override
            public void run() {
                boolean wasEnabled = manualSelectionRadioButton.isEnabled();
                loadSchemas();
                enableAndDisableComponents();
                if ( manualSelectionRadioButton.isEnabled() && !wasEnabled ) {
                    if (removeRadio.isSelected()) manualSelectionRadioButton.setSelected( true );
                    schemaSelectionComboBox.setSelectedIndex( 0 );
                }

                final Registry reg = Registry.getDefault();
                if ( reg == null || reg.getResourceAdmin() == null ) {
                    logger.warning("No access to registry. Cannot check fix.");
                    return;
                }

                boolean fixed;
                String fixedUri = null;
                try {
                    fixed = foreignRef.getName()!=null && reg.getResourceAdmin().findResourceHeaderByUriAndType(foreignRef.getName(), ResourceType.XML_SCHEMA) != null;
                    if (!fixed) {
                        final Collection<ResourceEntryHeader> headers = reg.getResourceAdmin().findResourceHeadersByTargetNamespace(foreignRef.getTns());
                        if ( headers.size() == 1 ) {
                            fixed = true;
                            fixedUri = foreignRef.getName()!=null ? headers.iterator().next().getUri() : null;
                        }
                    }
                } catch (FindException e) {
                    logger.log(Level.SEVERE, "cannot check fix", e);
                    throw new RuntimeException(e);
                }
                if ( fixed ) {
                    if ( removeRadio.isSelected() ) {
                        if ( fixedUri != null && ((DefaultComboBoxModel)schemaSelectionComboBox.getModel()).getIndexOf( fixedUri ) >= 0) {
                            manualSelectionRadioButton.setSelected( true );
                            schemaSelectionComboBox.setSelectedItem( fixedUri );
                            referenceValidityLabel.setText( "" ); // could be valid or invalid, depends on the assertion
                        } else {
                            asIsRadio.setSelected( true );
                            referenceValidityLabel.setText( "(Valid reference)" );
                        }
                    }
                } else {
                    referenceValidityLabel.setText( "(Invalid reference)" );
                }
                enableAndDisableComponents();
            }
        });
    }

    private void loadSchemas() {
        Collection<ResourceEntryHeader> schemas;
        try {
            final Registry reg = Registry.getDefault();
            if ( reg == null || reg.getResourceAdmin() == null ) {
                logger.warning("No access to registry. Cannot load schema options.");
                return;
            }

            if ( foreignRef.getTns()==null ) {
                schemas = reg.getResourceAdmin().findResourceHeadersByType(ResourceType.XML_SCHEMA);
            } else {
                schemas = reg.getResourceAdmin().findResourceHeadersByTargetNamespace( foreignRef.getTns() );
            }
        } catch ( FindException e ) {
            logger.log(Level.WARNING, "Error finding schema options.", e);
            schemas = Collections.emptyList();
        } catch ( IllegalArgumentException iae ) {
            logger.warning("No access to registry. Cannot load schema options.");
            schemas = Collections.emptyList();
        }

        java.util.List<String> names = Functions.map( schemas, new Functions.Unary<String,ResourceEntryHeader>(){
            @Override
            public String call( final ResourceEntryHeader resourceEntryHeader ) {
                return resourceEntryHeader.getUri();
            }
        } );

        if ( foreignRef.getName()!=null ) names.remove( foreignRef.getName() );
        Collections.sort( names, String.CASE_INSENSITIVE_ORDER );

        schemaSelectionComboBox.setModel( new DefaultComboBoxModel(names.toArray( new String[names.size()] )) );
    }

    private void enableAndDisableComponents() {
        boolean enableSchemaSelection = foreignRef.getName()!=null && schemaSelectionComboBox.getModel().getSize()>0;
        manualSelectionRadioButton.setEnabled( enableSchemaSelection );
        schemaSelectionComboBox.setEnabled( enableSchemaSelection && manualSelectionRadioButton.isSelected() );
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

    private String describe( final int detailLengthRestriction ) {
        String ref =  TextUtils.truncStringMiddleExact( foreignRef.getName(), detailLengthRestriction );
        if (ref == null) {
            ref = TextUtils.truncStringMiddleExact( foreignRef.getTns(), detailLengthRestriction );
        }
        return "Unresolved external schema " + ref;
    }

    private Functions.Unary<String,String> truncatingAccessor( final int minLength, final int maxLength ) {
        return new Functions.Unary<String,String>(){
            @Override
            public String call( final String s ) {
                return s.length() > minLength ? TextUtils.truncStringMiddleExact( s, maxLength ) : null;
            }
        };
    }
}
