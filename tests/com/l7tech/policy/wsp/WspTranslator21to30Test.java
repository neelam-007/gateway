/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.wsp;

import com.l7tech.common.util.XmlUtil;
import com.l7tech.policy.WspWriterTest;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.policy.assertion.composite.ExactlyOneAssertion;
import com.l7tech.policy.assertion.ext.Category;
import com.l7tech.policy.assertion.ext.CustomAssertion;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.w3c.dom.Document;

import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author mike
 */
public class WspTranslator21to30Test extends TestCase {
    private static Logger log = Logger.getLogger(WspTranslator21to30Test.class.getName());
    private static final ClassLoader cl = WspTranslator21to30Test.class.getClassLoader();
    private static final String RESOURCE_PATH = "com/l7tech/policy/resources";
    private static final String POLICY_21 = RESOURCE_PATH + "/simple_policy_21.xml";
    private static final String POLICY_21_REQRESPARTIAL = RESOURCE_PATH + "/policy_21_xmlReqResPartial.xml";
    private static final String POLICY_21_REQRESPFULL = RESOURCE_PATH + "/policy_21_xmlReqRespEncrypSignEntire.xml";
    private static final String POLICY_30 = RESOURCE_PATH + "/simple_policy_30.xml";

    public WspTranslator21to30Test(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(WspTranslator21to30Test.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testTranslate21AllAssertionsPolicy() throws Exception {
        InputStream policy21Stream = cl.getResourceAsStream(POLICY_21);

        Document policy21 = XmlUtil.parse(policy21Stream);
        assertTrue(WspVersionImpl.VERSION_2_1 == (WspReader.getPolicyVersion(policy21.getDocumentElement())));

        Document policy30 = WspTranslator21to30.INSTANCE.translatePolicy(policy21.getDocumentElement()).getOwnerDocument();
        assertTrue(WspVersionImpl.VERSION_3_0 == (WspReader.getPolicyVersion(policy30.getDocumentElement())));

        Assertion root = WspReader.parse(policy30.getDocumentElement());
        assertNotNull(root);

        log.info("Upgraded policy: " + root);

        // Check that custom assertion was deserialized correctly
        assertTrue(root instanceof ExactlyOneAssertion);
        ExactlyOneAssertion rootAll = (ExactlyOneAssertion)root;
        Collection rootKids = rootAll.getChildren();
        for (Iterator i = rootKids.iterator(); i.hasNext();) {
            Assertion ass = (Assertion)i.next();
            if (ass instanceof CustomAssertionHolder) {
                CustomAssertionHolder cah = (CustomAssertionHolder)ass;
                CustomAssertion ca = cah.getCustomAssertion();
                if (ca != null) {
                    assertTrue(Category.ACCESS_CONTROL.equals(cah.getCategory()));
                    Map map = WspWriterTest.checkTestCustomAssertion(ca);
                    assertTrue(map instanceof HashMap);
                }
            }
        }

        log.info("Upgraded polciy XML: " + WspWriter.getPolicyXml(root));
    }

    public void testTranslate21PartialEncryptionPolicy() throws Exception {
        InputStream policy21Stream = cl.getResourceAsStream(POLICY_21_REQRESPARTIAL);

        Document policy21 = XmlUtil.parse(policy21Stream);
        assertTrue(WspVersionImpl.VERSION_2_1 == (WspReader.getPolicyVersion(policy21.getDocumentElement())));

        Document policy30 = new WspTranslator21to30().translatePolicy(policy21.getDocumentElement()).getOwnerDocument();
        assertTrue(WspVersionImpl.VERSION_3_0 == (WspReader.getPolicyVersion(policy30.getDocumentElement())));

        Assertion root = WspReader.parse(policy30.getDocumentElement());
        assertNotNull(root);

        log.info("Upgraded policy: " + root);
    }

    public void testTranslate21FullEncryptionPolicy() throws Exception {
        InputStream policy21Stream = cl.getResourceAsStream(POLICY_21_REQRESPFULL);

        Document policy21 = XmlUtil.parse(policy21Stream);
        assertTrue(WspVersionImpl.VERSION_2_1 == (WspReader.getPolicyVersion(policy21.getDocumentElement())));

        Document policy30 = new WspTranslator21to30().translatePolicy(policy21.getDocumentElement()).getOwnerDocument();
        assertTrue(WspVersionImpl.VERSION_3_0 == (WspReader.getPolicyVersion(policy30.getDocumentElement())));

        Assertion root = WspReader.parse(policy30.getDocumentElement());
        assertNotNull(root);

        log.info("Upgraded policy: " + root);
    }
}
