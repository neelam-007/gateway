/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.uddi;

import org.junit.Test;
import com.l7tech.wsdl.Wsdl;
import com.l7tech.common.uddi.guddiv3.BusinessService;
import com.l7tech.common.uddi.guddiv3.TModel;
import javax.xml.bind.JAXB;

public class BusinessServicePublisherTest {

    /**
     * Test the entire process of publishing a list of Business Services
     *
     * Test test is in it's current form simply a pass through of the BusinessServicePublisher code with a given wsdl
     * It is also helpful for seeting the UDDI output which will be sent to the UDDI Registry.
     * 
     * @throws Exception
     */
    @Test
    public void testBusinessServicePublisher() throws Exception {
        Wsdl wsdl = Wsdl.newInstance(null, WsdlTUDDIModelConverterTest.getWsdlReader("Warehouse.wsdl"));

        final String gatewayWsdlUrl = "http://localhost:8080/3828382?wsdl";
        final String gatewayURL = "http://localhost:8080/3828382";

        final int serviceOid = 3828382;
        final String businessKey = "uddi:uddi_business_key";
        WsdlToUDDIModelConverter wsdlToUDDIModelConverter = new WsdlToUDDIModelConverter(wsdl, gatewayWsdlUrl, gatewayURL, businessKey, serviceOid);
        wsdlToUDDIModelConverter.convertWsdlToUDDIModel();

        UDDIClient uddiClient = getUDDIClient();

        //before
        for(BusinessService businessService: wsdlToUDDIModelConverter.getBusinessServices()){
            JAXB.marshal(businessService, System.out);
        }

        for(TModel tModel: wsdlToUDDIModelConverter.getKeysToPublishedTModels().values()){
            JAXB.marshal(tModel, System.out);
        }

        BusinessServicePublisher servicePublisher = new BusinessServicePublisher(wsdl, uddiClient, serviceOid, new UDDIClientConfig());
        servicePublisher.publishServicesToUDDIRegistry(gatewayURL, gatewayWsdlUrl, businessKey);

        //after
        for(BusinessService businessService: wsdlToUDDIModelConverter.getBusinessServices()){
            JAXB.marshal(businessService, System.out);
        }

        for(TModel tModel: wsdlToUDDIModelConverter.getKeysToPublishedTModels().values()){
            JAXB.marshal(tModel, System.out);
        }


    }

    public UDDIClient getUDDIClient() throws UDDIException {
        TestUddiClient uddiClient = new TestUddiClient();
        return uddiClient;
    }

}
