package com.l7tech.server.service.uddi.impl.systinet;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;
import javax.xml.soap.SOAPException;

import org.systinet.uddi.InvalidParameterException;
import org.systinet.uddi.client.UDDIException;
import org.systinet.uddi.client.base.StringArrayList;
import org.systinet.uddi.client.v3.UDDIInquiryStub;
import org.systinet.uddi.client.v3.UDDI_Inquiry_PortType;
import org.systinet.uddi.client.v3.struct.BindingDetail;
import org.systinet.uddi.client.v3.struct.BindingTemplate;
import org.systinet.uddi.client.v3.struct.BindingTemplateArrayList;
import org.systinet.uddi.client.v3.struct.Find_binding;
import org.systinet.uddi.client.v3.struct.Find_service;
import org.systinet.uddi.client.v3.struct.Get_tModelDetail;
import org.systinet.uddi.client.v3.struct.ListDescription;
import org.systinet.uddi.client.v3.struct.Name;
import org.systinet.uddi.client.v3.struct.NameArrayList;
import org.systinet.uddi.client.v3.struct.OverviewDoc;
import org.systinet.uddi.client.v3.struct.OverviewDocArrayList;
import org.systinet.uddi.client.v3.struct.ServiceInfo;
import org.systinet.uddi.client.v3.struct.ServiceInfoArrayList;
import org.systinet.uddi.client.v3.struct.ServiceList;
import org.systinet.uddi.client.v3.struct.TModel;
import org.systinet.uddi.client.v3.struct.TModelArrayList;
import org.systinet.uddi.client.v3.struct.TModelDetail;
import org.systinet.uddi.client.v3.struct.TModelInstanceInfo;
import org.systinet.uddi.client.v3.struct.TModelInstanceInfoArrayList;
import org.systinet.uddi.client.v3.struct.CategoryBag;
import org.systinet.uddi.client.v3.struct.KeyedReference;

import com.l7tech.common.uddi.WsdlInfo;
import com.l7tech.server.service.uddi.UddiAgentException;
import com.l7tech.server.service.uddi.UddiAgent;

/**
 * UDDI agent implementation that uses Systinet v3 client.
 *
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 *
 * @author fpang
 */
public class UddiAgentV3 implements UddiAgent {

    private final Logger logger = Logger.getLogger(getClass().getName());
    private int resultRowsMax;
    private int resultBatchSize;

    /**
     * Constructor
     */
    public UddiAgentV3() {
        resultRowsMax = -1;
        resultBatchSize = -1;
    }

    /**
     *
     * @param props The properties of the UDDI Agent.
     */
    public void init(Properties props) {
        if (resultRowsMax != -1) throw new IllegalStateException("already initialized");

        String rowsMax = props.getProperty(PROP_RESULT_ROWS_MAX, "100");     // default 100 rows max
        resultRowsMax = Integer.parseInt(rowsMax);
        String batchSize = props.getProperty(PROP_RESULT_BATCH_SIZE, "100");
        resultBatchSize = Integer.parseInt(batchSize);
    }

