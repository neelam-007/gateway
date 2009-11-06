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
    private JCheckBox monitoringEnabledCheckBox;
    private JCheckBox monitoringDisableServicecheckBox;
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
    private UDDIPublishStatus.PublishStatus publishStatus;

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
        monitoringEnabledCheckBox.addActionListener( enableDisableChangeListener );
        metricsEnabledCheckBox.addActionListener( enableDisableChangeListener );

        //Populate registry drop down
        loadUddiRegistries();

        //determine if there is a UDDIProxiedService for this service
        try {
            uddiProxyServiceInfo = getUDDIRegistryAdmin().getUDDIProxiedServiceInfo(service.getOid());
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
                    publishStatus = uddiProxyServiceInfo.getUddiPublishStatus().getPublishStatus();
                    setLabelStatus(publishProxystatusLabel, publishStatus);
                    break;
                case ENDPOINT:
                    publishGatewayEndpointAsRadioButton.setSelected(true);
                    publishStatus = uddiProxyServiceInfo.getUddiPublishStatus().getPublishStatus();
                    setLabelStatus(bindingTemplateStatusLabel, publishStatus);
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
        
        // Monitoring settings
        if ( uddiServiceControl != null ) {
            monitoringEnabledCheckBox.setSelected( uddiServiceControl.isMonitoringEnabled() );
            monitoringDisableServicecheckBox.setSelected( uddiServiceControl.isDisableServiceOnChange() );
        } else {
            monitoringEnabledCheckBox.setSelected( false );
            monitoringDisableServicecheckBox.setSelected( false );
        }
        
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
            if ( originalMetricsEnabled ) metricsServiceComboBox.setSelectedItem( OPTION_ORIGINAL_BUSINESS_SERVICE );
            if ( proxyMetricsEnabled ) metricsServiceComboBox.setSelectedItem( OPTION_PROXIED_BUSINESS_SERVICE );
            if ( originalMetricsEnabled && proxyMetricsEnabled ) metricsServiceComboBox.setSelectedItem( OPTION_BOTH_BUSINESS_SERVICES );
        }
    }

    private void setLabelStatus(JLabel label, UDDIPublishStatus.PublishStatus publishStatus) {
        final String status;
        switch (publishStatus) {
            case PUBLISHING:
                status = "Status: Publishing";
                break;
            case DELETING:
                status = "Status: Deleting";
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
                final boolean uddiControl = uddiServiceControl != null && uddiServiceControl.isUnderUddiControl();
                publishGatewayEndpointAsRadioButton.setEnabled(uddiControl);
                overwriteExistingBusinessServiceWithRadioButton.setEnabled(uddiControl);
            }
        }
    }    

    private void enableAndDisableComponents() {
        if ( canUpdate ) {
            //configure enable / disable for publish tab
            enableDisablePublishTabComponents();

            // monitoring
            boolean enableMonitoring = uddiServiceControl != null && uddiServiceControl.isUnderUddiControl();
            monitoringEnabledCheckBox.setEnabled( enableMonitoring );
            monitoringDisableServicecheckBox.setEnabled( enableMonitoring && monitoringEnabledCheckBox.isSelected() );

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

            //monitor tab
            monitoringEnabledCheckBox.setEnabled( false );
            monitoringDisableServicecheckBox.setEnabled( false );

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
        removeExistingBindingsCheckBox.setEnabled(enable);
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
                   uddiProxyServiceInfo.isMetricsEnabled() != isProxyMetricsEnabled() ){
                    uddiProxyServiceInfo.setUpdateProxyOnLocalChange(updateWhenGatewayWSDLCheckBox.isSelected());
                    uddiProxyServiceInfo.setMetricsEnabled(isProxyMetricsEnabled());
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
                publishEndpointToBusinessService();
            }
        }else if(overwriteExistingBusinessServiceWithRadioButton.isSelected()){

        } else if(dontPublishRadioButton.isSelected()){
            if(uddiProxyServiceInfo != null){
                publishStatus = uddiProxyServiceInfo.getUddiPublishStatus().getPublishStatus();//this is resetting, but no harm

                UDDIRegistry uddiRegistry = allRegistries.get(uddiRegistriesComboBox.getSelectedItem().toString());
                if(!uddiRegistry.isEnabled()){
                    showErrorMessage("Cannot Update UDDI", "UDDI Registry is not currently enabled", null, false);
                    return false;
                }
                if(publishStatus != UDDIPublishStatus.PublishStatus.PUBLISHED){
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
            if ( monitoringEnabledCheckBox.isSelected() ) {
                uddiServiceControl.setMonitoringEnabled( true );
                uddiServiceControl.setDisableServiceOnChange( monitoringDisableServicecheckBox.isSelected() );
            } else {
                uddiServiceControl.setMonitoringEnabled( false );
                uddiServiceControl.setDisableServiceOnChange( false );
            }
            uddiServiceControl.setMetricsEnabled( isMetricsEnabled() );
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
                                                   "Removal published Gateway WSDL from UDDI Registry?",
                                                   "Confirm Removal from UDDI",
                                                   JOptionPane.YES_NO_OPTION,
                                                   JOptionPane.QUESTION_MESSAGE, new DialogDisplayer.OptionListener() {
                    @Override
                    public void reportResult(int option) {
                        if (option == JOptionPane.YES_OPTION){
                            UDDIRegistryAdmin uddiRegistryAdmin = Registry.getDefault().getUDDIRegistryAdmin();
                            try {
                                final String errorMsg = uddiRegistryAdmin.deleteGatewayWsdlFromUDDI(uddiProxyServiceInfo);
                                if(errorMsg == null){
                                    DialogDisplayer.showMessageDialog(ServiceUDDISettingsDialog.this,
                                            "Removal of Gateway WSDL from UDDI successful", "Successful Deletion", JOptionPane.INFORMATION_MESSAGE, null);
                                }else{
                                    DialogDisplayer.showMessageDialog(ServiceUDDISettingsDialog.this,
                                            "Problem removing Gateway WSDL from UDDI: " + errorMsg+"\nManual updates to UDDI may be required",
                                            "Problem updating UDDI", JOptionPane.WARNING_MESSAGE , null);
                                }
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

    private void publishEndpointToBusinessService(){
        DialogDisplayer.showConfirmDialog(this,
                                                   "Publish Gateway endpoint to owning BusinessService in UDDI?",
                                                   "Confirm Publish to UDDI Task",
                                                   JOptionPane.YES_NO_OPTION,
                                                   JOptionPane.QUESTION_MESSAGE, new DialogDisplayer.OptionListener() {
                    @Override
                    public void reportResult(int option) {
                        if (option == JOptionPane.YES_OPTION){
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
        return metricsEnabledCheckBox.isSelected() &&
                ( OPTION_PROXIED_BUSINESS_SERVICE.equals( metricsServiceComboBox.getSelectedItem() ) ||
                  OPTION_BOTH_BUSINESS_SERVICES.equals( metricsServiceComboBox.getSelectedItem() ) );
    }

    public boolean isMetricsEnabled() {
        return metricsEnabledCheckBox.isSelected() &&
                ( OPTION_ORIGINAL_BUSINESS_SERVICE.equals( metricsServiceComboBox.getSelectedItem() ) ||
                  OPTION_BOTH_BUSINESS_SERVICES.equals( metricsServiceComboBox.getSelectedItem() ) );
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
