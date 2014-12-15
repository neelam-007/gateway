package com.l7tech.external.assertions.retrieveservicewsdl.console;

import com.l7tech.console.action.AbstractPublishServiceAction;
import com.l7tech.console.panels.AbstractPublishServiceWizard;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.ServicesAndPoliciesTree;
import com.l7tech.console.tree.servicesAndPolicies.FolderNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.AttemptedCreate;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.server.GatewayFeatureSets;
import com.l7tech.util.Option;
import com.l7tech.util.Pair;

import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;

/**
 * Launches Publish WSDL Query Handler wizard.
 *
 * @author Jamie Williams - jamie.williams2@ca.com
 */
public class PublishWsdlQueryHandlerAction extends AbstractPublishServiceAction {
    private static final String NAME_PREFIX = "Publish WSDL Query Handler in ";
    private static final String WSDL_FOLDER = "/WSDL";
    private static final String DESCRIPTION = "Publish a WSDL Query Handler service";
    private static final String ICON = "com/l7tech/console/resources/services16.png";

    public PublishWsdlQueryHandlerAction() {
        super(new AttemptedCreate(EntityType.FOLDER),
                Option.<Folder>none(), Option.<AbstractTreeNode>none(),
                GatewayFeatureSets.UI_PUBLISH_WSDL_QUERY_HANDLER_WIZARD);

        ServicesAndPoliciesTree tree = (ServicesAndPoliciesTree) TopComponents.getInstance().getComponent(ServicesAndPoliciesTree.NAME);
        tree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                PublishWsdlQueryHandlerAction.this.setActionValues();
            }
        });
    }

    @Override
    protected AbstractPublishServiceWizard createWizard() {
        return PublishWsdlQueryHandlerWizard.getInstance(TopComponents.getInstance().getTopParent());
    }

    @Override
    public String getName() {
        Pair<String, FolderNode> selectedFolder = getSelectedFolder();

        if (selectedFolder.left != null) {
            return NAME_PREFIX + selectedFolder.left + WSDL_FOLDER;
        } else {
            return NAME_PREFIX + WSDL_FOLDER;
        }
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    protected String iconResource() {
        return ICON;
    }

    private Pair<String, FolderNode> getSelectedFolder() {
        ServicesAndPoliciesTree tree =
                (ServicesAndPoliciesTree) TopComponents.getInstance().getComponent(ServicesAndPoliciesTree.NAME);

        String folderPath = null;
        FolderNode selectedFolder = null;

        if (tree != null) {
            FolderNode lastFolderNode = tree.getRootNode();

            final TreePath selectionPath = tree.getSelectionPath();

            if (selectionPath != null) {
                final Object[] path = selectionPath.getPath();

                // skip the root node
                if (path.length > 1) {
                    StringBuilder builder = new StringBuilder();

                    for (int i = 1, pathLength = path.length; i < pathLength; i++) {
                        Object o = path[i];

                        if (o instanceof FolderNode) {
                            FolderNode folderNode = (FolderNode) o;
                            builder.append("/");
                            builder.append(folderNode.getName());

                            lastFolderNode = folderNode;
                        }
                    }

                    folderPath = builder.toString();
                }
            }

            selectedFolder = lastFolderNode;
        }

        return new Pair<>(folderPath, selectedFolder);
    }
}
