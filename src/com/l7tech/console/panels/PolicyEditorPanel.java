package com.l7tech.console.panels;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.action.*;
import com.l7tech.console.event.ContainerVetoException;
import com.l7tech.console.event.VetoableContainerListener;
import com.l7tech.console.tree.*;
import com.l7tech.console.tree.policy.*;
import com.l7tech.console.util.ComponentRegistry;
import com.l7tech.console.util.PopUpMouseListener;
import com.l7tech.console.util.Registry;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.PolicyValidator;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.service.PublishedService;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.text.EditorKit;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ContainerEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.StringReader;
import java.net.URI;
import java.rmi.RemoteException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * The class represents the policy editor
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class PolicyEditorPanel extends JPanel implements VetoableContainerListener {
    static Logger log = Logger.getLogger(PolicyEditorPanel.class.getName());
    private PublishedService service;
    private JTextPane messagesTextPane;
    private AssertionTreeNode rootAssertion;
    private PolicyTree policyTree;
    private PolicyEditToolBar policyEditorToolbar;
    private JSplitPane splitPane;
    private final ComponentRegistry componentRegistry =
      Registry.getDefault().getComponentRegistry();
    private JScrollPane policyTreePane;
    private ServiceNode serviceNode;

    public PolicyEditorPanel(ServiceNode sn) throws FindException, RemoteException {
        layoutComponents();
        renderService(sn);
        this.serviceNode = sn;
        setEditorListeners();
    }

    /**
     * Sets the name of the component to the specified string. The
     * method is overriden to support/fire property events
     * @param name  the string that is to be this
     *		 component's name
     * @see #getName
     */
    public void setName(String name) {
        String oldName = getName();
        super.setName(name);
        this.firePropertyChange("name", oldName, name);
    }


    private void layoutComponents() {
        setLayout(new BorderLayout());
        add(getToolBar(), BorderLayout.NORTH);
        add(getSplitPane(), BorderLayout.CENTER);
    }

    private JSplitPane getSplitPane() {
        if (splitPane != null) return splitPane;
        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.add(getPolicyTreePane(), "top");
        splitPane.add(getMessagePane(), "bottom");
        splitPane.setDividerSize(2);
        return splitPane;
    }

    private JComponent getPolicyTreePane() {
        if (policyTreePane != null) return policyTreePane;
        // todo: hack, rework policy tree do it is instantiated
        // here.
        componentRegistry.unregisterComponent(PolicyTree.NAME);
        policyTree = (PolicyTree)componentRegistry.getPolicyTree();
        policyTree.setPolicyEditor(this);
        policyTree.setRootVisible(false);
        policyTree.setShowsRootHandles(true);
        policyTree.setRowHeight((int)(policyTree.getRowHeight()*1.3));
        policyTreePane = new JScrollPane(policyTree);
        return policyTreePane;
    }

    /**
     * Render the service and the policy into the editor
     *
     * @param sn the service node
     * @throws FindException
     */
    private void renderService(ServiceNode sn) throws FindException, RemoteException {
        this.service = sn.getPublishedService();
        setName(service.getName());
        getSplitPane().setName(service.getName());

        PolicyTreeModel model = PolicyTreeModel.make(service);
        rootAssertion = (AssertionTreeNode)model.getRoot();
        rootAssertion.addCookie(new AbstractTreeNode.NodeCookie(sn));
        TreeNode root = (TreeNode)model.getRoot();
        FilteredTreeModel filteredTreeModel = new FilteredTreeModel(root);
        policyTree.setModel(filteredTreeModel);
        componentRegistry.
          getMainWindow().
          getPolicyToolBar().registerPolicyTree(policyTree);
        filteredTreeModel.addTreeModelListener(policyTreeModellistener);

        final TreePath path = new TreePath(((DefaultMutableTreeNode)root).getPath());

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                policyTree.setSelectionPath(path);
            }
        });
    }

    /**
     * set various listeners that this panel uses
     */
    private void setEditorListeners() {
        serviceNode.addPropertyChangeListener(servicePropertyChangeListener);
        JTree tree = (JTree)componentRegistry.getComponent(ServicesTree.NAME);
        if (tree == null) {
            throw new IllegalStateException("Internal error - (could not get services tree component)");
        }
        tree.getModel().addTreeModelListener(servicesTreeModelListener);
    }


    private JComponent getMessagePane() {
        messagesTextPane = new JTextPane(new HTMLDocument());
        messagesTextPane.setEditorKit(new HTMLEditorKit());
        // messagesTextPane.setText("");
        messagesTextPane.addHyperlinkListener(hlinkListener);
        messagesTextPane.setEditable(false);
        JTabbedPane tabbedPane = new JTabbedPane();
        JScrollPane scrollPane = new JScrollPane(messagesTextPane);
        tabbedPane.addTab("Messages", scrollPane);
        messagesTextPane.addMouseListener(new PopUpMouseListener() {
            protected void popUpMenuHandler(MouseEvent mouseEvent) {
                JPopupMenu menu = new JPopupMenu();
                menu.add(new ClearMessageAreaAction());
                if (menu != null) {
                    menu.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
                }
            }

        });
        return tabbedPane;
    }

    private class ClearMessageAreaAction extends AbstractAction {
        public ClearMessageAreaAction() {
            putValue(Action.NAME, "Clear All");
            putValue(Action.SHORT_DESCRIPTION, "Clear message area");
        }

        /**
         * Invoked when an action occurs.
         */
        public void actionPerformed(ActionEvent e) {
            overWriteMessageArea("");
        }
    }


    /**
     * Return the ToolBarForTable instance for a given node or null.
     * @return ToolBarForTable
     */
    private PolicyEditToolBar getToolBar() {
        if (policyEditorToolbar != null) return policyEditorToolbar;
        policyEditorToolbar = new PolicyEditToolBar();
        policyEditorToolbar.setFloatable(false);
        return policyEditorToolbar;
    }

    /**
     */
    final class PolicyEditToolBar extends JToolBar {
        JButton buttonSave;
        JButton buttonSaveTemplate;
        JButton buttonValidate;

        public PolicyEditToolBar() {
            super();
            this.putClientProperty("JToolBar.isRollover", Boolean.TRUE);
            initComponents();
        }

        private void initComponents() {
            buttonSave = new JButton(new SavePolicyAction() {
                /** Actually perform the action. */
                public void performAction() {
                    this.node = rootAssertion;
                    super.performAction();
                }
            });

            this.add(buttonSave);
            final BaseAction ba = (BaseAction)buttonSave.getAction();
            ba.setEnabled(false);
            ba.addActionListener(new ActionListener() {
                /**
                 * Invoked when an action occurs.
                 */
                public void actionPerformed(ActionEvent e) {
                    appendToMessageArea("<i>Policy saved.</i>");
                    ba.setEnabled(false);
                    buttonSave.setEnabled(false);
                }
            });
            buttonSaveTemplate = new JButton(new SavePolicyTemplateAction() {
                /** Actually perform the action. */
                public void performAction() {
                    this.node = rootAssertion;
                    super.performAction();
                }
            });
            this.add(buttonSaveTemplate);

            buttonValidate = new JButton(new ValidatePolicyAction() {
                public void performAction() {
                    PolicyValidatorResult result
                      = PolicyValidator.getDefault().
                      validate(rootAssertion.asAssertion());
                    displayPolicyValidateResult(result);

                }

            });
            this.add(buttonValidate);

            Utilities.
              equalizeComponentSizes(
                new JComponent[]{
                    buttonSave, buttonValidate, buttonSaveTemplate
                });
        }
    }

    /**
     * package pruivate method that displays the policy validation
     * result
     * @param r the policy validation result
     */
    void displayPolicyValidateResult(PolicyValidatorResult r) {
        overWriteMessageArea("");
        for (Iterator iterator = r.getErrors().iterator();
             iterator.hasNext();) {
            PolicyValidatorResult.Error pe =
              (PolicyValidatorResult.Error)iterator.next();
            appendToMessageArea(getValidateMessageIntro(pe)
              +
              "</a>" + " Error :" + pe.getMessage() + ""
            );
        }
        for (Iterator iterator = r.getWarnings().iterator();
             iterator.hasNext();) {
            PolicyValidatorResult.Warning pe =
              (PolicyValidatorResult.Warning)iterator.next();
            appendToMessageArea(getValidateMessageIntro(pe)
              +
              " Warning :" + pe.getMessage() + ""
            );
        }
        if (r.getErrors().isEmpty() && r.getWarnings().isEmpty()) {
            appendToMessageArea("<i>Policy validated ok.</i>");
        }
    }

    public void updateActions(Action[] actions) {
        for (int i = 0; i < actions.length; i++) {
            Action action = actions[i];
            if (action instanceof SavePolicyAction) {
                actions[i] = policyEditorToolbar.buttonSave.getAction();
                actions[i].setEnabled(policyEditorToolbar.buttonSave.isEnabled());
            } else if (action instanceof ValidatePolicyAction) {
                actions[i] = policyEditorToolbar.buttonValidate.getAction();
                actions[i].setEnabled(policyEditorToolbar.buttonValidate.isEnabled());
            }
        }
    }

    private void appendToMessageArea(String s) {
        try {
            int pos = messagesTextPane.getDocument().getLength();
            //if (pos > 0) s = "\n" + s;
            StringReader sr = new StringReader(s);
            EditorKit editorKit = messagesTextPane.getEditorKit();
            editorKit.read(sr, messagesTextPane.getDocument(), pos);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void overWriteMessageArea(String s) {
        messagesTextPane.setText(s);
    }

    /**
     * get the validator result message intro
     *
     * @param pe
     * @return the string as a message
     */
    private String getValidateMessageIntro(PolicyValidatorResult.Message pe) {
        String msg = null;
        if (pe.getAssertion() != null) {
            msg = "Assertion : " +
              "<a href=\"file://assertion#" +
              pe.getAssertion().hashCode() + "\">" +
              Descriptions.getDescription(pe.getAssertion()).getShortDescription() + "</a>";
        } else {
            msg = ""; // supplied message (non single assertion related)
        }
        return msg;
    }

    private static class IdentityNodeFilter implements NodeFilter {
        /**
         * @param node  the <code>TreeNode</code> to examine
         * @return  true if filter accepts the node, false otherwise
         */
        public boolean accept(TreeNode node) {
            if (node instanceof SpecificUserAssertionTreeNode ||
              node instanceof MemberOfGroupAssertionTreeNode)
                return false;

            if (node instanceof CompositeAssertionTreeNode) {
                if (((CompositeAssertionTreeNode)node).getChildCount(this) == 0)
                    return false;
            }

            TreeNode[] path = ((DefaultMutableTreeNode)node).getPath();
            IdentityPolicyTreeNode in = (IdentityPolicyTreeNode)path[1];
            AssertionTreeNode an = (AssertionTreeNode)node;
            IdentityPath ip = in.getIdentityPath();
            Set paths = ip.getPaths();
            for (Iterator iterator = paths.iterator(); iterator.hasNext();) {
                Assertion[] apath = (Assertion[])iterator.next();
                for (int i = apath.length - 1; i >= 0; i--) {
                    Assertion assertion = apath[i];
                    if (assertion.equals(an.asAssertion())) return true;
                }
            }
            return false;
        }

    }

    // listener for policy tree changes
    TreeModelListener policyTreeModellistener = new TreeModelListener() {
        public void treeNodesChanged(TreeModelEvent e) {
            policyEditorToolbar.buttonSave.setEnabled(true);
        }

        public void treeNodesInserted(final TreeModelEvent e) {
            policyEditorToolbar.buttonSave.getAction().setEnabled(true);
        }

        public void treeNodesRemoved(TreeModelEvent e) {
            policyEditorToolbar.buttonSave.getAction().setEnabled(true);

        }

        public void treeStructureChanged(TreeModelEvent e) {
            policyEditorToolbar.buttonSave.getAction().setEnabled(true);
        }
    };

// listener for policy tree changes
    TreeModelListener servicesTreeModelListener = new TreeModelListener() {
        public void treeNodesChanged(TreeModelEvent e) {
        }

        public void treeNodesInserted(TreeModelEvent e) {
        }

        public void treeNodesRemoved(TreeModelEvent e) {
            Object[] children = e.getChildren();
            for (int i = 0; i < children.length; i++) {
                Object child = children[i];
                if (child == serviceNode) {
                    log.fine("Service node deleted, disabling save controls");
                    policyEditorToolbar.buttonSave.setEnabled(false);
                    policyEditorToolbar.buttonSave.getAction().setEnabled(false);
                }
            }
        }

        public void treeStructureChanged(TreeModelEvent e) {
        }

    };


    private final PropertyChangeListener
      servicePropertyChangeListener = new PropertyChangeListener() {
          /**
           * This method gets called when a bound property is changed.
           * @param evt A PropertyChangeEvent object describing the event source
           *   	and the property that has changed.
           */
          public void propertyChange(PropertyChangeEvent evt) {
              log.info(evt.getPropertyName() + "changed");
              try {
                  if ("service.name".equals(evt.getPropertyName())) {
                      renderService(serviceNode);
                  } else if ("policy".equals(evt.getPropertyName())) {
                      renderService(serviceNode);
                      policyEditorToolbar.buttonSave.setEnabled(true);
                      policyEditorToolbar.buttonSave.getAction().setEnabled(true);
                  }
              } catch (FindException e) {
                  log.log(Level.WARNING, "Error finding service "+serviceNode.getEntityHeader().getOid());
              } catch (RemoteException e) {
                  log.log(Level.WARNING, "Remote error with service "+serviceNode.getEntityHeader().getOid());
              }
          }
      };

    private final HyperlinkListener
      hlinkListener = new HyperlinkListener() {
          /**
           * Called when a hypertext link is updated.
           *
           * @param e the event responsible for the update
           */
          public void hyperlinkUpdate(HyperlinkEvent e) {
              if (HyperlinkEvent.EventType.ACTIVATED != e.getEventType())
                  return;
              URI uri = URI.create(e.getURL().toString());
              String f = uri.getFragment();
              if (f == null) return;
              try {
                  int hashcode = Integer.parseInt(f);
                  for (Enumeration en = rootAssertion.preorderEnumeration();
                       en.hasMoreElements();) {
                      AssertionTreeNode an = (AssertionTreeNode)en.nextElement();
                      if (an.asAssertion().hashCode() == hashcode) {
                          TreePath p = new TreePath(an.getPath());
                          if (!policyTree.hasBeenExpanded(p) || !policyTree.isExpanded(p)) {
                              policyTree.expandPath(p);
                          }
                          policyTree.setSelectionPath(p);
                      }
                  }
              } catch (NumberFormatException ex) {
                  ex.printStackTrace();
              }
          }
      };

    /**
     * Invoked when a component has been added to the container.
     */
    public void componentAdded(ContainerEvent e) {
    }

    /**
     * Invoked when a component has been removed from the container.
     */
    public void componentRemoved(ContainerEvent e) {
        log.fine("Resetting the policy editor panel");
        serviceNode.removePropertyChangeListener(servicePropertyChangeListener);
        serviceNode = null;
        policyTree.setPolicyEditor(null);
        policyTree.setModel(null);
    }

    /**
     * Invoked when a component has to be added to the container.
     * @param e     the container event
     * @throws com.l7tech.console.event.ContainerVetoException if the recipient wishes to stop
     *         (not perform) the action.
     */
    public void componentWillAdd(ContainerEvent e)
      throws ContainerVetoException {
    }

    /**
     * Invoked when a component has to be removed from to the container.
     * @param e     the container event
     * @throws ContainerVetoException if the recipient wishes to stop
     *         (not perform) the action.
     */
    public void componentWillRemove(ContainerEvent e)
      throws ContainerVetoException {
        if (e.getChild() == this) {
            if (policyEditorToolbar.buttonSave.isEnabled()) {
                int answer = (JOptionPane.showConfirmDialog(
                  ComponentRegistry.getInstance().getMainWindow(),
                  "<html><center><b>Do you want to save changes to service policy " +
                  "for<br> '" + serviceNode.getName() + "' ?</b></center></html>",
                  "Save Service policy",
                  JOptionPane.YES_NO_CANCEL_OPTION));
                if (answer == JOptionPane.YES_OPTION) {
                    policyEditorToolbar.
                      buttonSave.getAction().actionPerformed(null);
                } else if ((answer == JOptionPane.CANCEL_OPTION)) {
                    throw new ContainerVetoException(e, "User aborted");
                }
            }
        }
    }

}
