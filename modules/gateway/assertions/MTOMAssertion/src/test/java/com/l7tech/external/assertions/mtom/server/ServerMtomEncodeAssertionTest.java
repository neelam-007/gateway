package com.l7tech.external.assertions.mtom.server;

import com.l7tech.gateway.common.audit.AuditDetailMessage;
import com.l7tech.policy.assertion.RequestXpathAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.server.policy.assertion.ServerRequestXpathAssertion;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import org.w3c.dom.Document;
import com.l7tech.external.assertions.mtom.MtomEncodeAssertion;
import com.l7tech.server.audit.LogOnlyAuditor;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.common.mime.StashManager;
import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.message.Message;
import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.util.ResourceUtils;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.MessageTargetableSupport;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.HashMap;

/**
 *
 */
public class ServerMtomEncodeAssertionTest {

    private static final Logger logger = Logger.getLogger(ServerMtomEncodeAssertionTest.class.getName());

    private static final String message = "<S:Envelope xmlns:S=\"http://www.w3.org/2003/05/soap-envelope\">\n" +
                "    <S:Body>\n" +
                "        <ns2:echoFile xmlns:ns2=\"http://www.layer7tech.com/services/jaxws/echoservice\">\n" +
                "            <arg0>\n" +
                "                <data>QUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkK</data>\n" +
                "                <name>payload.txt</name>\n" +
                "            </arg0>\n" +
                "        </ns2:echoFile>\n" +
                "    </S:Body>\n" +
                "</S:Envelope>";

    private static final String messageEmbeddedWhitespace = "<S:Envelope xmlns:S=\"http://www.w3.org/2003/05/soap-envelope\">\n" +
                "    <S:Body>\n" +
                "        <ns2:echoFile xmlns:ns2=\"http://www.layer7tech.com/services/jaxws/echoservice\">\n" +
                "            <arg0>\n" +
                "                <data>QUJDREVGR0hJSktMTU5PUFF SU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkK</data>\n" +
                "                <name>payload.txt</name>\n" +
                "            </arg0>\n" +
                "        </ns2:echoFile>\n" +
                "    </S:Body>\n" +
                "</S:Envelope>";

    private static final String messageLeadingWhitespace = "<S:Envelope xmlns:S=\"http://www.w3.org/2003/05/soap-envelope\">\n" +
                "    <S:Body>\n" +
                "        <ns2:echoFile xmlns:ns2=\"http://www.layer7tech.com/services/jaxws/echoservice\">\n" +
                "            <arg0>\n" +
                "                <data> QUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkK</data>\n" +
                "                <name>payload.txt</name>\n" +
                "            </arg0>\n" +
                "        </ns2:echoFile>\n" +
                "    </S:Body>\n" +
                "</S:Envelope>";

    private static final String messageTrailingWhitespace = "<S:Envelope xmlns:S=\"http://www.w3.org/2003/05/soap-envelope\">\n" +
                "    <S:Body>\n" +
                "        <ns2:echoFile xmlns:ns2=\"http://www.layer7tech.com/services/jaxws/echoservice\">\n" +
                "            <arg0>\n" +
                "                <data>QUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkK </data>\n" +
                "                <name>payload.txt</name>\n" +
                "            </arg0>\n" +
                "        </ns2:echoFile>\n" +
                "    </S:Body>\n" +
                "</S:Envelope>";

    @Test
    public void testEncode() throws Exception {
        final MtomEncodeAssertion mea = new MtomEncodeAssertion();
        mea.setAlwaysEncode( true );
        mea.setFailIfNotFound( true );
        mea.setOptimizationThreshold( 0 );
        mea.setXpathExpressions( new XpathExpression[]{
                new XpathExpression( "/s12:Envelope/s12:Body/tns:echoFile/arg0/data", new HashMap<String,String>(){{
                    put("s12","http://www.w3.org/2003/05/soap-envelope");
                    put("tns","http://www.layer7tech.com/services/jaxws/echoservice");
                }} )
        } );

        final ServerMtomEncodeAssertion smea = buildServerAssertion( mea );
        final Document requestDoc = XmlUtil.parse( message );
        PolicyEnforcementContext context = null;
        try {
            context = PolicyEnforcementContextFactory.createPolicyEnforcementContext( new Message(requestDoc), null );
            AssertionStatus status = smea.checkRequest( context );
            assertEquals( "status ok", AssertionStatus.NONE, status );

            final String output = XmlUtil.nodeToString( context.getRequest().getXmlKnob().getDocumentReadOnly() );
            assertTrue( "Message raw", message.contains("QUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZ"));
            assertFalse( "Message encoded", output.contains("QUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZ"));
        } finally {
            ResourceUtils.closeQuietly( context );
        }
    }

