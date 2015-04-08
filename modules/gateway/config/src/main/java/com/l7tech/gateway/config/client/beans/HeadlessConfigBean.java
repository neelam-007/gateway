package com.l7tech.gateway.config.client.beans;


import com.l7tech.config.client.ConfigurationException;
import com.l7tech.config.client.ConfigurationFactory;
import com.l7tech.config.client.beans.ConfigurationBean;
import com.l7tech.config.client.options.Option;
import com.l7tech.config.client.options.OptionGroup;
import com.l7tech.config.client.options.OptionSet;
import com.l7tech.config.client.options.OptionType;
import com.l7tech.gateway.config.client.HeadlessConfig;
import com.l7tech.util.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class will configure the gateway from a properties file.
 */
public class HeadlessConfigBean {
    private static final Logger logger = Logger.getLogger(HeadlessConfigBean.class.getName());

    //The node configuration provider
    //TODO: should this be used or should a NodeManagementApi be used directly?
    private final NodeConfigurationBeanProvider provider;
    //This is the stream to print any output or error message info to.
    private final PrintStream printStream;

    public static final String GENERIC_HELP = "Usage: Configure a new Gateway node (database, node properties) using properties (key=value pairs) from standard in.\n" +
            "Credentials: Root or ssgconfig user.\n" +
            "\n" +
            "Command syntax: ssgconfig-headless <command> [-option]\n" +
            "Commands: create [-help | -template]\n" +
            "          help";

    /**
     * Creates a new HeadlessConfigBean.
     *
     * @param provider    The node NodeConfigurationBeanProvider to use to configure the gateway
     * @param printStream The PrintStream to print output to.
     */
    public HeadlessConfigBean(@NotNull final NodeConfigurationBeanProvider provider, @NotNull final PrintStream printStream) {
        this.provider = provider;
        this.printStream = printStream;
    }

    /**
     * This will do the actual work of configuring the gateway.
     *
     * @param command    The configuration command
     * @param option     The configuration command option
     * @param properties The properties accessor to use to configure the gateway with
     * @throws ConfigurationException This is thrown if there was an error configuring the gateway
     */
    public void configure(@NotNull final String command, @Nullable final String option, @NotNull final PropertiesAccessor properties) throws ConfigurationException {
        //find the command to configure the gateway with
        final Functions.UnaryVoidThrows<PropertiesAccessor, Throwable> commandFunction = commandMap.get(new Pair<>(command, option));

        if (commandFunction == null) {
            if (!containsCommand(command)) {
                throw new ConfigurationException("Unknown command '" + command + "'. Expected one of: " + getCommands());
            } else {
                throw new ConfigurationException("Unknown command option '" + option + "'. Expected one of: " + getCommandOptions(command));
            }
        } else {
            try {
                logger.log(Level.INFO, "Running command: " + command + (option == null ? "" : (" " + option)));
                //call the command to configure the gateway
                commandFunction.call(properties);
            } catch (Throwable e) {
                throw new ConfigurationException("Exception configuring gateway: " + ExceptionUtils.getMessage(e), e);
            }
        }
    }

