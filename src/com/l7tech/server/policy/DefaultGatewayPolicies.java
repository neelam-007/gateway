/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy;

import com.l7tech.common.util.Locator;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.credential.http.HttpClientCert;
import com.l7tech.policy.assertion.credential.http.HttpDigest;
import com.l7tech.policy.assertion.credential.wss.WssBasic;
import com.l7tech.policy.assertion.identity.PermissiveIdentityAssertion;
import com.l7tech.policy.assertion.xmlsec.RequestWssX509Cert;
import com.l7tech.policy.assertion.xmlsec.SecureConversation;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public class DefaultGatewayPolicies {
    public static DefaultGatewayPolicies getInstance() {
        return SingletonHolder.instance;
    }

    private static class SingletonHolder {
        static final DefaultGatewayPolicies instance = new DefaultGatewayPolicies();
    }

    private DefaultGatewayPolicies() {
        credentialSources = new OneOrMoreAssertion(Arrays.asList(new Assertion[] {
                                new AllAssertion(Arrays.asList(new Assertion[] {
                                    // SSL required for cleartext credential sources
                                    new SslAssertion(),
                                    new OneOrMoreAssertion(Arrays.asList(new Assertion[] {
                                        new HttpBasic(),
                                        new WssBasic(),
                                    }))
                                })),
                                // All other credential sources
                                new HttpClientCert(),
                                new HttpDigest(),
                                new RequestWssX509Cert(),
                                new SecureConversation(),
                            } ));

        certBasedCredentialSources = new OneOrMoreAssertion(Arrays.asList(new Assertion[] {
            // Certificate-based credential sources only
            new HttpClientCert(),
            new RequestWssX509Cert()
        }));

        IdentityProviderConfigManager ipcm = (IdentityProviderConfigManager)Locator.getDefault().lookup(IdentityProviderConfigManager.class);
        List identityAssertions = new ArrayList();
        try {
            Collection providers = ipcm.findAllHeaders();
            for ( Iterator i = providers.iterator(); i.hasNext(); ) {
                EntityHeader header = (EntityHeader)i.next();
                PermissiveIdentityAssertion ident = new PermissiveIdentityAssertion(header.getOid());
                identityAssertions.add(ident);
            }
            identities = new OneOrMoreAssertion(identityAssertions);
        } catch ( FindException e ) {
            final String msg = "Couldn't list identity providers";
            logger.log(Level.SEVERE, msg, e);
            throw new RuntimeException(msg, e);
        }

        defaultPolicy = new AllAssertion(Arrays.asList(new Assertion[] {
            credentialSources,
            identities
        }));

        certPolicy = new AllAssertion(Arrays.asList(new Assertion[] {
            certBasedCredentialSources,
            identities
        }));
    }

    public AllAssertion getCertPolicy() {
        return certPolicy;
    }

    public AllAssertion getDefaultPolicy() {
        return defaultPolicy;
    }

    private static final Logger logger = Logger.getLogger(DefaultGatewayPolicies.class.getName());

    private final OneOrMoreAssertion credentialSources;
    private final OneOrMoreAssertion identities;
    private final AllAssertion defaultPolicy;
    private final AllAssertion certPolicy;
    private OneOrMoreAssertion certBasedCredentialSources;
}
