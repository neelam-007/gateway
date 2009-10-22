package com.l7tech.skunkworks.uddi;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import com.l7tech.common.uddi.guddiv3.*;
import com.l7tech.uddi.*;
import com.l7tech.util.Pair;
import com.l7tech.wsdl.Wsdl;

import java.util.*;
import java.io.Reader;
import java.io.InputStream;
import java.io.InputStreamReader;

import junit.framework.Assert;

import javax.wsdl.WSDLException;

/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 *
 * Test the GenericUDDIClient implementation of UDDIClient with respect to publishing information to a UDDI Registry
 *
 * These tests requies a working UDDI Registry
 *
 * @author darmstrong
 */
public class TestGenericUDDIClient {

    private List<String> tModelKeys = new ArrayList<String>();
    private List<String> serviceKeys = new ArrayList<String>();
    private UDDIClient uddiClient = getUDDIClient();

    @Before
    public void setUp(){
        tModelKeys.clear();
        serviceKeys.clear();
    }

    @After
    public void tearDown() throws UDDIException {
        //Delete all published info
        for(String key: tModelKeys){
            uddiClient.deleteTModel(key);                        
        }

        for(String key: serviceKeys){
            uddiClient.deleteBusinessService(key);
        }
    }

    /**
     * Test the successful publish of a tModel to UDDI.
     *
     * Also test that once published successfully, a second publish attempt will return false, as the previously
     * published tModel should have been found
     */
    @Test
    public void testPublishTModel() throws UDDIException {
        //Create a tModel representing a wsdl:portType

        TModel tModel = new TModel();
        Name name = new Name();
        name.setLang("en-US");
        name.setValue("Skunkworks portType tModel " + Calendar.getInstance().getTimeInMillis());
        tModel.setName(name);

        CategoryBag categoryBag = new CategoryBag();
        tModel.setCategoryBag(categoryBag);
        List<KeyedReference> keyedReferences = categoryBag.getKeyedReference();

        //portType type ref
        KeyedReference typeRef = new KeyedReference();
        typeRef.setKeyValue("portType");
        typeRef.setTModelKey(WsdlToUDDIModelConverter.UDDI_WSDL_TYPES);
        keyedReferences.add(typeRef);

        //namespace ref
        KeyedReference nameSpaceRef = new KeyedReference();
        nameSpaceRef.setKeyValue("http://skunkworks.com/porttypenamespace");
        nameSpaceRef.setKeyName("portType namespace ");
        nameSpaceRef.setTModelKey(WsdlToUDDIModelConverter.UDDI_XML_NAMESPACE);
        keyedReferences.add(nameSpaceRef);

        //overview docs etc are considered in the logic in TestGenericUDDIClient, but not by UDDI find code
        OverviewDoc overviewDoc = new OverviewDoc();
        OverviewURL overviewURL = new OverviewURL();
        overviewURL.setUseType("wsdlInterface");
        overviewURL.setValue("http://skunkworks.ssg.com/serviceoid=23423432");
        overviewDoc.setOverviewURL(overviewURL);

        tModel.getOverviewDoc().add(overviewDoc);

        boolean published = uddiClient.publishTModel(tModel);
        Assert.assertTrue("TModel should have been published", published);
        tModelKeys.add(tModel.getTModelKey());

        published = uddiClient.publishTModel(tModel);
        Assert.assertFalse("TModel should not have been published", published);
        
    }

    @Test
    public void testPublishProxyBusinessService() throws UDDIException, WSDLException {
        Wsdl wsdl = Wsdl.newInstance(null, getWsdlReader("Warehouse.wsdl"));

        long serviceOid = Calendar.getInstance().getTimeInMillis();
        final String gatewayWsdlUrl = "http://localhost:8080/" + serviceOid + "?wsdl";
        final String gatewayURL = "http://localhost:8080/" + serviceOid;

        final String businessKey = "uddi:c4f2cbdd-beab-11de-8126-f78857d54072";//this exists in my local uddi registry
        WsdlToUDDIModelConverter wsdlToUDDIModelConverter = new WsdlToUDDIModelConverter(wsdl, gatewayWsdlUrl, gatewayURL, businessKey, serviceOid);
        Pair<List<BusinessService>, Map<String, TModel>> servicesAndTModels = wsdlToUDDIModelConverter.convertWsdlToUDDIModel();

        BusinessServicePublisher servicePublisher = new BusinessServicePublisher();
        servicePublisher.publishServicesToUDDIRegistry(uddiClient, servicesAndTModels.left, servicesAndTModels.right);

        for(BusinessService businessService: servicesAndTModels.left){
            serviceKeys.add(businessService.getServiceKey());
        }
    }

    /**
     * Bug number 7898
     * @throws UDDIException
     */
    @Test
    public void testUDDIBug() throws UDDIException {
        UDDIClient uddiClient = new GenericUDDIClient("http://rsbcentos.l7tech.com:8080/juddiv3/services/inquiry",
                        "http://rsbcentos.l7tech.com:8080/juddiv3/services/publish",
                        "http://rsbcentos.l7tech.com:8080/juddiv3/services/security",
                        "root",
                        "root",
                        PolicyAttachmentVersion.v1_2/*not important here*/);


        uddiClient.authenticate();

        Collection<UDDINamedEntity> uddiNamedEntities = uddiClient.listServices("%", false, 169401, 100);
        System.out.println("There were this many results: " + uddiNamedEntities.size());
        boolean moreAvail = uddiClient.listMoreAvailable();
        System.out.println("More are available: " + moreAvail);

    }

    private UDDIClient getUDDIClient(){
        return new GenericUDDIClient("http://DONALWINXP:53307/UddiRegistry/inquiry",
                        "http://DONALWINXP:53307/UddiRegistry/publish",
                        "http://DONALWINXP:53307/UddiRegistry/security",
                        "administrator",
                        "7layer",
                        PolicyAttachmentVersion.v1_2/*not important here*/);
    }

    public Reader getWsdlReader(String resourcetoread) {
        InputStream inputStream = this.getClass().getResourceAsStream(resourcetoread);
        return new InputStreamReader(inputStream);
    }

}
