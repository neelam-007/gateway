package com.l7tech.server.polback;

import com.l7tech.gateway.common.audit.LoggingAudit;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.ObjectNotFoundException;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionArgumentDescriptor;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionResultDescriptor;
import com.l7tech.objectmodel.polback.BackgroundTask;
import com.l7tech.objectmodel.polback.PolicyBackedInterfaceIntrospector;
import com.l7tech.objectmodel.polback.PolicyBackedService;
import com.l7tech.objectmodel.polback.PolicyBackedServiceOperation;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.variable.DataType;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.event.system.Started;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.PolicyCache;
import com.l7tech.server.policy.ServerPolicyHandle;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.server.util.PostStartupApplicationListener;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.ResourceUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEvent;

import javax.inject.Inject;
import javax.inject.Named;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Keeps track of registered policy-backed services and policy-backed service instances,
 * and allows server code to get interface implementations backed by the policies.
 */
public class PolicyBackedServiceRegistry implements PostStartupApplicationListener, InitializingBean {
    private static final Logger logger = Logger.getLogger( PolicyBackedServiceRegistry.class.getName() );

    @Inject
    @Named( "policyCache" )
    PolicyCache policyCache;

    @Inject
    @Named( "policyBackedServiceManager" )
    PolicyBackedServiceManager policyBackedServiceManager;

    private final ReadWriteLock rwlock = new ReentrantReadWriteLock();
    private final Map<String, Template> policyBackedInterfaces = new HashMap<>(  );
    private final Map<String, Set<PolicyBackedService>> implementations = new HashMap<>(  );


    /**
     * Register a policy-backed service interface.
     * <p/>
     * It is safe to call this method on an interface that might already be registered.
     *
     * @param annotatedInterface interface class to register, annotated with @PolicyBacked.  Required.
     * @return true if registration was successful, or false if the specified interface is already registered and ready to use.
     * @throws IllegalArgumentException if the specified class lacks @PolicyBacked/@PolicyBackedMethod annotations, or if there is
     *                                  some other problem with using it as a policy-backed service interface.
     */
    public boolean registerPolicyBackedServiceTemplate( @NotNull Class<?> annotatedInterface ) {
        rwlock.writeLock().lock();
        try {
            final String className = annotatedInterface.getName();
            if ( policyBackedInterfaces.keySet().contains( className ) )
                return false;

            EncapsulatedAssertionConfig[] operations = new PolicyBackedInterfaceIntrospector().getInterfaceDescription( annotatedInterface );

            policyBackedInterfaces.put( className, new Template( annotatedInterface,  operations ) );
            return true;

        } finally {
            rwlock.writeLock().unlock();
        }
    }

    /**
     * Get the names of all interfaces that have been registered with this in-memory registry.
     *
     * @return a list of full class names of interfaces annotated with @PolicyBacked that have been registered.  May be empty but never null.
     */
    @NotNull
    public Set<String> getPolicyBackedServiceTemplates() {
        rwlock.readLock().lock();
        try {
            return new HashSet<>( policyBackedInterfaces.keySet() );
        } finally {
            rwlock.readLock().unlock();
        }
    }

    /**
     * Get a list of the interface descriptions for each operation implemented by the specified annotated interface,
     * if it has previously been registered.
     *
     * @param interfaceClassName full class name of an interface that is annotated with @PolicyBacked.  Required.
     * @return zero or more EncapsulatedAssertionConfig instances, each describing context variable Input and Output mappings for a single method of the interface.
     *         Never null.
     * @throws ObjectNotFoundException if this interface has not been registered with this in-memory registry.
     */
    @NotNull
    public List<EncapsulatedAssertionConfig> getTemplateOperations( @NotNull String interfaceClassName ) throws ObjectNotFoundException {
        rwlock.readLock().lock();
        try {
            Template template = policyBackedInterfaces.get( interfaceClassName );
            if ( null == template )
                throw new ObjectNotFoundException( "No interface registered with class name: " + interfaceClassName );

            // TODO should we make a defensive read-only copy here, even though that destroys the goids?
            return Arrays.asList( template.operations );

        } finally {
            rwlock.readLock().unlock();
        }
    }

