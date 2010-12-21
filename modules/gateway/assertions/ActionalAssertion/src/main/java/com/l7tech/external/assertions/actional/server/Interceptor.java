package com.l7tech.external.assertions.actional.server;

import com.actional.GeneralUtil;
import com.actional.lg.interceptor.sdk.ClientInteraction;
import com.actional.lg.interceptor.sdk.ServerInteraction;
import com.actional.lg.interceptor.sdk.helpers.InterHelpBase;
import com.actional.lg.interceptor.sdk.helpers.InterHelpJ2ee;
import com.actional.lg.interceptor.sdk.helpers.InterHelpTrust;
import com.l7tech.message.HttpOutboundRequestFacet;
import com.l7tech.message.HttpServletRequestKnob;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.event.MessageProcessed;
import com.l7tech.server.event.MessageReceived;
import com.l7tech.server.event.PostRoutingEvent;
import com.l7tech.server.event.PreRoutingEvent;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.IOUtils;
import com.l7tech.util.Pair;
import com.l7tech.xml.SoapFaultDetail;

import javax.wsdl.Binding;
import javax.wsdl.Operation;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Class containing static methods that can handle different types of interceptor
 * events. It is this class that makes use of the Actional Interceptor SDK components.
 *
 * @author jules
 */
class Interceptor {

    // - PUBLIC

