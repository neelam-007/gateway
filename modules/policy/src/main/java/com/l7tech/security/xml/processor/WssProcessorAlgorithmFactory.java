package com.l7tech.security.xml.processor;

import com.ibm.xml.dsig.Canonicalizer;
import com.ibm.xml.dsig.SignatureMethod;
import com.ibm.xml.dsig.Transform;
import com.ibm.xml.dsig.transform.FixedExclusiveC11r;
import com.ibm.xml.enc.AlgorithmFactoryExtn;
import com.ibm.xml.enc.EncryptionEngine;
import com.ibm.xml.enc.StructureException;
import com.ibm.xml.enc.type.EncryptionMethod;
import com.l7tech.security.xml.*;
import static com.l7tech.util.CollectionUtils.set;
import com.l7tech.util.ConfigFactory;
import static com.l7tech.util.ConfigFactory.getProperty;
import com.l7tech.util.Option;
import static com.l7tech.util.Option.optional;
import static com.l7tech.util.TextUtils.split;
import com.l7tech.xml.soap.SoapUtil;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Node;

import javax.crypto.NoSuchPaddingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * An XSS4J AlgorithmFactory that adds some additonal features:
 * <ul>
 * <li>Exclusive canonicalization (xml-exc-c14n) now supports a non-empty PrefixList attribute
 * <li>The XSLT, XPATH, and xmldsig-filter2, Base64 and Decryption transforms are disallowed
 * <li>The STR-Transform is supported (if a lookup map is provided)
 * </ul>
 *
 */
public class WssProcessorAlgorithmFactory extends AlgorithmFactoryExtn {
    public static final java.lang.String TRANSFORM_DECRYPT = "http://www.w3.org/2001/04/decrypt#";
    public static final java.lang.String TRANSFORM_DECRYPT_XML = "http://www.w3.org/2002/07/decrypt#XML";
    public static final java.lang.String TRANSFORM_DECRYPT_BINARY = "http://www.w3.org/2002/07/decrypt#Binary";

    private static final Pattern LIST_SPLITTER = Pattern.compile( "," );
    private static final boolean USE_IBM_EXC_C11R = ConfigFactory.getBooleanProperty( "com.l7tech.common.security.xml.c14n.useIbmExcC11r", false );
    private static final String PROP_PERMITTED_DIGEST_ALGS = "com.l7tech.security.xml.dsig.permittedDigestAlgorithms";
    private static final String PROP_PERMITTED_TRANSFORM_ALGS = "com.l7tech.security.xml.dsig.permittedTransformAlgorithms";
    private static final String DEFAULT_PERMITTED_DIGEST_ALGS = "SHA,SHA-1,SHA-256,SHA-384,SHA-512";
    private static final String DEFAULT_PERMITTED_TRANSFORM_ALGS =
            "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#STR-Transform," +
            "http://docs.oasis-open.org/wss/2004/XX/oasis-2004XX-wss-swa-profile-1.0#Attachment-Complete-Transform," +
            "http://docs.oasis-open.org/wss/2004/XX/oasis-2004XX-wss-swa-profile-1.0#Attachment-Content-Only-Transform," +
            "http://www.w3.org/2000/09/xmldsig#enveloped-signature," +
            "http://www.w3.org/2001/10/xml-exc-c14n#," +
            "http://www.w3.org/2001/10/xml-exc-c14n#WithComments";

    private final Map<Node, Node> strToTarget;
    private boolean sawEnvelopedTransform = false;

    private final Map<String, String> ecdsaSignatureMethodTable = new HashMap<String, String>();
    private final Set<String> enabledDigestSet;
    private final Set<String> enabledTransformSet;

    /**
     * Create a basic algorithm factory
     */
    public WssProcessorAlgorithmFactory() {
        this( null );
    }

    /**
     * Create an algorithm factory that will allow the STR-Transform.
     *
     * @param strToTarget a map of SecurityTokenReference -> target nodes.  If null, STR-Transform will not be supported.
     */
    @SuppressWarnings("unchecked")
    public WssProcessorAlgorithmFactory( @Nullable final Map<Node, Node> strToTarget) {
        this.strToTarget = strToTarget;
        this.signatureMethodTable.put(SupportedSignatureMethods.RSA_SHA256.getAlgorithmIdentifier(), "SHA256withRSA");
        this.signatureMethodTable.put(SupportedSignatureMethods.RSA_SHA384.getAlgorithmIdentifier(), "SHA384withRSA");
        this.signatureMethodTable.put(SupportedSignatureMethods.RSA_SHA512.getAlgorithmIdentifier(), "SHA512withRSA");
        this.ecdsaSignatureMethodTable.put(SupportedSignatureMethods.ECDSA_SHA1.getAlgorithmIdentifier(), "SHA1withECDSA");
        this.ecdsaSignatureMethodTable.put(SupportedSignatureMethods.ECDSA_SHA256.getAlgorithmIdentifier(), "SHA256withECDSA");
        this.ecdsaSignatureMethodTable.put(SupportedSignatureMethods.ECDSA_SHA384.getAlgorithmIdentifier(), "SHA384withECDSA");
        this.ecdsaSignatureMethodTable.put(SupportedSignatureMethods.ECDSA_SHA512.getAlgorithmIdentifier(), "SHA512withECDSA");

        final String enabledDigestStr = getProperty( PROP_PERMITTED_DIGEST_ALGS, DEFAULT_PERMITTED_DIGEST_ALGS );
        final String[] enabledDigests = enabledDigestStr == null ? new String[0] : LIST_SPLITTER.split( enabledDigestStr.toUpperCase() );
        enabledDigestSet = new HashSet<String>();
        for ( final String digest : enabledDigests) {
            Collection<String> aliases = SupportedDigestMethods.getAliases(digest);
            if (aliases != null)
                enabledDigestSet.addAll(aliases);
        }

        final Option<String[]> transformsOption =
                optional(getProperty( PROP_PERMITTED_TRANSFORM_ALGS, DEFAULT_PERMITTED_TRANSFORM_ALGS ))
                        .map( split( LIST_SPLITTER ) );
        enabledTransformSet = transformsOption.isSome() ? set( transformsOption.some() ) : Collections.<String>emptySet();
    }

