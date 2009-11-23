package com.l7tech.uddi;

import com.l7tech.common.uddi.guddiv3.*;
import com.l7tech.wsdl.Wsdl;
import com.l7tech.util.Pair;

import javax.wsdl.Binding;
import javax.wsdl.Service;
import javax.wsdl.Port;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.soap12.SOAP12Address;
import javax.wsdl.extensions.soap.SOAPAddress;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * Utility methods for working with UDDI data model from jax-ws
 * <p/>
 * Provides serveral updateXXX() methods to update a BusinessService to contain references to valid tModels which
 * represent the wsdl:binding and wsdl:portType, as well as other utilities
 *
 * Any method which uses jax-ws classes are package private so as to not expose these interfaces
 * 
 * @author darmstrong
 */
public class UDDIUtilities {
    private static Logger logger = Logger.getLogger(UDDIUtilities.class.getName());

    public enum TMODEL_TYPE {
        WSDL_PORT_TYPE, WSDL_BINDING
    }


    /**
     * Update fake tModelKey references with real key references
     *
     * @param businessService  BusinessService which contains fake tModelKey references from it's TModelInstanceInfo s
     * @param dependentTModels all TModels contained in this Map should contain real tModelKeys assiged from the UDDI
     *                         to which the BusnessService will ultimetely be published to
     */
    static void updateBusinessServiceReferences(final BusinessService businessService,
                                                final Map<String, TModel> dependentTModels) {

        //the business service itself has no references to update
        BindingTemplates bindingTemplates = businessService.getBindingTemplates();
        List<BindingTemplate> allTemplates = bindingTemplates.getBindingTemplate();
        for (BindingTemplate bindingTemplate : allTemplates) {
            TModelInstanceDetails tModelInstanceDetails = bindingTemplate.getTModelInstanceDetails();
            List<TModelInstanceInfo> tModelInstanceInfos = tModelInstanceDetails.getTModelInstanceInfo();
            for (TModelInstanceInfo tModelInstanceInfo : tModelInstanceInfos) {
                //it's possible we have bindingTemplates which already exist. if the referenced tModelKey
                //does not reference a placeholder, then do not process
                if(!tModelInstanceInfo.getTModelKey().endsWith(WsdlToUDDIModelConverter.BINDING_TMODEL_IDENTIFIER) &&
                        !tModelInstanceInfo.getTModelKey().endsWith(WsdlToUDDIModelConverter.PORT_TMODEL_IDENTIFIER)) continue;

                final TModel tModel = dependentTModels.get(tModelInstanceInfo.getTModelKey());
                tModelInstanceInfo.setTModelKey(tModel.getTModelKey());
            }
        }
    }

    /**
     * TModels which represent wsdl:binding elements, have a dependency on the TModel which represent
     * wsdl:portType.
     * <p/>
     * Update all binding TModels with the correct tModelKey of their dependent portType tModel.
     *
     * @param bindingTModels   List of TModels to udpate. Each tModel must represent a binding tModel
     * @param dependentTModels The Map of all TModels for WSDL being published. This contains the wsdl:portType which
     *                         must have valid keys by the time this method is called
     */
    static void updateBindingTModelReferences(final List<TModel> bindingTModels,
                                              final Map<String, TModel> dependentTModels) {
        for (TModel tModel : bindingTModels) {
            if (getTModelType(tModel, true) != TMODEL_TYPE.WSDL_BINDING) continue;

            final CategoryBag categoryBag = tModel.getCategoryBag();
            List<KeyedReference> keyedReferences = categoryBag.getKeyedReference();
            //find the portType keyedReference
            for (KeyedReference keyedReference : keyedReferences) {
                if (!keyedReference.getTModelKey().equalsIgnoreCase(WsdlToUDDIModelConverter.UDDI_WSDL_PORTTYPEREFERENCE))
                    continue;
                final TModel publishedTModel = dependentTModels.get(keyedReference.getKeyValue());
                keyedReference.setKeyValue(publishedTModel.getTModelKey());
                break;
            }
        }
    }

