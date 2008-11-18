/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.config.client.beans;

import com.l7tech.config.client.ConfigurationException;
import com.l7tech.config.client.beans.ConfigurationBean;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Pair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

/** @author alex */
public class ConfigInterviewer {
    private static final Logger logger = Logger.getLogger(ConfigInterviewer.class.getName());

    private final ConfigurationBean[] beans;
    private static final Pair<MenuResultType,DynamicConfigurationBean> MENU_QUIT = new Pair<MenuResultType, DynamicConfigurationBean>(MenuResultType.QUIT, null);
    private static final Pair<MenuResultType,DynamicConfigurationBean> MENU_REPEAT = new Pair<MenuResultType, DynamicConfigurationBean>(MenuResultType.REPEAT, null);

    public ConfigInterviewer(ConfigurationBean... beans) {
        this.beans = beans;
    }

    @SuppressWarnings({"unchecked"})
    public List<ConfigurationBean> doInterview() throws IOException {
        BufferedReader inReader = new BufferedReader(new InputStreamReader(System.in));

        ConfigurationContext currentContext = new ConfigurationContext(null, beans);
        menuLoop: while (true) {
            List<Pair<String, DynamicConfigurationBean>> configurables = buildMenu(currentContext);

            final DynamicConfigurationBean selected;
            if (configurables.size() > 1) {
                Pair<MenuResultType, DynamicConfigurationBean> mr = doMenu(configurables, currentContext, inReader);
                switch(mr.left) {
                    case REPEAT:
                        continue menuLoop;

                    case APPLY:
                        return currentContext.getBeans();

                    case QUIT:
                        return Collections.emptyList();

                    case SELECT:
                        selected = mr.right;
                        break;

                    default:
                        throw new IllegalStateException();
                }
            } else if (configurables.isEmpty()) {
                System.out.println("Configuration cannot be changed");
                break;
            } else {
                selected = configurables.get(0).right;
            }

            boolean valueDone = false;
            while (!valueDone) {
                ConfigResult result;
                if (selected instanceof EditableConfigurationBean) {
                    EditableConfigurationBean cb = (EditableConfigurationBean)selected;
                    Object dflt = cb.getDefaultValue();
                    Object val = cb.getDisplayValue();
                    if (val == null) val = dflt;

                    System.out.println("Configuring " + cb.getConfigName());
                    System.out.println();
                    System.out.println("Name : " + cb.getConfigName());
                    if ( cb.getShortValueDescription() != null ) {
                        System.out.println("Value: " + cb.getShortValueDescription());
                    }
                    System.out.println();
                    System.out.print("Enter new value");
                    if ( val != null ) {
                        System.out.print(" [" + val + "]");
                    }
                    System.out.print(": ");

                    final String cmd = inReader.readLine().trim();
                    System.out.println();

                    if ("<".equals(cmd)) {
                        if ( currentContext.getBeans().size() <= 1 && currentContext.getParent() != null ) {
                            currentContext = currentContext.getParent();
                        }
                        continue menuLoop;
                    }

                    try {
                        if ( "".equals(cmd) ) {
                            if ( val!=null && !val.equals(cb.getConfigValue()) ) {
                                // set from default
                                cb.setConfigValue(val);
                            }
                            if ( cb.getConfigValue() == null ) {
                                break;
                            }
                        } else {
                            val = cb.parse(cmd);
                            cb.validate(val);
                            cb.setConfigValue(val);
                        }
                        result = cb.onConfiguration(val, currentContext);
                    } catch (ConfigurationException e) {
                        System.out.println(ExceptionUtils.getMessage(e));
                        logger.log(Level.INFO, "Bad input for " + cb + ": " + ExceptionUtils.getMessage(e), e);
                        continue;
                    }
                } else {
                    result = selected.onConfiguration(selected.getConfigValue(), currentContext);
                }

                ConfigurationContext parent = currentContext.getParent();
                List<ConfigurationBean> existingBeans = currentContext.getBeans();
                List<ConfigurationBean> newBeans = result.getBeans();
                switch (result.getType()) {
                    case PUSH:
                        if (newBeans.isEmpty()) throw new IllegalStateException("Can't push no beans");
                        currentContext = new ConfigurationContext(currentContext, newBeans.toArray(new ConfigurationBean[newBeans.size()]));
                        continue menuLoop;
                    case POP:
                        if (!newBeans.isEmpty()) {
                            List<ConfigurationBean> allBeans = new ArrayList<ConfigurationBean>();
                            allBeans.addAll(parent.getBeans());
                            allBeans.addAll(newBeans);
                            currentContext = new ConfigurationContext(parent.getParent(), allBeans.toArray(new ConfigurationBean[allBeans.size()]));
                        } else if (parent != null) {
                            currentContext = parent;
                        }
                        continue menuLoop;
                    case STAY:
                        List<ConfigurationBean> allBeans = new ArrayList<ConfigurationBean>();
                        allBeans.addAll(existingBeans);
                        allBeans.addAll(newBeans);
                        currentContext = new ConfigurationContext(parent, allBeans.toArray(new ConfigurationBean[allBeans.size()]));
                        continue menuLoop;
                    case CHAIN:
                        currentContext = new ConfigurationContext(parent, newBeans.toArray(new ConfigurationBean[newBeans.size()]));
                        continue menuLoop;
                }
            }
        }
        throw new IllegalStateException();
    }

