package com.l7tech.uddi;

import com.l7tech.wsdl.Wsdl;
import com.l7tech.common.uddi.guddiv3.*;

import javax.wsdl.Service;
import javax.wsdl.Port;
import javax.wsdl.Binding;
import javax.wsdl.PortType;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.net.URL;
import java.net.MalformedURLException;

/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 *
 * Convert a Wsdl to the UDDI information model according to the Technical Note "Using WSDL in a UDDI Registry, Version 2.0.2"
 * See http://www.oasis-open.org/committees/uddi-spec/doc/tn/uddi-spec-tc-tn-wsdl-v202-20040631.htm
 *
 * No actual UDDI interaction occurs in this class. It is only responsible for the conversion of the WSDL model to the
 * UDDI model
 *
 * @author darmstrong
 */

public class WsdlToUDDIModelConverter {
    private static Logger logger = Logger.getLogger(WsdlToUDDIModelConverter.class.getName());
    protected static final String LANAGUAGE = "en-US";
    protected static final String UDDI_WSDL_CATEGORIZATION_PROTOCOL = "uddi:uddi.org:wsdl:categorization:protocol";
    protected static final String SOAP_PROTOCOL_V3 = "uddi:uddi.org:protocol:soap"; //v2 = uuid:aa254698-93de-3870-8df3-a5c075d64a0e
    protected static final String HTTP_TRANSPORT_V3 = "uddi:uddi.org:transport:http"; //v2 = uuid:68DE9E80-AD09-469D-8A37-088422BFBC36
    protected static final String WSDL_INTERFACE = "wsdlInterface";
    public static final String UDDI_WSDL_TYPES = "uddi:uddi.org:wsdl:types";
    public static final String UDDI_XML_NAMESPACE = "uddi:uddi.org:xml:namespace";
    protected static final String UDDI_WSDL_PORTTYPEREFERENCE = "uddi:uddi.org:wsdl:porttypereference";
    protected static final String UDDI_CATEGORIZATION_TYPES = "uddi:uddi.org:categorization:types";
    protected static final String UDDI_WSDL_CATEGORIZATION_TRANSPORT = "uddi:uddi.org:wsdl:categorization:transport";
    protected static final String UDDI_XML_LOCALNAME = "uddi:uddi.org:xml:localname";
    protected static final String WSDL_BINDING_INSTANCE_DESCRIPTION = "the wsdl:binding that this wsdl:port implements";
    
    public static final String PORT_TMODEL_IDENTIFIER = "_PortType";
    public static final String BINDING_TMODEL_IDENTIFIER = "_Binding";

    private final Wsdl wsdl;
    private final String wsdlURL;
    private final String gatewayURL;
    private final long serviceOid;
    private Map<String, String> serviceNameToWsdlServiceNameMap;
    private List<BusinessService> businessServices;

    /**
     * When we convert a Published Service's WSDL into a Wsdl object, it does not complain about missing references
     * e.g. a wsdl:port references a wsdl:binding which does not exist. This represents it self in the Wsdl object
     * model as null pointers. When this is found an instance of this excetion is thrown instead of the
     * NullPointerException, calling code can deal with this anticipated exception
     */
    public static class MissingWsdlReferenceException extends Exception{
        public MissingWsdlReferenceException(String message) {
            super(message);
        }
    }
    /**
     * This businessKey will be the parent of all created Business Services
     */
    private final String businessKey;

    /**
     * wsdl:portType, wsdl:binding have a name attribute which is unique in a WSDL. When publishing a WSDL, any models
     * published which generates a key is recorded against the unique name, so it can be retrieved later if the
     * same wsdl element is referenced
     */
    private Map<String, TModel> keysToPublishedTModels;

