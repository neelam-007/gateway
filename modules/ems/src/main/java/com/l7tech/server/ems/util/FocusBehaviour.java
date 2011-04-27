package com.l7tech.server.ems.util;

import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.Component;

/**
 * A behavior which gives a component the default focus.
 *
 * https://issues.apache.org/jira/browse/WICKET-1404
 *
 * @auothor James Carman
 *
+ */
public class FocusBehaviour extends AbstractBehavior
{
    private Component component;

    @Override
    public void bind( Component component )
    {
        this.component = component;
        component.setOutputMarkupId(true);
    }

    @Override
    public void renderHead( IHeaderResponse iHeaderResponse )
    {
        super.renderHead(iHeaderResponse);
        iHeaderResponse.renderOnLoadJavascript("document.getElementById('" + component.getMarkupId() + "').focus();");
    }

    @Override
    public boolean isTemporary() {
        return true;
    }
}


