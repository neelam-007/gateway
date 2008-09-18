package com.l7tech.server;

import com.l7tech.gateway.common.admin.Administrative;
import com.l7tech.gateway.common.security.rbac.Secured;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Collections;
import java.lang.reflect.Method;

/**
 * Basic test for spring application context.
 *
 * @author Steve Jones
 */
public class ApplicationContextTest  extends TestCase {

    private static final String[] CONTEXTS = {
        "com/l7tech/server/resources/adminContext.xml",
        "com/l7tech/server/resources/dataAccessContext.xml",
        "com/l7tech/server/resources/ssgApplicationContext.xml"
    };

    private static final Set<String> NON_ADMIN_BEANS = new HashSet<String>( Collections.<String>emptySet());
    private static final Set<String> EXTRA_ADMIN_BEANS = new HashSet<String>( Arrays.asList( "adminLogin" ) );
    private static final Set<String> NON_SECURED_BEANS = new HashSet<String>( Arrays.asList( "customAssertionsAdmin" ) );

    public ApplicationContextTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(ApplicationContextTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    /**
     * Loading the definitions in this way will check the syntax and that all the
     * bean classes exist.
     */
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
    public void testApplicationContextReferences() {
        //
        DefaultListableBeanFactory dlbf = new DefaultListableBeanFactory();
        XmlBeanDefinitionReader xbdr = new XmlBeanDefinitionReader(dlbf);

        for (String context : CONTEXTS) {
            xbdr.loadBeanDefinitions(new ClassPathResource(context));
        }

        for (String beanId : dlbf.getBeanDefinitionNames()) {
            BeanDefinition bd = dlbf.getBeanDefinition(beanId);

            ConstructorArgumentValues cav = bd.getConstructorArgumentValues();
            if (cav != null) {
                List<ConstructorArgumentValues.ValueHolder> constructorArgs = new ArrayList<ConstructorArgumentValues.ValueHolder>();
                constructorArgs.addAll((Collection<ConstructorArgumentValues.ValueHolder>) cav.getGenericArgumentValues());
                constructorArgs.addAll((Collection<ConstructorArgumentValues.ValueHolder>) cav.getIndexedArgumentValues().values());
                for (ConstructorArgumentValues.ValueHolder holder : constructorArgs) {
                    Object value = holder.getValue();
                    if (value instanceof RuntimeBeanReference) {
                        RuntimeBeanReference rbr = (RuntimeBeanReference) value;
                        String name = rbr.getBeanName();
                        assertTrue("Application context uses bean with name '"+name+"', but no such bean exists (referenced from bean '"+beanId+"' as constructor-arg) ", 
                                   dlbf.containsBean(name));
                    }
                }
            }

            MutablePropertyValues mpv = bd.getPropertyValues();
            if (mpv != null) {
                for (PropertyValue propValue : mpv.getPropertyValues()) {
                    Object value = propValue.getValue();
                    if (value instanceof RuntimeBeanReference) {
                        RuntimeBeanReference rbr = (RuntimeBeanReference) value;
                        String name = rbr.getBeanName();
                        assertTrue("Application context uses bean with name '"+name+"', but no such bean exists (referenced from bean '"+beanId+"' as property) ", 
                                   dlbf.containsBean(name));
                    }
                }
            }
        }
    }

    /*
     * Ensure that all Administration beans in the application context are annotated with @Administrative
     */
    @SuppressWarnings({"unchecked"})
    public void testAdministrativeAutoProxy() throws Exception {
        //
        DefaultListableBeanFactory dlbf = new DefaultListableBeanFactory();
        XmlBeanDefinitionReader xbdr = new XmlBeanDefinitionReader(dlbf);

        for ( String context : CONTEXTS ) {
            xbdr.loadBeanDefinitions(new ClassPathResource(context));
        }

        List<String> beans = new ArrayList<String>();
        beans.addAll(EXTRA_ADMIN_BEANS);

        for ( String beanId : dlbf.getBeanDefinitionNames() ) {
            if ( beanId.endsWith("Admin") && !NON_ADMIN_BEANS.contains(beanId) ) {
                beans.add(beanId);
            }
        }

        for ( String beanId : beans ) {
            System.out.println("Checking bean '"+beanId+"'.");
            BeanDefinition beanDef = dlbf.getBeanDefinition( beanId );
            String className = beanDef.getBeanClassName();
            Class adminClass = Class.forName(className);

            if ( adminClass.getAnnotation(Administrative.class) == null ) {
                fail( "Administrative bean '"+beanId+"', is mssing the '"+Administrative.class.getName()+"' annotation." );
            }

            boolean secured = false;
            for ( Class interfaceClass : adminClass.getInterfaces() ) {
                if ( interfaceClass.getAnnotation(Secured.class) != null ) {
                    secured = true;
                }
            }

            if ( !secured && !NON_SECURED_BEANS.contains(beanId) ) {
                fail( "Administrative bean '"+beanId+"', has no interface with the '"+Secured.class.getName()+"' annotation." );
            }
        }
    }

    /**
     * Using the secured annotation on an instance is incorrect, it only works on an interface
     */
    @SuppressWarnings({"unchecked"})
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
                    fail( "Implementation bean '"+beanId+"', is annotated with the '"+Secured.class.getName()+"' annotation, this should only be used on interfaces." );
                }

                for ( Method method : beanClass.getDeclaredMethods() ) {
                    if ( method.getAnnotation(Secured.class) != null ) {
                        fail( "Implementation bean '"+beanId+"' method '"+method.getName()+"', is annotated with the '"+Secured.class.getName()+"' annotation, this should only be used on interfaces." );
                    }
                }
            }
        }
    }
}