    /**
     * These are the possible commands that can be executed.
     */
    private final Map<Pair<String, String>, Functions.UnaryVoidThrows<PropertiesAccessor, Throwable>> commandMap = CollectionUtils.MapBuilder.<Pair<String, String>, Functions.UnaryVoidThrows<PropertiesAccessor, Throwable>>builder()
            //This prints out general help on the command usage
            .put(new Pair<String, String>("help", null), new Functions.UnaryVoidThrows<PropertiesAccessor, Throwable>() {
                @Override
                public void call(PropertiesAccessor propertiesAccessor) {
                    printStream.println(GENERIC_HELP);
                }
            })
            .put(new Pair<>("create", "-template"), new Functions.UnaryVoidThrows<PropertiesAccessor, Throwable>() {
                @Override
                public void call(PropertiesAccessor propertiesAccessor) throws ConfigurationException {
                    printStream.println("#### Headless config create template properties file ####");

                    final OptionSet optionSet = getOptionSet();
                    for (final OptionGroup optionGroup : optionSet.getOptionGroups()) {
                        final Set<Option> options = optionSet.getOptionsForGroup(optionGroup.getId());
                        //do not use the properties accessor here, it will attempt to read properties from the input stream
                        final Collection<ConfigurationBean> configBeans = getConfigBeans(options, new Properties(), true);
                        if (configBeans.size() > 0) {
                            printStream.println("\n### " + optionGroup.getDescription() + " ###");
                        }
                        for (final ConfigurationBean configurationBean : configBeans) {
                            final String name = configurationBean.getConfigName();
                            final Object value = configurationBean.getConfigValue();
                            final Option option = getOption(configurationBean.getId(), options);
                            if (option != null && option.getDescription() != null) {
                                printStream.println("## " + option.getDescription());
                            }
                            if (value == null || "database.failover.port".equals(name)) {
                                printStream.print("#");
                            }
                            printStream.println(name + "=" + (value == null ? "" : value));
                        }
                    }
                }
            })
            .put(new Pair<>("create", "-help"), new Functions.UnaryVoidThrows<PropertiesAccessor, Throwable>() {
                @Override
                public void call(PropertiesAccessor propertiesAccessor) throws ConfigurationException {
                    printStream.println(
                            "Create a gateway node using properties (key=value pairs) from standard in.\n" +
                                    "\n" +
                                    "Usage: ssgconfig-headless create [-help | -template]\n" +
                                    "           -template: Output a properties template (key=value pairs) to standard out.\n" +
                                    "           -help: Display help for command.\n" +
                                    "\n" +
                                    "Examples:\n" +
                                    "Output a properties template to a file:\n" +
                                    "ssgconfig-headless create -template > create-node.properties\n" +
                                    "\n" +
                                    "Configure a new Gateway node using a properties file:\n" +
                                    "cat create-node.properties | ssgconfig-headless create\n" +
                                    "\n" +
                                    "Remotely configure a new Gateway node using a properties file:\n" +
                                    "cat create-node.properties | ssh ssgconfig@gatewayhost /opt/SecureSpan/Gateway/config/bin/ssgconfig-headless create");
                }
            })
            .put(new Pair<String, String>("create", null), new Functions.UnaryVoidThrows<PropertiesAccessor, Throwable>() {
                @Override
                public void call(final PropertiesAccessor propertiesAccessor) throws ConfigurationException {
                    final OptionSet optionSet = getOptionSet();
                    final Set<Option> options = optionSet.getOptions();
                    //validate the properties and create the config beans
                    final Collection<ConfigurationBean> configBeans = getConfigBeans(options, propertiesAccessor.getProperties(), false);
                    //validate the config beans
                    final String databaseType = getOptionValue("database.type", configBeans);
                    if (!"mysql".equals(databaseType) && !"embedded".equals(databaseType)) {
                        throw new ConfigurationException("Unknown database type '" + databaseType + "'. Expected one of: 'mysql' or 'embedded'");
                    }
                    if ("mysql".equals(databaseType)) {
                        checkNotNull(configBeans, Arrays.asList("database.host", "database.port", "database.name", "database.user", "database.pass", "database.admin.user", "database.admin.pass", "admin.user", "admin.pass", "cluster.host"), "This property is needed when configuring a mysql database.");
                        //check that both 'database.failover.host' and 'database.failover.port' are null or that they are both not null
                        final ConfigurationBean databaseFailoverHostConfigBean = getConfigBean("database.failover.host", configBeans);
                        final Object databaseFailoverHostValue = databaseFailoverHostConfigBean == null ? null : databaseFailoverHostConfigBean.getConfigValue();
                        final ConfigurationBean databaseFailoverPortConfigBean = getConfigBean("database.failover.port", configBeans);
                        final Object databaseFailoverPortValue = databaseFailoverPortConfigBean == null ? null : databaseFailoverPortConfigBean.getConfigValue();
                        if ((databaseFailoverHostValue == null && databaseFailoverPortValue != null) || (databaseFailoverHostValue != null && databaseFailoverPortValue == null)) {
                            throw new ConfigurationException("When configuring a failover database must specify both the 'database.failover.host' and the 'database.failover.port'. Missing: " + (databaseFailoverHostValue == null ? "database.failover.host" : "database.failover.port"));
                        }
                    } else {
                        //this means its derby
                        checkNotNull(configBeans, Arrays.asList("admin.user", "admin.pass", "cluster.host"), "This property is needed when configuring an embedded database.");
                        checkNull(configBeans, Arrays.asList("database.host", "database.port", "database.name", "database.user", "database.pass", "database.admin.user", "database.admin.pass", "database.failover.host", "database.failover.port"), "This property should not be specified when configuring an embedded database.");
                        //setting the database.host to null will configure a derby database
                        ConfigurationBean databaseHostConfigBean = getConfigBean("database.host", configBeans);
                        if (databaseHostConfigBean != null) {
                            //noinspection unchecked
                            databaseHostConfigBean.setConfigValue(null);
                        }
                    }
                    final Boolean configureNode = getOptionValue("configure.node", configBeans);
                    if (configureNode == null || configureNode) {
                        //validate the node options
                        checkNotNull(configBeans, Arrays.asList("cluster.pass"), "The cluster password must be specified when configuring the node.");
                    }
                    logger.log(Level.INFO, "Configuring '" + databaseType + "' database" + ((configureNode == null || configureNode) ? " and configuring node.properties" : ""));
                    provider.storeConfiguration(configBeans);

                    printStream.println("Configuration Successful");
                }
            })
            .map();

