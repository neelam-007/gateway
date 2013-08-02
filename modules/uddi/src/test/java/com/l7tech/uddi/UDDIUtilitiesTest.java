package com.l7tech.uddi;

import com.l7tech.common.uddi.guddiv3.*;
import com.l7tech.util.Pair;
import com.l7tech.util.SyspropUtil;
import com.l7tech.wsdl.Wsdl;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.wsdl.WSDLException;
import javax.xml.bind.JAXB;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.logging.LogManager;

import static com.l7tech.uddi.UDDIUtilities.TMODEL_TYPE.WSDL_BINDING;
import static com.l7tech.uddi.UDDIUtilities.TMODEL_TYPE.WSDL_PORT_TYPE;

/**
 * @author darmstrong
 */
public class UDDIUtilitiesTest {
    private Wsdl wsdl;
    private WsdlToUDDIModelConverter wsdlToUDDIModelConverter;
    private List<Pair<BusinessService, Map<String, TModel>>> serviceToDependentModels;
    private Map<String, TModel> referencedModels;
    private BusinessService singleService;

    @Before
    public void setUp() throws Exception {

        wsdl = Wsdl.newInstance(null, WsdlTUDDIModelConverterTest.getWsdlReader( "com/l7tech/uddi/Warehouse.wsdl" ));

        final String gatewayWsdlUrl = "http://localhost:8080/3828382?wsdl";
        final String gatewayURL = "http://localhost:8080/3828382";
        EndpointPair endpointPair = new EndpointPair(gatewayURL, gatewayWsdlUrl);

        final int serviceOid = 3828382;
        wsdlToUDDIModelConverter = new WsdlToUDDIModelConverter(wsdl, "uddi:uddi_business_key");
        wsdlToUDDIModelConverter.convertWsdlToUDDIModel(Arrays.asList(endpointPair), "Layer7", Long.toString(serviceOid));
        serviceToDependentModels = wsdlToUDDIModelConverter.getServicesAndDependentTModels();
        referencedModels = serviceToDependentModels.get(0).right;
        singleService = serviceToDependentModels.get(0).left;
    }

    @AfterClass
    public static void cleanupSystemProperties() {
        SyspropUtil.clearProperties(
            "com.l7tech.uddi.UDDIUtilities.fallBackOnFirstImplentingWsdlPort"
        );
    }

    /**
     * Test that references are correctly updated for all BusinessServices
     * @throws Exception
     */
    @Test
    public void testUDDIReferenceUpdater() throws Exception {
        final Map<String, TModel> tModels = serviceToDependentModels.get(0).right;
        for(TModel tModel: tModels.values()){
            UDDIUtilities.TMODEL_TYPE type = UDDIUtilities.getTModelType(tModel, true);
            final CategoryBag categoryBag = tModel.getCategoryBag();
            List<KeyedReference> keyedReferences = categoryBag.getKeyedReference();
            for (KeyedReference keyedReference : keyedReferences) {
                if (!keyedReference.getTModelKey().equalsIgnoreCase(WsdlToUDDIModelConverter.UDDI_WSDL_TYPES)) continue;
                final String keyValue = keyedReference.getKeyValue();
                if (keyValue.equals("portType")) {
                    Assert.assertEquals("Incorrect tModel type found", WSDL_PORT_TYPE, type);
                } else if (keyValue.equals("binding")) {
                    Assert.assertEquals("Incorrect tModel type found", WSDL_BINDING, type);
                }
            }
        }
    }

