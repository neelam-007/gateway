/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.uddi;

import org.junit.Test;
import org.junit.Assert;
import com.l7tech.wsdl.Wsdl;
import com.l7tech.common.uddi.guddiv3.*;
import com.l7tech.util.Pair;

import javax.xml.bind.JAXB;
import java.util.*;
import java.io.InputStream;

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

        final List<Pair<BusinessService, Map<String, TModel>>> serviceToDependentModels = wsdlToUDDIModelConverter.getServicesAndDependentTModels();

        //before
        for(Pair<BusinessService, Map<String, TModel>> serviceToModels: serviceToDependentModels){
            BusinessService businessService = serviceToModels.left;
            JAXB.marshal(businessService, System.out);
            Collection<TModel> allTModels = serviceToModels.right.values();
            for(TModel tModel: allTModels){
                JAXB.marshal(tModel, System.out);
            }
        }

        BusinessServicePublisher servicePublisher = new BusinessServicePublisher(wsdl, serviceOid, uddiClient);
        List<UDDIBusinessService> services = servicePublisher.publishServicesToUDDIRegistry(gatewayURL, gatewayWsdlUrl, businessKey);

        //after
//        for(UDDIBusinessService bs: services){
//            JAXB.marshal(bs, System.out);
//        }
    }

    //code to download and marshall UDDI objects
//        final String inquiry = "http://donalwinxp.l7tech.com:53307/UddiRegistry/inquiry";
//        final String publish = "http://donalwinxp.l7tech.com:53307/UddiRegistry/publish";
//        final String subscription = "http://donalwinxp.l7tech.com:53307/UddiRegistry/subscription";
//        final String security = "http://donalwinxp.l7tech.com:53307/UddiRegistry/security";
//        GenericUDDIClient uddiClient = new GenericUDDIClient(inquiry, publish, subscription, security, "administrator", "7layer", PolicyAttachmentVersion.v1_2);
//        UDDIClientConfig uddiClientConfig = new UDDIClientConfig(inquiry, publish, subscription, security, "administrator", "7layer");

