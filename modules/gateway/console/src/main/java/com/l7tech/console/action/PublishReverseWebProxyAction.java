package com.l7tech.console.action;

import com.l7tech.console.panels.AbstractPublishServiceWizard;
import com.l7tech.console.panels.reverseproxy.PublishReverseWebProxyWizard;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.AttemptedCreate;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.util.Option;
import org.jetbrains.annotations.NotNull;

/**
 * Action which opens up the
 */
public class PublishReverseWebProxyAction extends AbstractPublishServiceAction {

    public PublishReverseWebProxyAction(@NotNull final Option<Folder> folder, @NotNull final Option<AbstractTreeNode> parentNode) {
        super(new AttemptedCreate(EntityType.SERVICE), folder, parentNode);
    }

    public PublishReverseWebProxyAction() {
        this(Option.<Folder>none(), Option.<AbstractTreeNode>none());
    }

    public PublishReverseWebProxyAction(@NotNull final Folder folder,
                                        @NotNull final AbstractTreeNode parentNode) {
        this(Option.some(folder), Option.<AbstractTreeNode>some(parentNode));
    }

    @Override
    public String getName() {
        return "Publish Reverse Web Proxy";
    }

    @Override
    public String getDescription() {
        return "Publish a Reverse Web Proxy";
    }

    @Override
    protected String iconResource() {
        return "com/l7tech/console/resources/services16.png";
    }

    @Override
    public AbstractPublishServiceWizard createWizard() {
        return PublishReverseWebProxyWizard.getInstance(TopComponents.getInstance().getTopParent());
    }
}