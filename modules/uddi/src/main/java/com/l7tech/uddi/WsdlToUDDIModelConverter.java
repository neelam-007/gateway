package com.l7tech.uddi;

import com.l7tech.wsdl.Wsdl;
import com.l7tech.common.uddi.guddiv3.*;
import com.l7tech.util.Pair;
import com.l7tech.util.HexUtils;
import com.l7tech.util.ExceptionUtils;

import javax.wsdl.Service;
import javax.wsdl.Port;
import javax.wsdl.Binding;
import javax.wsdl.PortType;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.soap12.SOAP12Binding;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

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

    public static final String UDDI_WSDL_TYPES = "uddi:uddi.org:wsdl:types";
    public static final String UDDI_XML_NAMESPACE = "uddi:uddi.org:xml:namespace";

    protected static final String LANAGUAGE = "en-US";
    protected static final String UDDI_WSDL_CATEGORIZATION_PROTOCOL = "uddi:uddi.org:wsdl:categorization:protocol";
    protected static final String SOAP_1_2_V3 = "uuid:soap12";//we know about this odd soap identifier as we came across it during testing
    protected static final String SOAP_PROTOCOL_V3 = "uddi:uddi.org:protocol:soap";
    protected static final String SOAP_PROTOCOL_V2 = "UUID:AA254698-93DE-3870-8DF3-A5C075D64A0E";
    protected static final String HTTP_TRANSPORT_V3 = "uddi:uddi.org:transport:http";
    protected static final String HTTP_TRANSPORT_V2 = "uuid:68DE9E80-AD09-469D-8A37-088422BFBC36";
    protected static final String WSDL_INTERFACE = "wsdlInterface";
    protected static final String UDDI_WSDL_PORTTYPEREFERENCE = "uddi:uddi.org:wsdl:porttypereference";
    protected static final String UDDI_CATEGORIZATION_TYPES = "uddi:uddi.org:categorization:types";
    protected static final String UDDI_WSDL_CATEGORIZATION_TRANSPORT = "uddi:uddi.org:wsdl:categorization:transport";
    protected static final String UDDI_XML_LOCALNAME = "uddi:uddi.org:xml:localname";
    protected static final String WSDL_BINDING_INSTANCE_DESCRIPTION = "the wsdl:binding that this wsdl:port implements";
    protected static final String PORT_TMODEL_IDENTIFIER = "$Layer7$_PortType_Identifier";
    protected static final String BINDING_TMODEL_IDENTIFIER = "$Layer7$_Binding_Identifier";

    private final Wsdl wsdl;

    /**
     * it's ok if keys are not unique and a key gets overwritten. This will only happen when two wsdl:service have the same
     * name which results in a BusinessService with the same name. This is ok, the only purpose of this map is
     * to be able to retrieve the wsdl:service local name, so it doesn't matter if two busines services share the same key 
     */
    private Map<String, String> serviceNameToWsdlServiceNameMap = new HashMap<String, String>();;
    private List<Pair<BusinessService, Map<String, TModel>>> servicesAndDependentTModels;
    public static final String USE_TYPE_END_POINT = "endPoint";
    private static final String HTTP_SCHEMAS_SOAP_HTTP = "http://schemas.xmlsoap.org/soap/http";

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
     * If a wsdl:port does not contain either a soap:address or a soap12:address then this exception is thrown so that the wsdl:port is ignored
     */
    public static class NonSoapWsdlPortException extends Exception{
        public NonSoapWsdlPortException(String message) {
            super(message);
        }
    }

    /**
     * If a wsdl:port implements a non http binding, then this exception is thrown so that the wsdl:port is ignored
     */
    public static class NonHttpBindingException extends Exception{
        public NonHttpBindingException(String message) {
            super(message);
        }
    }

    /**
     * This businessKey will be the parent of all created Business Services
     */
    private final String businessKey;

    public WsdlToUDDIModelConverter(final Wsdl wsdl,
                                    final String businessKey ) {
        if(wsdl == null) throw new NullPointerException("wsdl cannot be null");

        if(businessKey == null || businessKey.trim().isEmpty()) throw new IllegalArgumentException("businessKey cannot be null or emtpy");

        this.wsdl = wsdl;
        this.businessKey = businessKey;
    }

    /**
     * Convert a WSDL into the UDDI data model. After conversion the converted data is available via the applicable
     * get method.
     * <p/>
     * A best effort conversion is attempted. Errors in the WSDL are tolerated up to the point where no valid wsdl:service
     * elements are defined. In this case an exception will be thrown.
     * <p/>
     *
     * @param allEndpointPairs     Collection<Pair<String, String>> A bindingTemplate will be created for each pair in this
     *                             collection. The left hand side is the external gateway url for service for the endpoint and the right hand side
     *                             is the WSDL URL for the service. More than one Pair is included if the cluster defines more than one http(s) listener.
     *                             Cannot be null or empty. Each String value is validated to be not null and not empty.
     * @param prependToServiceName String if not null this value will be prepended to the start of the name property for
     *                             each BusinessService created. The name property of each created BusinessService will be the corresponding
     *                             wsdl:service localname value.
     * @param appendToServiceName  String if not null this value will be appeneded to the end of the name property for
     *                             each BusinessService created. The name property of each created BusinessService will be the corresponding
     *                             wsdl:service localname value.
     * @throws com.l7tech.uddi.WsdlToUDDIModelConverter.MissingWsdlReferenceException
     *          if the WSDL contains no valid
     *          wsdl:service definitions due to each wsdl:service containing references to bindings which either themselves
     *          don't exist or reference wsdl:portType elements which dont exist. This means we cannot do any conversion.
     */
    public void convertWsdlToUDDIModel(final Collection<Pair<String, String>> allEndpointPairs,
                                       final String prependToServiceName,
                                       final String appendToServiceName) throws MissingWsdlReferenceException {

        UDDIUtilities.validateAllEndpointPairs(allEndpointPairs);

        serviceNameToWsdlServiceNameMap.clear();
        servicesAndDependentTModels = new ArrayList<Pair<BusinessService, Map<String, TModel>>>();

        Collection<Service> services = wsdl.getServices();
        for (final Service wsdlService : services) {
            final BusinessService businessService = new BusinessService();
            businessService.setBusinessKey(businessKey);
            try {
                createUddiBusinessService(businessService, wsdlService, allEndpointPairs, prependToServiceName, appendToServiceName);
                serviceNameToWsdlServiceNameMap.put(businessService.getName().get(0).getValue(), wsdlService.getQName().getLocalPart());
            } catch (MissingWsdlReferenceException e) {
                //already logged, we ignore the bindingTemplate as it is invalid
                logger.log(Level.INFO, "Ignored wsdl:service '" + wsdlService.getQName().getLocalPart() + "'as the wsdl:binding contains invalid references");
            }
        }

        if (servicesAndDependentTModels.isEmpty()) {
            final String msg = "The wsdl does not contain any valid / supported wsdl:service definitions. Cannot convert to UDDI model";
            logger.log(Level.INFO, msg);
            throw new MissingWsdlReferenceException(msg);
        }
    }

    /**
     * Get the Map of String BusinessService name to the wsdl:service localname which generated the BusinessService name.
     * Note: it is ok if two BusinessService's have the same name from a single WSDL. Anywhere those BusinessServices
     * are used, their unique namespace must be taken into consideration.
     *
     * @return Map String BusinessService name to wsdl:service localname. Never null. Can be empty if the model has yet
     *         to be converted
     */
    public Map<String, String> getServiceNameToWsdlServiceNameMap() {
        return serviceNameToWsdlServiceNameMap;
    }
    
    /**
     * Get the List of BusinessServices that the WSDL produced after it was converted.
     *
     * @return List of Pairs. Each Pair contains a BusinessService and a unique Map of TModels. Each BusinessService
     *         contains a null serviceKey property. It is up to clients to know when a service is being created for the first
     *         time or is being updated.
     *         The Map in each pair contains a Map of String placeholder key to it's corresponding tModel.
     *         This key will be referenced from within the corresponding BusinessService from it's bindingTemplates. Each
     *         bindingTemplate references two tModels. Each bindingTemplate references unique tModels. The tModelKey value in
     *         each bindingTemplates is a key into the Map.
     *         Each Map and tModel is unique to the corresponding BusinessService. No two maps will contain the same tModel reference.
     */
    public List<Pair<BusinessService, Map<String, TModel>>> getServicesAndDependentTModels() {
        return servicesAndDependentTModels;
    }

    private void createUddiBusinessService(final BusinessService businessService,
                                           final Service wsdlService,
                                           final Collection<Pair<String, String>> allEndpointPairs,
                                           final String prependToServiceName,
                                           final String appendToServiceName) throws MissingWsdlReferenceException {
        final String serviceName = wsdlService.getQName().getLocalPart();
        String serviceLocalName = serviceName;
        if(prependToServiceName != null && !prependToServiceName.trim().isEmpty()) {
            serviceLocalName = prependToServiceName + " " + serviceLocalName;
        }
        if(appendToServiceName != null && !appendToServiceName.trim().isEmpty()){
            serviceLocalName = serviceLocalName + " " + appendToServiceName;
        }

        final String localName = serviceLocalName;//this is ok to modify as the uddi:name of the BusinessService does not map to a wsdl element
        final Map<String, TModel> modelsForService = new HashMap<String, TModel>();
        Pair<BusinessService, Map<String, TModel>> serviceToTModels = new Pair<BusinessService, Map<String, TModel>>(businessService, modelsForService);

        businessService.getName().add(getName(localName));

        BindingTemplates bindingTemplates = new BindingTemplates();
        Map<String, Port> ports = wsdlService.getPorts();
        //if the wsdl:service contains no ports, this collection is empty and not null
        for (Map.Entry<String, Port> entry : ports.entrySet()) {
            final Port wsdlPort = entry.getValue();
            try {
                final List<BindingTemplate> allNewTemplates =
                        createUddiBindingTemplate(serviceToTModels.right, wsdlPort, allEndpointPairs);
                bindingTemplates.getBindingTemplate().addAll(allNewTemplates);
            } catch (MissingWsdlReferenceException e) {
                //already logged, we ignore the bindingTemplate as it is invalid
                logger.log(Level.INFO, "Ignored bindingTemplate as the wsdl:port contains invalid references");
            } catch (NonSoapWsdlPortException e) {
                logger.log(Level.INFO, "Ignored bindingTemplate as the wsdl:port does not implement a soap binding. (" + e.getMessage() + ")");
            } catch (NonHttpBindingException e) {
                logger.log(Level.INFO, "Ignored bindingTemplate as the wsdl:port does not implement a http binding. (" + e.getMessage() + ")");
            }
        }

        if(bindingTemplates.getBindingTemplate().isEmpty()){
            final String msg = "Cannot create BusinessService as the wsdl:service does not reference any valid / supported WSDL bindings";
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
        localNameRef.setKeyValue(serviceName);  //this cannot be the layer7 name, this must be the name of the wsdl:service
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
        servicesAndDependentTModels.add(serviceToTModels);
    }

    // - PACKAGE
    /**
     * @param wsdl
     */
    WsdlToUDDIModelConverter(final Wsdl wsdl) {
        if (wsdl == null) throw new NullPointerException("wsdl cannot be null");

        this.wsdl = wsdl;
        this.businessKey = null;
    }

    List<BindingTemplate> createUddiBindingTemplate(final Map<String, TModel> serviceToTModels,
                                                    final Port wsdlPort,
                                                    final Collection<Pair<String, String>> allEndpointPairs)
            throws MissingWsdlReferenceException, NonSoapWsdlPortException, NonHttpBindingException {

        List<ExtensibilityElement> elements = wsdlPort.getExtensibilityElements();

        boolean soapFound = false;
        if(elements != null){
            for (ExtensibilityElement ee : elements) {
                if (ee instanceof javax.wsdl.extensions.soap.SOAPAddress || ee instanceof javax.wsdl.extensions.soap12.SOAP12Address) {
                    soapFound = true;
                    break;
                }
            }
        }

        boolean isHttpBinding = false;
        final List<ExtensibilityElement> bindingExtElements = wsdlPort.getBinding().getExtensibilityElements();
        if(bindingExtElements != null){
            for(ExtensibilityElement ee: bindingExtElements) {
                String endPointUri = null;
                if (ee instanceof javax.wsdl.extensions.soap.SOAPBinding) {
                    javax.wsdl.extensions.soap.SOAPBinding soapBinding = (javax.wsdl.extensions.soap.SOAPBinding) ee;
                    endPointUri = soapBinding.getTransportURI();
                }else if(ee instanceof javax.wsdl.extensions.soap12.SOAP12Binding){
                    javax.wsdl.extensions.soap12.SOAP12Binding soapBinding = (javax.wsdl.extensions.soap12.SOAP12Binding) ee;
                    endPointUri = soapBinding.getTransportURI();
                }
                if(endPointUri != null && endPointUri.equalsIgnoreCase(HTTP_SCHEMAS_SOAP_HTTP)){
                    isHttpBinding = true;
                }
            }
        }

        //We will only convert wsdl:ports which implement a soap biding
        if (!soapFound) throw new NonSoapWsdlPortException("No soap:address or soap12:address found in wsdl:port");

        if(!isHttpBinding)
            throw new NonHttpBindingException("wsdl:port does not implement a binding which has '" + HTTP_SCHEMAS_SOAP_HTTP + "' as it's transport URI");

        List<BindingTemplate> allTemplates = new ArrayList<BindingTemplate>();
        for(Pair<String, String> anEndpointPair: allEndpointPairs){
            allTemplates.add(getBindingTemplateForEndpoint(serviceToTModels, wsdlPort, anEndpointPair.left, anEndpointPair.right));
        }

        return Collections.unmodifiableList(allTemplates);
    }

    private BindingTemplate getBindingTemplateForEndpoint(final Map<String, TModel> serviceToTModels,
                                                          final Port wsdlPort,
                                                          final String gatewayURL,
                                                          final String gatewayWsdlUrl) throws MissingWsdlReferenceException {
        final BindingTemplate bindingTemplate = new BindingTemplate();

        //Using a WSDL implies a SOAP request
        final AccessPoint accessPoint = new AccessPoint();
        accessPoint.setUseType(USE_TYPE_END_POINT);
        accessPoint.setValue(gatewayURL);
        bindingTemplate.setAccessPoint(accessPoint);

        final TModelInstanceDetails tModelInstanceDetails = new TModelInstanceDetails();

        //tModel for wsdl:binding, that this wsdl:port implements
        final Binding binding = wsdlPort.getBinding();
        if(binding == null){
            final String msg = "wsdl:port '" + wsdlPort.getName() + "' contains a reference to a non existing wsdl:portType";
            logger.log(Level.INFO, msg);
            throw new MissingWsdlReferenceException(msg);
        }
        final String bindingTModelKey = createUddiBindingTModel(serviceToTModels, binding, wsdlPort, gatewayURL, gatewayWsdlUrl);
        final TModelInstanceInfo bindingTModelInstanceInfo = new TModelInstanceInfo();
        bindingTModelInstanceInfo.setTModelKey(bindingTModelKey);

        final InstanceDetails instanceDetails = new InstanceDetails();
        instanceDetails.getDescription().add(getDescription(WSDL_BINDING_INSTANCE_DESCRIPTION));
        final String wsdlPortName = wsdlPort.getName();
        instanceDetails.setInstanceParms(wsdlPortName);
        bindingTModelInstanceInfo.setInstanceDetails(instanceDetails);

        tModelInstanceDetails.getTModelInstanceInfo().add(bindingTModelInstanceInfo);

        //tModel for the wsdl:portType referenced by the wsdl:binding
        final String portTypeTModelKey = createUddiPortTypeTModel(serviceToTModels, binding.getPortType(), wsdlPort, gatewayURL, gatewayWsdlUrl);
        TModelInstanceInfo portTypeTModelInstanceInfo = new TModelInstanceInfo();
        portTypeTModelInstanceInfo.setTModelKey(portTypeTModelKey);

        tModelInstanceDetails.getTModelInstanceInfo().add(portTypeTModelInstanceInfo);

        bindingTemplate.setTModelInstanceDetails(tModelInstanceDetails);
        return bindingTemplate;
    }

    // - PRIVATE

    /**
     * Create a wsdl:binding tModel according to the spec and return the tModel key of the created tModel
     * @param binding Binding wsdl:binding to model as a tModel in UDDI
     * @param bindingUniqueValue String a unique value which is guaranteed to be unique to the bindingTemplate that this
     * wsdl:binding tModel is being created for. This is important as the conversion can create the same bindingTemplate
     * which represents a wsdl:port twice or more..one for each endpoint, so its important that we can later identify which
     * bindingTemplate requires which specific tModels. This value is how this works.
     * @return String key of the created tModel. This key is not set on the tModel. This is a placeholder key used to
     * uniquely identify the created wsdl:binding tModel
     * @throws com.l7tech.uddi.WsdlToUDDIModelConverter.MissingWsdlReferenceException if the binding contains a reference
     * to a wsdl:portType which does not exist
     */
    private String createUddiBindingTModel(final Map<String, TModel> serviceToTModels,
                                           final Binding binding,
                                           final Port wsdlPort,
                                           final String bindingUniqueValue,
                                           final String wsdlUrl) throws MissingWsdlReferenceException {
        final String bindingName = binding.getQName().getLocalPart();//+ " " + serviceOid; - don't do this, it breaks the technical note

        //A binding name is UNIQUE across all wsdl:bindings in the entire WSDL, but not unique when imports are used
        //See http://www.w3.org/TR/wsdl#_bindings
        //need to ensure a unique key is created for each tModel.
        final String key = bindingName + wsdlPort.getName() + HexUtils.encodeBase64(HexUtils.getMd5Digest(bindingUniqueValue.getBytes())) + BINDING_TMODEL_IDENTIFIER;//don't append to the end of this key

        final TModel tModel = new TModel();

        tModel.setName(getName(bindingName));

        tModel.getOverviewDoc().addAll(getOverViewDocs(wsdlUrl));
        
        final CategoryBag categoryBag = new CategoryBag();

        final KeyedReference bindingEntityReference = new KeyedReference();
        bindingEntityReference.setKeyValue("binding");
        bindingEntityReference.setTModelKey(UDDI_WSDL_TYPES);
        categoryBag.getKeyedReference().add(bindingEntityReference);

        if(binding.getPortType() == null) {
            final String msg = "wsdl:binding '" + binding.getQName().getLocalPart() + "' contains a reference to a non existing wsdl:portType";
            logger.log(Level.INFO, msg);
            throw new MissingWsdlReferenceException(msg);
        }
        final String portTypeTModelKey = createUddiPortTypeTModel(serviceToTModels, binding.getPortType(), wsdlPort, bindingUniqueValue, wsdlUrl);
        final KeyedReference portTypeReference = new KeyedReference();
        portTypeReference.setKeyName("portType reference");
        portTypeReference.setTModelKey(UDDI_WSDL_PORTTYPEREFERENCE);
        portTypeReference.setKeyValue(portTypeTModelKey);
        categoryBag.getKeyedReference().add(portTypeReference);

        final KeyedReference wsdlSpecReference = new KeyedReference();
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
        //do not set the tModelKey property. Clients expect this value to be null on a newly created tModel
        serviceToTModels.put(key, tModel);
        return key;
    }

    private List<OverviewDoc> getOverViewDocs(final String wsdlUrl){
        List<OverviewDoc> returnList = new ArrayList<OverviewDoc>();
        OverviewDoc overviewDoc = new OverviewDoc();
        overviewDoc.getDescription().add(getDescription("the original WSDL document"));
        OverviewURL overviewURL = new OverviewURL();
        overviewURL.setUseType(WSDL_INTERFACE);
        overviewURL.setValue(wsdlUrl);
        overviewDoc.setOverviewURL(overviewURL);
        returnList.add(overviewDoc);

        returnList.add(getOverviewDocTechnicalNode());

        return returnList;
    }

    /**
     * Create a wsdl:portType tModel
     *
     * @param serviceToTModels
     * @param portType
     * @param wsdlPort
     * @param bindingUniqueValue String a unique value which is guaranteed to be unique to the bindingTemplate that this
     * wsdl:binding tModel is being created for. This is important as the conversion can create the same bindingTemplate
     * which represents a wsdl:port twice or more..one for each endpoint, so its important that we can later identify which
     * bindingTemplate requires which specific tModels. This value is how this works.
     * @param wsdlUrl
     * @return String key of the created tModel. This key is not set on the tModel. This is a placeholder key used to
     * uniquely identify the created wsdl:portType tModel
     */
    private String createUddiPortTypeTModel(final Map<String, TModel> serviceToTModels,
                                            final PortType portType,
                                            final Port wsdlPort,
                                            final String bindingUniqueValue,
                                            final String wsdlUrl) {
        //calling code should not have called with null pointer
        if(portType == null) throw new NullPointerException("Missing reference to wsdl:portType");
        final String portTypeName = portType.getQName().getLocalPart();//+ " " + serviceOid; - this breaks the techincal spec - don't modify the name

        //A portName is UNIQUE across all wsdl:portTypes in the entire WSDL, but not when imports are used
        //See http://www.w3.org/TR/wsdl#_porttypes
        //need to make a unique key for the tModel
        final String key = portTypeName + wsdlPort.getName() + HexUtils.encodeBase64(HexUtils.getMd5Digest(bindingUniqueValue.getBytes())) +  PORT_TMODEL_IDENTIFIER;//Cannot append any value to this constant

        final TModel tModel = new TModel();
        tModel.setName(getName(portTypeName));

        tModel.getOverviewDoc().addAll(getOverViewDocs(wsdlUrl));

        final CategoryBag categoryBag = new CategoryBag();

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
        //do not set the tModelKey property. Clients expect this value to be null on a newly created tModel
        serviceToTModels.put(key, tModel);
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
