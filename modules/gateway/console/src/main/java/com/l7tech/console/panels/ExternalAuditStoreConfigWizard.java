package com.l7tech.console.panels;

import com.l7tech.console.action.Actions;
import com.l7tech.console.event.EntityListener;
import com.l7tech.console.tree.ServicesAndPoliciesTree;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyCheckpointState;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.assertion.*;
import com.l7tech.util.HexUtils;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
            sinkPolicyHeader = getOrCreateSinkPolicyHeader(config.connection,config.auditRecordTableName,config.auditDetailTableName);
            lookupPolicyHeader = getOrCreateLookupPolicyHeader(config.connection,config.auditRecordTableName,config.auditDetailTableName);

            super.finish(evt);
        } catch (FindException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (PolicyAssertionException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (SaveException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }



    }
    private PolicyHeader getOrCreateLookupPolicyHeader(String connection, String recordTable, String detailTable) throws FindException, PolicyAssertionException, SaveException {
        PolicyHeader header = getLookupPolicyHeader();
        if (header == null) {
            // Create new lookup policy with default settings
            Policy policy = makeDefaultAuditLookupPolicyEntity(connection,recordTable,detailTable);
            PolicyCheckpointState checkpoint = Registry.getDefault().getPolicyAdmin().savePolicy(policy, true);
            policy = Registry.getDefault().getPolicyAdmin().findPolicyByPrimaryKey(checkpoint.getPolicyOid());
            header = new PolicyHeader(policy);

            // Refresh service tree
            ServicesAndPoliciesTree tree = (ServicesAndPoliciesTree) TopComponents.getInstance().getComponent(ServicesAndPoliciesTree.NAME);
            tree.refresh();
        }
        return header;
    }

    private static Policy makeDefaultAuditLookupPolicyEntity(String connection, String recordTable, String detailTable) {
        String theXml = makeDefaultAuditLookupPolicyXml(connection,recordTable,detailTable);
        Policy policy = new Policy(PolicyType.INTERNAL, "[Internal Audit Lookup Policy]", theXml, false);
        policy.setInternalTag(INTERNAL_TAG_AUDIT_LOOKUP);
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
                    "and lower(request_id) like lower(${recordQuery.requestId});";
    }

    private static String messaageIdLookupQuery ( String detailTable){
        return
            "select distinct audit_oid from "+detailTable+" where message_id >=${messageIdMin}  and message_id &lt;=${messageIdMax}";
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
                    "and lower(request_id) like lower(${recordQuery.requestId}) ;";
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
    private PolicyHeader getOrCreateSinkPolicyHeader(String connection, String recordTable, String detailTable) throws FindException, PolicyAssertionException, SaveException {
        PolicyHeader header = getSinkPolicyHeader();
        if (header == null) {
            // Create new sink policy with default settings
            Policy policy = makeDefaultAuditSinkPolicyEntity(connection,recordTable,detailTable);
            PolicyCheckpointState checkpoint = Registry.getDefault().getPolicyAdmin().savePolicy(policy, true);
            policy = Registry.getDefault().getPolicyAdmin().findPolicyByPrimaryKey(checkpoint.getPolicyOid());
            header = new PolicyHeader(policy);

            // Refresh service tree
            ServicesAndPoliciesTree tree = (ServicesAndPoliciesTree) TopComponents.getInstance().getComponent(ServicesAndPoliciesTree.NAME);
            tree.refresh();
        }
        return header;
    }

    private static Policy makeDefaultAuditSinkPolicyEntity(String connection, String recordTable, String detailTable) {
        String theXml = makeDefaultAuditSinkPolicyXml(connection,recordTable,detailTable);
        Policy policy = new Policy(PolicyType.INTERNAL, "[Internal Audit Sink Policy]", theXml, false);
        policy.setInternalTag(INTERNAL_TAG_AUDIT_SINK);
        return policy;
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
                "            <L7p:SqlQuery stringValue=\""+saveRecordQuery(recordTable)+"\"/>\n" +
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
                "                    <L7p:SqlQuery stringValue=\""+saveDetailQuery(detailTable)+"\"/>\n" +
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


    private static String saveDetailQuery(String detailTable){
        return "insert into "+detailTable +"(id, audit_oid,time,component_id,ordinal,message_id,exception_message,properties) values " +
                "(${detail.guid},${record.guid},${i.current.time},${i.current.componentId},${i.current.ordinal},${i.current.messageId},${i.current.exception},${i.current.properties});";
    }

    private static String saveRecordQuery(String recordTable){
        return  "insert into "+recordTable+"(id,nodeid,time,type,audit_level,name,message,ip_address,user_name,user_id,provider_oid,signature,properties," +
            "entity_class,entity_id," +
            "status,request_id,service_oid,operation_name,authenticated,authenticationType,request_length,response_length,request_xml,response_xml,response_status,routing_latency," +
            "component_id,action)" +
            " values " +
            "(${record.guid},${audit.nodeId},${audit.time},${audit.type},${audit.level},${audit.name},${audit.message},${audit.ipAddress},${audit.user.name},${audit.user.id},${audit.user.idProv},${audit.signature},${audit.properties}," +
            "${audit.entity.class},${audit.entity.oid}," +
            "${audit.status},${audit.requestId},${audit.serviceOid},${audit.operationName},${audit.authenticated},${audit.authType},${audit.request.size},${audit.response.size},${audit.reqZip},${audit.resZip},${audit.responseStatus},${audit.routingLatency}," +
            "${audit.componentId},${audit.action});";
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
        public String connection;
        public String auditRecordTableName = null;
        public String auditDetailTableName = null;
    }
}
