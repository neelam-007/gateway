package com.l7tech.gateway.common.spring.factory.config;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContext;

/**
 * Lazy proxy factory bean.
 *
 * @author Steve Jones
 */
public class LazyProxyFactoryBean extends AbstractFactoryBean implements ApplicationContextAware {

    //- PUBLIC

    public LazyProxyFactoryBean(final String targetBeanName, final Class targetInterface) {
        this.targetBeanName = targetBeanName;
        this.targetInterface = targetInterface;
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
                new LazyInvocationHandler(applicationContext, targetBeanName, targetInterface));
    }

    //- PRIVATE

    private final String targetBeanName;
    private final Class targetInterface;
    private ApplicationContext applicationContext;

    /**
     * InvocationHandler that lazily resolves the bean reference on first use.
     */
    private static final class LazyInvocationHandler implements InvocationHandler {
        private final ApplicationContext applicationContext;
        private final String targetBeanName;
        private final Class targetInterface;
        private final AtomicReference reference = new AtomicReference();

        private LazyInvocationHandler(final ApplicationContext applicationContext,
                                      final String targetBeanName,
                                      final Class targetInterface) {
            this.applicationContext = applicationContext;
            this.targetBeanName = targetBeanName;
            this.targetInterface = targetInterface;
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Object target = reference.get();

            if ( target == null ) {
                target = applicationContext.getBean(targetBeanName, targetInterface);
                reference.set(target);
            }

            return method.invoke(target, args);
        }
    }
}
