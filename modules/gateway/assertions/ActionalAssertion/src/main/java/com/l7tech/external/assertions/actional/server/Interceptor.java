package com.l7tech.external.assertions.actional.server;

import com.actional.GeneralUtil;
import com.actional.lg.interceptor.sdk.ClientInteraction;
import com.actional.lg.interceptor.sdk.ServerInteraction;
import com.actional.lg.interceptor.sdk.helpers.InterHelpBase;
import com.actional.lg.interceptor.sdk.helpers.InterHelpJ2ee;
import com.actional.lg.interceptor.sdk.helpers.InterHelpTrust;
import com.l7tech.message.HttpServletRequestKnob;
import com.l7tech.message.Message;
import com.l7tech.message.MessageKnob;
import com.l7tech.message.SoapKnob;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.event.MessageProcessed;
import com.l7tech.server.event.MessageReceived;
import com.l7tech.server.event.PostRoutingEvent;
import com.l7tech.server.event.PreRoutingEvent;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.external.assertions.actional.server.InterceptorException;
import com.l7tech.server.policy.assertion.ServerHttpRoutingAssertion;
import com.l7tech.util.IOUtils;
import com.l7tech.xml.SoapFaultDetail;

import javax.servlet.http.HttpServletRequest;
import javax.wsdl.Operation;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Class containing static methods that can handle different types of interceptor
 * events. It is this class that makes use of the Actional Interceptor SDK components.
 *
 * @author jules
 */
public class Interceptor {

    private static final Logger logger = Logger.getLogger(Interceptor.class.getName());
    private static final String PROVIDER_GROUP_NAME = "Layer 7 SecureSpan Gateway";
    private static final String ACTIONAL_LGTRANSPORT_HTTPHEADERNAME = InterHelpBase.kLGTransport;
    private static final String ACTIONAL_HEADERVALUE_VARIABLE = "actional.httpheader.value";

    /**
     * Report the client request data.
     *
     * @param event           the PreRoutingEvent to be processed
     * @param transmitPayload whether or not to intercept & transmit the payload
     */
    public static void handleClientRequest(PreRoutingEvent event, boolean transmitPayload) {
        logger.log(Level.FINE, "Interceptor.handleClientRequest");

        //collect all the information we need to populate the ClientInteraction object
        final ServerHttpRoutingAssertion serverHttpRoutingAssertion = (ServerHttpRoutingAssertion) event.getSource();
        final PolicyEnforcementContext pec = event.getContext();
        final Message requestMessage = pec.getRequest();

        Operation operation;
        long length;
        byte[] payload = null;
        String serviceName;                                         

        try {
            if (!requestMessage.isSoap()) {//ignore non-soap requests for now
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "Interceptor ignoring non-soap request.");
                }
                return;
            }

            //get the SOAP Operation
            operation = pec.getOperation();
            if (operation == null) {
                logger.log(Level.WARNING, "Interceptor cannot determine operation in SOAP request. Using service name.");
            }

            serviceName = pec.getService().getName();


