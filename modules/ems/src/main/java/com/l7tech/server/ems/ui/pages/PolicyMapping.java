package com.l7tech.server.ems.ui.pages;

import com.l7tech.server.ems.ui.NavigationPage;
import com.l7tech.server.ems.migration.MigrationRecordManager;
import com.l7tech.server.ems.migration.MigrationRecord;
import com.l7tech.server.ems.migration.MigrationSummary;
import com.l7tech.util.TimeUnit;
import com.l7tech.util.Functions;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.SizeUnit;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.gateway.common.security.rbac.AttemptedReadAll;
import com.l7tech.identity.User;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.util.value.ValueMap;

import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.Serializable;

/**
 * todo: rename to PolicyHistory to be consistent with the UI
 */
@NavigationPage(page="PolicyMapping",pageIndex=300,section="ManagePolicies",pageUrl="PolicyHistory.html")
public class PolicyMapping extends EsmStandardWebPage {
    private static final Logger logger = Logger.getLogger(PolicyMapping.class.getName());

    @SpringBean
    private MigrationRecordManager migrationManager;

    private WebMarkupContainer migrationTableContainer;
    private WebMarkupContainer migrationSummaryContainer;
    private WebMarkupContainer dynamicDialogHolder;
    private Model dateStartModel;
    private Model dateEndModel;
    private MigrationModel selectedMigrationModel;

    public PolicyMapping() {
        selectedMigrationModel = new MigrationModel();

        // Create a form to select a migration record
        add(buildSelectMigrationForm("migrationSelectForm"));

        // Create a dynamic holder just in case for displaying warning dialog.
        initDynamicDialogHolder();
        add(dynamicDialogHolder);

        // Create a container to store migration record table.
        initMigrationTableContainer();
        add(migrationTableContainer);

        // Create a contaner to store migration summary
        initMigrationSummaryContainer();
        add(migrationSummaryContainer);
    }

    /**
     * Build a wicket form to select one of migration records from the migration table.
     * @param componentId: the wicket id.
     * @return a form for selecting a migration.
     */
    private Form buildSelectMigrationForm(String componentId) {
        Form form = new Form(componentId);

        Date now = new Date();
        Date last7thDay = new Date(now.getTime() - TimeUnit.DAYS.toMillis(7));
        dateStartModel = new Model(last7thDay);
        dateEndModel = new Model(now);
        
        YuiDateSelector startDate = new YuiDateSelector("migrationStartSelector", dateStartModel, null, now);
        YuiDateSelector endDate = new YuiDateSelector("migrationEndSelector", dateEndModel, last7thDay, now);

        startDate.addInteractionWithOtherDateSelector(endDate, false, new YuiDateSelector.InteractionTasker());
        endDate.addInteractionWithOtherDateSelector(startDate, true, new YuiDateSelector.InteractionTasker());

        form.add(startDate);
        form.add(endDate);
        form.add(new YuiAjaxButton("migrationSelectButton") {
            @Override
            protected void onSubmit( final AjaxRequestTarget ajaxRequestTarget, final Form form ) {
                initMigrationTableContainer();
                migrationSummaryContainer.setVisible(false);

                ajaxRequestTarget.addComponent(migrationTableContainer);
                ajaxRequestTarget.addComponent(migrationSummaryContainer);
            }
        });

        return form;
    }

    /**
     * Initialize the dynamic dialog holder/container.
     */
    private void initDynamicDialogHolder() {
        dynamicDialogHolder = new WebMarkupContainer("dynamic.holder");
        dynamicDialogHolder.add(new EmptyPanel("dynamic.holder.content"));
        dynamicDialogHolder.setOutputMarkupId(true);
    }

