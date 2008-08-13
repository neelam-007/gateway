package com.l7tech.server.ems.pages;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.panel.FeedbackPanel;

/**
 * Error page
 */
public class EmsError extends EmsPage {

    public EmsError() {
        add( new FeedbackPanel("feedback") );        
    }

    @Override
	public boolean isVersioned() {
		return false;
	}

    @Override
    public boolean isErrorPage() {
        return true;
    }
}
