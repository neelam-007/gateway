package com.l7tech.server.module.ca_wsdm;

import com.ca.wsdm.monitor.ManagerEndpointNotDefinedException;
import com.ca.wsdm.monitor.ObserverProperties;
import com.ca.wsdm.monitor.WsdmHandlerUtilSOAP;
import com.ca.wsdm.monitor.WsdmMessageContext;
import com.l7tech.common.http.HttpConstants;
import com.l7tech.gateway.common.RequestId;
import com.l7tech.message.Message;
import com.l7tech.message.MimeKnob;
import com.l7tech.message.SoapKnob;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.server.event.PostRoutingEvent;
import com.l7tech.server.event.PreRoutingEvent;
import com.l7tech.server.event.RoutingEvent;
import com.l7tech.server.event.system.Started;
import com.l7tech.server.event.system.Starting;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.module.ModularAssertionModule;
import com.l7tech.server.policy.module.AssertionModuleRegistrationEvent;
import com.l7tech.server.util.ApplicationEventProxy;
import com.l7tech.server.util.EventChannel;
import com.l7tech.util.Config;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Pair;
import com.l7tech.xml.SoapFaultDetail;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import javax.wsdl.Binding;
import javax.wsdl.Operation;
import javax.xml.namespace.QName;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An Observer for CA Unicenter WSDM listens on routing events and feeds
 * request/response info to the WSDM Manager through the Observer Development
 * Kit (ODK) API.
 *
 * @author alex
 * @author rmak
 */
public class CaWsdmObserver implements ApplicationListener {
    protected static final Logger logger = Logger.getLogger(CaWsdmObserver.class.getName());

    static {
        disableLocalRequestLogger();
    }

    private static void disableLocalRequestLogger() {
        try {
            // Suppresses harmless empty SEVERE logs from CA Unicenter WSDM ODK.
            org.apache.log4j.Logger locLogger = org.apache.log4j.Logger.getLogger("LOCAL_REQUEST_LOG");
            if (locLogger == null)
                return;

            Method setLevel = locLogger.getClass().getMethod("setLevel", org.apache.log4j.Level.class);
            setLevel.invoke(locLogger, org.apache.log4j.Level.OFF);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unable to turn off empty SEVERE logs from CA Unicenter WSDM ODK LOCAL_REQUEST_LOG: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        }
    }

    /** A map that automatically purges the eldest entries when a size limit is reached. */
    private class CappedLinkedHashMap<K,V> extends LinkedHashMap<K,V> {
        private final int _maxEntries;

        public CappedLinkedHashMap(final int maxEntries) {
            _maxEntries = maxEntries;
        }

        protected boolean removeEldestEntry(Map.Entry eldest) {
           return size() > _maxEntries;
        }
    }

    private static final Logger _logger = Logger.getLogger(CaWsdmObserver.class.getName());

    /** This module's instance of the CaWsdmObserver. */
    private static CaWsdmObserver instance = null;

    private final Config _config;
    private final ApplicationEventProxy _applicationEventProxy;
    private final EventChannel _messageProcessingEventChannel;
    private CaWsdmPropertiesAdaptor propsAdaptor;

    private boolean _initializationAttempted = false;

    /** Determines whether to enable observer. */
    private boolean _enabled;

    /** Maximum number of characters per message body to send to WSDM Manager.
        The purpose is to limit excessive network usage. */
    private int _messageBodyLimit;

    /** Determines whether to extract message body for sending to WSDM Manager.
        Note that the actual decision to send is within the ODK API. */
    private boolean _sendSoap;

    /** The helper object from ODK API. */
    private WsdmHandlerUtilSOAP _wsdmHandlerUtilSOAP;

    /** Map to store WsdmMessageContext objects for correlating response to
        request. The map is capped in case responses arrive too slow and
        requests accumulate and use too much memory. */
    private Map<RequestId, WsdmMessageContext> _wsdmMessageContextMap =
            Collections.synchronizedMap(new CappedLinkedHashMap<RequestId, WsdmMessageContext>(5000));

