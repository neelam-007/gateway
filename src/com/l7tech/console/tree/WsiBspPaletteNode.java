package com.l7tech.console.tree;

import javax.swing.*;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.WsiBspAssertion;

/**
 * Tree node in the assertion palette corresponding to the WsiBsp assertion type.
 *
 * @author $Author$
 * @version $Revision$
 */
public class WsiBspPaletteNode extends AbstractLeafTreeNode  {

    public WsiBspPaletteNode() {
        super("WSI-BSP Compliancy", "com/l7tech/console/resources/policy16.gif");
    }

    public Assertion asAssertion() {
        return new WsiBspAssertion();
    }
}
