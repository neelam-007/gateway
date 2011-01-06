package com.l7tech.security.wstrust;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.util.HexUtils;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.util.SoapConstants;
import org.w3c.dom.Element;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
* Holds info parsed out of a WS-Trust RequestSecurityTokenResponse element.
*/
public class RstrInfo {
    private static final Logger logger = Logger.getLogger(RstrInfo.class.getName());

    public String identifier;
    public String tokenType;
    public String keySize;
    public String created;
    public String expires;
    public String secretType;
    public String nonce;
    public byte[] decodedNonce;

    @Override
    public String toString() {
        return "RstrInfo{" +
                "identifier='" + identifier + '\'' +
                ", tokenType='" + tokenType + '\'' +
                ", keySize='" + keySize + '\'' +
                ", created='" + created + '\'' +
                ", expires='" + expires + '\'' +
                ", secretType='" + secretType + '\'' +
                ", nonce='" + nonce + '\'' +
                '}';
    }

    public static RstrInfo parseRstrElement(Element rstrElem) throws InvalidDocumentFormatException {
        RstrInfo ctx = new RstrInfo();

        if (rstrElem != null) {

            Element tokenType = XmlUtil.findOnlyOneChildElementByName(rstrElem, SoapConstants.WST_NAMESPACE_ARRAY, "TokenType");
            Element keySize = XmlUtil.findOnlyOneChildElementByName(rstrElem, SoapConstants.WST_NAMESPACE_ARRAY, "KeySize");

            if (tokenType != null) ctx.tokenType = XmlUtil.getTextValue(tokenType);
            if (keySize != null) ctx.keySize = XmlUtil.getTextValue(keySize);

            // get the sct Identifier
            Element rstoken = XmlUtil.findOnlyOneChildElementByName(rstrElem, SoapConstants.WST_NAMESPACE_ARRAY, "RequestedSecurityToken");
            if (rstoken != null) {
                Element contextToken = XmlUtil.findOnlyOneChildElementByName(rstoken, SoapConstants.WSSC_NAMESPACE_ARRAY, "SecurityContextToken");

                if (contextToken != null) {
                    Element tokenIdentifier = XmlUtil.findOnlyOneChildElementByName(contextToken, SoapConstants.WSSC_NAMESPACE_ARRAY, "Identifier");
                    if (tokenIdentifier != null) {
                        ctx.identifier = XmlUtil.getTextValue(tokenIdentifier);
                    }
                }

            }

            // now get the nonce
            Element entropy = XmlUtil.findOnlyOneChildElementByName(rstrElem, SoapConstants.WST_NAMESPACE_ARRAY, "Entropy");
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

            // extract the lifetime
            Element lifetime = XmlUtil.findOnlyOneChildElementByName(rstrElem, SoapConstants.WST_NAMESPACE_ARRAY, "Lifetime");
            if (lifetime != null) {
                Element created = XmlUtil.findOnlyOneChildElementByName(lifetime, SoapConstants.WSU_URIS_ARRAY, "Created");
                Element expires = XmlUtil.findOnlyOneChildElementByName(lifetime, SoapConstants.WSU_URIS_ARRAY, "Expires");

                ctx.created = XmlUtil.getTextValue(created);
                ctx.expires = XmlUtil.getTextValue(expires);
            }
        }

        if (logger.isLoggable(Level.FINE))
            logger.log(Level.FINE, ctx.toString());

        return ctx;
    }
}
