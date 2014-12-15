package com.l7tech.server.util;

import org.apache.cxf.bus.extension.ExtensionManagerBus;

/**
 * Extending the extension manager bus to not return a class loader extension. This will force
 * org.apache.cxf.jaxws.EndpointImpl#doPublish and org.apache.cxf.transport.servlet.CXFNonSpringServlet#invoke to use
 * the existing thread local class loader instead of replacing it.
 *
 * See EM-1020
 */
public class ClassLoaderDisablingExtensionManagerBus extends ExtensionManagerBus {

    public ClassLoaderDisablingExtensionManagerBus() {
        super();
        //adding the class loader extension tot the missing extensions means it will not be returned.
        missingExtensions.add(ClassLoader.class);
    }
}