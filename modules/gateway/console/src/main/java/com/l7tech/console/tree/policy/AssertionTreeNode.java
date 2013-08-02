/*
 * Copyright (C) 2003-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree.policy;

import com.l7tech.console.action.*;
import com.l7tech.console.policy.exporter.PolicyExportUtils;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.EntityWithPolicyNode;
import com.l7tech.console.tree.PolicyTemplateNode;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.util.Cookie;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.AttemptedUpdate;
import com.l7tech.gateway.common.security.rbac.PermissionDeniedException;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.PolicyUtil;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.variable.PolicyVariableUtils;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.TextUtils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.lang.ref.SoftReference;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class <code>AssertionTreeNode</code> is the base superclass for the
 * assertion tree policy nodes.
 */
public abstract class AssertionTreeNode<AT extends Assertion> extends AbstractTreeNode implements Comparable<AssertionTreeNode>{
    private static final Logger logger = Logger.getLogger(AssertionTreeNode.class.getName());
    private static final OneOrMoreAssertion ONEORMORE_PROTOTYPE = new OneOrMoreAssertion();

    private List<PolicyValidatorResult.Message> validatorMessages = new ArrayList<PolicyValidatorResult.Message>();
    private List<PolicyValidatorResult.Message> viewValidatorMessages = null;

    protected AT assertion;
    private SoftReference<String> assertionPropsAsString;
    private Boolean ancestorDisabled;  // A flag to indicate if the current node's ancestor is disabled.  Status: null, true, false, where null means the flag hasn't initialized.

    protected static boolean decorateComment = true; // A flag to show whether it needs to decorate the comment such as using html tags.  Default value is true.
    /**
     * package private constructor accepting the assertion
     * this node represents.
     *
     * @param assertion that this node represents
     */
    AssertionTreeNode(AT assertion) {
        super(assertion);
        if (assertion == null) throw new IllegalArgumentException("Assertion is required");
        this.assertion = loadDesignTimeEntities(assertion);
        if (assertion != this.assertion)
            setUserObject(this.assertion);
        this.setAllowsChildren(false);
    }

