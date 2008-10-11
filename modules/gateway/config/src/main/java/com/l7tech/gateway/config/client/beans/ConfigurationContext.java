/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.config.client.beans;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** @author alex */
public class ConfigurationContext {
    private final ConfigurationContext parent;
    private final List<ConfigurationBean> beans;

    public ConfigurationContext(ConfigurationContext parent, ConfigurationBean... beans) {
        this.parent = parent;
        List<ConfigurationBean> tempBeans = new ArrayList<ConfigurationBean>();
        tempBeans.addAll(Arrays.asList(beans));
        this.beans = tempBeans;
    }

    public ConfigurationContext getParent() {
        return parent;
    }

    public List<ConfigurationBean> getBeans() {
        return Collections.unmodifiableList(beans);
    }

    public void removeBean(ConfigurationBean bean) {
        beans.remove(bean);
    }

    public boolean containsBean(Class<? extends ConfigurationBean> beanClass) {
        for (ConfigurationBean bean : beans) {
            if (beanClass.isAssignableFrom(bean.getClass())) return true;
        }
        return false;
    }
}
