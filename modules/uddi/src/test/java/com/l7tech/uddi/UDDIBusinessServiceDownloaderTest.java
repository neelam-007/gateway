package com.l7tech.uddi;

import org.junit.Test;
import org.junit.Assert;
import com.l7tech.wsdl.Wsdl;
import com.l7tech.common.uddi.guddiv3.BusinessService;
import com.l7tech.common.uddi.guddiv3.TModel;
import com.l7tech.util.Pair;

import javax.wsdl.WSDLException;
import java.util.*;
import java.io.Reader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
public class UDDIBusinessServiceDownloaderTest {


    /**
     * Tests that when given a list of BusinessServices, the UDDIProxiedServiceDownloader correctly processes
     * them 
     * @throws WSDLException
     */
    @Test
    public void testServiceDownload() throws Exception {
        //Set up the test, need to configure the UDDIClient
        Wsdl wsdl = Wsdl.newInstance(null, getWsdlReader( "com/l7tech/uddi/Warehouse.wsdl" ));

        final String gatewayWsdlUrl = "http://localhost:8080/3828382?wsdl";
        final String gatewayURL = "http://localhost:8080/3828382";
        EndpointPair endpointPair = new EndpointPair(gatewayURL, gatewayWsdlUrl);

        final int serviceOid = 3828382;
        WsdlToUDDIModelConverter wsdlToUDDIModelConverter = new WsdlToUDDIModelConverter(wsdl, "uddi:uddi_business_key");
        wsdlToUDDIModelConverter.convertWsdlToUDDIModel(Arrays.asList(endpointPair), "Layer7", Long.toString(serviceOid));

        final List<Pair<BusinessService, Map<String, TModel>>> serviceToDependentModels = wsdlToUDDIModelConverter.getServicesAndDependentTModels();
        List<BusinessService> allServices = new ArrayList<BusinessService>();
        List<String> allTModelKeys = new ArrayList<String>();
        for(Pair<BusinessService, Map<String, TModel>> serviceToModels: serviceToDependentModels){
            allServices.add(serviceToModels.left);
            for(String s: serviceToModels.right.keySet()){
                allTModelKeys.add(s);
            }
        }
        TestUddiClient uddiClient = new TestUddiClient(allServices);

        UDDIBusinessServiceDownloader serviceDownloader = new UDDIBusinessServiceDownloader(uddiClient);
        Set<String> serviceKeys = new HashSet<String>();
        for(BusinessService bs: allServices){
            serviceKeys.add(bs.getServiceKey());
        }

        final List<Pair<BusinessService, Map<String, TModel>>>
                downloadedServicesAndTModels = serviceDownloader.getBusinessServiceModels(serviceKeys);

        Assert.assertNotNull("Pair should never be null", downloadedServicesAndTModels);
        Assert.assertNotNull("List<BusinessService> should never be null", downloadedServicesAndTModels.get(0).left);
        Assert.assertNotNull("Map<String, TModel> should never be null", downloadedServicesAndTModels.get(0).right);

        Assert.assertEquals("Incorrect number of BusinessServices found", 1, downloadedServicesAndTModels.size());

        //confirm all tModels were extracted
        for(String s: allTModelKeys){
            Assert.assertTrue("tModel should have been found: " + s, downloadedServicesAndTModels.get(0).right.containsKey(s));
        }
    }

    private Reader getWsdlReader(String resourcetoread) {
        InputStream inputStream = WsdlTUDDIModelConverterTest.class.getClassLoader().getResourceAsStream(resourcetoread);
        return new InputStreamReader(inputStream);
    }

}
