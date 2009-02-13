package com.l7tech.server.ems.ui.pages;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.validation.validator.PatternValidator;

import java.util.logging.Logger;
import java.io.Serializable;

/**
 * Panel for editing of policy migration mapping values.
 */
public class PolicyMigrationMappingValueEditPanel extends Panel {

    //- PUBLIC

    /**
     * Create an edit panel for a mapping value.
     *
     * @param id The identifier for the component.
     * @param descriptionText The description for the value.
     * @param sourceValue The current source value.
     * @param targetValue The current destination value.
     * @param targetPattern The validation regex for the destination value.
     */
    public PolicyMigrationMappingValueEditPanel( final String id, final String descriptionText, final String sourceValue, final String targetValue, final String targetPattern ) {
        super( id );
        model = new MigrationMappingValue( sourceValue, targetValue );

        final FeedbackPanel feedback = new FeedbackPanel( "feedback" );
        final Label description = new Label( "description", descriptionText );
        final RequiredTextField source = new RequiredTextField( "source" );
        final RequiredTextField target = new RequiredTextField( "target" );
        final CheckBox applyToAll = new CheckBox( "applyToAll" );

        target.add( new PatternValidator( targetPattern ) );

        Form mappingValueForm = new Form("mappingValueForm", new CompoundPropertyModel(model)){
            @Override
            protected void onSubmit() {
                logger.info("Processing updated mapping value.");
            }
        };

        mappingValueForm.setOutputMarkupId(true);

        mappingValueForm.add( feedback );
        mappingValueForm.add( description );
        mappingValueForm.add( source );
        mappingValueForm.add( target );
        mappingValueForm.add( applyToAll );

        add( mappingValueForm );
    }

    /**
     * Does the selection apply to all similar mappings
     *
     * @return True if this value applies to all similar mappings.
     */
    public boolean isApplyToAll() {
        return model.isApplyToAll();
    }

    /**
     * Get the updated destination value.
     *
     * @return The value.
     */
    public String getValue() {
        return model.getTarget();
    }

    //- PRIVATE
    
    private static final Logger logger = Logger.getLogger(PolicyMigrationMappingValueEditPanel.class.getName());

    private final MigrationMappingValue model;

    private static final class MigrationMappingValue implements Serializable {
        private String source;
        private String target;
        private boolean applyToAll;

        public MigrationMappingValue( final String source, final String target ) {
            this.source = source;
            this.target = target==null ? source : target;
            this.applyToAll = false;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public String getTarget() {
            return target;
        }

        public void setTarget(String target) {
            this.target = target;
        }

        public boolean isApplyToAll() {
            return applyToAll;
        }

        public void setApplyToAll(boolean applyToAll) {
            this.applyToAll = applyToAll;
        }
    }
}