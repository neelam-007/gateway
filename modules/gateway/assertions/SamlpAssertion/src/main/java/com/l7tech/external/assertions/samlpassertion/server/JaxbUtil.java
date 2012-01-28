package com.l7tech.external.assertions.samlpassertion.server;

import com.l7tech.security.saml.SamlConstants;
import com.l7tech.util.CollectionUtils;
import static com.l7tech.util.CollectionUtils.set;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.Functions;
import com.l7tech.util.Pair;
import com.sun.xml.bind.marshaller.NamespacePrefixMapper;
import org.jetbrains.annotations.Nullable;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Get access to Marshaller and Unmarshaller objects configured for SAML and SAML Protocol Version 1 or 2 and
 * saml.support.ds (http://www.w3.org/2000/09/xmldsig#)
 *
 * This class will either create a new underlying JAXBContext per usage or use a static JAXBContext depending
 * on the value of system property com.l7tech.external.assertions.samlpassertion.useStaticContext
 * While the JAXB implementation used is thread safe a static context can be used.
 *
 * @author : vchan, sjones, darmstrong
 */
@SuppressWarnings({"JavaDoc"})
public class JaxbUtil {
    private static final Logger logger = Logger.getLogger(JaxbUtil.class.getName());

    private static final String SAML_V1_CTX_PACKAGES = "saml.v1.protocol:saml.v1.assertion:saml.support.ds";
    private static final String SAML_V2_CTX_PACKAGES = "saml.v2.protocol:saml.v2.assertion:saml.v2.authn.context:saml.support.ds";
    private static final boolean useStaticContext = ConfigFactory.getBooleanProperty("com.l7tech.external.assertions.samlpassertion.useStaticContext", true);
    private static final AtomicReference<JAXBContext> jaxbContextV1 = new AtomicReference<JAXBContext>();
    private static final AtomicReference<JAXBContext> jaxbContextV2 = new AtomicReference<JAXBContext>();

    private static final Functions.NullaryThrows<JAXBContext, JAXBException> ctxFactoryV1 = new Functions.NullaryThrows<JAXBContext, JAXBException>() {
        @Override
        public JAXBContext call() throws JAXBException {
            logger.log(Level.FINE, "Creating JAXBContext (V1)");
            return JAXBContext.newInstance(SAML_V1_CTX_PACKAGES, JaxbUtil.class.getClassLoader());
        }
    };

    private static final Functions.NullaryThrows<JAXBContext, JAXBException> ctxFactoryV2 = new Functions.NullaryThrows<JAXBContext, JAXBException>() {
        @Override
        public JAXBContext call() throws JAXBException {
            logger.log(Level.FINE, "Creating JAXBContext (V2)");
            return JAXBContext.newInstance(SAML_V2_CTX_PACKAGES, JaxbUtil.class.getClassLoader());
        }
    };

    public static final Pair<String, String> SAML_1_1 = new Pair<String, String>(SamlConstants.NS_SAML, SamlConstants.NS_SAML_PREFIX);
    public static final Pair<String, String> SAML_2 = new Pair<String, String>(SamlConstants.NS_SAML2, SamlConstants.NS_SAML2_PREFIX);
    public static final Pair<String, String> SAMLP = new Pair<String, String>(SamlConstants.NS_SAMLP, SamlConstants.NS_SAMLP_PREFIX);
    public static final Pair<String, String> SAMLP2 = new Pair<String, String>(SamlConstants.NS_SAMLP2, SamlConstants.NS_SAMLP2_PREFIX);
    public static final Pair<String, String> DS = new Pair<String, String>("http://www.w3.org/2000/09/xmldsig#", "ds");
    public static final Pair<String, String> XENC = new Pair<String, String>("http://www.w3.org/2001/04/xmlenc#", "xenc");
    public static final Pair<String, String> SAML2_PASSWORD = new Pair<String, String>(SamlConstants.AUTHENTICATION_SAML2_PASSWORD, "saccpwd");
    public static final Pair<String, String> SAML2_DSIG = new Pair<String, String>(SamlConstants.AUTHENTICATION_SAML2_XMLDSIG, "saccxds");
    public static final Pair<String, String> SAML2_TLS_CERT = new Pair<String, String>(SamlConstants.AUTHENTICATION_SAML2_TLS_CERT, "sacctlsc");
    public static final Pair<String, String> SAML2_AC = new Pair<String, String>("urn:oasis:names:tc:SAML:2.0:ac", "ac");

    public static final Set<Pair<String, String>> allSupportedNs = set(
        SAML_1_1,
        SAML_2,
        SAMLP,
        SAMLP2,
        DS,
        XENC,
        SAML2_PASSWORD,
        SAML2_DSIG,
        SAML2_TLS_CERT,
        SAML2_AC
    );

    private static final Map<String, String> NS_PREFIXES = CollectionUtils.<String, String>mapBuilder()
        .put(SAML_1_1.left,  SAML_1_1.right)
        .put(SAML_2.left,  SAML_2.right)
        .put(SAMLP.left,  SAMLP.right)
        .put(SAMLP2.left,  SAMLP2.right)
        .put(DS.left, DS.right)
        .put(XENC.left, XENC.right)
        .put(SAML2_PASSWORD.left, SAML2_PASSWORD.right)
        .put(SAML2_DSIG.left, SAML2_DSIG.right)
        .put(SAML2_TLS_CERT.left, SAML2_TLS_CERT.right)
        .put(SAML2_AC.left, SAML2_AC.right)
        .unmodifiableMap();

    /**
     * Get a Marshaller which will define all known namespaces on Marshaled root elements.
     *
     * @return Marshaller with no special configuration
     * @throws JAXBException
     */
    public static Marshaller getMarshallerV1() throws JAXBException {
        return getMarshallerV1(null, false);
    }

    /**
     * Get a Marshaller which only adds SAML Protocol namespaces to Marshaled root elements.
     *
     * @param onlyAddProtocolNs true if only protocol namespaces should be added
     * @return Marshaller with special configuration
     * @throws JAXBException
     */
    public static Marshaller getMarshallerV1(boolean onlyAddProtocolNs) throws JAXBException {
        return getMarshallerV1(null, onlyAddProtocolNs);
    }

    /**
     * Get a Marshaller which only adds SAML protocol namespaces in addition to those listed in nonProtocolRequiredNs
     * @param nonProtocolRequiredNs required namespaces which must be declared on marshaled elements. Unknown values
     * will be ignored.
     * @return Marshaller with special configuration
     * @throws JAXBException
     */
    public static Marshaller getMarshallerV1(@Nullable List<Pair<String, String>> nonProtocolRequiredNs) throws JAXBException {
        return getMarshallerV1(nonProtocolRequiredNs, true);
    }

    private static Marshaller getMarshallerV1(@Nullable List<Pair<String, String>> nonProtocolRequiredNs, boolean onlyAddProtocolNs) throws JAXBException {
        JAXBContext ctx = getContext(PackageVersion.V1);
        Marshaller m = ctx.createMarshaller();
        m.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
        m.setProperty("com.sun.xml.bind.namespacePrefixMapper",
                new SamlpNamespacePrefixMapper((nonProtocolRequiredNs != null) ? new HashSet<Pair<String, String>>(nonProtocolRequiredNs) : null, onlyAddProtocolNs));
        return m;
    }

    /**
     * @see {@link #getUnmarshallerV1()}
     */
    public static Marshaller getMarshallerV2() throws JAXBException {
        return getMarshallerV2(null, false);
    }

    /**
     * @see {@link #getMarshallerV1(boolean)}
     */
    public static Marshaller getMarshallerV2(boolean onlyAddProtocolNs) throws JAXBException {
        return getMarshallerV2(null, onlyAddProtocolNs);
    }

    /**
     * @see {@link #getMarshallerV1(java.util.List)} )}
     */
    public static Marshaller getMarshallerV2(@Nullable List<Pair<String, String>> nonProtocolRequiredNs) throws JAXBException {
        return getMarshallerV2(nonProtocolRequiredNs, true);
    }

    private static Marshaller getMarshallerV2(@Nullable List<Pair<String, String>> nonProtocolRequiredNs, boolean onlyAddProtocolNs) throws JAXBException {
        JAXBContext ctx = getContext(PackageVersion.V2);
        Marshaller m = ctx.createMarshaller();
        m.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
        m.setProperty("com.sun.xml.bind.namespacePrefixMapper",
                new SamlpNamespacePrefixMapper((nonProtocolRequiredNs != null)? new HashSet<Pair<String, String>>(nonProtocolRequiredNs): null, onlyAddProtocolNs));
        return m;
    }

    public static Unmarshaller getUnmarshallerV1() throws JAXBException  {
        JAXBContext ctx = getContext(PackageVersion.V1);
        return ctx.createUnmarshaller();
    }

    public static Unmarshaller getUnmarshallerV2() throws JAXBException {
        JAXBContext ctx = getContext(PackageVersion.V2);
        return ctx.createUnmarshaller();
    }

    private static JAXBContext getContext(PackageVersion ver) throws JAXBException {
        JAXBContext context = null;

        final AtomicReference<JAXBContext> contextForVersion;
        final Functions.NullaryThrows<JAXBContext, JAXBException> contextFactory;
        switch (ver) {
            case V1:
                contextForVersion = jaxbContextV1;
                contextFactory = ctxFactoryV1;
                break;
            case V2:
                contextForVersion = jaxbContextV2;
                contextFactory = ctxFactoryV2;
                break;
            default:
                throw new IllegalStateException("Unknown JAXB version");//Can't happen
        }

        if (useStaticContext) {
            context = contextForVersion.get();
        }

        if ( context == null ) {
            context = contextFactory.call();

            if (useStaticContext) {
                contextForVersion.compareAndSet(null, context);
            }
        }

        return context;
    }

    enum PackageVersion {
        V1,
        V2
    }

    protected static class SamlpNamespacePrefixMapper extends NamespacePrefixMapper {

        public SamlpNamespacePrefixMapper(@Nullable Set<Pair<String, String>> nonProtocolRequiredNs, boolean onlyAddProtocolNs) {
            this.nonProtocolRequiredNs = (nonProtocolRequiredNs != null)? nonProtocolRequiredNs : Collections.<Pair<String,String>>emptySet();
            this.onlyAddProtocolNs = onlyAddProtocolNs;
        }

        @Override
        public String getPreferredPrefix(String namespaceUri, String suggestion, boolean requirePrefix) {
            if (NS_PREFIXES.containsKey(namespaceUri))
                return NS_PREFIXES.get(namespaceUri);
            return suggestion;
        }

        /**
         * @return the String array of [prefix, namespace URI] which should not be added to the marshaled element, owning
         * to it being declared in scope already if it is needed.
         */
        @Override
        public String[] getContextualNamespaceDecls() {
            if (!onlyAddProtocolNs) {
                return super.getContextualNamespaceDecls();
            }

            final Set<Pair<String, String>> returnSet = new HashSet<Pair<String, String>>();
            for (Pair<String, String> knownNameSpace : allSupportedNs) {
                if (!nonProtocolRequiredNs.contains(knownNameSpace)) {
                    returnSet.add(knownNameSpace);
                }
            }

            // always include protocol
            //noinspection unchecked
            returnSet.removeAll(Arrays.asList(SAMLP, SAMLP2));

            final List<List<String>> transformed = Functions.map(returnSet, new Functions.Unary<List<String>, Pair<String, String>>() {
                @Override
                public List<String> call(Pair<String, String> pair) {
                    return Arrays.asList(pair.right, pair.left);
                }
            });

            final Collection<String> joined = CollectionUtils.join(transformed);
            return joined.toArray(new String[joined.size()]);
        }
        final Set<Pair<String, String>> nonProtocolRequiredNs;
        final boolean onlyAddProtocolNs;
    }
}