    @Test
    public void testEncodeFailureEmbeddedWhitespace() throws Exception {
        testEncodeFailure(messageEmbeddedWhitespace, "embedded whitespace");
    }

    @Test
    public void testEncodeFailureLeadingWhitespace() throws Exception {
        testEncodeFailure(messageLeadingWhitespace, "leading whitespace");
    }

    @Test
    public void testEncodeFailureTrailingWhitespace() throws Exception {
        testEncodeFailure(messageTrailingWhitespace, "trailing whitespace");
    }
    
    @Test
    public void testEncodeWithXPathVariable() throws Exception {
        final RequestXpathAssertion rxa = new RequestXpathAssertion();
        rxa.setVariablePrefix( "MTOM" );
        rxa.setXpathExpression(
                new XpathExpression( "/s12:Envelope/s12:Body/tns:echoFile/arg0/data", new HashMap<String,String>(){{
                    put("s12","http://www.w3.org/2003/05/soap-envelope");
                    put("tns","http://www.layer7tech.com/services/jaxws/echoservice");
                }} ) );

        final MtomEncodeAssertion mea = new MtomEncodeAssertion();
        mea.setAlwaysEncode( true );
        mea.setFailIfNotFound( true );
        mea.setOptimizationThreshold( 0 );
        mea.setXpathExpressions( new XpathExpression[]{
                new XpathExpression( "$MTOM.elements", Collections.<String, String>emptyMap() )
        } );

        new AllAssertion( Arrays.asList(rxa, mea) ); // So variable usage can be discovered

        final ServerRequestXpathAssertion srxa = new ServerRequestXpathAssertion(rxa, null);
        final ServerMtomEncodeAssertion smea = buildServerAssertion( mea );
        final Document requestDoc = XmlUtil.parse( message );
        PolicyEnforcementContext context = null;
        try {
            context = PolicyEnforcementContextFactory.createPolicyEnforcementContext( new Message(requestDoc), null );
            AssertionStatus xpathStatus = srxa.checkRequest( context );
            assertEquals( "status ok", AssertionStatus.NONE, xpathStatus );

            AssertionStatus status = smea.checkRequest( context );
            assertEquals( "status ok", AssertionStatus.NONE, status );

            final String output = XmlUtil.nodeToString( context.getRequest().getXmlKnob().getDocumentReadOnly() );
            assertTrue( "Message raw", message.contains("QUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZ"));
            assertFalse( "Message encoded", output.contains("QUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZ"));
        } finally {
            ResourceUtils.closeQuietly( context );
        }
    }

    @Test
    public void testEncodeToCustomTarget() throws Exception {
        final MtomEncodeAssertion mea = new MtomEncodeAssertion();
        mea.setAlwaysEncode( true );
        mea.setFailIfNotFound( true );
        mea.setOptimizationThreshold( 0 );
        mea.setXpathExpressions( new XpathExpression[]{
                new XpathExpression( "/s12:Envelope/s12:Body/tns:echoFile/arg0/data", new HashMap<String,String>(){{
                    put("s12","http://www.w3.org/2003/05/soap-envelope");
                    put("tns","http://www.layer7tech.com/services/jaxws/echoservice");
                }} )
        } );
        mea.setOutputTarget( new MessageTargetableSupport("output") );

        final ServerMtomEncodeAssertion smea = buildServerAssertion( mea );
        final Document requestDoc = XmlUtil.parse( message );
        PolicyEnforcementContext context = null;
        try {
            context = PolicyEnforcementContextFactory.createPolicyEnforcementContext( new Message(requestDoc), null );
            AssertionStatus status = smea.checkRequest( context );
            assertEquals( "status ok", AssertionStatus.NONE, status );

            final String output = XmlUtil.nodeToString( context.getTargetMessage( new MessageTargetableSupport("output") ).getXmlKnob().getDocumentReadOnly() );
            assertTrue( "Message raw", message.contains("QUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZ"));
            assertFalse( "Message encoded", output.contains("QUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZ"));
        } finally {
            ResourceUtils.closeQuietly( context );
        }
    }

