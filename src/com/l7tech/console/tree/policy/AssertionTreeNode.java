/*
 * Copyright (C) 2003-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree.policy;

import com.l7tech.common.security.rbac.AttemptedUpdate;
import com.l7tech.common.security.rbac.EntityType;
import com.l7tech.common.policy.Policy;
import com.l7tech.common.policy.PolicyType;
import com.l7tech.console.action.*;
import com.l7tech.console.policy.exporter.PolicyImporter;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.PolicyTemplateNode;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.tree.PolicyEntityNode;
import com.l7tech.console.util.Cookie;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.SetsVariables;
import com.l7tech.policy.assertion.Include;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.wsp.InvalidPolicyStreamException;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.service.PublishedService;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class <code>AssertionTreeNode</code> is the base superclass for the
 * asserttion tree policy nodes.
 */
public abstract class AssertionTreeNode<AT extends Assertion> extends AbstractTreeNode {
    private static final Logger logger = Logger.getLogger(AssertionTreeNode.class.getName());
    private static final OneOrMoreAssertion ONEORMORE_PROTOTYPE = new OneOrMoreAssertion();

    private List<PolicyValidatorResult.Message> validatorMessages = new ArrayList<PolicyValidatorResult.Message>();
    private List<PolicyValidatorResult.Message> viewValidatorMessages = null;

    protected final AT assertion;

    /**
     * package private constructor accepting the asseriton
     * this node represents.
     *
     * @param assertion that this node represents
     */
    AssertionTreeNode(AT assertion) {
        super(assertion);
        if (assertion == null) throw new IllegalArgumentException("Assertion is required");
        this.assertion = assertion;
        this.setAllowsChildren(false);
    }

    /**
     * @return the assertion this node represents
     */
    @Override
    public final AT asAssertion() {
        return assertion;
    }

    /**
     * Get the assertion index path for this treen node.
     *
     * <p>This is the index of the child assertion at each level
     * of the tree.</p>
     *
     * @return The list of integers (empty for root)
     */
    public final List<Integer> asAssertionIndexPath() {
        List<Integer> ordinals = new ArrayList<Integer>();

        TreeNode node = this;
        while ( node != null ) {
            TreeNode parent = node.getParent();
            if ( parent != null )
                ordinals.add( parent.getIndex( node ));
            node = parent;
        }

        Collections.reverse( ordinals );

        return ordinals;
    }

    /**
     * Get a descendent tree node by index path.
     *
     * @param indexPath the list of child indexes
     * @return the tree node or null if not found
     */
    public final AssertionTreeNode getAssertionByIndexPath(List<Integer> indexPath) {
        AssertionTreeNode assertion = null;

        if ( indexPath.isEmpty() ) {
            assertion = this;
        } else {
            int index = indexPath.get( 0 );
            if ( index >=0 && index < getChildCount() ) {
                TreeNode child = getChildAt( index );
                if ( child instanceof AssertionTreeNode ) {
                    AssertionTreeNode atnChild = (AssertionTreeNode) child;
                    assertion = atnChild.getAssertionByIndexPath( indexPath.subList( 1, indexPath.size() ));
                }
            }
        }

        return assertion;
    }

    /**
     * @return the node name that is displayed
     */
    @Override
    abstract public String getName();

    /**
     * Set the validator messages for this node.
     *
     * @param messages the messages
     */
    public void setValidatorMessages(Collection<PolicyValidatorResult.Message> messages) {
        this.validatorMessages = new ArrayList<PolicyValidatorResult.Message>();
        if (messages != null) {
            validatorMessages.addAll(messages);
        }
        viewValidatorMessages = null;
    }

    /**
     * Get the full collection of validator messages.
     *
     * @return the collection of validator messages
     */
    public Collection getAllValidatorMessages() {
        if (validatorMessages != null) {
            return validatorMessages;
        }
        return Collections.EMPTY_LIST;
    }


