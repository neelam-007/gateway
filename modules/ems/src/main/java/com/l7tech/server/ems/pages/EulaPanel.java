package com.l7tech.server.ems.pages;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import com.l7tech.gateway.common.License;

/**
 * Panel for License EULA display
 */
public class EulaPanel extends Panel {

    /**
     *
     */
    public EulaPanel(String s, IModel iModel) {
        super(s, iModel);

        add( new Label("eula", ((License)iModel.getObject()).getEulaText() ) );
    }
}
