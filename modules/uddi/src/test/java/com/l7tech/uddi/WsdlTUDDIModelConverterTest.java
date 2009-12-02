package com.l7tech.uddi;

import org.junit.Test;
import com.l7tech.wsdl.Wsdl;
import com.l7tech.common.uddi.guddiv3.*;
import com.l7tech.util.Pair;
import com.l7tech.test.BugNumber;

import java.io.*;
import java.util.*;

import junit.framework.Assert;

import javax.xml.bind.JAXB;

/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 *
 * Test the converstion from the WSDL data model to the UDDI information model
 * @author darmstrong
 */
public class WsdlTUDDIModelConverterTest {

    /**
     * Tests that each wsdl:service converted to a UDDI BusinessService only references tModels unique to it
     *
     */
    @Test
    @BugNumber(7970)
    public void testUniqueTModelPerService() throws Exception{
        Wsdl wsdl = Wsdl.newInstance(null, getWsdlReader( "com/l7tech/uddi/WarehouseTwoServices.wsdl" ));

        final String gatewayWsdlUrl = "http://localhost:8080/3828382?wsdl";
        final String gatewayURL = "http://localhost:8080/3828382";
        Pair<String, String> endpointPair = new Pair<String, String>(gatewayURL, gatewayWsdlUrl);

        final int serviceOid = 3828382;
        WsdlToUDDIModelConverter wsdlToUDDIModelConverter = new WsdlToUDDIModelConverter(wsdl, "uddi:uddi_business_key");
        wsdlToUDDIModelConverter.convertWsdlToUDDIModel(Arrays.asList(endpointPair), "Layer7", Long.toString(serviceOid));

        final List<Pair<BusinessService, Map<String, TModel>>> serviceToDependentModels = wsdlToUDDIModelConverter.getServicesAndDependentTModels();

        Assert.assertEquals("Incorrect number of services found", 2, serviceToDependentModels.size());
        Set<String> allTModelKeys = new HashSet<String>();
        for(Pair<BusinessService, Map<String, TModel>> serviceToModels: serviceToDependentModels){
            for(TModel m: serviceToModels.right.values()){
                allTModelKeys.add(m.toString());
            }
        }
        //2 services. Each service has two wsdl:ports. Each wsdlPort requires 2 tModels = 2 * 2 * 2 = 8
        Assert.assertEquals("Incorrect number of unique tModels found", 8, allTModelKeys.size());

    }

    /**
     * Same test as above, but with 2 endpoints
     * @throws Exception
     */
    @Test
    public void testUniqueTModelPerServiceTwoEndpoints() throws Exception{
        Wsdl wsdl = Wsdl.newInstance(null, getWsdlReader( "com/l7tech/uddi/Warehouse.wsdl" ));

        final String gatewayWsdlUrl = "http://localhost:8080/3828382?wsdl";
        final String gatewayURL = "http://localhost:8080/3828382";
        Pair<String, String> endpointPair = new Pair<String, String>(gatewayURL, gatewayWsdlUrl);

        final String gatewayURHttps = "https://localhost:8080/3828382";
        Pair<String, String> endpointPairHttps = new Pair<String, String>(gatewayURHttps, gatewayWsdlUrl);

        final int serviceOid = 3828382;
        WsdlToUDDIModelConverter wsdlToUDDIModelConverter = new WsdlToUDDIModelConverter(wsdl, "uddi:uddi_business_key");
        wsdlToUDDIModelConverter.convertWsdlToUDDIModel(Arrays.asList(endpointPair, endpointPairHttps), "Layer7", Long.toString(serviceOid));

        final List<Pair<BusinessService, Map<String, TModel>>> serviceToDependentModels = wsdlToUDDIModelConverter.getServicesAndDependentTModels();

        Assert.assertEquals("Incorrect number of services found", 1, serviceToDependentModels.size());
        Set<String> allTModelKeys = new HashSet<String>();
        for(Pair<BusinessService, Map<String, TModel>> serviceToModels: serviceToDependentModels){
            for(TModel m: serviceToModels.right.values()){
                allTModelKeys.add(m.toString());
            }
        }
        //1 service. Each service has two wsdl:ports, and each wsdl:port should be represented twice, 1 per endpoint.
        //Each wsdlPort requires 2 tModels = 1 * 2 * 2 * 2 (endpoints) =
        Assert.assertEquals("Incorrect number of unique tModels found", 8, allTModelKeys.size());
    }

