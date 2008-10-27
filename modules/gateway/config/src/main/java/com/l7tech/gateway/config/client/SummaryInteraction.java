package com.l7tech.gateway.config.client;

import com.l7tech.gateway.config.client.beans.ConfigurationBean;
import com.l7tech.gateway.config.client.options.Option;
import com.l7tech.gateway.config.client.options.OptionGroup;
import com.l7tech.gateway.config.client.options.OptionSet;
import java.io.Console;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Map;

/**
 * Interaction that displays a summary for a set of beans.
 * 
 * @author steve
 */
public class SummaryInteraction extends ConfigurationInteraction {

    //- PUBLIC
    
    public SummaryInteraction( final Console console,
                               final Reader reader,
                               final Writer writer,
                               final OptionSet optionSet,
                               final Map<String,ConfigurationBean> configBeans,
                               final boolean brief, 
                               final boolean updatable ) {
        super( console, reader, writer, optionSet, configBeans );
        this.brief = brief;
        this.updatable = updatable;
    }
    
    public SummaryInteraction( final OptionSet optionSet,
                               final Map<String,ConfigurationBean> configBeans,
                               final boolean brief, 
                               final boolean updatable ) {
        super( optionSet, configBeans );
        this.brief = brief;
        this.updatable = updatable;
    }

    @Override
    public boolean doInteraction() throws IOException {
        if ( !brief ) {
            if ( optionSet.getDescription() != null ) {
                println( optionSet.getDescription() );
            }
            println();

            println( "Configuration:" );
        }
        
        println( getConfigurationSummary() );
        
        return true;
    }
       
    //- PRIVATE
    
    /**
     * Show a full or brief summary
     */
    private final boolean brief;    
    
    /**
     * Show only updatable beans in the summary
     */
    private final boolean updatable;    
    
    private String getConfigurationSummary() {
        StringBuilder summary = new StringBuilder();

        String group = null;
        String skipGroupId = null;
        for ( Option option : optionSet.getOptions() ) {
            // check if we are skipping options in the group
            if ( skipGroupId != null && skipGroupId.equals( option.getGroup() ) ) {
                continue;
            } else {
                skipGroupId = null;
            }
            
            if ( option.getType().isHidden() ) {
                continue;
            }

            if ( updatable && !option.isUpdatable() ) {
                continue;
            }
            
            if ( option.getGroup() != null && !option.getGroup().equals(group) ) {
                group = option.getGroup();
                
                OptionGroup optionGroup = getOptionGroup( option.getGroup()  );
                if ( optionGroup != null ) {
                    if ( !optionGroup.isRequired() && !isOptionGroupValid( optionGroup.getId() )) {
                        skipGroupId = group;
                        continue;
                    }
                    
                    if ( optionGroup.getDescription() != null ) {
                        summary.append( "\n" );
                        summary.append( "  " );
                        summary.append( optionGroup.getDescription() );
                        summary.append( "\n" );
                    }
                }
            }

            summary.append( "    " );
            summary.append( option.getName() );
            summary.append( " = " );
            String currentValue = getCurrentValue( option );
            if ( currentValue != null ) {
                summary.append( currentValue );
            }
            summary.append( "\n" );
        }

        return summary.toString();
    }
    
}
