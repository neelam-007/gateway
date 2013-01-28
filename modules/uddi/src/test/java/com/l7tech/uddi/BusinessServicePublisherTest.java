package com.l7tech.uddi;

import com.l7tech.common.uddi.guddiv3.*;
import com.l7tech.test.BugId;
import com.l7tech.test.BugNumber;
import com.l7tech.util.Pair;
import com.l7tech.util.SyspropUtil;
import com.l7tech.wsdl.Wsdl;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;

import javax.xml.bind.JAXB;
import java.io.File;
import java.io.InputStream;
import java.util.*;

/**
 * @author darmstrong
 */
public class BusinessServicePublisherTest {

    @AfterClass
    public static void cleanupSystemProperties() {
        SyspropUtil.clearProperties(
            "com.l7tech.uddi.BusinessServicePublisher.prependServiceLocalName",
            "com.l7tech.uddi.BusinessServicePublisher.appendServiceLocalName"
        );
    }

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
        EndpointPair endpointPair = new EndpointPair(gatewayURL, gatewayWsdlUrl);

        final int serviceOid = 3828382;
        final String businessKey = "uddi:uddi_business_key";
        WsdlToUDDIModelConverter wsdlToUDDIModelConverter = new WsdlToUDDIModelConverter(wsdl, businessKey);
        wsdlToUDDIModelConverter.convertWsdlToUDDIModel(Arrays.asList(endpointPair), "Layer7", Long.toString(serviceOid));

        TestUddiClient uddiClient = new TestUddiClient(true);

        BusinessServicePublisher servicePublisher = new BusinessServicePublisher(wsdl, serviceOid, uddiClient);

        final Pair<Set<String>, Set<UDDIBusinessService>> pair =
                servicePublisher.publishServicesToUDDIRegistry(businessKey,
                        Collections.<String>emptySet(), false, null, Arrays.asList(endpointPair));

        Assert.assertNotNull("pair should not be null", pair);
        Assert.assertTrue("No services to delete should be found", pair.left.isEmpty());
        Assert.assertEquals("Incorrect number of services published", 1, pair.right.size());

        final Set<String> stringSet = new HashSet<String>();
        stringSet.addAll(Arrays.asList("service key"));
        final Pair<Set<String>, Set<UDDIBusinessService>> deletePair =
                servicePublisher.publishServicesToUDDIRegistry(businessKey ,stringSet, false, null, Arrays.asList(endpointPair));

        Assert.assertNotNull("pair should not be null", deletePair);
        Assert.assertEquals("One service to delete should be found", 1, deletePair.left.size());
        Assert.assertEquals("Invalid service key of service to delete found", "service key", deletePair.left.iterator().next());
        Assert.assertEquals("Incorrect number of services published", 1, deletePair.right.size());

        //validate categoryBag info
        final List<BusinessService> businessServiceList = uddiClient.getPublishedServices();
        Assert.assertEquals("Incorrect number of services found", 2, businessServiceList.size());

        final BusinessService pubService = businessServiceList.get(0);
        final List<KeyedReference> references = pubService.getCategoryBag().getKeyedReference();

        Assert.assertEquals("Incorrect tModelKey found","uddi:uddi.org:wsdl:types",references.get(0).getTModelKey());
        Assert.assertEquals("Incorrect keyValue found","service",references.get(0).getKeyValue());
        Assert.assertEquals("Incorrect keyName found","",references.get(0).getKeyName());

        Assert.assertEquals("Incorrect tModelKey found","uddi:uddi.org:xml:localname",references.get(1).getTModelKey());
        Assert.assertEquals("Incorrect keyValue found","Warehouse",references.get(1).getKeyValue());
        Assert.assertEquals("Incorrect keyName found","service local name",references.get(1).getKeyName());

        Assert.assertEquals("Incorrect tModelKey found","uddi:uddi.org:xml:namespace",references.get(2).getTModelKey());
        Assert.assertEquals("Incorrect keyValue found","http://warehouse.acme.com/ws",references.get(2).getKeyValue());
        Assert.assertEquals("Incorrect keyName found","service namespace",references.get(2).getKeyName());