    /**
     * Same test as above, but with 2 endpoints
     * @throws Exception
     */
    @Test
    public void testUniqueTModelPerServiceTwoEndpointsTwoServices() throws Exception{
        Wsdl wsdl = Wsdl.newInstance(null, getWsdlReader( "com/l7tech/uddi/WarehouseTwoServices.wsdl" ));

        final String gatewayWsdlUrl = "http://localhost:8080/3828382?wsdl";
        final String gatewayURL = "http://localhost:8080/3828382";
        Pair<String, String> endpointPair = new Pair<String, String>(gatewayURL, gatewayWsdlUrl);

        final String gatewayURHttps = "https://localhost:8080/3828382";
        Pair<String, String> endpointPairHttps = new Pair<String, String>(gatewayURHttps, gatewayWsdlUrl);

        final int serviceOid = 3828382;
        WsdlToUDDIModelConverter wsdlToUDDIModelConverter = new WsdlToUDDIModelConverter(wsdl, "uddi:uddi_business_key");
        wsdlToUDDIModelConverter.convertWsdlToUDDIModel(Arrays.asList(endpointPair, endpointPairHttps), "Layer7", Long.toString(serviceOid));

        final List<Pair<BusinessService, Map<String, TModel>>> serviceToDependentModels = wsdlToUDDIModelConverter.getServicesAndDependentTModels();

        Assert.assertEquals("Incorrect number of services found", 2, serviceToDependentModels.size());
        Set<String> allTModelKeys = new HashSet<String>();
        for(Pair<BusinessService, Map<String, TModel>> serviceToModels: serviceToDependentModels){
            for(TModel m: serviceToModels.right.values()){
                allTModelKeys.add(m.toString());
            }
        }
        //1 service. Each service has two wsdl:ports, and each wsdl:port should be represented twice, 1 per endpoint.
        //Each wsdlPort requires 2 tModels = 1 * 2 * 2 * 2 (endpoints) =
        Assert.assertEquals("Incorrect number of unique tModels found", 16, allTModelKeys.size());
    }
    /**
     * Tests that the Warehouse wsdl converts successfully and that the correct number of wsdl's and dependent
     * tmodels are found
     * @throws Exception
     */
    @Test
    public void testBasicConvert() throws Exception{
        Wsdl wsdl = Wsdl.newInstance(null, WsdlTUDDIModelConverterTest.getWsdlReader( "com/l7tech/uddi/Warehouse.wsdl" ));

        final String gatewayWsdlUrl = "http://localhost:8080/3828382?wsdl";
        final String gatewayURL = "http://localhost:8080/3828382";
        Pair<String, String> endpointPair = new Pair<String, String>(gatewayURL, gatewayWsdlUrl);

        final int serviceOid = 3828382;
        final String businessKey = "uddi:uddi_business_key";
        WsdlToUDDIModelConverter wsdlToUDDIModelConverter = new WsdlToUDDIModelConverter(wsdl, businessKey);
        wsdlToUDDIModelConverter.convertWsdlToUDDIModel(Arrays.asList(endpointPair), "Layer7", Long.toString(serviceOid));

        final List<Pair<BusinessService, Map<String, TModel>>> serviceToDependentModels = wsdlToUDDIModelConverter.getServicesAndDependentTModels();
        Assert.assertNotNull(serviceToDependentModels);
        Assert.assertEquals("Incorrect number of business services pairs found", 1, serviceToDependentModels.size());
        final Pair<BusinessService, Map<String, TModel>> serviceMapPair = serviceToDependentModels.iterator().next();
        Assert.assertEquals("Incorrect number of dependent tmodels found", 4, serviceMapPair.right.size());
    }
    