    /**
     * Register an implementation of a policy backed service with the in-memory registry.
     *
     * @param interfaceClassName name of interface class this policy backed service will implement.  Must be full classname of an interface
     *                           annotated with @PolicyBacked.  Required.
     * @param policyBackedService a PolicyBackedService instance with a unique Goid to identify it for subsequent use.  Required.
     */
    void registerImplementation( @NotNull String interfaceClassName, @NotNull PolicyBackedService policyBackedService ) {
        rwlock.writeLock().lock();
        try {
            Template template = policyBackedInterfaces.get( interfaceClassName );
            if ( null == template )
                throw new IllegalArgumentException( "No interface registered with class name: " + interfaceClassName );

            // Validate operations
            Set<PolicyBackedServiceOperation> operations = policyBackedService.getOperations();
            for ( PolicyBackedServiceOperation operation : operations ) {
                Goid policyGoid = operation.getPolicyGoid();
                if ( policyGoid == null )
                    throw new IllegalArgumentException( "operation " + operation.getName() + " is not bound to an implementation policy" );

                // There doesn't seem to be much point to looking up the policy by goid and validating it at registration
                // time, since it will happen at runtime for each operation invocation anyway
            }

            Set<PolicyBackedService> instances = implementations.get( interfaceClassName );
            if ( instances == null ) {
                instances = new HashSet<>();
                implementations.put( interfaceClassName, instances );
            }

            for ( PolicyBackedService instance : instances ) {
                if ( Goid.equals( instance.getGoid(), policyBackedService.getGoid() ) )
                    throw new IllegalArgumentException( "An implementation of interface " + interfaceClassName + " with policy backed service id " +
                            policyBackedService.getGoid() + " is already registered" );
            }

            // TODO should make a defensive read-only copy of the policy backed service before enrolling,
            //      but must be careful as currently this destroys the pbs and operation Goids, which must then be restored
            instances.add( policyBackedService );
        } finally {
            rwlock.writeLock().unlock();
        }
    }

    /**
     * Get an implementation proxy for a single method, mapped directly to an implementing policy
     * without mapping operations using a PolicyBackedService instance.
     * <p/>
     * When the specified method is called on the returned proxy object, the policy will be invoked using
     * context variable input and output mappings defined by the method's @PolicyBackedMethod and @PolicyParam annotations.
     * <p/>
     * The Method must be a method of a registered @PolicyBacked-annotated interface class.
     * <p/>
     * Proxies produced by this method would typically only be useful for a single-method interface.
     * If any method aside from the specified method is called, the proxy returned by this method
     * will always throw UnsupportedOperationException.
     * <p/>
     * To get a proxy capable of mapping more than one method, use {@link #getImplementationProxy(Class, com.l7tech.objectmodel.Goid)}
     * with the Goid of a PolicyBackedService instance (instead of the Goid of a single backing policy).
     *
     * @param method  a Method from a previously-registered @PolicyBacked interface class.  Required.
     * @param policyGoid the Goid of a Policy (that must already exist in the policyCache) that will provide the method implementation.  Required.
     * @param <T> method's declaring class
     * @return a proxy object that will invoke the specified method by looking up the specified policy in the policy cache and executing it.
     */
    @NotNull
    public <T> T getImplementationProxyForSingleMethod( final @NotNull Method method, final @NotNull Goid policyGoid,
                                                        final @Nullable PolicyEnforcementContext pec ) {
        final Template template;

        rwlock.readLock().lock();
        try {
            String interfaceClassName = method.getDeclaringClass().getName();

            template = policyBackedInterfaces.get( interfaceClassName );
            if ( null == template )
                throw new IllegalArgumentException( "No interface registered with class name: " + interfaceClassName );

            if ( template.interfaceClass != method.getDeclaringClass() )
                throw new IllegalArgumentException( "Registered interface with name " + interfaceClassName + " is from a different class loader than requested interface class");

        } finally {
            rwlock.readLock().unlock();
        }

        final EncapsulatedAssertionConfig templateOperation = new PolicyBackedInterfaceIntrospector().getMethodDescription( method );

        //noinspection unchecked
        return (T) Proxy.newProxyInstance( template.interfaceClass.getClassLoader(), new Class<?>[]{ template.interfaceClass }, new InvocationHandler() {
            @Override
            public Object invoke( Object proxy, Method invokedMethod, Object[] args ) throws Throwable {
                if ( !invokedMethod.equals( method ) )
                    throw new UnsupportedOperationException( "Method not supported by proxy: " + invokedMethod.getName() );

                // look up encapsulated assertion by method name
                String name = method.getName();

                return invokeProxyMethod( method,
                        args,
                        policyGoid,
                        name,
                        templateOperation.getArgumentDescriptors(),
                        templateOperation.getResultDescriptors(),
                        pec);
            }
        } );
    }

