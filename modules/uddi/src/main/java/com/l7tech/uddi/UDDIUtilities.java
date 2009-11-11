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
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
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
     * @param businessService BusinessService which contains fake tModelKey references from it's TModelInstanceInfo s
     * @param dependentTModels all TModels contained in this Map should contain real tModelKeys assiged from the UDDI
     * to which the BusnessService will ultimetely be published to
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
            throws PortNotFoundException, WsdlToUDDIModelConverter.MissingWsdlReferenceException {

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
     * Given the WSDL and the other supplied information, determine the URL which should be considered the end point
     * for the supplied info
     * <p/>
     * The search starts from wsdl:service down to wsdl:binding. An accessPoint is chosen if it's use type is "endPoint"
     * The rules for the accessPoint chosen are:
     * 1) Check the bindingTemplate of the wsdl:port of the supplied wsdl:service
     * 2) find any bindingTemplate which implements the correct wsdl:binding, return it's accessPoint value
     * <p/>
     * //todo work in progress - not needed anywhere yet - not deleting yet
     *
     * @param wsdl WSDL to extract end point information from
     * @param wsdlServiceName
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

    public static boolean isGatewayUrl(final String endPoint, final String ssgLocalHostName){

        final String hName;
        if(ssgLocalHostName.indexOf("[") != -1){
            hName = ssgLocalHostName.substring(0, ssgLocalHostName.indexOf("["));
        }else if(ssgLocalHostName.indexOf(".") != -1){
            hName = ssgLocalHostName.substring(0, ssgLocalHostName.indexOf("."));
        }else{
            hName = ssgLocalHostName;
        }

        return endPoint.indexOf(hName) != -1;

    }

    public static class WsdlEndPointNotFoundException extends Exception{
        public WsdlEndPointNotFoundException(String message) {
            super(message);
        }
    }
}