    /**
     * Get the WSDL info given the service name pattern.
     *
     * @param namePattern   the exact name or part of the name
     * @param caseSensitive  true if case sensitive, false otherwise.
     * @return WsdlInfo[] an array of WSDL info.
     * @throws UddiAgentException   if there was a problem accessing the requested information.
     */
    public WsdlInfo[] getWsdlByServiceName(String inquiryUrl, String namePattern, boolean caseSensitive) throws UddiAgentException {
        checkInit();
        // % denotes wildcard of string (any number of characters), underscore denotes wildcard of a single character

        int listHead = 1;

        int actualCount = 0;
        HashMap wsdlUrls = new HashMap();
        boolean maxedOutSearch = false;

        do {
            Map wsdlUrlsChunk;

            ServiceList services = retrieveServiceByName(inquiryUrl, namePattern, caseSensitive, listHead);

            ListDescription listDescription = services.getListDescription();

            if (listDescription != null) {
                actualCount = listDescription.getActualCount();
                listHead = listDescription.getListHead() + listDescription.getIncludeCount();

                wsdlUrlsChunk = retrieveWsdlUrl(inquiryUrl, services);
                int chunkSize = wsdlUrlsChunk.size();
                if (wsdlUrls.size() + chunkSize <= resultRowsMax) {
                    wsdlUrls.putAll(wsdlUrlsChunk);
                } else {
                    Iterator it = wsdlUrlsChunk.keySet().iterator();
                    for ( int wc = 0; wc < wsdlUrlsChunk.size() && wsdlUrls.size() < resultRowsMax && it.hasNext(); wc++) {
                        String key = (String) it.next();
                        Object value = wsdlUrlsChunk.get(key);
                        wsdlUrls.put(key, value);
                    }
                    maxedOutSearch = true;
                    break;
                }
            } else {
                break;
            }

        } while (actualCount >= listHead);

        WsdlInfo[] siList = new WsdlInfo[wsdlUrls.size() + (maxedOutSearch ? 1 : 0)];
        Iterator itr = wsdlUrls.entrySet().iterator();

        int i = 0;
        while (itr.hasNext()) {
            Map.Entry entry = (Map.Entry) itr.next();
            siList[i++] = new WsdlInfo((String) entry.getKey(), (String) entry.getValue());
        }

        if (maxedOutSearch)
            siList[i] = WsdlInfo.MAXED_OUT_SEARCH_RESULT;

        return siList;
    }

    /**
     * Check if initialized.
     *
     * @throws UddiAgentException if not initialized.
     */
    private void checkInit() throws UddiAgentException  {
        if (resultRowsMax == -1) throw new UddiAgentException("Not initialized.");
    }

    /**
     * Executes get_tModelDetail call with arguments stored in Get_tModelDetail object.
     *
     * @param get Argument to UDDI call.
     * @return TModelDetail
     * @throws SOAPException SOAP related problems.
     * @throws UDDIException If the UDDI call fails.
     */
    private TModelDetail getTModels(Get_tModelDetail get, String inquiryURL) throws UDDIException, SOAPException {
        UDDI_Inquiry_PortType inquiry = getInquiryStub(inquiryURL);
        TModelDetail tModelDetail = inquiry.get_tModelDetail(get);
        return tModelDetail;
    }

    /**
     * Retrieve services given the name pattern.
     *
     * @param serviceName   the exact name or part of the name
     * @param caseSensitive  true if case sensitive
     * @param listHead   the starting position of the retrieval from the list
     * @return ServiceList the list of services.
     */
    private ServiceList retrieveServiceByName(String inquiryURL, String serviceName, boolean caseSensitive, int listHead) throws UddiAgentException {

        Find_service find_service = createFindServiceByName(serviceName, caseSensitive, listHead);
        ServiceList services = null;
        try {
            services = findService(find_service, inquiryURL);
        } catch (UDDIException e) {
            logger.warning("Exception caught: " + e.getMessage());
            throw new UddiAgentException(e.getMessage());
        } catch (SOAPException e) {
            logger.warning("Exception caught: " + e.getMessage());
            throw new UddiAgentException(e.getMessage());
        } catch (UndeclaredThrowableException e) {
            String possibleCauses = "\nPossible causes: the UDDI Registry is not running properly, or \n the host/port settings in uddi.properties file is not correct.";
            if(e.getCause() != null) {
                logger.warning(e.getCause().getMessage() + ", " + possibleCauses);
                throw new UddiAgentException(e.getCause().getMessage() + possibleCauses, e);
            } else {
                logger.warning(e.getMessage() + ", " + possibleCauses);
                throw new UddiAgentException(e.getMessage() + possibleCauses, e);
            }
        }
        return services;
    }

    /**
     * Executes find_service call with arguments stored in Find_service object.
     *
     * @param find_service Arguments to UDDI call.
     * @return List of ServiceInfos.
     * @throws javax.xml.soap.SOAPException SOAP related problems.
     * @throws org.systinet.uddi.client.v3.UDDIException
     *                                      If the UDDI call fails.
     */
    private ServiceList findService(Find_service find_service, String inquiryURL) throws UDDIException, SOAPException {
        UDDI_Inquiry_PortType inquiry = getInquiryStub(inquiryURL);
        ServiceList serviceList = inquiry.find_service(find_service);
        return serviceList;
    }