    /**
     * This entire test case depends on the structure of the Warehoumse.wsdl resource
     * @throws Exception
     */
    @Test
    public void testWsdlToUddiConverstionEndPoint() throws Exception{

        Wsdl wsdl = Wsdl.newInstance(null, getWsdlReader( "com/l7tech/uddi/Warehouse.wsdl" ));

        final String gatewayWsdlUrl = "http://localhost:8080/3828382?wsdl";
        final String gatewayURL = "http://localhost:8080/3828382";
        Pair<String, String> endpointPair = new Pair<String, String>(gatewayURL, gatewayWsdlUrl);

        final String targetNameSpace = wsdl.getTargetNamespace();

        final int serviceOid = 3828382;
        WsdlToUDDIModelConverter wsdlToUDDIModelConverter = new WsdlToUDDIModelConverter(wsdl, "uddi:uddi_business_key");
        wsdlToUDDIModelConverter.convertWsdlToUDDIModel(Arrays.asList(endpointPair), "Layer7", Long.toString(serviceOid));

        final List<Pair<BusinessService, Map<String, TModel>>> serviceToDependentModels = wsdlToUDDIModelConverter.getServicesAndDependentTModels();

        Assert.assertEquals("Incorrect number of Business Services found", 1, serviceToDependentModels.size());

        Map<String, TModel> keysToTModels = serviceToDependentModels.get(0).right;
        //1 service with 2 wsdl:port. Each wsdl:port requries 2 tmodels = 1 * 2 * 2 = 4
        Assert.assertEquals("Incorrect number of tModels published", 4, keysToTModels.keySet().size());

        for(Map.Entry<String, TModel> entry: keysToTModels.entrySet()) {
            System.out.println("tModelKey: " + entry.getKey());
            JAXB.marshal(entry.getValue(), System.out);
        }

        BusinessService businessService = serviceToDependentModels.get(0).left;
        Assert.assertEquals("Incorrect Business Service name", "Layer7 Warehouse " + serviceOid, businessService.getName().get(0).getValue());

        List<BindingTemplate> bindingTemplates = businessService.getBindingTemplates().getBindingTemplate();
        Assert.assertEquals("Incorrect number of bindingTemplates found", 2, bindingTemplates.size());

        String[] bindingNames = new String[]{"WarehouseSoap12", "WarehouseSoap"};
        for (int i = 0; i < bindingTemplates.size(); i++) {
            BindingTemplate bindingTemplate = bindingTemplates.get(i);
            Assert.assertEquals("Incorrect use type found", "endPoint", bindingTemplate.getAccessPoint().getUseType());
            Assert.assertEquals("Incorrect gateway url found", gatewayURL, bindingTemplate.getAccessPoint().getValue());
            TModelInstanceDetails tModelInstanceDetails = bindingTemplate.getTModelInstanceDetails();
            List<TModelInstanceInfo> tModelInstanceInfos = tModelInstanceDetails.getTModelInstanceInfo();
            Assert.assertEquals("Incorrect number of TModelInstanceInfo found", 2, tModelInstanceInfos.size());
            TModelInstanceInfo bindingInfo = tModelInstanceInfos.get(0);
            InstanceDetails bindingDetails = bindingInfo.getInstanceDetails();
            Assert.assertEquals("Incorrect number of Description found", 1, bindingDetails.getDescription().size());
            Assert.assertEquals("Incorrect description found", "the wsdl:binding that this wsdl:port implements", bindingDetails.getDescription().get(0).getValue());
            Assert.assertEquals("Incorrect binding found as instance param", bindingNames[i], bindingDetails.getInstanceParms());

            TModelInstanceInfo portTypeInfo = tModelInstanceInfos.get(1);

            testBindingTModel(keysToTModels.get(bindingInfo.getTModelKey()),
                    bindingDetails.getInstanceParms(),
                    targetNameSpace, portTypeInfo.getTModelKey(), gatewayWsdlUrl);

            testPortTypeTModel(keysToTModels.get(portTypeInfo.getTModelKey()), "WarehouseSoap", gatewayWsdlUrl, targetNameSpace);
        }

        CategoryBag categoryBag = businessService.getCategoryBag();
        List<KeyedReference> keyedReferences = categoryBag.getKeyedReference();
        Assert.assertEquals("Incorrect number of keyedReferences found", 3, keyedReferences.size());

        KeyedReference serviceTypeRef = keyedReferences.get(0);
        Assert.assertEquals("Incorrect keyValue found", "service", serviceTypeRef.getKeyValue());
        Assert.assertEquals("Incorret tModelKey found", WsdlToUDDIModelConverter.UDDI_WSDL_TYPES, serviceTypeRef.getTModelKey());

        KeyedReference serviceLocalName = keyedReferences.get(1);
        Assert.assertEquals("Incorrect keyValue found", "Warehouse", serviceLocalName.getKeyValue());
        Assert.assertEquals("Incorrect keyName found", "service local name", serviceLocalName.getKeyName());
        Assert.assertEquals("Incorret tModelKey found", WsdlToUDDIModelConverter.UDDI_XML_LOCALNAME, serviceLocalName.getTModelKey());

        KeyedReference nameSpaceRef = keyedReferences.get(2);
        Assert.assertEquals("Incorrect keyValue found", targetNameSpace, nameSpaceRef.getKeyValue());
        Assert.assertEquals("Incorrect keyName found", "service namespace", nameSpaceRef.getKeyName());
        Assert.assertEquals("Incorret tModelKey found", WsdlToUDDIModelConverter.UDDI_XML_NAMESPACE, nameSpaceRef.getTModelKey());
    }