    /**
     * Get an implementation proxy for a policy backed service.
     * <p/>
     * If only a single implementation of a given service interface is registered,
     * the second argument may be null.
     *
     * @param interfaceClass  a previously-registered @PolicyBacked interface class.  Required.
     * @param policyBackedServiceGoid  Goid of implementation to use.  May be omitted if only one is registered.
     * @param <T>  interface class
     * @return a proxy object that, when invoked, runs the appropriate backing policy.  Never null.
     */
    @NotNull
    public <T> T getImplementationProxy( @NotNull Class<T> interfaceClass, @Nullable Goid policyBackedServiceGoid ) {
        final Template template;

        rwlock.readLock().lock();
        PolicyBackedService ourInstance = null;
        try {
            String interfaceClassName = interfaceClass.getName();

            template = policyBackedInterfaces.get( interfaceClassName );
            if ( null == template )
                throw new IllegalArgumentException( "No interface registered with class name: " + interfaceClassName );

            if ( template.interfaceClass != interfaceClass )
                throw new IllegalArgumentException( "Registered interface with name " + interfaceClassName + " is from a different class loader than requested interface class");

            Set<PolicyBackedService> instances = implementations.get( interfaceClassName );
            if ( instances != null ) {
                for ( PolicyBackedService instance : instances ) {
                    if ( policyBackedServiceGoid == null && instance.getServiceInterfaceName().equals( interfaceClassName ) ) {
                        if ( ourInstance != null ) {
                            throw new IllegalStateException( "More than one implementation of interface " + interfaceClassName + " is registered; must specify implementation Goid" );
                        }
                        ourInstance = instance;
                        continue;
                    }

                    if ( Goid.equals( instance.getGoid(), policyBackedServiceGoid ) ) {
                        ourInstance = instance;
                        break;
                    }
                }
            }

            if ( null == ourInstance ) {
                if ( policyBackedServiceGoid == null )
                    throw new IllegalArgumentException( "No implementations of interface " + interfaceClassName + " are registered" );

                throw new IllegalArgumentException( "No implementation of interface " + interfaceClassName + " with policy backed service id " +
                        policyBackedServiceGoid + " is registered" );

            }
        } finally {
            rwlock.readLock().unlock();
        }

        // Release read lock before invoking method (since the policy might run for an arbitrarily long time)
        // The PolicyBackedService instance shouldn't ever be modified by another thread after it is registered,
        // and neither should the Template.

        final PolicyBackedService instanceImpl = ourInstance;
        //noinspection unchecked
        return (T) Proxy.newProxyInstance( interfaceClass.getClassLoader(), new Class<?>[]{ interfaceClass }, new InvocationHandler() {
            @Override
            public Object invoke( Object proxy, Method method, Object[] args ) throws Throwable {

                // look up encapsulated assertion by method name
                String name = method.getName();
                final String serviceName = instanceImpl.getName();

                PolicyBackedServiceOperation operation = null;
                Set<PolicyBackedServiceOperation> ops = instanceImpl.getOperations();
                for ( PolicyBackedServiceOperation op : ops ) {
                    if ( name.equalsIgnoreCase( op.getName() ) ) {
                        operation = op;
                        break;
                    }
                }

                if ( null == operation ) {
                    throw new UnsupportedOperationException( "No operation for method named " + name + " configured in policy-backed service " + serviceName );
                }

                // convert params into input context variables
                EncapsulatedAssertionConfig templateOperation = null;
                EncapsulatedAssertionConfig[] templateOps = template.operations;
                for ( EncapsulatedAssertionConfig templateOp : templateOps ) {
                    if ( name.equalsIgnoreCase( templateOp.getName() ) ) {
                        templateOperation = templateOp;
                        break;
                    }
                }

                if ( null == templateOperation ) {
                    throw new UnsupportedOperationException( "No operation named " + name + " configured in template for policy-backed service " + serviceName );
                }

                return invokeProxyMethod( method,
                        args,
                        operation.getPolicyGoid(),
                        operation.getName(),
                        templateOperation.getArgumentDescriptors(),
                        templateOperation.getResultDescriptors(),
                        null);
            }
        } );
    }

