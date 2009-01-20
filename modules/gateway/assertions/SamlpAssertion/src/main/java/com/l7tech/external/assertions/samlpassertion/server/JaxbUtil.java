package com.l7tech.external.assertions.samlpassertion.server;

import com.l7tech.security.saml.SamlConstants;
import com.sun.xml.bind.marshaller.NamespacePrefixMapper;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Caching mechanism for JAXContext instances (to improve performance).
 *
 * @author : vchan
 */
public class JaxbUtil {
    private static final Logger logger = Logger.getLogger(JaxbUtil.class.getName());

    private static final int UNUSED_CACHE_LIMIT = 10;
    private static final int MAX_TOTAL_CACHE_LIMIT = 100;
    private static final String SAML_V1_CTX_PACKAGES = "saml.v1.protocol:saml.v1.assertion:saml.support.ds";
    private static final String SAML_V2_CTX_PACKAGES = "saml.v2.protocol:saml.v2.assertion:saml.support.ds";

    private static final List<JAXBContextWrapper> jxbContextCacheV1;
    private static final List<JAXBContextWrapper> jxbContextCacheV2;
    private static final Map<String, JAXBContextWrapper> contextInUse;
    private static final Map<String, String> NS_PREFIXES;
    private static final Object syncLock = new Object();

    static {
        // initialize the map
        jxbContextCacheV1 = new ArrayList<JAXBContextWrapper>();
        jxbContextCacheV2 = new ArrayList<JAXBContextWrapper>();
        contextInUse = new HashMap<String, JAXBContextWrapper>();

        NS_PREFIXES = new HashMap<String, String>();
        NS_PREFIXES.put(SamlConstants.NS_SAML,  SamlConstants.NS_SAML_PREFIX);
        NS_PREFIXES.put(SamlConstants.NS_SAML2,  SamlConstants.NS_SAML2_PREFIX);
        NS_PREFIXES.put(SamlConstants.NS_SAMLP,  SamlConstants.NS_SAMLP_PREFIX);
        NS_PREFIXES.put(SamlConstants.NS_SAMLP2,  SamlConstants.NS_SAMLP2_PREFIX);
        NS_PREFIXES.put("http://www.w3.org/2000/09/xmldsig#", "ds");
        NS_PREFIXES.put("http://www.w3.org/2001/04/xmlenc#", "xenc");
    }

    public static Marshaller getMarshallerV1(final String lockId) throws JAXBException {
        JAXBContext ctx = getContext(PackageVersion.V1, lockId).ctx;
        Marshaller m = ctx.createMarshaller();
        m.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
        m.setProperty("com.sun.xml.bind.namespacePrefixMapper", new SamlpNamespacePrefixMapper());
        return m;
    }

    public static Marshaller getMarshallerV2(final String lockId) throws JAXBException {
        JAXBContext ctx = getContext(PackageVersion.V2, lockId).ctx;
        Marshaller m = ctx.createMarshaller();
        m.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
        m.setProperty("com.sun.xml.bind.namespacePrefixMapper", new SamlpNamespacePrefixMapper());
        return m;
    }

    public static Unmarshaller getUnmarshallerV1(final String lockId) throws JAXBException  {
        JAXBContext ctx = getContext(PackageVersion.V1, lockId).ctx;
        return ctx.createUnmarshaller();
    }

    public static Unmarshaller getUnmarshallerV2(final String lockId) throws JAXBException {
        JAXBContext ctx = getContext(PackageVersion.V2, lockId).ctx;
        return ctx.createUnmarshaller();
    }

    public static void releaseJaxbResources(final String lockId) {
        if (contextInUse.containsKey(lockId)) {
            synchronized (syncLock) {
                JAXBContextWrapper wrpr = contextInUse.get(lockId);
                contextInUse.remove(lockId);
                if (wrpr.returnTo.size() > UNUSED_CACHE_LIMIT) {
                    wrpr.dispose();
                } else {
                    wrpr.unlock();
                }
            }
        }
    }

    private static JAXBContextWrapper getContext(PackageVersion ver, final String lockId) throws JAXBException {

        JAXBContextWrapper wrapper = null;
        synchronized (syncLock) {
            switch(ver) {
                case V1:
                    if (!jxbContextCacheV1.isEmpty())
                        wrapper = jxbContextCacheV1.remove(0);
                    break;
                case V2:
                    if (!jxbContextCacheV2.isEmpty())
                        wrapper = jxbContextCacheV2.remove(0);
                    break;
            }

            if (wrapper == null && contextInUse.size() < MAX_TOTAL_CACHE_LIMIT) {
                // create a new instance if no cached instance is found
                wrapper = new JAXBContextWrapper(ver, lockId);
            }

            if (wrapper != null) {
                wrapper.lock(lockId);
                contextInUse.put(lockId, wrapper);
            }
        }

        // no more caching, create per-request-use instance
        if (wrapper == null) {
            wrapper = new JAXBContextWrapper(ver, lockId);
        }
        return wrapper;
    }


    enum PackageVersion {
        V1,
        V2
    }

    protected static class JAXBContextWrapper {

        final PackageVersion version;
        String ctxLock;
        JAXBContext ctx;
        List<JAXBContextWrapper> returnTo;

        JAXBContextWrapper(PackageVersion version, String ctxLock)
            throws JAXBException
        {
            this.version = version;
            this.ctxLock = ctxLock;

            switch(version) {
                case V1:
                    this.ctx = JAXBContext.newInstance(SAML_V1_CTX_PACKAGES, JaxbUtil.class.getClassLoader());
                    this.returnTo = jxbContextCacheV1;
                    break;
                case V2:
                    this.ctx = JAXBContext.newInstance(SAML_V2_CTX_PACKAGES, JaxbUtil.class.getClassLoader());
                    this.returnTo = jxbContextCacheV2;
                    break;
            }
        }

        public void lock(String lockValue) {
            // remove from "returnTo" list before calling lock!!
            this.ctxLock = lockValue;
        }

        public void unlock() {
            this.ctxLock = null;

            synchronized (syncLock) {
                returnTo.add(this);
            }
        }

        public void dispose() {
            ctx = null;
        }
    }

    protected static class SamlpNamespacePrefixMapper extends NamespacePrefixMapper {

        public String getPreferredPrefix(String namespaceUri, String suggestion, boolean requirePrefix) {
            if (NS_PREFIXES.containsKey(namespaceUri))
                return NS_PREFIXES.get(namespaceUri);
            return suggestion;
        }
    }

}
