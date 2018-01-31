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

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.DirContext;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


/**
 * Unit tests for ldap query assertion.
 *
 * LDAP interaction is not currently tested.
 */
public class ServerLDAPQueryAssertionTest {

    public static String DEFAULT_TEST_DN = "dc=l7tech,dc=com";

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
    public void testSingleResultClassicBehaviour() throws Exception {
        doTestResults( AssertionStatus.NONE, 0, false, false, false, new String[]{ "a" }, "a", false);
        doTestResults( AssertionStatus.NONE, 0, false, false, false, new String[0], null, false);
        doTestResults( AssertionStatus.NONE, 0, true, false, true, new String[]{ "a", "b" }, "a, b", false);
        doTestResults( AssertionStatus.NONE, 0, true, false, false, new String[]{ "a", "b" }, new String[]{ "a", "b" }, false);
        doTestResults( AssertionStatus.NONE, 0, false, true, false, new String[]{ "a" }, "a", false);
        doTestResults( AssertionStatus.FALSIFIED, 0, false, true, false, new String[]{ "a", "b" }, null, false);
    }

    @Test
    public void testSingleResultIncludeEmptyAttributeBehaviour() throws Exception {
        doTestResults( AssertionStatus.NONE, 0, false, false, false, new String[]{ "a" }, "a", true);
        doTestResults( AssertionStatus.NONE, 0, false, false, false, new String[0], "", true);
        doTestResults( AssertionStatus.NONE, 0, true, false, true, new String[]{ "a", "b" }, "a, b", true);
        doTestResults( AssertionStatus.NONE, 0, true, false, false, new String[]{ "a", "b" }, new String[]{ "a", "b" }, true);
        doTestResults( AssertionStatus.NONE, 0, false, true, false, new String[]{ "a" }, "a", true);
        doTestResults( AssertionStatus.FALSIFIED, 0, false, true, false, new String[]{ "a", "b" }, null, true);
    }

    @Test
    public void testMultipleResultsClassicBehaviour() throws Exception {
        doTestResults( AssertionStatus.NONE, 1, false, false, false, new String[]{ "a" }, new String[]{ "a" }, false);
        doTestResults( AssertionStatus.NONE, 1, false, false, false, new String[0], null, false);
        doTestResults( AssertionStatus.NONE, 1, true, false, true, new String[]{ "a", "b" }, new String[]{ "a, b" }, false);
        doTestResults( AssertionStatus.NONE, 1, true, false, false, new String[]{ "a", "b" }, new String[]{ "a", "b" }, false);
        doTestResults( AssertionStatus.NONE, 1, false, true, false, new String[]{ "a" }, new String[]{ "a" }, false);

        doTestResults( AssertionStatus.NONE, 2, false, false, false, new String[]{ "a" }, new String[]{ "a", "a" }, false);
        doTestResults( AssertionStatus.NONE, 2, false, false, false, new String[0], null, false);
        doTestResults( AssertionStatus.NONE, 2, true, false, true, new String[]{ "a", "b" }, new String[]{ "a, b", "a, b" }, false);
        doTestResults( AssertionStatus.NONE, 2, true, false, false, new String[]{ "a", "b" }, new String[]{ "a", "b", "a", "b" }, false);
        doTestResults( AssertionStatus.NONE, 2, false, true, false, new String[]{ "a" }, new String[]{ "a", "a" }, false);

        doTestResults( AssertionStatus.FALSIFIED, 1, false, true, false, new String[]{ "a", "b" }, null, false);
    }

    @Test
    public void testMultipleResultIncludeEmptyAttributeBehaviour() throws Exception {
        doTestResults( AssertionStatus.NONE, 1, false, false, false, new String[]{ "a" }, new String[]{ "a" }, true);
        doTestResults( AssertionStatus.NONE, 1, false, false, false, new String[0], new String[]{ "" }, true);
        doTestResults( AssertionStatus.NONE, 1, true, false, true, new String[]{ "a", "b" }, new String[]{ "a, b" }, true);

        doTestResults( AssertionStatus.NONE, 1, true, false, false, new String[]{ "a", "b" }, new String[]{ "a", "b" }, true);
        doTestResults( AssertionStatus.NONE, 1, false, true, false, new String[]{ "a" }, new String[]{ "a" }, true);

        doTestResults( AssertionStatus.NONE, 2, false, false, false, new String[]{ "a" }, new String[]{ "a", "a" }, true);
        doTestResults( AssertionStatus.NONE, 2, false, false, false, new String[0], new String[] {"", ""}, true);
        doTestResults( AssertionStatus.NONE, 2, true, false, true, new String[]{ "a", "b" }, new String[]{ "a, b", "a, b" }, true);
        doTestResults( AssertionStatus.NONE, 2, true, false, false, new String[]{ "a", "b" }, new String[]{ "a", "b", "a", "b" }, true);
        doTestResults( AssertionStatus.NONE, 2, false, true, false, new String[]{ "a" }, new String[]{ "a", "a" }, true);

        doTestResults( AssertionStatus.FALSIFIED, 1, false, true, false, new String[]{ "a", "b" }, null, true);
    }

