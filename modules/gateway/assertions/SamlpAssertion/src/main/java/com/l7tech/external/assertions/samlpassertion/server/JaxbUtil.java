package com.l7tech.external.assertions.samlpassertion.server;

import com.l7tech.security.saml.SamlConstants;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.Functions;
import com.sun.xml.bind.marshaller.NamespacePrefixMapper;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
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

    public static final Map<String, String> NS_PREFIXES =  Collections.unmodifiableMap(new HashMap<String, String>() {{
        put(SamlConstants.NS_SAML,  SamlConstants.NS_SAML_PREFIX);
        put(SamlConstants.NS_SAML2,  SamlConstants.NS_SAML2_PREFIX);
        put(SamlConstants.NS_SAMLP,  SamlConstants.NS_SAMLP_PREFIX);
        put(SamlConstants.NS_SAMLP2,  SamlConstants.NS_SAMLP2_PREFIX);
        put("http://www.w3.org/2000/09/xmldsig#", "ds");
        put("http://www.w3.org/2001/04/xmlenc#", "xenc");
        put(SamlConstants.AUTHENTICATION_SAML2_PASSWORD, "saccpwd");
        put(SamlConstants.AUTHENTICATION_SAML2_XMLDSIG, "saccxds");
        put(SamlConstants.AUTHENTICATION_SAML2_TLS_CERT, "sacctlsc");
        put("urn:oasis:names:tc:SAML:2.0:ac", "ac");
    }});

    public static Marshaller getMarshallerV1() throws JAXBException {
        JAXBContext ctx = getContext(PackageVersion.V1);
        Marshaller m = ctx.createMarshaller();
        m.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
        m.setProperty("com.sun.xml.bind.namespacePrefixMapper", new SamlpNamespacePrefixMapper());
        return m;
    }

    public static Marshaller getMarshallerV2() throws JAXBException {
        JAXBContext ctx = getContext(PackageVersion.V2);
        Marshaller m = ctx.createMarshaller();
        m.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
        m.setProperty("com.sun.xml.bind.namespacePrefixMapper", new SamlpNamespacePrefixMapper());
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

        @Override
        public String getPreferredPrefix(String namespaceUri, String suggestion, boolean requirePrefix) {
            if (NS_PREFIXES.containsKey(namespaceUri))
                return NS_PREFIXES.get(namespaceUri);
            return suggestion;
        }
    }

}
