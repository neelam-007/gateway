package com.l7tech.gateway.config.client.beans.trust;

import com.l7tech.config.client.ConfigurationException;
import com.l7tech.gateway.config.client.beans.ConfigResult;
import com.l7tech.gateway.config.client.beans.ConfigurationContext;
import com.l7tech.gateway.config.client.beans.EditableConfigurationBean;
import com.l7tech.util.TextUtils;

import java.util.regex.Pattern;

/**
 * Configuration bean for a partial thumbprint of the trusted certificate
 */
public class TrustedCertThumbprint extends EditableConfigurationBean<String> {
    private static final Pattern hexPattern = Pattern.compile( "(?:[0-9A-Fa-f][0-9A-Fa-f]){1,20}" +
            "" );
    private final NewTrustedCertFactory factory;

    protected TrustedCertThumbprint( final String id,
                                     final String shortIntro,
                                     final String defaultValue,
                                     final NewTrustedCertFactory factory ) {
        super( id, shortIntro, reformatThumbprint(defaultValue) );
        this.factory = factory;
    }

    @Override
    public String parse( final String userInput ) throws ConfigurationException {
        if ( !isValidThumbprint(userInput) ) throw new ConfigurationException( "Invalid thumbprint" );
        return reformatThumbprint(userInput);
    }

    @Override
    public ConfigResult onConfiguration(String thumbprint, ConfigurationContext context) {
        if (thumbprint == null) throw new IllegalStateException("cert is null");
        return ConfigResult.chain(new ConfirmTrustedCert(thumbprint, factory));
    }

    /**
     * Is the given text a valid SHA-1 thumbprint.
     *
     * @param thumbprint The text to validate
     * @return true if valid
     */
    public static boolean isValidThumbprint( final String thumbprint ) {
        boolean valid = false;

        if ( thumbprint != null ) {
            final String clean = cleanThumbprint( thumbprint );
            valid = hexPattern.matcher( clean ).matches();
        }

        return valid;
    }

    private static String cleanThumbprint( final String thumbprint ) {
        return thumbprint.replace( " ", "" ).replace( ":", "" );
    }

    private static String reformatThumbprint( final String thumbprint ) {
        String formatted = thumbprint;

        if ( formatted != null ) {
            formatted = cleanThumbprint( formatted );
            formatted = TextUtils.enforceToBreakOnMultipleLines( formatted, 2, ":", false );
            if ( formatted.endsWith( ":" ) ) {
                formatted = formatted.substring( 0, formatted.length()-1 );
            }
        }

        return formatted;
    }
}
