package com.l7tech.console.action;

import com.l7tech.console.MainWindow;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.identity.IdentityProvidersTree;
import com.l7tech.console.panels.WorkSpacePanel;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLDocument;
import java.awt.*;
import java.io.IOException;
import java.net.URL;

/**
 * The <code>HomeAction</code> displays the dds the new user.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class HomeAction extends BaseAction {
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
    public void performAction() {
        try {
            wpanel.setComponent(getHomePageComponent());
        } catch (ActionVetoException e) {
            log.fine("workspace change vetoed");
        }
    }


    private JComponent getHomePageComponent() {
        final String home = MainWindow.RESOURCE_PATH + "/home.html";
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
                                new PublishServiceAction().performAction();
                            }
                        });
                    } else if (CREATE_DEFINITION.equals(url)) {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                new CreateServiceWsdlAction().performAction();
                            }
                        });
                    } else if (ADD_USER.equals(url)) {
                        new NewInternalUserAction(null).actionPerformed(null);
                    } else if (ADD_GROUP.equals(url)) {
                        new NewGroupAction(null).actionPerformed(null);
                    } else if (ADD_ID_PROVIDER.equals(url)) {
                        final DefaultMutableTreeNode root =
                                (DefaultMutableTreeNode) getIdentitiesTree().getModel().getRoot();
                        AbstractTreeNode node = (AbstractTreeNode) root;
                        new NewLdapProviderAction(node).actionPerformed(null);
                    }
                }
            }
        });
        try {
            htmlPane.setPage(url);
            return htmlPane;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
    private static final String CREATE_DEFINITION = "file://create.definition";
    private static final String ADD_USER = "file://add.user";
    private static final String ADD_GROUP = "file://add.group";
    private static final String ADD_ID_PROVIDER = "file://add.identity.provider";
}
