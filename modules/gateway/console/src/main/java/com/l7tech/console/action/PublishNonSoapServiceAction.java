package com.l7tech.console.action;

import com.l7tech.console.panels.AbstractPublishServiceWizard;
import com.l7tech.console.panels.PublishNonSoapServiceWizard;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.AttemptedCreate;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.util.Option;
import org.jetbrains.annotations.NotNull;

/**
 * SSM action to publish a non-soap xml service.
 */
public class PublishNonSoapServiceAction extends AbstractPublishServiceAction {
    public PublishNonSoapServiceAction() {
        this(Option.<Folder>none(), Option.<AbstractTreeNode>none());
    }

    public PublishNonSoapServiceAction(@NotNull final Folder folder,
                                       @NotNull final AbstractTreeNode abstractTreeNode) {
        super(new AttemptedCreate(EntityType.SERVICE), Option.some(folder), Option.some(abstractTreeNode));
    }

    public PublishNonSoapServiceAction(@NotNull final Option<Folder> folder,
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
        return PublishNonSoapServiceWizard.getInstance(TopComponents.getInstance().getTopParent());
    }

    private static final String TITLE = "Publish Web API";
    private static final String DESCRIPTION = "Publish an entry point for a generic service not based on a WSDL (REST, Web API, FTP)";
    private static final String ICON = "com/l7tech/console/resources/xmlObject16.gif";
}
