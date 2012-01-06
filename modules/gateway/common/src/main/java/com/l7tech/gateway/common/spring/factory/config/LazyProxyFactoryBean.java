package com.l7tech.gateway.common.spring.factory.config;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;

/**
 * Lazy proxy factory bean.
 *
 * @author Steve Jones
 */
public class LazyProxyFactoryBean extends AbstractFactoryBean implements ApplicationContextAware {

    //- PUBLIC

    public LazyProxyFactoryBean(final String targetBeanName, final Class<?> targetInterface) {
        this( targetBeanName, targetInterface, false );
    }

    public LazyProxyFactoryBean(final String targetBeanName, final Class<?> targetInterface, final boolean allowEarlyLookup ) {
        this.targetBeanName = targetBeanName;
        this.targetInterface = targetInterface;
        this.allowEarlyLookup = allowEarlyLookup;
    }

    /**
     * This factory creates targetInterfaces.
     *
     * @return the targetInterface class
     */
    public Class getObjectType() {
        return targetInterface;
    }

    /**
     * Set application context.
     *
     * @param applicationContext The context to use.
     */
    public void setApplicationContext(final ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    //- PROTECTED

    /**
     * Create a new instance on each invocation, supports both singletons
     * and templates.
     */
    protected Object createInstance() throws Exception {
        return Proxy.newProxyInstance(
                LazyProxyFactoryBean.class.getClassLoader(),
                new Class[]{ targetInterface },
                new LazyInvocationHandler<Object>(applicationContext, targetBeanName, targetInterface, allowEarlyLookup));
    }

    //- PRIVATE

    private final String targetBeanName;
    private final Class<?> targetInterface;
    private final boolean allowEarlyLookup;
    private ApplicationContext applicationContext;

    /**
     * InvocationHandler that lazily resolves the bean reference on first use.
     */
    private static final class LazyInvocationHandler<T> implements InvocationHandler {
        private final ApplicationContext applicationContext;
        private final String targetBeanName;
        private final Class<? extends T> targetInterface;
        private final boolean allowEarlyLookup;
        private final AtomicReference<T> reference = new AtomicReference<T>();

        private LazyInvocationHandler(final ApplicationContext applicationContext,
                                      final String targetBeanName,
                                      final Class<? extends T> targetInterface,
                                      final boolean allowEarlyLookup ) {
            this.applicationContext = applicationContext;
            this.targetBeanName = targetBeanName;
            this.targetInterface = targetInterface;
            this.allowEarlyLookup = allowEarlyLookup;
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            T target = reference.get();

            if ( target == null ) {
                // For safety, lazily referenced beans should not be used until the
                // context is started.
                assert allowEarlyLookup || ((AbstractApplicationContext)applicationContext).isRunning();
                target = applicationContext.getBean(targetBeanName, targetInterface);
                reference.set(target);
            }

            try {
                return method.invoke(target, args);
            } catch ( InvocationTargetException e ) {
                throw e.getCause();
            }
        }
    }
}