    public WsdlToUDDIModelConverter(final Wsdl wsdl,
                                    final String wsdlURL,
                                    final String gatewayURL,
                                    final String businessKey,
                                    final long serviceOid) {
        if(wsdl == null) throw new NullPointerException("wsdl cannot be null");
        if(wsdlURL == null || wsdlURL.trim().isEmpty()) throw new IllegalArgumentException("wsdlURL cannot be null or emtpy");
        try {
            new URL(wsdlURL);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid wsdlURL: " + e.getMessage());
        }

        if(gatewayURL == null || gatewayURL.trim().isEmpty()) throw new IllegalArgumentException("gatewayURL cannot be null or emtpy");
        try {
            new URL(gatewayURL);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid gatewayURL: " + e.getMessage());
        }

        if(businessKey == null || businessKey.trim().isEmpty()) throw new IllegalArgumentException("businessKey cannot be null or emtpy");

        if(serviceOid < 1) throw new IllegalArgumentException("Invalid serviceOid: " + serviceOid);

        this.wsdl = wsdl;
        this.wsdlURL = wsdlURL;
        this.gatewayURL = gatewayURL;
        this.businessKey = businessKey;
        this.serviceOid = serviceOid;
    }

    public Map<String, String> getServiceNameToWsdlServiceNameMap() {
        return serviceNameToWsdlServiceNameMap;
    }

    /**
     * Regardles of what is in UDDI, the job of this method is to convert the Wsdl instance variable into a List
     * of Business Services. These services and everything they contain / reference may already exist in UDDI.
     * All tModelkeys of required tModels contain placeholders
     * <p/>
     * Note: BusinessServices and bindingTemplates do not need placeholders, as if they do not already exist, then
     * they will be filled in automatically on save by the UDDI.
     * <p/>
     * <p/>
     * A best effort conversion is attempted. Errors in the WSDL are tolerated up to the point where no valid wsdl:service
     * elements are defined. In this case an exception will be thrown.
     * <p/>
     * Note: it is up to the client to work out what tModelKeys need to be created / retrieved from UDDI
     *
     * Note: Never set the service key on the BusinessService with setServiceKey. This will break publish functionality
     *
     * @throws com.l7tech.uddi.WsdlToUDDIModelConverter.MissingWsdlReferenceException
     *          if the WSDL contains no valid
     *          wsdl:service definitions due to each wsdl:service containing references to bindings which either themselves
     *          don't exist or reference wsdl:portType elements which dont exist.
     */
    public void convertWsdlToUDDIModel() throws MissingWsdlReferenceException {
        businessServices = new ArrayList<BusinessService>();
        keysToPublishedTModels = new HashMap<String, TModel>();
        serviceNameToWsdlServiceNameMap = new HashMap<String, String>();

        Collection<Service> services = wsdl.getServices();
        for (final Service wsdlService : services) {
            final BusinessService businessService = new BusinessService();
            businessService.setBusinessKey(businessKey);
            try {
                createUddiBusinessService(businessService, wsdlService);
                businessServices.add(businessService);
                serviceNameToWsdlServiceNameMap.put(businessService.getName().get(0).getValue(), wsdlService.getQName().getLocalPart());
            } catch (MissingWsdlReferenceException e) {
                //already logged, we ignore the bindingTemplate as it is invalid
                logger.log(Level.INFO, "Ignored wsdl:service '" + wsdlService.getQName().getLocalPart() + "'as the wsdl:binding contains invalid references");
            }
        }

        if (businessServices.isEmpty()) {
            final String msg = "The wsdl does not contain any valid wsdl:service definitions. Cannot convert to UDDI model";
            logger.log(Level.INFO, msg);
            throw new MissingWsdlReferenceException(msg);
        }
    }

    /**
     * @return List of Business Services. None of the references are real.
     */
    public List<BusinessService> getBusinessServices() {
        return businessServices;
    }

    /**
     * @return Map of TModels which are referenced from the Business Services. The tModelKeys are not real
     * The reference from a keyedReference in a BusinessService is the key into the Map to retrieve the tModel.
     */
    public Map<String, TModel> getKeysToPublishedTModels() {
        return keysToPublishedTModels;
    }

