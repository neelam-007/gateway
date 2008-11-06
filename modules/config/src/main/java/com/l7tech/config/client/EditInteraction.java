package com.l7tech.config.client;

import com.l7tech.config.client.beans.ConfigurationBean;
import com.l7tech.config.client.options.Option;
import com.l7tech.config.client.options.OptionGroup;
import com.l7tech.config.client.options.OptionSet;

import java.io.Console;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Interaction for menu driven edit of existing data.
 * 
 * @author steve
 */
public class EditInteraction extends ConfigurationInteraction {

    public EditInteraction( final Console console,
                            final Reader reader,
                            final Writer writer,
                            final OptionSet optionSet,
                            final Map<String,ConfigurationBean> configBeans ) {
        super( console, reader, writer, optionSet, configBeans );
    }
    
    public EditInteraction( final OptionSet optionSet,
                               final Map<String,ConfigurationBean> configBeans ) {
        super( optionSet, configBeans );
    }

    @Override
    public boolean doInteraction() throws IOException {
        if ( optionSet.getPrompt() != null ) {
            println( optionSet.getPrompt() );
        }
        println();
        
        boolean success = false;
        boolean edit = true;
        while( edit ) {
            int count = 1;
            Map<Integer,String> optionMap = new HashMap<Integer,String>();
            println( "Select option to configure:" );
            
            String group = null;
            for ( Option option : optionSet.getOptions() ) {
                OptionGroup optionGroup;

                if ( !option.isUpdatable() ) {
                    continue;
                }
                
                // process group for option
                if ( option.getGroup() != null && !option.getGroup().equals(group) ) {
                    optionGroup = optionSet.getOptionGroup( option.getGroup()  );
                } else {
                    // we've already processed this group so skip it
                    continue;
                }
                
                String id;
                String name;
                if ( optionGroup != null ) {
                    id = optionGroup.getId();
                    name = optionGroup.getDescription();
                    group = id;
                } else {
                    id = option.getId();
                    name = option.getName();
                    group = null;
                }
                
                optionMap.put(count, id);
                print( count++ );
                print( ") " );                
                println( name );
            }
            println();
            println( "S) Save and exit." );
            println( "X) Exit without saving." );
            print( "\nSelect: " );

            try {
                String selected = fallbackReadLine( console, reader );
                int selectedValue = parseOption(selected);
                if ( OPTION_ABORT == selectedValue ) { 
                    edit = false; 
                } else if ( OPTION_SAVE == selectedValue ) {
                    success = true;
                    edit = false; 
                } else {
                    String selectedOption = optionMap.get(selectedValue);
                    if ( selectedOption == null ) continue;
                    
                    Set<Option> optionSelection = optionSet.getOptions(selectedOption);
                    OptionGroup optionGroup = null;
                    if ( optionSelection.isEmpty() ) {
                        optionGroup = optionSet.getOptionGroup(selectedOption);
                        optionSelection = optionSet.getOptionsForGroup(selectedOption);
                    }

                    if ( optionGroup != null && !optionGroup.isRequired() && optionGroup.isDeletable() && isOptionGroupValid( optionGroup.getId() ) ) {
                        // allow deletion if currently configured but optional
                        print("Configuration of ");
                        print( optionGroup.getDescription() );
                        println(" is optional.");
                        println();

                        if ( promptConfirm( "Remove configuration for " + optionGroup.getDescription() + "?", false ) ) {
                            for ( Option option : optionSelection ) {
                                configBeans.remove( option.getId() );
                            }
                            continue;
                        }                        
                    }
                    
                    Option[] options = optionSelection.toArray( new Option[optionSelection.size()] );
                    for ( int i=0; i<options.length; i++ ) {
                        Option option = options[i];
                    
                        if ( !option.isUpdatable() ) {
                            continue;
                        }
                        
                        try {
                            doOption( option );
                        } catch ( WizardNavigationException wne ) {    
                            if ( i > 0 ) {
                                i = i - 2;
                            } else {
                                throw wne;
                            }
                        }
                    }                     
                }
            } catch ( WizardNavigationException wne ) {
                // back to main menu
            }
        }        
        
        return success;
    }

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
    
    private static final int OPTION_ABORT = -1;
    private static final int OPTION_SAVE = -2;
    
    private int parseOption( final String selected ) {
        int option = 0;

        if ( selected.trim().equalsIgnoreCase("x") ) {
            option = OPTION_ABORT;
        } else if ( selected.trim().equalsIgnoreCase("s") ) {
            option = OPTION_SAVE;
        } else {
            try {
                option = Integer.parseInt(selected);
            } catch( NumberFormatException nfe ) {
                // ok
            }
        }

        return option;
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
