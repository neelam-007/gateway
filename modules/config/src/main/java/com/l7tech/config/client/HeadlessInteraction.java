package com.l7tech.config.client;

import com.l7tech.config.client.beans.ConfigurationBean;
import com.l7tech.config.client.options.Option;
import com.l7tech.config.client.options.OptionSet;
import com.l7tech.config.client.options.OptionType;
import com.l7tech.util.ExceptionUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.text.ParseException;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Headless configuration mode that reads an answers file from STDIN.
 */
public class HeadlessInteraction extends ConfigurationInteraction {

    //this is the option to allow for creation of the database only without other configuration files
    private final Option dbOnlyOption = new Option() {{
        setId("db-only");
        setGroup("headless");
        setType(OptionType.BOOLEAN);
        setOrder(500);
        setName("DB Only");
        setConfigName("configure.dbonly");
        setConfigValue("false");
        setDescription("True creates database only, false will also create other configuration files.");
        setPrompt("Only create database.");
    }};

    public HeadlessInteraction( final OptionSet optionSet,
                            final Map<String,ConfigurationBean> configBeans )
    {
        // Headless mode will never try to use console-specific features -- it will only read from STDIN
        super( null,
                new InputStreamReader( System.in ){@Override public void close() throws IOException {}},
                new OutputStreamWriter( System.out ){@Override public void close() throws IOException {}},
                optionSet,
                configBeans );
        //need to add a headless only config option here:
        optionSet.getOptions().add(dbOnlyOption);
    }

    @Override
    public boolean doInteraction() throws IOException {
        for (;;) {
            String line = fallbackReadLine( console, reader );

            if ( null == line || ".".equals( line ) ) {
                return false;
            } else if ( "help".equals( line ) ) {
                // Emit answers file template
                writer.println( "create-db" );
                Properties properties = new Properties();
                Set<Option> options = optionSet.getOptions();
                for ( Option option : options ) {
                    String name = option.getConfigName();
                    String value = option.getConfigValue();
                    if ( value == null )
                        value = "null";
                    properties.put( name, value );
                }
                properties.store( writer, "Headless config create-db answers file" );
                writer.println( "." );
                writer.flush();
                return false;

            } else if ( "create-db".equals( line ) ) {
                // Read answers file from STDIN and process it
                StringBuilder props = new StringBuilder();
                boolean done = false;
                do {
                    line = fallbackReadLine( console, reader );
                    if ( ".".equals( line ) ) {
                        done = true;
                    } else {
                        props.append( line ).append( "\n" );
                    }
                } while ( !done );
                Properties properties = new Properties();
                properties.load( new StringReader( props.toString() ) );

                Set<Option> options = optionSet.getOptions();
                for ( Option option : options ) {
                    String name = option.getConfigName();
                    String value = properties.getProperty( name );
                    if ( "null".equals( value ) ) {
                        configBeans.remove( option.getId() );
                    } else if ( value != null ) {
                        ConfigurationBean<String> bean = new ConfigurationBean<>();
                        bean.setId( option.getId() );
                        bean.setFormatter( option.getType().getFormat() );
                        bean.setConfigName( option.getConfigName() );
                        try {
                            bean.processConfigValueInput( value );
                            configBeans.put( option.getId(), bean );
                        } catch ( ParseException pe ) {
                            System.err.println("Unable to parse option value for '" + name + "' value given: '" + value + "'. Message: " + ExceptionUtils.getMessage( pe ));
                            return false;
                        }
                    }
                }
                return true;
            } else {
                System.err.println( "Unrecognized command: must be one of: help, quit, create-db" );
                return false;
            }
        }
    }
}