    private void createUddiBusinessService(final BusinessService businessService, final Service wsdlService) throws MissingWsdlReferenceException {
        final String serviceName = wsdlService.getQName().getLocalPart();
        final String localName = "Layer7 " + serviceName + " " + serviceOid;//this is ok to modify as the uddi:name of the BusinessService does not map to a wsdl element
        businessService.getName().add(getName(localName));

        BindingTemplates bindingTemplates = new BindingTemplates();
        Map<String, Port> ports = wsdlService.getPorts();
        //if the wsdl:service contains no ports, this collection is empty and not null
        for (Map.Entry<String, Port> entry : ports.entrySet()) {
            final Port wsdlPort = entry.getValue();
            final BindingTemplate bindingTemplate;
            try {
                bindingTemplate = createUddiBindingTemplate(wsdlPort);
                bindingTemplates.getBindingTemplate().add(bindingTemplate);
            } catch (MissingWsdlReferenceException e) {
                //already logged, we ignore the bindingTemplate as it is invalid
                logger.log(Level.INFO, "Ignored bindingTemplate as the wsdl:port contains invalid references");
            }
        }

        if(bindingTemplates.getBindingTemplate().isEmpty()){
            final String msg = "Cannot create BusinessService as the wsdl:service does not reference any valid wsdl bindings";
            logger.log(Level.INFO, msg);
            throw new MissingWsdlReferenceException(msg);
        }

        businessService.setBindingTemplates(bindingTemplates);

        CategoryBag categoryBag = new CategoryBag();
        KeyedReference serviceEntityReference = new KeyedReference();
        serviceEntityReference.setKeyValue("service");
        serviceEntityReference.setTModelKey(UDDI_WSDL_TYPES);
        categoryBag.getKeyedReference().add(serviceEntityReference);

        KeyedReference localNameRef = new KeyedReference();
        localNameRef.setKeyName("service local name");
        localNameRef.setKeyValue(localName);  //this is ok to use the Layer7 name, allowed to be the name of the service
        localNameRef.setTModelKey(UDDI_XML_LOCALNAME);
        categoryBag.getKeyedReference().add(localNameRef);

        final String nameSpace = wsdlService.getQName().getNamespaceURI();
        if (nameSpace != null && !nameSpace.trim().isEmpty()) {
            KeyedReference bindingNameSpace = new KeyedReference();
            bindingNameSpace.setKeyValue(nameSpace);
            bindingNameSpace.setKeyName("service namespace");
            bindingNameSpace.setTModelKey(UDDI_XML_NAMESPACE);
            categoryBag.getKeyedReference().add(bindingNameSpace);
        }

        businessService.setCategoryBag(categoryBag);

    }

    private BindingTemplate createUddiBindingTemplate(final Port wsdlPort) throws MissingWsdlReferenceException {
        BindingTemplate bindingTemplate = new BindingTemplate();

        //For v3, we don't care about what type of element represents the endPoint e.g. whether it's soap:address
        //soap12:address or http:addresss, as they all represent an endpoint.
        AccessPoint accessPoint = new AccessPoint();
        accessPoint.setUseType("endPoint");
        accessPoint.setValue(gatewayURL);
        bindingTemplate.setAccessPoint(accessPoint);

        TModelInstanceDetails tModelInstanceDetails = new TModelInstanceDetails();

        //tModel for wsdl:binding, that this wsdl:port implements
        final Binding binding = wsdlPort.getBinding();
        if(binding == null){
            final String msg = "wsdl:port '" + wsdlPort.getName() + "'contains a reference to a non existing wsdl:portType";
            logger.log(Level.INFO, msg);
            throw new MissingWsdlReferenceException(msg);
        }
        final String bindingTModelKey = createUddiBindingTModel(binding);
        TModelInstanceInfo bindingTModelInstanceInfo = new TModelInstanceInfo();
        bindingTModelInstanceInfo.setTModelKey(bindingTModelKey);

        InstanceDetails instanceDetails = new InstanceDetails();
        instanceDetails.getDescription().add(getDescription(WSDL_BINDING_INSTANCE_DESCRIPTION));
        instanceDetails.setInstanceParms(binding.getQName().getLocalPart());
        bindingTModelInstanceInfo.setInstanceDetails(instanceDetails);

        tModelInstanceDetails.getTModelInstanceInfo().add(bindingTModelInstanceInfo);

        //tModel for the wsdl:portType referenced by the wsdl:binding
        final String portTypeTModelKey = createUddiPortTypeTModel(binding.getPortType());
        TModelInstanceInfo portTypeTModelInstanceInfo = new TModelInstanceInfo();
        portTypeTModelInstanceInfo.setTModelKey(portTypeTModelKey);

        tModelInstanceDetails.getTModelInstanceInfo().add(portTypeTModelInstanceInfo);

        bindingTemplate.setTModelInstanceDetails(tModelInstanceDetails);
        return bindingTemplate;
    }

