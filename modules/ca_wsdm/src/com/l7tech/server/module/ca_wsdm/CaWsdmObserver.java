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
import com.l7tech.common.xml.SoapFaultDetail;
import com.l7tech.server.event.PostRoutingEvent;
import com.l7tech.server.event.PreRoutingEvent;
import com.l7tech.server.event.RoutingEvent;
import com.l7tech.server.message.PolicyEnforcementContext;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
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
public class CaWsdmObserver implements ApplicationListener, InitializingBean, DisposableBean {

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

    /** Path of the JSAM custom assertion properties file (relative to the SSG config folder). */
    private static final String CA_WSDM_PROPS_PATH = "ca_wsdm_observer.properties";

    /** Default maximum number of characters per message body to send to WSDM Manager. */
    private static final int MESSAGE_BODY_LIMIT_DEFAULT = 5000;

    /** Default observer type (a.k.a. handler type); for display in WSDM Manager. */
    private static final int OBSERVER_TYPE_DEFAULT = 777;

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

    public void afterPropertiesSet() throws Exception {
        // Reads configuration properties from file.
        final String ssgConfigPath = System.getProperty("ssg.config.dir");
        final String propsPath = ssgConfigPath + File.separator + CA_WSDM_PROPS_PATH;

        Properties properties = new Properties();
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(propsPath);
            properties.load(fis);
            _logger.info("Loaded Observer for CA Unicenter WSDM properties from file \"" + propsPath + "\".");
        } catch (FileNotFoundException e) {
            _logger.severe("File \"" + propsPath + "\" not found. Not enabling Observer for CA Unicenter WSDM.");
            return;
        } catch (IOException e) {
            _logger.log(Level.SEVERE, "Cannot read \"" + propsPath + "\". Not enabling Observer for CA Unicenter WSDM.", e);
            return;
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
        if (observerProperties.getManagerSoapEndpoint() == null) {
            _logger.severe("Missing property \"" + ObserverProperties.CONFIG_MANAGER_SOAP_ENDPOINT +
                    "\" in \"" + propsPath + "\". Not enabling Observer for CA Unicenter WSDM.");
            _enabled = false;
            return;
        } else {
            _enabled = true;
        }

        _sendSoap = observerProperties.getSendSOAP();

        // Determines what observer type to show in WSDM Manager.
        int observerType;
        String s = properties.getProperty("com.l7tech.server.module.ca_wsdm.observerType");
        if (s == null) {
            observerType = OBSERVER_TYPE_DEFAULT;
        } else {
            try {
                observerType = Integer.parseInt(s);
            } catch (NumberFormatException e) {
                observerType = OBSERVER_TYPE_DEFAULT;
            }
        }
        if (_logger.isLoggable(Level.FINE)) {
            _logger.fine("Setting Observer type to " + observerType);
        }

        // Determines maximum number of characters per message body to send to WSDM Manager.
        s = properties.getProperty("com.l7tech.server.module.ca_wsdm.messageBodyLimit");
        if (s == null) {
            _messageBodyLimit = MESSAGE_BODY_LIMIT_DEFAULT;
        } else {
            try {
                _messageBodyLimit = Integer.parseInt(s);
            } catch (NumberFormatException e) {
                _messageBodyLimit = MESSAGE_BODY_LIMIT_DEFAULT;
            }
        }
        if (_logger.isLoggable(Level.FINE)) {
            _logger.fine("Setting message body sending limit to " + _messageBodyLimit + " characters.");
        }

        // Initializes the ODK API.
        try {
            _wsdmHandlerUtilSOAP = WsdmHandlerUtilSOAP.getWsdmHandlerUtilSOAP(observerProperties, observerType);
        } catch (ManagerEndpointNotDefinedException e) {
            _logger.log(Level.SEVERE, "Invalid CA Unicenter WSDM Manager SOAP endpoint URL. Not enabling Observer for CA Unicenter WSDM.", e);
            return;
        }
    }

    public void destroy() throws Exception {
        if (_enabled) {
            // Shuts down the ODK API threads.
            WsdmHandlerUtilSOAP.stop();
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
    }

    /** Spool request info to the WSDM Manager. */
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

    /** Spool request/fault info to the WSDM Manager. */
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
            int charsRead = 0;
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
}
