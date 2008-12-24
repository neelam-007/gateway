package com.l7tech.server.ems.ui.pages;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.validation.IFormValidator;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.util.value.ValueMap;

import java.util.Date;
import java.util.Calendar;
import java.util.TimeZone;

import com.l7tech.util.TimeUnit;
import com.l7tech.gateway.common.security.rbac.AttemptedReadAll;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.server.ems.ui.EsmSession;

/**
 * Panel for collection of details for audit download.
 */
public class AuditDownloadPanel extends Panel {

    //- PUBLIC

    public AuditDownloadPanel( final String id, final Model model ) {
        super(id, model);
        setOutputMarkupId(true);

        final Date now = new Date();
        Date last7thDay = new Date(now.getTime() - TimeUnit.DAYS.toMillis(7));
        final YuiDateSelector startDateField = new YuiDateSelector("audit.startdate", new Model(last7thDay), null, now);
        final YuiDateSelector endDateField = new YuiDateSelector("audit.enddate", new Model(now), last7thDay, now);

        startDateField.addInteractionWithOtherDateSelector(endDateField, false, new YuiDateSelector.InteractionTasker());
        endDateField.addInteractionWithOtherDateSelector(startDateField, true, new YuiDateSelector.InteractionTasker());

        Form form = new SecureForm("audit.form", new AttemptedReadAll(EntityType.AUDIT_RECORD)){
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

        form.add( new IFormValidator(){
            @Override
            public FormComponent[] getDependentFormComponents() {
                return new FormComponent[]{ startDateField.getDateTextField(), endDateField.getDateTextField() };
            }
            @Override
            public void validate( final Form form ) {
                Date start = (Date) startDateField.getDateTextField().getConvertedInput();
                Date end = (Date) endDateField.getDateTextField().getConvertedInput();
                if ( end.before(start) ) {
                    form.error( new StringResourceModel("message.daterange", AuditDownloadPanel.this, null).getString() );
                }
            }
        } );

        add( form );
    }

    //- PRIVATE

    private Date startOfDay( final Date date ) {
        Calendar calendar = Calendar.getInstance();
        if ( getSession() instanceof EsmSession && ((EsmSession)getSession()).getTimeZoneId() != null ) {
            calendar.setTimeZone( TimeZone.getTimeZone( ((EsmSession)getSession()).getTimeZoneId() ) );
        }
        calendar.setTime(date);

        calendar.set(Calendar.HOUR, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar.getTime();
    }
}