    /**
     * Test the updateBindingTModelReferences method
     *
     * Tests that the binding template tmodels are updated
     */
    @Test
    public void testUpdateBindingTModelReferences() throws UDDIException {
        //first update all wsdl:portType tModels
        BusinessServicePublisher servicePublisher = new BusinessServicePublisher(wsdl, "23424323", getUDDIClient());
        servicePublisher.publishDependentTModels(referencedModels, WSDL_PORT_TYPE, Collections.<Pair<String, BusinessService>>emptySet());

        final ArrayList<TModel> models = new ArrayList<TModel>();
        models.addAll(referencedModels.values());
        UDDIUtilities.updateBindingTModelReferences(models, referencedModels);

        int numBindingTModelsFound = 0;
        //Now each binding tModel should have a real tModelKey in it's keyedReference to the wsdl:portType tModel
        for(TModel tModel: referencedModels.values()){
            if(UDDIUtilities.getTModelType(tModel, true) != WSDL_BINDING) continue;
            numBindingTModelsFound++;
            //get it's portType keyedReference
            final CategoryBag categoryBag = tModel.getCategoryBag();
            List<KeyedReference> keyedReferences = categoryBag.getKeyedReference();
            //find the portType keyedReference
            boolean portKeyedReferenceFound = false;
            for (KeyedReference keyedReference : keyedReferences) {
                if (!keyedReference.getTModelKey().equalsIgnoreCase(WsdlToUDDIModelConverter.UDDI_WSDL_PORTTYPEREFERENCE)) continue;
                portKeyedReferenceFound = true;
                final String keyValue = keyedReference.getKeyValue();
                Assert.assertFalse("wsdl:portType reference was not updated correctly: " + keyValue, keyValue.endsWith(WsdlToUDDIModelConverter.PORT_TMODEL_IDENTIFIER));

                //now find a portType tModel with this tModelKey
                boolean refFound = false;
                for(TModel aTModel: referencedModels.values()){
                    if(UDDIUtilities.getTModelType(aTModel, true) != WSDL_PORT_TYPE) continue;
                    if(!aTModel.getTModelKey().equals(keyValue)) continue;
                    refFound = true;
                }
                Assert.assertTrue("wsdl:portType tModel referenced not found", refFound);

            }
            Assert.assertTrue("No wsdl:portType keyedreference found", portKeyedReferenceFound);
        }
        Assert.assertEquals("Incorrect number of binding tmodels found", 2, numBindingTModelsFound);
    }


    /**
     * Tests that the binding templates contained by BusinessService's are correctly updated with correct tModelKey
     * in keyedreferences to tmodels representing wsdl:portType and wsdl:binding
     * @throws UDDIException
     */
    @Test
    public void testUpdateBusinessServiceReferences() throws UDDIException {
        //setup
        BusinessServicePublisher servicePublisher = new BusinessServicePublisher(wsdl, "23424323", getUDDIClient());
        servicePublisher.publishDependentTModels(referencedModels, WSDL_PORT_TYPE, Collections.<Pair<String, BusinessService>>emptySet());

        final ArrayList<TModel> models = new ArrayList<TModel>();
        models.addAll(referencedModels.values());
        UDDIUtilities.updateBindingTModelReferences(models, referencedModels);

        servicePublisher.publishDependentTModels(referencedModels, WSDL_BINDING, Collections.<Pair<String, BusinessService>>emptySet());
        //finish setup

        UDDIUtilities.updateBusinessServiceReferences(singleService, referencedModels);
        
        JAXB.marshal(singleService, System.out);
        BindingTemplates bindingTemplates = singleService.getBindingTemplates();
        List<BindingTemplate> allTemplates = bindingTemplates.getBindingTemplate();
        for (BindingTemplate bindingTemplate : allTemplates) {
            int numUpdatedReferences = 0;
            TModelInstanceDetails tModelInstanceDetails = bindingTemplate.getTModelInstanceDetails();
            List<TModelInstanceInfo> tModelInstanceInfos = tModelInstanceDetails.getTModelInstanceInfo();
            for (TModelInstanceInfo tModelInstanceInfo : tModelInstanceInfos) {
                final String modelKey = tModelInstanceInfo.getTModelKey();
                Assert.assertFalse("Map should not contain the keyedReference: " + modelKey,
                        referencedModels.containsKey(modelKey));
                numUpdatedReferences++;
                //confirm the tModelKey found is valid and exists in the correct type of tModel
                final UDDIUtilities.TMODEL_TYPE currentTModelInstanceInfoType =
                        (tModelInstanceInfo.getInstanceDetails() != null)? WSDL_BINDING: WSDL_PORT_TYPE;
                //iterate through all bindings, find the tModel, confirm it's key and value
                boolean tModelReferencedFound = false;

                for(TModel aRefTModel: referencedModels.values()){
                    if(!aRefTModel.getTModelKey().equals(modelKey)) continue;
                    UDDIUtilities.TMODEL_TYPE modelType = UDDIUtilities.getTModelType(aRefTModel, true);
                    if(modelType != currentTModelInstanceInfoType) continue;
                    tModelReferencedFound = true;
                }

                Assert.assertTrue("Referenced tModel not found", tModelReferencedFound);
            }
            Assert.assertEquals("Incorrect number of references updated for bindingTemplate", 2, numUpdatedReferences);
        }

    }

