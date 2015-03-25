package com.l7tech.objectmodel.polback;

import com.l7tech.objectmodel.encass.EncapsulatedAssertionArgumentDescriptor;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionResultDescriptor;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.DataTypeUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;

import static com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig.*;

/**
 * Examines annotated policy backed service interfaces, and creates template encapsulated assertion configurations
 * for each operation.
 */
public class PolicyBackedInterfaceIntrospector {

    /**
     * Create a new introspector.
     */
    public PolicyBackedInterfaceIntrospector() {
    }

    /**
     * Find a method by name, ignoring parameter types, and disallowing overloaded method names.
     *
     * @param interfaceClass interface class to examine.  Required.
     * @param methodName simple name of method.  Required.
     * @return the unique method in the specified class with the specified name.  Never null.
     * @throws java.lang.IllegalArgumentException if the method is not found, or is overloaded.
     */
    static Method getMethod( @NotNull Class<?> interfaceClass, @NotNull String methodName ) {
        Method method = null;

        Method[] methods = interfaceClass.getMethods();
        for ( Method m : methods ) {
            if ( methodName.equals( m.getName() )) {
                if ( method != null )
                    throw new IllegalArgumentException( "More than one overloaded method with name " + methodName + " in interface class " + interfaceClass.getSimpleName() );
                method = m;
            }
        }

        if ( method == null ) {
            throw new IllegalArgumentException( "No such method " + methodName + " in interface class " + interfaceClass.getSimpleName() );
        }
        return method;
    }

    /**
     * Get result descriptors for the specified method.
     *
     * @param interfaceClass interface class to examine.  Required.
     * @param methodName simple name of method.  Required.
     * @return result descriptors.  Never null or empty.
     * @throws java.lang.IllegalArgumentException if the method is not found or is overloaded, if a return type is not supported,
     *                    or if a @PolicyBackedMethod annotation is present on the method but contains an invalid result specification.
     */
    @NotNull
    EncapsulatedAssertionResultDescriptor[] getMethodResults( @NotNull Class<?> interfaceClass, @NotNull String methodName ) {
        return getMethodResults( getMethod( interfaceClass, methodName ) );
    }