    /**
     * Get the validator messages for this node. Returns the messages
     * depending on the view. For the identity view only the messages
     * that rlate to the set of paths that are associated with the view
     * will be returned.
     *
     * @return the list of validator messages
     */
    public List getValidatorMessages() {
        if (viewValidatorMessages != null) {
            return viewValidatorMessages;
        }

        if (!PolicyTree.isIdentityView(this)) {
            viewValidatorMessages = this.validatorMessages;
        } else {
            //select only the path messages
            List<PolicyValidatorResult.Message> pathMessages = new ArrayList<PolicyValidatorResult.Message>();
            TreeNode[] path = getPath();
            if (path.length >= 2) {
                IdentityPolicyTreeNode in = (IdentityPolicyTreeNode)path[1];
                for (PolicyValidatorResult.Message message : validatorMessages) {
                    if (in.contains(message.getAssertionPathOrder())) {
                        pathMessages.add(message);
                    }
                }
            }
            viewValidatorMessages = pathMessages;
        }
        return viewValidatorMessages;
    }

    /**
     * Tooltip override, if there is an validator message, shows as tooltip.
     *
     * @return the tooltip text or null
     */
    @Override
    public String getTooltipText() {
        List messages = getValidatorMessages();
        StringBuffer sb = new StringBuffer();
        if (messages.isEmpty()) {
            final String st = super.getTooltipText();
            if (st != null) sb.append(st);
            Assertion ass = this.asAssertion();
            if (ass instanceof SetsVariables) {
                SetsVariables sv = (SetsVariables)ass;
                final VariableMetadata[] vars = sv.getVariablesSet();
                if (vars.length > 0) {
                    if (st != null)
                        sb.append(", setting ");
                    else
                        sb.append("Sets ");

                    for (int i = 0; i < vars.length; i++) {
                        String name = vars[i].getName();
                        sb.append(Syntax.SYNTAX_PREFIX)
                            .append("<b>")
                            .append(name)
                            .append("</b>")
                            .append(Syntax.SYNTAX_SUFFIX);
                        if (i < vars.length-1) sb.append(", ");
                    }
                }
            }
            if (sb.length() > 0) {
                sb.insert(0, "<html>");
                return sb.toString();
            } else {
                return null;
            }
        } else {
            sb.append("<html><strong>The policy may be invalid due to {0}<br>");
            Iterator it = messages.iterator();
            boolean first = true;
            boolean hasWarnings = false;
            boolean hasErrors = false;
            for (; it.hasNext();) {
                if (!first) {
                    sb.append("<br>");
                }
                first = false;
                PolicyValidatorResult.Message pm = (PolicyValidatorResult.Message)it.next();
                if (pm instanceof PolicyValidatorResult.Error) {
                    hasErrors = true;
                } else if ((pm instanceof PolicyValidatorResult.Warning)) {
                    hasWarnings = true;
                }
                String blahness = pm.getMessage();
                blahness = blahness.replaceAll("\\{", "'{'");
                blahness = blahness.replaceAll("\\}", "'}'");
                sb.append("<i>").append(blahness).append("</i>");
            }
            sb.append("</strong></html>");
            String format = sb.toString();
            String msg = "warnings and errors:";
            if (hasWarnings && !hasErrors) {
                msg = "warnings:";
            } else if (!hasWarnings && hasErrors) {
                msg = "errors:";
            }
            return MessageFormat.format(format, msg);
        }
    }

