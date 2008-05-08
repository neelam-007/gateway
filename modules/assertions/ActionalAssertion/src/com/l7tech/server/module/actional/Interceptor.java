package com.l7tech.server.module.actional;

import com.actional.lg.interceptor.sdk.ServerInteraction;
import com.actional.lg.interceptor.sdk.ClientInteraction;
import com.actional.lg.interceptor.sdk.helpers.InterHelpBase;
import com.l7tech.common.message.*;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.xml.SoapFaultDetail;
import com.l7tech.server.event.MessageProcessed;
import com.l7tech.server.event.MessageReceived;
import com.l7tech.server.event.PostRoutingEvent;
import com.l7tech.server.event.PreRoutingEvent;
import com.l7tech.server.message.PolicyEnforcementContext;

import javax.wsdl.Operation;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Class containing static methods that can handle different types of interceptor
 * events. It is this class that makes use of the Actional Interceptor SDK components.
 *
 * @author jules
 */
public class Interceptor {

    public static final Logger logger = Logger.getLogger(Interceptor.class.getName());

    /**
     * Report the client request data.
     *
     * @param event           the PreRoutingEvent to be processed
     * @param transmitPayload whether or not to intercept & transmit the payload
     */
    public static void handleClientRequest(PreRoutingEvent event, boolean transmitPayload) {
        logger.log(Level.SEVERE, "Interceptor.handleClientRequest");

        //collect all the information we need to populate the ClientInteraction object
        final PolicyEnforcementContext pec = event.getContext();
        final Message requestMessage = pec.getRequest();

        Operation operation;//TODO can we just get this from the ServerInteraction if its the same?
        try {
            if (!requestMessage.isSoap()) {//ignore non-soap requests for now
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "Interceptor ignoring non-soap request.");
                }
                return;
            } else {//get the SOAP Operation
                operation = pec.getOperation();
                if (operation == null) {
                    logger.log(Level.WARNING, "Interceptor cannot determine operation in SOAP request.");
                    return;
                }
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to acquire nescessary request data to populate ClientInteraction." + e);
            return;
        }
        ClientInteraction ci = ClientInteraction.begin();
        //TODO setGroupName() & setServiceName() ??
        //TODO get user name for outgoing request -- how to propagate this via the event?
        //TODO add full credentials if they are available?

        ci.setOpName(operation.getName());
        ci.setUrl(event.getUrl().toString());

        //build the Actional Manifest header
        //String actionalManifestHeader = InterHelpBase.writeHeader(ci);


    }

    /**
     * Report the client response data.
     *
     * @param event           the PostRoutingEvent to be processed
     * @param transmitPayload whether or not to intercept & transmit the payload
     */
    public static void handleClientResponse(PostRoutingEvent event, boolean transmitPayload) {
        logger.log(Level.SEVERE, "Interceptor.handleClientResponse");

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

        //is this an HTTP request?
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
        try {
            actionalManifestHeader = httpServletRequestKnob.getHeaderSingleValue(InterHelpBase.kLGTransport);

            if (!requestMessage.isSoap()) {//ignore non-soap requests for now
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "Interceptor ignoring non-soap request.");
                }
                return;
            } else {//get the SOAP Operation
                operation = pec.getOperation();
                if (operation == null) {
                    logger.log(Level.WARNING, "Interceptor cannot determine operation in SOAP request.");
                    return;
                }
            }

            //get the payload...if configured to do so
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

        //TODO should setGroupName() & setServiceName() be called explicitly?
        si.setOpName(operation.getName());
        //TODO the URI acquired below doesn't uniquely describe the service -- it will be /ssg/soap alot of the time
        si.setUrl(httpServletRequestKnob.getRequestUri());
        si.setPeerAddr(httpServletRequestKnob.getRemoteAddress());
        si.setUrlQuery(httpServletRequestKnob.getQueryString());//may be empty

        if (transmitPayload) {
            si.setPayload(payload);
        } else {
            si.setSize(length);
        }

        //TODO do we want/need to set the sub node? if so to what? hostname node in a cluster?
        //si.setSubnode();

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
                //get the content-length somehow
                length = responseMessage.getMimeKnob().getFirstPart().getContentLength();
                if (length == -1) {
                    //then the content-length isn't available yet so it must be computed
                    length = responseMessage.getMimeKnob().getFirstPart().getActualContentLength();
                }
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
        if (transmitPayload) {
            si.setPayload(payload);
        } else {
            si.setSize(length);
        }
        si.end();
    }

    private static byte[] getMessageBytes(Message message) throws Exception {
        final MimeKnob mimeKnob = message.getMimeKnob();

        if (mimeKnob.isMultipart()) {
            logger.log(Level.WARNING, "Interceptor encountered multipart SOAP message. ");
        }

        final PartInfo partInfo = mimeKnob.getFirstPart();

        //try the quick & dirty way first
        byte[] messageBytes = partInfo.getBytesIfAlreadyAvailable();

        if (messageBytes == null) {
            messageBytes = HexUtils.slurpStream(partInfo.getInputStream(false));
        }

        return messageBytes;
    }
}