    private static enum MenuResultType {
        REPEAT, QUIT, APPLY, SELECT
    }

    private Pair<MenuResultType, DynamicConfigurationBean> doMenu(List<Pair<String, DynamicConfigurationBean>> configurables, ConfigurationContext currentContext, BufferedReader inReader)
            throws IOException
    {
        for (Pair<String, DynamicConfigurationBean> pair : configurables) {
            System.out.println(pair.left);
        }

        System.out.println();

        if (currentContext.getParent() == null) {
            System.out.println("  S: Save changes and exit");
        }

        System.out.println("  X: Quit, discarding changes");

        System.out.println();
        System.out.print("Select: ");
        String cmd = inReader.readLine();
        if ("x".equalsIgnoreCase(cmd.trim().toLowerCase())) {
            currentContext = currentContext.getParent();
            if (currentContext == null) {
                return MENU_QUIT;
            } else {
                return MENU_REPEAT;
            }
        } else if ("s".equalsIgnoreCase(cmd.trim().toLowerCase())) {
            if (currentContext.getParent() == null) {
                return new Pair<MenuResultType, DynamicConfigurationBean>(MenuResultType.APPLY, null);
            } else {
                throw new IllegalStateException("Can't apply from non-root context!");
            }
        } else if ("".equals(cmd.trim())) {
            System.out.println();
            return MENU_REPEAT;
        }

        int choice;
        try {
            choice = Integer.valueOf(cmd);
        } catch (NumberFormatException e) {
            System.out.println("Invalid selection: " + ExceptionUtils.getMessage(e));
            return MENU_REPEAT;
        }

        if (choice < 1 || choice > configurables.size()) {
            System.out.println("Invalid selection: " + choice + " (must be between 1 and " + configurables.size() + ")");
            return MENU_REPEAT;
        }

        return new Pair<MenuResultType, DynamicConfigurationBean>(MenuResultType.SELECT, configurables.get(choice - 1).right);
    }

    @SuppressWarnings({"unchecked"})
    private List<Pair<String, DynamicConfigurationBean>> buildMenu(ConfigurationContext ctx) {
        int i = 0;
        final List<Pair<String, DynamicConfigurationBean>> configurables = new ArrayList<Pair<String,DynamicConfigurationBean>>();
        for (ConfigurationBean config : ctx.getBeans()) {
            if (config instanceof ConfigurableBeanFactory) {
                final ConfigurableBeanFactory factory = (ConfigurableBeanFactory)config;
                final ConfigurationBean bean = factory.make();
                configurables.add(new Pair<String, DynamicConfigurationBean>(
                            String.format("%3d: %s", ++i, "New " + config.getConfigName()),
                            new DynamicConfigurationBean("_new." + bean.getId(), "New " + bean.getConfigName(), null) {
                                @Override
                                public ConfigResult onConfiguration(Object value, ConfigurationContext context) {
                                    return ConfigResult.push(bean);
                                }
                            }
                        ));
            } else if (config instanceof EditableConfigurationBean) {
                // TODO what if a bean is both editable and deletable?
                EditableConfigurationBean configurableBean = (EditableConfigurationBean)config;
                String desc = config.getShortValueDescription();
                configurables.add(new Pair<String, DynamicConfigurationBean>(
                        String.format("%3d: %s: %s", ++i, config.getConfigName(), desc == null ? "<not configured>" : desc),
                        configurableBean));
            } else if (config.isDeletable()) {
                // TODO what if a bean is both editable and deletable?
                configurables.add(new Pair<String, DynamicConfigurationBean>(
                        String.format("%3d: %s %s", ++i, "Delete " + config.getConfigName(), config.getShortValueDescription()),
                        new ConfirmDeletion(config)));
            }
        }

        return configurables;
    }
}
