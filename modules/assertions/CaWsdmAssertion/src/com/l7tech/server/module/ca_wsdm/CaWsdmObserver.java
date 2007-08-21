/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.module.ca_wsdm;

import com.ca.wsdm.monitor.ManagerEndpointNotDefinedException;
import com.ca.wsdm.monitor.ObserverProperties;
import com.ca.wsdm.monitor.WsdmHandlerUtilSOAP;
import com.ca.wsdm.monitor.WsdmMessageContext;
import com.l7tech.common.RequestId;
import com.l7tech.common.http.HttpConstants;
import com.l7tech.common.message.Message;
import com.l7tech.common.message.MimeKnob;
import com.l7tech.common.message.SoapKnob;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.xml.SoapFaultDetail;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.event.PostRoutingEvent;
import com.l7tech.server.event.PreRoutingEvent;
import com.l7tech.server.event.RoutingEvent;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.util.ApplicationEventProxy;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import javax.wsdl.Operation;
import javax.xml.namespace.QName;
import java.io.*;
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

    private final ServerConfig _serverConfig;
    private final ApplicationEventProxy _applicationEventProxy;

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
        this._serverConfig = (ServerConfig)applicationContext.getBean("serverConfig", ServerConfig.class);
        this._applicationEventProxy = (ApplicationEventProxy)applicationContext.getBean("applicationEventProxy", ApplicationEventProxy.class);
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
            final Operation operation = context.getOperation();
            if (operation == null) {
                _logger.warning("Cannot determine operation in request. Probably no matching published service.");
                return;
            }
            final String operationName = context.getOperation().getName();

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
                final String charset = mimeKnob.getOuterContentType().getEncoding();
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
                            final String charset = mimeKnob.getOuterContentType().getEncoding();
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
     * @param charset   the name of a supported charset
     * @param limit     the maximum number of characters to extract
     *
     * @return extracted string content; <code>null</code> if IO error
     */
    private static String getStreamContent(final InputStream s, final String charset, final int limit) {
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


    public void initialize() throws Exception {

        // Get configuration properties from the cluster properties manager
        CaWsdmPropertiesAdaptor propsAdaptor = new CaWsdmPropertiesAdaptor(_serverConfig);
        final ObserverProperties observerProperties = propsAdaptor.getObserverProperties();

        if (observerProperties.getManagerSoapEndpoint() == null) {
            String msg = "Missing cluster property \"" +
                         CaWsdmPropertiesAdaptor.asCpName(ObserverProperties.CONFIG_MANAGER_SOAP_ENDPOINT) +
                         "\". Not enabling Observer for CA Unicenter WSDM.";
            _logger.severe(msg);
            _enabled = false;
            throw new IOException(msg);
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

        // Turn on the event spigot
        _applicationEventProxy.addApplicationListener(this);
    }

    private ObserverProperties getObserverProperties(String propsPath, Properties properties) throws IOException {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(propsPath);
            properties.load(fis);
            _logger.info("Loaded Observer for CA Unicenter WSDM properties from file \"" + propsPath + "\".");
        } catch (FileNotFoundException e) {
            throw new FileNotFoundException("File \"" + propsPath + "\" not found. Not enabling Observer for CA Unicenter WSDM.");
        } catch (IOException e) {
            throw new IOException("Cannot read \"" + propsPath + "\". Not enabling Observer for CA Unicenter WSDM.", e);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    _logger.log(Level.WARNING, "Cannot close \"" + propsPath + "\" after reading.", e);
                }
            }
        }

        final ObserverProperties observerProperties = new ObserverProperties(properties);
        return observerProperties;
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
    }

    public boolean isEnabled() {
        return _enabled;
    }

    public void destroy() throws Exception {
        if (_enabled) {
            try {
                // Shuts down the ODK API threads.
                WsdmHandlerUtilSOAP.stop();
            } finally {
                // Unsubscribe ourself from the applicationEventProxy
                if (_applicationEventProxy != null)
                    _applicationEventProxy.removeApplicationListener(this);
            }
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
            logger.log(Level.INFO, "CaWsdmObserver module is initializing a new instance");
            try {
                instance = new CaWsdmObserver(context);
                instance.initialize();
            } catch (Exception e) {
                logger.log(Level.WARNING, "CaWsdmObserver module exception while initializing: " + ExceptionUtils.getMessage(e), e);
                instance = null;
            }
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
