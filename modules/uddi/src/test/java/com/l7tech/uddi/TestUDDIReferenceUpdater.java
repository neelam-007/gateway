/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.uddi;

import org.junit.Test;
import com.l7tech.wsdl.Wsdl;
import com.l7tech.common.uddi.guddiv3.BusinessService;
import com.l7tech.common.uddi.guddiv3.TModel;
import com.l7tech.util.Pair;

import javax.xml.bind.JAXB;
import java.util.List;
import java.util.Map;

public class TestUDDIReferenceUpdater {

    @Test
    public void testUDDIReferenceUpdater() throws Exception {
        Wsdl wsdl = Wsdl.newInstance(null, TetstWsdlTUDDIModelConverter.getWsdlReader("Warehouse.wsdl"));

        final String gatewayWsdlUrl = "http://localhost:8080/3828382?wsdl";
        final String gatewayURL = "http://localhost:8080/3828382";

        WsdlToUDDIModelConverter wsdlToUDDIModelConverter = new WsdlToUDDIModelConverter(wsdl, gatewayWsdlUrl, gatewayURL, "uddi:uddi_business_key");
        Pair<List<BusinessService>, Map<String, TModel>> servicesAndTModels = wsdlToUDDIModelConverter.convertWsdlToUDDIModel();

        UDDIClient uddiClient = getUDDIClient();

        //before
        for(BusinessService businessService: servicesAndTModels.left){
            JAXB.marshal(businessService, System.out);
        }

        for(TModel tModel: servicesAndTModels.right.values()){
            JAXB.marshal(tModel, System.out);
        }

        BusinessServicePublisher servicePublisher = new BusinessServicePublisher();
        servicePublisher.publishServicesToUDDIRegistry(uddiClient, servicesAndTModels.left, servicesAndTModels.right);
        
        //Now test that all references were updated correctly

        //after
        for(BusinessService businessService: servicesAndTModels.left){
            JAXB.marshal(businessService, System.out);
        }

        for(TModel tModel: servicesAndTModels.right.values()){
            JAXB.marshal(tModel, System.out);
        }
    }

    public UDDIClient getUDDIClient() throws UDDIException {
        TestUddiClient uddiClient = new TestUddiClient();
        return uddiClient;
    }
}
