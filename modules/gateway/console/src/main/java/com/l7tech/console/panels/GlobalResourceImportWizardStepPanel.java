package com.l7tech.console.panels;

import javax.swing.*;
import java.util.ResourceBundle;

/**
 * Base class for global resource import wizard steps.
 */
abstract class GlobalResourceImportWizardStepPanel extends WizardStepPanel<GlobalResourceImportContext> {

    //- PUBLIC

    @Override
    public String getStepLabel() {
        return resources.getString( stepId + ".label" );
    }

    @Override
    public String getDescription() {
        return resources.getString( stepId + ".description" );
    }

    //- PROTECTED

    protected static final ResourceBundle resources = GlobalResourceImportWizard.resources;

    @Override
    protected void setOwner( final JDialog owner ) {
        if ( !(owner instanceof GlobalResourceImportWizard) ) throw new IllegalArgumentException("Owner must be a GlobalResourceImportWizard");
        super.setOwner( owner );
    }

    //- PACKAGE

    GlobalResourceImportWizardStepPanel( final String stepId,
                                         final GlobalResourceImportWizardStepPanel next ) {
        super( next );
        this.stepId = stepId;
    }

    GlobalResourceImportWizard getWizard() {
        return (GlobalResourceImportWizard)getOwner();
    }

    GlobalResourceImportContext getContext() {
        return getWizard().getWizardInput();
    }

    //- PRIVATE

    private String stepId;
}
