/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy;

import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.TestDocuments;
import com.l7tech.message.SoapRequest;
import com.l7tech.message.SoapResponse;
import com.l7tech.message.TestSoapRequest;
import com.l7tech.message.TestSoapResponse;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.TrueAssertion;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.proxy.util.PolicyServiceClient;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.w3c.dom.Document;

import java.util.logging.Logger;

/**
 * Unit tests for PolicyService.
 */
public class PolicyServiceTest extends TestCase {
    private static Logger log = Logger.getLogger(PolicyServiceTest.class.getName());

    public PolicyServiceTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(PolicyServiceTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    private void testPolicy(final Assertion policyToTest) throws Exception {
        Document request = PolicyServiceClient.createGetPolicyRequest("123",
                                                                      TestDocuments.getEttkClientCertificate(),
                                                                      TestDocuments.getEttkClientPrivateKey());
        log.info("Request (pretty-printed): " + XmlUtil.nodeToFormattedString(request));

        SoapRequest soapReq = new TestSoapRequest(request);
        SoapResponse soapRes = new TestSoapResponse();

        PolicyService ps = new PolicyService(TestDocuments.getDotNetServerPrivateKey(),
                                             TestDocuments.getDotNetServerCertificate());
        PolicyService.PolicyGetter policyGetter = new PolicyService.PolicyGetter() {
            public Assertion getPolicy(String serviceId) {
                return policyToTest;
            }
        };

        ps.respondToPolicyDownloadRequest(soapReq, soapRes, policyGetter);
        Document response = soapRes.getDocument();
        log.info("Response (pretty-printed:" + XmlUtil.nodeToFormattedString(response));
    }

    public void testSimplePolicyService() throws Exception {
        testPolicy(new TrueAssertion());
    }

    public void testWithIdentities() throws Exception {
        AllAssertion root = new AllAssertion();
        root.getChildren().add(new HttpBasic());
        OneOrMoreAssertion or = new OneOrMoreAssertion();
        root.getChildren().add(or);
        or.getChildren().add(new SpecificUser(-2, "franco", "666", "franco"));
        testPolicy(root);
    }
}
