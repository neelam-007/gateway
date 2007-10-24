package com.l7tech.common.uddi;

import java.util.Collection;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.systinet.uddi.client.v3.struct.Find_service;
import org.systinet.uddi.client.v3.struct.NameArrayList;
import org.systinet.uddi.client.v3.struct.Name;
import org.systinet.uddi.client.v3.struct.ServiceList;
import org.systinet.uddi.client.v3.struct.ServiceInfoArrayList;
import org.systinet.uddi.client.v3.struct.ServiceInfo;
import org.systinet.uddi.client.v3.struct.Get_authToken;
import org.systinet.uddi.client.v3.struct.Discard_authToken;
import org.systinet.uddi.client.v3.struct.TModel;
import org.systinet.uddi.client.v3.struct.CategoryBag;
import org.systinet.uddi.client.v3.struct.KeyedReference;
import org.systinet.uddi.client.v3.struct.DescriptionArrayList;
import org.systinet.uddi.client.v3.struct.Description;
import org.systinet.uddi.client.v3.struct.OverviewDocArrayList;
import org.systinet.uddi.client.v3.struct.OverviewDoc;
import org.systinet.uddi.client.v3.struct.OverviewURL;
import org.systinet.uddi.client.v3.struct.Save_tModel;
import org.systinet.uddi.client.v3.struct.TModelArrayList;
import org.systinet.uddi.client.v3.struct.TModelDetail;
import org.systinet.uddi.client.v3.struct.ServiceDetail;
import org.systinet.uddi.client.v3.struct.Get_serviceDetail;
import org.systinet.uddi.client.v3.struct.BusinessService;
import org.systinet.uddi.client.v3.struct.KeyedReferenceArrayList;
import org.systinet.uddi.client.v3.struct.BindingTemplate;
import org.systinet.uddi.client.v3.struct.Save_service;
import org.systinet.uddi.client.v3.struct.Find_tModel;
import org.systinet.uddi.client.v3.struct.TModelList;
import org.systinet.uddi.client.v3.struct.TModelInfoArrayList;
import org.systinet.uddi.client.v3.struct.TModelInfo;
import org.systinet.uddi.client.v3.struct.Save_binding;
import org.systinet.uddi.client.v3.struct.Get_tModelDetail;
import org.systinet.uddi.client.v3.struct.ListDescription;
import org.systinet.uddi.client.v3.UDDI_Inquiry_PortType;
import org.systinet.uddi.client.v3.UDDIInquiryStub;
import org.systinet.uddi.client.v3.UDDI_Security_PortType;
import org.systinet.uddi.client.v3.UDDISecurityStub;
import org.systinet.uddi.client.v3.UDDI_Publication_PortType;
import org.systinet.uddi.client.v3.UDDIPublishStub;
import org.systinet.uddi.client.base.StringArrayList;
import org.systinet.uddi.InvalidParameterException;

import com.l7tech.common.util.ExceptionUtils;

/**
 *
 */
class SystinetUDDIClient implements UDDIClient {

    private static final Logger logger = Logger.getLogger(SystinetUDDIClient.class.getName());

    private static final String URL_SUFFIX_SECURITY_API = "security";
    private static final String URL_SUFFIX_INQUIRY_API = "inquiry";
    private static final String URL_SUFFIX_PUBLICATION_API = "publishing";

    private final String inquiryUrl;
    private final String publishUrl;
    private final String securityUrl;
    private final String login;
    private final String password;
    private String authToken;
    private boolean moreAvailable;

    /**
     * If publishUrl or securityUrl are not set they are derived from the inquiry url.
     */
    public SystinetUDDIClient(String inquiryUrl, String publishUrl, String securityUrl, String login, String password) {
        this.inquiryUrl = inquiryUrl;
        this.publishUrl = publishUrl;
        this.securityUrl = securityUrl;
        this.login = login;
        this.password = password;
    }

    private String getSecurityUrl() {
        String url = securityUrl;
        if (url == null || url.length() == 0) {
            url =  getUrl(inquiryUrl, URL_SUFFIX_SECURITY_API);
        }
        return url;
    }

