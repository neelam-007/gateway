package com.l7tech.internal.signer.command;

import org.apache.commons.cli.Option;
import org.apache.commons.lang.StringUtils;

/**
 * Option builder class.
 */
@SuppressWarnings("UnusedDeclaration")
public final class OptionBuilder {
    /**
     * Convenient method for creating a {@code Option} with the specified long name and description.
     *
     * @param longOpt    the long option name
     * @param desc       the description of the option
     * @return an {@link org.apache.commons.cli.Option Option} instance with the specified {@code longOpt} and {@code desc}, never {@code null}.
     */
    public static Option create(final String longOpt, final String desc) {
        return new Option(null, longOpt, false, desc);
    }

    /**
     * Short option name
     */
    private String opt = null;
    public OptionBuilder opt(final String opt) {
        this.opt = opt;
        return this;
    }

    /**
     * Long option name
     */
    private String longOpt = null;
    public OptionBuilder longOpt(final String longOpt) {
        this.longOpt = longOpt;
        return this;
    }

    /**
     * Option description
     */
    private String desc = null;
    public OptionBuilder desc(final String desc) {
        this.desc = desc;
        return this;
    }

    /**
     * a flag indicating whether the option is a optional
     */
    private Boolean optional = null;
    public OptionBuilder optional(final boolean optional) {
        this.optional = optional;
        return this;
    }

    /**
     * a flag indicating whether the option is a required
     */
    private Boolean required = null;
    public OptionBuilder required(final boolean required) {
        this.required = required;
        return this;
    }

    /**
     * a flag indicating whether the option has arguments
     */
    private Boolean hasArg = null;
    public OptionBuilder hasArg(final boolean hasArg) {
        this.hasArg = hasArg;
        return this;
    }

    /**
     * Number of option arguments
     */
    private Integer args = null;
    public OptionBuilder args(final int args) {
        this.args = args;
        return this;
    }

    /**
     * The display name of the option value
     */
    private String argName = null;
    public OptionBuilder argName(final String argName) {
        this.argName = argName;
        return this;
    }

    /**
     * Build the {@link org.apache.commons.cli.Option Option} object.
     */
    public Option build() {
        final Option option = new Option(
                StringUtils.isNotEmpty(opt) ? opt : null,
                StringUtils.isNotEmpty(desc) ? desc : null
        );
        if (StringUtils.isNotEmpty(longOpt)) {
            option.setLongOpt(longOpt);
        }
        if (optional != null) {
            option.setOptionalArg(optional);
        }
        if (required != null) {
            option.setRequired(required);
        }
        if (args != null) {
            option.setArgs(args);
        } else if ( (hasArg != null && hasArg) || (argName !=null && StringUtils.isNotBlank(argName)) ) {
            option.setArgs(1);
        }
        if (argName!= null) {
            option.setArgName(argName);
        }

        return option;
    }
}