    /**
     * Create a CaWsdmObserver instance.
     *
     * @param applicationContext the Spring application context.  Required.
     */
    public CaWsdmObserver(ApplicationContext applicationContext) {
        this._config = applicationContext.getBean("serverConfig", Config.class);
        this._applicationEventProxy = applicationContext.getBean("applicationEventProxy", ApplicationEventProxy.class);
        this._messageProcessingEventChannel = applicationContext.getBean("messageProcessingEventChannel", EventChannel.class);
        _applicationEventProxy.addApplicationListener(this);
        _messageProcessingEventChannel.addApplicationListener(this);
    }

    /**
     * Spool request info to the WSDM Manager.
     *
     * @param preRoutingEvent the event to report.  Required
     */
    private void handlePreRoutingEvent(PreRoutingEvent preRoutingEvent) {
        try {
            final PolicyEnforcementContext context = preRoutingEvent.getContext();
            final Message request = context.getRequest();
            if (! request.isSoap()) {
                if (_logger.isLoggable(Level.FINE)) {
                    _logger.fine("Non-SOAP request ignored.");
                }
                return;
            }

            // Determines operation name.
            final Pair<Binding,Operation> pair = context.getBindingAndOperation();
            if (pair == null) {
                _logger.warning("Cannot determine operation in request. Probably no matching published service.");
                return;
            }
            final String operationName = pair.right.getName();

            // Determine operation namespace.
            QName[] names = request.getSoapKnob().getPayloadNames();
            if (names == null || names.length == 0) {
                _logger.warning("No namespace found in request.");
                return;
            } else if (names.length > 1) {
                _logger.warning("Using the first of multiple namespace URIs in request: " + Arrays.toString(names));
            }
            final String operationNameSpace = names[0].getNamespaceURI();

            final String requestorLocation = request.getTcpKnob().getRemoteHost();
            final String serviceUrl = preRoutingEvent.getUrl().toString();
            final MimeKnob mimeKnob = request.getMimeKnob();
            final long requestSize = mimeKnob.getContentLength();

            final WsdmMessageContext wsdmMessageContext = new WsdmMessageContext();
            wsdmMessageContext.setObservationType(WsdmHandlerUtilSOAP.SERVER_OBSERVATION_TYPE);
            wsdmMessageContext.setOperationName(operationName);
            wsdmMessageContext.setOperationNameSpace(operationNameSpace);
            wsdmMessageContext.setPortName(serviceUrl);
            wsdmMessageContext.setRequestorLocation(requestorLocation);
            wsdmMessageContext.setRequestSize(requestSize);

            // We will perform the time consuming task of extracting the
            // message body only if the ODK API may be sending it.
            if (_sendSoap) {
                // Extracts the message body.
                final Charset charset = mimeKnob.getOuterContentType().getEncoding();
                final String requestBody = getStreamContent(mimeKnob.getEntireMessageBodyAsInputStream(), charset, _messageBodyLimit);
                wsdmMessageContext.setRequestMessage(requestBody);
            }

            final String operationType = _wsdmHandlerUtilSOAP.spoolRequest(wsdmMessageContext);
            if (_logger.isLoggable(Level.FINE)) {
                _logger.fine("Spooled request to CA Unicenter WSDM Manager." +
                        " (operationName=" + operationName +
                        ", operationNameSpace=" + operationNameSpace +
                        ", serviceUrl=" + serviceUrl +
                        ", requestorLocation=" + requestorLocation +
                        ", requestSize=" + requestSize + ")");
            }

            if (WsdmHandlerUtilSOAP.OPER_TYPE_REQUEST_RESPONSE.equals(operationType)) {
                // Saves the WsdmMessageContext for spooling the response; using request ID as correlation.
                final RequestId requestId = context.getRequestId();
                _wsdmMessageContextMap.put(requestId, wsdmMessageContext);
                if (_logger.isLoggable(Level.FINEST)) {
                    _logger.finest("Request WsdmMessageContext cached. (operationType=" +
                            operationType + ", map size=" + _wsdmMessageContextMap.size() + ")");
                }
            } else {
                if (_logger.isLoggable(Level.FINEST)) {
                    _logger.finest("Request WsdmMessageContext not cached. (operationType = " + operationType + ")");
                }
            }
        } catch (Exception e) {
            _logger.log(Level.WARNING, "Failed to spool request to CA Unicenter WSDM Manager: " + e);
        }
    }

