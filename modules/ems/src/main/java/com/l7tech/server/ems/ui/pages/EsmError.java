package com.l7tech.server.ems.ui.pages;

import com.l7tech.util.ConfigFactory;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.WebPage;
import com.l7tech.gateway.common.admin.Administrative;
import com.l7tech.gateway.common.security.rbac.RequiredPermissionSet;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Error page
 */
@RequiredPermissionSet()
@Administrative(licensed=false,authenticated=false)
public class EsmError extends WebPage {

    //- PUBLIC

    public EsmError( final Throwable thrown ) {
        String errorText = "";

        if ( thrown != null && ConfigFactory.getBooleanProperty( PROP_ENABLE_STACK, false ) ) {
            StringWriter writer = new StringWriter(4096);
            thrown.printStackTrace(new PrintWriter(writer));
            errorText = writer.toString();
        }

        add( new Label("exception", errorText).setVisible(errorText.length()>0) );
    }

    public EsmError() {
        this( null );
    }

    @Override
	public boolean isVersioned() {
		return false;
	}

    @Override
    public boolean isErrorPage() {
        return true;
    }

    //- PRIVATE

    private static final String PROP_ENABLE_STACK = "com.l7tech.ems.showExceptions";
    
}