    /**
     * This will return the configuration bean with the given id from the given collection
     *
     * @param id          The id of the configuration bean to find
     * @param configBeans The config beans to search
     * @return The config bean or null if it cant be found
     */
    @Nullable
    private ConfigurationBean getConfigBean(@NotNull final String id, @NotNull final Collection<ConfigurationBean> configBeans) {
        for (ConfigurationBean bean : configBeans) {
            if (id.equals(bean.getConfigName())) {
                return bean;
            }
        }
        return null;
    }

    /**
     * Throws an exception if any of the given properties to check are null
     *
     * @param configBeans       The beans to search through
     * @param propertiesToCheck The properties to check
     * @param message           The message to add to the exception message
     * @throws ConfigurationException This is thrown if and of the given property are null
     */
    private void checkNotNull(@NotNull final Collection<ConfigurationBean> configBeans, @NotNull final List<String> propertiesToCheck, @Nullable final String message) throws ConfigurationException {
        for (String propertyName : propertiesToCheck) {
            if (getOptionValue(propertyName, configBeans) == null) {
                throw new ConfigurationException("Missing configuration property '" + propertyName + (message == null ? "'" : "'. Message: " + message));
            }
        }
    }

    /**
     * Throws an exception if any of the given properties to check are not null
     *
     * @param configBeans       The beans to search through
     * @param propertiesToCheck The properties to check
     * @param message           The message to add to the exception message
     * @throws ConfigurationException This is thrown if and of the given property are notnull
     */
    private void checkNull(@NotNull final Collection<ConfigurationBean> configBeans, @NotNull final List<String> propertiesToCheck, @Nullable final String message) throws ConfigurationException {
        for (String propertyName : propertiesToCheck) {
            if (getOptionValue(propertyName, configBeans) != null) {
                throw new ConfigurationException("Found unexpected configuration property '" + propertyName + (message == null ? "'" : "'. Message: " + message));
            }
        }
    }

    /**
     * Returns the value of a config bean with the given id
     *
     * @param id    The id of the config bean
     * @param beans The beans to search through
     * @param <T>   The Type of the config bean
     * @return The value of the config bean with the given id, or null if there is no bean with that value
     */
    @Nullable
    private static <T> T getOptionValue(@NotNull final String id, @NotNull final Collection<ConfigurationBean> beans) {
        T value = null;

        for (ConfigurationBean bean : beans) {
            if (id.equals(bean.getConfigName())) {
                //noinspection unchecked
                value = (T) bean.getConfigValue();
                break;
            }
        }

        return value;
    }

