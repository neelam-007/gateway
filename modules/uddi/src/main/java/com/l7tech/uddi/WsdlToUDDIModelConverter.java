package com.l7tech.uddi;

import com.l7tech.wsdl.Wsdl;
import com.l7tech.common.uddi.guddiv3.*;
import com.l7tech.util.Pair;

import javax.wsdl.Service;
import javax.wsdl.Port;
import javax.wsdl.Binding;
import javax.wsdl.PortType;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.soap12.SOAP12Address;
import javax.wsdl.extensions.soap.SOAPAddress;
import java.util.*;
import java.util.logging.Logger;
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
    public static final String LAYER7_PROXY_SERVICE_GENERAL_KEYWORD_URN = "urn_layer7tech-com_proxy_published_service_identifier";
    public static final String UDDI_GENERAL_KEYWORDS = "uddi:uddi.org:categorization:general_keywords";
    

    public static final String PORT_TMODEL_IDENTIFIER = "_PortType";
    public static final String BINDING_TMODEL_IDENTIFIER = "_Binding";

    private final Wsdl wsdl;
    private final String wsdlURL;
    private final String gatewayURL;
    private final long serviceOid;

    /**
     * This businessKey will be the parent of all created Business Services
     */
    private final String businessKey;

    private final String generalKeywordServiceIdentifier;

    /**
     * wsdl:portType, wsdl:binding have a name attribute which is unique in a WSDL. When publishing a WSDL, any models
     * published which generates a key is recorded against the unique name, so it can be retrieved later if the
     * same wsdl element is referenced
     */
    private final Map<String, TModel> keysToPublishedTModels = new HashMap<String, TModel>();

    public WsdlToUDDIModelConverter(final Wsdl wsdl,
                                    final String wsdlURL,
                                    final String gatewayURL,
                                    final String businessKey,
                                    final long serviceOid,
                                    final String generalKeywordServiceIdentifier) {
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

        if(generalKeywordServiceIdentifier == null || generalKeywordServiceIdentifier.trim().isEmpty())
            throw new IllegalArgumentException("generalKeywordServiceIdentifier cannot be null or emtpy");

        this.wsdl = wsdl;
        this.wsdlURL = wsdlURL;
        this.gatewayURL = gatewayURL;
        this.businessKey = businessKey;
        this.serviceOid = serviceOid;
        this.generalKeywordServiceIdentifier = generalKeywordServiceIdentifier;
    }

    /**
     * Regardles of what is in UDDI, the job of this method is to convert the Wsdl instance variable into a List
     * of Business Services. These services and everything they contain / reference may already exist in UDDI.
     * All tModelkeys of required tModels contain placeholders
     *
     * Note: BusinessServices and bindingTemplates do not need placeholders, as if they do not already exist, then
     * they will be filled in automatically on save by the UDDI.
     *
     * Note: it is up to the client to work out what tModelKeys need to be created / retrieved from UDDI
     * 
     * @return Pair of a List of Business Services and a Map of TModels which are referenced from the Business Services.
     * The reference from a keyedReference in a BusinessService is the key into the Map to retrieve the tModel.
     * Remember that <b>no real key references</b> exist for any tModelKeys from keyedReferences
     */
    public Pair<List<BusinessService>, Map<String, TModel>> convertWsdlToUDDIModel(){
        List<BusinessService> businessServices = new ArrayList<BusinessService>();

        Collection<Service> services = wsdl.getServices();
        for (final Service wsdlService : services) {
            BusinessService businessService = new BusinessService();
            businessService.setBusinessKey(businessKey);
            createUddiBusinessService(businessService, wsdlService);
            businessServices.add(businessService);
        }

        return new Pair<List<BusinessService>, Map<String, TModel>>(businessServices, Collections.unmodifiableMap(keysToPublishedTModels));
    }

    private void createUddiBusinessService(final BusinessService businessService, final Service wsdlService){
        final String serviceName = wsdlService.getQName().getLocalPart();
        final String localName = "Layer7 " + serviceName+ " " + serviceOid;
        businessService.getName().add(getName(localName));

        BindingTemplates bindingTemplates = new BindingTemplates();
        Map<String, Port> ports = wsdlService.getPorts();
        for (Map.Entry<String, Port> entry : ports.entrySet()) {
            final Port wsdlPort = entry.getValue();
            BindingTemplate bindingTemplate = createUddiBindingTemplate(wsdlPort);
            bindingTemplates.getBindingTemplate().add(bindingTemplate);
        }

        businessService.setBindingTemplates(bindingTemplates);

        CategoryBag categoryBag = new CategoryBag();
        KeyedReference serviceEntityReference = new KeyedReference();
        serviceEntityReference.setKeyValue("service");
        serviceEntityReference.setTModelKey(UDDI_WSDL_TYPES);
        categoryBag.getKeyedReference().add(serviceEntityReference);

        KeyedReference localNameRef = new KeyedReference();
        localNameRef.setKeyName("service local name");
        localNameRef.setKeyValue(localName);
        localNameRef.setTModelKey(UDDI_XML_LOCALNAME);
        categoryBag.getKeyedReference().add(localNameRef);

        final String nameSpace = wsdlService.getQName().getNamespaceURI();
        if(nameSpace != null && !nameSpace.trim().isEmpty()){
            KeyedReference bindingNameSpace = new KeyedReference();
            bindingNameSpace.setKeyValue(nameSpace);
            bindingNameSpace.setKeyName("service namespace");
            bindingNameSpace.setTModelKey(UDDI_XML_NAMESPACE);
            categoryBag.getKeyedReference().add(bindingNameSpace);
        }

        //Add in our Layer7 specific general keyword
        KeyedReference keyWordRef = new KeyedReference();
        keyWordRef.setKeyName(LAYER7_PROXY_SERVICE_GENERAL_KEYWORD_URN);
        keyWordRef.setKeyValue(generalKeywordServiceIdentifier);
        keyWordRef.setTModelKey(UDDI_GENERAL_KEYWORDS);
        categoryBag.getKeyedReference().add(keyWordRef);

        businessService.setCategoryBag(categoryBag);

    }

    private BindingTemplate createUddiBindingTemplate(final Port wsdlPort) {
        BindingTemplate bindingTemplate = new BindingTemplate();
        List<ExtensibilityElement> elements = wsdlPort.getExtensibilityElements();

        AccessPoint accessPoint = null;
        for (ExtensibilityElement ee : elements) {
            if (ee instanceof SOAPAddress || ee instanceof SOAP12Address) {
                accessPoint = new AccessPoint();
                accessPoint.setUseType("http");
                accessPoint.setValue(gatewayURL);
                bindingTemplate.setAccessPoint(accessPoint);
                break;
            }
        }
        if (accessPoint == null) throw new IllegalStateException("No soap:address found in wsdl");

        TModelInstanceDetails tModelInstanceDetails = new TModelInstanceDetails();

        //tModel for wsdl:binding, that this wsdl:port implements
        final Binding binding = wsdlPort.getBinding();
        final String bindingTModelKey = createUddiBindingTModel(binding);
        TModelInstanceInfo bindingTModelInstanceInfo = new TModelInstanceInfo();
        bindingTModelInstanceInfo.setTModelKey(bindingTModelKey);

        InstanceDetails instanceDetails = new InstanceDetails();
        instanceDetails.getDescription().add(getDescription("the wsdl:binding that this wsdl:port implements"));
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
     */
    private String createUddiBindingTModel(final Binding binding){
        final String bindingName = binding.getQName().getLocalPart()+ " " + serviceOid;

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
        final String portTypeName = portType.getQName().getLocalPart()+ " " + serviceOid;

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
