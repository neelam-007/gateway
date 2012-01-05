package com.l7tech.policy.assertion;

import com.l7tech.policy.wsp.WspReader;
import org.junit.Test;

import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Unit tests for Http Routing assertion.
 *
 * @author Wlui
 */
public class HttpRoutingAssertionTest {

    private String httpRoutingAssertionXml_6_1 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <wsp:All wsp:Usage=\"Required\">\n" +
            "        <L7p:HttpRoutingAssertion>\n" +
            "            <L7p:ConnectionTimeout boxedIntegerValue=\"10\"/>\n" +
            "            <L7p:Timeout boxedIntegerValue=\"13\"/>\n" +
            "            <L7p:CurrentSecurityHeaderHandling intValue=\"3\"/>\n" +
            "            <L7p:FailOnErrorStatus booleanValue=\"false\"/>\n" +
            "            <L7p:ProtectedServiceUrl stringValue=\"http://paris.l7tech.com/blah\"/>\n" +
            "            <L7p:ProxyPassword stringValueNull=\"null\"/>\n" +
            "            <L7p:ProxyUsername stringValueNull=\"null\"/>\n" +
            "            <L7p:RequestHeaderRules httpPassthroughRuleSet=\"included\">\n" +
            "                <L7p:Rules httpPassthroughRules=\"included\">\n" +
            "                    <L7p:item httpPassthroughRule=\"included\">\n" +
            "                        <L7p:Name stringValue=\"Cookie\"/>\n" +
            "                    </L7p:item>\n" +
            "                    <L7p:item httpPassthroughRule=\"included\">\n" +
            "                        <L7p:Name stringValue=\"SOAPAction\"/>\n" +
            "                    </L7p:item>\n" +
            "                </L7p:Rules>\n" +
            "            </L7p:RequestHeaderRules>\n" +
            "            <L7p:RequestParamRules httpPassthroughRuleSet=\"included\">\n" +
            "                <L7p:ForwardAll booleanValue=\"true\"/>\n" +
            "                <L7p:Rules httpPassthroughRules=\"included\"/>\n" +
            "            </L7p:RequestParamRules>\n" +
            "            <L7p:ResponseHeaderRules httpPassthroughRuleSet=\"included\">\n" +
            "                <L7p:ForwardAll booleanValue=\"true\"/>\n" +
            "                <L7p:Rules httpPassthroughRules=\"included\">\n" +
            "                    <L7p:item httpPassthroughRule=\"included\">\n" +
            "                        <L7p:Name stringValue=\"Set-Cookie\"/>\n" +
            "                    </L7p:item>\n" +
            "                </L7p:Rules>\n" +
            "            </L7p:ResponseHeaderRules>\n" +
            "            <L7p:ResponseSize stringValue=\"0\"/>\n" +
            "            <L7p:SamlAssertionVersion intValue=\"2\"/>\n" +
            "        </L7p:HttpRoutingAssertion>\n" +
            "    </wsp:All>\n" +
            "</wsp:Policy>";

    private String httpRoutingAssertionXml_6_1_5 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <wsp:All wsp:Usage=\"Required\">\n" +
            "        <L7p:HttpRoutingAssertion>\n" +
            "            <L7p:ConnectionTimeout stringValue=\"1\"/>\n" +
            "            <L7p:Timeout stringValue=\"2\"/>\n" +
            "            <L7p:CurrentSecurityHeaderHandling intValue=\"3\"/>\n" +
            "            <L7p:FailOnErrorStatus booleanValue=\"false\"/>\n" +
            "            <L7p:ProtectedServiceUrl stringValue=\"http://paris.l7tech.com/blah\"/>\n" +
            "            <L7p:ProxyPassword stringValueNull=\"null\"/>\n" +
            "            <L7p:ProxyUsername stringValueNull=\"null\"/>\n" +
            "            <L7p:RequestHeaderRules httpPassthroughRuleSet=\"included\">\n" +
            "                <L7p:Rules httpPassthroughRules=\"included\">\n" +
            "                    <L7p:item httpPassthroughRule=\"included\">\n" +
            "                        <L7p:Name stringValue=\"Cookie\"/>\n" +
            "                    </L7p:item>\n" +
            "                    <L7p:item httpPassthroughRule=\"included\">\n" +
            "                        <L7p:Name stringValue=\"SOAPAction\"/>\n" +
            "                    </L7p:item>\n" +
            "                </L7p:Rules>\n" +
            "            </L7p:RequestHeaderRules>\n" +
            "            <L7p:RequestParamRules httpPassthroughRuleSet=\"included\">\n" +
            "                <L7p:ForwardAll booleanValue=\"true\"/>\n" +
            "                <L7p:Rules httpPassthroughRules=\"included\"/>\n" +
            "            </L7p:RequestParamRules>\n" +
            "            <L7p:ResponseHeaderRules httpPassthroughRuleSet=\"included\">\n" +
            "                <L7p:ForwardAll booleanValue=\"true\"/>\n" +
            "                <L7p:Rules httpPassthroughRules=\"included\">\n" +
            "                    <L7p:item httpPassthroughRule=\"included\">\n" +
            "                        <L7p:Name stringValue=\"Set-Cookie\"/>\n" +
            "                    </L7p:item>\n" +
            "                </L7p:Rules>\n" +
            "            </L7p:ResponseHeaderRules>\n" +
            "            <L7p:ResponseSize stringValue=\"0\"/>\n" +
            "            <L7p:SamlAssertionVersion intValue=\"2\"/>\n" +
            "        </L7p:HttpRoutingAssertion>\n" +
            "    </wsp:All>\n" +
            "</wsp:Policy>" ;