    @Test
    public void testValidatePortBelongsToWsdl() throws WSDLException {

        final String namespace = "http://warehouse.acme.com/ws";
        Assert.assertTrue("Should be valid, all info supplied",
                UDDIUtilities.validatePortBelongsToWsdl(wsdl, "Warehouse", namespace, "WarehouseSoap" , "WarehouseSoap", namespace));

        Assert.assertTrue("Should be valid, even with no namespace",
                UDDIUtilities.validatePortBelongsToWsdl(wsdl, "Warehouse", null, "WarehouseSoap", "WarehouseSoap", ""));

        Assert.assertFalse("Should be invalid due to namespace supplied",
                UDDIUtilities.validatePortBelongsToWsdl(wsdl, "Warehouse", namespace+"1", "WarehouseSoap" , "WarehouseSoap", namespace));

        Assert.assertFalse("Should be invalid due to namespace supplied",
                UDDIUtilities.validatePortBelongsToWsdl(wsdl, "Warehouse", namespace, "WarehouseSoap" , "WarehouseSoap", namespace+"1"));

        Assert.assertFalse("Should be invalid due to service name supplied",
                UDDIUtilities.validatePortBelongsToWsdl(wsdl, "Warehouse1", namespace, "WarehouseSoap" , "WarehouseSoap", namespace));

        Assert.assertFalse("Should be invalid due to wsdl port name supplied",
                UDDIUtilities.validatePortBelongsToWsdl(wsdl, "Warehouse", namespace, "WarehouseSoap1" , "WarehouseSoap", namespace));

        Assert.assertFalse("Should be invalid due to wsdl binding name supplied",
                UDDIUtilities.validatePortBelongsToWsdl(wsdl, "Warehouse", namespace, "WarehouseSoap" , "WarehouseSoap1", namespace));

        //tests imports work correctly
        final Wsdl importWsdl = Wsdl.newInstance(null, WsdlTUDDIModelConverterTest.getWsdlReader( "com/l7tech/uddi/SpaceOrderofBattle_Parent.wsdl" ));
        final String spaceNs = "http://SOB.IPD.LMCO/";
        final String childNs = "http://SOB.IPD.LMCO/ExternalWSDL";
        Assert.assertTrue("Testing binding defined in child wsdl implement by a parent wsdl:port",
                UDDIUtilities.validatePortBelongsToWsdl(importWsdl, "SpaceOrderofBattle", spaceNs, "JMSSOB1" , "JMSSOB1", childNs));

        Assert.assertTrue("Testing binding defined with same name in parent, but different namespace",
                UDDIUtilities.validatePortBelongsToWsdl(importWsdl, "SpaceOrderofBattle", spaceNs, "JMSSOB" , "JMSSOB", spaceNs));

        Assert.assertFalse("Testing binding defined with same name in parent, incorrect binding namespace, provided child namespace which the port does not implement",
                UDDIUtilities.validatePortBelongsToWsdl(importWsdl, "SpaceOrderofBattle", spaceNs, "JMSSOB" , "JMSSOB", childNs));

        Assert.assertFalse("Testing binding defined with same name in parent, but is not implement by any wsdl:port",
                UDDIUtilities.validatePortBelongsToWsdl(importWsdl, "SpaceOrderofBattle", spaceNs, "JMSSOB" , "JMSSOB", childNs));

        Assert.assertTrue("Testing that true is returend when no namespace is supplied, inaccurate match",
                UDDIUtilities.validatePortBelongsToWsdl(importWsdl, "SpaceOrderofBattle", spaceNs, "JMSSOB1" , "JMSSOB1", null));

    }

