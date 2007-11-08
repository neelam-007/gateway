package com.l7tech.skunkworks.uddi;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.Toolkit;
import java.awt.Dimension;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.net.URL;
import javax.swing.*;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Binding;
import javax.xml.ws.handler.Handler;
import javax.xml.namespace.QName;

import com.l7tech.common.uddi.guddiv3.UDDISecurityPortType;
import com.l7tech.common.uddi.guddiv3.GetAuthToken;
import com.l7tech.common.uddi.guddiv3.DispositionReportFaultMessage;
import com.l7tech.common.uddi.guddiv3.DispositionReport;
import com.l7tech.common.uddi.guddiv3.Result;
import com.l7tech.common.uddi.guddiv3.UDDISecurity;
import com.l7tech.common.uddi.guddiv3.UDDIInquiryPortType;
import com.l7tech.common.uddi.guddiv3.UDDIInquiry;
import com.l7tech.common.uddi.guddiv3.UDDIPublicationPortType;
import com.l7tech.common.uddi.guddiv3.UDDIPublication;
import com.l7tech.common.uddi.guddiv3.ErrInfo;
import com.l7tech.common.uddi.guddiv3.CategoryBag;
import com.l7tech.common.uddi.guddiv3.Name;
import com.l7tech.common.uddi.guddiv3.FindQualifiers;
import com.l7tech.common.uddi.guddiv3.FindTModel;
import com.l7tech.common.uddi.guddiv3.TModelList;
import com.l7tech.common.uddi.guddiv3.TModelInfos;
import com.l7tech.common.uddi.guddiv3.TModelInfo;
import com.l7tech.common.uddi.guddiv3.GetTModelDetail;
import com.l7tech.common.uddi.guddiv3.TModelDetail;
import com.l7tech.common.uddi.guddiv3.TModel;
import com.l7tech.common.uddi.guddiv3.KeyedReference;
import com.l7tech.common.uddi.guddiv3.FindService;
import com.l7tech.common.uddi.guddiv3.ServiceList;
import com.l7tech.common.uddi.guddiv3.ServiceInfos;
import com.l7tech.common.uddi.guddiv3.ServiceInfo;
import com.l7tech.common.uddi.guddiv3.GetServiceDetail;
import com.l7tech.common.uddi.guddiv3.BusinessService;
import com.l7tech.common.uddi.guddiv3.SaveService;
import com.l7tech.common.uddi.guddiv3.ServiceDetail;
import com.l7tech.common.uddi.guddiv3.DeleteTModel;
import com.l7tech.common.util.Functions;
import com.l7tech.common.gui.widgets.TextListCellRenderer;

/**
 * UDDI Tool for WS-Policies in UDDI. 
 *
 * <p>Use to find and delete polcies and policy references in a UDDI.</p>
 *
 * <p>Note that this will ONLY work correctly for policies and references
 * created by the SecureSpan Manager.</p>
 *
 * <p>WARNING: If used incorrectly it is possible to cause registry corruption.
 * If the UDDI registry has tools to manage policies then that should be used
 * for deletion (however this tool is probably slightly better than Systinets
 * admin tool since it will delete references to a policy before deleting the
 * policy).</p>
 *
 * @author Steve Jones
 */
public class UDDIPolicyTool extends JFrame {
    private static final String UDDIV3_NAMESPACE = "urn:uddi-org:api_v3_service";
    private static final String POLICY_TYPE_KEY_VALUE = "policy";
    private static final String TMODEL_KEY_WSDL_TYPES = "uddi:uddi.org:wsdl:types";
    private static final String WSDL_TYPES_SERVICE = "service";
    private static final String FINDQUALIFIER_APPROXIMATE = "approximateMatch";
    private static final String FINDQUALIFIER_CASEINSENSITIVE = "caseInsensitiveMatch";

    // WS-Policy Attachment 1.2+
    private static final String tModelKeyPolicyType = "uddi:schemas.xmlsoap.org:policytypes:2003_03";
    private static final String tModelKeyLocalPolicyReference = "uddi:schemas.xmlsoap.org:localpolicyreference:2003_03";
    private static final String tModelKeyRemotePolicyReference = "uddi:schemas.xmlsoap.org:remotepolicyreference:2003_03";
    // WS-Policy Attachment 1.5
//    private static final String tModelKeyPolicyType = "uddi:w3.org:ws-policy:v1.5:attachment:policytypes";
//    private static final String tModelKeyLocalPolicyReference = "uddi:w3.org:ws-policy:v1.5:attachment:localpolicyreference";
//    private static final String tModelKeyRemotePolicyReference = "uddi:w3.org:ws-policy:v1.5:attachment:remotepolicyreference";
    
