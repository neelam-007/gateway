/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.gui;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.FalseAssertion;
import com.l7tech.policy.assertion.TrueAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.proxy.ClientProxy;
import com.l7tech.proxy.datamodel.Policy;
import com.l7tech.proxy.datamodel.PolicyAttachmentKey;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.SsgManagerStub;
import com.l7tech.proxy.gui.dialogs.SsgPropertyDialog;

import java.util.Arrays;
import java.util.logging.Logger;

/**
 * Manual test of SsgPropertyDialog
 * User: mike
 * Date: Jul 3, 2003
 * Time: 10:33:34 AM
 */
public class SsgPropertyDialogTest {
    private static Logger log = Logger.getLogger(SsgPropertyDialogTest.class.getName());

    public static void main(String[] args) {
        try {
            new SsgPropertyDialogTest().testSomeStuff();
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    public void testSomeStuff() throws Exception {
        Ssg ssg = new Ssg(1, "blah.bloof.com");
        ssg.getRuntime().getPolicyManager().setPolicy(new PolicyAttachmentKey("http://blah",  "http://gwerg.asd.gfa", null), new Policy(new AllAssertion(Arrays.asList(new Assertion[] {
            new HttpBasic(),
            new SpecificUser(444, "blahuser", null, null),
        })), "testpolicy"));
        ssg.getRuntime().getPolicyManager().setPolicy(new PolicyAttachmentKey("http://HugeTree.com/Other",
                         "http://example.com/soapaction/other", null), new Policy(
                         new AllAssertion(Arrays.asList(new Assertion[] {
                             new OneOrMoreAssertion(Arrays.asList(new Assertion[] {
                                 new TrueAssertion(),
                                 new FalseAssertion(),
                                 new TrueAssertion(),
                                 new OneOrMoreAssertion(Arrays.asList(new Assertion[] {
                                     new TrueAssertion(),
                                     new FalseAssertion(),
                                     new FalseAssertion(),
                                     new TrueAssertion(),
                                     new OneOrMoreAssertion(Arrays.asList(new Assertion[] {
                                         new TrueAssertion(),
                                         new FalseAssertion(),
                                         new OneOrMoreAssertion(Arrays.asList(new Assertion[] {
                                             new TrueAssertion(),
                                             new FalseAssertion(),
                                             new AllAssertion(Arrays.asList(new Assertion[] {
                                                 new OneOrMoreAssertion(Arrays.asList(new Assertion[] {
                                                     new TrueAssertion(),
                                                     new FalseAssertion(),
                                                     new TrueAssertion(),
                                                     new OneOrMoreAssertion(Arrays.asList(new Assertion[] {
                                                         new TrueAssertion(),
                                                         new FalseAssertion(),
                                                         new FalseAssertion(),
                                                         new TrueAssertion(),
                                                         new OneOrMoreAssertion(Arrays.asList(new Assertion[] {
                                                             new TrueAssertion(),
                                                             new FalseAssertion(),
                                                             new OneOrMoreAssertion(Arrays.asList(new Assertion[] {
                                                                 new TrueAssertion(),
                                                                 new FalseAssertion()
                                                             })),
                                                         })),
                                                     })),
                                                 })),
                                             })),
                                         })),
                                     })),
                                 })),
                                 new TrueAssertion(),
                                 new OneOrMoreAssertion(Arrays.asList(new Assertion[] {
                                     new TrueAssertion(),
                                     new FalseAssertion()
                                 })),
                                 new OneOrMoreAssertion(Arrays.asList(new Assertion[] {
                                     new TrueAssertion(),
                                     new FalseAssertion()
                                 })),
                                 new OneOrMoreAssertion(Arrays.asList(new Assertion[] {
                                     new TrueAssertion(),
                                     new FalseAssertion()
                                 })),
                                 new OneOrMoreAssertion(Arrays.asList(new Assertion[] {
                                     new TrueAssertion(),
                                     new FalseAssertion()
                                 })),
                                 new OneOrMoreAssertion(Arrays.asList(new Assertion[] {
                                     new TrueAssertion(),
                                     new FalseAssertion()
                                 })),
                                 new OneOrMoreAssertion(Arrays.asList(new Assertion[] {
                                     new TrueAssertion(),
                                     new FalseAssertion()
                                 })),
                                 new OneOrMoreAssertion(Arrays.asList(new Assertion[] {
                                     new TrueAssertion(),
                                     new FalseAssertion(),
                                 }))
                             }))
                         })), "testpoliciy"));
        Policy tp = new Policy(new TrueAssertion(), "foo");
        ssg.getRuntime().getPolicyManager().setPolicy(new PolicyAttachmentKey("http://example.com/Qpuotoer", "http://gwerg.asd.gfa", "/blah/bloo"), tp);
        ssg.getRuntime().getPolicyManager().setPolicy(new PolicyAttachmentKey("http://exwoamdple.coom/Quodoter", "http://gwearg.asdf.gfa", "/bloo/bloo"), tp);
        ssg.getRuntime().getPolicyManager().setPolicy(new PolicyAttachmentKey("http://exasmple.com/Quotesr", "http://gwerg.asd.gfa", null), tp);
        ssg.getRuntime().getPolicyManager().setPolicy(new PolicyAttachmentKey("http://edxamopdle.com/Qusoter", "http://gwderg.assd.gfa", null), tp);
        ssg.getRuntime().getPolicyManager().setPolicy(new PolicyAttachmentKey("http://dexdample.com/Qsuoter", "http://gwdrg.asd.fgfa", null), tp);
        ssg.getRuntime().getPolicyManager().setPolicy(new PolicyAttachmentKey("http://exdample.com/Quoater", "http://gwderg.asd.gfa", null), tp);
        ssg.getRuntime().getPolicyManager().setPolicy(new PolicyAttachmentKey("http://example.com/Quooter", "http://gwerg.asd.gfa", null), tp);
        ssg.getRuntime().getPolicyManager().setPolicy(new PolicyAttachmentKey("http://sexample.com/Quoadter", "http:/d/gwerg.asd.gfa", null), tp);
        ssg.getRuntime().getPolicyManager().setPolicy(new PolicyAttachmentKey("http://exawmple.com/Qduoter", "http://gwerg.asd.gfa", null), tp);
        ssg.getRuntime().getPolicyManager().setPolicy(new PolicyAttachmentKey("http://eoxaample.coms/Qfuoter", "http://gwerg.asd.gfaf", null), tp);
        ssg.getRuntime().getPolicyManager().setPolicy(new PolicyAttachmentKey("http://exoampdle.com/Quotzer", "http://gwerg.asd.gfa", null), tp);
        ssg.getRuntime().getPolicyManager().setPolicy(new PolicyAttachmentKey("http://exampale.cosm/Quzopter", "http://gwergs.asd.gfa", null), tp);
        ssg.getRuntime().getPolicyManager().setPolicy(new PolicyAttachmentKey("http://exampdles.com/Quzoter", "http://gwerfg.asdf.gfa", null), tp);
        ssg.getRuntime().getPolicyManager().setPolicy(new PolicyAttachmentKey("http://exsample.com/Quoater", "http://gwserg.asd.gfs", null), tp);

        log.info("Firing up an example SsgPropertyDialog");
        ClientProxy clientProxy = new ClientProxyStub(9797);
        Gui.setInstance(Gui.createGui(clientProxy, new SsgManagerStub()));
        Gui.getInstance().start();
        SsgPropertyDialog.makeSsgPropertyDialog(clientProxy, ssg).show();
        System.exit(0);
    }
}
