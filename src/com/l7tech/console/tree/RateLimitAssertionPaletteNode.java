package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.RateLimitAssertion;
import com.l7tech.console.panels.RateLimitAssertionPropertiesDialog;

/**
 * Palette node representing the Rate Limit assertion.
 * @see com.l7tech.policy.assertion.RateLimitAssertion
 */
public class RateLimitAssertionPaletteNode extends AbstractLeafPaletteNode {
    public RateLimitAssertionPaletteNode() {
        super("Rate Limit", "com/l7tech/console/resources/disconnect.gif");
    }

    public Assertion asAssertion() {
        RateLimitAssertion rla = new RateLimitAssertion();
        rla.setCounterName(RateLimitAssertionPropertiesDialog.makeDefaultCounterName());
        return rla;
    }
}