    /**
     * Invoke a method whose implementation will be a policy, creating a new policy enforcement context,
     * translating the method arguments into input context variables according to the inputs mapping,
     * executing the policy, then translating the output context variables into the return value according
     * to the outputs mapping.
     *
     * @param method the Method that is being invoked, from a registered @PolicyBacked interface.  Required.
     * @param args the method arguments.
     * @param policyGoid the Goid of the policy that will be executed to handle the method invocation.  Required.
     * @param operationName the user-meaningful operation name, for logging purposes.  Required.
     * @param inputs the input mappings.  Required, but may be empty.
     * @param outputs the output mappings.  Required, but may be empty.
     * @param policyEnforcementContext the parent policy enforcement context to use
     * @return the method return value, translated according to the output mappings from variables left behind
     *         in the policy enforcement context.
     */
    private Object invokeProxyMethod( @NotNull Method method,
                                      @Nullable Object[] args,
                                      @NotNull Goid policyGoid,
                                      @NotNull String operationName,
                                      @NotNull Set<EncapsulatedAssertionArgumentDescriptor> inputs,
                                      @NotNull Set<EncapsulatedAssertionResultDescriptor> outputs,
                                      @Nullable PolicyEnforcementContext policyEnforcementContext)
    {
        Map<String, Object> contextVariableInputs = new TreeMap<>( String.CASE_INSENSITIVE_ORDER );
        for ( EncapsulatedAssertionArgumentDescriptor argDescriptor : inputs ) {
            final String argumentName = argDescriptor.getArgumentName();

            int argNumber = argDescriptor.getOrdinal() - 1;
            Object argValue = args[ argNumber ];
            Class argClass = method.getParameterTypes()[ argNumber ];

            Object contextVarValue = toContextVariableValue( argClass, argValue, argDescriptor.getArgumentType() );

            contextVariableInputs.put( argumentName, contextVarValue );
        }

        // invoke encapsulated assertion
        PolicyEnforcementContext context = null;
        try {

            if(policyEnforcementContext!=null) {
                context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(policyEnforcementContext);
            }else{
                context = PolicyEnforcementContextFactory.createPolicyEnforcementContext( new Message(), new Message() );
            }

            invokePolicyBackedOperation(context, policyGoid, operationName, contextVariableInputs);

            // convert output context variables into return value (possibly multivalued Map)

            List<String> resultNames = new ArrayList<>();
            for ( EncapsulatedAssertionResultDescriptor resultDescriptor : outputs ) {
                String resultName = resultDescriptor.getResultName();
                resultNames.add( resultName );
            }
            Map<String, Object> resultsMap = context.getVariableMap( resultNames.toArray( new String[resultNames.size()] ), new LoggingAudit( logger ) );

            if ( outputs.size() == 0 ) {
                // Void method
                return null;
            } else if ( outputs.size() == 1 ) {
                // Single return value
                EncapsulatedAssertionResultDescriptor descriptor = outputs.iterator().next();
                return fromContextVariableValue( resultsMap.get( descriptor.getResultName() ), descriptor.getResultType(), method.getReturnType() );
            } else {
                // Return as Map
                return resultsMap;
            }

        } finally {
            ResourceUtils.closeQuietly( context );
        }
    }

    /**
     * Perform any data conversion from the data types used by the policy language back to the Java data types.
     * <p/>
     * For now this method does nothing.  Any un-coerceable type changes will fail inside the Proxy.
     *
     * @param policyValue value from context variable.  May be null.
     * @param policyDataType Policy data type.  Required.
     * @param returnType data type expected for method return.  Required.
     * @return the value the method return should use.  May be null.
     */
    @Nullable
    private Object fromContextVariableValue( @Nullable Object policyValue, @NotNull String policyDataType, @NotNull Class<?> returnType ) {
        // For now we won't attempt to do any type conversions, we will simply assert that the type is correct
        // We will let Proxy handle the type error if any
        return policyValue;
    }

    /**
     * Convert a Java value into a context variable value.
     *
     * @param argClass the Java argument type from the interface.  Required.
     * @param argValue the argument value as passed in from Java.  May be null.
     * @param argumentType the desired Policy context variable type.  Required.
     * @return the value to use for the input context variable corresponding to this argument.  May be null.
     */
    @Nullable
    private Object toContextVariableValue( @NotNull Class<?> argClass, @Nullable Object argValue, @NotNull String argumentType ) {
        // For now we won't attempt to do any type conversions, we will simply assert that the type is correct
        DataType dataType = DataType.forName( argumentType );
        if ( null == dataType || DataType.UNKNOWN.equals( dataType ) ) {
            throw new IllegalArgumentException( "Unrecognized data type name: " + argumentType );
        }

        if ( argValue != null && !argClass.isAssignableFrom( argValue.getClass() ) )
            throw new IllegalArgumentException( "Actual argument of type " + argValue.getClass() + " is not assignable to formal parameter type " + argClass );

        Class<?>[] valueClasses = dataType.getValueClasses();
        for ( Class<?> valueClass : valueClasses ) {
            if ( valueClass.isAssignableFrom( argClass ) ) {
                // Type is acceptable
                return argValue;
            }
        }

        if ( argValue == null )
            throw new IllegalArgumentException( "null value for formal parameter type " + argClass + " is not a valid representation of policy data type " + argumentType );

        throw new IllegalArgumentException( "Actual argument of type " + argValue.getClass() + " is not a valid representation of policy data type " + argumentType );
    }


