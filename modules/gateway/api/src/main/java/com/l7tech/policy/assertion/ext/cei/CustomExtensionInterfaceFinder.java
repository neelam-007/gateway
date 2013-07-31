package com.l7tech.policy.assertion.ext.cei;

import com.l7tech.policy.assertion.ext.ServiceException;

/**
 * Layer 7 API that provides ability to invoke server side methods on the Gateway from the Policy Manager.
 * Use this interface to find a registered CustomExtensionInterfaceBinding.
 */
public interface CustomExtensionInterfaceFinder {

    /**
     * The key for getting {@link CustomExtensionInterfaceFinder} from the Console Context.<p>
     * See {@link com.l7tech.policy.assertion.ext.cei.UsesConsoleContext UsesConsoleContext} for more details.
     */
    static final String CONSOLE_CONTEXT_KEY = "customExtensionInterfaceFinder";

    /**
     * @param interfaceClass a registered custom extension interface
     * @param <T> a custom class with implemented callback method(s) for server side invocation on the Gateway.
     * @return implementation of a custom class with implemented callback method(s) for server side invocation on the Gateway.
     * @throws com.l7tech.policy.assertion.ext.ServiceException if interfaceClass contains non primitive or String types (arrays allowed)
     */
    abstract <T> T getExtensionInterface(Class<T> interfaceClass) throws ServiceException;
}