    /**
     * Create a wsdl:binding tModel according to the spec and return the tModel key of the created tModel
     * @param binding Binding wsdl:binding to model as a tModel in UDDI
     * @return String key of the created tModel
     * @throws com.l7tech.uddi.WsdlToUDDIModelConverter.MissingWsdlReferenceException if the binding contains a reference
     * to a wsdl:portType which does not exist
     */
    private String createUddiBindingTModel(final Binding binding) throws MissingWsdlReferenceException {
        final String bindingName = binding.getQName().getLocalPart();//+ " " + serviceOid; - don't do this, it breaks the technical note

        //A binding name is UNIQUE across all wsdl:bindings in the entire WSDL!
        //See http://www.w3.org/TR/wsdl#_bindings
        //appending BINDING_TMODEL_IDENTIFIER as a wsdl:portType could have the same name
        final String key = bindingName + BINDING_TMODEL_IDENTIFIER;
        //no need to create the tModel if it already exists
        if(keysToPublishedTModels.containsKey(key)) return key;

        TModel tModel = new TModel();
        tModel.setName(getName(bindingName));

        tModel.getOverviewDoc().addAll(getOverViewDocs());
        
        CategoryBag categoryBag = new CategoryBag();

        KeyedReference bindingEntityReference = new KeyedReference();
        bindingEntityReference.setKeyValue("binding");
        bindingEntityReference.setTModelKey(UDDI_WSDL_TYPES);
        categoryBag.getKeyedReference().add(bindingEntityReference);

        if(binding.getPortType() == null) {
            final String msg = "wsdl:binding '" + binding.getQName().getLocalPart() + "'contains a reference to a non existing wsdl:portType";
            logger.log(Level.INFO, msg);
            throw new MissingWsdlReferenceException(msg);
        }
        final String portTypeTModelKey = createUddiPortTypeTModel(binding.getPortType());
        KeyedReference portTypeReference = new KeyedReference();
        portTypeReference.setKeyName("portType reference");
        portTypeReference.setTModelKey(UDDI_WSDL_PORTTYPEREFERENCE);
        portTypeReference.setKeyValue(portTypeTModelKey);
        categoryBag.getKeyedReference().add(portTypeReference);

        KeyedReference wsdlSpecReference = new KeyedReference();
        wsdlSpecReference.setKeyValue("wsdlSpec");
        wsdlSpecReference.setTModelKey(UDDI_CATEGORIZATION_TYPES);
        categoryBag.getKeyedReference().add(wsdlSpecReference);

        final String nameSpace = binding.getQName().getNamespaceURI();
        if(nameSpace != null && !nameSpace.trim().isEmpty()){
            KeyedReference bindingNameSpace = new KeyedReference();
            bindingNameSpace.setKeyValue(nameSpace);
            bindingNameSpace.setKeyName("binding namespace");
            bindingNameSpace.setTModelKey(UDDI_XML_NAMESPACE);
            categoryBag.getKeyedReference().add(bindingNameSpace);
        }

        KeyedReference soapProtocolReference = new KeyedReference();
        soapProtocolReference.setKeyName("SOAP protocol");
        soapProtocolReference.setKeyValue(SOAP_PROTOCOL_V3);
        soapProtocolReference.setTModelKey(UDDI_WSDL_CATEGORIZATION_PROTOCOL);
        categoryBag.getKeyedReference().add(soapProtocolReference);

        KeyedReference httpTransportReference = new KeyedReference();
        httpTransportReference.setKeyName("HTTP transport");
        httpTransportReference.setKeyValue(HTTP_TRANSPORT_V3);
        httpTransportReference.setTModelKey(UDDI_WSDL_CATEGORIZATION_TRANSPORT);
        categoryBag.getKeyedReference().add(httpTransportReference);

        tModel.setCategoryBag(categoryBag);
        //don't set the key, as it's meaningless to the tModel, its just used as a placeholder for references
//        tModel.setTModelKey(key);
        keysToPublishedTModels.put(key, tModel);
        return key;
    }

