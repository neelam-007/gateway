package com.l7tech.server.ems.pages;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import com.l7tech.server.ems.EmsSecurityManager;
import com.l7tech.server.ems.EmsSession;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;

/**
 * Base page for EMS
 */
public abstract class EmsPage extends WebPage {

    //- PUBLIC

    public EmsPage() {
        ServletWebRequest servletWebRequest = (ServletWebRequest) getRequest();
        HttpServletRequest request = servletWebRequest.getHttpServletRequest();
        final EmsSecurityManager.LoginInfo info = securityManager.getLoginInfo( request.getSession(true) );

        Model infoModel = new Model(info);
        add( new Label("titleLabel", new StringResourceModel( "page.title", this, null, new Object[]{ new StringResourceModel( "page.${pageName}.title", this, new Model(this))} )) );
        add( new Label("userLabel", new StringResourceModel( "page.user", this, infoModel )) );
        StringResourceModel sinceResourceModel = new StringResourceModel( "page.since", this, null, new Object[]{new Model(){
            public Object getObject() {
                return new SimpleDateFormat(getSession().getDateTimeFormatPattern()).format(info.getDate());
            }
        }} );
        add( new Label("sinceLabel", sinceResourceModel ));
        add( new NavigationPanel("navigation", new Model(this)) );
        add( new Label("timeZoneLabel", new StringResourceModel( "page.timeZone", this, null, new Object[]{getSession().getTimeZoneId()}) ) );
    }

    public String getPageName() {
        String name = getClass().getName();

        int packageIndex = name.lastIndexOf('.');
        if ( packageIndex > 0 ) {
            name = name.substring( packageIndex + 1 );
        }

        return name;
    }

    public EmsSession getSession() {
        return (EmsSession) super.getSession();
    }

    //- PRIVATE
    
    @SuppressWarnings({"UnusedDeclaration"})
    @SpringBean
    private EmsSecurityManager securityManager;
}
