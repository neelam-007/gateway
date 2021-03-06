package com.l7tech.policy.validator;

import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.policy.assertion.WsspAssertion;
import com.l7tech.policy.assertion.credential.XpathCredentialSource;
import com.l7tech.policy.assertion.credential.http.CookieCredentialSourceAssertion;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.credential.http.HttpDigest;
import com.l7tech.policy.assertion.credential.http.HttpNegotiate;
import com.l7tech.policy.assertion.credential.wss.EncryptedUsernameTokenAssertion;
import com.l7tech.policy.assertion.credential.wss.WssBasic;
import com.l7tech.policy.assertion.xmlsec.*;

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
    @Override
    public void validate(AssertionPath path, PolicyValidationContext pvc, PolicyValidatorResult result) {
        Map assertionsToTypes = new LinkedHashMap();
        Assertion[] assertions = path.getPath();
        for (int i = 0; i < assertions.length; i++) {
            Assertion assertion = assertions[i];
            if (!assertion.isEnabled()) continue;
            assertionsToTypes.put(assertion, assertion.getClass());
        }

        Collection assertionsInPath = assertionsToTypes.values();
        assertionsInPath.retainAll(CLIENT_ASSERTIONS);
        assertionsInPath.removeAll(SUPPORTED_ASSERTIONS);

        for (Iterator iterator = assertionsToTypes.keySet().iterator(); iterator.hasNext();) {
            Assertion assertion = (Assertion) iterator.next();
            result.addError(new PolicyValidatorResult.Error(assertion,
                    "Assertion cannot be used with the Enforce WS-Security Policy Compliance assertion.", null));
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(WsspAssertionValidator.class.getName());

    // All supported assertions
    private static final Collection SUPPORTED_ASSERTIONS = Collections.unmodifiableCollection(Arrays.asList(new Object[]{
        SslAssertion.class,
        WssBasic.class,
        RequireWssTimestamp.class,
        RequireWssX509Cert.class,
        RequireWssSignedElement.class,
        RequireWssEncryptedElement.class,
        AddWssTimestamp.class,
        WssSignElement.class,
        WssEncryptElement.class,
    }));

    // All client assertions
    private static final Collection CLIENT_ASSERTIONS = Collections.unmodifiableCollection(Arrays.asList(new Object[]{
        SslAssertion.class,
        HttpBasic.class,
        HttpDigest.class,
        WssBasic.class,
        HttpNegotiate.class,
        EncryptedUsernameTokenAssertion.class,
        RequireWssX509Cert.class,
        SecureConversation.class,
        RequireWssSignedElement.class,
        RequireWssEncryptedElement.class,
        WssSignElement.class,
        WssEncryptElement.class,
        WssReplayProtection.class,
        RequireWssSaml.class,
        RequireWssSaml2.class,
        RequestWssKerberos.class,
        CookieCredentialSourceAssertion.class,
        RequireWssTimestamp.class,
        AddWssTimestamp.class,
        XpathCredentialSource.class,
    }));
}
