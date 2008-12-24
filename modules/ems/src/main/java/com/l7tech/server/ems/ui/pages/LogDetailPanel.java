package com.l7tech.server.ems.ui.pages;

import org.apache.wicket.model.Model;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.behavior.HeaderContributor;

import java.util.Date;
import java.io.File;

/**
 * Panel to show log details and content.
 */
public class LogDetailPanel extends Panel {

    //- PUBLIC

    public LogDetailPanel( final String id, final File file ) {
        super(id);
        setOutputMarkupId(true);

        add( HeaderContributor.forJavaScript( YuiCommon.RES_JS_DOM_EVENT ) );
        add( HeaderContributor.forJavaScript( YuiCommon.RES_JS_ANIMATION ) );

        add( HeaderContributor.forJavaScript( "js/l7.js" ) );

        String name = file.getName();
        Date date = new Date( file.lastModified() );
        long size = file.length();

        ResourceReference imageResource = new ResourceReference("logResource");
        final String url = RequestCycle.get().urlFor(imageResource)+"?id="+name;

        add(new Label("log.name", new Model(name)));
        add(new Label("log.date", new Model(date)));
        add(new Label("log.size", new Model(Long.toString(size))));
        add( new WebMarkupContainer("log.text", null).add(new AttributeModifier("src", new Model(url))) );
    }

}
