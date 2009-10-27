package com.l7tech.uddi;

import com.l7tech.common.uddi.guddiv3.*;
import com.l7tech.util.Pair;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 *
 * Responsible for downloading proxied business service information from UDDI and making the info available it in the
 * UDDI api data model
 *
 * @author darmstrong
 */
public class UDDIProxiedServiceDownloader {

    private UDDIClient uddiClient;

    public UDDIProxiedServiceDownloader(UDDIClient uddiClient) {
        this.uddiClient = uddiClient;
    }

    /**
     * Find all Business Services which contain the supplied generalkeyword.
     *
     * @param generalkeyword the unique identifier to find all Business Services in a UDDI Registry which originated
     * from the same WSDL for a Published Service from the Gateway. Requiredd
     * @return Pair of a List of Business Services and a Map of tModelKeys to TModels, which the Business Services are
     * dependant apon. Neverl null. Neither left or right are ever null either.
     * @throws UDDIException any problems searching the UDDI Registry
     */
    public Pair<List<BusinessService>, Map<String, TModel>> downloadAllBusinessServicesForService(
            final String generalkeyword) throws UDDIException {

        if(generalkeyword == null) throw new NullPointerException("generalKeyword cannot be null");
        if(generalkeyword.trim().isEmpty()) throw new IllegalArgumentException("generalKeyword cannot be the empty string");
        
        List<BusinessService> businessServices = uddiClient.findMatchingBusinessServices(generalkeyword);

        Map<String, TModel> tModelKeyToModel = new HashMap<String, TModel>();
        //process all bus services and collect references

        for(final BusinessService businessService: businessServices){
             //each bindingTemplate contains all the information we need, we don't need to dig deeper
            //e.g. don't need to obtain the wsdl:binding tModel as it references the wsdl:port tModel, as the
            //bindingTemplate references both of them
            BindingTemplates bindingTemplates = businessService.getBindingTemplates();
            for(final BindingTemplate bindingTemplate: bindingTemplates.getBindingTemplate()){
                final TModelInstanceDetails tModelInstanceDetails = bindingTemplate.getTModelInstanceDetails();
                //our process of this is based on the technical note using wsdls in uddi, we don't worry about
                //any other info which may be there
                //http://www.oasis-open.org/committees/uddi-spec/doc/tn/uddi-spec-tc-tn-wsdl-v202-20040631.htm
                for(final TModelInstanceInfo tModelInstanceInfo: tModelInstanceDetails.getTModelInstanceInfo()){

                    //spec does not allow null keys, they will exist
                    final String tModelKey = tModelInstanceInfo.getTModelKey();
                    if(!tModelKeyToModel.containsKey(tModelKey)){
                        final TModel tModel = uddiClient.getTModel(tModelKey);
                        tModelKeyToModel.put(tModelKey, tModel);
                    }
                }
            }
        }

        return new Pair<List<BusinessService>, Map<String, TModel>>(businessServices, tModelKeyToModel);
    }
}
