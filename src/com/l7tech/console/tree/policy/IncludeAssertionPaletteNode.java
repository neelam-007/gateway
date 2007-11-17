/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree.policy;

import com.l7tech.console.tree.AbstractLeafPaletteNode;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.Include;

/**
 * @author alex
 */
public class IncludeAssertionPaletteNode extends AbstractLeafPaletteNode {
    private final EntityHeader header;

    public IncludeAssertionPaletteNode(EntityHeader header) {
        super(header.getName(), "com/l7tech/console/resources/include16.png");
        this.header = header;
    }

    @Override
    public Assertion asAssertion() {
        return new Include(header.getOid(), header.getName());
    }
}
