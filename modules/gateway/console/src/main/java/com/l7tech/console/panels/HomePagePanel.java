/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.console.panels;

import com.l7tech.gui.util.Utilities;
import com.l7tech.gateway.common.security.rbac.Permission;
import com.l7tech.console.MainWindow;
import com.l7tech.console.action.*;
import com.l7tech.console.panels.identity.finder.Options;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.identity.IdentityProvidersTree;
import com.l7tech.console.util.ConsoleLicenseManager;
import com.l7tech.console.util.LicenseListener;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;

import javax.swing.*;
import javax.swing.tree.TreeModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author mike
 */
public class HomePagePanel extends JPanel {
    private static final ClassLoader cl = HomePagePanel.class.getClassLoader();
    private JLabel topTopLabel;
    private JLabel footerLabel;
    private JLabel topMidLabel;
    private JLabel topBotLabel;
    private JPanel toolbarPanel;
    private JPanel rootPanel;
    private JLabel toolbarIndenter;
    private JPanel scrollPanel;
    private List<HomePageToolbarAction> actions = new ArrayList<HomePageToolbarAction>();
    private ImageIcon pageBanner;
    private final LicenseListener licenseListener = new LicenseListener() {
        public void licenseChanged(ConsoleLicenseManager licenseManager) {
                rebuildToolbar();
            }
        };

    public HomePagePanel() {
        setLayout(new BorderLayout());
        add(rootPanel, BorderLayout.CENTER);
        rootPanel.setBackground(Color.WHITE);
        scrollPanel.setBackground(Color.WHITE);
        setName("Home");

        ClassLoader cl = getClass().getClassLoader();
        pageBanner = new ImageIcon(cl.getResource(MainWindow.RESOURCE_PATH + "/PageBanner.png"));
        ImageIcon welcomeText = new ImageIcon(cl.getResource(MainWindow.RESOURCE_PATH + "/WelcomeText.png"));
        ImageIcon tasksBanner = new ImageIcon(cl.getResource(MainWindow.RESOURCE_PATH + "/TasksBanner.png"));
        ImageIcon logoSmall = new ImageIcon(cl.getResource(MainWindow.RESOURCE_PATH + "/LOGOsmall.png"));

        topTopLabel.setText("");
        topTopLabel.setIcon(pageBanner);

        topMidLabel.setText("");
        topMidLabel.setIcon(welcomeText);

        topBotLabel.setText("");
        topBotLabel.setIcon(tasksBanner);

        footerLabel.setText("");
        footerLabel.setIcon(logoSmall);

        toolbarPanel.setBackground(getBackground());
        rebuildToolbar();
    }

    protected void rebuildToolbar() {
        toolbarPanel.removeAll();
        toolbarPanel.setLayout(new BorderLayout());
        toolbarPanel.add(getToolbar(), BorderLayout.CENTER);
        validate();
        repaint();
    }

    private boolean isAdmin() {
        Collection<Permission> perms = Registry.getDefault().getSecurityProvider().getUserPermissions();
        return !perms.isEmpty();
    }

    private Component getToolbar() {
        JToolBar tb = new JToolBar(JToolBar.VERTICAL);
        tb.setBackground(rootPanel.getBackground());
        tb.setFloatable(false);
        tb.setOpaque(false);
        tb.setBorderPainted(false);
        tb.setBorder(null);
        List<? extends Action> actions = createActionList();
        List<JButton> buttons = new ArrayList<JButton>(actions.size());

        // Make buttons
        for (Action action : actions) {
            if (action.isEnabled()) {
                JButton button = new JButton(action);
                buttons.add(button);

                button.setBackground(rootPanel.getBackground());
                button.setHorizontalAlignment(SwingConstants.LEFT);
                button.setIconTextGap(48);
                button.setMargin(new Insets(6, 48, 6, 8));

                Font curfont = button.getFont();
                curfont = new Font(curfont.getName(), Font.PLAIN, (int)(curfont.getSize() * 1.6));
                button.setFont(curfont);
            }
        }

        // Make them all the right width
        List<JButton> equalizer = new ArrayList<JButton>();
        JButton fake = new JButton(" ");
        toolbarIndenter.validate();
        fake.setSize(pageBanner.getIconWidth() - 48 - 10, 1);
        fake.setPreferredSize(fake.getSize());
        equalizer.add(fake);
        equalizer.addAll(buttons);
        Utilities.equalizeComponentSizes(equalizer.toArray(new JComponent[0]));

        // Add buttons to toolbar
        for (JButton button : buttons)
            tb.add(button);

        // Set background colors
        Component[] kids = tb.getComponents();
        for (Component component : kids)
            component.setBackground(tb.getBackground());

        Registry.getDefault().getLicenseManager().addLicenseListener(licenseListener);

        return tb;
    }

    private void add(String iconResource, BaseAction action) {
        if (action.isEnabled())
            actions.add(new HomePageToolbarAction(MainWindow.RESOURCE_PATH + "/" + iconResource, action));
    }

    private List<HomePageToolbarAction> createActionList() {
        actions.clear();
        if (!Registry.getDefault().getLicenseManager().isPrimaryLicenseInstalled()) {
            add("cert32.gif", new ManageClusterLicensesAction());
        }

        add("CreateIdentityProvider32x32.gif", new NewLdapProviderAction(getIdentitiesRoot()));
        add("CreateIdentityProvider32x32.gif", new NewBindOnlyLdapProviderAction(getIdentitiesRoot()));
        add("CreateIdentityProvider32x32.gif", new NewFederatedIdentityProviderAction(getIdentitiesRoot()));
        add("user32.png", new NewInternalUserAction(null) {
            public String getName() {
                return "Create Internal User";
            }
        });
        add("group32.png", new NewGroupAction(null){
            public String getName() {
                return "Create Internal Group";
            }
        });

        Options options = new Options();
        options.setEnableDeleteAction(true);
        add("SearchIdentityProvider32x32.gif", new FindIdentityAction(options));

        add("services32.png", new PublishServiceAction());
        add("xmlObject32.gif", new PublishNonSoapServiceAction());
        add("xmlObject32.gif", new PublishRestServiceAction());
        add("services32.png", new PublishInternalServiceAction());
        add("CreateWSDL32x32.gif", new CreateServiceWsdlAction());

        if (!isAdmin() || actions.size() < 3) {
            // Give operator or IPS admin something else to look at (otherwise menu is too impoverished-looking)
            add("AnalyzeGatewayLog32x32.gif", new ViewGatewayAuditsAction());
        }

        return actions;
    }

    private AbstractTreeNode getIdentitiesRoot() {
        TreeModel model = getIdentitiesTree().getModel();
        if (model == null) return null; // It's not built yet
        return (AbstractTreeNode)model.getRoot();
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

    private static class HomePageToolbarAction extends BaseAction {
        private final String name;
        private final String iconResource;
        private final BaseAction delegate;

        protected HomePageToolbarAction(String iconResource, BaseAction delegate) {
            super(delegate.getName(), delegate.getDescription(), new ImageIcon(cl.getResource(iconResource)).getImage());
            this.name = delegate.getName();
            this.iconResource = iconResource;
            this.delegate = delegate;
            putValue(Action.NAME, getName());
            putValue(Action.SHORT_DESCRIPTION, delegate.getDescription());
        }

        public String getName() {
            return name;
        }

        protected String iconResource() {
            return iconResource;
        }

        protected void performAction() {
            delegate.invoke();
        }

        public boolean isEnabled() {
            return delegate.isEnabled();
        }
    }

}
