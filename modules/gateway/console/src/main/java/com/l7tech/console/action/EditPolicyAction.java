package com.l7tech.console.action;

import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.panels.WorkSpacePanel;
import com.l7tech.console.panels.HomePagePanel;
import com.l7tech.console.poleditor.PolicyEditorPanel;
import com.l7tech.console.tree.EntityWithPolicyNode;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTree;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.AttemptedUpdate;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyVersion;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.FalseAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.util.ExceptionUtils;

import javax.swing.*;
import javax.swing.tree.TreeModel;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.text.DateFormat;
import java.util.*;
import java.util.logging.Level;

/**
 * The <code>EditPolicyAction</code> invokes the policy editor on either a Service or a Policy.
 */
public class EditPolicyAction extends NodeAction {
    private final boolean validate;
    private final boolean service;
    private final PolicyVersion policyVersion;

    /**
     * default constructor. invoke the policy validate if
     * specified.
     *
     * @param node the service node
     * @param b    true validate the policy, false
     */
    public EditPolicyAction(EntityWithPolicyNode node, boolean b) {
        super(node);
        validate = b;
        service = node instanceof ServiceNode;
        this.policyVersion = null;
    }

    public EditPolicyAction(EntityWithPolicyNode node, boolean validate, PolicyVersion version) {
        super(node);
        this.validate = validate;
        service = node instanceof ServiceNode;
        this.policyVersion = version;
    }

    /**
     * default constructor. invoke the node validate
     *
     * @param node the service node
     */
    public EditPolicyAction(EntityWithPolicyNode node) {
        this(node, true);
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Active Policy Assertions";
    }

    /**
     * @return the action description
     */
    public String getDescription() {
        return service ? "Edit Web service policy assertions" : " Edit policy assertions";
    }

