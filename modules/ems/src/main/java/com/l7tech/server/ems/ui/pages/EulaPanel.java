package com.l7tech.server.ems.ui.pages;

import com.l7tech.gateway.common.License;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

/**
 * Panel for License EULA display
 */
public class EulaPanel extends Panel {

    /**
     *
     */
    public EulaPanel(String s, IModel iModel) {
        super(s, iModel);

        String text = ((License) iModel.getObject()).getEulaText();
        add( new MultiLineLabel("eula", text) );
    }
}
