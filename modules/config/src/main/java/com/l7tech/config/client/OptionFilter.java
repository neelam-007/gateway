package com.l7tech.config.client;

import com.l7tech.config.client.options.OptionSet;
import com.l7tech.config.client.options.Option;

/**
 * Filter that allows deactivation of Options.
 */
public interface OptionFilter {

    /**
     * Should the given option be active?
     *
     * @param optionSet The OptionSet for the option
     * @param option The option to check
     * @return true if the option is active
     */
    boolean isOptionActive( OptionSet optionSet, Option option );
}
