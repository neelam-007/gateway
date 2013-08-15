package com.l7tech.console.action;

import com.l7tech.console.tree.policy.DefaultAssertionPolicyNode;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.tree.*;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.admin.IdentityAdmin;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.ObjectModelException;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Supporting class for actions.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class Actions {
    static Logger log = Logger.getLogger(Actions.class.getName());

    /**
     * Deletes the given EntityHeaderNode
     *
     * @param bn - the node to be deleted
     * @param config  idp containing the entity to delete
     * @param result  callback that will be invoked with true if the node was deleted
     */
    static void deleteEntity(EntityHeaderNode bn, IdentityProviderConfig config, Functions.UnaryVoid<Boolean> result) {
        if (bn instanceof UserNode) {
            deleteUser((UserNode)bn, config, result);
        } else if (bn instanceof GroupNode) {
            deleteGroup((GroupNode)bn, config, result);
        } else if (bn instanceof IdentityProviderNode) {
            deleteProvider((IdentityProviderNode)bn, result);
        } else
            result.call(false);
    }

    // Deletes the given user
    private static void deleteUser(final UserNode node, final IdentityProviderConfig config, final Functions.UnaryVoid<Boolean> result) {
        if (!config.isWritable()) {
            result.call(false);
            return;
        }

        // Make sure
        DialogDisplayer.showConfirmDialog(getTopParent(),
          "Are you sure you want to delete " +
          node.getName() + "?",
          "Delete User",
          JOptionPane.YES_NO_OPTION, new DialogDisplayer.OptionListener() {
            public void reportResult(int opresult) {
                if (JOptionPane.YES_OPTION != opresult) {
                    result.call(false);
                    return;
                }

                // Delete the  node and update the tree
                try {
                    EntityHeader eh = node.getEntityHeader();
                    IdentityAdmin admin = Registry.getDefault().getIdentityAdmin();
                    admin.deleteUser(config.getGoid(), eh.getStrId());
                    result.call(true);
                    return;
                } catch (DeleteException e) {
                    log.log(Level.SEVERE, "Error deleting user", e);
                    // Error deleting realm - display error msg
                    DialogDisplayer.showMessageDialog(getTopParent(),
                                                      ExceptionUtils.getMessage(e),
                                                      "User Cannot Be Deleted",
                                                      JOptionPane.ERROR_MESSAGE, null);
                } catch (ObjectModelException ome) {
                    log.log(Level.SEVERE, "Error deleting user", ome);
                    // Error deleting realm - display error msg
                    DialogDisplayer.showMessageDialog(getTopParent(),
                                                      "Error encountered while deleting " +
                                                      node.getName() +
                                                      ". Please try again later.",
                                                      "Delete User",
                                                      JOptionPane.ERROR_MESSAGE, null);
                } catch (Throwable throwable) {
                    ErrorManager.getDefault().notify(Level.WARNING, throwable, "Error deleting user");
                }
                result.call(false);
            }
        });
    }

    // Deletes the given user
    private static void deleteGroup(final GroupNode node, final IdentityProviderConfig config, final Functions.UnaryVoid<Boolean> result) {
        if (!config.isWritable()) {
            result.call(false);
            return;
        }

        // Make sure
        DialogDisplayer.showConfirmDialog(getTopParent(),
          "Are you sure you want to delete " +
          node.getName() + "?",
          "Delete Group",
          JOptionPane.YES_NO_OPTION, new DialogDisplayer.OptionListener() {
            public void reportResult(int opresult) {
                if (opresult != JOptionPane.YES_OPTION) {
                    result.call(false);
                    return;
                }

                // Delete the  node and update the tree
                try {
                    EntityHeader eh = node.getEntityHeader();
                    Registry.getDefault().getIdentityAdmin().deleteGroup(config.getGoid(), eh.getStrId());
                    result.call(true);
                    return;
                } catch (ObjectModelException ome) {
                    log.log(Level.SEVERE, "Error deleting group", ExceptionUtils.getDebugException(ome));
                    // Error deleting realm - display error msg
                    String msg;
                    DeleteException de = ExceptionUtils.getCauseIfCausedBy(ome, DeleteException.class);
                    if (de != null) {
                        msg = de.getMessage();
                    } else
                        msg = "Error encountered while deleting " + node.getName() + ". Please try again later.";
                    DialogDisplayer.showMessageDialog(getTopParent(), msg, "Delete Group", JOptionPane.ERROR_MESSAGE, null);
                } catch (Throwable throwable) {
                    ErrorManager.getDefault().notify(Level.WARNING, throwable, "Error deleting group");
                }
                result.call(false);
            }
        });
    }

    // Deletes the given user
    private static void deleteProvider(final IdentityProviderNode nodeIdentity, final Functions.UnaryVoid<Boolean> result) {

        // Make sure
        DialogDisplayer.showConfirmDialog(getTopParent(),
          "Are you sure you want to delete " +
          nodeIdentity.getName() + "?",
          "Delete Provider",
          JOptionPane.YES_NO_OPTION, new DialogDisplayer.OptionListener() {
            public void reportResult(int opresult) {
                if (opresult != JOptionPane.YES_OPTION) {
                    result.call(false);
                    return;
                }

                // Delete the  node and update the tree
                try {
                    EntityHeader eh = nodeIdentity.getEntityHeader();
                    Registry.getDefault().getIdentityAdmin().deleteIdentityProviderConfig(eh.getGoid());
                    result.call(true);
                    return;
                } catch (ObjectModelException ome) {
                    log.log(Level.SEVERE, "Error deleting provider", ome);
                    // Error deleting realm - display error msg
                    DialogDisplayer.showMessageDialog(getTopParent(),
                      "Error encountered while deleting " +
                      nodeIdentity.getName() +
                      ". Please try again later.",
                      "Delete Provider",
                      JOptionPane.ERROR_MESSAGE, null);
                } catch (Throwable throwable) {
                    ErrorManager.getDefault().notify(Level.WARNING, throwable, "Error deleting provider");
                }
                result.call(false);
            }
        });
    }

    /**
     * Ask the user to confirm an action, if confirmed call the call back function result
     * @param message Message to ask the user to confirm
     * @param title Title of the dialog shown to the user
     * @param result call back function to call if dialog confirmed by user. true will be supplied if confirmed
     */
    public static void getUserConfirmationAndCallBack(final String message, final String title, final Functions.UnaryVoid<Boolean> result){
        DialogDisplayer.showConfirmDialog(getTopParent(),
          message,
          title,
          JOptionPane.YES_NO_OPTION, new DialogDisplayer.OptionListener() {
            public void reportResult(int opresult) {
                if (opresult != JOptionPane.YES_OPTION) {
                    result.call(false);
                    return;
                }
                result.call(true);
            }
        });
    }

    static void deleteAssertion(AssertionTreeNode node, final Functions.UnaryVoid<Boolean> result) {
        
        String nodeName= DefaultAssertionPolicyNode.getNameFromMeta(node.asAssertion(), true, false);
        if (nodeName != null && nodeName.length() > 80) {
            nodeName = nodeName.substring(0,76) + "...";
        }

        DialogDisplayer.showConfirmDialog(getTopParent(),
                                           "Are you sure you want to delete assertion \"" + nodeName + "\"?", "Delete Assertion",
                                           JOptionPane.YES_NO_OPTION, new DialogDisplayer.OptionListener() {
            public void reportResult(int opresult) {
                result.call(opresult == JOptionPane.YES_OPTION);
            }
        });
    }

    protected static Frame getTopParent() {
        return TopComponents.getInstance().getTopParent();
    }

    static void deleteAssertions(AssertionTreeNode[] nodes, final Functions.UnaryVoid<Boolean> result) {
        if (nodes == null) {
            result.call(false);
            return;
        }

        // Make sure
        DialogDisplayer.showConfirmDialog(getTopParent(),
                                           "Are you sure you want to delete " +nodes.length+ " assertions?", "Delete Assertions",
                                           JOptionPane.YES_NO_OPTION, new DialogDisplayer.OptionListener() {
            public void reportResult(int opresult) {
                result.call(opresult == JOptionPane.YES_OPTION);
            }
        });
    }

    /**
     * invoke help programatically the F1 - help
     *
     * @param c the event source component
     */
    public static void invokeHelp(Component c) {
        Runnable r = new Runnable() {
            public void run() {
                TopComponents.getInstance().showHelpTopics();
            }
        };
        SwingUtilities.invokeLater(r);
    }
}
