package com.l7tech.server.polback;

import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.ObjectNotFoundException;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.objectmodel.polback.*;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.PolicyCache;
import com.l7tech.server.policy.ServerPolicyHandle;
import com.l7tech.util.Functions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

/**
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class PolicyBackedServiceRegistryTest {

    static final long goidHiEac            = 3100L;
    static final long goidHiPolicy         = 3110L;
    static final long goidHiPbs            = 3120L;
    static final long goidHiOp             = 3130L;

    static final long goidLoCassandra      = 500100L;
    static final long goidLoCassandraGet   = 500110L;
    static final long goidLoCassandraPut   = 500120L;
    static final long goidLoCassandraQuery = 500130L;

    PolicyBackedServiceRegistry registry = new PolicyBackedServiceRegistry();

    @Mock
    PolicyCache policyCache;

    @Mock
    ServerPolicyHandle getPolicy;

    @Mock
    ServerPolicyHandle putPolicy;

    @Before
    public void init() {
        registry.policyCache = policyCache;
    }

    @Test
    public void testRegisterPolicyBackedServiceTemplate() throws Exception {
        registry.registerPolicyBackedServiceTemplate( TestFace.class );

        Set<String> templates = registry.getPolicyBackedServiceTemplates();
        assertEquals( 1, templates.size() );
        assertEquals( TestFace.class.getName(), templates.iterator().next() );
    }

    @Test
    public void testRegisterPolicyBackedServiceTemplate_notAnnotated() throws Exception {
        registry.registerPolicyBackedServiceTemplate( TestFace_notAnnotated.class );
    }

    @Test
    public void testRegisterPolicyBackedServiceTemplate_alreadyRegistered() throws Exception {
        assertTrue( registry.registerPolicyBackedServiceTemplate( TestFace.class ) );
        assertFalse( registry.registerPolicyBackedServiceTemplate( TestFace.class ) );

        Set<String> templates = registry.getPolicyBackedServiceTemplates();
        assertEquals( 1, templates.size() );
        assertEquals( TestFace.class.getName(), templates.iterator().next() );
    }

    @Test
    public void testGetOperations() throws Exception {
        registry.registerPolicyBackedServiceTemplate( TestFace.class );

        List<EncapsulatedAssertionConfig> operationTemplates = registry.getTemplateOperations( TestFace.class.getName() );
        assertEquals( 3, operationTemplates.size() );

        Set<String> names = new HashSet<>( Functions.map( operationTemplates, new Functions.Unary<String, EncapsulatedAssertionConfig>() {
            @Override
            public String call( EncapsulatedAssertionConfig eac ) {
                return eac.getName();
            }
        } ) );
        Set<String> expectedNames = new HashSet<>( Arrays.asList( "get", "put", "Query Entry Metadata" ) );

        assertEquals( expectedNames, names );
    }

    @Test( expected = ObjectNotFoundException.class )
    public void testGetOperations_noClassDef() throws Exception {
        registry.registerPolicyBackedServiceTemplate( TestFace.class );
        registry.getTemplateOperations( "com.l7tech.skunkworks.NonexistentClass" );
    }

    @Test( expected = ObjectNotFoundException.class )
    public void testGetOperations_notReg() throws Exception {
        registry.registerPolicyBackedServiceTemplate( TestFace.class );
        registry.getTemplateOperations( PolicyBackedServiceRegistryTest.class.getName() );
    }

    @Test
    public void testRegisterImplementation_nothingRegistered() throws Exception {
        PolicyBackedService pbs = newTestFaceImplementation( goidLoCassandra, goidLoCassandraGet, goidLoCassandraPut, goidLoCassandraQuery, "TestFace - Cassandra" );

        try {
            registry.registerImplementation( TestFace.class.getName(), pbs );
            fail( "expected exception not thrown" );
        } catch ( IllegalArgumentException e ) {
            // Ok
        }
    }

    @Test
    public void testRegisterImplementation_noClass() throws Exception {
        registry.registerPolicyBackedServiceTemplate( TestFace.class );

        PolicyBackedService pbs = newTestFaceImplementation( goidLoCassandra, goidLoCassandraGet, goidLoCassandraPut, goidLoCassandraQuery, "TestFace - Cassandra" );

        try {
            registry.registerImplementation( PolicyBackedServiceRegistryTest.class.getName(), pbs );
            fail( "expected exception not thrown" );
        } catch ( IllegalArgumentException e ) {
            // Ok
        }
    }

    @Test
    public void testRegisterImplementation() throws Exception {
        registry.registerPolicyBackedServiceTemplate( TestFace.class );

        PolicyBackedService pbs = newTestFaceImplementation( goidLoCassandra, goidLoCassandraGet, goidLoCassandraPut, goidLoCassandraQuery, "TestFace - Cassandra" );

        registry.registerImplementation( TestFace.class.getName(), pbs );

        try {
            registry.registerImplementation( TestFace.class.getName(), pbs );
            fail( "expected exception not thrown" );
        } catch ( IllegalArgumentException e ) {
            // Ok
        }

        PolicyBackedService pbs2 = newTestFaceImplementation( goidLoCassandra + 1, goidLoCassandraGet + 1, goidLoCassandraPut + 1, goidLoCassandraQuery + 1, "TestFace - Cassandra" );
        registry.registerImplementation( TestFace.class.getName(), pbs2 );

        try {
            PolicyBackedService pbs2_other = newTestFaceImplementation( goidLoCassandra + 1, goidLoCassandraGet + 1, goidLoCassandraPut + 1, goidLoCassandraQuery + 1, "TestFace - Cassandra" );
            registry.registerImplementation( TestFace.class.getName(), pbs2_other );
            fail( "expected exception not thrown" );
        } catch ( IllegalArgumentException e ) {
            // Ok
        }
    }

    @Test( expected = IllegalArgumentException.class )
    public void testGetImplementation_noClass() throws Exception {
        PolicyBackedService pbs = newTestFaceImplementation( goidLoCassandra, goidLoCassandraGet, goidLoCassandraPut, goidLoCassandraQuery, "TestFace - Cassandra" );
        registry.getImplementationProxy( TestFace.class, pbs.getGoid() );
    }

    @Test( expected = IllegalArgumentException.class )
    public void testGetImplementation_noImpl() throws Exception {
        registry.registerPolicyBackedServiceTemplate( TestFace.class );
        PolicyBackedService pbs = newTestFaceImplementation( goidLoCassandra, goidLoCassandraGet, goidLoCassandraPut, goidLoCassandraQuery, "TestFace - Cassandra" );
        // Note: we created the implementation, but didn't register it
        registry.getImplementationProxy( TestFace.class, pbs.getGoid() );
    }

    @Test
    public void testGetImplementation() throws Exception {
        registry.registerPolicyBackedServiceTemplate( TestFace.class );
        PolicyBackedService pbs = newTestFaceImplementation( goidLoCassandra, goidLoCassandraGet, goidLoCassandraPut, goidLoCassandraQuery, "TestFace - Cassandra" );
        registry.registerImplementation( TestFace.class.getName(), pbs );
        TestFace stub = registry.getImplementationProxy( TestFace.class, pbs.getGoid() );
        assertNotNull( stub );
    }

    @Test
    public void testInvokeImplementationMethod() throws Exception {
        registry.registerPolicyBackedServiceTemplate( TestFace.class );
        PolicyBackedService pbs = newTestFaceImplementation( goidLoCassandra, goidLoCassandraGet, goidLoCassandraPut, goidLoCassandraQuery, "TestFace - Cassandra" );
        registry.registerImplementation( TestFace.class.getName(), pbs );
        TestFace stub = registry.getImplementationProxy( TestFace.class, pbs.getGoid() );

        when( getPolicy.checkRequest( Matchers.<PolicyEnforcementContext>any() ) ).then( new Answer<AssertionStatus>() {
            @Override
            public AssertionStatus answer( InvocationOnMock invocation ) throws Throwable {
                PolicyEnforcementContext pec = (PolicyEnforcementContext) invocation.getArguments()[0];

                String key = (String) pec.getVariable( "key" );
                if ( "foo".equals( key ) ) {
                    pec.setVariable( "value", "bar" );
                }

                return AssertionStatus.NONE;
            }
        } );


        when( putPolicy.checkRequest( Matchers.<PolicyEnforcementContext>any() ) ).then( new Answer<AssertionStatus>() {
            @Override
            public AssertionStatus answer( InvocationOnMock invocation ) throws Throwable {
                PolicyEnforcementContext pec = (PolicyEnforcementContext) invocation.getArguments()[0];

                String key = (String) pec.getVariable( "key" );
                String value = (String) pec.getVariable( "value" );

                if ( "foo".equals( key ) && "bar".equals( value ) ) {
                    return AssertionStatus.NONE;
                }

                return AssertionStatus.BAD_REQUEST;
            }
        } );

        when( policyCache.getServerPolicy( new Goid( goidHiPolicy, goidLoCassandraGet ) ) ).thenReturn( getPolicy );
        when( policyCache.getServerPolicy( new Goid( goidHiPolicy, goidLoCassandraPut ) ) ).thenReturn( putPolicy );

        stub.put( "foo", "bar" );
        assertEquals( "bar", stub.get( "foo" ) );
    }

    private static PolicyBackedService newTestFaceImplementation( long goidLoService, long goidLoGet, long goidLoPut, long goidLoQuery, String baseName ) {
        return newServiceImplementation( goidLoService, baseName,
                    newOperation( goidLoGet, "get", baseName + " - get" ),
                    newOperation( goidLoPut, "put", baseName + " - put" ),
                    newOperation( goidLoQuery, "query", baseName + " - query" )
            );
    }

    private static PolicyBackedService newServiceImplementation( long goidLo, String name, PolicyBackedServiceOperation... ops ) {
        PolicyBackedService pbs = new PolicyBackedService();
        pbs.setGoid( new Goid( goidHiPbs, goidLo ) );
        pbs.setName( name );
        for ( PolicyBackedServiceOperation op : ops ) {
            op.setPolicyBackedService( pbs );
        }
        pbs.setOperations( new HashSet<>( Arrays.asList( ops ) ) );
        return pbs;
    }

    private static PolicyBackedServiceOperation newOperation( long goidLow, String methodName, String policyName ) {

        PolicyBackedServiceOperation op = new PolicyBackedServiceOperation();
        op.setGoid( new Goid( goidHiOp, goidLow ) );
        op.setName( methodName );
        op.setPolicyGoid( newPolicy( goidLow, methodName, policyName ).getGoid() );

        return op;
    }

    private static Policy newPolicy( long goidLow, String methodName, String policyName ) {
        Policy policy = new Policy( PolicyType.POLICY_BACKED_OPERATION, policyName, null, false );
        policy.setGoid( new Goid( goidHiPolicy, goidLow ) );
        policy.setInternalTag( TestFace.class.getName() );
        policy.setInternalSubTag( methodName );
        return policy;
    }

    /**
     * A test interface describing a simple key/value store service that might be policy-backed.
     */
    @PolicyBacked
    @SuppressWarnings( "UnusedDeclaration" )
    public interface TestFace {
        @PolicyBackedMethod( singleResult = @PolicyParam( "value" ))
        String get( @PolicyParam( "key" ) String key );

        void put( @PolicyParam( "key" ) String key,
                  @PolicyParam( "value" ) String value);

        @PolicyBackedMethod( name = "Query Entry Metadata",
                description = "Look up an entry along with metadata for the entry (key, date updated, and its contents)",
                mapResults = {
                @PolicyParam( value = "key", dataTypeName = "string" ),
                @PolicyParam( value = "entryDate", dataTypeName = "dateTime" ),
                @PolicyParam( value = "contents", dataTypeName = "message" )
        })
        Map query( @PolicyParam( "key" ) String key );
    }

    @PolicyBacked
    public interface TestFace_notAnnotated {
        String get( @PolicyParam( "key" ) String key );
    }

}
