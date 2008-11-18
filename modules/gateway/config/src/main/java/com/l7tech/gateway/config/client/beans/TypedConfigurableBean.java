package com.l7tech.gateway.config.client.beans;

import com.l7tech.config.client.ConfigurationException;
import com.l7tech.config.client.options.OptionType;
import com.l7tech.util.ExceptionUtils;

import java.text.ParseException;
import java.text.Format;
import java.util.regex.Pattern;

/**
 *
 */
public class TypedConfigurableBean<T> extends EditableConfigurationBean<T>  {

    //- PUBLIC

    public TypedConfigurableBean( final String id,
                                  final String intro,
                                  final String validationFailureMessage,
                                  final T defaultValue,
                                  final T currentValue,
                                  final OptionType type ) {
        super( id, intro, defaultValue );
        if ( currentValue != null ) {
            this.setConfigValue( currentValue );
        }
        this.validationFailureMessage = validationFailureMessage;
        this.type = type;
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public T parse( final String userInput ) throws ConfigurationException {
        T result;

        if ( !Pattern.matches( type.getDefaultRegex(), userInput ) ) {
            throw new ConfigurationException( validationFailureMessage );            
        }

        try {
            result = (T) type.getFormat().parseObject( userInput );
        } catch (ParseException e) {
            throw new ConfigurationException( ExceptionUtils.getMessage(e), e ); 
        }

        return result;
    }

    @Override
    public String getShortValueDescription() {
        StringBuilder description = new StringBuilder();

        Object value;
        if ( this.getConfigValue() == null ) {
            value = this.getDefaultValue();
        } else {
            value = this.getConfigValue();
        }

        Format format = type.getFormat();
        if ( format != null && value != null ) {
            description.append( format.format( value ) );
        } else if ( value != null ) {
            description.append( value.toString() );    
        }

        return description.toString();
    }

    @Override
    public String getDisplayValue() {
        return getShortValueDescription();
    }

    //- PRIVATE

    private final String validationFailureMessage;
    private final OptionType type;
}
