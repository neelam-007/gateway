package com.l7tech.xml.xpath;

import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathFactoryConfigurationException;
import javax.xml.xpath.XPathVariableResolver;
import javax.xml.xpath.XPathFunctionResolver;
import javax.xml.xpath.XPath;
import javax.xml.XMLConstants;

/**
 * XPathFactory for CursorXPaths.
 *
 * <p>Thin wrapper around CompiledXpath.</p>
 *
 * @author $Author$
 * @version $Revision$
 * @see XpathExpression XpathExpression
 * @see com.l7tech.xml.xpath.CompiledXpath CompiledXpath
 */
public class CursorXPathFactory extends XPathFactory {

    //- PUBLIC

    /**
     *
     */
    public CursorXPathFactory() {
        super();
    }

    /**
     *
     */
    public boolean isObjectModelSupported(String objectModel) {
        boolean supports = false;
        if(XPathFactory.DEFAULT_OBJECT_MODEL_URI.equals(objectModel)) {
            supports = true;
        }
        return supports;
    }

    /**
     *
     */
    public void setFeature(String name, boolean value) throws XPathFactoryConfigurationException {
        if(name==null) throw new NullPointerException("name must not be null");
        if(!XMLConstants.FEATURE_SECURE_PROCESSING.equals(name))
            throw new XPathFactoryConfigurationException("Feature not supported '"+name+"'.");
    }

    /**
     *
     */
    public boolean getFeature(String name) throws XPathFactoryConfigurationException {
        if(name==null) throw new NullPointerException("name must not be null");
        boolean feature = false;
        if(XMLConstants.FEATURE_SECURE_PROCESSING.equals(name)) {
            feature = true;
        }
        return feature;
    }

    /**
     *
     */
    public void setXPathVariableResolver(XPathVariableResolver resolver) {
        if(resolver==null) throw new NullPointerException("resolver must not be null");
        defaultVariableResolver = resolver;
    }

    /**
     *
     */
    public void setXPathFunctionResolver(XPathFunctionResolver resolver) {
        if(resolver==null) throw new NullPointerException("resolver must not be null");
        defaultFunctionResolver = resolver;
    }

    /**
     *
     */
    public XPath newXPath() {
        return new CursorXPath(defaultVariableResolver, defaultFunctionResolver);
    }

    //- PRIVATE

    private XPathVariableResolver defaultVariableResolver;
    private XPathFunctionResolver defaultFunctionResolver;
}
