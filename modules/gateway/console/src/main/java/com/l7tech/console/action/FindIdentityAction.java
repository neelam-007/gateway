package com.l7tech.console.action;

import com.l7tech.gui.util.Utilities;
import com.l7tech.gateway.common.security.rbac.EntityType;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.Permission;
import com.l7tech.console.panels.identity.finder.FindIdentitiesDialog;
import com.l7tech.console.panels.identity.finder.Options;
import com.l7tech.console.security.PermissionRefreshListener;
import com.l7tech.console.security.SecurityProvider;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.TreeNodeFactory;
import com.l7tech.console.util.ConsoleLicenseManager;
import com.l7tech.console.util.LicenseListener;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.objectmodel.IdentityHeader;

import javax.swing.*;
import java.awt.*;
import java.util.EnumSet;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * The <code>FindIdentityAction</code> action invokes the searche identity
 * dialog.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 * 
 * TODO why doesn't this extend SecureAction?
 */
public class FindIdentityAction extends BaseAction implements LicenseListener, PermissionRefreshListener {
    static final Logger log = Logger.getLogger(FindIdentityAction.class.getName());
    Options options = new Options();

    private static
    ResourceBundle resapplication =
      java.util.ResourceBundle.getBundle("com.l7tech.console.resources.console");

    /**
     * create the action with the default find dialog options
     */
    public FindIdentityAction() {
        this(new Options());
        Registry.getDefault().getLicenseManager().addLicenseListener(this);
    }

    /**
     * create the find idnetity action action with the dialog options
     * specified
     */
    public FindIdentityAction(Options opts) {
        if (opts == null) {
            throw new IllegalArgumentException();
        }
        options = opts;
        Registry.getDefault().getLicenseManager().addLicenseListener(this);
    }


    /**
     * @return the action name
     */
    public String getName() {
        return resapplication.getString("Find_MenuItem_text_name");
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "Search for users and/or groups in this Identity Provider";
    }

    /**
     * subclasses override this method specifying the resource name
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/SearchIdentityProvider16x16.gif";
    }

    /**
     * Actually perform the action.
     * This is the method which should be called programmatically.
     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    protected void performAction() {
        Frame f = TopComponents.getInstance().getTopParent();
        FindIdentitiesDialog fd = new FindIdentitiesDialog(f, true, options);
        fd.pack();
        fd.getSearchResultTable().getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        Utilities.centerOnScreen(fd);
        FindIdentitiesDialog.FindResult result = fd.showDialog();
        if (result != null && result.entityHeaders != null && result.entityHeaders.length > 0) {
            showEditDialog(result.providerConfigOid, result.entityHeaders[0]);
        }
    }

    /**
     * instantiate the dialog for given AbstractTreeNode
     *
     * @param header the principal instance to edit
     */
    private void showEditDialog(long providerId, IdentityHeader header) {
        AbstractTreeNode an = TreeNodeFactory.asTreeNode(header);
        final BaseAction a = (BaseAction)an.getPreferredAction();
        if (a == null) return;
        IdentityProviderConfig config;
        try {
            config = Registry.getDefault().getIdentityAdmin().findIdentityProviderConfigByID(providerId);
        } catch (Exception e) {
            log.log(Level.WARNING, "Couldn't find Identity Provider " + providerId, e);
            return;
        }
        if (config == null) return;

        if (a instanceof UserPropertiesAction) {
            ((UserPropertiesAction)a).setIdProviderConfig(config);
        } else if (a instanceof GroupPropertiesAction) {
            ((GroupPropertiesAction)a).setIdProviderConfig(config);
        }

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                a.invoke();
            }
        });
    }

    public void setEnabled(boolean newValue) {
        boolean wasEnabled = isEnabled();
        super.setEnabled(newValue);
        super.setEnabled(isEnabled());
        boolean isEnabled = isEnabled();
        if (wasEnabled != isEnabled)
            firePropertyChange("enabled", wasEnabled, isEnabled);
    }

    private static final EnumSet TYPES = EnumSet.of(
            EntityType.ANY, EntityType.ID_PROVIDER_CONFIG, EntityType.USER, EntityType.GROUP);

    public boolean isEnabled() {
        boolean e = super.isEnabled();
        if (!e) return false;
        if (!Registry.getDefault().getLicenseManager().isAuthenticationEnabled()) return false;
        SecurityProvider sp = Registry.getDefault().getSecurityProvider();
        if (sp == null) return false;
        for (Permission perm : sp.getUserPermissions()) {
            if (perm.getOperation() == OperationType.READ && TYPES.contains(perm.getEntityType())) return true;
        }
        return false;
    }

    public void licenseChanged(ConsoleLicenseManager licenseManager) {
        setEnabled(true); // it'll immediately get forced back to false if the license disallows it
    }

    public void onPermissionRefresh() {
        setEnabled(true);
    }
}