    /**
     * Spool request/fault info to the WSDM Manager.
     *
     * @param postRoutingEvent the event to report.  Required
     */
    private void handlePostRoutingEvent(PostRoutingEvent postRoutingEvent) {
        try {
            final PolicyEnforcementContext context = postRoutingEvent.getContext();
            final Message response = context.getResponse();
            if (! response.isSoap()) {
                if (_logger.isLoggable(Level.FINE)) {
                    _logger.fine("Non-SOAP response ignored.");
                }
                return;
            }

            final RequestId requestId = context.getRequestId();
            final WsdmMessageContext wsdmMessageContext = _wsdmMessageContextMap.remove(requestId);
            if (wsdmMessageContext == null) {
                _logger.warning("Missing WsdmMessageContext. Response not spooled to CA Unicenter WSDM Manager.");
            } else {
                if (postRoutingEvent.getHttpResponseStatus() >= HttpConstants.STATUS_ERROR_RANGE_START &&
                    postRoutingEvent.getHttpResponseStatus() < HttpConstants.STATUS_ERROR_RANGE_END) {
                    try {
                        final SoapKnob soapKnob = response.getSoapKnob();
                        if (soapKnob.isFault()) {
                            SoapFaultDetail soapFault = soapKnob.getFaultDetail();
                            _wsdmHandlerUtilSOAP.spoolFault(wsdmMessageContext,
                                                            soapFault.getFaultActor(),
                                                            soapFault.getFaultCode(),
                                                            soapFault.getFaultString(),
                                                            soapFault.getFaultDetail() == null ? null : new String[]{soapFault.getFaultDetail().toString()});
                            if (_logger.isLoggable(Level.FINE)) {
                                _logger.fine("Spooled fault to CA Unicenter WSDM Manager." +
                                        " (actor=" + soapFault.getFaultActor() +
                                        ", code=" + soapFault.getFaultCode() +
                                        ", string=" + soapFault.getFaultString() +
                                        ", detail=" + soapFault.getFaultDetail() + ")");
                            }
                        }
                    } catch (Exception e) {
                        _logger.log(Level.WARNING, "Failed to spool fault to CA Unicenter WSDM Manager.", e);
                    }
                } else {
                    try {
                        final MimeKnob mimeKnob = context.getResponse().getMimeKnob();
                        final long responseSize = mimeKnob.getContentLength();
                        wsdmMessageContext.setResponseSize(responseSize);

                        // We will perform the time consuming task of extracting the
                        // message body only if the ODK API may be sending it.
                        if (_sendSoap) {
                            // Extracts the message body.
                            final Charset charset = mimeKnob.getOuterContentType().getEncoding();
                            final String responseBody = getStreamContent(mimeKnob.getEntireMessageBodyAsInputStream(), charset, _messageBodyLimit);
                            wsdmMessageContext.setResponseMessage(responseBody);
                        }

                        _wsdmHandlerUtilSOAP.spoolResponse(wsdmMessageContext);
                        if (_logger.isLoggable(Level.FINE)) {
                            _logger.fine("Spooled response to CA Unicenter WSDM Manager. (responseSize=" + responseSize + ")");
                        }
                    } catch (Exception e) {
                        _logger.log(Level.WARNING, "Failed to spool response to CA Unicenter WSDM Manager:" + e);
                    }
                }
            }
        } catch (Exception e) {
            _logger.log(Level.WARNING, "Failed to spool response to CA Unicenter WSDM Manager: " + e);
        }
    }

