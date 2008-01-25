package com.l7tech.common.security.xml.processor;

import com.ibm.xml.enc.AlgorithmFactoryExtn;
import com.ibm.xml.dsig.Canonicalizer;
import com.ibm.xml.dsig.Transform;
import com.ibm.xml.dsig.SignatureMethod;
import com.ibm.xml.dsig.transform.FixedExclusiveC11r;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.security.xml.STRTransform;
import com.l7tech.common.security.xml.AttachmentCompleteTransform;
import com.l7tech.common.security.xml.AttachmentContentTransform;

import org.w3c.dom.Node;

import java.util.Map;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

/**
 * An XSS4J AlgorithmFactory that adds some additonal features:
 * <ul>
 * <li>Exclusive canonicalization (xml-exc-c14n) now supports a non-empty PrefixList attribute
 * <li>The XSLT, XPATH, and xmldsig-filter2 transforms are disallowed
 * <li>The STR-Transform is supported (if a lookup map is provided)
 * <li>RSA signature checking is cached using {@link com.l7tech.common.security.xml.processor.MemoizedRsaSha1SignatureMethod}.
 * </ul>
 *
 */
public class WssProcessorAlgorithmFactory extends AlgorithmFactoryExtn {
    private final Map<Node, Node> strToTarget;
    private boolean sawEnvelopedTransform = false;

    /**
     * Create an algorithm factory that will allow the STR-Transform.
     *
     * @param strToTarget a map of SecurityTokenReference -> target nodes.  If null, STR-Transform will not be supported.
     */
    public WssProcessorAlgorithmFactory(Map<Node, Node> strToTarget) {
        this.strToTarget = strToTarget;
    }

    public Canonicalizer getCanonicalizer(String string) {
         if (Transform.C14N_EXCLUSIVE.equals(string)) {
             // Fixed canonicalizer respects the PrefixList
             // See bug 3611
             return new FixedExclusiveC11r();
         }
         return super.getCanonicalizer(string);
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
        }
        return super.getTransform(s);
    }

    /** @return true if an #enveloped-signature transform algorithm has ever been requested. */
    public boolean isSawEnvelopedTransform() {
        return sawEnvelopedTransform;
    }

    public SignatureMethod getSignatureMethod(String alg, Object param) throws NoSuchAlgorithmException, NoSuchProviderException {
        final SignatureMethod sm = super.getSignatureMethod(alg, param);
        if (SignatureMethod.RSA.equals(alg) && MemoizedRsaSha1SignatureMethod.isEnabled()) {
            // Wrap with memoized version
            return new MemoizedRsaSha1SignatureMethod(sm);
        }
        return sm;
    }

    public void releaseSignatureMethod(SignatureMethod sm) {
        if (sm instanceof MemoizedRsaSha1SignatureMethod) {
            MemoizedRsaSha1SignatureMethod msm = (MemoizedRsaSha1SignatureMethod)sm;
            super.releaseSignatureMethod(msm.getDelegate());
        } else
            super.releaseSignatureMethod(sm);
    }

}
