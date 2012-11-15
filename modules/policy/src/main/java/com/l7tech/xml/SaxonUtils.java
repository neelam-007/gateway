package com.l7tech.xml;

import com.l7tech.util.ConfigFactory;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.xml.xpath.XpathVersion;
import com.saxonica.config.EnterpriseConfiguration;
import com.saxonica.config.ProfessionalConfiguration;
import net.sf.saxon.Configuration;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.CollationURIResolver;
import net.sf.saxon.lib.CollectionURIResolver;
import net.sf.saxon.lib.FeatureKeys;
import net.sf.saxon.lib.StringCollator;
import net.sf.saxon.om.NamespaceResolver;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.s9api.*;
import net.sf.saxon.serialize.MessageWarner;
import net.sf.saxon.sxpath.IndependentContext;
import net.sf.saxon.trans.XPathException;
import org.jetbrains.annotations.NotNull;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.logging.Logger;

/**
 * Utility methods and singletons related to use of the Saxon API.
 */
public class SaxonUtils {
    private static final Logger logger = Logger.getLogger(SaxonUtils.class.getName());

    public static final String CONFIG_PROP_ALWAYS_USE_SAXON = "xslt.engine.force20";
    private static final String PROP_SAXON_ALLOW_COLLATION = "com.l7tech.xml.xslt.saxon.allowCollation";
    private static final String PROP_SAXON_ALLOW_COLLECTIONS = "com.l7tech.xml.xslt.saxon.allowCollections";
    private static final String PROP_SAXON_ALLOW_ENTITY_RESOLUTION = "com.l7tech.xml.xslt.saxon.allowEntityResolution";

    // Layer 7 Technologies license key for Saxon Enterprise Edition
    private static final String key =
        "j0j0kbj09b3b0bv0v0b0q0kbj0000bj0k00bf0kb0bq03bv0fbf00bj0a030" +
        "90bb0bbb3ba0q0j0k0q0a0a090f0k0a0b00bq03bfb0bb0k0bbf030j00030" +
        "a0b0q0j0q09090q0q090j0j00bfbj030a00bk0k0kb00309bv030b0f0k0a0" +
        "bbk0a000r093kj9jbjf3w3j3q309zaza303000r00jqjf3bb9303w3f3w393" +
        "bjw3q3f3rbza303000r00jqjf3bb93b3f3kjj3aj99zakj933j93w3r0w3d3" +
        "q3bjf3kjq3ajvj9bzad3w3r0w3d3q3bjf39j$3f33j9bzaf0a0a0a0a9r0kj" +
        "930j99zav030a0a0a0a0j9r0$3f3q3kj9309zaj9r00j93q3kj9309zab0k0" +
        "rkq0a0rkk0f0a0k0r0b3939j0j0jqbza0j93qjr039fb09za0j93qjr0f9fb" +
        "09za0j93qjr0b9fb09za9b9br0w3d3q3bjq3b39bzar3d303wkv30393bjj0" +
        "kj93qjf3$3ab$3$3f3w3q333f3r0$3q3f3r39bzaj0akkj93qjf3$bakrb9b" +
        "dbr0qjw3f3ajr3d30bza$3$3f3w3q33bakjj93kjb3w3fbr093930jw39303" +
        "q3$bzaf303q3w3d3vjf309r0kjd30jw39303q3$b";

    private static final Configuration configuration;
    private static final Processor processor;

    static {
        // Activate licensed configuration and s9api processor
        EnterpriseConfiguration conf = new EnterpriseConfiguration();
        activate(conf);
        configuration = conf;

        processor = new Processor(configuration);
        activate(processor);
    }

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
        return configuration;
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

        transfactory.setAttribute(FeatureKeys.MESSAGE_EMITTER_CLASS, MessageWarner.class.getName());
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


    private static String deobfuscate(String x) {
        String digits = "afk0b93jvqz;$rwd";
        StringBuilder buff = new StringBuilder(x.length());
        for (int i = x.length() - 1; i >= 0; i -= 2) {
            int c1 = digits.indexOf(x.charAt(i));
            int c2 = digits.indexOf(x.charAt(i - 1));
            buff.append((char) (c1 << 4 | c2));
        }
        return buff.toString();
    }

    // Activate a ProfessionalConfiguration or EnterpriseConfiguration
    public static void activate(ProfessionalConfiguration config) {
        config.supplyLicenseKey(new BufferedReader(new StringReader(deobfuscate(key))));
    }

    // Activate Saxon at the level of a s9api Processor
    public static void activate(Processor processor) {
        processor.setConfigurationProperty("http://saxonica.com/oem-data", deobfuscate(key));
    }
}
