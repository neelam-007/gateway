/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.wsp;

import com.l7tech.common.util.XmlUtil;
import com.l7tech.policy.assertion.Assertion;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.w3c.dom.Document;

import java.io.InputStream;
import java.util.logging.Logger;

/**
 * @author mike
 */
public class WspTranslator21to30Test extends TestCase {
    private static Logger log = Logger.getLogger(WspTranslator21to30Test.class.getName());
    private static final ClassLoader cl = WspTranslator21to30Test.class.getClassLoader();
    private static String RESOURCE_PATH = "com/l7tech/policy/resources";
    private static String POLICY_21 = RESOURCE_PATH + "/simple_policy_21.xml";
    private static String POLICY_30 = RESOURCE_PATH + "/simple_policy_30.xml";

    public WspTranslator21to30Test(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(WspTranslator21to30Test.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testTranslate21Policy() throws Exception {
        InputStream policy21Stream = cl.getResourceAsStream(POLICY_21);
        
        Document policy21 = XmlUtil.parse(policy21Stream);
        assertTrue(WspVersionImpl.VERSION_2_1 == (WspReader.getPolicyVersion(policy21.getDocumentElement())));

        Document policy30 = new WspTranslator21to30().translatePolicy(policy21);
        assertTrue(WspVersionImpl.VERSION_3_0 == (WspReader.getPolicyVersion(policy30.getDocumentElement())));

        Assertion root = WspReader.parse(policy30.getDocumentElement());
        assertNotNull(root);
    }
}
