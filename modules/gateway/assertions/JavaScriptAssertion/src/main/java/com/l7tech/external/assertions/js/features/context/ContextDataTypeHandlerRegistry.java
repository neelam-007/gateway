package com.l7tech.external.assertions.js.features.context;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Singleton implementation to store handlers for writing context variables based on data type.
 */
public class ContextDataTypeHandlerRegistry {

    private final Map<String, ContextDataTypeHandler> contextHandlerMap = new HashMap<>();
    private final Map<String, String> scriptObjectMirrorClassnameMapping = new HashMap<>();
    private final GenericTypeHandler genericTypeHandler = new GenericTypeHandler();

    private ContextDataTypeHandlerRegistry() {
        final NumberTypeHandler numberTypeHandler = new NumberTypeHandler();
        final DateTypeHandler dateTypeHandler = new DateTypeHandler();
        final ObjectTypeHandler objectTypeHandler = new ObjectTypeHandler();

        contextHandlerMap.put(Double.class.getName(), numberTypeHandler);
        contextHandlerMap.put(Integer.class.getName(), numberTypeHandler);
        contextHandlerMap.put(Date.class.getName(), dateTypeHandler);
        contextHandlerMap.put(Object.class.getName(), objectTypeHandler);

        scriptObjectMirrorClassnameMapping.put("Date", Date.class.getName());
        scriptObjectMirrorClassnameMapping.put("Object", Object.class.getName());
    }

    private static class ContextHandlerRegistryHelper {
        private static final ContextDataTypeHandlerRegistry INSTANCE = new ContextDataTypeHandlerRegistry();
    }

    private static ContextDataTypeHandlerRegistry getInstance() {
        return ContextHandlerRegistryHelper.INSTANCE;
    }

    private ContextDataTypeHandler get(final Object object) {
        return object != null ? contextHandlerMap.getOrDefault(getClassName(object), genericTypeHandler) : genericTypeHandler;
    }

    private String getClassName(final Object object) {
        if (object instanceof ScriptObjectMirror) {
            return scriptObjectMirrorClassnameMapping.get(((ScriptObjectMirror) object).getClassName());
        } else {
            return object.getClass().getName();
        }
    }

    public static ContextDataTypeHandler getHandler(final Object object) {
        return getInstance().get(object);
    }
}
