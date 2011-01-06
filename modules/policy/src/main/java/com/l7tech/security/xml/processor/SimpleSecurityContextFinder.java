package com.l7tech.security.xml.processor;

import java.util.HashMap;
import java.util.Map;

/**
 * A simple SecurityContextFinder for use in unit tests.
 */
public class SimpleSecurityContextFinder implements SecurityContextFinder {
    private final Map<String, SecurityContext> contexts;

    /**
     * Create a context finder that will find the specified contexts by their identifier strings.
     *
     * @param contexts a Map from identifier string to security context.  Required.
     */
    public SimpleSecurityContextFinder(Map<String, SecurityContext> contexts) {
        this.contexts = contexts;
    }

    /**
     * Create a context finder that will always find the specified context, with optional matching of identifier string.
     *
     * @param identifier an identifier string that must match, or null to find this context regardless of the requested identifier.
     * @param context the context to find.  Required.
     */
    public SimpleSecurityContextFinder(String identifier, SecurityContext context) {
        Map<String, SecurityContext> map = new HashMap<String, SecurityContext>();
        map.put(identifier, context);
        this.contexts = map;
    }

    @Override
    public SecurityContext getSecurityContext(String securityContextIdentifier) {
        SecurityContext ret = contexts.get(securityContextIdentifier);
        if (ret == null) ret = contexts.get(null);
        return ret;
    }
}
