/*
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.action;

import com.l7tech.console.panels.ServicePropertiesDialog;
import com.l7tech.console.panels.WorkSpacePanel;
import com.l7tech.console.poleditor.PolicyEditorPanel;
import com.l7tech.console.tree.*;
import com.l7tech.console.tree.servicesAndPolicies.FolderNode;
import com.l7tech.console.tree.servicesAndPolicies.RootNode;
import com.l7tech.console.util.ClusterPropertyCrud;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.AttemptedUpdate;
import com.l7tech.gateway.common.security.rbac.AttemptedUpdateAll;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyCheckpointState;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.assertion.AuditAssertion;
import com.l7tech.policy.assertion.AuditDetailAssertion;
import com.l7tech.policy.assertion.CommentAssertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.util.ExceptionUtils;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.logging.Level;

/**
 * Action to edit the published service properties
 */
public class EditServiceProperties extends EntityWithPolicyNodeAction<ServiceNode> {
    public static final String INTERNAL_TAG_TRACE_POLICY = "debug-trace";
    public static final String TRACE_POLICY_GUID_CLUSTER_PROP = "trace.policy.guid";

    public EditServiceProperties(ServiceNode node) {
        super(node);
    }

    protected OperationType getOperation() {
        return OperationType.READ;
    }

    public String getName() {
        return "Service Properties";
    }

    public String getDescription() {
        return "View/Edit the properties of the published service";
    }

    protected String iconResource() {
        return "com/l7tech/console/resources/Edit16.gif";
    }