    /**
     * Executes find_binding call with arguments stored in Find_binding object.
     *
     * @param find_binding Arguments to UDDI call.
     * @return List of BindingInfos.
     * @throws SOAPException SOAP related problems.
     * @throws UDDIException If the UDDI call fails.
     */
    private BindingDetail findBinding(Find_binding find_binding, String inquiryURL) throws UDDIException, SOAPException {
        UDDI_Inquiry_PortType inquiry = getInquiryStub(inquiryURL);
        BindingDetail bindingDetail = inquiry.find_binding(find_binding);
        return bindingDetail;
    }

    /**
     * Finds stub for UDDI Inquiry API. The URL is read from properties.
     *
     * @return Inquiry API stub
     * @throws SOAPException If SOAP call fails.
     */
    private UDDI_Inquiry_PortType getInquiryStub(String inquiryURL) throws SOAPException {
        UDDI_Inquiry_PortType inquiry = UDDIInquiryStub.getInstance(inquiryURL);
        return inquiry;
    }

    /**
     * Creates and fills the Find_service structure.
     *
     * @param serviceName how the service must be categorized.
     * @param caseSensitive  True if case sensitive, false otherwise.
     * @return Object, which represents find_service UDDI call.
     */
    private Find_service createFindServiceByName(String serviceName, boolean caseSensitive, int listHead) throws UddiAgentException {
        Find_service find_service = new Find_service();
        try {
            NameArrayList businessKey = new NameArrayList(new Name(serviceName));
            find_service.setNameArrayList(businessKey);

            StringArrayList qualifierList = new StringArrayList();
            qualifierList.add("approximateMatch");
            if(!caseSensitive) {
                qualifierList.add("caseInsensitiveMatch");
            }
            find_service.setFindQualifierArrayList(qualifierList);

            CategoryBag cb = new CategoryBag();
            KeyedReference kr = new KeyedReference();
            kr.setKeyValue("service");
            kr.setTModelKey("uddi:uddi.org:wsdl:types");
            cb.addKeyedReference(kr);
            find_service.setCategoryBag(cb);

        } catch (InvalidParameterException e) {
            logger.warning("Exception caught: " + e.getMessage());
            throw new UddiAgentException(e.getMessage());
        }
        find_service.setMaxRows(new Integer(resultBatchSize));
        find_service.setListHead(new Integer(listHead));
        return find_service;
    }

    /**
     * Creates and fills the Find_binding structure.
     *
     * @param servicesKey business service to be searched.
     * @return Object, which represents find_binding UDDI call.
     * @throws InvalidParameterException If the value is invalid.
     */
    private Find_binding createFindBindingByServiceKey(String servicesKey) throws InvalidParameterException {
        Find_binding find_binding = new Find_binding();
        find_binding.setServiceKey(servicesKey);
        find_binding.setMaxRows(new Integer(resultBatchSize));

        CategoryBag cb = new CategoryBag();
        KeyedReference kr = new KeyedReference();
        kr.setKeyValue("port");
        kr.setTModelKey("uddi:uddi.org:wsdl:types");
        cb.addKeyedReference(kr);
        find_binding.setCategoryBag(cb);

        return find_binding;
    }

