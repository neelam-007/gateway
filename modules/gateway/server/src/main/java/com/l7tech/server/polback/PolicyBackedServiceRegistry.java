package com.l7tech.server.polback;

import com.l7tech.gateway.common.audit.LoggingAudit;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.ObjectNotFoundException;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionArgumentDescriptor;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionResultDescriptor;
import com.l7tech.objectmodel.polback.KeyValueStore;
import com.l7tech.objectmodel.polback.PolicyBackedInterfaceIntrospector;
import com.l7tech.objectmodel.polback.PolicyBackedService;
import com.l7tech.objectmodel.polback.PolicyBackedServiceOperation;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.variable.DataType;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.event.system.Started;
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
public class PolicyBackedServiceRegistry implements InitializingBean, PostStartupApplicationListener {
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


    public void registerPolicyBackedServiceTemplate( @NotNull Class<?> annotatedInterface ) {
        rwlock.writeLock().lock();
        try {
            final String className = annotatedInterface.getName();
            if ( policyBackedInterfaces.keySet().contains( className ) )
                throw new IllegalStateException( "Interface already registered with class name: " + className );

            EncapsulatedAssertionConfig[] operations = new PolicyBackedInterfaceIntrospector().getInterfaceDescription( annotatedInterface );

            policyBackedInterfaces.put( className, new Template( annotatedInterface,  operations ) );
        } finally {
            rwlock.writeLock().unlock();
        }
    }

    @NotNull
    public Set<String> getPolicyBackedServiceTemplates() {
        rwlock.readLock().lock();
        try {
            return new HashSet<>( policyBackedInterfaces.keySet() );
        } finally {
            rwlock.readLock().unlock();
        }
    }

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