    protected void performAction() {
        final ServiceNode serviceNode = ((ServiceNode)node);
        boolean hasUpdatePermission;
        boolean hasTracePermission;
        final PublishedService svc;
        try {
            svc = serviceNode.getEntity();
            hasUpdatePermission = Registry.getDefault().getSecurityProvider().hasPermission(new AttemptedUpdate(EntityType.SERVICE, svc));
            hasTracePermission = Registry.getDefault().getSecurityProvider().hasPermission(new AttemptedUpdateAll(EntityType.SERVICE));
        } catch (FindException e) {
            logger.log(Level.WARNING, "Cannot get service", e);
            throw new RuntimeException(e);
        }

        final Frame mw = TopComponents.getInstance().getTopParent();
        final ServicePropertiesDialog dlg = new ServicePropertiesDialog(mw, svc, hasUpdatePermission, hasTracePermission);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        final boolean wasTracingEnabled = svc.isTracingEnabled();
        DialogDisplayer.display(dlg, new Runnable() {
            public void run() {
                if (dlg.wasOKed()) {
                    if (svc.isTracingEnabled())
                        checkTracePolicyStatus(wasTracingEnabled);

                    serviceNode.clearCachedEntities();
                    serviceNode.reloadChildren();
                    serviceNode.clearIcons();

                    final ServicesAndPoliciesTree tree = (ServicesAndPoliciesTree) TopComponents.getInstance().getComponent(ServicesAndPoliciesTree.NAME);
                    if (tree != null) {
                        DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                        model.nodeChanged(node);
                        model.reload(node); // WSDL may have changed

                        //if this is an original entity, update any aliases it may have, in case it's name, uri or
                        //something else show to the user in the tree changes
                        if(!(serviceNode instanceof ServiceNodeAlias)){
                            ServiceHeader sH = (ServiceHeader) serviceNode.getUserObject();
                            tree.updateAllAliases(sH.getGoid());

                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    refreshTree(serviceNode, tree);
                                }
                            });
                        }
                    }

                    // update name on top of editor if that service is being edited
                    final WorkSpacePanel cws = TopComponents.getInstance().getCurrentWorkspace();
                    JComponent jc = cws.getComponent();
                    if (jc == null || !(jc instanceof PolicyEditorPanel)) return;

                    PolicyEditorPanel pe = (PolicyEditorPanel)jc;
                    try {
                        final EntityWithPolicyNode pn = pe.getPolicyNode();

                        if (pn instanceof ServiceNode) {
                            PublishedService editedSvc = ((ServiceNode) pn).getEntity();
                            // if currently edited service was deleted
                            if (Goid.equals(serviceNode.getEntityGoid(), editedSvc.getGoid())) {
                                // update name on top of editor
                                pe.changeSubjectName(serviceNode.getName());
                                pe.updateHeadings();
                            }
                        }
                    } catch (FindException e) {
                        logger.log(Level.WARNING, "problem modifying policy editor title");
                    }
                }
                serviceNode.clearCachedEntities();
            }
        });
    }

    /**
     * If a service was saved with tracing enabled, ensure a trace policy exists, and offer to allow the
     * admin to immediately open it for editing.
     * @param wasPreviouslyEnabled true if tracing was already enabled on this service before its properties dialog was opened
     */
    private void checkTracePolicyStatus(boolean wasPreviouslyEnabled) {
        try {
            // Ensure a trace policy exists
            PolicyHeader header = getOrCreateTracePolicyHeader();
            String guid = header.getGuid();
            ClusterPropertyCrud.putClusterProperty(TRACE_POLICY_GUID_CLUSTER_PROP, guid);

            // Don't bother offering to edit the trace policy if tracing was already on for this service
            if (wasPreviouslyEnabled)
                return;

            final Action editAction = prepareEditAction();

            DialogDisplayer.showConfirmDialog(TopComponents.getInstance().getTopParent(),
                    "Do you want to edit the debug trace policy now?",
                    "Edit Trace Policy",
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, new DialogDisplayer.OptionListener() {
                @Override
                public void reportResult(int option) {
                    if (JOptionPane.YES_OPTION == option) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                editAction.actionPerformed(null);
                            }
                        });
                    }
                }
            });

        } catch (ObjectModelException e) {
            err("configure audit sink", e);
        } catch (PolicyAssertionException e) {
            err("configure audit sink", e);
        }
    }


    private Action prepareEditAction() throws FindException, PolicyAssertionException, SaveException {
        PolicyHeader header = getOrCreateTracePolicyHeader();
        PolicyEntityNode node = new PolicyEntityNode(header);
        return new EditPolicyAction(node, true);
    }

    private PolicyHeader getTracePolicyHeader() throws FindException {
        Collection<PolicyHeader> allInternals = Registry.getDefault().getPolicyAdmin().findPolicyHeadersByType(PolicyType.INTERNAL);
        for (PolicyHeader internal : allInternals) {
            if (INTERNAL_TAG_TRACE_POLICY.equals(internal.getDescription())) {
                return internal;
            }
        }
        return null;
    }

    private PolicyHeader getOrCreateTracePolicyHeader() throws FindException, PolicyAssertionException, SaveException {
        PolicyHeader header = getTracePolicyHeader();
        if (header == null) {
            // Create new trace policy with default settings
            Policy policy = makeDefaultTracePolicyEntity();
            PolicyCheckpointState checkpoint = Registry.getDefault().getPolicyAdmin().savePolicy(policy, true);
            policy = Registry.getDefault().getPolicyAdmin().findPolicyByPrimaryKey(checkpoint.getPolicyGoid());
            header = new PolicyHeader(policy);

            // Refresh service tree
            ServicesAndPoliciesTree tree = (ServicesAndPoliciesTree) TopComponents.getInstance().getComponent(ServicesAndPoliciesTree.NAME);
            tree.refresh();
        }
        return header;
    }

    private static Policy makeDefaultTracePolicyEntity() {
        String theXml = makeDefaultTracePolicyXml();
        Policy policy = new Policy(PolicyType.INTERNAL, "[Internal Debug Trace Policy]", theXml, false);
        policy.setInternalTag(INTERNAL_TAG_TRACE_POLICY);
        return policy;
    }

    private static String makeDefaultTracePolicyXml() {
        AllAssertion all = makeDefaultTracePolicyAssertions();
        return WspWriter.getPolicyXml(all);
    }

    private static AllAssertion makeDefaultTracePolicyAssertions() {
        AllAssertion all = new AllAssertion();
        all.addChild(new CommentAssertion("A simple debug trace policy."));
        all.addChild(new CommentAssertion("This policy will be invoked after every assertion for any service with debug tracing enabled."));
        all.addChild(new CommentAssertion("For example, we can trigger auditing of trace information about the assertion that just finished."));
        all.addChild(new AuditAssertion(Level.WARNING, false, false, false, false));
        all.addChild(new AuditDetailAssertion("TRACE: service.name=${trace.service.name} policy.name=${trace.policy.name} policy.guid=${trace.policy.guid} assertion.number=${trace.assertion.numberstr} assertion.shortname=${trace.assertion.shortname} status=${trace.status}"));
        return all;
    }

    private void err(String what, Throwable t) {
        final String msg = "Unable to " + what + ": " + ExceptionUtils.getMessage(t);
        logger.log(Level.WARNING, msg, t);
        DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(), "Unable to " + what, msg, null);
    }

    private void sortChildren(AbstractTreeNode node){
        if(!(node instanceof FolderNode)){
            return;
        }

        java.util.List<AbstractTreeNode> childNodes = new ArrayList<AbstractTreeNode>();
        for(int i = 0; i < node.getChildCount(); i++){
            AbstractTreeNode childNode = (AbstractTreeNode)node.getChildAt(i);
            childNodes.add(childNode);
            if(childNode instanceof FolderNode){
                sortChildren(childNode);
            }
        }

        //Detach all children
        node.removeAllChildren();
        for(AbstractTreeNode atn: childNodes){
            node.insert(atn, node.getInsertPosition(atn, RootNode.getComparator()));
        }
    }

    private void refreshTree(EntityWithPolicyNode serviceNode, final JTree tree) {
        try {
            serviceNode.updateUserObject();

            TreePath rootPath = tree.getPathForRow(0);
            final TreePath selectedNodeTreePath = tree.getSelectionPath();
            final Enumeration pathEnum = tree.getExpandedDescendants(rootPath);

            DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
            RootNode rootNode = (RootNode) model.getRoot();
            sortChildren(rootNode);
            model.nodeStructureChanged(rootNode);

            while (pathEnum.hasMoreElements()) {
                Object pathObj = pathEnum.nextElement();
                TreePath tp = (TreePath) pathObj;
                tree.expandPath(tp);
            }
            tree.setSelectionPath(selectedNodeTreePath);

        } catch (FindException fe) {
            logger.info("Cannot the new service to update service node.");
        }
    }
}
