package com.l7tech.console.panels;

import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.util.TimeUnit;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.console.util.SortedListModel;

import javax.swing.*;
import java.util.ResourceBundle;
import java.util.Collection;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * Wizard step panel for LDAP Identity Provider advanced configuration.
 */
public class LdapAdvancedConfigurationPanel extends IdentityProviderStepPanel {

    //- PUBLIC

    public LdapAdvancedConfigurationPanel( final WizardStepPanel next ) {
        super( next );
        initComponents();
    }

    public LdapAdvancedConfigurationPanel( final WizardStepPanel next, final boolean readOnly ) {
        super( next, readOnly );
        initComponents();
    }

    /**
     * Provide the description for the step being taken on this panel.
     *
     * @return The description of the step.
     */
    @Override
    public String getDescription() {
        return resources.getString(RES_STEP_DESCRIPTION);
    }

    /**
     * Get the label for this step.
     *
     * @return the label    
     */
    @Override
    public String getStepLabel() {
        return resources.getString(RES_STEP_TITLE);
    }

    @Override
    public boolean canTest() {
        return true;
    }

    /**
     * Populate the configuration data from the wizard input object to the visual components of the panel.
     *
     * @param settings  The current value of configuration items in the wizard input object.
     *
     * @throws IllegalArgumentException   if the data provided by the wizard are not valid.
     */
    @Override
    public void readSettings( final Object settings ) {
        if ( settings instanceof LdapIdentityProviderConfig ) {
            readProviderConfig( (LdapIdentityProviderConfig) settings );
        }
    }

    /**
     * Store the values of all fields on the panel to the wizard object which is a used for
     * keeping all the modified values. The wizard object will be used for providing the
     * updated values when updating the server.
     *
     * @param settings the object representing wizard panel state
     */
    @Override
    public void storeSettings( final Object settings ) {
        if ( settings instanceof LdapIdentityProviderConfig ) {
            storeProviderConfig( (LdapIdentityProviderConfig) settings );
        }
    }

    //- PRIVATE
    
    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.IdentityProviderDialog");

    private static final String RES_STEP_TITLE = "advancedConfiguration.step.label";
    private static final String RES_STEP_DESCRIPTION = "advancedConfiguration.step.description";
    private static final String RES_ADD_ATTRIBUTE_TITLE = "advancedConfiguration.attrs.add.title";
    private static final String RES_EDIT_ATTRIBUTE_TITLE  = "advancedConfiguration.attrs.edit.title";
    private static final String RES_ATTRIBUTE_PROMPT  = "advancedConfiguration.attrs.prompt";

    private final SortedListModel<String> listModel = new SortedListModel<String>(String.CASE_INSENSITIVE_ORDER);

    private JPanel mainPanel;
    private JRadioButton retrieveAllAttributesRadioButton;
    private JRadioButton retrieveSpecifiedAttributesRadioButton;
    private JList attributeList;
    private JButton addButton;
    private JButton editButton;
    private JButton removeButton;
    private JTextField groupCacheSizeTextField;
    private JTextField groupCacheHierarchyMaxAgeTextField;
    private JTextField groupMaximumNestingTextField;
    private JTextField groupCacheMembershipMaxAgeTextField;
    private JComboBox hierarchyUnitcomboBox;
    private JComboBox membershipUnitComboBox;

