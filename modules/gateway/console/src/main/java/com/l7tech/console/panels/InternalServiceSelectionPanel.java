package com.l7tech.console.panels;

import com.l7tech.gateway.common.service.ServiceTemplate;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.FilterDocument;
import com.l7tech.util.ResolvingComparator;
import com.l7tech.util.Resolver;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;

/**
 *
 * User: Mike
 */
public class InternalServiceSelectionPanel extends WizardStepPanel {
    private JComboBox servicesChooser;
    private JTextField serviceUri;
    private JPanel mainPanel;

    public InternalServiceSelectionPanel() {
        this(null,true);
    }

    public InternalServiceSelectionPanel(WizardStepPanel next, boolean readOnly) {
        super(next, readOnly);
        initComponents();
    }

    private void initComponents() {
        servicesChooser.setModel(new DefaultComboBoxModel());
        servicesChooser.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateTemplateFields(servicesChooser.getSelectedItem());                
            }
        });

        serviceUri.getDocument().addDocumentListener(new RunOnChangeListener(new Runnable() {
            public void run() {
                notifyListeners();
            }
        }
        ));

        serviceUri.setDocument(new FilterDocument(128, null));

        serviceUri.addKeyListener(new KeyListener() {
            public void keyPressed(KeyEvent e) {
                //always start with "/" for URI
                if (!serviceUri.getText().startsWith("/")) {
                    String uri = serviceUri.getText();
                    serviceUri.setText("/" + uri);
                }
            }

            public void keyReleased(KeyEvent e) {}

            public void keyTyped(KeyEvent e) {}
        });

        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);        
    }

    private void updateTemplateFields(Object selectedItem) {
        if (selectedItem instanceof ServiceTemplate) {
            ServiceTemplate theTemplate = (ServiceTemplate) selectedItem;
            serviceUri.setText(theTemplate.getDefaultUriPrefix());
        }
    }

    public String getDescription() {
        return "Select a special service to publish";
    }

    public void readSettings(Object settings) throws IllegalArgumentException {
        updateView((PublishInternalServiceWizard.ServiceTemplateHolder)settings);
    }

    private void updateView(PublishInternalServiceWizard.ServiceTemplateHolder serviceTemplateHolder) {
        servicesChooser.removeAllItems();

        java.util.List<ServiceTemplate> allTemplates = new ArrayList<ServiceTemplate>(serviceTemplateHolder.getAllTemplates());
        //noinspection unchecked
        Collections.sort( allTemplates, new ResolvingComparator(new Resolver<ServiceTemplate,String>(){
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

        if (serviceTemplateHolder.getSelectedTemplate() != null) {
            servicesChooser.setSelectedItem(serviceTemplateHolder.getSelectedTemplate());
        }
    }

    public void storeSettings(Object settings) throws IllegalArgumentException {
        updateModel((PublishInternalServiceWizard.ServiceTemplateHolder)settings);
    }

    private void updateModel(PublishInternalServiceWizard.ServiceTemplateHolder serviceTemplateHolder) {
        ServiceTemplate fromList  = (ServiceTemplate) servicesChooser.getSelectedItem();
        String serviceUriText = serviceUri.getText();
        if (!serviceUriText.startsWith("/")) {
            serviceUriText = "/"+serviceUriText;
        }
        
        ServiceTemplate newOne = new ServiceTemplate(
                fromList.getName(),
                serviceUriText,
                fromList.getWsdlXml(),
                fromList.getWsdlUrl(),
                fromList.getDefaultPolicyXml(),
                fromList.getServiceDocuments(),
                fromList.getType(),
                fromList.getPolicyTags());

        serviceTemplateHolder.setSelectedTemplate(newOne);
    }

    public String getStepLabel() {
        return "Internal Web Service Description";
    }

    public boolean canFinish() {
        return servicesChooser.getModel().getSize() > 0 && serviceUri.getText() != null && !serviceUri.getText().trim().equals("");
    }
}