    static Pair<TModel, TModel> getPortTypeAndBindingTModels(final List<TModelInstanceInfo> originalInfos,
                                                             final Map<String, TModel> tModelKeyToTModel) throws UDDIException {
        //Go through each TModelInstanceInfo and copy any which look like they may represent
        // wsdl:binding and wsdl:portType elements
        final List<TModelInstanceInfo> copyAllTModelInstanceInfo = new ArrayList<TModelInstanceInfo>();
        for (TModelInstanceInfo origTModelInstanceInfo : originalInfos) {
            //Each TModelInstanceInfo can have 0..1 InstanceDetails an 0..* descriptions
            final TModelInstanceInfo copyTModelInstanceInfo = new TModelInstanceInfo();
            copyTModelInstanceInfo.setTModelKey(origTModelInstanceInfo.getTModelKey());//this is required

            if (origTModelInstanceInfo.getInstanceDetails() != null) {//this may represent a wsdl:binding
                //InstanceDetails can have:
                // 0..* descriptions - will copy
                // choice of InstanceParams OR 0..* OverviewDocs and 0..1 InstanceParams - only care about first option -
                // as the WSDL to UDDI technical note requires a mandatory instanceParam - which is the wsdl:binding localName
                final InstanceDetails origInstanceDetails = origTModelInstanceInfo.getInstanceDetails();
                if (origInstanceDetails.getInstanceParms() == null || origInstanceDetails.getInstanceParms().trim().isEmpty()) {
                    logger.log(Level.INFO, "Ignoring TModelInstanceInfo '" + origTModelInstanceInfo.getTModelKey() +
                            "' as it contains an instanceDetails which a null or empty instanceParams element");
                    continue;
                }

                final InstanceDetails copyInstanceDetails = new InstanceDetails();
                copyInstanceDetails.setInstanceParms(origInstanceDetails.getInstanceParms());//string value
                for (Description origDesc : origInstanceDetails.getDescription()) {
                    final Description copyDesc = new Description();
                    copyDesc.setLang(origDesc.getLang());
                    copyDesc.setValue(origDesc.getValue());
                    copyInstanceDetails.getDescription().add(copyDesc);
                }
                copyTModelInstanceInfo.setInstanceDetails(copyInstanceDetails);
            }

            copyAllTModelInstanceInfo.add(copyTModelInstanceInfo);
        }

        //now we have every candidate TModelInstanceInfo from the found bindingTemplate
        //need to ensure it contains a wsdl:binding and wsdl:portType tModelKey references
        //once we have found this, we know which tModels we need to copy
        TModel bindingTModel = null;
        TModel portTypeTModel = null;
        for (TModelInstanceInfo tModelInstanceInfo : copyAllTModelInstanceInfo) {
            final TModel tModel = tModelKeyToTModel.get(tModelInstanceInfo.getTModelKey());
            if (tModel == null)
                throw new UDDIException("tModel with tModelKey " + tModelInstanceInfo.getTModelKey()
                        + " referenced from the tModelInstanceInfo was not referenced from BusinessService");

            //what type of tModel is it?
            final UDDIUtilities.TMODEL_TYPE type = UDDIUtilities.getTModelType(tModel, false);
            if (type == null) continue;
            switch (type) {
                case WSDL_BINDING:
                    bindingTModel = tModel;
                    break;
                case WSDL_PORT_TYPE:
                    portTypeTModel = tModel;
                    break;
            }
        }

        if (bindingTModel == null || portTypeTModel == null) {
            final String msg = "Could not find a valid wsdl:binding and wsdl:portype tModel references from the bindingTemplate's tModelInstanceDetails element";
            logger.log(Level.WARNING, msg);
            throw new UDDIException(msg);
        }

        return new Pair<TModel, TModel>(portTypeTModel, bindingTModel);
    }

    /**
     * Update the tModel references in the bindingTemplate to the real references contained in the actual
     * tModels in the dependentTModels map
     *
     * This should only be used for BindingTemplates created by SSG code and should not be supplied a BindingTemplate
     * which came from the Wsdl class after a WSDL was parsed.
     *
     * @param bindingTemplate The BindingTemplate must contain only the standard 2 tModelInstanceInfo's in its
     * tModelInstanceDetails element.
     * @param dependentTModels
     */
    static void updateBindingTemplateTModelReferences(final BindingTemplate bindingTemplate,
                                                      final Map<String, TModel> dependentTModels) throws UDDIException {

        final List<TModelInstanceInfo> tModelInstanceDetails = bindingTemplate.getTModelInstanceDetails().getTModelInstanceInfo();
        if(tModelInstanceDetails.isEmpty()) return;//nothing to do

        final Pair<TModel, TModel> portAndBindingTModel = getPortTypeAndBindingTModels(tModelInstanceDetails, dependentTModels);

        for(TModelInstanceInfo tii: tModelInstanceDetails){
            final TModel tModel = dependentTModels.get(tii.getTModelKey());
            if(tModel == null)//this is a coding error
                throw new IllegalStateException("No tModel found for key: " + tii.getTModelKey());
            tii.setTModelKey(tModel.getTModelKey());
        }
    }

