package com.l7tech.util;

import java.util.Map;

/**
 * Configuration wrapper that switches value overrides based on other property values.
 */
public class SwitchedConfig implements Config {

    //- PUBLIC

    /**
     * Create a new switched configuration.
     *
     * @param config The configuration to delegate to
     * @param switchedProperties The map of switched properties to switches
     * @param enabledOverrides A map of switched properties to values
     * @param disabledOverrides A map of switched properties to values
     */
    public SwitchedConfig( final Config config,
                           final Map<String,String> switchedProperties,
                           final Map<String,Object> enabledOverrides,
                           final Map<String,Object> disabledOverrides ) {
        this.config = config;
        this.switchedProperties = switchedProperties;
        this.enabledOverrides = enabledOverrides;
        this.disabledOverrides = disabledOverrides;
    }

    @Override
    public boolean getBooleanProperty( final String propertyName, final boolean defaultValue ) {
        return getSwitchedValue( Boolean.class, propertyName, config.getBooleanProperty( propertyName, defaultValue ) );
    }

    @Override
    public int getIntProperty( final String propertyName, final int defaultValue ) {
        return getSwitchedValue( Integer.class, propertyName, config.getIntProperty( propertyName, defaultValue ) );
    }

    @Override
    public long getLongProperty( final String propertyName, final long defaultValue ) {
        return getSwitchedValue( Long.class, propertyName, config.getLongProperty( propertyName, defaultValue ) );
    }

    @Override
    public String getProperty( final String propertyName, final String defaultValue ) {
        return getSwitchedValue( String.class, propertyName, config.getProperty( propertyName, defaultValue ) );
    }

    @Override
    public long getTimeUnitProperty( final String propertyName, final long defaultValue ) {
        return getSwitchedValue( Long.class, propertyName, config.getTimeUnitProperty( propertyName, defaultValue ) );
    }

    //- PRIVATE

    private final Config config;
    private final Map<String,String> switchedProperties;
    private final Map<String,Object> enabledOverrides;
    private final Map<String,Object> disabledOverrides;

    @SuppressWarnings({ "unchecked" })
    private <T> T getSwitchedValue( final Class<T> type,
                                    final String propertyName,
                                    final T defaultValue ) {
        T result = defaultValue;

        final String switchName = switchedProperties.get( propertyName );
        if ( switchName != null ) {
            final Map<String,Object> overrideMap =
                    config.getBooleanProperty( switchName, false ) ?
                            enabledOverrides :
                            disabledOverrides;

            final Object switchedValue = overrideMap.get( propertyName );
            if ( type.isInstance( switchedValue ) ) {
                result = (T) switchedValue;
            }
        }

        return result;
    }
}