    /**
     * Returns the option in the set with the given id
     *
     * @param id      The id of the option to find
     * @param options The option set to search
     * @return The option with the given id, or null if it doesnt exist
     */
    @Nullable
    private Option getOption(@NotNull final String id, @NotNull final Set<Option> options) {
        for (Option option : options) {
            if (id.equals(option.getId())) {
                return option;
            }
        }
        return null;
    }

    /**
     * Checks if a command exists
     *
     * @param command The command to look for
     * @return True if this is a valid command, false otherwise
     */
    private boolean containsCommand(@NotNull final String command) {
        return Functions.exists(commandMap.keySet(), new Functions.Unary<Boolean, Pair<String, String>>() {
            @Override
            public Boolean call(Pair<String, String> commandPair) {
                return commandPair.getKey().equals(command);
            }
        });
    }

    /**
     * Returns a list of options for the given command
     *
     * @param command The command to get the options for
     * @return The options for the given command
     */
    @NotNull
    private Set<String> getCommandOptions(@NotNull final String command) {
        //use a TreeSet so that it is ordered
        final Set<String> options = new TreeSet<>();
        for (final Pair<String, String> commandPair : commandMap.keySet()) {
            if (commandPair.getKey().equals(command) && commandPair.getValue() != null) {
                options.add(commandPair.getValue());
            }
        }
        return options;
    }

    /**
     * Returns the list of commands available
     *
     * @return The set of commands available
     */
    @NotNull
    public Set<String> getCommands() {
        //use a TreeSet so that it is ordered
        final Set<String> commandSet = new TreeSet<>();
        for (final Pair<String, String> commandPair : commandMap.keySet()) {
            commandSet.add(commandPair.getKey());
        }
        return commandSet;
    }

    /**
     * Create the config beans from the option needed and properties given
     *
     * @param options    The options for the command being executed
     * @param properties The properties given
     * @return The collection of configuration beans
     * @throws ConfigurationException This is thrown if there was some error creating the configuration beans
     */
    @NotNull
    private static List<ConfigurationBean> getConfigBeans(@NotNull final Set<Option> options, @NotNull final Properties properties, boolean includeDefaultsAndNulls) throws ConfigurationException {
        final List<ConfigurationBean> configurationBeans = new ArrayList<>();
        for (final Option option : options) {
            final String name = option.getConfigName();
            final String value = getOptionValue(option, properties, includeDefaultsAndNulls);
            if (!includeDefaultsAndNulls) {
                validate(value, option);
            }
            final ConfigurationBean<String> bean = new ConfigurationBean<>();
            bean.setId(option.getId());
            bean.setFormatter(option.getType().getFormat());
            bean.setConfigName(option.getConfigName());
            if (value != null) {
                try {
                    bean.processConfigValueInput(value);
                } catch (ParseException pe) {
                    throw new ConfigurationException("Unable to parse option value for '" + name + "' value given: '" + value + "'. Message: " + ExceptionUtils.getMessage(pe), pe);
                }
            }
            if (value != null || includeDefaultsAndNulls) {
                configurationBeans.add(bean);
            }
        }
        return configurationBeans;
    }

    @Nullable
    private static String getOptionValue(@NotNull final Option option, @NotNull final Properties properties, boolean includeDefaults) {
        final String value = properties.getProperty(option.getConfigName(), includeDefaults ? option.getConfigValue() : null);
        //process special options
        if (value == null) {
            if ("cluster.host".equals(option.getConfigName())) {
                String hostname = null;
                try {
                    hostname = InetAddress.getLocalHost().getCanonicalHostName();
                } catch (UnknownHostException e) {
                    try {
                        hostname = InetAddress.getLocalHost().getHostName();
                    } catch (UnknownHostException e2) {
                        // fail
                    }
                }
                if (hostname != null && !InetAddressUtil.isValidIpv4Address(hostname) || !InetAddressUtil.isValidIpv6Address(hostname)) {
                    return hostname;
                }
            } else if ("java.path".equals(option.getConfigName())) {
                return SyspropUtil.getProperty("java.home");
            }
        }

        return value;
    }