    /**
     * Get result descriptors for the specified method.
     *
     * @param serviceMethod method to examine.  Required.
     * @return result descriptors.  Never null or empty.
     * @throws java.lang.IllegalArgumentException if a return type is not supported,
     *                    or if a @PolicyBackedMethod annotation is present on the method but contains an invalid result specification.
     */
    @NotNull
    EncapsulatedAssertionResultDescriptor[] getMethodResults( @NotNull Method serviceMethod ) {
        final Class<?> resultClass = serviceMethod.getReturnType();

        if ( resultClass == void.class ) {
            // Explicit lack of output
            return new EncapsulatedAssertionResultDescriptor[0];
        }

        final boolean isMap = Map.class.equals( resultClass );

        // We require the declared runtime return type to be one of our supported values, regardless of whether the type is overridden
        // by a @PolicyParam annotation.
        DataType type = DataTypeUtils.getDataTypeForClass( resultClass );
        if ( !isMap && type == null ) {
            throw new IllegalArgumentException( "Method " + serviceMethod.getName() + " has unsupported return type " + resultClass.getSimpleName() +
                    " in interface class " + serviceMethod.getDeclaringClass().getSimpleName() );
        }

        // Check for explicit name
        PolicyBackedMethod policyBackedMethod = serviceMethod.getAnnotation( PolicyBackedMethod.class );

        if ( isMap && ( policyBackedMethod == null || policyBackedMethod.mapResults().length < 1 ) )
            throw new IllegalArgumentException( "Method " + serviceMethod.getName() + " returns Map but lacks @PolicyBackedMethod with mapResults" +
                    " in interface class " + serviceMethod.getDeclaringClass().getSimpleName() );

        List<EncapsulatedAssertionResultDescriptor> ret = new ArrayList<>();

        PolicyParam[] params;
        if ( isMap ) {
            // Multiple return values
            params = policyBackedMethod.mapResults();

            for ( PolicyParam policyParam : params ) {
                String resultName = policyParam.value();
                if ( resultName.isEmpty() )
                    throw new IllegalArgumentException( "Method " + serviceMethod.getName() + " return value (type=" + resultClass.getSimpleName() +
                            ") @PolicyBackedMethod annotation specifies named return value with an empty result name " +
                            " in interface class " + serviceMethod.getDeclaringClass().getSimpleName() );

                type = maybeOverrideDataType( policyParam, null, serviceMethod );

                if ( type == null )
                    throw new IllegalArgumentException( "Method " + serviceMethod.getName() + " return value (type=" + resultClass.getSimpleName() +
                            ") @PolicyBackedMethod annotation specifies named return value without specifying data type " +
                            " in interface class " + serviceMethod.getDeclaringClass().getSimpleName() );

                final EncapsulatedAssertionResultDescriptor d = new EncapsulatedAssertionResultDescriptor();
                d.setResultName( resultName );
                d.setResultType( type.getShortName() );
                ret.add( d );
            }
        } else {
            String resultName;
            if ( policyBackedMethod == null || policyBackedMethod.singleResult().value().isEmpty() ) {
                resultName = "result";
            } else {
                resultName = policyBackedMethod.singleResult().value();
            }

            if ( policyBackedMethod != null ) {
                PolicyParam policyParam = policyBackedMethod.singleResult();
                type = maybeOverrideDataType( policyParam, type, serviceMethod );
            }

            final EncapsulatedAssertionResultDescriptor d = new EncapsulatedAssertionResultDescriptor();
            d.setResultName( resultName );
            d.setResultType( type.getShortName() );
            ret.add( d );
        }

        return ret.toArray( new EncapsulatedAssertionResultDescriptor[ret.size()] );
    }

    // return passed-in data type (unexamined), unless policyParam specifies an overridden data type name
    // throws if policyParam specifies a non-empty data type name that is not valid
    DataType maybeOverrideDataType( PolicyParam policyParam, DataType type, Method methodForLogging ) {
        final String typeName = policyParam.dataTypeName();
        if ( typeName != null && !typeName.isEmpty() ) {
            type = DataType.forName( typeName );
            if ( type == null || DataType.UNKNOWN.equals( type ) ) {
                throw new IllegalArgumentException( "Method " + methodForLogging.getName() + " return value (type=" + methodForLogging.getReturnType().getSimpleName() +
                        ") @PolicyBackedMethod annotation specifies unrecognized data type name " + typeName +
                        " in interface class " + methodForLogging.getDeclaringClass().getSimpleName() );
            }
        }
        return type;
    }

    /**
     * Get argument descriptors for the specified method.
     *
     * @param interfaceClass interface class to examine.  Required.
     * @param methodName simple name of method.  Required.
     * @return argument descriptors.  Never null or empty.
     * @throws java.lang.IllegalArgumentException if the method is not found or is overloaded, or if an argument type is not supported,
     *                  or if an argument has a missing or invalid @PolicyParam annotation.
     */
    @NotNull
    EncapsulatedAssertionArgumentDescriptor[] getMethodArguments( @NotNull Class<?> interfaceClass, @NotNull String methodName ) {
        return getMethodArguments( getMethod( interfaceClass, methodName ) );
    }

