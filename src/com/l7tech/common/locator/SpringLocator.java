/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.common.locator;

import com.l7tech.common.util.Locator;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;

import java.util.*;
import java.util.logging.Level;

/**
 * Locator to Spring framework.
 * This is a transitional class.
 *
 * @author emil
 * @version Oct 5, 2004
 */
public class SpringLocator extends AbstractLocator {
    private ApplicationContext context;

    public SpringLocator(ApplicationContext context) {
        if (context == null) {
            throw new IllegalArgumentException();
        }
        this.context = context;
    }

    /**
     * The general lookup method.
     *
     * @param template a template describing the services to look for
     * @return an object containing the matching results
     */
    public Locator.Matches lookup(Locator.Template template) {
        return new SpringMatches(template);
    }

    private class SpringMatches extends Matches {
        private Template template;

        public SpringMatches(Template template) {
            this.template = template;
        }

        /**
         * Get all registered Items that match the criteria.
         * This should include all pairs of instances together
         * with their classes, IDs, and so on.
         *
         * @return collection of {@link com.l7tech.common.util.Locator.Item}
         */
        public Collection allItems() {
            try {
                if (template.getId() != null) {
                    Object o = context.getBean(template.getId());
                    SpringBean sb = new SpringBean(template.getId(), o, o.getClass());
                    return Collections.singletonList(sb);
                }
                if (template.getType() != null) {
                    List springBeans = new ArrayList();
                    final Map beansOfType = context.getBeansOfType(template.getType(), true, true);
                    for (Iterator iterator = beansOfType.keySet().iterator(); iterator.hasNext();) {
                        String key = (String)iterator.next();
                        Object bean = beansOfType.get(key);
                        springBeans.add(new SpringBean(key, bean, bean.getClass()));
                    }
                    return springBeans;
                }
            } catch (NoSuchBeanDefinitionException e) {
                logger.log(Level.WARNING, "Could not find the bean " + template.getId(), e);
            }
            return Collections.EMPTY_LIST;
        }
    }

    private static class SpringBean extends Item {
        private final String id;
        private final Object instance;
        private final Class type;

        public SpringBean(String id, Object instance, Class type) {
            this.id = id;
            this.instance = instance;
            this.type = type;
        }

        /**
         * Get the instance itself.
         *
         * @return the instance or null if the instance cannot be created
         */
        public Object getInstance() {
            return instance;
        }

        /**
         * Get the implementing class of the instance.
         *
         * @return the class of the item
         */
        public Class getType() {
            return type;
        }

        /**
         * Get the identifier for the item.
         * This identifier should uniquely represent the item
         * within its containing lookup
         *
         * @return a string ID of the item
         */
        public String getId() {
            return id;
        }
    }
}