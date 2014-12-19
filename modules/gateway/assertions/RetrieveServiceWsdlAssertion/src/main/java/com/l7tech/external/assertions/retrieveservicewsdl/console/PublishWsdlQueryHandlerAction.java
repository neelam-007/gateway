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
import com.l7tech.util.Option;

import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;

/**
 * Launches Publish WSDL Query Handler wizard.
 *
 * @author Jamie Williams - jamie.williams2@ca.com
 */
public class PublishWsdlQueryHandlerAction extends AbstractPublishServiceAction {
    private static final String NAME = "Publish WSDL Query Handler";
    private static final String DESCRIPTION = "Publish a WSDL Query Handler service";
    private static final String ICON = "com/l7tech/console/resources/services16.png";

    public PublishWsdlQueryHandlerAction() {
        super(new AttemptedCreate(EntityType.FOLDER),
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
                PublishWsdlQueryHandlerAction.this.setActionValues();

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
    }

    @Override
    protected AbstractPublishServiceWizard createWizard() {
        return PublishWsdlQueryHandlerWizard.getInstance(TopComponents.getInstance().getTopParent());
    }

    @Override
    public String getName() {
        String selectedFolderPath = getSelectedFolderPath();

        return null == selectedFolderPath ? NAME : NAME + " in " + selectedFolderPath;
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
