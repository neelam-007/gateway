package com.l7tech.server.ems.pages;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
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

        add( new TextArea("eula", new Model(((License)iModel.getObject()).getEulaText() )) );
    }
}