        final List<KeyedReferenceGroup> groupRefs = pubService.getCategoryBag().getKeyedReferenceGroup();
        Assert.assertEquals("Incorrect number of groups found", 0, groupRefs.size());
    }

    /**
     * Same tests as above, just with two endpoints, makes no difference at a high level - the same number of services
     * are published
     * 
     * @throws Exception
     */
    @Test
    public void testBusinessServicePublisherTwoEndPoints() throws Exception {
        Wsdl wsdl = Wsdl.newInstance(null, WsdlTUDDIModelConverterTest.getWsdlReader("com/l7tech/uddi/Warehouse.wsdl"));

        final String gatewayWsdlUrl = "http://localhost:8080/3828382?wsdl";
        final String gatewayURL = "http://localhost:8080/3828382";
        EndpointPair endpointPair = new EndpointPair(gatewayURL, gatewayWsdlUrl);

        final String gatewayURHttps = "https://localhost:8080/3828382";
        EndpointPair endpointPairHttps = new EndpointPair(gatewayWsdlUrl, gatewayURHttps);

        final int serviceOid = 3828382;
        final String businessKey = "uddi:uddi_business_key";
        WsdlToUDDIModelConverter wsdlToUDDIModelConverter = new WsdlToUDDIModelConverter(wsdl, businessKey);
        wsdlToUDDIModelConverter.convertWsdlToUDDIModel(Arrays.asList(endpointPair, endpointPairHttps), "Layer7", Long.toString(serviceOid));

        UDDIClient uddiClient = new TestUddiClient(true);

        BusinessServicePublisher servicePublisher = new BusinessServicePublisher(wsdl, serviceOid, uddiClient);

        final Pair<Set<String>, Set<UDDIBusinessService>> pair =
                servicePublisher.publishServicesToUDDIRegistry(businessKey,
                        Collections.<String>emptySet(), false, null, Arrays.asList(endpointPair));

        Assert.assertNotNull("pair should not be null", pair);
        Assert.assertTrue("No services to delete should be found", pair.left.isEmpty());
        Assert.assertEquals("Incorrect number of services published", 1, pair.right.size());

        final Set<String> stringSet = new HashSet<String>();
        stringSet.addAll(Arrays.asList("service key"));
        final Pair<Set<String>, Set<UDDIBusinessService>> deletePair =
                servicePublisher.publishServicesToUDDIRegistry(businessKey ,stringSet, false, null, Arrays.asList(endpointPair));

        Assert.assertNotNull("pair should not be null", deletePair);
        Assert.assertEquals("One service to delete should be found", 1, deletePair.left.size());
        Assert.assertEquals("Invalid service key of service to delete found", "service key", deletePair.left.iterator().next());
        Assert.assertEquals("Incorrect number of services published", 1, deletePair.right.size());
    }

    /**
     * Same tests as above, just with two endpoints, makes no difference at a high level - the same number of services
     * are published
     *
     * @throws Exception
     */
    @Test
    public void testBusinessServicePublisherTwoEndPointsPlayerStats() throws Exception {
        Wsdl wsdl = Wsdl.newInstance(null, WsdlTUDDIModelConverterTest.getWsdlReader("com/l7tech/uddi/PlayerStats.wsdl"));

        final String gatewayWsdlUrl = "http://localhost:8080/3828382?wsdl";
        final String gatewayURL = "http://localhost:8080/3828382";
        EndpointPair endpointPair = new EndpointPair(gatewayURL, gatewayWsdlUrl);

        final String gatewayURHttps = "https://localhost:8080/3828382";
        EndpointPair endpointPairHttps = new EndpointPair(gatewayURHttps, gatewayWsdlUrl);

        final int serviceOid = 3828382;
        final String businessKey = "uddi:uddi_business_key";

        UDDIClient uddiClient = new TestUddiClient(true);

        BusinessServicePublisher servicePublisher = new BusinessServicePublisher(wsdl, serviceOid, uddiClient);

        final Pair<Set<String>, Set<UDDIBusinessService>> pair =
                servicePublisher.publishServicesToUDDIRegistry(businessKey,
                        Collections.<String>emptySet(), false, null, Arrays.asList(endpointPair, endpointPairHttps));

        Assert.assertNotNull("pair should not be null", pair);
        Assert.assertTrue("No services to delete should be found", pair.left.isEmpty());
        Assert.assertEquals("Incorrect number of services published", 1, pair.right.size());
    }

    @Test
    public void testImportServiceLocalNameClash() throws Exception{
        final Wsdl wsdl = Wsdl.newInstance(null, WsdlTUDDIModelConverterTest.getWsdlReader( "com/l7tech/uddi/SpaceOrderofBattleServiceLocalNameClash_Parent.wsdl" ));
        final String gatewayWsdlUrl = "http://localhost:8080/3828382?wsdl";
        final String gatewayURL = "http://localhost:8080/3828382";
        final EndpointPair endpointPair = new EndpointPair(gatewayURL, gatewayWsdlUrl);


        final int serviceOid = 3828382;
        final String businessKey = "uddi:uddi_business_key";
        WsdlToUDDIModelConverter wsdlToUDDIModelConverter = new WsdlToUDDIModelConverter(wsdl, businessKey);
        wsdlToUDDIModelConverter.convertWsdlToUDDIModel(Arrays.asList(endpointPair), "Layer7", Long.toString(serviceOid));

        UDDIClient uddiClient = new TestUddiClient(true);

        BusinessServicePublisher servicePublisher = new BusinessServicePublisher(wsdl, serviceOid, uddiClient);

        final Pair<Set<String>, Set<UDDIBusinessService>> pair =
                servicePublisher.publishServicesToUDDIRegistry(businessKey,
                        Collections.<String>emptySet(), false, null, Arrays.asList(endpointPair));

        Assert.assertNotNull("pair should not be null", pair);
        Assert.assertTrue("No services to delete should be found", pair.left.isEmpty());
        Assert.assertEquals("Incorrect number of services published", 2, pair.right.size());

        final Set<String> stringSet = new HashSet<String>();
        stringSet.addAll(Arrays.asList("service key"));
        final Pair<Set<String>, Set<UDDIBusinessService>> deletePair =
                servicePublisher.publishServicesToUDDIRegistry(businessKey, stringSet, false, null, Arrays.asList(endpointPair));

        Assert.assertNotNull("pair should not be null", deletePair);
        Assert.assertEquals("One service to delete should be found", 1, deletePair.left.size());
        Assert.assertEquals("Invalid service key of service to delete found", "service key", deletePair.left.iterator().next());
        Assert.assertEquals("Incorrect number of services published", 2, deletePair.right.size());

        Set<String> namespaces = new HashSet<String>();
        final String parentNs = "http://SOB.IPD.LMCO.SERVICES/";
        namespaces.add(parentNs);
        final String childNs = "http://SOB.IPD.LMCO/ExternalWSDL";
        namespaces.add(childNs);
        boolean foundParentNs = false;
        boolean foundChildNs = false;
        for(UDDIBusinessService service: deletePair.right){
            Assert.assertNotNull(service.getWsdlServiceNamespace());
            if(service.getWsdlServiceNamespace().equals(parentNs)) foundParentNs = true;
            if(service.getWsdlServiceNamespace().equals(childNs)) foundChildNs = true;
        }

        Assert.assertTrue("Parent namespace not found", foundParentNs);
        Assert.assertTrue("Child namespace not found", foundChildNs);
    }

    /**
     * Test that the created bindingTemplate is correctly constructed, itself, it's references to tModels, and the
     * tModels that were also created
     *
     * @throws Exception
     */
    @Test
    public void testPublishEndPoint() throws Exception {
        Wsdl wsdl = Wsdl.newInstance(null, WsdlTUDDIModelConverterTest.getWsdlReader("com/l7tech/uddi/artistregistry.wsdl"));

        final String gatewayWsdlUrl = "http://localhost:8080/3828382?wsdl";
        final String gatewayURL = "http://localhost:8080/3828382";
        final EndpointPair endpointPair = new EndpointPair(gatewayURL, gatewayWsdlUrl);

        final int serviceOid = 3828382;

        //Test is now setup

        UDDIClient uddiClient = new TestUddiClient();
        BusinessServicePublisher servicePublisher = new BusinessServicePublisher(wsdl, serviceOid, uddiClient);

        Pair<List<BindingTemplate>, List<TModel>> bindingAndModels =
                servicePublisher.publishEndPointToExistingService(
                        "serviceKey",
                        "YessoTestWebServicesPort",
                        "YessoTestWebServicesPortBinding",
                        "http://samples.soamoa.yesso.eu/", 
                        Arrays.asList(endpointPair),
                        null,
                        null,
                        false, null, null);


        testBindingTemplate(gatewayWsdlUrl, gatewayURL, "serviceKey", new Pair<BindingTemplate, List<TModel>>(bindingAndModels.left.get(0), bindingAndModels.right));
    }

    @BugNumber(8426)
    @BugId("FR-233")
    @Test
    public void testFirstPublishOfGifEndPoint() throws Exception {
        Wsdl wsdl = Wsdl.newInstance(null, WsdlTUDDIModelConverterTest.getWsdlReader("com/l7tech/uddi/artistregistry.wsdl"));

        final String gatewayWsdlUrl = "http://ssghost:8080/3828382?wsdl";
        final String gatewayURL = "http://ssghost:8080/3828382";
        final EndpointPair endpointPair = new EndpointPair(gatewayURL, gatewayWsdlUrl);

        InputStream stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/ArtistRegistryService.xml");
        final BusinessService businessService = JAXB.unmarshal(stream, BusinessService.class);
        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/ArtistRegistryServiceTModel_Binding.xml");
        final TModel bindingTModel = JAXB.unmarshal(stream, TModel.class);
        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/ArtistRegistryServiceTModel_PortType.xml");
        final TModel portTypeTModel = JAXB.unmarshal(stream, TModel.class);

        final int serviceOid = 3828382;
        TestUddiClient uddiClient = new TestUddiClient(businessService, Arrays.asList(bindingTModel, portTypeTModel));
        BusinessServicePublisher servicePublisher = new BusinessServicePublisher(wsdl, serviceOid, uddiClient);

        final String originalBindingKey = "uddi:ad36b520-d229-11df-a331-7bda57c0a325";
        final Pair<String, String> pair = servicePublisher.publishBindingTemplateGif(
                "uddi:ad3618e0-d229-11df-a331-7bda57c0a325",
                "YessoTestWebServicesPort",
                "YessoTestWebServicesPortBinding",
                "http://samples.soamoa.yesso.eu/",
                endpointPair,
                null,
                originalBindingKey,
                "Layer7 WSMS", null, null);

        Assert.assertNotNull("Pair should not be null", pair);
        Assert.assertEquals("Incorrect bindingKey returned", originalBindingKey, pair.left);
        //the new bindingKey for the functional endpoint will have been randomely generated by UDDI. Just check it's not null
        Assert.assertNotNull("The functional bindingKey should not be null", pair.right);

        //the service now has two endpoints.
        final List<BindingTemplate> publishedTemplates = uddiClient.getPublishedBindingTemplates();
        Assert.assertEquals("Two bindingTemplates should have been published - the original and the proxy", 2, publishedTemplates.size());

        BindingTemplate functionalTemplate = null;
        BindingTemplate proxyTemplate = null;
        
        for (BindingTemplate publishedTemplate : publishedTemplates) {
            //is this the proxy endpoint?
            if(publishedTemplate.getBindingKey().equals(originalBindingKey)){
                proxyTemplate = publishedTemplate;
            } else {
                functionalTemplate = publishedTemplate;//it got a new key
            }
        }

        Assert.assertNotNull("Functional endpoint should have been found", functionalTemplate);
        Assert.assertNotNull("Proxy endpoint should have been found", proxyTemplate);

        //the proxy endpoint should have the binding key of the original endpoint
        Assert.assertEquals("Proxy endpoint should have the original endpoints bindingKey", originalBindingKey, proxyTemplate.getBindingKey());

        //Validate all proxy meta data
        final List<KeyedReference> proxyReferences = proxyTemplate.getCategoryBag().getKeyedReference();
        validateProxyGifReferences(endpointPair, proxyReferences, 5);

        //the functional endPoint should have a new key
        Assert.assertNotNull("Functional endpoint should have a key", functionalTemplate.getBindingKey());
        Assert.assertNotSame("Functional endpoint should have a new key", originalBindingKey, functionalTemplate.getBindingKey());

        //the correct meta data should have been added to the proxy
        final List<KeyedReference> functionalReferences = functionalTemplate.getCategoryBag().getKeyedReference();
        validateFunctionalReferences(originalBindingKey, functionalReferences);

        //end test
        //code to create the UDDIObjects used in the test 'testUpdatePublishOfGifEndPoint' below
//        businessService.getBindingTemplates().getBindingTemplate().clear();
//        businessService.getBindingTemplates().getBindingTemplate().addAll(Arrays.asList(functionalTemplate, proxyTemplate));
//
//        JAXB.marshal(businessService, new File("/home/darmstrong/ideaprojects/UneasyRooster/modules/uddi/src/test/resources/com/l7tech/uddi/ArtistRegistryService_GIF.xml"));
//
//        final List<TModel> publishedTModels = uddiClient.getPublishedTModels();
//        int i = 0;
//        for (TModel publishedTModel : publishedTModels) {
//            JAXB.marshal(publishedTModel, new File("/home/darmstrong/ideaprojects/UneasyRooster/modules/uddi/src/test/resources/com/l7tech/uddi/ArtistRegistryServiceTModel_GIF_" + i + ".xml"));
//            i++;
//        }
    }

    @BugNumber(8426)
    @BugId("FR-233")
    @Test
    public void testFirstPublishOfGifEndPoint_WithMetaData() throws Exception {
        Wsdl wsdl = Wsdl.newInstance(null, WsdlTUDDIModelConverterTest.getWsdlReader("com/l7tech/uddi/artistregistry.wsdl"));

        final String gatewayWsdlUrl = "http://ssghost:8080/3828382?wsdl";
        final String gatewayURL = "http://ssghost:8080/3828382";
        final EndpointPair endpointPair = new EndpointPair(gatewayURL, gatewayWsdlUrl);

        InputStream stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/ArtistRegistryService.xml");
        final BusinessService businessService = JAXB.unmarshal(stream, BusinessService.class);
        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/ArtistRegistryServiceTModel_Binding.xml");
        final TModel bindingTModel = JAXB.unmarshal(stream, TModel.class);
        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/ArtistRegistryServiceTModel_PortType.xml");
        final TModel portTypeTModel = JAXB.unmarshal(stream, TModel.class);

        final int serviceOid = 3828382;
        TestUddiClient uddiClient = new TestUddiClient(businessService, Arrays.asList(bindingTModel, portTypeTModel));
        BusinessServicePublisher servicePublisher = new BusinessServicePublisher(wsdl, serviceOid, uddiClient);

        final Set<UDDIKeyedReference> configRefs = new HashSet<UDDIKeyedReference>();
        final UDDIKeyedReference metaData = new UDDIKeyedReference("key1", "name1", "value1");
        configRefs.add(metaData);

        final String originalBindingKey = "uddi:ad36b520-d229-11df-a331-7bda57c0a325";
        final Pair<String, String> pair = servicePublisher.publishBindingTemplateGif(
                "uddi:ad3618e0-d229-11df-a331-7bda57c0a325",
                "YessoTestWebServicesPort",
                "YessoTestWebServicesPortBinding",
                "http://samples.soamoa.yesso.eu/",
                endpointPair,
                null,
                originalBindingKey,
                "Layer7 WSMS",
                configRefs,
                null);

        Assert.assertNotNull("Pair should not be null", pair);
        Assert.assertEquals("Incorrect bindingKey returned", originalBindingKey, pair.left);
        //the new bindingKey for the functional endpoint will have been randomely generated by UDDI. Just check it's not null
        Assert.assertNotNull("The functional bindingKey should not be null", pair.right);

        //the service now has two endpoints.
        final List<BindingTemplate> publishedTemplates = uddiClient.getPublishedBindingTemplates();
        Assert.assertEquals("Two bindingTemplates should have been published - the original and the proxy", 2, publishedTemplates.size());

        BindingTemplate proxyTemplate = null;

        for (BindingTemplate publishedTemplate : publishedTemplates) {
            //is this the proxy endpoint?
            if(publishedTemplate.getBindingKey().equals(originalBindingKey)){
                proxyTemplate = publishedTemplate;
            } 
        }

        Assert.assertNotNull("Proxy endpoint should have been found", proxyTemplate);
        //Validate all proxy meta data
        final List<KeyedReference> proxyReferences = proxyTemplate.getCategoryBag().getKeyedReference();
        final Set<UDDIKeyedReference> beans = convertToBeans(proxyReferences);
        Assert.assertTrue("Reference should have been found", beans.contains(metaData));
    }

    private Set<UDDIKeyedReference> convertToBeans(final List<KeyedReference> refs){
        final Set<UDDIKeyedReference> returnSet = new HashSet<UDDIKeyedReference>();
        for (KeyedReference ref : refs) {
            returnSet.add(new UDDIKeyedReference(ref.getTModelKey(), ref.getKeyName(), ref.getKeyValue()));
        }
        return returnSet;
    }

    private void validateFunctionalReferences(String proxyBindingKey, List<KeyedReference> functionalReferences) {
        //build map of references
        final Map<String, KeyedReference> functionalRefMap = new HashMap<String, KeyedReference>();
        for (KeyedReference functionalReference : functionalReferences) {
            functionalRefMap.put(functionalReference.getTModelKey(), functionalReference);
        }

        //the correct meta data should have been added to the functional endpoint
        final KeyedReference functMgmtTypeRef = functionalRefMap.get(BusinessServicePublisher.UDDI_SYSTINET_COM_MANAGEMENT_TYPE);
        Assert.assertNotNull("Missing reference", functMgmtTypeRef);
        Assert.assertEquals("Incorrect value", BusinessServicePublisher.FUNCTIONAL_ENDPOINT, functMgmtTypeRef.getKeyValue());
        Assert.assertEquals("Incorrect name", "Management entity type", functMgmtTypeRef.getKeyName());

        final KeyedReference proxyRef = functionalRefMap.get(BusinessServicePublisher.UDDI_SYSTINET_COM_MANAGEMENT_PROXY_REFERENCE);
        Assert.assertNotNull("Missing reference", proxyRef);
        Assert.assertEquals("Incorrect value", proxyBindingKey, proxyRef.getKeyValue());
        Assert.assertEquals("Incorrect name", "Proxy reference", proxyRef.getKeyName());

        final KeyedReference typeRef = functionalRefMap.get("uddi:uddi.org:wsdl:types");
        Assert.assertNotNull("Missing reference", typeRef);
        Assert.assertEquals("Incorrect value", "port", typeRef.getKeyValue());
        Assert.assertEquals("Incorrect name", "uddi.org:wsdl:types", typeRef.getKeyName());
    }

    private void validateProxyGifReferences(EndpointPair endpointPair, List<KeyedReference> proxyReferences, int numExpected) {
        Assert.assertEquals("Incorrect number of keyedReferences found", numExpected, proxyReferences.size());
        //build up map of references
        final Map<String, KeyedReference> proxyRefMap = new HashMap<String, KeyedReference>();
        for (KeyedReference proxyReference : proxyReferences) {
            proxyRefMap.put(proxyReference.getTModelKey(), proxyReference);
        }

        final KeyedReference mgmtTypeRef = proxyRefMap.get(BusinessServicePublisher.UDDI_SYSTINET_COM_MANAGEMENT_TYPE);
        Assert.assertNotNull("Missing reference", mgmtTypeRef);
        Assert.assertEquals("Incorrect value", BusinessServicePublisher.MANAGED_ENDPOINT, mgmtTypeRef.getKeyValue());
        Assert.assertEquals("Incorrect name", "Management entity type", mgmtTypeRef.getKeyName());

        final KeyedReference mgmtSysRef = proxyRefMap.get(BusinessServicePublisher.UDDI_SYSTINET_COM_MANAGEMENT_SYSTEM);
        Assert.assertNotNull("Missing reference", mgmtSysRef);
        Assert.assertEquals("Incorrect value", "Layer7 WSMS", mgmtSysRef.getKeyValue());
        Assert.assertEquals("Incorrect name", "Management System", mgmtSysRef.getKeyName());

        final KeyedReference mgmtStateRef = proxyRefMap.get(BusinessServicePublisher.UDDI_SYSTINET_COM_MANAGEMENT_STATE);
        Assert.assertNotNull("Missing reference", mgmtStateRef);
        Assert.assertEquals("Incorrect value", BusinessServicePublisher.SYSTINET_STATE_MANAGED, mgmtStateRef.getKeyValue());
        Assert.assertEquals("Incorrect name", "Governance state", mgmtStateRef.getKeyName());

        final KeyedReference endPointRef = proxyRefMap.get(BusinessServicePublisher.UDDI_SYSTINET_COM_MANAGEMENT_URL);
        Assert.assertNotNull("Missing reference", endPointRef);
        Assert.assertEquals("Incorrect value", endpointPair.getEndPointUrl(), endPointRef.getKeyValue());
        Assert.assertEquals("Incorrect name", "URL from AccessPoint", endPointRef.getKeyName());

        //this was copied from the functional reference on the first publish
        final KeyedReference typeRef = proxyRefMap.get("uddi:uddi.org:wsdl:types");
        Assert.assertNotNull("Missing reference", typeRef);
        Assert.assertEquals("Incorrect value", "port", typeRef.getKeyValue());
        Assert.assertEquals("Incorrect name", "uddi.org:wsdl:types", typeRef.getKeyName());
    }

    @BugNumber(8426)
    @BugId("FR-233")
    @Test
    public void testDeleteOfGifEndpoint() throws Exception{
        Wsdl wsdl = Wsdl.newInstance(null, WsdlTUDDIModelConverterTest.getWsdlReader("com/l7tech/uddi/artistregistry.wsdl"));

        final int serviceOid = 3828382;
        final String gatewayWsdlUrl = "http://ssghost:8080/" + serviceOid + "?wsdl";
        final String gatewayURL = "http://ssghost:8080/" + serviceOid;
        final EndpointPair endpointPair = new EndpointPair(gatewayURL, gatewayWsdlUrl);

        InputStream stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/ArtistRegistryService_GIF.xml");
        final BusinessService businessService = JAXB.unmarshal(stream, BusinessService.class);
        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/ArtistRegistryServiceTModel_Binding_GIF.xml");
        final TModel bindingTModelGif = JAXB.unmarshal(stream, TModel.class);
        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/ArtistRegistryServiceTModel_PortType_GIF.xml");
        final TModel portTypeTModelGif = JAXB.unmarshal(stream, TModel.class);

        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/ArtistRegistryServiceTModel_Binding.xml");
        final TModel bindingTModel = JAXB.unmarshal(stream, TModel.class);
        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/ArtistRegistryServiceTModel_PortType.xml");
        final TModel portTypeTModel = JAXB.unmarshal(stream, TModel.class);


        TestUddiClient uddiClient = new TestUddiClient(businessService, Arrays.asList(bindingTModelGif, portTypeTModelGif, bindingTModel, portTypeTModel));
        BusinessServicePublisher servicePublisher = new BusinessServicePublisher(wsdl, serviceOid, uddiClient);

        final String proxyBindingKey = "uddi:ad36b520-d229-11df-a331-7bda57c0a325";
        final String functionalBindingKey = "uddi:c70283ce-c113-493f-b576-957a94dd1e70";

        servicePublisher.deleteGatewayGifBindingTemplates("uddi:ad3618e0-d229-11df-a331-7bda57c0a325",
                proxyBindingKey, functionalBindingKey);

        //validate what happened via the UDDIClient
        final List<String> deletedTemplatesOnly = uddiClient.getDeletedBindingTemplatesOnly();
        Assert.assertEquals("Incorrect number of bindingTemplates only deleted", 1, deletedTemplatesOnly.size());

        final String deletedTemplate = deletedTemplatesOnly.get(0);
        Assert.assertEquals("Incorrect bindingTemplate deleted", functionalBindingKey, deletedTemplate);

        //validate the BindingTemplate proxy was converted into the functional template via a publish
        final List<BindingTemplate> publishedTemplates = uddiClient.getPublishedBindingTemplates();
        Assert.assertEquals("Only 1 template should have been published", 1, publishedTemplates.size());

        final BindingTemplate revertedFuncTemplate = publishedTemplates.get(0);
        Assert.assertEquals("Reverted functional endpoint has the incorrect bindingKey", proxyBindingKey, revertedFuncTemplate.getBindingKey());

        final List<KeyedReference> references = revertedFuncTemplate.getCategoryBag().getKeyedReference();
        final Set<String> tModelKeysReferenced = new HashSet<String>();
        for (KeyedReference reference : references) {
            tModelKeysReferenced.add(reference.getTModelKey());
        }

        Assert.assertFalse("GIF reference D.1 should have been removed", tModelKeysReferenced.contains(BusinessServicePublisher.UDDI_SYSTINET_COM_MANAGEMENT_TYPE));
        Assert.assertFalse("GIF reference D.2 should have been removed", tModelKeysReferenced.contains(BusinessServicePublisher.UDDI_SYSTINET_COM_MANAGEMENT_PROXY_REFERENCE));

        //confirm the tModels referenced by the deleted GIF proxy endpoint have been removed also
        Assert.assertEquals("Incorrect number of tModels deleted", 2, uddiClient.getNumTModelsDeleted());

        final List<String> deletedTModelKeys = uddiClient.getDeletedTModelKeys();
        Assert.assertTrue("tModel to delete was not deleted", deletedTModelKeys.contains("uddi:ef16ff47-c66e-4cd1-bb0c-f84846c8afd4"));
        Assert.assertTrue("tModel to delete was not deleted", deletedTModelKeys.contains("uddi:5b3ffd40-b8ad-4965-9d51-292d6a966476"));
    }

    /**
     * Tests updating a BusinessService's proxy bindingTemplate once it has been published following GIF.
     * The only thing that needs to change at all in an update is the gateway's URLs and the URL refernece on the proxy.
     * The functional bindingTemplate should not be published during the update as nothing on it has changed.
     * @throws Exception
     */
    @BugNumber(8426)
    @BugId("FR-233")
    @Test
    public void testUpdatePublishOfGifEndPoint() throws Exception {
        Wsdl wsdl = Wsdl.newInstance(null, WsdlTUDDIModelConverterTest.getWsdlReader("com/l7tech/uddi/artistregistry.wsdl"));

        final String gatewayWsdlUrl = "http://ssghost:8081/3828382?wsdl"; //this is a different port than what is currently published (in the test objects)
        final String gatewayURL = "http://ssghost:8081/3828382"; //this is a different port than what is currently published (in the test objects)
        final EndpointPair endpointPair = new EndpointPair(gatewayURL, gatewayWsdlUrl);

        InputStream stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/ArtistRegistryService_GIF.xml");
        final BusinessService businessService = JAXB.unmarshal(stream, BusinessService.class);
        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/ArtistRegistryServiceTModel_Binding_GIF.xml");
        final TModel bindingTModelGif = JAXB.unmarshal(stream, TModel.class);
        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/ArtistRegistryServiceTModel_PortType_GIF.xml");
        final TModel portTypeTModelGif = JAXB.unmarshal(stream, TModel.class);

        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/ArtistRegistryServiceTModel_Binding.xml");
        final TModel bindingTModel = JAXB.unmarshal(stream, TModel.class);
        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/ArtistRegistryServiceTModel_PortType.xml");
        final TModel portTypeTModel = JAXB.unmarshal(stream, TModel.class);

        final int serviceOid = 3828382;
        TestUddiClient uddiClient = new TestUddiClient(businessService, Arrays.asList(bindingTModelGif, portTypeTModelGif, bindingTModel, portTypeTModel));
        BusinessServicePublisher servicePublisher = new BusinessServicePublisher(wsdl, serviceOid, uddiClient);

        final String proxyBindingKey = "uddi:ad36b520-d229-11df-a331-7bda57c0a325";
        final String functionalBindingKey = "uddi:c70283ce-c113-493f-b576-957a94dd1e70";
        final Pair<String, String> pair = servicePublisher.publishBindingTemplateGif(
                "uddi:ad3618e0-d229-11df-a331-7bda57c0a325",
                "YessoTestWebServicesPort",
                "YessoTestWebServicesPortBinding",
                "http://samples.soamoa.yesso.eu/",
                endpointPair,
                proxyBindingKey,
                functionalBindingKey,
                "Layer7 WSMS", null, null);

        Assert.assertNotNull("Pair should not be null", pair);
        Assert.assertEquals("Incorrect proxy bindingKey returned", proxyBindingKey, pair.left);
        Assert.assertEquals("Incorrect functional bindingKey returned", functionalBindingKey, pair.right);

        //test functional endpoint was not published - no updates were necessary
        final List<BindingTemplate> publishedTemplates = uddiClient.getPublishedBindingTemplates();
        Assert.assertEquals("Only 1 template should have been published", 1, publishedTemplates.size());

        final BindingTemplate proxyTemplate = publishedTemplates.get(0);
        Assert.assertEquals("Incorrect template published", proxyBindingKey, proxyTemplate.getBindingKey());

        //test that there is only one url reference on the proxy - the old one should have been removed
        final List<KeyedReference> proxyReferences = proxyTemplate.getCategoryBag().getKeyedReference();
        Assert.assertEquals("Incorrect number of proxy references found", 7, proxyReferences.size());

        validateProxyGifReferences(endpointPair, proxyReferences, 7);

        int numUrlRefsFound = 0;
        KeyedReference urlRef = new KeyedReference();
        for (KeyedReference proxyReference : proxyReferences) {
            if(proxyReference.getTModelKey().equals(BusinessServicePublisher.UDDI_SYSTINET_COM_MANAGEMENT_URL)){
                numUrlRefsFound++;
                urlRef = proxyReference;
            }
        }

        Assert.assertEquals("Only 1 URL ref should have been found", 1, numUrlRefsFound);

        //validate the value of the URL reference
        Assert.assertEquals("Incorrect URL found in keyed reference", endpointPair.getEndPointUrl(), urlRef.getKeyValue());

        //test the URLs in the bindingTemplates and tModels
        Assert.assertEquals("proxy template has the wrong endpoint URL", endpointPair.getEndPointUrl(), proxyTemplate.getAccessPoint().getValue());

        final List<TModel> publishedTModels = uddiClient.getPublishedTModels();
        //build map of models
        final Map<String, TModel> pubKeyToModel = new HashMap<String, TModel>();
        for (TModel publishedTModel : publishedTModels) {
            pubKeyToModel.put(publishedTModel.getTModelKey(), publishedTModel);
        }

        //test wsdl interface URLs
        final List<TModelInstanceInfo> instanceInfos = proxyTemplate.getTModelInstanceDetails().getTModelInstanceInfo();
        int numWsdlUrlsVerified = 0;
        for (TModelInstanceInfo instanceInfo : instanceInfos) {
            final TModel model = pubKeyToModel.get(instanceInfo.getTModelKey());
            final List<OverviewDoc> overviewDocs = model.getOverviewDoc();
            for (OverviewDoc overviewDoc : overviewDocs) {
                final OverviewURL overviewUrl = overviewDoc.getOverviewURL();
                if(overviewUrl.getUseType().equals("wsdlInterface")){
                    Assert.assertEquals("Incorrect WSDL URL found", overviewUrl.getValue(), endpointPair.getWsdlUrl());
                    numWsdlUrlsVerified++;
                }
            }
        }
        //validate all wsdl urls were validated
        Assert.assertEquals("Not all WSDL URLs were verified", 2 ,numWsdlUrlsVerified);
    }

    /**
     * Tests updating a BusinessService's proxy bindingTemplate once it has been published following GIF.
     * The only thing that needs to change at all in an update is the gateway's URLs and the URL reference on the proxy.
     * The functional bindingTemplate should not be published during the update as nothing on it has changed.
     * @throws Exception
     */
    @BugNumber(8426)
    @BugId("FR-233")
    @Test
    public void testUpdatePublishOfGifEndPoint_WithMeta() throws Exception {
        Wsdl wsdl = Wsdl.newInstance(null, WsdlTUDDIModelConverterTest.getWsdlReader("com/l7tech/uddi/artistregistry.wsdl"));

        final String gatewayWsdlUrl = "http://ssghost:8081/3828382?wsdl"; //this is a different port than what is currently published (in the test objects)
        final String gatewayURL = "http://ssghost:8081/3828382"; //this is a different port than what is currently published (in the test objects)
        final EndpointPair endpointPair = new EndpointPair(gatewayURL, gatewayWsdlUrl);

        InputStream stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/ArtistRegistryService_GIF.xml");
        final BusinessService businessService = JAXB.unmarshal(stream, BusinessService.class);
        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/ArtistRegistryServiceTModel_Binding_GIF.xml");
        final TModel bindingTModelGif = JAXB.unmarshal(stream, TModel.class);
        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/ArtistRegistryServiceTModel_PortType_GIF.xml");
        final TModel portTypeTModelGif = JAXB.unmarshal(stream, TModel.class);

        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/ArtistRegistryServiceTModel_Binding.xml");
        final TModel bindingTModel = JAXB.unmarshal(stream, TModel.class);
        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/ArtistRegistryServiceTModel_PortType.xml");
        final TModel portTypeTModel = JAXB.unmarshal(stream, TModel.class);

        final int serviceOid = 3828382;
        TestUddiClient uddiClient = new TestUddiClient(businessService, Arrays.asList(bindingTModelGif, portTypeTModelGif, bindingTModel, portTypeTModel));
        BusinessServicePublisher servicePublisher = new BusinessServicePublisher(wsdl, serviceOid, uddiClient);

        final String proxyBindingKey = "uddi:ad36b520-d229-11df-a331-7bda57c0a325";
        final String functionalBindingKey = "uddi:c70283ce-c113-493f-b576-957a94dd1e70";
        final Set<UDDIKeyedReference> configRefs = new HashSet<UDDIKeyedReference>();
        final UDDIKeyedReference metaData = new UDDIKeyedReference("uddi:systinet.com:management:server-reference", 
                "WSMS Business Service UPDATED", "uddi:key_of_wsms_bs UPDATED");
        configRefs.add(metaData);

        final Set<UDDIKeyedReference> runtimeRefs = new HashSet<UDDIKeyedReference>();
        final UDDIKeyedReference runtimeMetaData = new UDDIKeyedReference("uddi:systinet.com:management:server-reference",
                "WSMS Business Service", "uddi:key_of_wsms_bs");
        runtimeRefs.add(runtimeMetaData);

        servicePublisher.publishBindingTemplateGif(
                "uddi:ad3618e0-d229-11df-a331-7bda57c0a325",
                "YessoTestWebServicesPort",
                "YessoTestWebServicesPortBinding",
                "http://samples.soamoa.yesso.eu/",
                endpointPair,
                proxyBindingKey,
                functionalBindingKey,
                "Layer7 WSMS",
                configRefs,
                runtimeRefs);

        //test functional endpoint was not published - no updates were necessary
        final List<BindingTemplate> publishedTemplates = uddiClient.getPublishedBindingTemplates();
        final BindingTemplate proxyTemplate = publishedTemplates.get(0);
        Assert.assertEquals("Incorrect template published", proxyBindingKey, proxyTemplate.getBindingKey());

        //test that there is only one url reference on the proxy - the old one should have been removed
        final List<KeyedReference> proxyReferences = proxyTemplate.getCategoryBag().getKeyedReference();
        Assert.assertEquals("Incorrect number of proxy references found", 7, proxyReferences.size());

        //validate the keyed reference was updated
        final Set<UDDIKeyedReference> allRefs = convertToBeans(proxyReferences);
        Assert.assertTrue("Reference should have been found", allRefs.contains(metaData));
        Assert.assertTrue("Old reference should not be found", !allRefs.contains(runtimeMetaData));
    }

    /**
     * Tests that changing the keyName in a piece of meta data correctly updates in UDDI.
     * @throws Exception
     */
    @BugNumber(9462)
    @BugId("SSM-3031")
    @Test
    public void testUpdatePublishOfGifEndPoint_WithMetaUserKeyNameChange() throws Exception {
        Wsdl wsdl = Wsdl.newInstance(null, WsdlTUDDIModelConverterTest.getWsdlReader("com/l7tech/uddi/artistregistry.wsdl"));

        final String gatewayWsdlUrl = "http://ssghost:8081/3828382?wsdl"; //this is a different port than what is currently published (in the test objects)
        final String gatewayURL = "http://ssghost:8081/3828382"; //this is a different port than what is currently published (in the test objects)
        final EndpointPair endpointPair = new EndpointPair(gatewayURL, gatewayWsdlUrl);

        InputStream stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/ArtistRegistryService_GIF.xml");
        final BusinessService businessService = JAXB.unmarshal(stream, BusinessService.class);
        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/ArtistRegistryServiceTModel_Binding_GIF.xml");
        final TModel bindingTModelGif = JAXB.unmarshal(stream, TModel.class);
        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/ArtistRegistryServiceTModel_PortType_GIF.xml");
        final TModel portTypeTModelGif = JAXB.unmarshal(stream, TModel.class);

        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/ArtistRegistryServiceTModel_Binding.xml");
        final TModel bindingTModel = JAXB.unmarshal(stream, TModel.class);
        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/ArtistRegistryServiceTModel_PortType.xml");
        final TModel portTypeTModel = JAXB.unmarshal(stream, TModel.class);

        final int serviceOid = 3828382;
        TestUddiClient uddiClient = new TestUddiClient(businessService, Arrays.asList(bindingTModelGif, portTypeTModelGif, bindingTModel, portTypeTModel));
        BusinessServicePublisher servicePublisher = new BusinessServicePublisher(wsdl, serviceOid, uddiClient);

        final String proxyBindingKey = "uddi:ad36b520-d229-11df-a331-7bda57c0a325";
        final String functionalBindingKey = "uddi:c70283ce-c113-493f-b576-957a94dd1e70";
        final Set<UDDIKeyedReference> configRefs = new HashSet<UDDIKeyedReference>();
        final UDDIKeyedReference metaData = new UDDIKeyedReference("uddi:systinet.com:management:server-reference",
                "WSMS Business Service UPDATED", "uddi:key_of_wsms_bs");
        configRefs.add(metaData);

        final Set<UDDIKeyedReference> runtimeRefs = new HashSet<UDDIKeyedReference>();
        final UDDIKeyedReference runtimeMetaData = new UDDIKeyedReference("uddi:systinet.com:management:server-reference",
                "WSMS Business Service", "uddi:key_of_wsms_bs");
        runtimeRefs.add(runtimeMetaData);

        servicePublisher.publishBindingTemplateGif(
                "uddi:ad3618e0-d229-11df-a331-7bda57c0a325",
                "YessoTestWebServicesPort",
                "YessoTestWebServicesPortBinding",
                "http://samples.soamoa.yesso.eu/",
                endpointPair,
                proxyBindingKey,
                functionalBindingKey,
                "Layer7 WSMS",
                configRefs,
                runtimeRefs);

        //test functional endpoint was not published - no updates were necessary
        final List<BindingTemplate> publishedTemplates = uddiClient.getPublishedBindingTemplates();
        final BindingTemplate proxyTemplate = publishedTemplates.get(0);
        Assert.assertEquals("Incorrect template published", proxyBindingKey, proxyTemplate.getBindingKey());

        //test that there is only one url reference on the proxy - the old one should have been removed
        final List<KeyedReference> proxyReferences = proxyTemplate.getCategoryBag().getKeyedReference();
        Assert.assertEquals("Incorrect number of proxy references found", 7, proxyReferences.size());

        //validate the keyed reference was updated
        final Set<UDDIKeyedReference> allRefs = convertToBeans(proxyReferences);
        Assert.assertTrue("Reference should have been found", allRefs.contains(metaData));
        Assert.assertEquals("Updated keyName should have been persisted", "WSMS Business Service UPDATED", metaData.getKeyName());
        Assert.assertTrue("Old reference should be found", allRefs.contains(runtimeMetaData));
    }

    /**
     * UDDI has a keyedReference added with a null value. Gateway then publishes the same with a key value.
     * Uddi value should not be updated as the value from UDDI takes precedence.
     * @throws Exception
     */
    @BugNumber(9462)
    @BugId("SSM-3031")
    @Test
    public void testUpdatePublishOfGifEndPoint_NullUddiValue() throws Exception {
        Wsdl wsdl = Wsdl.newInstance(null, WsdlTUDDIModelConverterTest.getWsdlReader("com/l7tech/uddi/artistregistry.wsdl"));

        final String gatewayWsdlUrl = "http://ssghost:8081/3828382?wsdl"; //this is a different port than what is currently published (in the test objects)
        final String gatewayURL = "http://ssghost:8081/3828382"; //this is a different port than what is currently published (in the test objects)
        final EndpointPair endpointPair = new EndpointPair(gatewayURL, gatewayWsdlUrl);

        InputStream stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/ArtistRegistryService_GIF.xml");
        final BusinessService businessService = JAXB.unmarshal(stream, BusinessService.class);
        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/ArtistRegistryServiceTModel_Binding_GIF.xml");
        final TModel bindingTModelGif = JAXB.unmarshal(stream, TModel.class);
        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/ArtistRegistryServiceTModel_PortType_GIF.xml");
        final TModel portTypeTModelGif = JAXB.unmarshal(stream, TModel.class);

        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/ArtistRegistryServiceTModel_Binding.xml");
        final TModel bindingTModel = JAXB.unmarshal(stream, TModel.class);
        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/ArtistRegistryServiceTModel_PortType.xml");
        final TModel portTypeTModel = JAXB.unmarshal(stream, TModel.class);

        final int serviceOid = 3828382;
        TestUddiClient uddiClient = new TestUddiClient(businessService, Arrays.asList(bindingTModelGif, portTypeTModelGif, bindingTModel, portTypeTModel));
        BusinessServicePublisher servicePublisher = new BusinessServicePublisher(wsdl, serviceOid, uddiClient);

        final String proxyBindingKey = "uddi:ad36b520-d229-11df-a331-7bda57c0a325";
        final String functionalBindingKey = "uddi:c70283ce-c113-493f-b576-957a94dd1e70";
        final Set<UDDIKeyedReference> configRefs = new HashSet<UDDIKeyedReference>();
        final UDDIKeyedReference metaData = new UDDIKeyedReference("uddi:297aaa47-2de3-4454-a04a-cf38e889d0c4",
                "client", "value");
        configRefs.add(metaData);

        final Set<UDDIKeyedReference> runtimeRefs = new HashSet<UDDIKeyedReference>();
        final UDDIKeyedReference runtimeMetaData = new UDDIKeyedReference("uddi:systinet.com:management:server-reference",
                "client", "value");
        runtimeRefs.add(runtimeMetaData);

        servicePublisher.publishBindingTemplateGif(
                "uddi:ad3618e0-d229-11df-a331-7bda57c0a325",
                "YessoTestWebServicesPort",
                "YessoTestWebServicesPortBinding",
                "http://samples.soamoa.yesso.eu/",
                endpointPair,
                proxyBindingKey,
                functionalBindingKey,
                "Layer7 WSMS",
                configRefs,
                runtimeRefs);

        final List<BindingTemplate> publishedTemplates = uddiClient.getPublishedBindingTemplates();
        final BindingTemplate proxyTemplate = publishedTemplates.get(0);
        Assert.assertEquals("Incorrect template published", proxyBindingKey, proxyTemplate.getBindingKey());

        final List<KeyedReference> proxyReferences = proxyTemplate.getCategoryBag().getKeyedReference();
        final Set<UDDIKeyedReference> allRefs = convertToBeans(proxyReferences);
        Map<UDDIKeyedReference, UDDIKeyedReference> refToRef = new HashMap<UDDIKeyedReference, UDDIKeyedReference>();
        for (UDDIKeyedReference allRef : allRefs) {
            refToRef.put(allRef, allRef);
        }
        //validate the keyed reference continues to have the value from UDDI
        Assert.assertTrue("Reference should have been found", refToRef.containsKey(metaData));
        final UDDIKeyedReference actualRef = refToRef.get(metaData);
        Assert.assertEquals("Updated keyName should have been ignored, as UDDI has a different value", "", actualRef.getKeyName());
    }

    /**
     * Confirms that an empty set of runtimeKeyedReferences does not cause a null pointer
     * @throws Exception
     */
    @BugNumber(9400)
    @BugId("SSG-4270")
    @Test
    public void testUpdatePublishOfEndpoint_WithExternalMeta() throws Exception{

        //Set up test
        Wsdl wsdl = Wsdl.newInstance(null, WsdlTUDDIModelConverterTest.getWsdlReader("com/l7tech/uddi/bug8147_playerstats.wsdl"));

        final String gatewayWsdlUrl = "http://thegatewayhost.l7tech.com:8080/3828382?wsdl";
        final String gatewayURL = "http://thegatewayhost.l7tech.com:8080/service/3828382";
        EndpointPair endpointPair = new EndpointPair(gatewayURL, gatewayWsdlUrl);
        final String secureGatewayURL = "https://thegatewayhost.l7tech.com:8080/service/3828382";
        //this reflects the current state of trunk - there are no secure WSDL URLs currently published
        EndpointPair secureEndpointPair = new EndpointPair(secureGatewayURL, gatewayWsdlUrl);

        final int serviceOid = 3828382;

        InputStream stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/PlayerStatsProxiedEndpoint_NoMeta.xml");
        final BusinessService businessService = JAXB.unmarshal(stream, BusinessService.class);
        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/PlayerStatsTModel_Binding.xml");
        final TModel bindingTModel = JAXB.unmarshal(stream, TModel.class);
        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/PlayerStatsTModel_PortType.xml");
        final TModel portTypeTModel = JAXB.unmarshal(stream, TModel.class);
        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/PlayerStatsTModel_Binding_2endpoints.xml");
        final TModel bindingTModel2 = JAXB.unmarshal(stream, TModel.class);
        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/PlayerStatsTModel_PortType_2endpoints.xml");
        final TModel portTypeTModel2 = JAXB.unmarshal(stream, TModel.class);
        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/PlayerStatsTModel_Binding_3endpoints.xml");
        final TModel bindingTModel3 = JAXB.unmarshal(stream, TModel.class);
        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/PlayerStatsTModel_PortType_3endpoints.xml");
        final TModel portTypeTModel3 = JAXB.unmarshal(stream, TModel.class);

        TestUddiClient uddiClient = new TestUddiClient(businessService, Arrays.asList(bindingTModel, portTypeTModel, bindingTModel2, portTypeTModel2, bindingTModel3, portTypeTModel3));

        BusinessServicePublisher servicePublisher = new BusinessServicePublisher(wsdl, serviceOid, uddiClient);

        EndpointPair previousPair = new EndpointPair();
        previousPair.setEndPointUrl("http://thegatewayhost.l7tech.com");

        //test set up
        //if we can publish here, then bug is fixed
        servicePublisher.publishBindingTemplate(
                "uddi:e3544a00-2234-11df-acce-251c32a0acbe",
                "PlayerStatsWsdlPort",
                "PlayerStatsSoapBinding",
                "http://hugh:8081/axis/services/PlayerStats",
                Arrays.asList(previousPair),
                Arrays.asList(endpointPair, secureEndpointPair),
                null,
                false,
                null,
                Collections.<UDDIKeyedReference>emptySet());
    }

    /**
     * Tests that if the functional endpoints GIF keyedReferences were removed, that they are added again during an
     * update
     * @throws Exception
     */
    @BugNumber(8426)
    @BugId("FR-233")
    @Test
    public void testUpdatePublishOfGifEndPoint_FunctionalReferencesWereRemoved() throws Exception {
        Wsdl wsdl = Wsdl.newInstance(null, WsdlTUDDIModelConverterTest.getWsdlReader("com/l7tech/uddi/artistregistry.wsdl"));

        final String gatewayWsdlUrl = "http://ssghost:8081/3828382?wsdl"; //this is a different port than what is currently published (in the test objects)
        final String gatewayURL = "http://ssghost:8081/3828382"; //this is a different port than what is currently published (in the test objects)
        final EndpointPair endpointPair = new EndpointPair(gatewayURL, gatewayWsdlUrl);

        InputStream stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/ArgistRegistryService_GIF_FunctionalModified.xml");
        final BusinessService businessService = JAXB.unmarshal(stream, BusinessService.class);
        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/ArtistRegistryServiceTModel_Binding_GIF.xml");
        final TModel bindingTModelGif = JAXB.unmarshal(stream, TModel.class);
        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/ArtistRegistryServiceTModel_PortType_GIF.xml");
        final TModel portTypeTModelGif = JAXB.unmarshal(stream, TModel.class);

        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/ArtistRegistryServiceTModel_Binding.xml");
        final TModel bindingTModel = JAXB.unmarshal(stream, TModel.class);
        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/ArtistRegistryServiceTModel_PortType.xml");
        final TModel portTypeTModel = JAXB.unmarshal(stream, TModel.class);

        final int serviceOid = 3828382;
        TestUddiClient uddiClient = new TestUddiClient(businessService, Arrays.asList(bindingTModelGif, portTypeTModelGif, bindingTModel, portTypeTModel));
        BusinessServicePublisher servicePublisher = new BusinessServicePublisher(wsdl, serviceOid, uddiClient);

        final String proxyBindingKey = "uddi:ad36b520-d229-11df-a331-7bda57c0a325";
        final String functionalBindingKey = "uddi:c70283ce-c113-493f-b576-957a94dd1e70";
        final Pair<String, String> pair = servicePublisher.publishBindingTemplateGif(
                "uddi:ad3618e0-d229-11df-a331-7bda57c0a325",
                "YessoTestWebServicesPort",
                "YessoTestWebServicesPortBinding",
                "http://samples.soamoa.yesso.eu/",
                endpointPair,
                proxyBindingKey,
                functionalBindingKey,
                "Layer7 WSMS", null, null);

        Assert.assertNotNull("Pair should not be null", pair);

        BindingTemplate functionalTemplate = null;
        BindingTemplate proxyTemplate = null;

        final List<BindingTemplate> publishedTemplates = uddiClient.getPublishedBindingTemplates();
        for (BindingTemplate publishedTemplate : publishedTemplates) {
            //is this the proxy endpoint?
            if(publishedTemplate.getBindingKey().equals(proxyBindingKey)){
                proxyTemplate = publishedTemplate;
            } else if (publishedTemplate.getBindingKey().equals(functionalBindingKey)) {
                functionalTemplate = publishedTemplate;
            }
        }

        Assert.assertNotNull("Functional endpoint should have been found", functionalTemplate);
        Assert.assertNotNull("Proxy endpoint should have been found", proxyTemplate);

        validateFunctionalReferences(proxyBindingKey, functionalTemplate.getCategoryBag().getKeyedReference());
    }

    /**
     * Tests that uddi registry specific meta data is added correctly for published business services
     *
     * @throws Exception
     */
    @Test
    @BugNumber(7980)
    @BugId("SSG-3593")
    public void testRegistrySpecificMetaData() throws Exception {
        Wsdl wsdl = Wsdl.newInstance(null, WsdlTUDDIModelConverterTest.getWsdlReader("com/l7tech/uddi/Warehouse.wsdl"));

        final String gatewayWsdlUrl = "http://localhost:8080/3828382?wsdl";
        final String gatewayURL = "http://localhost:8080/3828382";
        EndpointPair endpointPair = new EndpointPair(gatewayURL, gatewayWsdlUrl);

        final int serviceOid = 3828382;

        TestUddiClient uddiClient = new TestUddiClient();
        BusinessServicePublisher servicePublisher = new BusinessServicePublisher(wsdl, serviceOid, uddiClient);

        UDDIRegistrySpecificMetaData metaData = new UDDIRegistrySpecificMetaData() {
            @Override
            public Collection<UDDIKeyedReference> getBusinessServiceKeyedReferences() {
                Collection<UDDIKeyedReference> returnColl = new ArrayList<UDDIKeyedReference>();
                UDDIKeyedReference kr =
                        new UDDIKeyedReference("uddi:088e847f-eab5-11dc-97f4-dcd945764407",
                                null, "Virtual service");
                returnColl.add(kr);
                return returnColl;
            }

            @Override
            public Collection<UDDIClient.UDDIKeyedReferenceGroup> getBusinessServiceKeyedReferenceGroups() {
                Collection<UDDIClient.UDDIKeyedReferenceGroup> returnColl = new ArrayList<UDDIClient.UDDIKeyedReferenceGroup>();
                Collection<UDDIKeyedReference> keyRefColl = new ArrayList<UDDIKeyedReference>();
                UDDIKeyedReference kr =
                        new UDDIKeyedReference("uddi:uddi.org:categorization:general_keywords",
                                "Contains", "pretend service key");
                keyRefColl.add(kr);

                UDDIClient.UDDIKeyedReferenceGroup krg =
                        new UDDIClient.UDDIKeyedReferenceGroup("uddi:centrasite.com:attributes:relationship", keyRefColl);

                returnColl.add(krg);
                return returnColl;
            }
        };

        final Pair<Set<String>, Set<UDDIBusinessService>> pair =
                servicePublisher.publishServicesToUDDIRegistry("business key",
                        Collections.<String>emptySet(), false, metaData, Arrays.asList(endpointPair));

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

    /**
     * Upgrade from Pandora to Albacore requires the ability to be able to identify and delete bindingTemplates with just a host
     * name. From Albacore on we track the full URL of all endpoints published, in Pandora we didn't.
     * This tests that with the hostname we will correctly delete previously published bindingTemplates
     * @throws Exception
     */
    @Test
    @BugNumber(8147)
    @BugId("SSG-3671")
    public void testDeleteBindingTemplateOnUpgrade() throws Exception{
        Wsdl wsdl = Wsdl.newInstance(null, WsdlTUDDIModelConverterTest.getWsdlReader("com/l7tech/uddi/Warehouse.wsdl"));

        final String gatewayURL = "http://devautotest.l7tech.com:8080/service/3828382";
        final int serviceOid = 3828382;

        BusinessService businessService = new BusinessService();
        businessService.setServiceKey("not needed");
        BindingTemplate bt1 = new BindingTemplate();
        bt1.setBindingKey("bt1key");
        AccessPoint ap1 = new AccessPoint();
        ap1.setValue(gatewayURL);
        ap1.setUseType("endPoint");
        bt1.setAccessPoint(ap1);
        BindingTemplates bindingTemplates = new BindingTemplates();
        bindingTemplates.getBindingTemplate().add(bt1);
        businessService.setBindingTemplates(bindingTemplates);

        TestUddiClient uddiClient = new TestUddiClient(businessService, null, true);
        BusinessServicePublisher servicePublisher = new BusinessServicePublisher(wsdl, serviceOid, uddiClient);
        EndpointPair endpointPair = new EndpointPair();
        endpointPair.setEndPointUrl("http://devautotest.l7tech.com");
        servicePublisher.deleteGatewayBindingTemplates("key", new HashSet<EndpointPair>(Arrays.asList(endpointPair)), null);

        Assert.assertEquals("Incorrect number of bindingTemplates deleted", 1, uddiClient.getNumBindingsDeleted());
    }

    /**
     * Test that when publishing a business service which already exists in UDDI, and the only difference between the
     * business service being published and the one in UDDI is the endpoint URLs, that no new keys should be created and
     * the keys from the existing service should be set correctly on the business service being published. 
     * @throws Exception
     */
    @Test
    @BugNumber(8147)
    @BugId("SSG-3671")
    public void testUpdateOfBusinessService() throws Exception{

        Wsdl wsdl = Wsdl.newInstance(null, WsdlTUDDIModelConverterTest.getWsdlReader("com/l7tech/uddi/bug8147_playerstats.wsdl"));

        final String gatewayWsdlUrl = "http://theoriginalhost.l7tech.com:8080/3828382?wsdl";
        final String gatewayURL = "http://theoriginalhost.l7tech.com:8080/service/3828382";
        EndpointPair endpointPair = new EndpointPair(gatewayURL, gatewayWsdlUrl);

        final int serviceOid = 3828382;

        InputStream stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/PlayerStats.xml");
        final BusinessService businessService = JAXB.unmarshal(stream, BusinessService.class);
        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/PlayerStatsTModel_Binding.xml");
        final TModel bindingTModel = JAXB.unmarshal(stream, TModel.class);
        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/PlayerStatsTModel_PortType.xml");
        final TModel portTypeTModel = JAXB.unmarshal(stream, TModel.class);

        TestUddiClient uddiClient = new TestUddiClient(businessService, Arrays.asList(bindingTModel, portTypeTModel), false);

        BusinessServicePublisher servicePublisher = new BusinessServicePublisher(wsdl, serviceOid, uddiClient);

        try {
            SyspropUtil.setProperty( "com.l7tech.uddi.BusinessServicePublisher.prependServiceLocalName", "" );
            SyspropUtil.setProperty( "com.l7tech.uddi.BusinessServicePublisher.appendServiceLocalName", "" );
            //going to test it's behaviour internally in TestUDDIClient and not any return value
            servicePublisher.publishServicesToUDDIRegistry("business key",
                            new HashSet(Arrays.asList("uddi:e3544a00-2234-11df-acce-251c32a0acbe")), false, null, Arrays.asList(endpointPair));
        } finally {
            SyspropUtil.clearProperty( "com.l7tech.uddi.BusinessServicePublisher.prependServiceLocalName" );
            SyspropUtil.clearProperty( "com.l7tech.uddi.BusinessServicePublisher.appendServiceLocalName" );
        }

        Assert.assertEquals("Only 1 service should have had empty keys", 1, uddiClient.getNumServicesWithNoKey());
        Assert.assertEquals("2 services should have been published", 2, uddiClient.getPublishedServices().size());
        Assert.assertEquals("Only 1 bindingTemplates should have had empty keys", 1, uddiClient.getNumBindingTemplatesWithNoKey());
        Assert.assertEquals("Only 2 tModels should have had empty keys", 2, uddiClient.getNumTModelsWithNoKey());
        Assert.assertEquals("No services should have been deleted", 0, uddiClient.getNumServicesDeleted());
        Assert.assertEquals("No tModels should have been deleted", 0, uddiClient.getNumTModelsDeleted());

        final Set<String> urls = uddiClient.getUniqueUrls();
        Assert.assertEquals("Only a single URL should have been found", 1, urls.size());
        Assert.assertEquals("Incorrect published URL found", gatewayURL, urls.iterator().next());

        final Set<String> wsdlUrls = uddiClient.getUniqueWsdlUrls();
        Assert.assertEquals("Only a single WSDL URL should have been found", 1, wsdlUrls.size());
        Assert.assertEquals("Incorrect published WSDL URL found", gatewayWsdlUrl, wsdlUrls.iterator().next());
    }

    /**
     * Test that when publishing a business service which has already been published to UDDI, and the Business Service
     * being published differs due to a new wsdl:port having been added to the local gateway WSDL. In this case
     * the new wsdl:port will result in a newly published bindingTemplate, and 2 new tModels.
     * @throws Exception
     */
    @Test
    @BugNumber(8147)
    @BugId("SSG-3671")
    public void testPublishAndUpdateOfBusinessService() throws Exception{

        Wsdl wsdl = Wsdl.newInstance(null, WsdlTUDDIModelConverterTest.getWsdlReader("com/l7tech/uddi/bug8147_playerstats_new_endpoint.wsdl"));

        final String gatewayWsdlUrl = "http://theoriginalhost.l7tech.com:8080/3828382?wsdl";
        final String gatewayURL = "http://theoriginalhost.l7tech.com:8080/service/3828382";
        EndpointPair endpointPair = new EndpointPair(gatewayURL, gatewayWsdlUrl);

        final int serviceOid = 3828382;

        //these UDDI Objects represent the previously published WSDL, and are now out of date
        InputStream stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/PlayerStats.xml");
        final BusinessService businessService = JAXB.unmarshal(stream, BusinessService.class);
        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/PlayerStatsTModel_Binding.xml");
        final TModel bindingTModel = JAXB.unmarshal(stream, TModel.class);
        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/PlayerStatsTModel_PortType.xml");
        final TModel portTypeTModel = JAXB.unmarshal(stream, TModel.class);

        TestUddiClient uddiClient = new TestUddiClient(businessService, Arrays.asList(bindingTModel, portTypeTModel));

        BusinessServicePublisher servicePublisher = new BusinessServicePublisher(wsdl, serviceOid, uddiClient);

        try {
            SyspropUtil.setProperty( "com.l7tech.uddi.BusinessServicePublisher.prependServiceLocalName", "" );
            SyspropUtil.setProperty( "com.l7tech.uddi.BusinessServicePublisher.appendServiceLocalName", "" );
            //going to test it's behaviour internally in TestUDDIClient and not any return value
            servicePublisher.publishServicesToUDDIRegistry("business key",
                            new HashSet(Arrays.asList("uddi:e3544a00-2234-11df-acce-251c32a0acbe")), false, null, Arrays.asList(endpointPair));
        } finally {
            SyspropUtil.clearProperty( "com.l7tech.uddi.BusinessServicePublisher.prependServiceLocalName" );
            SyspropUtil.clearProperty( "com.l7tech.uddi.BusinessServicePublisher.appendServiceLocalName" );
        }

        Assert.assertEquals("No services should have had empty keys", 0, uddiClient.getNumServicesWithNoKey());
        Assert.assertEquals("Only 1 service should have been published", 1, uddiClient.getPublishedServices().size());
        Assert.assertEquals("Only one bindingTemplate should have had empty keys", 1, uddiClient.getNumBindingTemplatesWithNoKey());
        Assert.assertEquals("Only two tModels should have had empty keys", 2, uddiClient.getNumTModelsWithNoKey());
        Assert.assertEquals("No services should have been deleted", 0, uddiClient.getNumServicesDeleted());
        Assert.assertEquals("No tModels should have been deleted", 0, uddiClient.getNumTModelsDeleted());

        final Set<String> urls = uddiClient.getUniqueUrls();
        Assert.assertEquals("Only a single URL should have been found", 1, urls.size());
        Assert.assertEquals("Incorrect published URL found", gatewayURL, urls.iterator().next());

        final Set<String> wsdlUrls = uddiClient.getUniqueWsdlUrls();
        Assert.assertEquals("Only a single WSDL URL should have been found", 1, wsdlUrls.size());
        Assert.assertEquals("Incorrect published WSDL URL found", gatewayWsdlUrl, wsdlUrls.iterator().next());

    }

    /**
     * Test that when publishing a business service which has already been published to UDDI, and the Business Service
     * being published differs due to a wsdl:port having been removed from the local gateway WSDL. In this case
     * the applicable bindingTemplate and it's 2 tModels should be deleted.
     * @throws Exception
     */
    @Test
    @BugNumber(8147)
    @BugId("SSG-3671")
    public void testEndpointRemovedUpdateOfBusinessService() throws Exception{

        Wsdl wsdl = Wsdl.newInstance(null, WsdlTUDDIModelConverterTest.getWsdlReader("com/l7tech/uddi/bug8147_playerstats.wsdl"));

        final String gatewayWsdlUrl = "http://theoriginalhost.l7tech.com:8080/3828382?wsdl";
        final String gatewayURL = "http://theoriginalhost.l7tech.com:8080/service/3828382";
        EndpointPair endpointPair = new EndpointPair(gatewayURL, gatewayWsdlUrl);

        final int serviceOid = 3828382;

        //these UDDI Objects represent the previously published WSDL, and are now out of date
        InputStream stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/PlayerStatsTwoEndpoints.xml");
        final BusinessService businessService = JAXB.unmarshal(stream, BusinessService.class);
        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/PlayerStatsTModel_Binding.xml");
        final TModel bindingTModel = JAXB.unmarshal(stream, TModel.class);
        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/PlayerStatsTModel_PortType.xml");
        final TModel portTypeTModel = JAXB.unmarshal(stream, TModel.class);
        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/PlayerStatsTModel_Binding_2endpoints.xml");
        final TModel bindingTModel2 = JAXB.unmarshal(stream, TModel.class);
        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/PlayerStatsTModel_PortType_2endpoints.xml");
        final TModel portTypeTModel2 = JAXB.unmarshal(stream, TModel.class);

        TestUddiClient uddiClient = new TestUddiClient(businessService, Arrays.asList(bindingTModel, portTypeTModel, bindingTModel2, portTypeTModel2));

        BusinessServicePublisher servicePublisher = new BusinessServicePublisher(wsdl, serviceOid, uddiClient);

        try {
            SyspropUtil.setProperty( "com.l7tech.uddi.BusinessServicePublisher.prependServiceLocalName", "" );
            SyspropUtil.setProperty( "com.l7tech.uddi.BusinessServicePublisher.appendServiceLocalName", "" );
            //going to test it's behaviour internally in TestUDDIClient and not any return value
            servicePublisher.publishServicesToUDDIRegistry("business key",
                            new HashSet(Arrays.asList("uddi:e3544a00-2234-11df-acce-251c32a0acbe")), false, null, Arrays.asList(endpointPair));
        } finally {
            SyspropUtil.clearProperty( "com.l7tech.uddi.BusinessServicePublisher.prependServiceLocalName" );
            SyspropUtil.clearProperty( "com.l7tech.uddi.BusinessServicePublisher.appendServiceLocalName" );
        }

        Assert.assertEquals("No services should have had empty keys", 1, uddiClient.getNumServicesWithNoKey());
        Assert.assertEquals("2 services should have been published", 2, uddiClient.getPublishedServices().size());
        Assert.assertEquals("Only 1 bindingTemplate should have had empty keys", 1, uddiClient.getNumBindingTemplatesWithNoKey());
        Assert.assertEquals("Only 2 tModels should have had empty keys", 2, uddiClient.getNumTModelsWithNoKey());
        Assert.assertEquals("No services should have been deleted", 0, uddiClient.getNumServicesDeleted());
        Assert.assertEquals("Two tModels should have been deleted", 2, uddiClient.getNumTModelsDeleted());

        final Set<String> urls = uddiClient.getUniqueUrls();
        Assert.assertEquals("Only a single URL should have been found", 1, urls.size());
        Assert.assertEquals("Incorrect published URL found", gatewayURL, urls.iterator().next());

        final Set<String> wsdlUrls = uddiClient.getUniqueWsdlUrls();
        Assert.assertEquals("Only a single WSDL URL should have been found", 1, wsdlUrls.size());
        Assert.assertEquals("Incorrect published WSDL URL found", gatewayWsdlUrl, wsdlUrls.iterator().next());

    }

    /**
     * Tests that when updating an overwritten service, that keys are correctly reused, as with the above test cases
     * @throws Exception
     */
    @Test
    @BugNumber(8147)
    @BugId("SSG-3671")
    public void testUpdateOfOverwrittenBusinessService() throws Exception{

        Wsdl wsdl = Wsdl.newInstance(null, WsdlTUDDIModelConverterTest.getWsdlReader("com/l7tech/uddi/bug8147_playerstats.wsdl"));

        final String gatewayWsdlUrl = "http://thegatewayhost.l7tech.com:8080/3828382?wsdl";
        final String gatewayURL = "http://thegatewayhost.l7tech.com:8080/service/3828382";
        final String secureGatewayURL = "https://thegatewayhost.l7tech.com:8080/service/3828382";
        EndpointPair endpointPair = new EndpointPair(gatewayURL, gatewayWsdlUrl);
        EndpointPair securePair = new EndpointPair(secureGatewayURL, gatewayWsdlUrl);

        final int serviceOid = 3828382;

        //these UDDI Objects represent the previously published WSDL, and are now out of date
        InputStream stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/PlayerStats.xml");
        final BusinessService businessService = JAXB.unmarshal(stream, BusinessService.class);
        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/PlayerStatsTModel_Binding.xml");
        final TModel bindingTModel = JAXB.unmarshal(stream, TModel.class);
        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/PlayerStatsTModel_PortType.xml");
        final TModel portTypeTModel = JAXB.unmarshal(stream, TModel.class);

        TestUddiClient uddiClient = new TestUddiClient(businessService, true, Arrays.asList(bindingTModel, portTypeTModel));

        BusinessServicePublisher servicePublisher = new BusinessServicePublisher(wsdl, serviceOid, uddiClient);

        try {
            SyspropUtil.setProperty( "com.l7tech.uddi.BusinessServicePublisher.prependServiceLocalName", "" );
            SyspropUtil.setProperty( "com.l7tech.uddi.BusinessServicePublisher.appendServiceLocalName", "" );
            //going to test it's behaviour internally in TestUDDIClient and not any return value
            final Set<String> publishedBindings = servicePublisher.overwriteServiceInUDDI(
                    "uddi:e3544a00-2234-11df-acce-251c32a0acbe",
                    "business key",
                    Arrays.asList(endpointPair, securePair));
            
            Assert.assertEquals("Incorrect number of bindings published", 2, publishedBindings.size());
        } finally {
            SyspropUtil.clearProperty( "com.l7tech.uddi.BusinessServicePublisher.prependServiceLocalName" );
            SyspropUtil.clearProperty( "com.l7tech.uddi.BusinessServicePublisher.appendServiceLocalName" );
        }

        Assert.assertEquals("No services should have had empty keys", 0, uddiClient.getNumServicesWithNoKey());
        Assert.assertEquals("Only 1 service should have been published", 1, uddiClient.getPublishedServices().size());
        Assert.assertEquals("Incorrect number of tModels with empty keys", 4, uddiClient.getNumTModelsWithNoKey());
        Assert.assertEquals("Incorrect number of bindingTemplates with empty keys", 2, uddiClient.getNumBindingTemplatesWithNoKey());
        Assert.assertEquals("No services should have been deleted", 0, uddiClient.getNumServicesDeleted());
        Assert.assertEquals("No tModels should have been deleted", 0, uddiClient.getNumTModelsDeleted());

        final Set<String> urls = uddiClient.getUniqueUrls();
        Assert.assertEquals("Incorrect number of unique endpoints URLs found", 2, urls.size());
        Assert.assertTrue("Incorrect published URLs found", new HashSet<String>(Arrays.asList(gatewayURL, secureGatewayURL)).containsAll(urls));

        final Set<String> wsdlUrls = uddiClient.getUniqueWsdlUrls();
        Assert.assertTrue("Incorrect unique WSDL URLs found", new HashSet<String>(Arrays.asList(gatewayWsdlUrl)).containsAll(wsdlUrls));

        final BusinessService service = uddiClient.getBusinessService("uddi:e3544a00-2234-11df-acce-251c32a0acbe");
        Assert.assertNotNull(service);
        Assert.assertEquals("Incorrect number of names found", 2, service.getName().size());
        Assert.assertEquals("Incorrect number of descriptions found", 1, service.getDescription().size());

    }

    /**
     * Tests the publishing of an endpoint to UDDI for the first time
     * @throws Exception
     */
    @Test
    @BugNumber(8147)
    @BugId("SSG-3671")
    public void testFirstPublishOfEndpoint() throws Exception{

        Wsdl wsdl = Wsdl.newInstance(null, WsdlTUDDIModelConverterTest.getWsdlReader("com/l7tech/uddi/bug8147_playerstats.wsdl"));

        final String gatewayWsdlUrl = "http://theoriginalhost.l7tech.com:8080/3828382?wsdl";
        final String gatewayURL = "http://theoriginalhost.l7tech.com:8080/service/3828382";
        EndpointPair endpointPair = new EndpointPair(gatewayURL, gatewayWsdlUrl);

        final int serviceOid = 3828382;

        InputStream stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/PlayerStats.xml");
        final BusinessService businessService = JAXB.unmarshal(stream, BusinessService.class);
        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/PlayerStatsTModel_Binding.xml");
        final TModel bindingTModel = JAXB.unmarshal(stream, TModel.class);
        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/PlayerStatsTModel_PortType.xml");
        final TModel portTypeTModel = JAXB.unmarshal(stream, TModel.class);

        TestUddiClient uddiClient = new TestUddiClient(businessService, Arrays.asList(bindingTModel, portTypeTModel));

        BusinessServicePublisher servicePublisher = new BusinessServicePublisher(wsdl, serviceOid, uddiClient);

        Set<String> bindingKeys;
        try {
            SyspropUtil.setProperty( "com.l7tech.uddi.BusinessServicePublisher.prependServiceLocalName", "" );
            SyspropUtil.setProperty( "com.l7tech.uddi.BusinessServicePublisher.appendServiceLocalName", "" );
            //going to test it's behaviour internally in TestUDDIClient and not any return value

            bindingKeys = servicePublisher.publishBindingTemplate(
                    "uddi:e3544a00-2234-11df-acce-251c32a0acbe",
                    "PlayerStatsWsdlPort",
                    "PlayerStatsSoapBinding",
                    "http://hugh:8081/axis/services/PlayerStats",
                    null,
                    Arrays.asList(endpointPair),
                    null,
                    false, null, null);
        } finally {
            SyspropUtil.clearProperty( "com.l7tech.uddi.BusinessServicePublisher.prependServiceLocalName" );
            SyspropUtil.clearProperty( "com.l7tech.uddi.BusinessServicePublisher.appendServiceLocalName" );
        }

        Assert.assertEquals("No services should have been published", 0, uddiClient.getPublishedServices().size());
        Assert.assertEquals("Only 1 bindingTemplates should have had empty keys", 1, uddiClient.getNumBindingTemplatesWithNoKey());
        Assert.assertEquals("Only 2 tModels should have had empty keys", 2, uddiClient.getNumTModelsWithNoKey());
        Assert.assertEquals("No services should have been deleted", 0, uddiClient.getNumServicesDeleted());
        Assert.assertEquals("No tModels should have been deleted", 0, uddiClient.getNumTModelsDeleted());
        Assert.assertEquals("Only 1 bindingTemplate should have been published", 1, uddiClient.getNumPublishedBindingTemplates());

        Assert.assertTrue("Incorrect bindingKeys found", bindingKeys.containsAll(uddiClient.getPublishedBindingTemplateKeys()));
        final Set<String> urls = uddiClient.getUniqueUrls();
        Assert.assertEquals("Only a single URL should have been found", 1, urls.size());
        Assert.assertEquals("Incorrect published URL found", gatewayURL, urls.iterator().next());

        final Set<String> wsdlUrls = uddiClient.getUniqueWsdlUrls();
        Assert.assertEquals("Only a single WSDL URL should have been found", 1, wsdlUrls.size());
        Assert.assertEquals("Incorrect published WSDL URL found", gatewayWsdlUrl, wsdlUrls.iterator().next());
    }

    /**
     * Tests the updating of a previously published endpoint - upgrade from Pandora to Albacore
     * The published endpoint should be updated correctly and the existing bindingTemplate keys and tModel keys
     * reused
     * @throws Exception
     */
    @Test
    @BugNumber(8147)
    @BugId("SSG-3671")
    public void testUpdatePublishOfEndpoint() throws Exception{

        Wsdl wsdl = Wsdl.newInstance(null, WsdlTUDDIModelConverterTest.getWsdlReader("com/l7tech/uddi/bug8147_playerstats.wsdl"));

        final String gatewayWsdlUrl = "http://thegatewayhost.l7tech.com:8080/3828382?wsdl";
        final String gatewayURL = "http://thegatewayhost.l7tech.com:8080/service/3828382";
        EndpointPair endpointPair = new EndpointPair(gatewayURL, gatewayWsdlUrl);
        final String secureGatewayURL = "https://thegatewayhost.l7tech.com:8080/service/3828382";
        //this reflects the current state of trunk - there are no secure WSDL URLs currently published
        EndpointPair secureEndpointPair = new EndpointPair(secureGatewayURL, gatewayWsdlUrl);

        final int serviceOid = 3828382;

        InputStream stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/PlayerStatsProxiedEndpoint.xml");
        final BusinessService businessService = JAXB.unmarshal(stream, BusinessService.class);
        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/PlayerStatsTModel_Binding.xml");
        final TModel bindingTModel = JAXB.unmarshal(stream, TModel.class);
        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/PlayerStatsTModel_PortType.xml");
        final TModel portTypeTModel = JAXB.unmarshal(stream, TModel.class);
        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/PlayerStatsTModel_Binding_2endpoints.xml");
        final TModel bindingTModel2 = JAXB.unmarshal(stream, TModel.class);
        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/PlayerStatsTModel_PortType_2endpoints.xml");
        final TModel portTypeTModel2 = JAXB.unmarshal(stream, TModel.class);
        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/PlayerStatsTModel_Binding_3endpoints.xml");
        final TModel bindingTModel3 = JAXB.unmarshal(stream, TModel.class);
        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/PlayerStatsTModel_PortType_3endpoints.xml");
        final TModel portTypeTModel3 = JAXB.unmarshal(stream, TModel.class);

        TestUddiClient uddiClient = new TestUddiClient(businessService, Arrays.asList(bindingTModel, portTypeTModel, bindingTModel2, portTypeTModel2, bindingTModel3, portTypeTModel3));

        BusinessServicePublisher servicePublisher = new BusinessServicePublisher(wsdl, serviceOid, uddiClient);

        EndpointPair previousPair = new EndpointPair();
        previousPair.setEndPointUrl("http://thegatewayhost.l7tech.com");

        Set<String> bindingKeys;
        try {
            SyspropUtil.setProperty( "com.l7tech.uddi.BusinessServicePublisher.prependServiceLocalName", "" );
            SyspropUtil.setProperty( "com.l7tech.uddi.BusinessServicePublisher.appendServiceLocalName", "" );
            //going to test it's behaviour internally in TestUDDIClient and not any return value

            bindingKeys = servicePublisher.publishBindingTemplate(
                    "uddi:e3544a00-2234-11df-acce-251c32a0acbe",
                    "PlayerStatsWsdlPort",
                    "PlayerStatsSoapBinding",
                    "http://hugh:8081/axis/services/PlayerStats",
                    Arrays.asList(previousPair),
                    Arrays.asList(endpointPair, secureEndpointPair),
                    null,
                    false, null, null);
        } finally {
            SyspropUtil.clearProperty( "com.l7tech.uddi.BusinessServicePublisher.prependServiceLocalName" );
            SyspropUtil.clearProperty( "com.l7tech.uddi.BusinessServicePublisher.appendServiceLocalName" );
        }

        Assert.assertEquals("No services should have been published", 0, uddiClient.getPublishedServices().size());
        Assert.assertEquals("No bindingTemplates should have had empty keys", 0, uddiClient.getNumBindingTemplatesWithNoKey());
        Assert.assertEquals("No tModels should have had empty keys", 0, uddiClient.getNumTModelsWithNoKey());
        Assert.assertEquals("No services should have been deleted", 0, uddiClient.getNumServicesDeleted());
        Assert.assertEquals("No tModels should have been deleted", 0, uddiClient.getNumTModelsDeleted());
        Assert.assertEquals("Incorrect number of tModels published", 4, uddiClient.getNumTModelsPublished());
        Assert.assertEquals("Only 2 bindingTemplates should have been published", 2, uddiClient.getNumPublishedBindingTemplates());


        Assert.assertTrue("Incorrect bindingKeys found", bindingKeys.containsAll(uddiClient.getPublishedBindingTemplateKeys()));
        final Set<String> urls = uddiClient.getUniqueUrls();
        Assert.assertEquals("Incorrect number of unique endpoints URLs found", 2, urls.size());
        Assert.assertTrue("Incorrect published URLs found", new HashSet<String>(Arrays.asList(gatewayURL, secureGatewayURL)).containsAll(urls));

        final Set<String> wsdlUrls = uddiClient.getUniqueWsdlUrls();
        Assert.assertTrue("Incorrect unique WSDL URLs found", new HashSet<String>(Arrays.asList(gatewayWsdlUrl)).containsAll(wsdlUrls));
    }

    /**
     * Tests the updating of a previously published endpoint - from Albacore onwards. The existing BindingTemplate keys
     * should be found by the bindingKey and not hostname and relative URL parts
     * The published endpoint should be updated correctly and the existing bindingTemplate keys and tModel keys
     * reused
     * @throws Exception
     */
    @Test
    @BugNumber(8147)
    @BugId("SSG-3671")
    public void testUpdatePublishOfEndpointWithBindingKeys() throws Exception{

        Wsdl wsdl = Wsdl.newInstance(null, WsdlTUDDIModelConverterTest.getWsdlReader("com/l7tech/uddi/bug8147_playerstats.wsdl"));

        final String gatewayWsdlUrl = "http://thegatewayhost.l7tech.com:8080/3828382?wsdl";
        final String gatewayURL = "http://thegatewayhost.l7tech.com:8080/service/3828382";
        EndpointPair endpointPair = new EndpointPair(gatewayURL, gatewayWsdlUrl);
        final String secureGatewayURL = "https://thegatewayhost.l7tech.com:8080/service/3828382";
        //this reflects the current state of trunk - there are no secure WSDL URLs currently published
        EndpointPair secureEndpointPair = new EndpointPair(secureGatewayURL, gatewayWsdlUrl);

        final int serviceOid = 3828382;

        InputStream stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/PlayerStatsProxiedEndpoint.xml");
        final BusinessService businessService = JAXB.unmarshal(stream, BusinessService.class);
        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/PlayerStatsTModel_Binding.xml");
        final TModel bindingTModel = JAXB.unmarshal(stream, TModel.class);
        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/PlayerStatsTModel_PortType.xml");
        final TModel portTypeTModel = JAXB.unmarshal(stream, TModel.class);
        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/PlayerStatsTModel_Binding_2endpoints.xml");
        final TModel bindingTModel2 = JAXB.unmarshal(stream, TModel.class);
        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/PlayerStatsTModel_PortType_2endpoints.xml");
        final TModel portTypeTModel2 = JAXB.unmarshal(stream, TModel.class);
        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/PlayerStatsTModel_Binding_3endpoints.xml");
        final TModel bindingTModel3 = JAXB.unmarshal(stream, TModel.class);
        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/PlayerStatsTModel_PortType_3endpoints.xml");
        final TModel portTypeTModel3 = JAXB.unmarshal(stream, TModel.class);

        TestUddiClient uddiClient = new TestUddiClient(businessService, Arrays.asList(bindingTModel, portTypeTModel, bindingTModel2, portTypeTModel2, bindingTModel3, portTypeTModel3));

        BusinessServicePublisher servicePublisher = new BusinessServicePublisher(wsdl, serviceOid, uddiClient);

        EndpointPair previousPair = new EndpointPair();
        previousPair.setEndPointUrl("http://thegatewayhost.l7tech.com");

        Set<String> bindingKeys;
        try {
            SyspropUtil.setProperty( "com.l7tech.uddi.BusinessServicePublisher.prependServiceLocalName", "" );
            SyspropUtil.setProperty( "com.l7tech.uddi.BusinessServicePublisher.appendServiceLocalName", "" );

            bindingKeys = servicePublisher.publishBindingTemplate(
                    "uddi:e3544a00-2234-11df-acce-251c32a0acbe",
                    "PlayerStatsWsdlPort",
                    "PlayerStatsSoapBinding",
                    "http://hugh:8081/axis/services/PlayerStats",
                    Arrays.asList(previousPair),
                    Arrays.asList(endpointPair, secureEndpointPair),
                    new HashSet<String>(Arrays.asList("uddi:e3549820-2234-11df-acce-251c32a0acbd", "uddi:e3549820-2234-11df-acce-251c32a03333")),
                    false, null, null);
        } finally {
            SyspropUtil.clearProperty( "com.l7tech.uddi.BusinessServicePublisher.prependServiceLocalName" );
            SyspropUtil.clearProperty( "com.l7tech.uddi.BusinessServicePublisher.appendServiceLocalName" );
        }

        Assert.assertEquals("No services should have been published", 0, uddiClient.getPublishedServices().size());
        Assert.assertEquals("No bindingTemplates should have had empty keys", 0, uddiClient.getNumBindingTemplatesWithNoKey());
        Assert.assertEquals("No tModels should have had empty keys", 0, uddiClient.getNumTModelsWithNoKey());
        Assert.assertEquals("No services should have been deleted", 0, uddiClient.getNumServicesDeleted());
        Assert.assertEquals("No tModels should have been deleted", 0, uddiClient.getNumTModelsDeleted());
        Assert.assertEquals("Incorrect number of tModels published", 4, uddiClient.getNumTModelsPublished());
        Assert.assertEquals("Only 2 bindingTemplates should have been published", 2, uddiClient.getNumPublishedBindingTemplates());
        

        Assert.assertTrue("Incorrect bindingKeys found", bindingKeys.containsAll(uddiClient.getPublishedBindingTemplateKeys()));
        final Set<String> urls = uddiClient.getUniqueUrls();
        Assert.assertEquals("Incorrect number of unique endpoints URLs found", 2, urls.size());
        Assert.assertTrue("Incorrect published URLs found", new HashSet<String>(Arrays.asList(gatewayURL, secureGatewayURL)).containsAll(urls));

        final Set<String> wsdlUrls = uddiClient.getUniqueWsdlUrls();
        Assert.assertTrue("Incorrect unique WSDL URLs found", new HashSet<String>(Arrays.asList(gatewayWsdlUrl)).containsAll(wsdlUrls));
    }

    /**
     * Tests that when endpoints are published for a published service, that any previously published bindingTemplates
     * which are no longer applicable are deleted from UDDI.
     *
     * @throws Exception
     */
    @Test
    @BugNumber(8147)
    @BugId("SSG-3671")
    public void testBindingTemplateListenerRemoved() throws Exception {
        Wsdl wsdl = Wsdl.newInstance(null, WsdlTUDDIModelConverterTest.getWsdlReader("com/l7tech/uddi/bug8147_playerstats.wsdl"));

        final String gatewayWsdlUrl = "http://thegatewayhost.l7tech.com:8080/3828382?wsdl";
        final String gatewayURL = "http://thegatewayhost.l7tech.com:8080/service/3828382";
        EndpointPair endpointPair = new EndpointPair(gatewayURL, gatewayWsdlUrl);
        final String secureGatewayURL = "https://thegatewayhost.l7tech.com:8080/service/3828382";
        //this reflects the current state of trunk - there are no secure WSDL URLs currently published
        EndpointPair secureEndpointPair = new EndpointPair(secureGatewayURL, gatewayWsdlUrl);

        final int serviceOid = 3828382;

        InputStream stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/PlayerStatsProxiedEndpoint.xml");
        final BusinessService businessService = JAXB.unmarshal(stream, BusinessService.class);
        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/PlayerStatsTModel_Binding.xml");
        final TModel bindingTModel = JAXB.unmarshal(stream, TModel.class);
        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/PlayerStatsTModel_PortType.xml");
        final TModel portTypeTModel = JAXB.unmarshal(stream, TModel.class);
        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/PlayerStatsTModel_Binding_2endpoints.xml");
        final TModel bindingTModel2 = JAXB.unmarshal(stream, TModel.class);
        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/PlayerStatsTModel_PortType_2endpoints.xml");
        final TModel portTypeTModel2 = JAXB.unmarshal(stream, TModel.class);
        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/PlayerStatsTModel_Binding_3endpoints.xml");
        final TModel bindingTModel3 = JAXB.unmarshal(stream, TModel.class);
        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/PlayerStatsTModel_PortType_3endpoints.xml");
        final TModel portTypeTModel3 = JAXB.unmarshal(stream, TModel.class);

        TestUddiClient uddiClient = new TestUddiClient(businessService, Arrays.asList(bindingTModel, portTypeTModel, bindingTModel2, portTypeTModel2, bindingTModel3, portTypeTModel3));

        BusinessServicePublisher servicePublisher = new BusinessServicePublisher(wsdl, serviceOid, uddiClient);

        EndpointPair previousPair = new EndpointPair();
        previousPair.setEndPointUrl("http://thegatewayhost.l7tech.com");

        Set<String> bindingKeys;
        try {
            SyspropUtil.setProperty( "com.l7tech.uddi.BusinessServicePublisher.prependServiceLocalName", "" );
            SyspropUtil.setProperty( "com.l7tech.uddi.BusinessServicePublisher.appendServiceLocalName", "" );

            bindingKeys = servicePublisher.publishBindingTemplate(
                    "uddi:e3544a00-2234-11df-acce-251c32a0acbe",
                    "PlayerStatsWsdlPort",
                    "PlayerStatsSoapBinding",
                    "http://hugh:8081/axis/services/PlayerStats",
                    Arrays.asList(previousPair),
                    Arrays.asList(secureEndpointPair),
                    new HashSet<String>(Arrays.asList("uddi:e3549820-2234-11df-acce-251c32a0acbd", "uddi:e3549820-2234-11df-acce-251c32a03333")),
                    false, null, null);
        } finally {
            SyspropUtil.clearProperty( "com.l7tech.uddi.BusinessServicePublisher.prependServiceLocalName" );
            SyspropUtil.clearProperty( "com.l7tech.uddi.BusinessServicePublisher.appendServiceLocalName" );
        }

        Assert.assertEquals("No services should have been published", 0, uddiClient.getPublishedServices().size());
        Assert.assertEquals("No bindingTemplates should have had empty keys", 0, uddiClient.getNumBindingTemplatesWithNoKey());
        Assert.assertEquals("No tModels should have had empty keys", 0, uddiClient.getNumTModelsWithNoKey());
        Assert.assertEquals("No services should have been deleted", 0, uddiClient.getNumServicesDeleted());
        Assert.assertEquals("No tModels should have been deleted", 0, uddiClient.getNumTModelsDeleted());
        Assert.assertEquals("Incorrect number of tModels published", 2, uddiClient.getNumTModelsPublished());
        Assert.assertEquals("Only 1 bindingTemplates should have been published", 1, uddiClient.getNumPublishedBindingTemplates());


        Assert.assertTrue("Incorrect bindingKeys found", bindingKeys.containsAll(uddiClient.getPublishedBindingTemplateKeys()));
        final Set<String> urls = uddiClient.getUniqueUrls();
        Assert.assertEquals("Incorrect number of unique endpoints URLs found", 1, urls.size());
        Assert.assertTrue("Incorrect published URLs found", new HashSet<String>(Arrays.asList(gatewayURL, secureGatewayURL)).containsAll(urls));

        final Set<String> wsdlUrls = uddiClient.getUniqueWsdlUrls();
        Assert.assertTrue("Incorrect unique WSDL URLs found", new HashSet<String>(Arrays.asList(gatewayWsdlUrl)).containsAll(wsdlUrls));

        //A bindingTemplate should have been deleted as the gateway configuration changed to remove the http listener
        Assert.assertEquals("Incorrect number of bindingTemplates deleted from UDDI", 1, uddiClient.getNumBindingsDeleted());


    }

    /**
     * When the gateway needs to update a BusinessService which has already been published to UDDI, it needs to maintain
     * any meta data added by 3rd parties. This meta data shows up as either keyedReferences or keyedReferenceGroups
     * in the categoryBag attached to the BusinessService. The BusinessService it self will already have it's own meta
     * data so only data which has been added by a 3rd party should be copied e.g. there should be no duplicate meta
     * data in the categoryBag.
     * @throws Exception
     */
    @Test
    @BugNumber(9180)
    @BugId("SSG-3671")
    public void testBusinessServiceMetaDataPreservedOnRepublish() throws Exception{
        InputStream stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/PlayerStats_Bug9180.xml");
        final BusinessService businessService = JAXB.unmarshal(stream, BusinessService.class);
        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/PlayerStatsTModel_Binding.xml");
        final TModel bindingTModel = JAXB.unmarshal(stream, TModel.class);
        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/PlayerStatsTModel_PortType.xml");
        final TModel portTypeTModel = JAXB.unmarshal(stream, TModel.class);

        final UDDIRegistrySpecificMetaData ssgSpecificData = new UDDIRegistrySpecificMetaData() {
            @Override
            public Collection<UDDIKeyedReference> getBusinessServiceKeyedReferences() {
                UDDIKeyedReference ref = new UDDIKeyedReference("Key1", "Name1", "Value1");
                return Arrays.asList(ref);
            }

            @Override
            public Collection<UDDIClient.UDDIKeyedReferenceGroup> getBusinessServiceKeyedReferenceGroups() {
                UDDIKeyedReference ref = new UDDIKeyedReference("GroupKey1", "GroupName1", "GroupValue1");
                //This group will collide with a group already in PlayerStats_Bug9180.xml, so the missing reference should be added
                UDDIClient.UDDIKeyedReferenceGroup group =
                        new UDDIClient.UDDIKeyedReferenceGroup("uddi:schemas.xmlsoap.org:localpolicyreference:2003_03",
                                Arrays.asList(ref));

                //This group should exist on it's own.
                UDDIKeyedReference refNoMerge = new UDDIKeyedReference("NoMergeGroupKey1", "NoMergeGroupName1", "NoMergeGroupValue1");
                //This group will collide with a group already in PlayerStats_Bug9180.xml, so the missing reference should be added
                UDDIClient.UDDIKeyedReferenceGroup groupNoMerge =
                        new UDDIClient.UDDIKeyedReferenceGroup("uddi:NoMerge.doesnotexist",
                                Arrays.asList(refNoMerge));

                return Arrays.asList(group, groupNoMerge);
            }
        };

        final TestUddiClient uddiClient = new TestUddiClient(businessService, Arrays.asList(bindingTModel, portTypeTModel));
        final Wsdl wsdl = Wsdl.newInstance(null, WsdlTUDDIModelConverterTest.getWsdlReader("com/l7tech/uddi/PlayerStats.wsdl"));
        final BusinessServicePublisher servicePublisher = new BusinessServicePublisher(wsdl, 123132123, uddiClient);
        final String gatewayWsdlUrl = "http://ssghost.l7tech.com:8080/123132123?wsdl";
        final String gatewayURL = "http://ssghost.l7tech.com:8080/service/123132123";
        final EndpointPair endpointPair = new EndpointPair(gatewayURL, gatewayWsdlUrl);

        servicePublisher.publishServicesToUDDIRegistry("business key",
                        Collections.<String>emptySet(), false, ssgSpecificData, Arrays.asList(endpointPair));

        final List<BusinessService> list = uddiClient.getPublishedServices();
        Assert.assertEquals("Only 1 service should have been published", 1, list.size());

        final BusinessService publishedService = list.get(0);

        //check how the meta data was merged - what was in UDDI and what was created by the SSG

        final CategoryBag categoryBag = publishedService.getCategoryBag();
        final List<KeyedReference> references = categoryBag.getKeyedReference();
        Assert.assertEquals("Incorrect number of keyed references found", 5, references.size());

        //Check each individual value that is expected
        Assert.assertEquals("Incorrect tModelKey found","uddi:uddi.org:wsdl:types",references.get(0).getTModelKey());
        Assert.assertEquals("Incorrect keyValue found","service",references.get(0).getKeyValue());
        //note: the keyname was changed in this keyed reference as it was found in UDDI with a different value.
        Assert.assertEquals("Incorrect keyName found","uddi.org:wsdl:types",references.get(0).getKeyName());

        Assert.assertEquals("Incorrect tModelKey found","uddi:uddi.org:xml:localname",references.get(1).getTModelKey());
        Assert.assertEquals("Incorrect keyValue found","PlayerStatsService",references.get(1).getKeyValue());
        //note: the keyname was changed in this keyed reference as it was found in UDDI with a different value.
        Assert.assertEquals("Incorrect keyName found","uddi.org:xml:localname",references.get(1).getKeyName());

        Assert.assertEquals("Incorrect tModelKey found","uddi:uddi.org:xml:namespace",references.get(2).getTModelKey());
        Assert.assertEquals("Incorrect keyValue found","http://hugh:8081/axis/services/PlayerStats",references.get(2).getKeyValue());
        //note: the keyname was changed in this keyed reference as it was found in UDDI with a different value.
        Assert.assertEquals("Incorrect keyName found","uddi.org:xml:namespace",references.get(2).getKeyName());

        Assert.assertEquals("Incorrect tModelKey found","Key1",references.get(3).getTModelKey());
        Assert.assertEquals("Incorrect keyValue found","Value1",references.get(3).getKeyValue());
        Assert.assertEquals("Incorrect keyName found","Name1",references.get(3).getKeyName());

        Assert.assertEquals("Incorrect tModelKey found","uddi:uddi.org:categorization:general_keywords",references.get(4).getTModelKey());
        Assert.assertEquals("Incorrect keyValue found","9180",references.get(4).getKeyValue());
        Assert.assertEquals("Incorrect keyName found","BugNumber",references.get(4).getKeyName());

        //Test that groups were correctly merged
        final List<KeyedReferenceGroup> referenceGroupList = categoryBag.getKeyedReferenceGroup();
        Assert.assertEquals("Incorrect number of groups found", 2, referenceGroupList.size());

        final KeyedReferenceGroup group = referenceGroupList.get(0);
        final List<KeyedReference> groupRefs = group.getKeyedReference();
        Assert.assertEquals("Incorrect number of group keyed references found", 2, groupRefs.size());

        Assert.assertEquals("Incorrect tModelKey found","GroupKey1",groupRefs.get(0).getTModelKey());
        Assert.assertEquals("Incorrect keyValue found","GroupValue1",groupRefs.get(0).getKeyValue());
        Assert.assertEquals("Incorrect keyName found","GroupName1",groupRefs.get(0).getKeyName());

        Assert.assertEquals("Incorrect tModelKey found","uddi:uddi.org:categorization:general_keywords",groupRefs.get(1).getTModelKey());
        Assert.assertEquals("Incorrect keyValue found","Bug9180",groupRefs.get(1).getKeyValue());
        Assert.assertEquals("Incorrect keyName found","BugNumberAgain",groupRefs.get(1).getKeyName());

        final KeyedReferenceGroup noMergeGroup = referenceGroupList.get(1);
        final List<KeyedReference> noMergeRefs = noMergeGroup.getKeyedReference();
        Assert.assertEquals("Incorrect number of group keyed references found", 1, noMergeRefs.size());

        Assert.assertEquals("Incorrect tModelKey found","NoMergeGroupKey1",noMergeRefs.get(0).getTModelKey());
        Assert.assertEquals("Incorrect keyValue found","NoMergeGroupValue1",noMergeRefs.get(0).getKeyValue());
        Assert.assertEquals("Incorrect keyName found","NoMergeGroupName1",noMergeRefs.get(0).getKeyName());

        final BindingTemplate template = publishedService.getBindingTemplates().getBindingTemplate().get(0);
        final CategoryBag bindingCatBag = template.getCategoryBag();
        Assert.assertNotNull("categoryBag should be found on the bindingTemplate as it was synchronized", bindingCatBag);
        final List<KeyedReference> refs = bindingCatBag.getKeyedReference();
        Assert.assertEquals("Incorrect number of keyed references found on bindingTemplate", 1, refs.size());

        final KeyedReference bindingRef = refs.get(0);
        Assert.assertEquals("Incorrect tModelKey found","uddi:uddi.org:wsdl:types",bindingRef.getTModelKey());
        Assert.assertEquals("Incorrect keyValue found","port",bindingRef.getKeyValue());
        Assert.assertEquals("Incorrect keyName found","uddi.org:wsdl:types",bindingRef.getKeyName());
    }

    /**
     * See test testUpdatePublishOfEndpoint - this test case extends it by examining the categoryBags of the updated
     * bindingTemplates.
     * @throws Exception
     */
    @Test
    @BugNumber(9180)
    @BugId("SSG-4166")
    public void testBindingMetaDataPreservedOnRepublish() throws Exception{
        Wsdl wsdl = Wsdl.newInstance(null, WsdlTUDDIModelConverterTest.getWsdlReader("com/l7tech/uddi/bug8147_playerstats.wsdl"));

        final String gatewayWsdlUrl = "http://thegatewayhost.l7tech.com:8080/3828382?wsdl";
        final String gatewayURL = "http://thegatewayhost.l7tech.com:8080/service/3828382";
        EndpointPair endpointPair = new EndpointPair(gatewayURL, gatewayWsdlUrl);
        final String secureGatewayURL = "https://thegatewayhost.l7tech.com:8080/service/3828382";
        //this reflects the current state of trunk - there are no secure WSDL URLs currently published
        EndpointPair secureEndpointPair = new EndpointPair(secureGatewayURL, gatewayWsdlUrl);

        final int serviceOid = 3828382;

        InputStream stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/PlayerStatsProxiedEndpoint.xml");
        final BusinessService businessService = JAXB.unmarshal(stream, BusinessService.class);
        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/PlayerStatsTModel_Binding.xml");
        final TModel bindingTModel = JAXB.unmarshal(stream, TModel.class);
        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/PlayerStatsTModel_PortType.xml");
        final TModel portTypeTModel = JAXB.unmarshal(stream, TModel.class);
        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/PlayerStatsTModel_Binding_2endpoints.xml");
        final TModel bindingTModel2 = JAXB.unmarshal(stream, TModel.class);
        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/PlayerStatsTModel_PortType_2endpoints.xml");
        final TModel portTypeTModel2 = JAXB.unmarshal(stream, TModel.class);
        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/PlayerStatsTModel_Binding_3endpoints.xml");
        final TModel bindingTModel3 = JAXB.unmarshal(stream, TModel.class);
        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/PlayerStatsTModel_PortType_3endpoints.xml");
        final TModel portTypeTModel3 = JAXB.unmarshal(stream, TModel.class);

        TestUddiClient uddiClient = new TestUddiClient(businessService, Arrays.asList(bindingTModel, portTypeTModel, bindingTModel2, portTypeTModel2, bindingTModel3, portTypeTModel3));

        BusinessServicePublisher servicePublisher = new BusinessServicePublisher(wsdl, serviceOid, uddiClient);

        EndpointPair previousPair = new EndpointPair();
        previousPair.setEndPointUrl("http://thegatewayhost.l7tech.com");

        Set<String> bindingKeys;
        try {
            SyspropUtil.setProperty( "com.l7tech.uddi.BusinessServicePublisher.prependServiceLocalName", "" );
            SyspropUtil.setProperty( "com.l7tech.uddi.BusinessServicePublisher.appendServiceLocalName", "" );
            //going to test it's behaviour internally in TestUDDIClient and not any return value

            servicePublisher.publishBindingTemplate(
                    "uddi:e3544a00-2234-11df-acce-251c32a0acbe",
                    "PlayerStatsWsdlPort",
                    "PlayerStatsSoapBinding",
                    "http://hugh:8081/axis/services/PlayerStats",
                    Arrays.asList(previousPair),
                    Arrays.asList(endpointPair, secureEndpointPair),
                    null,
                    false, null, null);
        } finally {
            SyspropUtil.clearProperty( "com.l7tech.uddi.BusinessServicePublisher.prependServiceLocalName" );
            SyspropUtil.clearProperty( "com.l7tech.uddi.BusinessServicePublisher.appendServiceLocalName" );
        }

        final List<BindingTemplate> bindingTemplates = uddiClient.getPublishedBindingTemplates();
        Assert.assertEquals("Incorrect number of binding templates published", 2, bindingTemplates.size());

        //http binding
        final BindingTemplate bt1 = bindingTemplates.get(0);
        final List<KeyedReference> references = bt1.getCategoryBag().getKeyedReference();
        Assert.assertEquals("Incorrect number of keyed references found", 2, references.size());
        KeyedReference portTypeRef = references.get(0);
        Assert.assertEquals("Incorrect tModelKey found", "uddi:uddi.org:wsdl:types", portTypeRef.getTModelKey());
        Assert.assertEquals("Incorrect keyValue found", "port", portTypeRef.getKeyValue());
        Assert.assertEquals("Incorrect keyName found", "uddi.org:wsdl:types", portTypeRef.getKeyName());

        final KeyedReference generalKeyRef = references.get(1);
        Assert.assertEquals("Incorrect tModelKey found", "uddi:uddi.org:categorization:general_keywords", generalKeyRef.getTModelKey());
        Assert.assertEquals("Incorrect keyValue found", "KeyValue2", generalKeyRef.getKeyValue());
        Assert.assertEquals("Incorrect keyName found", "KeyName1", generalKeyRef.getKeyName());

        //https binding
        final BindingTemplate bt2 = bindingTemplates.get(1);
        final List<KeyedReference> bt2References = bt2.getCategoryBag().getKeyedReference();
        Assert.assertEquals("Incorrect number of keyed references found", 1, bt2References.size());
        portTypeRef = references.get(0);
        Assert.assertEquals("Incorrect tModelKey found", "uddi:uddi.org:wsdl:types", portTypeRef.getTModelKey());
        Assert.assertEquals("Incorrect keyValue found", "port", portTypeRef.getKeyValue());
        Assert.assertEquals("Incorrect keyName found", "uddi.org:wsdl:types", portTypeRef.getKeyName());

        final List<KeyedReferenceGroup> refGroupList = bt2.getCategoryBag().getKeyedReferenceGroup();
        Assert.assertEquals("Incorrect number of reference groups found", 1, refGroupList.size());

        final KeyedReferenceGroup group = refGroupList.get(0);
        Assert.assertEquals("Incorrect group tModelKeyFound", "uddi:centrasite.com:attributes:relationship", group.getTModelKey());

        final List<KeyedReference> groupRefs = group.getKeyedReference();
        Assert.assertEquals("Incorrect number of keyed reference in group found", 1, groupRefs.size());

        final KeyedReference keyRefGroup = groupRefs.get(0);
        Assert.assertEquals("Incorrect tModelKey found", "uddi:made_up_key_not_general_keywords", keyRefGroup.getTModelKey());
        Assert.assertEquals("Incorrect keyValue found", "uddi:made_up_key_value", keyRefGroup.getKeyValue());
        Assert.assertEquals("Incorrect keyName found", "Contains", keyRefGroup.getKeyName());
    }

    private void testBindingTemplate(String gatewayWsdlUrl, String gatewayURL, String serviceKey, Pair<BindingTemplate, List<TModel>> bindingAndModels) {
        Map<String, TModel> keyToModel = new HashMap<String, TModel>();
        for (TModel model : bindingAndModels.right) {
            keyToModel.put(model.getTModelKey(), model);
        }
        BindingTemplate newTemplate = bindingAndModels.left;
        //were interested to know that the any models the binding references are new, and are not accidentally
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
                Assert.assertEquals("Invalid instanceParam found", "YessoTestWebServicesPort", instanceDetails.getInstanceParms());
                found = true;
                //while were here, test the wsdl:binding tModel
                TModel newbindingTModel = keyToModel.get(instanceInfo.getTModelKey());
                Assert.assertEquals("Incorrect tModel type found", UDDIUtilities.TMODEL_TYPE.WSDL_BINDING, UDDIUtilities.getTModelType(newbindingTModel, true));
                Assert.assertEquals("Incorrect tModel name found", "YessoTestWebServicesPortBinding", newbindingTModel.getName().getValue());
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
                Assert.assertEquals("Invalid namespace value found", "http://samples.soamoa.yesso.eu/", tModelKeyToRef.get(WsdlToUDDIModelConverter.UDDI_XML_NAMESPACE).getKeyValue());
                Assert.assertEquals("Invalid namespace name found", "binding namespace", tModelKeyToRef.get(WsdlToUDDIModelConverter.UDDI_XML_NAMESPACE).getKeyName());

                //Overview doc
                Assert.assertEquals("Incorrect number of overview docs found", 2, newbindingTModel.getOverviewDoc().size());
                OverviewDoc overviewDoc = newbindingTModel.getOverviewDoc().get(0);
                Assert.assertEquals("Invalid gateway WSDL URL found", gatewayWsdlUrl, overviewDoc.getOverviewURL().getValue());
            } else {
                //this is the wsdl:portType tModel reference
                TModel newPortTypeTModel = keyToModel.get(instanceInfo.getTModelKey());
                Assert.assertEquals("Incorrect tModel type found", UDDIUtilities.TMODEL_TYPE.WSDL_PORT_TYPE, UDDIUtilities.getTModelType(newPortTypeTModel, true));
                Assert.assertEquals("Incorrect tModel name found", "YessoTestWebServices", newPortTypeTModel.getName().getValue());

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
                Assert.assertEquals("Invalid namespace value found", "http://samples.soamoa.yesso.eu/", tModelKeyToRef.get(WsdlToUDDIModelConverter.UDDI_XML_NAMESPACE).getKeyValue());
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

//    public static void main(String [] args) throws Exception{
////        code to download and marshall UDDI objects
//        final String inquiry = "http://systinet.l7tech.com:8080/uddi/inquiry";
//        final String publish = "http://systinet.l7tech.com:8080/uddi/inquiry";
//        final String subscription = "http://systinet.l7tech.com:8080/uddi/inquiry";
//        final String security = "http://systinet.l7tech.com:8080/uddi/security";
//        GenericUDDIClient uddiClient = new GenericUDDIClient(inquiry, publish, subscription, security, "admin", "7layer", PolicyAttachmentVersion.v1_2, null, true);
//
//        UDDIBusinessServiceDownloader serviceDownloader = new UDDIBusinessServiceDownloader(uddiClient);
//
//        Set<String> serviceKeys = new HashSet<String>();
//        serviceKeys.add("uddi:e3544a00-2234-11df-acce-251c32a0acbe");
//        final List<Pair<BusinessService, Map<String, TModel>>> modelFromUddi = serviceDownloader.getBusinessServiceModels(serviceKeys);
////        Pair<List<BusinessService>, Map<String, TModel>> modelFromUddi = serviceDownloader.getBusinessServiceModels(serviceKeys);
//        Assert.assertEquals("", 1, modelFromUddi.size());
//
//        final Pair<BusinessService, Map<String, TModel>> serviceMapPair = modelFromUddi.iterator().next();
//        JAXB.marshal(serviceMapPair.left, new File("/home/darmstrong/Documents/Projects/Pandora/Bugs/Bug8147/UDDIObjects/PlayerStats.xml"));
//        int i = 0;
//        for(TModel model: serviceMapPair.right.values()) {
//            JAXB.marshal(model, new File("/home/darmstrong/Documents/Projects/Pandora/Bugs/Bug8147/UDDIObjects/tModel" + i + ".xml"));
//            i++;
//        }
//
//    }

    public static void main(String [] args) throws Exception{
//        code to download and marshall UDDI objects
        final String inquiry = "http://systinet.l7tech.com:8080/uddi/inquiry";
        final String publish = "http://systinet.l7tech.com:8080/uddi/inquiry";
        final String subscription = "http://systinet.l7tech.com:8080/uddi/inquiry";
        final String security = "http://systinet.l7tech.com:8080/uddi/security";
        GenericUDDIClient uddiClient = new GenericUDDIClient(inquiry, publish, subscription, security, "admin", "7layer", PolicyAttachmentVersion.v1_2, null, true);

        UDDIBusinessServiceDownloader serviceDownloader = new UDDIBusinessServiceDownloader(uddiClient);

        Set<String> serviceKeys = new HashSet<String>();
        serviceKeys.add("uddi:ad3618e0-d229-11df-a331-7bda57c0a325");
        final List<Pair<BusinessService, Map<String, TModel>>> modelFromUddi = serviceDownloader.getBusinessServiceModels(serviceKeys);
//        Pair<List<BusinessService>, Map<String, TModel>> modelFromUddi = serviceDownloader.getBusinessServiceModels(serviceKeys);
        Assert.assertEquals(1, modelFromUddi.size());

        final Pair<BusinessService, Map<String, TModel>> serviceMapPair = modelFromUddi.iterator().next();
        JAXB.marshal(serviceMapPair.left, new File("/home/darmstrong/ideaprojects/UneasyRooster/modules/uddi/src/test/resources/com/l7tech/uddi/ArtistRegistryService.xml"));
        int i = 0;
        for(TModel model: serviceMapPair.right.values()) {
            JAXB.marshal(model, new File("/home/darmstrong/ideaprojects/UneasyRooster/modules/uddi/src/test/resources/com/l7tech/uddi/ArtistRegistryServiceTModel" + i + ".xml"));
            i++;
        }

    }

}
