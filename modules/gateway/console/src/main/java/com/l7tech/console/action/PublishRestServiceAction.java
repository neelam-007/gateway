package com.l7tech.console.action;

import com.l7tech.console.panels.AbstractPublishServiceWizard;
import com.l7tech.console.panels.PublishRestServiceWizard;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.AttemptedCreate;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.util.Option;
import org.jetbrains.annotations.NotNull;

/**
 * SSM action to publish a RESTful service.
 *
 * @author KDiep
 */
public class PublishRestServiceAction extends AbstractPublishServiceAction {
    public PublishRestServiceAction() {
        this(Option.<Folder>none(), Option.<AbstractTreeNode>none());
    }

    public PublishRestServiceAction(@NotNull final Folder folder,
                                    @NotNull final AbstractTreeNode abstractTreeNode) {
        super(new AttemptedCreate(EntityType.SERVICE), Option.some(folder), Option.some(abstractTreeNode));
    }

    public PublishRestServiceAction(@NotNull final Option<Folder> folder,
                                    @NotNull final Option<AbstractTreeNode> abstractTreeNode) {
        super(new AttemptedCreate(EntityType.SERVICE), folder, abstractTreeNode, UI_PUBLISH_XML_WIZARD);
    }

    @Override
    public String getName() {
        return TITLE;
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    protected String iconResource() {
        return ICON;
    }

    @Override
    protected AbstractPublishServiceWizard createWizard() {
        return PublishRestServiceWizard.getInstance(TopComponents.getInstance().getTopParent());
    }

    private static final String TITLE = "Publish RESTful Service Proxy With WADL";
    private static final String DESCRIPTION = "Publish an entry point for a RESTful service from a WADL descriptor or manual entry.";
    private static final String ICON = "com/l7tech/console/resources/xmlObject16.gif";
}
