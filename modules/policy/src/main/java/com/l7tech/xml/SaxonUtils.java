package com.l7tech.xml;

import com.l7tech.util.ConfigFactory;
import net.sf.saxon.Configuration;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.CollationURIResolver;
import net.sf.saxon.lib.CollectionURIResolver;
import net.sf.saxon.lib.FeatureKeys;
import net.sf.saxon.lib.StringCollator;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.trans.XPathException;
import org.jetbrains.annotations.NotNull;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import java.io.IOException;

/**
 * Utility methods and singletons related to use of the Saxon API.
 */
public class SaxonUtils {
    public static final String PROP_ALWAYS_USE_SAXON = "com.l7tech.xml.xslt.useSaxon";
    private static final String PROP_SAXON_ALLOW_COLLATION = "com.l7tech.xml.xslt.saxon.allowCollation";
    private static final String PROP_SAXON_ALLOW_COLLECTIONS = "com.l7tech.xml.xslt.saxon.allowCollections";
    private static final String PROP_SAXON_ALLOW_ENTITY_RESOLUTION = "com.l7tech.xml.xslt.saxon.allowEntityResolution";

    private static final Processor processor = new Processor(false);

    /**
     * Get a singleton Processor instance.
     *
     * @return the singleton processor.  Never null.
     */
    public static Processor getProcessor() {
        return processor;
    }

    /**
     * Get a singleton Saxon Configuration instance.
     *
     * @return the singleton configuration.  Never null.
     */
    public static Configuration getConfiguration() {
        return processor.getUnderlyingConfiguration();
    }

    /**
     * Configure the specified Saxon TransformerFactory to be secure, to the extent possible, even when processing
     * not-entirely-trusted stylesheets.
     *
     * @param transfactory a Saxon TransformerFactory.  Required.
     * @throws TransformerConfigurationException if an error occurs while attempting to configure the transformer factory.
     */
    public static void configureSecureSaxonTransformerFactory(@NotNull TransformerFactory transfactory) throws TransformerConfigurationException {
        // Share global configuration for now
        transfactory.setAttribute(FeatureKeys.CONFIGURATION, getConfiguration());

        // disable calls to reflexive Java extension functions, system property access, relative result-document URIs, and XSLT extension instructions
        transfactory.setFeature(FeatureKeys.ALLOW_EXTERNAL_FUNCTIONS, false);

        // Disable saxon:threads="N" on an xsl:for-each instruction (no effect until we upgrade to Saxon-EE)
        transfactory.setFeature(FeatureKeys.ALLOW_MULTITHREADING, false);

        // Disable this feature until we establish that it should be enabled
        if (!ConfigFactory.getBooleanProperty(PROP_SAXON_ALLOW_COLLATION, false)) {
            transfactory.setAttribute(FeatureKeys.COLLATION_URI_RESOLVER, new CollationURIResolver() {
                @Override
                public StringCollator resolve(String relativeURI, String baseURI, Configuration config) {
                    return null;
                }
            });
        }

        // Disable this feature until we establish that it should be enabled
        if (!ConfigFactory.getBooleanProperty(PROP_SAXON_ALLOW_COLLECTIONS, false)) {
            transfactory.setAttribute(FeatureKeys.COLLECTION_URI_RESOLVER, new CollectionURIResolver() {
                @Override
                public SequenceIterator resolve(String href, String base, XPathContext context) throws XPathException {
                    throw new XPathException("Collection URI resolution not permitted");
                }
            });
        }

        // Disable this feature until we establish that it should be enabled
        if (!ConfigFactory.getBooleanProperty(PROP_SAXON_ALLOW_ENTITY_RESOLUTION, false)) {
            transfactory.setAttribute(FeatureKeys.ENTITY_RESOLVER_CLASS, "");
        }

        transfactory.setAttribute(FeatureKeys.ENTITY_RESOLVER_CLASS, SsgSaxonEntityResolver.class.getName());
    }

    public static class SsgSaxonEntityResolver implements EntityResolver {
        @Override
        public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
            // TODO
            return null;
        }
    }
}
