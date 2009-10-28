package com.l7tech.console.panels;

import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.uddi.UDDIRegistry;
import com.l7tech.gateway.common.uddi.UDDIProxiedService;
import com.l7tech.gateway.common.admin.UDDIRegistryAdmin;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.InputValidator;
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
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServiceUDDISettingsDialog extends JDialog {
    private static final Logger logger = Logger.getLogger(ServiceUDDISettingsDialog.class.getName());
    
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTabbedPane tabbedPane1;
    private JRadioButton donTPublishRadioButton;
    private JRadioButton publishProxiedWsdlButton;
    private JCheckBox updateWhenGatewayWSDLCheckBox;
    private JComboBox uddiRegistriesComboBox;
    private JButton selectBusinessEntityButton;
    private JLabel uddiRegistryLabel;
    private JLabel businessEntityLabel;
    private JLabel businessEntityNameLabel;
    private JLabel publishOptionLabel;

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

    public ServiceUDDISettingsDialog(Frame owner, PublishedService svc, boolean hasUpdatePermission) {
        super(owner, "Service UDDI Settings", true);
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

        DialogDisplayer.suppressSheetDisplay(this);

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        selectBusinessEntityButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    SearchUddiDialog uddiDialog = new SearchUddiDialog(ServiceUDDISettingsDialog.this,  SearchUddiDialog.SEARCH_TYPE.BUSINESS_ENTITY_SEARCH);
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
    }

    private void modelToView() {

        if (uddiProxyService != null) {
            publishProxiedWsdlButton.setSelected(true);
            publishProxiedWsdlButton.setEnabled(false);
            donTPublishRadioButton.setEnabled(true);
            enableDisablePublishGatewayWsdlControls(false);
            //we will allow this setting to be modified when the other settings cannot be
            updateWhenGatewayWSDLCheckBox.setEnabled(true);

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
        }else{
            //set other buttons values when implemented
            donTPublishRadioButton.setSelected(true);
            publishProxiedWsdlButton.setEnabled(true);
            enableDisablePublishGatewayWsdlControls(true);
            uddiRegistriesComboBox.setSelectedIndex(-1);
            businessEntityNameLabel.setText(businessEntityDefault);
        }
    }

    private void enableDisablePublishGatewayWsdlControls(boolean enable){
        publishOptionLabel.setEnabled(enable);
        uddiRegistryLabel.setEnabled(enable);
        uddiRegistriesComboBox.setEnabled(enable);
        businessEntityLabel.setEnabled(enable);
        businessEntityNameLabel.setEnabled(enable);
        selectBusinessEntityButton.setEnabled(enable);
        
        //undo when this feature is implemented
        updateWhenGatewayWSDLCheckBox.setEnabled(enable);
    }

    /**
     * Validation the view and update model.
     * @return true if view is valid and can be converted to model, false otherwise
     */
    private boolean viewToModel(){
        if(publishProxiedWsdlButton.isSelected()){
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
        } else if(donTPublishRadioButton.isSelected()){
            if(uddiProxyService != null){
                removeUDDIProxiedService();
            }
        }

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
                                uddiRegistryAdmin.deleteGatewayWsdlFromUDDI(uddiProxyService);
                                DialogDisplayer.showMessageDialog(ServiceUDDISettingsDialog.this,
                                        "Removal of Gateway WSDL from UDDI successful", "Successful Deletion", JOptionPane.INFORMATION_MESSAGE, null);
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
        if(viewToModel()) dispose();
    }

    private void onCancel() {
// add your code here if necessary
        dispose();
    }

    private void loadUddiRegistries() {
        try {
            UDDIRegistryAdmin uddiRegistryAdmin = getUDDIRegistryAdmin();
            //todo what are the rbac requirements here? Does the user need the permission to be able to read all registries?
            //todo canUpdate is set when the user has update on UDDI_REGISTRIES

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

    private void createUIComponents() {
        // TODO: place custom component creation code here
    }
}
