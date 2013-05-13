package com.l7tech.console.panels;

import com.l7tech.console.panels.SinkConfigurationFilterSelectionDialog.FilterContext;
import com.l7tech.console.panels.SinkConfigurationFilterSelectionDialog.FilterSelection;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.SecurityZoneWidget;
import com.l7tech.gateway.common.log.GatewayDiagnosticContextKeys;
import com.l7tech.gateway.common.log.LogSinkAdmin;
import com.l7tech.gateway.common.log.SinkConfiguration;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gui.util.*;
import com.l7tech.gui.widgets.OkCancelDialog;
import com.l7tech.gui.widgets.TextEntryPanel;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.util.*;
import com.l7tech.util.Functions.Binary;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.gateway.common.log.SinkConfiguration.SeverityThreshold;
import static com.l7tech.gateway.common.log.SinkConfiguration.SinkType;
import static com.l7tech.util.Option.some;

/**
 * This is the dialog for updating the properties of a log sink configuration.
 */
public class SinkConfigurationPropertiesDialog extends JDialog {

    private static final Logger logger = Logger.getLogger( SinkConfigurationPropertiesDialog.class.getName() );

    public static final String VALID_NAME_CHARACTERS = ValidationUtils.ALPHA_NUMERIC + "_-";

    /**
     * The title for the dialog window
     */
    private static final String DIALOG_TITLE = "Log Sink Properties";

    /**
     * Resource bundle with default locale
     */
    private ResourceBundle resources = null;

    private JPanel contentPane;
    private JTabbedPane tabbedPane;
    private JButton okButton;
    private JButton cancelButton;
    private JTextField nameField;
    private JCheckBox enabledField;
    private JTextField descriptionField;
    private JComboBox typeField;
    private JComboBox severityField;
    private JSpinner fileMaxSizeField;
    private JSpinner fileLogCount;
    private JComboBox syslogProtocolField;
    private JSpinner syslogFacilityField;
    private JComboBox syslogCharsetField;
    private JComboBox syslogTimezoneField;
    private JCheckBox syslogLogHostnameField;
    private JComboBox fileFormatField;
    private JButton syslogTestMessageButton;
    private JButton syslogHostAdd;
    private JButton syslogHostRemove;
    private JButton syslogHostEdit;
    private JList syslogHostList;
    private DefaultListModel syslogHostListModel;
    private JCheckBox syslogSSLClientAuthenticationCheckBox;
    private PrivateKeysComboBox syslogSSLKeystoreComboBox;
    private JButton syslogHostUp;
    private JButton syslogHostDown;
    private JPanel syslogSSLSettingsPanel;
    private JLabel syslogSSLKeystoreLabel;
    private JLabel syslogSSLSettingsLabel;
    private JButton addFilterButton;
    private JButton removeFilterButton;
    private JList filtersList;
    private JCheckBox rollingLogFileField;
    private JComboBox rollingIntervalField;
    private JComboBox syslogFormatField;
    private SecurityZoneWidget zoneControl;

    private final FilterContext filterContext;
    private final SinkConfiguration sinkConfiguration;
    private final boolean readOnly;
    private InputValidator inputValidator;
    private int testCount = 0;
    private boolean confirmed = false;
    private List<FilterInfo> filterList = new ArrayList<FilterInfo>();

    private long maximumLogFileSize;
    private long reservedLogFileSize;

    private static abstract class FilterBuilder {
        private final String typeId;
        protected FilterBuilder( final String typeId ) {
            this.typeId = typeId;
        }
        protected abstract FilterInfo buildFilterInfo( FilterSelection value );
        protected FilterInfo buildFilterInfo( final String value ) {
            return buildFilterInfo(
                    resolveFilterSelection( value )
                            .orSome( new FilterSelection( typeId, value, "Not Found/Inaccessible '" + value + "'" ) )
            );
        }
        protected Option<FilterSelection> resolveFilterSelection( final String value ) {
            return some( new FilterSelection( typeId, value, value ) );
        }
    }

