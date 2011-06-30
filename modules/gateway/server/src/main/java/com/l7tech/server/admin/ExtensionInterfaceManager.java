package com.l7tech.server.admin;

import com.l7tech.policy.assertion.ExtensionInterfaceBinding;
import com.l7tech.server.policy.AssertionModuleUnregistrationEvent;
import com.l7tech.util.Either;
import com.l7tech.util.ExceptionUtils;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NotNull;
import org.springframework.aop.PointcutAdvisor;
import org.springframework.aop.framework.ReflectiveMethodInvocation;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Allows registration and use of admin interfaces added at runtime.
 */
public class ExtensionInterfaceManager implements ApplicationListener {
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Map<String, Reference<Class>> classesByName = new HashMap<String, Reference<Class>>();
    private final Map<InterfaceKey, InterfaceImpl> implsByInterfaceClassAndId = new HashMap<InterfaceKey, InterfaceImpl>();

    private final @Nullable PointcutAdvisor rbacAdvisor;
    private final @Nullable MethodInterceptor rbacAdvice;
    private final @Nullable MethodInterceptor transactionInterceptor;

    /**
     * Create an extension interface manager bean.
     *
     * @param rbacAdvice method interceptor to do RBAC security enforcement, or null to disable.
     * @param rbacAdvisor advisor whose method matcher to use to determine which methods should have the rbacAdvice applied, or null to apply rbacAdvice (if any) to every invocation attempt.
     * @param transactionManager Transaction manager to use for creating the annotation-based transaction interceptor, or null to avoid using a transaction interceptor.
     */
    public ExtensionInterfaceManager(@Nullable MethodInterceptor rbacAdvice, @Nullable PointcutAdvisor rbacAdvisor, @Nullable PlatformTransactionManager transactionManager) {
        this.rbacAdvice = rbacAdvice;
        this.rbacAdvisor = rbacAdvisor;
        this.transactionInterceptor = transactionManager == null ? null :
                new TransactionInterceptor(transactionManager, new AnnotationTransactionAttributeSource());
    }

    public <T> void registerInterface(@NotNull ExtensionInterfaceBinding<T> binding) {
        registerInterface(binding.getInterfaceClass(), binding.getInstanceIdentifier(), binding.getImplementationObject());
    }