//        UDDIProxiedServiceDownloader serviceDownloader = new UDDIProxiedServiceDownloader(uddiClient, uddiClientConfig);
//        Set<String> serviceKeys = new HashSet<String>();
//        serviceKeys.add("uddi:8617fae0-c987-11de-8486-efda8c54fa31");
//        Pair<List<BusinessService>, Map<String, TModel>> modelFromUddi = serviceDownloader.getBusinessServiceModels(serviceKeys);
//        Assert.assertEquals("", 1, modelFromUddi.left.size());
//
//        JAXB.marshal(modelFromUddi.left.get(0), new File("/home/darmstrong/Documents/Projects/Bondo/UDDI/Marshalled/BusinessService.xml"));
//        int i = 0;
//        for(TModel model: modelFromUddi.right.values()) {
//            JAXB.marshal(model, new File("/home/darmstrong/Documents/Projects/Bondo/UDDI/Marshalled/tModel" + i + ".xml"));
//            i++;
//        }

    /**
     * Test that the created bindingTemplate is correctly constructed, itself, it's references to tModels, and the
     * tModels that were also created
     * @throws Exception
     */
    @Test
    public void testPublishEndPoint() throws Exception{
        Wsdl wsdl = Wsdl.newInstance(null, WsdlTUDDIModelConverterTest.getWsdlReader("Warehouse.wsdl"));

        final String gatewayWsdlUrl = "http://localhost:8080/3828382?wsdl";
        final String gatewayURL = "http://localhost:8080/3828382";

        final int serviceOid = 3828382;

        //Test is now setup

        UDDIClient uddiClient = new TestUddiClient();
        BusinessServicePublisher servicePublisher = new BusinessServicePublisher(wsdl, serviceOid, uddiClient);

        Pair<BindingTemplate, List<TModel>> bindingAndModels =
                servicePublisher.publishEndPointToExistingService("serviceKey", "WarehouseSoap", "WarehouseSoap", gatewayURL, gatewayWsdlUrl, false);


        testBindingTemplate(gatewayWsdlUrl, gatewayURL, "serviceKey", bindingAndModels);
    }

    private void testBindingTemplate(String gatewayWsdlUrl, String gatewayURL, String serviceKey, Pair<BindingTemplate, List<TModel>> bindingAndModels) {
        Map<String, TModel> keyToModel = new HashMap<String, TModel>();
        for(TModel model: bindingAndModels.right){
            keyToModel.put(model.getTModelKey(), model);
        }
        BindingTemplate newTemplate = bindingAndModels.left;
        //were interested to know that the any models the binding references are new, and are not accidently
        //referencing models from the binding that was copied

        Assert.assertNotNull("Binding key should not be null", newTemplate.getBindingKey());

        //serviceKey
        Assert.assertEquals("Incorrect serviceKey found", serviceKey, newTemplate.getServiceKey());

        //AccessPoint
        Assert.assertEquals("Invalid useType found", "endPoint", newTemplate.getAccessPoint().getUseType());
        Assert.assertEquals("Invalid gateway WSDL URL found", gatewayURL, newTemplate.getAccessPoint().getValue());

        List<TModelInstanceInfo> tModelInstanceInfos = newTemplate.getTModelInstanceDetails().getTModelInstanceInfo();
        Assert.assertEquals("Incorrect number of TModelInstanceInfo elements found", 2, tModelInstanceInfos.size());
        //check it references the newly created tModels
        Assert.assertTrue("tModelKey reference should have been found: " + tModelInstanceInfos.get(0).getTModelKey()
        , keyToModel.containsKey(tModelInstanceInfos.get(0).getTModelKey()));
        Assert.assertTrue("tModelKey reference should have been found: " + tModelInstanceInfos.get(1).getTModelKey()
        , keyToModel.containsKey(tModelInstanceInfos.get(1).getTModelKey()));

        //Confirm the binding instanceParam
        boolean found = false;
        for(TModelInstanceInfo instanceInfo: tModelInstanceInfos){
            if(instanceInfo.getInstanceDetails() != null){
                InstanceDetails instanceDetails = instanceInfo.getInstanceDetails();
                Assert.assertEquals("Invalid instanceParam found", "WarehouseSoap", instanceDetails.getInstanceParms());
                found = true;
                //while were here, test the wsdl:binding tModel
                TModel newbindingTModel = keyToModel.get(instanceInfo.getTModelKey());
                Assert.assertEquals("Incorrect tModel type found", UDDIUtilities.TMODEL_TYPE.WSDL_BINDING, UDDIUtilities.getTModelType(newbindingTModel, true));
                Assert.assertEquals("Incorrect tModel name found", "WarehouseSoap", newbindingTModel.getName().getValue());
                //make sure it's got all of it's references
                List<KeyedReference> allRefs = newbindingTModel.getCategoryBag().getKeyedReference();
                Assert.assertEquals("Incorrect number of keyedReferences found", 6, allRefs.size());

                Map<String, KeyedReference> tModelKeyToRef = new HashMap<String, KeyedReference>();
                for(KeyedReference keyRef: allRefs){
                    tModelKeyToRef.put(keyRef.getTModelKey(), keyRef);
                }

                Assert.assertTrue("binding type keyed ref not found",
                        tModelKeyToRef.containsKey(WsdlToUDDIModelConverter.UDDI_WSDL_TYPES));
                Assert.assertEquals("Invalid binding keyValue for binding key reference", "binding",
                        tModelKeyToRef.get(WsdlToUDDIModelConverter.UDDI_WSDL_TYPES).getKeyValue());

                Assert.assertTrue("port type keyed ref not found",
                        tModelKeyToRef.containsKey(WsdlToUDDIModelConverter.UDDI_WSDL_PORTTYPEREFERENCE));
                TModel portTypeRef = keyToModel.get(tModelKeyToRef.get(WsdlToUDDIModelConverter.UDDI_WSDL_PORTTYPEREFERENCE).getKeyValue());
                Assert.assertEquals("Invalid portType keyed ref found", portTypeRef.getTModelKey(), tModelKeyToRef.get(WsdlToUDDIModelConverter.UDDI_WSDL_PORTTYPEREFERENCE).getKeyValue());

                Assert.assertTrue("wsdlSpec keyed ref not found",
                        tModelKeyToRef.containsKey(WsdlToUDDIModelConverter.UDDI_CATEGORIZATION_TYPES));
                Assert.assertEquals("Invalid wsdlSpec value found", "wsdlSpec", tModelKeyToRef.get(WsdlToUDDIModelConverter.UDDI_CATEGORIZATION_TYPES).getKeyValue());

                Assert.assertTrue("protocol keyed ref not found",
                        tModelKeyToRef.containsKey(WsdlToUDDIModelConverter.UDDI_WSDL_CATEGORIZATION_PROTOCOL));
                Assert.assertEquals("Invalid protocol value found", "uddi:uddi.org:protocol:soap", tModelKeyToRef.get(WsdlToUDDIModelConverter.UDDI_WSDL_CATEGORIZATION_PROTOCOL).getKeyValue());
                Assert.assertEquals("Invalid protocol name found", "SOAP protocol", tModelKeyToRef.get(WsdlToUDDIModelConverter.UDDI_WSDL_CATEGORIZATION_PROTOCOL).getKeyName());

                Assert.assertTrue("transport keyed ref not found",
                        tModelKeyToRef.containsKey(WsdlToUDDIModelConverter.UDDI_WSDL_CATEGORIZATION_TRANSPORT));
                Assert.assertEquals("Invalid transport value found", "uddi:uddi.org:transport:http", tModelKeyToRef.get(WsdlToUDDIModelConverter.UDDI_WSDL_CATEGORIZATION_TRANSPORT).getKeyValue());
                Assert.assertEquals("Invalid transport name found", "HTTP transport", tModelKeyToRef.get(WsdlToUDDIModelConverter.UDDI_WSDL_CATEGORIZATION_TRANSPORT).getKeyName());

                Assert.assertTrue("namespace keyed ref not found",
                        tModelKeyToRef.containsKey(WsdlToUDDIModelConverter.UDDI_XML_NAMESPACE));
                Assert.assertEquals("Invalid namespace value found", "http://warehouse.acme.com/ws", tModelKeyToRef.get(WsdlToUDDIModelConverter.UDDI_XML_NAMESPACE).getKeyValue());
                Assert.assertEquals("Invalid namespace name found", "binding namespace", tModelKeyToRef.get(WsdlToUDDIModelConverter.UDDI_XML_NAMESPACE).getKeyName());

                //Overview doc
                Assert.assertEquals("Incorrect number of overview docs found", 2, newbindingTModel.getOverviewDoc().size());
                OverviewDoc overviewDoc = newbindingTModel.getOverviewDoc().get(0);
                Assert.assertEquals("Invalid gateway WSDL URL found", gatewayWsdlUrl, overviewDoc.getOverviewURL().getValue());
            }else{
                //this is the wsdl:portType tModel reference
                TModel newPortTypeTModel = keyToModel.get(instanceInfo.getTModelKey());
                Assert.assertEquals("Incorrect tModel type found", UDDIUtilities.TMODEL_TYPE.WSDL_PORT_TYPE, UDDIUtilities.getTModelType(newPortTypeTModel, true));
                Assert.assertEquals("Incorrect tModel name found", "WarehouseSoap", newPortTypeTModel.getName().getValue());

                List<KeyedReference> allRefs = newPortTypeTModel.getCategoryBag().getKeyedReference();
                Assert.assertEquals("Incorrect number of keyedReferences found", 2, allRefs.size());

                Map<String, KeyedReference> tModelKeyToRef = new HashMap<String, KeyedReference>();
                for(KeyedReference keyRef: allRefs){
                    tModelKeyToRef.put(keyRef.getTModelKey(), keyRef);
                }

                Assert.assertTrue("portType type keyed ref not found",
                        tModelKeyToRef.containsKey(WsdlToUDDIModelConverter.UDDI_WSDL_TYPES));
                Assert.assertEquals("Invalid keyValue for portType key reference", "portType",
                        tModelKeyToRef.get(WsdlToUDDIModelConverter.UDDI_WSDL_TYPES).getKeyValue());

                Assert.assertTrue("namespace keyed ref not found",
                        tModelKeyToRef.containsKey(WsdlToUDDIModelConverter.UDDI_XML_NAMESPACE));
                Assert.assertEquals("Invalid namespace value found", "http://warehouse.acme.com/ws", tModelKeyToRef.get(WsdlToUDDIModelConverter.UDDI_XML_NAMESPACE).getKeyValue());
                Assert.assertEquals("Invalid namespace name found", "portType namespace", tModelKeyToRef.get(WsdlToUDDIModelConverter.UDDI_XML_NAMESPACE).getKeyName());
                //Overview doc
                Assert.assertEquals("Incorrect number of overview docs found", 2, newPortTypeTModel.getOverviewDoc().size());
                OverviewDoc overviewDoc = newPortTypeTModel.getOverviewDoc().get(0);
                Assert.assertEquals("Invalid gateway WSDL URL found", gatewayWsdlUrl, overviewDoc.getOverviewURL().getValue());

            }
        }
        Assert.assertTrue("No wsdl:binding instanceParam found", found);
    }

    public UDDIClient getUDDIClient() throws UDDIException {
        return new TestUddiClient();
    }

}
