package com.l7tech.console.panels;

import com.l7tech.console.util.SecurityZoneWidget;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.uddi.UDDIRegistry;
import com.l7tech.gateway.common.uddi.UDDIServiceControl;
import com.l7tech.gateway.common.uddi.UDDIProxiedServiceInfo;
import com.l7tech.gateway.common.uddi.UDDIPublishStatus;

import static com.l7tech.gateway.common.uddi.UDDIProxiedServiceInfo.PublishType.*;
import com.l7tech.gateway.common.admin.UDDIRegistryAdmin;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.util.Registry;
import com.l7tech.console.poleditor.PolicyEditorPanel;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.uddi.UDDIKeyedReference;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.uddi.UDDINamedEntity;

import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServiceUDDISettingsDialog extends JDialog {//TODO rename to PublishToUDDIDialog
    private static final Logger logger = Logger.getLogger(ServiceUDDISettingsDialog.class.getName());
    private static final ResourceBundle resources = ResourceBundle.getBundle(ServiceUDDISettingsDialog.class.getName());
    private static final String OPTION_ORIGINAL_BUSINESS_SERVICE = "Original Business Service";
    private static final String OPTION_PUBLISHED_BUSINESS_SERVICES = "Published Business Services";
    private static final String OPTION_BOTH_BUSINESS_SERVICES = "Both Business Services";

    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JRadioButton dontPublishRadioButton;
    private JRadioButton publishProxiedWsdlRadioButton;
    private JCheckBox updateWhenGatewayWSDLCheckBox;
    private JComboBox uddiRegistriesComboBox;
    private JButton selectBusinessEntityButton;
    private JLabel uddiRegistryLabel;
    private JLabel businessEntityLabel;
    private JLabel businessEntityNameLabel;
    private JLabel publishProxystatusLabel;
    private JRadioButton publishGatewayEndpointAsRadioButton;
    private JLabel bindingTemplateStatusLabel;
    private JCheckBox removeExistingBindingsCheckBox;
    private JRadioButton overwriteExistingBusinessServiceWithRadioButton;
    private JLabel overwriteStatusLabel;
    private JCheckBox updateWhenGatewayWSDLCheckBoxOverwrite;
    private JCheckBox metricsEnabledCheckBox;
    private JCheckBox publishPolicyCheckBox;
    private JLabel policyServiceLabel;
    private JComboBox policyServiceComboBox;
    private JCheckBox publishFullPolicyCheckBox;
    private JCheckBox inlinePolicyIncludesCheckBox;
    private JButton manageMetaData;
    private JCheckBox gifPublishCheckBox;
    private JComboBox endPointTypeComboBox;
    private JLabel endPointTypeLabel;
    private SecurityZoneWidget zoneControl;

    private PublishedService service;
    private boolean canUpdate;
    private boolean confirmed;
    private Map<String, UDDIRegistry> allRegistries;
    private Map<Long, Boolean> registryMetricsEnabled = new HashMap<Long,Boolean>();
    private UDDIProxiedServiceInfo uddiProxyServiceInfo;
    private UDDIServiceControl uddiServiceControl;
    private UDDIRegistry originalServiceRegistry;
    private InputValidator publishWsdlValidators;

    private final static String businessEntityDefault = "<None Selected>";
    private String selectedBusinessName;
    private String selectedBusinessKey;
    private UDDIPublishStatus publishStatus;

    private boolean isSystinet;
    private final Set<UDDIKeyedReference> keyedReferenceSet = new HashSet<UDDIKeyedReference>();
    private UDDIRegistryAdmin.EndpointScheme gifEndpointScheme;

    public ServiceUDDISettingsDialog() {
        initialize();
    }

    public ServiceUDDISettingsDialog( final Window owner,
                                      final PublishedService svc,
                                      final boolean hasUpdatePermission ) {
        super(owner, resources.getString( "dialog.title" ), ServiceUDDISettingsDialog.DEFAULT_MODALITY_TYPE);
        service = svc;
        if (areUnsavedChangesToThisPolicy()) {
            //readOnlyWarningLabel.setText("Service has unsaved policy changes");
            this.canUpdate = false;
        } else {
            this.canUpdate = hasUpdatePermission;
        }
        publishStatus = null;
        initialize();
    }

    private void initialize(){
        setContentPane(contentPane);
        setModal(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        buttonOK.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        selectBusinessEntityButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if(uddiRegistriesComboBox.getSelectedIndex() == -1){
                        showErrorMessage("Select a UDDI Registry", "Select a UDDI Registry to search", null, false);
                        return;
                    }

                    UDDIRegistry uddiRegistry = allRegistries.get(uddiRegistriesComboBox.getSelectedItem().toString());
                    if(!uddiRegistry.isEnabled()){
                        showErrorMessage("Cannot Search", "UDDI Registry is not currently enabled", null, false);
                        return;
                    }

                    SearchUddiDialog uddiDialog = new SearchUddiDialog(ServiceUDDISettingsDialog.this,
                            SearchUddiDialog.SEARCH_TYPE.BUSINESS_ENTITY_SEARCH, uddiRegistriesComboBox.getSelectedItem().toString());
                    uddiDialog.addSelectionListener(new SearchUddiDialog.ItemSelectedListener(){
                        @Override
                        public void itemSelected(Object item) {
                            if(!(item instanceof UDDINamedEntity)) return;

                            UDDINamedEntity entity = (UDDINamedEntity) item;
                            selectedBusinessName = entity.getName();
                            selectedBusinessKey = entity.getKey();
                            businessEntityNameLabel.setText(selectedBusinessName);
                        }
                    });
                    uddiDialog.setSize(700, 500);
                    uddiDialog.setModal(true);
                    DialogDisplayer.display(uddiDialog);

                } catch (FindException e1) {
                    showErrorMessage("Problem Searching UDDI", "Cannot search for BusinessEntities", e1, true);
                }
            }
        });

        manageMetaData.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final ManageMetaDataDialog metaDialog = new ManageMetaDataDialog(ServiceUDDISettingsDialog.this, keyedReferenceSet);
                metaDialog.pack();
                metaDialog.setModal(true);
                metaDialog.setSize(600, 200);
                DialogDisplayer.display(metaDialog, new Runnable() {
                    @Override
                    public void run() {
                        if(metaDialog.isWasOked()){
                            keyedReferenceSet.clear();
                            keyedReferenceSet.addAll(metaDialog.getKeyedReferences());
                        }
                    }
                });
            }
        });

        final RunOnChangeListener enableDisableChangeListener = new RunOnChangeListener( new Runnable(){
            @Override
            public void run() {
                enableAndDisableComponents();
            }
        } );

        publishProxiedWsdlRadioButton.addActionListener(enableDisableChangeListener);
        publishGatewayEndpointAsRadioButton.addActionListener(enableDisableChangeListener);
        overwriteExistingBusinessServiceWithRadioButton.addActionListener(enableDisableChangeListener);
        dontPublishRadioButton.addActionListener(enableDisableChangeListener);
        metricsEnabledCheckBox.addActionListener( enableDisableChangeListener );
        publishPolicyCheckBox.addActionListener( enableDisableChangeListener );
        publishFullPolicyCheckBox.addActionListener( enableDisableChangeListener );
        gifPublishCheckBox.addActionListener(enableDisableChangeListener);
        removeExistingBindingsCheckBox.addActionListener(enableDisableChangeListener);

        //Populate registry drop down
        loadUddiRegistries();

        //determine if there is a UDDIProxiedService for this service
        try {
            uddiProxyServiceInfo = getUDDIRegistryAdmin().findProxiedServiceInfoForPublishedService(service.getOid());
            if(uddiProxyServiceInfo != null){
                try {
                    publishStatus =
                            Registry.getDefault().getUDDIRegistryAdmin().getPublishStatusForProxy(uddiProxyServiceInfo.getOid(), service.getOid());
                } catch (FindException e) {
                    showErrorMessage("Cannot get publish status", "Cannot find the publish status for information in UDDI", ExceptionUtils.getDebugException(e), true);
                    dispose();
                }
            }
        } catch (FindException e) {
            uddiProxyServiceInfo = null;
        }

        try {
            uddiServiceControl = getUDDIRegistryAdmin().getUDDIServiceControl(service.getOid());
            if(uddiServiceControl != null){
                originalServiceRegistry = getUDDIRegistryAdmin().findByPrimaryKey(uddiServiceControl.getUddiRegistryOid());
            }
        } catch (FindException e) {
            uddiServiceControl = null;
            originalServiceRegistry = null;
        }

        //Input validators
        publishWsdlValidators = new InputValidator(this, "Publish Gateway WSDL Validation");
        publishWsdlValidators.addRule(new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                if(uddiRegistriesComboBox.getSelectedIndex() == -1) return "Please select a UDDI Registry";

                UDDIRegistry uddiRegistry = allRegistries.get(uddiRegistriesComboBox.getSelectedItem().toString());
                if(!uddiRegistry.isEnabled()) return "UDDI Registry is not currently enabled.";
                
                return null;
            }
        });

        publishWsdlValidators.addRule(new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                if(businessEntityNameLabel.getText().equals(businessEntityDefault)) return "Please select a UDDI Business Entity";
                return null;
            }
        });

        endPointTypeComboBox.addItem(UDDIRegistryAdmin.EndpointScheme.HTTP);
        endPointTypeComboBox.addItem(UDDIRegistryAdmin.EndpointScheme.HTTPS);

        if (originalServiceRegistry != null) {
            isSystinet = originalServiceRegistry.getUddiRegistryType().equals(UDDIRegistry.UDDIRegistryType.SYSTINET.toString());
            gifPublishCheckBox.setVisible(isSystinet);
            endPointTypeLabel.setVisible(isSystinet);
            endPointTypeComboBox.setVisible(isSystinet);
        }
        zoneControl.setEntityType(EntityType.UDDI_PROXIED_SERVICE_INFO);
        modelToView();
        enableAndDisableComponents();
        Utilities.setEscKeyStrokeDisposes(this);
        pack();
    }

    /**
     * Only responsible for transferring data from model to view.
     * The enabling or disabling of UI components it done in the enable / disable methods
     */
    private void modelToView() {

        //publish tab configuration
        if (uddiProxyServiceInfo != null) {
            //find which registry to select
            long uddiRegOid = uddiProxyServiceInfo.getUddiRegistryOid();
            boolean found = false;
            for(UDDIRegistry registries: allRegistries.values()){
                if(registries.getOid() == uddiRegOid){
                    String regName = registries.getName();
                    uddiRegistriesComboBox.setSelectedItem(regName);
                    found = true;
                }
            }
            if(!found) throw new IllegalStateException("UDDI Registry not found for pulished Gateway WSDL");

            final UDDIProxiedServiceInfo.PublishType publishType = uddiProxyServiceInfo.getPublishType();
            clearStatusLabels(publishType);
            switch(publishType){
                case PROXY:
                    businessEntityNameLabel.setText(uddiProxyServiceInfo.getUddiBusinessName());
                    updateWhenGatewayWSDLCheckBox.setSelected(uddiProxyServiceInfo.isUpdateProxyOnLocalChange());
                    publishProxiedWsdlRadioButton.setSelected(true);
                    setLabelStatus(publishProxystatusLabel);
                    break;
                case ENDPOINT:
                    publishGatewayEndpointAsRadioButton.setSelected(true);
                    removeExistingBindingsCheckBox.setSelected(uddiProxyServiceInfo.isRemoveOtherBindings());

                    if(uddiProxyServiceInfo != null){
                        final Boolean isGif = uddiProxyServiceInfo.getProperty(UDDIProxiedServiceInfo.IS_GIF);
                        if( isGif != null && isGif){
                            gifPublishCheckBox.setSelected(true);
                            gifEndpointScheme = uddiProxyServiceInfo.getProperty(UDDIProxiedServiceInfo.GIF_SCHEME);
                            if(gifEndpointScheme != null){
                                endPointTypeComboBox.setSelectedItem(gifEndpointScheme);
                            } else {
                                endPointTypeComboBox.setSelectedIndex(-1);
                            }

                        } else {
                            endPointTypeComboBox.setSelectedIndex(-1);
                        }
                    }
                    setLabelStatus(bindingTemplateStatusLabel);
                    break;
                case OVERWRITE:
                    overwriteExistingBusinessServiceWithRadioButton.setSelected(true);
                    updateWhenGatewayWSDLCheckBoxOverwrite.setSelected(uddiProxyServiceInfo.isUpdateProxyOnLocalChange());
                    setLabelStatus(overwriteStatusLabel);
                    break;
            }

            final Set<UDDIKeyedReference> configuredRefs = uddiProxyServiceInfo.getProperty(UDDIProxiedServiceInfo.KEYED_REFERENCES_CONFIG);
            if(configuredRefs != null){
                keyedReferenceSet.addAll(configuredRefs);
            }
        }else{
            //set other buttons values when implemented
            dontPublishRadioButton.setSelected(true);
            uddiRegistriesComboBox.setSelectedIndex(-1);
            businessEntityNameLabel.setText(businessEntityDefault);
            if (isSystinet) {
                //some default behaviour to preference GIF when the registry is Systinet
                //set when no publish has been done
                gifPublishCheckBox.setSelected(true);
            }

            clearStatusLabels(null);
        }
        zoneControl.setSelectedZone(uddiProxyServiceInfo == null ? null : uddiProxyServiceInfo.getSecurityZone());
        
        // WS-Policy settings
        boolean originalWsPolicyAvailable = uddiServiceControl != null;
        boolean proxyWsPolicyAvailable = uddiProxyServiceInfo != null &&
                publishStatus.getPublishStatus() == UDDIPublishStatus.PublishStatus.PUBLISHED &&
                uddiProxyServiceInfo.getPublishType() == PROXY;
        
        boolean originalWsPolicyEnabled = uddiServiceControl != null && uddiServiceControl.isPublishWsPolicyEnabled();
        boolean proxyWsPolicyEnabled = uddiProxyServiceInfo != null && uddiProxyServiceInfo.isPublishWsPolicyEnabled(); 
        if ( originalWsPolicyAvailable || proxyWsPolicyAvailable ) {
            java.util.List<String> options = new ArrayList<String>();
            if ( originalWsPolicyAvailable ) options.add( OPTION_ORIGINAL_BUSINESS_SERVICE );
            if ( proxyWsPolicyAvailable ) options.add(OPTION_PUBLISHED_BUSINESS_SERVICES);
            if ( originalWsPolicyAvailable && proxyWsPolicyAvailable ) options.add( OPTION_BOTH_BUSINESS_SERVICES );
            policyServiceComboBox.setModel( new DefaultComboBoxModel( options.toArray(new String[options.size()] ) ));
            publishPolicyCheckBox.setSelected( originalWsPolicyEnabled || proxyWsPolicyEnabled );
            if ( originalWsPolicyEnabled && proxyWsPolicyEnabled ) policyServiceComboBox.setSelectedItem( OPTION_BOTH_BUSINESS_SERVICES );
            else if ( originalWsPolicyEnabled ) policyServiceComboBox.setSelectedItem( OPTION_ORIGINAL_BUSINESS_SERVICE );
            else if ( proxyWsPolicyEnabled ) policyServiceComboBox.setSelectedItem(OPTION_PUBLISHED_BUSINESS_SERVICES);
        }
        publishFullPolicyCheckBox.setSelected( getPublishFullPolicy(uddiServiceControl, uddiProxyServiceInfo) );
        inlinePolicyIncludesCheckBox.setSelected( getPublishInlinedPolicy(publishFullPolicyCheckBox.isSelected(), uddiServiceControl, uddiProxyServiceInfo) );

        // Metrics settings
        //ideally whether metrics are enabled for activesoa or not would be determined by whether we know the proxy has been correctly 'virtualized'
        boolean proxyMetricsEnabled = uddiProxyServiceInfo != null &&
                uddiProxyServiceInfo.isMetricsEnabled() &&
                publishStatus.getPublishStatus() == UDDIPublishStatus.PublishStatus.PUBLISHED &&
                uddiProxyServiceInfo.getPublishType() == PROXY;
        metricsEnabledCheckBox.setSelected( proxyMetricsEnabled );
    }

    /**
     * Method should only be called when we know publishStatus is not null
     * @param label
     */
    private void setLabelStatus(JLabel label) {
        if(publishStatus == null) throw new NullPointerException("publishStatus must not be null");
        
        final String status;
        switch (publishStatus.getPublishStatus()) {
            case PUBLISHED:
                status = "Status: Published";
                break;
            case PUBLISH:
                status = "Status: Publishing";
                break;
            case PUBLISH_FAILED:
                status = "Status: Publish failed " + publishStatus.getFailCount() + " times. Set to retry";
                break;
            case CANNOT_PUBLISH:
                status = "Status: Cannot publish. Tried " + publishStatus.getFailCount() + " times. Please select \"Don't Publish\" to clear";
                break;
            case DELETE:
                status = "Status: Deleting";
                break;
            case DELETE_FAILED:
                status = "Status: Delete failed " + publishStatus.getFailCount() + " times. Set to retry";
                break;
            case CANNOT_DELETE:
                status = "Status: Cannot delete. Tried " + publishStatus.getFailCount() + " times. Please select \"Don't Publish\" to clear";
                break;
            default:
                status = "";
        }

        label.setText(status);
    }
    /**
     * Clear the status labels for all labels which don't apply for the publish type 
     * @param publishType
     */
    private void clearStatusLabels(UDDIProxiedServiceInfo.PublishType publishType){
        if(publishType == null || publishType != PROXY) publishProxystatusLabel.setText("");
        if(publishType == null || publishType != ENDPOINT) bindingTemplateStatusLabel.setText("");
        if(publishType == null || publishType != OVERWRITE) overwriteStatusLabel.setText("");
    }
    
    /**
     * Enable or disable UI components in the 'Publish' tab only
     */
    private void enableDisablePublishTabComponents(){
        UDDIProxiedServiceInfo.PublishType publishType = uddiProxyServiceInfo == null ? null : uddiProxyServiceInfo.getPublishType();
        final boolean uddiControl = uddiServiceControl != null && !uddiServiceControl.isUnderUddiControl();

        publishProxiedWsdlRadioButton.setEnabled(publishType == null);
        enableDisablePublishGatewayWsdlControls(publishProxiedWsdlRadioButton.isSelected() && publishType == null);
        updateWhenGatewayWSDLCheckBox.setEnabled(publishProxiedWsdlRadioButton.isSelected());

        publishGatewayEndpointAsRadioButton.setEnabled(uddiControl && publishType == null);
        enableDisablePublishEndpointControls(publishGatewayEndpointAsRadioButton.isSelected() && publishType == null);

        overwriteExistingBusinessServiceWithRadioButton.setEnabled(uddiControl && publishType == null);
        enableDisablePublishOverwriteControls(overwriteExistingBusinessServiceWithRadioButton.isSelected() && publishType == null);
        updateWhenGatewayWSDLCheckBoxOverwrite.setEnabled(overwriteExistingBusinessServiceWithRadioButton.isSelected());
    }

    private void enableAndDisableComponents() {
        if ( canUpdate ) {
            //configure enable / disable for publish tab
            enableDisablePublishTabComponents();

            // ws-policy tab
            boolean enablePolicy = policyServiceComboBox.getModel().getSize() > 0;
            boolean enablePolicyOptions = enablePolicy && publishPolicyCheckBox.isSelected();
            publishPolicyCheckBox.setEnabled( enablePolicy );
            policyServiceLabel.setEnabled( enablePolicyOptions );
            policyServiceComboBox.setEnabled( enablePolicyOptions );
            publishFullPolicyCheckBox.setEnabled( enablePolicyOptions );
            inlinePolicyIncludesCheckBox.setEnabled( enablePolicyOptions && publishFullPolicyCheckBox.isSelected());

            //metrics tab
            boolean enableMetrics = uddiProxyServiceInfo != null &&
                    isMetricsEnabled(uddiProxyServiceInfo.getUddiRegistryOid()) &&
                    uddiProxyServiceInfo != null &&
                    publishStatus.getPublishStatus() == UDDIPublishStatus.PublishStatus.PUBLISHED &&
                    uddiProxyServiceInfo.getPublishType() == PROXY;
            
            metricsEnabledCheckBox.setEnabled( enableMetrics );
        } else {
            //publish tab
            publishProxiedWsdlRadioButton.setEnabled(false);
            enableDisablePublishGatewayWsdlControls(false);
            publishGatewayEndpointAsRadioButton.setEnabled(false);
            enableDisablePublishEndpointControls(false);
            overwriteExistingBusinessServiceWithRadioButton.setEnabled(false);
            enableDisablePublishOverwriteControls(false);
            dontPublishRadioButton.setEnabled(false);

            // ws-policy tab
            publishPolicyCheckBox.setEnabled( false );
            policyServiceLabel.setEnabled( false );
            policyServiceComboBox.setEnabled( false );
            publishFullPolicyCheckBox.setEnabled( false );
            inlinePolicyIncludesCheckBox.setEnabled( false );

            //metrics tab
            metricsEnabledCheckBox.setEnabled( false );
        }
    }

    private void enableDisablePublishGatewayWsdlControls(boolean enable){
        //publish proxy
        uddiRegistryLabel.setEnabled(enable);
        uddiRegistriesComboBox.setEnabled(enable);
        businessEntityLabel.setEnabled(enable);
        businessEntityNameLabel.setEnabled(enable);
        selectBusinessEntityButton.setEnabled(enable);
        updateWhenGatewayWSDLCheckBox.setEnabled(enable);
    }

    private void enableDisablePublishEndpointControls(boolean enable){
        //binding endpoint
        if(uddiServiceControl != null && uddiServiceControl.isUnderUddiControl()){
            removeExistingBindingsCheckBox.setEnabled(false);
            manageMetaData.setEnabled(false);
            gifPublishCheckBox.setEnabled(false);
            endPointTypeLabel.setEnabled(false);
            endPointTypeComboBox.setEnabled(false);
        }else{
            removeExistingBindingsCheckBox.setEnabled(enable);
            final boolean isRemove = removeExistingBindingsCheckBox.isSelected();
            if(isRemove){
                gifPublishCheckBox.setEnabled(false);
                endPointTypeLabel.setEnabled(false);
                endPointTypeComboBox.setEnabled(false);
            } else {
                gifPublishCheckBox.setEnabled(enable);
                final boolean gifSelected = gifPublishCheckBox.isSelected();
                endPointTypeLabel.setEnabled(enable && gifSelected);
                endPointTypeComboBox.setEnabled(enable && gifSelected);
                if(gifSelected){
                    removeExistingBindingsCheckBox.setEnabled(false);
                    if(gifEndpointScheme == null){
                        endPointTypeComboBox.setSelectedIndex(0);
                    }
                } else {
                    endPointTypeComboBox.setSelectedIndex(-1);
                }
            }
            if ( publishStatus != null &&
                    publishStatus.getPublishStatus() != UDDIPublishStatus.PublishStatus.PUBLISHED &&
                        publishStatus.getPublishStatus() != UDDIPublishStatus.PublishStatus.PUBLISH_FAILED &&
                    publishGatewayEndpointAsRadioButton.isSelected()) {
                    manageMetaData.setEnabled(false);
            } else {
                manageMetaData.setEnabled(publishGatewayEndpointAsRadioButton.isSelected());
            }

        }
    }

    private void enableDisablePublishOverwriteControls(boolean enable){
        //overwrite
        updateWhenGatewayWSDLCheckBoxOverwrite.setEnabled(enable);
    }

    /**
     * Validate the view and update model if valid.
     *
     * Warning: Do not use DialogDisplayer in this method without thinking about how it will affect usage in the Applet.
     * 
     * @return true if view is valid and can be converted to model, false otherwise. Return false to not dismiss the dialog.
     */
    private boolean viewToModel(){
        final UDDIRegistryAdmin uddiRegistryAdmin = Registry.getDefault().getUDDIRegistryAdmin();

        //PROCESS TABS OTHER THAN SERVICE
        if ( uddiServiceControl != null ) {
            final UDDIServiceControl test = UDDIServiceControl.copyFrom(uddiServiceControl);
            if ( isPublishWsPolicyEnabled() ) {
                uddiServiceControl.setPublishWsPolicyEnabled( true );
                uddiServiceControl.setPublishWsPolicyFull( publishFullPolicyCheckBox.isSelected() );
                uddiServiceControl.setPublishWsPolicyInlined( publishFullPolicyCheckBox.isSelected() && inlinePolicyIncludesCheckBox.isSelected() );
            } else {
                uddiServiceControl.setPublishWsPolicyEnabled( false );
                uddiServiceControl.setPublishWsPolicyFull( false );
                uddiServiceControl.setPublishWsPolicyInlined( false );
            }
            if(!test.equals(uddiServiceControl)){
                try {
                    uddiRegistryAdmin.saveUDDIServiceControlOnly( uddiServiceControl, null, null);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Error saving UDDIServiceControl '" + ExceptionUtils.getMessage(e) + "'.", e);
                    JOptionPane.showMessageDialog(this, "Error saving UDDI settings: " + ExceptionUtils.getMessage(e)
                            , "Error saving settings", JOptionPane.ERROR_MESSAGE);
                }
            }
            //note: if a rule is broken in any of these tabs, return false here so that the user can fix it.
        }

        //Process the SERVICE tab
        //All actions in the publish tab are mutually exclusive. It is only possible to take a single action
        //e.g. a single publish option. Following a publish an unpublish is the only option etc.
        if(publishProxiedWsdlRadioButton.isSelected()){
            if(uddiProxyServiceInfo == null){
                if(publishWsdlValidators.validateWithDialog()){
                    if(!publishUDDIProxiedService()) return false;
                }
                else return false;
            }else {
                //update if the check box's value has changed
                if(uddiProxyServiceInfo.isUpdateProxyOnLocalChange() != updateWhenGatewayWSDLCheckBox.isSelected() ||
                   uddiProxyServiceInfo.isMetricsEnabled() != isProxyMetricsEnabled() ||
                   publishWsPolicySettingsChanged() ){
                    uddiProxyServiceInfo.setUpdateProxyOnLocalChange(updateWhenGatewayWSDLCheckBox.isSelected());
                    uddiProxyServiceInfo.setMetricsEnabled(isProxyMetricsEnabled());
                    if ( isProxyPublishWsPolicyEnabled() ) {
                        uddiProxyServiceInfo.setPublishWsPolicyEnabled( true );
                        uddiProxyServiceInfo.setPublishWsPolicyFull( publishFullPolicyCheckBox.isSelected() );
                        uddiProxyServiceInfo.setPublishWsPolicyInlined( publishFullPolicyCheckBox.isSelected() && inlinePolicyIncludesCheckBox.isSelected() );
                    } else {
                        uddiProxyServiceInfo.setPublishWsPolicyEnabled( false );
                        uddiProxyServiceInfo.setPublishWsPolicyFull( false );
                        uddiProxyServiceInfo.setPublishWsPolicyInlined( false );
                    }

                    updateUddiProxiedServiceOnly(uddiRegistryAdmin);
                }
            }
        }else if(publishGatewayEndpointAsRadioButton.isSelected()) {
            if (uddiProxyServiceInfo == null) {
                //publish for first time
                return publishEndpointToBusinessService();
            } else {
                //check if the any meta data was modified
                if(keyedReferenceSet != null){
                    final boolean refsAreDifference = uddiProxyServiceInfo.areKeyedReferencesDifferent(keyedReferenceSet);
                    if (refsAreDifference) {
                        uddiProxyServiceInfo.setProperty(UDDIProxiedServiceInfo.KEYED_REFERENCES_CONFIG, keyedReferenceSet);
                        updateUddiProxiedServiceOnly(uddiRegistryAdmin);
                    }
                }
            }
        }else if(overwriteExistingBusinessServiceWithRadioButton.isSelected()){
            if(uddiProxyServiceInfo == null){
                return overwriteExistingService();
            }else {
                //update if the check box's value has changed
                if(uddiProxyServiceInfo.isUpdateProxyOnLocalChange() != updateWhenGatewayWSDLCheckBoxOverwrite.isSelected() ||
                   uddiProxyServiceInfo.isMetricsEnabled() != isProxyMetricsEnabled() ||
                   publishWsPolicySettingsChanged() ){
                    uddiProxyServiceInfo.setUpdateProxyOnLocalChange(updateWhenGatewayWSDLCheckBoxOverwrite.isSelected());
                    uddiProxyServiceInfo.setMetricsEnabled(isProxyMetricsEnabled());
                    if ( isProxyPublishWsPolicyEnabled() ) {
                        uddiProxyServiceInfo.setPublishWsPolicyEnabled( true );
                        uddiProxyServiceInfo.setPublishWsPolicyFull( publishFullPolicyCheckBox.isSelected() );
                        uddiProxyServiceInfo.setPublishWsPolicyInlined( publishFullPolicyCheckBox.isSelected() && inlinePolicyIncludesCheckBox.isSelected() );
                    } else {
                        uddiProxyServiceInfo.setPublishWsPolicyEnabled( false );
                        uddiProxyServiceInfo.setPublishWsPolicyFull( false );
                        uddiProxyServiceInfo.setPublishWsPolicyInlined( false );
                    }

                    updateUddiProxiedServiceOnly(uddiRegistryAdmin);
                }
            }
        } else if(dontPublishRadioButton.isSelected()){
            if(uddiProxyServiceInfo != null){
                UDDIRegistry uddiRegistry = allRegistries.get(uddiRegistriesComboBox.getSelectedItem().toString());
                if(!uddiRegistry.isEnabled()){
                    showErrorMessage("Cannot Update UDDI", "UDDI Registry is not currently enabled", null, false);
                    return false;
                }

                switch (publishStatus.getPublishStatus()) {
                    case PUBLISH:
                        showErrorMessage("Cannot Update UDDI", "Publishing to UDDI in progress. Please close dialog and try again in a few minutes", null, false);
                        return false;
                    case PUBLISH_FAILED:
                        JOptionPane.showMessageDialog(this, "Previous UDDI publishing attempts have failed; data may be orphaned in UDDI.", "UDDI Publish Errors", JOptionPane.WARNING_MESSAGE);
                        break;
                }

                UDDIProxiedServiceInfo.PublishType publishType = uddiProxyServiceInfo.getPublishType();
                switch (publishType){
                    case PROXY:
                        if(!removeUDDIProxiedService()) return false;
                        break;
                    case ENDPOINT:
                        if(!removeUDDIProxiedEndpoint()) return false;
                        break;
                    case OVERWRITE:
                        if(!removeUDDIOverwrittenProxiedService()) return false;
                        break;
                }
            }
        }

        if (uddiProxyServiceInfo != null && !dontPublishRadioButton.isSelected()) {
            // check if security zone update is needed but radio button selection did not change
            final SecurityZone existingZone = uddiProxyServiceInfo.getSecurityZone();
            final SecurityZone selectedZone = zoneControl.getSelectedZone();
            if ((existingZone == null && selectedZone != null) || (existingZone != null && !existingZone.equals(selectedZone))) {
                updateUddiProxiedServiceOnly(uddiRegistryAdmin);
            }
        }

        //The above processing of the SERVICE tab may have triggered an update to UDDI, in which case the dialog should dismiss.
        //Nothing below here should return false.
        return true;
    }

    private void updateUddiProxiedServiceOnly(UDDIRegistryAdmin uddiRegistryAdmin) {
        uddiProxyServiceInfo.setSecurityZone(zoneControl.getSelectedZone());
        try {
            uddiRegistryAdmin.updateProxiedServiceOnly(uddiProxyServiceInfo);
        } catch (UpdateException e) {
            showErrorMessage("Problem updating", "Problem updating UDDIProxiedService: " + e.getMessage(), e, true);
        } catch (FindException e) {
            showErrorMessage("Problem finding", "Problem finding UDDIProxiedService: " + e.getMessage(), e, true);
        }
    }

    private boolean doShowDeleteWarningBeforeTakingAction(final Callable<Boolean> callable) {
        final int proceed = JOptionPane.showConfirmDialog(this,
                "UDDI information failed to delete and is waiting to retry. Do you want to continue and possibly leave data in UDDI?",
                "Confirm Removal from UDDI",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if(proceed == JOptionPane.YES_OPTION){
            try {
                return callable.call();
            } catch (Exception e) {
                //all callable implementations must catch and handle their errors correctly. This should not happen
                logger.log(Level.WARNING, "Error attempting delete from UDDI: " + ExceptionUtils.getMessage(e));
            }
        }

        return false;
    }

    /**
     *
     * @return true if dialog can be disposed, false otherwise
     */
    private boolean removeUDDIProxiedEndpoint(){
        if( publishStatus.getPublishStatus() == UDDIPublishStatus.PublishStatus.DELETE_FAILED ){
            final Callable<Boolean> callable = new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return doRemoveProxiedEndpoint();
                }
            };
            return doShowDeleteWarningBeforeTakingAction(callable);
        }else{
            return doRemoveProxiedEndpoint();
        }
    }

    private boolean doRemoveProxiedEndpoint(){
        final boolean isGif = gifPublishCheckBox.isSelected();
        final int proceed = JOptionPane.showConfirmDialog(this,
                "Remove published Gateway " + ((isGif) ? "GIF " : "") + "endpoint from UDDI Registry?",
                "Confirm Removal from UDDI",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if(proceed == JOptionPane.YES_OPTION){
            UDDIRegistryAdmin uddiRegistryAdmin = Registry.getDefault().getUDDIRegistryAdmin();
            try {
                uddiRegistryAdmin.deleteGatewayEndpointFromUDDI(uddiProxyServiceInfo);
                JOptionPane.showMessageDialog(ServiceUDDISettingsDialog.this,
                        "Task to remove Gateway " + ((isGif) ? "GIF " : "") +
                                "endpoint from UDDI created successful",
                        "Successful Task Creation",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                final String msg = "Problem deleting published Gateway " + ((isGif) ? "GIF " : "") +
                        "endpoint from UDDI: " + ExceptionUtils.getMessage(ex);
                showErrorMessage("Error deleting from UDDI", msg, ex, true);
            }
            return true;
        }

        return false;
    }

    private boolean removeUDDIOverwrittenProxiedService(){
        if( publishStatus.getPublishStatus() == UDDIPublishStatus.PublishStatus.DELETE_FAILED ){
            final Callable<Boolean> callable = new Callable<Boolean>() {
                @Override
                public Boolean call() {
                    return doDeleteOverwrittenService();
                }
            };
            return doShowDeleteWarningBeforeTakingAction(callable);
        }else{
            return doDeleteOverwrittenService();
        }
    }

    private boolean doDeleteOverwrittenService() {
        final int proceed = JOptionPane.showConfirmDialog(this,
                "Remove gateway bindingTemplates from overwritten BusinessService in UDDI Registry?",
                "Confirm Removal from UDDI",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if(proceed == JOptionPane.YES_OPTION){
            UDDIRegistryAdmin uddiRegistryAdmin = Registry.getDefault().getUDDIRegistryAdmin();
            try {
                uddiRegistryAdmin.deleteGatewayWsdlFromUDDI(uddiProxyServiceInfo);
                JOptionPane.showMessageDialog(ServiceUDDISettingsDialog.this,
                        "Task to remove gateway bindingTemplates from overwritten BusinessService in UDDI created successful", "Successful Task Creation", JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception ex) {
                logger.log(Level.WARNING, "Problem deleting overwritten BusinessService UDDI: " + ex.getMessage());
                JOptionPane.showMessageDialog(ServiceUDDISettingsDialog.this, "Problem deleting overwritten BusinessService from UDDI Registry: " + ex.getMessage()
                        , "Error deleting from UDDI", JOptionPane.ERROR_MESSAGE);
            }
            return true;
        }
        return false;
    }

    private boolean removeUDDIProxiedService(){
        if( publishStatus.getPublishStatus() == UDDIPublishStatus.PublishStatus.DELETE_FAILED ){
            final Callable<Boolean> callable = new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return doRemoveUDDIProxiedService();
                }
            };
            return doShowDeleteWarningBeforeTakingAction(callable);
        }else{
            return doRemoveUDDIProxiedService();
        }
    }

    private boolean doRemoveUDDIProxiedService(){
        final int proceed = JOptionPane.showConfirmDialog(this,
                "Remove published Gateway WSDL from UDDI Registry?",
                "Confirm Removal from UDDI",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if(proceed == JOptionPane.YES_OPTION){
            final UDDIRegistryAdmin uddiRegistryAdmin = Registry.getDefault().getUDDIRegistryAdmin();
            try {
                uddiRegistryAdmin.deleteGatewayWsdlFromUDDI(uddiProxyServiceInfo);
                JOptionPane.showMessageDialog(ServiceUDDISettingsDialog.this,
                        "Task to remove Gateway WSDL from UDDI created successful", "Successful Task Creation", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                showErrorMessage("Error deleting from UDDI",
                        "Problem deleting pubished Gateway WSDL from UDDI: " + ExceptionUtils.getMessage(ex),
                        ex, true);
            }

            return true;
        }
        return false;
    }

    private boolean publishUDDIProxiedService(){
        final int result = JOptionPane.showConfirmDialog(this,
                "Publish Gateway WSDL to UDDI Registry?",
                "Confirm Publish to UDDI Task",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if(result == JOptionPane.YES_OPTION){
            UDDIRegistryAdmin uddiRegistryAdmin = Registry.getDefault().getUDDIRegistryAdmin();
            UDDIRegistry uddiRegistry = allRegistries.get(uddiRegistriesComboBox.getSelectedItem().toString());
            try {
                uddiRegistryAdmin.publishGatewayWsdl(service, uddiRegistry.getOid(), selectedBusinessKey, selectedBusinessName, updateWhenGatewayWSDLCheckBox.isSelected(), zoneControl.getSelectedZone());

                JOptionPane.showMessageDialog(ServiceUDDISettingsDialog.this,
                    "Task to publish Gateway WSDL to UDDI created successfully", "Successful Task Creation", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                final String msg = "Could not create publish gateway WSDL to UDDI task: " + ExceptionUtils.getMessage(ex);
                showErrorMessage("Error publishing to UDDI", msg, ex, true);
            }
            return true;
        }
        return false;
    }

    private boolean overwriteExistingService() {

        if (othersAffectedByModifyingOriginal()) {
            final int proceed = JOptionPane.showConfirmDialog(this,
                    "Other published services have the same original service from UDDI. Modifying the original may cause problems. Continue?",
                    "Confirm modification of original business service",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (proceed != JOptionPane.YES_OPTION) {
                return false;
            }
        }

        final int result = JOptionPane.showConfirmDialog(ServiceUDDISettingsDialog.this,
                "Overwrite existing BusinessService in UDDI with corresponding Gateway WSDL information?",
                "Confirm UDDI Overwrite Task",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (result == JOptionPane.YES_OPTION) {
            UDDIRegistryAdmin uddiRegistryAdmin = Registry.getDefault().getUDDIRegistryAdmin();
            try {
                uddiRegistryAdmin.overwriteBusinessServiceInUDDI(service, updateWhenGatewayWSDLCheckBoxOverwrite.isSelected(), zoneControl.getSelectedZone());
                JOptionPane.showMessageDialog(ServiceUDDISettingsDialog.this,
                        "Task to overwrite BusinessService in UDDI created successfully", "Successful Task Creation", JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception ex) {
                final String msg = "Could not create overwrite BusinessService in UDDI task: " + ExceptionUtils.getMessage(ex);
                showErrorMessage("Error overwriting UDDI", msg, ex, true);
            }
            return true;
        }
        return false;
    }

    private boolean publishEndpointToBusinessService() {
        final String msg;
        final int messageType;
        final boolean isGif = gifPublishCheckBox.isSelected();

        if (removeExistingBindingsCheckBox.isSelected()) {
            msg = "Publish Gateway endpoint to owning BusinessService in UDDI? Remove existing bindings?";
            messageType = JOptionPane.WARNING_MESSAGE;
        } else {
            msg = "Publish Gateway " + ((isGif) ? "GIF " : "") + "endpoint to owning BusinessService in UDDI?";
            messageType = JOptionPane.QUESTION_MESSAGE;
        }

        if(othersAffectedByModifyingOriginal()) {
            final int proceed = JOptionPane.showConfirmDialog(this,
                    "Other published services have the same original service from UDDI. Modifying the original may cause problems. Continue?",
                    "Confirm modification of original business service",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if(proceed != JOptionPane.YES_OPTION){
                return false;
            }
        }

        final int result = JOptionPane.showConfirmDialog(ServiceUDDISettingsDialog.this,
                msg,
                "Confirm Publish Endpoint to UDDI Task",
                JOptionPane.YES_NO_OPTION,
                messageType);

        if(result == JOptionPane.YES_OPTION){
            UDDIRegistryAdmin uddiRegistryAdmin = Registry.getDefault().getUDDIRegistryAdmin();
            try {
                final Map<String, Object> props = new HashMap<String, Object>();
                props.put(UDDIProxiedServiceInfo.KEYED_REFERENCES_CONFIG, keyedReferenceSet);//ok if null
                if(isGif){
                    props.put(UDDIProxiedServiceInfo.GIF_SCHEME, endPointTypeComboBox.getSelectedItem());
                    uddiRegistryAdmin.publishGatewayEndpointGif(service, props, zoneControl.getSelectedZone());
                } else {
                    uddiRegistryAdmin.publishGatewayEndpoint(service, removeExistingBindingsCheckBox.isSelected(), props, zoneControl.getSelectedZone());
                }

                JOptionPane.showMessageDialog(ServiceUDDISettingsDialog.this,
                        "Task to publish Gateway " + ((isGif) ? "GIF " : "") +
                                "endpoint to UDDI created successfully",
                        "Successful Task Creation",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                final String msg1 = "Could not create publish gateway " + ((isGif) ? "GIF " : "")
                        + "endpoint to UDDI task: " + ExceptionUtils.getMessage(ex);
                showErrorMessage("Error publishing to UDDI", msg1, ex, true);
            }
            return true;
        }
        return false;
    }

    private boolean othersAffectedByModifyingOriginal(){
        try {
            final Collection<UDDIServiceControl> controls =
                    Registry.getDefault().getUDDIRegistryAdmin().getAllServiceControlsForRegistry(uddiServiceControl.getUddiRegistryOid());

            int numOfAffectedControls = 0;
            for(UDDIServiceControl control: controls){
                if(control.getUddiServiceKey().equalsIgnoreCase(uddiServiceControl.getUddiServiceKey())){
                    numOfAffectedControls++;
                }
            }

            return numOfAffectedControls > 1;

        } catch (FindException e) {
            throw new RuntimeException("Cannot check if other published services have the same original service from UDDI", e);
        }
    }

    private void onOK() {
        if(viewToModel()) {
            confirmed = true;
            dispose();
        }
    }

    private void loadUddiRegistries() {
        try {
            UDDIRegistryAdmin uddiRegistryAdmin = getUDDIRegistryAdmin();

            allRegistries = new HashMap<String, UDDIRegistry>();
            Collection<UDDIRegistry> registries = uddiRegistryAdmin.findAllUDDIRegistries();
            for (UDDIRegistry uddiRegistry : registries){
                uddiRegistriesComboBox.addItem(uddiRegistry.getName());
                allRegistries.put(uddiRegistry.getName(), uddiRegistry);
            }

        } catch (FindException e) {
            showErrorMessage("Loading failed", "Unable to list all UDDI Registry: " + ExceptionUtils.getMessage(e), e, true);
        }
    }

    private boolean isMetricsEnabled( long registryOid ) {
        Boolean enabled = registryMetricsEnabled.get( registryOid );

        if ( enabled == null ) {
            UDDIRegistryAdmin uddiRegistryAdmin = getUDDIRegistryAdmin();
            try {
                enabled = uddiRegistryAdmin.metricsAvailable( registryOid );
            } catch (FindException e) {
                enabled = false;
            }
            registryMetricsEnabled.put( registryOid, enabled );
        }

        return enabled;
    }

    public boolean isProxyMetricsEnabled() {
        return metricsEnabledCheckBox.isEnabled() && metricsEnabledCheckBox.isSelected();
    }

    public boolean isProxyPublishWsPolicyEnabled() {
        return publishPolicyCheckBox.isEnabled() && publishPolicyCheckBox.isSelected() &&
                ( OPTION_PUBLISHED_BUSINESS_SERVICES.equals( policyServiceComboBox.getSelectedItem() ) ||
                  OPTION_BOTH_BUSINESS_SERVICES.equals( policyServiceComboBox.getSelectedItem() ) );
    }

    public boolean isPublishWsPolicyEnabled() {
        return publishPolicyCheckBox.isEnabled() && publishPolicyCheckBox.isSelected() &&
                ( OPTION_ORIGINAL_BUSINESS_SERVICE.equals( policyServiceComboBox.getSelectedItem() ) ||
                  OPTION_BOTH_BUSINESS_SERVICES.equals( policyServiceComboBox.getSelectedItem() ) );
    }

    /**
     * Settings from each entity related to a single UI control so it is assumed that
     * when both entities exist they have the same configuration options for policy
     * publishing.
     */
    private boolean getPublishFullPolicy( final UDDIServiceControl uddiServiceControl,
                                          final UDDIProxiedServiceInfo uddiProxyServiceInfo ) {
        boolean publishFull = true;

        if ( uddiServiceControl != null && uddiServiceControl.isPublishWsPolicyEnabled() ) {
            publishFull = uddiServiceControl.isPublishWsPolicyFull();
        } else if ( uddiProxyServiceInfo != null && uddiProxyServiceInfo.isPublishWsPolicyEnabled() ) {
            publishFull = uddiProxyServiceInfo.isPublishWsPolicyFull();
        }

        return publishFull;
    }

    private boolean getPublishInlinedPolicy( final boolean optionEnabled,
                                             final UDDIServiceControl uddiServiceControl,
                                             final UDDIProxiedServiceInfo uddiProxyServiceInfo ) {
        boolean publishInlined = false;

        if ( optionEnabled ) {
            if ( uddiServiceControl != null && uddiServiceControl.isPublishWsPolicyEnabled() ) {
                publishInlined = uddiServiceControl.isPublishWsPolicyInlined();
            } else if ( uddiProxyServiceInfo != null && uddiProxyServiceInfo.isPublishWsPolicyEnabled()  ) {
                publishInlined = uddiProxyServiceInfo.isPublishWsPolicyInlined();
            }
        }

        return publishInlined;
    }

    private boolean publishWsPolicySettingsChanged() {
        boolean publishFull = publishFullPolicyCheckBox.isEnabled() && publishFullPolicyCheckBox.isSelected();
        boolean publishInline = inlinePolicyIncludesCheckBox.isEnabled() && inlinePolicyIncludesCheckBox.isSelected();

        return
                uddiProxyServiceInfo.isPublishWsPolicyEnabled() != isProxyPublishWsPolicyEnabled() ||
                uddiProxyServiceInfo.isPublishWsPolicyFull() != publishFull ||
                uddiProxyServiceInfo.isPublishWsPolicyInlined() != publishInline;
    }


    private void showErrorMessage(String title, String msg, Throwable e, boolean log) {
        if(log) logger.log(Level.WARNING, msg, ExceptionUtils.getDebugException(e));
        JOptionPane.showMessageDialog(this, msg, title, JOptionPane.ERROR_MESSAGE);
    }

    /** @return the UDDIRegistryAdmin interface*/
    private UDDIRegistryAdmin getUDDIRegistryAdmin() {
        return Registry.getDefault().getUDDIRegistryAdmin();
    }
    
    private boolean areUnsavedChangesToThisPolicy() {
        PolicyEditorPanel pep = TopComponents.getInstance().getPolicyEditorPanel();
        return pep != null && pep.isEditingPublishedService() && service.getOid() == pep.getPublishedServiceOid() && pep.isUnsavedChanges();
    }

    /** @return true if the dialog has been dismissed with the ok button */
    public boolean isConfirmed() {
        return confirmed;
    }
    
    public static void main(String[] args) {
        ServiceUDDISettingsDialog dialog = new ServiceUDDISettingsDialog();
        dialog.pack();
        dialog.setVisible(true);
        System.exit(0);
    }
}
