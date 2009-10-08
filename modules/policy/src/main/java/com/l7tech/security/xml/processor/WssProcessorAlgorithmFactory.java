package com.l7tech.security.xml.processor;

import com.ibm.xml.dsig.Canonicalizer;
import com.ibm.xml.dsig.Transform;
import com.ibm.xml.dsig.SignatureMethod;
import com.ibm.xml.dsig.transform.FixedExclusiveC11r;
import com.ibm.xml.enc.AlgorithmFactoryExtn;
import com.l7tech.security.xml.AttachmentCompleteTransform;
import com.l7tech.security.xml.AttachmentContentTransform;
import com.l7tech.security.xml.STRTransform;
import com.l7tech.security.xml.SupportedSignatureMethods;
import com.l7tech.xml.soap.SoapUtil;
import org.w3c.dom.Node;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Map;
import java.util.HashMap;

/**
 * An XSS4J AlgorithmFactory that adds some additonal features:
 * <ul>
 * <li>Exclusive canonicalization (xml-exc-c14n) now supports a non-empty PrefixList attribute
 * <li>The XSLT, XPATH, and xmldsig-filter2 transforms are disallowed
 * <li>The STR-Transform is supported (if a lookup map is provided)
 * </ul>
 *
 */
public class WssProcessorAlgorithmFactory extends AlgorithmFactoryExtn {
    private static final boolean USE_IBM_EXC_C11R = Boolean.getBoolean("com.l7tech.common.security.xml.c14n.useIbmExcC11r");

    private final Map<Node, Node> strToTarget;
    private boolean sawEnvelopedTransform = false;

    private Map<String, String> ecdsaSignatureMethodTable = new HashMap<String, String>();

    /**
     * Create an algorithm factory that will allow the STR-Transform.
     *
     * @param strToTarget a map of SecurityTokenReference -> target nodes.  If null, STR-Transform will not be supported.
     */
    @SuppressWarnings("unchecked")
    public WssProcessorAlgorithmFactory(Map<Node, Node> strToTarget) {
        this.strToTarget = strToTarget;
        this.signatureMethodTable.put(SupportedSignatureMethods.RSA_SHA256.getAlgorithmIdentifier(), "SHA256withRSA");
        this.ecdsaSignatureMethodTable.put(SupportedSignatureMethods.ECDSA_SHA1.getAlgorithmIdentifier(), "SHA1withECDSA");
        this.ecdsaSignatureMethodTable.put(SupportedSignatureMethods.ECDSA_SHA256.getAlgorithmIdentifier(), "SHA256withECDSA");
        this.ecdsaSignatureMethodTable.put(SupportedSignatureMethods.ECDSA_SHA384.getAlgorithmIdentifier(), "SHA384withECDSA");
        this.ecdsaSignatureMethodTable.put(SupportedSignatureMethods.ECDSA_SHA512.getAlgorithmIdentifier(), "SHA512withECDSA");
    }

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
            return super.getSignatureMethod(uri, o);

        return new EcdsaSignatureMethod(sigMethod, uri, getProvider());
    }

    public Transform getTransform(String s) throws NoSuchAlgorithmException {
        if (Transform.ENVELOPED.equals(s)) {
            sawEnvelopedTransform = true;
        } else if (SoapUtil.TRANSFORM_STR.equals(s) && strToTarget != null) {
            return new STRTransform(strToTarget);
        } else if (Transform.XSLT.equals(s)
                   || Transform.XPATH.equals(s)
                   || Transform.XPATH2.equals(s)) {
            throw new NoSuchAlgorithmException(s);
        } else if (SoapUtil.TRANSFORM_ATTACHMENT_CONTENT.equals(s)) {
            return new AttachmentContentTransform();
        } else if (SoapUtil.TRANSFORM_ATTACHMENT_COMPLETE.equals(s)) {
            return new AttachmentCompleteTransform();
        } else if (Transform.C14N_EXCLUSIVE.equals(s) && !USE_IBM_EXC_C11R) {
            return new ApacheExclusiveC14nAdaptor();
        }
        return super.getTransform(s);
    }

    /** @return true if an #enveloped-signature transform algorithm has ever been requested. */
    public boolean isSawEnvelopedTransform() {
        return sawEnvelopedTransform;
    }

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
        return md;
    }
}