    /**
     * Initialize the migration-table container, which contains a migration table and a migration-deletion form
     */
    private void initMigrationTableContainer() {
        // Create a form for selected migration
        Form migrationForm = new Form("deleteMigrationForm");
        HiddenField hiddenFieldForMigration = new HiddenField("migrationId", new Model(""));
        migrationForm.add(hiddenFieldForMigration.setOutputMarkupId(true));

        YuiAjaxButton deleteMigrationButton = new YuiAjaxButton("deleteMigrationButton") {
            @Override
            protected void onSubmit(final AjaxRequestTarget ajaxRequestTarget, final Form form) {
                String warningText = "<p>Really delete migration record?</p>";
                Label label = new Label(YuiDialog.getContentId(), warningText);
                label.setEscapeModelStrings(false);
                YuiDialog dialog = new YuiDialog("dynamic.holder.content", "Confirm Migration Deletion", YuiDialog.Style.OK_CANCEL, label, new YuiDialog.OkCancelCallback() {
                    @Override
                    public void onAction( final YuiDialog dialog, final AjaxRequestTarget target, final YuiDialog.Button button) {
                        if ( button == YuiDialog.Button.OK ) {
                            final String migrationId = (String) form.get("migrationId").getModelObject();
                            try {
                                logger.fine("Deleting the migration (OID = " + migrationId + ")");

                                migrationManager.delete(Long.parseLong(migrationId));
                                migrationSummaryContainer.setVisible(false);

                                target.addComponent(migrationTableContainer);
                                target.addComponent(migrationSummaryContainer);
                            } catch (Exception e) {
                                logger.warning("Cannot delete the migration (OID = " + migrationId + "), '"+ ExceptionUtils.getMessage(e)+"'");
                            }
                        }
                        dynamicDialogHolder.replace(new EmptyPanel("dynamic.holder.content"));
                        target.addComponent(dynamicDialogHolder);
                    }
                });

                dynamicDialogHolder.replace(dialog);
                ajaxRequestTarget.addComponent(dynamicDialogHolder);
            }
        };

        YuiAjaxButton renameMigrationButton = new YuiAjaxButton("renameMigrationButton") {
            @Override
            protected void onSubmit(final AjaxRequestTarget ajaxRequestTarget, final Form form) {
                final String migrationId = (String) form.get("migrationId").getModelObject();

                final MigrationRecord record = findMigrationRecordById( migrationId );
                if ( record != null ) {
                    MigrationRecordEditPanel editPanel = new MigrationRecordEditPanel( YuiDialog.getContentId(), record );
                    YuiDialog dialog = new YuiDialog("dynamic.holder.content", "Migration Properties", YuiDialog.Style.OK_CANCEL, editPanel, new YuiDialog.OkCancelCallback() {
                        @Override
                        public void onAction( final YuiDialog dialog, final AjaxRequestTarget target, final YuiDialog.Button button) {
                            if ( button == YuiDialog.Button.OK ) {
                                try {
                                    logger.info("Renaming the migration (OID = " + migrationId + ")");

                                    if ( record.getName() == null ) record.setName("");
                                    migrationManager.update( record );
                                    selectedMigrationModel.setMigrationRecord( record );

                                    target.addComponent(migrationTableContainer);
                                    target.addComponent(migrationSummaryContainer);
                                } catch (Exception e) {
                                    logger.warning("Cannot update the migration (OID = " + migrationId + "), '"+ ExceptionUtils.getMessage(e)+"'.");
                                }
                            }
                            dynamicDialogHolder.replace(new EmptyPanel("dynamic.holder.content"));
                            target.addComponent(dynamicDialogHolder);
                        }
                    });

                    dynamicDialogHolder.replace(dialog);
                    ajaxRequestTarget.addComponent(dynamicDialogHolder);
                }
            }
        };

        YuiAjaxButton downloadArchiveButton = new YuiAjaxButton("downloadArchiveButton") {
            @Override
            protected void onSubmit(final AjaxRequestTarget ajaxRequestTarget, final Form form) {
                final String migrationId = (String) form.get("migrationId").getModelObject();
                if ( migrationId != null && migrationId.length() > 0 ) {
                    ValueMap vm = new ValueMap();
                    vm.add("migrationId", migrationId);
                    vm.add("disposition", "attachment");
                    ResourceReference resourceReference = new ResourceReference("migrationResource");
                    ajaxRequestTarget.appendJavascript("window.location = '" + RequestCycle.get().urlFor(resourceReference, vm).toString() + "';");
                }
            }
        };

        YuiAjaxButton deleteArchiveButton = new YuiAjaxButton("deleteArchiveButton") {
            @Override
            protected void onSubmit(final AjaxRequestTarget ajaxRequestTarget, final Form form) {
                final String migrationId = (String) form.get("migrationId").getModelObject();

                final MigrationRecord record = findMigrationRecordById( migrationId );
                if ( record != null ) {
                    String warningText = "<p>Really delete migration archive?</p>";
                    Label label = new Label(YuiDialog.getContentId(), warningText);
                    label.setEscapeModelStrings(false);
                    YuiDialog dialog = new YuiDialog("dynamic.holder.content", "Confirm Archive Deletion", YuiDialog.Style.OK_CANCEL, label, new YuiDialog.OkCancelCallback() {
                        @Override
                        public void onAction( final YuiDialog dialog, final AjaxRequestTarget target, final YuiDialog.Button button) {
                            if ( button == YuiDialog.Button.OK ) {
                                try {
                                    logger.fine("Deleting migration archive for migration record (OID = " + migrationId + ")");

                                    record.setBundleXml( null );
                                    migrationManager.update(record);
                                    migrationSummaryContainer.setVisible(false);

                                    target.addComponent(migrationTableContainer);
                                    target.addComponent(migrationSummaryContainer);
                                } catch (Exception e) {
                                    logger.warning("Cannot delete migration archive for migration record (OID = " + migrationId + "), '"+ ExceptionUtils.getMessage(e)+"'");
                                }
                            }
                            dynamicDialogHolder.replace(new EmptyPanel("dynamic.holder.content"));
                            target.addComponent(dynamicDialogHolder);
                        }
                    });

                    dynamicDialogHolder.replace(dialog);
                    ajaxRequestTarget.addComponent(dynamicDialogHolder);
                }
            }
        };

        YuiAjaxButton uploadArchiveButton = new YuiAjaxButton("uploadArchiveButton") {
            @Override
            protected void onSubmit(final AjaxRequestTarget ajaxRequestTarget, final Form form) {
                PolicyMigrationUploadPanel policyMigrationUploadPanel = new PolicyMigrationUploadPanel( YuiDialog.getContentId() ){
                    @Override
                    @SuppressWarnings({"UnusedDeclaration"})
                    protected void onSubmit(final AjaxRequestTarget target) {
                        migrationSummaryContainer.setVisible(false);
                        if ( target != null ) {
                            target.addComponent(migrationTableContainer);
                            target.addComponent(migrationSummaryContainer);
                        }
                    }
                };
                YuiDialog dialog = new YuiDialog("dynamic.holder.content", "Upload Migration Archive", YuiDialog.Style.OK_CANCEL, policyMigrationUploadPanel, new YuiDialog.OkCancelCallback(){
                    @Override
                    public void onAction( final YuiDialog dialog, final AjaxRequestTarget target, final YuiDialog.Button button ) {
                        //NOTE, due to YUI AJAX form submission for file upload this action is not run.
                    }
                } );
                policyMigrationUploadPanel.setSuccessScript( dialog.getSuccessScript() );
                dynamicDialogHolder.replace(dialog);
                if ( ajaxRequestTarget != null ) {
                    ajaxRequestTarget.addComponent(dynamicDialogHolder);
                }
            }
        };

        migrationForm.add(hiddenFieldForMigration.setOutputMarkupId(true));
        migrationForm.add(deleteMigrationButton.setEnabled(false));
        migrationForm.add(renameMigrationButton.setEnabled(false));
        migrationForm.add(downloadArchiveButton.setEnabled(false));
        migrationForm.add(deleteArchiveButton.setEnabled(false));
        migrationForm.add(uploadArchiveButton);

        // Create a migration table
        Panel migrationTable = buildMigrationTablePanel(hiddenFieldForMigration, new Button[]{deleteMigrationButton, renameMigrationButton}, new Button[]{ downloadArchiveButton, deleteArchiveButton } );

        // Add the above two components into the migrationTableContainer
        if (migrationTableContainer == null) {
            migrationTableContainer = new WebMarkupContainer("migrationTableContainer");
        } else {
            migrationTableContainer.removeAll();
        }
        migrationTableContainer.add(migrationTable);
        migrationTableContainer.add(migrationForm);
        migrationTableContainer.setOutputMarkupId(true);
    }

