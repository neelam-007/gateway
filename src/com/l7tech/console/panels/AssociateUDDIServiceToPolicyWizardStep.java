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
    private boolean done = false;
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
            Collection<UDDINamedEntity> serviceInfos = data.getUddi().listServices(serviceNameField.getText(), false, 0, MAX_ROWS);
            if (serviceInfos.isEmpty()) {
                showError("No services found with this filter.");
                return;
            }
            for (UDDINamedEntity serviceInfo : serviceInfos) {
                listData.add(new ListMember(serviceInfo.getName(), serviceInfo.getKey()));
            }
            serviceList.setListData(listData.toArray());
        } catch (Throwable e) {
            logger.log(Level.SEVERE, "cannot find service or get inquiry", e);
            showError("Cannot find services. Consult log for more info.");
        }
    }

    private void updateServiceWithPolicytModel() {
        // first, get service from the service key
        String serviceKey = listData.get(serviceList.getSelectedIndex()).serviceKey;

        try {
            String policyKey = data.getPolicytModelKey();
            String policyUrl = data.getCapturedPolicyURL();
            String serviceUrl = null;//data.getPolicyConsumptionURL(); // null means attach to service, not endpoint

            if (policyKey != null && policyKey.trim().length()>0) {
                policyUrl = null; // don't add remote policy ref if we are adding a local one    
            }

            try {
                //
                // NOTE: To enable prompting of the user for reference owerwriting
                //       replace Boolean.TRUE with null.
                //
                data.getUddi().referencePolicy(serviceKey, serviceUrl, false, policyKey, policyUrl, data.getPolicyDescription(), Boolean.TRUE, false);
            } catch (UDDIExistingReferenceException e) {
                int res = JOptionPane.showConfirmDialog(this,TextUtils.breakOnMultipleLines(
                                      "There is already a policy associated with this item " +
                                      "(key: " + e.getKeyValue() + "). Would you like to replace it?", 30),
                                      "Policy tModel already associated",
                                      JOptionPane.YES_NO_CANCEL_OPTION);
                if (res == JOptionPane.CANCEL_OPTION) {
                    logger.info("action cancelled.");
                    return;
                }
                else if (res == JOptionPane.NO_OPTION) {
                    data.getUddi().referencePolicy(serviceKey, serviceUrl, false, policyKey, policyUrl, data.getPolicyDescription(), Boolean.FALSE, false);
                }
                else {
                    data.getUddi().referencePolicy(serviceKey, serviceUrl, false, policyKey, policyUrl, data.getPolicyDescription(), Boolean.TRUE, false);
                }
            }

            JOptionPane.showMessageDialog(this, "Item updated with policy reference.",
                                          "Success", JOptionPane.PLAIN_MESSAGE);
            done = true;
            
            // this causes wizard's finish or next button to become enabled (because we're now ready to continue)
            notifyListeners();
        }
        catch (UDDIException e) {
            String msg = "could not update business service with policy tModel ref";
            logger.log(Level.WARNING, msg, e);
            showError(msg);
        }
    }

    public boolean canFinish() {
        return done;
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
