package com.l7tech.service;

import com.l7tech.objectmodel.*;
import com.l7tech.message.Request;

import java.util.Map;
import java.util.HashMap;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: Jun 6, 2003
 *
 * Service API. Get instance of this through the Locator class.
 */
public interface ServiceManager extends EntityManager {
    public String resolveWsdlTarget(String url) throws java.rmi.RemoteException;
    public long save( PublishedService service ) throws SaveException;
    public void update( PublishedService service ) throws UpdateException;
    public void delete( PublishedService service ) throws DeleteException;

    public PublishedService resolveService( Request request );

    // NOTE:
    // add methods as they become necessary
}
