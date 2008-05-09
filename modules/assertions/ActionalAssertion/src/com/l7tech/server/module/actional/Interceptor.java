package com.l7tech.server.module.actional;

import com.actional.lg.interceptor.sdk.ServerInteraction;
import com.actional.lg.interceptor.sdk.ClientInteraction;
import com.actional.lg.interceptor.sdk.helpers.InterHelpBase;
import com.l7tech.common.message.*;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.xml.SoapFaultDetail;
import com.l7tech.common.http.GenericHttpHeader;
import com.l7tech.common.http.HttpConstants;
import com.l7tech.server.event.MessageProcessed;
import com.l7tech.server.event.MessageReceived;
import com.l7tech.server.event.PostRoutingEvent;
import com.l7tech.server.event.PreRoutingEvent;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerHttpRoutingAssertion;
import com.l7tech.policy.assertion.HttpRoutingAssertion;

import javax.wsdl.Operation;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.IOException;
import java.net.InetAddress;


/**
 * Class containing static methods that can handle different types of interceptor
 * events. It is this class that makes use of the Actional Interceptor SDK components.
 *
 * @author jules
 */
public class Interceptor {

    private static final Logger logger = Logger.getLogger(Interceptor.class.getName());
    private static final String PROVIDER_GROUP_NAME = "SSG";

    /**
     * Report the client request data.
     *
     * @param event           the PreRoutingEvent to be processed
     * @param transmitPayload whether or not to intercept & transmit the payload
     */
    public static void handleClientRequest(PreRoutingEvent event, boolean transmitPayload) {
        logger.log(Level.SEVERE, "Interceptor.handleClientRequest");

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
                payload = getMessageBytes(requestMessage);
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

        setSecurityId(ci, pec, serverHttpRoutingAssertion);

        if (operation != null) {
            ci.setOpName(operation.getName());
        } else {
            ci.setOpName(serviceName);
        }
        ci.setUrl(event.getUrl().toString());

        //build the Actional Manifest header
        String actionalManifestHeader = null;
        try {
            actionalManifestHeader = InterHelpBase.writeHeader(ci);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Interceptor could not construct Actional manifest header. No header will be sent.");
            serverHttpRoutingAssertion.setActionalHeader(null);
        } finally {
            serverHttpRoutingAssertion.setActionalHeader(new GenericHttpHeader(InterHelpBase.kLGTransport, actionalManifestHeader));
        }

        if (payload != null) {
            ci.setPayload(payload);
        } else {
            ci.setSize(length);
        }

        ci.requestAnalyzed();
    }

    /**
     * Report the client response data.
     *
     * @param event           the PostRoutingEvent to be processed
     * @param transmitPayload whether or not to intercept & transmit the payload
     */
    public static void handleClientResponse(PostRoutingEvent event, boolean transmitPayload) {
        logger.log(Level.SEVERE, "Interceptor.handleClientResponse");

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
                failureMessage = "SOAP fault code: " + soapFaultDetail.getFaultCode() + ". Reason: " + soapFaultDetail.getFaultString() + ".";
            }

