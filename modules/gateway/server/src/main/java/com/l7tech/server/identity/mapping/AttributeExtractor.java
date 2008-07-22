/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.identity.mapping;

import com.l7tech.identity.Identity;

/**
 * Implementations are parameterized with an {@link com.l7tech.identity.mapping.IdentityMapping}, and are responsible
 * for extracting the attribute values described by that mapping from a given {@link Identity}.
 * 
 * @author alex
 */
public interface AttributeExtractor {
    public Object[] extractValues(Identity identity);
}
