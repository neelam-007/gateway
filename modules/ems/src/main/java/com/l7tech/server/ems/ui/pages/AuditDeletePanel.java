package com.l7tech.server.ems.ui.pages;

import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IObjectClassAwareModel;
import javax.inject.Inject;
import org.apache.wicket.validation.validator.RangeValidator;

import java.util.logging.Logger;
import java.util.logging.Level;

import com.l7tech.server.audit.AuditRecordManager;
import com.l7tech.util.TimeUnit;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.gateway.common.security.rbac.AttemptedDeleteAll;

/**
 * Panel for collection of details for audit deletion.
 */
public class AuditDeletePanel extends Panel {

    //- PUBLIC

    public AuditDeletePanel( final String id ) {
        super(id);
        setOutputMarkupId(true);

        final Integer[] intHolder = new Integer[]{7};
        TextField<Integer> minAgeTextField = new TextField<Integer>("audit.minage", new IObjectClassAwareModel<Integer>(){
            @Override
            public Class<Integer> getObjectClass() { return Integer.class; }
            @Override
            public Integer getObject() { return intHolder[0]; }
            @Override
            public void setObject(Integer object) { intHolder[0] = object; }
            @Override
            public void detach() { }
        });
        minAgeTextField.add( new RangeValidator<Integer>(7, 365) );
        minAgeTextField.setRequired(true);

        SecureForm form = new SecureForm("audit.form",  new AttemptedDeleteAll(EntityType.AUDIT_RECORD) ){
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

    @Inject
    private AuditRecordManager auditRecordManager;

}