    /**
     * Work out what wsdl element the tModel represents. Either wsdl:portType or wsdl:binding
     *
     * @param tModel the TModel to get the type for
     * @param throwIfNotFound
     * @return TMODEL_TYPE the type of TModel
     */
    static TMODEL_TYPE getTModelType(final TModel tModel, boolean throwIfNotFound) {
        final CategoryBag categoryBag = tModel.getCategoryBag();
        List<KeyedReference> keyedReferences = categoryBag.getKeyedReference();
        for (KeyedReference keyedReference : keyedReferences) {
            if (!keyedReference.getTModelKey().equalsIgnoreCase(WsdlToUDDIModelConverter.UDDI_WSDL_TYPES)) continue;
            final String keyValue = keyedReference.getKeyValue();
            if (keyValue.equals("portType")) return TMODEL_TYPE.WSDL_PORT_TYPE;
            if (keyValue.equals("binding")) return TMODEL_TYPE.WSDL_BINDING;
            throw new IllegalStateException("Type of TModel does not follow UDDI Technical Note: '" + keyValue + "'");
        }
        if(throwIfNotFound) throw new IllegalStateException("TModel did not contain a KeyedReference of type " + WsdlToUDDIModelConverter.UDDI_WSDL_TYPES);
        return null;
    }

    /**
     * From the WSDL, extract out a BindingTemplate based on the wsdlPortName and wsdlPortBinding
     * <p/>
     * The URLs are required in the dependent tModels which will also be created.
     *
     * None of the UDDI Objects contain real keys
     *
     * @param wsdl
     * @param wsdlPortName a wsdl:port name is unique in a WSDL document
     * @param wsdlPortBinding a wsdl:binding name is unique in a WSDL document
     * @param protectedServiceExternalURL
     * @param protectedServiceWsdlURL
     * @return Pair of BindingTemplate to it's dependent tModels
     */
    static Pair<BindingTemplate, Map<String, TModel>> createBindingTemplateFromWsdl(final Wsdl wsdl,
                                                                                    final String wsdlPortName,
                                                                                    final String wsdlPortBinding,
                                                                                    final String protectedServiceExternalURL,
                                                                                    final String protectedServiceWsdlURL)
            throws PortNotFoundException, WsdlToUDDIModelConverter.MissingWsdlReferenceException, WsdlToUDDIModelConverter.NonSoapBindingFoundException {

        Port foundPort = null;
        Port candidatePort = null;
        outer: for(Service wsdlService: wsdl.getServices()){
            Map<String, Port> ports = wsdlService.getPorts();
            for (Map.Entry<String, Port> entry : ports.entrySet()) {
                if(entry.getKey().equalsIgnoreCase(wsdlPortName)){
                    foundPort = entry.getValue();
                    break outer;
                }
                if(entry.getValue().getBinding().getQName().getLocalPart().equalsIgnoreCase(wsdlPortBinding)){
                    candidatePort = entry.getValue();
                    if(foundPort != null) break outer;
                }
            }
        }

        final Port wsdlPort = (foundPort != null)? foundPort: candidatePort;
        if (wsdlPort == null) throw new PortNotFoundException("Cannot find any wsdl:port by the name of '" + wsdlPortName+
                " or any port which implements the binding '" + wsdlPortBinding);
        
        final WsdlToUDDIModelConverter modelConverter =
                new WsdlToUDDIModelConverter(wsdl, protectedServiceWsdlURL, protectedServiceExternalURL);

        final Map<String, TModel> keysToModels = new HashMap<String, TModel>();
        final BindingTemplate template = modelConverter.createUddiBindingTemplate(keysToModels, wsdlPort);
        return new Pair<BindingTemplate, Map<String, TModel>>(template, keysToModels);
    }

    static class PortNotFoundException extends Exception{
        public PortNotFoundException(String message) {
            super(message);
        }
    }

