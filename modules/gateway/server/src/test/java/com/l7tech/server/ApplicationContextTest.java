package com.l7tech.server;

import com.l7tech.objectmodel.EntityManager;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.springframework.aop.framework.autoproxy.BeanNameAutoProxyCreator;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.config.RuntimeBeanNameReference;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.core.io.ClassPathResource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Collections;

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
     * Ensure that all EntityManager beans in the application context are registered with the BeanNameAutoProxyCreator
     */
    @SuppressWarnings({"unchecked"})                  
    public void testEntityManagerAutoProxy() throws Exception {
        //
        DefaultListableBeanFactory dlbf = new DefaultListableBeanFactory();
        XmlBeanDefinitionReader xbdr = new XmlBeanDefinitionReader(dlbf);

        for (String context : CONTEXTS) {
            xbdr.loadBeanDefinitions(new ClassPathResource(context));
        }

        String[] autoProxyDefn = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(dlbf, BeanNameAutoProxyCreator.class, false, false);
        assertNotNull(autoProxyDefn); assertTrue(autoProxyDefn.length == 1);
        BeanDefinition proxyDef = dlbf.getBeanDefinition(autoProxyDefn[0]);

        //noinspection unchecked
        List<String> regbeans = new ArrayList<String>();
        for ( RuntimeBeanNameReference nameRef : (List<RuntimeBeanNameReference>)proxyDef.getPropertyValues().getPropertyValue("beanNames").getValue() ) {
            regbeans.add( nameRef.getBeanName() );
        }
        Collections.sort(regbeans);

        System.out.println("Bean names: " + regbeans);

        String[] entityManagerDefns = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(dlbf, EntityManager.class, false, false);
        assertNotNull(entityManagerDefns); assertTrue(entityManagerDefns.length > 0);

        for (String name : entityManagerDefns) {
            System.out.println("Checking for bean auto-proxy: " + name);
            assertTrue("Bean " + name + " should be registered with BeanNameAutoProxyCreator", regbeans.contains(name));
        }
    }
}