    @Test
    public void testGetEndPoint() throws Exception {
        String endPoint = UDDIUtilities.extractEndPointFromWsdl(wsdl, "Warehouse", "WarehouseSoap", "WarehouseSoap");
        System.out.println(endPoint);
        Assert.assertEquals("Invalid end point found", "http://hugh/ACMEWarehouseWS/Service1.asmx", endPoint);

        //now ask for a binding which is not implemented by the service requested
        endPoint = UDDIUtilities.extractEndPointFromWsdl(wsdl, "Warehouse which does not exist", "WarehouseSoap", "WarehouseSoap");
        System.out.println(endPoint);
        Assert.assertEquals("Invalid end point found", "http://hugh/ACMEWarehouseWS/Service1.asmx", endPoint);

        //now ask for a binding which is not implemented by the service requested, or the port
        endPoint = UDDIUtilities.extractEndPointFromWsdl(wsdl, "Warehouse which does not exist", "WarehouseSoap does not exist", "WarehouseSoap");
        System.out.println(endPoint);
        Assert.assertEquals("Invalid end point found", "http://hugh/ACMEWarehouseWS/Service1.asmx", endPoint);
    }

    @Test(expected = UDDIUtilities.WsdlEndPointNotFoundException.class)
    public void testGetNonExistentBinding() throws UDDIUtilities.WsdlEndPointNotFoundException {
        String endPoint = UDDIUtilities.extractEndPointFromWsdl(wsdl, "Warehouse", "WarehouseSoap", "WarehouseSoap does not exist");
        System.out.println(endPoint);
    }


    /**
     * Test that the correct information is extracted from the UDDI data model objects when we recieve a notification
     * @throws UDDIException
     */
    @Test
    public void testGetUDDIBindingImplInfo() throws UDDIException {
        InputStream stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/BusinessService.xml");
        final BusinessService businessService = JAXB.unmarshal(stream, BusinessService.class);
        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/tModel_binding.xml");
        final TModel bindingTModel = JAXB.unmarshal(stream, TModel.class);
        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/tModel_portType.xml");
        final TModel portTypeTModel = JAXB.unmarshal(stream, TModel.class);

        TestUddiClient uddiClient = new TestUddiClient(businessService, Arrays.asList(bindingTModel, portTypeTModel));        

        UDDIUtilities.UDDIBindingImplementionInfo bindingInfo = UDDIUtilities.getUDDIBindingImplInfo(uddiClient,
                "uddi:8617fae0-c987-11de-8486-efda8c54fa31", "WarehouseSoap", "WarehouseSoap", null);

        Assert.assertNotNull(bindingInfo);

        Assert.assertEquals("Invalid access point found", "http://irishman.l7tech.com:8080/service/91783168", bindingInfo.getEndPoint());
        Assert.assertEquals("Invalid WSDL URL found", "http://irishman.l7tech.com:8080/ssg/wsdl?serviceoid=91783168", bindingInfo.getImplementingWsdlUrl());
        Assert.assertEquals("Invalid wsdl:port information found", "WarehouseSoap", bindingInfo.getImplementingWsdlPort());
    }

    /**
     * Tests that when the wsdl:port we want is not found, that we will find another bindingTemplate which implements
     * the correct binding
     * @throws UDDIException
     */
    @Test
    public void testGetUDDIBindingImplInfoFindOtherBinding() throws UDDIException {
        SyspropUtil.setProperty( "com.l7tech.uddi.UDDIUtilities.fallBackOnFirstImplentingWsdlPort", "true" );
        try {
            InputStream stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/BusinessServiceDiffBindings.xml");
            final BusinessService businessService = JAXB.unmarshal(stream, BusinessService.class);
            stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/tModel_binding.xml");
            final TModel bindingTModel = JAXB.unmarshal(stream, TModel.class);
            stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/tModel_portType.xml");
            final TModel portTypeTModel = JAXB.unmarshal(stream, TModel.class);

            TestUddiClient uddiClient = new TestUddiClient(businessService, Arrays.asList(bindingTModel, portTypeTModel));

            UDDIUtilities.UDDIBindingImplementionInfo bindingInfo = UDDIUtilities.getUDDIBindingImplInfo(uddiClient,
                    "uddi:8617fae0-c987-11de-8486-efda8c54fa31", "WarehouseSoap", "WarehouseSoap", "http://warehouse.acme.com/ws");

            Assert.assertNotNull(bindingInfo);

            Assert.assertEquals("Invalid access point found", "http://irishman.l7tech.com:8080/service/91783168", bindingInfo.getEndPoint());
            Assert.assertEquals("Invalid WSDL URL found", "http://irishman.l7tech.com:8080/ssg/wsdl?serviceoid=91783168", bindingInfo.getImplementingWsdlUrl());
            Assert.assertFalse("Invalid wsdl:port information found", "WarehouseSoap".equals(bindingInfo.getImplementingWsdlPort()));
            Assert.assertEquals("Invalid wsdl:port information found", "WarehouseSoap1" , bindingInfo.getImplementingWsdlPort());
        } finally {
            SyspropUtil.clearProperty( "com.l7tech.uddi.UDDIUtilities.fallBackOnFirstImplentingWsdlPort" );
        }

    }

