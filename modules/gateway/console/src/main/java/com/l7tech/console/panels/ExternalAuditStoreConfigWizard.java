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

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Wizard that guides the administrator through the publication of a non-soap service.
 */
public class ExternalAuditStoreConfigWizard extends Wizard {
    private static final Logger logger = Logger.getLogger(ExternalAuditStoreConfigWizard.class.getName());

    public static final String INTERNAL_TAG_AUDIT_SINK = "audit-sink";
    public static final String INTERNAL_TAG_AUDIT_LOOKUP = "audit-lookup";

    public static final Pattern STRICT_CONNECTION_NAME_PATTERN = Pattern.compile("[xA-Za-z][\\sA-Za-z0-9_\\-]*");

    private PolicyHeader sinkPolicyHeader = null;
    private PolicyHeader lookupPolicyHeader = null;

    public static ExternalAuditStoreConfigWizard getInstance(Window parent,PolicyHeader sinkPolicyHeader,PolicyHeader lookupPolicyHeader) {
        ExternalAuditStoreConfigJdbc panel1 = new ExternalAuditStoreConfigJdbc(new ExternalAuditStoreConfigDatabase(new ExternalAuditStoreConfigSchema(null)));
        ExternalAuditStoreConfigWizard output = new ExternalAuditStoreConfigWizard(parent, panel1,sinkPolicyHeader,lookupPolicyHeader);

        return output;
    }

