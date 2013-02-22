package com.l7tech.config.client;

import com.l7tech.config.client.beans.ConfigurationBean;
import com.l7tech.config.client.options.Option;
import com.l7tech.config.client.options.OptionSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Console;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.text.Format;
import java.text.ParseException;
import java.util.Map;

/**
 * Extension of Interaction with support for configuration.
 *
 * @author steve
 */
public abstract class ConfigurationInteraction extends Interaction {

    //- PROTECTED

    protected final OptionSet optionSet;
    protected final Map<String,ConfigurationBean> configBeans;

    protected ConfigurationInteraction( final OptionSet optionSet,
                                        final Map<String,ConfigurationBean> configBeans ) {
        super();
        this.optionSet = optionSet;
        this.configBeans = configBeans;
    }

    protected ConfigurationInteraction( final Console console,
                                        final Reader reader,
                                        final Writer writer,
                                        final OptionSet optionSet,
                                        final Map<String, ConfigurationBean> configBeans ) {
        super( console, reader, writer );
        this.optionSet = optionSet;
        this.configBeans = configBeans;
    }

    protected boolean doConfirmOption( final Option option, final String firstValue ) throws IOException {
        boolean confirmed = false;

        print( "Confirm " );
        print( option.getName() );
        print( ": " );

        String value = option.getType().isHidden() ?
            fallbackReadPassword( console, reader ) :
            fallbackReadLine( console, reader );

        if ( firstValue.equals(value) ) {
            confirmed = true;
        }

        return confirmed;
    }

    protected void doOption( final Option option ) throws IOException {
        if ( option.getPrompt() != null ) {
            println();
            println( option.getPrompt() );
        }
        println();
        print( option.getName() );
        String currentValue = getCurrentValue( option );
        if ( currentValue != null ) {
            print( " [" );
            if ( option.getType().isHidden() ) {
                print( "****" );
            } else {
                print( currentValue );
            }
            print( "]: " );
        } else {
            print( ": " );
        }

        boolean read = false;
        while ( !read ) {
            String value = option.getType().isHidden() ?
                fallbackReadPassword( console, reader ) :
                fallbackReadLine( console, reader );
            if ( value.trim().length() == 0 ) {
                if ( currentValue != null ) {
                    if ( hasBeanCurrentValue(option) ) {
                        read = true;
                        continue;
                    } else {
                        value = currentValue;
                    }
                }
            }

            if ( option.getEmptyValue()!=null && option.getEmptyValue().equals(value.trim()) ) {
                read = true;
                configBeans.remove( option.getId() );
                continue;
            }

            boolean set = false;
            if ( option.getType().matches(value) && isValid(value, option) && (!option.isConfirmed() || doConfirmOption( option, value ))) {
                read = true;
                ConfigurationBean<String> bean = new ConfigurationBean<String>();
                bean.setId( option.getId() );
                bean.setFormatter( option.getType().getFormat() );
                bean.setConfigName( option.getConfigName() );
                try {
                    bean.processConfigValueInput( value );
                    set = true;
                    configBeans.put( option.getId(), bean );
                } catch ( ParseException pe ) {
                    // show invalid prompt
                }
            }

            if ( !set ) {
                print("Invalid value, please try again: ");
            }
        }
    }

    protected boolean isValid( final String value, final Option option ) {
        boolean valid = true;

        if ( option.getMinlength() != null && (value==null || value.length() < option.getMinlength())  ) {
            valid = false;
        } else if ( option.getMaxlength() != null && (value!=null && value.length() > option.getMaxlength()) ){
            valid = false;
        }

        if ( ( option.getMin() != null || option.getMax() != null ) && value != null ) {
            try {
                Long numberValue = Long.parseLong( value.trim() );
                if ( option.getMin() != null && numberValue < option.getMin() ) {
                    valid = false;
                } else if ( option.getMax() != null && numberValue > option.getMax() ) {
                    valid = false;
                }
            } catch ( NumberFormatException nfe ) {
                //
            }
        }

        return valid;
    }

    protected boolean hasBeanCurrentValue( Option option ) {
        boolean hasValue = false;

        ConfigurationBean configBean = configBeans.get( option.getId() );
        if ( configBean != null && configBean.getConfigValue() != null ) {
            hasValue = true;
        }

        return hasValue;
    }

    protected String getCurrentValue( Option option ) {
        String value = null;
        ConfigurationBean configBean = configBeans.get(option.getId());
        if ( configBean != null ) {
            Object valueObj = configBean.getConfigValue();

            if ( valueObj instanceof String ) {
                value = (String) valueObj;
            } else if ( valueObj != null ) {
                Format format = configBean.getFormatter();
                if ( format == null ) format = option.getType().getFormat(); // fallback to option format
                if ( format != null ) {
                    value = format.format( valueObj );
                } else {
                    value = valueObj.toString();
                }
            }
        }

        if ( value == null ) {
            value = option.getConfigValue();
            Format format = option.getType().getFormat();
            if ( format != null && value != null ) {
                // round trip to normalize format
                try {
                    value = format.format( format.parseObject(value) );
                } catch ( ParseException pe ) {
                    // use existing format                    
                }
            }
        }

        return value;
    }

    protected boolean isOptionGroupValid( String groupId ) {
        return isOptionGroupValid(groupId, null);
    }

    /**
     * Determine if all Options in an OptionGroup have an associated ConfigurationBean.
     *
     * @param groupId the group id of the OptionGroup to evaluate.
     * @param optionFilter an optional OptionFilter which will be used to determine if a particular Option in an OptionSet should be ignored when validating.
     * @return true if every Option in the OptionGroup that hasn't been filtered by the OptionFilter has an associated ConfigurationBean.
     */
    protected boolean isOptionGroupValid(final String groupId , @Nullable OptionFilter optionFilter) {
        boolean valid = true;

        for ( Option option : optionSet.getOptionsForGroup(groupId) ) {
            if (optionFilter == null || optionFilter.isOptionActive(optionSet, option)) {
                ConfigurationBean bean = configBeans.get( option.getId() );
                //
                // Currently checking emptyValue to allow for some config values to be null.
                // May be better to have a separate property for "nullable"
                //
                if ( (bean == null || bean.getConfigValue() == null) && option.getEmptyValue()==null ) {
                    valid = false;
                    break;
                }
            }
        }

        return valid;
    }
}