    /**
     * Tests that http:address is supported. - does not cause exception if encountered, the port itself is ignored
     * @throws Exception
     */
    @BugNumber(7925)
    @Test
    public void testHttpAddressSupport() throws Exception{
        Wsdl wsdl = Wsdl.newInstance(null, getWsdlReader( "com/l7tech/uddi/httpaddress.wsdl" ));

        final String gatewayWsdlUrl = "http://localhost:8080/3828382?wsdl";
        final String gatewayURL = "http://localhost:8080/3828382";
        Pair<String, String> endpointPair = new Pair<String, String>(gatewayURL, gatewayWsdlUrl);

        final int serviceOid = 3828382;
        WsdlToUDDIModelConverter wsdlToUDDIModelConverter = new WsdlToUDDIModelConverter(wsdl, "uddi:uddi_business_key");
        wsdlToUDDIModelConverter.convertWsdlToUDDIModel(Arrays.asList(endpointPair), "Layer7", Long.toString(serviceOid));

        final List<Pair<BusinessService, Map<String, TModel>>> serviceToDependentModels = wsdlToUDDIModelConverter.getServicesAndDependentTModels();
        Assert.assertEquals("Incorrect number of Business Services found", 1, serviceToDependentModels.size());
        //bug is fixed, can successfully convert into UDDI data model
    }

