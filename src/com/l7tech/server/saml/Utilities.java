package com.l7tech.server.saml;

import com.l7tech.common.security.saml.Constants;
import org.apache.xmlbeans.XmlOptions;
import x0Protocol.oasisNamesTcSAML1.ResponseDocument;
import x0Protocol.oasisNamesTcSAML1.ResponseType;
import x0Protocol.oasisNamesTcSAML1.StatusCodeType;
import x0Protocol.oasisNamesTcSAML1.StatusType;

import javax.xml.namespace.QName;
import java.util.HashMap;
import java.util.Map;

/**
 * Package private class with saml protocol utility methods.
 * @author emil
 * @version 5-Aug-2004
 */
class Utilities {

    /**
     * Tests if the response document has a success status.
     *
     * @param rdoc the response document
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

    static XmlOptions xmlOptions() {
        XmlOptions options = new XmlOptions();
        Map prefixes = new HashMap();
        prefixes.put(Constants.NS_SAMLP, Constants.NS_SAMLP_PREFIX);
        prefixes.put(Constants.NS_SAML, Constants.NS_SAML_PREFIX);
        options.setSaveSuggestedPrefixes(prefixes);
        return options;
    }
}
