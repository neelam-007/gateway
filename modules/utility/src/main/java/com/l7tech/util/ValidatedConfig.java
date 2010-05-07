package com.l7tech.util;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Validating configuration wrapper.
 */
public class ValidatedConfig implements Config {

    //- PUBLIC

    public ValidatedConfig( final Config config,
                            final Logger logger ) {
        this( config, logger, new Resolver<String,String>(){
            @Override
            public String resolve( final String key ) {
                return key;
            }
        } );
    }

    public ValidatedConfig( final Config config,
                            final Logger logger,
                            final Resolver<String,String> nameResolver ) {
        this.config = config;
        this.logger = logger;
        this.nameResolver = nameResolver;
    }

    @Override
    public String getProperty( final String propertyName,
                               final String defaultValue ) {
        String value = config.getProperty( propertyName, defaultValue );
        return validateStringProperty( propertyName, resolveName(propertyName), value, defaultValue );
    }

    @Override
    public int getIntProperty( final String propertyName,
                               final int defaultValue ) {
        int value = config.getIntProperty( propertyName, defaultValue );
        return validateIntProperty( propertyName, resolveName(propertyName), value, defaultValue );
    }

    @Override
    public long getLongProperty( final String propertyName,
                                 final long defaultValue ) {
        long value = config.getLongProperty( propertyName, defaultValue );
        return validateLongProperty( propertyName, resolveName(propertyName), value, defaultValue );
    }

    @Override
    public boolean getBooleanProperty( final String propertyName,
                                       final boolean defaultValue ) {
        return config.getBooleanProperty( propertyName, defaultValue );
    }

    @Override
    public long getTimeUnitProperty( final String propertyName,
                                     final long defaultValue ) {
        long value = config.getTimeUnitProperty( propertyName, defaultValue );
        return validateLongProperty( propertyName, resolveName(propertyName), value, defaultValue );
    }

    /**
     * Set the minimum acceptable value for a property.
     *
     * @param propertyName The (unresolved) property name
     * @param value The minimum value.
     */
    public void setMinimumValue( final String propertyName, final Number value ) {
        minimumValues.put( propertyName, value );
    }

    /**
     * Set the maximum acceptable value for a property.
     *
     * @param propertyName The (unresolved) property name
     * @param value The maximum value.
     */
    public void setMaximumValue( final String propertyName, final Number value ) {
        maximumValues.put( propertyName, value );
    }

    /**
     * Set the validation pattern for a property.
     *
     * @param propertyName The (unresolved) property name
     * @param pattern The validation patten to match.
     */
    public void setValidationPattern( final String propertyName, final Pattern pattern ) {
        validationPatterns.put( propertyName, pattern );
    }

    /**
     * Set the validation message for a property.
     *
     * @param propertyName The (unresolved) property name
     * @param message The message for validation failures.
     */
    public void setValidationMessage( final String propertyName, final String message ) {
        validationMessages.put( propertyName, message );
    }

    //- PROTECTED

    protected String validateStringProperty( final String name,
                                             final String resolvedName,
                                             final String value,
                                             final String defaultValue ) {
        final String validatedValue;

        final Pattern pattern = validationPatterns.get( name );
        if ( value!=null && (pattern == null || pattern.matcher( value ).matches()) ) {
            validatedValue = value;
        } else {
            validatedValue = defaultValue;
            invalidProperty( name, resolvedName, value, defaultValue );
        }

        return validatedValue;
    }

    protected int validateIntProperty( final String name,
                                       final String resolvedName,
                                       final int value,
                                       final int defaultValue ) {
        final int validatedValue;

        final Number minimum = minimumValues.get( name );
        final Number maximum = maximumValues.get( name );
        if ( (minimum == null || minimum.intValue() <= value) &&
             (maximum == null || maximum.intValue() >= value) ) {
            validatedValue = value;
        } else {
            validatedValue = defaultValue;
            invalidProperty( name, resolvedName, value, defaultValue );
        }

        return validatedValue;
    }

    protected long validateLongProperty( final String name,
                                         final String resolvedName,
                                         final long value,
                                         final long defaultValue ) {
        final long validatedValue;

        final Number minimum = minimumValues.get( name );
        final Number maximum = maximumValues.get( name );
        if ( (minimum == null || minimum.longValue() <= value) &&
             (maximum == null || maximum.longValue() >= value) ) {
            validatedValue = value;
        } else {
            validatedValue = defaultValue;
            invalidProperty( name, resolvedName, value, defaultValue );
        }

        return validatedValue;
    }

    protected String resolveName( final String name ) {
        return nameResolver.resolve( name );
    }

    protected void invalidProperty( final String name,
                                    final String resolvedName,
                                    final Object value,
                                    final Object defaultValue ) {
        final String validationMessage = validationMessages.get( name );
        if ( validationMessage != null ) {
            logger.warning( validationMessage );
        } else {
            logger.warning( "Invalid value for property '" + resolvedName + "' ('"+value+"'), using default value '"+defaultValue+"'." );
        }
    }

    //- PRIVATE

    private final Config config;
    private final Logger logger;
    private final Resolver<String,String> nameResolver;
    private final Map<String,Number> minimumValues = new HashMap<String,Number>();
    private final Map<String,Number> maximumValues = new HashMap<String,Number>();
    private final Map<String,Pattern> validationPatterns = new HashMap<String,Pattern>();
    private final Map<String,String> validationMessages = new HashMap<String,String>();
}