    /**
     * Validates the option value. This will throw a ConfigurationException if the value is not valid
     *
     * @param value  The value to validate
     * @param option The option to validate
     * @throws ConfigurationException This is thrown if the value is not valid
     */
    private static void validate(final String value, final Option option) throws ConfigurationException {
        if (option.getMinlength() != null && (value == null || value.length() < option.getMinlength())) {
            throw new ConfigurationException("Invalid value for option '" + option.getConfigName() + "' value given: '" + value + "'. Message: length too short, min length is " + option.getMinlength());
        } else if (option.getMaxlength() != null && (value != null && value.length() > option.getMaxlength())) {
            throw new ConfigurationException("Invalid value for option '" + option.getConfigName() + "' value given: '" + value + "'. Message: length too long, max length is " + option.getMaxlength());
        }

        if ((option.getMin() != null || option.getMax() != null) && value != null) {
            try {
                Long numberValue = Long.parseLong(value.trim());
                if (option.getMin() != null && numberValue < option.getMin()) {
                    throw new ConfigurationException("Invalid value for option '" + option.getConfigName() + "' value given: '" + value + "'. Message: value too small, min value is " + option.getMin());
                } else if (option.getMax() != null && numberValue > option.getMax()) {
                    throw new ConfigurationException("Invalid value for option '" + option.getConfigName() + "' value given: '" + value + "'. Message: value too large, max value is " + option.getMax());
                }
            } catch (NumberFormatException nfe) {
                throw new ConfigurationException("Invalid value for option '" + option.getConfigName() + "' value given: '" + value + "'. Could not convert to number. Message: " + ExceptionUtils.getMessage(nfe), nfe);
            }
        }
    }

    //this is the option specified whether the database should be created
    //TODO: This could be used later to support creation of node.properties only
    private static final com.l7tech.config.client.options.Option dbOption = new com.l7tech.config.client.options.Option() {{
        setId("configure-db");
        setGroup("db");
        setType(OptionType.BOOLEAN);
        setOrder(90);
        setName("Configure Database");
        setConfigName("configure.db");
        setConfigValue("true");
        setDescription("Creates the database");
        setPrompt("Create database.");
    }};

    //this is the option specified if the node.properties should be created
    private static final com.l7tech.config.client.options.Option nodeOption = new com.l7tech.config.client.options.Option() {{
        setId("configure-node");
        setGroup("node");
        setType(OptionType.BOOLEAN);
        setOrder(410);
        setName("Configure Node");
        setConfigName("configure.node");
        setConfigValue("true");
        setDescription("Configure the node.properties");
        setPrompt("Create database.");
    }};

    //this is the option specifies the database type
    private static final com.l7tech.config.client.options.Option dbTypeOption = new com.l7tech.config.client.options.Option() {{
        setId("configure-db-type");
        setGroup("db");
        setType(OptionType.TEXT);
        setOrder(91);
        setName("Configure Database Type");
        setConfigName("database.type");
        setConfigValue("mysql");
        setDescription("The database type, either 'mysql' or 'embedded'");
        setPrompt("Database type. Either 'mysql' or 'embedded'");
    }};

    /**
     * Returns the OptionSet for the create command
     *
     * @return The option set for the create command
     * @throws ConfigurationException This is thrown if there is some error attempting to read the configuration xml file
     */
    @NotNull
    private static OptionSet getOptionSet() throws ConfigurationException {
        final OptionSet optionSet;
        optionSet = ConfigurationFactory.newConfiguration(HeadlessConfig.class, "configTemplates/GatewayApplianceConfiguration.xml");
        //optionSet.getOptions().add(dbOption);
        optionSet.getOptions().add(nodeOption);
        optionSet.getOptions().add(dbTypeOption);
        return optionSet;
    }
}