    /**
     * Get argument descriptors for the specified method.
     *
     * @param method interface method to examine.  Required.
     * @return argument descriptors.  Never null or empty.
     * @throws java.lang.IllegalArgumentException if an argument type is not supported,
     *                  or if an argument has a missing or invalid @PolicyParam annotation.
     */
    @NotNull
    EncapsulatedAssertionArgumentDescriptor[] getMethodArguments( @NotNull Method method ) {
        // TODO JAVA 8: support last resort guessing of parameter names via Method.getParameters()
        Class<?>[] argClasses = method.getParameterTypes();
        Annotation[][] argAnnotations = method.getParameterAnnotations();

        int ordinal = 1;

        List<EncapsulatedAssertionArgumentDescriptor> ret = new ArrayList<>();
        for ( int i = 0; i < argClasses.length; i++ ) {
            Class<?> argClass = argClasses[i];
            Annotation[] annotations = argAnnotations[i];

            PolicyParam policyParam = null;
            for ( Annotation annotation : annotations ) {
                if ( annotation instanceof PolicyParam ) {
                    if ( policyParam != null )
                        throw new IllegalArgumentException( "Method " + method.getName() + " parameter number " + i + " (type=" + argClass.getSimpleName() + ") has more than one @PolicyParam annotation" +
                                " in interface class " + method.getDeclaringClass().getSimpleName() );
                    policyParam = (PolicyParam) annotation;
                }
            }

            if ( policyParam == null )
                throw new IllegalArgumentException( "Method " + method.getName() + " parameter number " + i + " (type=" + argClass.getSimpleName() + ") does not have a @PolicyParam annotation" +
                        " in interface class " + method.getDeclaringClass().getSimpleName() );

            String name = policyParam.value();
            if ( name == null || name.isEmpty() )
                throw new IllegalArgumentException( "Method " + method.getName() + " parameter number " + i + " (type=" + argClass.getSimpleName() + ") @PolicyParam annotation does not specify a parameter name" +
                        " in interface class " + method.getDeclaringClass().getSimpleName() );

            // We require the declared runtime type of the parameter to be one of our supported values, regardless of whether the type is overridden
            // by the @PolicyParam annotation.
            DataType type = DataTypeUtils.getDataTypeForClass( argClass );
            if ( type == null || DataType.UNKNOWN.equals( type ) ) {
                throw new IllegalArgumentException( "Method " + method.getName() + " parameter number " + i + " (type=" + argClass.getSimpleName() + ") is not a supported argument type" +
                        " in interface class " + method.getDeclaringClass().getSimpleName() );
            }

            final String typeName = policyParam.dataTypeName();
            if ( typeName != null && !typeName.isEmpty() ) {
                type = DataType.forName( typeName );
                if ( type == null || DataType.UNKNOWN.equals( type ) ) {
                    throw new IllegalArgumentException( "Method " + method.getName() + " parameter number " + i + " (type=" + argClass.getSimpleName() +
                            ") @PolicyParam annotation specifies unrecognized data type name " + typeName +
                            " in interface class " + method.getDeclaringClass().getSimpleName() );
                }
            }

            final EncapsulatedAssertionArgumentDescriptor d = new EncapsulatedAssertionArgumentDescriptor();
            d.setArgumentName( name );
            d.setArgumentType( type.getShortName() );
            d.setOrdinal( ordinal++ );

            ret.add( d );
        }

        return ret.toArray( new EncapsulatedAssertionArgumentDescriptor[ret.size()] );
    }

    /**
     * Create a description of the specified method as an EncapsulatedAssertionConfig.
     * This requires that the interface by annotated with the @{@link PolicyBacked} annotation,
     * that any method arguments are annotated with the @{@link PolicyParam} annotaiton,
     * and (depending on the method) possibly that the method be annotated with the @{@link PolicyBackedMethod}
     * annotation.
     *
     * @param interfaceClass interface class that contains the method.  Required.
     * @param methodName name of method to examine.  Required.
     * @return a new ephemeral EncapsulatedAssertionConfig instance describing this method.
     * @throws java.lang.IllegalArgumentException if the method is not found or is overloaded,
     *                  or if an argument type is not supported,
     *                  or if an argument has a missing or invalid @PolicyParam annotation,
     *                  or if a return type is not supported,
     *                  or if a @PolicyBackedMethod annotation is present on the method but contains an invalid result specification.
     */
    EncapsulatedAssertionConfig getMethodDescription( @NotNull Class<?> interfaceClass, @NotNull String methodName ) {
        return getMethodDescription( getMethod( interfaceClass, methodName ) );
    }