    /**
     * Tests that a wsdl with no soap bindings causes the correct exception
     * @throws Exception
     */
    @Test (expected = WsdlToUDDIModelConverter.MissingWsdlReferenceException.class)
    public void testHttpAddressException() throws Exception{
        Wsdl wsdl = Wsdl.newInstance(null, getWsdlReader( "com/l7tech/uddi/nosoapbindings.wsdl" ));

        final String gatewayWsdlUrl = "http://localhost:8080/3828382?wsdl";
        final String gatewayURL = "http://localhost:8080/3828382";
        Pair<String, String> endpointPair = new Pair<String, String>(gatewayURL, gatewayWsdlUrl);
        
        final int serviceOid = 3828382;
        WsdlToUDDIModelConverter wsdlToUDDIModelConverter = new WsdlToUDDIModelConverter(wsdl, "uddi:uddi_business_key");
        wsdlToUDDIModelConverter.convertWsdlToUDDIModel(Arrays.asList(endpointPair), "Layer7", Long.toString(serviceOid));

        final List<Pair<BusinessService, Map<String, TModel>>> serviceToDependentModels = wsdlToUDDIModelConverter.getServicesAndDependentTModels();
        Assert.assertEquals("Incorrect number of Business Services found", 1, serviceToDependentModels.size());
        //bug is fixed, can successfully convert into UDDI data model
    }

    @BugNumber(7930)
    @Test(expected = WsdlToUDDIModelConverter.MissingWsdlReferenceException.class)
    public void testProblemWsdl() throws Exception{
        Wsdl wsdl = Wsdl.newInstance(null, getWsdlReader( "com/l7tech/uddi/problemwsdl.wsdl" ));

        final String gatewayWsdlUrl = "http://localhost:8080/3828382?wsdl";
        final String gatewayURL = "http://localhost:8080/3828382";
        Pair<String, String> endpointPair = new Pair<String, String>(gatewayURL, gatewayWsdlUrl);

        final int serviceOid = 3828382;
        WsdlToUDDIModelConverter wsdlToUDDIModelConverter = new WsdlToUDDIModelConverter(wsdl, "uddi:uddi_business_key");
        wsdlToUDDIModelConverter.convertWsdlToUDDIModel(Arrays.asList(endpointPair), "Layer7", Long.toString(serviceOid));
        final List<Pair<BusinessService, Map<String, TModel>>> models = wsdlToUDDIModelConverter.getServicesAndDependentTModels();
    }

    /**
     * Similar to bug above which tests the no valid wsdl:service case, this test tests that an invalid wsdl:service
     * is ignored, when at least one valid wsdl:service is defined
     * @throws Exception
     */
    @BugNumber(7930)
    @Test
    public void testProblemWsdlConverts() throws Exception{
        Wsdl wsdl = Wsdl.newInstance(null, getWsdlReader( "com/l7tech/uddi/problemWsdlOneValidService.wsdl" ));

        final String gatewayWsdlUrl = "http://localhost:8080/3828382?wsdl";
        final String gatewayURL = "http://localhost:8080/3828382";
        Pair<String, String> endpointPair = new Pair<String, String>(gatewayURL, gatewayWsdlUrl);

        final int serviceOid = 3828382;
        WsdlToUDDIModelConverter wsdlToUDDIModelConverter = new WsdlToUDDIModelConverter(wsdl, "uddi:uddi_business_key");
        wsdlToUDDIModelConverter.convertWsdlToUDDIModel(Arrays.asList(endpointPair), "Layer7", Long.toString(serviceOid));
        final List<Pair<BusinessService, Map<String, TModel>>> serviceToDependentModels = wsdlToUDDIModelConverter.getServicesAndDependentTModels();
        Assert.assertEquals("Incorrect number of BusinessServices found", 1, serviceToDependentModels.size());
    }


