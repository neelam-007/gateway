package com.l7tech.uddi;

import com.l7tech.common.uddi.guddiv3.BusinessService;
import com.l7tech.common.uddi.guddiv3.TModel;

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
     * Publish a tModel to UDDI. May already exist.
     * <p/>
     * If the tModel does not already exist, it will be created and the tModelKey assigned to it by the UDDI
     * registry will be set on the tModel following this operation.
     *
     * If the tModel does exist the overviewDoc URL with use type = "wsdlInterface" will be compared. If they are the
     * same then the tModels match, otherwise they do not match and the tModel will be published
     *
     * When searching for the TModel in the UDDI Registry, all keyedReferences from the tModel's identifierBag
     * and categoryBag must be included in the search. The search should be exact, but case insensitive
     * @param tModelToPublish the tModel to publish.
     * @return true if the TModel was published, false otherwise i.e. it was found already in the UDDI Registry
     * @throws UDDIException any problems searching / publishing UDDI
     */
    boolean publishTModel(final TModel tModelToPublish) throws UDDIException;

    /**
     * Retrieve the tModel with the supplied key
     * @param tModelKey String tModelKey of the tModel to get
     * @return TModel of the supplied key. Null if not found
     * @throws UDDIException if any problem retireving the TModel from the UDDI registry
     */
    TModel getTModel(final String tModelKey) throws UDDIException;


}
