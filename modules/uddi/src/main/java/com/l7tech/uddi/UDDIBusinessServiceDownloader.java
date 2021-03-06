package com.l7tech.uddi;

import com.l7tech.common.uddi.guddiv3.*;
import com.l7tech.util.Pair;

import java.util.*;

/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 *
 * Responsible for downloading proxied business service information from UDDI and making the info available it in the
 * UDDI api data model
 *
 * @author darmstrong
 */
class UDDIBusinessServiceDownloader {
    private final JaxWsUDDIClient jaxWsUDDIClient;

    UDDIBusinessServiceDownloader( final JaxWsUDDIClient jaxClient) {
        this.jaxWsUDDIClient = jaxClient;
    }

    /**
     * Get all Business Services from the supplied keys
     *
     * @param serviceKeys Set String of serviceKeys to download
     * @return Pair of a List of Business Services and a Map of tModelKeys to TModels, which the Business Services are
     * dependant apon. Never null. Neither left or right are ever null either.
     * @throws UDDIException any problems searching the UDDI Registry
     */
    List<Pair<BusinessService, Map<String, TModel>>> getBusinessServiceModels(Set<String> serviceKeys) throws UDDIException {

        final List<Pair<BusinessService, Map<String, TModel>>> servicesToDependentTModels =
                new ArrayList<Pair<BusinessService, Map<String, TModel>>>();

        List<BusinessService> businessServices = jaxWsUDDIClient.getBusinessServices(serviceKeys, true);

        for(final BusinessService businessService: businessServices){
            final Map<String, TModel> tModelKeyToModel = new HashMap<String, TModel>();
            //process all bus services and collect references

            //each bindingTemplate contains all the information we need, we don't need to dig deeper
            //e.g. don't need to obtain the wsdl:binding or the wsdl:port tModel, as the
            //bindingTemplate references both of them
            BindingTemplates bindingTemplates = businessService.getBindingTemplates();
            if(bindingTemplates != null){
                for(final BindingTemplate bindingTemplate: bindingTemplates.getBindingTemplate()){
                    final TModelInstanceDetails tModelInstanceDetails = bindingTemplate.getTModelInstanceDetails();
                    //our process of this is based on the technical note using wsdls in uddi, we don't worry about
                    //any other info which may be there
                    //http://www.oasis-open.org/committees/uddi-spec/doc/tn/uddi-spec-tc-tn-wsdl-v202-20040631.htm
                    for(final TModelInstanceInfo tModelInstanceInfo: tModelInstanceDetails.getTModelInstanceInfo()){
                        //spec does not allow null keys, they will exist
                        final String tModelKey = tModelInstanceInfo.getTModelKey();
                        final TModel tModel = jaxWsUDDIClient.getTModel(tModelKey);
                        //todo turn this into one query for both keyes
                        tModelKeyToModel.put(tModelKey, tModel);
                    }
                }
            }
            final Pair<BusinessService, Map<String, TModel>> aServiceToItsModels =
                    new Pair<BusinessService, Map<String, TModel>>(businessService, tModelKeyToModel);
            servicesToDependentTModels.add(aServiceToItsModels);
        }

        return servicesToDependentTModels;
    }
}
