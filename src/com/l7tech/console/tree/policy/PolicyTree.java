package com.l7tech.console.tree.policy;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.util.ClipboardActions;
import com.l7tech.common.security.rbac.AttemptedUpdate;
import com.l7tech.common.security.rbac.EntityType;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.console.action.ActionManager;
import com.l7tech.console.action.DeleteAssertionAction;
import com.l7tech.console.action.EditServicePolicyAction;
import com.l7tech.console.action.SecureAction;
import com.l7tech.console.poleditor.PolicyEditorPanel;
import com.l7tech.console.policy.PolicyTransferable;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.TransferableTreePath;
import com.l7tech.console.tree.TreeNodeHidingTransferHandler;
import com.l7tech.console.util.*;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.service.PublishedService;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class PolicyTree is the extended <code>JTree</code> with addtional
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class PolicyTree extends JTree implements DragSourceListener,
  DragGestureListener, Autoscroll, TreeModelListener, Refreshable {
    static final Logger log = Logger.getLogger(PolicyTree.class.getName());
    /**
     * component name
     */
    public final static String NAME = "policy.tree";
    private PolicyEditorPanel policyEditorPanel;

    // d&d
    private TreePath pathSource;                // The path being dragged
    private BufferedImage imgGhost;                    // The 'drag image'
    private Point ptOffset = new Point();    // Where, in the drag image, the mouse was clicked
    private Border topBorder;
    private boolean writeAccess;


    /**
     * Create the new policy tree with the policy model.
     *
     * @param newModel
     */
    public PolicyTree(PolicyTreeModel newModel) {
        super(newModel);
        initialize();
        setSelectionModel(getTreeSelectionModel());
    }

    public PolicyEditorPanel getPolicyEditorPanel() {
        return policyEditorPanel;
    }

    /**
     * Create empty policy tree
     */
    public PolicyTree() {
        this(null);
    }

    public void setModel(TreeModel newModel) {
        if (newModel == null) return;

        TreeModel oldModel = getModel();
        if (oldModel != null) {
            oldModel.removeTreeModelListener(this);
        }
        super.setModel(newModel);
        newModel.addTreeModelListener(this);
    }

    /**
     * initialize
     */
    private void initialize() {
        topBorder = BorderFactory.createEmptyBorder(10, 0, 0, 0);
        setBorder(topBorder);
        // Make this JTree a drag source
        DragSource dragSource = DragSource.getDefaultDragSource();
        dragSource.createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_COPY_OR_MOVE, this);

        // Also, make this JTree a drag target
        DropTarget dropTarget = new DropTarget(this, new PolicyDropTargetListener());
        dropTarget.setDefaultActions(DnDConstants.ACTION_COPY_OR_MOVE);

        addKeyListener(new TreeKeyListener());
        addMouseListener(new TreeMouseListener());
        setCellRenderer(new PolicyTreeCellRenderer());

        ToolTipManager.sharedInstance().registerComponent(this);


        TreeSelectionListener tsl = new TreeSelectionListener() {
            ClipboardOwner owner = new ClipboardOwner() {
                public void lostOwnership(Clipboard clipboard, Transferable contents) {
                    // No action required
                }
            };

            public void valueChanged(TreeSelectionEvent e) {
                Clipboard sel = Utilities.getDefaultSystemSelection();
                if (sel == null) return;
                sel.setContents(createTransferable(e.getPath()), owner);
            }
        };

        if (Utilities.getDefaultSystemSelection() != null)
            getSelectionModel().addTreeSelectionListener(tsl);

        ClipboardActions.replaceClipboardActionMap(this);

        // To support "Copy All", need to register a "copyAll" action that does equivalent of Select All followed by Copy.
        getActionMap().put("copyAll", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                getSelectionModel().clearSelection();
                ClipboardActions.getCopyAction().actionPerformed(e);
            }
        });

        setTransferHandler(new PolicyTreeTransferHandler());
    }

    private Transferable createTransferable(TreePath path) {
        if (path != null) {
            Object node = path.getLastPathComponent();
            if (node == null) return null;
            if (node instanceof AbstractTreeNode)
                return new PolicyTransferable((AbstractTreeNode)node);
            else
                log.fine("Unable to create transferable for non-AbstractTreeNode: " + node.getClass().getName());
        } else {
            // No selection, so copy entire policy
            Object node = getModel().getRoot();
            if (node == null) return null;
            if (node instanceof AbstractTreeNode)
                return new PolicyTransferable((AbstractTreeNode)node);
            else
                log.fine("Unable to create transferable for non-AbstractTreeNode: " + node.getClass().getName());
        }
        return null;
    }


    /**
     * Import the specified assertion subtree into this policy.  Attempt to insert it at the current selection,
     * if possible; otherwise, insert it at the end.
     * @param ass
     * @return true if the assertion subtree was imported successfully; false if nothing was done.
     */
    private boolean importAssertion(Assertion ass) {
        TreePath path = getSelectionPath();
        if (path == null) {
            AbstractTreeNode atn = (AbstractTreeNode)getModel().getRoot();
            path = new TreePath(atn.getPath());
        }

        if (!(path.getLastPathComponent() instanceof AssertionTreeNode)) {
            // todo is this possible?
            log.warning("Rejecting paste -- paste target is not an AssertionTreeNode");
            return false;
        }
        AssertionTreeNode targetTreeNode = (AssertionTreeNode)path.getLastPathComponent();
        AssertionTreeNode assertionTreeNodeCopy = AssertionTreeNodeFactory.asTreeNode(ass);
        boolean dropAsFirstContainerChild = true; // TODO not really any way to change this.. test and see what feels better
        PolicyTreeModel model = (PolicyTreeModel)getModel();

        if (targetTreeNode.getAllowsChildren()) {
            int targetIndex = 0;
            if (!dropAsFirstContainerChild && targetTreeNode == model.getRoot()) {
                targetIndex = targetTreeNode.getChildCount();
            }
            model.insertNodeInto(assertionTreeNodeCopy, targetTreeNode, targetIndex);
            return true;
        } else {
            final DefaultMutableTreeNode parent = (DefaultMutableTreeNode)targetTreeNode.getParent();

            int index = parent.getIndex(targetTreeNode);
            if (index != -1) {
                model.insertNodeInto(assertionTreeNodeCopy, parent, index + 1);
                return true;
            }
        }

        return false;
    }

    public void setPolicyEditor(PolicyEditorPanel pe) {
        policyEditorPanel = pe;
    }

    /**
     * KeyAdapter for the policy trees
     */
    class TreeKeyListener extends KeyAdapter {
        /**
         * Invoked when a key has been pressed.
         */
        public void keyPressed(KeyEvent e) {
            if (isIdentityView()) return;
            JTree tree = (JTree)e.getSource();
            TreePath path = tree.getSelectionPath();
            if (path == null) return;
            AssertionTreeNode node =
              (AssertionTreeNode)path.getLastPathComponent();
            if (node == null) return;
            int keyCode = e.getKeyCode();
            if (keyCode == KeyEvent.VK_DELETE) {
                AssertionTreeNode[] nodes = toAssertionTreeNodeArray(tree.getSelectionPaths());
                if (nodes.length < 2) nodes = null;
                if (canDelete(node, nodes)){
                    new DeleteAssertionAction(node, nodes).actionPerformed(null);
                }
            } else if (keyCode == KeyEvent.VK_ENTER) {
                // default properties
            }
        }

        private AssertionTreeNode[] toAssertionTreeNodeArray(TreePath[] paths) {
            java.util.List assertionTreeNodes = new ArrayList();

            if (paths != null) {
                for (int p=0; p<paths.length; p++) {
                    TreePath path = paths[p];
                    assertionTreeNodes.add((AssertionTreeNode)path.getLastPathComponent());
                }
            }

            return (AssertionTreeNode[]) assertionTreeNodes.toArray(new AssertionTreeNode[assertionTreeNodes.size()]);
        }

        private boolean canDelete(AssertionTreeNode node, AssertionTreeNode[] nodes) {
            if (!Registry.getDefault().isAdminContextPresent()) return false;
            PublishedService svc = null;
            try {
                svc = node.getService();
            } catch (Exception e) {
                throw new RuntimeException("Couldn't get current service", e);
            }
            if (!Registry.getDefault().getSecurityProvider().hasPermission(new AttemptedUpdate(EntityType.SERVICE, svc))) return false;
            boolean delete = false;

            if (nodes == null) {
                delete = node.canDelete();
            } else if (nodes != null && nodes.length > 0){
                boolean allDelete = true;
                for (int n=0; n<nodes.length; n++) {
                    AssertionTreeNode current = nodes[n];
                    if (current == null || !current.canDelete()) {
                        allDelete = false;
                        break;
                    }
                }
                delete = allDelete;
            }

            return delete;
        }
    }

    class TreeMouseListener extends PopUpMouseListener {
        int initialToolTipDelay = -1;
        int dismissToolTipDelay = -1;

        /**
         * Handle the mouse click popup when the Tree item is right clicked. The context sensitive
         * menu is displayed if the right click was over an item.
         *
         * @param mouseEvent
         */
        protected void popUpMenuHandler(MouseEvent mouseEvent) {
            JTree tree = (JTree)mouseEvent.getSource();
            AbstractTreeNode node = null;

            if (isIdentityView()) return; // non editable if identity view

            if (mouseEvent.isPopupTrigger()) {
                int closestRow = tree.getClosestRowForLocation(mouseEvent.getX(), mouseEvent.getY());
                if (closestRow == -1) {
                    node = (AbstractTreeNode)tree.getModel().getRoot();
                    if (node == null) {
                        return;
                    }
                } else {
                    int[] rows = tree.getSelectionRows();
                    boolean found = false;

                    for (int i = 0; rows != null && i < rows.length; i++) {
                        if (rows[i] == closestRow) {
                            found = true;
                            break;
                        }
                    }

                    if (!found) {
                        tree.setSelectionRow(closestRow);
                    }
                    node = (AbstractTreeNode)tree.getLastSelectedPathComponent();
                }

                if (node != null) {

                    Action[] actions = node.getActions();
                    if (policyEditorPanel != null) {
                        policyEditorPanel.updateActions(actions);
                    }
                    JPopupMenu menu = getPopupMenu(actions);
                    if (menu != null) {
                        menu.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
                    }

                }
            }
        }

        /**
         * Invoked when the mouse has been clicked on a component.
         */
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() != 2) return;
            if (isIdentityView()) return;
            JTree tree = (JTree)e.getSource();
            TreePath path = tree.getSelectionPath();
            if (path == null) return;
            AssertionTreeNode node = (AssertionTreeNode)path.getLastPathComponent();
            if (node == null) return;

            AbstractTreeNode closestNode = null;
            int closestRow = tree.getRowForLocation(e.getX(), e.getY());
            if (closestRow == -1) {
                closestNode = (AbstractTreeNode)tree.getModel().getRoot();
                if (closestNode != node) {
                    return;
                }
            }

            Action a = node.getPreferredAction();
            if (a != null) {
                if (a instanceof SecureAction) {
                    SecureAction sa = (SecureAction)a;
                    if (!sa.isAuthorized()) return;
                }

                ActionManager.getInstance().invokeAction(a);
            }

        }

        /**
         * Invoked when the mouse enters a component.
         */
        public void mouseEntered(MouseEvent e) {
            initialToolTipDelay = ToolTipManager.sharedInstance().getInitialDelay();
            ToolTipManager.sharedInstance().setInitialDelay(100);
            dismissToolTipDelay = ToolTipManager.sharedInstance().getDismissDelay();
            ToolTipManager.sharedInstance().setDismissDelay(60 * 1000);
        }

        /**
         * Invoked when the mouse exits a component.
         */
        public void mouseExited(MouseEvent e) {
            if (initialToolTipDelay != -1) {
                ToolTipManager.sharedInstance().setInitialDelay(initialToolTipDelay);
            }
            if (dismissToolTipDelay != -1) {
                ToolTipManager.sharedInstance().setDismissDelay(dismissToolTipDelay);
            }
        }
    }

    /**
     * Make a popup menu from actions.
     * The menu is constructed from the set of actions and returned. If the actions arrays
     * is empty, or there are no actions that could be added to the menu (unauthorized user)
     * the <b>null</b>  <code>JPopupMenu</code> is returned
     *
     * @return the popup menu
     */
    private JPopupMenu getPopupMenu(Action[] actions) {
        if (actions == null || actions.length == 0)
            return null;
        JPopupMenu pm = new JPopupMenu();
        boolean empty = true;
        for (int i = 0; i < actions.length; i++) {
            final Action action = actions[i];
            if (action instanceof SecureAction) {
                if (((SecureAction)action).isAuthorized()) {
                    pm.add(action);
                    empty = false;
                }
            } else {
                pm.add(action);
                empty = false;
            }
        }

        // If system clipboard is unavailable, the context menu would go to the "fake" clipboard,
        // but keyboard shortcuts might still work properly with the "real" system clipboard.
        // To prevent this confusing behavior, we'll just suppress the context menu Copy/Paste if
        // the system clipboard isn't accessible to our code.
        if (ClipboardActions.isSystemClipboardAvailable() && ClipboardActions.getGlobalCopyAction().isEnabled()) {
            pm.add(new JPopupMenu.Separator());
            pm.add(ClipboardActions.getGlobalCopyAction());
            empty = false;
            if (ClipboardActions.getGlobalCopyAllAction().isEnabled())
                pm.add(ClipboardActions.getGlobalCopyAllAction());
            // To prevent obvious UI tragedy, we never add Paste as first item unless Copy is safely above it
            if (ClipboardActions.getGlobalPasteAction().isEnabled())
                pm.add(ClipboardActions.getGlobalPasteAction());
        }

        if (empty) { // no items have been added
            return null;
        }

        Utilities.removeToolTipsFromMenuItems(pm);
        return pm;
    }


    public void dragEnter(DragSourceDragEvent dsde) {
        log.entering(this.getClass().getName(), "dragEnter");
    }

    public void dragOver(DragSourceDragEvent dsde) {
        //log.entering(this.getClass().getName(), "dragOver");  // very spammy trace
    }

    public void dropActionChanged(DragSourceDragEvent dsde) {
        log.entering(this.getClass().getName(), "dropActionChanged");
    }

    public void dragDropEnd(DragSourceDropEvent dsde) {
        log.entering(this.getClass().getName(), "dragDropEnd");
        if (dsde.getDropSuccess()) {
            int nAction = dsde.getDropAction();
            if (nAction == DnDConstants.ACTION_MOVE) {
                // The dragged item (pathSource) has been inserted at the target selected by the user.
                // Now it is time to delete it from its original location.
                log.fine("REMOVING: " + pathSource.getLastPathComponent());
                DefaultTreeModel model = (DefaultTreeModel)getModel();
                model.removeNodeFromParent((MutableTreeNode)pathSource.getLastPathComponent());
                pathSource = null;
            }
        }
        //repaint();
    }

    public void dragExit(DragSourceEvent dse) {
    }

    public void dragGestureRecognized(DragGestureEvent dge) {
        Point ptDragOrigin = dge.getDragOrigin();
        TreePath path = getPathForLocation(ptDragOrigin.x, ptDragOrigin.y);
        if (!canStartDrag(path)) {
            return;
        }

        // Work out the offset of the drag point from the TreePath bounding rectangle origin
        Rectangle raPath = getPathBounds(path);
        ptOffset.setLocation(ptDragOrigin.x - raPath.x, ptDragOrigin.y - raPath.y);

        // Get the cell renderer (which is a JLabel) for the path being dragged
        JLabel lbl = (JLabel)getCellRenderer().getTreeCellRendererComponent
          (this, // tree
           path.getLastPathComponent(), // value
           false, // isSelected	(dont want a colored background)
           isExpanded(path), // isExpanded
           getModel().isLeaf(path.getLastPathComponent()), // isLeaf
           0, // row			(not important for rendering)
           false                                            // hasFocus		(dont want a focus rectangle)
          );
        lbl.setSize((int)raPath.getWidth(), (int)raPath.getHeight()); // <-- The layout manager would normally do this

        // Get a buffered image of the selection for dragging a ghost image
        imgGhost = new BufferedImage((int)raPath.getWidth(), (int)raPath.getHeight(), BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D g2 = imgGhost.createGraphics();

        // Ask the cell renderer to paint itself into the BufferedImage
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC, 0.5f));        // Make the image ghostlike
        lbl.paint(g2);

        // Now paint a gradient UNDER the ghosted JLabel text (but not under the icon if any)
        Icon icon = lbl.getIcon();
        int nStartOfText = (icon == null) ? 0 : icon.getIconWidth() + lbl.getIconTextGap();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.DST_OVER, 0.5f));    // Make the gradient ghostlike
        g2.setPaint(new GradientPaint(nStartOfText, 0, SystemColor.controlShadow,
                                      getWidth(), 0, new Color(255, 255, 255, 0)));
        g2.fillRect(nStartOfText, 0, getWidth(), imgGhost.getHeight());

        g2.dispose();


        setSelectionPath(path);    // Select this path in the tree

        log.fine("DRAGGING: " + path.getLastPathComponent());

        // Wrap the path being transferred into a Transferable object
        Transferable transferable = new TransferableTreePath(path);

        // Remember the path being dragged (because if it is being moved, we will have to delete it later)
        pathSource = path;

        // We pass our drag image just in case it IS supported by the platform
        dge.startDrag(null, imgGhost, new Point(5, 5), transferable, this);
    }

    private boolean canStartDrag(TreePath path) {
        if (path == null)
            return false;
        if (isRootPath(path))
            return false;    // Ignore user trying to drag the root node
        if (isIdentityView())
            return false; // Ignore if in identity view

        return hasWriteAccess();

    }

    private boolean hasWriteAccess() {
        /*Subject s = Subject.getSubject(AccessController.getContext());
        AssertionTreeNode an = (AssertionTreeNode)getModel().getRoot();
        final ServiceNode serviceNodeCookie = an.getServiceNodeCookie();
        if (serviceNodeCookie == null) {
            throw new IllegalStateException();
        }
        try {
            ObjectPermission op = new ObjectPermission(serviceNodeCookie.getPublishedService(), ObjectPermission.WRITE);
            return Registry.getDefault().getSecurityProvider().hasPermission(s, op);
        } catch (Exception e) {
            log.log(Level.WARNING, "Error performing permisison check", e);
        }*/

        return writeAccess;
    }

    public void setWriteAccess(boolean writeAccess) {
        this.writeAccess = writeAccess;
    }


    // helpers...
      private TreePath getChildPath(TreePath pathParent, int nChildIndex) {
        TreeModel model = getModel();
        return pathParent.pathByAddingChild(model.getChild(pathParent.getLastPathComponent(), nChildIndex));
    }


    private boolean isRootPath(TreePath path) {
        TreePath rp = new TreePath(getModel().getRoot());
        return rp.equals(path);
        // return isRootVisible() && getRowForPath(path) == 0;
    }

    private boolean isIdentityView() {
        TreeModel model = getModel();
        AssertionTreeNode node = (AssertionTreeNode)model.getRoot();
        return isIdentityView(node);
    }

    public static boolean isIdentityView(AssertionTreeNode an) {
        return an.getRoot() instanceof IdentityViewRootNode;
    }

    private void sayWhat(TreeModelEvent e) {
        log.fine(e.getTreePath().getLastPathComponent().toString());
        int[] nIndex = e.getChildIndices();
        for (int i = 0; nIndex != null && i < nIndex.length; i++) {
            log.fine(i + ". " + nIndex[i]);
        }
    }

