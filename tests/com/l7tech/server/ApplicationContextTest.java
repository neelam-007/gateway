package com.l7tech.server;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.core.io.ClassPathResource;

/**
 * Basic test for spring application context.
 *
 * @author Steve Jones
 */
public class ApplicationContextTest  extends TestCase {

    private static final String[] CONTEXTS = {
        "com/l7tech/server/resources/adminContext.xml",
        "com/l7tech/server/resources/dataAccessContext.xml",
        "com/l7tech/server/resources/rbacEnforcementContext.xml",
        "com/l7tech/server/resources/webApplicationContext.xml"
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
                List<ConstructorArgumentValues.ValueHolder> constructorArgs = new ArrayList();
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
}
