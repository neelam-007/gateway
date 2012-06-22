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

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
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

    public static ExternalAuditStoreConfigWizard getInstance(Window parent) {
        ExternalAuditStoreConfigJdbc panel1 = new ExternalAuditStoreConfigJdbc(new ExternalAuditStoreConfigSchema(null));

        ExternalAuditStoreConfigWizard output = new ExternalAuditStoreConfigWizard(parent, panel1);

        return output;
    }

    public ExternalAuditStoreConfigWizard(Window parent, WizardStepPanel panel) {
        super(parent, panel);
        setTitle("Configure External Audit Store Wizard");

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
        if(!safetyCheckNames(config.connection,config.auditRecordTableName,config.auditDetailTableName))
            return;

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

    private boolean safetyCheckNames(String connection, String auditRecordTableName, String auditDetailTableName) {
        return STRICT_CONNECTION_NAME_PATTERN.matcher(connection).matches();
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
        String theXml = createEmptyPolicy ? getDefaultPolicyXml(policy) : ExternalAuditsCommonUtils.makeDefaultAuditLookupPolicyXml(connection,recordTable,detailTable);

        policy.setXml(theXml);
        return policy;
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

        String theXml = createEmptyPolicy ? getSendToHttpSinkPolicyXml() : ExternalAuditsCommonUtils.makeDefaultAuditSinkPolicyXml(connection,recordTable,detailTable);

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
