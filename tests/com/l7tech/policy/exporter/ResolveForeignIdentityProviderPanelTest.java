package com.l7tech.policy.exporter;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.panels.ResolveForeignIdentityProviderPanel;
import com.l7tech.console.panels.Wizard;

/**
 * Test the look and feel of the panel.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jul 23, 2004<br/>
 * $Id$
 */
public class ResolveForeignIdentityProviderPanelTest {
    public static void main(String[] args) throws Exception {
        System.setProperty("com.l7tech.common.locator", "com.l7tech.common.locator.StubModeLocator");
        IdProviderReference ref = new IdProviderReference(-1);
        ref.setProviderName("MS International Directory");
        ref.setIdProviderTypeVal(2);

        ResolveForeignIdentityProviderPanel panel = new ResolveForeignIdentityProviderPanel(null, ref);
        Wizard wiz = new Wizard(null, panel);
        wiz.pack();
        wiz.setSize(1100, 500);
        Utilities.centerOnScreen(wiz);
        wiz.setModal(true);
        wiz.setVisible(true);
        System.exit(0);
    }
}
