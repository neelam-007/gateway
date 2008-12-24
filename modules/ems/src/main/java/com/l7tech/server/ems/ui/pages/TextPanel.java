package com.l7tech.server.ems.ui.pages;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.model.IModel;

/**
 * Panel for preformatted text display
 */
public class TextPanel extends Panel {

    /**
     *
     */
    public TextPanel(String s, IModel iModel) {
        super(s, iModel);

        add( new MultiLineLabel("text", iModel ) );
    }
}