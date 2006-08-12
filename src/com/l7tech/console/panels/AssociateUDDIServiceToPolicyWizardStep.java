package com.l7tech.console.panels;

import org.systinet.uddi.client.v3.struct.*;
import org.systinet.uddi.client.v3.UDDI_Inquiry_PortType;
import org.systinet.uddi.client.v3.UDDIInquiryStub;
import org.systinet.uddi.client.v3.UDDI_Publication_PortType;
import org.systinet.uddi.client.v3.UDDIPublishStub;
import org.systinet.uddi.client.base.StringArrayList;
import org.systinet.uddi.InvalidParameterException;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Iterator;
import java.util.ArrayList;

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
    private JPanel mainPanel;
    private JTextField serviceNameField;
    private JButton listServicesButton;
    private JList serviceList;
    private JButton updateServiceButton;
    private boolean done = false;
    private PublishPolicyToUDDIWizard.Data data;
    private static final Logger logger = Logger.getLogger(AssociateUDDIServiceToPolicyWizardStep.class.getName());
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
        Find_service findService = new Find_service();
        findService.setAuthInfo(data.getAuthInfo());

        try {
            findService.check();
        } catch (InvalidParameterException e) {
            logger.log(Level.SEVERE, "Find_service structure did not check out ok", e);
        }
        String filter = serviceNameField.getText();
        if (filter != null) {
            filter = filter.toLowerCase();
        }
        try {
            KeyedReference category = new KeyedReference("uddi:uddi.org:wsdl:types", "service", "");
            findService.setCategoryBag(new CategoryBag(new KeyedReferenceArrayList(category)));
            UDDI_Inquiry_PortType inquiry = UDDIInquiryStub.getInstance(data.getUddiurl() + "inquiry");
            ServiceList uddiServiceListRes = inquiry.find_service(findService);

            // display those services in the list instead
            listData.clear();
            ServiceInfoArrayList serviceInfoArrayList = uddiServiceListRes.getServiceInfoArrayList();
            if (serviceInfoArrayList==null) {
                showError("No services found with this filter.");
                return;
            }
            for (Iterator iterator = serviceInfoArrayList.iterator(); iterator.hasNext();) {
                ServiceInfo serviceInfo = (ServiceInfo) iterator.next();
                String businessKey = serviceInfo.getBusinessKey();
                if (businessKey != null && !businessKey.startsWith("uddi:systinet")) {
                    String name = serviceInfo.getNameArrayList().get(0).getValue();
                    name = name.toLowerCase();
                    if (name.indexOf(filter) >= 0) {
                        listData.add(new ListMember(name, serviceInfo.getServiceKey()));
                    }
                }
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
        ServiceDetail serviceDetail;
        try {
            Get_serviceDetail getServiceDetail = new Get_serviceDetail();
            getServiceDetail.setServiceKeyArrayList(new StringArrayList(serviceKey));
            UDDI_Inquiry_PortType inquiry = UDDIInquiryStub.getInstance(data.getUddiurl() + "inquiry");
            serviceDetail = inquiry.get_serviceDetail(getServiceDetail);
        } catch (Throwable e) {
            logger.log(Level.SEVERE, "cannot find service or get inquiry", e);
            showError("Cannot get service details for service key " + serviceKey + ". Consult log for more info.");
            return;
        }
        if (serviceDetail.getBusinessServiceArrayList().size() != 1) {
            String msg = "UDDI registry returned either empty serviceDetail or " +
                         "more than one business services (" + serviceDetail.getBusinessServiceArrayList().size() + ")";
            logger.warning(msg);
            showError(msg);
            return;
        }
        KeyedReference existingLocalpolicyreference = null;
        BusinessService toUpdate = serviceDetail.getBusinessServiceArrayList().get(0);
        CategoryBag cbag = toUpdate.getCategoryBag();
        if (cbag != null && toUpdate.getCategoryBag().getKeyedReferenceArrayList() != null) {
            KeyedReferenceArrayList kreflist = toUpdate.getCategoryBag().getKeyedReferenceArrayList();
            for (Iterator i = kreflist.iterator(); i.hasNext(); ) {
                KeyedReference kref = (KeyedReference)i.next();
                if (kref.getTModelKey().equals("uddi:schemas.xmlsoap.org:localpolicyreference:2003_03")) {
                    existingLocalpolicyreference = kref;
                    break;
                }
            }
            // we can remove this before asking user because
            // if user refuses, the whole update is not going
            // to occur
            if (existingLocalpolicyreference != null) {
                kreflist.remove(existingLocalpolicyreference);
            }
        }
        if (existingLocalpolicyreference != null) {
            int res = JOptionPane.showConfirmDialog(this,UDDIPolicyDetailsWizardStep.breakOnMultipleLines(
                                  "There is already a policy associated to this Business " +
                                  "Service (key: " + existingLocalpolicyreference.getKeyValue() + "). Would you like to override it?", 30),
                                  "Policy tModel already associated",
                                  JOptionPane.YES_NO_OPTION);
            if (res == JOptionPane.NO_OPTION) {
                logger.info("action cancelled.");
                return;
            }
        }
        if (cbag == null) {
            cbag = new CategoryBag();
            toUpdate.setCategoryBag(cbag);
        }
        String keyvalue = data.getPolicytModelKey();
        String keyname = "Policy for " + data.getPolicyName();
        try {
            // change access point for the service
            if (toUpdate.getBindingTemplateArrayList() != null) {
                for (Iterator i = toUpdate.getBindingTemplateArrayList().iterator(); i.hasNext(); ) {
                    BindingTemplate bt = (BindingTemplate)i.next();
                    bt.setAccessPoint(new AccessPoint(data.getPolicyConsumptionURL()));
                }
            }

            // assign policy tModel in categoryBag
            cbag.addKeyedReference(new KeyedReference("uddi:schemas.xmlsoap.org:localpolicyreference:2003_03", keyvalue, keyname));

            // update service in uddi
            Save_service save = new Save_service();
            save.addBusinessService(toUpdate);
            save.setAuthInfo(data.getAuthInfo());
            save.check();
            UDDI_Publication_PortType publishing = UDDIPublishStub.getInstance(data.getUddiurl() + "publishing");
            System.out.print("Save in progress ...");
            publishing.save_service(save);
            JOptionPane.showMessageDialog(this, "Service updated with policy tModel",
                                          "Success", JOptionPane.PLAIN_MESSAGE);
            done = true;
            // this causes wizard's finish or next button to become enabled (because we're now ready to continue)
            notifyListeners();
        } catch (Throwable e) {
            String msg = "could not update business service with policy tModel ref";
            logger.log(Level.WARNING, msg, e);
            showError(msg);
        }
    }

    public boolean canAdvance() {
        return done;
    }

    public boolean canFinish() {
        return done;
    }

    public boolean onNextButton() {
        return done;
    }

    public void readSettings(Object settings) throws IllegalArgumentException {
        data = (PublishPolicyToUDDIWizard.Data)settings;
    }

    private void showError(String err) {
        JOptionPane.showMessageDialog(this, UDDIPolicyDetailsWizardStep.breakOnMultipleLines(err, 30),
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
