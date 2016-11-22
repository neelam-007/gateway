package com.l7tech.external.assertions.ldapquery.server;

import com.l7tech.external.assertions.ldapquery.LDAPQueryAssertion;
import com.l7tech.external.assertions.ldapquery.QueryAttributeMapping;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.identity.TestIdentityProviderConfigManager;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.MockTimer;
import com.l7tech.util.ResourceUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

import static org.junit.Assert.*;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


/**
 * Unit tests for ldap query assertion.
 *
 * LDAP interaction is not currently tested.
 */
public class ServerLDAPQueryAssertionTest {

    @Test
    public void testTooManyResults() throws Exception {
        doTestTooManyResults( AssertionStatus.FALSIFIED, true, 1000 );
        doTestTooManyResults( AssertionStatus.FALSIFIED, true, 10 );
        doTestTooManyResults( AssertionStatus.FALSIFIED, true, 1 );
        doTestTooManyResults( AssertionStatus.NONE, true, 1001 );
        doTestTooManyResults( AssertionStatus.NONE, true, 0 );
        doTestTooManyResults( AssertionStatus.NONE, false, 10 );
    }

    @Test
    public void testTooFewResults() throws Exception {
        doTestResultCount( AssertionStatus.FALSIFIED, false, true, 0, 0 );
        doTestResultCount( AssertionStatus.FALSIFIED, false, true, 0, 10 );
        doTestResultCount( AssertionStatus.NONE, false, false, 0, 0 );
        doTestResultCount( AssertionStatus.NONE, false, false, 0, 10 );
    }

    @Test
    public void testSearchFilterInjectionProtection() throws Exception {
        doTestSearchFilterInjectionProtection( true, "(&(cn=*)(objectClass=top))", "(&(cn=*)(objectClass=top))", null );
        doTestSearchFilterInjectionProtection( true, "(cn=${var})", "(cn=value)", "value" );
        doTestSearchFilterInjectionProtection( true, "(cn=${var})", "(cn=\\2a)", "*" );
        doTestSearchFilterInjectionProtection( false, "(cn=${var})", "(cn=*)", "*" );
    }

    @Test
    public void testSingleResult() throws Exception {
        doTestResults( AssertionStatus.NONE, 0, false, false, false, new String[]{ "a" }, "a" );
        doTestResults( AssertionStatus.NONE, 0, false, false, false, new String[0], null );
        doTestResults( AssertionStatus.NONE, 0, true, false, true, new String[]{ "a", "b" }, "a, b" );
        doTestResults( AssertionStatus.NONE, 0, true, false, false, new String[]{ "a", "b" }, new String[]{ "a", "b" } );
        doTestResults( AssertionStatus.NONE, 0, false, true, false, new String[]{ "a" }, "a" );
        doTestResults( AssertionStatus.FALSIFIED, 0, false, true, false, new String[]{ "a", "b" }, null );
    }

    @Test
    public void testMultipleResults() throws Exception {
        doTestResults( AssertionStatus.NONE, 1, false, false, false, new String[]{ "a" }, new String[]{ "a" } );
        doTestResults( AssertionStatus.NONE, 1, false, false, false, new String[0], null );
        doTestResults( AssertionStatus.NONE, 1, true, false, true, new String[]{ "a", "b" }, new String[]{ "a, b" } );
        doTestResults( AssertionStatus.NONE, 1, true, false, false, new String[]{ "a", "b" }, new String[]{ "a", "b" } );
        doTestResults( AssertionStatus.NONE, 1, false, true, false, new String[]{ "a" }, new String[]{ "a" } );

        doTestResults( AssertionStatus.NONE, 2, false, false, false, new String[]{ "a" }, new String[]{ "a", "a" } );
        doTestResults( AssertionStatus.NONE, 2, false, false, false, new String[0], null );
        doTestResults( AssertionStatus.NONE, 2, true, false, true, new String[]{ "a", "b" }, new String[]{ "a, b", "a, b" } );
        doTestResults( AssertionStatus.NONE, 2, true, false, false, new String[]{ "a", "b" }, new String[]{ "a", "b", "a", "b" } );
        doTestResults( AssertionStatus.NONE, 2, false, true, false, new String[]{ "a" }, new String[]{ "a", "a" } );

        doTestResults( AssertionStatus.FALSIFIED, 1, false, true, false, new String[]{ "a", "b" }, null );
    }

