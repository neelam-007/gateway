package com.l7tech.gateway.config.client.beans;


import com.l7tech.config.client.ConfigurationException;
import com.l7tech.config.client.ConfigurationFactory;
import com.l7tech.config.client.beans.ConfigurationBean;
import com.l7tech.config.client.options.Option;
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

/**
 * This class will configure the gateway from a properties file.
 */
public class HeadlessConfigBean {

    //The node configuration provider
    //TODO: should this be used or should a NodeManagementApi be used directly?
    private final NodeConfigurationBeanProvider provider;
    //This is the stream to print any output or error message info to.
    private final PrintStream printStream;

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
     * @param subCommand The configuration sub command
     * @param properties The properties accessor to use to configure the gateway with
     * @throws ConfigurationException This is thrown if there was an error configuring the gateway
     */
    public void configure(@NotNull final String command, @Nullable final String subCommand, @NotNull final PropertiesAccessor properties) throws ConfigurationException {
        //find the command to configure the gateway with
        final Functions.UnaryVoidThrows<PropertiesAccessor, Throwable> commandFunction = commandMap.get(new Pair<>(command, subCommand));

        if (commandFunction == null) {
            if (!containsCommand(command)) {
                throw new ConfigurationException("Unknown command '" + command + "'. Expected one of: " + getCommands());
            } else {
                throw new ConfigurationException("Unknown sub-command '" + subCommand + "'. Expected one of: " + getSubCommands(command));
            }
        } else {
            try {
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
                    printStream.println("This is a tool to headlessly configure the gateway. It takes a properties file as input on the input stream. The specific configuration is specified as an argument to this command. Possible commands are:");
                    printStream.println(" 'create' ");
                    printStream.println("  - Create a gateway node, including the database and the gateway node properties file.");
                    printStream.println(" 'help' ");
                    printStream.println("  - This shows this help file.");
                    printStream.println("");
                    printStream.println("Most commands will also have a sub-command, it is specified as the second command line argument. For example most commands will have a help sub-command to print out command specific help.");
                    printStream.println("");
                    printStream.println("Example Usages:");
                    printStream.println("Configures a new gateway node:");
                    printStream.println("cat create-node.properties | ssgconfig-headless create");
                    printStream.println("");
                    printStream.println("Print out the create command help:");
                    printStream.println("ssgconfig-headless create help");
                    printStream.println("");
                    printStream.println("Prints out create command template properties:");
                    printStream.println("ssgconfig-headless create template");
                    printStream.flush();
                }
            })
            .put(new Pair<>("create", "template"), new Functions.UnaryVoidThrows<PropertiesAccessor, Throwable>() {
                @Override
                public void call(PropertiesAccessor propertiesAccessor) throws ConfigurationException {
                    final OptionSet optionSet = getOptionSet();
                    final Set<Option> options = optionSet.getOptions();
                    final Collection<ConfigurationBean> configBeans = getConfigBeans(options, propertiesAccessor.getProperties(), true);
                    printStream.println("#Headless config create template properties file");
                    for (final ConfigurationBean configurationBean : configBeans) {
                        final String name = configurationBean.getConfigName();
                        final Object value = configurationBean.getConfigValue();
                        if (value == null) {
                            printStream.println("#" + name + "=");
                        } else {
                            printStream.println(name + "=" + value);
                        }
                    }
                    printStream.flush();
                }
            })
            .put(new Pair<>("create", "help"), new Functions.UnaryVoidThrows<PropertiesAccessor, Throwable>() {
                @Override
                public void call(PropertiesAccessor propertiesAccessor) throws ConfigurationException {
                    printStream.println("This command will create a database and the node.properties file. Help to be completed!");
                    printStream.flush();
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
                    final String databaseType = getOption("database.type", configBeans);
                    if(!"mysql".equals(databaseType) && !"derby".equals(databaseType) ){
                        throw new ConfigurationException("Unknown database type '"+databaseType+"'. Expected one of: 'mysql' or 'derby'");
                    }
                    if("mysql".equals(databaseType)) {
                        checkNotNull(configBeans, Arrays.asList("database.host", "database.port", "database.name", "database.user", "database.pass", "database.admin.user", "database.admin.pass", "admin.user", "admin.pass", "cluster.host"));
                    } else {
                        //this means its derby
                        checkNotNull(configBeans, Arrays.asList("admin.user", "admin.pass", "cluster.host"));
                        //setting the database.host to null will configure a derby database
                        ConfigurationBean databaseHostConfigBean = getConfigBean("database.host", configBeans);
                        if(databaseHostConfigBean != null){
                            databaseHostConfigBean.setConfigValue(null);
                        }
                    }
                    Boolean configureNode = getOption("configure.node", configBeans);
                    if (configureNode == null || configureNode) {
                        //validate the node options
                        checkNotNull(configBeans, Arrays.asList("cluster.pass"));
                    }
                    provider.storeConfiguration(configBeans);

                    printStream.println("Configuration Successful");
                }
            })
            .map();

