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
import com.l7tech.test.BugNumber;

import javax.xml.bind.JAXB;
import java.util.*;

public class BusinessServicePublisherTest {

    /**
     * Test that the business service publisher will report that the correct number of services were published and
     * that it reports the correct number of services which need to be deleted as they no longer exist in uddi or
     * are no longer part of the services gateway's wsdl
     *
     * @throws Exception
     */
    @Test
    public void testBusinessServicePublisher() throws Exception {
        Wsdl wsdl = Wsdl.newInstance(null, WsdlTUDDIModelConverterTest.getWsdlReader("com/l7tech/uddi/Warehouse.wsdl"));

        final String gatewayWsdlUrl = "http://localhost:8080/3828382?wsdl";
        final String gatewayURL = "http://localhost:8080/3828382";

        final int serviceOid = 3828382;
        final String businessKey = "uddi:uddi_business_key";
        WsdlToUDDIModelConverter wsdlToUDDIModelConverter = new WsdlToUDDIModelConverter(wsdl, gatewayWsdlUrl, gatewayURL, businessKey, serviceOid);
        wsdlToUDDIModelConverter.convertWsdlToUDDIModel();

        UDDIClient uddiClient = new TestUddiClient(true);

        //before
//        for(Pair<BusinessService, Map<String, TModel>> serviceToModels: serviceToDependentModels){
//            BusinessService businessService = serviceToModels.left;
//            JAXB.marshal(businessService, System.out);
//            Collection<TModel> allTModels = serviceToModels.right.values();
//            for(TModel tModel: allTModels){
//                JAXB.marshal(tModel, System.out);
//            }
//        }

        BusinessServicePublisher servicePublisher = new BusinessServicePublisher(wsdl, serviceOid, uddiClient);

        final Pair<Set<String>, Set<UDDIBusinessService>> pair =
                servicePublisher.publishServicesToUDDIRegistry(gatewayURL, gatewayWsdlUrl, businessKey,
                        Collections.<String>emptySet(), false, null);

        Assert.assertNotNull("pair should not be null", pair);
        Assert.assertTrue("No services to delete should be found", pair.left.isEmpty());
        Assert.assertEquals("Incorrect number of services published", 1, pair.right.size());

        final Set<String> stringSet = new HashSet<String>();
        stringSet.addAll(Arrays.asList("service key"));
        final Pair<Set<String>, Set<UDDIBusinessService>> deletePair =
                servicePublisher.publishServicesToUDDIRegistry(gatewayURL, gatewayWsdlUrl, businessKey,
                        stringSet, false, null);

        Assert.assertNotNull("pair should not be null", deletePair);
        Assert.assertEquals("One service to delete should be found", 1, deletePair.left.size());
        Assert.assertEquals("Invalid service key of service to delete found", "service key", deletePair.left.iterator().next());
        Assert.assertEquals("Incorrect number of services published", 1, deletePair.right.size());

    }