    /**
     * Retrieve the URLs of the given services.
     *
     * @param services  the list of services whose URLs are to be retrieved.
     * @return  HashMap contains list of the serivceName/serviceURL retrieved.
     */
    private HashMap retrieveWsdlUrl(String inquiryURL, ServiceList services) throws UddiAgentException {
        HashMap wsdlList = new HashMap();

        ServiceInfoArrayList serviceInfoList = services.getServiceInfoArrayList();
        if (serviceInfoList == null) return wsdlList;

        for (int i = 0; i < serviceInfoList.size(); i++) {

            ServiceInfo si = serviceInfoList.get(i);
            BindingDetail bindingDetail = null;
            try {
                Find_binding find_binding = createFindBindingByServiceKey(si.getServiceKey());
                bindingDetail = findBinding(find_binding, inquiryURL);
            }catch (UDDIException e) {
                logger.warning("Exception caught: " + e.getMessage());
                throw new UddiAgentException(e.getMessage());
            } catch (SOAPException e) {
                logger.warning("Exception caught: " + e.getMessage());
                throw new UddiAgentException(e.getMessage());
            } catch (InvalidParameterException e) {
                logger.warning("Exception caught: " + e.getMessage());
                throw new UddiAgentException(e.getMessage());
            }
            BindingTemplateArrayList btal = bindingDetail.getBindingTemplateArrayList();

            if(btal == null) continue;

            List<String> tModelKeys = new ArrayList();
            for (int j = 0; j < btal.size(); j++) {
                BindingTemplate bt = btal.get(j);
                TModelInstanceInfoArrayList tModelInstances = bt.getTModelInstanceInfoArrayList();

                if(tModelInstances == null) continue;

                for (int k = 0; k < tModelInstances.size(); k++) {
                    TModelInstanceInfo tModelInstanceInfo = tModelInstances.get(k);
                    String tModelKey = tModelInstanceInfo.getTModelKey();
                    if (tModelInstanceInfo.getInstanceDetails()==null) continue; // binding must have name param
                    tModelKeys.add(tModelKey);
                }
            }

            if (tModelKeys.size()==0) continue;

            TModelDetail tModelDetail = null;

            try {
                Get_tModelDetail get_tModelDetail = createGetTModelDetails(tModelKeys);
                tModelDetail = getTModels(get_tModelDetail, inquiryURL);
            } catch (UDDIException e) {
                logger.warning("Exception caught: " + e.getMessage());
                throw new UddiAgentException(e.getMessage());
            } catch (SOAPException e) {
                logger.warning("Exception caught: " + e.getMessage());
                throw new UddiAgentException(e.getMessage());
            } catch (InvalidParameterException e) {
                logger.warning("Exception caught: " + e.getMessage());
                throw new UddiAgentException(e.getMessage());
            }
            //                   printTModelDetail(tModelDetail);
            TModelArrayList tmal = tModelDetail.getTModelArrayList();

            if(tmal == null) continue;

            for (int l = 0; l < tmal.size(); l++) {
                TModel tm = tmal.get(l);
                OverviewDocArrayList odal = tm.getOverviewDocArrayList();

                if(odal == null) continue;

                for (int m = 0; m < odal.size(); m++) {
                    OverviewDoc od = odal.get(m);

                    if (od != null &&
                            od.getOverviewURL() != null &&
                            od.getOverviewURL().getUseType() != null &&
                            od.getOverviewURL().getUseType().equalsIgnoreCase("wsdlInterface")) {

                        final String wsdlURL = od.getOverviewURL().getValue();
                        final String serviceName = si.getNameArrayList().get(0).getValue();
                        if (wsdlURL != null) {
                            // we don't store duplcated entry
                            if(!wsdlList.containsKey(serviceName) ||
                                    (wsdlList.containsKey(serviceName) && !((String)wsdlList.get(serviceName)).equals(wsdlURL))) {
                                wsdlList.put(serviceName, wsdlURL);
                            } else {
                                logger.fine("Ignore the duplicated entry: Service Name = " + serviceName + ", wsdl url = " + wsdlURL);
                            }
                        }
                    }
                }
            }
        }

        return wsdlList;
    }

    /**
     * Creates and fills the Get_tModelDetail structure.
     *
     * @param tModelKeys keys of tModels to be retrieved.
     * @return Object, which represents Get_tModelDetail UDDI call.
     * @throws InvalidParameterException If the value is invalid.
     */
    private static Get_tModelDetail createGetTModelDetails(List<String> tModelKeys) throws InvalidParameterException {
        Get_tModelDetail get = new Get_tModelDetail();
        get.setTModelKeyArrayList(new StringArrayList(tModelKeys.toArray(new String[0])));
        return get;
    }
}
