/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.uddi;

import org.junit.Test;
import org.junit.Before;
import org.junit.Assert;
import com.l7tech.wsdl.Wsdl;
import com.l7tech.common.uddi.guddiv3.*;
import com.l7tech.util.Pair;
import static com.l7tech.uddi.UDDIUtilities.TMODEL_TYPE.WSDL_PORT_TYPE;
import static com.l7tech.uddi.UDDIUtilities.TMODEL_TYPE.WSDL_BINDING;

import javax.xml.bind.JAXB;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Arrays;
import java.io.InputStream;

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

        final int serviceOid = 3828382;
        wsdlToUDDIModelConverter = new WsdlToUDDIModelConverter(wsdl, gatewayWsdlUrl, gatewayURL, "uddi:uddi_business_key", serviceOid);
        wsdlToUDDIModelConverter.convertWsdlToUDDIModel();
        serviceToDependentModels = wsdlToUDDIModelConverter.getServicesAndDependentTModels();
        referencedModels = serviceToDependentModels.get(0).right;
        singleService = serviceToDependentModels.get(0).left;
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
        BusinessServicePublisher servicePublisher = new BusinessServicePublisher(wsdl, 23424323L, getUDDIClient());
        servicePublisher.publishDependentTModels(referencedModels, WSDL_PORT_TYPE);

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
        BusinessServicePublisher servicePublisher = new BusinessServicePublisher(wsdl, 23424323L, getUDDIClient());
        servicePublisher.publishDependentTModels(referencedModels, WSDL_PORT_TYPE);

        final ArrayList<TModel> models = new ArrayList<TModel>();
        models.addAll(referencedModels.values());
        UDDIUtilities.updateBindingTModelReferences(models, referencedModels);

        servicePublisher.publishDependentTModels(referencedModels, WSDL_BINDING);
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
                "uddi:8617fae0-c987-11de-8486-efda8c54fa31", "WarehouseSoap", "WarehouseSoap");

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
        InputStream stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/BusinessServiceDiffBindings.xml");
        final BusinessService businessService = JAXB.unmarshal(stream, BusinessService.class);
        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/tModel_binding.xml");
        final TModel bindingTModel = JAXB.unmarshal(stream, TModel.class);
        stream = UDDIUtilitiesTest.class.getClassLoader().getResourceAsStream("com/l7tech/uddi/UDDIObjects/tModel_portType.xml");
        final TModel portTypeTModel = JAXB.unmarshal(stream, TModel.class);

        TestUddiClient uddiClient = new TestUddiClient(businessService, Arrays.asList(bindingTModel, portTypeTModel));

        UDDIUtilities.UDDIBindingImplementionInfo bindingInfo = UDDIUtilities.getUDDIBindingImplInfo(uddiClient,
                "uddi:8617fae0-c987-11de-8486-efda8c54fa31", "WarehouseSoap", "WarehouseSoap");

        Assert.assertNotNull(bindingInfo);

        Assert.assertEquals("Invalid access point found", "http://irishman.l7tech.com:8080/service/91783168", bindingInfo.getEndPoint());
        Assert.assertEquals("Invalid WSDL URL found", "http://irishman.l7tech.com:8080/ssg/wsdl?serviceoid=91783168", bindingInfo.getImplementingWsdlUrl());
        Assert.assertFalse("Invalid wsdl:port information found", "WarehouseSoap".equals(bindingInfo.getImplementingWsdlPort()));
        Assert.assertEquals("Invalid wsdl:port information found", "WarehouseSoap1" , bindingInfo.getImplementingWsdlPort());
    }

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