    @Test
    public void testFailIfNotFound() throws Exception {
        final MtomEncodeAssertion mea = new MtomEncodeAssertion();
        mea.setAlwaysEncode( true );
        mea.setFailIfNotFound( true );
        mea.setOptimizationThreshold( 0 );
        mea.setXpathExpressions( new XpathExpression[]{
                new XpathExpression( "/s12:Envelope/s12:Body/tns:echoFile/arg0/daeta", new HashMap<String,String>(){{
                    put("s12","http://www.w3.org/2003/05/soap-envelope");
                    put("tns","http://www.layer7tech.com/services/jaxws/echoservice");
                }} )
        } );

        final ServerMtomEncodeAssertion smea = buildServerAssertion( mea );
        final Document requestDoc = XmlUtil.parse( message );
        PolicyEnforcementContext context = null;
        try {
            context = PolicyEnforcementContextFactory.createPolicyEnforcementContext( new Message(requestDoc), null );
            AssertionStatus status = smea.checkRequest( context );
            assertEquals( "status fail", AssertionStatus.FALSIFIED, status );

            final String output = XmlUtil.nodeToString( context.getRequest().getXmlKnob().getDocumentReadOnly() );
            System.out.println(output);
            assertTrue( "Message raw", message.contains("QUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZ"));
            assertTrue( "Message encoded", output.contains("QUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZ"));
        } finally {
            ResourceUtils.closeQuietly( context );
        }
    }

    @Test
    public void testOptimizationThreshold() throws Exception {
        final MtomEncodeAssertion mea = new MtomEncodeAssertion();
        mea.setAlwaysEncode( true );
        mea.setFailIfNotFound( true );
        mea.setOptimizationThreshold( 2048 );
        mea.setXpathExpressions( new XpathExpression[]{
                new XpathExpression( "/s12:Envelope/s12:Body/tns:echoFile/arg0/data", new HashMap<String,String>(){{
                    put("s12","http://www.w3.org/2003/05/soap-envelope");
                    put("tns","http://www.layer7tech.com/services/jaxws/echoservice");
                }} )
        } );

        final ServerMtomEncodeAssertion smea = buildServerAssertion( mea );
        final Document requestDoc = XmlUtil.parse( message );
        PolicyEnforcementContext context = null;
        try {
            context = PolicyEnforcementContextFactory.createPolicyEnforcementContext( new Message(requestDoc), null );
            AssertionStatus status = smea.checkRequest( context );
            assertEquals( "status fail", AssertionStatus.NONE, status );

            final String output = XmlUtil.nodeToString( context.getRequest().getXmlKnob().getDocumentReadOnly() );
            System.out.println(output);
            assertTrue( "Message raw", message.contains("QUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZ"));
            assertTrue( "Message encoded", output.contains("QUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZ"));
        } finally {
            ResourceUtils.closeQuietly( context );
        }
    }

    private void testEncodeFailure( final String message, final String description ) throws Exception {
        final MtomEncodeAssertion mea = new MtomEncodeAssertion();
        mea.setAlwaysEncode( true );
        mea.setFailIfNotFound( true );
        mea.setOptimizationThreshold( 0 );
        mea.setXpathExpressions( new XpathExpression[]{
                new XpathExpression( "/s12:Envelope/s12:Body/tns:echoFile/arg0/data", new HashMap<String,String>(){{
                    put("s12","http://www.w3.org/2003/05/soap-envelope");
                    put("tns","http://www.layer7tech.com/services/jaxws/echoservice");
                }} )
        } );

        final List<String> messages = new ArrayList<String>();
        final ServerMtomEncodeAssertion smea = buildServerAssertion( mea, messages );
        final Document requestDoc = XmlUtil.parse( message );
        PolicyEnforcementContext context = null;
        try {
            context = PolicyEnforcementContextFactory.createPolicyEnforcementContext( new Message(requestDoc), null );
            AssertionStatus status = smea.checkRequest( context );
            assertEquals( "status falsified for " + description, AssertionStatus.FALSIFIED, status );
            boolean sawAudit = false;
            for ( String auditMessage : messages ) {
                if ( auditMessage.contains("Base64 element content must not include whitespace." )) {
                    sawAudit = true;
                }
            }
            assertTrue("failed due to element whitespace", sawAudit);

        } finally {
            ResourceUtils.closeQuietly( context );
        }
    }

    private ServerMtomEncodeAssertion buildServerAssertion( final MtomEncodeAssertion mea ) {
        return buildServerAssertion( mea, null );
    }

    private ServerMtomEncodeAssertion buildServerAssertion( final MtomEncodeAssertion mea, final List<String> messages ) {
        return new ServerMtomEncodeAssertion(
                mea,
                new LogOnlyAuditor( logger ){
                    @Override
                    public void logAndAudit( final AuditDetailMessage msg, final String[] params, final Throwable e ) {
                        if ( messages != null ) {
                            messages.add( MessageFormat.format( msg.getMessage(), params ) );
                        }
                        super.logAndAudit( msg, params, e );
                    }
                },
                new StashManagerFactory(){
                    @Override
                    public StashManager createStashManager() {
                        return new ByteArrayStashManager();
                    }
                }

        );
    }
}
