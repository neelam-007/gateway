package com.l7tech.server.ems.ui.pages;

import com.l7tech.gateway.common.security.rbac.RequiredPermissionSet;
import com.l7tech.server.ems.EsmSecurityManager;
import org.apache.wicket.Component;
import org.apache.wicket.RequestListenerInterface;
import org.apache.wicket.authorization.UnauthorizedActionException;
import org.apache.wicket.behavior.HeaderContributor;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

/**
 * Base page for EMS pages that include the standard top-level navigation tabs and controls.
 * TODO rename to something like EmsNavigationPage
 */
@RequiredPermissionSet()
public abstract class EsmPage extends EsmBaseWebPage {

    //- PUBLIC

    public EsmPage() {
        final EsmSecurityManager.LoginInfo info = getLoginInfo();
        final StringResourceModel sinceResourceModel = new StringResourceModel( "page.since", this, null, new Object[]{new Model(){
            @Override
            public Object getObject() {
                return getSession().buildDateFormat().format(info.getDate());
            }
        }} );

        Model infoModel = new Model(info);
        add( new UnlicensedLabel("titleLabel", new StringResourceModel( "page.title", this, null, new Object[]{ new StringResourceModel( "page.${pageName}.title", this, new Model(this))} )) );
        add( new UnlicensedLabel("userLabel", new StringResourceModel( "page.user", this, infoModel )) );
        add( new UnlicensedLabel("sinceLabel", sinceResourceModel ));
        add( new NavigationPanel("navigation", new Model(this), securityManager) );
    }

    public String getPageName() {
        String name = getClass().getName();

        int packageIndex = name.lastIndexOf('.');
        if ( packageIndex > 0 ) {
            name = name.substring( packageIndex + 1 );
        }

        return name;
    }

    @Override
    public void beforeCallComponent( final Component component, final RequestListenerInterface listener ) {
        if ( !securityManager.isAuthorized( component ) ) {
            throw new UnauthorizedActionException( component, Component.RENDER );
        }
    }

    //- PACKAGE

    static final String RES_CSS_SKIN = "css/l7-yui-skin.css";

    //- PROTECTED

    @Override
    protected void onBeforeRender() {
        // component is added on before render so it is the last component in the page
        // this means that the CSS overrides any styles in any other components in the page
        if ( get("timeZoneLabel") == null ) {
            add( new UnlicensedLabel("timeZoneLabel", new StringResourceModel( "page.timeZone", this, null, new Object[]{getSession().getTimeZoneId()}) ).add(
                HeaderContributor.forCss( RES_CSS_SKIN )
            ) );
        }

        // disable any secured component that the user is not permitted to access
        this.visitChildren( null, new IVisitor(){
            @Override
            public Object component(Component component) {
                if ( !securityManager.isAuthorized( component ) ) {
                    if ( component instanceof Form ||
                         component instanceof FormComponent ) {
                        component.setEnabled( false );
                    } else {
                        component.setEnabled( false );
                        component.setVisible( false );
                    }
                }
                return null;
            }
        } );

        super.onBeforeRender();
    }
}
