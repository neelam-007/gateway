package com.l7tech.objectmodel.polback;

import com.l7tech.objectmodel.encass.EncapsulatedAssertionArgumentDescriptor;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionResultDescriptor;
import com.l7tech.policy.variable.DataType;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Unit test for PolicyBackedInterfaceIntrospector.
 */
public class  PolicyBackedInterfaceIntrospectorTest {

    PolicyBackedInterfaceIntrospector introspector = new PolicyBackedInterfaceIntrospector();


    @Test()
    public void testGetMethod() throws Exception {
        Method method = PolicyBackedInterfaceIntrospector.getMethod( TestAnnotatedIface.class, "nullaryVoid" );
        assertNotNull( method );

        // This comparison is a bit silly, since both values currently were ultimately looked up by the same method anyway
        assertEquals( "Method lookup succeeds", m_nullaryVoid, method );
    }

    @Test( expected = IllegalArgumentException.class )
    public void testGetMethod_overloadedName() throws Exception {
        PolicyBackedInterfaceIntrospector.getMethod( TestAnnotatedIface.class, "overload" );
    }

    @Test( expected = IllegalArgumentException.class )
    public void testGetMethod_noSuchMethod() throws Exception {
        PolicyBackedInterfaceIntrospector.getMethod( TestAnnotatedIface.class, "bogusNonExistent" );
    }

    @Test
    public void testGetMethodResults() throws Exception {
        EncapsulatedAssertionResultDescriptor[] results = introspector.getMethodResults( m_get );
        assertNotNull( results );
        assertEquals( 1, results.length );
        EncapsulatedAssertionResultDescriptor result = results[0];
        assertNotNull( result );
        assertEquals( "Must use specified name", "value", result.getResultName() );
        assertEquals( "Must use specified data type", DataType.STRING.getShortName(), result.getResultType() );
    }

    @Test
    public void testGetMethodResults_byname() throws Exception {
        EncapsulatedAssertionResultDescriptor[] results = introspector.getMethodResults( TestAnnotatedIface.class, "get" );
        assertNotNull( results );
        assertEquals( 1, results.length );
        EncapsulatedAssertionResultDescriptor result = results[0];
        assertNotNull( result );
        assertEquals( "Must use specified name", "value", result.getResultName() );
        assertEquals( "Must use specified data type", DataType.STRING.getShortName(), result.getResultType() );
    }

    @Test
    public void testGetMethodResults_unannotated() throws Exception {
        EncapsulatedAssertionResultDescriptor[] results = introspector.getMethodResults( m_nullaryString );
        assertEquals( 1, results.length );
        EncapsulatedAssertionResultDescriptor result = results[0];
        assertEquals( "Must have default name", "result", result.getResultName() );
        assertEquals( "Must use inferred data type", DataType.STRING.getShortName(), result.getResultType() );
    }

    @Test
    public void testGetMethodResults_void() throws Exception {
        EncapsulatedAssertionResultDescriptor[] results = introspector.getMethodResults( m_nullaryVoid );
        assertEquals( 0, results.length );
    }

    @Test( expected = IllegalArgumentException.class )
    public void testGetMethodResults_weirdDataType() throws Exception {
        introspector.getMethodResults( m_nullaryWeirdDataType );
    }

    @Test
    public void testGetMethodResults_Map() throws Exception {
        EncapsulatedAssertionResultDescriptor[] results = introspector.getMethodResults( m_query );
        assertEquals( 3, results.length );

        EncapsulatedAssertionResultDescriptor result = results[0];
        assertEquals( "key", result.getResultName() );
        assertEquals( DataType.STRING.getShortName(), result.getResultType() );

        result = results[1];
        assertEquals( "entryDate", result.getResultName() );
        assertEquals( DataType.DATE_TIME.getShortName(), result.getResultType() );

        result = results[2];
        assertEquals( "contents", result.getResultName() );
        assertEquals( DataType.MESSAGE.getShortName(), result.getResultType() );
    }

    @Test( expected = IllegalArgumentException.class )
    public void testGetMethodResults_Map_MissingDataType() throws Exception {
        introspector.getMethodResults( m_multiResultMissingDataType );
    }

    @Test( expected = IllegalArgumentException.class )
    public void testGetMethodResults_Map_MissingResultName() throws Exception {
        introspector.getMethodResults( m_multiResultMissingResultName );
    }

