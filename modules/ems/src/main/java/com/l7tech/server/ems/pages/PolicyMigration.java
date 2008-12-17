package com.l7tech.server.ems.pages;

import com.l7tech.server.ems.NavigationPage;
import com.l7tech.server.ems.TypedPropertyColumn;
import com.l7tech.server.ems.migration.MigrationRecordManager;
import com.l7tech.server.ems.migration.MigrationMappingRecordManager;
import com.l7tech.server.ems.enterprise.SsgClusterManager;
import com.l7tech.server.ems.enterprise.SsgCluster;
import com.l7tech.server.ems.enterprise.JSONConstants;
import com.l7tech.server.ems.gateway.GatewayContextFactory;
import com.l7tech.server.ems.gateway.GatewayContext;
import com.l7tech.server.ems.gateway.GatewayException;
import com.l7tech.server.management.api.node.MigrationApi;
import com.l7tech.server.management.api.node.GatewayApi;
import com.l7tech.server.management.migration.bundle.MigrationMetadata;
import com.l7tech.server.management.migration.bundle.MigrationBundle;
import com.l7tech.server.management.migration.bundle.ExportedItem;
import com.l7tech.server.management.migration.bundle.MigratedItem;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.EntityHeaderRef;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.EntityHeaderSet;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.migration.MigrationMapping;
import com.l7tech.util.Pair;
import com.l7tech.util.ExceptionUtils;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.HiddenField;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.ajax.form.AjaxFormSubmitBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.protocol.http.WebRequest;
import org.mortbay.util.ajax.JSON;

import javax.xml.ws.soap.SOAPFaultException;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.*;
import java.io.Serializable;

/**
 *
 */
@NavigationPage(page="PolicyMigration",section="ManagePolicies",pageUrl="PolicyMigration.html")
public class PolicyMigration extends EmsPage  {

    //- PUBLIC