    private final Map<String,FilterBuilder> filterBuilders = CollectionUtils.<String,FilterBuilder>mapBuilder()
            .put( GatewayDiagnosticContextKeys.SERVICE_ID, new FilterBuilder( GatewayDiagnosticContextKeys.SERVICE_ID ) {
                @Override
                protected FilterInfo buildFilterInfo( final FilterSelection value ) {
                    return newServiceFilter( value );
                }
                @Override
                protected Option<FilterSelection> resolveFilterSelection( final String value ) {
                    return filterContext.resolveServiceFilter( value );
                }
            } )
            .put( GatewayDiagnosticContextKeys.CLIENT_IP, new FilterBuilder( GatewayDiagnosticContextKeys.CLIENT_IP ) {
                @Override
                protected FilterInfo buildFilterInfo( final FilterSelection value ) {
                    return newIPAddressFilter( value.getValue() );
                }
            } )
            .put( GatewayDiagnosticContextKeys.LOGGER_NAME, new FilterBuilder( GatewayDiagnosticContextKeys.LOGGER_NAME ) {
                @Override
                protected FilterInfo buildFilterInfo( final FilterSelection value ) {
                    return newLoggerNameFilter( value.getValue() );
                }
            } )
            .put( GatewayDiagnosticContextKeys.EMAIL_LISTENER_ID, new FilterBuilder( GatewayDiagnosticContextKeys.EMAIL_LISTENER_ID ) {
                @Override
                protected FilterInfo buildFilterInfo( final FilterSelection value ) {
                    return newEmailTransportFilter( value );
                }
                @Override
                protected Option<FilterSelection> resolveFilterSelection( final String value ) {
                    return filterContext.resolveEmailTransportFilter( value );
                }
            } )
            .put( GatewayDiagnosticContextKeys.JMS_LISTENER_ID, new FilterBuilder( GatewayDiagnosticContextKeys.JMS_LISTENER_ID ) {
                @Override
                protected FilterInfo buildFilterInfo( final FilterSelection value ) {
                    return newJMSTransportFilter( value );
                }
                @Override
                protected Option<FilterSelection> resolveFilterSelection( final String value ) {
                    return filterContext.resolveJmsTransportFilter( value );
                }
            } )
            .put( GatewayDiagnosticContextKeys.LISTEN_PORT_ID, new FilterBuilder( GatewayDiagnosticContextKeys.LISTEN_PORT_ID ) {
                @Override
                protected FilterInfo buildFilterInfo( final FilterSelection value ) {
                    return newListenPortTransportFilter( value );
                }
                @Override
                protected Option<FilterSelection> resolveFilterSelection( final String value ) {
                    return filterContext.resolveListenPortTransportFilter( value );
                }
            } )
            .put( GatewayDiagnosticContextKeys.USER_ID, new FilterBuilder( GatewayDiagnosticContextKeys.USER_ID ) {
                @Override
                protected FilterInfo buildFilterInfo( final FilterSelection value ) {
                    return newUserFilter( value );
                }
                @Override
                protected Option<FilterSelection> resolveFilterSelection( final String value ) {
                    return filterContext.resolveUserFilter( value );
                }
            } )
            .put( GatewayDiagnosticContextKeys.POLICY_ID, new FilterBuilder( GatewayDiagnosticContextKeys.POLICY_ID ) {
                @Override
                protected FilterInfo buildFilterInfo( final FilterSelection value ) {
                    return newPolicyFilter( value );
                }
                @Override
                protected Option<FilterSelection> resolveFilterSelection( final String value ) {
                    return filterContext.resolvePolicyFilter( value );
                }
            } )
            .put( GatewayDiagnosticContextKeys.FOLDER_ID, new FilterBuilder( GatewayDiagnosticContextKeys.FOLDER_ID ) {
                @Override
                protected FilterInfo buildFilterInfo( final FilterSelection value ) {
                    return newFolderFilter( value );
                }
                @Override
                protected Option<FilterSelection> resolveFilterSelection( final String value ) {
                    return filterContext.resolveFolderFilter( value );
                }
            } )
            .unmodifiableMap();

    private static final int ACTION_ADD    = 0;
    private static final int ACTION_REMOVE = 2;
    private static final int ACTION_EDIT   = 1;
    private static final int ACTION_UP     = 3;
    private static final int ACTION_DOWN   = 4;

    /**
     * Creates a new instance of SinkConfigurationPropertiesDialog. The fields in the dialog
     * will be set from the values in the provided SinkConfiguration and if the dialog is
     * dismissed with the OK button, then the provided SinkConfiguration will be updated with
     * the values from the fields in this dialog.
     *
     * @param owner The owner of this dialog window
     * @param sinkConfiguration The SinkConfiguration to read values from and possibly update
     */
    public SinkConfigurationPropertiesDialog( final Window owner,
                                              final SinkConfiguration sinkConfiguration,
                                              final boolean readOnly ) {
        super( owner, DIALOG_TITLE, ModalityType.APPLICATION_MODAL );
        this.filterContext = new FilterContext();
        this.sinkConfiguration = sinkConfiguration;
        this.readOnly = readOnly;
        initialize();
    }

    public void selectNameField() {
        nameField.requestFocus();
        nameField.selectAll();
    }
    /**
     * Loads locale-specific resources: strings, images, etc
     */
    private void initResources() {
        resources = ResourceBundle.getBundle("com.l7tech.console.resources.SinkConfigurationPropertiesDialog");
    }

    /**
     * Initializes this dialog window and sets all of the fields based on the provided SinkConfiguration
     * object.
     */
    private void initialize() {
        initResources();
        maximumLogFileSize = getMaximumLogFileSize();
        reservedLogFileSize = getReservedLogFileSize();

        initializeBaseFields();
        initializeFileFields();
        initializeSyslogFields();

        Utilities.setEscKeyStrokeDisposes( this );

        // Update all of the fields using the values from SinkConfiguration
        modelToView();
        enableDisableMain();
        enableDisableTabs();
        enableDisableSyslogSettings();
        if (nameField.getText().length() < 1)
            nameField.requestFocusInWindow();
        Utilities.setMinimumSize(this);
        disableTabsForSSPC();
    }

