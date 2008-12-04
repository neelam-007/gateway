package com.l7tech.server.ems.pages;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.spring.injection.annot.SpringBean;
import com.l7tech.server.ems.EmsSession;
import com.l7tech.server.ems.EmsSecurityManager;
import com.l7tech.identity.User;

import javax.servlet.http.HttpServletRequest;

/**
 * Base for all EMS web pages, that provides access to the current user, LoginInfo, and EmsSession.
 */
public class EmsBaseWebPage extends WebPage {
    @SpringBean
    protected EmsSecurityManager securityManager;

    @Override
    public EmsSession getSession() {
        return (EmsSession) super.getSession();
    }

    EmsSecurityManager.LoginInfo getLoginInfo() {
        EmsSecurityManager.LoginInfo info;

        ServletWebRequest servletWebRequest = (ServletWebRequest) getRequest();
        HttpServletRequest request = servletWebRequest.getHttpServletRequest();
        info = securityManager.getLoginInfo( request.getSession(true) );

        return info;
    }

    User getUser() {
        User user = null;

        EmsSecurityManager.LoginInfo info = getLoginInfo();
        if ( info != null ) {
            user = info.getUser();
        }

        return user;
    }
}
