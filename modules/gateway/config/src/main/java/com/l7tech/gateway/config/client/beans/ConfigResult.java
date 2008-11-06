/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.config.client.beans;

import com.l7tech.config.client.beans.ConfigurationBean;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** @author alex */
public class ConfigResult {
    public static enum Type {
        /**
         * Push the provided beans onto the configuration stack and enter a new context.
         * If only one bean is provided, it will be prompted to configure itself immediately.
         * If multiple beans are provided, the new beans are used to build a submenu.
         */
        PUSH,

        /**
         * Terminates configuration of this bean and returns zero or more beans to the previous context
         */
        POP,

        /**
         * Chains the configuration process to a new set of beans without pushing the context stack. Useful for
         * wizard-like workflows with variable number of steps, where you don't want to care how deep you are in the stack.
         */
        CHAIN,

        /**
         * Remains in the current context with a (possibly) expanded menu.
         */
        STAY
    }

    private final Type type;
    private final ConfigurationBean[] beans;

    private ConfigResult(Type type, ConfigurationBean... beans) {
        this.type = type;
        this.beans = beans;
    }

    public Type getType() {
        return type;
    }

    public List<ConfigurationBean> getBeans() {
        return Collections.unmodifiableList(Arrays.asList(beans));
    }

    public static ConfigResult push(ConfigurationBean... beans) {
        return new ConfigResult(Type.PUSH, beans);
    }

    public static ConfigResult chain(ConfigurationBean... beans) {
        return new ConfigResult(Type.CHAIN, beans);
    }

    public static ConfigResult stay(ConfigurationBean... beans) {
        return new ConfigResult(Type.STAY, beans);
    }

    public static ConfigResult pop(ConfigurationBean... beans) {
        return new ConfigResult(Type.POP, beans);
    }
}
