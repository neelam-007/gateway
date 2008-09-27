package com.l7tech.server.ems.pages;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.validation.validator.DateValidator;
import org.apache.wicket.util.value.ValueMap;

import java.util.Date;
import java.util.Calendar;

import com.l7tech.util.TimeUnit;

/**
 * Panel for collection of details for audit download.
 */
public class AuditDownloadPanel extends Panel {

    //- PUBLIC

    public AuditDownloadPanel( final String id, final Model model ) {
        super(id, model);
        setOutputMarkupId(true);

        final YuiDateSelector startDateField = new YuiDateSelector("audit.startdate", new Model(new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7))));
        final YuiDateSelector endDateField = new YuiDateSelector("audit.enddate", new Model(new Date()));

        startDateField.getDateTextField().add(DateValidator.maximum(new Date()));
        endDateField.getDateTextField().add(DateValidator.maximum(new Date()));

        Form form = new Form("audit.form"){
            @Override
            protected void onSubmit() {
                Date startDate = startOfDay((Date) startDateField.getModelObject());
                Date endDate = startOfDay((Date) endDateField.getModelObject());

                ValueMap vm = new ValueMap();
                vm.add("start", Long.toString(startDate.getTime()));
                vm.add("end", Long.toString(endDate.getTime() + TimeUnit.DAYS.toMillis(1))); // end date is inclusive

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

    //- PRIVATE

    private Date startOfDay( final Date date ) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        calendar.set(Calendar.HOUR, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar.getTime();
    }
}