    private JTextField urlInquiryTextField;
    private JTextField urlPublishTextField;
    private JTextField urlSecurityTextField;
    private JTextField searchPolicyNameTextField;
    private JButton searchPolicyButton;
    private JList policySearchList;
    private JTextField credsUsernameTextField;
    private JPasswordField credsPasswordField;
    private JPanel mainPanel;
    private JList serviceForPolicyList;
    private JButton searchServiceButton;
    private JButton policyDeleteButton;

    private String authToken;
    private String login;
    private String password;

    public UDDIPolicyTool() {
        super("UDDI Policy Attachment Tool v0.1");
        setContentPane(mainPanel);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        policySearchList.setCellRenderer(new TextListCellRenderer(new TextListCellProvider(true), new TextListCellProvider(false), false));
        serviceForPolicyList.setCellRenderer(new TextListCellRenderer(new TextListCellProvider(true), new TextListCellProvider(false), false));
        initListeners();
        initData();
        pack();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation((int)(screenSize.getWidth()-getWidth())/2, (int)(screenSize.getHeight()-getHeight())/2);
    }

    private void initListeners() {
        searchPolicyButton.addActionListener(new ActionListener(){
            public void actionPerformed(final ActionEvent e) {
                try {
                    findPolicies();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        searchServiceButton.addActionListener(new ActionListener(){
            public void actionPerformed(final ActionEvent e) {
                try {
                    findServices();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        policyDeleteButton.addActionListener(new ActionListener(){
            public void actionPerformed(final ActionEvent e) {
                try {
                    int result = JOptionPane.showConfirmDialog(
                            UDDIPolicyTool.this,
                            "Really delete policy and any reference(s) to it?",
                            "Confirm Policy Deletion",
                            JOptionPane.OK_CANCEL_OPTION);
                    if ( result == JOptionPane.OK_OPTION ) {
                        deletePolicy();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    private void initData() {
        urlInquiryTextField.setText("http://centrasitegov:2020/registry/uddi/inquiry");
        urlPublishTextField.setText("http://centrasitegov:2020/registry/uddi/publish");
        urlSecurityTextField.setText("http://centrasitegov:2020/registry/uddi/security");
        credsUsernameTextField.setText("administrator@soamasterclass.com");
    }

    /**
     * Find all the policy tModels (filter by name)
     */
    private void findPolicies() throws Exception {
        String policyPattern = searchPolicyNameTextField.getText();
        if (policyPattern.length()==0) {
            policyPattern = "%";
        }

        List<UDDINamedEntity> policies = new ArrayList<UDDINamedEntity>();

        try {
            String authToken = authToken();

            CategoryBag cbag = new CategoryBag();
            cbag.getKeyedReference().add(buildKeyedReference(tModelKeyPolicyType, null, POLICY_TYPE_KEY_VALUE));
            Name name = null;
            FindQualifiers findQualifiers = null;
            if (policyPattern != null) {
                // if approximate match is used for the URL then CentraSite will perform a prefix match ...
                findQualifiers = buildFindQualifiers(false);
                name = buildName(policyPattern);
            }

            UDDIInquiryPortType inquiryPort = getInquirePort();

            // find policy tmodel(s)
            FindTModel findTModel = new FindTModel();
            findTModel.setAuthInfo(authToken);
            findTModel.setFindQualifiers(findQualifiers);
            findTModel.setName(name);
            findTModel.setCategoryBag(cbag);
            TModelList tModelList = inquiryPort.findTModel(findTModel);

            List<String> policyKeys = new ArrayList();
            TModelInfos tModelInfos = tModelList.getTModelInfos();
            if (tModelInfos != null) {
                for (TModelInfo tModel : tModelInfos.getTModelInfo()) {
                    if (tModel.getName() != null) {
                        String key = tModel.getTModelKey().toString();
                        policyKeys.add(key);
                        String tmodelname = tModel.getName().getValue();
                        policies.add(new UDDINamedEntity(key, tmodelname));
                    }
                }
            }

            // Get policy urls the named info
            if ( !policyKeys.isEmpty() ) {
                GetTModelDetail getTModelDetail = new GetTModelDetail();
                getTModelDetail.setAuthInfo(authToken);
                getTModelDetail.getTModelKey().addAll(policyKeys);
                TModelDetail res = inquiryPort.getTModelDetail(getTModelDetail);
                if (res != null) {
                    for (TModel tModel : res.getTModel()) {
                        CategoryBag categoryBag = tModel.getCategoryBag();
                        if (categoryBag == null)
                            continue;

                        List<KeyedReference> keyedReferences = categoryBag.getKeyedReference();
                        if (keyedReferences == null || keyedReferences.isEmpty())
                            continue;

                        for (KeyedReference keyedReference : keyedReferences) {
                            if (keyedReference.getTModelKey().equals(tModelKeyRemotePolicyReference)) {
                                mergePolicyUrlToInfo(
                                        tModel.getTModelKey().toString(),
                                        keyedReference.getKeyValue(),
                                        policies);
                                break;
                            }
                        }
                    }
                }
            }

            // update view 
            policySearchList.setListData(policies.toArray());
            serviceForPolicyList.setListData(new Object[0]);
        } catch (DispositionReportFaultMessage drfm) {
            throw buildFaultException("Error listing policies: ", drfm);
        }
    }

    /**
     * Find the services associated with the selected policy tModel 
     */
    private void findServices() throws Exception {
        UDDINamedEntity entity = (UDDINamedEntity) policySearchList.getSelectedValue();
        String policyKey = entity==null ? null : entity.getKey();

        List<UDDINamedEntity> services = new ArrayList<UDDINamedEntity>();
        try {
            String authToken = authToken();

            UDDIInquiryPortType inquiryPort = getInquirePort();

            // find service
            FindService findService = new FindService();
            findService.setAuthInfo(authToken);
            findService.setFindQualifiers(buildFindQualifiers(false));

            // category constraints
            CategoryBag cb2 = new CategoryBag();

            // WSDL services only
            KeyedReference keyedReference = new KeyedReference();
            keyedReference.setKeyValue(WSDL_TYPES_SERVICE);
            keyedReference.setTModelKey(TMODEL_KEY_WSDL_TYPES);
            cb2.getKeyedReference().add(keyedReference);

            // Policy tModel reference
            KeyedReference keyedReference2 = new KeyedReference();
            keyedReference2.setKeyValue(policyKey);
            keyedReference2.setTModelKey(tModelKeyLocalPolicyReference);
            cb2.getKeyedReference().add(keyedReference2);

            findService.setCategoryBag(cb2);

            if (policyKey != null) {
                ServiceList serviceList = inquiryPort.findService(findService);

                //
                ServiceInfos serviceInfos = serviceList.getServiceInfos();
                if (serviceInfos != null) {
                    for (ServiceInfo serviceInfo : serviceInfos.getServiceInfo()) {
                        String name = get(serviceInfo.getName(), "service name", false).getValue();
                        services.add(new UDDINamedEntity(serviceInfo.getServiceKey().toString(), name));
                    }
                }
            }

        } catch (DispositionReportFaultMessage drfm) {
            throw buildFaultException("Error listing policies: ", drfm);
        } finally {
            // update view
            serviceForPolicyList.setListData(services.toArray());
        }
    }

    /**
     * Find the policy references for services associated with the selected policy tModel
     * then delete the policy tModel.
     */
    private void deletePolicy() throws Exception {
        UDDINamedEntity entity = (UDDINamedEntity) policySearchList.getSelectedValue();
        String policyKey = entity==null ? null : entity.getKey();

        List<UDDINamedEntity> services = new ArrayList<UDDINamedEntity>();
        try {
            String authToken = authToken();

            UDDIInquiryPortType inquiryPort = getInquirePort();

            // find service
            FindService findService = new FindService();
            findService.setAuthInfo(authToken);
            findService.setFindQualifiers(buildFindQualifiers(false));

            // category constraints
            CategoryBag cb2 = new CategoryBag();

            // WSDL services only
            KeyedReference keyedReference = new KeyedReference();
            keyedReference.setKeyValue(WSDL_TYPES_SERVICE);
            keyedReference.setTModelKey(TMODEL_KEY_WSDL_TYPES);
            cb2.getKeyedReference().add(keyedReference);

            // Policy tModel reference
            KeyedReference keyedReference2 = new KeyedReference();
            keyedReference2.setKeyValue(policyKey);
            keyedReference2.setTModelKey(tModelKeyLocalPolicyReference);
            cb2.getKeyedReference().add(keyedReference2);

            findService.setCategoryBag(cb2);

            if (policyKey != null) {
                ServiceList serviceList = inquiryPort.findService(findService);

                //
                ServiceInfos serviceInfos = serviceList.getServiceInfos();
                if (serviceInfos != null) {
                    for (ServiceInfo serviceInfo : serviceInfos.getServiceInfo()) {
                        String name = get(serviceInfo.getName(), "service name", false).getValue();
                        services.add(new UDDINamedEntity(serviceInfo.getServiceKey().toString(), name));
                    }
                }

                List<UDDINamedEntity> oldServicesList = new ArrayList<UDDINamedEntity>();
                for (int i=0; i<serviceForPolicyList.getModel().getSize(); i++) {
                    oldServicesList.add((UDDINamedEntity)serviceForPolicyList.getModel().getElementAt(i));
                }

                if ( !services.equals(oldServicesList) ) {
                    JOptionPane.showMessageDialog(this,
                            "The list of services does not match those found in UDDI.\nPlease search for services and try again.",
                            "Unable to delete policy",
                            JOptionPane.OK_OPTION);
                } else {
                    UDDIPublicationPortType publicationPort = getPublishPort();
                    for (UDDINamedEntity uddiEntity : oldServicesList) {
                        String serviceKey = uddiEntity.getKey();

                        GetServiceDetail getServiceDetail = new GetServiceDetail();
                        getServiceDetail.setAuthInfo(authToken);
                        getServiceDetail.getServiceKey().add(serviceKey);
                        ServiceDetail serviceDetail = inquiryPort.getServiceDetail(getServiceDetail);

                        if (serviceDetail.getBusinessService().size() != 1) {
                            String msg = "UDDI registry returned either empty serviceDetail or " +
                                         "more than one business services";
                            throw new Exception(msg);
                        }

                        //get the bag for the service
                        BusinessService toUpdate = get(serviceDetail.getBusinessService(), "service", true);
                        Collection<CategoryBag> cbags = new ArrayList<CategoryBag>();
                        {
                            CategoryBag cbag = toUpdate.getCategoryBag();
                            if (cbag == null) {
                                cbag = new CategoryBag();
                                toUpdate.setCategoryBag(cbag);
                            }
                            cbags.add(cbag);
                        }

                        // find references and remove them
                        for (CategoryBag cbag : cbags) {
                            Collection<KeyedReference> updated = new ArrayList<KeyedReference>();
                            if (cbag.getKeyedReference() != null) {
                                for (KeyedReference kref : cbag.getKeyedReference()) {
                                    if (kref.getTModelKey().toString().equals(tModelKeyLocalPolicyReference) &&
                                        kref.getKeyValue().toString().equals(policyKey)) {
                                        // then we don't wan't this one
                                    } else {
                                        updated.add(kref);
                                    }
                                }
                            }

                            cbag.getKeyedReference().clear();
                            cbag.getKeyedReference().addAll(updated);
                        }

                        // update service in uddi
                        SaveService saveService = new SaveService();
                        saveService.setAuthInfo(authToken);
                        saveService.getBusinessService().add(toUpdate);
                        publicationPort.saveService(saveService);
                    }

                    // delete policy
                    DeleteTModel deleteTModel = new DeleteTModel();
                    deleteTModel.setAuthInfo(authToken);
                    deleteTModel.getTModelKey().add(policyKey);
                    publicationPort.deleteTModel(deleteTModel);

                    // update view
                    policySearchList.setListData(new Object[0]);
                    serviceForPolicyList.setListData(new Object[0]);
                }
            }
        } catch (DispositionReportFaultMessage drfm) {
            throw buildFaultException("Error listing policies: ", drfm);
        }
    }

    private String authToken() throws Exception {
        login = credsUsernameTextField.getText();
        password = new String(credsPasswordField.getPassword());

        if (authToken == null && (login!=null && login.trim().length()>0)) {
            authToken = getAuthToken(login.trim(), password);
        }

        return authToken;
    }

    private String getAuthToken(final String login,
                                  final String password) throws Exception {
        String authToken = null;

        try {
            UDDISecurityPortType securityPort = getSecurityPort();
            GetAuthToken getAuthToken = new GetAuthToken();
            getAuthToken.setUserID(login);
            getAuthToken.setCred(password);
            authToken = securityPort.getAuthToken(getAuthToken).getAuthInfo();
        } catch (DispositionReportFaultMessage drfm) {
            throw buildFaultException("Error getting authentication token: ", drfm);
        } catch (RuntimeException e) {
            throw new Exception("Error getting authentication token.", e);
        }

        return authToken;
    }

   private UDDISecurityPortType getSecurityPort() {
        UDDISecurity security = new UDDISecurity(buildUrl("resources/uddi_v3_service_s.wsdl"), new QName(UDDIV3_NAMESPACE, "UDDISecurity"));
        UDDISecurityPortType securityPort = security.getUDDISecurityPort();
        stubConfig(securityPort, urlSecurityTextField.getText());
        return securityPort;
    }

    private UDDIInquiryPortType getInquirePort() {
        UDDIInquiry inquiry = new UDDIInquiry(buildUrl("resources/uddi_v3_service_i.wsdl"), new QName(UDDIV3_NAMESPACE, "UDDIInquiry"));
        UDDIInquiryPortType inquiryPort = inquiry.getUDDIInquiryPort();
        stubConfig(inquiryPort, urlInquiryTextField.getText());
        return inquiryPort;
    }

    private UDDIPublicationPortType getPublishPort() {
        UDDIPublication publication = new UDDIPublication(buildUrl("resources/uddi_v3_service_p.wsdl"), new QName(UDDIV3_NAMESPACE, "UDDIPublication"));
        UDDIPublicationPortType publicationPort = publication.getUDDIPublicationPort();
        stubConfig(publicationPort, urlPublishTextField.getText());
        return publicationPort;
    }

    private URL buildUrl(String relativeUrl) {
        return UDDIInquiry.class.getResource(relativeUrl);
    }

    private void stubConfig(Object proxy, String url) {
        BindingProvider bindingProvider = (BindingProvider) proxy;
        Binding binding = bindingProvider.getBinding();
        Map<String,Object> context = bindingProvider.getRequestContext();
        List<Handler> handlerChain = new ArrayList();

        // Add handler to fix any issues with invalid faults
        //handlerChain.add(new FaultRepairSOAPHandler());

        // Set handlers
        binding.setHandlerChain(handlerChain);

        // Set endpoint
        context.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, url);
    }

    private <T> T get(List<T> list, String description, boolean onlyOne) throws Exception {
        if (list == null || list.isEmpty()) {
            throw new Exception("Missing " + description);
        } else if (onlyOne && list.size()!=1) {
            throw new Exception("Duplicate " + description);
        }

        return list.get(0);
    }

    private Exception buildFaultException(final String contextMessage,
                                          final DispositionReportFaultMessage faultMessage) {
        Exception exception;

        if ( hasResult(faultMessage, 10150) ) {
            exception = new Exception("Authentication failed for '" + login + "'.");
        } else if ( hasResult(faultMessage, 10140) ||
                    hasResult(faultMessage, 10120)) {
                exception = new Exception("Authorization failed for '" + login + "'.");
        } else if ( hasResult(faultMessage, 10110)) {
                exception = new Exception("Session expired or invalid.");
        } else if ( hasResult(faultMessage, 10400)) {
                exception = new Exception("UDDI registry is too busy.");
        } else if ( hasResult(faultMessage, 10040)) {
                exception = new Exception("UDDI registry version mismatch.");
        } else if ( hasResult(faultMessage, 10050)) {
                exception = new Exception("UDDI registry does not support a required feature.");
        } else {
            // handle general exception
            exception = new Exception(contextMessage + toString(faultMessage));
        }

        return exception;
    }

    private String toString(DispositionReportFaultMessage dispositionReport) {
        StringBuffer buffer = new StringBuffer(512);

        DispositionReport report = dispositionReport.getFaultInfo();
        if ( report != null ) {
            for (Result result : report.getResult()) {
                buffer.append("errno:");
                buffer.append(result.getErrno());
                ErrInfo info = result.getErrInfo();
                buffer.append("/errcode:");
                buffer.append(info.getErrCode());
                buffer.append("/description:");
                buffer.append(info.getValue());
            }
        }

        return buffer.toString();
    }    

    private boolean hasResult(DispositionReportFaultMessage faultMessage, int errorCode) {
        boolean foundResult = false;

        DispositionReport report = faultMessage.getFaultInfo();
        if ( report != null ) {
            for (Result result : report.getResult()) {
                if ( result.getErrno() == errorCode ) {
                    foundResult = true;
                    break;
                }
            }
        }

        return foundResult;
    }

    private void mergePolicyUrlToInfo(final String policyKey,
                                      final String policyUrl,
                                      final List<UDDINamedEntity> uddiNamedEntity) {
        for (int i=0; i<uddiNamedEntity.size(); i++) {
            UDDINamedEntity current = uddiNamedEntity.get(i);

            if ( policyKey.equals(current.getKey()) ) {
                UDDINamedEntity merged = new UDDINamedEntity(
                        current.getKey(),
                        current.getName(),
                        policyUrl,
                        current.getWsdlUrl());
                uddiNamedEntity.remove(i);
                uddiNamedEntity.add(i, merged);
                break;
            }
        }
    }

    private FindQualifiers buildFindQualifiers(boolean caseSensitive) {
        FindQualifiers findQualifiers = new FindQualifiers();
        List<String> qualifiers = findQualifiers.getFindQualifier();
        qualifiers.add(FINDQUALIFIER_APPROXIMATE);

        if ( !caseSensitive )
            qualifiers.add(FINDQUALIFIER_CASEINSENSITIVE);

        return findQualifiers;
    }

    private KeyedReference buildKeyedReference(String key, String name, String value) {
        KeyedReference keyedReference = new KeyedReference();
        keyedReference.setTModelKey(key);
        keyedReference.setKeyName(name);
        keyedReference.setKeyValue(value);
        return keyedReference;
    }

    private Name buildName(String value) {
        Name name = new Name();
        name.setValue(value);
        return name;
    }

    /**
     *
     */
    public static void main(String[] args) {
        new UDDIPolicyTool().setVisible(true);
    }

    /**
     * Function for accessing either the display or tooltip text.
     */
    private static final class TextListCellProvider implements Functions.Unary<String, Object> {
        private final boolean showName;

        private TextListCellProvider(final boolean showName) {
            this.showName = showName;
        }

        public String call(final Object entityObject) {
            UDDINamedEntity uddiNamedEntity = (UDDINamedEntity) entityObject;
            String text;

            if ( showName ) {
                // generate the entry text
                String tModelName = uddiNamedEntity.getName();
                String tModelKey = uddiNamedEntity.getKey();
                text = tModelName + " [" + tModelKey + "]";
            } else {
                // generate the tooltip text
                text = uddiNamedEntity.getPolicyUrl();
            }

            return text;
        }
    }

    public static class UDDINamedEntity {
        private final String key;
        private final String name;
        private final String wsdlUrl;
        private final String policyUrl;

        public UDDINamedEntity(String key, String name) {
            this(key, name, null, null);
        }

        public UDDINamedEntity(String key, String name, String policyUrl, String wsdlUrl) {
            this.key = key;
            this.name = name;
            this.policyUrl = policyUrl;
            this.wsdlUrl = wsdlUrl;
        }

        public String getKey() {
            return key;
        }

        public String getName() {
            return name;
        }

        public String getPolicyUrl() {
            return policyUrl;
        }

        public String getWsdlUrl() {
            return wsdlUrl;
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            UDDINamedEntity that = (UDDINamedEntity) o;

            if (key != null ? !key.equals(that.key) : that.key != null) return false;
            if (name != null ? !name.equals(that.name) : that.name != null) return false;
            if (policyUrl != null ? !policyUrl.equals(that.policyUrl) : that.policyUrl != null) return false;
            if (wsdlUrl != null ? !wsdlUrl.equals(that.wsdlUrl) : that.wsdlUrl != null) return false;

            return true;
        }

        public int hashCode() {
            int result;
            result = (key != null ? key.hashCode() : 0);
            result = 31 * result + (name != null ? name.hashCode() : 0);
            result = 31 * result + (wsdlUrl != null ? wsdlUrl.hashCode() : 0);
            result = 31 * result + (policyUrl != null ? policyUrl.hashCode() : 0);
            return result;
        }
    }
}
