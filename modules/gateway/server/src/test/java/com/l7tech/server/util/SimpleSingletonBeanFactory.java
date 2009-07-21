package com.l7tech.server.util;

import com.l7tech.util.Pair;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;

import java.util.HashMap;
import java.util.Map;

/**
 * A bean factory to use in unit tests that don't need to bring up an entire ApplicationContext.
 */
public class SimpleSingletonBeanFactory extends AbstractBeanFactory {
    private final Map<String, Pair<BeanDefinition, Object>> beans = new HashMap<String, Pair<BeanDefinition, Object>>();

    public SimpleSingletonBeanFactory() {
        this(null, null);
    }

    public SimpleSingletonBeanFactory(BeanFactory parentBeanFactory) {
        this(parentBeanFactory, null);
    }

    public SimpleSingletonBeanFactory(Map<String,Object> beans) {
        this(null, beans);
    }

    public SimpleSingletonBeanFactory(BeanFactory parentBeanFactory, Map<String,Object> beans) {
        super(parentBeanFactory);
        if (beans != null) {
            for (Map.Entry<String, Object> entry : beans.entrySet())
                addBean(entry.getKey(), entry.getValue());
        }
    }

    protected Map findMatchingBeans(Class requiredType) throws BeansException {
        Map<String,Object> ret = new HashMap<String,Object>();
        for (Map.Entry<String, Pair<BeanDefinition, Object>> entry : beans.entrySet()) {
            if (requiredType.isAssignableFrom(entry.getValue().right.getClass()))
                ret.put(entry.getKey(), entry.getValue().right);
        }
        return ret;
    }

    @Override
    protected boolean containsBeanDefinition(String beanName) {
        return beans.containsKey(beanName);
    }

    @Override
    protected BeanDefinition getBeanDefinition(String beanName) throws BeansException {
        Pair<BeanDefinition, Object> bean = beans.get(beanName);
        if (bean == null) throw new NoSuchBeanDefinitionException(beanName, "no such bean " + beanName);
        return bean.left;
    }

    @Override
    protected Object createBean(String beanName, RootBeanDefinition mbd, Object[] args) throws BeanCreationException {
        Pair<BeanDefinition, Object> bean = beans.get(beanName);
        if (bean == null) throw new NoSuchBeanDefinitionException(beanName, "no such bean " + beanName);
        return bean.right;
    }

    public final void addBean(String beanName, Object bean) {
        beans.put(beanName, new Pair<BeanDefinition, Object>(new RootBeanDefinition(bean.getClass()), bean));
    }
}