    /**
     * Get the set of actions associated with this node.
     * This may be used e.g. in constructing a context menu.
     *
     * @return actions appropriate to the node
     */
    @Override
    public Action[] getActions() {
        java.util.List<Action> list = new ArrayList<Action>();
        list.addAll(Arrays.asList(super.getActions()));
        CompositeAssertionTreeNode ca;
        if (this instanceof CompositeAssertionTreeNode) {
            ca = (CompositeAssertionTreeNode)this;
        } else {
            TreeNode parent = getParent();
            if (parent instanceof CompositeAssertionTreeNode) {
                ca = (CompositeAssertionTreeNode) parent;
            } else {
                if (isDescendantOfInclude(true)) return list.toArray(new Action[list.size()]);
                throw new IllegalStateException("Assertion parent is neither an Include nor a Composite");
            }
        }

        int position = (this instanceof CompositeAssertionTreeNode) ? 0 : this.getParent().getIndex(this) + 1;
        list.add(new AddAllAssertionAction(ca, position));

        if (Registry.getDefault().getLicenseManager().isAssertionEnabled(ONEORMORE_PROTOTYPE)) {
            list.add(new AddOneOrMoreAssertionAction(ca, position));
        }

        try {
            PublishedService svc = getService();
            if (Registry.getDefault().getSecurityProvider().hasPermission(new AttemptedUpdate(EntityType.SERVICE, svc))) {
                Action da = new DeleteAssertionAction(this);
                da.setEnabled(canDelete());
                list.add(da);

                /*
                commented out as it is currently NOT supported
                Action ea = new ExplainAssertionAction();
                ea.setEnabled(canDelete());
                list.add(ea);
                */

                Action mu = new AssertionMoveUpAction(this);
                mu.setEnabled(canMoveUp());
                list.add(mu);

                Action md = new AssertionMoveDownAction(this);
                md.setEnabled(canMoveDown());
                list.add(md);
            }
        } catch (Exception e) {
            throw new RuntimeException("Couldn't get current service", e);
        }

        /*
            Action sp = new SavePolicyAction(this);
            list.add(sp);

            Action vp = new ValidatePolicyAction((AssertionTreeNode)getRoot());
            list.add(vp);
        */

        return list.toArray(new Action[list.size()]);
    }

    @Override
    public boolean canDelete() {
        return !isDescendantOfInclude(false);
    }
    
    /**
     * Can the node move up in the assertion tree
     *
     * @return true if the node can move up, false otherwise
     */
    public boolean canMoveUp() {
        return !isDescendantOfInclude(false) && getParent() != null && getPreviousSibling() != null;
    }

    /**
     * Can the node move down in the assertion tree
     *
     * @return true if the node can move up, false otherwise
     */
    public boolean canMoveDown() {
        return !isDescendantOfInclude(false) && getNextSibling() != null;
    }

    /**
     * Test if the node may be dragged.
     *
     * @return true if the node can be dragged, false otherwise
     */
    public boolean canDrag() {
        return !isDescendantOfInclude(false);
    }

    /**
     * Swap the position of this node with the target
     * node.
     */
    public void swap(AssertionTreeNode target) {
        final JTree tree = TopComponents.getInstance().getPolicyTree();
        final PolicyTreeModel model = (PolicyTreeModel)tree.getModel();
        final DefaultMutableTreeNode parent = (DefaultMutableTreeNode)this.getParent();

        int indexThis = parent.getIndex(this);
        model.removeNodeFromParent(this);
        int indexThat = parent.getIndex(target);
        model.removeNodeFromParent(target);

        model.moveNodeInto(this, parent, indexThat);
        model.moveNodeInto(target, parent, indexThis);

        Runnable runnable = new Runnable() {
            public void run() {
                TreeNode[] path = AssertionTreeNode.this.getPath();
                if (path != null) {
                    tree.setSelectionPath(new TreePath(path));
                }
            }
        };
        SwingUtilities.invokeLater(runnable);
    }

    /**
     * Receive the abstract tree node
     *
     * @param node the node to receive
     * @return true if the node has been received by this assertion node
     *         false otherwise
     */
    public boolean receive(AbstractTreeNode node) {
        if (node instanceof PolicyTemplateNode) {
            assignPolicyTemplate((PolicyTemplateNode)node);
            return true;
        }
        return false;
    }