    private String httpRoutingAssertionXml_6_1_5_ctxVar = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <wsp:All wsp:Usage=\"Required\">\n" +
            "        <L7p:HttpRoutingAssertion>\n" +
            "            <L7p:ConnectionTimeout stringValue=\"${timeout}\"/>\n" +
            "            <L7p:Timeout stringValue=\"${timeout1}\"/>\n" +
            "            <L7p:CurrentSecurityHeaderHandling intValue=\"3\"/>\n" +
            "            <L7p:FailOnErrorStatus booleanValue=\"false\"/>\n" +
            "            <L7p:ProtectedServiceUrl stringValue=\"http://paris.l7tech.com/blah\"/>\n" +
            "            <L7p:ProxyPassword stringValueNull=\"null\"/>\n" +
            "            <L7p:ProxyUsername stringValueNull=\"null\"/>\n" +
            "            <L7p:RequestHeaderRules httpPassthroughRuleSet=\"included\">\n" +
            "                <L7p:Rules httpPassthroughRules=\"included\">\n" +
            "                    <L7p:item httpPassthroughRule=\"included\">\n" +
            "                        <L7p:Name stringValue=\"Cookie\"/>\n" +
            "                    </L7p:item>\n" +
            "                    <L7p:item httpPassthroughRule=\"included\">\n" +
            "                        <L7p:Name stringValue=\"SOAPAction\"/>\n" +
            "                    </L7p:item>\n" +
            "                </L7p:Rules>\n" +
            "            </L7p:RequestHeaderRules>\n" +
            "            <L7p:RequestParamRules httpPassthroughRuleSet=\"included\">\n" +
            "                <L7p:ForwardAll booleanValue=\"true\"/>\n" +
            "                <L7p:Rules httpPassthroughRules=\"included\"/>\n" +
            "            </L7p:RequestParamRules>\n" +
            "            <L7p:ResponseHeaderRules httpPassthroughRuleSet=\"included\">\n" +
            "                <L7p:ForwardAll booleanValue=\"true\"/>\n" +
            "                <L7p:Rules httpPassthroughRules=\"included\">\n" +
            "                    <L7p:item httpPassthroughRule=\"included\">\n" +
            "                        <L7p:Name stringValue=\"Set-Cookie\"/>\n" +
            "                    </L7p:item>\n" +
            "                </L7p:Rules>\n" +
            "            </L7p:ResponseHeaderRules>\n" +
            "            <L7p:ResponseSize stringValue=\"0\"/>\n" +
            "            <L7p:SamlAssertionVersion intValue=\"2\"/>\n" +
            "        </L7p:HttpRoutingAssertion>\n" +
            "    </wsp:All>\n" +
            "</wsp:Policy>" ;

    /**
     * Test compatibility from version 6.1 assertion.
     */
    @Test
    public void test61toCurrent() throws Exception {
        testPolicyConnectionMsValue(httpRoutingAssertionXml_6_1, "10000");
        testPolicyTimeoutMsValue(httpRoutingAssertionXml_6_1, "13000");
    }

    /**
     * Test compatibility from version 6.1.5 assertions.
     */
    @Test
    public void test615toCurrent() throws Exception {
        testPolicyConnectionMsValue(httpRoutingAssertionXml_6_1_5, "1000");
        testPolicyTimeoutMsValue(httpRoutingAssertionXml_6_1_5, "2000");
        testPolicyConnectionMsValue(httpRoutingAssertionXml_6_1_5_ctxVar,"${timeout}");
        testPolicyTimeoutMsValue(httpRoutingAssertionXml_6_1_5_ctxVar, "${timeout1}");
    }


    /**
     * Test connection timeout  ms value
     */
    private void testPolicyConnectionMsValue(final String policyXml,
                                  final String connectionTimeOutInMS) throws Exception {
        Assertion policy = WspReader.getDefault().parseStrictly(policyXml, WspReader.INCLUDE_DISABLED);

        boolean hasAssertion = false;

        Iterator<Assertion> assertions = policy.preorderIterator();
        while ( assertions.hasNext() ) {
            Assertion assertion = assertions.next();

            if( assertion instanceof HttpRoutingAssertion)
            {
                HttpRoutingAssertion hra = (HttpRoutingAssertion) assertion;
                assertEquals("Connection timeout in ms",connectionTimeOutInMS,hra.getConnectionTimeoutMs());
                hasAssertion = true;
            }
        }

        if ( !hasAssertion ) {
            fail("No Http Routing assertion found");
        }

    }

    /**
     * Test timeout  ms value
     */
    private void testPolicyTimeoutMsValue(final String policyXml,
                                  final String timeOutInMS) throws Exception {
        Assertion policy = WspReader.getDefault().parseStrictly(policyXml, WspReader.INCLUDE_DISABLED);

        boolean hasAssertion = false;

        Iterator<Assertion> assertions = policy.preorderIterator();
        while ( assertions.hasNext() ) {
            Assertion assertion = assertions.next();

            if( assertion instanceof HttpRoutingAssertion)
            {
                HttpRoutingAssertion hra = (HttpRoutingAssertion) assertion;
                assertEquals("Timeout in ms",timeOutInMS,hra.getTimeoutMs());
                hasAssertion = true;
            }
        }

        if ( !hasAssertion ) {
            fail("No Http Routing assertion found");
        }

    }
}