// PolicyDropTargetListener interface object...

    class PolicyDropTargetListener implements DropTargetListener {
        // Fields...
        private TreePath pathLast = null;
        private Rectangle2D raCueLine = new Rectangle2D.Float();
        private Rectangle2D raGhost = new Rectangle2D.Float();
        private Color colorCueLine;
        private Point ptLast = new Point();
        private Timer timerHover;
        private int nLeftRight = 0;    // Cumulative left/right mouse movement
        private BufferedImage imgRight = new ArrowImage(15, 15, ArrowImage.ARROW_RIGHT);
        private BufferedImage imgLeft = new ArrowImage(15, 15, ArrowImage.ARROW_LEFT);
        private int nShift = 0;

        // Constructor...
          public PolicyDropTargetListener() {
            colorCueLine = new Color(SystemColor.controlShadow.getRed(),
                                     SystemColor.controlShadow.getGreen(),
                                     SystemColor.controlShadow.getBlue(),
                                     64);

            // Set up a hover timer, so that a node will be automatically expanded or collapsed
            // if the user lingers on it for more than a short time
            timerHover = new Timer(1000, new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    nLeftRight = 0;    // Reset left/right movement trend
                    if (isRootPath(pathLast))
                        return;    // Do nothing if we are hovering over the root node
                    if (isExpanded(pathLast))
                        collapsePath(pathLast);
                    else
                        expandPath(pathLast);
                }
            });
            timerHover.setRepeats(false);    // Set timer to one-shot mode
        }

        // PolicyDropTargetListener interface
          public void dragEnter(DropTargetDragEvent e) {
            if (!hasWriteAccess()) {
                e.rejectDrag();
                return;
            }
            if (!isDragAcceptable(e)) {
                e.rejectDrag();
            } else {
                e.acceptDrag(e.getDropAction());
            }
        }

        public void dragExit(DropTargetEvent e) {
            if (!DragSource.isDragImageSupported()) {
                repaint(raGhost.getBounds());
            }
        }

        public void dragOver(DropTargetDragEvent e) {
            if (!hasWriteAccess()) {
                e.rejectDrag();
                return;
            }

            if (isIdentityView()) {
                e.rejectDrag();
                return;
            }
            DataFlavor[] flavors = e.getCurrentDataFlavors();
            for (int i = 0; i < flavors.length; i++) {
                DataFlavor flavor = flavors[i];
                if (TransferableTreePath.TREEPATH_FLAVOR.equals(flavor)) {
                    treePathdragOver(e);
                    break;
                } else if (PolicyTransferable.ASSERTION_DATAFLAVOR.equals(flavor)) {
                    assertionDragOver(e);
                }
            }
        }

        /**
         * The policy tree drag over handler
         * This is where the ghost image is drawn
         */
        private void treePathdragOver(DropTargetDragEvent e) {
            Point pt = e.getLocation();
            if (pt.equals(ptLast))
                return;

            // Try to determine whether the user is flicking the cursor right or left
            int nDeltaLeftRight = pt.x - ptLast.x;
            if ((nLeftRight > 0 && nDeltaLeftRight < 0) || (nLeftRight < 0 && nDeltaLeftRight > 0))
                nLeftRight = 0;
            nLeftRight += nDeltaLeftRight;
            ptLast = pt;
            Graphics2D g2 = (Graphics2D)getGraphics();

            // If a drag image is not supported by the platform, then draw our own drag image
            if (!DragSource.isDragImageSupported()) {
                paintImmediately(raGhost.getBounds());    // Rub out the last ghost image and cue line
                // And remember where we are about to draw the new ghost image
                raGhost.setRect(pt.x - ptOffset.x, pt.y - ptOffset.y, imgGhost.getWidth(), imgGhost.getHeight());
                g2.drawImage(imgGhost, AffineTransform.getTranslateInstance(raGhost.getX(), raGhost.getY()), null);
            } else    // Just rub out the last cue line
                paintImmediately(raCueLine.getBounds());


            TreePath path = getClosestPathForLocation(pt.x, pt.y);

            int row = getRowForLocation(pt.x, pt.y);
            if (row == -1) {
                path = new TreePath(getModel().getRoot());
            }
            if (!(path == pathLast)) {
                nLeftRight = 0;     // We've moved up or down, so reset left/right movement trend
                pathLast = path;
                timerHover.restart();
            }

            // In any case draw (over the ghost image if necessary) a cue line indicating where a drop will occur
            Rectangle raPath = getPathBounds(path);
            raCueLine.setRect(0, raPath.y + (int)raPath.getHeight(), getWidth(), 2);

            g2.setColor(colorCueLine);
            g2.fill(raCueLine);

            // Now superimpose the left/right movement indicator if necessary
            if (nLeftRight > 20) {
                g2.drawImage(imgRight, AffineTransform.getTranslateInstance(pt.x - ptOffset.x, pt.y - ptOffset.y), null);
                nShift = +1;
            } else if (nLeftRight < -20) {
                g2.drawImage(imgLeft, AffineTransform.getTranslateInstance(pt.x - ptOffset.x, pt.y - ptOffset.y), null);
                nShift = -1;
            } else
                nShift = 0;


            // And include the cue line in the area to be rubbed out next time
            raGhost = raGhost.createUnion(raCueLine);

            // Do this if you want to prohibit dropping onto the drag source
            if (path.equals(pathSource)) {
                e.rejectDrag();
            } else if (pathSource.isDescendant(path) &&
              ((TreeNode)pathSource.getLastPathComponent()).getAllowsChildren()) {
                e.rejectDrag();
            } else
                e.acceptDrag(e.getDropAction());
        }

        /**
         * @param e the drop target event
         */
        private void assertionDragOver(DropTargetDragEvent e) {
            Point pt = e.getLocation();
            TreePath path = getClosestPathForLocation(pt.x, pt.y);
            if (path != null) {
                setSelectionPath(path);
            }
            e.acceptDrag(e.getDropAction());

            //e.rejectDrag();
        }

        public void dropActionChanged(DropTargetDragEvent e) {
            log.fine("dropActionChanged " + e.getCurrentDataFlavors());
            if (!isDragAcceptable(e))
                e.rejectDrag();
            else
                e.acceptDrag(e.getDropAction());
        }

        public void drop(DropTargetDropEvent e) {
            DataFlavor[] flavors = e.getCurrentDataFlavors();
            for (int i = 0; i < flavors.length; i++) {
                DataFlavor flavor = flavors[i];
                if (TransferableTreePath.TREEPATH_FLAVOR.equals(flavor)) {
                    dropTreePath(e);
                    break;
                } else if (PolicyTransferable.ASSERTION_DATAFLAVOR.equals(flavor)) {
                    dropAssertion(e);
                }
            }

        }

        private void dropAssertion(DropTargetDropEvent e) {
            try {
                final Object transferData = e.getTransferable().getTransferData(PolicyTransferable.ASSERTION_DATAFLAVOR);
                boolean dropAsFirstContainerChild = false;
                log.fine("DROPPING: " + transferData);
                AbstractTreeNode node = (AbstractTreeNode)transferData;
                TreePath path = getSelectionPath();
                final Object root = getModel().getRoot();
                if (path == null) {
                    path = new TreePath(root);
                } else {
                    Point location = e.getLocation();
                    int row = getRowForLocation(location.x, location.y);
                    Insets insets = topBorder.getBorderInsets(PolicyTree.this);
                    if (insets.top >= location.y) {
                        dropAsFirstContainerChild = true;
                    }
                    if (row == -1) {
                        path = new TreePath(root);
                    }
                }
                AssertionTreeNode target = (AssertionTreeNode)path.getLastPathComponent();
                if (target.accept(node)) {
                    e.acceptDrop(e.getDropAction());
                    if (dropAsFirstContainerChild) {
                        if (target instanceof CompositeAssertionTreeNode) {
                            CompositeAssertionTreeNode compositeAssertionTreeNode = (CompositeAssertionTreeNode)target;
                            compositeAssertionTreeNode.receive(node, 0);
                        } else {
                            target.receive(node);
                        }
                        e.dropComplete(true);
                    } else if ((target instanceof CompositeAssertionTreeNode && target != root)) {
                        CompositeAssertionTreeNode compositeAssertionTreeNode = (CompositeAssertionTreeNode)target;
                        compositeAssertionTreeNode.receive(node, 0);
                    } else {
                        target.receive(node);
                    }
                    e.dropComplete(true);
                } else {
                    e.rejectDrop();
                }
                return;
            } catch (UnsupportedFlavorException e1) {
                log.log(Level.WARNING, "error in drop ", e1);
            } catch (IOException e1) {
                log.log(Level.WARNING, "error in drop ", e1);
            }
            e.rejectDrop();
        }

        private void dropTreePath(DropTargetDropEvent e) {
            timerHover.stop();    // Prevent hover timer from doing an unwanted expandPath or collapsePath

            if (!isTreePathDropAcceptable(e)) {
                e.rejectDrop();
                return;
            }

            e.acceptDrop(e.getDropAction());

            Transferable transferable = e.getTransferable();
            DataFlavor[] flavors = transferable.getTransferDataFlavors();
            boolean dropComplete = true;
            for (int i = 0; i < flavors.length; i++) {
                DataFlavor flavor = flavors[i];
                if (TransferableTreePath.TREEPATH_FLAVOR.equals(flavor)) {
                    try {
                        Point pt = e.getLocation();

                        TreePath pathTarget = getClosestPathForLocation(pt.x, pt.y);
                        int row = getRowForLocation(pt.x, pt.y);
                        if (row == -1) {
                            pathTarget = new TreePath(getModel().getRoot());
                        }
                        boolean dropAsFirstContainerChild = false;

                        Insets insets = topBorder.getBorderInsets(PolicyTree.this);
                        if (insets.top >= pt.y) {
                            dropAsFirstContainerChild = true;
                        }

                        TreePath pathSource = (TreePath)transferable.getTransferData(flavor);
                        log.fine("DROPPING: " + pathSource.getLastPathComponent());
                        PolicyTreeModel model = (PolicyTreeModel)getModel();

                        final AssertionTreeNode an = (AssertionTreeNode)pathSource.getLastPathComponent();
                        Assertion a = (Assertion)an.asAssertion().clone();
                        final AssertionTreeNode assertionTreeNodeCopy = AssertionTreeNodeFactory.asTreeNode(a);

                        DefaultMutableTreeNode targetTreeNode =
                          ((DefaultMutableTreeNode)pathTarget.getLastPathComponent());

                        if (targetTreeNode.getAllowsChildren()) {
                            int targetIndex = 0;
                            if (!dropAsFirstContainerChild && targetTreeNode == model.getRoot()) {
                                targetIndex = targetTreeNode.getChildCount();
                            }
                            if (e.getDropAction() == DnDConstants.ACTION_MOVE)
                                model.moveNodeInto(assertionTreeNodeCopy, targetTreeNode, targetIndex);
                            else
                                model.insertNodeInto(assertionTreeNodeCopy, targetTreeNode, targetIndex);
                        } else {
                            final DefaultMutableTreeNode parent = (DefaultMutableTreeNode)targetTreeNode.getParent();

                            int index = parent.getIndex(targetTreeNode);
                            if (index != -1) {
                                if (e.getDropAction() == DnDConstants.ACTION_MOVE)
                                    model.moveNodeInto(assertionTreeNodeCopy, parent, index + 1);
                                else
                                    model.insertNodeInto(assertionTreeNodeCopy, parent, index + 1);
                            }
                        }

                        //setSelectionPath(new TreePath(assertionTreeNodeCopy.getPath()));	// Mark this as the selected path in the tree
                        break; // No need to check remaining flavors
                    } catch (UnsupportedFlavorException ufe) {
                        log.log(Level.WARNING, "Internal error", ufe);
                        dropComplete = false;
                    } catch (IOException ioe) {
                        log.log(Level.WARNING, "Internal error", ioe);
                        dropComplete = false;
                    }
                }
            }
            repaint(raGhost.getBounds());
            e.dropComplete(dropComplete);
        }


        // Helpers...
          public boolean isDragAcceptable(DropTargetDragEvent e) {
            if (!hasWriteAccess()) return false;

            // Only accept COPY or MOVE gestures (ie LINK is not supported)
            if ((e.getDropAction() & DnDConstants.ACTION_COPY_OR_MOVE) == 0)
                return false;

            // Only accept this particular flavor
            if (e.isDataFlavorSupported(TransferableTreePath.TREEPATH_FLAVOR)) {
                // prohibit dropping onto the drag source...
                Point pt = e.getLocation();
                TreePath path = getClosestPathForLocation(pt.x, pt.y);
                int row = getRowForLocation(pt.x, pt.y);
                if (row == -1) {
                    path = new TreePath(getModel().getRoot());
                }

                if (path.equals(pathSource)) {
                    log.fine("REJECTING DRAG: " + pathSource.getLastPathComponent());
                    return false;
                }
                return true;
            } else if (e.isDataFlavorSupported(PolicyTransferable.ASSERTION_DATAFLAVOR)) {
                return true;
            }
            log.log(Level.INFO, "not supported dataflavor " + e.getCurrentDataFlavors());
            return false;
        }

        public boolean isTreePathDropAcceptable(DropTargetDropEvent e) {
            // Only accept COPY or MOVE gestures (ie LINK is not supported)
            if ((e.getDropAction() & DnDConstants.ACTION_COPY_OR_MOVE) == 0)
                return false;

            // Only accept this particular flavor
            if (!e.isDataFlavorSupported(TransferableTreePath.TREEPATH_FLAVOR)) {
                log.log(Level.INFO, "not supported dataflavor " + e.getCurrentDataFlavors());
                return false;
            }

            // Do this if you want to prohibit dropping onto the drag source...
//            Point pt = e.getLocation();
//            TreePath path = getClosestPathForLocation(pt.x, pt.y);
//            if (path == null) {
//                path = new TreePath(getModel().getRoot());
//            }
//            if (path.equals(pathSource))
//                return false;

            return true;
        }
    }


