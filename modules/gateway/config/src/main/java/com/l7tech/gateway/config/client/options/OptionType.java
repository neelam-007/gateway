package com.l7tech.gateway.config.client.options;

/**
 * Type for a configuration option.
 *
 * <p>A type defines a validation (default) rule and visibility for an option.</p>
 *
 * @author steve
 */
public enum OptionType {
    
    //- PUBLIC

    /**
     * A host name
     */
    HOSTNAME("^[a-zA-Z0-9][a-zA-Z0-9\\-\\_.]{0,254}"),

    /**
     * A v4 IP address
     */
    IP_ADDRESS("^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"),

    /**
     * A password (should not be displayed in UI or recorded in logs)
     */
    PASSWORD("^[\\p{Graph}\\p{Blank}]{0,255}$", true),

    /**
     * A file path
     */
    PATH("^[\\p{Graph}\\p{Blank}]{0,1024}$"),

    /**
     * A port number
     */
    PORT("^(?:6(?:[1-4]\\d{3}|(?:5(?:[0-4]\\d{2}|5(?:[0-2]\\d|3[0-5]))))|[1-5]\\d{4}|(?!0)\\d{2,4}|[1-9])$"),   // 1 - 65535

    /**
     * Any text
     */
    TEXT("^[\\p{Graph}\\p{Blank}]{0,10240}$"),

    /**
     * A username / login
     */
    USERNAME("^[\\p{Graph}\\p{Blank}]{1,255}$");

    /**
     * The default validation regex for this option type.
     *
     * @return The validation regular expression.
     */
    public String getDefaultRegex() {
        return defaultPattern;
    }

    /**
     * True if this option should be hidden in the GUI.
     *
     * @return true if hidden
     */
    public boolean isHidden() {
        return hidden;
    }
    
    //- PRIVATE
        
    private String defaultPattern;
    private boolean hidden;
    
    private OptionType( String pattern ) {
        this( pattern, false );
    }
    
    private OptionType( String pattern, boolean hidden ) {
        this.defaultPattern = pattern;
        this.hidden = hidden;
    } 
}
