/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.common;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.io.Resource;

import java.util.Map;
import java.util.Locale;
import java.io.IOException;

/**
 * Test application contexts
 * @author emil
 * @version Dec 13, 2004
 */
public class ApplicationContexts {

    /**
     * Empty/null context that laways returns null for every method.
     */
    public static final ApplicationContext NULL_CONTEXT = new ApplicationContext() {
        public ApplicationContext getParent() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public String getDisplayName() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public long getStartupDate() {
            return 0;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public void publishEvent(ApplicationEvent event) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public int getBeanDefinitionCount() {
            return 0;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public String[] getBeanDefinitionNames() {
            return new String[0];  //To change body of implemented methods use File | Settings | File Templates.
        }

        public String[] getBeanDefinitionNames(Class type) {
            return new String[0];  //To change body of implemented methods use File | Settings | File Templates.
        }

        public boolean containsBeanDefinition(String name) {
            return false;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public Map getBeansOfType(Class type) throws BeansException {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public Map getBeansOfType(Class type, boolean includePrototypes, boolean includeFactoryBeans) throws BeansException {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public Object getBean(String name) throws BeansException {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public Object getBean(String name, Class requiredType) throws BeansException {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public boolean containsBean(String name) {
            return false;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
            return false;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public Class getType(String name) throws NoSuchBeanDefinitionException {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public String[] getAliases(String name) throws NoSuchBeanDefinitionException {
            return new String[0];  //To change body of implemented methods use File | Settings | File Templates.
        }

        public BeanFactory getParentBeanFactory() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public String getMessage(String code, Object[] args, String defaultMessage, Locale locale) {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public String getMessage(String code, Object[] args, Locale locale) throws NoSuchMessageException {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public String getMessage(MessageSourceResolvable resolvable, Locale locale) throws NoSuchMessageException {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public Resource getResource(String location) {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        /**
         * Resolve the given location pattern into Resource objects.
         *
         * @param locationPattern the location pattern to resolve
         * @return the corresponding Resource objects
         * @throws java.io.IOException in case of I/O errors
         */
        public Resource[] getResources(String locationPattern) throws IOException {
            return new Resource[0];
        }
    };

    public static ApplicationContext getTestApplicationContext() {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[]{DEFAULT_TEST_BEAN_DEFINITIONS});
        return context;
    }
    public static final String DEFAULT_TEST_BEAN_DEFINITIONS = "com/l7tech/common/testApplicationContext.xml";
}