// Autoscroll Interface...
// The following code was borrowed from the book:
//		Java Swing
//		By Robert Eckstein, Marc Loy & Dave Wood
//		Paperback - 1221 pages 1 Ed edition (September 1998)
//		O'Reilly & Associates; ISBN: 156592455X
//
// The relevant chapter of which can be found at:
//		http://www.oreilly.com/catalog/jswing/chapter/dnd.beta.pdf

    private static final int AUTOSCROLL_MARGIN = 12;

// Ok, we've been told to scroll because the mouse cursor is in our
// scroll zone.

    public void autoscroll(Point pt) {
        // Figure out which row we're on.
        int nRow = getRowForLocation(pt.x, pt.y);

        // If we are not on a row then ignore this autoscroll request
        if (nRow < 0)
            return;

        Rectangle raOuter = getBounds();
// Now decide if the row is at the top of the screen or at the
// bottom. We do this to make the previous row (or the next
// row) visible as appropriate. If we're at the absolute top or
// bottom, just return the first or last row respectively.

        nRow = (pt.y + raOuter.y <= AUTOSCROLL_MARGIN)            // Is row at top of screen?
               ?
               (nRow <= 0 ? 0 : nRow - 1)                        // Yes, scroll up one row
               :
               (nRow < getRowCount() - 1 ? nRow + 1 : nRow);    // No, scroll down one row

        scrollRowToVisible(nRow);
    }

