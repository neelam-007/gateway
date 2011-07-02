package com.l7tech.server;

import com.l7tech.server.util.Injector;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.Map;

/**
 * A bag of application contexts
 * @author emil
 * @version Dec 13, 2004
 */
public class ApplicationContexts {
    static ClassPathXmlApplicationContext testApplicationContext;
    static ClassPathXmlApplicationContext prodApplicationContext;

    public static synchronized ApplicationContext getTestApplicationContext() {
        if (testApplicationContext !=null) {
            return testApplicationContext;
        }
        testApplicationContext = createTestApplicationContext();
        return testApplicationContext;
    }

    public static ApplicationContext getProdApplicationContext() {
            if (prodApplicationContext !=null) {
            return prodApplicationContext;
        }
        prodApplicationContext = createProdApplicationContext();
        return prodApplicationContext;

    }

    private static ClassPathXmlApplicationContext createProdApplicationContext() {
        return new ClassPathXmlApplicationContext(PRODUCTION_BEAN_DEFINITIONS);
    }

    private static ClassPathXmlApplicationContext createTestApplicationContext() {
        return new ClassPathXmlApplicationContext(new String[]{DEFAULT_TEST_BEAN_DEFINITIONS});
    }
    public static final String DEFAULT_TEST_BEAN_DEFINITIONS = "com/l7tech/server/resources/testApplicationContext.xml";

    public static final String[] PRODUCTION_BEAN_DEFINITIONS = {
            "com/l7tech/server/resources/dataAccessContext.xml",
            "com/l7tech/server/resources/ssgApplicationContext.xml",
            "com/l7tech/server/resources/adminContext.xml"
    };

    /**
     * Inject dependencies into the given bean.
     *
     * <p>The primary usage is for injecting server assertions, but this can be
     * used with any object.</p>
     *
     * @param bean The object to inject.
     * @param beans The injectable dependencies
     * @param <B> The bean type
     * @param <T> Any type, typically Object
     */
    public static <B,T> B inject( final B bean,
                                  final Map<String,T> beans ) {
        final DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        final AutowiredAnnotationBeanPostProcessor autowiredAnnotationBeanPostProcessor = new AutowiredAnnotationBeanPostProcessor();
        autowiredAnnotationBeanPostProcessor.setBeanFactory( factory );
        factory.addBeanPostProcessor( autowiredAnnotationBeanPostProcessor );
        final Injector injector = new Injector() {
            @Override
            public void inject( final Object target ) {
                factory.autowireBeanProperties( bean, AutowireCapableBeanFactory.AUTOWIRE_NO, true );
            }
        };
        factory.registerSingleton( "injector", injector );
        CollectionUtils.foreach( beans.entrySet(), false, new Functions.UnaryVoid<Map.Entry<String,T>>(){
            @Override
            public void call( final Map.Entry<String, T> nameAndBean ) {
                factory.registerSingleton( nameAndBean.getKey(), nameAndBean.getValue() );
            }
        } );
        injector.inject( bean );
        return bean;
    }
}