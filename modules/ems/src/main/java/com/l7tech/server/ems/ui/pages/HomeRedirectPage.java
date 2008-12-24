package com.l7tech.server.ems.ui.pages;

import org.apache.wicket.markup.html.pages.RedirectPage;
import com.l7tech.gateway.common.security.rbac.RequiredPermissionSet;
import com.l7tech.gateway.common.admin.Administrative;

/**
 * Page for redirect to "/".
 */
@RequiredPermissionSet()
@Administrative(licensed=false,authenticated=false)
public class HomeRedirectPage extends RedirectPage {
    public HomeRedirectPage() {
        super("/");
    }
}
