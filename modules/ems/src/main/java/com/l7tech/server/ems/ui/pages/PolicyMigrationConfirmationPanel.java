package com.l7tech.server.ems.ui.pages;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.validation.validator.StringValidator;

import java.io.Serializable;

/**
 * Panel for display of policy migration confirmation.
 */
public class PolicyMigrationConfirmationPanel extends Panel {

    //- PUBLIC

    /**
     *
     */
    public PolicyMigrationConfirmationPanel( final String id, final IModel confirmationModel ) {
        super( id );
        this.model = new ConfirmationModel();

        final FeedbackPanel feedback = new FeedbackPanel("feedback");

        final TextField name = new TextField("name");
        name.add( new StringValidator.LengthBetweenValidator(0, 32) );

        Form editForm = new Form("editForm", new CompoundPropertyModel(model));

        editForm.add( feedback );
        editForm.add( name );
        editForm.add( new MultiLineLabel( "text", confirmationModel ) );

        add( editForm );
    }

    /**
     * Get the label entered for the confirmation.
     *
     * @return The label.
     */
    public String getLabel() {
        return model.getName();
    }

    //- PRIVATE

    private ConfirmationModel model;

    private static final class ConfirmationModel implements Serializable {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}