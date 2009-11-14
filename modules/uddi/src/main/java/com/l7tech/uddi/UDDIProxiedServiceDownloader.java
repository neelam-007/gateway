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
public class UDDIProxiedServiceDownloader {//TODO [Donal] rename to be UDDIBusinessService downloaded, as that's what it does

    private final UDDIClient uddiClient;
    private final JaxWsUDDIClient jaxWsUDDIClient;

    public UDDIProxiedServiceDownloader( final UDDIClientConfig uddiCfg ) {
        this( buildUDDIClient(uddiCfg) );
    }

    protected UDDIProxiedServiceDownloader(final UDDIClient uddiClient ) {
        this.uddiClient = uddiClient;
        if(uddiClient instanceof JaxWsUDDIClient){
            jaxWsUDDIClient = (JaxWsUDDIClient) uddiClient;
        }else{
            throw new IllegalStateException( "JaxWsUDDIClient is required." );
        }
    }

    /**
     * Get all Business Services from the supplied keys
     *
     * @param serviceKeys Set String of serviceKeys to download
     * @return Pair of a List of Business Services and a Map of tModelKeys to TModels, which the Business Services are
     * dependant apon. Never null. Neither left or right are ever null either.
     * @throws UDDIException any problems searching the UDDI Registry
     */
    public List<Pair<BusinessService, Map<String, TModel>>> getBusinessServiceModels(Set<String> serviceKeys) throws UDDIException {

        final List<Pair<BusinessService, Map<String, TModel>>> servicesToDependentTModels =
                new ArrayList<Pair<BusinessService, Map<String, TModel>>>();

        List<BusinessService> businessServices = uddiClient.getBusinessServices(serviceKeys, true);

        for(final BusinessService businessService: businessServices){
            final Map<String, TModel> tModelKeyToModel = new HashMap<String, TModel>();
            //process all bus services and collect references

            //each bindingTemplate contains all the information we need, we don't need to dig deeper
            //e.g. don't need to obtain the wsdl:binding tModel as it references the wsdl:port tModel, as the
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

    private static UDDIClient buildUDDIClient( final UDDIClientConfig uddiCfg ) {
        if(uddiCfg == null) throw new NullPointerException("uddiCfg cannot be null");

        UDDIClient uddiClient = UDDIClientFactory.getInstance().newUDDIClient( uddiCfg );
        if(!(uddiClient instanceof JaxWsUDDIClient)){
            uddiClient = new GenericUDDIClient(uddiCfg.getInquiryUrl(), uddiCfg.getPublishUrl(), uddiCfg.getSubscriptionUrl(),
                    uddiCfg.getSecurityUrl(), uddiCfg.getLogin(), uddiCfg.getPassword(),
                    UDDIClientFactory.getDefaultPolicyAttachmentVersion(), uddiCfg.getTlsConfig());
        }

        return uddiClient;
    }
}