    @Test( expected = IllegalArgumentException.class )
    public void testGetMethodResults_Map_MissingAnnotation() throws Exception {
        introspector.getMethodResults( m_multiResultMissingAnnotation );
    }

    @Test
    public void testMaybeOverrideDataType() throws Exception {
        Method method = m_query;
        PolicyBackedMethod resultNames = (PolicyBackedMethod) method.getAnnotations()[0];
        PolicyParam policyParam = resultNames.mapResults()[0];
        assertEquals( "string", policyParam.dataTypeName() );
        DataType result = introspector.maybeOverrideDataType( policyParam, DataType.BOOLEAN, method );
        assertEquals( "Data type must be overridden", DataType.STRING, result );
    }

    @Test( expected = IllegalArgumentException.class )
    public void testMaybeOverrideDataType_badDataTypeName() throws Exception {
        Method method = m_singleStringVoid_BadDataTypeName;
        PolicyParam policyParam = (PolicyParam) method.getParameterAnnotations()[0][0];
        assertEquals( "bogusInvalid", policyParam.dataTypeName() );
        introspector.maybeOverrideDataType( policyParam, DataType.STRING, method );
    }

    @Test
    public void testGetMethodArguments() throws Exception {
        EncapsulatedAssertionArgumentDescriptor[] args = introspector.getMethodArguments( m_put );
        assertEquals( 2, args.length );

        EncapsulatedAssertionArgumentDescriptor arg;
        arg = args[0];
        assertEquals( "key", arg.getArgumentName() );
        assertEquals( DataType.STRING.getShortName(), arg.getArgumentType() );

        arg = args[1];
        assertEquals( "value", arg.getArgumentName() );
        assertEquals( DataType.STRING.getShortName(), arg.getArgumentType() );
    }

    @Test
    public void testGetMethodArguments_byname() throws Exception {
        EncapsulatedAssertionArgumentDescriptor[] args = introspector.getMethodArguments( TestAnnotatedIface.class, "put" );
        assertEquals( 2, args.length );

        EncapsulatedAssertionArgumentDescriptor arg;
        arg = args[0];
        assertEquals( "key", arg.getArgumentName() );
        assertEquals( DataType.STRING.getShortName(), arg.getArgumentType() );
        assertEquals( 1, arg.getOrdinal() );

        arg = args[1];
        assertEquals( "value", arg.getArgumentName() );
        assertEquals( DataType.STRING.getShortName(), arg.getArgumentType() );
        assertEquals( 2, arg.getOrdinal() );
    }

    @Test
    public void testGetMethodArguments_singleStringVoid() throws Exception {
        EncapsulatedAssertionArgumentDescriptor[] args = introspector.getMethodArguments( m_singleStringVoid );
        assertEquals( 1, args.length );

        EncapsulatedAssertionArgumentDescriptor arg;
        arg = args[0];
        assertEquals( "s1", arg.getArgumentName() );
        assertEquals( DataType.STRING.getShortName(), arg.getArgumentType() );
        assertEquals( 1, arg.getOrdinal() );
    }

    @Test( expected = IllegalArgumentException.class )
    public void testGetMethodArguments_MissingParamAnnotation() throws Exception {
        introspector.getMethodArguments( m_singleStringVoid_MissingParamAnnotation );
    }

    @Test( expected = IllegalArgumentException.class )
    public void testGetMethodArguments_MissingParamName() throws Exception {
        introspector.getMethodArguments( m_singleStringVoid_MissingParamName );
    }

    @Test( expected = IllegalArgumentException.class )
    public void testGetMethodArguments_BadDataTypeName() throws Exception {
        introspector.getMethodArguments( m_singleStringVoid_BadDataTypeName );
    }

    @Test( expected = IllegalArgumentException.class )
    public void testGetMethodArguments_BadDataTypeRuntimeClass() throws Exception {
        introspector.getMethodArguments( m_singleStringVoid_BadDataTypeRuntimeClass );
    }

    @Test
    public void testGetMethodDescription() throws Exception {
        EncapsulatedAssertionConfig result = introspector.getMethodDescription( m_query );
        assertNotNull( result );
        assertEquals( "Query Entry Metadata", result.getName() );
        assertEquals( "Look up an entry along with metadata for the entry (key, date updated, and its contents)", result.getProperty( EncapsulatedAssertionConfig.PROP_DESCRIPTION ) );
        assertEquals( "true", result.getProperty( EncapsulatedAssertionConfig.PROP_EPHEMERAL ) );

        Set<EncapsulatedAssertionArgumentDescriptor> args = result.getArgumentDescriptors();
        assertEquals( 1, args.size() );

        Set<EncapsulatedAssertionResultDescriptor> results = result.getResultDescriptors();
        assertEquals( 3, results.size() );

    }