    /**
     * Return a version (ideally the same instance) of the specified assertion with any design time entity dependencies
     * populated.
     *
     * @param assertion the assertion to preprocess.  Required.
     * @return the assertion with any design time entities filled in.
     */
    protected AT loadDesignTimeEntities(AT assertion) {
        if (Registry.getDefault().isAdminContextPresent()) {
            if (assertion instanceof UsesEntitiesAtDesignTime) {
                try {
                    PolicyUtil.provideNeededEntities((UsesEntitiesAtDesignTime) assertion, Registry.getDefault().getEntityFinder(), null);
                } catch (FindException e) {
                    logger.log(Level.WARNING, "Error looking up entities for assertion: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                } catch (final PermissionDeniedException e) {
                    logger.log(Level.WARNING, "Permission denied when looking up entities for assertion: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                }
            }
        }
        return assertion;
    }

    /**
     * @return the assertion this node represents
     */
    @Override
    public final AT asAssertion() {
        return assertion;
    }

    /**
     * Get the contained assertion's policy xml. If the contained assertion is a CompositeAssertion or an Include,
     * then null will be returned
     *
     * @return String properties xml for the contained assertion, or null if the assertion is a folder assertion.
     */
    public String getAssertionPropsAsString(){
        String props = null;

        if(assertionPropsAsString != null){
            props = assertionPropsAsString.get();
            if(props != null) return props;
            //props == null, the String has been garbage collected or cleared
        }

        final Assertion assertion = asAssertion();
        //ignorning folder assertions now as they have no interesting props of their own to search and copying them
        //just to remove their children for their props xml would likely be expensive
        if (!(assertion instanceof CompositeAssertion) && !(assertion instanceof Include)) {
            final StringBuilder sb = new StringBuilder(WspWriter.getPolicyXml(assertion));
            sb.append(AssertionUtils.getBase64EncodedPropsDecoded(assertion));
            props = sb.toString();
            assertionPropsAsString = new SoftReference<String>(props);
            return props;
        }
        //todo post pandora it should be possible to search comments on 'folder' assertions

        return props;
    }

    @SuppressWarnings({ "unchecked" })
    @Override
    public void setUserObject(Object userObject) {
        // TODO we don't do a proper type check here because SAML assertion may try to replace
        // a RequestWssSaml2 with a RequestWssSaml
        assertion = (AT)userObject;
        super.setUserObject(userObject);
        clearPropsString();
    }

    /**
     * Call when ever the user object is set or the contents of the user object (Assertion) has changed
     */
    public void clearPropsString(){
        if(assertionPropsAsString != null){
            assertionPropsAsString.clear();
            //don't recreate the properties string until it is needed
        }
    }

    /**
     * Get the assertion index path for this tree node.
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
     * Get a descendant tree node by index path.
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
     * Obtain the name to display in the policy editor, validator warnings pane and assertion pallete tree.
     * @param decorate should only be true when the name will be displayed in the policy window. No comments
     * should ever be added to the return string when decorate is false.
     * @return String representation of the assertion contained within the TreeNode
     */
    abstract public String getName(boolean decorate);

    /**
     * @return the node name that is displayed
     */
    @Override
    public String getName(){
        return getName(true);
    }

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
     * that relate to the set of paths that are associated with the view
     * will be returned.
     *
     * @return the list of validator messages
     */
    public List getValidatorMessages() {
        if (viewValidatorMessages != null) {
            return viewValidatorMessages;
        }

        return viewValidatorMessages = this.validatorMessages;
    }

    /**
     * Tooltip override, if there is an validator message, shows as tooltip.
     *
     * @return the tooltip text or null
     */
    @Override
    public String getTooltipText() {
        List messages = getValidatorMessages();
        StringBuilder sb = new StringBuilder();
        if (messages.isEmpty()) {
            final String st = super.getTooltipText();
            if (st != null) sb.append(st);
            Assertion ass = this.asAssertion();
            if (ass instanceof SetsVariables) {
                SetsVariables sv = (SetsVariables)ass;
                final VariableMetadata[] vars = PolicyVariableUtils.getVariablesSetNoThrow(sv);
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
                        if (i%3 == 2) sb.append("<br>");
                    }
                }
            }

            final Assertion.Comment comment = ass.getAssertionComment();
            if (comment != null){
                if(sb.length() > 0) sb.append(sb.toString().endsWith("<br>")? "<br>" : "<br><br>");

                StringBuilder builder = new StringBuilder();
                String leftComment = comment.getAssertionComment(Assertion.Comment.LEFT_COMMENT);
                final boolean hasLeft = leftComment != null && !leftComment.trim().isEmpty();

                if (hasLeft) {
                    builder.append(TextUtils.enforceToBreakOnMultipleLines(leftComment, 100, "<br>", true)); // "true" means to escape HTML special characters.
                }

                String rightComment = comment.getAssertionComment(Assertion.Comment.RIGHT_COMMENT);
                if (rightComment != null && !rightComment.trim().isEmpty()) {
                    if (hasLeft) builder.append("<br>");

                    builder.append(TextUtils.enforceToBreakOnMultipleLines(rightComment, 100, "<br>", true)); // "true" means to escape HTML special characters.
                }

                sb.append(builder.toString());
            }

            if (sb.length() > 0) {
                sb.insert(0, "<html>");
                sb.append("</html>");
                return sb.toString();
            } else {
                return null;
            }
        } else {
            String toBeFormatted = "<html><strong>The policy may be invalid due to {0}<br>";
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
            String msg = "warnings and errors:";
            if (hasWarnings && !hasErrors) {
                msg = "warnings:";
            } else if (!hasWarnings && hasErrors) {
                msg = "errors:";
            }
            sb.insert(0, MessageFormat.format(toBeFormatted, msg));
            return sb.toString();
        }
    }

    /**
     * Check if the node's ancestor is disabled.
     * The attribute "ancestorDisabled" will be cached after this method called.
     *
     * @return true if there exists an ancestor disabled.
     */
    public boolean isAncestorDisabled() {
        if (ancestorDisabled != null) return ancestorDisabled;

        AssertionTreeNode parent = (AssertionTreeNode) getParent();
        if (parent == null) ancestorDisabled = false;
        else ancestorDisabled = parent.isAncestorDisabled()? true : !parent.assertion.isEnabled();

        return ancestorDisabled;
    }

    public void setAncestorDisabled(boolean ancestorDisabled) {
        this.ancestorDisabled = ancestorDisabled;
    }

    /**
     * Check if an assertion node is enabled or disabled.
     * If there exists an ancestor disabled, then the node must be disabled.
     * Otherwise, the node enabling status depends its own enabling status.
     *
     * @return true if the node is evaluated as "enabled".
     */
    public boolean isAssertionEnabled() {
        return isAncestorDisabled()?
            false :                   // If the ancestor is disabled, then the node must be disabled.
            assertion.isEnabled();    // Otherwise, the result depends on the node's enabling status.
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

        // Add Expand/Collapse assertion action
        // Note: the expand/collapse assertion action is not limited into the below condition checking, since the action is purely for GUI displaying purpose.
        ExpandOrCollapseAssertionAction expandOrCollapseAssertionAction;
        if (isExpanded()) {
            expandOrCollapseAssertionAction = new CollapseAssertionAction();
        } else {
            expandOrCollapseAssertionAction = new ExpandAssertionAction();
        }
        expandOrCollapseAssertionAction.setEnabled(this.getChildCount() > 0);
        list.add(expandOrCollapseAssertionAction);

        CompositeAssertionTreeNode ca;
        if (this instanceof CompositeAssertionTreeNode) {
            ca = (CompositeAssertionTreeNode)this;
        } else {
            TreeNode parent = getParent();
            if (parent instanceof CompositeAssertionTreeNode) {
                ca = (CompositeAssertionTreeNode) parent;
            } else {
                list.add(new AssertionInfoAction(assertion));
                if (isDescendantOfInclude(true)) return list.toArray(new Action[list.size()]);
                throw new IllegalStateException("Assertion parent is neither an Include nor a Composite");
            }
        }

        int position = (this instanceof CompositeAssertionTreeNode) ? 0 : this.getParent().getIndex(this) + 1;
        list.add(new AddAllAssertionAction(ca, position));

        if (Registry.getDefault().getLicenseManager().isAssertionEnabled(ONEORMORE_PROTOTYPE)) {
            list.add(new AddOneOrMoreAssertionAction(ca, position));
        }

        if(!isDescendantOfInclude(false)){
            list.add(new CreateIncludeFragmentAction(this));
        }

        try {
            // Case 1: if the node is associated to a published service
            PublishedService svc = getService();
            boolean hasPermission = Registry.getDefault().getSecurityProvider().hasPermission(new AttemptedUpdate(EntityType.SERVICE, svc));

            // Case 2: if the node is associated to a policy fragment
            if (svc == null && !hasPermission) {
                Policy policy = getPolicy();
                hasPermission = Registry.getDefault().getSecurityProvider().hasPermission(new AttemptedUpdate(EntityType.POLICY, policy));
            }

            if (hasPermission) {
                Action da = new DeleteAssertionAction(this);
                da.setEnabled(canDelete());
                list.add(da);

                Action mu = new AssertionMoveUpAction(this);
                mu.setEnabled(canMoveUp());
                list.add(mu);

                Action md = new AssertionMoveDownAction(this);
                md.setEnabled(canMoveDown());
                list.add(md);

                // Add a disable assertion action or an enable assertion action.
                if (isAssertionEnabled()) {
                    list.add(new DisableAssertionAction(this));
                } else {
                    list.add(new EnableAssertionAction(this));
                }

                // Add "Enable All Assertions" action onto a composite tree node.
                if (this instanceof CompositeAssertionTreeNode) {
                    list.add(new EnableAllAssertionsAction(this));
                }

                list.add( new AddEditDeleteCommentAction(this));
                if(assertion.getAssertionComment() != null) list.add(new AddEditDeleteCommentAction(this, true));

            }
        } catch (Exception e) {
            throw new RuntimeException("Couldn't get current service or policy", e);
        }

        list.add(new AssertionInfoAction(assertion));
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
     * Is this nodes assertion editable?
     *
     * @return false if read only
     */
    @Override
    public boolean canEdit() {
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
            @Override
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

    @Override
    public int compareTo(AssertionTreeNode o) {
        final List<Integer> whatVirtualOrdinal = getVirtualOrdinal(o);
        final List<Integer> thisOrdinals = getVirtualOrdinal(this);

        for (int i = 0; i < thisOrdinals.size() && i < whatVirtualOrdinal.size(); i++) {
            final int compare = thisOrdinals.get(i).compareTo(whatVirtualOrdinal.get(i));
            if (compare != 0) return compare;
        }
        //e.g. 2.2.2 compare to 2.2.2.1 => 3 compare to 4 = -1 as 2.2.2 comes first
        return new Integer(thisOrdinals.size()).compareTo(whatVirtualOrdinal.size());
    }

    /**
     * assign the policy template.
     */
    private void assignPolicyTemplate(PolicyTemplateNode templateNode) {
        EntityWithPolicyNode policyNode = getPolicyNodeCookie();
        if ( policyNode == null )
            throw new IllegalArgumentException("No edited policy specified");

        try {
            final Policy policy = policyNode.getPolicy();
            final String oldPolicyXml = policy.getXml();
            if ( PolicyExportUtils.importPolicyFromFile( policy, templateNode.getFile() ) ) {
                policyNode.firePropertyChange(this, "policy", oldPolicyXml, policy.getXml());
            }
        } catch (FindException e) {
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
    protected EntityWithPolicyNode getPolicyNodeCookie() {
        return getPolicyNode(this);
    }

    public PublishedService getService() throws FindException {
        ServiceNode sn = getServiceNodeCookie();
        if (sn == null) return null;
        return sn.getEntity();
    }

    public Policy getPolicy() throws FindException {
        EntityWithPolicyNode pn = getPolicyNodeCookie();
        if (pn == null) return null;
        return pn.getPolicy();
    }

    public static boolean isDecorateComment() {
        return decorateComment;
    }

    public static void setDecorateComment(boolean decorateComment) {
        AssertionTreeNode.decorateComment = decorateComment;
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
    public static EntityWithPolicyNode getPolicyNode(AssertionTreeNode node) {
        for (Iterator i = ((AbstractTreeNode)node.getRoot()).cookies(); i.hasNext();) {
            Object value = ((Cookie)i.next()).getValue();
            if (value instanceof EntityWithPolicyNode) return (EntityWithPolicyNode)value;
        }
        return null;
    }

    static List<Integer> getVirtualOrdinal(final AssertionTreeNode treeNode){
        AbstractTreeNode parent = (AbstractTreeNode) treeNode.getParent();
        List<Integer> ordinalList = new ArrayList<Integer>();
        ordinalList.add(treeNode.asAssertion().getOrdinal());
        while (parent != null) {
            if (parent.asAssertion() instanceof Include){
                ordinalList.add(parent.asAssertion().getOrdinal());
            }
            parent = (AbstractTreeNode) parent.getParent();
        }

        Collections.reverse(ordinalList);
        return ordinalList;
    }

    public static String getVirtualOrdinalString(final AssertionTreeNode treeNode) {
        final List<Integer> virtualOrdinal = getVirtualOrdinal(treeNode);

        StringBuilder sb = new StringBuilder();
        for (int i = 0, virtualOrdinalSize = virtualOrdinal.size(); i < virtualOrdinalSize; i++) {
            Integer integer = virtualOrdinal.get(i);
            sb.append(integer);
            if(i < virtualOrdinalSize - 1) sb.append(".");
        }

        return sb.toString();
    }

    /**
     * Enable all disabled ancestors of the current tree node and update these ancestors' attribute "ancestorDisabled".
     */
    public void enableAncestors() {
        CompositeAssertionTreeNode parent = (CompositeAssertionTreeNode) getParent();
        CompositeAssertionTreeNode processedPrevNode = null;

        while (parent != null) {
            boolean isParentEnabled = parent.isAssertionEnabled();

            if (isParentEnabled) {
                // If an ancestor is found to be enabled, then stop searching.
                break;
            } else {
                // Enable the parent (one of ancestors)
                parent.asAssertion().setEnabled(true);
                parent.setAncestorDisabled(false);

                // Disable only next level children of the parent and update their attribute "ancestorDisabled" as "false".
                // In UI, the parent's children should be shown as "disabled" as well, so their enabling status is forced to
                // change to "disabled" (i.e., setEnabled(false)).
                int count = parent.getChildCount();
                for (int i = 0; i < count; i++) {
                    AssertionTreeNode child = (AssertionTreeNode) parent.getChildAt(i);
                    if (child != this && child != processedPrevNode) {
                        child.asAssertion().setEnabled(false);
                        child.setAncestorDisabled(false);
                    }
                }
            }

            processedPrevNode = parent;
            parent = (CompositeAssertionTreeNode) parent.getParent();
        }
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

    public void serviceChanged(PublishedService service) {
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
        if (draggingNode instanceof IncludeAssertionPolicyNode) {
            IncludeAssertionPolicyNode iapn = (IncludeAssertionPolicyNode) draggingNode;
            include = iapn.asAssertion();
        }
        if (include != null) {
            try {
                Policy thisPolicy = getPolicyNodeCookie().getPolicy();
                if ( thisPolicy.getType() == PolicyType.INCLUDE_FRAGMENT && !Goid.isDefault(thisPolicy.getGoid()) ) {
                    Set<String> policyGuids = new HashSet<String>();
                    policyGuids.add(thisPolicy.getGuid());
                    try {
                        Registry.getDefault().getPolicyPathBuilderFactory().makePathBuilder().inlineIncludes( include, policyGuids, true );
                    } catch(PolicyAssertionException e) {
                        logger.warning("Refusing to create circular reference to policy #" + thisPolicy.getGuid() + ", not accepting drag of " + draggingNode.getClass().getSimpleName() + " into " + this.getClass().getSimpleName());
                        return true;
                    }
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Couldn't get current policy");
            }
        }

        return isDescendantOfInclude(true);
    }
}