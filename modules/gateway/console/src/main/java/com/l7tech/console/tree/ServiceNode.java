package com.l7tech.console.tree;

import com.l7tech.console.MainWindow;
import com.l7tech.console.action.*;
import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.tree.servicesAndPolicies.ServiceNodeFilter;
import com.l7tech.console.tree.wsdl.WsdlTreeNode;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.gui.util.ImageCache;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.Policy;
import com.l7tech.wsdl.Wsdl;

import javax.swing.*;
import javax.swing.tree.MutableTreeNode;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * The class represents a node element in the TreeModel.
 * It represents the published service.
 *
 * @author Emil Marceta
 */
public class ServiceNode extends EntityWithPolicyNode<PublishedService, ServiceHeader> implements Comparable<ServiceNode> {
    static final Logger log = Logger.getLogger(ServiceNode.class.getName());
    private static Map<String, Reference<Image>> bugDecoratedImages = new ConcurrentHashMap<String, Reference<Image>>();
    private volatile Reference<PublishedService> svc = null;

    /**
     * construct the <CODE>ServiceNode</CODE> instance for
     * a given entity header.
     *
     * @param e the EntityHeader instance, must represent published service
     * @throws IllegalArgumentException thrown if unexpected type
     */
    public ServiceNode(ServiceHeader e)
      throws IllegalArgumentException {
        this(e, null);
    }

    public ServiceNode(ServiceHeader e, Comparator c){
        super(e, c);
        setAllowsChildren(e.isSoap());
    }

    @Override
    public void updateUserObject() throws FindException{
        svc = null;
        getEntity();
    }

    @Override
    public PublishedService getEntity() throws FindException {
        if (svc != null) {
            PublishedService s = svc.get();
            if (s != null)
                return s;
        }

        ServiceHeader serviceHeader = getEntityHeader();
        PublishedService svc = Registry.getDefault().getServiceManager().findServiceByID(serviceHeader.getStrId());

        // throw something if null, the service may have been deleted
        if (svc == null) {
            orphanMe();
            return null; // Unreached; method always throws
        }

        this.svc = new SoftReference<PublishedService>(svc);

        ServiceHeader newEh = new ServiceHeader(svc);
        newEh.setPolicyDisabled(svc.getPolicy() == null ? false : svc.getPolicy().isDisabled());
        newEh.setAliasGoid(serviceHeader.getAliasGoid());
        newEh.setFolderId(serviceHeader.getFolderId());
        setUserObject(newEh);
        firePropertyChange(this, "UserObject", serviceHeader, newEh);
        return svc;
    }

    @Override
    public Policy getPolicy() throws FindException {
        return getEntity().getPolicy();
    }

    /**
     * Nullify service,  will cause service reload next time.
     */
    @Override
    public void clearCachedEntities() {
        svc = null;
    }

    /**
     * Get the set of actions associated with this node.
     * This may be used e.g. in constructing a context menu.
     *
     * @return actions appropriate to the node
     */
    @Override
    public Action[] getActions() {
        final Collection<Action> actions = new ArrayList<Action>();

        actions.add(new EditPolicyAction(this));
        actions.add(new EditServiceProperties(this));
        if (getEntityHeader().isSoap()) actions.add(new EditServiceUDDISettingsAction(this));
        actions.add(new DeleteServiceAction(this));
        actions.add(new MarkEntityToAliasAction(this));
        actions.add(new CreateEntityLogSinkAction(this.getEntityHeader()));
        actions.add(new PolicyRevisionsAction(this));
        actions.add(new RefreshTreeNodeAction(this));
        Action secureCut = ServicesAndPoliciesTree.getSecuredAction(ServicesAndPoliciesTree.ClipboardActionType.CUT);
        Action secureCopy = ServicesAndPoliciesTree.getSecuredAction(ServicesAndPoliciesTree.ClipboardActionType.COPY, EntityType.SERVICE);
        if(secureCut != null){
            actions.add(secureCut);
        }
        if(secureCopy != null){
            actions.add(secureCopy);
        }
        return actions.toArray(new Action[actions.size()]);
    }

    /**
     * Returns true if the receiver is a leaf.
     *
     * @return true if leaf, false otherwise
     */
    @Override
    public boolean isLeaf() {
        return !allowsChildren;
    }

