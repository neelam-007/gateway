package com.l7tech.security.xml.processor;

import com.ibm.xml.dsig.Canonicalizer;
import com.ibm.xml.dsig.Transform;
import com.ibm.xml.dsig.transform.FixedExclusiveC11r;
import com.ibm.xml.enc.AlgorithmFactoryExtn;
import com.l7tech.security.xml.AttachmentCompleteTransform;
import com.l7tech.security.xml.AttachmentContentTransform;
import com.l7tech.security.xml.STRTransform;
import com.l7tech.util.SoapConstants;
import org.w3c.dom.Node;

import java.security.NoSuchAlgorithmException;
import java.util.Map;

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
             // Use the Sun canonicalizer to avoid some bugs in xss4j (Bug #6002)
             // When using IBM c11r, the FixedExclusiveC11r respects the PrefixList (Bug #3611)
             return USE_IBM_EXC_C11R ? new FixedExclusiveC11r() : new ApacheExclusiveC14nAdaptor();
         }
         return super.getCanonicalizer(string);
     }

    public Transform getTransform(String s) throws NoSuchAlgorithmException {
        if (Transform.ENVELOPED.equals(s)) {
            sawEnvelopedTransform = true;
        } else if ( SoapConstants.TRANSFORM_STR.equals(s) && strToTarget != null) {
            return new STRTransform(strToTarget);
        } else if (Transform.XSLT.equals(s)
                   || Transform.XPATH.equals(s)
                   || Transform.XPATH2.equals(s)) {
            throw new NoSuchAlgorithmException(s);
        } else if ( SoapConstants.TRANSFORM_ATTACHMENT_CONTENT.equals(s)) {
            return new AttachmentContentTransform();
        } else if ( SoapConstants.TRANSFORM_ATTACHMENT_COMPLETE.equals(s)) {
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
}
