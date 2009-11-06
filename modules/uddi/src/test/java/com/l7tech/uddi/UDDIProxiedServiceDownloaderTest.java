package com.l7tech.uddi;

import org.junit.Test;
import org.junit.Assert;
import com.l7tech.wsdl.Wsdl;
import com.l7tech.common.uddi.guddiv3.BusinessService;
import com.l7tech.common.uddi.guddiv3.TModel;
import com.l7tech.util.Pair;

import javax.wsdl.WSDLException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.io.Reader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
public class UDDIProxiedServiceDownloaderTest {


    /**
     * Tests that when given a list of BusinessServices, the UDDIProxiedServiceDownloader correct processes
     * them 
     * @throws WSDLException
     */
    @Test
    public void testServiceDownload() throws Exception {
        //Set up the test, need to configure the UDDIClient
        Wsdl wsdl = Wsdl.newInstance(null, getWsdlReader("Warehouse.wsdl"));

        final String gatewayWsdlUrl = "http://localhost:8080/3828382?wsdl";
        final String gatewayURL = "http://localhost:8080/3828382";

        final int serviceOid = 3828382;
        WsdlToUDDIModelConverter wsdlToUDDIModelConverter = new WsdlToUDDIModelConverter(wsdl, gatewayWsdlUrl, gatewayURL, "uddi:uddi_business_key", serviceOid);
        wsdlToUDDIModelConverter.convertWsdlToUDDIModel();

        UDDIClient uddiClient = new TestUddiClient(wsdlToUDDIModelConverter.getBusinessServices());

        UDDIProxiedServiceDownloader serviceDownloader = new UDDIProxiedServiceDownloader(uddiClient);
        Set<String> serviceKeys = new HashSet<String>();
        for(BusinessService bs: wsdlToUDDIModelConverter.getBusinessServices()){
            serviceKeys.add(bs.getServiceKey());
        }

        Pair<List<BusinessService>, Map<String, TModel>>
                downloadedServicesAndTModels = serviceDownloader.getBusinessServiceModels(serviceKeys);

        Assert.assertNotNull("Pair should never be null", downloadedServicesAndTModels);
        Assert.assertNotNull("List<BusinessService> should never be null", downloadedServicesAndTModels.left);
        Assert.assertNotNull("Map<String, TModel> should never be null", downloadedServicesAndTModels.right);

        Assert.assertEquals("Incorrect number of BusinessServices found", 1, downloadedServicesAndTModels.left.size());
        //confirm all tModels were extracted
        for(String s: wsdlToUDDIModelConverter.getKeysToPublishedTModels().keySet()){
            Assert.assertTrue("tModel should have been found: " + s, downloadedServicesAndTModels.right.containsKey(s));
        }
    }

    private Reader getWsdlReader(String resourcetoread) {
        InputStream inputStream = WsdlTUDDIModelConverterTest.class.getClassLoader().getResourceAsStream(resourcetoread);
        return new InputStreamReader(inputStream);
    }

}