    /**
     * Find migration record by id.
     *
     * @param migrationId The record to find.
     * @return The record or null.
     */
    private MigrationRecord findMigrationRecordById( final String migrationId ) {
        MigrationRecord record = null;

        try {
            record = migrationManager.findByPrimaryKey(Long.parseLong(migrationId));
        } catch ( FindException fe ) {
            logger.log( Level.WARNING, "Error loading migration record.", fe );
        } catch ( NumberFormatException nfe ) {
            logger.log( Level.FINE, "Ignoring invalid migration id '"+migrationId+"'." );
        }

        return record;
    }

    /**
     * Build a migration table based on given the start date and the end date.
     *
     * @param hidden: the hidden field to store the selected migration id.
     * @param selectionComponents: the buttons needed to be updated after the migration is selected.
     * @return a panel that contains a migration table.
     */
    private Panel buildMigrationTablePanel( final HiddenField hidden, final Button[] selectionComponents, final Button[] archiveSelectionComponents ) {
        List<PropertyColumn> columns = new ArrayList<PropertyColumn>();
        columns.add(new PropertyColumn(new Model("id"), "id"));
        columns.add(new PropertyColumn(new StringResourceModel("migration.column.name", this, null), MigrationRecordManager.SortProperty.NAME.toString(), "name"));
        columns.add(new PropertyColumn(new StringResourceModel("migration.column.time", this, null), MigrationRecordManager.SortProperty.TIME.toString(), "timeCreated"));
        columns.add(new PropertyColumn(new StringResourceModel("migration.column.from", this, null), "sourceCluster"));
        columns.add(new PropertyColumn(new StringResourceModel("migration.column.to", this, null), "targetCluster"));
        columns.add(new PropertyColumn(new StringResourceModel("migration.column.size", this, null), "size"));

        Date start = startOfDay((Date)dateStartModel.getObject());
        Date end = new Date(startOfDay((Date)dateEndModel.getObject()).getTime() + TimeUnit.DAYS.toMillis(1));

        return new YuiDataTable("migrationTable", columns, "timeCreated", false, new MigrationDataProvider(start, end, "timeCreated", false), hidden, "id", true, selectionComponents) {
            @Override
            protected void onSelect( final AjaxRequestTarget ajaxRequestTarget, final String value) {
                boolean selected = false;
                boolean archivePresent = false;

                if (value != null && value.length() > 0) {
                    try {
                        MigrationRecord migration = migrationManager.findByPrimaryKey(Long.parseLong(value));
                        if ( migration != null ) {
                            selected = true;
                            if ( migration.getBundleSize() > 0 ) {
                                archivePresent = true;
                            }
                            selectedMigrationModel.setMigrationRecord( migration );
                        }
                    } catch ( FindException fe ) {
                        logger.log( Level.WARNING, "Error finding policy migration record (OID = '" + value + "').", fe );
                    }
                }

                for ( Button button : selectionComponents ) {
                    button.setEnabled( selected );
                    ajaxRequestTarget.addComponent( button );
                }

                for ( Button button : archiveSelectionComponents ) {
                    button.setEnabled( archivePresent );
                    ajaxRequestTarget.addComponent( button );                    
                }

                migrationSummaryContainer.setVisible(selected);
                ajaxRequestTarget.addComponent(migrationSummaryContainer);
            }
        };
    }

