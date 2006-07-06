package com.l7tech.console.panels;

import org.systinet.uddi.client.v3.struct.*;
import org.systinet.uddi.client.v3.*;
import org.systinet.uddi.client.base.StringArrayList;
import org.systinet.uddi.InvalidParameterException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.xml.soap.SOAPException;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Iterator;
import java.util.ArrayList;
import java.net.URL;
import java.io.IOException;

import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.policy.wsp.WspConstants;

/**
 * Second step in the ImportPolicyFromUDDIWizard.
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jun 16, 2006<br/>
 */
public class ImportPolicyFromUDDIWizardStep extends WizardStepPanel {
    private JPanel mainPanel;
    private JTextField nameField;
    private JButton searchButton;
    private JList policyList;
    private ImportPolicyFromUDDIWizard.Data data;
    private static final Logger logger = Logger.getLogger(ImportPolicyFromUDDIWizardStep.class.getName());
    private String authInfo;
    private boolean readyToAdvance = false;

    public ImportPolicyFromUDDIWizardStep(WizardStepPanel next) {
        super(next);
        initialize();
    }

    public String getDescription() {
        return "Search UDDI Directory for Policy and Import it";
    }

    public String getStepLabel() {
        return "Select Policy From UDDI";
    }

    public boolean canAdvance() {
        return readyToAdvance;
    }

    public boolean canFinish() {
        return false;
    }

    public boolean onNextButton() {
        ImportPolicyFromUDDIWizardStep.this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        boolean res = importPolicy();
        ImportPolicyFromUDDIWizardStep.this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        return res;
    }