    public PolicyMigration() {
        final EntityMappingModel mappingModel = new EntityMappingModel();

        final WebMarkupContainer dialogContainer = new WebMarkupContainer("dialogContainer");
        dialogContainer.add( new EmptyPanel("dialog") );
        add( dialogContainer.setOutputMarkupId(true) );

        final WebMarkupContainer dependenciesContainer = new WebMarkupContainer("dependencies");
        dependenciesContainer.setOutputMarkupPlaceholderTag(true);
        final Form dependenciesOptionsContainer = new Form("dependencyOptionsContainer");
        YuiDataTable.contributeHeaders(this);

        final WebMarkupContainer srcItemDependencies = new WebMarkupContainer("srcItemDependencies");
        add( srcItemDependencies.setOutputMarkupId(true) );
        showDependencies( srcItemDependencies, Collections.<DependencyItem>emptyList() );
        srcItemDependencies.add( buildDependencyDisplayBehaviour(srcItemDependencies, "srcItemSelectionCallbackUrl" ) );

        final WebMarkupContainer destItemDependencies = new WebMarkupContainer("destItemDependencies");
        add( destItemDependencies.setOutputMarkupId(true) );
        showDependencies( destItemDependencies, Collections.<DependencyItem>emptyList() );
        destItemDependencies.add( buildDependencyDisplayBehaviour(destItemDependencies, "destItemSelectionCallbackUrl" ) );

        final DepenencySummaryModel dependencySummaryModel = new DepenencySummaryModel();
        final CandidateModel candidateModel = new CandidateModel();
        final SearchModel searchModel = new SearchModel();

        final YuiAjaxButton clearDependencyButton = new YuiAjaxButton("dependencyClearButton") {
            @Override
            protected void onSubmit( final AjaxRequestTarget ajaxRequestTarget, final Form form ) {
                final Pair<DependencyKey,String> mappingKey = new Pair<DependencyKey,String>(lastSourceKey, lastTargetClusterId);
                mappingModel.dependencyMap.put( mappingKey, null ); // put null so we don't reload the mapping from history
                updateDependencies( dependenciesContainer, dependenciesOptionsContainer, candidateModel, searchModel, mappingModel, dependencySummaryModel );
                setEnabled(false);
                for ( String id : DEPENDENCY_REFRESH_COMPONENTS ) ajaxRequestTarget.addComponent( dependenciesContainer.get(id) );
                ajaxRequestTarget.addComponent( dependenciesOptionsContainer );
            }
        };

        Form selectionJsonForm = new Form("selectionForm");
        final HiddenField hiddenSelectionForm = new HiddenField("selectionJson", new Model(""));
        final HiddenField hiddenDestClusterId = new HiddenField("destinationClusterId", new Model(""));
        final HiddenField hiddenDestFolderId = new HiddenField("destinationFolderId", new Model(""));
        final HiddenField hiddenEnableNewServices = new HiddenField("enableNewServices", new Model(""));
        final HiddenField hiddenOverwriteDependencies = new HiddenField("overwriteDependencies", new Model(""));
        selectionJsonForm.add( hiddenSelectionForm );
        selectionJsonForm.add( hiddenDestClusterId );
        selectionJsonForm.add( hiddenDestFolderId );
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

                    final String sourceClusterId = dir.clusterId;
                    final String targetClusterId = lastTargetClusterId;
                    final Collection<DependencyItem> deps = retrieveDependencies(dir);

                    lastDependencyItems = deps;
                    lastSourceKey = null;
                    candidateModel.reset();                    

                    loadMappings( mappingModel, sourceClusterId, targetClusterId, deps );
                    updateDependencies( dependenciesContainer, dependenciesOptionsContainer, candidateModel, searchModel, mappingModel, dependencySummaryModel );
                    addDependencyOptions( dependenciesContainer, dependenciesOptionsContainer, candidateModel, searchModel, mappingModel, dependencySummaryModel );
                    target.addComponent( dependenciesContainer );

                    // build info dialog
                    Label label = new Label(YuiDialog.getContentId(), summarize(dir, targetClusterId, visible(deps), mappingModel));
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
                    } else {
                        failureMessage = "Unexpected error from cluster.";                                
                        logger.log( Level.WARNING, "Error processing selection.", e);
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
                            final boolean enableServices = Boolean.valueOf(enableNewServices);
                            final boolean overwrite = Boolean.valueOf(overwriteDependencies);

                            // load mappings for top-level items that have been previously migrated
                            loadMappings( mappingModel, dir.clusterId, targetClusterId, buildDependencyItems(dir) );
                            TextPanel textPanel = new TextPanel(YuiDialog.getContentId(), new Model(performMigration( dir.clusterId, targetClusterId, targetFolderId, enableServices, overwrite, dir, mappingModel, true )));
                            YuiDialog dialog = new YuiDialog("dialog", "Confirm Migration", YuiDialog.Style.OK_CANCEL, textPanel, new YuiDialog.OkCancelCallback(){
                                @Override
                                public void onAction( final YuiDialog dialog, final AjaxRequestTarget target, final YuiDialog.Button button) {
                                    if ( button == YuiDialog.Button.OK ) {
                                        logger.info("Migration confirmed.");
                                        try {
                                            String message = performMigration( dir.clusterId, targetClusterId, targetFolderId, enableServices, overwrite, dir, mappingModel, false );
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
                                            } else {
                                                failureMessage = "Unexpected error from cluster.";
                                                logger.log( Level.WARNING, "Error processing selection.", e);
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
                    } else {
                        failureMessage = "Unexpected error from cluster.";
                        logger.log( Level.WARNING, "Error processing selection.", e);
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

        add( selectionJsonForm );

        dependenciesContainer.add( new Label("dependenciesTotalLabel", new PropertyModel(dependencySummaryModel, "totalDependencies")).setOutputMarkupId(true) );
        dependenciesContainer.add( new Label("dependenciesUnmappedLabel", new PropertyModel(dependencySummaryModel, "unmappedDependencies")).setOutputMarkupId(true) );
        dependenciesContainer.add( new Label("dependenciesRequiredUnmappedLabel", new PropertyModel(dependencySummaryModel, "requiredUnmappedDependencies")).setOutputMarkupId(true) );
        updateDependencies( dependenciesContainer, dependenciesOptionsContainer, candidateModel, searchModel, mappingModel, dependencySummaryModel );

        dependenciesContainer.add( dependenciesOptionsContainer.setOutputMarkupId(true) );

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
                addDependencyOptions( dependenciesContainer, dependenciesOptionsContainer, candidateModel, searchModel, mappingModel, dependencySummaryModel );
                ajaxRequestTarget.addComponent(dependenciesOptionsContainer);
            }
        });
        dependenciesOptionsContainer.add( clearDependencyButton.setOutputMarkupId(true) );
        dependenciesOptionsContainer.add( new Label("dependencyCandidateName", new PropertyModel(candidateModel, "name")) );
        dependenciesOptionsContainer.add( new Label("dependencyCandidateType", new PropertyModel(candidateModel, "type")) );

        addDependencyOptions( dependenciesContainer, dependenciesOptionsContainer, candidateModel, searchModel, mappingModel, dependencySummaryModel );

        add( dependenciesContainer.setOutputMarkupId(true) );
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( PolicyMigration.class.getName() );

    private static final String[] DEPENDENCY_REFRESH_COMPONENTS = { "dependenciesTable", "dependenciesTotalLabel", "dependenciesUnmappedLabel", "dependenciesRequiredUnmappedLabel" };
    private static final String[] SEARCH_REFRESH_COMPONENTS = { "dependencySearchManner", "dependencySearchText", "dependencySearchButton" };
    private static final String[] SELECTION_REFRESH_COMPONENTS = { "dependencyClearButton" };

    @SpringBean
    private SsgClusterManager ssgClusterManager;

    @SpringBean
    private MigrationRecordManager migrationRecordManager;

    @SpringBean
    private MigrationMappingRecordManager migrationMappingRecordManager;

    @SpringBean
    private GatewayContextFactory gatewayContextFactory;

    private DependencyKey lastSourceKey;
    private String lastSourceClusterId;
    private String lastTargetClusterId;
    private Collection<DependencyItem> lastDependencyItems = Collections.emptyList();

    private AbstractDefaultAjaxBehavior buildDependencyDisplayBehaviour( final WebMarkupContainer itemDependenciesContainer,
                                                                         final String jsVar ) {
        return new AbstractDefaultAjaxBehavior(){
            @Override
            public void renderHead( final IHeaderResponse iHeaderResponse ) {
                super.renderHead( iHeaderResponse );
                iHeaderResponse.renderJavascript("var "+jsVar+" = '"+getCallbackUrl(true)+"';", null);
            }

            @Override
            protected void respond( final AjaxRequestTarget ajaxRequestTarget ) {
                WebRequest request = (WebRequest) RequestCycle.get().getRequest();
                String clusterId = request.getParameter("clusterId");
                String type = request.getParameter("type");
                String id = request.getParameter("id");
                logger.info( "Processing request for cluster " + clusterId + " type " + type + " id " + id );
                DependencyItemsRequest dir = new DependencyItemsRequest();
                dir.clusterId = clusterId;
                dir.entities = new DependencyItem[]{ new DependencyItem() };
                dir.entities[0].type = type;
                dir.entities[0].id = id;
                List<DependencyItem> options = Collections.emptyList();
                try {
                    options = new ArrayList<DependencyItem>(retrieveDependencies( dir ));
                    for ( Iterator<DependencyItem> itemIter = options.iterator(); itemIter.hasNext();  ) {
                        DependencyItem item = itemIter.next();
                        if ( item.hidden || EntityType.FOLDER.toString().equals(item.type) ) {
                            itemIter.remove();
                        }
                    }
                } catch ( MigrationApi.MigrationException me ) {
                    logger.log( Level.INFO, "Error processing selection '"+ExceptionUtils.getMessage(me)+"'." );
                } catch ( SOAPFaultException sfe ) {
                    if ( !GatewayContext.isNetworkException(sfe) ) {
                        logger.log( Level.WARNING, "Error processing selection.", sfe);
                    }
                } catch ( GatewayException ge ) {
                    logger.log( Level.INFO, "Error processing selection '"+ExceptionUtils.getMessage(ge)+"'.", ExceptionUtils.getDebugException(ge) );
                } catch ( FindException fe ) {
                    logger.log( Level.WARNING, "Error processing selection.", fe );
                }
                
                showDependencies( itemDependenciesContainer, options );
                ajaxRequestTarget.addComponent( itemDependenciesContainer );
            }
        };
    }

    private List<DependencyItem> visible( final Collection<DependencyItem> items ) {
        List<DependencyItem> visibleDeps = new ArrayList<DependencyItem>( items.size() );

        for ( DependencyItem item : items ) {
            if ( !item.hidden ) visibleDeps.add( item );
        }

        return visibleDeps;
    }

    private void showDependencies( final WebMarkupContainer container, final List<DependencyItem> options ) {
        ListView listView = new ListView("optionRepeater", visible(options)) {
            @Override
            protected void populateItem( final ListItem item ) {
                final DependencyItem dependencyItem = ((DependencyItem)item.getModelObject());
                item.add(new Label("optional", dependencyItem.getOptional()).setEscapeModelStrings(false));
                item.add(new Label("name", dependencyItem.name));
                item.add(new Label("type", fromEntityType(EntityType.valueOf(dependencyItem.type))));
                item.add(new Label("version", dependencyItem.getVersionAsString()));
            }
        };

        if ( container.get( "optionRepeater" ) == null ) {
            container.add( listView.setOutputMarkupId(true) );
        } else {
            container.replace( listView.setOutputMarkupId(true) );
        }
    }

    private static String fromEntityType( final EntityType entityType ) {
        String type;

        switch ( entityType ) {
            case FOLDER:
                type = "folder";
                break;
            case SERVICE:
                type = "published service";
                break;
            case POLICY:
                type = "policy fragment";
                break;
            case ID_PROVIDER_CONFIG:
                type = "identity provider";
                break;
            case USER:
                type = "user";
                break;
            case GROUP:
                type = "group";
                break;
            case JMS_ENDPOINT:
                type = "jms endpoint";
                break;
            case TRUSTED_CERT:
                type = "trusted certificate";
                break;
            default:
                type = entityType.toString();
                break;
        }

        return type;
    }

    private void updateDependencies( final WebMarkupContainer dependenciesContainer,
                                     final WebMarkupContainer dependenciesOptionsContainer,
                                     final CandidateModel candidateModel,
                                     final SearchModel searchModel,
                                     final EntityMappingModel mappingModel,
                                     final DepenencySummaryModel dependencySummaryModel ) {
        final String sourceClusterId = this.lastSourceClusterId;
        final String targetClusterId = this.lastTargetClusterId;
        final Collection<DependencyItem> items = visible(this.lastDependencyItems);

        // update dependency counts and items destination names
        dependencySummaryModel.reset();
        for ( DependencyItem item : items ) {
            DependencyKey sourceKey = new DependencyKey( sourceClusterId, item.asEntityHeader() );
            Pair<DependencyKey,String> mappingKey = new Pair<DependencyKey,String>(sourceKey, targetClusterId);
            DependencyItem mappedItem = mappingModel.dependencyMap.get( mappingKey );
            if ( mappedItem != null ) {
                item.destName = mappedItem.name;
                dependencySummaryModel.incrementTotalDependencies();
            } else {
                item.destName = "-";
                if ( item.optional ) {
                    dependencySummaryModel.incrementUnmappedDependencies();
                } else {
                    dependencySummaryModel.incrementRequiredUnmappedDependencies();
                }
            }
        }

        // update UI
        final List<PropertyColumn> dependencyColumns =  Arrays.asList(
            new PropertyColumn(new Model(""), "uid"),
            new TypedPropertyColumn(new Model(""), "optional", "optional", String.class, false),
            new PropertyColumn(new Model("Name"), "name", "name"),
            new TypedPropertyColumn(new Model("Ver."), "version", "version", Integer.class, true),
            new PropertyColumn(new Model("Type"), "type", "type"),
            new PropertyColumn(new Model("Dest. Name"), "destName", "destName")
        );

        final YuiDataTable ydt = new YuiDataTable( "dependenciesTable", dependencyColumns, "name", true, items, null, "uid", true, null ){
            @Override
            @SuppressWarnings({"UnusedDeclaration"})
            protected void onSelect( final AjaxRequestTarget ajaxRequestTarget, final String value ) {
                if ( targetClusterId != null && !targetClusterId.isEmpty() && value != null && !value.isEmpty() ) {
                    String[] typeIdPair = value.split(":", 2);
                    logger.fine("Selected dependency for mapping id '"+typeIdPair[1]+"', type '"+typeIdPair[0]+"'.");

                    DependencyItem selectedItem = null;
                    for ( DependencyItem item : items ) {
                        if ( item.type.equals(typeIdPair[0]) && item.id.equals(typeIdPair[1]) ) {
                            selectedItem = item;
                            break;
                        }
                    }

                    if ( selectedItem != null ) {
                        lastSourceKey = new DependencyKey( sourceClusterId, selectedItem.asEntityHeader() );
                        candidateModel.setName( selectedItem.name );
                        candidateModel.setType( selectedItem.getType() );                        
                    } else {
                        lastSourceKey = null;
                        candidateModel.reset();
                    }

                    addDependencyOptions( dependenciesContainer, dependenciesOptionsContainer, candidateModel, searchModel, mappingModel, dependencySummaryModel );
                    ajaxRequestTarget.addComponent(dependenciesOptionsContainer);
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
                                       final WebMarkupContainer dependenciesOptionsContainer,
                                       final CandidateModel candidateModel,
                                       final SearchModel searchModel,
                                       final EntityMappingModel mappingModel,
                                       final DepenencySummaryModel dependencySummaryModel ) {
        String targetClusterId = lastTargetClusterId;
        DependencyKey sourceKey = lastSourceKey;
        final Pair<DependencyKey,String> mappingKey = new Pair<DependencyKey,String>(sourceKey, targetClusterId);

        WebMarkupContainer markupContainer = new WebMarkupContainer("dependencyOptions");
        if ( dependenciesOptionsContainer.get( markupContainer.getId() ) == null ) {
            dependenciesOptionsContainer.add( markupContainer.setOutputMarkupId(true) );
        } else {
            dependenciesOptionsContainer.replace( markupContainer.setOutputMarkupId(true) );
        }

        searchModel.setSearchManner("contains");
        searchModel.setSearchValue("");

        List<DependencyItem> options;
        if ( sourceKey == null  ) {
            options = Collections.emptyList();

            for ( String id : SEARCH_REFRESH_COMPONENTS ) {
                Component component = dependenciesOptionsContainer.get(id);
                component.setEnabled( false );
            }
        } else {
            options = retrieveDependencyOptions( lastTargetClusterId, lastSourceKey, searchModel.getSearchFilter() );

            for ( String id : SEARCH_REFRESH_COMPONENTS ) {
                Component component = dependenciesOptionsContainer.get(id);
                component.setEnabled( isSearchable( sourceKey.asEntityHeader().getType() ) );
            }
        }

        boolean selectedItem = mappingModel.dependencyMap.get( mappingKey ) != null;
        for ( String id : SELECTION_REFRESH_COMPONENTS ) {
            Component component = dependenciesOptionsContainer.get(id);
            component.setEnabled( selectedItem );
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
                        updateDependencies( dependenciesContainer, dependenciesOptionsContainer, candidateModel, searchModel, mappingModel, dependencySummaryModel );

                        for ( String id : SELECTION_REFRESH_COMPONENTS ) {
                            Component component = dependenciesOptionsContainer.get(id);
                            component.setEnabled( true );
                        }
                        
                        for ( String id : DEPENDENCY_REFRESH_COMPONENTS ) ajaxRequestTarget.addComponent( dependenciesContainer.get(id) );
                        ajaxRequestTarget.addComponent( dependenciesOptionsContainer );
                    }
                } );

                DependencyItem currentItem = mappingModel.dependencyMap.get( mappingKey );
                if ( currentItem!=null && currentItem.equals( dependencyItem ) ) {
                    radioComponent.add( new AttributeModifier("checked", true, new Model("checked")) );
                }

                item.add(radioComponent);
                item.add(new Label("name", dependencyItem.name));
                item.add(new Label("version", dependencyItem.getVersionAsString()));
            }
        });
    }

    private Collection<DependencyItem> retrieveDependencies( final DependencyItemsRequest request ) throws FindException, GatewayException, MigrationApi.MigrationException {
        Collection<DependencyItem> deps = new LinkedHashSet<DependencyItem>();

        SsgCluster cluster = ssgClusterManager.findByGuid(request.clusterId);
        if ( cluster != null ) {
            if ( cluster.getTrustStatus() ) {
                GatewayContext context = gatewayContextFactory.getGatewayContext( getUser(), cluster.getSslHostName(), cluster.getAdminPort() );
                MigrationApi api = context.getMigrationApi();
                MigrationMetadata metadata = api.findDependencies( request.asEntityHeaders() );
                for (EntityHeader header : metadata.getMappableDependencies()) {
                    deps.add( new DependencyItem( header, !metadata.isMappingRequired(header) ) );
                }

                for (EntityHeader header : metadata.getHeaders() ) {
                    boolean alreadyPresent = false;

                    for ( DependencyItem item : deps ) {
                        if ( item.asEntityHeader().equals(header) ) {
                            alreadyPresent = true;
                            break;
                        }
                    }

                    if ( !alreadyPresent ) deps.add( new DependencyItem( header, true, true ) );
                }
            }
        }

        return deps;
    }

    @SuppressWarnings({"unchecked"})
    private List<DependencyItem> retrieveDependencyOptions( final String targetClusterId, final DependencyKey sourceKey, final String filter ) {
        List<DependencyItem> deps = new ArrayList<DependencyItem>();

        try {
            SsgCluster cluster = ssgClusterManager.findByGuid(targetClusterId);
            if ( cluster != null ) {
                if ( cluster.getTrustStatus() ) {
                    GatewayContext context = gatewayContextFactory.getGatewayContext( getUser(), cluster.getSslHostName(), cluster.getAdminPort() );
                    MigrationApi api = context.getMigrationApi();
                    EntityHeader entityHeader = new EntityHeader( sourceKey.id, sourceKey.type, "", null );
                    Map candidates = MigrationApi.MappingCandidate.fromCandidates(api.retrieveMappingCandidates( Collections.singletonList( entityHeader ), filter ));
                    if ( candidates != null && candidates.containsKey(entityHeader) ) {
                        EntityHeaderSet<EntityHeader> entitySet = (EntityHeaderSet<EntityHeader>) candidates.get(entityHeader);
                        if ( entitySet != null ) {
                            for ( EntityHeader header : entitySet ) {
                                deps.add( new DependencyItem( header, true) );
                            }
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
            if ( !GatewayContext.isNetworkException( sfe ) ) {
                logger.log( Level.WARNING, "Error while gettings dependency options.", sfe );
            }
        }

        return deps;
    }

    private Collection<DependencyItem> buildDependencyItems( final DependencyItemsRequest dependencyItemsRequest ) {
        Collection<DependencyItem> items = new ArrayList<DependencyItem>();

        for ( EntityHeader entityHeader : dependencyItemsRequest.asEntityHeaders() ) {
            items.add( new DependencyItem( entityHeader, true ) );
        }

        return items;
    }

    private void loadMappings( final EntityMappingModel mappingModel,
                               final String sourceClusterId,
                               final String targetClusterId,
                               final Collection<DependencyItem> dependencyItems ) {
        if ( !dependencyItems.isEmpty() ) {
            try {
                // load mappings saved in EM db
                for ( DependencyItem item : dependencyItems ) {
                    DependencyKey sourceKey = new DependencyKey( sourceClusterId, item.asEntityHeader() );
                    Pair<DependencyKey,String> mapKey = new Pair<DependencyKey,String>( sourceKey, targetClusterId );
                    if ( !mappingModel.dependencyMap.containsKey(mapKey) ) {
                        EntityHeader targetHeader = migrationMappingRecordManager.findEntityHeaderForMapping( sourceClusterId, item.asEntityHeader(), targetClusterId );
                        if ( targetHeader != null ) {
                            mappingModel.dependencyMap.put( mapKey, new DependencyItem( targetHeader, true ) );
                        }
                    }
                }


            } catch ( FindException fe ) {
                logger.log( Level.WARNING, "Error loading dependency mappings.", fe );
            }

            try {
                // discard the dependencies that no longer exist on the target cluster
                SsgCluster targetCluster = ssgClusterManager.findByGuid(targetClusterId);
                GatewayContext targetContext = gatewayContextFactory.getGatewayContext(getUser(), targetCluster.getSslHostName(), targetCluster.getAdminPort());
                MigrationApi targetMigrationApi = targetContext.getMigrationApi();

                Collection<EntityHeader> headersToCheck = new HashSet<EntityHeader>();
                for (DependencyItem dependencyItem : mappingModel.dependencyMap.values()) {
                    if (dependencyItem == null) continue;
                    headersToCheck.add(dependencyItem.asEntityHeader());
                }
                Collection<EntityHeader> validatedHeaders = targetMigrationApi.checkHeaders(headersToCheck);
                if (validatedHeaders != null && validatedHeaders.size() > 0) {
                    Set<Pair<DependencyKey,String>> keysToRemove = new HashSet<Pair<DependencyKey, String>>();
                    for(Pair<DependencyKey,String> mapKey : mappingModel.dependencyMap.keySet()) {
                        DependencyItem dependencyItem = mappingModel.dependencyMap.get(mapKey);
                        if (dependencyItem == null || ! validatedHeaders.contains(dependencyItem.asEntityHeader())) {
                            keysToRemove.add(mapKey);
                        }
                    }
                    for(Pair<DependencyKey,String> mapKey : keysToRemove) {
                        mappingModel.dependencyMap.remove(mapKey);
                    }
                } else {
                    mappingModel.dependencyMap.clear();
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error validating mappings against the target cluster.", e);
                mappingModel.dependencyMap.clear();
            }
        }
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
                    Collection<DependencyItem> items = retrieveDependencies( requestedItems );
                    loadMappings( mappingModel, requestedItems.clusterId, targetClusterId, items );
                    StringBuilder builder = new StringBuilder();
                    for ( DependencyItem item : items ) {
                        DependencyKey sourceKey = new DependencyKey( sourceClusterId, item.asEntityHeader() );
                        Pair<DependencyKey,String> mapKey = new Pair<DependencyKey,String>( sourceKey, targetClusterId );
                        if ( !item.optional && mappingModel.dependencyMap.get(mapKey) == null ) {
                            EntityHeader ih = item.asEntityHeader();
                            builder.append(ih.getType().getName()).append(", ").append(ih.getName())
                                .append("(#").append(ih.getStrId()).append(")\n");
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
                                     final boolean enableNewServices,
                                     final boolean overwriteDependencies,
                                     final DependencyItemsRequest requestedItems,
                                     final EntityMappingModel mappingModel,
                                     final boolean dryRun ) throws MigrationFailedException {
        String summary = "";
        try {
            SsgCluster sourceCluster = ssgClusterManager.findByGuid(sourceClusterId);
            SsgCluster targetCluster = ssgClusterManager.findByGuid(targetClusterId);
            if ( sourceCluster != null && targetCluster != null) {
                if ( sourceCluster.getTrustStatus() && targetCluster.getTrustStatus() ) {
                    GatewayContext sourceContext = gatewayContextFactory.getGatewayContext( getUser(), sourceCluster.getSslHostName(), sourceCluster.getAdminPort() );
                    MigrationApi sourceMigrationApi = sourceContext.getMigrationApi();

                    GatewayContext targetContext = gatewayContextFactory.getGatewayContext( getUser(), targetCluster.getSslHostName(), targetCluster.getAdminPort() );
                    MigrationApi targetMigrationApi = targetContext.getMigrationApi();
                    GatewayApi targetGatewayApi = targetContext.getApi();

                    MigrationBundle export = sourceMigrationApi.exportBundle( requestedItems.asEntityHeaders() );
                    MigrationMetadata metadata = export.getMetadata();
                    Set<Pair<EntityHeaderRef,EntityHeader>> mappings = new HashSet<Pair<EntityHeaderRef, EntityHeader>>();
                    for ( Map.Entry<Pair<DependencyKey,String>,DependencyItem> mapping : mappingModel.dependencyMap.entrySet() ) {
                        if ( mapping.getValue() != null && mapping.getKey().left.clusterId.equals(sourceClusterId) && mapping.getKey().right.equals(targetClusterId) ) {
                            mappings.add(new Pair<EntityHeaderRef,EntityHeader>(
                                new EntityHeaderRef( mapping.getKey().left.type, mapping.getKey().left.id ),
                                mapping.getValue().asEntityHeader() ));
                            migrationMappingRecordManager.persistMapping(
                                    sourceCluster.getGuid(),
                                    mapping.getKey().left.asEntityHeader(),
                                    targetCluster.getGuid(),
                                    mapping.getValue().asEntityHeader() );
                        }
                    }
                    metadata.mapNames(mappings);

                    Collection<GatewayApi.EntityInfo> folders = targetGatewayApi.getEntityInfo( Collections.singleton(EntityType.FOLDER) );
                    EntityHeader targetFolderHeader = null;
                    for ( GatewayApi.EntityInfo info : folders ) {
                        if ( targetFolderId.equals( info.getId() ) ) {
                            targetFolderHeader = new EntityHeader(info.getId(), EntityType.FOLDER, info.getName(), null, info.getVersion());
                        }
                    }

                    Collection<MigratedItem> migratedItems = targetMigrationApi.importBundle( export, targetFolderHeader, false, overwriteDependencies, enableNewServices, dryRun);
                    summary = summarize(export, migratedItems, summarize(folders, targetFolderId), enableNewServices, overwriteDependencies, dryRun);
                    if ( !dryRun ) {
                        if ( migratedItems != null ) {
                            for ( MigratedItem item : migratedItems ) {
                                EntityHeader source = item.getSourceHeader();
                                EntityHeader target = item.getTargetHeader();

                                if ( source != null && target != null ) {
                                    migrationMappingRecordManager.persistMapping( sourceCluster.getGuid(), source, targetCluster.getGuid(), target );
                                }
                            }
                        }
                        migrationRecordManager.create( null, getUser(), sourceCluster, targetCluster, summary, new byte[]{} ); // TODO save migrated bundle
                    }
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

        return summary;
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
            if ( !item.optional && mappings.dependencyMap.get(mapKey)==null ) {
                count++;
            }
        }
        builder.append(count);
        builder.append(" dependencies require mapping.</p>");


        return builder.toString();
    }

    private String summarize( final Collection<GatewayApi.EntityInfo> folders, final String targetFolderId ) throws FindException {
        StringBuilder builder = new StringBuilder();

        List<GatewayApi.EntityInfo> folderPath = new ArrayList<GatewayApi.EntityInfo>();
        String targetId = targetFolderId;
        while ( targetId != null ) {
            GatewayApi.EntityInfo folder = null;
            for ( GatewayApi.EntityInfo info : folders ) {
                if ( targetId.equals( info.getId() ) ) {
                    folder = info;
                    break;
                }
            }

            if ( folder == null ) {
                throw new FindException("Could not find target folder.");
            }

            targetId = folder.getParentId();
            if ( targetId != null || !folder.getName().equals("Root Node") ) {
                folderPath.add(0, folder);
            }
        }

        if ( folderPath.isEmpty() ) {
            builder.append("/");
        } else {
            for ( GatewayApi.EntityInfo folder : folderPath ) {
                builder.append( "/ " );
                builder.append( folder.getName() );
                builder.append( " " );
            }
        }

        return builder.toString().trim();
    }

    private String summarize( final MigrationBundle export, final Collection<MigratedItem> migratedItems, String targetFolderPath, final boolean enabled, final boolean overwrite, boolean dryRun ) {
        StringBuilder builder = new StringBuilder();

        MigrationMetadata metadata = export.getMetadata();

        // overview
        builder.append( "Migration Options\n" );
        builder.append( "Imported to folder: " );
        builder.append( targetFolderPath );
        builder.append( "\n" );
        builder.append( "Services enabled on import: " );
        builder.append( enabled );
        builder.append( "\n" );
        builder.append( "Existing dependencies replaced: " );
        builder.append( overwrite );
        builder.append( "\n" );
        builder.append( "Services migrated: " );
        builder.append( count(export.getExportedItems(),metadata,EntityType.SERVICE) );
        builder.append( "\n" );
        builder.append( "Policies migrated: " );
        builder.append( count(export.getExportedItems(),metadata,EntityType.POLICY) );
        builder.append( "\n\n" );

        // entity details
        builder.append( "Migrated Data\n" );
        if ( migratedItems != null ) {
            for ( MigratedItem item : migratedItems ) {
                EntityHeader ih = dryRun ? item.getSourceHeader() : item.getTargetHeader();
                builder.append(ih.getType().getName()).append(", ").append(ih.getName())
                    .append("(#").append(ih.getStrId()).append("): ").append(item.getStatus()).append("\n");
            }
        } else {
            builder.append("None.\n");
        }
        builder.append( "\n" );

        // entity mappings
        StringBuilder mappingBuilder = new StringBuilder();
        for ( MigrationMapping mapping : metadata.getMappings() ) {
            if ( mapping.isMappedDependency() && !mapping.isUploadedByParent() ) {
                EntityHeaderRef sourceRef = mapping.getSourceDependency();
                EntityHeaderRef targetRef = mapping.getMappedDependency();
                EntityHeader sourceHeader = metadata.getOriginalHeader( sourceRef );
                EntityHeader targetHeader = metadata.getHeader( targetRef );
                if (targetHeader == null) continue;
                mappingBuilder.append( fromEntityType(sourceRef.getType()) );
                mappingBuilder.append( ", " );
                mappingBuilder.append( sourceHeader.getName() );
                mappingBuilder.append( " (#" );
                mappingBuilder.append( sourceHeader.getStrId() );
                mappingBuilder.append( ") mapped to " );
                mappingBuilder.append( targetHeader.getName() );
                mappingBuilder.append( " (#" );
                mappingBuilder.append( targetHeader.getStrId() );
                mappingBuilder.append( ")" );
                mappingBuilder.append( "\n" );
            }
        }
        String mappingText = mappingBuilder.toString();
        if ( !mappingText.isEmpty() ) {
            builder.append( "Mappings\n" );
            builder.append( mappingText );
        }

        return builder.toString();
    }

    private int count( final Collection<ExportedItem> items, final MigrationMetadata metadata, final EntityType type ) {
        int count = 0;

        for ( ExportedItem item : items ) {
            if ( !item.isMappedValue() && !metadata.isUploadedByParent(item.getHeaderRef()) && item.getHeaderRef().getType() == type ) {
                count++;
            }
        }

        return count;
    }

    private static String toImgIcon( final boolean optional ) {
        String icon = "";

        if ( !optional ) {
            icon = "<img src=/images/unresolved.png />"; //TODO JSON quote escaping
        }

        return icon;
    }

    private static boolean isSearchable( final EntityType type ) {
        return  type == EntityType.POLICY ||
                type == EntityType.USER ||
                type == EntityType.GROUP;
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

        public Collection<EntityHeader> asEntityHeaders() {
            Collection<EntityHeader> headers = new ArrayList<EntityHeader>();

            for ( DependencyItem entity : entities ) {
                EntityType type = JSONConstants.Entity.ENTITY_TYPE_MAP.get( entity.type );
                if ( type != null ) {
                    headers.add( new EntityHeader( entity.id, type, entity.name, null) );
                } else {
                    logger.warning("Entity with unknown type '"+entity.type+"' requested.");
                }
            }

            return headers;
        }
    }

    private static final class DependencyKey implements Serializable {
        private final String clusterId;
        private final EntityType type;
        private final String id;
        private final EntityHeader header;

        DependencyKey( final String clusterId,
                       final EntityHeader header ) {
            this.clusterId = clusterId;
            this.type = header.getType();
            this.id = header.getStrId();
            this.header = header;
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

            return true;
        }

        @Override
        public int hashCode() {
            int result;
            result = clusterId.hashCode();
            result = 31 * result + type.hashCode();
            result = 31 * result + id.hashCode();
            return result;
        }

        public EntityHeader asEntityHeader() {
            return header;
        }
    }

    private static final class DependencyItem implements JSON.Convertible, Serializable {
        private EntityHeader entityHeader;
        private String uid; // id and type
        private String id;
        private String type;
        private String name;
        private boolean optional;
        private boolean hidden;
        private Integer version;
        @SuppressWarnings({"UnusedDeclaration"})
        private String destName;

        public DependencyItem() {
        }

        public DependencyItem( final EntityHeader entityHeader,
                               final boolean optional ) {
            this( entityHeader, optional, false );
        }

        public DependencyItem( final EntityHeader entityHeader,
                               final boolean optional,
                               final boolean hidden ) {
            this.entityHeader = entityHeader;
            this.uid = entityHeader.getType().toString() +":" + entityHeader.getStrId();
            this.id = entityHeader.getStrId();
            this.type = entityHeader.getType().toString();
            this.name = entityHeader.getName();
            this.optional = optional;
            this.hidden = hidden;
            this.version = entityHeader.getVersion();
        }

        public String getType() {
            return fromEntityType(EntityType.valueOf(type));
        }

        public String getOptional() {
            return toImgIcon(optional);
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
        }

        @SuppressWarnings({"RedundantIfStatement"})
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            DependencyItem that = (DependencyItem) o;

            if (id != null ? !id.equals(that.id) : that.id != null) return false;
            if (name != null ? !name.equals(that.name) : that.name != null) return false;
            if (type != null ? !type.equals(that.type) : that.type != null) return false;
            if (uid != null ? !uid.equals(that.uid) : that.uid != null) return false;
            if (version != null ? !version.equals(that.version) : that.version != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result;
            result = (uid != null ? uid.hashCode() : 0);
            result = 31 * result + (id != null ? id.hashCode() : 0);
            result = 31 * result + (type != null ? type.hashCode() : 0);
            result = 31 * result + (name != null ? name.hashCode() : 0);
            result = 31 * result + (version != null ? version.hashCode() : 0);
            return result;
        }

        public String getVersionAsString() {
            return version==null ? "" : Integer.toString(version);
        }

        public EntityHeader asEntityHeader() {
            return entityHeader;
        }
    }

    /**
     * A model to store the selected entity mappings (anything the user has
     * selected, not just mappings for currently selected source entities)
     */
    public final static class EntityMappingModel implements Serializable {
        private final Map<Pair<DependencyKey,String>,DependencyItem> dependencyMap =
            new HashMap<Pair<DependencyKey,String>,DependencyItem>();
    }

    /**
     * A model to store a search manner and a value to search.
     */
    public final static class SearchModel implements Serializable {
        private String searchManner;
        private String searchValue;

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
                filter = "*" + filter + "*";
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
