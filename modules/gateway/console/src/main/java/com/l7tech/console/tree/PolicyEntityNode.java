package com.l7tech.console.tree;

import com.l7tech.console.MainWindow;
import com.l7tech.console.action.*;
import com.l7tech.console.tree.servicesAndPolicies.PolicyNodeFilter;
import com.l7tech.console.util.EncapsulatedAssertionConsoleUtil;
import com.l7tech.console.util.Registry;
import com.l7tech.gui.util.ImageCache;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.policy.PolicyType;
import com.l7tech.util.ExceptionUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/** @author alex */
@SuppressWarnings( { "serial" } )
public class PolicyEntityNode extends EntityWithPolicyNode<Policy, PolicyHeader> {
    protected volatile Reference<Policy> policy;
    private static Map<String, Reference<Image>> aliasDecoratedImages = new ConcurrentHashMap<String, Reference<Image>>();

    private static final String OVERLAY_ALIAS = MainWindow.RESOURCE_PATH + "/alias16.png";

    private static final String OVERLAY_DISABLED = MainWindow.RESOURCE_PATH + "/RedCrossSign16.gif";

    public PolicyEntityNode(PolicyHeader e) {
        this(e, null);
    }

    public PolicyEntityNode(PolicyHeader e, Comparator c) {
        super(e, c);
    }

    @Override
    public void updateUserObject() throws FindException{
        policy = null;
        getEntity();
    }
    
    @Override
    public Policy getPolicy() throws FindException {
        if (policy != null) {
            Policy p = policy.get();
            if (p != null)
                return p;
        }

        PolicyHeader eh = getEntityHeader();
        Policy updatedPolicy = Registry.getDefault().getPolicyAdmin().findPolicyByGuid(eh.getGuid());

        // throw something if null, the service may have been deleted
        if (updatedPolicy == null) {
            orphanMe();
            return null; // Unreached; method always throws
        }

        PolicyHeader newEh = new PolicyHeader(updatedPolicy);
        newEh.setAliasGoid(eh.getAliasGoid());
        newEh.setFolderId(eh.getFolderId());
        newEh.setPolicyDisabled(updatedPolicy.isDisabled());

        setUserObject(newEh);
        firePropertyChange(this, "UserObject", eh, newEh);

        this.policy = new SoftReference<Policy>(updatedPolicy);
        return updatedPolicy;
    }

    @Override
    public Policy getEntity() throws FindException {
        return getPolicy();
    }

    @Override
    public List<? extends AbstractTreeNode> collectSearchableChildren(Class[] assignableFromClass, NodeFilter filter) {
        // Has no searchable children; override to avoid forcing a pointless WSDL download and parse (Bug #6936)
        return Collections.emptyList();
    }

    @Override
    public boolean isSearchable(NodeFilter filter) {
        return filter == null || filter instanceof PolicyNodeFilter;
    }