    /**
     * Initialize the migration-summary container, which contains a text area displaying the migration summary.
     */
    private void initMigrationSummaryContainer() {
        migrationSummaryContainer = new WebMarkupContainer("migrationSummaryContainer");
        migrationSummaryContainer.add(new MultiLineLabel("migrationSummaryTextarea", new PropertyModel(selectedMigrationModel, "summary")));
        migrationSummaryContainer.add(new Label("name", new PropertyModel(selectedMigrationModel, "name")));
        migrationSummaryContainer.add(new Label("timeCreated", new PropertyModel(selectedMigrationModel, "timeCreated")));
        migrationSummaryContainer.add(new Label("sourceCluster", new PropertyModel(selectedMigrationModel, "sourceCluster")));
        migrationSummaryContainer.add(new Label("targetCluster", new PropertyModel(selectedMigrationModel, "targetCluster")));
        migrationSummaryContainer.setOutputMarkupPlaceholderTag(true);
        migrationSummaryContainer.setVisible(false);
    }

    private Date startOfDay( final Date date ) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone( TimeZone.getTimeZone( getSession().getTimeZoneId() ) );
        calendar.setTime(date);

        calendar.set(Calendar.HOUR, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar.getTime();
    }

    /**
     * Migration Model, which has the following attributes: id, name, time created, source cluster, and destination cluster.
     */
    private final class MigrationModel implements Serializable {
        private MigrationRecord migration;

