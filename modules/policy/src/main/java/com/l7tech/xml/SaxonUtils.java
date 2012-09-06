package com.l7tech.xml;

import com.l7tech.util.ConfigFactory;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.xml.xpath.XpathVersion;
import net.sf.saxon.Configuration;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.CollationURIResolver;
import net.sf.saxon.lib.CollectionURIResolver;
import net.sf.saxon.lib.FeatureKeys;
import net.sf.saxon.lib.StringCollator;
import net.sf.saxon.om.NamespaceResolver;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.s9api.*;
import net.sf.saxon.sxpath.IndependentContext;
import net.sf.saxon.trans.XPathException;
import org.jetbrains.annotations.NotNull;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Utility methods and singletons related to use of the Saxon API.
 */
public class SaxonUtils {
    private static final Logger logger = Logger.getLogger(SaxonUtils.class.getName());

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
     *
     * @param transfactory a Saxon TransformerFactory.  Required.
     * @param useSharedConfiguration true to use the global shared Configuration; false to create a new one for this factory.
     * @throws TransformerConfigurationException if an error occurs while attempting to configure the transformer factory.
     */
    public static void configureSecureSaxonTransformerFactory(@NotNull TransformerFactory transfactory, boolean useSharedConfiguration) throws TransformerConfigurationException {
        // Share global configuration for now
        final Configuration config = useSharedConfiguration
                                        ? getConfiguration()
                                        : Configuration.newConfiguration();
        transfactory.setAttribute(FeatureKeys.CONFIGURATION, config);

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
            transfactory.setAttribute(FeatureKeys.ENTITY_RESOLVER_CLASS, SsgSaxonEntityResolver.class.getName());
        }
    }

    /**
     * Use Saxon to parse the specified XPath expression and determine names of all referenced variables with
     * no namespace specified.
     *
     * @param expr XPath 1.0 or 2.0 expression to examine.  Required.
     * @return a list of used variables.  May be empty but never null.
     * @throws InvalidXpathException if the expression could not be parsed.
     */
    public static List<String> getUnprefixedVariablesUsedInXpath(@NotNull String expr, @NotNull XpathVersion xpathVersion) throws InvalidXpathException {
        final XPathCompiler compiler = SaxonUtils.getProcessor().newXPathCompiler();

        final PrefixCollectingNamespaceResolver pcnr = new PrefixCollectingNamespaceResolver();
        ((IndependentContext)compiler.getUnderlyingStaticContext()).setNamespaceResolver(pcnr);

        XPathExecutable xpe = compile(expr, xpathVersion, compiler);

        Set<String> varnames = new HashSet<String>();

        Iterator<QName> vars = xpe.iterateExternalVariables();
        while (vars.hasNext()) {
            QName var = vars.next();
            String nsUri = var.getNamespaceURI();
            String localName = var.getLocalName();
            if (nsUri == null || nsUri.length() < 1)
                varnames.add(localName);
        }
        return new ArrayList<String>(varnames);
    }

    /**
     * Use Saxon to determine what namespace prefixes are used by the specified XPath expression.
     *
     * @param expr expression to evaluate.  Required.
     * @param xpathVersion  XPath version to use to parse expression.  Required but may be UNSPECIFIED.
     * @return a set of String of namespace prefixes queried-for against a custom namespace resolver while using Saxon to parse the expression.
     * @throws InvalidXpathException
     */
    public static Set<String> getNamespacePrefixesUsedByXpath(@NotNull String expr, @NotNull XpathVersion xpathVersion) throws InvalidXpathException {
        final XPathCompiler compiler = SaxonUtils.getProcessor().newXPathCompiler();

        final PrefixCollectingNamespaceResolver pcnr = new PrefixCollectingNamespaceResolver();
        ((IndependentContext)compiler.getUnderlyingStaticContext()).setNamespaceResolver(pcnr);

        compile(expr, xpathVersion, compiler);

        return pcnr.getSeenPrefixes();
    }

    private static XPathExecutable compile(String expr, XpathVersion xpathVersion, XPathCompiler compiler) throws InvalidXpathException {
        compiler.setAllowUndeclaredVariables(true);

        String ver = xpathVersion.getVersionString();
        if (ver != null)
            compiler.setLanguageVersion(ver);

        XPathExecutable xpe;
        try {
            xpe = compiler.compile(expr);
        } catch (SaxonApiException e) {
            throw new InvalidXpathException(ExceptionUtils.getMessage(e), e);
        }
        return xpe;
    }

    /**
     * Use Saxon to compile the specified XPath expression using the specified namespace declarations.
     *
     * @param expression the expression to check. Required.
     * @param xpathVersion the xpath version.  Required, but may be UNSPECIFIED.
     * @param namespaceMap the namespace map, or null.
     */
    public static void validateSyntaxAndNamespacePrefixes(String expression, XpathVersion xpathVersion, Map<String, String> namespaceMap) throws InvalidXpathException {
        final XPathCompiler compiler = SaxonUtils.getProcessor().newXPathCompiler();
        if (namespaceMap != null) {
            for (Map.Entry<String, String> entry : namespaceMap.entrySet()) {
                compiler.declareNamespace(entry.getKey(), entry.getValue());
            }
        }
        compile(expression, xpathVersion, compiler);
    }

    private static class PrefixCollectingNamespaceResolver implements NamespaceResolver {
        private Set<String> seenPrefixes = new HashSet<String>();

        @Override
        public String getURIForPrefix(String prefix, boolean useDefault) {
            if (prefix == null || prefix.length() < 1)
                return "";

            seenPrefixes.add(prefix);
            return "http://l7tech.com/ns/xpath/placeholder/nsprefix/" + prefix;
        }

        @Override
        public Iterator<String> iteratePrefixes() {
            throw new UnsupportedOperationException("Not supported");
        }

        public Set<String> getSeenPrefixes() {
            return seenPrefixes;
        }
    }

    public static class SsgSaxonEntityResolver implements EntityResolver {
        @Override
        public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
            String msg = "Document referred to an external entity with system id '" + systemId + "'";
            logger.warning( msg );
            throw new SAXException(msg);
        }
    }
}