    /**
     * Tests that an incorrect binding is not found if a wsdl:binding has the same name as an imported wsdl:binding
     * which we are looking for 
     * @throws UDDIException
     */
    @Test
    public void testGetUDDIBindingImplInfoNameSpaceSupportNotFound() throws UDDIException{
        initializeLogging();
        InputStream stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/SpaceOrderService.xml");
        final BusinessService businessService = JAXB.unmarshal(stream, BusinessService.class);
        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/spaceBindingTModel.xml");
        final TModel bindingTModel = JAXB.unmarshal(stream, TModel.class);
        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/spacePortTypeTModel.xml");
        final TModel portTypeTModel = JAXB.unmarshal(stream, TModel.class);

        TestUddiClient uddiClient = new TestUddiClient(businessService, Arrays.asList(bindingTModel, portTypeTModel));

        UDDIUtilities.UDDIBindingImplementionInfo bindingInfo = UDDIUtilities.getUDDIBindingImplInfo(uddiClient,
                "uddi:9c7d0e97-d88c-11de-bd69-c3cafb478058", "ChildWsdlPort", "SameBindingName", "http://SOB.IPD.LMCO/ExternalWSDL");

        Assert.assertNull("No wsdl:port implements the requested binding. No binding should be found", bindingInfo);
    }

    /**
     * Tests that an incorrect binding is not found if a wsdl:binding has the same name as an imported wsdl:binding
     * which we are looking for
     * @throws UDDIException
     */
    @Test
    public void testGetUDDIBindingImplInfoNameSpaceSupportFound() throws UDDIException{
        initializeLogging();
        InputStream stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/SpaceOrderService.xml");
        final BusinessService businessService = JAXB.unmarshal(stream, BusinessService.class);
        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/spaceBindingTModel.xml");
        final TModel bindingTModel = JAXB.unmarshal(stream, TModel.class);
        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/spacePortTypeTModel.xml");
        final TModel portTypeTModel = JAXB.unmarshal(stream, TModel.class);

        TestUddiClient uddiClient = new TestUddiClient(businessService, Arrays.asList(bindingTModel, portTypeTModel));

        UDDIUtilities.UDDIBindingImplementionInfo bindingInfo = UDDIUtilities.getUDDIBindingImplInfo(uddiClient,
                "uddi:9c7d0e97-d88c-11de-bd69-c3cafb478058", "ParentWsdlPort", "SameBindingName", "http://SOB.IPD.LMCO/");

        Assert.assertNotNull("A wsdl:port implementing the requested binding should be found", bindingInfo);
    }

    /**
     * Tests UDDIUtilities.isHttpBinding
     *
     * @throws Exception
     */
    @Test
    public void testBindingIsHttp() throws Exception{
        InputStream stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/tModel_binding.xml");
        final TModel bindingTModel = JAXB.unmarshal(stream, TModel.class);
        Assert.assertTrue("tModel is a http binding tModel", UDDIUtilities.isHttpBinding(bindingTModel));
    }

    /**
     * Tests UDDIUtilities.isHttpBinding V2
     *
     * @throws Exception
     */
    @Test
    public void testBindingIsHttp_V2() throws Exception{
        InputStream stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/tModelBinding_V2.xml");
        final TModel bindingTModel = JAXB.unmarshal(stream, TModel.class);
        Assert.assertTrue("tModel is a http binding tModel", UDDIUtilities.isHttpBinding(bindingTModel));
    }

    /**
     * Tests UDDIUtilities.isSoapBinding
     * 
     * @throws Exception
     */
    @Test
    public void testBindingIsSoap() throws Exception{
        InputStream stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/tModel_binding.xml");
        final TModel bindingTModel = JAXB.unmarshal(stream, TModel.class);
        Assert.assertTrue("tModel is a soap binding tModel", UDDIUtilities.isSoapBinding(bindingTModel));
    }