    /**
     * subclasses override this method
     */
    @Override
    protected void doLoadChildren() {
        try {
            PublishedService s = getEntity();
            if (s != null && s.isSoap()) {
                Wsdl wsdl = s.parsedWsdl();
//                Wsdl wsdl = Wsdl.newInstance(Wsdl.extractBaseURI(s.getWsdlUrl()), new StringReader(svc.getWsdlXml()));
                WsdlTreeNode.Options opts = new WsdlTreeNode.Options();
                opts.setShowMessages(false);
                opts.setShowPortTypes(false);
                WsdlTreeNode node = WsdlTreeNode.newInstance(wsdl, opts);
                node.setChildrenCut(isCut());
                children = null;
                node.getChildCount();
                //noinspection unchecked
                List<MutableTreeNode> nodes = Collections.list(node.children());
                int index = 0;
                for (MutableTreeNode n : nodes) {
                    insert(n, index++);
                }
            }
        } catch (Exception e) {
            ErrorManager.getDefault().
              notify(Level.SEVERE, e,
                "Error accessing service id=" + getEntityHeader().getGoid());
        }
    }

    @Override
    public List<? extends AbstractTreeNode> collectSearchableChildren(Class[] assignableFromClass, NodeFilter filter) {
        // Has no searchable children; override to avoid forcing a pointless WSDL download and parse (Bug #6936)
        return Collections.emptyList();
    }

    @Override
    public boolean isSearchable(NodeFilter filter) {
        return filter == null || filter instanceof ServiceNodeFilter;
    }

    @Override
    public void setChildrenCut(boolean cut) {
        if (hasLoadedChildren)
            super.setChildrenCut(cut);
    }

    /**
     * @return the node name that is displayed
     */
    @Override
    public String getName() {
        return getEntityHeader().getDisplayName();
    }

    @Override
    protected String getEntityName() {
        return getEntityHeader().getName();
    }

    public String getRoutingUri() {
        return getEntityHeader().getRoutingUri();
    }

    /**
     *
     */
    @Override
    public int compareTo( ServiceNode serviceNode ) {
        return getName().toLowerCase().compareTo( serviceNode.getName().toLowerCase() );
    }

    @Override
    public Image getIcon() {
        Image image = super.getIcon();
        ServiceHeader header = getEntityHeader();
        return header == null || !header.isTracingEnabled() ? image : getBugDecoratedImage(iconResource(false), image);
    }

    @Override
    public Image getOpenedIcon() {
        Image image = super.getOpenedIcon();
        ServiceHeader header = getEntityHeader();
        return header == null || !header.isTracingEnabled() ? image : getBugDecoratedImage(iconResource(true), image);
    }

    private static Image getBugDecoratedImage(String bgPath, Image bgImage) {
        Reference<Image> ref = bugDecoratedImages.get(bgPath);
        if (ref != null) {
            Image ret = ref.get();
            if (ret != null)
                return ret;
        }

        Image ret = addBugOverlay(bgImage);
        bugDecoratedImages.put(bgPath, new SoftReference<Image>(ret));
        return ret;
    }

    private static Image addBugOverlay(Image bgImage) {
        Image bugImage = ImageCache.getInstance().getIcon(MainWindow.RESOURCE_PATH + "/bug16.gif");
        Image ret = new BufferedImage(24, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics g = ret.getGraphics();
        g.drawImage(bgImage, 0, 0, null);
        g.drawImage(bugImage, 8, -1, null);
        return ret;
    }

    /**
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    @Override
    protected String iconResource(boolean open) {
        ServiceHeader header = getEntityHeader();

        if (header == null) {
            return "com/l7tech/console/resources/services_disabled16.png";
        }
        else if (header.isPolicyDisabled() || header.isDisabled()) {
            if (!header.isSoap()) {
                if(header.isAlias()){
                    return "com/l7tech/console/resources/xmlObject_disabled16Alias.png";                    
                }else{
                    return "com/l7tech/console/resources/xmlObject_disabled16.png";
                }
            } else {
                if(header.isAlias()){
                    return "com/l7tech/console/resources/services_disabled16Alias.png";                    
                }else{
                    return "com/l7tech/console/resources/services_disabled16.png";
                }
            }
        } else {
            if(header.isSoap()) {
                if(header.isAlias()){
                    return "com/l7tech/console/resources/services16Alias.png";                    
                }else{
                    return "com/l7tech/console/resources/services16.png";
                }
            } else {
                if(header.isAlias()){
                    return "com/l7tech/console/resources/xmlObject16Alias.gif";                    
                }else{
                    return "com/l7tech/console/resources/xmlObject16.gif";
                }
            }
        }
    }

    @Override
    public String toString() {
        return getName();
    }
}
