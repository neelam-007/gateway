/*
 * Copyright (C) 2005-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.common.service;

import com.l7tech.gateway.common.security.rbac.MethodStereotype;
import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.gateway.common.admin.Administrative;

import static org.springframework.transaction.annotation.Propagation.SUPPORTS;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.MalformedURLException;

/**
 * Defines the operations to be exposed as admin web service.
 *
 * IMPORTANT: Once the web service is published, backward compatibility must
 *            be maintaned. Therefore, do not remove or modify existing
 *            method declarations.
 */
@Secured(types= EntityType.SERVICE)
public interface ServiceAdminPublic {
    /**
     * Retrieve all available published services.
     *
     * @return array of entity headers for all existing published services.  May be empty but never null.
     * @throws FindException   if there was a problem accessing the requested information.
     */
    @Secured(stereotype=MethodStereotype.FIND_HEADERS)
    @Transactional(readOnly=true)
    @Administrative(licensed=false)            
    ServiceHeader[] findAllPublishedServices() throws FindException;

    /**
     * Overloaded findAllPublishedServices to all caller to explicitly choose whether aliases are returned in
     * the results or not. This is the only findAll method which will return aliases 
     * @param includeAliases true if the returned array should contain aliases
     * @return ServiceHeader []. If includeAliases is true then this array can contain aliases. Call isAlias
     * on each ServiceHeader to determine if it is an alias.
     * @throws FindException
     */
    @Secured(stereotype=MethodStereotype.FIND_HEADERS)
    @Transactional(readOnly=true)
    @Administrative(licensed=false)
    ServiceHeader[] findAllPublishedServices(boolean includeAliases) throws FindException;

    /**
     * Retrieve a specified published service given its service ID.
     *
     * @param oid the unique identifier of the service
     * @return the requested {@link PublishedService}, or null if no service with that service ID was found
     * @throws FindException   if there was a problem accessing the requested information.
     */
    @Secured(stereotype=MethodStereotype.FIND_ENTITY)
    @Transactional(readOnly=true)
    @Administrative(licensed=false)
    PublishedService findServiceByID(String oid) throws FindException;

    /**
     * Get a wsdl document from a URL. The WSDL document will be resolved by the gateway so that the manager
     * can get at services that are 'hidden' behind the gateway.
     * This is meant to be used when a service is originally published.
     *
     * @param url the url that the gateway will use to resolve the wsdl document. this may contain
     * userinfo type credentials
     * @return the contents resolved by this url
     *
     * @throws IOException thrown on I/O error accessing the WSDL url
     * @throws MalformedURLException thrown on malformed WSDL url
     */
    @Transactional(propagation=SUPPORTS)
    String resolveWsdlTarget(String url) throws IOException;
}