    /**
     * assign the policy template.
     * todo: find a better place for this
     */
    private void assignPolicyTemplate(PolicyTemplateNode templateNode) {
        PolicyEntityNode policyNode = getPolicyNodeCookie();
        if (policyNode == null)
            throw new IllegalArgumentException("No edited policy specified");
        try {
            Assertion newRoot = PolicyImporter.importPolicy(templateNode.getFile());
            // for some reason, the PublishedService class does not allow to set a policy
            // directly, it must be set through the XML
            if (newRoot != null) {
                String oldPolicyXml = policyNode.getPolicy().getXml();
                policyNode.getPolicy().setXml(WspWriter.getPolicyXml(newRoot));
                policyNode.firePropertyChange(this, "policy", oldPolicyXml, policyNode.getPolicy().getXml());
            }
        } catch (FindException e) {
            logger.log(Level.WARNING, "Could not import the policy", e);
        } catch (InvalidPolicyStreamException e) {
            logger.log(Level.WARNING, "Could not import the policy", e);
        }
    }

    /**
     * @return the published service cookie or null if not found
     */
    protected ServiceNode getServiceNodeCookie() {
        return getServiceNode(this);
    }

    /**
     * @return the published service cookie or null if not found
     */
    protected PolicyEntityNode getPolicyNodeCookie() {
        return getPolicyNode(this);
    }

    public PublishedService getService() throws FindException {
        ServiceNode sn = getServiceNodeCookie();
        if (sn == null) return null;
        return sn.getPublishedService();
    }

    /**
     * Get the service that this assertion tree node belongs to
     *
     * @param node the assertion tree node
     * @return the published service or null if not found
     */
    public static ServiceNode getServiceNode(AssertionTreeNode node) {
        for (Iterator i = ((AbstractTreeNode)node.getRoot()).cookies(); i.hasNext();) {
            Object value = ((Cookie)i.next()).getValue();
            if (value instanceof ServiceNode) return (ServiceNode)value;
        }
        return null;
    }

    /**
     * Get the service that this assertion tree node belongs to
     *
     * @param node the assertion tree node
     * @return the published service or null if not found
     */
    public static PolicyEntityNode getPolicyNode(AssertionTreeNode node) {
        for (Iterator i = ((AbstractTreeNode)node.getRoot()).cookies(); i.hasNext();) {
            Object value = ((Cookie)i.next()).getValue();
            if (value instanceof PolicyEntityNode) return (PolicyEntityNode)value;
        }
        return null;
    }

    /**
     * Does the assertion node accepts the abstract tree node
     *
     * @param node the node to accept
     * @return true if the node can be accepted, false otherwise
     */
    public boolean accept(AbstractTreeNode node) {
        return !checkForInclude(node);
    }

    protected boolean isDescendantOfInclude(boolean includeSelf) {
        for (TreeNode ancestor : getPath()) {
            if (ancestor instanceof IncludeAssertionPolicyNode) {
                return ancestor != this || includeSelf;
            }
        }
        return false;
    }

    protected boolean checkForInclude(AbstractTreeNode draggingNode) {
        Include include = null;
        if (draggingNode instanceof IncludeAssertionPaletteNode) {
            IncludeAssertionPaletteNode iapn = (IncludeAssertionPaletteNode) draggingNode;
            include = (Include) iapn.asAssertion();
        } else if (draggingNode instanceof IncludeAssertionPolicyNode) {
            IncludeAssertionPolicyNode iapn = (IncludeAssertionPolicyNode) draggingNode;
            include = iapn.asAssertion();
        }
        if (include != null) {
            try {
                Policy thisPolicy = getPolicyNodeCookie().getPolicy();
                if (thisPolicy.getType() == PolicyType.INCLUDE_FRAGMENT && thisPolicy.getOid() == include.getPolicyOid()) {
                    logger.warning("Refusing to create circular reference to policy #" + thisPolicy.getOid() + ", not accepting drag of " + draggingNode.getClass().getSimpleName() + " into " + this.getClass().getSimpleName());
                    return true;
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Couldn't get current policy");
            }
        }

        return isDescendantOfInclude(true);
    }
}

