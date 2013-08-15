package com.l7tech.console.policy.exporter;

import com.l7tech.gui.util.Utilities;
import com.l7tech.console.panels.ResolveExternalPolicyReferencesWizard;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.exporter.ExternalReference;
import com.l7tech.policy.exporter.IdProviderReference;
import com.l7tech.util.SyspropUtil;
import org.junit.Ignore;

/**
 * Test the look and feel of the panel.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jul 23, 2004<br/>
 */
@Ignore
public class ResolveForeignIdentityProviderPanelTest {
    public static void main(String[] args) throws Exception {
        SyspropUtil.setProperty( "com.l7tech.common.locator", "com.l7tech.common.locator.StubModeLocator" );
        IdProviderReference ref = new IdProviderReference(new ConsoleExternalReferenceFinder(), new Goid(0,-1));
        ref.setProviderName("MS International Directory");
        ref.setIdProviderTypeVal(2);

        IdProviderReference ref2 = new IdProviderReference(new ConsoleExternalReferenceFinder(),  new Goid(0,5));
        ref2.setProviderName("IBM dir");
        ref2.setIdProviderTypeVal(2);

        //ResolveForeignIdentityProviderPanel panel = new ResolveForeignIdentityProviderPanel(null, ref);
        //Wizard wiz = new Wizard(null, panel);
        ResolveExternalPolicyReferencesWizard wiz = ResolveExternalPolicyReferencesWizard.fromReferences(null, new ExternalReference[]{ref, ref2});
        wiz.pack();
        Utilities.centerOnScreen(wiz);
        wiz.setModal(true);
        wiz.setVisible(true);
        System.exit(0);
    }
}
