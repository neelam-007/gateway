package com.l7tech.policy.validator;

import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.credential.http.HttpDigest;
import com.l7tech.policy.assertion.credential.http.CookieCredentialSourceAssertion;
import com.l7tech.policy.assertion.credential.http.HttpNegotiate;
import com.l7tech.policy.assertion.credential.wss.EncryptedUsernameTokenAssertion;
import com.l7tech.policy.assertion.credential.wss.WssBasic;
import com.l7tech.policy.assertion.credential.XpathCredentialSource;
import com.l7tech.policy.assertion.xmlsec.*;
import com.l7tech.common.xml.Wsdl;

import java.util.*;
import java.util.logging.Logger;

/**
 * Assertion validator for WS Security Policy compliance.
 *
 * <p>This validator differs from others in that it places additional contraints
 * on other assertions rather than validating its own configuration.</p>
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class WsspAssertionValidator implements AssertionValidator {

    //- PUBLIC

    /**
     *
     */
    public WsspAssertionValidator(WsspAssertion assertion) {
    }

    /**
     *
     */
    public void validate(AssertionPath path, Wsdl wsdl, boolean soap, PolicyValidatorResult result) {
        Map assertionsToTypes = new LinkedHashMap();
        Assertion[] assertions = path.getPath();
        for (int i = 0; i < assertions.length; i++) {
            Assertion assertion = assertions[i];
            assertionsToTypes.put(assertion, assertion.getClass());
        }

        Collection assertionsInPath = assertionsToTypes.values();
        assertionsInPath.retainAll(CLIENT_ASSERTIONS);
        assertionsInPath.removeAll(SUPPORTED_ASSERTIONS);

        for (Iterator iterator = assertionsToTypes.keySet().iterator(); iterator.hasNext();) {
            Assertion assertion = (Assertion) iterator.next();
            result.addError(new PolicyValidatorResult.Error(assertion, path,
                    "Assertion cannot be used with WS-Security Policy Compliance assertion.", null));
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(WsspAssertionValidator.class.getName());

    // All supported assertions
    private static final Collection SUPPORTED_ASSERTIONS = Collections.unmodifiableCollection(Arrays.asList(new Object[]{
        SslAssertion.class,
        WssBasic.class,
        RequestWssTimestamp.class,
        RequestWssX509Cert.class,
        RequestWssIntegrity.class,
        RequestWssConfidentiality.class,
        ResponseWssTimestamp.class,
        ResponseWssIntegrity.class,
        ResponseWssConfidentiality.class,
    }));

    // All client assertions
    private static final Collection CLIENT_ASSERTIONS = Collections.unmodifiableCollection(Arrays.asList(new Object[]{
        SslAssertion.class,
        HttpBasic.class,
        HttpDigest.class,
        WssBasic.class,
        HttpNegotiate.class,
        EncryptedUsernameTokenAssertion.class,
        RequestWssX509Cert.class,
        SecureConversation.class,
        RequestWssIntegrity.class,
        RequestWssConfidentiality.class,
        ResponseWssIntegrity.class,
        ResponseWssConfidentiality.class,
        RequestWssReplayProtection.class,
        RequestWssSaml.class,
        RequestWssSaml2.class,
        RequestWssKerberos.class,
        CookieCredentialSourceAssertion.class,
        RequestWssTimestamp.class,
        ResponseWssTimestamp.class,
        XpathCredentialSource.class,
    }));
}