    /**
     * Tests UDDIUtilities.isSoapBinding V2
     *
     * @throws Exception
     */
    @Test
    public void testBindingIsSoap_V2() throws Exception{
        InputStream stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/tModelBinding_V2.xml");
        final TModel bindingTModel = JAXB.unmarshal(stream, TModel.class);
        Assert.assertTrue("tModel is a soap binding tModel", UDDIUtilities.isSoapBinding(bindingTModel));
    }

    /**
     * Tests UDDIUtilities.isSoapBinding - invalid binding tModel
     *
     * @throws Exception
     */
    @Test
    public void testBindingIsSoap_InvalidBinding() throws Exception{
        InputStream stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/InvalidBinding.xml");
        final TModel bindingTModel = JAXB.unmarshal(stream, TModel.class);
        Assert.assertFalse("tModel is an invalid binding soap binding tModel", UDDIUtilities.isSoapBinding(bindingTModel));
    }

    /**
     * Tests UDDIUtilities.isSoapBinding - invalid argument
     *
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void testBindingIsSoap_WrongType() throws Exception {
        InputStream stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/tModel_portType.xml");

        final TModel nonBindingTModel = JAXB.unmarshal(stream, TModel.class);
        UDDIUtilities.isSoapBinding(nonBindingTModel);
    }

    /**
     * Tests UDDIUtilities.isSoapBinding - invalid argument
     *
     * @throws Exception
     */
    @Test(expected = NullPointerException.class)
    public void testBindingIsSoap_NullPointer() throws Exception {
        UDDIUtilities.isSoapBinding(null);
    }



    private static void initializeLogging() {
        final LogManager logManager = LogManager.getLogManager();

        final File file = new File("logging.properties");
        if (file.exists()) {
            InputStream in = null;
            try {
                in = file.toURI().toURL().openStream();
                if (in != null) {
                    logManager.readConfiguration(in);
                }
            } catch (IOException e) {
                System.err.println("Cannot initialize logging " + e.getMessage());
            } finally {
                try {
                    if (in != null) in.close();
                } catch (IOException e) { // should not happen
                    System.err.println("Cannot close logging properties input stream " + e.getMessage());
                }
            }
        } else {
            System.err.println("Cannot initialize logging");
        }
    }

    //code to download and marshall business servics and tmodels
//    final String inquiry = "http://10.7.2.200:53307/UddiRegistry/inquiry";
//    final String publish = "http://10.7.2.200:53307/UddiRegistry/publish";
//    final String subscription = "http://10.7.2.200:53307/UddiRegistry/subscription";
//    final String security = "http://10.7.2.200:53307/UddiRegistry/security";
//    GenericUDDIClient uddiClient = new GenericUDDIClient(inquiry, publish, subscription, security, "administrator", "7layer", PolicyAttachmentVersion.v1_2, null);
//
//    UDDIBusinessServiceDownloader serviceDownloader = new UDDIBusinessServiceDownloader(uddiClient);
//    Set<String> serviceKeys = new HashSet<String>();
//    serviceKeys.add("uddi:9c7d0e97-d88c-11de-bd69-c3cafb478058");
//    final List<Pair<BusinessService, Map<String, TModel>>> businessServiceModels = serviceDownloader.getBusinessServiceModels(serviceKeys);
//    Assert.assertEquals("Incorrect number of services found", 1, businessServiceModels.size());
//
//    JAXB.marshal(businessServiceModels.get(0).left, new File("/home/darmstrong/Documents/Projects/Bondo/UDDI/Marshalled/SpaceOrderService.xml"));
//    int i = 0;
//    for(TModel model: businessServiceModels.get(0).right.values()) {
//        JAXB.marshal(model, new File("/home/darmstrong/Documents/Projects/Bondo/UDDI/Marshalled/spacetModel" + i + ".xml"));
//        i++;
//    }


    /**
     * Test the getTModelType method
     */
//    @Test
//    public void testGetTModelType(){
//
//    }

    public UDDIClient getUDDIClient() throws UDDIException {
        TestUddiClient uddiClient = new TestUddiClient();
        return uddiClient;
    }
}
