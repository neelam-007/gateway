package com.l7tech.common.xml;

import javax.wsdl.Binding;
import javax.wsdl.Types;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.schema.Schema;
import javax.wsdl.extensions.soap.SOAPBinding;
import java.util.List;

/**
 * WSDL utility methods.
 */
public class WsdlUtil {

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
}