    protected ExternalAuditStoreConfigWizard(Window parent, WizardStepPanel panel,PolicyHeader sinkPolicyHeader,PolicyHeader lookupPolicyHeader) {
        super(parent, panel);
        setTitle("Configure External Audit Store Wizard");

        getButtonHelp().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Actions.invokeHelp(ExternalAuditStoreConfigWizard.this);
            }
        });

        wizardInput = new ExternalAuditStoreWizardConfig();
        this.sinkPolicyHeader = sinkPolicyHeader;
        this.lookupPolicyHeader = lookupPolicyHeader;

    }

    @Override
    protected void finish( final ActionEvent evt ) {
        getSelectedWizardPanel().storeSettings(wizardInput);
        final ExternalAuditStoreConfigWizard.ExternalAuditStoreWizardConfig config = (ExternalAuditStoreConfigWizard.ExternalAuditStoreWizardConfig)wizardInput;


        if(!config.custom && !safetyCheckNames(config.connection,config.auditRecordTableName,config.auditDetailTableName))
            return;

        // check if already exist
        PolicyHeader sinkHeader = getSinkPolicyHeader();
        PolicyHeader lookupHeader = getLookupPolicyHeader();

        if(sinkHeader !=null || lookupHeader !=null){
            int result = JOptionPane.showConfirmDialog(this,"This will overwrite the existing audit sink and lookup policies.",getTitle(), JOptionPane.OK_CANCEL_OPTION,JOptionPane.INFORMATION_MESSAGE,null);
            if(result != JOptionPane.YES_OPTION)
                return;
        }

        // create policies
        try {
            sinkPolicyHeader = createSinkPolicyHeader(config.connection, config.auditRecordTableName, config.auditDetailTableName, config.custom);
            lookupPolicyHeader = createLookupPolicyHeader(config.connection, config.connectionDriverClass, config.auditRecordTableName, config.auditDetailTableName, config.custom);

            super.finish(evt);
        } catch (FindException e) {
            logger.warning("Error creating external sink/lookup policies: "+e.getMessage());
        } catch (PolicyAssertionException e) {
            logger.warning("Error creating external sink/lookup policies: "+e.getMessage());
        } catch (SaveException e) {
            logger.warning("Error creating external sink/lookup policies: "+e.getMessage());
        }

    }

    private boolean safetyCheckNames(String connection, String auditRecordTableName, String auditDetailTableName) {
        return STRICT_CONNECTION_NAME_PATTERN.matcher(connection).matches();
    }

    private PolicyHeader createLookupPolicyHeader(String connection, String connectionDriverClass, String recordTable, String detailTable, boolean createEmptyPolicy) throws FindException, PolicyAssertionException, SaveException {
        PolicyHeader header = getLookupPolicyHeader() ;
        Policy policy;

        if(header == null){
            policy = makeDefaultAuditLookupPolicyEntity();
        }
        else {
            policy =  Registry.getDefault().getPolicyAdmin().findPolicyByGuid(header.getGuid());
        }
        // Create new lookup policy with default settings
        policy.setXml(getLookupPolicyXML(connection, connectionDriverClass,recordTable,detailTable, createEmptyPolicy));
        PolicyCheckpointState checkpoint = Registry.getDefault().getPolicyAdmin().savePolicy(policy, true);
        policy = Registry.getDefault().getPolicyAdmin().findPolicyByPrimaryKey(checkpoint.getPolicyOid());
        header = new PolicyHeader(policy);

        // Refresh service tree
        ServicesAndPoliciesTree tree = (ServicesAndPoliciesTree) TopComponents.getInstance().getComponent(ServicesAndPoliciesTree.NAME);
        tree.refresh();
        return header;
    }

    private static Policy makeDefaultAuditLookupPolicyEntity() {
        Policy policy = new Policy(PolicyType.INTERNAL, "[Internal Audit Lookup Policy]", null, false);
        policy.setInternalTag(INTERNAL_TAG_AUDIT_LOOKUP);
        return policy;
    }

    private static String getLookupPolicyXML(String connection, String connectionDriverClass, String recordTable, String detailTable, boolean createEmptyPolicy){
        return  createEmptyPolicy ? getDefaultLookupPolicyXml() : ExternalAuditsCommonUtils.makeDefaultAuditLookupPolicyXml(connection,recordTable,detailTable,getJdbcDbType(connectionDriverClass));
    }

    private static String getJdbcDbType(String connectionDriverClass){
        String type = null;
        if(connectionDriverClass.contains("sqlserver"))
            type = "sqlserver";
        else if(connectionDriverClass.contains("mysql"))
            type = "mysql";
        else if(connectionDriverClass.contains("db2"))
            type = "db2";
        else if(connectionDriverClass.contains("oracle"))
            type = "oracle";
        return type;
    }

    public PolicyHeader getLookupPolicyHeader() {
        return lookupPolicyHeader;
    }

    public PolicyHeader getSinkPolicyHeader() {
        return sinkPolicyHeader;
    }

    private PolicyHeader createSinkPolicyHeader(String connection, String recordTable, String detailTable, boolean createEmptyPolicy) throws FindException, PolicyAssertionException, SaveException {
        PolicyHeader header = getSinkPolicyHeader();
        Policy policy;
        if(header == null){
            // Create new sink policy with default settings
            policy = makeDefaultAuditSinkPolicyEntity();
        }
        else{
            policy = Registry.getDefault().getPolicyAdmin().findPolicyByGuid(header.getGuid());
        }

        policy.setXml(getSinkPolicyXML(connection, recordTable, detailTable, createEmptyPolicy));
        PolicyCheckpointState checkpoint = Registry.getDefault().getPolicyAdmin().savePolicy(policy, true);
        policy = Registry.getDefault().getPolicyAdmin().findPolicyByPrimaryKey(checkpoint.getPolicyOid());
        header = new PolicyHeader(policy);

        // Refresh service tree
        ServicesAndPoliciesTree tree = (ServicesAndPoliciesTree) TopComponents.getInstance().getComponent(ServicesAndPoliciesTree.NAME);
        tree.refresh();
        return header;
    }

    private static Policy makeDefaultAuditSinkPolicyEntity() {
        Policy policy = new Policy(PolicyType.INTERNAL, "[Internal Audit Sink Policy]", null, false);
        policy.setInternalTag(INTERNAL_TAG_AUDIT_SINK);
        return policy;
    }

    private static String getSinkPolicyXML(String connection, String recordTable, String detailTable, boolean createEmptyPolicy) {
        return createEmptyPolicy ? getSendToHttpSinkPolicyXml() : ExternalAuditsCommonUtils.makeDefaultAuditSinkPolicyXml(connection, recordTable, detailTable);
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

    private static String getDefaultLookupPolicyXml(){
        AllAssertion all = new AllAssertion();
        all.addChild(new AuditDetailAssertion("Internal Policy: [Internal Audit Lookup Policy]"));
        return WspWriter.getPolicyXml(all);
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
        public String connectionDriverClass;
        public String auditRecordTableName = null;
        public String auditDetailTableName = null;
    }
}
