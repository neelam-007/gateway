package com.l7tech.console.panels;

import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.uddi.UDDIRegistry;
import com.l7tech.gateway.common.uddi.UDDIServiceControl;
import com.l7tech.gateway.common.uddi.UDDIProxiedServiceInfo;
import com.l7tech.gateway.common.uddi.UDDIPublishStatus;
import com.l7tech.gateway.common.admin.UDDIRegistryAdmin;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.util.Registry;
import com.l7tech.console.poleditor.PolicyEditorPanel;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.uddi.UDDINamedEntity;

import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.util.Map;
import java.util.Collection;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServiceUDDISettingsDialog extends JDialog {
    private static final Logger logger = Logger.getLogger(ServiceUDDISettingsDialog.class.getName());
    private static final ResourceBundle resources = ResourceBundle.getBundle(ServiceUDDISettingsDialog.class.getName());
    private static final String OPTION_ORIGINAL_BUSINESS_SERVICE = "Original Business Service";
    private static final String OPTION_PROXIED_BUSINESS_SERVICE = "Proxied Business Service";
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
    private JComboBox metricsServiceComboBox;
    private JLabel metricsServiceLabel;
    private JCheckBox publishPolicyCheckBox;
    private JLabel policyServiceLabel;
    private JComboBox policyServiceComboBox;
    private JCheckBox publishFullPolicyCheckBox;
    private JCheckBox inlinePolicyIncludesCheckBox;

    private PublishedService service;
    private boolean canUpdate;
    private boolean confirmed;
    private Map<String, UDDIRegistry> allRegistries;
    private Map<Long, Boolean> registryMetricsEnabled = new HashMap<Long,Boolean>();
    private UDDIProxiedServiceInfo uddiProxyServiceInfo;
    private UDDIServiceControl uddiServiceControl;
    private InputValidator publishWsdlValidators;

    private final static String businessEntityDefault = "<<None Selected>>";
    private String selectedBusinessName;
    private String selectedBusinessKey;
    private UDDIPublishStatus publishStatus;

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

        RunOnChangeListener enableDisableChangeListener = new RunOnChangeListener( new Runnable(){
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

        //Populate registry drop down
        loadUddiRegistries();

        //determine if there is a UDDIProxiedService for this service
        try {
            uddiProxyServiceInfo = getUDDIRegistryAdmin().getUDDIProxiedServiceInfo(service.getOid());
            if(uddiProxyServiceInfo != null){
                try {
                    publishStatus =
                            Registry.getDefault().getUDDIRegistryAdmin().getPublishStatusForProxy(uddiProxyServiceInfo.getOid());
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
        } catch (FindException e) {
            uddiServiceControl = null;
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

        Utilities.setEscKeyStrokeDisposes(this);

        pack();
        modelToView();
        enableAndDisableComponents();
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
                    setLabelStatus(bindingTemplateStatusLabel);
                    break;
                case OVERWRITE:
                    break;
            }

        }else{
            //set other buttons values when implemented
            dontPublishRadioButton.setSelected(true);
            uddiRegistriesComboBox.setSelectedIndex(-1);
            businessEntityNameLabel.setText(businessEntityDefault);
            clearStatusLabels(null);
        }
        
        // WS-Policy settings
        boolean originalWsPolicyAvailable = uddiServiceControl != null;
        boolean proxyWsPolicyAvailable = uddiProxyServiceInfo != null;
        boolean originalWsPolicyEnabled = uddiServiceControl != null && uddiServiceControl.isPublishWsPolicyEnabled();
        boolean proxyWsPolicyEnabled = uddiProxyServiceInfo != null && uddiProxyServiceInfo.isPublishWsPolicyEnabled(); 
        if ( originalWsPolicyAvailable || proxyWsPolicyAvailable ) {
            java.util.List<String> options = new ArrayList<String>();
            if ( originalWsPolicyAvailable ) options.add( OPTION_ORIGINAL_BUSINESS_SERVICE );
            if ( proxyWsPolicyAvailable ) options.add( OPTION_PROXIED_BUSINESS_SERVICE );
            if ( originalWsPolicyAvailable && proxyWsPolicyAvailable ) options.add( OPTION_BOTH_BUSINESS_SERVICES );
            policyServiceComboBox.setModel( new DefaultComboBoxModel( options.toArray(new String[options.size()] ) ));
            publishPolicyCheckBox.setSelected( originalWsPolicyEnabled || proxyWsPolicyEnabled );
            if ( originalWsPolicyEnabled && proxyWsPolicyEnabled ) policyServiceComboBox.setSelectedItem( OPTION_BOTH_BUSINESS_SERVICES );
            else if ( originalWsPolicyEnabled ) policyServiceComboBox.setSelectedItem( OPTION_ORIGINAL_BUSINESS_SERVICE );
            else if ( proxyWsPolicyEnabled ) policyServiceComboBox.setSelectedItem( OPTION_PROXIED_BUSINESS_SERVICE );
        }
        publishFullPolicyCheckBox.setSelected( getPublishFullPolicy(uddiServiceControl, uddiProxyServiceInfo) );
        inlinePolicyIncludesCheckBox.setSelected( getPublishInlinedPolicy(publishFullPolicyCheckBox.isSelected(), uddiServiceControl, uddiProxyServiceInfo) );

        // Metrics settings
        boolean originalMetricsAvailable = uddiServiceControl != null && isMetricsEnabled(uddiServiceControl.getUddiRegistryOid());
        boolean proxyMetricsAvailable = uddiProxyServiceInfo != null  && isMetricsEnabled(uddiProxyServiceInfo.getUddiRegistryOid());
        boolean originalMetricsEnabled = uddiServiceControl != null && uddiServiceControl.isMetricsEnabled();
        boolean proxyMetricsEnabled = uddiProxyServiceInfo != null && uddiProxyServiceInfo.isMetricsEnabled();
        if ( originalMetricsAvailable || proxyMetricsAvailable ) {
            java.util.List<String> options = new ArrayList<String>();
            if ( originalMetricsAvailable ) options.add( OPTION_ORIGINAL_BUSINESS_SERVICE );
            if ( proxyMetricsAvailable ) options.add( OPTION_PROXIED_BUSINESS_SERVICE );
            if ( originalMetricsAvailable && proxyMetricsAvailable ) options.add( OPTION_BOTH_BUSINESS_SERVICES );
            metricsServiceComboBox.setModel( new DefaultComboBoxModel( options.toArray(new String[options.size()] ) ));
            metricsEnabledCheckBox.setSelected( originalMetricsEnabled || proxyMetricsEnabled );
            if ( originalMetricsEnabled && proxyMetricsEnabled ) metricsServiceComboBox.setSelectedItem( OPTION_BOTH_BUSINESS_SERVICES );
            else if ( originalMetricsEnabled ) metricsServiceComboBox.setSelectedItem( OPTION_ORIGINAL_BUSINESS_SERVICE );
            else if ( proxyMetricsEnabled ) metricsServiceComboBox.setSelectedItem( OPTION_PROXIED_BUSINESS_SERVICE );
        }
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
                status = "Status: Publish failed "+ publishStatus.getFailCount() +" times. Set to retry";
                break;
            case CANNOT_PUBLISH:
                status = "Status: Cannot publish. Tried "+ publishStatus.getFailCount() +" times. Please select 'Dont Publish' to retry";
                break;
            case DELETE:
                status = "Status: Deleting";
                break;
            case DELETE_FAILED:
                status = "Status: Delete failed "+ publishStatus.getFailCount() +" times. Set to retry";
                break;
            case CANNOT_DELETE:
                status = "Status: Cannot delete. Tried "+ publishStatus.getFailCount() +" times. Please select 'Dont Publish' to retry";
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
        if(publishType == null || publishType != UDDIProxiedServiceInfo.PublishType.PROXY) publishProxystatusLabel.setText("");
        if(publishType == null || publishType != UDDIProxiedServiceInfo.PublishType.ENDPOINT) bindingTemplateStatusLabel.setText("");
        if(publishType == null || publishType != UDDIProxiedServiceInfo.PublishType.OVERWRITE) overwriteStatusLabel.setText("");
    }
    
    /**
     * Enable or disable UI components in the 'Publish' tab only
     */
    private void enableDisablePublishTabComponents(){

        //disable everything first
        enableDisablePublishGatewayWsdlControls(false);
        enableDisablePublishEndpointControls(false);
        enableDisablePublishOverwriteControls(false);

        if(publishProxiedWsdlRadioButton.isSelected()){
            final boolean proxyPublished = uddiProxyServiceInfo != null &&
                    uddiProxyServiceInfo.getPublishType() == UDDIProxiedServiceInfo.PublishType.PROXY;
            enableDisablePublishGatewayWsdlControls(!proxyPublished);
            updateWhenGatewayWSDLCheckBox.setEnabled(true);
        }else if(publishGatewayEndpointAsRadioButton.isSelected()){
            final boolean endPointPublished = uddiProxyServiceInfo != null &&
                    uddiProxyServiceInfo.getPublishType() == UDDIProxiedServiceInfo.PublishType.ENDPOINT;
            enableDisablePublishEndpointControls(!endPointPublished);
        }else if(overwriteExistingBusinessServiceWithRadioButton.isSelected()){
            final boolean overwritePublished = uddiProxyServiceInfo != null &&
                    uddiProxyServiceInfo.getPublishType() == UDDIProxiedServiceInfo.PublishType.OVERWRITE;
            enableDisablePublishOverwriteControls(!overwritePublished);
        } else if(dontPublishRadioButton.isSelected()){
            //enable all applicable radio buttons
            if(uddiProxyServiceInfo != null){
                final UDDIProxiedServiceInfo.PublishType type = uddiProxyServiceInfo.getPublishType();
                switch(type){
                    case PROXY:
                        publishProxiedWsdlRadioButton.setEnabled(true);
                        break;
                    case ENDPOINT:
                        publishGatewayEndpointAsRadioButton.setEnabled(true);
                        break;
                    case OVERWRITE:
                        overwriteExistingBusinessServiceWithRadioButton.setEnabled(true);
                        break;
                }
            }else{
                publishProxiedWsdlRadioButton.setEnabled(true);
                final boolean uddiControl = uddiServiceControl != null;
                publishGatewayEndpointAsRadioButton.setEnabled(uddiControl);
                overwriteExistingBusinessServiceWithRadioButton.setEnabled(uddiControl);
            }
        }
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
            inlinePolicyIncludesCheckBox.setEnabled( enablePolicyOptions );

            //metrics tab
            boolean enableMetrics = metricsServiceComboBox.getModel().getSize() > 0;
            metricsEnabledCheckBox.setEnabled( enableMetrics );
            metricsServiceComboBox.setEnabled( enableMetrics && metricsEnabledCheckBox.isSelected() );
            metricsServiceLabel.setEnabled( enableMetrics && metricsEnabledCheckBox.isSelected() );
        } else {
            //publish tab
            enableDisablePublishGatewayWsdlControls(false);
            enableDisablePublishEndpointControls(false);
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
            metricsServiceComboBox.setEnabled( false );
            metricsServiceLabel.setEnabled( false );            
        }
    }

    private void enableDisablePublishGatewayWsdlControls(boolean enable){
        //publish proxy
        publishProxiedWsdlRadioButton.setEnabled(enable);
        uddiRegistryLabel.setEnabled(enable);
        uddiRegistriesComboBox.setEnabled(enable);
        businessEntityLabel.setEnabled(enable);
        businessEntityNameLabel.setEnabled(enable);
        selectBusinessEntityButton.setEnabled(enable);
        updateWhenGatewayWSDLCheckBox.setEnabled(enable);
    }

    private void enableDisablePublishEndpointControls(boolean enable){
        //binding endpoint
        publishGatewayEndpointAsRadioButton.setEnabled(enable);
        if(uddiServiceControl != null && uddiServiceControl.isUnderUddiControl()){
            removeExistingBindingsCheckBox.setEnabled(false);
        }else{
            removeExistingBindingsCheckBox.setEnabled(enable);
        }
    }

    private void enableDisablePublishOverwriteControls(boolean enable){
        //overwrite
        overwriteExistingBusinessServiceWithRadioButton.setEnabled(false);//todo change when implemented
        updateWhenGatewayWSDLCheckBoxOverwrite.setEnabled(false); //todo change when implemented
    }

    /**
     * Validate the view and update model if valid.
     * @return true if view is valid and can be converted to model, false otherwise
     */
    private boolean viewToModel(){
        final UDDIRegistryAdmin uddiRegistryAdmin = Registry.getDefault().getUDDIRegistryAdmin();

        if(publishProxiedWsdlRadioButton.isSelected()){
            if(uddiProxyServiceInfo == null){
                if(publishWsdlValidators.validateWithDialog()) publishUDDIProxiedService();
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

                    try {
                        uddiRegistryAdmin.updateProxiedServiceOnly(uddiProxyServiceInfo);
                    } catch (UpdateException e) {
                        logger.log(Level.WARNING, "Problem updating UDDIProxiedService: " + e.getMessage());
                        DialogDisplayer.showMessageDialog(this, "Problem updating Gateway: " + e.getMessage()
                                , "Problem updating", JOptionPane.ERROR_MESSAGE, null);
                    } catch (FindException e) {
                        logger.log(Level.WARNING, "Problem finding UDDIProxiedService: " + e.getMessage());
                        DialogDisplayer.showMessageDialog(this, "Problem finding UDDIProxiedService: " + e.getMessage()
                                , "Problem finding", JOptionPane.ERROR_MESSAGE, null);
                    }
                }
            }
        }else if(publishGatewayEndpointAsRadioButton.isSelected()){
            if(uddiProxyServiceInfo == null){
                //publish for first time
                return publishEndpointToBusinessService();
            }
        }else if(overwriteExistingBusinessServiceWithRadioButton.isSelected()){

        } else if(dontPublishRadioButton.isSelected()){
            if(uddiProxyServiceInfo != null){
                UDDIRegistry uddiRegistry = allRegistries.get(uddiRegistriesComboBox.getSelectedItem().toString());
                if(!uddiRegistry.isEnabled()){
                    showErrorMessage("Cannot Update UDDI", "UDDI Registry is not currently enabled", null, false);
                    return false;
                }
                if(publishStatus.getPublishStatus() == UDDIPublishStatus.PublishStatus.PUBLISH){
                    showErrorMessage("Cannot Update UDDI", "UDDI is being updated. Please close dialog and try again in a few minutes", null, false);
                    return false;
                }

                UDDIProxiedServiceInfo.PublishType publishType = uddiProxyServiceInfo.getPublishType();
                switch (publishType){
                    case PROXY:
                        removeUDDIProxiedService();
                        break;
                    case ENDPOINT:
                        removeUDDIProxiedEndpoint();
                        break;
                    case OVERWRITE:
                        //todo let user know we cannot undo, we will just delete from UDDI
                        break;
                }
            }
        }

        if ( uddiServiceControl != null ) {
            uddiServiceControl.setMetricsEnabled( isMetricsEnabled() );
            if ( isPublishWsPolicyEnabled() ) {
                uddiServiceControl.setPublishWsPolicyEnabled( true );
                uddiServiceControl.setPublishWsPolicyFull( publishFullPolicyCheckBox.isSelected() );
                uddiServiceControl.setPublishWsPolicyInlined( publishFullPolicyCheckBox.isSelected() && inlinePolicyIncludesCheckBox.isSelected() );
            } else {
                uddiServiceControl.setPublishWsPolicyEnabled( false );
                uddiServiceControl.setPublishWsPolicyFull( false );
                uddiServiceControl.setPublishWsPolicyInlined( false );
            }
            try {
                uddiRegistryAdmin.saveUDDIServiceControlOnly( uddiServiceControl );
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error saving UDDIServiceControl '" + ExceptionUtils.getMessage(e) + "'.", e);
                DialogDisplayer.showMessageDialog(this, "Error saving UDDI settings: " + ExceptionUtils.getMessage(e)
                        , "Error saving settings", JOptionPane.ERROR_MESSAGE, null);
            }
        }

        return true;
    }

    private void removeUDDIProxiedEndpoint(){
        DialogDisplayer.showConfirmDialog(this,
                                                   "Remove published Gateway endpoint from UDDI Registry?",
                                                   "Confirm Removal from UDDI",
                                                   JOptionPane.YES_NO_OPTION,
                                                   JOptionPane.QUESTION_MESSAGE, new DialogDisplayer.OptionListener() {
                    @Override
                    public void reportResult(int option) {
                        if (option == JOptionPane.YES_OPTION){
                            UDDIRegistryAdmin uddiRegistryAdmin = Registry.getDefault().getUDDIRegistryAdmin();
                            try {
                                uddiRegistryAdmin.deleteGatewayEndpointFromUDDI(uddiProxyServiceInfo);
                                DialogDisplayer.showMessageDialog(ServiceUDDISettingsDialog.this,
                                        "Task to remove Gateway endpoint from UDDI successful", "Successful Task Creation", JOptionPane.INFORMATION_MESSAGE, null);
                            } catch (Exception ex) {
                                final String msg = "Problem deleting pubished Gateway endpoint from UDDI: " + ExceptionUtils.getMessage(ex);
                                logger.log(Level.WARNING, msg);
                                DialogDisplayer.showMessageDialog(ServiceUDDISettingsDialog.this, msg
                                        , "Error deleting from UDDI", JOptionPane.ERROR_MESSAGE, null);
                            }
                        }
                    }
                });
    }

    private void removeUDDIProxiedService(){
        DialogDisplayer.showConfirmDialog(this,
                                                   "Remove published Gateway WSDL from UDDI Registry?",
                                                   "Confirm Removal from UDDI",
                                                   JOptionPane.YES_NO_OPTION,
                                                   JOptionPane.QUESTION_MESSAGE, new DialogDisplayer.OptionListener() {
                    @Override
                    public void reportResult(int option) {
                        if (option == JOptionPane.YES_OPTION){
                            UDDIRegistryAdmin uddiRegistryAdmin = Registry.getDefault().getUDDIRegistryAdmin();
                            try {
                                uddiRegistryAdmin.deleteGatewayWsdlFromUDDI(uddiProxyServiceInfo);
                                DialogDisplayer.showMessageDialog(ServiceUDDISettingsDialog.this,
                                        "Task to removal Gateway WSDL from UDDI created successful", "Successful Task Creation", JOptionPane.INFORMATION_MESSAGE, null);

                            } catch (Exception ex) {
                                logger.log(Level.WARNING, "Problem deleting pubished Gateway WSDL from UDDI: " + ex.getMessage());
                                DialogDisplayer.showMessageDialog(ServiceUDDISettingsDialog.this, "Problem deleting Gateway WSDL from UDDI Registry: " + ex.getMessage()
                                        , "Error deleting from UDDI", JOptionPane.ERROR_MESSAGE, null);
                            }
                        }
                    }
                });
    }

    private void publishUDDIProxiedService(){
        DialogDisplayer.showConfirmDialog(this,
                                                   "Publish Gateway WSDL to UDDI Registry?",
                                                   "Confirm Publish to UDDI Task",
                                                   JOptionPane.YES_NO_OPTION,
                                                   JOptionPane.QUESTION_MESSAGE, new DialogDisplayer.OptionListener() {
                    @Override
                    public void reportResult(int option) {
                        if (option == JOptionPane.YES_OPTION){
                            UDDIRegistryAdmin uddiRegistryAdmin = Registry.getDefault().getUDDIRegistryAdmin();
                            UDDIRegistry uddiRegistry = allRegistries.get(uddiRegistriesComboBox.getSelectedItem().toString());
                            try {
                                uddiRegistryAdmin.publishGatewayWsdl(service.getOid(), uddiRegistry.getOid(), selectedBusinessKey, selectedBusinessName, updateWhenGatewayWSDLCheckBox.isSelected());

                                DialogDisplayer.showMessageDialog(ServiceUDDISettingsDialog.this,
                                        "Task to publish Gateway WSDL to UDDI created successfully", "Successful Task Creation", JOptionPane.INFORMATION_MESSAGE, null);
                            } catch (Exception ex) {
                                final String msg = "Could not create publish gateway WSDL to UDDI task: " + ExceptionUtils.getMessage(ex);
                                logger.log(Level.WARNING, msg, ExceptionUtils.getDebugException(ex));
                                DialogDisplayer.showMessageDialog(ServiceUDDISettingsDialog.this, msg,
                                        "Error publishing to UDDI", JOptionPane.ERROR_MESSAGE, null);
                            }
                        }
                    }
                });
    }

    private boolean publishEndpointToBusinessService() {
        final String msg;
        final int messageType;

        if (removeExistingBindingsCheckBox.isSelected()) {
            msg = "Publish Gateway endpoint to owning BusinessService in UDDI? Remove existing bindings?";
            messageType = JOptionPane.WARNING_MESSAGE;
        } else {
            msg = "Publish Gateway endpoint to owning BusinessService in UDDI?";
            messageType = JOptionPane.QUESTION_MESSAGE;
        }

        final boolean [] choice = new boolean[1];
        DialogDisplayer.showConfirmDialog(this,
                msg,
                "Confirm Publish Endpoint to UDDI Task",
                JOptionPane.YES_NO_OPTION,
                messageType, new DialogDisplayer.OptionListener() {
                    @Override
                    public void reportResult(int option) {
                        if (option == JOptionPane.YES_OPTION) {
                            choice[0] = true;
                            UDDIRegistryAdmin uddiRegistryAdmin = Registry.getDefault().getUDDIRegistryAdmin();
                            try {
                                uddiRegistryAdmin.publishGatewayEndpoint(service.getOid(), removeExistingBindingsCheckBox.isSelected());

                                DialogDisplayer.showMessageDialog(ServiceUDDISettingsDialog.this,
                                        "Task to publish Gateway endpoint to UDDI created successfully", "Successful Task Creation", JOptionPane.INFORMATION_MESSAGE, null);
                            } catch (Exception ex) {
                                final String msg = "Could not create publish gateway endpoint to UDDI task: " + ExceptionUtils.getMessage(ex);
                                logger.log(Level.WARNING, msg, ExceptionUtils.getDebugException(ex));
                                DialogDisplayer.showMessageDialog(ServiceUDDISettingsDialog.this, msg,
                                        "Error publishing to UDDI", JOptionPane.ERROR_MESSAGE, null);
                            }
                        }
                    }
                });
        return choice[0];
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
        return metricsEnabledCheckBox.isEnabled() && metricsEnabledCheckBox.isSelected() &&
                ( OPTION_PROXIED_BUSINESS_SERVICE.equals( metricsServiceComboBox.getSelectedItem() ) ||
                  OPTION_BOTH_BUSINESS_SERVICES.equals( metricsServiceComboBox.getSelectedItem() ) );
    }

    public boolean isMetricsEnabled() {
        return metricsEnabledCheckBox.isEnabled() && metricsEnabledCheckBox.isSelected() &&
                ( OPTION_ORIGINAL_BUSINESS_SERVICE.equals( metricsServiceComboBox.getSelectedItem() ) ||
                  OPTION_BOTH_BUSINESS_SERVICES.equals( metricsServiceComboBox.getSelectedItem() ) );
    }

    public boolean isProxyPublishWsPolicyEnabled() {
        return publishPolicyCheckBox.isEnabled() &&publishPolicyCheckBox.isSelected() &&
                ( OPTION_PROXIED_BUSINESS_SERVICE.equals( policyServiceComboBox.getSelectedItem() ) ||
                  OPTION_BOTH_BUSINESS_SERVICES.equals( policyServiceComboBox.getSelectedItem() ) );
    }

    public boolean isPublishWsPolicyEnabled() {
        return publishPolicyCheckBox.isEnabled() &&publishPolicyCheckBox.isSelected() &&
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
                uddiProxyServiceInfo.isPublishWsPolicyEnabled() != publishPolicyCheckBox.isSelected() ||
                uddiProxyServiceInfo.isPublishWsPolicyFull() != publishFull ||
                uddiProxyServiceInfo.isPublishWsPolicyInlined() != publishInline;
    }


    private void showErrorMessage(String title, String msg, Throwable e, boolean log) {
        showErrorMessage(title, msg, e, null, log);
    }

    private void showErrorMessage(String title, String msg, Throwable e, Runnable continuation, boolean log) {
        if(log) logger.log(Level.WARNING, msg, e);
        DialogDisplayer.showMessageDialog(this, msg, title, JOptionPane.ERROR_MESSAGE, continuation);
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