// Calculate the insets for the *JTREE*, not the viewport
// the tree is in. This makes it a bit messy.

    public Insets getAutoscrollInsets() {
        Rectangle raOuter = getBounds();
        Rectangle raInner = getParent().getBounds();
        return new Insets(raInner.y - raOuter.y + AUTOSCROLL_MARGIN, raInner.x - raOuter.x + AUTOSCROLL_MARGIN,
                          raOuter.height - raInner.height - raInner.y + raOuter.y + AUTOSCROLL_MARGIN,
                          raOuter.width - raInner.width - raInner.x + raOuter.x + AUTOSCROLL_MARGIN);
    }

// TreeModelListener interface implemntations

    public void treeNodesChanged(TreeModelEvent e) {
        //   log.fine("treeNodesChanged");
        //   sayWhat(e);
// We dont need to reset the selection path, since it has not moved
    }

    public void treeNodesInserted(TreeModelEvent e) {
        //sayWhat(e);
        int nChildIndex = e.getChildIndices()[0];
        TreePath pathParent = e.getTreePath();
        final TreePath childPath = getChildPath(pathParent, nChildIndex);
        Runnable doSelect = new Runnable() {
            public void run() {
                setSelectionPath(childPath);
                PolicyTree.this.requestFocusInWindow();
            }
        };

        AssertionTreeNode parent =
          (AssertionTreeNode)pathParent.getLastPathComponent();
        CompositeAssertion ca = (CompositeAssertion)parent.asAssertion();

        java.util.List newChildren = new ArrayList();
        Enumeration en = parent.children();
        while (en.hasMoreElements()) {
            newChildren.add(((AssertionTreeNode)en.nextElement()).asAssertion());
        }
        SwingUtilities.invokeLater(doSelect);

        log.finer("set children " + newChildren);
        ca.setChildren(newChildren);
        log.finer("children assertions = " + ca.getChildren().size());
        log.finer("nodes          tree = " + parent.getChildCount());
    }

    public void treeNodesRemoved(TreeModelEvent e) {
        java.util.List removed = new ArrayList();
        Object[] objects = e.getChildren();
        int index = 0;
        int[] indices = e.getChildIndices();
        for (int i = 0; i < objects.length; i++) {
            AssertionTreeNode an = (AssertionTreeNode)objects[i];
            removed.add(an.asAssertion());
            index = indices[i];
        }
        final int lastIndex = index;

        final AssertionTreeNode parent =
          (AssertionTreeNode)e.getTreePath().getLastPathComponent();
        CompositeAssertion ca = (CompositeAssertion)parent.asAssertion();
        java.util.List children = ca.getChildren();
        java.util.List<Assertion> remove = new ArrayList<Assertion>();
        for (Iterator iterator = ca.getChildren().iterator(); iterator.hasNext();) {
            Assertion a = (Assertion)iterator.next();
            // fla bugfix 2531, this catches all assertions using equals instead of the one assertion targeted
            // if (removed.contains(a)) {
            //   remove.add(a);
            // }
            for (Object aRemoved : removed) {
                Assertion toRemove = (Assertion) aRemoved;
                if (toRemove == a) {
                    remove.add(a);
                }
            }
        }
        log.finer("removing " + remove);
        // fla bugfix 2531, if you pass one element to removeAll and children has that element more
        // than once, all instances will be removed
        // children.removeAll(remove);

        for (Assertion toRemove : remove) {
            for (int i = 0 ; i < children.size(); i++) {
                Object o = children.get(i);
                if (o == toRemove) {
                    children.remove(i);
                    break;
                }
            }
        }
        log.finer("children assertions = " + ca.getChildren().size());
        log.finer("nodes          tree = " + parent.getChildCount());
    }

    public void treeStructureChanged(TreeModelEvent e) {
        //log.fine("treeStructureChanged ");
        //sayWhat(e);
    }

    public void refresh() {
        if (canRefresh()) {
            new EditServicePolicyAction(policyEditorPanel.getServiceNode(), true).invoke();
        }
    }

    public boolean canRefresh() {
        return policyEditorPanel != null && policyEditorPanel.getServiceNode() != null;
    }

    private TreeSelectionModel getTreeSelectionModel() {
        return new DefaultTreeSelectionModel(){
            public void addSelectionPath(TreePath path) {
                TreePath currentPath = getSelectionPath();
                if (isUserOrGroupPath(currentPath) &&
                    isUserOrGroupPath(path) &&
                    sameFolder(currentPath, path)) {
                    super.addSelectionPaths(asArray(path));
                } else {
                    super.setSelectionPaths(asArray(path));
                }
            }
            public void addSelectionPaths(TreePath[] paths) {
                TreePath currentPath = getSelectionPath();
                boolean canAddPaths = isUserOrGroupPath(currentPath);
                if (canAddPaths && paths != null) {
                    for (int p=0; p<paths.length; p++) {
                        TreePath path = paths[p];
                        if (!isUserOrGroupPath(path) ||
                            !sameFolder(currentPath, path)) {
                            canAddPaths = false;
                            break;
                        }
                    }
                }
                if (canAddPaths || paths == null) {
                    super.addSelectionPaths(paths);
                }
                else {
                    super.setSelectionPaths(asArray(paths[paths.length-1]));
                }
            }
            public void setSelectionPaths(TreePath[] paths) {
                TreePath currentPath = getSelectionPath();
                boolean canSetPaths = currentPath==null || isUserOrGroupPath(currentPath);
                if (canSetPaths && paths != null) {
                    for (int p=0; p<paths.length; p++) {
                        TreePath path = paths[p];
                        if (!isUserOrGroupPath(path) ||
                            !(sameFolder(currentPath, path) || currentPath==null)) {
                            canSetPaths = false;
                            break;
                        }
                    }
                }
                if (canSetPaths || paths == null) {
                    super.setSelectionPaths(paths);
                }
                else {
                    super.setSelectionPaths(asArray(paths[paths.length-1]));
                }
            }
            private TreePath[] asArray(TreePath path) {
                TreePath[] paths = null;

                if (path != null) {
                    paths = new TreePath[]{path};
                }

                return paths;
            }
            private boolean isUserOrGroupPath(TreePath path) {
                boolean isUserOrGroup = false;

                if (path != null) {
                    if (path.getLastPathComponent() instanceof SpecificUserAssertionTreeNode ||
                        path.getLastPathComponent() instanceof MemberOfGroupAssertionTreeNode) {
                        isUserOrGroup = true;
                    }
                }

                return isUserOrGroup;
            }
            private boolean sameFolder(TreePath path1, TreePath path2) {
                boolean same = false;

                if (path1 != null && path2 != null) {
                    TreePath parent1 = path1.getParentPath();
                    TreePath parent2 = path2.getParentPath();
                    if (path1.getPathCount() == path2.getPathCount() &&
                        parent1 != null && parent2 != null &&
                        parent1.equals(parent2)) {
                        same = true;
                    }
                }

                return same;
            }
        };
    }

    private class PolicyTreeTransferHandler extends TreeNodeHidingTransferHandler {

        protected Transferable createTransferable(JComponent c) {
            PolicyTree policyTree = c instanceof PolicyTree ? (PolicyTree)c : PolicyTree.this;
            return policyTree.createTransferable(policyTree.getSelectionPath());
        }

        public boolean importData(JComponent comp, Transferable t) {
            PolicyTree policyTree = comp instanceof PolicyTree ? (PolicyTree)comp : PolicyTree.this;

            String maybePolicyXml = null;
            if (t instanceof PolicyTransferable) {
                PolicyTransferable policyTransferable = (PolicyTransferable)t;
                maybePolicyXml = policyTransferable.getPolicyXml();
            } else if (t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                try {
                    Object dat = t.getTransferData(DataFlavor.stringFlavor);
                    if (dat instanceof String) {
                        maybePolicyXml = (String)dat;
                    }
                } catch (UnsupportedFlavorException e) {
                    log.log(Level.FINE, "Paste rejected: " + ExceptionUtils.getMessage(e), e);
                    return false;
                } catch (IOException e) {
                    log.log(Level.FINE, "Paste rejected: " + ExceptionUtils.getMessage(e), e);
                    return false;
                }
            }

            if (maybePolicyXml == null) {
                log.fine("Paste of unrecognized transferable: " + t.getClass().getName());
                return false;
            }

            try {

                Assertion ass = WspReader.parsePermissively(maybePolicyXml);
                if (ass == null) {
                    log.fine("Paste of null policy; ignoring");
                    return false;
                }

                // Now we have an assertion tree to import into this location in the policy tree.
                return policyTree.importAssertion(ass);

            } catch (IOException e) {
                log.log(Level.FINE, "Paste rejected: " + ExceptionUtils.getMessage(e), e);
                return false;
            }
        }

        protected void exportDone(JComponent source, Transferable data, int action) {
            if (action == TransferHandler.MOVE) {
                PolicyTree policyTree = source instanceof PolicyTree ? (PolicyTree)source : PolicyTree.this;
                TreePath path = policyTree.getSelectionPath();
                if (path != null) {
                    PolicyTreeModel model = (PolicyTreeModel)policyTree.getModel();
                    model.removeNodeFromParent((MutableTreeNode)path.getLastPathComponent());
                }
            }
        }

        public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
            for (DataFlavor flav : transferFlavors) {
                if (PolicyTransferable.ASSERTION_DATAFLAVOR.equals(flav) || flav != null && DataFlavor.stringFlavor.equals(flav))
                    return true;
            }
            return false;
        }

        public int getSourceActions(JComponent c) {
            return COPY_OR_MOVE;
        }
    }

}
