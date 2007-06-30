package com.l7tech.common.xml;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.l7tech.common.util.XmlUtil;

/**
 * WSDL utility methods.
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class WsdlUtil {

    /**
     * Check if the given wsdl document contains an RPC style binding and has no schema.
     *
     * <p>WARNING! This does not check imports</p>
     *
     * @param wsdl The wsdl xml text.
     * @return true if this is an RPC with no schema (false if null)
     * @throws SAXException If the given wsdl cannot be parsed
     */
    public static boolean isRPCWithNoSchema(String wsdl) throws SAXException {
        boolean rpcNoSchema = false;
        if (wsdl != null) {
            rpcNoSchema = isRPCWithNoSchema(XmlUtil.stringToDocument(wsdl));
        }
        return rpcNoSchema;
    }

    /**
     * Check if the given wsdl document contains an RPC style binding and has no schema.
     *
     * <p>WARNING! This does not check imports</p>
     *
     * @param wsdlDocument The wsdl DOM
     * @return true if this is an RPC with no schema (false if null)
     */
    public static boolean isRPCWithNoSchema(Document wsdlDocument) {
        boolean rpcNoSchema = false;

        if (wsdlDocument != null) {
            if (hasRPCBinding(wsdlDocument) && !hasSchema(wsdlDocument)) {
                rpcNoSchema = true;
            }
        }

        return rpcNoSchema;
    }

    /**
     * Check if the given WSDL document contains a binding with an RPC style.
     *
     * @param wsdlDocument The document to check
     * @return true if any binding in the WSDL is RPC
     */
    public static boolean hasRPCBinding(Document wsdlDocument) {
        boolean isRpc = false;
        NodeList bindings = wsdlDocument.getDocumentElement().getElementsByTagNameNS(
                Wsdl.WSDL_SOAP_NAMESPACE, "binding");

        for (int n=0; n<bindings.getLength(); n++) {
            Element bindingElement = (Element) bindings.item(n);
            if (bindingElement.getAttribute("style").equals(Wsdl.STYLE_RPC)) {
                isRpc = true;
                break;
            }
        }

        return isRpc;
    }

    /**
     * Check if the given wsdl contains a schema.
     *
     * <p>WARNING! This does not check imports</p>
     *
     * @return true if the given DOM contains a schema (false if null)
     */
    public static boolean hasSchema(Document wsdlDocument) {
        boolean hasSchema = false;

        if (wsdlDocument != null) {
            WsdlSchemaAnalizer wsdlSchemaAnalizer = new WsdlSchemaAnalizer(wsdlDocument);
            if (wsdlSchemaAnalizer.getFullSchemas() != null) {
                hasSchema = true;
            }
        }

        return hasSchema;
    }
}