    /**
     * Get a UDDIBindingImplementionInfo which contains information from UDDI on the UDDI information found
     * from a bindingTemplate which implements the wsdlBinding we are interested in.
     * <p/>
     * We request the specific wsdl:port implementation, but if it is not found, then the first bindingTemplate
     * found which implements the wsdl:binding value of wsdlBinding is returned.
     * <p/>
     * When a bindingTemplate is found, the tModel it references which represents the wsdl:binding is found, and from
     * it the WSDL URL is extracted.
     * <p/>
     * Contents of the returned UDDIBindingImplementionInfo should be validated by caller
     *
     * @param uddiClient   UDDIClient configured for the correct uddi registry
     * @param serviceKey   String serviceKey belonging to the above uddi registry
     * @param wsdlPortName name of the wsdl:port, whose corresponding bindingTemplate's accessPoint's endPoint URL
     *                     location should be obtained
     * @return String URL of the protected service's end point from UDDI. Null if not found
     * @throws UDDIException any problems searching UDDI
     */
    public static UDDIBindingImplementionInfo getUDDIBindingImplInfo(final UDDIClient uddiClient,  //TODO [Donal] change to a UDDIClientConfig - just create an instance of JaxWsUDDIClient
                                                                     final String serviceKey,
                                                                     final String wsdlPortName,
                                                                     final String wsdlBinding) throws UDDIException {

        final JaxWsUDDIClient jaxWsUDDIClient;
        if (uddiClient instanceof JaxWsUDDIClient) {
            jaxWsUDDIClient = (JaxWsUDDIClient) uddiClient;
        } else {
            throw new IllegalStateException("JaxWsUDDIClient is required.");
        }

        final BusinessService businessService = jaxWsUDDIClient.getBusinessService(serviceKey);
        if (businessService == null) {
            logger.log(Level.INFO, "No BusinessService found for serviceKey: " + serviceKey);
            return null;
        }

        final BindingTemplates bindingTemplates = businessService.getBindingTemplates();
        if (bindingTemplates == null) {
            logger.log(Level.INFO, "Service contains no bindingTemplate elements. Serivce key: " + serviceKey);
            return null;
        }

        //this is searching for any bindingTemplate which implements the binding
        BindingTemplate foundTemplate = null;
        //record each tModelKey incase we need to search through them
        final Set<String> tModelKeys = new HashSet<String>();
        final Map<String, Set<String>> bindingKeyToReferencedTModels = new HashMap<String, Set<String>>();
        final Map<String, BindingTemplate> bindingKeyToTemplate = new HashMap<String, BindingTemplate>();
        String wsdlBindingTModelKey = null;//record which binding tmodel should provide the wsdl url
        outer:
        for (BindingTemplate bt : bindingTemplates.getBindingTemplate()) {
            //find the BindingTemplate which represents the wsdl:port
            //this information is stored as an instance param in a tModelInstanceInf-> instanceDetails element
            final TModelInstanceDetails tModelInstanceDetails = bt.getTModelInstanceDetails();
            if (tModelInstanceDetails == null) continue;

            bindingKeyToTemplate.put(bt.getBindingKey(), bt);
            final Set<String> allTModelsForBinding = new HashSet<String>();
            bindingKeyToReferencedTModels.put(bt.getBindingKey(), allTModelsForBinding);

            for (TModelInstanceInfo tModelInstanceInfo : tModelInstanceDetails.getTModelInstanceInfo()) {
                final InstanceDetails instanceDetails = tModelInstanceInfo.getInstanceDetails();
                tModelKeys.add(tModelInstanceInfo.getTModelKey());
                allTModelsForBinding.add(tModelInstanceInfo.getTModelKey());

                if (instanceDetails == null) continue;//not the binding instance info
                final String instanceParam = instanceDetails.getInstanceParms();
                if (instanceParam == null || instanceParam.trim().isEmpty()) continue;

                if (instanceParam.equals(wsdlPortName)) {
                    foundTemplate = bt;
                    wsdlBindingTModelKey = tModelInstanceInfo.getTModelKey();
                    break outer;
                }
            }
        }

        final UDDIBindingImplementionInfo returnInfo = new UDDIBindingImplementionInfo();
        final Collection<TModel> allFoundModels = jaxWsUDDIClient.getTModels(tModelKeys);
        if (foundTemplate == null) {
            logger.log(Level.INFO, "Service with serviceKey " + serviceKey + " does not contain a bindingTemplate which maps to the wsdl:port with name: " + wsdlPortName);
            logger.log(Level.INFO, "Searching for a bindingTemplate which implements the wsdl:binding with name: " + wsdlBinding);

            //find the first tModel which implements the wsdl:binding, then find the bindingTemplate which references it
            TModel applicableTModel = null;
            for (TModel tModel : allFoundModels) {
                final TMODEL_TYPE tModelType = getTModelType(tModel, false);
                if (tModelType != TMODEL_TYPE.WSDL_BINDING) continue;

                if (tModel.getName().getValue().equals(wsdlBinding)) {
                    applicableTModel = tModel;
                    break;
                }
            }

            if (applicableTModel == null) {
                logger.log(Level.INFO, "No bindingTemplate found which implements the wsdl:binding with name: " + wsdlBinding);
                return null;
            }

            //Get it's bindingTemplate
            String foundBindingKey = null;
            for (Map.Entry<String, Set<String>> entry : bindingKeyToReferencedTModels.entrySet()) {
                if (entry.getValue().contains(applicableTModel.getTModelKey())) {
                    foundBindingKey = entry.getKey();
                    break;
                }
            }

            if (foundBindingKey == null)//this is a coding error/ uddi got updated during action
                throw new IllegalStateException("Could not find bindingTemplate referenced from BusinessService");

            if (!bindingKeyToTemplate.containsKey(foundBindingKey)) //this is a coding error/ uddi got updated during action
                throw new IllegalStateException("Could not find bindingTemplate referenced from BusinessService");

            wsdlBindingTModelKey = applicableTModel.getTModelKey();

            foundTemplate = bindingKeyToTemplate.get(foundBindingKey);
            //find out which wsdl:port this bindingTemplate represents
            //we know that the getTModelInstanceDetails cannot be null due to above loop logic
            for (TModelInstanceInfo tModelInstanceInfo : foundTemplate.getTModelInstanceDetails().getTModelInstanceInfo()) {
                if (!tModelInstanceInfo.getTModelKey().equals(applicableTModel.getTModelKey())) continue;
                final InstanceDetails instanceDetails = tModelInstanceInfo.getInstanceDetails();
                final String implementingWsdlPortName = instanceDetails.getInstanceParms();
                returnInfo.setImplementingWsdlPort(implementingWsdlPortName);
            }
        } else {
            returnInfo.setImplementingWsdlPort(wsdlPortName);//unchanged
        }

        //Get the binding TModel which will tell us the wsdl url
        if (wsdlBindingTModelKey == null) throw new IllegalStateException("wsdl:binding tModelKey not found");
        TModel wsdlBindingTModel = null;
        for (TModel tModel : allFoundModels) {
            if (!tModel.getTModelKey().equals(wsdlBindingTModelKey)) continue;
            wsdlBindingTModel = tModel;
        }

        if (wsdlBindingTModel == null) throw new IllegalStateException("wsdl:binding tModel not found");
        final String wsdlUrl = extractWsdlUrl(wsdlBindingTModel);
        if (wsdlUrl == null || wsdlUrl.trim().isEmpty())
            throw new UDDIException("Invalid / Unsupported wsdl:binding tModel. No wsdlInterface overviewDoc found. tModelKey: " + wsdlBindingTModel.getTModelKey());

        final AccessPoint accessPoint = foundTemplate.getAccessPoint();
        if (accessPoint == null) {
            logger.log(Level.INFO, "Service with serviceKey " + serviceKey + " contians a bindingTemplate which maps to the wsdl:port with name: " + wsdlPortName +
                    " but it does not contain an accessPoint element");
            return null;
        }

        if (!accessPoint.getUseType().equalsIgnoreCase("endPoint") && !accessPoint.getUseType().equalsIgnoreCase("http")) {
            logger.log(Level.INFO, "Service with serviceKey " + serviceKey + " contians a bindingTemplate which maps to the wsdl:port with name: " + wsdlPortName +
                    " but it does not contain an accessPoint element with a useType of 'endPoint' or 'http'");
            return null;
        }

        returnInfo.setEndPoint(accessPoint.getValue());
        returnInfo.setImplementingWsdlUrl(wsdlUrl);
        return returnInfo;
    }