    @Override
    public Canonicalizer getCanonicalizer(String string) {
         if (Transform.C14N_EXCLUSIVE.equals(string)) {
             // Use the Sun canonicalizer to avoid some bugs in xss4j (Bug #6002)
             // When using IBM c11r, the FixedExclusiveC11r respects the PrefixList (Bug #3611)
             return USE_IBM_EXC_C11R ? new FixedExclusiveC11r() : new ApacheExclusiveC14nAdaptor();
         }
         return super.getCanonicalizer(string);
     }

    @Override
    public SignatureMethod getSignatureMethod(String uri, Object o) throws NoSuchAlgorithmException, NoSuchProviderException {
        String sigMethod = ecdsaSignatureMethodTable.get(uri);
        if (sigMethod == null)
            return checkSignatureMethod(super.getSignatureMethod(uri, o));

        return checkSignatureMethod(new EcdsaSignatureMethod(sigMethod, uri, getProvider()));
    }

    private SignatureMethod checkSignatureMethod(SignatureMethod signatureMethod) throws NoSuchAlgorithmException {
        // Ensure that the signature method uses a digest algorithm which is enabled
        SupportedSignatureMethods suppsig = SupportedSignatureMethods.fromSignatureAlgorithm(signatureMethod.getURI());
        if (!enabledDigestSet.contains(suppsig.getDigestAlgorithmName().toUpperCase()))
            throw new NoSuchAlgorithmException("The algorithm " + suppsig.getDigestAlgorithmName() + " is not permitted for digital signature verification");
        return signatureMethod;
    }

    @Override
    public Transform getTransform( final String uri ) throws NoSuchAlgorithmException {
        if ( !enabledTransformSet.contains( uri ) )
            throw new NoSuchAlgorithmException( uri );

        if (Transform.ENVELOPED.equals(uri)) {
            sawEnvelopedTransform = true;
        } else if (SoapUtil.TRANSFORM_STR.equals(uri) && strToTarget != null) {
            return new STRTransform(strToTarget);
        }else if (SoapUtil.TRANSFORM_ATTACHMENT_CONTENT.equals(uri)) {
            return new AttachmentContentTransform();
        } else if (SoapUtil.TRANSFORM_ATTACHMENT_COMPLETE.equals(uri)) {
            return new AttachmentCompleteTransform();
        } else if (Transform.C14N_EXCLUSIVE.equals(uri) && !USE_IBM_EXC_C11R) {
            return new ApacheExclusiveC14nAdaptor();
        }

        return super.getTransform(uri);
    }

    /** @return true if an #enveloped-signature transform algorithm has ever been requested. */
    public boolean isSawEnvelopedTransform() {
        return sawEnvelopedTransform;
    }

    @Override
    public MessageDigest getDigestMethod(String s) throws NoSuchAlgorithmException, NoSuchProviderException {

        MessageDigest md;

        if ("http://www.w3.org/2001/04/xmlenc#sha256".equals(s)) {
            md = MessageDigest.getInstance("SHA-256");
        } else if ("http://www.w3.org/2001/04/xmldsig-more#sha384".equals(s)) {
            md = MessageDigest.getInstance("SHA-384");
        } else if ("http://www.w3.org/2001/04/xmlenc#sha512".equals(s)) {
            md = MessageDigest.getInstance("SHA-512");
        } else {
            md = super.getDigestMethod(s);
        }
        return checkDigestMethod(md);
    }

    @Override
    public EncryptionEngine getEncryptionEngine(EncryptionMethod encMeth) throws NoSuchAlgorithmException, NoSuchPaddingException, NoSuchProviderException, StructureException {
        final String algorithm = encMeth.getAlgorithm();
        if (XencUtil.AES_128_GCM.equals(algorithm)) {
            return new AesGcmEncryptionEngine(128);
        } else if (XencUtil.AES_256_GCM.equals(algorithm)) {
            return new AesGcmEncryptionEngine(256);
        }

        return super.getEncryptionEngine(encMeth);
    }

    public static void clearAlgorithmPools() {
        rsaPool.clear();
        dsaPool.clear();
        sha1Pool.clear();
        hmacPool.clear();
    }

    private MessageDigest checkDigestMethod(MessageDigest md) throws NoSuchAlgorithmException {
        // Ensure that the digest method uses a digest algorithm which is enabled
        if (!enabledDigestSet.contains(md.getAlgorithm().toUpperCase()))
            throw new NoSuchAlgorithmException("The algorithm " + md.getAlgorithm() + " is not permitted for use as the digest method for signature verification.");
        return md;
    }
}
