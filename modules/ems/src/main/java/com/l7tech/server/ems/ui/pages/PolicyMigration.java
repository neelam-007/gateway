package com.l7tech.server.ems.ui.pages;

import com.l7tech.common.io.ByteLimitInputStream;
import com.l7tech.gateway.common.security.rbac.AttemptedReadAll;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.*;
import static com.l7tech.objectmodel.migration.MigrationMappingSelection.*;
import com.l7tech.objectmodel.migration.MigrationDependency;
import com.l7tech.server.ems.enterprise.*;
import com.l7tech.server.ems.gateway.*;
import com.l7tech.server.ems.migration.*;
import com.l7tech.server.ems.ui.NavigationPage;
import com.l7tech.server.ems.util.TypedPropertyColumn;
import com.l7tech.server.management.api.node.GatewayApi;
import com.l7tech.server.management.api.node.MigrationApi;
import com.l7tech.server.management.migration.bundle.ExportedItem;
import com.l7tech.server.management.migration.bundle.MigratedItem;
import com.l7tech.server.management.migration.bundle.MigrationBundle;
import com.l7tech.server.management.migration.bundle.MigrationMetadata;
import com.l7tech.util.ArrayUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.Pair;
import com.l7tech.util.TextUtils;
import com.l7tech.util.ValidationUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.calldecorator.AjaxCallDecorator;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import javax.inject.Inject;

