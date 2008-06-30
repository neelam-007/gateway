package com.l7tech.console.panels;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.ArrayList;
import java.util.Collection;

import com.l7tech.common.util.TextUtils;
import com.l7tech.common.uddi.UDDIException;
import com.l7tech.common.uddi.UDDIExistingReferenceException;
import com.l7tech.common.uddi.UDDINamedEntity;

/**
 * Wizard step in the PublishPolicyToUDDIWizard wizard that
 * allows a saved tModel policy to be associated to a UDDI
 * business service in the registry.
 *
 * once the remotepolicyreference tModel is published, find a businessService to associate to, and add :
 *   <categoryBag>
 *       <keyedReference
 *           keyName="policy for service foo"
 *           keyValue="the tModel key saved in remotepolicyreference"
 *           tModelKey="uddi:schemas.xmlsoap.org:localpolicyreference:2003_03" />
 *   </categoryBag>
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jun 14, 2006<br/>
 */
public class AssociateUDDIServiceToPolicyWizardStep extends WizardStepPanel {
    private static final Logger logger = Logger.getLogger(AssociateUDDIServiceToPolicyWizardStep.class.getName());
    private static final int MAX_ROWS = 100;

    private JPanel mainPanel;
    private JTextField serviceNameField;
    private JButton listServicesButton;
    private JList serviceList;
    private JButton updateServiceButton;
    private PublishPolicyToUDDIWizard.Data data;
    private ArrayList<ListMember> listData = new ArrayList<ListMember>();

    public AssociateUDDIServiceToPolicyWizardStep(WizardStepPanel next) {
        super(next);
        initialize();
    }

    public String getDescription() {
        return "Associate Policy tModel to Business Service";
    }

    public String getStepLabel() {
        return "Associate Policy tModel to Business Service";
    }

    private void initialize() {
        setLayout(new BorderLayout());
        add(mainPanel);
        listServicesButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                listServices();
            }
        });
        serviceList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        serviceList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (serviceList.getSelectedIndex() < 0) {
                    updateServiceButton.setEnabled(false);
                } else {
                    updateServiceButton.setEnabled(true);
                }
            }
        });
        updateServiceButton.setEnabled(false);
        updateServiceButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateServiceWithPolicytModel();
            }
        });
    }

    private void listServices() {
        try {
            // display those services in the list instead
            listData.clear();
            Collection<UDDINamedEntity> serviceInfos;

            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            try {
                serviceInfos = data.getUddi().listServices(serviceNameField.getText(), false, 0, MAX_ROWS);
            } finally {
                setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }

            if (serviceInfos.isEmpty()) {
                showError("No services found with this filter.");
                return;
            }
            for (UDDINamedEntity serviceInfo : serviceInfos) {
                listData.add(new ListMember(serviceInfo.getName(), serviceInfo.getKey()));
            }
        } catch (Throwable e) {
            logger.log(Level.SEVERE, "cannot find service or get inquiry", e);
            showError("Cannot find services. Consult log for more info.");
        } finally {
            serviceList.setListData(listData.toArray());            
        }
    }

    private void updateServiceWithPolicytModel() {
        // first, get service from the service key
        String serviceKey = listData.get(serviceList.getSelectedIndex()).serviceKey;

        try {
            String policyKey = data.getPolicytModelKey();
            String policyUrl = data.getCapturedPolicyURL();
            String serviceUrl = data.getPolicyConsumptionURL(); 

            if (policyKey != null && policyKey.trim().length()>0) {
                policyUrl = null; // don't add remote policy ref if we are adding a local one    
            }

            try {
                // add policy reference
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                try {
                    data.getUddi().referencePolicy(serviceKey, serviceUrl, policyKey, policyUrl, data.getPolicyDescription(), null);
                } finally {
                    setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                }
            } catch (UDDIExistingReferenceException e) {
                int res = JOptionPane.showConfirmDialog(this,TextUtils.breakOnMultipleLines(
                        "There is already a policy associated to this Business " +
                        "Service (key: " + e.getKeyValue() + "). Would you like to override it?", 30),
                        "Policy tModel already associated",
                        JOptionPane.YES_NO_OPTION);
                if (res == JOptionPane.NO_OPTION) {
                    logger.info("action cancelled.");
                    return;
                } else {
                    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    try {
                        data.getUddi().referencePolicy(serviceKey, serviceUrl, policyKey, policyUrl, data.getPolicyDescription(), Boolean.TRUE);
                    } finally {
                        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    }
                }
            }

            JOptionPane.showMessageDialog(this, "Service updated with policy tModel",
                                          "Success", JOptionPane.PLAIN_MESSAGE);
            
            // this causes wizard's finish or next button to become enabled (because we're now ready to continue)
            notifyListeners();
        }
        catch (UDDIException e) {
            String msg = "could not update business service with policy tModel ref";
            logger.log(Level.WARNING, msg, e);
            showError(msg);
        }
    }

    public void readSettings(Object settings) throws IllegalArgumentException {
        data = (PublishPolicyToUDDIWizard.Data)settings;
    }

    private void showError(String err) {
        JOptionPane.showMessageDialog(this, TextUtils.breakOnMultipleLines(err, 30),
                                      "Invalid Input", JOptionPane.ERROR_MESSAGE);
    }

    private class ListMember {
        String serviceName;
        String serviceKey;
        public ListMember(String name, String key) {
            serviceName = name;
            serviceKey = key;
        }
        public String toString() {
            return serviceName + " [" + serviceKey + "]";
        }
    }
}
