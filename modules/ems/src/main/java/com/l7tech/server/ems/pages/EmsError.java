package com.l7tech.server.ems.pages;

import org.apache.wicket.markup.html.basic.Label;
import com.l7tech.util.SyspropUtil;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Error page
 */
public class EmsError extends EmsPage {

    //- PUBLIC

    public EmsError( final Throwable thrown ) {
        String errorText = "";

        if ( thrown != null && SyspropUtil.getBoolean(PROP_ENABLE_STACK) ) {
            StringWriter writer = new StringWriter(4096);
            thrown.printStackTrace(new PrintWriter(writer));
            errorText = writer.toString();
        }

        add( new Label("exception", errorText).setVisible(errorText.length()>0) );
    }

    public EmsError() {
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
