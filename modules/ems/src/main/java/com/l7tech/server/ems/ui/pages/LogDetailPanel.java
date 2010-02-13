package com.l7tech.server.ems.ui.pages;

import org.apache.wicket.markup.html.JavascriptPackageResource;
import org.apache.wicket.model.Model;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.AttributeModifier;

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

        add( JavascriptPackageResource.getHeaderContribution( YuiCommon.RES_JS_DOM_EVENT ) );
        add( JavascriptPackageResource.getHeaderContribution( YuiCommon.RES_JS_ANIMATION ) );

        add( JavascriptPackageResource.getHeaderContribution( "js/l7.js" ) );

        String name = file.getName();
        Date date = new Date( file.lastModified() );
        long size = file.length();

        ResourceReference imageResource = new ResourceReference("logResource");
        final String url = RequestCycle.get().urlFor(imageResource)+"?id="+name;

        add(new Label("log.name", new Model<String>(name)));
        add(new Label("log.date", new Model<Date>(date)));
        add(new Label("log.size", new Model<String>(Long.toString(size))));
        add( new WebMarkupContainer("log.text", null).add(new AttributeModifier("src", new Model<String>(url))) );
    }

}
