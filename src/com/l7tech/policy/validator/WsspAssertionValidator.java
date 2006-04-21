package com.l7tech.policy.validator;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.FalseAssertion;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.policy.assertion.TrueAssertion;
import com.l7tech.policy.assertion.WsspAssertion;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.credential.http.HttpDigest;
import com.l7tech.policy.assertion.credential.wss.EncryptedUsernameTokenAssertion;
import com.l7tech.policy.assertion.credential.wss.WssBasic;
import com.l7tech.policy.assertion.xmlsec.RequestWssConfidentiality;
import com.l7tech.policy.assertion.xmlsec.RequestWssIntegrity;
import com.l7tech.policy.assertion.xmlsec.RequestWssKerberos;
import com.l7tech.policy.assertion.xmlsec.RequestWssReplayProtection;
import com.l7tech.policy.assertion.xmlsec.RequestWssSaml;
import com.l7tech.policy.assertion.xmlsec.RequestWssTimestamp;
import com.l7tech.policy.assertion.xmlsec.RequestWssX509Cert;
import com.l7tech.policy.assertion.xmlsec.ResponseWssConfidentiality;
import com.l7tech.policy.assertion.xmlsec.ResponseWssIntegrity;
import com.l7tech.policy.assertion.xmlsec.SecureConversation;
import com.l7tech.policy.assertion.xmlsec.WssTimestamp;
import com.l7tech.service.PublishedService;

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
    public void validate(AssertionPath path, PublishedService service, PolicyValidatorResult result) {
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
                    "Assertion cannot be used with WS-SecurityPolicy assertion.", null));
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(WsspAssertionValidator.class.getName());

    // All supported assertions
    private static final Collection SUPPORTED_ASSERTIONS = Collections.unmodifiableCollection(Arrays.asList(new Object[]{
        SslAssertion.class,
        WssBasic.class,
        WssTimestamp.class,
        RequestWssTimestamp.class,
        RequestWssX509Cert.class,
        RequestWssIntegrity.class,
        RequestWssConfidentiality.class,
        ResponseWssIntegrity.class,
        ResponseWssConfidentiality.class,
    }));

    // All client assertions
    private static final Collection CLIENT_ASSERTIONS = Collections.unmodifiableCollection(Arrays.asList(new Object[]{
        FalseAssertion.class,
        SslAssertion.class,
        TrueAssertion.class,
        HttpBasic.class,
        HttpDigest.class,
        WssBasic.class,
        EncryptedUsernameTokenAssertion.class,
        RequestWssX509Cert.class,
        SecureConversation.class,
        RequestWssIntegrity.class,
        RequestWssConfidentiality.class,
        ResponseWssIntegrity.class,
        ResponseWssConfidentiality.class,
        //RequestXpathAssertion.class, fla, please leave commented - this is causing mucho problems
        //ResponseXpathAssertion.class, fla, please leave commented - this is causing mucho problems
        RequestWssReplayProtection.class,
        RequestWssSaml.class,
        RequestWssKerberos.class,
        WssTimestamp.class
    }));
}
