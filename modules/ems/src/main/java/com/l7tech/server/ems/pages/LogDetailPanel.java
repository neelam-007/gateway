package com.l7tech.server.ems.pages;

import org.apache.wicket.model.Model;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
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

    public LogDetailPanel( final String id, final File file, final ModalWindow window ) {
        super(id);
        setOutputMarkupId(true);

        String name = file.getName();
        Date date = new Date( file.lastModified() );
        long size = file.length();

        OkCancelForm logForm = new OkCancelForm("log.form", "feedback", "button.ok", "button.cancel", window){};

        ResourceReference imageResource = new ResourceReference("logResource");
        final String url = RequestCycle.get().urlFor(imageResource)+"?id="+name;

        logForm.add(new Label("log.name", new Model(name)));
        logForm.add(new Label("log.date", new Model(date)));
        logForm.add(new Label("log.size", new Model(Long.toString(size))));
        logForm.add( new WebMarkupContainer("log.text", null).add(new AttributeModifier("src", new Model(url))) );

        add(logForm);
    }

}
