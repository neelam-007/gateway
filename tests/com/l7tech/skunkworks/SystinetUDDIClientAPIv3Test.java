package com.l7tech.skunkworks;

import org.systinet.uddi.client.v3.struct.*;
import org.systinet.uddi.client.v3.UDDI_Inquiry_PortType;
import org.systinet.uddi.client.v3.UDDIInquiryStub;
import org.systinet.uddi.client.UDDIException;
import org.systinet.uddi.client.base.StringArrayList;
import org.systinet.uddi.InvalidParameterException;

import javax.xml.soap.SOAPException;
import java.util.*;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class SystinetUDDIClientAPIv3Test {
    //The number of values that will be listed in the results of find calls
    public static final int MAX_ROWS = 100;
    public static final String URL_HOST = "http://localhost:8085/";
    public static final String URL_INQUIRY = URL_HOST + "uddi/inquiry";
    private static boolean printDetails = false;

    /**
     * Creates and fills the Find_service structure.
     *
     * @param serviceName how the service must be categorized.
     * @return Object, which represents find_service UDDI call.
     * @throws InvalidParameterException If the value is invalid.
     */
    public static Find_service createFindServcieByName(String serviceName) throws InvalidParameterException {
        Find_service find_service = new Find_service();
        NameArrayList businessKey = new NameArrayList(new Name(serviceName));
        find_service.setNameArrayList(businessKey);
        StringArrayList qualifierList = new StringArrayList();
        qualifierList.add("approximateMatch");
        find_service.setFindQualifierArrayList(qualifierList);
        find_service.setMaxRows(new Integer(MAX_ROWS));
        return find_service;
    }

    /**
     * Creates and fills the Find_service structure.
     *
     * @param businessKey business to be searched.
     * @param category    how the service must be categorized.
     * @return Object, which represents find_service UDDI call.
     * @throws InvalidParameterException If the value is invalid.
     */
    public static Find_service createFindServiceByCategory(String businessKey, KeyedReference category) throws InvalidParameterException {
        Find_service find_service = new Find_service();
        find_service.setBusinessKey(businessKey);
        StringArrayList qualifierList = new StringArrayList();
        qualifierList.add("approximateMatch");
        find_service.setFindQualifierArrayList(qualifierList);
        find_service.setCategoryBag(new CategoryBag(new KeyedReferenceArrayList(category)));
        find_service.setMaxRows(new Integer(MAX_ROWS));
        return find_service;
    }

    /**
     * Creates and fills the Find_binding structure.
     *
     * @param servicesKey business service to be searched.
     * @return Object, which represents find_binding UDDI call.
     * @throws InvalidParameterException If the value is invalid.
     */
    public static Find_binding createFindBindingByServiceKey(String servicesKey) throws InvalidParameterException {
        System.out.println("serviceKey = " + servicesKey);
        Find_binding find_binding = new Find_binding();
        find_binding.setServiceKey(servicesKey);
        find_binding.setMaxRows(new Integer(MAX_ROWS));
        return find_binding;
    }

    /**
     * Creates and fills the Get_tModelDetail structure.
     *
     * @param tModelKey key of tModel to be retrieved.
     * @return Object, which represents Get_tModelDetail UDDI call.
     * @throws InvalidParameterException If the value is invalid.
     */
    public static Get_tModelDetail createGetTModelDetail(String tModelKey) throws InvalidParameterException {
        System.out.println("tModelKey = " + tModelKey);
        Get_tModelDetail get = new Get_tModelDetail();
        get.setTModelKeyArrayList(new StringArrayList(tModelKey));
        return get;
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
    public static ServiceList findService(Find_service find_service) throws UDDIException, SOAPException {
        UDDI_Inquiry_PortType inquiry = getInquiryStub();
        System.out.print("Search in progress ..");
        ServiceList serviceList = inquiry.find_service(find_service);
        System.out.println(" done");
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
    public static BindingDetail findBinding(Find_binding find_binding) throws UDDIException, SOAPException {
        UDDI_Inquiry_PortType inquiry = getInquiryStub();
        System.out.print("Search in progress ..");
        BindingDetail bindingDetail = inquiry.find_binding(find_binding);
        System.out.println(" done");
        return bindingDetail;
    }

    /**
     * Executes get_tModelDetail call with arguments stored in Get_tModelDetail object.
     *
     * @param get Argument to UDDI call.
     * @return TModelDetail
     * @throws SOAPException SOAP related problems.
     * @throws UDDIException If the UDDI call fails.
     */
    public static TModelDetail getTModels(Get_tModelDetail get) throws UDDIException, SOAPException {
        UDDI_Inquiry_PortType inquiry = getInquiryStub();
        System.out.print("Search in progress ..");
        TModelDetail tModelDetail = inquiry.get_tModelDetail(get);
        System.out.println(" done");
        return tModelDetail;
    }


    /**
     * Finds stub for UDDI Inquiry API. The URL is read from properties.
     *
     * @return Inquiry API stub
     * @throws SOAPException If SOAP call fails.
     */
    public static UDDI_Inquiry_PortType getInquiryStub() throws SOAPException {
        // you can specify your own URL in property - uddi.demos.url.inquiry
        System.out.print("Using Inquiry at url " + URL_INQUIRY + " ..");
        UDDI_Inquiry_PortType inquiry = UDDIInquiryStub.getInstance(URL_INQUIRY);
        System.out.println(" done");
        return inquiry;
    }

    /**
     * Prints argument to the console.
     *
     * @param serviceList parameter to be displayed
     */
    public static void printServiceList(ServiceList serviceList) {
        if (!printDetails) return;

        System.out.println();
        ListDescription listDescription = serviceList.getListDescription();
        if (listDescription != null) {
            // list description is mandatory part of result, if the resultant list is subset of available data
            int includeCount = listDescription.getIncludeCount();
            int actualCount = listDescription.getActualCount();
            int listHead = listDescription.getListHead();
            System.out.println("Displaying " + includeCount + " of " + actualCount + ", starting at position " + listHead);
        }

        ServiceInfoArrayList serviceInfoArrayList = serviceList.getServiceInfoArrayList();
        if (serviceInfoArrayList == null) {
            System.out.println("Nothing found");
            return;
        }

        int position = 1;
        for (Iterator iterator = serviceInfoArrayList.iterator(); iterator.hasNext();) {
            ServiceInfo serviceInfo = (ServiceInfo) iterator.next();
            System.out.println("Service " + position + " : " + serviceInfo.getServiceKey());
            System.out.println(serviceInfo.toXML());
            System.out.println();
            System.out.println("********************************************************");
            position++;
        }
    }

    /**
     * Prints argument to the console.
     *
     * @param bindingDetail parameter to be displayed
     */
    public static void printBindingList(BindingDetail bindingDetail) {
        if (!printDetails) return;

        System.out.println();
        ListDescription listDescription = bindingDetail.getListDescription();
        if (listDescription != null) {
            // list description is mandatory part of result, if the resultant list is subset of available data
            int includeCount = listDescription.getIncludeCount();
            int actualCount = listDescription.getActualCount();
            int listHead = listDescription.getListHead();
            System.out.println("Displaying " + includeCount + " of " + actualCount + ", starting at position " + listHead);
        }

        BindingTemplateArrayList bindingTemplateArrayList = bindingDetail.getBindingTemplateArrayList();
        if (bindingTemplateArrayList == null) {
            System.out.println("Nothing found");
            return;
        }

        int position = 1;
        for (Iterator iterator = bindingTemplateArrayList.iterator(); iterator.hasNext();) {
            BindingTemplate bindingTemplate = (BindingTemplate) iterator.next();
            System.out.println("Binding " + position + " : " + bindingTemplate.getBindingKey());
            System.out.println(bindingTemplate.toXML());
            System.out.println();
            System.out.println("********************************************************");
            position++;
        }
    }

    /**
     * Prints argument to the console.
     *
     * @param tModelDetail parameter to be displayed
     */
    public static void printTModelDetail(TModelDetail tModelDetail) {
        if (!printDetails) return;

        System.out.println();
        TModelArrayList tModelArrayList = tModelDetail.getTModelArrayList();
        if (tModelArrayList == null) {
            System.out.println("Nothing found");
            return;
        }

        int position = 1;
        for (Iterator iterator = tModelArrayList.iterator(); iterator.hasNext();) {
            TModel entity = (TModel) iterator.next();
            System.out.println("TModel " + position + " : " + entity.getTModelKey());
            System.out.println(entity.toXML());
            System.out.println();
            System.out.println("********************************************************");
            position++;
        }
    }

    public static ServiceList retrieveServiceByName(String serviceName) throws Exception {

        System.out.println("Searching for service by name: " + serviceName);

        Find_service find_service = createFindServcieByName(serviceName);
        ServiceList services = findService(find_service);
        printServiceList(services);
        return services;
    }

    public static ServiceList retrieveServiceByCategory(String businessKey, KeyedReference category) throws Exception {
        System.out.println("Searching for service by category: " + category.getTModelKey() + ", " + category.getKeyName() + ", " + category.getKeyValue());
        Find_service find_service = createFindServiceByCategory(businessKey, category);
        ServiceList services = findService(find_service);
        printServiceList(services);
        return services;
    }

    public static Set retrieveWsdlUrl(ServiceList services) throws Exception {
        HashSet wsdlList = new HashSet();

        ServiceInfoArrayList serviceInfoList = services.getServiceInfoArrayList();
        if (serviceInfoList == null) return wsdlList;

        System.out.println("Number of servcies found: " + serviceInfoList.size() + "\n");

        for (int i = 0; i < serviceInfoList.size(); i++) {

            ServiceInfo si = serviceInfoList.get(i);
            Find_binding find_binding = createFindBindingByServiceKey(si.getServiceKey());
            BindingDetail bindingDetail = findBinding(find_binding);
            printBindingList(bindingDetail);

            BindingTemplateArrayList btal = bindingDetail.getBindingTemplateArrayList();

            for (int j = 0; j < btal.size(); j++) {
                BindingTemplate bt = btal.get(j);
                TModelInstanceInfoArrayList tModelInstances = bt.getTModelInstanceInfoArrayList();

                for (int k = 0; k < tModelInstances.size(); k++) {
                    TModelInstanceInfo tModelInstanceInfo = tModelInstances.get(k);
                    String tModelKey = tModelInstanceInfo.getTModelKey();

                    Get_tModelDetail get_tModelDetail = createGetTModelDetail(tModelKey);
                    TModelDetail tModelDetail = getTModels(get_tModelDetail);
                    printTModelDetail(tModelDetail);
                    TModelArrayList tmal = tModelDetail.getTModelArrayList();

                    for (int l = 0; l < tmal.size(); l++) {
                        TModel tm = tmal.get(l);
                        OverviewDocArrayList odal = tm.getOverviewDocArrayList();

                        for (int m = 0; m < odal.size(); m++) {
                            OverviewDoc od = odal.get(m);
                            if (od.getOverviewURL() != null) {
                                final String wsdlURL = od.getOverviewURL().getValue();
                                if (!wsdlList.contains(wsdlURL)) {
                                    wsdlList.add(wsdlURL);
                                }
                            }
                        }
                    }
                }
            }
        }

        return wsdlList;
    }

    public static void printWSDLs(Set wsdlList) {
        Iterator itr = wsdlList.iterator();
        for (int i = 0; itr.hasNext(); i++) {
            String wsdlURL = (String) itr.next();
            System.out.println("[" + i + "]: " + wsdlURL);
        }
    }

    private static void testGetWsdlByServiceName() throws Exception {
        // % denotes wildcard of string (any number of characters), underscore denotes wildcard of a single character
        String serviceName = "%U%DI%";
        ServiceList services = retrieveServiceByName(serviceName);
        Set wsdlList = retrieveWsdlUrl(services);

        if (wsdlList.size() > 0) {
            System.out.println("The WSDL URL found:");
            printWSDLs(wsdlList);
        }
    }

    private static void testGetWsdlUrlByCategory() throws Exception {
        String businessKey = "uddi:systinet.com:uddinodebusinessKey";
        String tModelKey = "uddi:uddi.org:wsdl:types";
        String keyValue = "service";
       // String keyName = "uddi.org:wsdl:types";
        String keyName = "service";

        KeyedReference category = new KeyedReference(tModelKey, keyValue, keyName);
        ServiceList services = retrieveServiceByCategory(businessKey, category);
        Set wsdlList = retrieveWsdlUrl(services);
        if (wsdlList.size() > 0) {
            System.out.println("The WSDL URL found:");
            printWSDLs(wsdlList);
        }
    }

    public static void main(String args[]) throws Exception {

        //  Systinet UDDI Client API Ver 3 test cases

        System.out.println("**************** Test 1 ***********************************");
        testGetWsdlByServiceName();

        System.out.println("\n\n**************** Test 2 ***********************************");
        testGetWsdlUrlByCategory();
    }

}