    @Override
    public Action[] getActions() {
        Collection<Action> actions = new ArrayList<Action>();
        actions.add(new EditPolicyAction(this));
        actions.add(new EditPolicyProperties(this));
        actions.add(new DeletePolicyAction(this));
        actions.add(new MarkEntityToAliasAction(this));
        actions.add(new CreateEntityLogSinkAction(getEntityHeader()));
        actions.add(new PolicyRevisionsAction(this));
        if (getEntityHeader().getPolicyType().equals(PolicyType.INCLUDE_FRAGMENT)) {
            try {
                EncapsulatedAssertionConfig config;
                final Collection<EncapsulatedAssertionConfig> found = Registry.getDefault().getEncapsulatedAssertionAdmin().findByPolicyGoid(getEntityHeader().getGoid());
                EncapsulatedAssertionConsoleUtil.attachPolicies(found);
                if (found.isEmpty()) {
                    // policy not yet associated with an EncapsulatedAssertionConfig
                    config = new EncapsulatedAssertionConfig();
                    config.setPolicy(getPolicy());
                    actions.add(new CreateEncapsulatedAssertionAction(config, null, true));
                } else {
                    // policy is already associated with at least one EncapsulatedAssertionConfig
                    final EditEncapsulatedAssertionAction editAction = new EditEncapsulatedAssertionAction(found, null);
                    if (editAction.isAuthorized()) {
                        actions.add(editAction);
                    } else {
                        actions.add(new ViewEncapsulatedAssertionAction(found, null));
                    }
                }
            } catch (final FindException e) {
                logger.log(Level.WARNING, "Cannot add EncapsulatedConfig action because Unable to retrieve policy", ExceptionUtils.getDebugException(e));
            }
        }
        actions.add(new RefreshTreeNodeAction(this));

        Action secureCut = ServicesAndPoliciesTree.getSecuredAction(ServicesAndPoliciesTree.ClipboardActionType.CUT);
        Action secureCopy = ServicesAndPoliciesTree.getSecuredAction(ServicesAndPoliciesTree.ClipboardActionType.COPY, EntityType.POLICY);
        if(secureCut != null){
            actions.add(secureCut);
        }
        if(secureCopy != null && getEntityHeader().getPolicyType()==PolicyType.INCLUDE_FRAGMENT){
            actions.add(secureCopy);
        }
        
        return actions.toArray(new Action[actions.size()]);
    }

    @Override
    protected String getEntityName() {
        return getEntityHeader().getName();
    }

    @Override
    public void clearCachedEntities() {
        policy = null;
    }

    @Override
    public String getName() {
        return getEntityHeader().getDisplayName();
    }

    @Override
    public Image getIcon() {
        Image image = super.getIcon();
        if ( getEntityHeader().isAlias() ) {
            image = getAliasDecoratedImage(OVERLAY_ALIAS, iconResource(false), image );
        }
        else if(getEntityHeader().isPolicyDisabled()){
            image = getAliasDecoratedImage(OVERLAY_DISABLED, iconResource(false), image );
        }
        return image;
    }

    @Override
    protected String iconResource(boolean open) {
        PolicyHeader header = getEntityHeader();
        if(header == null) return "com/l7tech/console/resources/include16.png";

        boolean isSoap = header.isSoap();
        boolean isInternal = header.getPolicyType() == PolicyType.INTERNAL;
        boolean isGlobal = header.getPolicyType() == PolicyType.GLOBAL_FRAGMENT;

        String typeName = "";
        if (isInternal) {
            typeName = "internal";
        } else if (isGlobal) {
            typeName = "global";
        }

        if (isSoap){
            return "com/l7tech/console/resources/include_"+typeName+"soap16.png";
        } else{
            return "com/l7tech/console/resources/include"+(typeName.isEmpty()?"":"_"+typeName)+"16.png";
        }
    }

    @Override
    public String toString() {
        return getName();
    }

    private static Image getAliasDecoratedImage(final String overlaySrc, final String bgPath, final Image bgImage) {
        Reference<Image> ref = aliasDecoratedImages.get(bgPath);
        if (ref != null) {
            Image ret = ref.get();
            if (ret != null)
                return ret;
        }

        Image ret = addOverlay(overlaySrc, bgImage);
        aliasDecoratedImages.put(bgPath, new SoftReference<Image>(ret));
        return ret;
    }

    private static Image addOverlay(final String overlaySrc, final Image bgImage) {
        if(null == overlaySrc || overlaySrc.trim().isEmpty()) return bgImage;
        final Image ret = new BufferedImage(18, 18, BufferedImage.TYPE_INT_ARGB);
        final Image overlayImage = ImageCache.getInstance().getIcon(overlaySrc, PolicyEntityNode.class.getClassLoader() ,java.awt.Transparency.TRANSLUCENT);
        final Graphics g = ret.getGraphics();
        g.drawImage( bgImage, 0, 0, null );
        g.drawImage( overlayImage, 0, 0, null );
        return ret;
    }

}
