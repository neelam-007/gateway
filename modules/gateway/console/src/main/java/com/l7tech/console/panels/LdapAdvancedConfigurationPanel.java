package com.l7tech.console.panels;

import com.l7tech.gui.NumberField;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.util.TimeUnit;
import com.l7tech.util.ValidationUtils;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.console.util.SortedListModel;

import javax.swing.*;
import javax.swing.text.NumberFormatter;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ResourceBundle;
import java.util.Collection;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.logging.Logger;

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
        return maxAgeGreater100Years ?
                " <html>"+resources.getString(RES_STEP_DESCRIPTION)+"<p>"+resources.getString(RES_MAX_CACHE_AGE_ERROR)+"</p></html>" :
                resources.getString(RES_STEP_DESCRIPTION);
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

    @Override
    public boolean canAdvance() {
        return isValid;
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
    private static final String RES_MAX_CACHE_AGE_ERROR  =  "advancedConfiguration.max.cache.age.error";

    private static final int DEFAULT_GROUP_CACHE_SIZE = 0;
    private static final long DEFAULT_GROUP_CACHE_HIERARCHY_MAXAGE = 60000L;
    /**
     * This is for backwards compatibility -> if no value is found, then 0 is assumed.
     */
    private static final int DEFAULT_GROUP_MAX_NESTING = 0;

    private static final long MILLIS_100_YEARS = 100L * 365L * 86400L * 1000L;

    private final SortedListModel<String> listModel = new SortedListModel<String>(String.CASE_INSENSITIVE_ORDER);

    private boolean isValid = true;

    private JPanel mainPanel;
    private JRadioButton retrieveAllAttributesRadioButton;
    private JRadioButton retrieveSpecifiedAttributesRadioButton;
    private JList attributeList;
    private JButton addButton;
    private JButton editButton;
    private JButton removeButton;
    private JTextField groupCacheSizeTextField;
    private JTextField groupMaximumNestingTextField;
    private JComboBox hierarchyUnitcomboBox;
    private JCheckBox groupMembershipCaseInsensitive;
    private JFormattedTextField groupCacheHierarchyMaxAgeTextField;
    private TimeUnit oldTimeUnit;
    private boolean maxAgeGreater100Years = false;

     private final Logger logger = Logger.getLogger(LdapAdvancedConfigurationPanel.class.getName());
    private void initComponents() {
        this.setLayout( new BorderLayout() );
        this.add( mainPanel, BorderLayout.CENTER );
        boolean readOnly = isReadOnly();

        groupCacheSizeTextField.setEnabled( !readOnly );
        groupCacheHierarchyMaxAgeTextField.setEnabled( !readOnly );
        groupMaximumNestingTextField.setEnabled( !readOnly );
        groupMembershipCaseInsensitive.setEnabled( !readOnly );

        groupCacheSizeTextField.setDocument(new NumberField(Integer.MAX_VALUE));
        groupMaximumNestingTextField.setDocument(new NumberField(Integer.MAX_VALUE));

        final NumberFormatter numberFormatter = new NumberFormatter(new DecimalFormat("0.####"));
        numberFormatter.setValueClass(Double.class);
        numberFormatter.setMinimum(0.0);
        numberFormatter.setAllowsInvalid(false);
        groupCacheHierarchyMaxAgeTextField.setFormatterFactory(new JFormattedTextField.AbstractFormatterFactory() {
            @Override
            public JFormattedTextField.AbstractFormatter getFormatter(JFormattedTextField tf) {
                return numberFormatter;
            }
        });
         groupCacheHierarchyMaxAgeTextField.getDocument().addDocumentListener(  new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                try {
                    groupCacheHierarchyMaxAgeTextField.commitEdit();
                } catch (ParseException e) {
                   // ignore 
                }
            }
        } ));
        groupCacheHierarchyMaxAgeTextField.addPropertyChangeListener("value",new PropertyChangeListener(){
            @Override
            public void propertyChange(PropertyChangeEvent evt){
                validateComponents();
            }
        });

        RunOnChangeListener validationListener = new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                validateComponents();
            }
        });

        groupCacheSizeTextField.getDocument().addDocumentListener( validationListener );
        groupMaximumNestingTextField.getDocument().addDocumentListener( validationListener );

        oldTimeUnit = TimeUnit.MINUTES ;
        hierarchyUnitcomboBox.setModel( new DefaultComboBoxModel( TimeUnit.ALL ) );
        hierarchyUnitcomboBox.setSelectedItem( oldTimeUnit);
        hierarchyUnitcomboBox.setEnabled( !readOnly );

        hierarchyUnitcomboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                TimeUnit newTimeUnit = (TimeUnit)hierarchyUnitcomboBox.getSelectedItem();
                if ( newTimeUnit != null && oldTimeUnit != null &&  newTimeUnit != oldTimeUnit) {
                    Double time = (Double)groupCacheHierarchyMaxAgeTextField.getValue();
                    long oldMillis = (long)(oldTimeUnit.getMultiplier() * time);
                    groupCacheHierarchyMaxAgeTextField.setValue((double) oldMillis / newTimeUnit.getMultiplier());
                }
                validateComponents();
                oldTimeUnit = newTimeUnit;
            }
        });

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

    private void validateComponents() {
        boolean valid = true;
        int multiplier = ((TimeUnit)hierarchyUnitcomboBox.getSelectedItem()).getMultiplier();
        Double val = (Double) groupCacheHierarchyMaxAgeTextField.getValue();
        maxAgeGreater100Years =   val == null || val > MILLIS_100_YEARS/multiplier;
        boolean groupCacheSize = !ValidationUtils.isValidInteger(groupCacheSizeTextField.getText(), false, 0, Integer.MAX_VALUE);
        boolean nesting = !ValidationUtils.isValidInteger(groupMaximumNestingTextField.getText(), false, 0, Integer.MAX_VALUE);
        if ( groupCacheSize ||
             maxAgeGreater100Years ||
                nesting) {
            valid = false;
        }

        groupCacheHierarchyMaxAgeTextField.setToolTipText( maxAgeGreater100Years? resources.getString(RES_MAX_CACHE_AGE_ERROR) : null);

        if ( isValid != valid ) {
            isValid = valid;
            notifyListeners();            
        }
    }

    private Integer getInteger( final JTextField textField ) {
        return getInteger( textField , 1 );
    }


    private Integer getInteger( final JTextField textField, final int multiplier ) {
        Integer value = null;

        if ( textField != null ) {
            try {
                value = Integer.parseInt( textField.getText() ) * multiplier;
            } catch ( NumberFormatException nfe ) {
                //                 
            }
        }

        return value;
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
        Integer groupCacheSize = config.getGroupCacheSize();
        Long groupHierarchyMaxAge = config.getGroupCacheMaxAge();
        Integer groupMaxNesting = config.getGroupMaxNesting();
        TimeUnit maxAgeUnit = config.getGroupCacheMaxAgeUnit();

        if ( groupCacheSize==null ) groupCacheSize = DEFAULT_GROUP_CACHE_SIZE;
        if ( groupHierarchyMaxAge==null ) groupHierarchyMaxAge = DEFAULT_GROUP_CACHE_HIERARCHY_MAXAGE;
        if ( groupMaxNesting==null ) groupMaxNesting = DEFAULT_GROUP_MAX_NESTING;

        groupCacheSizeTextField.setText( Integer.toString(groupCacheSize) );
        groupCacheHierarchyMaxAgeTextField.setValue( (double)groupHierarchyMaxAge / oldTimeUnit.getMultiplier() );
        hierarchyUnitcomboBox.setSelectedItem(maxAgeUnit);
        groupMaximumNestingTextField.setText( Integer.toString(groupMaxNesting) );
        groupMembershipCaseInsensitive.setSelected( config.isGroupMembershipCaseInsensitive() );

        String[] attributes = config.getReturningAttributes();
        if ( attributes == null ) {
            retrieveAllAttributesRadioButton.setSelected(true);
        } else {
            retrieveSpecifiedAttributesRadioButton.setSelected(true);
            listModel.clear();
            listModel.addAll( attributes );
        }

        validateComponents();
    }

    private void storeProviderConfig( final LdapIdentityProviderConfig config ) {
        config.setGroupCacheSize( getInteger(groupCacheSizeTextField) );
        Double maxAge = (Double)groupCacheHierarchyMaxAgeTextField.getValue()* ((TimeUnit)hierarchyUnitcomboBox.getSelectedItem()).getMultiplier();
        config.setGroupCacheMaxAge( maxAge.longValue() );  
        config.setGroupCacheMaxAgeUnit((TimeUnit) hierarchyUnitcomboBox.getSelectedItem());
        config.setGroupMaxNesting( getInteger(groupMaximumNestingTextField) );
        config.setGroupMembershipCaseInsensitive(groupMembershipCaseInsensitive.isSelected());

        if ( retrieveAllAttributesRadioButton.isSelected() ) {
            config.setReturningAttributes( null );
        } else {
            Collection<String> attributes = listModel.toList();
            config.setReturningAttributes( attributes.toArray(new String[attributes.size()]) );
        }
    }

}
