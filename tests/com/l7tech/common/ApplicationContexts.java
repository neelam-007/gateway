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
    
    public static ApplicationContext getTestApplicationContext() {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[]{DEFAULT_TEST_BEAN_DEFINITIONS});
        return context;
    }
    public static final String DEFAULT_TEST_BEAN_DEFINITIONS = "com/l7tech/common/testApplicationContext.xml";
}