package com.l7tech.console.panels;

import com.l7tech.console.action.PolicyIdentityViewAction;
import com.l7tech.console.action.SavePolicyAction;
import com.l7tech.console.action.ValidatePolicyAction;
import com.l7tech.console.action.SavePolicyTemplateAction;
import com.l7tech.console.tree.FilteredTreeModel;
import com.l7tech.console.tree.NodeFilter;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.tree.policy.*;
import com.l7tech.console.util.PopUpMouseListener;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.ComponentRegistry;
import com.l7tech.console.logging.ErrorManager;
import com.l7tech.policy.PolicyValidator;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.RoutingAssertion;
import com.l7tech.service.PublishedService;
import com.l7tech.objectmodel.FindException;
import com.l7tech.common.gui.util.Utilities;

import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.EditorKit;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.DefaultTreeModel;
import javax.wsdl.WSDLException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.StringReader;
import java.util.Iterator;
import java.util.Set;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.net.URI;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.rmi.RemoteException;

/**
 * The class represnts the policy editor
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class PolicyEditorPanel extends JPanel {
    static Logger log = Logger.getLogger(PolicyEditorPanel.class.getName());
    private PublishedService service;
    private JTextPane messagesTextPane;
    private AssertionTreeNode rootAssertion;
    private JTree policyTree;
    private PolicyEditToolBar policyEditorToolbar;
    private JSplitPane splitPane;
    private final ComponentRegistry componentRegistry = Registry.getDefault().getWindowManager();
    private JScrollPane policyTreePane;
    private ServiceNode serviceNode;

    public PolicyEditorPanel(ServiceNode sn) throws FindException, RemoteException {
        layoutComponents();
        renderService(sn);
        addServiceListeners(sn);
        this.serviceNode = sn;
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

    public JSplitPane getSplitPane() {
        if (splitPane != null) return splitPane;

        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.add(getPolicyTreePane(), "top");
        splitPane.add(getMessagePane(), "bottom");
        splitPane.setDividerSize(2);

        return splitPane;
    }

    private JComponent getPolicyTreePane() {
        if (policyTreePane != null) return policyTreePane;

        policyTree = componentRegistry.getPolicyTree();
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

        policyTree.putClientProperty("service.node", sn);
        PolicyTreeModel model = PolicyTreeModel.make(service);
        rootAssertion = (AssertionTreeNode)model.getRoot();

        TreeNode root = (TreeNode)model.getRoot();
        FilteredTreeModel filteredTreeModel = new FilteredTreeModel(root);
        policyTree.setModel(filteredTreeModel);
        filteredTreeModel.addTreeModelListener(treeModellistener);

        final TreePath path = new TreePath(((DefaultMutableTreeNode)root).getPath());

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                policyTree.setSelectionPath(path);
            }
        });
    }

    private void addServiceListeners(ServiceNode sn) {
        sn.addPropertyChangeListener(servicePropertyChangeListener);
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
        JToggleButton identityViewButton;

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
            buttonSave.setEnabled(false);
            buttonSave.addActionListener(new ActionListener() {
                /**
                 * Invoked when an action occurs.
                 */
                public void actionPerformed(ActionEvent e) {
                    appendToMessageArea("<i>Policy saved.</i>");
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

            buttonValidate = new JButton(new ValidatePolicyAction());
            this.add(buttonValidate);
            buttonValidate.addActionListener(
              new ActionListener() {
                  /** Invoked when an action occurs.*/
                  public void actionPerformed(ActionEvent e) {
                      PolicyValidatorResult result
                        = PolicyValidator.getDefault().
                        validate(rootAssertion.asAssertion());
                      displayPolicyValidateResult(result);

                      if (result.getErrors().isEmpty() && result.getWarnings().isEmpty()) {
                          appendToMessageArea("<i>Policy validated ok.</i>");
                      }
                  }
              });

            identityViewButton = new JToggleButton(new PolicyIdentityViewAction());
            identityViewButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    boolean selected = identityViewButton.isSelected();
                    policyTree.getModel().removeTreeModelListener(treeModellistener);
                    if (selected) {
                        PolicyTreeModel model =
                          PolicyTreeModel.identitityModel(rootAssertion.asAssertion());
                        FilteredTreeModel fm = new FilteredTreeModel((TreeNode)model.getRoot());
                        fm.setFilter(new IdentityNodeFilter());
                        policyTree.setModel(fm);
                    } else {
                        PolicyTreeModel model =
                          new PolicyTreeModel(rootAssertion.asAssertion());
                        FilteredTreeModel fm = new FilteredTreeModel((TreeNode)model.getRoot());
                        policyTree.setModel(fm);
                    }
                    policyTree.getModel().addTreeModelListener(treeModellistener);
                }
            });
            this.add(identityViewButton);
            Utilities.
              equalizeComponentSizes(
                new JComponent[]{
                    buttonSave, buttonValidate, identityViewButton
                });
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

    private void displayPolicyValidateResult(PolicyValidatorResult r) {
        for (Iterator iterator = r.getErrors().iterator();
             iterator.hasNext();) {
            PolicyValidatorResult.Error pe =
              (PolicyValidatorResult.Error)iterator.next();
            appendToMessageArea(
              "Assertion : "+
              "<a href=\"file://assertion#" +
              pe.getAssertion().hashCode() + "\">" +
              Descriptions.getDescription(pe.getAssertion()).getShortDescription() +
              "</a>"+ " Error :" + pe.getMessage() + ""
            );
        }
        for (Iterator iterator = r.getWarnings().iterator();
             iterator.hasNext();) {
            PolicyValidatorResult.Warning pe =
              (PolicyValidatorResult.Warning)iterator.next();
            appendToMessageArea(
              "Assertion : "+
              "<a href=\"file://assertion#" +
              pe.getAssertion().hashCode() + "\">" +
              Descriptions.getDescription(pe.getAssertion()).getShortDescription() +
              "</a>"+ " Error :" + pe.getMessage() + ""
            );
        }
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
            IdentityViewTreeNode in = (IdentityViewTreeNode)path[1];
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

    // listen for tree changes
    TreeModelListener treeModellistener = new TreeModelListener() {

        public void treeNodesChanged(TreeModelEvent e) {
            policyEditorToolbar.buttonSave.setEnabled(true);
            TreePath path = e.getTreePath();
            TreeNode parent = (TreeNode)path.getLastPathComponent();
            int[] indices = e.getChildIndices();
            for (int i = 0; i < indices.length; i++) {
                int indice = indices[i];
                TreeNode node = parent.getChildAt(indice);
                log.info("nodes changed received " + node);
            }

        }

        public void treeNodesInserted(final TreeModelEvent e) {
            policyEditorToolbar.buttonSave.setEnabled(true);
            //todo: refactor this out
            SwingUtilities.invokeLater(
              new Runnable() {
                  public void run() {
                      try {
                          TreePath path = e.getTreePath();
                          TreeNode parent = (TreeNode)path.getLastPathComponent();
                          TreeNode lastNode = null;
                          int[] indices = e.getChildIndices();
                          for (int i = 0; i < indices.length; i++) {
                              int indice = indices[i];
                              TreeNode node = parent.getChildAt(indice);
                              lastNode = node;
                              if (node instanceof RoutingAssertionTreeNode) {
                                  RoutingAssertionTreeNode rn = (RoutingAssertionTreeNode)node;
                                  ((RoutingAssertion)rn.asAssertion()).setProtectedServiceUrl(service.parsedWsdl().getServiceURI());
                                  ((DefaultTreeModel)policyTree.getModel()).nodeChanged(node);
                              }
                          }
                          if (!policyTree.isExpanded(path)) {
                              policyTree.expandPath(path);
                          }
                          if (lastNode != null)
                              policyTree.setSelectionPath(path.pathByAddingChild(lastNode));
                      } catch (WSDLException e1) {
                          ErrorManager.getDefault().
                            notify(Level.WARNING, e1,
                              "Error parsing wsdl- service " + service.getName());
                      }
                  }
              });
        }

        public void treeNodesRemoved(TreeModelEvent e) {
            policyEditorToolbar.buttonSave.setEnabled(true);

        }

        public void treeStructureChanged(TreeModelEvent e) {
            policyEditorToolbar.buttonSave.setEnabled(true);
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
              if ("service.name".equals(evt.getPropertyName()) ||
                "policy".equals(evt.getPropertyName())) {
                  try {
                      renderService(serviceNode);
                      policyEditorToolbar.buttonSave.setEnabled(true);
                  } catch (Exception e) {
                      e.printStackTrace();
                  }
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
}
