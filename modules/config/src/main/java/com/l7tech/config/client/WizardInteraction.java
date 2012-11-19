package com.l7tech.config.client;

import com.l7tech.config.client.beans.ConfigurationBean;
import com.l7tech.config.client.options.Option;
import com.l7tech.config.client.options.OptionGroup;
import com.l7tech.config.client.options.OptionSet;

import java.io.Console;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Interaction that follows a Wizard format.
 * 
 * @author steve
 */
public class WizardInteraction extends ConfigurationInteraction {

    //- PUBLIC
    
    public WizardInteraction( final Console console,
                              final Reader reader,
                              final Writer writer,
                              final OptionSet optionSet,
                              final Map<String,ConfigurationBean> configBeans ) {
        super( console, reader, writer, optionSet, configBeans );
    }
    
    public WizardInteraction( final OptionSet optionSet,
                              final Map<String, ConfigurationBean> configBeans ) {
        super( optionSet, configBeans );
    }

    @Override
    public boolean doInteraction() throws IOException {
        if ( optionSet.getPrompt() != null ) {
            println( optionSet.getPrompt() );
        }
        println();
        String group = null;
        final Set<String> skipGroupIds = new HashSet<String>();
        Option[] options = optionSet.getOptions().toArray( new Option[optionSet.getOptions().size()] );
        for ( int i=0; i<options.length; i++ ) {
            Option option = options[i];
            currentIndex = i;
            
            // check if we are skipping options in the group
            if ( skipGroupIds.contains( option.getGroup() ) ) {
                continue;
            }
            
            try {
                // process group for option
                if ( option.getGroup() != null && !option.getGroup().equals(group) ) {
                    group = option.getGroup();
                    OptionGroup optionGroup = optionSet.getOptionGroup( option.getGroup()  );
                    if ( optionGroup != null ) {
                        if ( !optionGroup.isRequired() && skipGroupIds.contains( optionGroup.getGroup() ) ) {
                            // then the parent group was skipped so skip this one too
                            skipGroupIds.add( optionGroup.getId() );
                            continue;
                        } else {
                            skipGroupIds.remove( optionGroup.getId() );
                        }

                        if ( optionGroup.getPrompt() != null ) {
                            println( optionGroup.getPrompt() );
                        }

                        if ( !optionGroup.isRequired() && !isOptionGroupValid( optionGroup.getId() ) ) {
                            println();
                            println("This step is optional.  Enter \"Yes\" to continue or \"No\" to skip.");
                            println();

                            if ( !promptConfirm( optionGroup.getDescription() + "?", optionGroup.isOptionalDefault() ) ) {
                                skipGroupIds.add( optionGroup.getId() );
                                previousOptions.push(i);
                                continue;
                            } else {
                                skipGroupIds.remove( optionGroup.getId() );
                            }
                        }
                    } else {
                        logger.log( Level.WARNING, "Referenced OptionGroup was not found ''{0}''.", option.getGroup() );
                    }
                }

                doOption( option );
                
                previousOptions.push(i);
            } catch ( WizardNavigationException wne ) {    
                if ( !previousOptions.isEmpty() ) {
                    i = previousOptions.pop() - 1;
                } else {
                    i = -1;
                }
            }
        }
        
        return true;
    }

    @Override
    protected boolean doConfirmOption( final Option option, final String firstValue ) throws IOException {
        boolean confirmed;
        
        // if user goes back while confirming they should go back to initial prompt
        previousOptions.push(currentIndex);
        
        confirmed = super.doConfirmOption( option, firstValue );
        
        previousOptions.pop();
        
        return confirmed;
    }
    
    @Override
    protected boolean handleInput( final String inputText ) {
        boolean handled ;
        
        handled = handleBackwardsNavigation( inputText );
        
        if ( !handled ) {
            handled = super.handleInput(inputText);
        }
        
        return handled;
    }
    
    //- PRIVATE
    
    private static final Logger logger = Logger.getLogger(WizardInteraction.class.getName());
    
    private Deque<Integer> previousOptions = new ArrayDeque<Integer>();
    private int currentIndex = 0;
    
    private boolean handleBackwardsNavigation(final String perhapsNav) {
        boolean handled = false;
        
        if ("<".equals(perhapsNav.trim())) {
            throw new WizardNavigationException();
        }
        
        return handled;
    }    
    
    private static final class WizardNavigationException extends RuntimeException {}
}