    /**
     * Extracts the string content of a given <code>InputStream</code>.
     *
     * @param s         the input stream
     * @param charset   a supported charset
     * @param limit     the maximum number of characters to extract
     *
     * @return extracted string content; <code>null</code> if IO error
     */
    private static String getStreamContent(final InputStream s, final Charset charset, final int limit) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(s, charset));
            StringBuilder sb = new StringBuilder();
            final char[] buf = new char[1024];
            int charsRemaining = limit;
            int charsRead;
            while (charsRemaining > 0 && (charsRead = reader.read(buf)) > 0) {
                final int charsToAppend = charsRemaining < charsRead ? charsRemaining : charsRead;
                sb.append(buf, 0, charsToAppend);
                charsRemaining -= charsToAppend;
            }
            return sb.toString();
        } catch (IOException e) {
            _logger.log(Level.WARNING, "Failed to extract message body.", e);
            return null;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    _logger.log(Level.WARNING, "Cannot close input stream after reading message body:" + e);
                }
            }
        }
    }

    /** @return the ProperteisAdaptor that provides the CA WSDM observer and SOMMA properties from the DB. Never null. */
    public synchronized CaWsdmPropertiesAdaptor getPropertiesAdaptor() {
        if (propsAdaptor == null)
            propsAdaptor = new CaWsdmPropertiesAdaptor( _config );
        return propsAdaptor;
    }

    // Runs on the Spring event delivery thread that told us our module had just been loaded.
    // Typically this is the timer thread in ServerAssertionRegistry.
    // This spawns an init thread, starts it, and waits for it to finish.
    private void initializeIfNeeded() {
        synchronized (this) {
            if (_initializationAttempted)
                return;
            _initializationAttempted = true;
        }

        logger.log(Level.INFO, "CaWsdmObserver module is initializing a new instance");
        Thread initThread = null;
        try {
            // Ensure that initialize runs on a thread with the context classloader set to the modular assertion classloader
            final Throwable[] initError = new Throwable[] { null };
            Runnable initJob = new Runnable() {
                public void run() {
                    try {
                        initializeOnInitThread();
                    } catch (Exception e) {
                        initError[0] = e;
                    }
                }
            };
            initThread = new Thread(initJob, "CA WSDM module initialization");
            initThread.setContextClassLoader(getClass().getClassLoader());
            initThread.start();
            initThread.join();
            initThread = null;
            if (initError[0] != null)
                throw new RuntimeException("Error while initializing CaWsdmObserver module: " + ExceptionUtils.getMessage(initError[0]), initError[0]);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("interrupted while initializing CaWsdmObserver module", e);
        } catch (Throwable t) {
            logger.log(Level.SEVERE, "CaWsdmObserver threw exception while initializing: " + ExceptionUtils.getMessage(t), t);
            _enabled = false;
        } finally {
            if (initThread != null) {
                try {
                    initThread.interrupt();
                    initThread.join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    // Runs on the init thread, with context classloader set to this class's classloader
    private void initializeOnInitThread() throws Exception {

        // Get configuration properties from the cluster properties manager
        CaWsdmPropertiesAdaptor propsAdaptor = getPropertiesAdaptor();
        final ObserverProperties observerProperties = propsAdaptor.getObserverProperties();

        String endpoint = observerProperties.getManagerSoapEndpoint();
        if (endpoint == null || endpoint.trim().length() < 1 || CaWsdmPropertiesAdaptor.DEFAULT_SOAP_ENDPOINT.equals(endpoint)) {
            String msg = "Observer for CA Unicenter WSDM is NOT enabled: Please customize the manager SOAP endpoint by editing " +
                         "the cluster property \"" +
                         CaWsdmPropertiesAdaptor.asCpName(ObserverProperties.CONFIG_MANAGER_SOAP_ENDPOINT) +
                         "\" and then restart the Gateway.";
            _logger.severe(msg);
            _enabled = false;
            synchronized (this) {
                instance = null;
            }
            return;
        } else {
            _enabled = true;
        }

        _sendSoap = observerProperties.getSendSOAP();

        // Determines what observer type to show in WSDM Manager.
        int observerType = propsAdaptor.getObserverType();

        // Determines maximum number of characters per message body to send to WSDM Manager.
        _messageBodyLimit = propsAdaptor.getMessageBodyLimit();

        // Initializes the ODK API.
        try {
            _wsdmHandlerUtilSOAP = WsdmHandlerUtilSOAP.getWsdmHandlerUtilSOAP(observerProperties, observerType);
        } catch (ManagerEndpointNotDefinedException e) {
            _logger.log(Level.SEVERE, "Invalid CA Unicenter WSDM Manager SOAP endpoint URL. Not enabling Observer for CA Unicenter WSDM.", e);
        }
    }

    public void onApplicationEvent(ApplicationEvent event) {
        if (_enabled && event instanceof RoutingEvent) {
            if (event instanceof PreRoutingEvent) {
                final PreRoutingEvent preRoutingEvent = (PreRoutingEvent) event;
                handlePreRoutingEvent(preRoutingEvent);
            } else if (event instanceof PostRoutingEvent) {
                final PostRoutingEvent postRoutingEvent = (PostRoutingEvent) event;
                handlePostRoutingEvent(postRoutingEvent);
            }
        }
        if (_initializationAttempted)
            return;

        if (event instanceof AssertionModuleRegistrationEvent) {
            AssertionModuleRegistrationEvent regEvent = (AssertionModuleRegistrationEvent)event;
            if (regEvent.getModule() instanceof ModularAssertionModule) {
                final ModularAssertionModule assMod = (ModularAssertionModule)regEvent.getModule();
                Set<? extends Assertion> protos = assMod.getAssertionPrototypes();
                if (protos.size() > 0) {
                    Assertion proto = protos.iterator().next();
                    if (proto.getClass().getClassLoader() == getClass().getClassLoader()) {
                        // Our module has just been registered.  Time to do our delayed initialization.
                        initializeIfNeeded();
                    }
                }
            }
        } else if (event instanceof Starting || event instanceof Started) {
            // Make sure we initialize properly on startup (too early to get our own registration event) 
            initializeIfNeeded();
        }
    }

    public boolean isEnabled() {
        return _enabled;
    }

    public void destroy() throws Exception {
        try {
            if (_enabled) {
                // Shuts down the ODK API threads.
                WsdmHandlerUtilSOAP.stop();
            }
        } finally {
            // Unsubscribe ourself from the applicationEventProxy
            if (_applicationEventProxy != null)
                _applicationEventProxy.removeApplicationListener(this);
            if (_messageProcessingEventChannel != null)
                _messageProcessingEventChannel.removeApplicationListener(this);
        }
    }

    /**
     * Get the current instance, if there is one.
     *
     * @return  the current instance, created when onModuleLoaded() was called, or null if there isn't one.
     */
    public static CaWsdmObserver getInstance() {
        return instance;
    }

    /*
     * Called by the ServerAssertionRegistry when the module containing this class is first loaded
     */
    public static synchronized void onModuleLoaded(ApplicationContext context) {
        if (instance != null) {
            logger.log(Level.WARNING, "CaWsdmObserver module is already initialized");
        } else {
            instance = new CaWsdmObserver(context);
        }
    }

    /*
     * Called reflectively by module class loader when module is unloaded, to ask us to clean up any globals
     * that would otherwise keep our instances from getting collected.
     */
    public static synchronized void onModuleUnloaded() {
        if (instance != null) {
            logger.log(Level.INFO, "CaWsdmObserver module is shutting down");
            try {
                instance.destroy();
            } catch (Exception e) {
                logger.log(Level.WARNING, "CaWsdmObserver module threw exception on shutdown: " + ExceptionUtils.getMessage(e), e);
            } finally {
                instance = null;
            }
        }
    }
}
