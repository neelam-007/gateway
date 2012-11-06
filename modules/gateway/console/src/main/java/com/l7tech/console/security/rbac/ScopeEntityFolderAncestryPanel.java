package com.l7tech.console.security.rbac;

import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.security.rbac.EntityFolderAncestryPredicate;
import com.l7tech.gateway.common.security.rbac.Permission;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.ValidatedPanel;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.folder.FolderHeader;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.util.ExceptionUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Panel for editing EntityFolderAncestryPredicate.
 */
public class ScopeEntityFolderAncestryPanel extends ValidatedPanel<EntityFolderAncestryPredicate> {
    private static final Logger logger = Logger.getLogger(ScopeEntityFolderAncestryPanel.class.getName());

    private JPanel contentPane;
    private JTextField specificText;
    private JButton specificFindButton;

    private final EntityType entityType;
    private final Permission permission;
    private EntityFolderAncestryPredicate model;
    private String entityId;
    private EntityHeader entityHeader;

    public ScopeEntityFolderAncestryPanel(@NotNull EntityFolderAncestryPredicate model, @NotNull EntityType entityType) {
        super("folderPredicate");
        this.model = model;
        this.permission = model.getPermission();
        this.entityId = model.getEntityId();
        this.entityType = entityType;
        entityHeader = lookupEntityHeader(entityType, entityId);
        init();
    }

    @Override
    protected EntityFolderAncestryPredicate getModel() {
        doUpdateModel();
        return model;
    }

    @Override
    protected void initComponents() {
        updateSpecificText();

        specificFindButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final FindEntityDialog fed = new FindEntityDialog((JDialog) Utilities.getRootPaneContainerAncestor(ScopeEntityFolderAncestryPanel.this), EntityType.FOLDER, null);
                fed.pack();
                Utilities.centerOnScreen(fed);
                DialogDisplayer.display(fed, new Runnable() {
                    public void run() {
                        EntityHeader header = fed.getSelectedEntityHeader();
                        if (header != null) {
                            entityHeader = header;
                            entityId = header.getStrId();
                            updateSpecificText();
                            checkSyntax();
                        }
                    }
                });
            }
        });

        setLayout(new BorderLayout());
        add(contentPane, BorderLayout.CENTER);
    }

    @Override
    public void focusFirstComponent() {
        specificFindButton.requestFocusInWindow();
    }

    @Override
    protected void doUpdateModel() {
        model = new EntityFolderAncestryPredicate(permission, entityType, entityId);
        if(entityHeader != null) model.setName(entityHeader.getName());
    }

    @Override
    protected String getSyntaxError(final EntityFolderAncestryPredicate model) {
        String error = null;

        if (entityHeader == null) {
            error = "An entity must be selected.";
        }

        return error;
    }

    private EntityHeader lookupEntityHeader(EntityType entityType, String entityId) {
        long oid = Long.valueOf(entityId);

        try {
            switch (entityType) {
                case FOLDER:
                    Folder folder = Registry.getDefault().getFolderAdmin().findByPrimaryKey(oid);
                    return folder == null ? null : new FolderHeader(folder);

                case POLICY:
                    Policy policy = Registry.getDefault().getPolicyAdmin().findPolicyByPrimaryKey(oid);
                    return policy == null ? null : new PolicyHeader(policy);

                case SERVICE:
                    PublishedService service = Registry.getDefault().getServiceManager().findServiceByID(entityId);
                    return service == null ? null : new ServiceHeader(service);

                default:
                    logger.warning("Entity type not supported for folder ancestry predicate: " + entityType);
                    return null;
            }
        } catch (FindException e) {
            logger.log(Level.WARNING, "Unable to look up entity header for " + entityType + " oid " + entityId + ": " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            return null;
        }
    }

    private void updateSpecificText() {
        specificText.setText(createSpecificText());
    }

    private String createSpecificText() {
        if (entityHeader != null) {
            return entityType.getName() + " named " + entityHeader.getName();
        }

        return entityType.getName() + " with ID " + entityId;
    }
}