        MigrationModel(){            
        }

        MigrationModel(MigrationRecord migration) {
            this.migration = migration;
        }

        public String getId() {
            return migration==null ? null : migration.getId();
        }

        public Date getTimeCreated() {
            return migration==null ? null : new Date(migration.getTimeCreated());
        }

        public String getSourceCluster() {
            return migration==null ? null : migration.getSourceClusterName();
        }

        public String getTargetCluster() {
            return migration==null ? null : migration.getTargetClusterName();
        }

        public String getName() {
            return migration==null ? null : migration.getName();
        }

        public String getSize() {
            return migration==null ? null : SizeUnit.format(migration.getBundleSize());
        }

        public String getSummary() {
            return migration==null ? null : MigrationSummary.deserializeXml(migration.getSummaryXml()).toString();
        }

        public void setMigrationRecord(final MigrationRecord migration) {
            this.migration = migration;
        }
    }

    /**
     * Migration Data Provider used by the migration table.
     */
    private final class MigrationDataProvider extends SortableDataProvider {
        private final Date start;
        private final Date end;
        private final User user;

        public MigrationDataProvider(final Date start, final Date end, String sort, boolean asc) {
            this.start = start;
            this.end = end;

            User user = null;
            if ( !securityManager.hasPermission( new AttemptedReadAll( EntityType.ESM_MIGRATION_RECORD ) ) ) {
                user = securityManager.getLoginInfo( ((WebRequest) RequestCycle.get().getRequest()).getHttpServletRequest().getSession(true) ).getUser();
            }
            this.user = user;

            setSort(sort, asc);
        }

        @Override
        public Iterator iterator(int first, int count) {
            try {
                MigrationRecordManager.SortProperty sort = MigrationRecordManager.SortProperty.valueOf(getSort().getProperty());
                Iterator<MigrationRecord> itr = migrationManager.findPage(user, sort, getSort().isAscending(), first, count, start, end).iterator();

                return Functions.map(itr, new Functions.Unary<MigrationModel, MigrationRecord>() {
                    @Override
                    public MigrationModel call(MigrationRecord migration) {
                        return new MigrationModel(migration);
                    }
                });
            } catch (FindException fe) {
                logger.log( Level.WARNING, "Error finding policy migration records", fe);
                return Collections.emptyList().iterator();
            }
        }

        @Override
        public int size() {
            try {
                return migrationManager.findCount(user, start, end);
            } catch (FindException fe) {
                logger.warning("Error getting migration record count");
                return 0;
            }
        }

        @Override
        public IModel model(final Object migrationObject) {
            return new AbstractReadOnlyModel() {
                @Override
                public Object getObject() {
                    return migrationObject;
                }
            };
        }
    }
}
