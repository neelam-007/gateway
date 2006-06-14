package com.l7tech.console.panels;

import org.systinet.uddi.client.v3.struct.*;
import org.systinet.uddi.client.v3.UDDI_Inquiry_PortType;
import org.systinet.uddi.client.v3.UDDIInquiryStub;
import org.systinet.uddi.InvalidParameterException;

import javax.swing.*;
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
        updateServiceButton.setEnabled(false);
        listServicesButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                listServices();
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
        // todo, fix this url below
        String url = "http://systinet:8080/uddi/inquiry";
        try {
            KeyedReference category = new KeyedReference("uddi:uddi.org:wsdl:types", "service", "");
            findService.setCategoryBag(new CategoryBag(new KeyedReferenceArrayList(category)));
            UDDI_Inquiry_PortType inquiry = UDDIInquiryStub.getInstance(url);
            ServiceList uddiServiceListRes = inquiry.find_service(findService);

            // display those services in the list instead
            listData.clear();
            ServiceInfoArrayList serviceInfoArrayList = uddiServiceListRes.getServiceInfoArrayList();
            if (serviceInfoArrayList==null) {
                showError("No services found");
                return;
            }
            for (Iterator iterator = serviceInfoArrayList.iterator(); iterator.hasNext();) {
                ServiceInfo serviceInfo = (ServiceInfo) iterator.next();
                String businessKey = serviceInfo.getBusinessKey();
                if (businessKey != null && !businessKey.startsWith("uddi:systinet")) {
                    String name = serviceInfo.getNameArrayList().get(0).getValue();
                    if (name.indexOf(filter) >= 0) {
                        listData.add(new ListMember(name, serviceInfo.getServiceKey()));
                    }
                }
            }
            serviceList.setListData(listData.toArray());
        } catch (Throwable e) {
            logger.log(Level.SEVERE, "cannot find service or get inquiry", e);
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
        JOptionPane.showMessageDialog(this, err, "Invalid Input", JOptionPane.ERROR_MESSAGE);
    }
    /*

    todo, once the remotepolicyreference tModel is published, find a businessService to associate to,
    then add :
    <categoryBag>
        <keyedReference
            keyName="policy for service foo"
            keyValue="the tModel key saved in remotepolicyreference"
            tModelKey="uddi:schemas.xmlsoap.org:localpolicyreference:2003_03" />
    </categoryBag>

    */

    private class ListMember {
        String serviceName;
        String serviceKey;
        public ListMember(String name, String key) {
            serviceName = name;
            serviceKey = key;
        }
        public String toString() {
            return serviceName;
        }
    }
}