    public static String extractWsdlUrl(final TModel bindingTModel){
        if(bindingTModel == null) throw new NullPointerException("bindingTModel cannot be null");

        for(OverviewDoc overviewDoc: bindingTModel.getOverviewDoc()){
            if(overviewDoc.getOverviewURL().getUseType().equalsIgnoreCase(WsdlToUDDIModelConverter.WSDL_INTERFACE)){
                return overviewDoc.getOverviewURL().getValue();
            }
        }

        return null;
    }

    public static class UDDIBindingImplementionInfo{

        public String getEndPoint() {
            return endPoint;
        }

        public void setEndPoint(String endPoint) {
            this.endPoint = endPoint;
        }

        public String getImplementingWsdlPort() {
            return implementingWsdlPort;
        }

        public void setImplementingWsdlPort(String implementingWsdlPort) {
            if(implementingWsdlPort == null || implementingWsdlPort.trim().isEmpty())
                throw new IllegalArgumentException("implementingWsdlPort cannot be null or empty");
            
            this.implementingWsdlPort = implementingWsdlPort;
        }

        public String getImplementingWsdlUrl() {
            return implementingWsdlUrl;
        }

        public void setImplementingWsdlUrl(String implementingWsdlUrl) {
            if(implementingWsdlUrl == null || implementingWsdlUrl.trim().isEmpty())
                throw new IllegalArgumentException("implementingWsdlUrl cannot be null or empty");

            this.implementingWsdlUrl = implementingWsdlUrl;
        }