    /**
     * Create a description of the specified method as an EncapsulatedAssertionConfig.
     * This requires that the interface by annotated with the @{@link PolicyBacked} annotation,
     * that any method arguments are annotated with the @{@link PolicyParam} annotaiton,
     * and (depending on the method) possibly that the method be annotated with the @{@link PolicyBackedMethod}
     * annotation.
     *
     * @param method method to examine.  Required.
     * @return a new ephemeral EncapsulatedAssertionConfig instance describing this method.
     * @throws java.lang.IllegalArgumentException if an argument type is not supported,
     *                  or if an argument has a missing or invalid @PolicyParam annotation,
     *                  or if a return type is not supported,
     *                  or if a @PolicyBackedMethod annotation is present on the method but contains an invalid result specification.
     */
    public EncapsulatedAssertionConfig getMethodDescription( @NotNull Method method ) {

        EncapsulatedAssertionConfig config = new EncapsulatedAssertionConfig();

        PolicyBackedMethod policyBackedMethod = method.getAnnotation( PolicyBackedMethod.class );

        String name = "";
        if ( policyBackedMethod != null ) {
            name = policyBackedMethod.name();
            String desc = policyBackedMethod.description();
            if ( desc != null && desc.length() > 0 )
                config.putProperty( PROP_DESCRIPTION, desc );
        }
        if ( name == null || name.length() < 1 )
            name = method.getName();
        config.setName( name );

        // TODO should we allow caller to specify a UUID?
        config.setGuid( UUID.randomUUID().toString() );
        config.putProperty( PROP_EPHEMERAL, "true" );
        config.putProperty( PROP_SERVICE_INTERFACE, method.getDeclaringClass().getName() );
        config.putProperty( PROP_SERVICE_METHOD, method.getName() );

        EncapsulatedAssertionArgumentDescriptor[] inputs = getMethodArguments( method );
        for ( EncapsulatedAssertionArgumentDescriptor input : inputs ) {
            input.setEncapsulatedAssertionConfig( config );
        }
        config.setArgumentDescriptors( new HashSet<>( Arrays.asList( inputs ) ) );

        EncapsulatedAssertionResultDescriptor[] outputs = getMethodResults( method );
        for ( EncapsulatedAssertionResultDescriptor output : outputs ) {
            output.setEncapsulatedAssertionConfig( config );
        }
        config.setResultDescriptors( new HashSet<>( Arrays.asList( outputs ) ) );

        return config;
    }

    /**
     * Get a description of the specified policy-backed interface, including one template encapsulated assertion
     * config per method.
     *
     * @param interfaceClass interface class to introspect.  Required.
     * @return an array of operation descriptions, one per method.  Never null or empty.
     */
    public EncapsulatedAssertionConfig[] getInterfaceDescription( @NotNull Class<?> interfaceClass ) {
        List<EncapsulatedAssertionConfig> ret = new ArrayList<>();

        Method[] methods = interfaceClass.getMethods();

        if ( methods.length < 1 )
            throw new IllegalArgumentException( "Interface " + interfaceClass.getSimpleName() + " contains no methods" );

        Set<String> seenMethodNames = new HashSet<>();
        for ( Method method : methods ) {
            String methodName = method.getName();
            if ( seenMethodNames.contains( methodName ) )
                throw new IllegalArgumentException( "Interface " + interfaceClass.getSimpleName() + " contains multiple overloads for method named " + methodName );
            seenMethodNames.add( methodName );

            EncapsulatedAssertionConfig config = getMethodDescription( method );
            ret.add( config );
        }

        return ret.toArray( new EncapsulatedAssertionConfig[ret.size()] );
    }
}