    public void registerImplementation( @NotNull String interfaceClassName, @NotNull PolicyBackedService policyBackedService ) {
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
     * Get an implementation proxy for a policy backed service.
     * <p/>
     * If only a single implementation of a given service interface is registered,
     * the second argument may be null.
     *
     * @param interfaceClass  annotated interface class.  Required.
     * @param implementationPolicyBackedServiceGoid  Goid of implementation to use.  May be omitted if only one is registered.
     * @param <T>  interface class
     * @return a proxy object that, when invoked, runs the appropriate backing policy.  Never null.
     */
    @NotNull
    public <T> T getImplementationProxy( @NotNull Class<T> interfaceClass, @Nullable Goid implementationPolicyBackedServiceGoid ) {
        // TODO see if we really need to hold the read lock all the while through running the policy, which could take an arbitrarily long time
        // Can probably release earlier if info in Template and LiveInstance are never modified (or if defensively copied)
        rwlock.readLock().lock();
        try {
            String interfaceClassName = interfaceClass.getName();

            final Template template = policyBackedInterfaces.get( interfaceClassName );
            if ( null == template )
                throw new IllegalArgumentException( "No interface registered with class name: " + interfaceClassName );

            if ( template.interfaceClass != interfaceClass )
                throw new IllegalArgumentException( "Registered interface with name " + interfaceClassName + " is from a different class loader than requested interface class");

            PolicyBackedService ourInstance = null;
            Set<PolicyBackedService> instances = implementations.get( interfaceClassName );
            if ( instances != null ) {
                for ( PolicyBackedService instance : instances ) {
                    if ( implementationPolicyBackedServiceGoid == null && instance.getServiceInterfaceName().equals( interfaceClassName ) ) {
                        if ( ourInstance != null ) {
                            throw new IllegalStateException( "More than one implementation of interface " + interfaceClassName + " is registered; must specify implementation Goid" );
                        }
                        ourInstance = instance;
                        continue;
                    }

                    if ( Goid.equals( instance.getGoid(), implementationPolicyBackedServiceGoid ) ) {
                        ourInstance = instance;
                        break;
                    }
                }
            }

            if ( null == ourInstance ) {
                if ( implementationPolicyBackedServiceGoid == null )
                    throw new IllegalArgumentException( "No implementations of interface " + interfaceClassName + " are registered" );

                throw new IllegalArgumentException( "No implementation of interface " + interfaceClassName + " with policy backed service id " +
                        implementationPolicyBackedServiceGoid + " is registered" );

            }

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

                    Map<String, Object> contextVariableInputs = new TreeMap<>( String.CASE_INSENSITIVE_ORDER );
                    Set<EncapsulatedAssertionArgumentDescriptor> argDescriptors = templateOperation.getArgumentDescriptors();
                    for ( EncapsulatedAssertionArgumentDescriptor argDescriptor : argDescriptors ) {
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
                        context = PolicyEnforcementContextFactory.createPolicyEnforcementContext( new Message(), new Message() );

                        invokePolicyBackedOperation( context, operation, contextVariableInputs );

                        // convert output context variables into return value (possibly multivalued Map)

                        List<String> resultNames = new ArrayList<>();
                        final Set<EncapsulatedAssertionResultDescriptor> resultDescriptors = templateOperation.getResultDescriptors();
                        for ( EncapsulatedAssertionResultDescriptor resultDescriptor : resultDescriptors ) {
                            String resultName = resultDescriptor.getResultName();
                            resultNames.add( resultName );
                        }
                        Map<String, Object> resultsMap = context.getVariableMap( resultNames.toArray( new String[resultNames.size()] ), new LoggingAudit( logger ) );

                        if ( resultDescriptors.size() == 0 ) {
                            // Void method
                            return null;
                        } else if ( resultDescriptors.size() == 1 ) {
                            // Single return value
                            EncapsulatedAssertionResultDescriptor descriptor = resultDescriptors.iterator().next();
                            return fromContextVariableValue( resultsMap.get( descriptor.getResultName() ), descriptor.getResultType(), method.getReturnType() );
                        } else {
                            // Return as Map
                            return resultsMap;
                        }

                    } finally {
                        ResourceUtils.closeQuietly( context );
                    }
                }
            } );

        } finally {
            rwlock.readLock().unlock();
        }
    }

    @Nullable
    private Object fromContextVariableValue( @Nullable Object policyValue, @NotNull String policyDataType, @NotNull Class<?> returnType ) {
        // For now we won't attempt to do any type conversions, we will simply assert that the type is correct
        // We will let Proxy handle the type error if any
        return policyValue;
    }

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

    private void invokePolicyBackedOperation( @NotNull PolicyEnforcementContext context,
                                                         @NotNull PolicyBackedServiceOperation operation,
                                                         @NotNull Map<String, Object> contextVariableInputs )
    {
        final Goid policyGoid = operation.getPolicyGoid();
        try (ServerPolicyHandle sph = policyCache.getServerPolicy( policyGoid )) {
            if ( sph == null ) {
                throw new UnsupportedOperationException(
                        MessageFormat.format( "Unable to invoke policy backed service operation {0} -- no policy with ID {1} is present in policy cache (invalid policy?)",
                        new Object[] { operation.getName(), policyGoid } ) );
            }

            for ( Map.Entry<String, Object> entry : contextVariableInputs.entrySet() ) {
                context.setVariable( entry.getKey(), entry.getValue() );
            }

            AssertionStatus status = sph.checkRequest( context );

            if ( !AssertionStatus.NONE.equals( status ) ) {
                throw new AssertionStatusException( status, "Backing policy for policy-backed operation " + operation.getName() + " failed with assertion status " + status );
            }

        } catch ( RuntimeException e) {
            throw e;
        } catch ( Exception e ) {
            throw new RuntimeException( "Exception in policy backed service operation " + operation.getName() + ": " + ExceptionUtils.getMessage(e), e );
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
        // Register well-known core service interfaces (TODO move this somewhere that makes more sense)
        registerPolicyBackedServiceTemplate( KeyValueStore.class );
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
