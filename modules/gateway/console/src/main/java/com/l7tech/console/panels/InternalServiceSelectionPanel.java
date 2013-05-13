package com.l7tech.console.panels;

import com.l7tech.console.util.Registry;
import com.l7tech.console.util.SecurityZoneWidget;
import com.l7tech.console.util.ValidatorUtils;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.gateway.common.service.ServiceTemplate;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.FilterDocument;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.util.ResolvingComparator;
import com.l7tech.util.Resolver;
import com.l7tech.util.SoapConstants;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Logger;

/**
 *
 * User: Mike
 */
public class InternalServiceSelectionPanel extends WizardStepPanel<PublishInternalServiceWizard.InputHolder> {
    private static final Logger logger = Logger.getLogger(InternalServiceSelectionPanel.class.getName());

    private JComboBox servicesChooser;
    private JTextField serviceUri;
    private JPanel mainPanel;
    private JComboBox wstNsComboBox;
    private JPanel wstNsPanel;
    private SecurityZoneWidget zoneControl;

    public InternalServiceSelectionPanel() {
        this(null,true);
    }

    public InternalServiceSelectionPanel(WizardStepPanel<PublishInternalServiceWizard.InputHolder> next, boolean readOnly) {
        super(next, readOnly);
        initComponents();
    }

    private void initComponents() {
        servicesChooser.setModel(new DefaultComboBoxModel());
        servicesChooser.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateTemplateFields(servicesChooser.getSelectedItem());                
            }
        });
        
        serviceUri.setDocument(new FilterDocument(128, null));
        serviceUri.getDocument().addDocumentListener(new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                notifyListeners();
            }
        }
        ));
        serviceUri.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                //always start with "/" for URI
                if (!serviceUri.getText().startsWith("/") && e.getKeyChar()!='/') {
                    String uri = serviceUri.getText();
                    serviceUri.setText("/" + uri);
                }
            }
        });

        wstNsPanel.setVisible(false);
        wstNsComboBox.setModel(new DefaultComboBoxModel(SoapConstants.WST_NAMESPACE_ARRAY));
        wstNsComboBox.setSelectedIndex(3); // The last one, WS-Trust v1.4.
        wstNsComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final ServiceAdmin serviceAdmin = getServiceAdmin();
                if (serviceAdmin == null) return;

                String routingURI = serviceUri.getText();  // Preserve the routing URI, since the services combo box will be restructured.

                ServiceTemplate newTemplate = serviceAdmin.createSecurityTokenServiceTemplate((String) wstNsComboBox.getSelectedItem());
                ServiceTemplate selectedTemplate  = (ServiceTemplate) servicesChooser.getSelectedItem();
                int selectedIndex = servicesChooser.getSelectedIndex();

                DefaultComboBoxModel model = (DefaultComboBoxModel) servicesChooser.getModel();
                model.removeElement(selectedTemplate);
                model.insertElementAt(newTemplate, selectedIndex); // Put a new STS Internal Service back into the list at the same position.
                model.setSelectedItem(newTemplate);
                
                serviceUri.setText(routingURI);
            }
        });
        zoneControl.configure(EntityType.SERVICE, OperationType.CREATE, null);

        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);        
    }

    private ServiceAdmin getServiceAdmin() {
        Registry reg = Registry.getDefault();
        if (!reg.isAdminContextPresent()) {
            logger.warning("Cannot get Service Admin due to no Admin Context present.");
            return null;
        }
        return reg.getServiceManager();
    }

    private void updateTemplateFields(Object selectedItem) {
        if (selectedItem instanceof ServiceTemplate) {
            ServiceTemplate theTemplate = (ServiceTemplate) selectedItem;
            serviceUri.setText(theTemplate.getDefaultUriPrefix());
            wstNsPanel.setVisible("Security Token Service".equals(theTemplate.getName()));  // If the template is not a STS, then set wstNsPanel as invisible.
        }
    }

    @Override
    public String getDescription() {
        return "Select a special service to publish";
    }

    @Override
    public void readSettings(PublishInternalServiceWizard.InputHolder settings) throws IllegalArgumentException {
        updateView(settings);
    }

    private void updateView(PublishInternalServiceWizard.InputHolder settings) {
        servicesChooser.removeAllItems();

        java.util.List<ServiceTemplate> allTemplates = new ArrayList<ServiceTemplate>(settings.getAllTemplates());
        //noinspection unchecked
        Collections.sort( allTemplates, new ResolvingComparator(new Resolver<ServiceTemplate,String>(){
            @Override
            public String resolve(final ServiceTemplate serviceTemplate) {
                return serviceTemplate.getName();
            }
        }, false));

        if (allTemplates.isEmpty()) {
            servicesChooser.setEnabled(false);
            servicesChooser.setToolTipText("No Internal services have been defined. Internal services are provided by some modular assertions.");
            serviceUri.setEnabled(false);
            return;
        } else {
            servicesChooser.setEnabled(true);
            servicesChooser.setToolTipText("Select the internal services that will be published.");
            serviceUri.setEnabled(true);
        }


        for (ServiceTemplate aTemplate : allTemplates) {
            servicesChooser.addItem(aTemplate);
        }

        if (settings.getSelectedTemplate() != null) {
            servicesChooser.setSelectedItem(settings.getSelectedTemplate());
        }
        zoneControl.setSelectedZone(settings.getSelectedSecurityZone());
    }

    @Override
    public void storeSettings(PublishInternalServiceWizard.InputHolder settings) throws IllegalArgumentException {
        updateModel(settings);
    }

    private void updateModel(PublishInternalServiceWizard.InputHolder settings) {
        ServiceTemplate fromList  = (ServiceTemplate) servicesChooser.getSelectedItem();
        String serviceUriText = serviceUri.getText();
        if (!serviceUriText.startsWith("/")) {
            serviceUriText = "/"+serviceUriText;
        }
        
        ServiceTemplate newOne = fromList.customize( serviceUriText );

        settings.setSelectedTemplate(newOne);
        settings.setSelectedSecurityZone(zoneControl.getSelectedZone());
    }

    @Override
    public String getStepLabel() {
        return "Internal Web Service Description";
    }

    @Override
    public boolean canFinish() {
        String servicePath = serviceUri.getText();
        if ( servicePath != null ) {
            servicePath = servicePath.trim();
            while ( servicePath.startsWith("//") ) {
                servicePath = servicePath.substring(1);
            }
        }

        return servicesChooser.getModel().getSize() > 0 &&
                ValidatorUtils.validateResolutionPath(servicePath, true, true) == null;
    }
}