    private void initComponents() {
        this.setLayout( new BorderLayout() );
        this.add( mainPanel, BorderLayout.CENTER );
        boolean readOnly = isReadOnly();

        groupCacheSizeTextField.setEnabled( !readOnly );
        groupCacheHierarchyMaxAgeTextField.setEnabled( !readOnly );
        groupMaximumNestingTextField.setEnabled( !readOnly );
        groupCacheMembershipMaxAgeTextField.setEnabled( !readOnly );

        hierarchyUnitcomboBox.setModel( new DefaultComboBoxModel( TimeUnit.ALL ) );
        hierarchyUnitcomboBox.setSelectedItem( TimeUnit.SECONDS );
        hierarchyUnitcomboBox.setEnabled( !readOnly );
        membershipUnitComboBox.setModel( new DefaultComboBoxModel( TimeUnit.ALL ) );
        membershipUnitComboBox.setSelectedItem( TimeUnit.SECONDS );
        membershipUnitComboBox.setEnabled( !readOnly );

        addButton.setEnabled( !readOnly );
        addButton.addActionListener( new ActionListener(){
            @Override
            public void actionPerformed( final ActionEvent e ) {
                addAttribute();
            }
        } );

        editButton.setEnabled( !readOnly );
        editButton.addActionListener( new ActionListener(){
            @Override
            public void actionPerformed( final ActionEvent e ) {
                editAttribute();
            }
        } );

        removeButton.setEnabled( !readOnly );
        removeButton.addActionListener( new ActionListener(){
            @Override
            public void actionPerformed( final ActionEvent e ) {
                removeAttribute();
            }
        } );

        attributeList.setModel( listModel );
        attributeList.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
        if (!readOnly) Utilities.setDoubleClickAction( attributeList, editButton );

        RunOnChangeListener enableDisableListener = new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                enableAndDisableComponents();
            }
        });
        attributeList.getSelectionModel().addListSelectionListener( enableDisableListener );

        retrieveAllAttributesRadioButton.setEnabled( !readOnly );
        retrieveAllAttributesRadioButton.addActionListener( enableDisableListener );
        retrieveSpecifiedAttributesRadioButton.setEnabled( !readOnly );
        retrieveSpecifiedAttributesRadioButton.addActionListener( enableDisableListener );

        enableAndDisableComponents();
    }

    private void addAttribute() {
        DialogDisplayer.showInputDialog(
                this,
                resources.getString(RES_ATTRIBUTE_PROMPT),
                resources.getString(RES_ADD_ATTRIBUTE_TITLE),
                JOptionPane.PLAIN_MESSAGE, null, null, "", new DialogDisplayer.InputListener(){
                    @Override
                    public void reportResult( final Object newAttributeObj ) {
                        String newAttribute = (String) newAttributeObj;

                        if ( newAttribute != null && ! newAttribute.trim().isEmpty() ) {
                            listModel.add( newAttribute.trim() );
                        }
                    }
                });
    }

    private void editAttribute() {
        final String selected = (String) attributeList.getSelectedValue();
        if ( selected != null ) {
                DialogDisplayer.showInputDialog(
                this,
                resources.getString(RES_ATTRIBUTE_PROMPT),
                resources.getString(RES_EDIT_ATTRIBUTE_TITLE),
                JOptionPane.PLAIN_MESSAGE, null, null, selected, new DialogDisplayer.InputListener(){
                    @Override
                    public void reportResult( final Object attributeObj ) {
                        String attribute = (String) attributeObj;

                        if ( !selected.equalsIgnoreCase(attribute) ) {
                            listModel.removeElement( selected );

                            if ( attribute != null && !attribute.trim().isEmpty() ) {
                                listModel.add( attribute.trim() );
                            }
                        }
                    }
                });
        }
    }

    private void removeAttribute() {
        String selected = (String) attributeList.getSelectedValue();
        if ( selected != null ) {
            listModel.removeElement( selected );
            enableAndDisableComponents();
        }
    }

    private void enableAndDisableComponents() {
        boolean specifiedControlsEnabled = retrieveSpecifiedAttributesRadioButton.isSelected();
        boolean attributeSelected = attributeList.getSelectedIndex() > -1;

        attributeList.setEnabled( specifiedControlsEnabled );
        addButton.setEnabled( specifiedControlsEnabled );
        editButton.setEnabled( specifiedControlsEnabled && attributeSelected );
        removeButton.setEnabled( specifiedControlsEnabled && attributeSelected );
    }

    private void readProviderConfig( final LdapIdentityProviderConfig config ) {
        String[] attributes = config.getReturningAttributes();
        if ( attributes == null ) {
            retrieveAllAttributesRadioButton.setSelected(true);
        } else {
            retrieveSpecifiedAttributesRadioButton.setSelected(true);
            listModel.clear();
            listModel.addAll( attributes );
        }
    }

    private void storeProviderConfig( final LdapIdentityProviderConfig config ) {
        if ( retrieveAllAttributesRadioButton.isSelected() ) {
            config.setReturningAttributes( null );
        } else {
            Collection<String> attributes = listModel.toList();
            config.setReturningAttributes( attributes.toArray(new String[attributes.size()]) );
        }
    }

}
