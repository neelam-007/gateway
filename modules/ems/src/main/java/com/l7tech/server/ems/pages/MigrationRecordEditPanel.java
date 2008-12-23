package com.l7tech.server.ems.pages;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.validation.validator.StringValidator;

import com.l7tech.server.ems.migration.MigrationRecord;

/**
 * Panel for edit of migration record properties
 */
public class MigrationRecordEditPanel extends Panel {

    public MigrationRecordEditPanel( final String id, final MigrationRecord record ) {
        super( id );

        final FeedbackPanel feedback = new FeedbackPanel("feedback");

        final TextField name = new TextField("name");
        name.add( new StringValidator.LengthBetweenValidator(0, 32) );

        Form editForm = new Form("editForm", new CompoundPropertyModel(record)){
            @Override
            protected void onSubmit() {
                MigrationRecordEditPanel.this.onSubmit();
            }
        };

        editForm.add( feedback );
        editForm.add( name );

        add( editForm );
    }

    /**
     * Override to customize onSubmit behaviour.
     */
    protected void onSubmit() {
    }
}