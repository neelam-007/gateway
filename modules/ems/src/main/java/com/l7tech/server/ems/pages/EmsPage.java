package com.l7tech.server.ems.pages;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import com.l7tech.server.ems.EmsSecurityManager;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.io.Serializable;

/**
 * Base page for EMS
 */
public abstract class EmsPage extends WebPage {

    //- PUBLIC

    public EmsPage() {
        ServletWebRequest servletWebRequest = (ServletWebRequest) getRequest();
        HttpServletRequest request = servletWebRequest.getHttpServletRequest();
        EmsSecurityManager.LoginInfo info = securityManager.getLoginInfo( request.getSession(true) );

        add( new Label("titleLabel", new StringResourceModel( "page.title", this, null, new Object[]{ new StringResourceModel( "page.${pageName}.title", this, new Model(this))} )) );
        add( new Label("userLabel",  new StringResourceModel( "page.user", this, new Model(info) )) );
        add( new Label("sinceLabel",  new StringResourceModel( "page.since", this, new Model(info) )) );
        add( new NavigationPanel("navigation", new Model(this)) );
        add( new Label("timeZoneLabel", new StringResourceModel( "page.timeZone", this, new Model((Serializable)Collections.singletonMap("timeZone", "UTC (TODO)")) )) );
    }

    public String getPageName() {
        String name = getClass().getName();

        int packageIndex = name.lastIndexOf('.');
        if ( packageIndex > 0 ) {
            name = name.substring( packageIndex + 1 );
        }

        return name;
    }

    //- PRIVATE
    
    @SuppressWarnings({"UnusedDeclaration"})
    @SpringBean
    private EmsSecurityManager securityManager;
}
