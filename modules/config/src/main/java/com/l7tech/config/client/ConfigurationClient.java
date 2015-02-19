package com.l7tech.config.client;

import com.l7tech.config.client.beans.ConfigurationBean;
import com.l7tech.config.client.beans.ConfigurationBeanProvider;
import com.l7tech.config.client.options.Option;
import com.l7tech.config.client.options.OptionSet;
import com.l7tech.util.ExceptionUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Text UI configuration client.
 * 
 * <p>Performs a given command for a set of options and configuration
 * provider.</p>
 * 
 * @author steve
 */
public class ConfigurationClient extends Interaction {

    //- PUBLIC

    /**
     * Create a new configuration client.
     * 
     * @param provider The configuration provider
     * @param optionSet The configuration options
     * @param command The command to perform
     */
    public ConfigurationClient( final ConfigurationBeanProvider provider,
                                final OptionSet optionSet,
                                final String command ) throws ConfigurationException {
        this.provider = provider;
        this.optionInit = provider instanceof OptionInitializer ? (OptionInitializer) provider : null;
        this.optionFilter = provider instanceof OptionFilter ? (OptionFilter) provider : null;
        this.optionSet = optionSet;
        this.configBeanList = provider.loadConfiguration();
        this.command = validateCommand(command);
        filterOptions(); // do filtering after the provider has loaded the configuration
    }

    /**
     * Perform the interaction for this clients command.
     * 
     * @return true if successful
     */
    @Override
    public boolean doInteraction() throws IOException {
        boolean success = false;
        boolean reconfigure;
        Map<String,ConfigurationBean> configBeans = new TreeMap<String,ConfigurationBean>();
        initConfig(optionSet, configBeans, configBeanList);
        try {
            do {
                reconfigure = false;
                boolean store = false;
                if (COMMAND_WIZARD.equals(command)) {
                    store = doWizard(optionSet, configBeans);
                } else if (COMMAND_EDIT.equals(command)) {
                    store = doEdit(optionSet, configBeans);
                } else if (COMMAND_SHOW.equals(command)) {
                    doShow(optionSet, configBeans, false, true);
                    promptContinue();
                } else if (COMMAND_HEADLESS.equals(command)) {
                    // Run a headless session
                    store = doHeadless( optionSet, configBeans );

                    try {
                        if ( store ) {
                            provider.storeConfiguration( configBeans.values() );
                            return true;
                        }
                    } catch ( ConfigurationException e ) {
                        System.err.println( "ERROR: " + ExceptionUtils.getMessage( e ) );
                        e.printStackTrace( System.err );
                    }
                    return false;
                }

                if (store) {
                    System.out.println(bundle.getString("message.results.pleasewait"));
                    try {
                        provider.storeConfiguration(configBeans.values());
                        System.out.println(bundle.getString("message.results.success"));
                        success = true;
                    } catch (ConfigurationException ce) {
                        System.out.println(bundle.getString("message.results.failure"));
                        System.out.println(ce.getMessage());

                        //prompt if user would like to reconfigure to fix the problem 
                        reconfigure = promptConfirm(bundle.getString("message.reconfigure"), true);
                    }
                } else {
                    success = true;
                }

            } while (reconfigure);

        } catch ( WizardNavigationException wne ) {
            logger.log(Level.WARNING, "Navigation error during configuration", wne );
        }

        return success;
    }

    private boolean doHeadless( OptionSet optionSet, Map<String, ConfigurationBean> configBeans ) throws IOException {
        Interaction wizard = new HeadlessInteraction( optionSet, configBeans );
        boolean success = wizard.doInteraction();
        wizard.close();
        return success;
    }

    public static String getDefaultCommand() {
        return COMMAND_AUTO;
    }
    
    public static boolean isValidCommand( final String command ) {
        return COMMANDS.contains(command);                
    }
    
    //- PROTECTED
        
    @Override
    protected boolean handleInput( final String inputText ) {
        boolean handled;
        
        handled = handleBackwardsNavigation( inputText );
        
        if ( !handled ) {
            handled = super.handleInput(inputText);
        }
        
        return handled;
    }    
    
    //- PRIVATE

    private static final Logger logger = Logger.getLogger(ConfigurationClient.class.getName());
    private static final ResourceBundle bundle = ResourceBundle.getBundle(ConfigurationClient.class.getName());
    
    private static final String COMMAND_AUTO = "auto"; // wizard if not already configured, else edit
    private static final String COMMAND_WIZARD = "wizard";
    private static final String COMMAND_EDIT = "edit";
    private static final String COMMAND_SHOW = "show";
    private static final String COMMAND_HEADLESS = "headless";
    private static final Collection COMMANDS = Collections.unmodifiableCollection(Arrays.asList( COMMAND_AUTO, COMMAND_WIZARD, COMMAND_EDIT, COMMAND_SHOW, COMMAND_HEADLESS ));
    
