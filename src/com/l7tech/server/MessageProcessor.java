/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server;

import EDU.oswego.cs.dl.util.concurrent.ReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.ReentrantWriterPreferenceReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.Sync;
import com.l7tech.common.security.xml.Session;
import com.l7tech.common.security.xml.SessionNotFoundException;
import com.l7tech.common.util.Locator;
import com.l7tech.logging.LogManager;
import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.server.policy.ServerPolicyFactory;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.service.PublishedService;
import com.l7tech.service.ServiceListener;
import com.l7tech.service.ServiceManager;
import com.l7tech.service.ServiceStatistics;
import com.l7tech.service.resolution.ServiceResolutionException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public class MessageProcessor implements ServiceListener {
    public AssertionStatus processMessage( Request request, Response response ) throws IOException, PolicyAssertionException {
        try {
            if ( _serviceManager == null ) throw new IllegalStateException( "ServiceManager is null!" );

            PublishedService service = _serviceManager.resolveService( request );

            AssertionStatus status;
            if ( service == null ) {
                logger.warning( "Service not found" );
                status = AssertionStatus.SERVICE_NOT_FOUND;
            } else if ( service.isDisabled() ) {
                logger.warning( "Service disabled" );
                status = AssertionStatus.SERVICE_DISABLED;
            } else {
                logger.finer( "Resolved service #" + service.getOid() );
                request.setParameter( Request.PARAM_SERVICE, service );

                // check if requestor provided a version number for published service
                String requestorVersion = (String)request.getParameter( Request.PARAM_HTTP_POLICY_VERSION );
                if (requestorVersion != null && requestorVersion.length() > 0) {
                    // format is policyId|policyVersion (seperated with char '|')
                    boolean wrongPolicyVersion = false;
                    int indexofbar = requestorVersion.indexOf('|');
                    if (indexofbar < 0) {
                        logger.finest("policy version passed has incorrect format");
                        wrongPolicyVersion = true;
                    } else {
                        try {
                            long reqPolicyId = Long.parseLong(requestorVersion.substring(0, indexofbar));
                            long reqPolicyVer = Long.parseLong(requestorVersion.substring(indexofbar+1));
                            if (reqPolicyVer != service.getVersion() || reqPolicyId != service.getOid()) {
                                logger.finest("policy version passed is invalid");
                                wrongPolicyVersion = true;
                            }
                        } catch (NumberFormatException e) {
                            wrongPolicyVersion = true;
                            logger.log(Level.FINE, "wrong format for policy version", e);
                        }
                    }
                    if (wrongPolicyVersion) {
                        String msg = "Wrong policy version " + requestorVersion;
                        logger.fine(msg);
                        // this will make the servlet send back the URL of the policy
                        response.setPolicyViolated(true);
                        throw new PolicyAssertionException(msg);
                    }
                } else {
                    logger.fine("Requestor did not provide policy id.");
                }

                // If an xml-enc session id is provided, make sure it's still valid
                // (because we can't wait for the XmlResponseSecurity to fail because it happens after routing)
                if (checkForInvalidXmlSessIdRef(request)) {
                    response.setParameter( Response.PARAM_HTTP_SESSION_STATUS, "invalid" );
                    logger.info("Request referred to an invalid session id. Policy will not be executed.");
                    return AssertionStatus.FALSIFIED;
                }

                // Get the server policy
                Assertion genericPolicy = service.rootAssertion();
                Long oid = new Long( service.getOid() );
                ServerAssertion serverPolicy;
                Sync read = _policyCacheLock.readLock();
                Sync write = _policyCacheLock.writeLock();
                try {
                    read.acquire();
                    serverPolicy = (ServerAssertion)_serverPolicyCache.get( oid );
                    if ( serverPolicy == null ) {
                        // Upgrade to a write lock
                        read.release();
                        write.acquire();

                        serverPolicy = ServerPolicyFactory.getInstance().makeServerPolicy( genericPolicy );
                        _serverPolicyCache.put( oid, serverPolicy );

                        write.release();
                    }
                } catch ( InterruptedException ie ) {
                    String msg = "Interrupted while acquiring policy cache lock!";
                    logger.fine( msg );
                    Thread.currentThread().interrupt();
                    throw new PolicyAssertionException( msg, ie );
                }

                // Run the policy
                getServiceStatistics( service.getOid() ).attemptedRequest();
                status = serverPolicy.checkRequest( request, response );

                if ( status == AssertionStatus.NONE ) {
                    getServiceStatistics( service.getOid() ).authorizedRequest();
                    RoutingStatus rstat = request.getRoutingStatus();
                    if ( rstat == RoutingStatus.ROUTED ) {
                        logger.fine( "Request was routed with status " + " " + status.getNumeric() + " (" + status.getMessage() + ")" );
                        getServiceStatistics( service.getOid() ).completedRequest();
                    } else if ( rstat == RoutingStatus.ATTEMPTED ) {
                        logger.severe( "Request routing failed with status " + status.getNumeric() + " (" + status.getMessage() + ")" );
                        status = AssertionStatus.FALSIFIED;
                    }
                } else {
                    logger.warning( "Policy evaluation resulted in status " + status.getNumeric() + " (" + status.getMessage() + ")" );
                }
            }

            return status;
        } catch ( ServiceResolutionException sre ) {
            logger.log(Level.SEVERE, sre.getMessage(), sre);
            return AssertionStatus.SERVER_ERROR;
        }
    }

    public ServiceStatistics getServiceStatistics( long serviceOid ) {
        Long oid = new Long( serviceOid );
        ServiceStatistics stats;
        Sync read = _statsLock.readLock();
        Sync write = _statsLock.writeLock();
        try {
            read.acquire();
            stats = (ServiceStatistics)_serviceStatistics.get(oid);
            if ( stats == null ) {
                // Upgrade read lock to write lock
                read.release();
                stats = new ServiceStatistics( serviceOid );
                write.acquire();
                _serviceStatistics.put( oid, stats );
                write.release();
            } else {
                read.release();
            }
            return stats;
        } catch ( InterruptedException ie ) {
            logger.fine( "Interrupted while acquiring statistics lock!" );
            Thread.currentThread().interrupt();
            return null;
        }
    }

    public static MessageProcessor getInstance() {
        if ( _instance == null )
            _instance = new MessageProcessor();
        return _instance;
    }

    public DocumentBuilder getDomParser() throws ParserConfigurationException {
        return _dbf.newDocumentBuilder();
    }

    public XmlPullParser getPullParser() throws XmlPullParserException {
        return _xppf.newPullParser();
    }

    private MessageProcessor() {
        // This only uses Locator because only one instance of ServiceManager must
        // be active at once.
        _serviceManager = (ServiceManager)Locator.getDefault().lookup( ServiceManager.class );
        _serviceManager.addServiceListener( this );

        _dbf = DocumentBuilderFactory.newInstance();
        _dbf.setNamespaceAware(true);
        _dbf.setValidating(false);

        try {
            _xppf = XmlPullParserFactory.newInstance();
        } catch (XmlPullParserException e) {
            throw new RuntimeException( e );
        }
        _xppf.setNamespaceAware( true );
        _xppf.setValidating( false );
    }

    /** Returns the thread-local current request. Could be null! */
    public static Request getCurrentRequest() {
        return (Request)_currentRequest.get();
    }

    public static void setCurrentRequest( Request request ) {
        _currentRequest.set( request );
    }

    public static Response getCurrentResponse() {
        return (Response)_currentResponse.get();
    }

    public static void setCurrentResponse( Response response ) {
        _currentResponse.set( response );
    }

    /**
     * This looks a http header containing a reference to an xml session id which has become invalid.
     * This prevents ServerXmlResponseSecurity to fail after the routing assertion succeeds.
     * @return If there is indeed a reference to an invalid session id, then this returns true
     */
    private boolean checkForInvalidXmlSessIdRef(Request request) {
        // RETREIVE SESSION ID
        // get the header containing the xml session id
        String sessionIDHeaderValue = (String)request.getParameter( Request.PARAM_HTTP_XML_SESSID );
        if (sessionIDHeaderValue == null || sessionIDHeaderValue.length() < 1) {
            // no trace of this, no worries then
            return false;
        }
        // retrieve the session
        Session xmlsession = null;
        try {
            xmlsession = SessionManager.getInstance().getSession(Long.parseLong(sessionIDHeaderValue));
        } catch (SessionNotFoundException e) {
            logger.log(Level.WARNING, "Exception finding session with id=" + sessionIDHeaderValue, e);
            return true;
        } catch (NumberFormatException e) {
            logger.log(Level.WARNING, "Session id is not long value : " + sessionIDHeaderValue, e);
            return true;
        }

        // upload this session into the request message's context
        request.setParameter(Request.PARAM_HTTP_XML_SESSID, xmlsession);

        return false;
    }

    public void serviceCreated( PublishedService service ) {
        // Lazy
    }

    public void serviceDeleted(PublishedService service) {
        Long oid = new Long( service.getOid() );
        _serverPolicyCache.remove( oid );
        _serviceStatistics.remove( oid );
    }

    public void serviceUpdated(PublishedService service) {
        _serverPolicyCache.remove( new Long( service.getOid() ) );
    }

    // todo make this live, when all dependencies are complete
    /**
     * resolves a service from a request. ensure cache integrity through version checking
     * @param request
     * @return
     */
    protected PublishedService resolveService(Request request) throws ServiceResolutionException {
        // logic (from wiki design page)
        // 1. request comes in soap processor
        // 2. soap processor tries to resolves using cache
        //    * 2.1. if no service resolves, do the following
        //          o 2.1.1. if no "db resolve" attempt occured for this particular request, do a "db resolve", go
        //                   back to 2.1
        //          o 2.2.2. if we already tried to do a "db resolve", return SERVICE_NOT_FOUND
        // 3. the service is resolved.
        // 4. if no "db resolve" occured to get there, do the following
        //    * 4.1. do a version check (between cache and database version) on the published service this step
        //           should be fast â a query on the version column only, dont let hibernate parse the entire
        //           service object from the database.
        //    * 4.2. if version mismatch
        //          o 4.2.1. update the cache (this service only)
        //          o 4.2.2. go back to step 2
        //    * 4.3. if version check reveals that service no longer exist (it has been deleted)
        //          o 4.3.1. delete the cached version
        //          o 4.3.2. do a "db resolve" and go back to step 2.1
        // 5. execute policy
        boolean resolvedFromDB = false;
        PublishedService service = cacheResolve(request);
        for (int i = 0; i < 2; i++) {
            if (service == null) {
                if (!resolvedFromDB) {
                    service = dbResolve(request);
                    resolvedFromDB = true;
                }
                if (service == null) {
                    logger.warning("Request does not resolve to service either against cache nor db.");
                    return null;
                }
            }

            if (resolvedFromDB) {
                return service;
            } else {
                int versioncheckres = checkVersionAgainstDB(service);
                switch (versioncheckres) {
                    case VERSIONOK:
                        return service;
                    case VERSIONOUTDATED:
                        // update the service
                        updateCache(service.getOid());
                        // back to resolve from cache
                        service = cacheResolve(request);
                        break; // will go back
                    case SERVICENOTINDB:
                        // delete cache version
                        deleteFromCache(service.getOid());
                        // try to resolve from db
                        service = null;
                        break;
                }
            }
        }
        logger.fine("**should not get here**");
        return null;
    }

    protected void deleteFromCache(long serviceOidToDelete) {
        // todo
    }

    protected void updateCache(long serviceOidToUpdate) {
        // todo
    }

    protected PublishedService cacheResolve(Request request) throws ServiceResolutionException {
        // todo
        return null;
    }

    protected PublishedService dbResolve(Request request) throws ServiceResolutionException {
        // todo
        // resolve service
        // if something is found, update or add service to cache
        return null;
    }

    protected static final int VERSIONOK = 1;
    protected static final int VERSIONOUTDATED = 2;
    protected static final int SERVICENOTINDB = 0;
    protected int checkVersionAgainstDB(PublishedService service) {
        // todo
        return SERVICENOTINDB;
    }

    private static MessageProcessor _instance = null;
    private static ThreadLocal _currentRequest = new ThreadLocal();
    private static ThreadLocal _currentResponse = new ThreadLocal();

    private Map _serverPolicyCache = new HashMap();
    private ServiceManager _serviceManager;
    private Logger logger = LogManager.getInstance().getSystemLogger();
    private Map _serviceStatistics = new HashMap();

    private ReadWriteLock _policyCacheLock = new ReentrantWriterPreferenceReadWriteLock();
    private ReadWriteLock _statsLock = new ReentrantWriterPreferenceReadWriteLock();

    private DocumentBuilderFactory _dbf;
    private XmlPullParserFactory _xppf;
}