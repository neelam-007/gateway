package com.l7tech.gateway.api.impl;

import javax.xml.bind.annotation.XmlAnyAttribute;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;
import java.util.List;
import java.util.Map;

/**
 * Placeholder for future extension.
 *
 * <p>Extensions will be nested, so this extension type should either be used
 * in every object that supports extension or in the latest extension for that
 * class.</p>
 *
 * <p>An example of extension in an XML document would be:</p>
 *
 * <pre>
 *   &lt;datatype>
 *      &lt;name>Example&lt;/name>
 *   &lt;/datatype>
 *
 *   &lt;datatype>
 *      &lt;name>Example&lt;/name>
 *      &lt;extension>
 *        &lt;extensionProperty1>1&lt;extensionProperty1>
 *        &lt;extension>
 *          &lt;extensionProperty2>1&lt;extensionProperty2>
 *        &lt;/extension>
 *      &lt;/extension>
 *   &lt;/datatype>
 * </pre>
 *
 * <p>The example shows an instance of a type that has been extended twice
 * (two compatible schema revisions).</p>
 *
 * <p>Extensions will not extend this class, when an extension is used the
 * type of that extension replaces this type in the extended object.</p>
 */
@XmlType(name="ExtensionType", propOrder={"extensions"})
public class Extension {

    //- PUBLIC

    @XmlAnyAttribute
    public Map<QName, Object> getAttributeExtensions() {
        return attributeExtensions;
    }

    public void setAttributeExtensions( final Map<QName, Object> attributeExtensions ) {
        this.attributeExtensions = attributeExtensions;
    }

    @XmlAnyElement(lax=true)
    public List<Object> getExtensions() {
        return extensions;
    }

    public void setExtensions( final List<Object> extensions ) {
        this.extensions = extensions;
    }

    //- PRIVATE

    private List<Object> extensions;
    private Map<QName,Object> attributeExtensions;
}