    private void doTestResults( final AssertionStatus expectedStatus,
                                final int repeats, // 0 for multiple results not expected
                                final boolean multivalued,
                                final boolean failMultivalued,
                                final boolean joinMultivalued,
                                final String[] results,
                                final Object expectedValue ) throws Exception {
        final LDAPQueryAssertion assertion = new LDAPQueryAssertion();
        assertion.setSearchFilter( "(cn=*)" );
        assertion.setAllowMultipleResults( repeats > 0 );
        assertion.setQueryMappings( new QueryAttributeMapping[]{
            new QueryAttributeMapping( "cn", "lqcn", joinMultivalued, failMultivalued, multivalued )
        } );

        final ServerLDAPQueryAssertion test = new  ServerLDAPQueryAssertion( assertion, beanFactory() ){
            @Override
            protected int doSearch( final String filter,
                                    final String[] attributeNames,
                                    final int maxResults,
                                    final Functions.BinaryVoidThrows<QueryAttributeMapping, ServerLDAPQueryAssertion.SimpleAttribute, Exception> resultCallback ) throws FindException {
                try {
                    int callbacks = repeats==0 ? 1 : repeats;
                    for ( int i=0; i<callbacks; i++ ) {
                        resultCallback.call( assertion.getQueryMappings()[0], new SimpleAttribute( "cn=a", null ){
                            @Override
                            protected String getStringValue( final int index ) throws Exception {
                                return results[ index ];
                            }

                            @Override
                            public boolean isPresent() {
                                return results.length > 0;
                            }

                            @Override
                            public int getSize() {
                                return results.length;
                            }
                        } );
                    }
                } catch ( Exception e ) {
                    throw ExceptionUtils.wrap( e );
                }
                return 1;
            }
        };

        final AssertionStatus status = evaluateAssertion( test, Collections.<String, Object>emptyMap(), new Functions.UnaryVoid<PolicyEnforcementContext>(){
            @Override
            public void call( final PolicyEnforcementContext policyEnforcementContext ) {
                try {
                    final Object value = policyEnforcementContext.getVariable( "lqcn" );
                    if ( expectedValue == null ) {
                        assertNull( "Result should be null", null );
                    } else {
                        assertNotNull( "Result var present", value );
                        if ( expectedValue instanceof String[] ) {
                            assertTrue( "Result is array", value instanceof String[] );
                            assertArrayEquals( "Result var value", (String[])expectedValue, (String[])value );
                        } else {
                            assertEquals( "Result var value", expectedValue, value );
                        }
                    }
                } catch ( NoSuchVariableException e ) {
                    if ( expectedValue != null ) throw new RuntimeException(e);
                }
            }
        } );
        assertEquals( "Status", expectedStatus, status );
    }

    private void doTestTooManyResults( final AssertionStatus expectedStatus,
                                       final boolean failIfTooMany,
                                       final int inMaxResults ) throws Exception {
        doTestResultCount( expectedStatus, failIfTooMany, false, 1001, inMaxResults );
    }

    private void doTestSearchFilterInjectionProtection( final boolean injectionProtected,
                                                        final String filter,
                                                        final String expectedFilter,
                                                        final String variableValue ) throws Exception {
        final LDAPQueryAssertion assertion = new LDAPQueryAssertion();
        assertion.setSearchFilter( filter );
        assertion.setSearchFilterInjectionProtected( injectionProtected );

        final ServerLDAPQueryAssertion test = new  ServerLDAPQueryAssertion( assertion, beanFactory() ){
            @Override
            protected int doSearch( final String filter,
                                    final String[] attributeNames,
                                    final int maxResults,
                                    final Functions.BinaryVoidThrows<QueryAttributeMapping, ServerLDAPQueryAssertion.SimpleAttribute, Exception> resultCallback ) throws FindException {
                assertEquals( "Search filter", expectedFilter, filter );
                return 1;
            }
        };

        final Map<String,Object> variables = new HashMap<String,Object>();
        if ( variableValue != null ) {
            variables.put( "var", variableValue );
        }

        final AssertionStatus status = evaluateAssertion( test, variables, null );
        assertEquals( "Status", AssertionStatus.NONE, status );
    }

    private void doTestResultCount( final AssertionStatus expectedStatus,
                                    final boolean failIfTooMany,
                                    final boolean failIfTooFew,
                                    final int availableResults,
                                    final int inMaxResults ) throws Exception {
        final LDAPQueryAssertion assertion = new LDAPQueryAssertion();
        assertion.setSearchFilter( "(cn=*)" );
        assertion.setAllowMultipleResults( true );
        assertion.setMaximumResults( inMaxResults );
        assertion.setFailIfNoResults( failIfTooFew );
        assertion.setFailIfTooManyResults( failIfTooMany );

        final ServerLDAPQueryAssertion test = new  ServerLDAPQueryAssertion( assertion, beanFactory() ){
            @Override
            protected int doSearch( final String filter,
                                    final String[] attributeNames,
                                    final int maxResults,
                                    final Functions.BinaryVoidThrows<QueryAttributeMapping, ServerLDAPQueryAssertion.SimpleAttribute, Exception> resultCallback ) throws FindException {
                assertEquals( "Maximum results", inMaxResults, maxResults );
                return availableResults;
            }
        };

        final AssertionStatus status = evaluateAssertion( test );
        assertEquals( "Status", expectedStatus, status );
    }

    private AssertionStatus evaluateAssertion( final ServerLDAPQueryAssertion test ) throws IOException, PolicyAssertionException {
        return evaluateAssertion( test, Collections.<String, Object>emptyMap(), null );
    }

    private AssertionStatus evaluateAssertion( final ServerLDAPQueryAssertion test,
                                               final Map<String,Object> variables,
                                               final Functions.UnaryVoid<PolicyEnforcementContext> validationCallback ) throws IOException, PolicyAssertionException {
        final Message request = new Message();
        final Message response = new Message();

        AssertionStatus status;
        PolicyEnforcementContext context = null;
        try {
            context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

            for( final Map.Entry<String,Object> varEntry : variables.entrySet() ) {
                context.setVariable( varEntry.getKey(), varEntry.getValue() );
            }

            status = test.checkRequest( context );
        } catch ( final AssertionStatusException ase ) {
            status = ase.getAssertionStatus();
        } finally {
            if ( validationCallback != null ) validationCallback.call( context );
            ResourceUtils.closeQuietly( context );
        }
        return status;
    }

    private BeanFactory beanFactory() {
        final StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
        beanFactory.addBean( "identityProviderFactory", new IdentityProviderFactory( new TestIdentityProviderConfigManager() ) );
        beanFactory.addBean( "managedBackgroundTimer", new MockTimer() );
        return beanFactory;
    }
}
