package com.l7tech.policy.assertion;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class RequestSwAAssertion extends SwAAssertion {
    private Map bindings = new LinkedHashMap();
    private Map namespaceMap = new LinkedHashMap();

    public RequestSwAAssertion() {
    }

    public RequestSwAAssertion(Map bindings) {
        this.bindings = bindings;
    }

    /**
     * @return the Bindings map.  Never null.
     */
    public Map getBindings() {
        return bindings;
    }

    /**
     * @param bindings the new Binding info.  May not be null.
     */
    public void setBindings(Map bindings) {
        if (bindings == null)
            throw new IllegalArgumentException("bindings map may not be null");
        this.bindings.putAll(bindings);
    }

    public Map getNamespaceMap() {
        return namespaceMap;
    }

    public void setNamespaceMap(Map namespaceMap) {
        if (namespaceMap == null)
            throw new IllegalArgumentException("Namespace map may not be null");
        this.namespaceMap.putAll(namespaceMap);
    }

}