        private String endPoint;
        private String implementingWsdlPort;
        private String implementingWsdlUrl;
    }

    /**
     * Validate that a wsdl:port name is contained within the supplied WSDL
     * <p/>
     * This can be used to determine if  WSDL can go under the control of a UDDI Business Service.
     * The wsdl parameters should have been obtained from UDDI
     *
     * Supports namespaces
     * 
     * @param wsdl
     * @param wsdlServiceName
     * @param wsdlPortName
     * @return true if the wsdl implements the wsdl:port name, false otherwise
     * @throws WsdlEndPointNotFoundException
     */
    public static boolean validatePortBelongsToWsdl(final Wsdl wsdl,
                                                    final String wsdlServiceName,
                                                    final String wsdlServiceNamespace,
                                                    final String wsdlPortName,
                                                    final String wsdlBinding,
                                                    final String wsdlBindingNamespace) {
        if(wsdl == null) throw new NullPointerException("wsdl cannot be null");
        if(wsdlServiceName == null || wsdlServiceName.trim().isEmpty()) throw new IllegalArgumentException("wsdlServiceName cannot be null or empty");
        if(wsdlPortName == null || wsdlPortName.trim().isEmpty()) throw new IllegalArgumentException("wsdlPortName cannot be null or empty");
        if(wsdlBinding == null || wsdlBinding.trim().isEmpty()) throw new IllegalArgumentException("wsdlBinding cannot be null or empty");
        
        final boolean useServiceNamespace = wsdlServiceNamespace != null && !wsdlServiceNamespace.trim().isEmpty();
        final boolean useBindingNamespace = wsdlBindingNamespace != null && !wsdlBindingNamespace.trim().isEmpty();

        for (Service wsdlService : wsdl.getServices()){
            if(!wsdlService.getQName().getLocalPart().equalsIgnoreCase(wsdlServiceName)){
                logger.log(Level.FINE, "No wsdl:service name match. Wsdl service name is '" +
                        wsdlService.getQName().getLocalPart()+"' requested service name is '" + wsdlServiceName+"'");
                continue;
            }

            if(useServiceNamespace && !wsdlService.getQName().getNamespaceURI().equalsIgnoreCase(wsdlServiceNamespace)){
                logger.log(Level.FINE, "No wsdl:service namespace match. Wsdl service namespace is '" +
                        wsdlService.getQName().getNamespaceURI()+"' requested service namespace is '" + wsdlServiceNamespace+"'");
                continue;
            }

            final Map<String, Port> stringPortMap = wsdlService.getPorts();
            for(Port port: stringPortMap.values()){
                if(!port.getName().equalsIgnoreCase(wsdlPortName)){
                    logger.log(Level.FINE, "No wsdl:port name match. Wsdl port name is '" +
                            port.getName()+"' requested port name is '" + wsdlPortName+"'");
                    continue;
                }

                final Binding binding = port.getBinding();
                if(!binding.getQName().getLocalPart().equalsIgnoreCase(wsdlBinding)) {
                    logger.log(Level.FINE, "No wsdl:binding name match. Wsdl binding name is '" +
                            binding.getQName().getLocalPart()+"' requested binding name is '" + wsdlBinding+"'");
                    continue;
                }
                if(useBindingNamespace && !binding.getQName().getNamespaceURI().equalsIgnoreCase(wsdlBindingNamespace)) {
                    logger.log(Level.FINE, "No wsdl:binding namespace match. Wsdl binding namespace is '" +
                            binding.getQName().getNamespaceURI()+"' requested binding namespace is '" + wsdlBindingNamespace+"'");
                    continue;
                }
                return true;
            }
        }

        return false;
    }

