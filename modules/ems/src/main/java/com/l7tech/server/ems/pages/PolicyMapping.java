package com.l7tech.server.ems.pages;

import com.l7tech.server.ems.NavigationPage;
import com.l7tech.server.ems.enterprise.SsgClusterManager;
import com.l7tech.server.ems.enterprise.SsgCluster;
import com.l7tech.server.ems.enterprise.EnterpriseFolderManager;
import com.l7tech.server.ems.migration.MigrationManager;
import com.l7tech.server.ems.migration.Migration;
import com.l7tech.util.TimeUnit;
import com.l7tech.util.Functions;
import com.l7tech.objectmodel.FindException;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.validation.validator.DateValidator;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.spring.injection.annot.SpringBean;

import java.util.*;
import java.util.logging.Logger;
import java.io.Serializable;

/**
 *
 */
@NavigationPage(page="PolicyMapping",pageIndex=300,section="ManagePolicies",pageUrl="PolicyMapping.html")
public class PolicyMapping extends EmsPage  {
    private static final Logger logger = Logger.getLogger(PolicyMapping.class.getName());

    @SpringBean
    private MigrationManager migrationManager;
    @SpringBean
    private SsgClusterManager ssgClusterManager;
    @SpringBean
    private EnterpriseFolderManager enterpriseFolderManager;

    private WebMarkupContainer migrationTableContainer;
    private WebMarkupContainer migrationSummaryContainer;
    private WebMarkupContainer dynamicDialogHolder;
    private Model dateStartModel;
    private Model dateEndModel;


