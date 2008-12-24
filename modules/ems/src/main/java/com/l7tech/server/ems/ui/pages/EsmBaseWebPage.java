package com.l7tech.server.ems.ui.pages;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.spring.injection.annot.SpringBean;
import com.l7tech.server.ems.ui.EsmSession;
import com.l7tech.server.ems.ui.EsmSecurityManager;
import com.l7tech.identity.User;

import javax.servlet.http.HttpServletRequest;

/**
 * Base for all EMS web pages, that provides access to the current user, LoginInfo, and EmsSession.
 */
public class EsmBaseWebPage extends WebPage {
    @SpringBean
    protected EsmSecurityManager securityManager;

    @Override
    public EsmSession getSession() {
        return (EsmSession) super.getSession();
    }

    EsmSecurityManager.LoginInfo getLoginInfo() {
        EsmSecurityManager.LoginInfo info;

        ServletWebRequest servletWebRequest = (ServletWebRequest) getRequest();
        HttpServletRequest request = servletWebRequest.getHttpServletRequest();
        info = securityManager.getLoginInfo( request.getSession(true) );

        return info;
    }

    User getUser() {
        User user = null;

        EsmSecurityManager.LoginInfo info = getLoginInfo();
        if ( info != null ) {
            user = info.getUser();
        }

        return user;
    }
}
