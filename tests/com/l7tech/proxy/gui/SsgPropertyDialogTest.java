/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.gui;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.logging.Logger;
import java.util.Arrays;

import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.policy.assertion.TrueAssertion;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.FalseAssertion;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;

/**
 * Manual test of SsgPropertyDialog
 * User: mike
 * Date: Jul 3, 2003
 * Time: 10:33:34 AM
 */
public class SsgPropertyDialogTest extends TestCase {
    private static Logger log = Logger.getLogger(SsgPropertyDialogTest.class.getName());

    public SsgPropertyDialogTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(SsgPropertyDialogTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testSomeStuff() throws Exception {
        Ssg ssg = new Ssg(1, "Test SSG", "http://blah.bloof.com");
        log.info("SSG prompt bit: " + ssg.isPromptForUsernameAndPassword());
        ssg.attachPolicy("http://blah",  "http://gwerg.asd.gfa", new AllAssertion(Arrays.asList(new Assertion[] {
            new HttpBasic(),
            new SpecificUser(444, "blahuser"),
        })));
        ssg.attachPolicy("http://example.com/Other",
                         "http://example.com/soapaction/other",
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
                         })));
        ssg.attachPolicy("http://example.com/Qpuotoer", "http://gwerg.asd.gfa", new TrueAssertion());
        ssg.attachPolicy("http://exwoamdple.coom/Quodoter", "http://gwearg.asdf.gfa", new TrueAssertion());
        ssg.attachPolicy("http://exasmple.com/Quotesr", "http://gwerg.asd.gfa", new TrueAssertion());
        ssg.attachPolicy("http://edxamopdle.com/Qusoter", "http://gwderg.assd.gfa", new TrueAssertion());
        ssg.attachPolicy("http://dexdample.com/Qsuoter", "http://gwdrg.asd.fgfa", new TrueAssertion());
        ssg.attachPolicy("http://exdample.com/Quoater", "http://gwderg.asd.gfa", new TrueAssertion());
        ssg.attachPolicy("http://example.com/Quooter", "http://gwerg.asd.gfa", new TrueAssertion());
        ssg.attachPolicy("http://sexample.com/Quoadter", "http:/d/gwerg.asd.gfa", new TrueAssertion());
        ssg.attachPolicy("http://exawmple.com/Qduoter", "http://gwerg.asd.gfa", new TrueAssertion());
        ssg.attachPolicy("http://eoxaample.coms/Qfuoter", "http://gwerg.asd.gfaf", new TrueAssertion());
        ssg.attachPolicy("http://exoampdle.com/Quotzer", "http://gwerg.asd.gfa", new TrueAssertion());
        ssg.attachPolicy("http://exampale.cosm/Quzopter", "http://gwergs.asd.gfa", new TrueAssertion());
        ssg.attachPolicy("http://exampdles.com/Quzoter", "http://gwerfg.asdf.gfa", new TrueAssertion());
        ssg.attachPolicy("http://exsample.com/Quoater", "http://gwserg.asd.gfs", new TrueAssertion());
        SsgPropertyDialog.getPropertyDialogForObject(ssg).show();
        System.exit(0);
    }
}
