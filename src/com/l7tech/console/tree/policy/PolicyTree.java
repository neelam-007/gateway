package com.l7tech.console.tree.policy;

import com.l7tech.console.action.ActionManager;
import com.l7tech.console.action.DeleteAssertionAction;
import com.l7tech.console.action.EditServicePolicyAction;
import com.l7tech.console.poleditor.PolicyEditorPanel;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.AssertionsTree;
import com.l7tech.console.tree.TransferableTreePath;
import com.l7tech.console.util.ArrowImage;
import com.l7tech.console.util.PopUpMouseListener;
import com.l7tech.console.util.Refreshable;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.common.gui.util.Utilities;

import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
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
    private TreePath pathSource;				// The path being dragged
    private BufferedImage imgGhost;					// The 'drag image'
    private Point ptOffset = new Point();	// Where, in the drag image, the mouse was clicked


    /**
     * Create the new policy tree with the policy model.
     * 
     * @param newModel 
     */
    public PolicyTree(PolicyTreeModel newModel) {
        super(newModel);
        initialize();
        getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
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
            AbstractTreeNode node =
              (AbstractTreeNode)path.getLastPathComponent();
            if (node == null) return;
            int keyCode = e.getKeyCode();
            if (keyCode == KeyEvent.VK_DELETE) {
                if (!node.canDelete()) return;
                if (node instanceof AssertionTreeNode)
                    new DeleteAssertionAction((AssertionTreeNode)node).actionPerformed(null);
            } else if (keyCode == KeyEvent.VK_ENTER) {
                // default properties
            }
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

            Action a = node.getPreferredAction();
            if (a != null) {
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
     * The menu is constructed from the set of actions returned
     * 
     * @return the popup menu
     */
    private JPopupMenu getPopupMenu(Action[] actions) {
        if (actions == null || actions.length == 0)
            return null;
        JPopupMenu pm = new JPopupMenu();
        for (int i = 0; i < actions.length; i++) {
            pm.add(actions[i]);
        }
        Utilities.removeToolTipsFromMenuItems(pm);
        return pm;
    }


    public void dragEnter(DragSourceDragEvent dsde) {
        log.entering(this.getClass().getName(), "dragEnter");
    }

    public void dragOver(DragSourceDragEvent dsde) {
        log.entering(this.getClass().getName(), "dragOver");
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
        if (path == null)
            return;
        if (isRootPath(path))
            return;	// Ignore user trying to drag the root node
        if (isIdentityView())
            return; // Ignore if in identity view
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
            false											// hasFocus		(dont want a focus rectangle)
          );
        lbl.setSize((int)raPath.getWidth(), (int)raPath.getHeight()); // <-- The layout manager would normally do this

        // Get a buffered image of the selection for dragging a ghost image
        imgGhost = new BufferedImage((int)raPath.getWidth(), (int)raPath.getHeight(), BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D g2 = imgGhost.createGraphics();

        // Ask the cell renderer to paint itself into the BufferedImage
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC, 0.5f));		// Make the image ghostlike
        lbl.paint(g2);

        // Now paint a gradient UNDER the ghosted JLabel text (but not under the icon if any)
        Icon icon = lbl.getIcon();
        int nStartOfText = (icon == null) ? 0 : icon.getIconWidth() + lbl.getIconTextGap();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.DST_OVER, 0.5f));	// Make the gradient ghostlike
        g2.setPaint(new GradientPaint(nStartOfText, 0, SystemColor.controlShadow,
          getWidth(), 0, new Color(255, 255, 255, 0)));
        g2.fillRect(nStartOfText, 0, getWidth(), imgGhost.getHeight());

        g2.dispose();


        setSelectionPath(path);	// Select this path in the tree

        log.fine("DRAGGING: " + path.getLastPathComponent());

        // Wrap the path being transferred into a Transferable object
        Transferable transferable = new TransferableTreePath(path);

        // Remember the path being dragged (because if it is being moved, we will have to delete it later)
        pathSource = path;	
		
        // We pass our drag image just in case it IS supported by the platform
        dge.startDrag(null, imgGhost, new Point(5, 5), transferable, this);
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
        private int nLeftRight = 0;	// Cumulative left/right mouse movement
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
                    nLeftRight = 0;	// Reset left/right movement trend
                    if (isRootPath(pathLast))
                        return;	// Do nothing if we are hovering over the root node
                    if (isExpanded(pathLast))
                        collapsePath(pathLast);
                    else
                        expandPath(pathLast);
                }
            });
            timerHover.setRepeats(false);	// Set timer to one-shot mode
        }

        // PolicyDropTargetListener interface
        public void dragEnter(DropTargetDragEvent e) {
            if (!isDragAcceptable(e)) {
                e.rejectDrag();
                log.fine("REJECTING DRAG:");
            } else {
                log.fine("ACCEPT DRAG:");
                e.acceptDrag(e.getDropAction());
            }
        }

        public void dragExit(DropTargetEvent e) {
            if (!DragSource.isDragImageSupported()) {
                repaint(raGhost.getBounds());
            }
        }

        public void dragOver(DropTargetDragEvent e) {
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
                } else if (AssertionsTree.ASSERTION_DATAFLAVOR.equals(flavor)) {
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
                paintImmediately(raGhost.getBounds());	// Rub out the last ghost image and cue line
                // And remember where we are about to draw the new ghost image
                raGhost.setRect(pt.x - ptOffset.x, pt.y - ptOffset.y, imgGhost.getWidth(), imgGhost.getHeight());
                g2.drawImage(imgGhost, AffineTransform.getTranslateInstance(raGhost.getX(), raGhost.getY()), null);
            } else	// Just rub out the last cue line
                paintImmediately(raCueLine.getBounds());


            TreePath path = getClosestPathForLocation(pt.x, pt.y);

            int row = getRowForLocation(pt.x, pt.y);
            if (row == -1) {
                path = new TreePath(getModel().getRoot());
            }
            if (!(path == pathLast)) {
                nLeftRight = 0; 	// We've moved up or down, so reset left/right movement trend
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
                } else if (AssertionsTree.ASSERTION_DATAFLAVOR.equals(flavor)) {
                    dropAssertion(e);
                }
            }

        }

        private void dropAssertion(DropTargetDropEvent e) {
            try {
                final Object transferData = e.getTransferable().getTransferData(AssertionsTree.ASSERTION_DATAFLAVOR);
                log.fine("DROPPING: " + transferData);
                AbstractTreeNode node = (AbstractTreeNode)transferData;
                TreePath path = getSelectionPath();

                if (path == null) {
                    path = new TreePath(getModel().getRoot());
                } else {
                    Point location = e.getLocation();
                    int row = getRowForLocation(location.x, location.y);
                    if (row == -1) {
                        path = new TreePath(getModel().getRoot());
                    }
                }
                AssertionTreeNode target = (AssertionTreeNode)path.getLastPathComponent();
                if (target.accept(node)) {
                    e.acceptDrop(e.getDropAction());
                    target.receive(node);
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
            timerHover.stop();	// Prevent hover timer from doing an unwanted expandPath or collapsePath

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
                        TreePath pathSource = (TreePath)transferable.getTransferData(flavor);
                        log.fine("DROPPING: " + pathSource.getLastPathComponent());
                        PolicyTreeModel model = (PolicyTreeModel)getModel();
                        TreePath pathNewChild = null;

                        final AssertionTreeNode an = (AssertionTreeNode)pathSource.getLastPathComponent();
                        Assertion a = (Assertion)an.asAssertion().clone();
                        final AssertionTreeNode assertionTreeNodeCopy = AssertionTreeNodeFactory.asTreeNode(a);

                        DefaultMutableTreeNode targetTreeNode =
                          ((DefaultMutableTreeNode)pathTarget.getLastPathComponent());

                        if (targetTreeNode.getAllowsChildren()) {
                            int targetIndex = 0;
                            if (targetTreeNode == model.getRoot()) {
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

                        if (pathNewChild != null)
                            setSelectionPath(pathNewChild);	// Mark this as the selected path in the tree
                        break; // No need to check remaining flavors
                    } catch (UnsupportedFlavorException ufe) {
                        log.log(Level.WARNING, "Internal error", ufe);
                        dropComplete = false;
                    } catch (IOException ioe) {
                        log.log(Level.WARNING, "Internal error", ioe);
                        dropComplete = false;
                    } catch (CloneNotSupportedException e1) {
                        log.log(Level.SEVERE, "Assertion faile", e1);
                        dropComplete = false;
                    }
                }
            }
            repaint(raGhost.getBounds());
            e.dropComplete(dropComplete);
        }


        // Helpers...
        public boolean isDragAcceptable(DropTargetDragEvent e) {
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
            } else if (e.isDataFlavorSupported(AssertionsTree.ASSERTION_DATAFLAVOR)) {
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

// Ok, we�ve been told to scroll because the mouse cursor is in our
// scroll zone.
    public void autoscroll(Point pt) {
        // Figure out which row we�re on.
        int nRow = getRowForLocation(pt.x, pt.y);
		
// If we are not on a row then ignore this autoscroll request
        if (nRow < 0)
            return;

        Rectangle raOuter = getBounds();
// Now decide if the row is at the top of the screen or at the
// bottom. We do this to make the previous row (or the next
// row) visible as appropriate. If we�re at the absolute top or
// bottom, just return the first or last row respectively.
		
        nRow = (pt.y + raOuter.y <= AUTOSCROLL_MARGIN)			// Is row at top of screen? 
          ?
          (nRow <= 0 ? 0 : nRow - 1)						// Yes, scroll up one row
          :
          (nRow < getRowCount() - 1 ? nRow + 1 : nRow);	// No, scroll down one row

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
        java.util.List remove = new ArrayList();
        for (Iterator iterator = ca.getChildren().iterator(); iterator.hasNext();) {
            Assertion a = (Assertion)iterator.next();
            if (removed.contains(a)) {
                remove.add(a);
            }
        }
        log.finer("removing " + remove);
        children.removeAll(remove);
        log.finer("children assertions = " + ca.getChildren().size());
        log.finer("nodes          tree = " + parent.getChildCount());

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                DefaultMutableTreeNode n = null;
                int currentIndex = 0;
                Enumeration e = parent.children();
                for (; e.hasMoreElements() && lastIndex >= currentIndex;) {
                    n = (DefaultMutableTreeNode)e.nextElement();
                }
                if (n != null) {
                    setSelectionPath(new TreePath(n.getPath()));
                } else {
                    if (parent != parent.getRoot()) {
                        setSelectionPath(new TreePath(parent.getPath()));
                    }
                }
            }
        });
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
}
