/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy;

import com.l7tech.common.util.Locator;
import com.l7tech.identity.IdentityProviderConfigManager;
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
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
            // All credential sources, in descending order of preference
            new SecureConversation(),
            new RequestWssX509Cert(),
            new HttpClientCert(),
            new HttpDigest(),
            new AllAssertion(Arrays.asList(new Assertion[] {
                // SSL required for cleartext credential sources
                new SslAssertion(SslAssertion.REQUIRED),
                new OneOrMoreAssertion(Arrays.asList(new Assertion[] {
                    new WssBasic(),
                    new HttpBasic(),
                }))
            }))
        } ));

        certBasedCredentialSources = new OneOrMoreAssertion(Arrays.asList(new Assertion[] {
            // Certificate-based credential sources only
            new RequestWssX509Cert(),
            new HttpClientCert()
        }));

        configManager = (IdentityProviderConfigManager)Locator.getDefault().lookup(IdentityProviderConfigManager.class);
        identities = new OneOrMoreAssertion();
        identities.getChildren().add(new PermissiveIdentityAssertion(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID));
        updateIdentities();

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
        synchronized(identities) {
            updateIdentities();
            return certPolicy;
        }
    }

    public AllAssertion getDefaultPolicy() {
        synchronized(identities) {
            updateIdentities();
            return defaultPolicy;
        }
    }

    private void updateIdentities() {
        try {
            // Don't bother updating if it's been less than CHECK_DELAY since we did it last
            if (lastVersionCheck >= (System.currentTimeMillis() - CHECK_DELAY)) return;

            Map versions = configManager.findVersionMap();

            for ( Iterator i = versions.keySet().iterator(); i.hasNext(); ) {
                Long providerOid = (Long)i.next();
                Integer currentVersion = (Integer)versions.get(providerOid);
                Integer previousVersion = (Integer)providerVersionMap.get(providerOid);
                if (currentVersion == null) {
                    logger.info("Removing deleted provider " + providerOid + " from default policies");
                    for ( Iterator j = identities.getChildren().iterator(); j.hasNext(); ) {
                        PermissiveIdentityAssertion assertion = (PermissiveIdentityAssertion)j.next();
                        if (assertion.getIdentityProviderOid() == providerOid.longValue()) {
                            j.remove();
                        }
                    }
                    i.remove();
                } else if (previousVersion == null) {
                    logger.info("Adding new provider " + providerOid + " to default policies");
                    identities.getChildren().add(new PermissiveIdentityAssertion(providerOid.longValue()));
                }
            }
            lastVersionCheck = System.currentTimeMillis();
            providerVersionMap = versions;
        } catch ( FindException e ) {
            logger.log( Level.INFO, e.getMessage(), e );
        }
    }

    private static final Logger logger = Logger.getLogger(DefaultGatewayPolicies.class.getName());

    private final OneOrMoreAssertion credentialSources;
    private final OneOrMoreAssertion identities;
    private final AllAssertion defaultPolicy;
    private final AllAssertion certPolicy;
    private final OneOrMoreAssertion certBasedCredentialSources;
    private final IdentityProviderConfigManager configManager;

    private Map providerVersionMap = new HashMap();
    private long lastVersionCheck;
    public static final int CHECK_DELAY = 5000;
}