    public PolicyMapping() {
        // For demo only, create some demo policy migration record.  It will be removed later on.
        generatePolicyMigrationsForDemo();

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
        dateStartModel = new Model(new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)));
        YuiDateSelector startDate = new YuiDateSelector("migrationStartSelector", dateStartModel, now);
        startDate.getDateTextField().add(DateValidator.maximum(new Date()));

        dateEndModel = new Model(new Date());
        YuiDateSelector endDate = new YuiDateSelector("migrationEndSelector", dateEndModel, now);

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
        // Create a form to delete a migration
        Form deleteMigrationForm = new Form("deleteMigrationForm");
        HiddenField hiddenFieldForMigration = new HiddenField("migrationId", new Model(""));
        YuiAjaxButton deleteMigrationButton = new YuiAjaxButton("deleteMigrationButton") {
            protected void onSubmit(final AjaxRequestTarget ajaxRequestTarget, final Form form) {
                String warningText =
                    "<p>This will irrevocably delete the migration record and cannot be undone.</p><br/>" +
                        "<p>Really delete the migration record?</p>";
                Label label = new Label(YuiDialog.getContentId(), warningText);
                label.setEscapeModelStrings(false);
                YuiDialog dialog = new YuiDialog("dynamic.holder.content", "Confirm Migration Deletion", YuiDialog.Style.OK_CANCEL, label, new YuiDialog.OkCancelCallback() {
                    @Override
                    public void onAction( final YuiDialog dialog, final AjaxRequestTarget target, final YuiDialog.Button button) {
                        if ( button == YuiDialog.Button.OK ) {
                            final String migrationId = (String) form.get("migrationId").getModelObject();
                            try {
                                logger.info("Deleting the migration (OID = " + migrationId + ")");

                                migrationManager.delete(Long.parseLong(migrationId));
                                form.get("deleteMigrationButton").setEnabled(false);
                                migrationSummaryContainer.setVisible(false);

                                target.addComponent(migrationTableContainer);
                                target.addComponent(migrationSummaryContainer);
                            } catch (Exception e) {
                                logger.warning("Cannot delete the migration (OID = " + migrationId + ")");
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
        deleteMigrationForm.add(hiddenFieldForMigration.setOutputMarkupId(true));
        deleteMigrationForm.add(deleteMigrationButton.setEnabled(false));

        // Create a migration table
        Panel migrationTable = buildMigrationTablePanel("migrationTable", hiddenFieldForMigration, deleteMigrationButton);

        // Add the above two components into the migrationTableContainer
        if (migrationTableContainer == null) {
            migrationTableContainer = new WebMarkupContainer("migrationTableContainer");
        } else {
            migrationTableContainer.removeAll();
        }
        migrationTableContainer.add(migrationTable);
        migrationTableContainer.add(deleteMigrationForm);
        migrationTableContainer.setOutputMarkupId(true);
    }

    /**
     * Build a migration table based on given the start date and the end date.
     * @param componentId: the wicket id for the table
     * @param hidden: the hidden field to store the selected migration id.
     * @param button: the button needed to be updated after the migration is selected.
     * @return a panel that contains a migration table.
     */
    private Panel buildMigrationTablePanel(String componentId, final HiddenField hidden, final Button button) {
        List<PropertyColumn> columns = new ArrayList<PropertyColumn>();
        columns.add(new PropertyColumn(new Model("id"), "id"));
        columns.add(new PropertyColumn(new StringResourceModel("migration.column.name", this, null), "NAME", "name"));
        columns.add(new PropertyColumn(new StringResourceModel("migration.column.time", this, null), "TIME", "timeCreated"));
        columns.add(new PropertyColumn(new StringResourceModel("migration.column.from", this, null), "FROM", "sourceCluster"));
        columns.add(new PropertyColumn(new StringResourceModel("migration.column.to", this, null), "TO", "destinationCluster"));

        Date start = (Date)dateStartModel.getObject();
        Date end = (Date)dateEndModel.getObject();

        return new YuiDataTable("migrationTable", columns, "timeCreated", true, new MigrationDataProvider(start, end, "timeCreated", true), hidden, "id", true, new Button[]{button}) {
            @Override
            protected void onSelect(AjaxRequestTarget ajaxRequestTarget, String value) {
                boolean downloadButtonEnabled;

                if (value != null && value.length() > 0) {
                    String summary;
                    try {
                        Migration migration = migrationManager.findByPrimaryKey(Long.parseLong(value));
                        summary = migration.getSummary();
                    } catch (FindException e) {
                        logger.warning("Cannot find a policy migration (OID = '" + value + "'.");
                        return;
                    }

                    migrationSummaryContainer.removeAll();
                    migrationSummaryContainer.add(new TextArea("migrationSummaryTextarea", new Model(summary)));
                    migrationSummaryContainer.setVisible(true);
                    ajaxRequestTarget.addComponent(migrationSummaryContainer);

                    downloadButtonEnabled = true;
                } else {
                    downloadButtonEnabled = false;
                }
                button.setEnabled(downloadButtonEnabled);
                ajaxRequestTarget.addComponent(button);
            }
        };
    }

    /**
     * Initialize the migration-summary container, which contains a text area displaying the migration summary.
     */
    private void initMigrationSummaryContainer() {
        migrationSummaryContainer = new WebMarkupContainer("migrationSummaryContainer");
        migrationSummaryContainer.add(new TextArea("migrationSummaryTextarea", new Model("")));
        migrationSummaryContainer.setOutputMarkupPlaceholderTag(true);
        migrationSummaryContainer.setVisible(false);
    }

    /**
     * Migration Model, which has the following attributes: id, name, time created, source cluster, and destination cluster.
     */
    private final class MigrationModel implements Serializable {
        private final Migration migration;

        MigrationModel(Migration migration) {
            this.migration = migration;
        }

        public String getId() {
            return migration.getId();
        }

        public Date getTimeCreated() {
            return new Date(migration.getTimeCreated());
        }

        public SsgCluster getSourceCluster() {
            return migration.getSourceCluster();
        }

        public SsgCluster getDestinationCluster() {
            return migration.getDestinationCluster();
        }

        public String getName() {
            return migration.getName();
        }
    }

    /**
     * Migration Data Provider used by the migration table.
     */
    private final class MigrationDataProvider extends SortableDataProvider {
        private Date start;
        private Date end;

        public MigrationDataProvider(final Date start, final Date end, String sort, boolean asc) {
            this.start = start;
            this.end = end;
            setSort(sort, asc);
        }

        @Override
        public Iterator iterator(int first, int count) {
            try {
                MigrationManager.SortProperty sort = MigrationManager.SortProperty.valueOf(getSort().getProperty());
                checkStartEndDays();
                Iterator<Migration> itr = migrationManager.findPage(sort, getSort().isAscending(), first, count, start, end).iterator();

                return Functions.map(itr, new Functions.Unary<MigrationModel, Migration>() {
                    @Override
                    public MigrationModel call(Migration migration) {
                        return new MigrationModel(migration);
                    }
                });
            } catch (FindException fe) {
                logger.warning("Error finding policy migration records");
                return Collections.emptyList().iterator();
            }
        }

        @Override
        public int size() {
            try {
                return migrationManager.findCount(start, end);
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

        /**
         * Check if they are the same day.  If so, then make the "start" day start 00 hour, 00 minute, and 00 second.
         * Make "end" day start 23 hour, 59 minute, and 59 second. 
         */
        private void checkStartEndDays() {
            Calendar calendar = Calendar.getInstance();
            if (start.compareTo(end) == 0) {
                calendar.setTime(start);
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                start = calendar.getTime();

                calendar.setTime(end);
                calendar.set(Calendar.HOUR_OF_DAY, 23);
                calendar.set(Calendar.MINUTE, 59);
                calendar.set(Calendar.SECOND, 59);
                end = calendar.getTime();
            }
        }
    }

    /**
     * For demo only, recreate two demo clusters and a few demo migrations.
     */
    private void generatePolicyMigrationsForDemo() {
        try {
            for (Migration m: migrationManager.findAll()) {
                migrationManager.delete(m);
            }

            for (SsgCluster c: ssgClusterManager.findAll()) {
                if (c.getName().equals("source cluster") || c.getName().equals("destination cluster")) {
                    ssgClusterManager.delete(c);
                }
            }
            SsgCluster source = ssgClusterManager.create("source cluster", "source.l7tech.com", 9443, enterpriseFolderManager.findRootFolder());
            SsgCluster dest = ssgClusterManager.create("destination cluster", "dest.l7tech.com", 8443, enterpriseFolderManager.findRootFolder());

            Date[] dates = new Date[] {
                makeDate(1996, 11, 4, 7, 22, 0),
                makeDate(1998, 0, 4, 7, 22, 0),
                makeDate(1999, 9, 22, 7, 22, 0),
                makeDate(2001, 8, 2, 7, 22, 0),
                makeDate(2005, 7, 24, 7, 22, 0),
                makeDate(2006, 1, 7, 7, 22, 0),
                makeDate(2007, 2, 1, 7, 22, 0),
                makeDate(2007, 4, 29, 7, 22, 0),
                makeDate(2007, 8, 4, 7, 22, 0),
                makeDate(2008, 10, 13, 0, 0, 0),
                makeDate(2008, 10, 19, 7, 22, 0),
                makeDate(2008, 10, 19, 8, 22, 0),
                makeDate(2008, 10, 19, 9, 22, 0),
                makeDate(2008, 10, 19, 10, 22, 0),
                makeDate(2008, 10, 19, 11, 22, 0),
                makeDate(2008, 10, 21, 7, 22, 0),
                makeDate(2008, 11, 25, 7, 22, 0),
                makeDate(2009, 0, 1, 7, 22, 0)
            };

            for (int i = 0; i < dates.length; i++) {
                String summary =
                    "Source cluster     : " + source.getName() + "\n" +
                        "Destination cluster: " + dest.getName() + "\n" +
                        "Date created       : " + dates[i].toString();
                Migration m = new Migration("SourceCluser -> DestCluster " + i, dates[i].getTime(), source, dest, summary);
                migrationManager.save(m);
            }
        } catch (Exception e) {
            logger.warning("### Exception from saving migration ###");
        }
    }

    private static Date makeDate(int year, int month, int day, int hour, int minute, int second) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, day, hour, minute, second);
        return calendar.getTime();
    }
}