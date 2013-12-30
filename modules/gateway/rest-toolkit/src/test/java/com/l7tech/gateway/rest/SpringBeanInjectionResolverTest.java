package com.l7tech.gateway.rest;


import com.l7tech.util.CollectionUtils;
import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.Injectee;
import org.glassfish.hk2.api.Unqualified;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;
import java.util.Set;

/**
 * This was created: 10/25/13 as 6:36 PM
 *
 * @author Victor Kazakov
 */
@RunWith(MockitoJUnitRunner.class)
public class SpringBeanInjectionResolverTest {

    private SpringBeanInjectionResolver factoryInjectionResolver;

    @Mock
    private ApplicationContext applicationContext;

    @Before
    public void before() {
        factoryInjectionResolver = new SpringBeanInjectionResolver(applicationContext);
    }

    @Test
    public void testResolve() {
        MyBean resourceFactoryStub = new MyBean();
        Mockito.when(applicationContext.getBean(Matchers.eq(MyBean.class))).thenReturn(resourceFactoryStub);

        InjecteeStub injectee = new InjecteeStub(MyBean.class, null, null);

        Object returnedFactoryBean = factoryInjectionResolver.resolve(injectee, null);

        Assert.assertEquals(resourceFactoryStub, returnedFactoryBean);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testResolveNoBeansFound() {
        InjecteeStub injectee = new InjecteeStub(MyBean.class, new AnnotatedElementStub("ObjectResource"), Object.class);

        Object returnedFactoryBean = factoryInjectionResolver.resolve(injectee, null);

        Assert.assertNull(returnedFactoryBean);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testResolveMultipleBeansFound() {
        MyBean resourceFactoryStub = new MyBean();
        MyBean resourceFactoryStub2 = new MyBean();
        Mockito.when(applicationContext.getBeansOfType(Matchers.eq(MyBean.class))).thenReturn(CollectionUtils.MapBuilder.<String, MyBean>builder().put("myFactory", resourceFactoryStub).put("myFactory2", resourceFactoryStub2).map());

        InjecteeStub injectee = new InjecteeStub(MyBean.class, new AnnotatedElementStub("ObjectResource"), Object.class);

        Object returnedFactoryBean = factoryInjectionResolver.resolve(injectee, null);

        Assert.assertNull(returnedFactoryBean);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testResolveNotFactoryFound() {
        InjecteeStub injectee = new InjecteeStub(Object.class, new AnnotatedElementStub("ObjectResource"), Object.class);

        Object returnedFactoryBean = factoryInjectionResolver.resolve(injectee, null);

        Assert.assertNull(returnedFactoryBean);
    }

    private class InjecteeStub implements Injectee {

        private Class requiredType;
        private AnnotatedElement parent;
        private Class injecteeClass;

        private InjecteeStub(Class requiredType, AnnotatedElement parent, Class injecteeClass) {
            this.requiredType = requiredType;
            this.parent = parent;
            this.injecteeClass = injecteeClass;
        }

        @Override
        public Type getRequiredType() {
            return requiredType;
        }

        @Override
        public Set<Annotation> getRequiredQualifiers() {
            return null;
        }

        @Override
        public int getPosition() {
            return 0;
        }

        @Override
        public Class<?> getInjecteeClass() {
            return injecteeClass;
        }

        @Override
        public AnnotatedElement getParent() {
            return parent;
        }

        @Override
        public boolean isOptional() {
            return false;
        }

        @Override
        public boolean isSelf() {
            return false;
        }

        @Override
        public Unqualified getUnqualified() {
            return null;
        }

        @Override
        public ActiveDescriptor<?> getInjecteeDescriptor() {
            return null;
        }
    }


    private class AnnotatedElementStub implements AnnotatedElement {
        private String toString;

        private AnnotatedElementStub(String toString) {
            this.toString = toString;
        }

        @Override
        public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
            return false;
        }

        @Override
        public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
            return null;
        }

        @Override
        public Annotation[] getAnnotations() {
            return new Annotation[0];
        }

        @Override
        public Annotation[] getDeclaredAnnotations() {
            return new Annotation[0];
        }

        @Override
        public String toString() {
            return toString;
        }
    }

    public class MyBean {}
}
