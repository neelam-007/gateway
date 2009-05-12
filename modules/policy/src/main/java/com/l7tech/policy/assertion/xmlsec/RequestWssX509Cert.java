package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.annotation.ProcessesRequest;
import com.l7tech.policy.assertion.annotation.RequiresSOAP;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.util.Functions;

/**
 * This assertion verifies that the soap request contained
 * an xml digital signature but does not care about which
 * elements were signed. The cert used for the signature is
 * remembered to identify the user. This cert can later
 * be used for comparaison in an identity assertion.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jul 14, 2004<br/>
 * $Id$<br/>
 */
@ProcessesRequest
@RequiresSOAP(wss=true)
public class RequestWssX509Cert extends SecurityHeaderAddressableSupport {
    
    /**
     * The WSS X509 security token is credential source.
     *
     * @return always true
     */
    @Override
    public boolean isCredentialSource() {
        return true;
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(AssertionMetadata.SHORT_NAME, "WSS Signature");
        meta.put(AssertionMetadata.DESCRIPTION, "The soap request must contain a WSS signature with an X509 SecurityToken");
        meta.putNull(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME);
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlencryption.gif");
        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, new Functions.Unary<String, RequestWssX509Cert>() {
            @Override
            public String call( final RequestWssX509Cert requestWssX509Cert ) {
                StringBuilder name = new StringBuilder("WSS Sign SOAP Request");
                name.append(SecurityHeaderAddressableSupport.getActorSuffix(requestWssX509Cert));
                return name.toString();
            }
        });

        return meta;
    }
}