    /**
     * Initializes the base settings fields.
     */
    private void initializeBaseFields() {

        setTitle(resources.getString("sinkConfigurationProperties.window.title"));
        inputValidator = new InputValidator(this, resources.getString("sinkConfigurationProperties.window.title"));

        setContentPane(contentPane);
        pack();
        setModal(true);
        getRootPane().setDefaultButton(okButton);

        if ( !readOnly ) {
            // Attach the validator to the OK button
            inputValidator.attachToButton(okButton, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    onOk();
                }
            });
        } else {
            okButton.setEnabled( false );
        }

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        // When the type field is changed, enable or disable the tabs that aren't associated
        // with the new type value.
        typeField.setModel(new DefaultComboBoxModel(SinkType.values()));
        typeField.setRenderer(new Renderers.KeyedResourceRenderer(resources, "baseSettings.type.{0}.text"));
        typeField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableDisableTabs();
            }
        });

        severityField.setModel(new DefaultComboBoxModel(SeverityThreshold.values()));
        severityField.setRenderer(new Renderers.KeyedResourceRenderer(resources, "baseSettings.severity.{0}.text"));

        filtersList.addListSelectionListener( new RunOnChangeListener(){
            @Override
            protected void run() {
                enableDisableMain();
            }
        } );

        addFilterButton.addActionListener(new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                addFilter();
            }
        }));

        removeFilterButton.addActionListener(new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                if ( filtersList.getSelectedValues().length > 0) {
                    for(Object sel :filtersList.getSelectedValues() )
                        filterList.remove(sel);
                    filtersList.setModel( Utilities.listModel( filterList ) );
                    disableTabsForSSPC();
                }
            }
        }));

        // Name field must not be empty and must not be longer than 32 characters
        ((AbstractDocument)nameField.getDocument()).setDocumentFilter(new DocumentSizeFilter(32));
        inputValidator.constrainTextFieldToBeNonEmpty("Name", nameField, new InputValidator.ComponentValidationRule(nameField) {
            @Override
            public String getValidationError() {
                if( nameField.getText().trim().length()==0 ) {
                    return resources.getString("baseSettings.name.errors.empty");
                } else if ( !ValidationUtils.isValidCharacters(nameField.getText().trim(), VALID_NAME_CHARACTERS) ) {
                    return resources.getString("baseSettings.name.errors.chars");
                }

                return null;
            }
        });
        inputValidator.validateWhenDocumentChanges( nameField );

        // Description field must not be longer than 1000 characters
        ((AbstractDocument)descriptionField.getDocument()).setDocumentFilter(new DocumentSizeFilter(1000));

        zoneControl.configure(EntityType.LOG_SINK,
                sinkConfiguration.getOid() == SinkConfiguration.DEFAULT_OID ? OperationType.CREATE : readOnly ? OperationType.READ : OperationType.UPDATE,
                sinkConfiguration.getSecurityZone());
    }

    private void disableTabsForSSPC(){
        boolean found = false;
        for(FilterInfo fi : filterList){
            if(fi.filterItemId.equals("SSPC")){
                found = true;
                break;
            }
        }
        if(found && filterList.size() == 1){
            tabbedPane.setEnabledAt(1, false);
            tabbedPane.setEnabledAt(2, false);
        }
        else {
            enableDisableTabs();
        }
    }

    private void addFilter() {
        final SinkConfigurationFilterSelectionDialog filterSelectionDialog = new SinkConfigurationFilterSelectionDialog( this, filterContext );
        DialogDisplayer.display( filterSelectionDialog, new Runnable(){
            @Override
            public void run() {
                boolean added = false;
                for ( final FilterSelection filterSelection : filterSelectionDialog.getSelections() ) {
                    final FilterBuilder builder = filterBuilders.get( filterSelection.getTypeId() );
                    FilterInfo info = null;
                    if ( builder != null ) {
                        info = builder.buildFilterInfo( filterSelection );
                    } else if ( "category".equals( filterSelection.getTypeId() ) ) {
                        info = newCategoryFilter( filterSelection.getValue() );
                    }

                    if ( info != null && ! filterList.contains( info ) ) {
                        filterList.add( info );
                        added = true;
                    }
                }
                if ( added ) {
                    Collections.sort( filterList );
                    SwingUtilities.invokeLater( new Runnable() {
                        @Override
                        public void run() {
                            filtersList.setModel( Utilities.listModel( filterList ) );
                            disableTabsForSSPC();
                        }
                    } );
                }
            }
        } );
    }

    /**
     * Initializes the file settings fields.
     */
    private void initializeFileFields() {
        JSpinner.NumberEditor numberEditor = new JSpinner.NumberEditor(fileMaxSizeField);
        fileMaxSizeField.setEditor(numberEditor);
        numberEditor.getModel().setMinimum(1);
        numberEditor.getModel().setMaximum(1024 * 1024);
        numberEditor.getModel().setValue(1024);

        numberEditor = new JSpinner.NumberEditor(fileLogCount);
        fileLogCount.setEditor(numberEditor);
        numberEditor.getModel().setMinimum(1);
        numberEditor.getModel().setMaximum(100);
        numberEditor.getModel().setValue(1);

        fileFormatField.setModel(new DefaultComboBoxModel(SinkConfiguration.FILE_FORMAT_SET.toArray()));
        fileFormatField.setRenderer(new Renderers.KeyedResourceRenderer(resources, "fileSettings.format.{0}.text"));
        fileFormatField.setSelectedIndex(1);

        rollingIntervalField.setModel(new DefaultComboBoxModel(SinkConfiguration.RollingInterval.values()));
        rollingIntervalField.setSelectedItem(SinkConfiguration.RollingInterval.DAILY.toString());
        rollingIntervalField.setRenderer(new Renderers.KeyedResourceRenderer(resources, "fileSettings.rollingInterval.{0}.text"));
        rollingLogFileField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                final boolean enabled = rollingLogFileField.isSelected();
                rollingIntervalField.setEnabled(enabled);
                fileMaxSizeField.setEnabled(!enabled);
                fileLogCount.setEnabled(!enabled);
            }
        });
        ;
        // add validation rule for maximum file use
        inputValidator.addRule(new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                String error = null;

                long maximum = maximumLogFileSize;
                long reserved = reservedLogFileSize;

                // deduct space used for previous configuration
                reserved -= getSpaceUsed(sinkConfiguration);

                // add space used for this configuration
                SinkConfiguration tempConfig = new SinkConfiguration();
                viewToModel(tempConfig);
                reserved += getSpaceUsed(tempConfig);

                if (enabledField.isSelected() &&
                        reserved > maximum) {
                    return MessageFormat.format(
                            resources.getString("fileSettings.maxFileSize.errors.toolarge"),
                            maximum,
                            reserved);
                }

                return error;
            }
        });
    }

    /**
     * Get the space used for the given sinkConfiguration.
     */
    private long getSpaceUsed( final SinkConfiguration sinkConfiguration ) {
        if ( sinkConfiguration.isEnabled() && SinkConfiguration.SinkType.FILE == sinkConfiguration.getType() ) {
            final long limit = getLongProperty( sinkConfiguration, SinkConfiguration.PROP_FILE_MAX_SIZE, 0L );
            final long count = getLongProperty( sinkConfiguration, SinkConfiguration.PROP_FILE_LOG_COUNT, 0L );
            return 1024L * limit * count;
        }

        return 0L;
    }

    private long getLongProperty( final SinkConfiguration sinkConfiguration,
                                  final String propertyName,
                                  final long defaultValue ) {
        long value = defaultValue;
        try {
            value = Long.parseLong( sinkConfiguration.getProperty( propertyName ) );
        } catch (NumberFormatException e) {
            logger.log(
                    Level.WARNING,
                    "Error parsing sink configuration property '"+propertyName+"':" + ExceptionUtils.getMessage( e ) );
        }
        return value;
    }

    /**
     * Initializes the syslog settings fields.
     */
    private void initializeSyslogFields() {
        syslogProtocolField.setModel(new DefaultComboBoxModel(SinkConfiguration.SYSLOG_PROTOCOL_SET.toArray()));
        syslogProtocolField.setRenderer(new Renderers.KeyedResourceRenderer(resources, "syslogSettings.protocol.{0}.text"));
        syslogProtocolField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableDisableSyslogSettings();
            }
        });

        JSpinner.NumberEditor numberEditor = new JSpinner.NumberEditor(syslogFacilityField);
        syslogFacilityField.setEditor(numberEditor);
        numberEditor.getModel().setMinimum(0);
        numberEditor.getModel().setMaximum(23);
        numberEditor.getModel().setValue(1);

        syslogLogHostnameField.setSelected(true);

        syslogFormatField.setModel(new DefaultComboBoxModel(SinkConfiguration.SYSLOG_FORMAT_SET.toArray()));
        syslogFormatField.setRenderer(new Renderers.KeyedResourceRenderer(resources, "syslogSettings.format.{0}.text"));
        syslogFormatField.setSelectedIndex(1);

        // Currently only give options for ASCII, LATIN-1 and UTF-8. Are more encodings needed?
        syslogCharsetField.setModel(new DefaultComboBoxModel(new Object[] {
                "ASCII",
                "LATIN-1",
                "UTF-8"
        }));
        syslogCharsetField.setSelectedItem("UTF-8");

        // Put all possible timezones into the timezone field.
        String[] tzIds = TimeZone.getAvailableIDs();
        Arrays.sort(tzIds);
        String[] tzValues = new String[tzIds.length + 1];
        tzValues[0] = resources.getString("syslogSettings.timezone.values.useExisting");
        System.arraycopy(tzIds, 0, tzValues, 1, tzIds.length);
        syslogTimezoneField.setModel(new DefaultComboBoxModel(tzValues));
        syslogTimezoneField.setSelectedItem(resources.getString("syslogSettings.timezone.values.useExisting"));

        // Setup the test button
        inputValidator.attachToButton(syslogTestMessageButton, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SinkConfiguration newSinkConfiguration = new SinkConfiguration();
                viewToModel( newSinkConfiguration );

                Registry reg = Registry.getDefault();
                if (!reg.isAdminContextPresent())
                    return;
                LogSinkAdmin logSinkAdmin = reg.getLogSinkAdmin();
                if(logSinkAdmin == null) {
                    return;
                }

                logSinkAdmin.sendTestSyslogMessage(newSinkConfiguration, "Test message " + (++testCount) + ".");
            }
        });

        // populate host list
        syslogHostListModel = new DefaultListModel();
        syslogHostList.setModel(syslogHostListModel);
        syslogHostList.addListSelectionListener( new RunOnChangeListener() {
            @Override
            protected void run() {
                enableDisableSyslogSettings();
            }
        } );

        // host list add, edit, and remove button
        syslogHostAdd.addActionListener(new SyslogHostListActionListener(ACTION_ADD));
        syslogHostRemove.addActionListener(new SyslogHostListActionListener(ACTION_REMOVE));
        syslogHostEdit.addActionListener(new SyslogHostListActionListener(ACTION_EDIT));
        // host list up/down
        syslogHostUp.addActionListener(new SyslogHostListActionListener(ACTION_UP));
        syslogHostDown.addActionListener(new SyslogHostListActionListener(ACTION_DOWN));

        // add validator rule for Syslog hosts
        inputValidator.addRule(new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                // check for empty host list
                if (SinkConfiguration.SinkType.SYSLOG.equals(typeField.getSelectedItem()) &&
                    syslogHostList.getModel().getSize() <= 0)
                {
                    return resources.getString("syslogSettings.host.errors.empty");
                }
                return null;
            }
        });

        // SSL Keystore combo box
        syslogSSLKeystoreComboBox.setRenderer( TextListCellRenderer.basicComboBoxRenderer() );
        if (syslogSSLKeystoreComboBox.getModel().getSize() == 0)
            syslogSSLKeystoreComboBox.repopulate();

        // SSL client auth checkbox
        syslogSSLClientAuthenticationCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                // enable the keystore combo box based this checkbox
                if (syslogSSLClientAuthenticationCheckBox.isSelected()) {
                    syslogSSLKeystoreComboBox.setEnabled(true);
                } else {
                    syslogSSLKeystoreComboBox.setEnabled(false);
                }
            }
        });
    }

    /**
     * Get the reserved space for current log configuration (bytes)
     */
    private long getReservedLogFileSize() {
        try {
            LogSinkAdmin logSinkAdmin = Registry.getDefault().getLogSinkAdmin();
            return logSinkAdmin.getReservedFileSize();
        } catch ( IllegalStateException ise ) {
            // no longer connected to server, just return a value that will validate
            return 0L;
        }
    }

    /**
     * Get the maximum space for all log configurations (bytes)
     */
    private long getMaximumLogFileSize() {
        try {
            LogSinkAdmin logSinkAdmin = Registry.getDefault().getLogSinkAdmin();
            return logSinkAdmin.getMaximumFileSize();
        } catch ( IllegalStateException ise ) {
            // no longer connected to server, just return a value that will validate
            return Long.MAX_VALUE;
        }
    }

    public void enableDisableMain() {
        final boolean enableFilterRemove = filtersList.getSelectedValues().length > 0;
        addFilterButton.setEnabled( !readOnly );
        removeFilterButton.setEnabled( !readOnly && enableFilterRemove );
    }

    /**
     * Updates the enabled status of the File Settings and Syslog Settings tabs based on the new value
     * of the type field.
     */
    private void enableDisableTabs() {
        SinkType type = (SinkType) typeField.getSelectedItem();
        switch(type) {
        case FILE:
            tabbedPane.setEnabledAt(1, true);
            tabbedPane.setEnabledAt(2, false);
            break;
        case SYSLOG:
            tabbedPane.setEnabledAt(1, false);
            tabbedPane.setEnabledAt(2, true);
            enableDisableSyslogSettings();
            break;
        default:
            tabbedPane.setEnabledAt(1, false);
            tabbedPane.setEnabledAt(2, false);
            break;
        }
    }

    /**
     * Updates the enabled status for the SSL settings section in the Syslog Settings Tab.
     */
    private void enableDisableSyslogSettings() {

        if (SinkType.SYSLOG == typeField.getSelectedItem()) {

            if (SinkConfiguration.SYSLOG_PROTOCOL_SSL.equals(syslogProtocolField.getSelectedItem())) {
                syslogSSLSettingsPanel.setEnabled(true);
                syslogSSLClientAuthenticationCheckBox.setEnabled(true);
                syslogSSLKeystoreLabel.setEnabled(true);
                syslogSSLKeystoreComboBox.setEnabled(syslogSSLClientAuthenticationCheckBox.isSelected());
                syslogSSLSettingsLabel.setEnabled(true);
            } else {
                syslogSSLSettingsPanel.setEnabled(false);
                syslogSSLClientAuthenticationCheckBox.setEnabled(false);
                syslogSSLKeystoreComboBox.setEnabled(false);
                syslogSSLKeystoreLabel.setEnabled(false);
                syslogSSLSettingsLabel.setEnabled(false);
            }
        }

        final boolean syslogHostSelected = syslogHostList.getSelectedValue() != null;
        syslogTestMessageButton.setEnabled( !readOnly );
        syslogHostAdd.setEnabled( !readOnly );
        syslogHostRemove.setEnabled( !readOnly && syslogHostSelected );
        syslogHostEdit.setEnabled( !readOnly && syslogHostSelected );
        syslogHostUp.setEnabled( !readOnly && syslogHostSelected );
        syslogHostDown.setEnabled( !readOnly && syslogHostSelected );
    }

    /**
     * Updates the dialog fields to match the values from the backing SinkConfiguration.
     */
    private void modelToView() {
        modelToView( sinkConfiguration );
    }

    /**
     * Updates the dialog fields to match the values from the given SinkConfiguration.
     */
    private void modelToView(final SinkConfiguration sinkConfiguration) {
        modelToViewBase(sinkConfiguration);
        if(sinkConfiguration.getType() == null) {
            modelToViewFile(sinkConfiguration);
        } else {
            switch(sinkConfiguration.getType()) {
            case FILE:
                modelToViewFile(sinkConfiguration);
                break;
            case SYSLOG:
                modelToViewSyslog(sinkConfiguration);
                break;
            }
        }
    }

    /**
     * Updates the base settings fields to match the values from the given SinkConfiguration.
     */
    private void modelToViewBase(final SinkConfiguration sinkConfiguration) {
        if ( sinkConfiguration.getOid() != SinkConfiguration.DEFAULT_OID ) {
            nameField.setEditable(false);
        }
        nameField.setText( sinkConfiguration.getName() );
        enabledField.setSelected(sinkConfiguration.isEnabled());
        descriptionField.setText( sinkConfiguration.getDescription() );
        if ( sinkConfiguration.getType()!=null )
            typeField.setSelectedItem(sinkConfiguration.getType());
        else
            typeField.setSelectedItem(SinkType.FILE);
        if ( sinkConfiguration.getSeverity()!=null )
            severityField.setSelectedItem(sinkConfiguration.getSeverity());
        else
            severityField.setSelectedItem(SeverityThreshold.INFO);

        filterList.clear();

        if( sinkConfiguration.getCategories() != null && !sinkConfiguration.getCategories().isEmpty() ) {
            String[] categories = sinkConfiguration.getCategories().split(",");
            for( final String category : categories) {
                filterList.add( newCategoryFilter( category ) );
            }
        }

        if( sinkConfiguration.getFilters() != null ) {
            for ( final Entry<String,List<String>> entry : sinkConfiguration.getFilters().entrySet() ) {
                final FilterBuilder builder = filterBuilders.get( entry.getKey() );
                if ( builder != null ) {
                    for ( final String value : entry.getValue() ) {
                        filterList.add( builder.buildFilterInfo( value ) );
                    }
                }
            }
        }

        Collections.sort( filterList );
        filtersList.setModel( Utilities.listModel( filterList ) );
    }

    /**
     * Updates the file settings fields to match the values from the given SinkConfiguration.
     */
    private void modelToViewFile(final SinkConfiguration sinkConfiguration) {
        String value = sinkConfiguration.getProperty(SinkConfiguration.PROP_FILE_MAX_SIZE);
        fileMaxSizeField.setValue(value == null ? 1024 : Integer.parseInt(value));
        value = sinkConfiguration.getProperty(SinkConfiguration.PROP_FILE_LOG_COUNT);
        fileLogCount.setValue(value == null ? 2 : Integer.parseInt(value));
        value = sinkConfiguration.getProperty(SinkConfiguration.PROP_FILE_FORMAT);
        if(value == null) {
            fileFormatField.setSelectedItem( SinkConfiguration.FILE_FORMAT_STANDARD );
        } else {
            fileFormatField.setSelectedItem(value);
        }
        rollingLogFileField.setSelected(Boolean.valueOf(sinkConfiguration.getProperty(SinkConfiguration.PROP_ENABLE_ROLLING_FILE)));

        final boolean enabled = rollingLogFileField.isSelected();
        rollingIntervalField.setEnabled(enabled);
        fileMaxSizeField.setEnabled(!enabled);
        fileLogCount.setEnabled(!enabled);

        String interval = sinkConfiguration.getProperty(SinkConfiguration.PROP_ROLLING_INTERVAL);
        rollingIntervalField.setSelectedItem(interval == null ? SinkConfiguration.RollingInterval.DAILY : SinkConfiguration.RollingInterval.valueOf(interval));
    }

    /**
     * Updates the syslog settings to match the values from the given SinkConfiguration.
     */
    private void modelToViewSyslog(final SinkConfiguration sinkConfiguration) {
        String value = sinkConfiguration.getProperty(SinkConfiguration.PROP_SYSLOG_PROTOCOL);
        if( value == null ) {
            syslogProtocolField.setSelectedItem(SinkConfiguration.SYSLOG_PROTOCOL_TCP);
        } else {
            syslogProtocolField.setSelectedItem(value);
        }

        // set the host list values
        for (String entry : sinkConfiguration.syslogHostList()) {
            syslogHostListModel.addElement(entry);
        }

        value = sinkConfiguration.getProperty(SinkConfiguration.PROP_SYSLOG_FACILITY);
        syslogFacilityField.setValue(value == null ? 1 : new Integer(value));
        value = sinkConfiguration.getProperty(SinkConfiguration.PROP_SYSLOG_LOG_HOSTNAME);
        syslogLogHostnameField.setSelected( value == null || Boolean.parseBoolean( value ) );

        value = sinkConfiguration.getProperty(SinkConfiguration.PROP_SYSLOG_FORMAT);
        if(value == null) {
            // default to pre ssg 7 format
            syslogFormatField.setSelectedItem( SinkConfiguration.SYSLOG_FORMAT_RAW );
        } else {
            syslogFormatField.setSelectedItem(value);
        }

        value = sinkConfiguration.getProperty(SinkConfiguration.PROP_SYSLOG_CHAR_SET);
        syslogCharsetField.setSelectedItem( value == null ? "UTF-8" : value );
        value = sinkConfiguration.getProperty(SinkConfiguration.PROP_SYSLOG_TIMEZONE);
        syslogTimezoneField.setSelectedItem(value == null ? resources.getString("syslogSettings.timezone.values.useExisting") : value);
        value = sinkConfiguration.getProperty(SinkConfiguration.PROP_SYSLOG_SSL_CLIENTAUTH);
        syslogSSLClientAuthenticationCheckBox.setSelected("true".equals(value));

        if (syslogSSLClientAuthenticationCheckBox.isSelected()) {
            String id = sinkConfiguration.getProperty(SinkConfiguration.PROP_SYSLOG_SSL_KEYSTORE_ID);
            String alias = sinkConfiguration.getProperty(SinkConfiguration.PROP_SYSLOG_SSL_KEY_ALIAS);
            if (id != null && alias != null)
                syslogSSLKeystoreComboBox.select(Long.parseLong(id), alias);
            else
                syslogSSLKeystoreComboBox.selectDefaultSsl();
        }
    }

    /**
     * Updates the backing SinkConfiguration with the values from this dialog.
     */
    private void viewToModel() {
        viewToModel( sinkConfiguration );
    }

    /**
     * Updates the backing SinkConfiguration with the values from this dialog.
     */
    private void viewToModel(final SinkConfiguration sinkConfiguration) {
        viewToModelBase(sinkConfiguration);
        switch(sinkConfiguration.getType()) {
        case FILE:
            viewToModelFile(sinkConfiguration);
            break;
        case SYSLOG:
            viewToModelSyslog(sinkConfiguration);
            break;
        }
    }

    /**
     * Updates the backing SinkConfiguration with the base settings field values.
     */
    private void viewToModelBase(final SinkConfiguration sinkConfiguration) {
        if ( nameField.isEditable())
            sinkConfiguration.setName(nameField.getText().trim());
        sinkConfiguration.setEnabled( enabledField.isSelected() );
        sinkConfiguration.setDescription( descriptionField.getText() );
        sinkConfiguration.setType((SinkType)typeField.getSelectedItem());
        sinkConfiguration.setSeverity((SeverityThreshold)severityField.getSelectedItem());

        final Map<String, List<String>> allFilters = Functions.reduce( filterList, new HashMap<String, List<String>>(), new Binary<Map<String, List<String>>,Map<String, List<String>>,FilterInfo>(){
            @Override
            public Map<String, List<String>> call( final Map<String, List<String>> stringListMap, final FilterInfo filterInfo ) {
                final String filterTypeId = filterInfo.getFilterTypeId();
                final String filterItemId = filterInfo.getFilterItemId();

                List<String> values = stringListMap.get( filterTypeId );
                if ( values == null ) {
                    values = new ArrayList<String>();
                    stringListMap.put( filterTypeId, values );
                }
                if ( !values.contains( filterItemId ) ) values.add( filterItemId );

                return stringListMap;
            }
        } );

        final String categories = TextUtils.join( ",", allFilters.remove( "category" ) ).toString();
        sinkConfiguration.setCategories( categories.isEmpty() ? null : categories );
        sinkConfiguration.setFilters( allFilters ); // Categories removed above
        sinkConfiguration.setSecurityZone(zoneControl.getSelectedZone());
    }

    /**
     * Updates the backing SinkConfiguration with the file settings field values.
     */
    private void viewToModelFile(final SinkConfiguration sinkConfiguration) {
        sinkConfiguration.setProperty( SinkConfiguration.PROP_FILE_MAX_SIZE, fileMaxSizeField.getValue().toString() );
        sinkConfiguration.setProperty(SinkConfiguration.PROP_FILE_LOG_COUNT, fileLogCount.getValue().toString());
        sinkConfiguration.setProperty( SinkConfiguration.PROP_FILE_FORMAT, (String) fileFormatField.getSelectedItem() );

        sinkConfiguration.setProperty( SinkConfiguration.PROP_ENABLE_ROLLING_FILE, String.valueOf(rollingLogFileField.isSelected()) );
        sinkConfiguration.setProperty( SinkConfiguration.PROP_ROLLING_INTERVAL, rollingIntervalField.getSelectedItem().toString() );

        if (!sinkConfiguration.syslogHostList().isEmpty())
            sinkConfiguration.syslogHostList().clear();
    }

    /**
     * Updates the backing SinkConfiguration with the syslog settings field values.
     */
    private void viewToModelSyslog(final SinkConfiguration sinkConfiguration) {
        sinkConfiguration.setProperty(SinkConfiguration.PROP_SYSLOG_PROTOCOL, (String) syslogProtocolField.getSelectedItem());

        // re-populate the sinkConfig from the host list
        sinkConfiguration.syslogHostList().clear();
        for (int i=0; i<syslogHostListModel.getSize(); i++) {
            sinkConfiguration.addSyslogHostEntry(syslogHostListModel.getElementAt(i).toString());
        }

        sinkConfiguration.setProperty( SinkConfiguration.PROP_SYSLOG_FACILITY, syslogFacilityField.getValue().toString() );

        if (syslogLogHostnameField.isSelected()) {
            sinkConfiguration.setProperty(SinkConfiguration.PROP_SYSLOG_LOG_HOSTNAME, Boolean.TRUE.toString());
        } else {
            sinkConfiguration.setProperty(SinkConfiguration.PROP_SYSLOG_LOG_HOSTNAME, Boolean.FALSE.toString());
        }

        sinkConfiguration.setProperty( SinkConfiguration.PROP_SYSLOG_FORMAT, (String) syslogFormatField.getSelectedItem() );

        sinkConfiguration.setProperty(SinkConfiguration.PROP_SYSLOG_CHAR_SET, (String) syslogCharsetField.getSelectedItem());

        String value = (String) syslogTimezoneField.getSelectedItem();
        if (value != null && !value.equals(resources.getString("syslogSettings.timezone.values.useExisting"))) {
            sinkConfiguration.setProperty(SinkConfiguration.PROP_SYSLOG_TIMEZONE, (String) syslogTimezoneField.getSelectedItem());
        } else {
            sinkConfiguration.setProperty(SinkConfiguration.PROP_SYSLOG_TIMEZONE, null);
        }

        if (syslogSSLClientAuthenticationCheckBox.isEnabled() && syslogSSLClientAuthenticationCheckBox.isSelected()) {
            sinkConfiguration.setProperty(SinkConfiguration.PROP_SYSLOG_SSL_CLIENTAUTH, "true");
            sinkConfiguration.setProperty(SinkConfiguration.PROP_SYSLOG_SSL_KEYSTORE_ID, Long.toString(syslogSSLKeystoreComboBox.getSelectedKeystoreId()));
            sinkConfiguration.setProperty(SinkConfiguration.PROP_SYSLOG_SSL_KEY_ALIAS, syslogSSLKeystoreComboBox.getSelectedKeyAlias());
        } else {
            sinkConfiguration.setProperty(SinkConfiguration.PROP_SYSLOG_SSL_CLIENTAUTH, "false");
            sinkConfiguration.setProperty(SinkConfiguration.PROP_SYSLOG_SSL_KEYSTORE_ID, null);
            sinkConfiguration.setProperty(SinkConfiguration.PROP_SYSLOG_SSL_KEY_ALIAS, null);
        }
    }

    @Override
    public void setVisible(boolean b) {
        if (b && !isVisible()) confirmed = false;
        super.setVisible( b );
    }

    private void onOk() {
        viewToModel();
        confirmed = true;
        dispose();
    }

    /** @return true if the dialog has been dismissed with the ok button */
    public boolean isConfirmed() {
        return confirmed;
    }

    private FilterInfo newCategoryFilter( final String categoryId ) {
        return new FilterInfo( "category", "Category", categoryId, resources.getString( "baseSettings.categories." + categoryId + ".text" ) );
    }

    private FilterInfo newLoggerNameFilter( final String loggerName ) {
        return new FilterInfo( "logger-name", "Package", loggerName, loggerName );
    }

    private FilterInfo newServiceFilter( final FilterSelection filterSelection ) {
        return new FilterInfo( GatewayDiagnosticContextKeys.SERVICE_ID, "Service", filterSelection.getValue(), filterSelection.getDisplayValue() );
    }

    private FilterInfo newUserFilter( final FilterSelection filterSelection ) {
        return new FilterInfo( GatewayDiagnosticContextKeys.USER_ID, "User", filterSelection.getValue(), filterSelection.getDisplayValue() );
    }

    private FilterInfo newEmailTransportFilter( final FilterSelection filterSelection ) {
        return new FilterInfo( GatewayDiagnosticContextKeys.EMAIL_LISTENER_ID, "Transport", filterSelection.getValue(), "Email/" + filterSelection.getDisplayValue() );
    }

    private FilterInfo newJMSTransportFilter( final FilterSelection filterSelection ) {
        return new FilterInfo( GatewayDiagnosticContextKeys.JMS_LISTENER_ID, "Transport", filterSelection.getValue(), "JMS/" + filterSelection.getDisplayValue() );
    }

    private FilterInfo newListenPortTransportFilter( final FilterSelection filterSelection ) {
        return new FilterInfo( GatewayDiagnosticContextKeys.LISTEN_PORT_ID, "Transport", filterSelection.getValue(), "Listen Port/" + filterSelection.getDisplayValue() );
    }

    private FilterInfo newIPAddressFilter( final String ipPattern ) {
        return new FilterInfo( GatewayDiagnosticContextKeys.CLIENT_IP, "IP", ipPattern, ipPattern );
    }

    private FilterInfo newPolicyFilter( final FilterSelection filterSelection ) {
        return new FilterInfo( GatewayDiagnosticContextKeys.POLICY_ID, "Policy", filterSelection.getValue(), filterSelection.getDisplayValue() );
    }

    private FilterInfo newFolderFilter( final FilterSelection filterSelection ) {
        return new FilterInfo( GatewayDiagnosticContextKeys.FOLDER_ID, "Folder", filterSelection.getValue(), filterSelection.getDisplayValue() );
    }

    private static final class FilterInfo implements Comparable<FilterInfo> {
        private final String filterTypeId;
        private final String filterTypeName;
        private final String filterItemId;
        private final String filterItemName;

        private FilterInfo( @NotNull final String filterTypeId,
                            @NotNull final String filterTypeName,
                            @NotNull final String filterItemId,
                            @NotNull final String filterItemName ) {
            this.filterTypeId = filterTypeId;
            this.filterTypeName = filterTypeName;
            this.filterItemId = filterItemId;
            this.filterItemName = filterItemName;
        }

        public String getFilterItemId() {
            return filterItemId;
        }

        public String getFilterItemName() {
            return filterItemName;
        }

        public String getFilterTypeId() {
            return filterTypeId;
        }

        public String getFilterTypeName() {
            return filterTypeName;
        }

        public String toString() {
            return filterTypeName + "=" + filterItemName;
        }

        @Override
        public int compareTo( final FilterInfo o ) {
            return toString().toLowerCase().compareTo( o.toString().toLowerCase() );
        }

        @SuppressWarnings({ "RedundantIfStatement" })
        @Override
        public boolean equals( final Object o ) {
            if ( this == o ) return true;
            if ( o == null || getClass() != o.getClass() ) return false;

            final FilterInfo that = (FilterInfo) o;

            if ( !filterItemId.equals( that.filterItemId ) ) return false;
            if ( !filterItemName.equals( that.filterItemName ) ) return false;
            if ( !filterTypeId.equals( that.filterTypeId ) ) return false;
            if ( !filterTypeName.equals( that.filterTypeName ) ) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = filterTypeId.hashCode();
            result = 31 * result + filterTypeName.hashCode();
            result = 31 * result + filterItemId.hashCode();
            result = 31 * result + filterItemName.hashCode();
            return result;
        }
    }

    public static class SyslogHostPanel extends TextEntryPanel {
        public SyslogHostPanel() {
            this(null);
        }

        public SyslogHostPanel(String initialValue) {
            super("Syslog host and port:", "host", initialValue);
        }

        @Override
        protected String getSyntaxError(String model) {
            if (model == null || model.length() == 0) return null;

            final Pair<String,String> hostAndPort = InetAddressUtil.getHostAndPort( model, null );

            if ( !ValidationUtils.isValidDomain( InetAddressUtil.stripIpv6Brackets( hostAndPort.left ) ) ) {
                return "Invalid host value";
            }

            if ( !ValidationUtils.isValidInteger( hostAndPort.right, false, 1, 0xFFFF ) ) {
                return "Port number must be from 1 to 65535";
            }

            return null;
        }

    }

    /**
     * ActionListener for handling the syslog host list buttons.
     */
    protected class SyslogHostListActionListener implements ActionListener {

        private final int listenerAction;

        protected SyslogHostListActionListener(int action) {

            this.listenerAction = action;
        }

        @Override
        public void actionPerformed(ActionEvent e) {

            switch(listenerAction) {
                case ACTION_ADD:    doAdd(); break;
                case ACTION_REMOVE: doRemove(); break;
                case ACTION_EDIT:   doEdit(); break;
                case ACTION_UP:     doUp(); break;
                case ACTION_DOWN:   doDown(); break;
            }
        }

        void doAdd() {
            // open a dialog box
            OkCancelDialog dialog = OkCancelDialog.createOKCancelDialog(
                    getRootPane(), "Add Syslog Host", true, new SyslogHostPanel());
            dialog.pack();
            Utilities.centerOnScreen(dialog);
            dialog.setVisible(true);

            // add value to list, only when value is specified
            String value = (String) dialog.getValue();
            if (value != null && value.trim().length() > 0) {
                syslogHostListModel.addElement(value);
                syslogHostList.setSelectedIndex(syslogHostListModel.size()-1);
            }
        }

        void doRemove() {
            // remove value to model
            Object val = syslogHostList.getSelectedValue();
            if (val != null) {
                syslogHostListModel.removeElementAt(syslogHostList.getSelectedIndex());
            }
        }

        void doEdit() {
            if (syslogHostList.getSelectedValue() != null) {
                // open a dialog box
                int selected = syslogHostList.getSelectedIndex();
                OkCancelDialog dialog = OkCancelDialog.createOKCancelDialog(
                        getRootPane(), "Edit Syslog Host", true, new SyslogHostPanel(syslogHostList.getSelectedValue().toString()));
                dialog.pack();
                Utilities.centerOnScreen(dialog);
                dialog.setVisible(true);

                // add value to list, only when value is specified
                String value = (String) dialog.getValue();
                if (value != null && value.trim().length() > 0) {
                    syslogHostListModel.removeElementAt(selected);
                    syslogHostListModel.add(selected, value);
                    syslogHostList.setSelectedIndex(selected);
                }
            }
        }

        void doUp() {
            if (syslogHostList.getSelectedValue() != null) {
                int selected = syslogHostList.getSelectedIndex();
                if (selected > 0) {
                    Object obj = syslogHostListModel.getElementAt(selected);
                    syslogHostListModel.removeElementAt(selected);
                    syslogHostListModel.add(selected-1, obj);
                      syslogHostList.setSelectedIndex(selected-1);
              }
            }
        }

        void doDown() {
            if (syslogHostList.getSelectedValue() != null) {
                int selected = syslogHostList.getSelectedIndex();
                if (selected < syslogHostListModel.size()-1) {
                    Object obj = syslogHostListModel.getElementAt(selected);
                    syslogHostListModel.removeElementAt(selected);
                    syslogHostListModel.add(selected+1, obj);
                    syslogHostList.setSelectedIndex(selected+1);
                }
            }
        }

    }
}
