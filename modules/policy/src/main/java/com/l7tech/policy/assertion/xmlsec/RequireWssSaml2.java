package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.security.saml.SamlConstants;
import com.l7tech.util.Functions;

/**
 * Require WS-Security SAML for version 2.0
 *
 * @see RequireWssSaml
 * @see RequireSaml
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class RequireWssSaml2 extends RequireWssSaml {

    public RequireWssSaml2() {
        super();
        setVersion(Integer.valueOf(2));
    }

    public RequireWssSaml2(RequireWssSaml requestWssSaml) {
        super(requestWssSaml);
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = (DefaultAssertionMetadata) super.meta();

        meta.put(AssertionMetadata.CLIENT_ASSERTION_CLASSNAME, "com.l7tech.proxy.policy.assertion.xmlsec.ClientRequestWssSaml2");
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "accessControl" });
        meta.put(AssertionMetadata.ASSERTION_FACTORY, new Functions.Unary<RequireWssSaml, RequireWssSaml>(){
            @Override
            public RequireWssSaml call( final RequireWssSaml requestWssSaml ) {
                return RequireWssSaml2.newHolderOfKey();
            }
        });

        return meta;
    }

    /**
     * Factory method that creates the Holder-Of-Key assertion
     *
     * @return the RequireWssSaml2 with Holder-Of-Key subject confirmation
     */
    public static RequireWssSaml2 newHolderOfKey() {
        RequireWssSaml2 ass = new RequireWssSaml2();
        setNewHolderOfKeyProperties(ass);
        return ass;
    }
}