    private String getInquiryUrl() {
        return getUrl(inquiryUrl, URL_SUFFIX_INQUIRY_API);
    }

    private String getPublicationUrl() {
        String url = publishUrl;
        if (url == null || url.length() == 0) {
            url =  getUrl(inquiryUrl, URL_SUFFIX_PUBLICATION_API);
        }
        return url;
    }

    private String getUrl(String base, String suffix) {
        String url = base;

        if (url.endsWith(URL_SUFFIX_INQUIRY_API)) {
            url = url.substring(0, url.length() - URL_SUFFIX_INQUIRY_API.length());                        
        }

        if (url.indexOf("/uddi") < 1) {
            if (url.endsWith("/")) {
                url += "uddi/";
            } else {
                url += "/uddi/";
            }
        }

        if (!url.endsWith("/")) {
            url += "/";
        }

        return url + suffix;
    }

    private String getAuthToken(String login, String password) throws UDDIException {
        try {
            UDDI_Security_PortType security = UDDISecurityStub.getInstance(getSecurityUrl());
            Get_authToken getToken = new Get_authToken(login, password);
            return security.get_authToken(getToken).getAuthInfo();
        } catch (Throwable e) {
            throw new UDDIException("Error getting authentication token.", e);
        }
    }

    private String authToken() throws UDDIException {
        if (authToken == null && (login!=null && login.trim().length()>0)) {
            authToken = getAuthToken(login.trim(), password);
        }

        return authToken;
    }

    private boolean authenticated() {
        return authToken != null;
    }

    public void authenticate() throws UDDIException {
        authToken();
    }

    public String publishPolicy(String name, String description, String url) throws UDDIException {
        // create a tmodel to save
        TModel tmodel = new TModel();
        try {
            CategoryBag cbag = new CategoryBag();
            cbag.addKeyedReference(new KeyedReference("uddi:schemas.xmlsoap.org:policytypes:2003_03", "policy", "policy"));
            cbag.addKeyedReference(new KeyedReference("uddi:schemas.xmlsoap.org:remotepolicyreference:2003_03",
                                                      url,
                                                      description
                                                      ));
            tmodel.setCategoryBag(cbag);
            tmodel.setName(new Name(name));
            DescriptionArrayList dal = new DescriptionArrayList();
            dal.add(new Description(description));
            OverviewDocArrayList odal = new OverviewDocArrayList();
            OverviewDoc odoc = new OverviewDoc();
            odoc.setOverviewURL(new OverviewURL(url));
            odal.add(odoc);
            tmodel.setOverviewDocArrayList(odal);
            tmodel.setDescriptionArrayList(dal);
        } catch (InvalidParameterException e) {
            throw new UDDIException("Error creating model", e);
        }

        Save_tModel stm = new Save_tModel();
        String authToken = authToken();
        if (authToken != null)
            stm.setAuthInfo(authToken);

        try {
            TModelArrayList tmal = new TModelArrayList();
            tmal.add(tmodel);
            stm.setTModelArrayList(tmal);
            UDDI_Publication_PortType publishing = UDDIPublishStub.getInstance(getPublicationUrl());
            TModelDetail tModelDetail = publishing.save_tModel(stm);
            TModel saved = tModelDetail.getTModelArrayList().get(0);

            return saved.getTModelKey();
        }  catch (Throwable e) {
            throw new UDDIException("Error publishing model.", e);
        }
    }

    public void close() {
        if (authenticated()) {
            try {
                UDDI_Security_PortType security = UDDISecurityStub.getInstance(getSecurityUrl());
                security.discard_authToken(new Discard_authToken(authToken()));
                authToken = null;
            } catch (Throwable e) {
                logger.log(Level.INFO, "Error logging out.", e);   
            }
        }
    }

    public Collection<UDDINamedEntity> listServiceWsdls(String servicePattern, boolean caseSensitive, int offset, int maxRows) throws UDDIException {
        throw new UnsupportedOperationException();
    }

