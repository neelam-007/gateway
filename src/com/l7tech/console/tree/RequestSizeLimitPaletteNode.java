package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.RequestSizeLimit;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Sep 29, 2005
 * Time: 3:43:47 PM
 * To change this template use File | Settings | File Templates.
 */
public class RequestSizeLimitPaletteNode extends AbstractLeafPaletteNode {
    public RequestSizeLimitPaletteNode() {
        super("Request Size Limit", "com/l7tech/console/resources/MessageLength-16x16.gif");
    }

    public Assertion asAssertion() {
        return new RequestSizeLimit();
    }
}