    @Test
    public void testMultipleMultiValueResults() throws Exception {
        doJoinMultiValueTestResults(new String[]{ "a", "" }, true, new String[] { "a" }, new String[] { null });
        doJoinMultiValueTestResults(new String[]{ "a", "" }, true, new String[] { "a" }, new String[] { "" });
        doJoinMultiValueTestResults(new String[]{ "a, b", "c" }, true, new String[] { "a", "b" }, new String[] { "c" });
        doJoinMultiValueTestResults(new String[]{ "a, b", "" }, true, new String[] { "a", "b" }, new String[] { null });
        doJoinMultiValueTestResults(new String[]{ "a, , b", "" }, true, new String[] { "a", "", "b" }, new String[] { null });
        doJoinMultiValueTestResults(new String[]{ ", b", "" }, true, new String[] { "", "b" }, new String[] { null });
    }

    @Test
    public void testIncludeEmptyAttributes() throws Exception{
        doJoinMultiValueTestResults(new String[]{ "" }, true, new String[] { null });
        doJoinMultiValueTestResults(null, false, new String[] { null });
    }

    private void doJoinMultiValueTestResults(final Object expectedValue,
                               boolean includeEmptyAttributes,
                               final String[]... results
                               ) throws Exception {
        final LDAPQueryAssertion assertion = new LDAPQueryAssertion();
        assertion.setIncludeEmptyAttributes(includeEmptyAttributes);
        assertion.setSearchFilter( "(cn=*)" );
        assertion.setDnText(DEFAULT_TEST_DN);
        assertion.setAllowMultipleResults( true );
        assertion.setQueryMappings( new QueryAttributeMapping[]{
                new QueryAttributeMapping( "cn", "lqcn", true, false, true )
        } );

        final ServerLDAPQueryAssertion test = new ServerLDAPQueryAssertion( assertion, beanFactory() ){
            @Override
            protected int doSearch( final String filter,
                                    final Map<String, Object> varMap,
                                    final String[] attributeNames,
                                    final int maxResults,
                                    final Functions.BinaryVoidThrows<QueryAttributeMapping, ServerLDAPQueryAssertion.SimpleAttribute, Exception> resultCallback ) throws FindException {
                try {
                    for (int i=0; i < results.length; i++) {

                        Attribute attribute = null;
                        if (Arrays.stream(results[i]).filter(x -> x != null).count() > 0) {
                            final int finalI = i;
                            attribute = new StubAttribute() {
                                @Override
                                public Object get(int index) throws NamingException {
                                    return results[finalI][index];
                                }

                                @Override
                                public int size() {
                                    return results[finalI].length;
                                }
                            };
                        }

                        resultCallback.call( assertion.getQueryMappings()[0], new SimpleAttribute( "cn=a", attribute ));
                    }
                } catch ( Exception e ) {
                    throw ExceptionUtils.wrap( e );
                }
                return 1;
            }
        };

        final AssertionStatus status = evaluateAssertion( test, Collections.emptyMap(), new Functions.UnaryVoid<PolicyEnforcementContext>(){
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
        assertEquals(AssertionStatus.NONE, status);
    }

    private void doTestResults(final AssertionStatus expectedStatus,
                               final int repeats, // 0 for multiple results not expected
                               final boolean multivalued,
                               final boolean failMultivalued,
                               final boolean joinMultivalued,
                               final String[] results,
                               final Object expectedValue,
                               boolean includeEmptyAttributes) throws Exception {
        final LDAPQueryAssertion assertion = new LDAPQueryAssertion();
        assertion.setIncludeEmptyAttributes(includeEmptyAttributes);
        assertion.setSearchFilter( "(cn=*)" );
        assertion.setDnText(DEFAULT_TEST_DN);
        assertion.setAllowMultipleResults( repeats > 0 );
        assertion.setQueryMappings( new QueryAttributeMapping[]{
            new QueryAttributeMapping( "cn", "lqcn", joinMultivalued, failMultivalued, multivalued )
        } );

        final ServerLDAPQueryAssertion test = new  ServerLDAPQueryAssertion( assertion, beanFactory() ){
            @Override
            protected int doSearch( final String filter,
                                    final Map<String, Object> varMap,
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
        assertion.setDnText(DEFAULT_TEST_DN);
        assertion.setSearchFilterInjectionProtected( injectionProtected );

        final ServerLDAPQueryAssertion test = new  ServerLDAPQueryAssertion( assertion, beanFactory() ){
            @Override
            protected int doSearch( final String filter,
                                    final Map<String, Object> varMap,
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
                                    final Map<String, Object> varMap,
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

    private static class StubAttribute implements Attribute {

        @Override
        public NamingEnumeration<?> getAll() throws NamingException {
            return null;
        }

        @Override
        public Object get() throws NamingException {
            return null;
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public String getID() {
            return null;
        }

        @Override
        public boolean contains(Object attrVal) {
            return false;
        }

        @Override
        public boolean add(Object attrVal) {
            return false;
        }

        @Override
        public boolean remove(Object attrval) {
            return false;
        }

        @Override
        public void clear() {

        }

        @Override
        public DirContext getAttributeSyntaxDefinition() throws NamingException {
            return null;
        }

        @Override
        public DirContext getAttributeDefinition() throws NamingException {
            return null;
        }

        @Override
        public Object clone() {
            return null;
        }

        @Override
        public boolean isOrdered() {
            return false;
        }

        @Override
        public Object get(int ix) throws NamingException {
            return null;
        }

        @Override
        public Object remove(int ix) {
            return null;
        }

        @Override
        public void add(int ix, Object attrVal) {

        }

        @Override
        public Object set(int ix, Object attrVal) {
            return null;
        }
    }
}