    private void initialize() {
        setLayout(new BorderLayout());
        add(mainPanel);
        searchButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ImportPolicyFromUDDIWizardStep.this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                find();
                ImportPolicyFromUDDIWizardStep.this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }
        });
        policyList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (policyList.getSelectedIndex() < 0) {
                    readyToAdvance = false;
                    // this causes wizard's finish or next button to become enabled (because we're now ready to continue)
                    notifyListeners();
                } else {
                    readyToAdvance = true;
                    // this causes wizard's finish or next button to become enabled (because we're now ready to continue)
                    notifyListeners();
                }
            }
        });
    }

    private String getAuthInfo() throws SOAPException, UDDIException, InvalidParameterException {
        if (authInfo == null) {
            UDDI_Security_PortType security = UDDISecurityStub.getInstance(data.getUddiurl() + "security");
            authInfo = security.get_authToken(new Get_authToken(data.getAccountName(), data.getAccountPasswd())).getAuthInfo();
        }
        return authInfo;
    }

    private void find() {
        try {
            getAuthInfo();
        } catch (Throwable e) {
            logger.log(Level.WARNING, "cannot get security token from " + data.getUddiurl() + "security", e);
            showError("ERROR cannot get security token from " + data.getUddiurl() + "security. " + UDDIPolicyDetailsWizardStep.getRootCauseMsg(e));
            return;
        }
        String filter = nameField.getText();
        Find_tModel findtModel = new Find_tModel();
        try {
            findtModel.addFindQualifier("approximateMatch");
            CategoryBag cbag = new CategoryBag();
            cbag.addKeyedReference(new KeyedReference("uddi:schemas.xmlsoap.org:policytypes:2003_03", "policy", "policy"));
            findtModel.setCategoryBag(cbag);
            findtModel.setAuthInfo(authInfo);
            /* this does not seem to work unless the name has a complete match
            if (filter != null && filter.length() > 0) {
                findtModel.setName(new Name(filter));
            }*/
            findtModel.check();
        } catch (Throwable e) {
            logger.log(Level.WARNING, "cannot construct find_tModel", e);
            showError("cannot construct find_tModel. " + UDDIPolicyDetailsWizardStep.getRootCauseMsg(e));
            return;
        }
        TModelList tModelList;
        try {
            UDDI_Inquiry_PortType inquiry = UDDIInquiryStub.getInstance(data.getUddiurl() + "inquiry");
            tModelList = inquiry.find_tModel(findtModel);
        } catch (Throwable e) {
            logger.log(Level.WARNING, "Error getting find result", e);
            showError("Error getting find result . " + UDDIPolicyDetailsWizardStep.getRootCauseMsg(e));
            authInfo = null;
            return;
        }

        TModelInfoArrayList tModelInfoArrayList = tModelList.getTModelInfoArrayList();
        if (tModelInfoArrayList==null) {
            logger.info("nothing found");
            return;
        }

        policyList.setListData(new Object[0]);
        ArrayList<ListMember> output = new ArrayList<ListMember>();
        for (Iterator iterator = tModelInfoArrayList.iterator(); iterator.hasNext();) {
            TModelInfo tModel = (TModelInfo) iterator.next();
            if (tModel.getName() != null) {
                String tmodelname = tModel.getName().getValue();
                if (filter != null && filter.length() > 0) {
                    if (!tmodelname.startsWith(filter)) continue;
                }
                output.add(new ListMember(tmodelname, tModel.getTModelKey()));
            }
        }
        policyList.setListData(output.toArray());
    }

    private class ListMember {
        String tModelName;
        String tModelKey;
        public ListMember(String name, String key) {
            tModelName = name;
            tModelKey = key;
        }
        public String toString() {
            return tModelName + " [" + tModelKey + "]";
        }
    }
    
    public void readSettings(Object settings) throws IllegalArgumentException {
        data = (ImportPolicyFromUDDIWizard.Data)settings;
    }

    private void showError(String err) {
        JOptionPane.showMessageDialog(this, UDDIPolicyDetailsWizardStep.breakOnMultipleLines(err, 30),
                                      "Error", JOptionPane.ERROR_MESSAGE);
    }

    private boolean importPolicy() {
        String tModelKey = ((ListMember)policyList.getSelectedValue()).tModelKey;

        try {
            getAuthInfo();
        } catch (Throwable e) {
            logger.log(Level.WARNING, "cannot get security token from " + data.getUddiurl() + "security", e);
            showError("ERROR cannot get security token from " + data.getUddiurl() + "security. " + UDDIPolicyDetailsWizardStep.getRootCauseMsg(e));
            return false;
        }
        // get the policy url and try to fetch it
        Get_tModelDetail gettmDetail;
        try {
            gettmDetail = new Get_tModelDetail();
            gettmDetail.setAuthInfo(authInfo);
            gettmDetail.setTModelKeyArrayList(new StringArrayList(tModelKey));
        } catch (Throwable e) {
            logger.log(Level.WARNING, "ERROR cannot construct get_tModelDetail for " + tModelKey, e);
            showError("ERROR cannot construct get_tModelDetail for " + tModelKey + ". " + UDDIPolicyDetailsWizardStep.getRootCauseMsg(e));
            return false;
        }

        ArrayList<String> policyURLs = new ArrayList<String>();
        try {
            UDDI_Inquiry_PortType inquiry = UDDIInquiryStub.getInstance(data.getUddiurl() + "inquiry");
            TModelDetail res = inquiry.get_tModelDetail(gettmDetail);
            if (res != null && res.getTModelArrayList() != null && res.getTModelArrayList().size() == 1) {
                TModel tModel = res.getTModelArrayList().get(0);
                if (tModel.getOverviewDocArrayList() != null) {
                    for (int i = 0; i < tModel.getOverviewDocArrayList().size(); i++) {
                        OverviewDoc odoc = tModel.getOverviewDocArrayList().get(i);
                        if (odoc != null && odoc.getOverviewURL() != null) {
                            String url = odoc.getOverviewURL().getValue();
                            if (!policyURLs.contains(url)) policyURLs.add(url);
                        }
                    }
                } else {
                    showError("Could not get overviewDoc from the tModel");
                    return false;
                }
            } else {
                showError("ERROR get_tModelDetail returned zero or multiple tModels");
                return false;
            }
        } catch (Throwable e) {
            logger.log(Level.WARNING, "ERROR cannot get tModel Detail", e);
            showError("ERROR cannot get tModel Detail. " + UDDIPolicyDetailsWizardStep.getRootCauseMsg(e));
            return false;
        }

        if (policyURLs.size() < 1) {
            showError("ERROR tModel did not have a overviewURL to resolve a policy document from");
            return false;
        }

        ArrayList<Document> policyDocs = new ArrayList<Document>();
        // for each URL, try to get a policy document
        for (String url : policyURLs) {
            try {
                Document doc = XmlUtil.parse(new URL(url).openStream());


                if (XmlUtil.findFirstChildElementByName(doc,
                                                        new String[] {WspConstants.L7_POLICY_NS,
                                                                      SoapUtil.WSP_NAMESPACE,
                                                                      SoapUtil.WSP_NAMESPACE2},
                                                        WspConstants.POLICY_ELNAME) != null) {
                    policyDocs.add(doc);
                } else {
                    logger.info("xml document resolved from " + url + " was not a policy: " +
                                XmlUtil.nodeToFormattedString(doc));
                }
            } catch (IOException e) {
                logger.log(Level.WARNING, "cannot get xml document from url " + url);
            } catch (SAXException e) {
                logger.log(Level.WARNING, "cannot get xml document from url " + url);
            }
        }
        if (policyDocs.size() < 1) {
            showError("ERROR none of the overviewURL resolved a policy document");
            return false;
        }
        String output;
        try {
            output = XmlUtil.nodeToFormattedString(policyDocs.get(0));
        } catch (IOException e) {
            // cannot happen
            throw new RuntimeException(e);
        }
        data.setPolicyXML(output);
        return true;
    }
}
