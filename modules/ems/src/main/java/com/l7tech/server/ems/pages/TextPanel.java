package com.l7tech.server.ems.pages;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import com.l7tech.gateway.common.License;

/**
 * Panel for preformatted text display
 */
public class TextPanel extends Panel {

    /**
     *
     */
    public TextPanel(String s, IModel iModel) {
        super(s, iModel);

        add( new Label("text", iModel ) );
    }
}