            if (transmitPayload) {
                payload = getMessageBytes(responseMessage);
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
    public static void handleServerRequest(MessageReceived event, boolean transmitPayload) {
        logger.log(Level.SEVERE, "Interceptor.handleServerRequest");

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
        try {
            actionalManifestHeader = httpServletRequestKnob.getHeaderSingleValue(InterHelpBase.kLGTransport);

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
                payload = getMessageBytes(requestMessage);
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

        InterHelpBase.readHeader(actionalManifestHeader, si);

        //not sure that an authUser will ever exist at this point
        //TODO can this be set during response processing?
        if (pec.isAuthenticated()) {
            si.setSecurityID(pec.getLastAuthenticatedUser().getName());
        }

        si.setGroupName(Interceptor.PROVIDER_GROUP_NAME);
        si.setServiceName(serviceName);
        if (operation != null) {
            si.setOpName(operation.getName());
        } else {
            si.setOpName(serviceName);
        }

        si.setUrl(httpServletRequestKnob.getRequestUri());
        si.setPeerAddr(httpServletRequestKnob.getRemoteAddress());
        si.setUrlQuery(httpServletRequestKnob.getQueryString());//may be empty
        si.setSubnode(subNodeName);

        if (transmitPayload && !requestMessage.getMimeKnob().isMultipart()) {
            si.setPayload(payload);
        } else {
            si.setSize(length);
        }

        si.requestAnalyzed();
    }

    /**
     * Report the server response data.
     *
     * @param event           the MessageProcessed event to be processed
     * @param transmitPayload whether or not to intercept & transmit the payload
     */
    public static void handleServerResponse(MessageProcessed event, boolean transmitPayload) {
        logger.log(Level.SEVERE, "Interceptor.handleServerResponse");

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
                SoapFaultDetail soapFaultDetail = soapKnob.getFaultDetail();
                failureMessage = "SOAP fault code: " + soapFaultDetail.getFaultCode() + ". Reason: " + soapFaultDetail.getFaultString() + ".";
            }

            //get the payload....if configured to do so
            //otherwise, just the content-length
            if (transmitPayload) {
                payload = getMessageBytes(responseMessage);
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
        }
        //TODO can the securityID and/or full credentials be set here?
        if (payload != null) {
            si.setPayload(payload);
        } else {
            si.setSize(length);
        }
        si.end();
    }

    private static byte[] getMessageBytes(Message message) throws Exception {
        final MimeKnob mimeKnob = message.getMimeKnob();

        if (mimeKnob.isMultipart()) {
            logger.log(Level.WARNING, "Interceptor encountered multipart SOAP message. Payload will not be captured.");
            return null;
        }

        final PartInfo partInfo = mimeKnob.getFirstPart();

        //try the quick & dirty way first
        byte[] messageBytes = partInfo.getBytesIfAlreadyAvailable();

        if (messageBytes == null) {
            messageBytes = HexUtils.slurpStream(partInfo.getInputStream(false));
        }

        return messageBytes;
    }

    /**
     * Sets the security ID of the given ClientInteraction.  The security ID is only set when
     * HTTP credentials are specificed in the routing assertion OR if the routing assertion is configured
     * to passthrough HTTP credentials.
     *
     * @param ci                         a ClientInteraction object to which the security ID will be added, must not be null
     * @param pec                        PolicyEnforcementContext from which to acquire the security ID
     * @param serverHttpRoutingAssertion an instance of ServerHttpRoutingAssertion from which to get the credentials.
     */
    private static void setSecurityId(ClientInteraction ci, PolicyEnforcementContext pec, ServerHttpRoutingAssertion serverHttpRoutingAssertion) {
        HttpRoutingAssertion httpRoutingAssertion = serverHttpRoutingAssertion.getData();
        final Message message = pec.getRequest();


        String securityId = httpRoutingAssertion.getLogin();
        if (httpRoutingAssertion.isPassthroughHttpAuthentication()) {
            try {
                String authHeaderValue = message.getHttpRequestKnob().getHeaderSingleValue(HttpConstants.HEADER_AUTHORIZATION);
                if (authHeaderValue.contains("Basic")) {//TODO this is brittle, there must be a better way of doing this
                    //strip the "Basic"
                    authHeaderValue = authHeaderValue.substring(5).trim();
                    byte[] bytes = HexUtils.decodeBase64(authHeaderValue);
                    securityId = new String(bytes);//of the form username:password
                    securityId = securityId.substring(0, securityId.indexOf(":"));
                }
            } catch (IOException ioe) {
                logger.log(Level.WARNING, "Interceptor could not extract pass through HTTP credentials header");
            }
        }

        if (securityId != null) {
            ci.setSecurityID(securityId);
            if (logger.isLoggable(Level.SEVERE)) {
                logger.log(Level.SEVERE, "Added security ID to request ClientInteraction.");
            }
        }
    }

    //TODO find a more economical way of computing the content-length?
}
