package com.l7tech.server.ems.pages;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.validation.validator.DateValidator;
import org.apache.wicket.util.value.ValueMap;

import java.util.Date;

/**
 * Panel for collection of details for audit download.
 */
public class AuditDownloadPanel extends Panel {

    //- PUBLIC

    public AuditDownloadPanel( final String id, final Model model ) {
        super(id, model);
        setOutputMarkupId(true);

        final YuiDateSelector startDateField = new YuiDateSelector("audit.startdate", new Model(new Date()));
        final YuiDateSelector endDateField = new YuiDateSelector("audit.enddate", new Model(new Date()));

        startDateField.getDateTextField().add(DateValidator.maximum(new Date()));
        endDateField.getDateTextField().add(DateValidator.maximum(new Date()));

        Form form = new Form("audit.form"){
            @Override
            protected void onSubmit() {
                Date startDate = (Date) startDateField.getModelObject();
                Date endDate = (Date) endDateField.getModelObject();

                ValueMap vm = new ValueMap();
                vm.add("start", Long.toString(startDate.getTime()));
                vm.add("end", Long.toString(endDate.getTime()));

                // Set model content to download url
                ResourceReference reference = new ResourceReference("auditResource");
                String url = RequestCycle.get().urlFor(reference, vm).toString();

                model.setObject( url );
            }
        };

        form.add( startDateField );
        form.add( endDateField );

        add( form );
    }

}
