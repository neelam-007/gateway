package com.l7tech.console.action;

import com.l7tech.console.MainWindow;
import com.l7tech.console.panels.FindIdentitiesDialog;
import com.l7tech.console.panels.WorkSpacePanel;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.identity.IdentityProvidersTree;
import com.l7tech.console.util.TopComponents;
import com.l7tech.identity.Group;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.EditorKit;
import javax.swing.text.StyledEditorKit;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.List;

/**
 * The <code>HomeAction</code> displays the dds the new user.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class HomeAction extends SecureAction {
    private WorkSpacePanel wpanel;
    private ClassLoader cl = getClass().getClassLoader();

    public HomeAction() {
        wpanel = TopComponents.getInstance().getCurrentWorkspace();
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Home";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "Go to the SecureSpan Manager home page";
    }

    /**
     * specify the resource name for this action
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/server16.gif";
    }

    /**
     * Actually perform the action.
     * This is the method which should be called programmatically.
     * <p/>
     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    protected void performAction() {
        try {
            wpanel.setComponent(getHomePageComponent());
        } catch (ActionVetoException e) {
            log.fine("workspace change vetoed");
        }
    }


    private JComponent getHomePageComponent() {

        final String home = getHomePageForAdminRole();
        HTMLDocument doc = new HTMLDocument();
        JTextPane htmlPane = new JTextPane(doc) {
            public void paint(Graphics g) {
                try {
                    super.paint(g);
                } catch (NullPointerException e) {
                    log.info("Workaround for Bugzilla 267 (Java bugparade 4829437)");
                    repaint();
                }
            }
        };

        URL url = cl.getResource(home);
        htmlPane.setName("Home");
        htmlPane.setEditable(false);
        htmlPane.addHyperlinkListener(new HyperlinkListener() {
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (HyperlinkEvent.EventType.ACTIVATED == e.getEventType()) {
                    String url = e.getURL().toString();
                    if (ADD_SERVICE.equals(url)) {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                new PublishServiceAction().invoke();
                            }
                        });
                    } else if (ADD_XML_APP.equals(url)) {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                new PublishNonSoapServiceAction().invoke();
                            }
                        });
                    } else if (CREATE_DEFINITION.equals(url)) {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                new CreateServiceWsdlAction().invoke();
                            }
                        });
                    } else if (ADD_USER.equals(url)) {
                        new NewInternalUserAction(null).invoke();
                    } else if (ADD_GROUP.equals(url)) {
                        new NewGroupAction(null).invoke();
                    } else if (SEARCH_ID_PROVIDER.equals(url)) {
                        FindIdentitiesDialog.Options options = new FindIdentitiesDialog.Options();
                        options.enableDeleteAction();
                        options.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                        new FindIdentityAction(options).invoke();
                    } else if (ADD_LDAP_ID_PROVIDER.equals(url)) {
                        final DefaultMutableTreeNode root =
                          (DefaultMutableTreeNode)getIdentitiesTree().getModel().getRoot();
                        AbstractTreeNode node = (AbstractTreeNode)root;
                        new NewLdapProviderAction(node).invoke();
                    } else if (ADD_FEDERATED_ID_PROVIDER.equals(url)) {
                        final DefaultMutableTreeNode root =
                          (DefaultMutableTreeNode)getIdentitiesTree().getModel().getRoot();
                        AbstractTreeNode node = (AbstractTreeNode)root;
                        new NewFederatedIdentityProviderAction(node).invoke();
                    } else if (ANALYZE_GATEWAY_LOG.equals(url)) {
                        new ViewGatewayLogsAction().invoke();
                    } else if (VIEW_CLUSTER_STATUS.equals(url)) {
                        new ViewClusterStatusAction().invoke();
                    }
                }
            }
        });
        try {
            htmlPane.setPage(url);
            // bugzilla 1165, disable the up/dn actions tha cause NPE
            disableActions(htmlPane.getEditorKit(), new String[] {StyledEditorKit.upAction, StyledEditorKit.downAction});
            return htmlPane;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * disable editor actions by name
     * @param editorKit  the editr kit
     * @param actionNames the action names to disable
     */
    private void disableActions(EditorKit editorKit, String[] actionNames) {
        Action[] actions = editorKit.getActions();
        List namesList = Arrays.asList(actionNames);
        for (int i = 0; i < actions.length; i++) {
            Action action = actions[i];
            String name = (String)action.getValue(Action.NAME);
            if (name !=null) {
                int index = namesList.indexOf(name);
                if (index != -1) {
                    action.setEnabled(false);
                }
            }
        }
    }


    /**
     * Return the required roles for this action, one of the roles. The base
     * implementatoinm requires the strongest admin role.
     *
     * @return the list of roles that are allowed to carry out the action
     */
    protected String[] requiredRoles() {
        return new String[]{Group.ADMIN_GROUP_NAME, Group.OPERATOR_GROUP_NAME};
    }

    private String getHomePageForAdminRole() {
        if (isInRole(new String[]{Group.ADMIN_GROUP_NAME})) {
            return MainWindow.RESOURCE_PATH + "/home.html";
        }
        return MainWindow.RESOURCE_PATH + "/home-operator.html";
    }

    private JTree getIdentitiesTree() {
        JTree tree = (JTree)TopComponents.getInstance().getComponent(IdentityProvidersTree.NAME);
        if (tree != null) return tree;
        IdentityProvidersTree identityProvidersTree = new IdentityProvidersTree();
        identityProvidersTree.setShowsRootHandles(true);
        identityProvidersTree.setBorder(null);
        TopComponents.getInstance().registerComponent(IdentityProvidersTree.NAME, identityProvidersTree);
        return identityProvidersTree;
    }

    private static final String ADD_SERVICE = "file://add.service";
    private static final String ADD_XML_APP = "file://add.xml.app";
    private static final String CREATE_DEFINITION = "file://create.definition";
    private static final String ADD_USER = "file://add.user";
    private static final String ADD_GROUP = "file://add.group";
    private static final String ADD_LDAP_ID_PROVIDER = "file://add.ldap.identity.provider";
    private static final String ADD_FEDERATED_ID_PROVIDER = "file://add.federated.identity.provider";
    private static final String SEARCH_ID_PROVIDER = "file://search.identity.provider";

    private static final String ANALYZE_GATEWAY_LOG = "file://analyze.gateway.log";
    private static final String VIEW_CLUSTER_STATUS = "file://view.cluster.status";
}
