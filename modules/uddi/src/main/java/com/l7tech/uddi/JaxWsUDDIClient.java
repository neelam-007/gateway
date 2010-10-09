package com.l7tech.uddi;

import com.l7tech.common.uddi.guddiv3.BusinessService;
import com.l7tech.common.uddi.guddiv3.TModel;
import com.l7tech.common.uddi.guddiv3.BindingTemplate;

import java.util.Set;
import java.util.List;
import java.util.Collection;

/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 *
 * Package private UDDIClient only for internal use of the UDDI module
 *
 * This interface is to stop the jax-ws classes from being exposed outside of the package.
 * @author darmstrong
 */
interface JaxWsUDDIClient {

    /**
     * Find all BusinessServices with the supplied keys
     *
     * @param serviceKeys Set String of serviceKeys to find. Cannot be null.
     * @param allowInvalidKeys if true then an invalid key will not caus a UDDIException to be thrown. This does not
     * stop other UDDIExceptions from being thrown.
     * @return List of BusinessServices which were found. Not guaranteed to be the same size as the serviceKeys set
     * @throws UDDIException if problem searching UDDI
     */                      
    List<BusinessService> getBusinessServices(final Set<String> serviceKeys, final boolean allowInvalidKeys) throws UDDIException;
    
    /**
     * Publish a Business Service to UDDI. The Business Service may already exist. This is known not by searching
     * UDDI but my whether or not the BusinessService has it's serviceKey property set. Null means it has not
     * been published to UDDI yet
     * <p/>
     * If the BusinessService does not already exist it will be created and the serviceKey will be assigned by
     * the UDDI registry and set on the BusinessService following this operation.
     *
     * @param businessService the Business Service to publish
     * @return true if the BusinessService was created, false otherwise as it already existed
     * @throws UDDIException any problems searching / publishing UDDI
     */
    boolean publishBusinessService(final BusinessService businessService) throws UDDIException;

    /**
     * Save the bindingTemplate.
     *
     * @param bindingTemplate BindingTemplate to save. It's bindingKey will be set after the save. Required
     * @throws UDDIException any UDDI problems
     */
    void publishBindingTemplate(final BindingTemplate bindingTemplate) throws UDDIException;

    /**
     * Publish a list of BindingTemplates to UDDI. All BindingTemplates will be saved in the same publish operation.
     *
     * @param bindingTemplates
     * @throws UDDIException
     */
    void publishBindingTemplate(final List<BindingTemplate> bindingTemplates) throws UDDIException;

    /**
     * Publish a tModel.
     *
     * @param tModelToPublish TModel to publish.
     * @throws UDDIException any problems publishing to UDDI
     */
    void publishTModel(final TModel tModelToPublish) throws UDDIException;

    /**
     * Retrieve the tModel with the supplied key
     * @param tModelKey String tModelKey of the tModel to get. Required.
     * @return TModel of the supplied key. Null if not found
     * @throws UDDIException if any problem retireving the TModel from the UDDI registry
     */
    TModel getTModel(final String tModelKey) throws UDDIException;

    /**
     * Retrieve all the tModels represented by the supplied keys
     * @param tModelKeys Set string of tModels to retrieve
     * @return Collection of found tModels
     * @throws UDDIException if problem searching
     */
    Collection<TModel> getTModels(final Set<String> tModelKeys) throws UDDIException;

    /**
     * Get the BusinessService for the supplied key
     * @param serviceKey String serviceKey of BusinessService to get. Required
     * @return BusinessService, never null as an exception is thrown if the business service is not found.
     * @throws UDDIException if problem searching UDDI or if the serviceKey is not known to the UDDI Registry
     */
    BusinessService getBusinessService(final String serviceKey) throws UDDIException;

    /**
     * Update an existing BusinessService. Simply save it as it is
     * @param businessService
     * @throws UDDIException
     */
    void updateBusinessService(final BusinessService businessService) throws UDDIException;

}