    /**
     * specify the resource name for this action
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/policy16.gif";
    }

    /**
     * Actually perform the action.
     * This is the method which should be called programmatically.
     * <p/>
     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    protected void performAction() {
        final EntityWithPolicyNode policyNode = (EntityWithPolicyNode)node;
        try {
            TopComponents windowManager = TopComponents.getInstance();
            WorkSpacePanel wpanel = windowManager.getCurrentWorkspace();

            // clear work space here will prompt user to save or cancel the changes in the current policy first
            // it makes sure the user will see the updated policy if the policy is saved
            wpanel.clearWorkspace();

            if (policyVersion == null)
                policyNode.clearCachedEntities();
            TopComponents topComponents = TopComponents.getInstance();
            topComponents.unregisterComponent(PolicyTree.NAME);
            PolicyTree policyTree = (PolicyTree)topComponents.getPolicyTree();

            boolean startsDirty = false;
            try {
                final Policy policy = policyNode.getPolicy();
                if (policy != null && policy.isDisabled()) {
                    startsDirty = true;
                    if (findStartingAssertion(policy) == null) {
                        new HomeAction().actionPerformed(null);
                        return;
                    }
                }
                if (policyVersion != null) {
                    policy.setVersionOrdinal(policyVersion.getOrdinal());
                    policy.setVersionActive(policyVersion.isActive());
                }
            } catch (IOException e) {
                log.log(Level.SEVERE, "cannot get service or policy", e);
                throw new RuntimeException(e);
            } catch (FindException e) {
                wpanel.setComponent(new HomePagePanel());
                log.log(Level.INFO, "cannot get service or policy", ExceptionUtils.getMessage(e));
                JOptionPane.showMessageDialog(null, "Service or policy does not exist.", "Cannot find service or policy", JOptionPane.ERROR_MESSAGE);
                return;
            }

            PolicyEditorPanel.PolicyEditorSubject subject = new PolicyEditorPanel.PolicyEditorSubject() {
                public EntityWithPolicyNode getPolicyNode() {return policyNode;}

                private Policy getPolicy() {
                    try {
                        return policyNode.getPolicy();
                    } catch (Exception e) {
                        throw new RuntimeException("Unable to load policy", e);
                    }
                }

                public Assertion getRootAssertion() {
                    try {
                        return getPolicy().getAssertion();
                    } catch (Exception e) {
                        throw new RuntimeException("Unable to load policy", e); // Doctor, it hurts when I do this!
                    }
                }

                public String getName() {
                    return policyNode.getName();
                }

                public long getVersionNumber() {
                    return getPolicy().getVersionOrdinal();
                }

                public boolean isActive() {
                    return getPolicy().isVersionActive();
                }

                public void addPropertyChangeListener(PropertyChangeListener servicePropertyChangeListener) {
                    policyNode.addPropertyChangeListener(servicePropertyChangeListener);
                }
                public void removePropertyChangeListener(PropertyChangeListener servicePropertyChangeListener) {
                    policyNode.removePropertyChangeListener(servicePropertyChangeListener);
                }
                public boolean hasWriteAccess() {
                    try {
                        EntityWithPolicyNode pn = getPolicyNode();
                        EntityType type;
                        Entity entity = pn.getEntity();
                        if (pn instanceof ServiceNode) {
                            type = EntityType.SERVICE;
                        } else {
                            type = EntityType.POLICY;
                        }
                        return Registry.getDefault().getSecurityProvider().hasPermission(new AttemptedUpdate(type, entity));
                    } catch (Exception e) {
                        log.log(Level.WARNING, "Error performing permisison check", e);
                        throw new RuntimeException(e);
                    }
                }
            };
            final PolicyEditorPanel pep = new PolicyEditorPanel(subject, policyTree, validate, !TopComponents.getInstance().isApplet());
            wpanel.setComponent(pep);
            wpanel.addWorkspaceContainerListener(pep);
            TopComponents.getInstance().firePolicyEdit(pep);
            if (startsDirty) {
                TreeModel ptm = TopComponents.getInstance().getPolicyTree().getModel();
                ((PolicyTreeModel)ptm).assertionTreeNodeChanged((AssertionTreeNode)ptm.getRoot());                
            }
        } catch (ActionVetoException e) {
            // action vetoed
            log.log(Level.WARNING, "vetoed!", e);
        } catch (Exception e) {
            ErrorManager.getDefault().
              notify(Level.SEVERE, e, "Unable to retrieve service properties");
        }
    }

    interface AssertionHaver {
        Assertion getAssertion() throws IOException, FindException;
    }

    /**
     * Prompts the user for a revision to start an edit from in the case of editing a disabled policy or revision.
     *
     * @param policy the disabled policy to ask about
     * @return the policy assertion tree on which to base this edit, or null to cancel the edit
     * @throws java.io.IOException if there is a problem parsing policy XML
     * @throws com.l7tech.objectmodel.FindException if there is a problem finding the selected revision's policy XML
     */
    private static Assertion findStartingAssertion(Policy policy) throws IOException, FindException {
        final List<PolicyVersion> versions;
        try {
            versions = Registry.getDefault().getPolicyAdmin().findPolicyVersionHeadersByPolicy(policy.getOid());
        } catch (FindException e) {
            String msg = "Unable to retrive versions for disabled policy oid " + policy.getOid() + ": " + ExceptionUtils.getMessage(e);
            logger.log(Level.WARNING, msg, e);
            JOptionPane.showMessageDialog(TopComponents.getInstance().getTopParent(),
                                          msg, "Unable to Retrieve Revisions", JOptionPane.ERROR_MESSAGE);
            return policy.getAssertion();
        }

        // Sort more recent revisions to the top
        Collections.sort(versions, new Comparator<PolicyVersion>() {
            public int compare(PolicyVersion o1, PolicyVersion o2) {
                return Long.valueOf(o2.getOrdinal()).compareTo(o1.getOrdinal());
            }
        });

        List<AssertionHaver> options = new ArrayList<AssertionHaver>();

        AssertionHaver startFromEmpty = new AssertionHaver() {
            public Assertion getAssertion() throws IOException, FindException {
                return new AllAssertion(new ArrayList<Assertion>(Arrays.asList(new FalseAssertion())));
            }

            public String toString() {
                return "Start from an empty policy";
            }
        };
        options.add(startFromEmpty);

        final DateFormat dateFormat = DateFormat.getInstance();
        for (final PolicyVersion policyVersion : versions) {
            long ordinal = policyVersion.getOrdinal();
            String date = dateFormat.format(policyVersion.getTime());
            String name = policyVersion.getName();
            name = name == null ? "" : " (" + name + ')';
            final String displayString = ordinal + " " + date + name;

            options.add(new AssertionHaver() {
                public Assertion getAssertion() throws IOException, FindException {
                    PolicyVersion fullVersion = Registry.getDefault().getPolicyAdmin().
                            findPolicyVersionByPrimaryKey(policyVersion.getPolicyOid(), policyVersion.getOid());
                    return WspReader.getDefault().parsePermissively(fullVersion.getXml());
                }

                public String toString() {
                    return displayString;
                }
            });
        }

        Object result = JOptionPane.showInputDialog(TopComponents.getInstance().getTopParent(),
                                                    "This policy has been disabled by having its active revision revoked.\n" +
                                                    "Please choose a revision on which to base your edit.",
                                                    "Chose Starting Revision",
                                                    JOptionPane.QUESTION_MESSAGE,
                                                    null,
                                                    options.toArray(new AssertionHaver[options.size()]),
                                                    startFromEmpty);

        if (result == null)
            return null;

        return ((AssertionHaver)result).getAssertion();
    }
}