    /**
     * Confirms that the tModel created confirms to
     * http://www.oasis-open.org/committees/uddi-spec/doc/tn/uddi-spec-tc-tn-wsdl-v2.htm#_Toc76437775
     * 
     */
    private void testPortTypeTModel(TModel tModel, String localName, String gatewayWsdlUrl, String targetNameSpace){
        Assert.assertEquals("Incorrect tModel name found", localName , tModel.getName().getValue());
        //No longer setting key as it's unnecessary
//        Assert.assertEquals("Incorrect tModel key found", tModelKey, tModel.getTModelKey());

        List<OverviewDoc> overviewDocs = tModel.getOverviewDoc();
        Assert.assertEquals("Incorrect number of overviewDocs found", 2, overviewDocs.size());

        OverviewDoc wsdlDoc = overviewDocs.get(0);
        Assert.assertEquals("Incorrect description found", "the original WSDL document", wsdlDoc.getDescription().get(0).getValue());
        OverviewURL wsdlUrl = wsdlDoc.getOverviewURL();
        Assert.assertEquals("Incorrect useType found", "wsdlInterface", wsdlUrl.getUseType());
        Assert.assertEquals("Incorrect wsdl URL found", gatewayWsdlUrl, wsdlUrl.getValue());

        OverviewDoc techNotedoc = overviewDocs.get(1);
        Assert.assertEquals("Incorrect description found", "Technical Note \"Using WSDL in a UDDI Registry, Version 2.0.2\"", techNotedoc.getDescription().get(0).getValue());
        OverviewURL textUrl = techNotedoc.getOverviewURL();
        Assert.assertEquals("Incorrect useType found", "text", textUrl.getUseType());
        Assert.assertEquals("Incorrect wsdl URL found", "http://www.oasis-open.org/committees/uddi-spec/doc/tn/uddi-spec-tc-tn-wsdl-v202-20040631.htm", textUrl.getValue());

        CategoryBag categoryBag = tModel.getCategoryBag();
        List<KeyedReference> keyedReferences = categoryBag.getKeyedReference();
        Assert.assertEquals("Incorrect number of keyedReferences found", 2, keyedReferences.size());

        KeyedReference portTypeRef = keyedReferences.get(0);
        Assert.assertEquals("Incorrect keyValue found", "portType", portTypeRef.getKeyValue());
        Assert.assertEquals("Incorret tModelKey found", WsdlToUDDIModelConverter.UDDI_WSDL_TYPES, portTypeRef.getTModelKey());

        KeyedReference nameSpaceRef = keyedReferences.get(1);
        Assert.assertEquals("Incorrect keyValue found", targetNameSpace, nameSpaceRef.getKeyValue());
        Assert.assertEquals("Incorrect keyName found", "portType namespace", nameSpaceRef.getKeyName());
        Assert.assertEquals("Incorret tModelKey found", WsdlToUDDIModelConverter.UDDI_XML_NAMESPACE, nameSpaceRef.getTModelKey());
    }

