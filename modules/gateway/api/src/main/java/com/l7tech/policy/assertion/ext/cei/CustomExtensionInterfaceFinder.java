package com.l7tech.policy.assertion.ext.cei;

import com.l7tech.policy.assertion.ext.ServiceException;

/*
* Layer 7 API that provides ability to invoke server side methods on the Gateway from the Policy Manager.
* Use this interface to find a registered CustomExtensionInterfaceBinding.
*/
public interface CustomExtensionInterfaceFinder {
    /**
     * @param interfaceClass a registered custom extension interface
     * @param <T> a custom class with implemented callback method(s) for server side invocation on the Gateway.
     * @return implementation of a custom class with implemented callback method(s) for server side invocation on the Gateway.
     * @throws com.l7tech.policy.assertion.ext.ServiceException if interfaceClass contains non primitive or String types (arrays allowed)
     */
    public abstract <T> T getExtensionInterface(Class<T> interfaceClass) throws ServiceException;
}
