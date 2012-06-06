package com.l7tech.console.panels;

import com.l7tech.console.action.Actions;
import com.l7tech.console.event.EntityListener;
import com.l7tech.console.tree.ServicesAndPoliciesTree;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.audit.ExternalAuditsCommonUtils;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyCheckpointState;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.util.HexUtils;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * Wizard that guides the administrator through the publication of a non-soap service.
 */
public class ExternalAuditStoreConfigWizard extends Wizard {
    private static final Logger logger = Logger.getLogger(ExternalAuditStoreConfigWizard.class.getName());

    public static final String INTERNAL_TAG_AUDIT_SINK = "audit-sink";
    public static final String INTERNAL_TAG_AUDIT_LOOKUP = "audit-lookup";

    private PolicyHeader sinkPolicyHeader = null;
    private PolicyHeader lookupPolicyHeader = null;

    public static ExternalAuditStoreConfigWizard getInstance(Window parent) {
        ExternalAuditStoreConfigJdbc panel1 = new ExternalAuditStoreConfigJdbc(new ExternalAuditStoreConfigSchema(null));

        ExternalAuditStoreConfigWizard output = new ExternalAuditStoreConfigWizard(parent, panel1);

        return output;
    }

    public ExternalAuditStoreConfigWizard(Window parent, WizardStepPanel panel) {
        super(parent, panel);
        setTitle("Configure External Audit Sink Wizard");

        getButtonHelp().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Actions.invokeHelp(ExternalAuditStoreConfigWizard.this);
            }
        });

        wizardInput = new ExternalAuditStoreWizardConfig();
    }

    @Override
    protected void finish( final ActionEvent evt ) {
        getSelectedWizardPanel().storeSettings(wizardInput);
        ExternalAuditStoreConfigWizard.ExternalAuditStoreWizardConfig config = (ExternalAuditStoreConfigWizard.ExternalAuditStoreWizardConfig)wizardInput;
        // create policies
        try {
            sinkPolicyHeader = getOrCreateSinkPolicyHeader(config.connection,config.auditRecordTableName,config.auditDetailTableName, config.custom);
            lookupPolicyHeader = getOrCreateLookupPolicyHeader(config.connection,config.auditRecordTableName,config.auditDetailTableName, config.custom);

            super.finish(evt);
        } catch (FindException e) {
            logger.warning("Error creating external sink/lookup policies: "+e.getMessage());
        } catch (PolicyAssertionException e) {
            logger.warning("Error creating external sink/lookup policies: "+e.getMessage());
        } catch (SaveException e) {
            logger.warning("Error creating external sink/lookup policies: "+e.getMessage());
        }



    }
    private PolicyHeader getOrCreateLookupPolicyHeader(String connection, String recordTable, String detailTable, boolean createEmptyPolicy) throws FindException, PolicyAssertionException, SaveException {
        PolicyHeader header = getLookupPolicyHeader();
        if (header == null) {
            // Create new lookup policy with default settings
            Policy policy = makeDefaultAuditLookupPolicyEntity(connection,recordTable,detailTable, createEmptyPolicy);
            PolicyCheckpointState checkpoint = Registry.getDefault().getPolicyAdmin().savePolicy(policy, true);
            policy = Registry.getDefault().getPolicyAdmin().findPolicyByPrimaryKey(checkpoint.getPolicyOid());
            header = new PolicyHeader(policy);

            // Refresh service tree
            ServicesAndPoliciesTree tree = (ServicesAndPoliciesTree) TopComponents.getInstance().getComponent(ServicesAndPoliciesTree.NAME);
            tree.refresh();
        }
        return header;
    }

    private static Policy makeDefaultAuditLookupPolicyEntity(String connection, String recordTable, String detailTable, boolean createEmptyPolicy) {
        Policy policy = new Policy(PolicyType.INTERNAL, "[Internal Audit Lookup Policy]", null, false);
        policy.setInternalTag(INTERNAL_TAG_AUDIT_LOOKUP);
        String theXml = createEmptyPolicy ? getDefaultPolicyXml(policy) : makeDefaultAuditLookupPolicyXml(connection,recordTable,detailTable);

        policy.setXml(theXml);
        return policy;
    }

    private static String defaultLookupQuery(String recordTable) {
        return
            "select top 1024 * from "+recordTable+" where " +
                    "time>=${recordQuery.minTime} and time&lt;${recordQuery.maxTime} " +
                    "and audit_level in (${recordQuery.levels}) " +
                    "and  nodeid like ${recordQuery.nodeId} " +
                    "and type like ${recordQuery.auditType} " +
                    "and lower(name) like lower(${recordQuery.serviceName}) {escape '#' } " +
                    "and lower(user_name) like lower(${recordQuery.userName}) " +
                    "and lower(user_id) like lower(${recordQuery.userIdOrDn}) " +
                    "and lower(entity_class) like lower(${recordQuery.entityClassName}) " +
                    "and entity_id >=${entityIdMin}  " +
                    "and entity_id &lt;=${entityIdMax} " +
                    "and lower(request_id) like lower(${recordQuery.requestId}) order by time desc;";
    }

    private static String messaageIdLookupQuery ( String detailTable){
        return
            "select top 1024 distinct audit_oid from "+detailTable+" where message_id >=${messageIdMin}  and message_id &lt;=${messageIdMax} order by time desc";
    }

    private static String lookupQueryWithAuditId(String recordTable){
        return
            "select top 1024 * from "+recordTable+" where " +
                    "id in (${recordIdQuery.audit_oid}) and " +
                    "time>=${recordQuery.minTime} and time&lt;${recordQuery.maxTime} and " +
                    "audit_level in (${recordQuery.levels}) and " +
                    " nodeid like ${recordQuery.nodeId} and " +
                    "type like ${recordQuery.auditType} and " +
                    "lower(name) like lower(${recordQuery.serviceName}) {escape '#' } " +
                    "and lower(user_name) like lower(${recordQuery.userName}) " +
                    "and lower(user_id) like lower(${recordQuery.userIdOrDn}) " +
                    "and lower(entity_class) like lower(${recordQuery.entityClassName}) " +
                    "and entity_id >=${entityIdMin}  and entity_id &lt;=${entityIdMax} " +
                    "and lower(request_id) like lower(${recordQuery.requestId}) order by time desc;";
    }

    private static String detailLookupQuery (String detailTable){
        return
           "select * from "+detailTable+" where audit_oid in (${recordQuery.id});";
    }

    private static String makeDefaultAuditLookupPolicyXml(String connection, String recordTable, String detailTable) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:CommentAssertion>\n" +
                "            <L7p:Comment stringValue=\"Start Default Lookup Policy\"/>\n" +
                "        </L7p:CommentAssertion>\n" +
                "        <L7p:SetVariable>\n" +
                "            <L7p:Base64Expression stringValue=\""+HexUtils.encodeBase64(HexUtils.encodeUtf8(connection), true)+"\"/>\n" +
                "            <L7p:VariableToSet stringValue=\"auditConnection\"/>\n" +
                "        </L7p:SetVariable>\n" +
                "        <L7p:Regex>\n" +
                "            <L7p:AutoTarget booleanValue=\"false\"/>\n" +
                "            <L7p:OtherTargetMessageVariable stringValue=\"recordQuery.serviceName\"/>\n" +
                "            <L7p:Regex stringValue=\"\\[\"/>\n" +
                "            <L7p:Replace booleanValue=\"true\"/>\n" +
                "            <L7p:Replacement stringValue=\"#[\"/>\n" +
                "            <L7p:Target target=\"OTHER\"/>\n" +
                "        </L7p:Regex>\n" +
                "        <L7p:Regex>\n" +
                "            <L7p:AutoTarget booleanValue=\"false\"/>\n" +
                "            <L7p:CaptureVar stringValue=\"serviceName\"/>\n" +
                "            <L7p:OtherTargetMessageVariable stringValue=\"recordQuery.serviceName\"/>\n" +
                "            <L7p:Regex stringValue=\"\\]\"/>\n" +
                "            <L7p:Replace booleanValue=\"true\"/>\n" +
                "            <L7p:Replacement stringValue=\"#]\"/>\n" +
                "            <L7p:Target target=\"OTHER\"/>\n" +
                "        </L7p:Regex>\n" +
                "        <L7p:SetVariable>\n" +
                "            <L7p:Base64Expression stringValue=\""+HexUtils.encodeBase64(HexUtils.encodeUtf8("10000000000"), true)+"\"/>\n" +
                "            <L7p:VariableToSet stringValue=\"entityIdMax\"/>\n" +
                "        </L7p:SetVariable>\n" +
                "        <L7p:SetVariable>\n" +
                "            <L7p:Base64Expression stringValue=\""+HexUtils.encodeBase64(HexUtils.encodeUtf8("-10000000000"), true)+"\"/>\n" +
                "            <L7p:VariableToSet stringValue=\"entityIdMin\"/>\n" +
                "        </L7p:SetVariable>\n" +
                "        <wsp:OneOrMore wsp:Usage=\"Required\">\n" +
                "            <L7p:ComparisonAssertion>\n" +
                "                <L7p:CaseSensitive booleanValue=\"false\"/>\n" +
                "                <L7p:Expression1 stringValue=\"${recordQuery.entityId}\"/>\n" +
                "                <L7p:Operator operatorNull=\"null\"/>\n" +
                "                <L7p:Predicates predicates=\"included\">\n" +
                "                    <L7p:item dataType=\"included\">\n" +
                "                        <L7p:Type variableDataType=\"string\"/>\n" +
                "                    </L7p:item>\n" +
                "                    <L7p:item binary=\"included\">\n" +
                "                        <L7p:RightValue stringValue=\"%\"/>\n" +
                "                    </L7p:item>\n" +
                "                </L7p:Predicates>\n" +
                "            </L7p:ComparisonAssertion>\n" +
                "            <wsp:All wsp:Usage=\"Required\">\n" +
                "                <L7p:SetVariable>\n" +
                "                    <L7p:Base64Expression stringValue=\"JHtyZWNvcmRRdWVyeS5lbnRpdHlJZH0=\"/>\n" +
                "                    <L7p:VariableToSet stringValue=\"entityIdMax\"/>\n" +
                "                </L7p:SetVariable>\n" +
                "                <L7p:SetVariable>\n" +
                "                    <L7p:Base64Expression stringValue=\"JHtyZWNvcmRRdWVyeS5lbnRpdHlJZH0=\"/>\n" +
                "                    <L7p:VariableToSet stringValue=\"entityIdMin\"/>\n" +
                "                </L7p:SetVariable>\n" +
                "            </wsp:All>\n" +
                "        </wsp:OneOrMore>\n" +
                "        <wsp:OneOrMore wsp:Usage=\"Required\">\n" +
                "            <wsp:All wsp:Usage=\"Required\">\n" +
                "                <L7p:SetVariable>\n" +
                "                    <L7p:Base64Expression stringValue=\"JHtyZWNvcmRRdWVyeS5tZXNzYWdlSWR9\"/>\n" +
                "                    <L7p:VariableToSet stringValue=\"messageId\"/>\n" +
                "                </L7p:SetVariable>\n" +
                "                <L7p:ComparisonAssertion>\n" +
                "                    <L7p:Expression1 stringValue=\"${messageId}\"/>\n" +
                "                    <L7p:Expression2 stringValue=\"\"/>\n" +
                "                    <L7p:Operator operator=\"EMPTY\"/>\n" +
                "                    <L7p:Predicates predicates=\"included\">\n" +
                "                        <L7p:item binary=\"included\">\n" +
                "                            <L7p:Operator operator=\"EMPTY\"/>\n" +
                "                            <L7p:RightValue stringValue=\"\"/>\n" +
                "                        </L7p:item>\n" +
                "                    </L7p:Predicates>\n" +
                "                </L7p:ComparisonAssertion>\n" +
                "                <L7p:JdbcQuery>\n" +
                "                    <L7p:AllowMultiValuedVariables booleanValue=\"true\"/>\n" +
                "                    <L7p:AssertionFailureEnabled booleanValue=\"false\"/>\n" +
                "                    <L7p:ConnectionName stringValue=\"${auditConnection}\"/>\n" +
                "                    <L7p:MaxRecords intValue=\"1000\"/>\n" +
                "                    <L7p:NamingMap mapValue=\"included\">\n" +
                "                        <L7p:entry>\n" +
                "                            <L7p:key stringValue=\"id\"/>\n" +
                "                            <L7p:value stringValue=\"id\"/>\n" +
                "                        </L7p:entry>\n" +
                "                    </L7p:NamingMap>\n" +
                "                    <L7p:SqlQuery stringValue=\""+defaultLookupQuery(recordTable)+"\"/>\n" +
                "                    <L7p:VariablePrefix stringValue=\"recordQuery\"/>\n" +
                "                </L7p:JdbcQuery>\n" +
                "            </wsp:All>\n" +
                "            <wsp:All wsp:Usage=\"Required\">\n" +
                "                <L7p:SetVariable>\n" +
                "                    <L7p:Base64Expression stringValue=\"JHtyZWNvcmRRdWVyeS5tZXNzYWdlSWR9\"/>\n" +
                "                    <L7p:VariableToSet stringValue=\"messageIdMax\"/>\n" +
                "                </L7p:SetVariable>\n" +
                "                <L7p:SetVariable>\n" +
                "                    <L7p:Base64Expression stringValue=\"JHtyZWNvcmRRdWVyeS5tZXNzYWdlSWR9\"/>\n" +
                "                    <L7p:VariableToSet stringValue=\"messageIdMin\"/>\n" +
                "                </L7p:SetVariable>\n" +
                "                <L7p:JdbcQuery>\n" +
                "                    <L7p:AssertionFailureEnabled booleanValue=\"false\"/>\n" +
                "                    <L7p:ConnectionName stringValue=\"${auditConnection}\"/>\n" +
                "                    <L7p:MaxRecords intValue=\"1000\"/>\n" +
                "                    <L7p:SqlQuery stringValue=\""+messaageIdLookupQuery(detailTable)+"\"/>\n" +
                "                    <L7p:VariablePrefix stringValue=\"recordIdQuery\"/>\n" +
                "                </L7p:JdbcQuery>\n" +
                "                <L7p:JdbcQuery>\n" +
                "                    <L7p:AllowMultiValuedVariables booleanValue=\"true\"/>\n" +
                "                    <L7p:AssertionFailureEnabled booleanValue=\"false\"/>\n" +
                "                    <L7p:ConnectionName stringValue=\"${auditConnection}\"/>\n" +
                "                    <L7p:MaxRecords intValue=\"1000\"/>\n" +
                "                    <L7p:NamingMap mapValue=\"included\">\n" +
                "                        <L7p:entry>\n" +
                "                            <L7p:key stringValue=\"id\"/>\n" +
                "                            <L7p:value stringValue=\"id\"/>\n" +
                "                        </L7p:entry>\n" +
                "                    </L7p:NamingMap>\n" +
                "                    <L7p:SqlQuery stringValue=\""+lookupQueryWithAuditId(recordTable)+"\"/>\n" +
                "                    <L7p:VariablePrefix stringValue=\"recordQuery\"/>\n" +
                "                </L7p:JdbcQuery>\n" +
                "            </wsp:All>\n" +
                "        </wsp:OneOrMore>\n" +
                "        <wsp:OneOrMore wsp:Usage=\"Required\">\n" +
                "            <L7p:ComparisonAssertion>\n" +
                "                <L7p:CaseSensitive booleanValue=\"false\"/>\n" +
                "                <L7p:Expression1 stringValue=\"${recordQuery.queryresult.count}\"/>\n" +
                "                <L7p:Operator operatorNull=\"null\"/>\n" +
                "                <L7p:Predicates predicates=\"included\">\n" +
                "                    <L7p:item dataType=\"included\">\n" +
                "                        <L7p:Type variableDataType=\"int\"/>\n" +
                "                    </L7p:item>\n" +
                "                    <L7p:item binary=\"included\">\n" +
                "                        <L7p:RightValue stringValue=\"0\"/>\n" +
                "                    </L7p:item>\n" +
                "                </L7p:Predicates>\n" +
                "            </L7p:ComparisonAssertion>\n" +
                "            <L7p:JdbcQuery>\n" +
                "                <L7p:AllowMultiValuedVariables booleanValue=\"true\"/>\n" +
                "                <L7p:AssertionFailureEnabled booleanValue=\"false\"/>\n" +
                "                <L7p:ConnectionName stringValue=\"${auditConnection}\"/>\n" +
                "                <L7p:MaxRecords intValue=\"1000\"/>\n" +
                "                <L7p:SqlQuery stringValue=\""+detailLookupQuery(detailTable)+"\"/>\n" +
                "                <L7p:VariablePrefix stringValue=\"detailQuery0\"/>\n" +
                "            </L7p:JdbcQuery>\n" +
                "        </wsp:OneOrMore>\n" +
                "        <L7p:CommentAssertion>\n" +
                "            <L7p:Comment stringValue=\"End Default Lookup Policy\"/>\n" +
                "        </L7p:CommentAssertion>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>\n";
    }

    public PolicyHeader getLookupPolicyHeader() {
        return lookupPolicyHeader;
    }

    public PolicyHeader getSinkPolicyHeader() {
        return sinkPolicyHeader;
    }
    private PolicyHeader getOrCreateSinkPolicyHeader(String connection, String recordTable, String detailTable, boolean createEmptyPolicy) throws FindException, PolicyAssertionException, SaveException {
        PolicyHeader header = getSinkPolicyHeader();
        if (header == null) {
            // Create new sink policy with default settings
            Policy policy = makeDefaultAuditSinkPolicyEntity(connection,recordTable,detailTable,createEmptyPolicy);
            PolicyCheckpointState checkpoint = Registry.getDefault().getPolicyAdmin().savePolicy(policy, true);
            policy = Registry.getDefault().getPolicyAdmin().findPolicyByPrimaryKey(checkpoint.getPolicyOid());
            header = new PolicyHeader(policy);

            // Refresh service tree
            ServicesAndPoliciesTree tree = (ServicesAndPoliciesTree) TopComponents.getInstance().getComponent(ServicesAndPoliciesTree.NAME);
            tree.refresh();
        }
        return header;
    }

    private static Policy makeDefaultAuditSinkPolicyEntity(String connection, String recordTable, String detailTable, boolean createEmptyPolicy) {
        Policy policy = new Policy(PolicyType.INTERNAL, "[Internal Audit Sink Policy]", null, false);
        policy.setInternalTag(INTERNAL_TAG_AUDIT_SINK);

        String theXml = createEmptyPolicy ? getSendToHttpSinkPolicyXml() : makeDefaultAuditSinkPolicyXml(connection,recordTable,detailTable);

        policy.setXml(theXml);
        return policy;
    }

    private static String getSendToHttpSinkPolicyXml(){
        AllAssertion all = new AllAssertion();
        all.addChild(new CommentAssertion("A simple audit sink policy could convert the audit record to XML, then post it somewhere via HTTP"));
        all.addChild(new AuditRecordToXmlAssertion());
        all.addChild(new CommentAssertion("Enable the below routing assertion or replace it with something else"));
        HttpRoutingAssertion routingAssertion = new HttpRoutingAssertion();
        routingAssertion.setProtectedServiceUrl("${gateway.audit.sink.url}");
        routingAssertion.setEnabled(false);
        all.addChild(routingAssertion);
        FalseAssertion falseAssertion = new FalseAssertion();
        String falseName = falseAssertion.meta().get(AssertionMetadata.SHORT_NAME);
        all.addChild(new CommentAssertion("The below " + falseName + " causes this sink policy to always fail."));
        all.addChild(new CommentAssertion("This will (by default) cause the record to be saved to the internal audit database."));
        all.addChild(new CommentAssertion("Remove it once you have customized the audit sink policy."));
        all.addChild(falseAssertion);
        return WspWriter.getPolicyXml(all);
    }

    private static String getDefaultPolicyXml(Policy policy){
        String defaultPolicyXml =  Registry.getDefault().getPolicyAdmin().getDefaultPolicyXml(policy.getType(),policy.getInternalTag());
        String xml = (defaultPolicyXml != null)? defaultPolicyXml: WspWriter.getPolicyXml(
                new AllAssertion(Arrays.<Assertion>asList(new AuditDetailAssertion("Internal Policy: " + policy.getName()))));
        return xml;
    }
    private static String makeDefaultAuditSinkPolicyXml(String connection, String recordTable, String detailTable) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:CommentAssertion>\n" +
                "            <L7p:Comment stringValue=\"Start default audit sink policy\"/>\n" +
                "        </L7p:CommentAssertion>\n" +
                "        <L7p:SetVariable>\n" +
                "            <L7p:Base64Expression stringValue=\""+HexUtils.encodeBase64(HexUtils.encodeUtf8(connection), true)+"\"/>\n" +
                "            <L7p:VariableToSet stringValue=\"auditConnection\"/>\n" +
                "        </L7p:SetVariable>\n" +
                "        <L7p:UUIDGenerator>\n" +
                "            <L7p:TargetVariable stringValue=\"record.guid\"/>\n" +
                "        </L7p:UUIDGenerator>\n" +
                "        <L7p:JdbcQuery>\n" +
                "            <L7p:AssertionFailureEnabled booleanValue=\"false\"/>\n" +
                "            <L7p:ConnectionName stringValue=\"${auditConnection}\"/>\n" +
                "            <L7p:SqlQuery stringValue=\""+ExternalAuditsCommonUtils.saveRecordQuery(recordTable)+"\"/>\n" +
                "        </L7p:JdbcQuery>\n" +
                "        <wsp:OneOrMore wsp:Usage=\"Required\">\n" +
                "            <L7p:ComparisonAssertion>\n" +
                "                <L7p:CaseSensitive booleanValue=\"false\"/>\n" +
                "                <L7p:Expression1 stringValue=\"${audit.numDetails}\"/>\n" +
                "                <L7p:Operator operatorNull=\"null\"/>\n" +
                "                <L7p:Predicates predicates=\"included\">\n" +
                "                    <L7p:item dataType=\"included\">\n" +
                "                        <L7p:Type variableDataType=\"int\"/>\n" +
                "                    </L7p:item>\n" +
                "                    <L7p:item binary=\"included\">\n" +
                "                        <L7p:RightValue stringValue=\"0\"/>\n" +
                "                    </L7p:item>\n" +
                "                </L7p:Predicates>\n" +
                "            </L7p:ComparisonAssertion>\n" +
                "            <L7p:ForEachLoop L7p:Usage=\"Required\"\n" +
                "                loopVariable=\"audit.details\" variablePrefix=\"i\">\n" +
                "                <L7p:UUIDGenerator>\n" +
                "                    <L7p:TargetVariable stringValue=\"detail.guid\"/>\n" +
                "                </L7p:UUIDGenerator>\n" +
                "                <L7p:JdbcQuery>\n" +
                "                    <L7p:AssertionFailureEnabled booleanValue=\"false\"/>\n" +
                "                    <L7p:ConnectionName stringValue=\"${auditConnection}\"/>\n" +
                "                    <L7p:SqlQuery stringValue=\""+ExternalAuditsCommonUtils.saveDetailQuery(detailTable)+"\"/>\n" +
                "                </L7p:JdbcQuery>\n" +
                "            </L7p:ForEachLoop>\n" +
                "        </wsp:OneOrMore>\n" +
                "        <L7p:CommentAssertion>\n" +
                "            <L7p:Comment stringValue=\"End default audit sink policy\"/>\n" +
                "        </L7p:CommentAssertion>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>\n" +
                "";
    }

    /**
     * add the EntityListener
     *
     * @param listener the EntityListener
     */
    public void addEntityListener(EntityListener listener) {
        listenerList.add(EntityListener.class, listener);
    }

    /**
     * remove the the EntityListener
     *
     * @param listener the EntityListener
     */
    public void removeEntityListener(EntityListener listener) {
        listenerList.remove(EntityListener.class, listener);
    }

    public class ExternalAuditStoreWizardConfig {
        public boolean custom;  // crate custom policies
        public String connection;
        public String auditRecordTableName = null;
        public String auditDetailTableName = null;
    }
}