    /**
     * Given the WSDL and the other supplied information, determine the URL which should be considered the end point
     * for the supplied info
     * <p/>
     * The search starts from wsdl:service down to wsdl:binding. An accessPoint is chosen if it's use type is "endPoint"
     * The rules for the accessPoint chosen are:
     * 1) Check the bindingTemplate of the wsdl:port of the supplied wsdl:service
     * 2) find any bindingTemplate which implements the correct wsdl:binding, return it's accessPoint value
     * <p/>
     *
     * @param wsdl WSDL to extract end point information from
     * @param wsdlServiceName        //todo delete when sure no use for this
     * @param wsdlPortName
     * @param wsdlPortBinding
     * @return String end point
     * @throws com.l7tech.uddi.UDDIUtilities.WsdlEndPointNotFoundException
     */
    public static String extractEndPointFromWsdl(final Wsdl wsdl,
                                                 final String wsdlServiceName,
                                                 final String wsdlPortName,
                                                 final String wsdlPortBinding) throws WsdlEndPointNotFoundException {

        //URL must come from a wsdl:port which implements this binding
        final Binding binding = wsdl.getBinding(wsdlPortBinding);
        if (binding == null)
            throw new WsdlEndPointNotFoundException("binding '" + wsdlPortBinding + "' not found in WSDL");

        Map<String, String> wsdlServicePortToEndPoint = new HashMap<String, String>();
        Map<String, String> wsdlPortToEndPoint = new HashMap<String, String>();
        String anyImplementingPort = null;
        for (Service wsdlService : wsdl.getServices()) {
            final String svcName = wsdlService.getQName().getLocalPart();
            final boolean requestedService = svcName.equals(wsdlServiceName);

            Map<String, Port> stringPortMap = wsdlService.getPorts();
            for (Port port : stringPortMap.values()) {
                final String portName = port.getName();
                final boolean requestedPort = portName.equals(wsdlPortName) && requestedService;
                Binding portBinding = port.getBinding();
                if (portBinding == null) continue;

                if (!portBinding.getQName().getLocalPart().equals(binding.getQName().getLocalPart())) continue;

                List<ExtensibilityElement> extList = port.getExtensibilityElements();
                for (ExtensibilityElement ee : extList) {
                    final String endPoint;
                    if (ee instanceof SOAPAddress) {
                        SOAPAddress soapAddress = (SOAPAddress) ee;
                        endPoint = soapAddress.getLocationURI();
                    } else if (ee instanceof SOAP12Address) {
                        SOAP12Address soapAddress = (SOAP12Address) ee;
                        endPoint = soapAddress.getLocationURI();
                    } else {
                        endPoint = null;
                    }

                    if (endPoint == null) continue;

                    if (requestedPort) return endPoint;
                    wsdlServicePortToEndPoint.put(svcName + portName, endPoint);
                    wsdlPortToEndPoint.put(portName, endPoint);
                    anyImplementingPort = endPoint;
                }
            }
        }

        //Weve collected all end point information from the WSDL which is referenced
        //and the service requested was not found
        if (wsdlServicePortToEndPoint.containsKey(wsdlServiceName + wsdlPortName))
            return wsdlServicePortToEndPoint.get(wsdlServiceName + wsdlPortName);

        if (wsdlPortToEndPoint.containsKey(wsdlPortName)) return wsdlPortToEndPoint.get(wsdlPortName);

        if (anyImplementingPort != null) return anyImplementingPort;

        throw new WsdlEndPointNotFoundException("No soap address found for a wsdl:port which implements the '"
                + wsdlPortBinding + "' binding");
    }

    public static class WsdlEndPointNotFoundException extends Exception{
        public WsdlEndPointNotFoundException(String message) {
            super(message);
        }
    }
}

