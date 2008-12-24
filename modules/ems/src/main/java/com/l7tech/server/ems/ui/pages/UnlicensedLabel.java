package com.l7tech.server.ems.ui.pages;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.model.IModel;
import com.l7tech.gateway.common.admin.Administrative;

/**
 * Wicket label that shows up even when there is no installed license.
 */
@Administrative(licensed=false)
public class UnlicensedLabel extends Label {

    public UnlicensedLabel(String id) {
        super(id);
    }

    public UnlicensedLabel(String id, String label) {
        super(id, label);
    }

    public UnlicensedLabel(String id, IModel model) {
        super(id, model);
    }

    @Override
    protected void onRender(MarkupStream markupStream) {
        super.onRender(markupStream);
    }


}
