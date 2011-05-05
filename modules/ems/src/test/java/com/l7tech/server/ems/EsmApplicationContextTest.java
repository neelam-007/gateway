package com.l7tech.server.ems;

import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.server.ApplicationContextTest;
import com.l7tech.util.BeanUtils;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;
import org.junit.Test;
import org.junit.Assert;

import java.beans.PropertyDescriptor;
import java.util.*;
import java.lang.reflect.Method;

/**
 * Basic test for ESM spring application context.
 *
 * @author Steve Jones
 */
public class EsmApplicationContextTest {

    private static final String[] CONTEXTS = {
        "com/l7tech/server/ems/resources/esmApplicationContext.xml",
        "com/l7tech/server/ems/resources/webApplicationContext.xml"
    };

    /**
     * Loading the definitions in this way will check the syntax and that all the
     * bean classes exist.
     */
    @Test
    public void testApplicationContextSyntaxAndBeanClasses() {
        //
        DefaultListableBeanFactory dlbf = new DefaultListableBeanFactory();
        XmlBeanDefinitionReader xbdr = new XmlBeanDefinitionReader(dlbf);

        for (String context : CONTEXTS) {
            xbdr.loadBeanDefinitions(new ClassPathResource(context));
        }
    }

    /**
     * Ensure that bean references are valid (constructor args and properties)
     */
    @SuppressWarnings({"unchecked"})
    @Test
    public void testApplicationContextReferences() {
        //
        DefaultListableBeanFactory dlbf = new DefaultListableBeanFactory();
        XmlBeanDefinitionReader xbdr = new XmlBeanDefinitionReader(dlbf);

        for (String context : CONTEXTS) {
            xbdr.loadBeanDefinitions(new ClassPathResource(context));
        }

        for (String beanId : dlbf.getBeanDefinitionNames()) {
            BeanDefinition bd = dlbf.getBeanDefinition(beanId);

            final String className = bd.getBeanClassName();
            Class beanClass = null;
            if ( className != null ) {
                try {
                    final Class<?> clazz = Class.forName( className );
                    if ( !FactoryBean.class.isAssignableFrom( clazz ) && className.startsWith( "com.l7tech" ) ) {
                        beanClass = clazz;
                    }
                } catch ( ClassNotFoundException e ) {
                    Assert.fail("Class not found for bean '"+beanId+"': " + className);
                }
            }

            ConstructorArgumentValues cav = bd.getConstructorArgumentValues();
            if (cav != null) {
                List<ConstructorArgumentValues.ValueHolder> constructorArgs = new ArrayList<ConstructorArgumentValues.ValueHolder>();
                constructorArgs.addAll( cav.getGenericArgumentValues() );
                constructorArgs.addAll( cav.getIndexedArgumentValues().values() );
                for (ConstructorArgumentValues.ValueHolder holder : constructorArgs) {
                    Object value = holder.getValue();
                    if (value instanceof RuntimeBeanReference) {
                        RuntimeBeanReference rbr = (RuntimeBeanReference) value;
                        String name = rbr.getBeanName();
                        Assert.assertTrue("Application context uses bean with name '"+name+"', but no such bean exists (referenced from bean '"+beanId+"' as constructor-arg) ",
                                   dlbf.containsBean(name));
                    }
                }

                ApplicationContextTest.checkBeanConstructor( beanId, bd, cav, constructorArgs );
            }

            MutablePropertyValues mpv = bd.getPropertyValues();
            if (mpv != null) {
                final Set<PropertyDescriptor> descriptors = beanClass==null ?
                        Collections.<PropertyDescriptor>emptySet() :
                        BeanUtils.getProperties( beanClass, false, true );

                for (PropertyValue propValue : mpv.getPropertyValues()) {
                    Object value = propValue.getValue();
                    if (value instanceof RuntimeBeanReference) {
                        RuntimeBeanReference rbr = (RuntimeBeanReference) value;
                        String name = rbr.getBeanName();
                        Assert.assertTrue("Application context uses bean with name '"+name+"', but no such bean exists (referenced from bean '"+beanId+"' as property) ",
                                   dlbf.containsBean(name));
                    }

                    if ( beanClass != null ) {
                        boolean foundProperty = false;
                        for ( final PropertyDescriptor descriptor : descriptors ) {
                            if ( descriptor.getName().equals( propValue.getName() ) ) {
                                foundProperty = true;
                                break;
                            }
                        }

                        Assert.assertTrue( "Property '"+propValue.getName()+"' for bean '"+beanId+"' does not exist", foundProperty );
                    }
                }
            }
        }
    }

    /**
     * Using the secured annotation on an instance is incorrect, it only works on an interface
     */
    @SuppressWarnings({"unchecked"})
    @Test
    public void testNoSecuredImplementations() throws Exception {
        //
        DefaultListableBeanFactory dlbf = new DefaultListableBeanFactory();
        XmlBeanDefinitionReader xbdr = new XmlBeanDefinitionReader(dlbf);

        for ( String context : CONTEXTS ) {
            xbdr.loadBeanDefinitions(new ClassPathResource(context));
        }

        for ( String beanId : dlbf.getBeanDefinitionNames() ) {
            BeanDefinition beanDef = dlbf.getBeanDefinition( beanId );
            String className = beanDef.getBeanClassName();
            if ( className != null && className.startsWith("com.l7tech" ) ) {
                Class beanClass = Class.forName(className);
                if ( beanClass.getAnnotation(Secured.class) != null ) {
                    Assert.fail( "Implementation bean '"+beanId+"', is annotated with the '"+Secured.class.getName()+"' annotation, this should only be used on interfaces." );
                }

                for ( Method method : beanClass.getDeclaredMethods() ) {
                    if ( method.getAnnotation(Secured.class) != null ) {
                        Assert.fail( "Implementation bean '"+beanId+"' method '"+method.getName()+"', is annotated with the '"+Secured.class.getName()+"' annotation, this should only be used on interfaces." );
                    }
                }
            }
        }
    }
}