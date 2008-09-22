package com.l7tech.server.ems.pages;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IObjectClassAwareModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.validation.validator.NumberValidator;

import java.util.logging.Logger;
import java.util.logging.Level;

import com.l7tech.server.audit.AuditRecordManager;
import com.l7tech.util.TimeUnit;
import com.l7tech.objectmodel.DeleteException;

/**
 * Panel for collection of details for audit deletion.
 */
public class AuditDeletePanel extends Panel {

    //- PUBLIC

    public AuditDeletePanel( final String id ) {
        super(id);
        setOutputMarkupId(true);

        final int[] intHolder = new int[]{7};
        TextField minAgeTextField = new TextField("audit.minage", new IObjectClassAwareModel(){
            public Class getObjectClass() { return Integer.class; }
            public Object getObject() { return intHolder[0]; }
            public void setObject(Object object) { intHolder[0] = (Integer) object; }
            public void detach() { }
        });
        minAgeTextField.add( new NumberValidator.RangeValidator(7, 365) );

        Form form = new Form("audit.form"){
            @Override
            protected void onSubmit() {
                try {
                    auditRecordManager.deleteOldAuditRecords( TimeUnit.DAYS.toMillis(intHolder[0]) );
                } catch ( DeleteException de ) {
                    logger.log( Level.WARNING, "Error deleting audit data.", de );
                }
            }
        };

        form.add( minAgeTextField );

        add( form );
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(AuditDeletePanel.class.getName());

    @SuppressWarnings({"UnusedDeclaration"})
    @SpringBean
    private AuditRecordManager auditRecordManager;

}