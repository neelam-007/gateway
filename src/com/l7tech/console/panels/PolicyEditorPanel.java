package com.l7tech.console.panels;

import com.l7tech.console.action.PolicyIdentityViewAction;
import com.l7tech.console.action.SavePolicyAction;
import com.l7tech.console.action.ValidatePolicyAction;
import com.l7tech.console.action.SavePolicyTemplateAction;
import com.l7tech.console.tree.FilteredTreeModel;
import com.l7tech.console.tree.NodeFilter;
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
import java.net.URI;

/**
 * The class represnts the policy editor
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class PolicyEditorPanel extends JPanel {
    private PublishedService service;
    private JTextPane messagesTextPane;
    private AssertionTreeNode rootAssertion;
    private JTree policyTree;
    private PolicyEditToolBar policyEditorToolbar;

    public PolicyEditorPanel(PublishedService svc) {
        this.service = svc;
        layoutComponents();
        setName(svc.getName());
    }

    private void layoutComponents() {
        setLayout(new BorderLayout());
        ComponentRegistry windowManager =
          Registry.getDefault().getWindowManager();

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

        splitPane.add(getPolicyTreePane(windowManager), "top");
        splitPane.add(getMessagePane(), "bottom");
        splitPane.setDividerSize(2);
        splitPane.setName(service.getName());
        add(getToolBar(), BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
    }


    private JComponent getPolicyTreePane(ComponentRegistry windowManager) {
        policyTree = windowManager.getPolicyTree();
        policyTree.putClientProperty("service", service);
        PolicyTreeModel model = PolicyTreeModel.make(service);
        rootAssertion = (AssertionTreeNode)model.getRoot();
        TreeNode root = (TreeNode)model.getRoot();
        FilteredTreeModel filteredTreeModel = new FilteredTreeModel(root);
        policyTree.setModel(filteredTreeModel);
        filteredTreeModel.addTreeModelListener(treeModellistener);
        policyTree.setName(service.getName());

        JScrollPane scrollPane = new JScrollPane(policyTree);
        final TreePath path = new TreePath(((DefaultMutableTreeNode)root).getPath());
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                policyTree.setSelectionPath(path);
            }
        });

        return scrollPane;
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
            buttonSave = new JButton(new SavePolicyAction(rootAssertion));
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
            buttonSaveTemplate = new JButton(new SavePolicyTemplateAction(rootAssertion));
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
               appendToMessageArea("<a href=\"file://assertion#"+pe.getAssertion().hashCode()+"\"> Assertion : " +
              pe.getAssertion().getClass() + " Error :" + pe.getMessage()+"</a><br>");
        }
        for (Iterator iterator = r.getWarnings().iterator();
             iterator.hasNext();) {
            PolicyValidatorResult.Warning pe =
              (PolicyValidatorResult.Warning)iterator.next();
            appendToMessageArea("<a href=\"file://assertion#"+pe.getAssertion().hashCode()+"\"> Assertion : " +
              pe.getAssertion().getClass() + " Warning :" + pe.getMessage()+"</a><br>");
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
                          int[] indices = e.getChildIndices();
                          for (int i = 0; i < indices.length; i++) {
                              int indice = indices[i];
                              TreeNode node = parent.getChildAt(indice);
                              if (node instanceof RoutingAssertionTreeNode) {
                                  RoutingAssertionTreeNode rn = (RoutingAssertionTreeNode)node;
                                  ((RoutingAssertion)rn.asAssertion()).setProtectedServiceUrl(service.parsedWsdl().getServiceURI());
                                  ((DefaultTreeModel)policyTree.getModel()).nodeChanged(node);
                              }
                          }
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
              if (f ==null) return;
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


              } catch(NumberFormatException ex) {
                  ex.printStackTrace();
              }
          }

      };

}
