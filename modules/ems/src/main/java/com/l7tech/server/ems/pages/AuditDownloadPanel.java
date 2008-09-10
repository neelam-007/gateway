package com.l7tech.server.ems.pages;

import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.form.DateTextField;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.util.value.ValueMap;
import org.apache.wicket.ajax.AjaxRequestTarget;

import java.util.Date;

/**
 * Panel for collection of details for audit download.
 */
public class AuditDownloadPanel extends Panel {

    //- PUBLIC

    public AuditDownloadPanel( final String id, final ModalWindow window ) {
        super(id);
        setOutputMarkupId(true);

        final DateTextField startDateField = new DateTextField("audit.startdate", new Model(new Date()));
        final DateTextField endDateField = new DateTextField("audit.enddate", new Model(new Date()));

        startDateField.setRequired(true);
        endDateField.setRequired(true);

        OkCancelForm form = new OkCancelForm("audit.form", "feedback", "button.ok", "button.cancel", window){
            @Override
            protected void onSubmit( final AjaxRequestTarget ajaxRequestTarget, final Form form, final ModalWindow window ) {
                super.onSubmit( ajaxRequestTarget, form, window );

                Date startDate = (Date) startDateField.getModelObject();
                Date endDate = (Date) endDateField.getModelObject();

                ValueMap vm = new ValueMap();
                vm.add("start", Long.toString(startDate.getTime()));
                vm.add("end", Long.toString(endDate.getTime()));

                // redirect uses JS so we can close the modal window first.
                ResourceReference reference = new ResourceReference("auditResource");
                String url = RequestCycle.get().urlFor(reference, vm).toString();
                ajaxRequestTarget.appendJavascript( "window.setTimeout(function() { window.location = '" + url + "'; }, 0)" );
            }
        };

        form.add( startDateField );
        form.add( endDateField );

        add( form );
    }

}