import javax.xml.ws.WebServiceException;
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
        srcClusterModel = new Model<String>();
        destClusterModel = new Model<String>();
        mappingModel = new EntityMappingModel();

        dialogContainer = new WebMarkupContainer("dialogContainer");
        dialogContainer.add( new EmptyPanel("dialog") );
        add( dialogContainer.setOutputMarkupId(true) );

        srcItemDependencies = new WebMarkupContainer("srcItemDependencies");
        add( srcItemDependencies.setOutputMarkupId(true) );
        srcItemDetails = new WebMarkupContainer("srcItemDetails");
        add( srcItemDetails.setOutputMarkupId(true) );

        showDependencies( srcItemDependencies, Collections.<DependencyItem>emptyList(), srcItemDetails, null );

        destItemDependencies = new WebMarkupContainer("destItemDependencies");
        add( destItemDependencies.setOutputMarkupId(true) );
        destItemDetails = new WebMarkupContainer("destItemDetails");
        add( destItemDetails.setOutputMarkupId(true) );
        showDependencies( destItemDependencies, Collections.<DependencyItem>emptyList(), destItemDetails, null );

        final Set<EntityType> highlightEntities = EnumSet.of( EntityType.SERVICE, EntityType.SERVICE_ALIAS, EntityType.POLICY, EntityType.POLICY_ALIAS );
        final Set<EntityType> selectEntities = EnumSet.of( EntityType.FOLDER, EntityType.SERVICE, EntityType.SERVICE_ALIAS, EntityType.POLICY, EntityType.POLICY_ALIAS );
        srcContentSelector = new SSGClusterContentSelectorPanel( "srcSSGClusterContentSelector", getUserModel(), null, highlightEntities, selectEntities, true ){
            @Override
            protected void onEntityHighlighted( final AjaxRequestTarget target, final EntityType entityType, final String entityTypeStr, final String entityId ) {
                refreshDependencyDisplayModel( srcItemDetails, srcItemDependencies, srcClusterModel.getObject(), entityTypeStr,  entityId);
                target.addComponent( srcItemDetails );
                target.addComponent( srcItemDependencies );
                enableDisableMainButtons( target );
            }
            @Override
            protected void onSelectionChanged( final AjaxRequestTarget target ) {
                enableDisableMainButtons( target );
            }
        };
        add( srcContentSelector );

        final AjaxCallback refreshCallback = new AjaxCallback(){
            @Override
            public void call( final AjaxRequestTarget target ) {
                enableDisableMainButtons( target );
            }
        };
        srcClusterSelector = new MigrationSSGClusterSelectorPanel( "srcSSGClusterSelector", getUserModel(), srcClusterModel, srcContentSelector, dialogContainer, srcItemDetails, srcItemDependencies, refreshCallback );
        add( srcClusterSelector );

        destContentSelector = new SSGClusterContentSelectorPanel( "destSSGClusterContentSelector", getUserModel(), null, highlightEntities, EnumSet.of( EntityType.FOLDER ), false ){
            @Override
            protected void onEntityHighlighted( final AjaxRequestTarget target, final EntityType entityType, final String entityTypeStr, final String entityId ) {
                refreshDependencyDisplayModel( destItemDetails, destItemDependencies, destClusterModel.getObject(), entityTypeStr, entityId );
                target.addComponent( destItemDetails );
                target.addComponent( destItemDependencies );
            }

            @Override
            protected void onSelectionChanged( final AjaxRequestTarget target ) {
                enableDisableMainButtons( target );
            }
        };
        add( destContentSelector );

        destClusterSelector = new MigrationSSGClusterSelectorPanel( "destSSGClusterSelector", getUserModel(), destClusterModel, destContentSelector, dialogContainer, destItemDetails, destItemDependencies, refreshCallback );
        add( destClusterSelector );

        final WebMarkupContainer refreshSourceClusterContainer = new WebMarkupContainer( "refreshSourceCluster" );
        refreshSourceClusterContainer.add( new SSGClusterRefreshBehaviour( srcClusterSelector, srcContentSelector, srcItemDetails, srcItemDependencies ) );
        add( refreshSourceClusterContainer );

        final WebMarkupContainer refreshDestinationClusterContainer = new WebMarkupContainer( "refreshDestinationCluster" );
        refreshDestinationClusterContainer.add( new SSGClusterRefreshBehaviour( destClusterSelector, destContentSelector, destItemDetails, destItemDependencies ) );
        add( refreshDestinationClusterContainer );

        final WebMarkupContainer refreshSourceItemContainer = new WebMarkupContainer( "refreshSourceItem" );
        refreshSourceItemContainer.add( new ContentRefreshBehaviour( srcContentSelector, srcItemDetails, srcItemDependencies ) );
        add( refreshSourceItemContainer );

        final WebMarkupContainer refreshDestinationItemContainer = new WebMarkupContainer( "refreshDestinationItem" );
        refreshDestinationItemContainer.add( new ContentRefreshBehaviour( destContentSelector, destItemDetails, destItemDependencies ) );
        add( refreshDestinationItemContainer );

        dependenciesContainer = new WebMarkupContainer("dependencies");
        dependenciesContainer.setOutputMarkupPlaceholderTag(true);
        dependencyCandidateContainer = new WebMarkupContainer("dependencyCandidateContainer");
        dependenciesOptionsContainer = new Form("dependencyOptionsContainer");
        dependencyRefreshContainers = new WebMarkupContainer[]{ dependencyCandidateContainer, dependenciesOptionsContainer };

        YuiDataTable.contributeHeaders(this);

        migrateFoldersModel = new Model<Boolean>(true);
        enableNewServiceModel = new Model<Boolean>(false);
        overwriteDependenciesModel = new Model<Boolean>(false);
        offlineDestinationModel = new Model<Boolean>(false);

        migrateFoldersCheckBox = new AjaxCheckBox("migrateFoldersCheckBox", migrateFoldersModel){
            @Override
            protected void onUpdate( final AjaxRequestTarget target ) {}
        };
        add( migrateFoldersCheckBox.setOutputMarkupId( true ).setMarkupId( "migrateFoldersCheckBox" ) );

        enableNewServicesCheckBox = new AjaxCheckBox("enableNewServicesCheckBox", enableNewServiceModel){
            @Override
            protected void onUpdate( final AjaxRequestTarget target ) {}
        };
        add( enableNewServicesCheckBox.setOutputMarkupId( true ).setMarkupId( "enableNewServicesCheckBox" ) );

        overwriteDependenciesCheckBox = new AjaxCheckBox("overwriteDependenciesCheckBox", overwriteDependenciesModel){
            @Override
            protected void onUpdate( final AjaxRequestTarget target ) {}
        };
        add( overwriteDependenciesCheckBox.setOutputMarkupId( true ).setMarkupId( "overwriteDependenciesCheckBox" ) );

        offlineDestinationCheckBox = new AjaxCheckBox("offlineDestinationCheckBox", offlineDestinationModel){
            @Override
            protected void onUpdate( final AjaxRequestTarget target ) {
                updateOfflineDestination( target );
            }
        };
        add( offlineDestinationCheckBox.setOutputMarkupId( true ).setMarkupId( "offlineDestinationCheckBox" ) );

        dependencySummaryModel = new DependencySummaryModel();
        candidateModel = new CandidateModel();
        searchModel = new SearchModel();

        reloadMigrationButton = new YuiAjaxButton("reloadMigrationButton") {
            @Override
            protected void onSubmit( final AjaxRequestTarget target, final Form form ) {
                onReloadMigration( target, form );
            }
        };

        final List<PreviousMigrationModel> previous = loadPreviousMigrations();
        final Form reloadForm = new Form("reloadForm");
        reloadForm.add( reloadMigrationButton.setOutputMarkupId(true).setEnabled(!previous.isEmpty()) );
        reloadForm.add( new DropDownChoice<PreviousMigrationModel>( "reloadSelect", new Model<PreviousMigrationModel>( previous.isEmpty() ? null : previous.iterator().next() ), previous ) {
            @Override
            protected String getDefaultChoice( final Object selected ) {
                return "-";
            }
        } );
        add( reloadForm );

        final WebMarkupContainer container = new WebMarkupContainer("refresh");
        container.add( new AjaxEventBehavior("onclick"){
            @Override
            protected IAjaxCallDecorator getAjaxCallDecorator() {
                return new BusyAjaxCallDecorator();
            }

            @Override
            protected void onEvent( final AjaxRequestTarget target ) {
                List<PreviousMigrationModel> previous = loadPreviousMigrations();
                @SuppressWarnings({"unchecked"})
                DropDownChoice<PreviousMigrationModel> reloadDropDown = (DropDownChoice<PreviousMigrationModel>) reloadForm.get("reloadSelect");
                reloadDropDown.setChoices( previous );
                reloadDropDown.setModelObject( previous.isEmpty() ? null : previous.iterator().next() );                
                reloadMigrationButton.setEnabled(!previous.isEmpty());

                target.addComponent( reloadForm );
            }
        } );
        add( container );

        dependencyLoadButton = new YuiAjaxButton("dependencyLoadButton") {
            @Override
            protected void onSubmit( final AjaxRequestTarget target, final Form form ) {
                onLoadDependency( target );
            }
        };

        final YuiAjaxButton clearDependencyButton = new YuiAjaxButton( "dependencyClearButton" ) {
            @Override
            protected void onSubmit( final AjaxRequestTarget target, final Form form ) {
                setEnabled( false );
                onClearDependency( target );
            }
        };

        final YuiAjaxButton editDependencyButton = new YuiAjaxButton( "dependencyEditButton" ) {
            @Override
            protected void onSubmit( final AjaxRequestTarget target, final Form form ) {
                onEditDependency( target );
            }
        };

        final Form dependencyControlsForm = new Form("dependencyControlsForm");
        dependencyControlsForm.add( dependencyLoadButton.setOutputMarkupId(true).setEnabled(false) );
        dependencyControlsForm.add( clearDependencyButton.setOutputMarkupId( true ).setEnabled(false) );
        dependencyControlsForm.add( editDependencyButton.setOutputMarkupId( true ).setEnabled( false ) );

        final Form identifyDependenciesForm = new Form("identifyDependenciesForm");
        add( identifyDependenciesForm );
        identifyDependenciesButton = new YuiAjaxButton( "identifyDependenciesButton" , "Identify dependencies" ) {
            @Override
            protected void onSubmit( final AjaxRequestTarget target, final Form<?> form ) {
                onIdentifyDependencies( target );
            }
        };
        identifyDependenciesForm.add( identifyDependenciesButton.setMarkupId( "identifyDependenciesButton" ).setEnabled( false ) ); // markup id is set to match CSS

        final Form migrateForm = new Form("migrateForm");
        add( migrateForm );
        migrateButton = new YuiAjaxButton("migrateButton", "Migrate selected policies with dependency mapping"){
            @Override
            protected void onSubmit( final AjaxRequestTarget target, final Form<?> form ) {
                onMigrate( target );
            }
        };
        migrateForm.add( migrateButton.setMarkupId( "migrateButton" ).setEnabled( false ) ); // markup id is set to match CSS

        dependenciesContainer.add( new Label("dependenciesTotalLabel", new PropertyModel(dependencySummaryModel, "totalDependencies")).setOutputMarkupId(true) );
        dependenciesContainer.add( new Label("dependenciesUnmappedLabel", new PropertyModel(dependencySummaryModel, "unmappedDependencies")).setOutputMarkupId(true) );
        dependenciesContainer.add( new Label("dependenciesRequiredUnmappedLabel", new PropertyModel(dependencySummaryModel, "requiredUnmappedDependencies")).setOutputMarkupId(true) );
        dependenciesContainer.add( dependencyControlsForm.setOutputMarkupId(true) );

        updateDependencies();

        dependenciesContainer.add( dependencyCandidateContainer.setOutputMarkupId(true) );
        dependenciesContainer.add( dependenciesOptionsContainer.setOutputMarkupId(true) );

        dependenciesOptionsContainer.add(new DropDownChoice<SearchTarget>("dependencySearchTarget", new PropertyModel<SearchTarget>(searchModel, "searchTarget"), Arrays.asList(new SearchTarget())));
        String[] searchManners = new String[] {
            "contains",
            "starts with"
        };
        dependenciesOptionsContainer.add(new DropDownChoice<String>("dependencySearchManner", new PropertyModel<String>(searchModel, "searchManner"), Arrays.asList(searchManners)) {
            @Override
            protected CharSequence getDefaultChoice(Object o) {
                return "contains";
            }
        });
        dependenciesOptionsContainer.add(new TextField<String>("dependencySearchText", new PropertyModel<String>(searchModel, "searchValue")));
        dependenciesOptionsContainer.add(new YuiAjaxButton("dependencySearchButton", dependenciesOptionsContainer) {
            @Override
            protected void onSubmit( final AjaxRequestTarget target, final Form form ) {
                logger.fine("Searching for dependencies with filter '"+searchModel.getSearchFilter()+"'.");
                addDependencyOptions( false );
                target.addComponent( dependenciesOptionsContainer );
            }
        });
        dependencyCandidateContainer.add( new Label("dependencyCandidateName", new PropertyModel(candidateModel, "name")) );
        dependencyCandidateContainer.add( new Label("dependencyCandidateType", new PropertyModel(candidateModel, "type")) );

        addDependencyOptions( true );

        add( dependenciesContainer.setOutputMarkupId(true) );
    }

    @Override
    public void detachModels() {
        super.detachModels();

        for ( final MigrationSource source : detailProvidersByCluster.values() ) {
            if ( source instanceof IDetachable ) {
                ((IDetachable)source).detach();
            }
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( PolicyMigration.class.getName() );

    private static final String[] EXTRA_PROPERTIES = new String[]{ "Policy Revision", "SOAP", "Enabled" };
    private static final String[] DEPENDENCY_REFRESH_COMPONENTS = { "dependenciesTable", "dependenciesTotalLabel", "dependenciesUnmappedLabel", "dependenciesRequiredUnmappedLabel", "dependencyControlsForm" };
    private static final String[] SEARCH_REFRESH_COMPONENTS = { "dependencySearchTarget", "dependencySearchManner", "dependencySearchText", "dependencySearchButton" };

    @Inject
    private SsgClusterManager ssgClusterManager;

    @Inject
    private MigrationRecordManager migrationRecordManager;

    @Inject
    private MigrationMappingRecordManager migrationMappingRecordManager;

    @Inject
    private GatewayClusterClientManager gatewayClusterClientManager;

    @Inject
    private GatewayTrustTokenFactory gatewayTrustTokenFactory;

    private final IModel<String> srcClusterModel;
    private final IModel<String> destClusterModel;
    private final WebMarkupContainer dialogContainer;
    private final WebMarkupContainer srcItemDependencies;
    private final WebMarkupContainer srcItemDetails;
    private final WebMarkupContainer destItemDependencies;
    private final WebMarkupContainer destItemDetails;
    private final SSGClusterContentSelectorPanel srcContentSelector;
    private final SSGClusterSelectorPanel srcClusterSelector;
    private final SSGClusterContentSelectorPanel destContentSelector;
    private final SSGClusterSelectorPanel destClusterSelector;
    private final WebMarkupContainer dependenciesContainer;
    private final WebMarkupContainer dependencyCandidateContainer;
    private final Form dependenciesOptionsContainer;
    private final WebMarkupContainer[] dependencyRefreshContainers;
    private final Model<Boolean> migrateFoldersModel;
    private final Model<Boolean> enableNewServiceModel;
    private final Model<Boolean> overwriteDependenciesModel;
    private final Model<Boolean> offlineDestinationModel;
    private final AjaxCheckBox migrateFoldersCheckBox;
    private final AjaxCheckBox enableNewServicesCheckBox;
    private final AjaxCheckBox overwriteDependenciesCheckBox;
    private final AjaxCheckBox offlineDestinationCheckBox;
    private final DependencySummaryModel dependencySummaryModel;
    private final CandidateModel candidateModel;
    private final SearchModel searchModel;
    private final YuiAjaxButton reloadMigrationButton;
    private final YuiAjaxButton identifyDependenciesButton;
    private final YuiAjaxButton migrateButton;
    private final YuiAjaxButton dependencyLoadButton;
    private final EntityMappingModel mappingModel;
    private DependencyKey lastSourceKey;
    private String lastSourceClusterId;
    private String lastDestinationClusterId;
    private Collection<DependencyItem> lastDependencyItems = Collections.emptyList();
    private final Map<String,MigrationSource> detailProvidersByCluster = new HashMap<String,MigrationSource>();

    private void onReloadMigration( final AjaxRequestTarget target, final Form form ) {
        final PreviousMigrationModel model = (PreviousMigrationModel) form.get("reloadSelect").getDefaultModelObject();
        logger.fine( "Reloading migration '" + model + "'.");
        try {
            final MigrationRecord record = migrationRecordManager.findByPrimaryKeyNoBundle( model.id );
            if ( record != null ) {
                final MigrationSummary summary = record.getMigrationSummary();

                // Update model
                final List<Pair<ExternalEntityHeader,ExternalEntityHeader>> mappings = new ArrayList<Pair<ExternalEntityHeader,ExternalEntityHeader>>();
                for ( MigratedItem item : summary.getMigratedItems() ) {
                    if ( item.getSourceHeader().getType() == EntityType.FOLDER ) {
                        continue;
                    }

                    final ExternalEntityHeader source = item.getSourceHeader();
                    final ExternalEntityHeader destination = item.getTargetHeader();
                    if (item.getOperation().modifiesTarget()) {
                        if ( source.getMappedValue() != null ) {
                            mappingModel.valueMap.put( new Pair<ValueKey,String>(new ValueKey( record.getSourceClusterGuid(), source ), record.getTargetClusterGuid()), source.getMappedValue() );
                        }
                    } else {
                        mappings.add( new Pair<ExternalEntityHeader,ExternalEntityHeader>( source, destination ) );
                    }
                }
                validateAndRestoreMappings( mappingModel, record.getSourceClusterGuid(), record.getTargetClusterGuid(), mappings );
                final SsgCluster srcCluster = this.ssgClusterManager.findByGuid( record.getSourceClusterGuid() );
                final SsgCluster destCluster = this.ssgClusterManager.findByGuid( record.getTargetClusterGuid() );
                final boolean offlineSource = srcCluster != null && srcCluster.isOffline();

                // Update source and destinations
                srcClusterModel.setObject( record.getSourceClusterGuid() );
                destClusterModel.setObject( record.getTargetClusterGuid() );
                srcClusterSelector.refresh();
                destClusterSelector.refresh();
                migrateFoldersModel.setObject( summary.isMigrateFolders() );
                enableNewServiceModel.setObject( summary.isEnableNewServices() );
                overwriteDependenciesModel.setObject( summary.isOverwrite() );
                offlineDestinationModel.setObject( record.getTargetClusterGuid() == null && !offlineSource );
                if ( offlineSource ) {
                    final MigrationRecord fullRecord = migrationRecordManager.findByPrimaryKey(model.id);
                    if (fullRecord == null) {
                        // unexpected as the record was just found above.
                        throw new FindException("Could not find record with id " + model.id);
                    }

                    final MigrationRecordModel migrationRecordModel = new MigrationRecordModel( migrationRecordManager, fullRecord );
                    srcClusterSelector.setIncludedOfflineClusters( Collections.singleton( summary.getSourceClusterGuid() ) );
                    srcContentSelector.setProviderForCluster( summary.getSourceClusterGuid(), new OfflineMigrationContentProvider( migrationRecordModel ) );
                    detailProvidersByCluster.put( summary.getSourceClusterGuid(), new OfflineMigrationSource( migrationRecordModel ) );
                }
                srcContentSelector.setClusterId( summary.getSourceClusterGuid() );
                destContentSelector.setClusterId( summary.getTargetClusterGuid() );
                srcContentSelector.setSelectedContentIds( new HashSet<String>(record.getSourceItems()) );
                if ( record.getTargetFolderId() == null || destCluster==null || JSONConstants.SsgClusterOnlineState.DOWN.equals( destCluster.getOnlineStatus() ) ) {
                    destContentSelector.setSelectedContentIds( Collections.<String>emptySet() );
                } else {
                    destContentSelector.setSelectedContentIds( Collections.singleton( record.getTargetFolderId() ) );
                }
                showDependencies( srcItemDependencies, Collections.<DependencyItem>emptyList(), srcItemDetails, null );
                showDependencies( destItemDependencies, Collections.<DependencyItem>emptyList(), destItemDetails, null );

                // Clear dependencies mapping section
                lastSourceKey = null;
                lastSourceClusterId = null;
                lastDestinationClusterId = null;
                lastDependencyItems = Collections.emptyList();
                updateDependencies();
                addDependencyOptions( true );

                target.addComponent( srcClusterSelector );
                target.addComponent( srcContentSelector );
                target.addComponent( migrateFoldersCheckBox );
                target.addComponent( enableNewServicesCheckBox );
                target.addComponent( overwriteDependenciesCheckBox );
                target.addComponent( offlineDestinationCheckBox );
                target.addComponent( srcItemDetails );
                target.addComponent( srcItemDependencies );
                target.addComponent( destItemDetails );
                target.addComponent( destItemDependencies );
                target.addComponent( dependenciesContainer );
                updateOfflineDestination( target );
            }
        } catch ( FindException fe ) {
            logger.log( Level.WARNING, "Unexpected error when loading previous migration.", fe );
            popupCloseDialog(dialogContainer, target, "Error Loading Previous Migration", ExceptionUtils.getMessage( fe ));
        }
    }

    private void onClearDependency( final AjaxRequestTarget target ) {
        final Pair<DependencyKey,String> mappingKey = new Pair<DependencyKey,String>(lastSourceKey, lastDestinationClusterId );
        final Pair<ValueKey,String> valueKey = lastSourceKey.asEntityHeader().isValueMappable() ?
            new Pair<ValueKey,String>(new ValueKey(lastSourceKey), lastDestinationClusterId ):
            null;

        mappingModel.dependencyMap.remove( mappingKey );
        if( valueKey != null ) mappingModel.valueMap.remove( valueKey );
        updateDependencies();
        for ( String id : DEPENDENCY_REFRESH_COMPONENTS ) target.addComponent( dependenciesContainer.get(id) );
        target.addComponent( dependencyCandidateContainer );
        target.addComponent( dependenciesOptionsContainer );
    }

    private void onEditDependency( final AjaxRequestTarget target ) {
        if ( lastSourceKey != null && lastSourceKey.header.isValueMappable()) {
            ExternalEntityHeader.ValueType valueType = lastSourceKey.header.getValueType();
            String displayValue = lastSourceKey.header.getDisplayValue();
            String mappedValue = lastSourceKey.header.getMappedValue();
            if ( displayValue == null ) displayValue = "";

            String prompt = "Enter new value.";
            String regex = "^(?:.{1,8192})$";
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

            final PolicyMigrationMappingValueEditPanel mapping = new PolicyMigrationMappingValueEditPanel( YuiDialog.getContentId(), prompt, displayValue, mappedValue, regex);
            YuiDialog resultDialog = new YuiDialog("dialog", "Edit Mapping Value", YuiDialog.Style.OK_CANCEL, mapping, new YuiDialog.OkCancelCallback(){
                @Override
                public void onAction( final YuiDialog dialog, final AjaxRequestTarget target, final YuiDialog.Button button ) {
                    if ( button == YuiDialog.Button.OK ) {
                        lastSourceKey.header.setMappedValue(mapping.getValue());
                        final Collection<DependencyItem> items = lastDependencyItems;
                        DependencyItem updatedItem = null;
                        if ( items != null ) {
                            for ( DependencyItem item : items ) {
                                if ( item.type.equals(lastSourceKey.header.getType().toString()) && item.id.equals(lastSourceKey.header.getExternalId()) ) {
                                    mappingModel.valueMap.put( new Pair<ValueKey,String>(new ValueKey(lastSourceKey), lastDestinationClusterId ), mapping.getValue() );
                                    updatedItem = item;
                                    break;
                                }
                            }
                        }

                        if ( updatedItem != null ) {
                            for ( DependencyItem item : items ) {
                                if ( item != updatedItem && isValueMappingSameOwnerAndTypeAndValue(item, updatedItem) ||
                                     (isValueMappingSameTypeAndValue(item, updatedItem) && mapping.isApplyToAll())) {
                                    mappingModel.valueMap.put( new Pair<ValueKey,String>( new ValueKey(lastSourceKey.clusterId, item.asEntityHeader()), lastDestinationClusterId ), mapping.getValue() );
                                }
                            }
                        }

                        updateDependencies();
                        for ( String id : DEPENDENCY_REFRESH_COMPONENTS ) target.addComponent( dependenciesContainer.get(id) );
                    }
                }
            });
            dialogContainer.replace( resultDialog );
            target.addComponent( dialogContainer );
        }
    }

    private void onLoadDependency( final AjaxRequestTarget target ) {
        final String srcClusterId = lastSourceClusterId;
        final String destClusterId = lastDestinationClusterId;
        final Collection<DependencyItem> items =  lastDependencyItems;

        try {
            final int before = countUnMappedDependencies( mappingModel, srcClusterId, destClusterId, items  );
            loadMappings( mappingModel, srcClusterId, destClusterId, items, false );
            final int after = countUnMappedDependencies( mappingModel, srcClusterId, destClusterId, items  );
            if ( before != after ) {
                updateDependencies();
                addDependencyOptions( true );

                popupCloseDialog(dialogContainer, target, "Loaded Previous Mappings", "Loaded " +(before-after)+ " previous mappings.");
                target.addComponent( dependenciesContainer );
            } else {
                popupCloseDialog(dialogContainer, target, "Previous Mappings Not Loaded", "No previous mappings were found.");
            }
        } catch ( FindException fe ) {
            logger.log( Level.WARNING, "Unexpected error when loading previous mappings.", fe );
            popupCloseDialog(dialogContainer, target, "Error Loading Previous Mappings", ExceptionUtils.getMessage( fe ));
        } catch ( GatewayException ge ) {
            popupCloseDialog(dialogContainer, target, "Error Loading Previous Mappings", ExceptionUtils.getMessage(ge));
        } catch (FailoverException fo) {
            popupCloseDialog(dialogContainer, target, "Error Loading Previous Mappings", ExceptionUtils.getMessage(fo));
        } catch ( WebServiceException e ) {
            String failureMessage;
            if ( GatewayContext.isNetworkException( e ) ) {
                failureMessage = "Could not connect to cluster.";
            } else if ( GatewayContext.isConfigurationException( e ) ) {
                failureMessage = "Could not connect to cluster.";
            } else {
                failureMessage = "Unexpected error from cluster.";
                logger.log( Level.WARNING, "Error loading previous mappings '"+ExceptionUtils.getMessage(e)+"'.", ExceptionUtils.getDebugException(e));
            }
            popupCloseDialog(dialogContainer, target, "Error Loading Previous Mappings", failureMessage);
        }
    }

    private void onMigrate( final AjaxRequestTarget target ) {
        final boolean folders = migrateFoldersModel.getObject();
        final boolean enableServices = enableNewServiceModel.getObject();
        final boolean overwrite = overwriteDependenciesModel.getObject();
        final boolean offlineDestination = offlineDestinationModel.getObject();
        final String destClusterId = destClusterModel.getObject();
        final String targetFolderId = offlineDestination || destContentSelector.getSelectedContent().size()!=1 ? null : destContentSelector.getSelectedContent().iterator().next().getExternalId();
        try {
            final DependencyItemsRequest dir = new DependencyItemsRequest();
            dir.fromSelection( srcContentSelector );

            try {
                final String dependencyValidationMessage = offlineDestination ? null : validateDependencies( dir.clusterId, destClusterId, dir, mappingModel );
                if ( dependencyValidationMessage != null && !dependencyValidationMessage.isEmpty() ) {
                    TextPanel textPanel = new TextPanel( YuiDialog.getContentId(), new Model<String>(dependencyValidationMessage));
                    YuiDialog dialog = new YuiDialog("dialog", "Dependency Mapping Required", YuiDialog.Style.CLOSE, textPanel, null, "600px");
                    dialogContainer.replace( dialog );
                    target.addComponent( dialogContainer );
                } else {
                    // load mappings for top-level items that have been previously migrated
                    if ( !offlineDestination ) {
                        final MigrationSource provider = getMigrationSource( srcContentSelector.getClusterId() );
                        loadMappings( mappingModel, dir.clusterId, destClusterId, provider.getDependencies( dir ), true);
                    }
                    final PolicyMigrationConfirmationPanel confirmationPanel = new PolicyMigrationConfirmationPanel(YuiDialog.getContentId(), new Model<String>(performMigration( dir.clusterId, destClusterId, targetFolderId, folders, enableServices, overwrite, offlineDestination, dir, "", true )));
                    YuiDialog dialog = new YuiDialog("dialog", "Confirm Migration", YuiDialog.Style.OK_CANCEL, confirmationPanel, new YuiDialog.OkCancelCallback(){
                        @Override
                        public void onAction( final YuiDialog dialog, final AjaxRequestTarget target, final YuiDialog.Button button) {
                            if ( button == YuiDialog.Button.OK ) {
                                logger.fine("Migration confirmed.");
                                try {
                                    String message = performMigration( dir.clusterId, destClusterId, targetFolderId, folders, enableServices, overwrite,offlineDestination, dir, confirmationPanel.getLabel(), false );
                                    YuiDialog resultDialog = new YuiDialog("dialog", "Migration Result", YuiDialog.Style.CLOSE, new TextPanel(YuiDialog.getContentId(), new Model<String>(message)), null, "600px");
                                    dialogContainer.replace( resultDialog );
                                } catch ( MigrationFailedException mfe ) {
                                    String failureMessage = mfe.getMessage();
                                    YuiDialog resultDialog;
                                    if ( failureMessage != null && failureMessage.indexOf('\n') < 0 ) {
                                        resultDialog = new YuiDialog("dialog", "Migration Error", YuiDialog.Style.CLOSE, new Label(YuiDialog.getContentId(), failureMessage), null);
                                    } else {
                                        resultDialog = new YuiDialog("dialog", "Migration Error", YuiDialog.Style.CLOSE, new TextPanel(YuiDialog.getContentId(), new Model<String>(failureMessage)), null, "600px");
                                    }
                                    dialogContainer.replace( resultDialog );
                                    target.addComponent( dialogContainer );
                                } catch (FailoverException fo) {
                                    popupCloseDialog(dialogContainer, target, "Could not connect to cluster", ExceptionUtils.getMessage( fo ));
                                } catch ( WebServiceException e ) {
                                    String failureMessage;
                                    if ( GatewayContext.isNetworkException( e ) ) {
                                        failureMessage = "Could not connect to cluster.";
                                    } else if ( GatewayContext.isConfigurationException( e ) ) {
                                        failureMessage = "Could not connect to cluster.";
                                    } else {
                                        failureMessage = "Unexpected error from cluster.";
                                        logger.log( Level.WARNING, "Error processing selection '"+ExceptionUtils.getMessage(e)+"'.", ExceptionUtils.getDebugException(e));
                                    }
                                    popupCloseDialog(dialogContainer, target, "Migration Error", failureMessage);
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
                popupCloseDialog(dialogContainer, target, "Error Loading Previous Mappings", ExceptionUtils.getMessage(fe));
            } catch ( GatewayException ge ) {
                popupCloseDialog(dialogContainer, target, "Error Loading Previous Mappings", ExceptionUtils.getMessage(ge));
            } catch (MigrationApi.MigrationException mae) {
                popupCloseDialog(dialogContainer, target, "Error Retrieving Dependencies", ExceptionUtils.getMessage(mae));
            } catch ( MigrationFailedException mfe ) {
                String failureMessage = mfe.getMessage();
                YuiDialog resultDialog;
                if ( failureMessage != null && failureMessage.indexOf('\n') < 0 ) {
                    resultDialog = new YuiDialog("dialog", "Migration Error", YuiDialog.Style.CLOSE, new Label(YuiDialog.getContentId(), failureMessage), null);
                } else {
                    resultDialog = new YuiDialog("dialog", "Migration Error", YuiDialog.Style.CLOSE, new TextPanel(YuiDialog.getContentId(), new Model<String>(failureMessage)), null, "600px");
                }
                dialogContainer.replace( resultDialog );
                target.addComponent( dialogContainer );
            }
        } catch (FailoverException fo) {
            popupCloseDialog(dialogContainer, target, "Could not connect to cluster.", ExceptionUtils.getMessage(fo));
        } catch ( WebServiceException e ) {
            String failureMessage;
            if ( GatewayContext.isNetworkException( e ) ) {
                failureMessage = "Could not connect to cluster.";
            } else if ( GatewayContext.isConfigurationException( e ) ) {
                failureMessage = "Could not connect to cluster.";
            } else if ( ByteLimitInputStream.SIZE_LIMIT_EXCEEDED.equals( ExceptionUtils.getMessage( e ) ) ) {
                failureMessage = "Request size exceeded Gateway limit.";
            } else {
                failureMessage = "Unexpected error from cluster.";
                logger.log( Level.WARNING, "Error during migration '"+ExceptionUtils.getMessage(e)+"'.", ExceptionUtils.getDebugException(e));
            }
            popupCloseDialog(dialogContainer, target, "Migration Error", failureMessage);
        }
    }

    private void onIdentifyDependencies( final AjaxRequestTarget target ) {
        lastDestinationClusterId = destClusterModel.getObject();
        lastSourceClusterId = srcClusterModel.getObject();
        try {
            final DependencyItemsRequest dir = new DependencyItemsRequest();
            dir.fromSelection( srcContentSelector );
            final String destClusterId = lastDestinationClusterId;
            final MigrationSource provider = getMigrationSource( srcContentSelector.getClusterId() );
            final Collection<DependencyItem> dependencyItems = provider.getDependencies( dir );

            for ( final DependencyItem item : dependencyItems ) {
                if ( Boolean.FALSE == item.resolved ) {
                    item.resolved = isResolved( mappingModel, item.asEntityHeader(), dir.clusterId, destClusterId );
                }
            }

            lastDependencyItems = dependencyItems;
            lastSourceKey = null;
            candidateModel.reset();
            dependencyLoadButton.setEnabled(true);

            updateDependencies();
            addDependencyOptions( true );
            target.addComponent( dependenciesContainer );

            // build info dialog
            Label label = new Label( YuiDialog.getContentId(), summarize(dir, destClusterId, visible(dependencyItems,true), mappingModel));
            label.setEscapeModelStrings(false);
            YuiDialog dialog = new YuiDialog("dialog", "Identified Dependencies", YuiDialog.Style.CLOSE, label, null);
            dialogContainer.replace( dialog );
            target.addComponent( dialogContainer );
        } catch ( FindException fe ) {
            logger.log( Level.WARNING, "Unexpected error when processing selection.", fe );
            popupCloseDialog(dialogContainer, target, "Error Identifying Dependencies", ExceptionUtils.getMessage( fe ));
        } catch ( GatewayException ge ) {
            popupCloseDialog(dialogContainer, target, "Error Identifying Dependencies", ExceptionUtils.getMessage(ge));
        } catch ( MigrationApi.MigrationException mfe ) {
            String failureMessage = mfe.getMessage();
            YuiDialog resultDialog;
            if ( failureMessage != null && failureMessage.indexOf('\n') < 0 ) {
                resultDialog = new YuiDialog("dialog", "Error Identifying Dependencies", YuiDialog.Style.CLOSE, new Label(YuiDialog.getContentId(), failureMessage), null);
            } else {
                resultDialog = new YuiDialog("dialog", " Error Identifying Dependencies", YuiDialog.Style.CLOSE, new TextPanel(YuiDialog.getContentId(), new Model<String>(failureMessage)), null, "600px");
            }
            dialogContainer.replace( resultDialog );
            target.addComponent( dialogContainer );
        } catch (FailoverException fo) {
            popupCloseDialog(dialogContainer, target, "Error Identifying Dependencies", ExceptionUtils.getMessage(fo));
        } catch ( WebServiceException e ) {
            String failureMessage;
            if ( GatewayContext.isNetworkException( e ) ) {
                failureMessage = "Could not connect to cluster.";
            } else if ( GatewayContext.isConfigurationException( e ) ) {
                failureMessage = "Could not connect to cluster.";
            } else {
                failureMessage = "Unexpected error from cluster.";
                logger.log( Level.WARNING, "Error identifying dependencies '"+ExceptionUtils.getMessage(e)+"'.", ExceptionUtils.getDebugException(e));
            }
            popupCloseDialog(dialogContainer, target, "Error Identifying Dependencies", failureMessage);
        }
    }

    private void enableDisableMainButtons( final AjaxRequestTarget target ) {
        final boolean offlineDestination = offlineDestinationModel.getObject();
        final boolean validSource =
                srcContentSelector.getClusterId() != null &&
                !srcContentSelector.getSelectedContentIds().isEmpty();

        final boolean enableIdentifyDependencies = validSource &&
                !offlineDestination &&
                destContentSelector.getClusterId() != null &&
                !destContentSelector.getClusterId().equals( srcContentSelector.getClusterId() );

        final boolean enableMigration =
                ( offlineDestination && validSource ) ||
                ( enableIdentifyDependencies && !destContentSelector.getSelectedContentIds().isEmpty() );

        identifyDependenciesButton.setEnabled( enableIdentifyDependencies );
        migrateButton.setEnabled( enableMigration );

        if ( target != null ) {
            target.addComponent( identifyDependenciesButton );
            target.addComponent( migrateButton );
        }
    }

    private void updateOfflineDestination( final AjaxRequestTarget target ) {
        if ( offlineDestinationModel.getObject() ) {
            destClusterSelector.setMessage("Offline destination.");
            destContentSelector.setMessage("Offline destination.");
        } else {
            destClusterSelector.clearMessage();
            destContentSelector.clearMessage();
        }
        target.addComponent( destClusterSelector );
        target.addComponent( destContentSelector );
        enableDisableMainButtons( target );
    }

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

    private String buildClusterConfigDialogJavascript( final String clusterId,
                                                       final String dialogName,
                                                       final String urlSuffix ) throws GatewayException, FindException {
        final SsgCluster cluster = ssgClusterManager.findByGuid(clusterId);
        final StringBuilder accessBuilder = new StringBuilder();

        accessBuilder.append( "launchClusterConfigDialog(" );
        accessBuilder.append( dialogName );
        accessBuilder.append( "Dialog, '" );
        accessBuilder.append( dialogName );
        accessBuilder.append( "', '" );
        accessBuilder.append( cluster.getGuid() );
        accessBuilder.append( "', '" );
        accessBuilder.append( cluster.getName() );
        accessBuilder.append( "', '" );
        accessBuilder.append( gatewayTrustTokenFactory.getTrustToken() );
        accessBuilder.append( "', 'https://" );
        accessBuilder.append( cluster.getSslHostName() );
        accessBuilder.append( ":" );
        accessBuilder.append( cluster.getAdminPort() );
        accessBuilder.append( "/ssg/esmtrust");
        if ( urlSuffix != null ) accessBuilder.append( urlSuffix );
        accessBuilder.append( "' );" );

        return  accessBuilder.toString();
    }

    private void popupCloseDialog( final WebMarkupContainer container,
                                   final AjaxRequestTarget target,
                                   final String title,
                                   final String label ) {
        YuiDialog resultDialog = new YuiDialog("dialog", title, YuiDialog.Style.CLOSE, new Label(YuiDialog.getContentId(), label), null);
        container.replace( resultDialog );
        target.addComponent( container );
    }

    private static final class PreviousMigrationModel implements Comparable, Serializable {
        private final String label;
        private final long id;

        private PreviousMigrationModel( final long id,
                                       final String label )  {
            this.id = id;
            this.label = label;
        }

        @Override
        public int compareTo(Object o) {
            PreviousMigrationModel other = (PreviousMigrationModel) o;
            return this.label.toLowerCase().compareTo(other.label.toLowerCase());
        }

        @SuppressWarnings({ "RedundantIfStatement" })
        @Override
        public boolean equals( final Object o ) {
            if ( this == o ) return true;
            if ( o == null || getClass() != o.getClass() ) return false;

            final PreviousMigrationModel that = (PreviousMigrationModel) o;

            if ( id != that.id ) return false;
            if ( label != null ? !label.equals( that.label ) : that.label != null ) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = label != null ? label.hashCode() : 0;
            result = 31 * result + (int) (id ^ (id >>> 32));
            return result;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private List<PreviousMigrationModel> loadPreviousMigrations() {
        List<PreviousMigrationModel> previousMigrations = new ArrayList<PreviousMigrationModel>();

        try {
            final User user;
            if ( !securityManager.hasPermission( new AttemptedReadAll( EntityType.ESM_MIGRATION_RECORD ) ) ) {
                user = getUser();
            } else {
                user = null;
            }

            Collection<MigrationRecord> records = migrationRecordManager.findNamedMigrations( user, 100, null, null );
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

    private void refreshDependencyDisplayModel( final WebMarkupContainer itemDetailsContainer,
                                                final WebMarkupContainer itemDependenciesContainer,
                                                final String clusterId,
                                                final String entityType,
                                                final String entityId ) {
        logger.fine( "Processing request for cluster " + clusterId + " type " + entityType + " id " + entityId );
        DependencyItem detailItem = null;
        List<DependencyItem> options = Collections.emptyList();

        if ( clusterId != null && !clusterId.isEmpty() ) {
            final MigrationSource provider = getMigrationSource( clusterId );

            try {
                options = new ArrayList<DependencyItem>( provider.getDependencies( entityType, entityId ) );
                for ( Iterator<DependencyItem> itemIter = options.iterator(); itemIter.hasNext();  ) {
                    DependencyItem item = itemIter.next();
                    if ( item.hidden || com.l7tech.objectmodel.EntityType.FOLDER.toString().equals(item.type) ) {
                        itemIter.remove();
                    }
                }

                detailItem = provider.getItem( entityType, entityId );
            } catch ( MigrationApi.MigrationException me ) {
                logger.log( Level.INFO, "Error processing selection '"+ExceptionUtils.getMessage(me)+"'." );
            } catch ( FailoverException fo ) {
                logger.log( Level.INFO, "Error processing selection '"+ExceptionUtils.getMessage(fo)+"'.", ExceptionUtils.getDebugException(fo) );
            } catch ( WebServiceException sfe ) {
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
    }

    private MigrationSource getMigrationSource( final String clusterId ) {
        MigrationSource migrationSource = detailProvidersByCluster.get( clusterId );
        if ( migrationSource == null ) {
            migrationSource = new DefaultMigrationSource( clusterId );
        }
        return migrationSource;
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
        ListView<DependencyItem> listView = new ListView<DependencyItem>("optionRepeater", visible(options, false)) {
            @Override
            protected void populateItem( final ListItem<DependencyItem> item ) {
                final DependencyItem dependencyItem = item.getModelObject();
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
        ListView<Pair<String,String>> detailsListView = new ListView<Pair<String,String>>("itemDetailsRepeater", nvp(detailsItem)) {
            @SuppressWarnings({"unchecked"})
            @Override
            protected void populateItem( final ListItem<Pair<String,String>> item ) {
                final Pair<String,String> nvp = item.getModelObject();
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

    private void updateDependencies() {
        final String srcClusterId = this.lastSourceClusterId;
        final String destClusterId = this.lastDestinationClusterId;
        final Collection<DependencyItem> items = visible(this.lastDependencyItems, true);

        // update dependency counts and items destination names
        dependencySummaryModel.reset();
        for ( DependencyItem item : items ) {
            DependencyKey sourceKey = new DependencyKey( srcClusterId, item.asEntityHeader() );
            Pair<DependencyKey,String> mappingKey = new Pair<DependencyKey,String>(sourceKey, destClusterId);
            Pair<ValueKey,String> valueKey = item.asEntityHeader().isValueMappable() ?
                    new Pair<ValueKey,String>(new ValueKey( srcClusterId, item.asEntityHeader() ), destClusterId) :
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
        final List<PropertyColumn<?>> dependencyColumns =  Arrays.<PropertyColumn<?>>asList(
            new PropertyColumn<String>(new Model<String>(""), "uid"),
            new TypedPropertyColumn<String>(new Model<String>(""), "optional", "optional", String.class, false),
            new TypedPropertyColumn<String>(new Model<String>("Name"), "displayNameWithScope", "displayNameWithScope", String.class, true, true),
            new PropertyColumn<String>(new Model<String>("Type"), "type", "type"),
            new PropertyColumn<String>(new Model<String>("Dest. Name / Value"), "destName", "destName")
        );

        final YuiDataTable ydt = new YuiDataTable( "dependenciesTable", dependencyColumns, "displayNameWithScope", true, items, null, false, "uid", true, null ){
            @Override
            @SuppressWarnings({"UnusedDeclaration"})
            protected void onSelect( final AjaxRequestTarget target, final String value ) {
                if ( destClusterId != null && !destClusterId.isEmpty() && value != null && !value.isEmpty() ) {
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
                        lastSourceKey = new DependencyKey( srcClusterId, selectedItem.asEntityHeader() );
                        if ( isSearchable( selectedItem.asEntityHeader() ) ) {
                            candidateModel.setName( selectedItem.getDisplayNameWithScope() );
                            candidateModel.setType( selectedItem.getType() );
                        } else {
                            candidateModel.reset();
                        }
                        Pair<DependencyKey,String> key = new Pair<DependencyKey,String>(lastSourceKey, lastDestinationClusterId );
                        final Pair<ValueKey,String> valueKey = lastSourceKey.asEntityHeader().isValueMappable() ?
                            new Pair<ValueKey,String>(new ValueKey(lastSourceKey), lastDestinationClusterId ):
                            null;

                        selectionComponent.setEnabled( mappingModel.dependencyMap.containsKey(key) || mappingModel.valueMap.containsKey(valueKey) );
                        selectionComponent2.setEnabled( selectedItem.asEntityHeader().isValueMappable() );
                    } else {
                        lastSourceKey = null;
                        candidateModel.reset();
                        selectionComponent.setEnabled(false);
                        selectionComponent2.setEnabled(false);
                    }

                    addDependencyOptions( true );
                    target.addComponent( selectionComponent );
                    target.addComponent( selectionComponent2 );
                    for ( Component component : dependencyRefreshContainers ) target.addComponent( component );
                }
            }
        };

        if ( dependenciesContainer.get( ydt.getId() ) == null ) {
            dependenciesContainer.add( ydt );
        } else {
            dependenciesContainer.replace( ydt );
        }
    }

    private void addDependencyOptions( final boolean skipSearch ) {
        String destClusterId = lastDestinationClusterId;
        DependencyKey sourceKey = lastSourceKey;
        final Pair<DependencyKey,String> mappingKey = new Pair<DependencyKey,String>(sourceKey, destClusterId);

        WebMarkupContainer markupContainer = new WebMarkupContainer("dependencyOptions");
        if ( dependenciesOptionsContainer.get( markupContainer.getId() ) == null ) {
            dependenciesOptionsContainer.add( markupContainer.setOutputMarkupId(true) );
        } else {
            dependenciesOptionsContainer.replace( markupContainer.setOutputMarkupId(true) );
        }

        List<DependencyItem> options  = Collections.emptyList();
        @SuppressWarnings({"unchecked"})
        DropDownChoice<SearchTarget> targetChoice = (DropDownChoice<SearchTarget>) dependenciesOptionsContainer.get("dependencySearchTarget");
        if ( skipSearch ) {
            searchModel.setSearchManner("contains");
            searchModel.setSearchValue("");

            if ( sourceKey != null && isSearchable( sourceKey.asEntityHeader() ) ) {
                for ( String id : SEARCH_REFRESH_COMPONENTS ) {
                    Component component = dependenciesOptionsContainer.get(id);
                    component.setEnabled( true );
                }

                List<DependencyItem> scopeOptions = null;
                if ( sourceKey.asEntityHeader().getProperty("Scope Type") != null ) {
                    ExternalEntityHeader externalEntityHeader = new ExternalEntityHeader( null, EntityType.valueOf(sourceKey.asEntityHeader().getProperty("Scope Type")), null, null, null, -1);
                    scopeOptions = retrieveDependencyOptions( lastDestinationClusterId, new DependencyKey(sourceKey.clusterId, externalEntityHeader), null, null );
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
                    Component component = dependenciesOptionsContainer.get(id);
                    component.setEnabled( false );
                }
            }
        } else {
            final SearchTarget target = searchModel.getSearchTarget();
            if ( sourceKey != null && target != null && isSearchable( sourceKey.asEntityHeader() ) ) {
                options = retrieveDependencyOptions( lastDestinationClusterId, lastSourceKey, target.item, searchModel.getSearchFilter() );
            }
        }

        markupContainer.add(new ListView<DependencyItem>("optionRepeater", options) {
            @Override
            protected void populateItem( final ListItem<DependencyItem> item ) {
                final DependencyItem dependencyItem = item.getModelObject();

                Component radioComponent = new WebComponent("uid").add( new AjaxEventBehavior("onclick"){
                    @Override
                    protected void onEvent( final AjaxRequestTarget target ) {
                        logger.fine("Selection callback for : " + dependencyItem);
                        mappingModel.dependencyMap.put( mappingKey, dependencyItem );
                        updateDependencies();

                        for ( String id : DEPENDENCY_REFRESH_COMPONENTS ) target.addComponent( dependenciesContainer.get(id) );
                        for ( Component component : dependencyRefreshContainers ) target.addComponent( component );
                    }
                } );

                DependencyItem currentItem = mappingModel.dependencyMap.get( mappingKey );
                if ( currentItem!=null && currentItem.equals( dependencyItem ) ) {
                    radioComponent.add( new AttributeModifier("checked", true, new Model<String>("checked")) );
                }

                item.add(radioComponent);
                item.add(new Label("name", dependencyItem.getDisplayName()));
            }
        });
    }

    private Collection<DependencyItem> retrieveDependencies( final DependencyItemsRequest request ) throws FindException, GatewayException, MigrationApi.MigrationException {
        Collection<DependencyItem> deps = new LinkedHashSet<DependencyItem>();

        SsgCluster cluster = ssgClusterManager.findByGuid(request.clusterId);
        if ( cluster != null ) {
            if ( cluster.getTrustStatus() ) {
                GatewayClusterClient context = gatewayClusterClientManager.getGatewayClusterClient(cluster, getUser());
                MigrationApi api = context.getUncachedMigrationApi();
                MigrationMetadata metadata = api.findDependencies( request.asEntityHeaders() );
                for (MigrationDependency dep : metadata.getMappableDependencies()) {
                    deps.add( new DependencyItem( dep.getDependency(), metadata.isMappingRequired(dep.getDependency()) ? Boolean.FALSE : null ) );
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

                deps = processDependencyItems( deps );
            }
        }

        return deps;
    }

    private static Collection<DependencyItem> processDependencyItems( Collection<DependencyItem> deps ) {
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

        return deps;
    }

    /**
     * If the given item represents a value mapping with an array type it is expanded to
     * a collection of non-array value mappings.
     *
     * <p>If the mappable values are null, they are filtered out, so the returned collection can
     * be empty.</p>
     *
     * @return  Collection containing either the exploded items, or the item parameter
     *          if it is not value-mappable, or it does not hold an array-type value, or the source mappable value is null.
     */
    private static Collection<DependencyItem> explode( final DependencyItem item ) {
        Collection<DependencyItem> exploded = new ArrayList<DependencyItem>();

        ExternalEntityHeader eeh = item == null ? null : item.asEntityHeader();
        if (eeh != null && eeh.isValueMappable()) {
            for(ExternalEntityHeader vmHeader : eeh.getValueMappableHeaders()) {
                if ( vmHeader.getDisplayValue() != null ) {
                    exploded.add(new DependencyItem(vmHeader));
                }
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
    private List<DependencyItem> retrieveDependencyOptions( final String destClusterId,
                                                            final DependencyKey sourceKey,
                                                            final DependencyItem scope,
                                                            final String filter ) {
        List<DependencyItem> deps = new ArrayList<DependencyItem>();

        try {
            SsgCluster cluster = ssgClusterManager.findByGuid(destClusterId);
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
        } catch ( FailoverException fo ) {
            logger.log( Level.INFO, "Error while gettings dependency options '"+ExceptionUtils.getMessage(fo)+"'.", ExceptionUtils.getDebugException(fo) );
        } catch ( WebServiceException e ) {
            if ( !GatewayContext.isNetworkException( e ) && !GatewayContext.isConfigurationException( e ) ) {
                logger.log( Level.WARNING, "Error while gettings dependency options'"+ExceptionUtils.getMessage(e)+"'.", ExceptionUtils.getDebugException(e));
            }
        }

        return deps;
    }

    /**
     * Populates the mapping model with the mappings persisted in the database.
     *
     * @param onlyIsSame If true only the mappings having the flag isSame == true are loaded
     *                   (for entities that were created on the destination cluster as a result of a previous migration);
     *                   If false all mappings are loaded, including the ones manually configured by the EM users.
     */
    private void loadMappings( final EntityMappingModel mappingModel,
                               final String srcClusterId,
                               final String destClusterId,
                               final Collection<DependencyItem> dependencyItems,
                               final boolean onlyIsSame) throws FindException, GatewayException {
        if ( !dependencyItems.isEmpty() ) {
            // load mappings saved in EM db
            for ( DependencyItem item : dependencyItems ) {
                if ( item.asEntityHeader().isValueMappable() ) {
                    if ( !onlyIsSame ) {
                        Pair<ValueKey,String> valueKey = new Pair<ValueKey,String>(new ValueKey( srcClusterId, item.asEntityHeader() ), destClusterId);
                        if ( !mappingModel.valueMap.containsKey(valueKey) ) {
                            MigrationMappingRecord mapping = migrationMappingRecordManager.findByMapping( srcClusterId, item.asEntityHeader(), destClusterId, true );
                            if ( mapping != null && mapping.getTarget() != null ) {
                                mappingModel.valueMap.put( valueKey, mapping.getTarget().getEntityValue() );
                            }
                        }
                    }
                }

                DependencyKey sourceKey = new DependencyKey(srcClusterId, item.asEntityHeader());
                Pair<DependencyKey, String> mapKey = new Pair<DependencyKey, String>(sourceKey, destClusterId);
                if (!mappingModel.dependencyMap.containsKey(mapKey)) {
                    MigrationMappingRecord mapping = migrationMappingRecordManager.findByMapping(srcClusterId, item.asEntityHeader(), destClusterId, false);
                    if (mapping != null && mapping.getTarget() != null && (mapping.isSameEntity() || !onlyIsSame) ) {
                        mappingModel.dependencyMap.put(mapKey, new DependencyItem(MigrationMappedEntity.asEntityHeader(mapping.getTarget()), null, false, mapping.isSameEntity()));
                    }
                }
            }

            // discard the dependencies that no longer exist on the destination cluster
            SsgCluster destCluster = ssgClusterManager.findByGuid(destClusterId);
            GatewayClusterClient destContext = gatewayClusterClientManager.getGatewayClusterClient(destCluster, getUser());
            MigrationApi destMigrationApi = destContext.getUncachedMigrationApi();

            Collection<ExternalEntityHeader> headersToCheck = new HashSet<ExternalEntityHeader>();
            for (Map.Entry<Pair<DependencyKey,String>,DependencyItem> entry : mappingModel.dependencyMap.entrySet()) {
                if ( entry.getValue() == null || !entry.getKey().right.equals(destClusterId)) continue;
                headersToCheck.add( entry.getValue().asEntityHeader() );
            }

            Collection<ExternalEntityHeader> validatedHeaders = destMigrationApi.checkHeaders(headersToCheck);
            if ( validatedHeaders == null )
                validatedHeaders = Collections.emptyList();

            Map<Pair<DependencyKey,String>,DependencyItem> keysToUpdate = new HashMap<Pair<DependencyKey, String>,DependencyItem>();
            Set<Pair<DependencyKey,String>> keysToNull = new HashSet<Pair<DependencyKey, String>>();
            for( Map.Entry<Pair<DependencyKey,String>,DependencyItem> entry : mappingModel.dependencyMap.entrySet() ) {
                if ( !entry.getKey().right.equals(destClusterId) || entry.getValue() == null || entry.getValue() == null )
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
                                             final String srcClusterGuid,
                                             final String destClusterGuid,
                                             final List<Pair<ExternalEntityHeader, ExternalEntityHeader>> mappings ) {
        try {
            // discard the mappings that no longer exist on the source or destination cluster
            final SsgCluster sourceCluster = ssgClusterManager.findByGuid(srcClusterGuid);
            final MigrationApi sourceMigrationApi;
            if ( !sourceCluster.isOffline() ) {
                final GatewayClusterClient sourceContext = gatewayClusterClientManager.getGatewayClusterClient(sourceCluster, getUser());
                sourceMigrationApi = sourceContext.getUncachedMigrationApi();
            } else {
                sourceMigrationApi = null;
            }

            final SsgCluster destCluster = ssgClusterManager.findByGuid(destClusterGuid);
            final GatewayClusterClient destContext = gatewayClusterClientManager.getGatewayClusterClient(destCluster, getUser());
            final MigrationApi destMigrationApi = destContext.getUncachedMigrationApi();

            final List<ExternalEntityHeader> srcHeaders = new ArrayList<ExternalEntityHeader>();
            final List<ExternalEntityHeader> destHeaders = new ArrayList<ExternalEntityHeader>();
            for ( final Pair<ExternalEntityHeader, ExternalEntityHeader> mapping : mappings ) {
                srcHeaders.add( mapping.left );
                destHeaders.add( mapping.right );
            }

            final Collection<ExternalEntityHeader> validatedSourceHeaders = sourceCluster.isOffline() ?
                    Collections.<ExternalEntityHeader>emptyList() :
                    sourceMigrationApi.checkHeaders( srcHeaders );
            final Collection<ExternalEntityHeader> validatedDestinationHeaders = destMigrationApi.checkHeaders( destHeaders );

            for ( Pair<ExternalEntityHeader, ExternalEntityHeader> mapping : mappings ) {
                if ( ( sourceCluster.isOffline() || containsHeader( mapping.left, validatedSourceHeaders ) ) &&
                     containsHeader( mapping.right, validatedDestinationHeaders ) ) {
                    final DependencyKey sourceKey = new DependencyKey( srcClusterGuid, mapping.left );
                    final Pair<DependencyKey,String> mapKey = new Pair<DependencyKey,String>( sourceKey, destClusterGuid );
                    mappingModel.dependencyMap.put( mapKey, new DependencyItem(getHeader(validatedDestinationHeaders, mapping.right.getType(), mapping.right.getExternalId())) );
                }
            }

        } catch ( GatewayException ge ) {
            logger.log( Level.INFO, "Error while reloading previous migration, mappings not restored due to '"+ExceptionUtils.getMessage(ge)+"'.", ExceptionUtils.getDebugException(ge) );
        } catch ( FindException fe ) {
            logger.log( Level.WARNING, "Error while reloading previous migration, mappings not restored.", fe );
        } catch ( FailoverException fo ) {
            logger.log( Level.INFO, "Error while reloading previous migration, mappings not restored due to '"+ExceptionUtils.getMessage(fo)+"'.", ExceptionUtils.getDebugException(fo) );
        } catch ( WebServiceException e ) {
            if ( !GatewayContext.isNetworkException( e ) && !GatewayContext.isConfigurationException( e ) ) {
                logger.log( Level.WARNING, "Error while reloading previous migration, mappings not restored due to '"+ExceptionUtils.getMessage(e)+"'.", ExceptionUtils.getDebugException(e));
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
     * Count the number of unmapped dependencies in the mapping model for the given source/destination cluster
     */
    private int countUnMappedDependencies( final EntityMappingModel mappingModel,
                                           final String srcClusterId,
                                           final String destClusterId,
                                           final Collection<DependencyItem> items ) {
        int count = 0;

        if ( srcClusterId != null && destClusterId != null ) {
            for ( DependencyItem item : items ) {
                if ( item.hidden ) continue;

                final DependencyKey sourceKey = new DependencyKey( srcClusterId, item.asEntityHeader() );
                final Pair<DependencyKey,String> mapKey = new Pair<DependencyKey,String>( sourceKey, destClusterId );
                final Pair<ValueKey,String> valueKey = item.asEntityHeader().isValueMappable() ?
                    new Pair<ValueKey,String>(new ValueKey(srcClusterId, item.asEntityHeader()), destClusterId):
                    null;

                if ( !mappingModel.dependencyMap.containsKey(mapKey) && (valueKey==null || !mappingModel.valueMap.containsKey(valueKey)) ) {
                    count++;
                }
            }
        }

        return count;
    }

    private String validateDependencies( final String srcClusterId,
                                         final String destClusterId,
                                         final DependencyItemsRequest requestedItems,
                                         final EntityMappingModel mappingModel ) throws MigrationFailedException {
        String summary = "";
        try {
            SsgCluster srcCluster = ssgClusterManager.findByGuid(srcClusterId);
            SsgCluster destCluster = ssgClusterManager.findByGuid(destClusterId);
            if ( srcCluster != null && destCluster != null) {
                if ( (srcCluster.isOffline() || srcCluster.getTrustStatus()) && destCluster.getTrustStatus() ) {
                    final MigrationSource provider = getMigrationSource( srcContentSelector.getClusterId() );
                    final Collection<DependencyItem> items = provider.getDependencies( requestedItems );
                    StringBuilder builder = new StringBuilder();
                    for ( DependencyItem item : items ) {
                        final DependencyKey sourceKey = new DependencyKey( srcClusterId, item.asEntityHeader() );
                        final Pair<DependencyKey,String> mapKey = new Pair<DependencyKey,String>( sourceKey, destClusterId );
                        final Pair<ValueKey,String> valueKey = item.asEntityHeader().isValueMappable() ?
                            new Pair<ValueKey,String>(new ValueKey(srcClusterId, item.asEntityHeader()), destClusterId):
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
        private MigrationFailedException(String message) {
            super(message);
        }
    }

    private String performMigration( final String srcClusterId,
                                     final String destClusterId,
                                     final String targetFolderId,
                                     final boolean migrateFolders,
                                     final boolean enableNewServices,
                                     final boolean overwriteEntities,
                                     final boolean offlineDestination,
                                     final DependencyItemsRequest requestedItems,
                                     final String label,
                                     final boolean dryRun ) throws MigrationFailedException {
        String summaryString = "";
        try {
            final MigrationSource migrationSource = getMigrationSource( srcClusterId );
            final SsgCluster srcCluster = ssgClusterManager.findByGuid(srcClusterId);
            final SsgCluster destCluster = offlineDestination ? null : ssgClusterManager.findByGuid(destClusterId);
            if ( srcCluster != null && (offlineDestination || (destCluster != null  && destCluster.getTrustStatus())) ) {
                MigrationApi destMigrationApi = null;
                GatewayApi destGatewayApi = null;
                if ( !offlineDestination ) {
                    GatewayClusterClient destContext = gatewayClusterClientManager.getGatewayClusterClient(destCluster, getUser());
                    destMigrationApi = destContext.getUncachedMigrationApi();
                    destGatewayApi = destContext.getUncachedGatewayApi();
                }

                String targetFolderDescription = null;
                final Collection<MigratedItem> migratedItems;
                final MigrationBundle bundle = migrationSource.exportBundle( requestedItems );
                final MigrationMetadata metadata = bundle.getMetadata();
                metadata.setMigrateFolders(migrateFolders);
                metadata.setOverwrite(overwriteEntities);
                metadata.setEnableNewServices(enableNewServices);
                if ( !offlineDestination ) {
                    for ( Map.Entry<Pair<DependencyKey,String>,DependencyItem> mapping : mappingModel.dependencyMap.entrySet() ) {
                        if ( mapping.getValue() != null && mapping.getKey().left.clusterId.equals(srcClusterId) && mapping.getKey().right.equals(destClusterId) ) {
                            metadata.addMappingOrCopy(mapping.getKey().left.asEntityHeader(), mapping.getValue().asEntityHeader(), mapping.getValue().same);
                            migrationMappingRecordManager.persistMapping(
                                    srcClusterId,
                                    mapping.getKey().left.asEntityHeader(),
                                    destCluster.getGuid(),
                                    mapping.getValue().asEntityHeader(),
                                    mapping.getValue().same);
                        }
                    }
                    for ( MigrationDependency dep : metadata.getMappableDependencies() ) {
                        Collection<ExternalEntityHeader> persistenceHeaders = mappingModel.updateMappedValues( srcClusterId, destClusterId, dep );
                        for ( ExternalEntityHeader pHeader : persistenceHeaders ) {
                            migrationMappingRecordManager.persistMapping( srcClusterId, pHeader, destCluster.getGuid(), pHeader.getMappedValue() );
                        }
                    }

                    Collection<GatewayApi.EntityInfo> folders = destGatewayApi.getEntityInfo( Collections.singleton(com.l7tech.objectmodel.EntityType.FOLDER) );
                    if (folders == null) throw new FindException("Empty list of folders retrieved from the destination cluster (check gateway account permissions).");
                    ExternalEntityHeader targetFolderHeader = null;
                    for ( GatewayApi.EntityInfo info : folders ) {
                        if ( targetFolderId.equals( info.getId() ) ) {
                            targetFolderHeader = new ExternalEntityHeader(info.getExternalId(), com.l7tech.objectmodel.EntityType.FOLDER, info.getId(), info.getName(), info.getDescription(), info.getVersion());
                        }
                    }
                    if (targetFolderHeader == null) throw new FindException("Could not find target folder.");
                    metadata.setTargetFolder(targetFolderHeader);
                    targetFolderDescription = targetFolderHeader.getDescription();

                    migratedItems = destMigrationApi.importBundle(bundle, dryRun);
                } else {
                    migratedItems = summarizeExport(bundle);
                }

                final MigrationSummary summary = new MigrationSummary(srcCluster, destCluster, migratedItems, dryRun,
                                                        targetFolderDescription , migrateFolders, overwriteEntities, enableNewServices);

                if ( !dryRun ) {
                    if ( migratedItems != null && !offlineDestination ) {
                        for ( MigratedItem item : migratedItems ) {
                            ExternalEntityHeader source = item.getSourceHeader();
                            ExternalEntityHeader destination = item.getTargetHeader();

                            if ( source != null && destination != null && item.getOperation().modifiesTarget() && source.getMappedValue() == null) {
                                migrationMappingRecordManager.persistMapping( srcClusterId, source, destCluster.getGuid(), destination, true );
                            }
                        }
                    }
                    migrationRecordManager.create( label, getUser(), srcCluster, destCluster, summary, bundle );
                }

                summaryString = summary.toString();
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

    private Collection<MigratedItem> summarizeExport( final MigrationBundle bundle ) {
        return Functions.map( bundle.getExportedItems().values(), new Functions.Unary<MigratedItem, ExportedItem>(){
            @Override
            public MigratedItem call( final ExportedItem exportedItem ) {
                MigratedItem item = new MigratedItem();

                item.setSourceHeader( exportedItem.getHeader() );
                item.setOperation( MigratedItem.ImportOperation.OFFLINE );

                return item;
            }
        });
    }

    private String summarizeMigrationException(MigrationApi.MigrationException me) {
        return "Migration failed: " + me.getMessage() + (me.hasErrors() ? " : \n\n" + me.getErrors() : "");
    }

    /**
     *
     */
    private String summarize( final DependencyItemsRequest request,
                              final String destClusterId,
                              final Collection<DependencyItem> dependencies,
                              final EntityMappingModel mappings ) {
        StringBuilder builder = new StringBuilder();

        builder.append("<p>Found ");
        builder.append(dependencies.size());
        builder.append(" dependencies.</p>");

        builder.append("<p>");
        int count = 0;
        for ( DependencyItem item : dependencies ) {
            ExternalEntityHeader header = item.asEntityHeader();
            DependencyKey sourceKey = new DependencyKey( request.clusterId, header);
            final Pair<DependencyKey,String> mapKey = new Pair<DependencyKey,String>( sourceKey, destClusterId );
            final Pair<ValueKey,String> valueKey = item.asEntityHeader().isValueMappable() ?
                new Pair<ValueKey,String>(new ValueKey(request.clusterId, item.asEntityHeader()), destClusterId):
                null;

            boolean isNameMapped = mappings.dependencyMap.containsKey(mapKey);
            boolean isValueMapped = valueKey!=null && mappings.valueMap.containsKey(valueKey);
            if ( ( ! item.isOptional() && ! isNameMapped && ! isValueMapped ) ||
                 ( header.getValueMapping() == REQUIRED && ! isValueMapped ) ) {
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
                icon = "<img src=\"/images/resolved.png\" />";
            } else {
                icon = "<img src=\"/images/unresolved.png\" />";
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
               REQUIRED != header.getValueMapping();

    }

    /**
     * Hidden dependency items are not shown in the item dependencies view, but may be mappable.
     */
    private static boolean isHiddenDependency( final DependencyItem item ) {
        return  item.asEntityHeader().getType() == com.l7tech.objectmodel.EntityType.VALUE_REFERENCE;
    }

    public Boolean isResolved( final EntityMappingModel mappingModel,
                               final ExternalEntityHeader externalEntityHeader,
                               final String srcClusterId,
                               final String destClusterId ) {
        boolean resolved = false;

        if ( mappingModel != null && destClusterId != null && srcClusterId != null) {
            DependencyKey sourceKey = new DependencyKey( srcClusterId, externalEntityHeader );
            Pair<DependencyKey,String> mappingKey = new Pair<DependencyKey,String>( sourceKey, destClusterId );
            DependencyItem mappingValue = mappingModel.dependencyMap.get( mappingKey );
            if ( mappingValue != null ) {
                resolved = true;
            }
        }

        return resolved;
    }

    private static class DependencyItemsRequest implements Serializable {
        private String clusterId;
        private DependencyItem[] entities;

        @Override
        public String toString() {
            return "DependencyItemsRequest[clusterId='"+clusterId+"'; entities="+Arrays.asList(entities)+"]";
        }

        public void fromSelection( final SSGClusterContentSelectorPanel data ) {
            clusterId = data.getClusterId();

            final Collection<SsgClusterContent> selectedContent = data.getSelectedContent();
            entities = new DependencyItem[selectedContent.size()];
            int i=0;
            for ( final SsgClusterContent content : selectedContent ) {
                entities[i] = new DependencyItem();
                entities[i++].fromContent(content);
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
        private final Integer version;
        private final String mappingKey;
        private final ExternalEntityHeader header;

        DependencyKey( final String clusterId,
                       final ExternalEntityHeader header ) {
            this.clusterId = clusterId;
            this.type = header.getType();
            this.id = header.getExternalId();
            this.version = header.getVersion();
            this.mappingKey = header.getMappingKey();
            this.header = header;
        }

        @Override
        public String toString() {
            return "DependencyKey[clusterId='"+clusterId+"'; id='"+id+"'; type='"+type+"'; version='" + version +"']";
        }

        @SuppressWarnings({"RedundantIfStatement"})
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            DependencyKey that = (DependencyKey) o;

            if (clusterId != null ? !clusterId.equals(that.clusterId) : that.clusterId != null) return false;
            if (id != null ? !id.equals(that.id) : that.id != null) return false;
            if (mappingKey != null ? !mappingKey.equals(that.mappingKey) : that.mappingKey != null) return false;
            if (type != that.type) return false;
            if (version != null ? !version.equals(that.version) : that.version != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result;
            result = (clusterId != null ? clusterId.hashCode() : 0);
            result = 31 * result + (type != null ? type.hashCode() : 0);
            result = 31 * result + (id != null ? id.hashCode() : 0);
            result = 31 * result + (version != null ? version.hashCode() : 0);
            result = 31 * result + (mappingKey != null ? mappingKey.hashCode() : 0);
            result = 31 * result + (header != null ? header.hashCode() : 0);
            return result;
        }

        public ExternalEntityHeader asEntityHeader() {
            return header;
        }
    }

    private static final class DependencyItem implements Comparable, Serializable {
        private ExternalEntityHeader entityHeader;

        @SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration"})
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
        private boolean same; // flag for this entity is the same identity on source/destination

        private DependencyItem() {
        }

        private DependencyItem( final ExternalEntityHeader entityHeader  ) {
            this( entityHeader,
                  OPTIONAL == entityHeader.getValueMapping() ? null :
                  NONE == entityHeader.getValueMapping() || entityHeader.getMappedValue() != null,
                  false );
        }

        private DependencyItem( final ExternalEntityHeader entityHeader,
                                final Boolean resolved ) {
            this( entityHeader, resolved, false );
        }

        private DependencyItem( final ExternalEntityHeader entityHeader,
                                final Boolean resolved,
                                final boolean hidden ) {
            this( entityHeader, resolved, hidden, false );
        }

        /**
         * Constructs a new dependency item copying any metadata from the given item.
         *
         * <p>This is useful when getting updated entity header information.</p>
         *
         * @param entityHeader The header to use
         * @param dependencyItem The dependency item to use for metadata
         */
        private DependencyItem( final ExternalEntityHeader entityHeader,
                                final DependencyItem dependencyItem ) {
            this( entityHeader, dependencyItem.resolved, dependencyItem.hidden, dependencyItem.same );
        }

        private DependencyItem( final ExternalEntityHeader entityHeader,
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

        public void fromContent(final SsgClusterContent data) {
            id = data.getExternalId();
            type = SsgClusterContent.getJsonType(data.getEntityType());
            name = data.getName();
            version = data.getVersion();
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
                displayName = displayName.substring( 0, index ) + " \n" + displayName.substring( index );
            }

            return displayName;
        }
    }

    private static interface MigrationSource extends Serializable {
        Collection<DependencyItem> getDependencies( String entityType, String entityId ) throws FindException, GatewayException, MigrationApi.MigrationException;
        Collection<DependencyItem> getDependencies( DependencyItemsRequest request ) throws FindException, GatewayException, MigrationApi.MigrationException;
        DependencyItem getItem( String entityType, String entityId ) throws FindException, GatewayException;
        MigrationBundle exportBundle( final DependencyItemsRequest requestedItems ) throws FindException, GatewayException, MigrationApi.MigrationException;
    }

    private class DefaultMigrationSource implements MigrationSource {
        private final String sourceClusterId;

        private DefaultMigrationSource( final String sourceClusterId ) {
            this.sourceClusterId = sourceClusterId;
        }

        @Override
        public Collection<DependencyItem> getDependencies( final String entityType, final String entityId ) throws FindException, GatewayException, MigrationApi.MigrationException {
            final DependencyItemsRequest dir = buildDependencyItemsRequest( entityType, entityId );
            return getDependencies( dir );
        }

        @Override
        public Collection<DependencyItem> getDependencies( final DependencyItemsRequest dir ) throws FindException, GatewayException, MigrationApi.MigrationException {
            return new ArrayList<DependencyItem>(retrieveDependencies( dir ));
        }

        @Override
        public DependencyItem getItem( final String entityType, final String entityId ) throws FindException, GatewayException {
            DependencyItem detailItem = null;

            // discard the dependencies that no longer exist on the destination cluster
            final DependencyItemsRequest dir = buildDependencyItemsRequest( entityType, entityId );
            SsgCluster cluster = ssgClusterManager.findByGuid( dir.clusterId );
            GatewayClusterClient destContext = gatewayClusterClientManager.getGatewayClusterClient(cluster, getUser());
            MigrationApi destMigrationApi = destContext.getUncachedMigrationApi();
            Collection<ExternalEntityHeader> headers = destMigrationApi.checkHeaders( Collections.singleton( dir.entities[0].asEntityHeader() ) );
            if ( headers != null && !headers.isEmpty() ) {
                ExternalEntityHeader header = headers.iterator().next();
                if ( header.getProperty("Alias Of") != null || header.getProperty("Alias Of Internal") != null ) {
                    // resolve alias
                    ExternalEntityHeader aliasTargetHeader =
                            new ExternalEntityHeader( header.getProperty("Alias Of"),
                                                      EntityType.valueOf(header.getProperty("Alias Type")),
                                                      header.getProperty("Alias Of Internal"), null, null, -1 );
                    headers = destMigrationApi.checkHeaders( Collections.singleton( aliasTargetHeader ) );
                    if ( headers != null && !headers.isEmpty() ) {
                        detailItem = new DependencyItem( headers.iterator().next(), false );
                    }
                } else {
                    detailItem = new DependencyItem( header, false );
                }
            }

            return detailItem;
        }

        @Override
        public MigrationBundle exportBundle( final DependencyItemsRequest requestedItems ) throws FindException, GatewayException, MigrationApi.MigrationException {
            SsgCluster sourceCluster = ssgClusterManager.findByGuid(sourceClusterId);
            GatewayClusterClient sourceContext = gatewayClusterClientManager.getGatewayClusterClient(sourceCluster, getUser());
            MigrationApi sourceMigrationApi = sourceContext.getUncachedMigrationApi();
            return sourceMigrationApi.exportBundle( requestedItems.asEntityHeaders() );
        }

        private DependencyItemsRequest buildDependencyItemsRequest( final String entityType, final String entityId ) {
            DependencyItemsRequest dir = new DependencyItemsRequest();
            dir.clusterId = sourceClusterId;
            dir.entities = new DependencyItem[]{ new DependencyItem() };
            dir.entities[0].type = entityType;
            dir.entities[0].id = entityId;
            return dir;
        }
    }

    private static class OfflineMigrationContentProvider implements SSGClusterContentSelectorPanel.SsgClusterContentProvider, IDetachable {
        private final MigrationRecordModel migrationRecordModel;

        private OfflineMigrationContentProvider( final MigrationRecordModel migrationRecordModel ) {
            this.migrationRecordModel = migrationRecordModel;
        }

        @Override
        public void detach() {
            migrationRecordModel.detach();
        }

        @Override
        public Collection<SsgClusterContent> getContent( final EntityType[] entityTypes ) {
            final List<SsgClusterContent> content = new ArrayList<SsgClusterContent>();

            final MigrationRecord record = migrationRecordModel.getObject();
            if ( record != null ) {
                final MigrationBundle bundle = MigrationBundle.deserializeXml( record.getBundleXml() );

                for ( final MigratedItem item : record.getMigrationSummary().getMigratedItems() ) {
                    final ExternalEntityHeader source = item.getSourceHeader();

                    if ( !ArrayUtils.contains( entityTypes, source.getType() ) ) continue;

                    String parentId = null;

                    for ( final MigrationDependency dependency : bundle.getMetadata().getDependencies( source ) ) {
                        if ( dependency.getDependency().getType() == EntityType.FOLDER ) {
                            parentId = dependency.getDependency().getExternalId();
                        }
                    }

                    String displayName = source.getDisplayName();
                    if ( displayName == null ) {
                        // Aliases will be missing a name
                        if ( source.getType() == EntityType.SERVICE_ALIAS ||
                             source.getType() == EntityType.POLICY_ALIAS ) {
                            final String externalId = source.getType() == EntityType.SERVICE_ALIAS ? source.getProperty( "Alias Of" ) : source.getProperty( "Alias Of Internal" );
                            final EntityType type = source.getType() == EntityType.SERVICE_ALIAS ? EntityType.SERVICE : EntityType.POLICY;
                            for ( final ExternalEntityHeader header : bundle.getMetadata().getAllHeaders() ) {
                                if ( header.getType() == type && header.getExternalId().equals( externalId ) ) {
                                    displayName = header.getDisplayName() + " alias";
                                    break;
                                }
                            }

                        }

                        if ( displayName == null ) {
                            displayName = "";
                        }
                    }

                    content.add( new SsgClusterContent( source.getExternalId(), null, parentId, source.getType(), displayName, source.getVersion() ) );

                    if ( source.getType() == EntityType.SERVICE || source.getType() == EntityType.SERVICE_ALIAS ) {
                        final String operations = source.getProperty( "WSDL Operations" );
                        if ( operations != null ) {
                            final StringTokenizer tokenizer = new StringTokenizer( operations, ", " );
                            while ( tokenizer.hasMoreTokens() ) {
                                content.add( new SsgClusterContent( UUID.randomUUID().toString(), source.getExternalId(), tokenizer.nextToken() ) );
                            }
                        }
                    }
                }
            }

            return content;
        }
    }

    private static class OfflineMigrationSource implements MigrationSource, IDetachable {
        private final MigrationRecordModel migrationRecordModel;

        private OfflineMigrationSource( final MigrationRecordModel migrationRecordModel ) {
            this.migrationRecordModel = migrationRecordModel;
        }

        @Override
        public void detach() {
            migrationRecordModel.detach();
        }

        @Override
        public Collection<DependencyItem> getDependencies( final String entityType, final String entityId ) {
            final Map<ExternalEntityHeader,DependencyItem> dependencies = new HashMap<ExternalEntityHeader,DependencyItem>();

            final MigrationRecord record = migrationRecordModel.getObject();
            final ExternalEntityHeader header = getHeader( entityType, entityId, true );
            if ( record != null && header != null ) {
                final MigrationBundle bundle = MigrationBundle.deserializeXml( record.getBundleXml() );
                addTransitiveDependencies( bundle.getMetadata(), dependencies, header );
            }

            return processDependencyItems( dependencies.values() );
        }

        @Override
        public Collection<DependencyItem> getDependencies( final DependencyItemsRequest dir ) {
            final Map<ExternalEntityHeader,DependencyItem> dependencies = new HashMap<ExternalEntityHeader,DependencyItem>();

            final MigrationRecord record = migrationRecordModel.getObject();
            if ( record != null ) {
                final MigrationBundle bundle = MigrationBundle.deserializeXml( record.getBundleXml() );
                for ( final DependencyItem item : dir.entities ) {
                    final ExternalEntityHeader header = getHeader( item.type, item.id, false );
                    if ( header != null ) {
                        dependencies.put( header, new DependencyItem( header, null, true ) );
                        addTransitiveDependencies( bundle.getMetadata(), dependencies, header );
                    }
                }
            }

            return processDependencyItems( dependencies.values() );
        }

        @Override
        public DependencyItem getItem( final String entityType, final String entityId ) {
            final ExternalEntityHeader header = getHeader( entityType, entityId, true );
            return header == null ? null : new DependencyItem( header );
        }

        private ExternalEntityHeader getHeader( final String entityType, final String entityId, final boolean resolveAliases ) {
            ExternalEntityHeader header = null;

            final MigrationRecord record = migrationRecordModel.getObject();
            final EntityType type = JSONConstants.EntityType.ENTITY_TYPE_MAP.get( entityType );
            if ( type != null && record != null ) {
                final Collection<MigratedItem> items = record.getMigrationSummary().getMigratedItems();
                header = getHeader( entityId, type, items );

                if ( header != null && resolveAliases ) {
                    if ( header.getType() == EntityType.SERVICE_ALIAS ||
                         header.getType() == EntityType.POLICY_ALIAS   ) {
                        final String aliasedId = header.getType() == EntityType.SERVICE_ALIAS ? header.getProperty( "Alias Of" ) : header.getProperty( "Alias Of Internal" );
                        final EntityType aliasedType = header.getType() == EntityType.SERVICE_ALIAS ? EntityType.SERVICE : EntityType.POLICY;
                        final ExternalEntityHeader aliasedHeader = getHeader( aliasedId, aliasedType, items );

                        if ( aliasedHeader != null ) {
                            header = aliasedHeader;
                        }
                    }
                }
            }

            return header;
        }

        private ExternalEntityHeader getHeader( final String entityId,
                                                final EntityType type,
                                                final Collection<MigratedItem> items ) {
            ExternalEntityHeader header = null;

            for ( final MigratedItem item : items ) {
                if ( item.getSourceHeader().getType() == type &&
                     item.getSourceHeader().getExternalId().equals( entityId ) ) {
                    header = item.getSourceHeader();
                    break;
                }
            }
            return header;
        }

        @Override
        public MigrationBundle exportBundle( final DependencyItemsRequest requestedItems ) throws GatewayException {
            final MigrationRecord record = migrationRecordModel.getObject();
            if ( record == null ) {
                throw new GatewayException( "Migration archive not available" );
            }
            final MigrationBundle bundle = MigrationBundle.deserializeXml( record.getBundleXml() );
            final MigrationMetadata metadata = bundle.getMetadata();

            // Identify all the dependencies of the requested items.
            final Map<ExternalEntityHeader,DependencyItem> dependencyMap = new HashMap<ExternalEntityHeader,DependencyItem>();
            for ( final ExternalEntityHeader header : requestedItems.asEntityHeaders() ) {
                dependencyMap.put( header, new DependencyItem( header ) );
                addTransitiveDependencies( metadata,  dependencyMap, header );
            }
            final Set<ExternalEntityHeader> requiredItems = new HashSet<ExternalEntityHeader>( dependencyMap.keySet() );
            for ( final ExternalEntityHeader header : new HashSet<ExternalEntityHeader>(requiredItems) ) {
                if ( header.getType() == EntityType.SERVICE ) {
                    // Special case for services that picks up service document dependants
                    for ( final MigrationDependency dependency : metadata.getDependants( header ) ) {
                        requiredItems.add( dependency.getDependant() );
                    }
                }
            }

            // Remove any items that are not required
            final Set<ExternalEntityHeader> headers = new HashSet<ExternalEntityHeader>(metadata.getHeaders());
            for ( final ExternalEntityHeader header : headers ) {
                if ( !requiredItems.contains( header ) ) {
                    metadata.removeHeader( header );
                }
            }
            final Set<MigrationDependency> dependencies = new HashSet<MigrationDependency>(metadata.getDependencies());
            for ( final MigrationDependency dependency : dependencies ) {
                if ( !requiredItems.contains( dependency.getDependant() ) ) {
                    metadata.removeDependency( dependency );
                }
            }
            final Set<ExternalEntityHeader> exportedHeaders = new HashSet<ExternalEntityHeader>(bundle.getExportedItems().keySet());
            for ( final ExternalEntityHeader header : exportedHeaders ) {
                if ( !requiredItems.contains( header ) ) {
                    bundle.removeExportedItem( header );
                }
            }

            return bundle;
        }

        private void addTransitiveDependencies( final MigrationMetadata metadata,
                                                final Map<ExternalEntityHeader,DependencyItem> dependencies,
                                                final ExternalEntityHeader header ) {
            for ( final MigrationDependency dependency : metadata.getDependencies( header ) ) {
                final ExternalEntityHeader depHeader = dependency.getDependency();

                final DependencyItem item;
                if ( dependency.getMappingType() != NONE || depHeader.isValueMappable() ) {
                    item = new DependencyItem( depHeader, metadata.isMappingRequired(depHeader) ? Boolean.FALSE : null );
                } else {
                    item = new DependencyItem( depHeader, null, true );
                }

                if ( !dependencies.containsKey( depHeader ) ) {
                    dependencies.put( depHeader, item );

                    // add dependencies recursively
                    addTransitiveDependencies( metadata, dependencies, depHeader );
                }
            }
        }
    }

    /**
     * A model to store the selected entity mappings (anything the user has
     * selected, not just mappings for currently selected source entities)
     */
    public final static class EntityMappingModel implements Serializable {
        /**
         * Map for entity mappings keyed by source item / destination cluster, values are the destination item.
         */
        private final Map<Pair<DependencyKey,String>,DependencyItem> dependencyMap =
            new HashMap<Pair<DependencyKey, String>, DependencyItem>();

        /**
         * Map for value mappings keyed by source item / destination cluster, values are the (string) mapped values
         */
        private final Map<Pair<ValueKey,String>,String> valueMap =
            new HashMap<Pair<ValueKey,String>,String>();

        /**
         * Update the mapped values in the given entity header.
         *
         * @param srcClusterId The source cluster id for the mapping
         * @param destClusterId The destination cluster id for the mapping
         * @param dep The ExternalEntityHeader to update.
         * @return The collection of values for persistence
         */
        public Collection<ExternalEntityHeader> updateMappedValues(final String srcClusterId,
                                                                   final String destClusterId,
                                                                   final MigrationDependency dep) {

            Collection<ExternalEntityHeader> pHeaders = new LinkedHashSet<ExternalEntityHeader>();

            ExternalEntityHeader eeh = dep.getDependency();
            if (eeh.isValueMappable()) {

                ExternalEntityHeader.ValueType valueType = eeh.getValueType();
                Collection<String> mappedValues = new ArrayList<String>();
                for (ExternalEntityHeader vmHeader : eeh.getValueMappableHeaders()) {
                    final Pair<ValueKey, String> valueKey = new Pair<ValueKey, String>(new ValueKey(srcClusterId, vmHeader), destClusterId);
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

        private ValueKey( final DependencyKey dependencyKey ) {
            this( dependencyKey.clusterId, dependencyKey.asEntityHeader() );
        }

        private ValueKey( final String sourceClusterId, final ExternalEntityHeader valueMappableHeader ) {
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

        private SearchTarget() {
            this.item = null;
        }

        private SearchTarget( final DependencyItem item ) {
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
    public final static class DependencySummaryModel implements Serializable {
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

    private final class BusyAjaxCallDecorator extends AjaxCallDecorator {
        @Override
        public CharSequence decorateScript( final CharSequence script ) {
            return " l7.Dialog.showBusyDialog(this); " + script;
        }

        @Override
        public CharSequence decorateOnSuccessScript( final CharSequence script ) {
            return " l7.Dialog.hideBusyDialog(); " + script;
        }

        @Override
        public CharSequence decorateOnFailureScript( final CharSequence script ) {
            return " l7.Dialog.hideBusyDialog(); " + script;
        }
    }

    private final class SSGClusterRefreshBehaviour extends AjaxEventBehavior {
        private final SSGClusterSelectorPanel clusterSelector;
        private final SSGClusterContentSelectorPanel contentSelector;
        private final WebMarkupContainer itemDetails;
        private final WebMarkupContainer itemDependencies;

        private SSGClusterRefreshBehaviour( final SSGClusterSelectorPanel clusterSelector,
                                            final SSGClusterContentSelectorPanel contentSelector,
                                            final WebMarkupContainer itemDetails,
                                            final WebMarkupContainer itemDependencies ) {
            super("onclick");
            this.clusterSelector = clusterSelector;
            this.contentSelector = contentSelector;
            this.itemDetails = itemDetails;
            this.itemDependencies = itemDependencies;
        }

        @Override
        protected IAjaxCallDecorator getAjaxCallDecorator() {
            return new BusyAjaxCallDecorator();
        }

        @Override
        protected void onEvent( final AjaxRequestTarget target ) {
            clusterSelector.refresh();
            target.addComponent( clusterSelector );

            if ( clusterSelector.getClusterId()==null ||
                 !clusterSelector.getClusterId().equals( contentSelector.getClusterId() ) ) {
                contentSelector.setClusterId( clusterSelector.getClusterId() );
                target.addComponent( contentSelector );
                showDependencies( itemDependencies, Collections.<DependencyItem>emptyList(), itemDetails, null );
                target.addComponent( itemDetails );
                target.addComponent( itemDependencies );
            }
        }
    }

    private final class ContentRefreshBehaviour extends AjaxEventBehavior {
        private final SSGClusterContentSelectorPanel contentSelector;
        private final WebMarkupContainer itemDetails;
        private final WebMarkupContainer itemDependencies;

        private ContentRefreshBehaviour( final SSGClusterContentSelectorPanel contentSelector,
                                         final WebMarkupContainer itemDetails,
                                         final WebMarkupContainer itemDependencies ) {
            super("onclick");
            this.contentSelector = contentSelector;
            this.itemDetails = itemDetails;
            this.itemDependencies = itemDependencies;
        }

        @Override
        protected IAjaxCallDecorator getAjaxCallDecorator() {
            return new BusyAjaxCallDecorator();
        }

        @Override
        protected void onEvent( final AjaxRequestTarget target ) {
            contentSelector.refresh();
            target.addComponent( contentSelector );
            showDependencies( itemDependencies, Collections.<DependencyItem>emptyList(), itemDetails, null );
            target.addComponent( itemDetails );
            target.addComponent( itemDependencies );
        }
    }

    private interface AjaxCallback extends Functions.UnaryVoid<AjaxRequestTarget>, Serializable{ }

    private final class MigrationSSGClusterSelectorPanel extends SSGClusterSelectorPanel {
        private final SSGClusterContentSelectorPanel contentSelectorPanel;
        private final WebMarkupContainer dialogContainer;
        private final WebMarkupContainer itemDetails;
        private final WebMarkupContainer itemDependencies;
        private final AjaxCallback selectionCallback;

        private MigrationSSGClusterSelectorPanel( final String id,
                                                  final IModel<User> userModel,
                                                  final IModel<String> clusterModel,
                                                  final SSGClusterContentSelectorPanel contentSelectorPanel,
                                                  final WebMarkupContainer dialogContainer,
                                                  final WebMarkupContainer itemDetails,
                                                  final WebMarkupContainer itemDependencies,
                                                  final AjaxCallback selectionCallback ) {
            super( id, userModel, clusterModel );
            this.contentSelectorPanel = contentSelectorPanel;
            this.dialogContainer = dialogContainer;
            this.itemDetails = itemDetails;
            this.itemDependencies = itemDependencies;
            this.selectionCallback = selectionCallback;
        }

        @Override
        protected void onClusterSelected( final AjaxRequestTarget target, final String clusterId ) {
            contentSelectorPanel.setClusterId( clusterId );
            showDependencies( itemDependencies, Collections.<DependencyItem>emptyList(), itemDetails, null );
            target.addComponent( contentSelectorPanel );
            target.addComponent( itemDetails );
            target.addComponent( itemDependencies );
            selectionCallback.call( target );
        }

        @Override
        protected void onClusterTrust( final String clusterId, final AjaxRequestTarget target ) {
            try {
                final String script = buildClusterConfigDialogJavascript( clusterId, "ssgClusterTrust", "?esmtrust=1" );
                target.appendJavascript( script );
            } catch ( GatewayException e ) {
                popupCloseDialog(dialogContainer, target, "Account Mapping Error", ExceptionUtils.getMessage(e));
            } catch ( FindException e ) {
                popupCloseDialog( dialogContainer, target, "Account Mapping Error", ExceptionUtils.getMessage( e ) );
            }
        }

        @Override
        protected void onClusterAccess( final String clusterId, final AjaxRequestTarget target ) {
            try {
                final Map<String,String> userProperties = userPropertyManager.getUserProperties( getUser() );
                final boolean mapped = userProperties.containsKey("cluster." +  clusterId + ".trusteduser");

                final String script;
                if ( mapped ) {
                    script = buildClusterConfigDialogJavascript( clusterId, "changeAccessAccount", null );
                } else {
                    script = buildClusterConfigDialogJavascript( clusterId, "mapAccessAccount", null );
                }
                target.appendJavascript( script );
            } catch ( GatewayException e ) {
                popupCloseDialog(dialogContainer, target, "Account Mapping Error", ExceptionUtils.getMessage(e));
            } catch ( FindException e ) {
                popupCloseDialog( dialogContainer, target, "Account Mapping Error", ExceptionUtils.getMessage( e ) );
            }
        }
    }

    private static class MigrationRecordModel extends LoadableDetachableModel<MigrationRecord> {
        private final MigrationRecordManager manager;
        private final long recordId;

        private MigrationRecordModel( final MigrationRecordManager manager,
                                      final MigrationRecord migrationRecord ) {
            super( migrationRecord );
            this.manager = manager;
            this.recordId = migrationRecord.getOid();
        }

        @Override
        protected MigrationRecord load() {
            MigrationRecord record = null;
            try {
                record = manager.findByPrimaryKey( recordId );
            } catch ( FindException e ) {
                logger.log( Level.WARNING, "Error loading migration archive", e );
            }
            return record;
        }
    }
}
