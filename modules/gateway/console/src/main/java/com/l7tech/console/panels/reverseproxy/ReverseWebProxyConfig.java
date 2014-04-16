package com.l7tech.console.panels.reverseproxy;

import com.l7tech.console.util.TopComponents;
import com.l7tech.objectmodel.folder.Folder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Configuration POJO for a reverse web proxy policy.
 */
public class ReverseWebProxyConfig {
    private Folder folder;
    private String name;
    private String webAppHost;
    private String routingUri = "*";
    private WebApplicationType webAppType = WebApplicationType.GENERIC;
    private boolean rewriteLocationHeader = true;
    private boolean rewriteCookies = true;
    private boolean rewriteResponseContent = true;
    private boolean useHttps;
    private String htmlTagsToRewrite;

    public ReverseWebProxyConfig() {
        this.folder = getDefaultFolder();
    }

    @NotNull
    public Folder getFolder() {
        return folder;
    }

    public void setFolder(@NotNull final Folder folder) {
        this.folder = folder;
    }

    @Nullable
    public String getName() {
        return name;
    }

    public void setName(@NotNull final String name) {
        this.name = name;
    }

    @Nullable
    public String getWebAppHost() {
        return webAppHost;
    }

    public void setWebAppHost(@NotNull final String webAppHost) {
        this.webAppHost = webAppHost;
    }

    @NotNull
    public String getRoutingUri() {
        return routingUri;
    }

    public void setRoutingUri(@NotNull final String routingUri) {
        this.routingUri = routingUri;
    }

    @NotNull
    public WebApplicationType getWebAppType() {
        return webAppType;
    }

    public void setWebAppType(@NotNull final WebApplicationType webAppType) {
        this.webAppType = webAppType;
    }

    public boolean isRewriteLocationHeader() {
        return rewriteLocationHeader;
    }

    public void setRewriteLocationHeader(final boolean rewriteLocationHeader) {
        this.rewriteLocationHeader = rewriteLocationHeader;
    }

    public boolean isRewriteCookies() {
        return rewriteCookies;
    }

    public void setRewriteCookies(final boolean rewriteCookies) {
        this.rewriteCookies = rewriteCookies;
    }

    public boolean isRewriteResponseContent() {
        return rewriteResponseContent;
    }

    public void setRewriteResponseContent(final boolean rewriteResponseContent) {
        this.rewriteResponseContent = rewriteResponseContent;
    }

    public boolean isUseHttps() {
        return useHttps;
    }

    public void setUseHttps(final boolean useHttps) {
        this.useHttps = useHttps;
    }

    @Nullable
    public String getHtmlTagsToRewrite() {
        return htmlTagsToRewrite;
    }

    public void setHtmlTagsToRewrite(@Nullable final String htmlTagsToRewrite) {
        this.htmlTagsToRewrite = htmlTagsToRewrite;
    }

    public enum WebApplicationType {
        SHAREPOINT("Sharepoint"), GENERIC("Generic");

        private final String name;

        private WebApplicationType(@NotNull final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    @NotNull
    protected Folder getDefaultFolder() {
        return TopComponents.getInstance().getRootNode().getFolder();
    }
}