    private final ConfigurationBeanProvider provider;
    private final OptionInitializer optionInit;
    private final OptionFilter optionFilter;
    private final OptionSet optionSet;
    private final String command;
    private final Collection<ConfigurationBean> configBeanList;
    
    private String validateCommand( final String command ) throws InvalidConfigurationStateException {
        String effectiveCommand = command;
        
        if ( COMMAND_AUTO.equals(command) ) {
            if ( configBeanList.isEmpty() ) {
                effectiveCommand = COMMAND_WIZARD;
            } else {
                effectiveCommand = COMMAND_EDIT;
            }
        } else if ( COMMAND_WIZARD.equals(command) ) {
            if ( !configBeanList.isEmpty() ) {
                throw new InvalidConfigurationStateException( bundle.getString("message.error.configured") );
            }
        } else if ( COMMAND_EDIT.equals(command) ) {
            if ( configBeanList.isEmpty() ) {
                throw new InvalidConfigurationStateException( bundle.getString("message.error.notconfigured") );
            }
        } else if ( COMMAND_SHOW.equals(command) ) {
            if ( configBeanList.isEmpty() ) {
                throw new InvalidConfigurationStateException( bundle.getString("message.error.notconfigured") );
            }
        }     
        
        return effectiveCommand;
    }
    
    private void doShow( final OptionSet os,
                         final Map<String,ConfigurationBean> configBeans,
                         final boolean brief,
                         final boolean updatable ) throws IOException {
        Interaction summary = new SummaryInteraction(os, configBeans, brief, updatable);
        summary.doInteraction();
        summary.close();
    }

    private boolean doEdit( final OptionSet optionSet,
                            final Map<String,ConfigurationBean> configBeans  ) throws IOException {
        boolean done = false;
        while ( !done ) {
            try {
                Interaction edit = new EditInteraction( optionSet, configBeans );
                boolean success = edit.doInteraction();
                edit.close();

                if ( success ) {
                    System.out.println( bundle.getString("message.summary") );

                    doShow(optionSet, configBeans, true, true);
                    promptContinue();
    
                    done = true;
                } else {
                    break;
                }                
            } catch ( WizardNavigationException wne ) {
                // loop
            }
        }
        
        return done;
    }    
    
    private boolean doWizard( final OptionSet optionSet,
                              final Map<String,ConfigurationBean> configBeans  ) throws IOException {
        boolean done = false;
        while ( !done ) {
            try {
                Interaction wizard = new WizardInteraction( optionSet, configBeans );
                wizard.doInteraction();
                wizard.close();

                System.out.println( bundle.getString("message.summary") );

                doShow(optionSet, configBeans, true, false);
                promptContinue();

                done = true;
            } catch ( WizardNavigationException wne ) {
                // loop
            }
        }
        
        return done;
    }

    private void filterOptions() {
        if ( optionFilter != null ) {
            for ( Iterator<Option> optionIterator = optionSet.getOptions().iterator(); optionIterator.hasNext();  ) {
                Option option = optionIterator.next();
                if ( !optionFilter.isOptionActive( optionSet, option ) ) {
                    optionIterator.remove();
                }
            }
        }
    }

    @SuppressWarnings({"unchecked"})
    private void initConfig( final OptionSet os,
                             final Map<String,ConfigurationBean> configBeans,
                             final Collection<ConfigurationBean> configBeanList ) {
        if ( configBeanList != null ) {
            for ( Option option : os.getOptions() ) {
                boolean optionInitialized = false;

                for ( ConfigurationBean configBean : configBeanList ) {
                    if ( option.getConfigName().equals(configBean.getConfigName()) ) {
                        configBean.setId( option.getId() );
                        configBean.setFormatter( option.getType().getFormat() );
                        configBeans.put( option.getId(), configBean);
                        optionInitialized = true;
                        break;
                    }
                }

                if ( !optionInitialized && optionInit != null) {
                    final Object value = optionInit.getInitialValue( option.getConfigName() );
                    if ( value != null ) {
                        ConfigurationBean configBean = new ConfigurationBean();
                        configBean.setConfigName( option.getConfigName() );
                        configBean.setConfigValue( value );
                        configBean.setId( option.getId() );
                        configBean.setFormatter( option.getType().getFormat() );
                        configBeans.put( option.getId(), configBean);
                    }
                }
            }
        }
    }
        
    private boolean handleBackwardsNavigation(final String perhapsNav) {
        boolean handled = false;
        
        if ("<".equals(perhapsNav.trim())) {
            throw new WizardNavigationException();
        }
        
        return handled;
    }    
    
    private static final class WizardNavigationException extends RuntimeException {}    
}
