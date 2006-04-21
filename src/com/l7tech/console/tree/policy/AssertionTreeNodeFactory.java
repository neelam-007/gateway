package com.l7tech.console.tree.policy;

import com.l7tech.common.util.ConstructorInvocation;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.alert.EmailAlertAssertion;
import com.l7tech.policy.assertion.alert.SnmpTrapAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.assertion.credential.WsTrustCredentialExchange;
import com.l7tech.policy.assertion.credential.XpathCredentialSource;
import com.l7tech.policy.assertion.credential.WsFederationPassiveTokenRequest;
import com.l7tech.policy.assertion.credential.WsFederationPassiveTokenExchange;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.credential.http.HttpDigest;
import com.l7tech.policy.assertion.credential.wss.WssBasic;
import com.l7tech.policy.assertion.credential.wss.EncryptedUsernameTokenAssertion;
import com.l7tech.policy.assertion.identity.MemberOfGroup;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.policy.assertion.identity.MappingAssertion;
import com.l7tech.policy.assertion.sla.ThroughputQuota;
import com.l7tech.policy.assertion.xml.SchemaValidation;
import com.l7tech.policy.assertion.xml.XslTransformation;
import com.l7tech.policy.assertion.xmlsec.*;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;


/**
 * The class <code>AssertionTreeNodeFactory</code> is a factory
 * class that creates policy <code>TreeNode</code> instances based on
 * <code>Assertion</code> instances.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.1
 */
public class AssertionTreeNodeFactory {
    private static Map assertionMap = new HashMap();

    // maping assertions to assertion tree nodes
    static {
        assertionMap.put(SslAssertion.class, SslAssertionTreeNode.class);
        assertionMap.put(SpecificUser.class, SpecificUserAssertionTreeNode.class);
        assertionMap.put(MemberOfGroup.class, MemberOfGroupAssertionTreeNode.class);
        assertionMap.put(OneOrMoreAssertion.class, OneOrMoreAssertionTreeNode.class);
        assertionMap.put(AllAssertion.class, AllAssertionTreeNode.class);
        assertionMap.put(TrueAssertion.class, TrueAssertionPolicyNode.class);
        assertionMap.put(FalseAssertion.class, FalseAssertionPolicyNode.class);

        assertionMap.put(HttpBasic.class, HttpBasicAuthAssertionTreeNode.class);
        assertionMap.put(HttpDigest.class, HttpDigestAuthAssertionTreeNode.class);
        assertionMap.put(WssBasic.class, WssBasicAuthAssertionTreeNode.class);
        assertionMap.put(EncryptedUsernameTokenAssertion.class, EncryptedUsernameTokenAssertionTreeNode.class);
        assertionMap.put(RequestWssX509Cert.class, RequestWssX509CertTreeNode.class);
        assertionMap.put(SecureConversation.class, SecureConversationTreeNode.class);
        assertionMap.put(RequestWssReplayProtection.class, RequestWssReplayProtectionTreeNode.class);

        assertionMap.put(RoutingAssertion.class, HttpRoutingAssertionTreeNode.class);
        assertionMap.put(HttpRoutingAssertion.class, HttpRoutingAssertionTreeNode.class);
        assertionMap.put(BridgeRoutingAssertion.class, BridgeRoutingAssertionTreeNode.class);
        assertionMap.put(JmsRoutingAssertion.class, JmsRoutingAssertionTreeNode.class);
        assertionMap.put(EchoRoutingAssertion.class, EchoRoutingAssertionPolicyNode.class);

        assertionMap.put(RequestWssIntegrity.class, RequestWssIntegrityTreeNode.class);
        assertionMap.put(ResponseWssIntegrity.class, ResponseWssIntegrityTreeNode.class);
        assertionMap.put(RequestWssConfidentiality.class, RequestWssConfidentialityTreeNode.class);
        assertionMap.put(ResponseWssConfidentiality.class, ResponseWssConfidentialityTreeNode.class);
        assertionMap.put(RequestXpathAssertion.class, RequestXpathPolicyTreeNode.class);
        assertionMap.put(ResponseXpathAssertion.class, ResponseXpathPolicyTreeNode.class);
        assertionMap.put(RequestSwAAssertion.class, RequestSwAAssertionPolicyTreeNode.class);
        assertionMap.put(SchemaValidation.class, SchemaValidationTreeNode.class);
        assertionMap.put(XslTransformation.class, XslTransformationTreeNode.class);
        assertionMap.put(TimeRange.class, TimeRangeTreeNode.class);
        assertionMap.put(SnmpTrapAssertion.class, SnmpTrapAssertionTreeNode.class);
        assertionMap.put(EmailAlertAssertion.class, EmailAlertAssertionTreeNode.class);
        assertionMap.put(RemoteIpRange.class, RemoteIpRangeTreeNode.class);
        assertionMap.put(CustomAssertionHolder.class, CustomAssertionTreeNode.class);
        assertionMap.put(AuditAssertion.class, AuditAssertionTreeNode.class);
        assertionMap.put(AuditDetailAssertion.class, AuditDetailAssertionTreeNode.class);
        assertionMap.put(RequestWssSaml.class, RequestWssSamlTreeNode.class);
        assertionMap.put(WsTrustCredentialExchange.class, WsTrustCredentialExchangeTreeNode.class);
        assertionMap.put(WsFederationPassiveTokenExchange.class, WsFederationPassiveTokenExchangeTreeNode.class);
        assertionMap.put(WsFederationPassiveTokenRequest.class, WsFederationPassiveTokenRequestTreeNode.class);
        assertionMap.put(XpathCredentialSource.class, XpathCredentialSourceTreeNode.class);
        assertionMap.put(SamlBrowserArtifact.class, SamlBrowserArtifactTreeNode.class);
        assertionMap.put(Regex.class, RegexPolicyNode.class);
        assertionMap.put(ThroughputQuota.class, ThroughputQuotaTreeNode.class);
        assertionMap.put(HttpFormPost.class, HttpFormPostPolicyNode.class);
        assertionMap.put(InverseHttpFormPost.class, InverseHttpFormPostPolicyNode.class);
        assertionMap.put(CommentAssertion.class, CommentAssertionPolicyNode.class);
        assertionMap.put(ComparisonAssertion.class, ComparisonAssertionPolicyNode.class);
        assertionMap.put(StealthFault.class, StealthFaultTreeNode.class);
        assertionMap.put(SqlAttackAssertion.class, SqlAttackAssertionTreeNode.class);
        assertionMap.put(OversizedTextAssertion.class, OversizedTextAssertionTreeNode.class);
        assertionMap.put(RequestSizeLimit.class, RequestSizeLimitTreeNode.class);
        assertionMap.put(ResponseWssTimestamp.class, ResponseWssTimestampPolicyNode.class);
        assertionMap.put(RequestWssTimestamp.class, RequestWssTimestampPolicyNode.class);
        assertionMap.put(WssTimestamp.class, WssTimestampPolicyNode.class);
        assertionMap.put(HardcodedResponseAssertion.class, HardcodedResponseTreeNode.class);
        assertionMap.put(ResponseWssSecurityToken.class, ResponseWssSecurityTokenPolicyNode.class);
        assertionMap.put(RequestWssKerberos.class, KerberosTreeNode.class);
        assertionMap.put(MappingAssertion.class, MappingAssertionPolicyNode.class);
        assertionMap.put(WsiBspAssertion.class, WsiBspAssertionPolicyNode.class);
        assertionMap.put(WsiSamlAssertion.class, WsiSamlAssertionPolicyNode.class);
        assertionMap.put(WsspAssertion.class, WsspAssertionPolicyNode.class);
    }