    // Look up a server policy handle, set variables in context, and call checkRequest.
    private void invokePolicyBackedOperation( @NotNull PolicyEnforcementContext context,
                                              @NotNull Goid policyGoid,
                                              @NotNull String operationName,
                                              @NotNull Map<String, Object> contextVariableInputs )
    {
        try (ServerPolicyHandle sph = policyCache.getServerPolicy( policyGoid )) {
            if ( sph == null ) {
                throw new UnsupportedOperationException(
                        MessageFormat.format( "Unable to invoke policy backed service operation {0} -- no policy with ID {1} is present in policy cache (invalid policy?)",
                        new Object[] { operationName, policyGoid } ) );
            }

            for ( Map.Entry<String, Object> entry : contextVariableInputs.entrySet() ) {
                context.setVariable( entry.getKey(), entry.getValue() );
            }

            AssertionStatus status = sph.checkRequest( context );

            if ( !AssertionStatus.NONE.equals( status ) ) {
                throw new AssertionStatusException( status, "Backing policy for policy-backed operation " + operationName + " failed with assertion status " + status );
            }

        } catch ( RuntimeException e) {
            throw e;
        } catch ( Exception e ) {
            throw new RuntimeException( "Exception in policy backed service operation " + operationName + ": " + ExceptionUtils.getMessage(e), e );
        }
    }

    @Override
    public void onApplicationEvent( ApplicationEvent event ) {
        if ( event instanceof EntityInvalidationEvent ) {
            EntityInvalidationEvent eie = (EntityInvalidationEvent) event;
            if ( PolicyBackedService.class == eie.getEntityClass() ) {
                // TODO figure out which service was changed and (de)(re)register it (for D/U/C)
                flushAndReRegisterAllImplementations();
            }
        } else if ( event instanceof Started ) {
            flushAndReRegisterAllImplementations();
        }
    }

    // Forget all currently in-memory registered PolicyBackedService interfaces and replace them with an up-to-date list
    // loaded from the database.
    private void flushAndReRegisterAllImplementations() {
        final Collection<PolicyBackedService> serviceImplementations;
        try {
            serviceImplementations = policyBackedServiceManager.findAll();
        } catch ( FindException e ) {
            logger.log( Level.SEVERE, "Unable to load policy backed services: " + ExceptionUtils.getMessage(e), e );
            // TODO is there anything else we can/should do here?
            return;
        }

        rwlock.writeLock().lock();
        try {
            implementations.clear();
            for ( PolicyBackedService pbs : serviceImplementations ) {
                String serviceInterfaceName = pbs.getServiceInterfaceName();

                final Template template = policyBackedInterfaces.get( serviceInterfaceName );
                if ( template == null ) {
                    logger.log( Level.INFO, "Ignoring policy-backed service " + pbs.getName() +
                            " (id=" + pbs.getGoid() + ") because no template interface is registered for its interface class " + serviceInterfaceName );
                } else {
                    try {
                        registerImplementation( serviceInterfaceName, pbs );
                    } catch ( Exception e ) {
                        logger.log( Level.WARNING, "Error registering policy-backed service " + pbs.getName() + " (id=" + pbs.getGoid() + ") " +
                                    " as implementation of " + serviceInterfaceName + ": " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException( e ) );
                    }
                }
            }
        } finally {
            rwlock.writeLock().unlock();
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        preregisterWellKnownInterfaces();
    }

    private void preregisterWellKnownInterfaces() {
        // TODO find some better way/place to do this

        // BackgroundTask - Used by work queues and scheduled tasks
        registerPolicyBackedServiceTemplate( BackgroundTask.class );

        // KeyValueStore - Used by add-on modules
        // Not used by any core code yet; currently will be registered as needed by extensions that need to use it
        // At some point will come preregistered but not yet needed.
        //registerPolicyBackedServiceTemplate( KeyValueStore.class );
    }

    static class Template {
        private final @NotNull Class<?> interfaceClass;
        private final @NotNull EncapsulatedAssertionConfig[] operations;

        Template( @NotNull Class<?> interfaceClass, @NotNull EncapsulatedAssertionConfig[] operations ) {
            this.interfaceClass = interfaceClass;
            this.operations = operations;
        }
    }
}