    /**
     * Report the client request data.
     *
     * @param event           the PreRoutingEvent to be processed
     * @param transmitPayload whether or not to intercept & transmit the payload
     */
    public static void handleClientRequest( final PreRoutingEvent event,
                                            final boolean transmitPayload,
                                            final String headerName ) {
        logger.log(Level.FINE, "Interceptor.handleClientRequest");
        final PolicyEnforcementContext pec = event.getContext();
        final Message requestMessage = event.getRequest();

        final ClientInteraction ci = ClientInteraction.begin();
        try {
            ci.setUrl(event.getUrl().toString());

            // group, service, operation, used for identification purposes by actional
            ci.setGroupName(event.getUrl().getHost());
            ci.setServiceName(pec.getService().getName());
            try {
                if (pec.getService().isSoap()){
                    final Pair<Binding,Operation> pair = pec.getBindingAndOperation();
                    ci.setOpName(pair.right.getName());
                }
            } catch (Exception e) {
                logger.log(Level.FINE, "Failed to determine soap operation: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            }

            // payload
            if (transmitPayload) {
                try {
                    ci.setPayload(IOUtils.slurpStream(requestMessage.getMimeKnob().getEntireMessageBodyAsInputStream()));
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Error intercepting request message payload: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                }
            }
            try {
                ci.setSize(requestMessage.getMimeKnob().getContentLength());
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error determining response size: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            }

            //don't actually need explicit display type setting at the moment, but it will guard against future API changes
            ci.setAppType(GeneralUtil.DISPLAY_TYPE_GROUP.shortValue());//old group name
            ci.setSvcType(GeneralUtil.DISPLAY_TYPE_SERVICE.shortValue());
            ci.setOpType(GeneralUtil.DISPLAY_TYPE_OP.shortValue());

            if ( headerName != null ) {
                //build the Actional Manifest header
                try {
                    final String actionalManifestHeader = InterHelpBase.writeHeader(ci, true);
                    HttpOutboundRequestFacet.getOrCreateHttpOutboundRequestKnob(requestMessage).setHeader( headerName, actionalManifestHeader );
                    // todo: store this into InterHelpBase.kLGTransport header
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Interceptor could not construct Actional manifest header. No header will be sent.");
                }                
            }
        } finally {
            ci.requestAnalyzed();
        }
    }

    /**
     * Report the client response data.
     *
     * @param event           the PostRoutingEvent to be processed
     * @param transmitPayload whether or not to intercept & transmit the payload
     */
    public static void handleClientResponse( final PostRoutingEvent event,
                                             final boolean transmitPayload ) {
        logger.log(Level.FINE, "Interceptor.handleClientResponse");
        final Message responseMessage = event.getRoutedResponse();

        final ClientInteraction ci = ClientInteraction.get();
        if (ci == null) {
            throw new InterceptorException("Null client interaction for response (not initialized by request?)");
        }
        try {
            try {
                // process soap faults
                if (responseMessage.isXml() && responseMessage.isSoap() && responseMessage.getSoapKnob().isFault()) {
                    //then construct the failure message
                    //TODO get more detailed fault
                    //TODO SoapFaultManager = context.getSoapFaultManager();
                    //TODO construct a fault containing Medium details
                    SoapFaultDetail soapFaultDetail = responseMessage.getSoapKnob().getFaultDetail();
                    ci.setFailure("SOAP fault code: " + soapFaultDetail.getFaultCode() + ". Reason: " + soapFaultDetail.getFaultString() + ".");
                    final int status = event.getHttpResponseStatus();
                    if (status == HttpURLConnection.HTTP_UNAUTHORIZED || status == HttpURLConnection.HTTP_FORBIDDEN) {
                        //trivial way of determining a "security" fault, but better than nothing
                        ci.setHasSecurityFault(true);
                    }
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error processing response message: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            }

            // payload
            if (transmitPayload) {
                try {
                    ci.setPayload(IOUtils.slurpStream(responseMessage.getMimeKnob().getEntireMessageBodyAsInputStream()));
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Error intercepting response message payload: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                }
            }
            try {
                ci.setSize(responseMessage.getMimeKnob().getContentLength());
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error determining response size: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            }
        } finally {
            ci.end();
        }
    }

    /**
     * Report the server request data.
     *
     * @param event           the MessageReceived event to be processed
     * @param transmitPayload whether or not to intercept & transmit the payload
     */
    public static void handleServerRequest( final MessageReceived event,
                                            final boolean transmitPayload,
                                            final boolean enforceTrustZone,
                                            final String httpHeaderName ) {
        logger.log(Level.FINE, "Interceptor.handleServerRequest");
        final PolicyEnforcementContext pec = event.getContext();
        final Message requestMessage = pec.getRequest();

        ServerInteraction si = ServerInteraction.begin();
        try {
            // get transport-level data from the HttpServletRequestKnob
            final HttpServletRequestKnob httpServletRequestKnob = requestMessage.getKnob(HttpServletRequestKnob.class);
            if (httpServletRequestKnob == null) {
                throw new IllegalStateException("Interceptor could not get HttpServletRequestKnob from HTTP request message.");
            }
            //enable flow IDs & deal with the Actional manifest header -- order matters here
            InterHelpJ2ee.extractTransportHeaders(si, httpServletRequestKnob.getHttpServletRequest());
            if ( httpHeaderName != null ) {
                try {
                    InterHelpBase.readHeader(httpServletRequestKnob.getHeaderSingleValue(httpHeaderName), si);
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Found more than one value for the header: " + httpHeaderName);
                }
            }

            //not sure that an authUser will ever exist at this point
            if (pec.getAuthenticationContext(requestMessage).isAuthenticated()) {
                si.setSecurityID(pec.getAuthenticationContext(requestMessage).getLastAuthenticatedUser().getName());
            }

            // request URL
            si.setUrl(httpServletRequestKnob.getRequestUrl());

            // group, service, operation, used for identification purposes by actional 
            si.setGroupName(PROVIDER_GROUP_NAME);
            si.setServiceName(pec.getService().getName());
            String operation;
            try {
                operation = pec.getService().isSoap() ? pec.getBindingAndOperation().right.getName() : httpServletRequestKnob.getRequestUri();
            } catch (Exception e) {
                operation = httpServletRequestKnob.getRequestUri();
            }
            si.setOpName(operation != null ? operation : httpServletRequestKnob.getRequestUri());

            //not required but provides the Actional Server an additional dimension along which to partition statistics
            try {
                si.setSubnode(InetAddress.getLocalHost().getCanonicalHostName());
            } catch (UnknownHostException e) {
                // do nothing
            }
            si.setPeerAddr(httpServletRequestKnob.getRemoteAddress());
            si.setUrlQuery(httpServletRequestKnob.getQueryString()); // may be empty


            // payload
            if (transmitPayload) {
                try {
                    si.setPayload(IOUtils.slurpStream(requestMessage.getMimeKnob().getEntireMessageBodyAsInputStream()));
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Error intercepting request message payload: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                }
            }
            si.setSize(httpServletRequestKnob.getHttpServletRequest().getContentLength());

            /* todo: throw if length is 0?
            if (length < 0) {
                throw new IllegalStateException("Interceptor could not establish the size of the request payload.");
            }
            */

            if (enforceTrustZone) {
                boolean trustVerified = InterHelpTrust.checkTrust(si);
                if (!trustVerified) {
                    logger.log(Level.WARNING, InterHelpTrust.TRUST_VIOLATION_MSG);
                    throw new InterceptorException(InterHelpTrust.TRUST_VIOLATION_MSG);
                }
            }

            //don't actually need explicit display type setting at the moment, but it will guard against future API changes
            si.setAppType(GeneralUtil.DISPLAY_TYPE_GROUP.shortValue());//old group name
            si.setSvcType(GeneralUtil.DISPLAY_TYPE_SERVICE.shortValue());
            si.setOpType(GeneralUtil.DISPLAY_TYPE_OP.shortValue());
        } finally {
            si.requestAnalyzed();
        }
    }


    /**
     * Report the server response data.
     *
     * @param event           the MessageProcessed event to be processed
     * @param transmitPayload whether or not to intercept & transmit the payload
     */
    public static void handleServerResponse( final MessageProcessed event,
                                             final boolean transmitPayload ) {
        logger.log(Level.FINE, "Interceptor.handleServerResponse");
        final PolicyEnforcementContext pec = event.getContext();
        final Message responseMessage = pec.getResponse();

        final ServerInteraction si = ServerInteraction.get();
        if (si == null) {
            throw new InterceptorException("Null server interaction for response (not initialized by request?)");
        }
        try {
            try {
                if (responseMessage.isXml() && responseMessage.isSoap() && responseMessage.getSoapKnob().isFault()) {
                    // process soap faults
                    //TODO get more detailed fault
                    //TODO SoapFaultManager = context.getSoapFaultManager();
                    //TODO construct a fault containing Medium details
                    SoapFaultDetail soapFaultDetail = responseMessage.getSoapKnob().getFaultDetail();
                    si.setFailure("SOAP fault code: " + soapFaultDetail.getFaultCode() + ". Reason: " + soapFaultDetail.getFaultString() + ".");
                    AssertionStatus status = event.getStatus();
                    //again, a trivial way of determining a "security" fault, but better than nothing
                    if (status == AssertionStatus.UNAUTHORIZED || status == AssertionStatus.AUTH_FAILED || status == AssertionStatus.AUTH_REQUIRED) {
                        si.setHasSecurityFault(true);
                    }
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error processing response message: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            }

            // payload
            if (transmitPayload) {
                try {
                    si.setPayload(IOUtils.slurpStream(responseMessage.getMimeKnob().getEntireMessageBodyAsInputStream()));
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Error intercepting response message payload: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                }
            }
            try {
                si.setSize(responseMessage.getMimeKnob().getContentLength());
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error determining response size: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            }
        } finally {
            si.end();
        }
    }

    // - PRIVATE

    private static final Logger logger = Logger.getLogger(Interceptor.class.getName());
    private static final String PROVIDER_GROUP_NAME = "Layer 7 SecureSpan Gateway";
}