            //get the payload....if configured to do so
            //otherwise, just the content-length
            if (transmitPayload) {
                payload = IOUtils.slurpStream(requestMessage.getMimeKnob().getEntireMessageBodyAsInputStream());
                length = payload.length;
            } else {
                //get the content-length only
                length = requestMessage.getMimeKnob().getContentLength();
            }

        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to acquire nescessary request data to populate ClientInteraction." + e);
            return;
        }

        //populate the ClientInteraction
        ClientInteraction ci = ClientInteraction.begin();

        //set the group name of outgoing client requests to the hostname to which we are sending the requests
        //this value will get overwritten by a downstream Agent if it exists
        ci.setGroupName(event.getUrl().getHost());
        ci.setServiceName(serviceName);


        if (operation != null) {
            ci.setOpName(operation.getName());
        } else {
            ci.setOpName(serviceName);
        }
        ci.setUrl(event.getUrl().toString());
        //build the Actional Manifest header
        String actionalManifestHeader = null;
        try {
            actionalManifestHeader = InterHelpBase.writeHeader(ci, true);
            pec.setVariable(ACTIONAL_HEADERVALUE_VARIABLE, actionalManifestHeader);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Interceptor could not construct Actional manifest header. No header will be sent.");
        }

        if (payload != null) {
            ci.setPayload(payload);
        } else {
            ci.setSize(length);
        }

        //don't actually need explicit display type setting at the moment, but it will guard against future API changes
        ci.setAppType(GeneralUtil.DISPLAY_TYPE_GROUP.shortValue());//old group name
        ci.setSvcType(GeneralUtil.DISPLAY_TYPE_SERVICE.shortValue());
        ci.setOpType(GeneralUtil.DISPLAY_TYPE_OP.shortValue());
        ci.requestAnalyzed();
    }

    /**
     * Report the client response data.
     *
     * @param event           the PostRoutingEvent to be processed
     * @param transmitPayload whether or not to intercept & transmit the payload
     */
    public static void handleClientResponse(PostRoutingEvent event, boolean transmitPayload) {
        logger.log(Level.FINE, "Interceptor.handleClientResponse");

        //collect all the information we need to populate the ClientInteraction object
        final PolicyEnforcementContext pec = event.getContext();
        final Message responseMessage = pec.getResponse();

        SoapKnob soapKnob;
        long length;
        byte[] payload = null;
        String failureMessage = null;
        try {

            if (!responseMessage.isSoap()) {//ignore non-soap responses for now
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "Interceptor ignoring non-soap response.");
                }
                return;
            }

            soapKnob = responseMessage.getSoapKnob();
            if (soapKnob.isFault()) {
                //then construct the failure message
                SoapFaultDetail soapFaultDetail = soapKnob.getFaultDetail();
                //TODO get more detailed fault
                //TODO SoapFaultManager = context.getSoapFaultManager();
                //TODO construct a fault containing Medium details
                failureMessage = "SOAP fault code: " + soapFaultDetail.getFaultCode() + ". Reason: " + soapFaultDetail.getFaultString() + ".";
            }

            if (transmitPayload) {
                payload = IOUtils.slurpStream(responseMessage.getMimeKnob().getEntireMessageBodyAsInputStream());
                length = payload.length;
            } else {
                //get the content-length only
                length = responseMessage.getMimeKnob().getContentLength();
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to acquire nescessary response data to populate ClientInteraction." + e);
            return;
        }

        //populate the ClientInteraction
        ClientInteraction ci = ClientInteraction.get();

        if (failureMessage != null) {
            ci.setFailure(failureMessage);
            final int status = event.getHttpResponseStatus();
            if (status == HttpURLConnection.HTTP_UNAUTHORIZED || status == HttpURLConnection.HTTP_FORBIDDEN) {
                //trivial way of determining a "security" fault, but better than nothing
                ci.setHasSecurityFault(true);
            }
        }

        if (payload != null) {
            ci.setPayload(payload);
        } else {
            ci.setSize(length);
        }

        ci.end();
    }

    /**
     * Report the server request data.
     *
     * @param event           the MessageReceived event to be processed
     * @param transmitPayload whether or not to intercept & transmit the payload
     */
    public static void handleServerRequest(MessageReceived event, boolean transmitPayload, boolean enforceTrustZone) {
        logger.log(Level.FINE, "Interceptor.handleServerRequest");

        //collect all the information we need to populate the ServerInteraction object
        final PolicyEnforcementContext pec = event.getContext();
        final Message requestMessage = pec.getRequest();

        //is this an HTTP request ? does this even need to be checked?
        if (!requestMessage.isHttpRequest()) {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Interceptor ignoring non-http request.");
            }
            return;
        }

        //get the HttpServletRequestKnob
        final MessageKnob messageKnob = requestMessage.getKnob(HttpServletRequestKnob.class);
        if (messageKnob == null) {
            throw new IllegalStateException("Interceptor could not get HttpServletRequestKnob from HTTP request message.");
        }

        final HttpServletRequestKnob httpServletRequestKnob = (HttpServletRequestKnob) messageKnob;

        String actionalManifestHeader;
        Operation operation;
        byte[] payload = null;
        int length;
        String subNodeName;
        String serviceName;
        HttpServletRequest httpServletRequest;
        try {
            actionalManifestHeader = httpServletRequestKnob.getHeaderSingleValue(ACTIONAL_LGTRANSPORT_HTTPHEADERNAME);
            httpServletRequest = httpServletRequestKnob.getHttpServletRequest();

            if (!requestMessage.isSoap()) {//ignore non-soap requests for now
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "Interceptor ignoring non-soap request.");
                }
                return;
            }

            //get the SOAP Operation
            operation = pec.getOperation();
            if (operation == null) {
                logger.log(Level.WARNING, "Interceptor cannot determine operation in SOAP request. Using service name.");
            }

            serviceName = pec.getService().getName();

            //not required but provides the Actional Server an additional dimension along which to partition statistics
            subNodeName = InetAddress.getLocalHost().getCanonicalHostName();

            if (transmitPayload) {
                payload = IOUtils.slurpStream(requestMessage.getMimeKnob().getEntireMessageBodyAsInputStream());
                length = payload.length;
            } else {
                //must get the size of the message from somewhere else
                length = Integer.parseInt(httpServletRequestKnob.getHeaderSingleValue("content-length"));
            }

            if (length < 0) {
                throw new IllegalStateException("Interceptor could not establish the size of the request payload.");
            }

        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to acquire nescessary request data to populate ServerInteraction." + e);
            return;
        }

        //populate the ServerInteraction
        ServerInteraction si = ServerInteraction.begin();

        //enable flow IDs & deal with the Actional manifest header -- order matters here
        InterHelpJ2ee.extractTransportHeaders(si, httpServletRequest);
        InterHelpBase.readHeader(actionalManifestHeader, si);

        if (enforceTrustZone) {
            boolean trustVerified = InterHelpTrust.checkTrust(si);
            if (!trustVerified) {
                logger.log(Level.WARNING, InterHelpTrust.TRUST_VIOLATION_MSG);
                throw new InterceptorException(InterHelpTrust.TRUST_VIOLATION_MSG);
            }
        }
        //not sure that an authUser will ever exist at this point
        if (pec.getAuthenticationContext(requestMessage).isAuthenticated()) {
            si.setSecurityID(pec.getAuthenticationContext(requestMessage).getLastAuthenticatedUser().getName());
        }

        si.setGroupName(Interceptor.PROVIDER_GROUP_NAME);
        serviceName += httpServletRequestKnob.getRequestUri();
        si.setServiceName(serviceName);
        if (operation != null) {
            si.setOpName(operation.getName());
        } else {
            si.setOpName(serviceName);
        }

        si.setUrl("/" + Interceptor.PROVIDER_GROUP_NAME + "/" + serviceName);
        si.setPeerAddr(httpServletRequestKnob.getRemoteAddress());
        si.setUrlQuery(httpServletRequestKnob.getQueryString());//may be empty
        si.setSubnode(subNodeName);

        if (transmitPayload && !requestMessage.getMimeKnob().isMultipart()) {
            si.setPayload(payload);
        } else {
            si.setSize(length);
        }

        //don't actually need explicit display type setting at the moment, but it will guard against future API changes
        si.setAppType(GeneralUtil.DISPLAY_TYPE_GROUP.shortValue());//old group name
        si.setSvcType(GeneralUtil.DISPLAY_TYPE_SERVICE.shortValue());
        si.setOpType(GeneralUtil.DISPLAY_TYPE_OP.shortValue());

        si.requestAnalyzed();
    }

    /**
     * Report the server response data.
     *
     * @param event           the MessageProcessed event to be processed
     * @param transmitPayload whether or not to intercept & transmit the payload
     */
    public static void handleServerResponse(MessageProcessed event, boolean transmitPayload) {
        logger.log(Level.FINE, "Interceptor.handleServerResponse");

        //collect all the information we need to populate the ServerInteraction object
        final PolicyEnforcementContext pec = event.getContext();
        final Message responseMessage = pec.getResponse();

        if (responseMessage.getHttpResponseKnob() == null) {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Interceptor ignoring non-http response.");
            }
            return;
        }

        SoapKnob soapKnob;
        long length;
        byte[] payload = null;
        String failureMessage = null;
        try {
            if (!responseMessage.isSoap()) {//ignore non-soap responses for now
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "Interceptor ignoring non-soap response.");
                }
                return;
            }

            soapKnob = responseMessage.getSoapKnob();
            if (soapKnob.isFault()) {
                //then construct the failure message
                //TODO get more detailed fault
                //TODO SoapFaultManager = context.getSoapFaultManager();
                //TODO construct a fault containing Medium details
                SoapFaultDetail soapFaultDetail = soapKnob.getFaultDetail();
                failureMessage = "SOAP fault code: " + soapFaultDetail.getFaultCode() + ". Reason: " + soapFaultDetail.getFaultString() + ".";
            }

            //get the payload....if configured to do so
            //otherwise, just the content-length
            if (transmitPayload) {
                payload = IOUtils.slurpStream(responseMessage.getMimeKnob().getEntireMessageBodyAsInputStream());
                length = payload.length;
            } else {
                //get the content-length only
                length = responseMessage.getMimeKnob().getContentLength();
            }

        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to acquire nescessary response data to populate ServerInteraction." + e);
            return;
        }

        //populate the ServerInteraction
        ServerInteraction si = ServerInteraction.get();
        if (failureMessage != null) {
            si.setFailure(failureMessage);
            AssertionStatus status = event.getStatus();
            //again, a trivial way of determining a "security" fault, but better than nothing
            if (status == AssertionStatus.UNAUTHORIZED || status == AssertionStatus.AUTH_FAILED || status == AssertionStatus.AUTH_REQUIRED) {
                si.setHasSecurityFault(true);
            }
        }

        if (payload != null) {
            si.setPayload(payload);
        } else {
            si.setSize(length);
        }
        si.end();
    }
}
