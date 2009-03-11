package com.l7tech.server.ems.ui.pages;

import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.server.ems.enterprise.*;
import com.l7tech.server.ems.gateway.*;
import com.l7tech.server.ems.migration.*;
import com.l7tech.server.ems.ui.NavigationPage;
import com.l7tech.server.ems.util.TypedPropertyColumn;
import com.l7tech.server.management.api.node.GatewayApi;
import com.l7tech.server.management.api.node.MigrationApi;
import com.l7tech.server.management.migration.bundle.MigratedItem;
import com.l7tech.server.management.migration.bundle.MigrationBundle;
import com.l7tech.server.management.migration.bundle.MigrationMetadata;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Pair;
import com.l7tech.util.TextUtils;
import com.l7tech.util.ValidationUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormSubmitBehavior;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.HiddenField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.mortbay.util.ajax.JSON;

import javax.xml.ws.soap.SOAPFaultException;
import java.io.Serializable;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
@NavigationPage(page="PolicyMigration",section="ManagePolicies",pageUrl="PolicyMigration.html")
public class PolicyMigration extends EsmStandardWebPage {

    //- PUBLIC

    public PolicyMigration() {
        final EntityMappingModel mappingModel = new EntityMappingModel();

        final WebMarkupContainer dialogContainer = new WebMarkupContainer("dialogContainer");
        dialogContainer.add( new EmptyPanel("dialog") );
        add( dialogContainer.setOutputMarkupId(true) );

        final WebMarkupContainer dependenciesContainer = new WebMarkupContainer("dependencies");
        dependenciesContainer.setOutputMarkupPlaceholderTag(true);
        final WebMarkupContainer dependencyCandidateContainer = new WebMarkupContainer("dependencyCandidateContainer");
        final Form dependenciesOptionsContainer = new Form("dependencyOptionsContainer");
        final WebMarkupContainer[] dependencyRefreshContainers = { dependencyCandidateContainer, dependenciesOptionsContainer };

        YuiDataTable.contributeHeaders(this);

        final WebMarkupContainer srcItemDependencies = new WebMarkupContainer("srcItemDependencies");
        add( srcItemDependencies.setOutputMarkupId(true) );
        final WebMarkupContainer srcItemDetails = new WebMarkupContainer("srcItemDetails");
        add( srcItemDetails.setOutputMarkupId(true) );

        showDependencies( srcItemDependencies, Collections.<DependencyItem>emptyList(), srcItemDetails, null );
        srcItemDependencies.add( buildDependencyDisplayBehaviour(srcItemDependencies, srcItemDetails, "srcItemSelectionCallbackUrl", mappingModel) );

        final WebMarkupContainer destItemDependencies = new WebMarkupContainer("destItemDependencies");
        add( destItemDependencies.setOutputMarkupId(true) );
        final WebMarkupContainer destItemDetails = new WebMarkupContainer("destItemDetails");
        add( destItemDetails.setOutputMarkupId(true) );
        showDependencies( destItemDependencies, Collections.<DependencyItem>emptyList(), destItemDetails, null );
        destItemDependencies.add( buildDependencyDisplayBehaviour(destItemDependencies, destItemDetails, "destItemSelectionCallbackUrl", null ) );

        final DepenencySummaryModel dependencySummaryModel = new DepenencySummaryModel();
        final CandidateModel candidateModel = new CandidateModel();
        final SearchModel searchModel = new SearchModel();

        final Label javascript = new Label("javascript", "");
        final YuiAjaxButton reloadMigrationButton = new YuiAjaxButton("reloadMigrationButton") {
            @Override
            protected void onSubmit( final AjaxRequestTarget ajaxRequestTarget, final Form form ) {
                final PreviousMigrationModel model = (PreviousMigrationModel) form.get("reloadSelect").getModelObject();
                logger.fine( "Reloading migration '" + model + "'.");
                try {
                    MigrationRecord record = migrationRecordManager.findByPrimaryKey( model.id );
                    if ( record != null ) {
                        MigrationSummary summary = record.getMigrationSummary();

                        // Update model
                        List<Pair<ExternalEntityHeader,ExternalEntityHeader>> mappings = new ArrayList<Pair<ExternalEntityHeader,ExternalEntityHeader>>();
                        for ( MigratedItem item : summary.getMigratedItems() ) {
                            if ( item.getSourceHeader().getType() == EntityType.FOLDER ) {
                                continue;
                            }

                            ExternalEntityHeader source = item.getSourceHeader();
                            ExternalEntityHeader target = item.getTargetHeader();
                            if (item.getOperation().modifiesTarget()) {
                                if ( source.getMappedValue() != null ) {
                                    mappingModel.valueMap.put( new Pair<ValueKey,String>(new ValueKey( record.getSourceClusterGuid(), source ), record.getTargetClusterGuid()), source.getMappedValue() );
                                }
                            } else {
                                mappings.add( new Pair<ExternalEntityHeader,ExternalEntityHeader>( source, target ) );
                            }
                        }
                        validateAndRestoreMappings( mappingModel, record.getSourceClusterGuid(), record.getTargetClusterGuid(), mappings );

                        // Update UI selections
                        javascript.setModelObject("selectClusters( '"+record.getSourceClusterGuid()+"', "+jsArray(record.getSourceItems())+", '"+record.getTargetClusterGuid()+"', '"+record.getTargetFolderId()+"', "+summary.isMigrateFolders()+", "+summary.isEnableNewServices()+", "+summary.isOverwrite()+");");
                        ajaxRequestTarget.addComponent( javascript );

                        // Clear dependencies mapping section
                        lastSourceKey = null;
                        lastSourceClusterId = null;
                        lastTargetClusterId = null;
                        lastDependencyItems = Collections.emptyList();
                        updateDependencies( dependenciesContainer, dependencyRefreshContainers, candidateModel, searchModel, mappingModel, dependencySummaryModel );
                        addDependencyOptions( dependenciesContainer, dependencyRefreshContainers, candidateModel, searchModel, mappingModel, dependencySummaryModel, true );
                        ajaxRequestTarget.addComponent( dependenciesContainer );
                    }
                } catch ( FindException fe ) {
                    logger.log( Level.WARNING, "Unexpected error when loading previous migration.", fe );
                    String failureMessage = ExceptionUtils.getMessage(fe);
                    YuiDialog resultDialog = new YuiDialog("dialog", "Error Loading Previous Migration", YuiDialog.Style.CLOSE, new Label(YuiDialog.getContentId(), failureMessage), null);
                    dialogContainer.replace( resultDialog );
                    ajaxRequestTarget.addComponent( dialogContainer );
                }
            }
        };

        List<PreviousMigrationModel> previous = loadPreviousMigrations();
        final Form reloadForm = new Form("reloadForm");
        reloadForm.add( reloadMigrationButton.setOutputMarkupId(true).setEnabled(!previous.isEmpty()) );
        reloadForm.add( new DropDownChoice( "reloadSelect", new Model(previous.isEmpty() ? null : previous.iterator().next()), previous ){
            @Override
            protected String getDefaultChoice( final Object selected ) {
                return "-";
            }
        } );
        add( reloadForm );
        add( javascript.setOutputMarkupId(true).setEscapeModelStrings(false) );

        final WebMarkupContainer container = new WebMarkupContainer("refresh");
        container.add( new AjaxEventBehavior("onclick"){
            @Override
            protected void onEvent( final AjaxRequestTarget target ) {
                List<PreviousMigrationModel> previous = loadPreviousMigrations();
                DropDownChoice reloadDropDown = (DropDownChoice) reloadForm.get("reloadSelect");
                reloadDropDown.setChoices( previous );
                reloadDropDown.setModelObject( previous.isEmpty() ? null : previous.iterator().next() );                
                reloadMigrationButton.setEnabled(!previous.isEmpty());

                target.addComponent( reloadForm );
            }
        } );
        add( container );

        final YuiAjaxButton dependencyLoadButton = new YuiAjaxButton("dependencyLoadButton") {
            @Override
            protected void onSubmit( final AjaxRequestTarget ajaxRequestTarget, final Form form ) {
                final String sourceClusterId = lastSourceClusterId;
                final String targetClusterId = lastTargetClusterId;
                final Collection<DependencyItem> items =  lastDependencyItems;

                try {
                    final int before = countUnMappedDependencies( mappingModel, sourceClusterId, targetClusterId, items  );
                    loadMappings( mappingModel, sourceClusterId, targetClusterId, items, false );
                    final int after = countUnMappedDependencies( mappingModel, sourceClusterId, targetClusterId, items  );
                    if ( before != after ) {
                        updateDependencies( dependenciesContainer, dependencyRefreshContainers, candidateModel, searchModel, mappingModel, dependencySummaryModel );
                        addDependencyOptions( dependenciesContainer, dependencyRefreshContainers, candidateModel, searchModel, mappingModel, dependencySummaryModel, true );

                        YuiDialog resultDialog = new YuiDialog("dialog", "Loaded Previous Mappings", YuiDialog.Style.CLOSE, new Label(YuiDialog.getContentId(), "Loaded " +(before-after)+ " previous mappings."), null);
                        dialogContainer.replace( resultDialog );
                        ajaxRequestTarget.addComponent( dialogContainer );
                        ajaxRequestTarget.addComponent( dependenciesContainer );
                    } else {
                        YuiDialog resultDialog = new YuiDialog("dialog", "Previous Mappings Not Loaded", YuiDialog.Style.CLOSE, new Label(YuiDialog.getContentId(), "No previous mappings were found."), null);
                        dialogContainer.replace( resultDialog );
                        ajaxRequestTarget.addComponent( dialogContainer );
                    }
                } catch ( FindException fe ) {
                    logger.log( Level.WARNING, "Unexpected error when loading previous mappings.", fe );
                    String failureMessage = ExceptionUtils.getMessage(fe);
                    YuiDialog resultDialog = new YuiDialog("dialog", "Error Loading Previous Mappings", YuiDialog.Style.CLOSE, new Label(YuiDialog.getContentId(), failureMessage), null);
                    dialogContainer.replace( resultDialog );
                    ajaxRequestTarget.addComponent( dialogContainer );
                } catch ( GatewayException ge ) {
                    String failureMessage = ExceptionUtils.getMessage(ge);
                    YuiDialog resultDialog = new YuiDialog("dialog", "Error Loading Previous Mappings", YuiDialog.Style.CLOSE, new Label(YuiDialog.getContentId(), failureMessage), null);
                    dialogContainer.replace( resultDialog );
                    ajaxRequestTarget.addComponent( dialogContainer );
                } catch ( SOAPFaultException e ) {
                    String failureMessage;
                    if ( GatewayContext.isNetworkException( e ) ) {
                        failureMessage = "Could not connect to cluster.";
                    } else if ( GatewayContext.isConfigurationException( e ) ) {
                        failureMessage = "Could not connect to cluster.";
                    } else {
                        failureMessage = "Unexpected error from cluster.";
                        logger.log( Level.WARNING, "Error loading previous mappings '"+ExceptionUtils.getMessage(e)+"'.", ExceptionUtils.getDebugException(e));
                    }
                    YuiDialog resultDialog = new YuiDialog("dialog", "Error Loading Previous Mappings", YuiDialog.Style.CLOSE, new Label(YuiDialog.getContentId(), failureMessage), null);
                    dialogContainer.replace( resultDialog );
                    ajaxRequestTarget.addComponent( dialogContainer );
                }
            }
        };

        final YuiAjaxButton clearDependencyButton = new YuiAjaxButton("dependencyClearButton") {
            @Override
            protected void onSubmit( final AjaxRequestTarget ajaxRequestTarget, final Form form ) {
                final Pair<DependencyKey,String> mappingKey = new Pair<DependencyKey,String>(lastSourceKey, lastTargetClusterId);
                final Pair<ValueKey,String> valueKey = lastSourceKey.asEntityHeader().isValueMappable() ?
                    new Pair<ValueKey,String>(new ValueKey(lastSourceKey), lastTargetClusterId):
                    null;

                mappingModel.dependencyMap.remove( mappingKey );
                if( valueKey != null ) mappingModel.valueMap.remove( valueKey );
                updateDependencies( dependenciesContainer, dependencyRefreshContainers, candidateModel, searchModel, mappingModel, dependencySummaryModel );
                setEnabled(false);
                for ( String id : DEPENDENCY_REFRESH_COMPONENTS ) ajaxRequestTarget.addComponent( dependenciesContainer.get(id) );
                ajaxRequestTarget.addComponent( dependencyCandidateContainer );
                ajaxRequestTarget.addComponent( dependenciesOptionsContainer );
            }
        };

        final YuiAjaxButton editDependencyButton = new YuiAjaxButton("dependencyEditButton") {
            @Override
            protected void onSubmit( final AjaxRequestTarget ajaxRequestTarget, final Form form ) {
                if ( lastSourceKey != null && lastSourceKey.header.isValueMappable()) {
                    ExternalEntityHeader.ValueType valueType = lastSourceKey.header.getValueType();
                    String displayValue = lastSourceKey.header.getDisplayValue();
                    String mappedValue = lastSourceKey.header.getMappedValue();
                    if ( displayValue == null ) displayValue = "";

                    String prompt = "Enter new value.";
                    String regex = "^(?:.{1,1024})$";
                    switch ( valueType ) {
                        case HTTP_URL:
                            prompt = "Enter HTTP(S) URL.";
                            regex = ValidationUtils.getHttpUrlRegex();
                            break;
                        case IP_ADDRESS:
                            prompt = "Enter IP address.";
                            regex = "^(?:(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?))$";
                            break;
                    }

                    final PolicyMigrationMappingValueEditPanel mapping = new PolicyMigrationMappingValueEditPanel(YuiDialog.getContentId(), prompt, displayValue, mappedValue, regex);
                    YuiDialog resultDialog = new YuiDialog("dialog", "Edit Mapping Value", YuiDialog.Style.OK_CANCEL, mapping, new YuiDialog.OkCancelCallback(){
                        @Override
                        public void onAction( final YuiDialog dialog, final AjaxRequestTarget ajaxRequestTarget, final YuiDialog.Button button ) {
                            if ( button == YuiDialog.Button.OK ) {
                                lastSourceKey.header.setMappedValue(mapping.getValue());
                                final Collection<DependencyItem> items = lastDependencyItems;
                                DependencyItem updatedItem = null;
                                if ( items != null ) {
                                    for ( DependencyItem item : items ) {
                                        if ( item.type.equals(lastSourceKey.header.getType().toString()) && item.id.equals(lastSourceKey.header.getExternalId()) ) {
                                            mappingModel.valueMap.put( new Pair<ValueKey,String>(new ValueKey(lastSourceKey), lastTargetClusterId), mapping.getValue() );
                                            updatedItem = item;
                                            break;
                                        }
                                    }
                                }

                                if ( updatedItem != null ) {
                                    for ( DependencyItem item : items ) {
                                        if ( item != updatedItem && isValueMappingSameOwnerAndTypeAndValue(item, updatedItem) ||
                                             (isValueMappingSameTypeAndValue(item, updatedItem) && mapping.isApplyToAll())) {
                                            mappingModel.valueMap.put( new Pair<ValueKey,String>( new ValueKey(lastSourceKey.clusterId, item.asEntityHeader()), lastTargetClusterId), mapping.getValue() );
                                        }
                                    }
                                }

                                updateDependencies( dependenciesContainer, dependencyRefreshContainers, candidateModel, searchModel, mappingModel, dependencySummaryModel );
                                for ( String id : DEPENDENCY_REFRESH_COMPONENTS ) ajaxRequestTarget.addComponent( dependenciesContainer.get(id) );
                            }
                        }
                    });
                    dialogContainer.replace( resultDialog );
                    ajaxRequestTarget.addComponent( dialogContainer );
                }
            }
        };

        Form dependencyControlsForm = new Form("dependencyControlsForm");
        dependencyControlsForm.add( dependencyLoadButton.setOutputMarkupId(true).setEnabled(false) );
        dependencyControlsForm.add( clearDependencyButton.setOutputMarkupId(true).setEnabled(false) );
        dependencyControlsForm.add( editDependencyButton.setOutputMarkupId(true).setEnabled(false) );

        Form selectionJsonForm = new Form("selectionForm");
        final HiddenField hiddenSelectionForm = new HiddenField("selectionJson", new Model(""));
        final HiddenField hiddenDestClusterId = new HiddenField("destinationClusterId", new Model(""));
        final HiddenField hiddenDestFolderId = new HiddenField("destinationFolderId", new Model(""));
        final HiddenField hiddenMigrateFolders = new HiddenField("migrateFolders", new Model(""));
        final HiddenField hiddenEnableNewServices = new HiddenField("enableNewServices", new Model(""));
        final HiddenField hiddenOverwriteDependencies = new HiddenField("overwriteDependencies", new Model(""));
        selectionJsonForm.add( hiddenSelectionForm );
        selectionJsonForm.add( hiddenDestClusterId );
        selectionJsonForm.add( hiddenDestFolderId );
        selectionJsonForm.add( hiddenMigrateFolders );
        selectionJsonForm.add( hiddenEnableNewServices );
        selectionJsonForm.add( hiddenOverwriteDependencies );
        AjaxFormSubmitBehavior dependenciesBehaviour = new AjaxFormSubmitBehavior(selectionJsonForm, "onclick"){
            @Override
            protected void onSubmit(final AjaxRequestTarget target) {
                final String value = hiddenSelectionForm.getModelObjectAsString();
                lastTargetClusterId = hiddenDestClusterId.getModelObjectAsString();
                try {
                    String jsonData = value.replaceAll("&quot;", "\"");

                    Map jsonMap = (Map) JSON.parse(jsonData);
                    final DependencyItemsRequest dir = new DependencyItemsRequest();
                    dir.fromJSON(jsonMap);
                    lastSourceClusterId = dir.clusterId;

                    final String targetClusterId = lastTargetClusterId;
                    final Collection<DependencyItem> deps = retrieveDependencies(dir, null, null);

                    lastDependencyItems = deps;
                    lastSourceKey = null;
                    candidateModel.reset();
                    dependencyLoadButton.setEnabled(true);

                    updateDependencies( dependenciesContainer, dependencyRefreshContainers, candidateModel, searchModel, mappingModel, dependencySummaryModel );
                    addDependencyOptions( dependenciesContainer, dependencyRefreshContainers, candidateModel, searchModel, mappingModel, dependencySummaryModel, true );
                    target.addComponent( dependenciesContainer );

                    // build info dialog
                    Label label = new Label(YuiDialog.getContentId(), summarize(dir, targetClusterId, visible(deps,true), mappingModel));
                    label.setEscapeModelStrings(false);
                    YuiDialog dialog = new YuiDialog("dialog", "Identified Dependencies", YuiDialog.Style.CLOSE, label, null);
                    dialogContainer.replace( dialog );
                    target.addComponent( dialogContainer );
                } catch ( FindException fe ) {
                    logger.log( Level.WARNING, "Unexpected error when processing selection.", fe );
                    String failureMessage = ExceptionUtils.getMessage(fe);
                    YuiDialog resultDialog = new YuiDialog("dialog", "Error Identifying Dependencies", YuiDialog.Style.CLOSE, new Label(YuiDialog.getContentId(), failureMessage), null);
                    dialogContainer.replace( resultDialog );
                    target.addComponent( dialogContainer );
                } catch ( GatewayException ge ) {
                    String failureMessage = ExceptionUtils.getMessage(ge);
                    YuiDialog resultDialog = new YuiDialog("dialog", "Error Identifying Dependencies", YuiDialog.Style.CLOSE, new Label(YuiDialog.getContentId(), failureMessage), null);
                    dialogContainer.replace( resultDialog );
                    target.addComponent( dialogContainer );
                } catch ( MigrationApi.MigrationException mfe ) {
                    String failureMessage = mfe.getMessage();
                    YuiDialog resultDialog;
                    if ( failureMessage != null && failureMessage.indexOf('\n') < 0 ) {
                        resultDialog = new YuiDialog("dialog", "Error Identifying Dependencies", YuiDialog.Style.CLOSE, new Label(YuiDialog.getContentId(), failureMessage), null);
                    } else {
                        resultDialog = new YuiDialog("dialog", " Error Identifying Dependencies", YuiDialog.Style.CLOSE, new TextPanel(YuiDialog.getContentId(), new Model(failureMessage)), null, "600px");
                    }
                    dialogContainer.replace( resultDialog );
                    target.addComponent( dialogContainer );
                } catch ( SOAPFaultException e ) {
                    String failureMessage;
                    if ( GatewayContext.isNetworkException( e ) ) {
                        failureMessage = "Could not connect to cluster.";
                    } else if ( GatewayContext.isConfigurationException( e ) ) {
                        failureMessage = "Could not connect to cluster.";
                    } else {
                        failureMessage = "Unexpected error from cluster.";
                        logger.log( Level.WARNING, "Error identifiying dependencies '"+ExceptionUtils.getMessage(e)+"'.", ExceptionUtils.getDebugException(e));
                    }
                    YuiDialog resultDialog = new YuiDialog("dialog", "Error Identifying Dependencies", YuiDialog.Style.CLOSE, new Label(YuiDialog.getContentId(), failureMessage), null);
                    dialogContainer.replace( resultDialog );
                    target.addComponent( dialogContainer );
                }
            }
            @Override
            protected void onError(final AjaxRequestTarget target) {
            }
        };
        WebComponent image = new WebComponent("identifyDependenciesImage");
        image.setMarkupId("identifyDependenciesImage");
        image.add(dependenciesBehaviour);
        add(image);

        AjaxFormSubmitBehavior migrateBehaviour = new AjaxFormSubmitBehavior(selectionJsonForm, "onclick"){
            @Override
            protected void onSubmit(final AjaxRequestTarget target) {
                final String value = hiddenSelectionForm.getModelObjectAsString();
                final String targetClusterId = hiddenDestClusterId.getModelObjectAsString();
                final String targetFolderId = hiddenDestFolderId.getModelObjectAsString();
                final String migrateFolders = hiddenMigrateFolders.getModelObjectAsString();
                final String enableNewServices = hiddenEnableNewServices.getModelObjectAsString();
                final String overwriteDependencies = hiddenOverwriteDependencies.getModelObjectAsString();
                try {
                    String jsonData = value.replaceAll("&quot;", "\"");

                    Map jsonMap = (Map) JSON.parse(jsonData);
                    final DependencyItemsRequest dir = new DependencyItemsRequest();
                    dir.fromJSON(jsonMap);

                    try {
                        String dependencyValidationMessage = validateDependencies( dir.clusterId, targetClusterId, dir, mappingModel );
                        if ( dependencyValidationMessage != null && !dependencyValidationMessage.isEmpty() ) {
                            TextPanel textPanel = new TextPanel(YuiDialog.getContentId(), new Model(dependencyValidationMessage));
                            YuiDialog dialog = new YuiDialog("dialog", "Dependency Mapping Required", YuiDialog.Style.CLOSE, textPanel, null, "600px");
                            dialogContainer.replace( dialog );
                            target.addComponent( dialogContainer );
                        } else {
                            final boolean folders = Boolean.valueOf(migrateFolders);
                            final boolean enableServices = Boolean.valueOf(enableNewServices);
                            final boolean overwrite = Boolean.valueOf(overwriteDependencies);

                            // load mappings for top-level items that have been previously migrated
                            loadMappings( mappingModel, dir.clusterId, targetClusterId, retrieveDependencies(dir, null, null), true);
                            final PolicyMigrationConfirmationPanel confirmationPanel = new PolicyMigrationConfirmationPanel(YuiDialog.getContentId(), new Model(performMigration( dir.clusterId, targetClusterId, targetFolderId, folders, enableServices, overwrite, dir, mappingModel, "", true )));
                            YuiDialog dialog = new YuiDialog("dialog", "Confirm Migration", YuiDialog.Style.OK_CANCEL, confirmationPanel, new YuiDialog.OkCancelCallback(){
                                @Override
                                public void onAction( final YuiDialog dialog, final AjaxRequestTarget target, final YuiDialog.Button button) {
                                    if ( button == YuiDialog.Button.OK ) {
                                        logger.fine("Migration confirmed.");
                                        try {
                                            String message = performMigration( dir.clusterId, targetClusterId, targetFolderId, folders, enableServices, overwrite, dir, mappingModel, confirmationPanel.getLabel(), false );
                                            YuiDialog resultDialog = new YuiDialog("dialog", "Migration Result", YuiDialog.Style.CLOSE, new TextPanel(YuiDialog.getContentId(), new Model(message)), null, "600px");
                                            dialogContainer.replace( resultDialog );
                                        } catch ( MigrationFailedException mfe ) {
                                            String failureMessage = mfe.getMessage();
                                            YuiDialog resultDialog;
                                            if ( failureMessage != null && failureMessage.indexOf('\n') < 0 ) {
                                                resultDialog = new YuiDialog("dialog", "Migration Error", YuiDialog.Style.CLOSE, new Label(YuiDialog.getContentId(), failureMessage), null);
                                            } else {
                                                resultDialog = new YuiDialog("dialog", "Migration Error", YuiDialog.Style.CLOSE, new TextPanel(YuiDialog.getContentId(), new Model(failureMessage)), null, "600px");
                                            }
                                            dialogContainer.replace( resultDialog );
                                            target.addComponent( dialogContainer );
                                        } catch ( SOAPFaultException e ) {
                                            String failureMessage;
                                            if ( GatewayContext.isNetworkException( e ) ) {
                                                failureMessage = "Could not connect to cluster.";
                                            } else if ( GatewayContext.isConfigurationException( e ) ) {
                                                failureMessage = "Could not connect to cluster.";
                                            } else {
                                                failureMessage = "Unexpected error from cluster.";
                                                logger.log( Level.WARNING, "Error processing selection '"+ExceptionUtils.getMessage(e)+"'.", ExceptionUtils.getDebugException(e));
                                            }
                                            YuiDialog resultDialog = new YuiDialog("dialog", "Migration Error", YuiDialog.Style.CLOSE, new Label(YuiDialog.getContentId(), failureMessage), null);
                                            dialogContainer.replace( resultDialog );
                                            target.addComponent( dialogContainer );
                                        }
                                    } else {
                                        dialogContainer.replace( new EmptyPanel("dialog") );
                                    }
                                    target.addComponent(dialogContainer);
                                }
                            }, "600px");

                            dialogContainer.replace( dialog );
                            target.addComponent( dialogContainer );
                        }
                    } catch ( FindException fe ) {
                        String failureMessage = ExceptionUtils.getMessage(fe);
                        YuiDialog resultDialog = new YuiDialog("dialog", "Error Loading Previous Mappings", YuiDialog.Style.CLOSE, new Label(YuiDialog.getContentId(), failureMessage), null);
                        dialogContainer.replace( resultDialog );
                        target.addComponent( dialogContainer );
                    } catch ( GatewayException ge ) {
                        String failureMessage = ExceptionUtils.getMessage(ge);
                        YuiDialog resultDialog = new YuiDialog("dialog", "Error Loading Previous Mappings", YuiDialog.Style.CLOSE, new Label(YuiDialog.getContentId(), failureMessage), null);
                        dialogContainer.replace( resultDialog );
                        target.addComponent( dialogContainer );
                    } catch (MigrationApi.MigrationException mae) {
                        String failureMessage = ExceptionUtils.getMessage(mae);
                        YuiDialog resultDialog = new YuiDialog("dialog", "Error Retrieving Dependencies", YuiDialog.Style.CLOSE, new Label(YuiDialog.getContentId(), failureMessage), null);
                        dialogContainer.replace( resultDialog );
                        target.addComponent( dialogContainer );
                    } catch ( MigrationFailedException mfe ) {
                        String failureMessage = mfe.getMessage();
                        YuiDialog resultDialog;
                        if ( failureMessage != null && failureMessage.indexOf('\n') < 0 ) {
                            resultDialog = new YuiDialog("dialog", "Migration Error", YuiDialog.Style.CLOSE, new Label(YuiDialog.getContentId(), failureMessage), null);
                        } else {
                            resultDialog = new YuiDialog("dialog", "Migration Error", YuiDialog.Style.CLOSE, new TextPanel(YuiDialog.getContentId(), new Model(failureMessage)), null, "600px");
                        }
                        dialogContainer.replace( resultDialog );
                        target.addComponent( dialogContainer );
                    }
                } catch ( SOAPFaultException e ) {
                    String failureMessage;
                    if ( GatewayContext.isNetworkException( e ) ) {
                        failureMessage = "Could not connect to cluster.";
                    } else if ( GatewayContext.isConfigurationException( e ) ) {
                        failureMessage = "Could not connect to cluster.";
                    } else {
                        failureMessage = "Unexpected error from cluster.";
                        logger.log( Level.WARNING, "Error during migration '"+ExceptionUtils.getMessage(e)+"'.", ExceptionUtils.getDebugException(e));
                    }
                    YuiDialog resultDialog = new YuiDialog("dialog", "Migration Error", YuiDialog.Style.CLOSE, new Label(YuiDialog.getContentId(), failureMessage), null);
                    dialogContainer.replace( resultDialog );
                    target.addComponent( dialogContainer );
                }
            }
            @Override
            protected void onError(final AjaxRequestTarget target) {
            }
        };
        WebComponent migrateImage = new WebComponent("migrateImage");
        migrateImage.setMarkupId("migrateImage");
        migrateImage.add(migrateBehaviour);
        add(migrateImage);

        final Form getGatewayTrustServletInputsForm = new JsonDataResponseForm("getGatewayTrustServletInputsForm"){
            @Override
            protected Object getJsonResponseData() {
                try {
                    logger.fine("Responding to request for trust token.");
                    final String token = gatewayTrustTokenFactory.getTrustToken();
                    return new JSONSupport() {
                        @Override
                        protected void writeJson() {
                            add("token", token);
                        }
                    };
                } catch (Exception e) {
                    logger.warning(e.toString());
                    return new JSONException(e);
                }
            }
        };

        add( selectionJsonForm );
        add( getGatewayTrustServletInputsForm );

        dependenciesContainer.add( new Label("dependenciesTotalLabel", new PropertyModel(dependencySummaryModel, "totalDependencies")).setOutputMarkupId(true) );
        dependenciesContainer.add( new Label("dependenciesUnmappedLabel", new PropertyModel(dependencySummaryModel, "unmappedDependencies")).setOutputMarkupId(true) );
        dependenciesContainer.add( new Label("dependenciesRequiredUnmappedLabel", new PropertyModel(dependencySummaryModel, "requiredUnmappedDependencies")).setOutputMarkupId(true) );
        dependenciesContainer.add( dependencyControlsForm.setOutputMarkupId(true) );

        updateDependencies( dependenciesContainer, dependencyRefreshContainers, candidateModel, searchModel, mappingModel, dependencySummaryModel );

        dependenciesContainer.add( dependencyCandidateContainer.setOutputMarkupId(true) );
        dependenciesContainer.add( dependenciesOptionsContainer.setOutputMarkupId(true) );

        dependenciesOptionsContainer.add(new DropDownChoice("dependencySearchTarget", new PropertyModel(searchModel, "searchTarget"), Arrays.asList(new SearchTarget())));
        String[] searchManners = new String[] {
            "contains",
            "starts with"
        };
        dependenciesOptionsContainer.add(new DropDownChoice("dependencySearchManner", new PropertyModel(searchModel, "searchManner"), Arrays.asList(searchManners)) {
            @Override
            protected CharSequence getDefaultChoice(Object o) {
                return "contains";
            }
        });
        dependenciesOptionsContainer.add(new TextField("dependencySearchText", new PropertyModel(searchModel, "searchValue")));
        dependenciesOptionsContainer.add(new YuiAjaxButton("dependencySearchButton") {
            @Override
            protected void onSubmit( final AjaxRequestTarget ajaxRequestTarget, final Form form ) {
                logger.fine("Searching for dependencies with filter '"+searchModel.getSearchFilter()+"'.");
                addDependencyOptions( dependenciesContainer, dependencyRefreshContainers, candidateModel, searchModel, mappingModel, dependencySummaryModel, false );
                ajaxRequestTarget.addComponent( dependenciesOptionsContainer );
            }
        });
        dependencyCandidateContainer.add( new Label("dependencyCandidateName", new PropertyModel(candidateModel, "name")) );
        dependencyCandidateContainer.add( new Label("dependencyCandidateType", new PropertyModel(candidateModel, "type")) );

        addDependencyOptions( dependenciesContainer, dependencyRefreshContainers, candidateModel, searchModel, mappingModel, dependencySummaryModel, true );

        add( dependenciesContainer.setOutputMarkupId(true) );
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( PolicyMigration.class.getName() );

    private static final String[] EXTRA_PROPERTIES = new String[]{ "Policy Revision", "SOAP", "Enabled" };
    private static final String[] DEPENDENCY_REFRESH_COMPONENTS = { "dependenciesTable", "dependenciesTotalLabel", "dependenciesUnmappedLabel", "dependenciesRequiredUnmappedLabel", "dependencyControlsForm" };
    private static final String[] SEARCH_REFRESH_COMPONENTS = { "dependencySearchTarget", "dependencySearchManner", "dependencySearchText", "dependencySearchButton" };

    @SpringBean
    private SsgClusterManager ssgClusterManager;

    @SpringBean
    private MigrationRecordManager migrationRecordManager;

    @SpringBean
    private MigrationMappingRecordManager migrationMappingRecordManager;

    @SpringBean
    private GatewayClusterClientManager gatewayClusterClientManager;

    @SpringBean
    private GatewayTrustTokenFactory gatewayTrustTokenFactory;

    private DependencyKey lastSourceKey;
    private String lastSourceClusterId;
    private String lastTargetClusterId;
    private Collection<DependencyItem> lastDependencyItems = Collections.emptyList();

    private static String truncateDisplayValue( final String text ) {
        return TextUtils.truncStringMiddleExact( text, 128 );
    }

    private static boolean isValueMappingSameTypeAndValue(final DependencyItem item1,
                                                          final DependencyItem item2) {
        boolean same = false;

        ExternalEntityHeader eeh1 = item1.asEntityHeader();
        ExternalEntityHeader eeh2 = item2.asEntityHeader();

        if (eeh1.isValueMappable() && eeh2.isValueMappable()) {
            ExternalEntityHeader.ValueType valueType1 = eeh1.getValueType();
            String sourceValue1 = eeh1.getDisplayValue();

            ExternalEntityHeader.ValueType valueType2 = eeh2.getValueType();
            String sourceValue2 = eeh2.getDisplayValue();

            if (valueType1 == valueType2 && sourceValue1 != null && sourceValue1.equals(sourceValue2)) {
                same = true;
            }
        }
        return same;
    }

    private static boolean isValueMappingSameOwnerAndTypeAndValue( final DependencyItem item1,
                                                                   final DependencyItem item2 ) {
        boolean same = false;

        ExternalEntityHeader eeh1 = item1.asEntityHeader();
        ExternalEntityHeader eeh2 = item2.asEntityHeader();
        if ( eeh1 instanceof ValueReferenceEntityHeader && eeh2 instanceof ValueReferenceEntityHeader &&
             eeh1.isValueMappable() && eeh2.isValueMappable()) {
            ValueReferenceEntityHeader vreh1 = (ValueReferenceEntityHeader) eeh1;
            ValueReferenceEntityHeader vreh2 = (ValueReferenceEntityHeader) eeh2;

            EntityType type1 = vreh1.getOwnerType();
            String id1 = vreh1.getOwnerId();
            ExternalEntityHeader.ValueType valueType1 = vreh1.getValueType();
            String sourceValue1 = vreh1.getDisplayValue();

            EntityType type2 = vreh2.getOwnerType();
            String id2 = vreh2.getOwnerId();
            ExternalEntityHeader.ValueType valueType2 = vreh2.getValueType();
            String sourceValue2 = vreh2.getDisplayValue();

            if ( type1 == type2 && id1 != null && id1.equals(id2) && valueType1 == valueType2 && sourceValue1 != null && sourceValue1.equals(sourceValue2) ) {
                same = true;
            }
        }

        return same;
    }

    private static final class PreviousMigrationModel implements Comparable, Serializable {
        private final String label;
        private final long id;

        public PreviousMigrationModel( final long id,
                                       final String label )  {
            this.id = id;
            this.label = label;
        }

        @Override
        public int compareTo(Object o) {
            PreviousMigrationModel other = (PreviousMigrationModel) o;
            return this.label.toLowerCase().compareTo(other.label.toLowerCase());
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private String jsArray( final Collection<String> identifiers ) {
        StringBuilder builder = new StringBuilder();

        builder.append('[');
        boolean first = true;
        for ( String identifier : identifiers ) {
            if ( first ) {
                first = false;
            } else {
                builder.append( "," );
            }
            builder.append( "'" );
            builder.append( identifier );
            builder.append( "'" );
        }
        builder.append(']');

        return builder.toString();
    }

    private List<PreviousMigrationModel> loadPreviousMigrations() {
        List<PreviousMigrationModel> previousMigrations = new ArrayList<PreviousMigrationModel>();

        try {
            Collection<MigrationRecord> records = migrationRecordManager.findNamedMigrations( getUser(), 100, null, null );
            if ( records != null ) {
                for ( MigrationRecord record : records ) {
                    previousMigrations.add( new PreviousMigrationModel( record.getOid(), record.getName() ) );
                }
            }
        } catch ( FindException fe ) {
            logger.log( Level.INFO, "Error loading previous migration records.", fe );
        }

        return previousMigrations;
    }

    private AbstractDefaultAjaxBehavior buildDependencyDisplayBehaviour( final WebMarkupContainer itemDependenciesContainer,
                                                                         final WebMarkupContainer itemDetailsContainer,
                                                                         final String jsVar,
                                                                         final EntityMappingModel mappingModel ) {
        return new AbstractDefaultAjaxBehavior(){
            @Override
            public void renderHead( final IHeaderResponse iHeaderResponse ) {
                super.renderHead( iHeaderResponse );
                iHeaderResponse.renderJavascript("var "+jsVar+" = '"+getCallbackUrl(true)+"';", null);
            }

            @Override
            protected void respond( final AjaxRequestTarget ajaxRequestTarget ) {
                final WebRequest request = (WebRequest) RequestCycle.get().getRequest();
                final String clusterId = request.getParameter("clusterId");
                final String targetClusterId = request.getParameter("targetClusterId");
                final String type = request.getParameter("type");
                final String id = request.getParameter("id");

                logger.fine( "Processing request for cluster " + clusterId + " type " + type + " id " + id );
                DependencyItemsRequest dir = new DependencyItemsRequest();
                dir.clusterId = clusterId;
                dir.entities = new DependencyItem[]{ new DependencyItem() };
                dir.entities[0].type = type;
                dir.entities[0].id = id;
                DependencyItem detailItem = null;
                List<DependencyItem> options = Collections.emptyList();

                if ( id != null && !id.isEmpty() &&
                     type != null && !type.isEmpty() &&
                     clusterId != null && !clusterId.isEmpty() ) {
                    try {
                        options = new ArrayList<DependencyItem>(retrieveDependencies( dir, mappingModel, targetClusterId ));
                        for ( Iterator<DependencyItem> itemIter = options.iterator(); itemIter.hasNext();  ) {
                            DependencyItem item = itemIter.next();
                            if ( item.hidden || com.l7tech.objectmodel.EntityType.FOLDER.toString().equals(item.type) ) {
                                itemIter.remove();
                            }
                        }

                        // discard the dependencies that no longer exist on the target cluster
                        SsgCluster cluster = ssgClusterManager.findByGuid( dir.clusterId );
                        GatewayClusterClient targetContext = gatewayClusterClientManager.getGatewayClusterClient(cluster, getUser());
                        MigrationApi targetMigrationApi = targetContext.getUncachedMigrationApi();
                        Collection<ExternalEntityHeader> headers = targetMigrationApi.checkHeaders( Collections.singleton( dir.entities[0].asEntityHeader() ) );
                        if ( headers != null && !headers.isEmpty() ) {
                            ExternalEntityHeader header = headers.iterator().next();
                            if ( header.getProperty("Alias Of") != null || header.getProperty("Alias Of Internal") != null ) {
                                // resolve alias
                                ExternalEntityHeader aliasTargetHeader =
                                        new ExternalEntityHeader( header.getProperty("Alias Of"),
                                                                  EntityType.valueOf(header.getProperty("Alias Type")),
                                                                  header.getProperty("Alias Of Internal"), null, null, -1 );
                                headers = targetMigrationApi.checkHeaders( Collections.singleton( aliasTargetHeader ) );
                                if ( headers != null && !headers.isEmpty() ) {
                                    detailItem = new DependencyItem( headers.iterator().next(), false );
                                }
                            } else {
                                detailItem = new DependencyItem( header, false );
                            }
                        }

                    } catch ( MigrationApi.MigrationException me ) {
                        logger.log( Level.INFO, "Error processing selection '"+ExceptionUtils.getMessage(me)+"'." );
                    } catch ( SOAPFaultException sfe ) {
                        if ( !GatewayContext.isNetworkException(sfe) && !GatewayContext.isConfigurationException(sfe) ) {
                            logger.log( Level.WARNING, "Error processing selection '"+ExceptionUtils.getMessage(sfe)+"'.", ExceptionUtils.getDebugException(sfe));
                        }
                    } catch ( GatewayException ge ) {
                        logger.log( Level.INFO, "Error processing selection '"+ExceptionUtils.getMessage(ge)+"'.", ExceptionUtils.getDebugException(ge) );
                    } catch ( FindException fe ) {
                        logger.log( Level.WARNING, "Error processing selection.", fe );
                    }
                }

                Collections.sort(options);
                showDependencies( itemDependenciesContainer, options, itemDetailsContainer, detailItem );
                ajaxRequestTarget.addComponent( itemDependenciesContainer );
                ajaxRequestTarget.addComponent( itemDetailsContainer );
            }
        };
    }

    private List<DependencyItem> visible( final Collection<DependencyItem> items, final boolean showHiddenDeps ) {
        List<DependencyItem> visibleDeps = new ArrayList<DependencyItem>( items.size() );

        for ( DependencyItem item : items ) {
            if ( !item.hidden && (showHiddenDeps || !isHiddenDependency(item)) ) visibleDeps.add( item );
        }

        return visibleDeps;
    }

    private List<Pair<String,String>> nvp( final DependencyItem item ) {
        List<Pair<String,String>> properties = new ArrayList<Pair<String,String>>();

        if ( item != null ) {
            Map<String,String> extraProps = Collections.emptyMap();
            if ( item.entityHeader != null && item.entityHeader.getExtraProperties() != null ) {
                extraProps = item.entityHeader.getExtraProperties();
            }

            properties.add( new Pair<String,String>( "Name", item.getDisplayNameWithScope()) );
            properties.add( new Pair<String,String>( "Type", item.getType() ) );
            properties.add( new Pair<String,String>( "Version", item.getVersionAsString() ) );


            for ( String extraProperty : EXTRA_PROPERTIES ) {
                if ( extraProps.containsKey(extraProperty) ) {
                    properties.add( new Pair<String,String>( extraProperty, extraProps.get(extraProperty) ) );
                }
            }
        }

        return properties;
    }

    private void showDependencies( final WebMarkupContainer container,
                                   final List<DependencyItem> options,
                                   final WebMarkupContainer detailsContainer,
                                   final DependencyItem detailsItem ) {
        // dependencies
        ListView listView = new ListView("optionRepeater", visible(options, false)) {
            @Override
            protected void populateItem( final ListItem item ) {
                final DependencyItem dependencyItem = ((DependencyItem)item.getModelObject());
                item.add(new Label("optional", dependencyItem.getOptional()).setEscapeModelStrings(false));
                item.add(new Label("name", dependencyItem.getDisplayNameWithScope()));
                item.add(new Label("type", com.l7tech.objectmodel.EntityType.valueOf(dependencyItem.type).getName().toLowerCase()));
            }
        };

        if ( container.get( "optionRepeater" ) == null ) {
            container.add( listView.setOutputMarkupId(true) );
        } else {
            container.replace( listView.setOutputMarkupId(true) );
        }

        // details
        ListView detailsListView = new ListView("itemDetailsRepeater", nvp(detailsItem)) {
            @SuppressWarnings({"unchecked"})
            @Override
            protected void populateItem( final ListItem item ) {
                final Pair<String,String> nvp = (Pair<String,String>)item.getModelObject();
                item.add(new Label("name", nvp.left));
                item.add(new Label("value", nvp.right));
            }
        };

        if ( detailsContainer.get( "itemDetailsRepeater" ) == null ) {
            detailsContainer.add( detailsListView.setOutputMarkupId(true) );
        } else {
            detailsContainer.replace( detailsListView.setOutputMarkupId(true) );
        }

    }

    private void updateDependencies( final WebMarkupContainer dependenciesContainer,
                                     final WebMarkupContainer[] optionRefreshComponents,
                                     final CandidateModel candidateModel,
                                     final SearchModel searchModel,
                                     final EntityMappingModel mappingModel,
                                     final DepenencySummaryModel dependencySummaryModel ) {
        final String sourceClusterId = this.lastSourceClusterId;
        final String targetClusterId = this.lastTargetClusterId;
        final Collection<DependencyItem> items = visible(this.lastDependencyItems, true);

        // update dependency counts and items destination names
        dependencySummaryModel.reset();
        for ( DependencyItem item : items ) {
            DependencyKey sourceKey = new DependencyKey( sourceClusterId, item.asEntityHeader() );
            Pair<DependencyKey,String> mappingKey = new Pair<DependencyKey,String>(sourceKey, targetClusterId);
            Pair<ValueKey,String> valueKey = item.asEntityHeader().isValueMappable() ?
                    new Pair<ValueKey,String>(new ValueKey( sourceClusterId, item.asEntityHeader() ), targetClusterId) :
                    null;
            DependencyItem mappedItem = mappingModel.dependencyMap.get( mappingKey );
            boolean resolved = false;

            String mappedValue = valueKey != null ? mappingModel.valueMap.get( valueKey ) : null;
            if ( mappedValue != null ) {
                item.destName = truncateDisplayValue( mappedValue );
                item.entityHeader.setMappedValue(mappedValue);
                resolved = true;
            } else if (mappedItem != null ) {
                item.destName = mappedItem.getDisplayNameWithScope();
                resolved = true;
            } else {
                item.destName = "-";
            }

            if ( item.isOptional() ) {
                if ( resolved ) {
                    dependencySummaryModel.incrementTotalDependencies();
                } else {
                    dependencySummaryModel.incrementUnmappedDependencies();
                }
            } else {
                item.resolved = resolved; // tristate, should be null if optional
                if ( resolved ) {
                    dependencySummaryModel.incrementTotalDependencies();
                } else {
                    dependencySummaryModel.incrementRequiredUnmappedDependencies();
                }
            }
        }

        // update UI
        final List<PropertyColumn> dependencyColumns =  Arrays.asList(
            new PropertyColumn(new Model(""), "uid"),
            new TypedPropertyColumn(new Model(""), "optional", "optional", String.class, false),
            new TypedPropertyColumn(new Model("Name"), "displayNameWithScope", "displayNameWithScope", String.class, true, true),
            new PropertyColumn(new Model("Type"), "type", "type"),
            new PropertyColumn(new Model("Dest. Name / Value"), "destName", "destName")
        );

        final YuiDataTable ydt = new YuiDataTable( "dependenciesTable", dependencyColumns, "displayNameWithScope", true, items, null, false, "uid", true, null ){
            @Override
            @SuppressWarnings({"UnusedDeclaration"})
            protected void onSelect( final AjaxRequestTarget ajaxRequestTarget, final String value ) {
                if ( targetClusterId != null && !targetClusterId.isEmpty() && value != null && !value.isEmpty() ) {
                    String[] typeIdPair = value.split(":", 2);
                    logger.fine("Selected dependency for mapping id '"+typeIdPair[1]+"', type '"+typeIdPair[0]+"'.");

                    DependencyItem selectedItem = null;
                    for ( DependencyItem item : items ) {
                        if ( item.type.equals(typeIdPair[0]) && item.mappingKey.equals(typeIdPair[1]) ) {
                            selectedItem = item;
                            break;
                        }
                    }

                    Component selectionComponent = ((Form)dependenciesContainer.get("dependencyControlsForm")).get("dependencyClearButton");
                    Component selectionComponent2 = ((Form)dependenciesContainer.get("dependencyControlsForm")).get("dependencyEditButton");
                    if ( selectedItem != null ) {
                        lastSourceKey = new DependencyKey( sourceClusterId, selectedItem.asEntityHeader() );
                        if ( isSearchable( selectedItem.asEntityHeader() ) ) {
                            candidateModel.setName( selectedItem.getDisplayNameWithScope() );
                            candidateModel.setType( selectedItem.getType() );
                        } else {
                            candidateModel.reset();
                        }
                        Pair<DependencyKey,String> key = new Pair<DependencyKey,String>(lastSourceKey,lastTargetClusterId);
                        final Pair<ValueKey,String> valueKey = lastSourceKey.asEntityHeader().isValueMappable() ?
                            new Pair<ValueKey,String>(new ValueKey(lastSourceKey), lastTargetClusterId):
                            null;

                        selectionComponent.setEnabled( mappingModel.dependencyMap.containsKey(key) || mappingModel.valueMap.containsKey(valueKey) );
                        selectionComponent2.setEnabled( selectedItem.asEntityHeader().isValueMappable() );
                    } else {
                        lastSourceKey = null;
                        candidateModel.reset();
                        selectionComponent.setEnabled(false);
                        selectionComponent2.setEnabled(false);
                    }

                    addDependencyOptions( dependenciesContainer, optionRefreshComponents, candidateModel, searchModel, mappingModel, dependencySummaryModel, true );
                    ajaxRequestTarget.addComponent( selectionComponent );
                    ajaxRequestTarget.addComponent( selectionComponent2 );
                    for ( Component component : optionRefreshComponents ) ajaxRequestTarget.addComponent( component );
                }
            }
        };

        if ( dependenciesContainer.get( ydt.getId() ) == null ) {
            dependenciesContainer.add( ydt );
        } else {
            dependenciesContainer.replace( ydt );
        }
    }

    private void addDependencyOptions( final WebMarkupContainer dependenciesContainer,
                                       final WebMarkupContainer[] optionRefreshComponents,
                                       final CandidateModel candidateModel,
                                       final SearchModel searchModel,
                                       final EntityMappingModel mappingModel,
                                       final DepenencySummaryModel dependencySummaryModel,
                                       final boolean skipSearch ) {
        String targetClusterId = lastTargetClusterId;
        DependencyKey sourceKey = lastSourceKey;
        final Pair<DependencyKey,String> mappingKey = new Pair<DependencyKey,String>(sourceKey, targetClusterId);

        WebMarkupContainer markupContainer = new WebMarkupContainer("dependencyOptions");
        if ( optionRefreshComponents[1].get( markupContainer.getId() ) == null ) {
            optionRefreshComponents[1].add( markupContainer.setOutputMarkupId(true) );
        } else {
            optionRefreshComponents[1].replace( markupContainer.setOutputMarkupId(true) );
        }

        List<DependencyItem> options  = Collections.emptyList();
        DropDownChoice targetChoice = (DropDownChoice) optionRefreshComponents[1].get("dependencySearchTarget");
        if ( skipSearch ) {
            searchModel.setSearchManner("contains");
            searchModel.setSearchValue("");

            if ( sourceKey != null && isSearchable( sourceKey.asEntityHeader() ) ) {
                for ( String id : SEARCH_REFRESH_COMPONENTS ) {
                    Component component = optionRefreshComponents[1].get(id);
                    component.setEnabled( true );
                }

                List<DependencyItem> scopeOptions = null;
                if ( sourceKey.asEntityHeader().getProperty("Scope Type") != null ) {
                    ExternalEntityHeader externalEntityHeader = new ExternalEntityHeader( null, EntityType.valueOf(sourceKey.asEntityHeader().getProperty("Scope Type")), null, null, null, -1);
                    scopeOptions = retrieveDependencyOptions( lastTargetClusterId, new DependencyKey(sourceKey.clusterId, externalEntityHeader), null, null );
                }

                if ( scopeOptions != null && !scopeOptions.isEmpty() ) {
                    List<SearchTarget> targetChoices = new ArrayList<SearchTarget>();
                    SearchTarget selected = null;
                    for ( DependencyItem item : scopeOptions ) {
                        if ( selected == null ) selected = new SearchTarget( item );
                        targetChoices.add( new SearchTarget( item ) );
                    }
                    Collections.sort( targetChoices );
                    targetChoice.setChoices( targetChoices );
                    targetChoice.setModelObject( selected );
                } else {
                    targetChoice.setChoices( Arrays.asList( new SearchTarget() ) );
                    targetChoice.setModelObject( new SearchTarget() );
                }
            } else {
                for ( String id : SEARCH_REFRESH_COMPONENTS ) {
                    Component component = optionRefreshComponents[1].get(id);
                    component.setEnabled( false );
                }
            }
        } else {
            if ( sourceKey != null && isSearchable( sourceKey.asEntityHeader() ) ) {
                options = retrieveDependencyOptions( lastTargetClusterId, lastSourceKey, searchModel.getSearchTarget().item, searchModel.getSearchFilter() );
            }
        }

        markupContainer.add(new ListView("optionRepeater", options) {
            @Override
            protected void populateItem( final ListItem item ) {
                final DependencyItem dependencyItem = ((DependencyItem)item.getModelObject());

                Component radioComponent = new WebComponent("uid").add( new AjaxEventBehavior("onchange"){
                    @Override
                    protected void onEvent( final AjaxRequestTarget ajaxRequestTarget ) {
                        logger.fine("Selection callback for : " + dependencyItem);
                        mappingModel.dependencyMap.put( mappingKey, dependencyItem );
                        updateDependencies( dependenciesContainer, optionRefreshComponents, candidateModel, searchModel, mappingModel, dependencySummaryModel );

                        for ( String id : DEPENDENCY_REFRESH_COMPONENTS ) ajaxRequestTarget.addComponent( dependenciesContainer.get(id) );
                        for ( Component component : optionRefreshComponents ) ajaxRequestTarget.addComponent( component );
                    }
                } );

                DependencyItem currentItem = mappingModel.dependencyMap.get( mappingKey );
                if ( currentItem!=null && currentItem.equals( dependencyItem ) ) {
                    radioComponent.add( new AttributeModifier("checked", true, new Model("checked")) );
                }

                item.add(radioComponent);
                item.add(new Label("name", dependencyItem.getDisplayName()));
            }
        });
    }

    private Collection<DependencyItem> retrieveDependencies( final DependencyItemsRequest request,
                                                             final EntityMappingModel mappingModel,
                                                             final String targetClusterId ) throws FindException, GatewayException, MigrationApi.MigrationException {
        Collection<DependencyItem> deps = new LinkedHashSet<DependencyItem>();

        SsgCluster cluster = ssgClusterManager.findByGuid(request.clusterId);
        if ( cluster != null ) {
            if ( cluster.getTrustStatus() ) {
                GatewayClusterClient context = gatewayClusterClientManager.getGatewayClusterClient(cluster, getUser());
                MigrationApi api = context.getUncachedMigrationApi();
                MigrationMetadata metadata = api.findDependencies( request.asEntityHeaders() );
                for (ExternalEntityHeader header : metadata.getMappableDependencies()) {
                    deps.add( new DependencyItem( header, metadata.isMappingRequired(header) ? isResolved( mappingModel, header, request.clusterId, targetClusterId ) : null ) );
                }

                for (ExternalEntityHeader header : metadata.getAllHeaders() ) {
                    boolean alreadyPresent = false;

                    for ( DependencyItem item : deps ) {
                        if ( item.asEntityHeader().equals(header) ) {
                            alreadyPresent = true;
                            break;
                        }
                    }

                    if ( !alreadyPresent ) deps.add( new DependencyItem( header, null, true ) );
                }

                // expand value-mappables of array types
                Collection<DependencyItem> exploded = new ArrayList<DependencyItem>();
                for (DependencyItem item : deps) {
                    exploded.addAll( explode( item ) );
                }
                deps = exploded;

                // populate entity owner info
                for ( DependencyItem item : deps ) {
                    if ( item.asEntityHeader() instanceof ValueReferenceEntityHeader ) {
                        ValueReferenceEntityHeader vreh = (ValueReferenceEntityHeader) item.asEntityHeader();
                        if ( vreh.getOwnerType() != null && vreh.getOwnerId() != null ) {
                            for ( DependencyItem itemOwner : deps ) {
                                if ( vreh.getOwnerType()==itemOwner.asEntityHeader().getType() &&
                                     vreh.getOwnerId().equals(itemOwner.id) ) {
                                    item.ownerName = itemOwner.getDisplayNameWithScope();
                                    break;
                                }
                            }
                        }
                    }
                }

                // remove duplicate entries (same service / value)
                Collection<DependencyItem> duplicates = new ArrayList<DependencyItem>();
                for ( DependencyItem item1 : deps ) {
                    boolean isAfter = false;
                    for ( DependencyItem item2 : deps ) {
                        if ( isAfter && isValueMappingSameOwnerAndTypeAndValue( item1, item2 ) ) {
                            duplicates.add( item2 );
                        }
                        if ( item1==item2 ) {
                            isAfter = true;
                        }
                    }
                }
                deps.removeAll( duplicates );
            }
        }

        return deps;
    }

    /**
     * If the given item represents a value mapping with an array type it is expanded to
     * a collection of non-array value mappings.
     *
     * @return  Collection containing either the exploded items, or the item parameter
     *          if it is not value-mappable, or it does not hold an array-type value, or the source mappable value is null.
     */
    private static Collection<DependencyItem> explode( final DependencyItem item ) {
        Collection<DependencyItem> exploded = new ArrayList<DependencyItem>();

        ExternalEntityHeader eeh = item == null ? null : item.asEntityHeader();
        if (eeh != null && eeh.isValueMappable()) {
            for(ExternalEntityHeader vmHeader : eeh.getValueMappableHeaders()) {
                exploded.add(new DependencyItem(vmHeader));
            }
        } else {
            exploded.add(item);
        }

        return exploded;
    }

    private static boolean matches( final String data, final String pattern ) {
        boolean matches = true;

        if(pattern!=null && pattern.trim().length()>0) {
            if(pattern.equals("*")) {
                matches = true;
            }
            else if(data==null) {
                matches = false;
            }
            else {
                String lpattern = pattern.toLowerCase();
                String ldata = data.toLowerCase();

                String[] patterns = lpattern.split("\\*");
                boolean wildStart = lpattern.startsWith("*");
                boolean wildEnd = lpattern.endsWith("*");

                int offset = 0;
                for (int i = 0; i < patterns.length; i++) {
                    String pat = patterns[i];
                    if(i==0 && !wildStart && !wildEnd && patterns.length==1 && !ldata.equals(pat)) {
                        matches = false;
                        break;
                    }
                    else if(i==0 && !wildStart && !ldata.startsWith(pat)) {
                        matches = false;
                        break;
                    }
                    else if(i==patterns.length-1 && !wildEnd && !ldata.endsWith(pat)) {
                        matches = false;
                        break;
                    }
                    else {
                        if(pat.length()==0) continue;
                        int patIndex = ldata.indexOf(pat, offset);
                        if(patIndex<0) {
                            matches = false;
                            break;
                        }
                        else {
                            offset = patIndex + pat.length();
                        }
                    }
                }
            }
        }

        return matches;
    }

    @SuppressWarnings({"unchecked"})
    private List<DependencyItem> retrieveDependencyOptions( final String targetClusterId, final DependencyKey sourceKey, final DependencyItem scope, final String filter ) {
        List<DependencyItem> deps = new ArrayList<DependencyItem>();

        try {
            SsgCluster cluster = ssgClusterManager.findByGuid(targetClusterId);
            if ( cluster != null ) {
                if ( cluster.getTrustStatus() ) {
                    GatewayClusterClient context = gatewayClusterClientManager.getGatewayClusterClient(cluster, getUser());
                    MigrationApi api = context.getUncachedMigrationApi();
                    ExternalEntityHeader entityHeader = sourceKey.asEntityHeader();
                    Map candidates = MigrationApi.MappingCandidate.fromCandidates(api.retrieveMappingCandidates(
                                Collections.singletonList( entityHeader ),
                                scope == null ? null : scope.asEntityHeader(),
                                new HashMap<String,String>() {{put(SearchableEntityManager.DEFAULT_SEARCH_NAME, filter);}} ));
                    if ( candidates != null && candidates.containsKey(entityHeader) ) {
                        EntityHeaderSet<ExternalEntityHeader> entitySet = (EntityHeaderSet<ExternalEntityHeader>) candidates.get(entityHeader);
                        if ( entitySet != null ) {
                            for ( ExternalEntityHeader header : entitySet ) {
                                if ( matches( header.getName(), filter ) ) {
                                    deps.add( new DependencyItem( header ) );
                                }
                            }
                            Collections.sort( deps );
                        } else {
                            logger.fine("No entities found when searching for candidates of ID '"+sourceKey.id+"', type '"+sourceKey.type+"'.");
                        }
                    } else {
                        logger.fine("No candidates found when searching for candidates of ID '"+sourceKey.id+"', type '"+sourceKey.type+"'.");
                    }
                }
            }
        } catch ( GatewayException ge ) {
            logger.log( Level.INFO, "Error while gettings dependency options '"+ExceptionUtils.getMessage(ge)+"'.", ExceptionUtils.getDebugException(ge) );
        } catch ( FindException fe ) {
            logger.log( Level.WARNING, "Error while gettings dependency options.", fe );
        } catch ( MigrationApi.MigrationException me ) {
            logger.log( Level.INFO, "Error while gettings dependency options '"+ExceptionUtils.getMessage(me)+"'.", ExceptionUtils.getDebugException(me) );
        } catch ( SOAPFaultException sfe ) {
            if ( !GatewayContext.isNetworkException( sfe ) && !GatewayContext.isConfigurationException( sfe ) ) {
                logger.log( Level.WARNING, "Error while gettings dependency options'"+ExceptionUtils.getMessage(sfe)+"'.", ExceptionUtils.getDebugException(sfe));
            }
        }

        return deps;
    }

    /**
     * Populates the mapping model with the mappings persisted in the database.
     *
     * @param onlyIsSame If true only the mappings having the flag isSame == true are loaded
     *                   (for entities that were created on the target cluster as a result of a previous migration);
     *                   If false all mappings are loaded, including the ones manually configured by the EM users.
     */
    private void loadMappings( final EntityMappingModel mappingModel,
                               final String sourceClusterId,
                               final String targetClusterId,
                               final Collection<DependencyItem> dependencyItems,
                               final boolean onlyIsSame) throws FindException, GatewayException {
        if ( !dependencyItems.isEmpty() ) {
            // load mappings saved in EM db
            for ( DependencyItem item : dependencyItems ) {
                if ( item.asEntityHeader().isValueMappable() ) {
                    if ( !onlyIsSame ) {
                        Pair<ValueKey,String> valueKey = new Pair<ValueKey,String>(new ValueKey( sourceClusterId, item.asEntityHeader() ), targetClusterId);
                        if ( !mappingModel.valueMap.containsKey(valueKey) ) {
                            MigrationMappingRecord mapping = migrationMappingRecordManager.findByMapping( sourceClusterId, item.asEntityHeader(), targetClusterId, true );
                            if ( mapping != null && mapping.getTarget() != null ) {
                                mappingModel.valueMap.put( valueKey, mapping.getTarget().getEntityValue() );
                            }
                        }
                    }
                }

                DependencyKey sourceKey = new DependencyKey(sourceClusterId, item.asEntityHeader());
                Pair<DependencyKey, String> mapKey = new Pair<DependencyKey, String>(sourceKey, targetClusterId);
                if (!mappingModel.dependencyMap.containsKey(mapKey)) {
                    MigrationMappingRecord mapping = migrationMappingRecordManager.findByMapping(sourceClusterId, item.asEntityHeader(), targetClusterId, false);
                    if (mapping != null && mapping.getTarget() != null && (mapping.isSameEntity() || !onlyIsSame) ) {
                        mappingModel.dependencyMap.put(mapKey, new DependencyItem(MigrationMappedEntity.asEntityHeader(mapping.getTarget()), null, false, mapping.isSameEntity()));
                    }
                }
            }

            // discard the dependencies that no longer exist on the target cluster
            SsgCluster targetCluster = ssgClusterManager.findByGuid(targetClusterId);
            GatewayClusterClient targetContext = gatewayClusterClientManager.getGatewayClusterClient(targetCluster, getUser());
            MigrationApi targetMigrationApi = targetContext.getUncachedMigrationApi();

            Collection<ExternalEntityHeader> headersToCheck = new HashSet<ExternalEntityHeader>();
            for (Map.Entry<Pair<DependencyKey,String>,DependencyItem> entry : mappingModel.dependencyMap.entrySet()) {
                if ( entry.getValue() == null || !entry.getKey().right.equals(targetClusterId)) continue;
                headersToCheck.add( entry.getValue().asEntityHeader() );
            }

            Collection<ExternalEntityHeader> validatedHeaders = targetMigrationApi.checkHeaders(headersToCheck);
            if ( validatedHeaders == null )
                validatedHeaders = Collections.emptyList();

            Map<Pair<DependencyKey,String>,DependencyItem> keysToUpdate = new HashMap<Pair<DependencyKey, String>,DependencyItem>();
            Set<Pair<DependencyKey,String>> keysToNull = new HashSet<Pair<DependencyKey, String>>();
            for( Map.Entry<Pair<DependencyKey,String>,DependencyItem> entry : mappingModel.dependencyMap.entrySet() ) {
                if ( !entry.getKey().right.equals(targetClusterId) || entry.getValue() == null || entry.getValue() == null )
                    continue;

                if ( !containsHeader( entry.getValue().asEntityHeader(), validatedHeaders ) ) {
                    keysToNull.add( entry.getKey() );
                } else if (! entry.getValue().same) {
                    keysToUpdate.put( entry.getKey(), new DependencyItem(getHeader( validatedHeaders, entry.getValue().asEntityHeader().getType(), entry.getValue().id ), entry.getValue()) );
                }
            }
            mappingModel.dependencyMap.putAll( keysToUpdate );
            mappingModel.dependencyMap.keySet().removeAll( keysToNull );
        }
    }

    /**
     * Restore any of the given mappings if they are still valid.
     */
    private void validateAndRestoreMappings( final EntityMappingModel mappingModel,
                                             final String sourceClusterGuid,
                                             final String targetClusterGuid,
                                             final List<Pair<ExternalEntityHeader, ExternalEntityHeader>> mappings ) {
        try {
            // discard the mappings that no longer exist on the source or target cluster
            SsgCluster sourceCluster = ssgClusterManager.findByGuid(sourceClusterGuid);
            GatewayClusterClient sourceContext = gatewayClusterClientManager.getGatewayClusterClient(sourceCluster, getUser());
            MigrationApi sourceMigrationApi = sourceContext.getUncachedMigrationApi();

            SsgCluster targetCluster = ssgClusterManager.findByGuid(targetClusterGuid);
            GatewayClusterClient targetContext = gatewayClusterClientManager.getGatewayClusterClient(targetCluster, getUser());
            MigrationApi targetMigrationApi = targetContext.getUncachedMigrationApi();

            List<ExternalEntityHeader> sourceHeaders = new ArrayList<ExternalEntityHeader>();
            List<ExternalEntityHeader> targetHeaders = new ArrayList<ExternalEntityHeader>();
            for ( Pair<ExternalEntityHeader, ExternalEntityHeader> mapping : mappings ) {
                sourceHeaders.add( mapping.left );
                targetHeaders.add( mapping.right );
            }

            Collection<ExternalEntityHeader> validatedSourceHeaders = sourceMigrationApi.checkHeaders( sourceHeaders );
            Collection<ExternalEntityHeader> validatedTargetHeaders = targetMigrationApi.checkHeaders( targetHeaders );

            for ( Pair<ExternalEntityHeader, ExternalEntityHeader> mapping : mappings ) {
                if ( containsHeader( mapping.left, validatedSourceHeaders ) &&
                     containsHeader( mapping.right, validatedTargetHeaders ) ) {
                    DependencyKey sourceKey = new DependencyKey( sourceClusterGuid, mapping.left );
                    Pair<DependencyKey,String> mapKey = new Pair<DependencyKey,String>( sourceKey, targetClusterGuid );
                    mappingModel.dependencyMap.put( mapKey, new DependencyItem(getHeader(validatedTargetHeaders, mapping.right.getType(), mapping.right.getExternalId())) );
                }
            }

        } catch ( GatewayException ge ) {
            logger.log( Level.INFO, "Error while reloading previous migration '"+ExceptionUtils.getMessage(ge)+"'.", ExceptionUtils.getDebugException(ge) );
        } catch ( FindException fe ) {
            logger.log( Level.WARNING, "Error while reloading previous migration.", fe );
        } catch ( SOAPFaultException sfe ) {
            if ( !GatewayContext.isNetworkException( sfe ) && !GatewayContext.isConfigurationException( sfe ) ) {
                logger.log( Level.WARNING, "Error while reloading previous migrations'"+ExceptionUtils.getMessage(sfe)+"'.", ExceptionUtils.getDebugException(sfe));
            }
        }
    }

    private ExternalEntityHeader getHeader( final Collection<ExternalEntityHeader> headers, final EntityType type, final String id ) {
        ExternalEntityHeader eeh = null;

        if ( headers != null ) {
            for ( ExternalEntityHeader header : headers ) {
                if ( header.getExternalId().equals( id ) &&
                     header.getType() == type ) {
                    eeh = header;
                    break;
                }
            }
        }

        return eeh;
    }

    private boolean containsHeader( final ExternalEntityHeader header, final Collection<ExternalEntityHeader> headers ) {
        return getHeader( headers, header.getType(), header.getExternalId() ) != null;
    }

    /**
     * Count the number of unmapped dependencies in the mapping model for the given source/target cluster
     */
    private int countUnMappedDependencies( final EntityMappingModel mappingModel,
                                           final String sourceClusterId,
                                           final String targetClusterId,
                                           final Collection<DependencyItem> items ) {
        int count = 0;

        if ( sourceClusterId != null && targetClusterId != null ) {
            for ( DependencyItem item : items ) {
                if ( item.hidden ) continue;

                final DependencyKey sourceKey = new DependencyKey( sourceClusterId, item.asEntityHeader() );
                final Pair<DependencyKey,String> mapKey = new Pair<DependencyKey,String>( sourceKey, targetClusterId );
                final Pair<ValueKey,String> valueKey = item.asEntityHeader().isValueMappable() ?
                    new Pair<ValueKey,String>(new ValueKey(sourceClusterId, item.asEntityHeader()), targetClusterId):
                    null;

                if ( !mappingModel.dependencyMap.containsKey(mapKey) && (valueKey==null || !mappingModel.valueMap.containsKey(valueKey)) ) {
                    count++;
                }
            }
        }

        return count;
    }

    private String validateDependencies( final String sourceClusterId,
                                         final String targetClusterId,
                                         final DependencyItemsRequest requestedItems,
                                         final EntityMappingModel mappingModel ) throws MigrationFailedException {
        String summary = "";
        try {
            SsgCluster sourceCluster = ssgClusterManager.findByGuid(sourceClusterId);
            SsgCluster targetCluster = ssgClusterManager.findByGuid(targetClusterId);
            if ( sourceCluster != null && targetCluster != null) {
                if ( sourceCluster.getTrustStatus() && targetCluster.getTrustStatus() ) {
                    Collection<DependencyItem> items = retrieveDependencies( requestedItems, null, null );
                    StringBuilder builder = new StringBuilder();
                    for ( DependencyItem item : items ) {
                        final DependencyKey sourceKey = new DependencyKey( sourceClusterId, item.asEntityHeader() );
                        final Pair<DependencyKey,String> mapKey = new Pair<DependencyKey,String>( sourceKey, targetClusterId );
                        final Pair<ValueKey,String> valueKey = item.asEntityHeader().isValueMappable() ?
                            new Pair<ValueKey,String>(new ValueKey(sourceClusterId, item.asEntityHeader()), targetClusterId):
                            null;

                        if ( !item.isOptional() && mappingModel.dependencyMap.get(mapKey) == null && (valueKey==null || mappingModel.valueMap.get(valueKey) == null) ) {
                            ExternalEntityHeader ih = item.asEntityHeader();
                            builder.append(ih.getType().getName().toLowerCase()).append(", ").append(ih.getName())
                                .append("(#").append(ih.getExternalId()).append(")\n");
                        }
                    }
                    if ( builder.length() > 0 ) {
                        summary = "The following items require mapping:\n" + builder.toString();
                    }
                } else {
                    summary = "Missing trust relationship for source or destination cluster.";
                }
            } else {
                summary = "Source or destination cluster is invalid.";
            }
        } catch ( FindException fe ) {
            logger.log( Level.WARNING, "Error while checking dependencies for migration.", fe );
            throw new MigrationFailedException("Migration failed '"+ ExceptionUtils.getMessage(fe)+"'.");
        } catch ( GatewayException e ) {
            logger.log( Level.INFO, "Error while checking dependencies for migration '"+ExceptionUtils.getMessage(e)+"'.", ExceptionUtils.getDebugException(e) );
            throw new MigrationFailedException("Migration failed '"+ ExceptionUtils.getMessage(e)+"'.");
        } catch ( MigrationApi.MigrationException e ) {
            logger.log( Level.INFO, "Error while checking dependencies for migration '"+ExceptionUtils.getMessage(e)+"'.", ExceptionUtils.getDebugException(e) );
            throw new MigrationFailedException(summarizeMigrationException(e));
        }

        return summary;
    }

    private static final class MigrationFailedException extends Exception {
        public MigrationFailedException(String message) {
            super(message);
        }
    }

    private String performMigration( final String sourceClusterId,
                                     final String targetClusterId,
                                     final String targetFolderId,
                                     final boolean migrateFolders,
                                     final boolean enableNewServices,
                                     final boolean overwriteEntities,
                                     final DependencyItemsRequest requestedItems,
                                     final EntityMappingModel mappingModel,
                                     final String label,
                                     final boolean dryRun ) throws MigrationFailedException {
        String summaryString = "";
        try {
            SsgCluster sourceCluster = ssgClusterManager.findByGuid(sourceClusterId);
            SsgCluster targetCluster = ssgClusterManager.findByGuid(targetClusterId);
            if ( sourceCluster != null && targetCluster != null) {
                if ( sourceCluster.getTrustStatus() && targetCluster.getTrustStatus() ) {
                    GatewayClusterClient sourceContext = gatewayClusterClientManager.getGatewayClusterClient(sourceCluster, getUser());
                    MigrationApi sourceMigrationApi = sourceContext.getUncachedMigrationApi();

                    GatewayClusterClient targetContext = gatewayClusterClientManager.getGatewayClusterClient(targetCluster, getUser());
                    MigrationApi targetMigrationApi = targetContext.getUncachedMigrationApi();
                    GatewayApi targetGatewayApi = targetContext.getUncachedGatewayApi();

                    MigrationBundle bundle = sourceMigrationApi.exportBundle( requestedItems.asEntityHeaders() );
                    MigrationMetadata metadata = bundle.getMetadata();
                    for ( Map.Entry<Pair<DependencyKey,String>,DependencyItem> mapping : mappingModel.dependencyMap.entrySet() ) {
                        if ( mapping.getValue() != null && mapping.getKey().left.clusterId.equals(sourceClusterId) && mapping.getKey().right.equals(targetClusterId) ) {
                            metadata.addMappingOrCopy(mapping.getKey().left.asEntityHeader(), mapping.getValue().asEntityHeader(), mapping.getValue().same);
                            migrationMappingRecordManager.persistMapping(
                                    sourceCluster.getGuid(),
                                    mapping.getKey().left.asEntityHeader(),
                                    targetCluster.getGuid(),
                                    mapping.getValue().asEntityHeader(),
                                    mapping.getValue().same);
                        }
                    }
                    for ( ExternalEntityHeader eeh : metadata.getMappableDependencies() ) {
                        Collection<ExternalEntityHeader> persistenceHeaders = mappingModel.updateMappedValues( sourceClusterId, targetClusterId, eeh );
                        for ( ExternalEntityHeader pHeader : persistenceHeaders ) {
                            migrationMappingRecordManager.persistMapping( sourceCluster.getGuid(), pHeader, targetCluster.getGuid(), pHeader.getMappedValue() );
                        }
                    }

                    Collection<GatewayApi.EntityInfo> folders = targetGatewayApi.getEntityInfo( Collections.singleton(com.l7tech.objectmodel.EntityType.FOLDER) );
                    ExternalEntityHeader targetFolderHeader = null;
                    for ( GatewayApi.EntityInfo info : folders ) {
                        if ( targetFolderId.equals( info.getId() ) ) {
                            targetFolderHeader = new ExternalEntityHeader(info.getExternalId(), com.l7tech.objectmodel.EntityType.FOLDER, info.getId(), info.getName(), info.getDescription(), info.getVersion());
                        }
                    }
                    if (targetFolderHeader == null) throw new FindException("Could not find target folder.");

                    metadata.setTargetFolder(targetFolderHeader);
                    metadata.setMigrateFolders(migrateFolders);
                    metadata.setOverwrite(overwriteEntities);
                    metadata.setEnableNewServices(enableNewServices);

                    Collection<MigratedItem> migratedItems = targetMigrationApi.importBundle(bundle, dryRun);
                    MigrationSummary summary = new MigrationSummary(sourceCluster, targetCluster, migratedItems, dryRun,
                                                            targetFolderHeader.getDescription(), migrateFolders, overwriteEntities, enableNewServices);

                    if ( !dryRun ) {
                        if ( migratedItems != null ) {
                            for ( MigratedItem item : migratedItems ) {
                                ExternalEntityHeader source = item.getSourceHeader();
                                ExternalEntityHeader target = item.getTargetHeader();

                                if ( source != null && target != null && item.getOperation().modifiesTarget()) {
                                    migrationMappingRecordManager.persistMapping( sourceCluster.getGuid(), source, targetCluster.getGuid(), target, true );
                                }
                            }
                        }
                        migrationRecordManager.create( label, getUser(), sourceCluster, targetCluster, summary, bundle );
                    }

                    summaryString = summary.toString();
                }
            }
        } catch ( GatewayException ge ) {
            logger.log( Level.INFO, "Error while performing migration '"+ExceptionUtils.getMessage(ge)+"'.", ExceptionUtils.getDebugException(ge) );
            throw new MigrationFailedException("Migration failed '"+ ExceptionUtils.getMessage(ge)+"'.");
        } catch ( FindException fe ) {
            logger.log( Level.WARNING, "Error while performing migration.", fe );
            throw new MigrationFailedException("Migration failed '"+ ExceptionUtils.getMessage(fe)+"'.");
        } catch ( SaveException se ) {
            logger.log( Level.WARNING, "Error while performing migration.", se );
            throw new MigrationFailedException("Migration failed '"+ ExceptionUtils.getMessage(se)+"'.");
        } catch ( MigrationApi.MigrationException me ) {
            logger.log( Level.INFO, "Error while performing migration '"+ExceptionUtils.getMessage(me)+"'.", ExceptionUtils.getDebugException(me) );
            throw new MigrationFailedException(summarizeMigrationException(me));
        } catch (GatewayApi.GatewayException ge) {
            logger.log( Level.INFO, "Error while performing migration '"+ExceptionUtils.getMessage(ge)+"'.", ExceptionUtils.getDebugException(ge) );
            throw new MigrationFailedException("Migration failed '"+ ExceptionUtils.getMessage(ge)+"'.");
        }

        return summaryString;
    }

    private String summarizeMigrationException(MigrationApi.MigrationException me) {
        return "Migration failed: " + me.getMessage() + (me.hasErrors() ? " : \n\n" + me.getErrors() : "");
    }

    /**
     *
     */
    private String summarize( final DependencyItemsRequest request, final String targetClusterId, final Collection<DependencyItem> dependencies, final EntityMappingModel mappings ) {
        StringBuilder builder = new StringBuilder();

        builder.append("<p>Found ");
        builder.append(dependencies.size());
        builder.append(" dependencies.</p>");

        builder.append("<p>");
        int count = 0;
        for ( DependencyItem item : dependencies ) {
            DependencyKey sourceKey = new DependencyKey( request.clusterId, item.asEntityHeader() );
            Pair<DependencyKey,String> mapKey = new Pair<DependencyKey,String>( sourceKey, targetClusterId );
            if ( !item.isOptional() && !mappings.dependencyMap.containsKey(mapKey) ) {
                count++;
            }
        }
        builder.append(count);
        builder.append(" dependencies require mapping.</p>");


        return builder.toString();
    }

    private static String toImgIcon( final Boolean resolved ) {
        String icon = "";

        if ( resolved != null ) {
            if ( resolved ) {
                icon = "<img src=/images/resolved.png />"; //TODO JSON quote escaping
            } else {
                icon = "<img src=/images/unresolved.png />"; //TODO JSON quote escaping
            }

        }

        return icon;
    }

    /**
     * Searchable items will have the search controls enabled.
     */
    private static boolean isSearchable( final ExternalEntityHeader header ) {
        return header != null &&
               com.l7tech.objectmodel.EntityType.VALUE_REFERENCE != header.getType() &&
               MigrationMappingSelection.REQUIRED != header.getValueMapping();

    }

    /**
     * Hidden dependency items are not shown in the item dependencies view, but may be mappable.
     */
    private static boolean isHiddenDependency( final DependencyItem item ) {
        return  item.asEntityHeader().getType() == com.l7tech.objectmodel.EntityType.VALUE_REFERENCE;
    }

    public Boolean isResolved( final EntityMappingModel mappingModel,
                               final ExternalEntityHeader externalEntityHeader,
                               final String sourceClusterId,
                               final String targetClusterId ) {
        boolean resolved = false;

        if ( mappingModel != null && targetClusterId != null && sourceClusterId != null) {
            DependencyKey sourceKey = new DependencyKey( sourceClusterId, externalEntityHeader );
            Pair<DependencyKey,String> mappingKey = new Pair<DependencyKey,String>( sourceKey, targetClusterId );
            DependencyItem mappingValue = mappingModel.dependencyMap.get( mappingKey );
            if ( mappingValue != null ) {
                resolved = true;
            }
        }

        return resolved;
    }

    private static class DependencyItemsRequest implements JSON.Convertible, Serializable {
        private String clusterId;
        private DependencyItem[] entities;

        @Override
        public String toString() {
            return "DependencyItemsRequest[clusterId='"+clusterId+"'; entities="+Arrays.asList(entities)+"]";
        }

        @Override
        public void toJSON(final JSON.Output out) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void fromJSON(final Map data) {
            clusterId = (String)data.get("clusterId");
            Object[] entitiesMap = (Object[])data.get("entities");
            if ( entitiesMap==null ) {
                entities = new DependencyItem[0];
            } else {
                entities = new DependencyItem[entitiesMap.length];
                int i=0;
                for ( Object entityMap : entitiesMap ) {
                    entities[i] = new DependencyItem();
                    entities[i++].fromJSON((Map)entityMap);
                }
            }
        }

        public Collection<ExternalEntityHeader> asEntityHeaders() {
            Collection<ExternalEntityHeader> headers = new ArrayList<ExternalEntityHeader>();

            for ( DependencyItem entity : entities ) {
                com.l7tech.objectmodel.EntityType type = JSONConstants.EntityType.ENTITY_TYPE_MAP.get( entity.type );
                if ( type != null ) {
                    headers.add( entity.asEntityHeader() );
                } else {
                    logger.warning("Entity with unknown type '"+entity.type+"' requested.");
                }
            }

            return headers;
        }
    }

    private static final class DependencyKey implements Serializable {
        private final String clusterId;
        private final com.l7tech.objectmodel.EntityType type;
        private final String id;
        private final String mappingKey;
        private final ExternalEntityHeader header;

        DependencyKey( final String clusterId,
                       final ExternalEntityHeader header ) {
            this.clusterId = clusterId;
            this.type = header.getType();
            this.id = header.getExternalId();
            this.header = header;
            this.mappingKey = header.getMappingKey();
        }

        @Override
        public String toString() {
            return "DependencyKey[clusterId='"+clusterId+"'; id='"+id+"'; type='"+type+"']";
        }

        @Override
        @SuppressWarnings({"RedundantIfStatement"})
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            DependencyKey that = (DependencyKey) o;

            if (!clusterId.equals(that.clusterId)) return false;
            if (!id.equals(that.id)) return false;
            if (type != that.type) return false;
            if (mappingKey != null ? !mappingKey.equals(that.mappingKey) : that.mappingKey != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result;
            result = clusterId.hashCode();
            result = 31 * result + type.hashCode();
            result = 31 * result + id.hashCode();
            result = 31 * result + (mappingKey != null ? mappingKey.hashCode() : 0);
            return result;
        }

        public ExternalEntityHeader asEntityHeader() {
            return header;
        }
    }

    private static final class DependencyItem implements Comparable, JSON.Convertible, Serializable {
        private ExternalEntityHeader entityHeader;

        private String uid; // type:mappingKey
        private String id;
        private String type;
        private String mappingKey;
        private String name;
        private Integer version;

        private Boolean resolved;
        private boolean hidden; // flag for hidden in the UI
        @SuppressWarnings({"UnusedDeclaration"})
        private String ownerName; // todo: is this used?
        private String destName;
        private boolean same; // flag for this entity is the same identity on source/target

        public DependencyItem() {
        }

        public DependencyItem( final ExternalEntityHeader entityHeader  ) {
            this( entityHeader, null, false );
        }

        public DependencyItem( final ExternalEntityHeader entityHeader,
                               final Boolean resolved ) {
            this( entityHeader, resolved, false );
        }

        public DependencyItem( final ExternalEntityHeader entityHeader,
                               final Boolean resolved,
                               final boolean hidden ) {
            this( entityHeader, resolved, hidden, false );
        }

        /**
         * Contructs a new dependency item copying any metadata from the given item.
         *
         * <p>This is useful when getting updated entity header information.</p>
         *
         * @param entityHeader The header to use
         * @param dependencyItem The dependency item to use for metadata
         */
        public DependencyItem( final ExternalEntityHeader entityHeader,
                               final DependencyItem dependencyItem ) {
            this( entityHeader, dependencyItem.resolved, dependencyItem.hidden, dependencyItem.same );
        }

        public DependencyItem( final ExternalEntityHeader entityHeader,
                               final Boolean resolved,
                               final boolean hidden,
                               final boolean isSame ) {
            this.entityHeader = entityHeader;
            this.uid = entityHeader.getType().toString() +":" + entityHeader.getMappingKey();
            this.id = entityHeader.getExternalId();
            this.type = entityHeader.getType().toString();
            this.mappingKey = entityHeader.getMappingKey();
            this.name = entityHeader.getName();
            this.resolved = resolved;
            this.hidden = hidden;
            this.version = entityHeader.getVersion();
            this.same = isSame;
        }

        public boolean isOptional() {
            return resolved == null;
        }

        public boolean isResolved() {
            return resolved == null || resolved;
        }

        public String getType() {
            return com.l7tech.objectmodel.EntityType.valueOf(type).getName().toLowerCase();
        }

        public String getOptional() {
            return toImgIcon(resolved);
        }

        @Override
        public String toString() {
            return "DependencyItem[id='"+id+"'; type='"+type+"'; name='"+name+"']";
        }

        @Override
        public void toJSON(JSON.Output out) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void fromJSON(final Map data) {
            id = (String)data.get("id");
            type = (String)data.get("type");
            name = (String)data.get("name");
            version = Integer.parseInt((String)data.get("version"));
        }

        @Override
        public int compareTo(Object o) {
            DependencyItem otherItem = (DependencyItem) o;
            int compared = getDisplayNameWithScope().toLowerCase().compareTo( otherItem.getDisplayNameWithScope().toLowerCase() );
            if ( compared == 0 ) {
                compared = getType().compareTo( otherItem.getType() );
            }

            return compared;
        }

        @SuppressWarnings({"RedundantIfStatement"})
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            DependencyItem that = (DependencyItem) o;

            return asEntityHeader().equals(that.asEntityHeader());
        }

        @Override
        public int hashCode() {
            return asEntityHeader().hashCode();
        }

        public String getVersionAsString() {
            return version==null ? "" : Integer.toString(version);
        }

        public ExternalEntityHeader asEntityHeader() {
            if ( entityHeader == null ) {
                EntityType entityType = JSONConstants.EntityType.ENTITY_TYPE_MAP.get( type );
                entityHeader = new ExternalEntityHeader( id, entityType, null, name, null, getVersion() );
            }
            return entityHeader;
        }

        public Integer getVersion() {
            return version;
        }

        public String getDestName() {
            return destName;
        }

        public String getDisplayName() {
            return entityHeader != null ? entityHeader.getDisplayName() : name;
        }

        public String getDisplayNameWithScope() {
            String displayName = entityHeader != null ? entityHeader.getDisplayNameWithScope() : getDisplayName();

            int index = displayName.lastIndexOf("http://");
            if ( index < 0 ) {
                index = displayName.lastIndexOf("https://");
            }

            if ( index >= 0 ) {
                displayName = displayName.substring( 0, index ) + " \\n" + displayName.substring( index );                
            }

            return displayName;
        }
    }

    /**
     * A model to store the selected entity mappings (anything the user has
     * selected, not just mappings for currently selected source entities)
     */
    public final static class EntityMappingModel implements Serializable {
        /**
         * Map for entity mappings keyed by source item / target cluster, values are the target item.
         */
        private final Map<Pair<DependencyKey,String>,DependencyItem> dependencyMap =
            new HashMap<Pair<DependencyKey, String>, DependencyItem>();

        /**
         * Map for value mappings keyed by source item / target cluster, values are the (string) mapped values
         */
        private final Map<Pair<ValueKey,String>,String> valueMap =
            new HashMap<Pair<ValueKey,String>,String>();

        /**
         * Update the mapped values in the given entity header.
         *
         * @param sourceClusterId The source cluster id for the mapping
         * @param targetClusterId The destination cluster id for the mapping
         * @param eeh The ExternalEntityHeader to update.
         * @return The collection of values for persistence
         */
        public Collection<ExternalEntityHeader> updateMappedValues(final String sourceClusterId,
                                                                   final String targetClusterId,
                                                                   final ExternalEntityHeader eeh) {

            Collection<ExternalEntityHeader> pHeaders = new LinkedHashSet<ExternalEntityHeader>();

            if (eeh.isValueMappable()) {

                ExternalEntityHeader.ValueType valueType = eeh.getValueType();
                Collection<String> mappedValues = new ArrayList<String>();
                for (ExternalEntityHeader vmHeader : eeh.getValueMappableHeaders()) {
                    final Pair<ValueKey, String> valueKey = new Pair<ValueKey, String>(new ValueKey(sourceClusterId, vmHeader), targetClusterId);
                    final String mappedValue = valueMap.get(valueKey);
                    mappedValues.add(mappedValue != null ? mappedValue : vmHeader.getDisplayValue());
                    if (mappedValue != null) {
                        vmHeader.setMappedValue(mappedValue);
                        pHeaders.add(vmHeader);
                    }
                }

                if (!pHeaders.isEmpty()) {
                    // there actually were mapped values
                    eeh.setMappedValue(valueType.serialize(mappedValues.toArray(new String[mappedValues.size()])));
                }
            }

            return pHeaders;
        }
    }

    private final static class ValueKey implements Serializable {
        private final String sourceClusterId;
        private final String mappingKey;

        public ValueKey( final DependencyKey dependencyKey ) {
            this( dependencyKey.clusterId, dependencyKey.asEntityHeader() );
        }

        public ValueKey( final String sourceClusterId, final ExternalEntityHeader valueMappableHeader ) {
            if (  valueMappableHeader.getValueType().isArray() )
                logger.log(Level.WARNING, "Value key must not be array type.");
                //throw new IllegalArgumentException("Value key must not be array type.");
            this.sourceClusterId = sourceClusterId;
            this.mappingKey = valueMappableHeader.getMappingKey();
        }

        @SuppressWarnings({"RedundantIfStatement"})
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ValueKey valueKey = (ValueKey) o;

            if (mappingKey != null ? !mappingKey.equals(valueKey.mappingKey) : valueKey.mappingKey != null)
                return false;
            if (sourceClusterId != null ? !sourceClusterId.equals(valueKey.sourceClusterId) : valueKey.sourceClusterId != null)
                return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result;
            result = (sourceClusterId != null ? sourceClusterId.hashCode() : 0);
            result = 31 * result + (mappingKey != null ? mappingKey.hashCode() : 0);
            return result;
        }
    }

    private final static class SearchTarget implements Serializable, Comparable {
        private final DependencyItem item;

        public SearchTarget() {
            this.item = null;
        }

        public SearchTarget( final DependencyItem item ) {
            this.item = item;
        }

        @Override
        public String toString() {
            return item == null ? "-" : item.getDisplayName();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            SearchTarget that = (SearchTarget) o;

            if ( that.item == null && item == null ) {
                return true;
            } else if ( that.item != null && this.item != null && that.item.id.equals(this.item.id) ) {
                return true;
            }

            return false;
        }

        @Override
        public int hashCode() {
            return item == null ? 1 : item.id.hashCode();
        }

        @Override
        public int compareTo(Object o) {
            SearchTarget other = (SearchTarget) o;
            return this.toString().toLowerCase().compareTo(other.toString().toLowerCase());
        }
    }

    /**
     * A model to store a search manner and a value to search.
     */
    public final static class SearchModel implements Serializable {
        private SearchTarget searchTarget;
        private String searchManner;
        private String searchValue;

        public SearchTarget getSearchTarget() {
            return searchTarget;
        }

        public void setSearchTarget(SearchTarget searchTarget) {
            this.searchTarget = searchTarget;
        }

        public String getSearchManner() {
            return searchManner;
        }

        public void setSearchManner(String searchManner) {
            this.searchManner = searchManner;
        }

        public String getSearchValue() {
            return searchValue;
        }

        public void setSearchValue(String searchValue) {
            this.searchValue = searchValue;
        }

        /**
         * Get the filter for the selected manner/value
         */
        public String getSearchFilter() {
            String filter = searchValue;
            if ( filter == null ) {
                filter = "";                
            } else if ( "contains".equals(searchManner) ) {
                filter = "*" + filter + (filter.length() > 0 ? "*" : "");
            } else {
                filter = filter + "*";
            }
            return filter;
        }
    }

    /**
     * Model to store candidate information
     */
    public static final class CandidateModel implements Serializable {
        private String name;
        private String type;

        public CandidateModel() {
            reset();
        }

        public void reset() {
            name = "-";
            type = "-";
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

    /**
     * A model to store dependency summary
     */
    public final static class DepenencySummaryModel implements Serializable {
        private int totalDependencies;
        private int unmappedDependencies;
        private int requiredUnmappedDependencies;

        public void reset() {
            totalDependencies = 0;
            unmappedDependencies = 0;
            requiredUnmappedDependencies = 0;    
        }

        public void incrementTotalDependencies() {
            totalDependencies++;
        }

        public void incrementUnmappedDependencies() {
            totalDependencies++;
            unmappedDependencies++;
        }

        public void incrementRequiredUnmappedDependencies() {
            totalDependencies++;
            unmappedDependencies++;
            requiredUnmappedDependencies++;
        }

        public int getRequiredUnmappedDependencies() {
            return requiredUnmappedDependencies;
        }

        public void setRequiredUnmappedDependencies(int requiredUnmappedDependencies) {
            this.requiredUnmappedDependencies = requiredUnmappedDependencies;
        }

        public int getTotalDependencies() {
            return totalDependencies;
        }

        public void setTotalDependencies(int totalDependencies) {
            this.totalDependencies = totalDependencies;
        }

        public int getUnmappedDependencies() {
            return unmappedDependencies;
        }

        public void setUnmappedDependencies(int unmappedDependencies) {
            this.unmappedDependencies = unmappedDependencies;
        }
    }
}
