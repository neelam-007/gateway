package com.l7tech.external.assertions.swagger.console;

import com.l7tech.console.action.AbstractPublishServiceAction;
import com.l7tech.console.panels.AbstractPublishServiceWizard;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.ServicesAndPoliciesTree;
import com.l7tech.console.tree.servicesAndPolicies.FolderNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.AttemptedCreate;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.util.Option;

import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;

/**
 * Launches Publish Swagger Service wizard.
 *
 * @author Jamie Williams - jamie.williams2@ca.com
 */
public class PublishSwaggerServiceAction extends AbstractPublishServiceAction {
    private static final String NAME = "Publish Swagger Service";
    private static final String DESCRIPTION = "Publish a Swagger service";
    private static final String ICON = "com/l7tech/external/assertions/swagger/console/resources/swagger-16x16.png";

    public PublishSwaggerServiceAction() {
        super(new AttemptedCreate(EntityType.SERVICE),
                Option.<Folder>none(), Option.<AbstractTreeNode>none(),
                UI_PUBLISH_WSDL_QUERY_HANDLER_WIZARD);

        init();
    }

    private void init() {
        final ServicesAndPoliciesTree tree =
                (ServicesAndPoliciesTree) TopComponents.getInstance().getComponent(ServicesAndPoliciesTree.NAME);

        tree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                PublishSwaggerServiceAction.this.setActionValues();

                FolderNode folderNode = tree.getDeepestFolderNodeInSelectionPath();

                if (null == folderNode) {
                    setFolder(Option.<Folder>none());
                    setParentNode(Option.<AbstractTreeNode>none());
                } else {
                    setFolder(new Option<>(folderNode.getFolder()));
                    setParentNode(new Option<AbstractTreeNode>(folderNode));
                }
            }
        });

        putValue("MenuHint", "Services");
    }

    @Override
    protected AbstractPublishServiceWizard createWizard() {
        return PublishSwaggerServiceWizard.getInstance(TopComponents.getInstance().getTopParent());
    }

    @Override
    public String getName() {
        String selectedFolderPath = getSelectedFolderPath();

        return (null == selectedFolderPath || selectedFolderPath.isEmpty()) ? NAME : NAME + " in " + selectedFolderPath;
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    protected String iconResource() {
        return ICON;
    }

    private static String getSelectedFolderPath() {
        ServicesAndPoliciesTree tree =
                (ServicesAndPoliciesTree) TopComponents.getInstance().getComponent(ServicesAndPoliciesTree.NAME);

        String folderPath = null;

        if (tree != null) {
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
                        }
                    }

                    folderPath = builder.toString();
                }
            }
        }

        return folderPath;
    }
}
