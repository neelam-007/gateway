package com.l7tech.console.panels;

import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.uddi.UDDIRegistry;
import com.l7tech.gateway.common.uddi.UDDIProxiedService;
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
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServiceUDDISettingsDialog extends JDialog {
    private static final Logger logger = Logger.getLogger(ServiceUDDISettingsDialog.class.getName());
    private static final ResourceBundle resources = ResourceBundle.getBundle(ServiceUDDISettingsDialog.class.getName());

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

    private PublishedService service;
    private boolean canUpdate;
    private boolean confirmed;
    private Map<String, UDDIRegistry> allRegistries;
    private UDDIProxiedService uddiProxyService;
    private InputValidator publishWsdlValidators;

    private final static String businessEntityDefault = "<<None Selected>>";
    private String selectedBusinessName;
    private String selectedBusinessKey;

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
                        showErrorMessage("Select a UDDI Registry", "Select a UDDI Registry to search", null);
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
                    showErrorMessage("Cannot search for BusinessEntities", "Exception UDDIRegistry information", e1);
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
        dontPublishRadioButton.addActionListener(enableDisableChangeListener);
        monitoringEnabledCheckBox.addActionListener( enableDisableChangeListener );

        //Populate registry drop down
        loadUddiRegistries();

        //determine if there is a UDDIProxiedService for this service
        try {
            uddiProxyService = getUDDIRegistryAdmin().getUDDIProxiedService(service.getOid());
        } catch (FindException e) {
            uddiProxyService = null;
        }

        //Input validators
        publishWsdlValidators = new InputValidator(this, "Publish Gateway WSDL Validation");
        publishWsdlValidators.addRule(new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                if(uddiRegistriesComboBox.getSelectedIndex() == -1) return "Please select a UDDI Registry";
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
        if (uddiProxyService != null) {
            //find which registry to select
            long uddiRegOid = uddiProxyService.getUddiRegistryOid();
            boolean found = false;
            for(UDDIRegistry registries: allRegistries.values()){
                if(registries.getOid() == uddiRegOid){
                    String regName = registries.getName();
                    uddiRegistriesComboBox.setSelectedItem(regName);
                    found = true;
                }
            }
            if(!found) throw new IllegalStateException("UDDI Registry not found for pulished Gateway WSDL");
            businessEntityNameLabel.setText(uddiProxyService.getUddiBusinessName());
            updateWhenGatewayWSDLCheckBox.setSelected(uddiProxyService.isUpdateProxyOnLocalChange());
            publishProxiedWsdlRadioButton.setSelected(true);
        }else{
            //set other buttons values when implemented
            dontPublishRadioButton.setSelected(true);
            uddiRegistriesComboBox.setSelectedIndex(-1);
            businessEntityNameLabel.setText(businessEntityDefault);
        }

        // Monitoring tab settings
        monitoringEnabledCheckBox.setSelected( false ); //TODO implement when UDDIServiceControl is available
        monitoringDisableServicecheckBox.setSelected( false );

    }

    /**
     * Enable or disable UI components in the 'Publish' tab only
     */
    private void publishTabEnableDisableComponents(){

        if(dontPublishRadioButton.isSelected()){
            enableDisablePublishGatewayWsdlControls(false);
        }else if(publishProxiedWsdlRadioButton.isSelected()){
            final boolean proxyPublished = uddiProxyService != null;
            enableDisablePublishGatewayWsdlControls(!proxyPublished);
            updateWhenGatewayWSDLCheckBox.setEnabled(true);
        }
    }

    private void enableAndDisableComponents() {
        if ( canUpdate ) {
            monitoringDisableServicecheckBox.setEnabled( monitoringEnabledCheckBox.isSelected() );
            publishTabEnableDisableComponents();
        } else {
            //publish tab
            enableDisablePublishGatewayWsdlControls(false);            

            //monitor tab
            monitoringEnabledCheckBox.setEnabled( false );
            monitoringDisableServicecheckBox.setEnabled( false );
        }
    }

    private void enableDisablePublishGatewayWsdlControls(boolean enable){
        uddiRegistryLabel.setEnabled(enable);
        uddiRegistriesComboBox.setEnabled(enable);
        businessEntityLabel.setEnabled(enable);
        businessEntityNameLabel.setEnabled(enable);
        selectBusinessEntityButton.setEnabled(enable);
        updateWhenGatewayWSDLCheckBox.setEnabled(enable);
    }

    /**
     * Validation the view and update model.
     * @return true if view is valid and can be converted to model, false otherwise
     */
    private boolean viewToModel(){
        if(publishProxiedWsdlRadioButton.isSelected()){
            if(uddiProxyService == null){
                if(publishWsdlValidators.validateWithDialog()) publishUDDIProxiedService();
                else return false;
            }else {
                //update if the check box's value has changed
                if(uddiProxyService.isUpdateProxyOnLocalChange() != updateWhenGatewayWSDLCheckBox.isSelected()){
                    uddiProxyService.setUpdateProxyOnLocalChange(updateWhenGatewayWSDLCheckBox.isSelected());
                    UDDIRegistryAdmin uddiRegistryAdmin = Registry.getDefault().getUDDIRegistryAdmin();
                    try {
                        uddiRegistryAdmin.updateProxiedServiceOnly(uddiProxyService);
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
        } else if(dontPublishRadioButton.isSelected()){
            if(uddiProxyService != null){
                removeUDDIProxiedService();
            }
        }

        //TODO implement monitoring persistence when UDDIServiceControl is available

        return true;
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
                                final String errorMsg = uddiRegistryAdmin.deleteGatewayWsdlFromUDDI(uddiProxyService);
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
                                                   "Confirm Publish to UDDI",
                                                   JOptionPane.YES_NO_OPTION,
                                                   JOptionPane.QUESTION_MESSAGE, new DialogDisplayer.OptionListener() {
                    @Override
                    public void reportResult(int option) {
                        if (option == JOptionPane.YES_OPTION){
                            UDDIRegistryAdmin uddiRegistryAdmin = Registry.getDefault().getUDDIRegistryAdmin();
                            UDDIRegistry uddiRegistry = allRegistries.get(uddiRegistriesComboBox.getSelectedItem().toString());
                            try {
                                UDDIProxiedService newProxiedService =
                                        new UDDIProxiedService(service.getOid(), uddiRegistry.getOid(), selectedBusinessKey, selectedBusinessName, updateWhenGatewayWSDLCheckBox.isSelected());
                                uddiRegistryAdmin.publishGatewayWsdl(newProxiedService);
                                DialogDisplayer.showMessageDialog(ServiceUDDISettingsDialog.this,
                                        "Publication of Gateway WSDL to UDDI successful", "Successful Publication", JOptionPane.INFORMATION_MESSAGE, null);
                            } catch (Exception ex) {
                                logger.log(Level.WARNING, "Could not publish gateway WSDL to UDDI: " + ex.getMessage());
                                DialogDisplayer.showMessageDialog(ServiceUDDISettingsDialog.this, "Failed to publish Gateway WSDL to UDDI Registry: " + ex.getMessage(),
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
            showErrorMessage("Loading failed", "Unable to list all UDDI Registry: " + ExceptionUtils.getMessage(e), e);
        }
    }

    private void showErrorMessage(String title, String msg, Throwable e) {
        showErrorMessage(title, msg, e, null);
    }

    private void showErrorMessage(String title, String msg, Throwable e, Runnable continuation) {
        logger.log(Level.WARNING, msg, e);
        DialogDisplayer.showMessageDialog(this, msg, title, JOptionPane.ERROR_MESSAGE, continuation);
    }

    /** @return the UDDIRegistryAdmin interface, or null if not connected or it's unavailable for some other reason */
    private UDDIRegistryAdmin getUDDIRegistryAdmin() {
        Registry reg = Registry.getDefault();
        if (!reg.isAdminContextPresent())
            return null;
        return reg.getUDDIRegistryAdmin();
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