    private List<OverviewDoc> getOverViewDocs(){
        List<OverviewDoc> returnList = new ArrayList<OverviewDoc>();
        OverviewDoc overviewDoc = new OverviewDoc();
        overviewDoc.getDescription().add(getDescription("the original WSDL document"));
        OverviewURL overviewURL = new OverviewURL();
        overviewURL.setUseType(WSDL_INTERFACE);
        overviewURL.setValue(wsdlURL);
        overviewDoc.setOverviewURL(overviewURL);
        returnList.add(overviewDoc);

        returnList.add(getOverviewDocTechnicalNode());

        return returnList;
    }

    private String createUddiPortTypeTModel(final PortType portType) {
        //calling code should not have called with null pointer
        if(portType == null) throw new NullPointerException("Missing reference to wsdl:portType");
        final String portTypeName = portType.getQName().getLocalPart();//+ " " + serviceOid; - this breaks the techincal spec - don't modify the name

        //A portName is UNIQUE across all wsdl:portTypes in the entire WSDL!
        //See http://www.w3.org/TR/wsdl#_porttypes
        //appending PORT_TMODEL_IDENTIFIER as a wsdl:binding could have the same name
        final String key = portTypeName + PORT_TMODEL_IDENTIFIER;
        //no need to create the tModel if it already exists
        if(keysToPublishedTModels.containsKey(key)) return key;

        TModel tModel = new TModel();
        tModel.setName(getName(portTypeName));

        tModel.getOverviewDoc().addAll(getOverViewDocs());

        CategoryBag categoryBag = new CategoryBag();

        //all required keyed references
        KeyedReference portTypeEntityReference = new KeyedReference();
        portTypeEntityReference.setKeyValue("portType");
        portTypeEntityReference.setTModelKey(UDDI_WSDL_TYPES);
        categoryBag.getKeyedReference().add(portTypeEntityReference);

        final String nameSpace = portType.getQName().getNamespaceURI();
        //If the namespace is not defined, it must not be added
        if(nameSpace != null && !nameSpace.trim().isEmpty()){
            KeyedReference portTypeNameSpace = new KeyedReference();
            portTypeNameSpace.setKeyValue(nameSpace);
            portTypeNameSpace.setKeyName("portType namespace");
            portTypeNameSpace.setTModelKey(UDDI_XML_NAMESPACE);
            categoryBag.getKeyedReference().add(portTypeNameSpace);
        }

        tModel.setCategoryBag(categoryBag);
        //don't set the key, as it's meaningless to the tModel, its just used as a placeholder for references
//        tModel.setTModelKey(key);
        keysToPublishedTModels.put(key, tModel);
        return key;
    }

    private Description getDescription(final String value){
        Description description = new Description();
        //don't set the language see bug 7922
        description.setValue(value);
        return description;
    }

    private Name getName(final String value){
        Name name = new Name();
        //don't set the language see bug 7922
        name.setValue(value);
        return name;
    }

    private OverviewDoc getOverviewDocTechnicalNode(){
        OverviewDoc overviewDoc = new OverviewDoc();
        overviewDoc.getDescription().add(getDescription("Technical Note \"Using WSDL in a UDDI Registry, Version 2.0.2\""));
        OverviewURL overviewURL = new OverviewURL();
        overviewURL.setUseType("text");
        overviewURL.setValue("http://www.oasis-open.org/committees/uddi-spec/doc/tn/uddi-spec-tc-tn-wsdl-v202-20040631.htm");
        overviewDoc.setOverviewURL(overviewURL);
        return overviewDoc;
    }
}
