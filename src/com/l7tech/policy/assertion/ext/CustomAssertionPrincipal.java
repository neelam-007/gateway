package com.l7tech.policy.assertion.ext;

import java.security.Principal;
import java.io.Serializable;

/**
 * @author emil
 * @version 3-Jun-2004
 */
public class CustomAssertionPrincipal implements Principal, Serializable {
    private static final long serialVersionUID = -5113651738803487432L;
    
    private String name;

    public CustomAssertionPrincipal(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