    /**
     * Test that the created bindingTemplate is correctly constructed, itself, it's references to tModels, and the
     * tModels that were also created
     *
     * @throws Exception
     */
    @Test
    public void testPublishEndPoint() throws Exception {
        Wsdl wsdl = Wsdl.newInstance(null, WsdlTUDDIModelConverterTest.getWsdlReader("com/l7tech/uddi/Warehouse.wsdl"));

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

    /**
     * Tests that uddi registry specific meta data is added correctly for published business services
     *
     * @throws Exception
     */
    @Test
    @BugNumber(7980)
    public void testRegistrySpecificMetaData() throws Exception {
        Wsdl wsdl = Wsdl.newInstance(null, WsdlTUDDIModelConverterTest.getWsdlReader("com/l7tech/uddi/Warehouse.wsdl"));

        final String gatewayWsdlUrl = "http://localhost:8080/3828382?wsdl";
        final String gatewayURL = "http://localhost:8080/3828382";

        final int serviceOid = 3828382;

        //Test is now setup

        TestUddiClient uddiClient = new TestUddiClient();
        BusinessServicePublisher servicePublisher = new BusinessServicePublisher(wsdl, serviceOid, uddiClient);

        UDDIRegistrySpecificMetaData metaData = new UDDIRegistrySpecificMetaData() {
            @Override
            public Collection<UDDIClient.UDDIKeyedReference> getBusinessServiceKeyedReferences() {
                Collection<UDDIClient.UDDIKeyedReference> returnColl = new ArrayList<UDDIClient.UDDIKeyedReference>();
                UDDIClient.UDDIKeyedReference kr =
                        new UDDIClient.UDDIKeyedReference("uddi:088e847f-eab5-11dc-97f4-dcd945764407",
                                null, "Virtual service");
                returnColl.add(kr);
                return returnColl;
            }

            @Override
            public Collection<UDDIClient.UDDIKeyedReferenceGroup> getBusinessServiceKeyedReferenceGroups() {
                Collection<UDDIClient.UDDIKeyedReferenceGroup> returnColl = new ArrayList<UDDIClient.UDDIKeyedReferenceGroup>();
                Collection<UDDIClient.UDDIKeyedReference> keyRefColl = new ArrayList<UDDIClient.UDDIKeyedReference>();
                UDDIClient.UDDIKeyedReference kr =
                        new UDDIClient.UDDIKeyedReference("uddi:uddi.org:categorization:general_keywords",
                                "Contains", "pretend service key");
                keyRefColl.add(kr);

                UDDIClient.UDDIKeyedReferenceGroup krg =
                        new UDDIClient.UDDIKeyedReferenceGroup("uddi:centrasite.com:attributes:relationship", keyRefColl);

                returnColl.add(krg);
                return returnColl;
            }
        };

        final Pair<Set<String>, Set<UDDIBusinessService>> pair =
                servicePublisher.publishServicesToUDDIRegistry(gatewayURL, gatewayWsdlUrl, "business key",
                        Collections.<String>emptySet(), false, metaData);

        List<BusinessService> publishedServices = uddiClient.getPublishedServices();
        BusinessService testService = publishedServices.get(0);
        Assert.assertNotNull("Need to find the first service for the test", testService);

        Assert.assertNotNull("A categoryBag should always be found", testService.getCategoryBag());

        final List<KeyedReference> references = testService.getCategoryBag().getKeyedReference();
        Assert.assertEquals("Incorrect number of keyed references found", 4, references.size());

        KeyedReference virtualRef = references.get(references.size() - 1);
        Assert.assertEquals("Invalid key found for virtual keyed reference",
                "uddi:088e847f-eab5-11dc-97f4-dcd945764407", virtualRef.getTModelKey());

        Assert.assertEquals("Invalid name for virtual keyed reference",
                "Virtual service", virtualRef.getKeyValue());

        final List<KeyedReferenceGroup> referenceGroupList = testService.getCategoryBag().getKeyedReferenceGroup();
        Assert.assertNotNull("keyedReferenceGroup list should not be null", referenceGroupList);

        KeyedReferenceGroup krg = referenceGroupList.iterator().next();
        Assert.assertNotNull("KeyedReferenceGroup should have been added to the service and found", krg);

        final List<KeyedReference> referenceList = krg.getKeyedReference();
        Assert.assertNotNull("keyedReferenceGroup should contain a non null list of keyedReferences", referenceList);

        Assert.assertEquals("Invalid number of keyed references found in the keyed reference group", 1, referenceList.size());

        KeyedReference containsRef = referenceList.iterator().next();
        Assert.assertNotNull("containsRef should not be null", containsRef);

        Assert.assertEquals("Invalid tModelKey found", "uddi:uddi.org:categorization:general_keywords", containsRef.getTModelKey());
        Assert.assertEquals("Invalid key name found", "Contains", containsRef.getKeyName());
        Assert.assertEquals("Invalid key value found", "pretend service key", containsRef.getKeyValue());

    }

    private void testBindingTemplate(String gatewayWsdlUrl, String gatewayURL, String serviceKey, Pair<BindingTemplate, List<TModel>> bindingAndModels) {
        Map<String, TModel> keyToModel = new HashMap<String, TModel>();
        for (TModel model : bindingAndModels.right) {
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
        for (TModelInstanceInfo instanceInfo : tModelInstanceInfos) {
            if (instanceInfo.getInstanceDetails() != null) {
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
                for (KeyedReference keyRef : allRefs) {
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
            } else {
                //this is the wsdl:portType tModel reference
                TModel newPortTypeTModel = keyToModel.get(instanceInfo.getTModelKey());
                Assert.assertEquals("Incorrect tModel type found", UDDIUtilities.TMODEL_TYPE.WSDL_PORT_TYPE, UDDIUtilities.getTModelType(newPortTypeTModel, true));
                Assert.assertEquals("Incorrect tModel name found", "WarehouseSoap", newPortTypeTModel.getName().getValue());

                List<KeyedReference> allRefs = newPortTypeTModel.getCategoryBag().getKeyedReference();
                Assert.assertEquals("Incorrect number of keyedReferences found", 2, allRefs.size());

                Map<String, KeyedReference> tModelKeyToRef = new HashMap<String, KeyedReference>();
                for (KeyedReference keyRef : allRefs) {
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
}