    private ConfigurationBean getConfigBean(String id, Collection<ConfigurationBean> configBeans) {
        for (ConfigurationBean bean : configBeans) {
            if (id.equals(bean.getConfigName())) {
                return bean;
            }
        }
        return null;
    }

    private void checkNotNull(Collection<ConfigurationBean> configBeans, List<String> propertiesToCheck) throws ConfigurationException {
        for(String propertyName : propertiesToCheck){
            if(getOption(propertyName, configBeans) == null){
                throw new ConfigurationException("Missing configuration property: " + propertyName);
            }
        }
    }

    /**
     * Returns th valid of a config bean with the given id
     *
     * @param id    The id of the config bean
     * @param beans The beans to search through
     * @param <T>   The Type of the config bean
     * @return The value of the config bean with the given id, or null if there is no bean with that value
     */
    private static <T> T getOption(final String id, final Collection<ConfigurationBean> beans) {
        T value = null;

        for (ConfigurationBean bean : beans) {
            if (id.equals(bean.getConfigName())) {
                value = (T) bean.getConfigValue();
                break;
            }
        }

        return value;
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
     * Returns a list of sub-commands for the given command
     *
     * @param command The command to get the sub-commands for
     * @return The sub-commands for the given command
     */
    @NotNull
    private Set<String> getSubCommands(@NotNull final String command) {
        //use a TreeSet so that it is ordered
        final Set<String> subCommandSet = new TreeSet<>();
        for (final Pair<String, String> commandPair : commandMap.keySet()) {
            if (commandPair.getKey().equals(command) && commandPair.getValue() != null) {
                subCommandSet.add(commandPair.getValue());
            }
        }
        return subCommandSet;
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
    private static Collection<ConfigurationBean> getConfigBeans(@NotNull final Set<Option> options, @NotNull final Properties properties, boolean includeDefaultsAndNulls) throws ConfigurationException {
        final Collection<ConfigurationBean> configurationBeans = new HashSet<>();
        for (final Option option : options) {
            final String name = option.getConfigName();
            final String value = getOptionValue(option, properties, includeDefaultsAndNulls);
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
            if(value != null || includeDefaultsAndNulls){
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

    //this is the option specified whether the database should be created
    private static final com.l7tech.config.client.options.Option dbOption = new com.l7tech.config.client.options.Option() {{
        setId("configure-db");
        setGroup("headless");
        setType(OptionType.BOOLEAN);
        setOrder(500);
        setName("Configure Database");
        setConfigName("configure.db");
        setConfigValue("true");
        setDescription("True creates database. Setting this to false will not create ");
        setPrompt("Create database.");
    }};

    //this is the option specified if the node.properties should be created
    private static final com.l7tech.config.client.options.Option nodeOption = new com.l7tech.config.client.options.Option() {{
        setId("configure-node");
        setGroup("headless");
        setType(OptionType.BOOLEAN);
        setOrder(501);
        setName("Configure Node");
        setConfigName("configure.node");
        setConfigValue("true");
        setDescription("True configures a new node. Setting this to false will not configure a new node");
        setPrompt("Create database.");
    }};

    //this is the option specifies the database type
    private static final com.l7tech.config.client.options.Option dbTypeOption = new com.l7tech.config.client.options.Option() {{
        setId("configure-db-type");
        setGroup("headless");
        setType(OptionType.TEXT);
        setOrder(502);
        setName("Configure Database Type");
        setConfigName("database.type");
        setConfigValue("mysql");
        setDescription("This is the database type, either 'mysql' or 'derby'");
        setPrompt("Database type. Either 'mysql' or 'derby'");
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
