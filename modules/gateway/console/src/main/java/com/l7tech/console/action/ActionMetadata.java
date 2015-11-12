package com.l7tech.console.action;

/**
 * Provides constants defining supplementary information about Actions.
 *
 * @author Jamie Williams - jamie.williams2@ca.com
 */
public interface ActionMetadata {

    /**
     * Provides a hint as to which menu/sub-menu an action should be placed in, if the interface supports dynamic
     * addition of actions. Intended for optional use by modular assertion actions.
     */
    public static final String MENU_HINT = "menuHint";

    // Transports
    public static final String TRANSPORTS_MENU_HINT = "transportsMenu";

    // Services and APIs
    public static final String SERVICES_AND_APIS_MENU_HINT = "servicesMenu";
}
