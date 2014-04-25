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
    public PublishReverseWebProxyAction(@NotNull final Option<Folder> folder,
                                        @NotNull final Option<AbstractTreeNode> parentNode) {
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
        return PublishReverseWebProxyWizard.getInstance(TopComponents.getInstance().getTopParent());
    }

    private static final String TITLE = "Publish Reverse Web Proxy";
    private static final String DESCRIPTION = "Publish a Reverse Web Proxy service";
    private static final String ICON = "com/l7tech/console/resources/services16.png";
}