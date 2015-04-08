package com.l7tech.gateway.config.client;

import com.l7tech.config.client.ConfigurationException;
import com.l7tech.gateway.config.client.beans.HeadlessConfigBean;
import com.l7tech.gateway.config.client.beans.NodeConfigurationBeanProvider;
import com.l7tech.gateway.config.client.beans.NodeManagementApiFactory;
import com.l7tech.gateway.config.client.beans.PropertiesAccessor;
import com.l7tech.util.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Properties;
import java.util.concurrent.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This allows for a non interactive configuration of the gateway.
 */
public class HeadlessConfig {
    private static final Logger logger = Logger.getLogger(HeadlessConfig.class.getName());

    private static final String pcUrl = "https://" + InetAddressUtil.getLocalHostUrlAddress() + ":8765/services/nodeManagementApi";

    public static void main(String[] args) {
        //TODO: possibly use the nodeManagement api directly instead of going through the config bean provider.
        int status = doHeadlessConfig(new HeadlessConfigBean(new NodeConfigurationBeanProvider(new NodeManagementApiFactory(pcUrl)), System.out), args);
        System.exit(status);
    }

    //package protected so that it can be unit tested
    static int doHeadlessConfig(@NotNull final HeadlessConfigBean headlessConfigBean, String[] args) {
        JdkLoggerConfigurator.configure("com.l7tech.logging", "com/l7tech/gateway/config/client/logging.properties", "configlogging.properties");

        //tests for strong crypto, see SSG-4916
        if (!ConfigUtils.isStrongCryptoEnabledInJvm()) {
            return handleException("The Java virtual machine does not have strong cryptography enabled.  The unlimited strength jurisdiction JCE policy files may need to be installed.");
        }

        //get the command, it should the the first argument
        @NotNull final String command;
        if (args.length > 0) {
            command = args[0];
        } else {
            return handleException("Error: Must specify a command. One of: " + headlessConfigBean.getCommands() + "\n" + HeadlessConfigBean.GENERIC_HELP);
        }
        //get any command options, this could be help or template for example
        @Nullable final String option;
        if (args.length > 1) {
            option = args[1];
        } else {
            option = null;
        }

        //read properties read it in a future so that there can be a timeout in case the input stream is not properly closed.
        final FutureTask<Either<Throwable, Properties>> loadProperties = new FutureTask<>(new Callable<Either<Throwable, Properties>>() {
            @Override
            public Either<Throwable, Properties> call() throws Exception {
                final Properties properties = new Properties();
                try {
                    logger.log(Level.FINE, "Loading properties from Standard In");
                    properties.load(System.in);
                    logger.log(Level.FINE, "Loaded properties from Standard In.");
                } catch (Throwable e) {
                    return Either.left(e);
                }
                return Either.right(properties);
            }
        });

        final long loadPropertiesTimeoutMillis = SyspropUtil.getLong("com.l7tech.gateway.config.client.headlessConfig.loadPropertiesTimeout", 10000L);
        try {
            headlessConfigBean.configure(command, option, new PropertiesAccessor() {
                @NotNull
                @Override
                public Properties getProperties() throws ConfigurationException {
                    Executors.newFixedThreadPool(1).execute(loadProperties);
                    final Either<Throwable, Properties> propertiesOrException;
                    try {
                        propertiesOrException = loadProperties.get(loadPropertiesTimeoutMillis, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException | ExecutionException e) {
                        throw new ConfigurationException("Could not load configuration properties from input: " + ExceptionUtils.getMessage(e), e);
                    } catch (TimeoutException e) {
                        throw new ConfigurationException("Could not load configuration properties from input, input string is likely not properly closed: " + ExceptionUtils.getMessage(e), e);
                    }
                    try {
                        return Eithers.extract(propertiesOrException);
                    } catch (Throwable t) {
                        throw new ConfigurationException("Could not load configuration properties from input: " + ExceptionUtils.getMessage(t), t);
                    }

                }
            });
        } catch (ConfigurationException e) {
            return handleException(ExceptionUtils.getMessage(e), e);
        }
        logger.log(Level.INFO, "Command successfully finished!");
        return 0;
    }

    private static int handleException(@NotNull final String message) {
        return handleException(message, null);
    }

    private static int handleException(@NotNull final String message, @Nullable final Throwable throwable) {
        logger.log(Level.WARNING, message, throwable);
        System.out.println(message);
        return 1;
    }
}
