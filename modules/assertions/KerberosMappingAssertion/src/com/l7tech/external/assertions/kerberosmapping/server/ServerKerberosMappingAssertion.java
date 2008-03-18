package com.l7tech.external.assertions.kerberosmapping.server;

import com.l7tech.server.audit.Auditor;
import com.l7tech.server.audit.LogOnlyAuditor;
import com.l7tech.external.assertions.kerberosmapping.KerberosMappingAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.common.security.kerberos.KerberosServiceTicket;
import com.l7tech.common.audit.AssertionMessages;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Collections;
import java.util.HashMap;
import java.util.Arrays;

/**
 * Server side implementation of the KerberosMappingAssertion.
 *
 * @see com.l7tech.external.assertions.kerberosmapping.KerberosMappingAssertion
 */
public class ServerKerberosMappingAssertion extends AbstractServerAssertion<KerberosMappingAssertion> {

    //- PUBLIC

    public ServerKerberosMappingAssertion(final KerberosMappingAssertion assertion,
                                          final ApplicationContext context) throws PolicyAssertionException {
        super(assertion);

        this.auditor = context != null ? new Auditor(this, context, logger) : new LogOnlyAuditor(logger);
        this.realmToSuffixMapping = buildMappings(assertion.getMappings());
    }

    public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        List<LoginCredentials> credentials = context.getCredentials();
        List<LoginCredentials> kerberosCredentials = new ArrayList<LoginCredentials>();

        for ( LoginCredentials creds : credentials ) {
            if ( creds.getFormat() == CredentialFormat.KERBEROSTICKET ) {
                kerberosCredentials.add( creds );
            }
        }

        // remove all kerberos creds as we will replace them with ones that work
        credentials.removeAll( kerberosCredentials );

        for ( LoginCredentials creds : kerberosCredentials ) {
            final KerberosServiceTicket kerberosServiceTicket = (KerberosServiceTicket) creds.getPayload();
            KerberosServiceTicket replacementServiceTicket =
                new KerberosServiceTicket(
                        mapPrincipal( kerberosServiceTicket.getClientPrincipalName() ),
                        kerberosServiceTicket.getServicePrincipalName(),
                        kerberosServiceTicket.getKey(),
                        kerberosServiceTicket.getExpiry(),
                        kerberosServiceTicket.getGSSAPReqTicket() );

            // re-use previously calculated ticket if applicable
            replacementServiceTicket = getCachedTicket(replacementServiceTicket);

            LoginCredentials replacementCredentials =
                    new LoginCredentials(
                            null,
                            null,
                            CredentialFormat.KERBEROSTICKET,
                            creds.getCredentialSourceAssertion(),
                            null,
                            replacementServiceTicket);

            credentials.add( replacementCredentials );

            context.setVariable( "kerberos.realm", extractRealm(kerberosServiceTicket.getClientPrincipalName()) );
            context.setVariable( "kerberos.enterprisePrincipal",
                    isEnterprisePrincipalType(kerberosServiceTicket.getClientPrincipalName()) ? "true" : "false" );
        }

        return AssertionStatus.NONE;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(ServerKerberosMappingAssertion.class.getName());

    private final ThreadLocal<KerberosServiceTicket> mappedCredentials = new ThreadLocal<KerberosServiceTicket>();
    private final Map<String,String> realmToSuffixMapping;
    private final Auditor auditor;

    /**
     * Use the cached version of the ticket if all properties are equal.
     *
     * If we don't do this the auth cache will not work when using windows integrated auth.
     */
    private KerberosServiceTicket getCachedTicket(final KerberosServiceTicket ticket) {
        KerberosServiceTicket cached = ticket;

        KerberosServiceTicket localTicket = mappedCredentials.get();
        if ( localTicket != null &&
             localTicket.getClientPrincipalName().equals( ticket.getClientPrincipalName() ) &&
             localTicket.getServicePrincipalName().equals( ticket.getServicePrincipalName() ) &&
             localTicket.getExpiry() == ticket.getExpiry() &&
             Arrays.equals( localTicket.getKey(), ticket.getKey() ) &&
             Arrays.equals( localTicket.getGSSAPReqTicket().toByteArray(), ticket.getGSSAPReqTicket().toByteArray()) ) {
            // use cached ticket
            cached = localTicket;
        } else {
            // cache new ticket
            mappedCredentials.set( cached );
        }

        return cached;
    }

    private Map<String,String> buildMappings(final String[] mappings) {
        Map<String,String> map = new HashMap<String,String>();

        if ( mappings != null ) {
            for ( String mappingStr : mappings ) {
                String[] nvp = mappingStr.split( "!!", 2 );
                map.put( nvp[0].toUpperCase().trim(), nvp[1].trim() );
            }
        }

        return Collections.unmodifiableMap( map );
    }

    /**
     * Map the given principal to a format suitable for using in an LDAP lookup.
     *
     * @param principal The kerberos principal
     * @return a mapped name
     */
    private String mapPrincipal( final String principal ) {
        String mappedPrincipal = principal;

        int firstIndex = principal.indexOf( "@" );
        int index = principal.lastIndexOf( "@" );
        if ( index > -1 ) {
            String realm = extractRealm( principal );
            String suffix = realmToSuffixMapping.get( realm );
            if ( suffix != null || firstIndex != index ) {
                mappedPrincipal = principal.substring( 0, index );

                if ( firstIndex == index ) {
                    // then this is not a UPN format so add suffix
                    auditor.logAndAudit(
                        AssertionMessages.USERDETAIL_INFO,
                        "Mapping Realm '"+realm+"', to UPN Suffix '"+suffix+"'." );

                    // if the suffix is empty then we want to have a name with
                    // no @ suitable for lookup by sAMAccountName
                    if ( suffix.length() > 0 ) {
                        if ( suffix.startsWith( "@" )) {
                            mappedPrincipal += suffix;
                        } else {
                            mappedPrincipal += "@" + suffix;
                        }
                    }
                }
            }
        }

        return mappedPrincipal;
    }

    private String extractRealm( final String principal ) {
        String realm = "";

        int index = principal.lastIndexOf( "@" );
        if ( index > -1 ) {
            realm = principal.substring( index + 1 );              
        }

        return realm;
    }

    private boolean isEnterprisePrincipalType( final String principal ) {
        return principal.split( "@" ).length == 3;
    }
}
