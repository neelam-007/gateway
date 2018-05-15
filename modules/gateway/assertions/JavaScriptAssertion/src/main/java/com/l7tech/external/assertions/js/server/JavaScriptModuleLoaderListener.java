package com.l7tech.external.assertions.js.server;

import com.l7tech.external.assertions.js.features.JavaScriptAssertionConstants;
import com.l7tech.external.assertions.js.features.JavaScriptEngineManager;
import com.l7tech.util.ConfigFactory;
import org.springframework.context.ApplicationContext;

/**
 * ModuleLoaderListener for capturing ECMA Version change event and re-initializing the ScriptEngine.
 */
@SuppressWarnings("unused")
public class JavaScriptModuleLoaderListener {

    private JavaScriptModuleLoaderListener() {}

    @SuppressWarnings("unused")
    public static synchronized void onModuleLoaded(final ApplicationContext context) {
        ConfigFactory.addListener(new ConfigFactory.SmartConfigurationListener() {
            @Override
            public boolean supportsProperty(final String propertyName) {
                return propertyName.equals(JavaScriptAssertionConstants.ECMA_VERSION_PROPERTY);
            }

            @Override
            public void notifyPropertyChanged(final String propertyName) {
                JavaScriptEngineManager.getInstance().reInitializeEngine();
            }
        });
    }
}
