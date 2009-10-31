package com.l7tech.uddi;

import com.l7tech.common.uddi.guddiv3.*;
import com.l7tech.uddi.WsdlToUDDIModelConverter;
import com.l7tech.wsdl.Wsdl;

import javax.wsdl.Binding;
import javax.wsdl.Service;
import javax.wsdl.Port;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.soap12.SOAP12Address;
import javax.wsdl.extensions.soap.SOAPAddress;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

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

    public enum TMODEL_TYPE {
        WSDL_PORT_TYPE, WSDL_BINDING
    }

    /**
     * Update all placeholder tModelKey's instead of keyedReferences in the list of BusinessServices. This applies
     * to the bindingTemplates they contain, and not any category bag of the Business Services themselves
     *
     * @param businessServices the list of BusinessService s to update
     * @param dependentTModels the list of dependent TModel s which the BusinessServices depend on. All have valid
     *                         tModelKeys from the correct UDDIRegistry
     */
    static void updateBusinessServiceReferences(final List<BusinessService> businessServices,
                                                       final Map<String, TModel> dependentTModels) {

        for (BusinessService businessService : businessServices) {
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
            if (getTModelType(tModel) != TMODEL_TYPE.WSDL_BINDING) continue;

            final CategoryBag categoryBag = tModel.getCategoryBag();
            List<KeyedReference> keyedReferences = categoryBag.getKeyedReference();
            //find the portType keyedReference
            for (KeyedReference keyedReference : keyedReferences) {
                if (!keyedReference.getTModelKey().equals(WsdlToUDDIModelConverter.UDDI_WSDL_PORTTYPEREFERENCE)) continue;
                final TModel publishedTModel = dependentTModels.get(keyedReference.getKeyValue());
                keyedReference.setKeyValue(publishedTModel.getTModelKey());
            }
        }
    }

    /**
     * Work out what wsdl element the tModel represents. Either wsdl:portType or wsdl:binding
     *
     * @param tModel the TModel to get the type for
     * @return TMODEL_TYPE the type of TModel
     */
    static TMODEL_TYPE getTModelType(final TModel tModel) {
        final CategoryBag categoryBag = tModel.getCategoryBag();
        List<KeyedReference> keyedReferences = categoryBag.getKeyedReference();
        for (KeyedReference keyedReference : keyedReferences) {
            if (!keyedReference.getTModelKey().equals(WsdlToUDDIModelConverter.UDDI_WSDL_TYPES)) continue;
            final String keyValue = keyedReference.getKeyValue();
            if (keyValue.equals("portType")) return TMODEL_TYPE.WSDL_PORT_TYPE;
            if (keyValue.equals("binding")) return TMODEL_TYPE.WSDL_BINDING;
            throw new IllegalStateException("Type of TModel does not follow UDDI Technical Note: '" + keyValue + "'");
        }
        throw new IllegalStateException("TModel did not contain a KeyedReference of type " + WsdlToUDDIModelConverter.UDDI_WSDL_TYPES);
    }

    /**
     * Given the WSDL and the other supplied information, determine the URL which should be considered the end point
     * for the supplied info
     *
     * The search starts from wsdl:service down to wsdl:binding. An accessPoint is chosen if it's use type is "endPoint"
     * The rules for the accessPoint chosen are:
     * 1) Check the bindingTemplate of the wsdl:port of the supplied wsdl:service
     * 2) find any bindingTemplate which implements the correct wsdl:binding, return it's accessPoint value
     *
     * //todo work in progress
     * @param wsdl WSDL to extract end point information from
     * @return String end point
     */
    public static String extractEndPointFromWsdl(final Wsdl wsdl,
                                          final String wsdlServiceName,
                                          final String wsdlPortName,
                                          final String wsdlPortBinding) throws WsdlEndPointNotFoundException {

        //URL must come from a wsdl:port which implements this binding
        final Binding binding = wsdl.getBinding(wsdlPortBinding);
        if(binding == null) throw new WsdlEndPointNotFoundException("binding '"+wsdlPortBinding+"' not found in WSDL");

        Map<String, String> wsdlServicePortToEndPoint = new HashMap<String, String>();
        Map<String, String> wsdlPortToEndPoint = new HashMap<String, String>();
        String anyImplementingPort = null;
        for(Service wsdlService: wsdl.getServices()){
            final String svcName = wsdlService.getQName().getLocalPart();
            final boolean requestedService = svcName.equals(wsdlServiceName);

            Map<String, Port> stringPortMap = wsdlService.getPorts();
            for(Port port: stringPortMap.values()){
                final String portName = port.getName();
                final boolean requestedPort = portName.equals(wsdlPortName) && requestedService;
                Binding portBinding = port.getBinding();
                if(portBinding == null) continue;

                if(!portBinding.getQName().getLocalPart().equals(binding.getQName().getLocalPart())) continue;

                List<ExtensibilityElement> extList = port.getExtensibilityElements();
                for(ExtensibilityElement ee: extList){
                    final String endPoint;
                    if(ee instanceof SOAPAddress){
                        SOAPAddress soapAddress = (SOAPAddress) ee;
                        endPoint = soapAddress.getLocationURI();
                    } else if(ee instanceof SOAP12Address){
                        SOAP12Address soapAddress = (SOAP12Address) ee;
                        endPoint = soapAddress.getLocationURI();
                    }else{
                        endPoint = null;
                    }

                    if(endPoint == null) continue;

                    if(requestedPort) return endPoint;
                    wsdlServicePortToEndPoint.put(svcName + portName, endPoint);
                    wsdlPortToEndPoint.put(portName, endPoint);
                    anyImplementingPort = endPoint;
                }
            }
        }

        //Weve collected all end point information from the WSDL which is referenced
        //and the service requested was not found
        if(wsdlServicePortToEndPoint.containsKey(wsdlServiceName+wsdlPortName))
            return wsdlServicePortToEndPoint.get(wsdlServiceName+wsdlPortName);

        if(wsdlPortToEndPoint.containsKey(wsdlPortName)) return wsdlPortToEndPoint.get(wsdlPortName);

        if(anyImplementingPort != null) return anyImplementingPort;

        throw new WsdlEndPointNotFoundException("No soap address found for a wsdl:port which implements the '"
                + wsdlPortBinding+"' binding");
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