    @Test
    public void testGetMethodDescription_methodLookup_nameNotSpecified() throws Exception {
        EncapsulatedAssertionConfig result = introspector.getMethodDescription( TestAnnotatedIface.class, "get" );
        assertNotNull( result );
        assertEquals( "get", result.getName() );
        assertNull( result.getProperty( EncapsulatedAssertionConfig.PROP_DESCRIPTION ) );
        assertEquals( "true", result.getProperty( EncapsulatedAssertionConfig.PROP_EPHEMERAL ) );
    }

    @Test
    public void testGetInterfaceDescription() throws Exception {

    }

    // TODO java8 method literals
    private static final Method m_nullaryVoid = findMethod( "nullaryVoid" );
    private static final Method m_nullaryString = findMethod( "nullaryString" );
    private static final Method m_nullaryWeirdDataType = findMethod( "nullaryWeirdDataType" );
    private static final Method m_singleStringVoid = findMethod( "singleStringVoid" );
    private static final Method m_singleStringVoid_MissingParamAnnotation = findMethod( "singleStringVoid_MissingParamAnnotation" );
    private static final Method m_singleStringVoid_MissingParamName = findMethod( "singleStringVoid_MissingParamName" );
    private static final Method m_singleStringVoid_BadDataTypeRuntimeClass = findMethod( "singleStringVoid_BadDataTypeRuntimeClass" );
    private static final Method m_singleStringVoid_BadDataTypeName = findMethod( "singleStringVoid_BadDataTypeName" );
    private static final Method m_multiResultMissingDataType = findMethod( "multiResultMissingDataType" );
    private static final Method m_multiResultMissingResultName = findMethod( "multiResultMissingResultName" );
    private static final Method m_multiResultMissingAnnotation = findMethod( "multiResultMissingAnnotation" );
    private static final Method m_get = findMethod( "get" );
    private static final Method m_put = findMethod( "put" );
    private static final Method m_query = findMethod( "query" );


    private static Method findMethod(String name) {
        return PolicyBackedInterfaceIntrospector.getMethod( TestAnnotatedIface.class, name );
    }

    public static class WeirdDataType {}

    @PolicyBacked
    @SuppressWarnings( "UnusedDeclaration" )
    public interface TestAnnotatedIface {

        void nullaryVoid();

        String nullaryString();

        WeirdDataType nullaryWeirdDataType();

        void singleStringVoid( @PolicyParam( "s1" ) String s1 );

        void singleStringVoid_MissingParamAnnotation( String s1 );

        void singleStringVoid_MissingParamName( @PolicyParam( "" ) String s1 );

        void singleStringVoid_BadDataTypeName( @PolicyParam( value = "s1", dataTypeName = "bogusInvalid" ) String s1 );

        void singleStringVoid_BadDataTypeRuntimeClass( @PolicyParam( value = "s1", dataTypeName = "string" ) WeirdDataType s1 );

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

        @PolicyBackedMethod( mapResults = {
                @PolicyParam( value = "key", dataTypeName = "string" ),
                @PolicyParam( value = "entryDate", dataTypeName = "dateTime" ),
                @PolicyParam( value = "contents" )
        })
        Map multiResultMissingDataType( @PolicyParam( "key" ) String key );

        @PolicyBackedMethod( mapResults = {
                @PolicyParam( value = "key", dataTypeName = "string" ),
                @PolicyParam( value = "", dataTypeName = "dateTime" ),
                @PolicyParam( value = "contents", dataTypeName = "message" )
        })
        Map multiResultMissingResultName( @PolicyParam( "key" ) String key );

        Map multiResultMissingAnnotation( @PolicyParam( "key" ) String key );

    }

    @PolicyBacked
    @SuppressWarnings( "UnusedDeclaration" )
    public interface InterfaceWithOverloadedMethodName {
        @PolicyBackedMethod( singleResult = @PolicyParam( "value" ))
        String get( @PolicyParam( "key" ) String key );

        void overload( String s1 );

        void overload( int i1 );
    }

    @PolicyBacked
    public interface InterfaceWithNoMethods {
    }
}