    /**
     * Confirms that the tModel created to represent the binding conforms to
     * http://www.oasis-open.org/committees/uddi-spec/doc/tn/uddi-spec-tc-tn-wsdl-v2.htm#_Toc76437776
     * 
     */
    private void testBindingTModel(TModel tModel,
                                   String localName,
                                   String targetNameSpace,
                                   String portTypeTModelKey,
                                   String gatewayWsdlUrl){

        Assert.assertEquals("Incorrect tModel name found", localName , tModel.getName().getValue());
        //no longer setting key as unnecesssary
        //Assert.assertEquals("Incorrect tModel key found", tModelKey, tModel.getTModelKey());

        List<OverviewDoc> overviewDocs = tModel.getOverviewDoc();
        Assert.assertEquals("Incorrect number of overviewDocs found", 2, overviewDocs.size());

        OverviewDoc wsdlDoc = overviewDocs.get(0);
        Assert.assertEquals("Incorrect description found", "the original WSDL document", wsdlDoc.getDescription().get(0).getValue());
        OverviewURL wsdlUrl = wsdlDoc.getOverviewURL();
        Assert.assertEquals("Incorrect useType found", "wsdlInterface", wsdlUrl.getUseType());
        Assert.assertEquals("Incorrect wsdl URL found", gatewayWsdlUrl, wsdlUrl.getValue());

        OverviewDoc techNotedoc = overviewDocs.get(1);
        Assert.assertEquals("Incorrect description found", "Technical Note \"Using WSDL in a UDDI Registry, Version 2.0.2\"", techNotedoc.getDescription().get(0).getValue());
        OverviewURL textUrl = techNotedoc.getOverviewURL();
        Assert.assertEquals("Incorrect useType found", "text", textUrl.getUseType());
        Assert.assertEquals("Incorrect wsdl URL found", "http://www.oasis-open.org/committees/uddi-spec/doc/tn/uddi-spec-tc-tn-wsdl-v202-20040631.htm", textUrl.getValue());

        CategoryBag categoryBag = tModel.getCategoryBag();
        List<KeyedReference> keyedReferences = categoryBag.getKeyedReference();
        Assert.assertEquals("Incorrect number of keyedReferences found", 6, keyedReferences.size());

        KeyedReference bindingTypeRef = keyedReferences.get(0);
        Assert.assertEquals("Incorrect keyValue found", "binding", bindingTypeRef.getKeyValue());
        Assert.assertEquals("Incorret tModelKey found", WsdlToUDDIModelConverter.UDDI_WSDL_TYPES, bindingTypeRef.getTModelKey());

        KeyedReference portTypeRef = keyedReferences.get(1);
        Assert.assertEquals("Incorrect portType tModelKey found in keyValue", portTypeTModelKey, portTypeRef.getKeyValue());
        Assert.assertEquals("Incorrect keyName found", "portType reference", portTypeRef.getKeyName());
        Assert.assertEquals("Incorret tModelKey found", WsdlToUDDIModelConverter.UDDI_WSDL_PORTTYPEREFERENCE, portTypeRef.getTModelKey());

        KeyedReference wsdlSpecRef = keyedReferences.get(2);
        Assert.assertEquals("Incorrect keyValue", "wsdlSpec", wsdlSpecRef.getKeyValue());
        Assert.assertEquals("Incorrect tModelKey found", wsdlSpecRef.getTModelKey(), WsdlToUDDIModelConverter.UDDI_CATEGORIZATION_TYPES);

        KeyedReference nameSpaceRef = keyedReferences.get(3);
        Assert.assertEquals("Incorrect keyValue found", targetNameSpace, nameSpaceRef.getKeyValue());
        Assert.assertEquals("Incorrect keyName found", "binding namespace", nameSpaceRef.getKeyName());
        Assert.assertEquals("Incorret tModelKey found", WsdlToUDDIModelConverter.UDDI_XML_NAMESPACE, nameSpaceRef.getTModelKey());

        KeyedReference protocolSoapRef = keyedReferences.get(4);
        Assert.assertEquals("Incorrect keyValue found", WsdlToUDDIModelConverter.SOAP_PROTOCOL_V3, protocolSoapRef.getKeyValue());
        Assert.assertEquals("Incorrect keyName found", "SOAP protocol", protocolSoapRef.getKeyName());
        Assert.assertEquals("Incorret tModelKey found", WsdlToUDDIModelConverter.UDDI_WSDL_CATEGORIZATION_PROTOCOL, protocolSoapRef.getTModelKey());

        KeyedReference transportHttpRef = keyedReferences.get(5);
        Assert.assertEquals("Incorrect keyValue found", WsdlToUDDIModelConverter.HTTP_TRANSPORT_V3, transportHttpRef.getKeyValue());
        Assert.assertEquals("Incorrect keyName found", "HTTP transport", transportHttpRef.getKeyName());
        Assert.assertEquals("Incorret tModelKey found", WsdlToUDDIModelConverter.UDDI_WSDL_CATEGORIZATION_TRANSPORT, transportHttpRef.getTModelKey());
    }

    private void printBusinessService(List<BusinessService> services){
        for(BusinessService bs: services){
            JAXB.marshal(bs, System.out);
        }
    }

    public static Reader getWsdlReader(String resourcetoread) {
        InputStream inputStream = WsdlTUDDIModelConverterTest.class.getClassLoader().getResourceAsStream(resourcetoread);
        return new InputStreamReader(inputStream);
    }
}
