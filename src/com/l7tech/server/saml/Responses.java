package com.l7tech.server.saml;

import org.apache.xmlbeans.XmlOptions;
import x0Protocol.oasisNamesTcSAML1.ResponseType;
import x0Protocol.oasisNamesTcSAML1.StatusType;
import x0Protocol.oasisNamesTcSAML1.StatusCodeType;
import x0Protocol.oasisNamesTcSAML1.ResponseDocument;

import javax.xml.namespace.QName;

/**
 * Package private class that contains precanned responses and utility methods
 * that deal with saml resonses.
 *
 * @author emil
 */
class Responses {
    /** cannot instantiate this class */
    Responses() {
    }

    /**
     *
     * @param rdoc
     * @return whether the response document has a SAML status code "samlp:Success"
     * @throws IllegalArgumentException if the response is malformed
     */
    static boolean isSuccess(ResponseDocument rdoc) throws IllegalArgumentException {
        ResponseType rt = rdoc.getResponse();
        if (rt == null) {
            throw new IllegalArgumentException();
        }

        StatusType status = rt.getStatus();
        if (status == null) {
            throw new IllegalArgumentException();
        }
        StatusCodeType scode = status.getStatusCode();
        if (scode == null) {
            throw new IllegalArgumentException();
        }

        QName qn = scode.getValue();
        return Constants.STATUS_SUCCESS.equals(qn.getLocalPart());
    }

    /**
     * Precanned response that indicates not implemented functionality. It
     * is a empty response with the status code 'Responder' (see saml1.1 core 3.4.3.1).
     * @param detail message - optional message , on <b>null</b> the default
     *              "Not Implemented is returned"
     *
     * @return the precanned 'not implemented' <code>ResponseDocument</code>
     */
    static ResponseDocument getNotImplementedResponse(String detail) {
        ResponseType notImplemented = ResponseType.Factory.newInstance(options());
        StatusType status = notImplemented.addNewStatus();
        StatusCodeType scode = status.addNewStatusCode();
        scode.setValue(new QName(Constants.NS_SAMLP, Constants.STATUS_RESPONDER));
        String msg = detail;
        if (msg == null) {
            msg = "Not implemented in this provider/version";
        }
        status.setStatusMessage(msg);
        ResponseDocument rdoc = ResponseDocument.Factory.newInstance();
        rdoc.setResponse(notImplemented);
        return rdoc;
    }

    /**
     * Precanned empty response that contains no assertions with the 'Success' status.
     * Used in scenarios such as SAML authority unable provide an assertion with any
     * statements satisfying the constraints expressed by a query (saml1.1 core 3.4.4).
     *
     * @param detail message
     * @return the precanned empty  <code>ResponseDocument</code>
     */
    static ResponseDocument getEmpty(String detail) {
        ResponseType empty = ResponseType.Factory.newInstance(options());
        StatusType status = empty.addNewStatus();
        StatusCodeType scode = status.addNewStatusCode();
        scode.setValue(new QName(Constants.NS_SAMLP, Constants.STATUS_SUCCESS));
        if (detail !=null) {
            status.setStatusMessage(detail);
        }
        ResponseDocument rdoc = ResponseDocument.Factory.newInstance();
        rdoc.setResponse(empty);
        return rdoc;
    }


    static XmlOptions options() {
        XmlOptions opts = new XmlOptions();
        return opts;
    }
}
