package com.l7tech.security.wstrust;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.util.HexUtils;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.util.SoapConstants;
import org.w3c.dom.Element;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
* Holds info parsed out of a WS-Trust RequestSecurityToken element.
*/
public class RstInfo {
    private static final Logger logger = Logger.getLogger(RstInfo.class.getName());

    public String tokenType;
    public String requestType;
    public int keySize = 256;
    public String secretType;
    public String nonce;
    public byte[] decodedNonce;

    @Override
    public String toString() {
        return "RstInfo{" +
                "tokenType='" + tokenType + '\'' +
                ", requestType='" + requestType + '\'' +
                ", keySize=" + keySize +
                ", secretType='" + secretType + '\'' +
                ", nonce='" + nonce + '\'' +
                '}';
    }

    /**
     * Create RstInfo by parsing the specified RST element.
     *
     * @param rstElem the elment to parse.  Required.
     * @return the parsed information.  Never null, but some fields may be null.
     * @throws InvalidDocumentFormatException if the element could not be parsed.
     */
    public static RstInfo parseRstElement(Element rstElem) throws InvalidDocumentFormatException {
        RstInfo ctx = new RstInfo();

        if (rstElem != null) {

            Element tokenType = XmlUtil.findOnlyOneChildElementByName(rstElem, SoapConstants.WST_NAMESPACE_ARRAY, "TokenType");
            Element requestType = XmlUtil.findOnlyOneChildElementByName(rstElem, SoapConstants.WST_NAMESPACE_ARRAY, "RequestType");
            Element keySize = XmlUtil.findOnlyOneChildElementByName(rstElem, SoapConstants.WST_NAMESPACE_ARRAY, "KeySize");

            if (tokenType != null) ctx.tokenType = XmlUtil.getTextValue(tokenType);
            if (requestType != null) ctx.requestType = XmlUtil.getTextValue(requestType);
            if (keySize != null) {
                String value = XmlUtil.getTextValue(keySize);

                try {
                    ctx.keySize = Integer.parseInt(value);
                } catch (NumberFormatException nfe) {
                    ctx.keySize = 256; // default
                }
            }

            // now get the nonce
            Element entropy = XmlUtil.findOnlyOneChildElementByName(rstElem, SoapConstants.WST_NAMESPACE_ARRAY, "Entropy");
            if (entropy != null) {
                Element secret = XmlUtil.findOnlyOneChildElementByName(entropy, SoapConstants.WST_NAMESPACE_ARRAY, "BinarySecret");

                if (secret != null) {
                    String type = secret.getAttribute("Type");
                    String nonce = XmlUtil.getTextValue(secret);
                    ctx.secretType = type;
                    ctx.nonce = nonce;
                    if (nonce != null && nonce.length() > 0)
                        ctx.decodedNonce = HexUtils.decodeBase64(nonce);
                }

            }
        }

        if (logger.isLoggable(Level.FINE))
            logger.log(Level.FINE, ctx.toString());

        return ctx;
    }
}