    public Collection<UDDINamedEntity> listServices(String servicePattern, boolean caseSensitive, int offset, int maxRows) throws UDDIException {
        moreAvailable = false;
        Collection<UDDINamedEntity> serviceInfos = new ArrayList();

        Find_service findService = new Find_service();
        String authToken = authToken();
        if (authToken != null)
            findService.setAuthInfo(authToken);

        String filter = servicePattern;
        if (filter != null) {
            filter = filter.toLowerCase();
        }

        try {
            if (filter != null && filter.length() > 0) {
                NameArrayList businessKey = new NameArrayList(new Name(filter));
                findService.setNameArrayList(businessKey);
            }
            StringArrayList qualifierList = new StringArrayList();
            qualifierList.add("approximateMatch");
            if (!caseSensitive)
                qualifierList.add("caseInsensitiveMatch");
            findService.setFindQualifierArrayList(qualifierList);
            if (maxRows > 0)
                findService.setMaxRows(Integer.valueOf(maxRows));
            if (offset > 0)
                findService.setListHead(Integer.valueOf(offset));

            UDDI_Inquiry_PortType inquiry = UDDIInquiryStub.getInstance(getInquiryUrl());
            ServiceList uddiServiceListRes = inquiry.find_service(findService);

            // more results?
            ListDescription listDescription = uddiServiceListRes.getListDescription();
            if (listDescription != null) {
                moreAvailable = listDescription.getActualCount() > listDescription.getListHead() + (listDescription.getIncludeCount()-1);
            }

            // display those services in the list instead
            ServiceInfoArrayList serviceInfoArrayList = uddiServiceListRes.getServiceInfoArrayList();
            if (serviceInfoArrayList != null) {
                for (Iterator iterator = serviceInfoArrayList.iterator(); iterator.hasNext();) {
                    ServiceInfo serviceInfo = (ServiceInfo) iterator.next();
                    String name = serviceInfo.getNameArrayList().get(0).getValue();
                    name = name.toLowerCase();
                    serviceInfos.add(new UDDINamedEntityImpl(serviceInfo.getServiceKey(), name));
                }
            }
            
            return serviceInfos;
        } catch (Throwable e) {
            throw new UDDIException("Error listing services.", e);
        }
    }

    public Collection<UDDINamedEntity> listPolicies(String policyPattern, String policyUrl) throws UDDIException {
        moreAvailable = false;
        Collection<UDDINamedEntity> policyInfos = new ArrayList();
        String filter = policyPattern;
        Find_tModel findtModel = new Find_tModel();
        try {
            findtModel.addFindQualifier("approximateMatch");
            findtModel.addFindQualifier("caseInsensitiveMatch");
            CategoryBag cbag = new CategoryBag();
            cbag.addKeyedReference(new KeyedReference("uddi:schemas.xmlsoap.org:policytypes:2003_03", "policy", null));
            if (policyUrl != null && policyUrl.trim().length() > 0)
                cbag.addKeyedReference(new KeyedReference("uddi:schemas.xmlsoap.org:remotepolicyreference:2003_03", policyUrl, null));
            findtModel.setCategoryBag(cbag);
            if (filter != null && filter.trim().length() > 0)
                findtModel.setName(new Name(filter));

            String authToken = authToken();
            if (authToken != null)
                findtModel.setAuthInfo(authToken);
            
            UDDI_Inquiry_PortType inquiry = UDDIInquiryStub.getInstance(getInquiryUrl());
            TModelList tModelList = inquiry.find_tModel(findtModel);

            // more results?
            ListDescription listDescription = tModelList.getListDescription();
            if (listDescription != null) {
                moreAvailable = listDescription.getActualCount() > listDescription.getListHead() + (listDescription.getIncludeCount()-1);
            }

            TModelInfoArrayList tModelInfoArrayList = tModelList.getTModelInfoArrayList();
            if (tModelInfoArrayList != null) {
                for (Iterator iterator = tModelInfoArrayList.iterator(); iterator.hasNext();) {
                    TModelInfo tModel = (TModelInfo) iterator.next();
                    if (tModel.getName() != null) {
                        String tmodelname = tModel.getName().getValue();
                        policyInfos.add(new UDDINamedEntityImpl(tModel.getTModelKey(), tmodelname));
                    }
                }
            }

            return policyInfos;
        } catch (Throwable e) {
            throw new UDDIException("Error listing policies.", e);
        }
    }

