/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.logging.Logger;
import java.io.Reader;
import java.io.IOException;

import com.l7tech.proxy.util.PolicyServiceClient;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.TestDocuments;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.message.SoapRequest;
import com.l7tech.message.TestSoapRequest;
import com.l7tech.message.TestSoapResponse;
import com.l7tech.message.SoapResponse;
import com.l7tech.server.transport.jms.JmsSoapRequest;
import com.l7tech.server.transport.jms.JmsTransportMetadata;
import org.w3c.dom.Document;

/**
 * @author mike
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

    public void testPolicyService() throws Exception {
        Document request = PolicyServiceClient.createGetPolicyRequest("123",
                                                                      TestDocuments.getEttkClientCertificate(),
                                                                      TestDocuments.getEttkClientPrivateKey());
        log.info("Request (pretty-printed): " + XmlUtil.nodeToFormattedString(request));

        SoapRequest soapReq = new TestSoapRequest(request);
        SoapResponse soapRes = new TestSoapResponse();

        PolicyService ps = new PolicyService(TestDocuments.getDotNetServerPrivateKey(),
                                             TestDocuments.getDotNetServerCertificate());
        PolicyService.PolicyGetter policyGetter = new PolicyService.PolicyGetter() {
            public AllAssertion getPolicy(String serviceId) {
                return null;
            }
        };

        ps.respondToPolicyDownloadRequest(soapReq, soapRes, policyGetter);
        Document response = soapRes.getDocument();
        log.info("Response (pretty-printed:" + XmlUtil.nodeToFormattedString(response));

    }
}
