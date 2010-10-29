package com.l7tech.wsdl;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.util.ExceptionUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.wsdl.Binding;
import javax.wsdl.Types;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.schema.Schema;
import javax.wsdl.extensions.soap.SOAPBinding;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.wsdl.WsdlConstants.*;

/**
 * WSDL utility methods.
 */
public class WsdlUtil {
    private static final Logger logger = Logger.getLogger(WsdlUtil.class.getName());

    /**
     * Check if the given wsdl document contains an RPC style binding and has no schema.
     *
     * <p>WARNING! This does not check imports</p>
     *
     * @param wsdl The wsdl
     * @return true if this is an RPC with no schema (false if null)
     */
    public static boolean isRPCWithNoSchema(Wsdl wsdl) {
        return wsdl != null && hasRPCBinding(wsdl) && !hasSchema(wsdl);
    }

    /**
     * Check if the given wsdl document contains an RPC style binding and has no schema.
     *
     * <p>WARNING! This does not check imports</p>
     *
     * @param wsdl The wsdl
     * @return true if this is an RPC with no schema (false if null)
     */
    public static boolean hasRPCBinding(Wsdl wsdl) {
        for (Binding binding : wsdl.getBindings()) {
            //noinspection unchecked
            final List<ExtensibilityElement> eels = binding.getExtensibilityElements();
            for (ExtensibilityElement eel : eels) {
                if (eel instanceof SOAPBinding) {
                    SOAPBinding soapBinding = (SOAPBinding) eel;
                    if (Wsdl.STYLE_RPC.equals(soapBinding.getStyle())) return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns true if this WSDL contains at least one Schema in its &lt;types&gt; element.
     * @param wsdl the WSDL in which to look for schemas
     * @return true if the provided WSDL contains at least on Schema
     */
    public static boolean hasSchema(Wsdl wsdl) {
        Types types = wsdl.getDefinition().getTypes();
        if (types == null) return false;
        //noinspection unchecked
        List<ExtensibilityElement> eels = types.getExtensibilityElements();
        for (ExtensibilityElement eel : eels) {
            if (eel instanceof Schema) return true;
        }
        return false;
    }


    public interface LocationBuilder {
        /**
         * Return a new URL to use for the specified address Location.
         *
         * @param address a soap:address, soap12:address, or http:address element whose location attr to rewrite.
         * @return the new attr value, or null to leave the existing value unchanged.
         * @throws MalformedURLException if existing value cannot be parsed.
         */
        public String buildLocation(Element address) throws MalformedURLException;
    }

    /**
     * Rewrite all soap:address, soap12:address, and http:address elements in the specified WSDL using the specified
     * LocationBuilder.
     *
     * @param wsdlDoc the WSDL to modify.  Required.
     * @param locationBuilder the LocationBuilder that will translate the URLs.  Required.
     */
    public static void rewriteAddressLocations(Document wsdlDoc, LocationBuilder locationBuilder) {
        final NodeList portList = wsdlDoc.getElementsByTagNameNS( NAMESPACE_WSDL, ELEMENT_PORT );
        for (int i = 0; i < portList.getLength(); i++) {
            final Element portElement = (Element)portList.item(i);
            final List<Element> addresses = new ArrayList<Element>();
            addresses.addAll(XmlUtil.findChildElementsByName(portElement, NAMESPACE_WSDL_SOAP_1_1, ELEMENT_ADDRESS ));
            addresses.addAll(XmlUtil.findChildElementsByName(portElement, NAMESPACE_WSDL_SOAP_1_2, ELEMENT_ADDRESS ));
            addresses.addAll(XmlUtil.findChildElementsByName(portElement, NAMESPACE_WSDL_HTTP, ELEMENT_ADDRESS));
            for ( final Element address : addresses ) {
                try {
                    String newval = locationBuilder.buildLocation(address);
                    if (newval != null)
                        address.setAttribute( ATTR_LOCATION, newval);
                } catch (MalformedURLException e) {
                    //noinspection ThrowableResultOfMethodCallIgnored
                    logger.log(Level.WARNING, "Unable to rewrite URL in WSDL: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                }
            }
        }
    }
}