    public boolean listMoreAvailable() {
        return moreAvailable;
    }

    public String getPolicyUrl(String policyKey) throws UDDIException {
        String policyURL = null;
        try {
            // get the policy url and try to fetch it
            Get_tModelDetail gettmDetail = new Get_tModelDetail();
            gettmDetail.setTModelKeyArrayList(new StringArrayList(policyKey));
            
            UDDI_Inquiry_PortType inquiry = UDDIInquiryStub.getInstance(getInquiryUrl());

            String authToken = authToken();
            if (authToken != null)
                gettmDetail.setAuthInfo(authToken);

            TModelDetail res = inquiry.get_tModelDetail(gettmDetail);
            if (res != null && res.getTModelArrayList() != null && res.getTModelArrayList().size() == 1) {
                TModel tModel = res.getTModelArrayList().get(0);

                CategoryBag categoryBag = tModel.getCategoryBag();
                if (categoryBag == null)
                    throw new UDDIException("ERROR missing categoryBag");

                KeyedReferenceArrayList keyedReferenceArrayList = categoryBag.getKeyedReferenceArrayList();
                if (keyedReferenceArrayList == null)
                    throw new UDDIException("ERROR missing keyedReferenceArrayList");

                for (int i=0; i<keyedReferenceArrayList.size(); i++) {
                    KeyedReference keyedReference = keyedReferenceArrayList.get(i);
                    if ("uddi:schemas.xmlsoap.org:remotepolicyreference:2003_03".equals(keyedReference.getTModelKey())) {
                        policyURL = keyedReference.getKeyValue();
                        break;
                    }
                }

               if (policyURL == null) {
                    throw new UDDIException("Invalid policy '"+policyKey+"' (policy reference not found).");
                }                                
            } else {
                throw new UDDIException("ERROR get_tModelDetail returned zero or multiple tModels");
            }
        } catch (Throwable e) {
            throw new UDDIException("ERROR cannot get tModel Detail. " + ExceptionUtils.getMessage(e), e);
        }

        return policyURL;
    }

    public Collection<UDDINamedEntity> listEndpoints(String serviceName, boolean caseSensitive, int offset, int maxRows) throws UDDIException {
        throw new UDDIException("Not supported");
    }

    public Collection<UDDINamedEntity> listOrganizations(String serviceName, boolean caseSensitive, int offset, int maxRows) throws UDDIException {
        throw new UDDIException("Not supported");
    }

    public Collection<String> listPolicyUrlsByEndpoint(String key) throws UDDIException {
        throw new UDDIException("Not supported");
    }

    public Collection<String> listPolicyUrlsByOrganization(String key) throws UDDIException {
        throw new UDDIException("Not supported");
    }

    public Collection<String> listPolicyUrlsByService(String key) throws UDDIException {
        throw new UDDIException("Not supported");
    }

