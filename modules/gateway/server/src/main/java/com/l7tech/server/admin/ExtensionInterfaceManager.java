package com.l7tech.server.admin;

import com.l7tech.policy.assertion.ExtensionInterfaceBinding;
import com.l7tech.server.policy.AssertionModuleUnregistrationEvent;
import com.l7tech.util.Either;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Pair;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.jetbrains.annotations.Nullable;
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
    private final Map<Pair<Class, String>, Object> implsByInterfaceClassAndId = new HashMap<Pair<Class, String>, Object>();

    private final PointcutAdvisor rbacAdvisor;
    private final MethodInterceptor rbacAdvice;
    private final MethodInterceptor transactionInterceptor;

    public ExtensionInterfaceManager(MethodInterceptor rbacAdvice, PointcutAdvisor rbacAdvisor, PlatformTransactionManager transactionManager) {
        this.rbacAdvice = rbacAdvice;
        this.rbacAdvisor = rbacAdvisor;
        if (rbacAdvisor == null && rbacAdvice != null)
            throw new IllegalArgumentException("rbacAdvisor must be provided if rbacAdvice is specified");
        this.transactionInterceptor = transactionManager == null ? null :
                new TransactionInterceptor(transactionManager, new AnnotationTransactionAttributeSource());
    }

    public <T> void registerInterface(ExtensionInterfaceBinding<T> binding) {
        registerInterface(binding.getInterfaceClass(), binding.getInstanceIdentifier(), binding.getImplementationObject());
    }

    public <T> void registerInterface(Class<T> interfaceClass, @Nullable String instanceIdentifier, T implementation) {
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

            Pair<Class, String> key = new Pair<Class, String>(interfaceClass, instanceIdentifier);
            if (implsByInterfaceClassAndId.containsKey(key))
                throw new IllegalArgumentException("A different implementation of the interface " + interfaceClass.getName() + " with instance ID \"" + instanceIdentifier + "\" was already registered.");

            implsByInterfaceClassAndId.put(key, implementation);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean isInterfaceRegistered(String interfaceClassname, @Nullable String instanceIdentifier) {
        try {
            lock.readLock().lock();
            Class interfaceClass = getClass(interfaceClassname);
            return interfaceClass != null && implsByInterfaceClassAndId.containsKey(new Pair<Class, String>(interfaceClass, instanceIdentifier));

        } finally {
            lock.readLock().unlock();
        }
    }

    public Collection<ExtensionInterfaceBinding<?>> getRegisteredInterfaces() {
        List<ExtensionInterfaceBinding<?>> ret = new ArrayList<ExtensionInterfaceBinding<?>>();
        try {
            lock.readLock().lock();
            for (Map.Entry<Pair<Class, String>, Object> entry : implsByInterfaceClassAndId.entrySet()) {
                //noinspection unchecked
                ret.add(new ExtensionInterfaceBinding<Object>(entry.getKey().left, entry.getKey().right, entry.getValue()));
            }
            return ret;
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean unRegisterInterface(String interfaceClassname, @Nullable String instanceIdentifier) {
        try {
            lock.writeLock().lock();
            Class interfaceClass = getClass(interfaceClassname);
            return interfaceClass != null && implsByInterfaceClassAndId.remove(new Pair<Class, String>(interfaceClass, instanceIdentifier)) != null;

        } finally {
            lock.writeLock().unlock();
        }
    }

    public Either<Object, Throwable> invokeExtensionMethod(String interfaceClassname, String targetObjectId, String methodName, Class[] parameterTypes, Object[] arguments)
            throws ClassNotFoundException, NoSuchMethodException
    {
        try {
            lock.readLock().lock();
            final Class interfaceClass = getClass(interfaceClassname);
            if (interfaceClass == null)
                throw new ClassNotFoundException("No extension interface is registered with classname " + interfaceClassname);
            final Object impl = implsByInterfaceClassAndId.get(new Pair<Class, String>(interfaceClass, targetObjectId));

            if (impl == null)
                throw new ClassNotFoundException("No extension interface for classname " + interfaceClassname + " is registered with instance identifier " + targetObjectId);

            Method method = interfaceClass.getMethod(methodName, parameterTypes);
            try {
                List<Object> interceptors = new ArrayList<Object>();
                if (rbacAdvice != null && rbacAdvisor.getPointcut().getMethodMatcher().matches(method, interfaceClass))
                    interceptors.add(rbacAdvice);
                if (transactionInterceptor != null)
                    interceptors.add(transactionInterceptor);

                MethodInvocation invocation = new ReflectiveMethodInvocation(null, impl, method, arguments, interfaceClass, interceptors) {};

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
            Iterator<Object> it = implsByInterfaceClassAndId.values().iterator();
            while (it.hasNext()) {
                Object impl = it.next();
                if (impl.getClass().getClassLoader().equals(classLoader)) {
                    it.remove();
                }
            }

        } finally {
            lock.writeLock().unlock();
        }
    }
}