    /**
     * private constructor, this class cannot be instantiated
     */
    private AssertionTreeNodeFactory() {
    }

    /**
     * Returns the corresponding <code>AssertionTreeNode</code> instance
     * for an <code>Assertion</code><br>
     * In case there is no corresponding <code>AssertionTreeNode</code>
     * the <code>UnknownAssertionTreeNode</code> is returned
     * 
     * @return the AssertionTreeNode for a given assertion
     */
    public static AssertionTreeNode asTreeNode(Assertion assertion) {
        if (assertion == null) {
            throw new IllegalArgumentException();
        }
        // assertion lookup, find the  assertion tree node
        Class classNode = (Class)assertionMap.get(assertion.getClass());
        if (null == classNode) {
            return new UnknownAssertionTreeNode(assertion);
        }
        try {
            return makeAssertionNode(classNode, assertion);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Create the assertion tree node <code>classNode</code> using reflection.
     * The target class is searched for the constructor that accepts the assertion
     * as a parameter.
     * That is, the <code>AssertionTreeNode</code> subclass is searched for
     * the constructor accepting the <code>Aseertion</code> parameter.
     * 
     * @param classNode the class that is a subclass of AssertionTreeNode
     * @param assertion the assertion constructor parameter
     * @return the corresponding assertion tree node
     * @throws InstantiationException    thrown on error instantiating the assertion
     *                                   tree node
     * @throws InvocationTargetException thrown on error invoking the constructor
     * @throws IllegalAccessException    thrown if there is no access to the desired
     *                                   constructor
     */
    private static AssertionTreeNode makeAssertionNode(Class classNode, Assertion assertion)
      throws InstantiationException, InvocationTargetException, IllegalAccessException {

        ConstructorInvocation ci = new ConstructorInvocation(classNode, new Object[]{assertion});
        return (AssertionTreeNode)ci.invoke();
    }

    /**
     * special assertion tree node that describesd unknown assertion
     */
    static class UnknownAssertionTreeNode extends LeafAssertionTreeNode {
        String name = null;

        public UnknownAssertionTreeNode(Assertion assertion) {
            super(assertion);
            if (assertion instanceof UnknownAssertion) {
                UnknownAssertion unknownAssertion = (UnknownAssertion)assertion;
                name = unknownAssertion.getDetailMessage();
            }
            if (name == null) {
                name = "Unknown assertion class '" + assertion.getClass() + "'";
            }
        }

        /**
         * subclasses override this method specifying the resource name
         * 
         * @param open for nodes that can be opened, can have children
         */
        protected String iconResource(boolean open) {
            return "com/l7tech/console/resources/unknown.gif";
        }

        /**
         * Override can delete
         * 
         * @return always true
         */
        public boolean canDelete() {
            return true;
        }

        public String getName() {
            return name;
        }
    }
}