    public <T> void registerInterface(@NotNull Class<T> interfaceClass, @Nullable String instanceIdentifier, @NotNull T implementation) {
        try {
            lock.writeLock().lock();
            if (!interfaceClass.isAssignableFrom(implementation.getClass()))
                throw new IllegalArgumentException("implementation object is not an instance of the interface class");

            Class existingClass = getClass(interfaceClass.getName());
            if (existingClass == null) {
                classesByName.put(interfaceClass.getName(), new WeakReference<Class>(interfaceClass));
            } else if (existingClass != interfaceClass) {
                // Not allowed -- if an extension interface is owned by a module classloader, it should be in a package unique to that module
                throw new IllegalArgumentException("A version of the interface " + interfaceClass.getName() + " from a different classloader is already registered");
            }

            InterfaceKey key = new InterfaceKey(interfaceClass, instanceIdentifier);
            if (implsByInterfaceClassAndId.containsKey(key))
                throw new IllegalArgumentException("A different implementation of the interface " + interfaceClass.getName() + " with instance ID \"" + instanceIdentifier + "\" was already registered.");

            // Currently we don't do any wrapping here
            implsByInterfaceClassAndId.put(key, new InterfaceImpl(key, implementation, implementation));
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean isInterfaceRegistered(@NotNull String interfaceClassname, @Nullable String instanceIdentifier) {
        try {
            lock.readLock().lock();
            Class interfaceClass = getClass(interfaceClassname);
            return interfaceClass != null && implsByInterfaceClassAndId.containsKey(new InterfaceKey(interfaceClass, instanceIdentifier));

        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Get the registered interfaces.
     *
     * @return all registered interface bindings.  The returned implementation objects are the raw objects, as they were originally registered,
     *         and not including any wrappers/interceptors Spring may have applied to process annotations like @Secured or @Transactional.
     */
    @NotNull
    public Collection<ExtensionInterfaceBinding<?>> getRegisteredInterfaces() {
        List<ExtensionInterfaceBinding<?>> ret = new ArrayList<ExtensionInterfaceBinding<?>>();
        try {
            lock.readLock().lock();
            for (InterfaceImpl impl : implsByInterfaceClassAndId.values()) {
                //noinspection unchecked
                ret.add(new ExtensionInterfaceBinding<Object>(impl.getKey().getInterfaceClass(), impl.getKey().getInstanceId(), impl.getRawImpl()));
            }
            return ret;
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean unRegisterInterface(@NotNull String interfaceClassname, @Nullable String instanceIdentifier) {
        try {
            lock.writeLock().lock();
            Class interfaceClass = getClass(interfaceClassname);
            return interfaceClass != null && implsByInterfaceClassAndId.remove(new InterfaceKey(interfaceClass, instanceIdentifier)) != null;

        } finally {
            lock.writeLock().unlock();
        }
    }

    @NotNull
    public Either<Object, Throwable> invokeExtensionMethod(@NotNull String interfaceClassname, @Nullable String targetObjectId, @NotNull String methodName, @NotNull Class[] parameterTypes, @NotNull Object[] arguments)
            throws ClassNotFoundException, NoSuchMethodException
    {
        try {
            lock.readLock().lock();
            final Class interfaceClass = getClass(interfaceClassname);
            if (interfaceClass == null)
                throw new ClassNotFoundException("No extension interface is registered with classname " + interfaceClassname);
            InterfaceImpl implHolder = implsByInterfaceClassAndId.get(new InterfaceKey(interfaceClass, targetObjectId));

            if (implHolder == null)
                throw new ClassNotFoundException("No extension interface for classname " + interfaceClassname + " is registered with instance identifier " + targetObjectId);

            Method method = interfaceClass.getMethod(methodName, parameterTypes);
            try {
                List<Object> interceptors = new ArrayList<Object>();
                if (rbacAdvice != null && (rbacAdvisor == null || rbacAdvisor.getPointcut().getMethodMatcher().matches(method, interfaceClass)))
                    interceptors.add(rbacAdvice);
                if (transactionInterceptor != null)
                    interceptors.add(transactionInterceptor);

                MethodInvocation invocation = new ReflectiveMethodInvocation(null, implHolder.getWrappedImpl(), method, arguments, interfaceClass, interceptors) {};

                Object ret = invocation.proceed();
                return Either.left(ret);
            } catch (IllegalAccessException e) {
                throw (NoSuchMethodException)new NoSuchMethodException("Method " + methodName + " not accessible: " + ExceptionUtils.getMessage(e)).initCause(e);
            } catch (InvocationTargetException e) {
                return Either.right(e.getTargetException());
            } catch (Throwable t) {
                return Either.right(t);
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (applicationEvent instanceof AssertionModuleUnregistrationEvent) {
            AssertionModuleUnregistrationEvent event = (AssertionModuleUnregistrationEvent) applicationEvent;
            unregisterAllFromClassLoader(event.getModule().getModuleClassLoader());
        }
    }

    // Caller must hold at least read lock.
    private Class getClass(String interfaceClassname) {
        final Reference<Class> ref = classesByName.get(interfaceClassname);
        return ref == null ? null : ref.get();
    }

    void unregisterAllFromClassLoader(ClassLoader classLoader) {
        try {
            lock.writeLock().lock();

            Iterator<InterfaceImpl> it = implsByInterfaceClassAndId.values().iterator();
            while (it.hasNext()) {
                InterfaceImpl next = it.next();
                if (next.getRawImpl().getClass().getClassLoader() == classLoader)
                    it.remove();
            }

        } finally {
            lock.writeLock().unlock();
        }
    }

    /** Holds a description of an interface instance -- the interface class, and a (possibly-null) instance ID. */
    private static final class InterfaceKey {
        private final @NotNull Class interfaceClass;
        private final @Nullable String instanceId;

        private InterfaceKey(@NotNull Class interfaceClass, @Nullable String instanceId) {
            this.interfaceClass = interfaceClass;
            this.instanceId = instanceId;
        }

        @NotNull
        public Class getInterfaceClass() {
            return interfaceClass;
        }

        @Nullable
        public String getInstanceId() {
            return instanceId;
        }

        @SuppressWarnings({"RedundantIfStatement"})
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof InterfaceKey)) return false;

            InterfaceKey that = (InterfaceKey) o;

            if (!interfaceClass.equals(that.interfaceClass)) return false;
            if (instanceId != null ? !instanceId.equals(that.instanceId) : that.instanceId != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = interfaceClass.hashCode();
            result = 31 * result + (instanceId != null ? instanceId.hashCode() : 0);
            return result;
        }
    }

    /** Holds a registered interface binding. */
    private static final class InterfaceImpl {
        private final @NotNull InterfaceKey key;
        private final @NotNull Object wrappedImpl;
        private final @NotNull Object rawImpl;

        private InterfaceImpl(@NotNull InterfaceKey key, @NotNull Object wrappedImpl, @NotNull Object rawImpl) {
            //noinspection ConstantConditions
            if (key == null) throw new NullPointerException("key");
            //noinspection ConstantConditions
            if (wrappedImpl == null) throw new NullPointerException("wrappedImpl");
            //noinspection ConstantConditions
            if (rawImpl == null) throw new NullPointerException("rawImpl");
            this.key = key;
            this.wrappedImpl = wrappedImpl;
            this.rawImpl = rawImpl;
        }

        @NotNull
        public InterfaceKey getKey() {
            return key;
        }

        @NotNull
        public Object getWrappedImpl() {
            return wrappedImpl;
        }

        @NotNull
        public Object getRawImpl() {
            return rawImpl;
        }
    }
}
