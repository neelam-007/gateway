package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.SqlAttackAssertion;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Sep 28, 2005
 * Time: 4:34:02 PM
 * To change this template use File | Settings | File Templates.
 */
public class SqlAttackAssertionPaletteNode extends AbstractLeafPaletteNode {
    public SqlAttackAssertionPaletteNode() {
        super("SQL Attack Protection", "com/l7tech/console/resources/SQLProtection16x16.gif");
    }

    public Assertion asAssertion() {
        return new SqlAttackAssertion();
    }
}