    public void referencePolicy(String serviceKey, String serviceUrl, boolean organization, String policyKey, String policyUrl, String description, Boolean force, boolean create) throws UDDIException {
        if (create)
            throw new UDDIException("Create not implemented");

        // first, get service from the service key
        boolean localReference = policyKey!=null && policyKey.trim().length()>0;
        boolean remoteReference = policyUrl!=null && policyUrl.trim().length()>0;
        if (!localReference && !remoteReference)
            throw new UDDIException("No policy to attach.");

        if (organization)
            throw new UDDIException("Organization attachment is not supported.");

        boolean isEndpoint = serviceUrl != null && serviceUrl.trim().length()>0;
        
        String authToken = authToken();
        ServiceDetail serviceDetail;
        try {
            Get_serviceDetail getServiceDetail = new Get_serviceDetail();
            getServiceDetail.setServiceKeyArrayList(new StringArrayList(serviceKey));
            UDDI_Inquiry_PortType inquiry = UDDIInquiryStub.getInstance(getInquiryUrl());
            if (authToken != null)
                getServiceDetail.setAuthInfo(authToken);

            serviceDetail = inquiry.get_serviceDetail(getServiceDetail);
        } catch (Throwable e) {
            throw new UDDIException("Error getting service details.", e);
        }

        if (serviceDetail.getBusinessServiceArrayList() == null ||
            serviceDetail.getBusinessServiceArrayList().size() != 1) {
            String msg = "UDDI registry returned either empty serviceDetail or " +
                         "more than one business services (" + serviceDetail.getBusinessServiceArrayList().size() + ")";
            throw new UDDIException(msg);
        }

        //get the right bag for either service or endpoint
        BusinessService toUpdate = serviceDetail.getBusinessServiceArrayList().get(0);
        Collection<BindingTemplate> bindingTemplatesToUpdate = new ArrayList();
        Collection<CategoryBag> cbags = new ArrayList();
        if (isEndpoint) {
            if (toUpdate.getBindingTemplateArrayList() != null && serviceUrl != null) {
                for (Iterator i = toUpdate.getBindingTemplateArrayList().iterator(); i.hasNext(); ) {
                    BindingTemplate bt = (BindingTemplate) i.next();
                    if (bt.getAccessPoint() != null &&
                        bt.getAccessPoint().getValue() != null &&
                        bt.getAccessPoint().getValue().equals(serviceUrl)) {
                        CategoryBag cbag = bt.getCategoryBag();
                        if (cbag == null) {
                            cbag = new CategoryBag();
                            bt.setCategoryBag(cbag);
                        }
                        cbags.add(cbag);
                        bindingTemplatesToUpdate.add(bt);
                    }
                }
                if (bindingTemplatesToUpdate.isEmpty())
                    throw new UDDIException("Service has no binding for this endpoint '" + serviceUrl + "'.");
            }
        }
        else {
            CategoryBag cbag = toUpdate.getCategoryBag();
            if (cbag == null) {
                cbag = new CategoryBag();
                toUpdate.setCategoryBag(cbag);
            }
            cbags.add(cbag);
        }

        // check for existing references and remove them
        for (CategoryBag cbag : cbags) {
            if (cbag.getKeyedReferenceArrayList() != null) {
                KeyedReferenceArrayList kreflist = cbag.getKeyedReferenceArrayList();
                for (Iterator i = kreflist.iterator(); i.hasNext(); ) {
                    KeyedReference kref = (KeyedReference)i.next();
                    if (kref.getTModelKey().equals("uddi:schemas.xmlsoap.org:localpolicyreference:2003_03") ||
                        kref.getTModelKey().equals("uddi:schemas.xmlsoap.org:remotepolicyreference:2003_03")) {
                        if (force == null)
                            throw new UDDIExistingReferenceException(kref.getKeyValue());
                        if (force.booleanValue() == true)
                            i.remove();
                    }
                }
            }
        }

        try {
            for (CategoryBag cbag : cbags) {
                // assign policy tModel in categoryBag
                if ( localReference ) {
                    cbag.addKeyedReference(new KeyedReference("uddi:schemas.xmlsoap.org:localpolicyreference:2003_03", policyKey, description));
                }

                if ( remoteReference ) {
                    cbag.addKeyedReference(new KeyedReference("uddi:schemas.xmlsoap.org:remotepolicyreference:2003_03", policyUrl, description));
                }
            }

            if (!isEndpoint) {
                // update service in uddi
                Save_service save = new Save_service();
                save.addBusinessService(toUpdate);
                if (authToken != null)
                    save.setAuthInfo(authToken);
                UDDI_Publication_PortType publishing = UDDIPublishStub.getInstance(getPublicationUrl());
                publishing.save_service(save);
            } else {
                // save the binding templates
                Save_binding save = new Save_binding();
                for (BindingTemplate bt : bindingTemplatesToUpdate) {
                    save.addBindingTemplate(bt);
                }
                if (authToken != null)
                    save.setAuthInfo(authToken);
                UDDI_Publication_PortType publishing = UDDIPublishStub.getInstance(getPublicationUrl());
                publishing.save_binding(save);                
            }
        } catch (Throwable e) {
            throw new UDDIException("Error updating service details.", e);
        }
    }
}
