package com.l7tech.server.ems.ui.pages;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.HiddenField;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.util.value.ValueMap;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import com.l7tech.server.ems.standardreports.StandardReportManager;
import com.l7tech.server.ems.standardreports.StandardReport;
import com.l7tech.server.ems.EsmSecurityManager;
import com.l7tech.identity.User;
import com.l7tech.gateway.common.security.rbac.AttemptedReadAll;
import com.l7tech.gateway.common.security.rbac.AttemptedDeleteSpecific;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.util.Functions;

import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collections;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class StandardReportsManagementPanel extends Panel {

    //- PUBLIC

    public StandardReportsManagementPanel(String id) {
        super(id);

        final WebMarkupContainer tableContainer = new WebMarkupContainer("tableContainer");
        add( tableContainer.setOutputMarkupId(true) );

        final WebMarkupContainer container = new WebMarkupContainer("refresh");
        container.add( new AjaxEventBehavior("onclick"){
            @Override
            protected void onEvent( final AjaxRequestTarget target ) {
                target.addComponent( tableContainer );
            }
        } );
        add( container );

        final Form pageForm = new Form("form");
        add ( pageForm );

        final Button viewButton = new YuiAjaxButton("viewReportButton") {
            @Override
            protected void onSubmit( final AjaxRequestTarget ajaxRequestTarget, final Form form ) {
                String reportIdentifier = (String)form.get("reportId").getModel().getObject();
                if ( reportIdentifier != null && reportIdentifier.length() > 0 ) {
                    ValueMap vm = new ValueMap();
                    vm.add("reportId", reportIdentifier);
                    vm.add("type", "application/pdf");
                    ResourceReference logReference = new ResourceReference("reportResource");
                    ajaxRequestTarget.appendJavascript("window.open('" + RequestCycle.get().urlFor(logReference, vm).toString() + "', '_blank');");
                }
            }
        };

        final Button downloadButton = new YuiAjaxButton("downloadReportButton") {
            @Override
            protected void onSubmit( final AjaxRequestTarget ajaxRequestTarget, final Form form ) {
                String reportIdentifier = (String)form.get("reportId").getModel().getObject();
                if ( reportIdentifier != null && reportIdentifier.length() > 0 ) {
                    ValueMap vm = new ValueMap();
                    vm.add("reportId", reportIdentifier);
                    vm.add("type", "application/pdf");
                    vm.add("disposition", "attachment");
                    ResourceReference logReference = new ResourceReference("reportResource");
                    ajaxRequestTarget.appendJavascript("window.location = '" + RequestCycle.get().urlFor(logReference, vm).toString() + "';");
                }
            }
        };

        Button deleteButton = new YuiAjaxButton("deleteReportButton") {
            @Override
            protected void onSubmit( final AjaxRequestTarget ajaxRequestTarget, final Form form ) {
                String reportIdentifier = (String)form.get("reportId").getModel().getObject();
                if ( reportIdentifier != null && reportIdentifier.length() > 0 ) {
                    try {
                        final StandardReport report = reportManager.findByPrimaryKey( Long.parseLong(reportIdentifier) );
                        if ( report != null ) {

                            // Pop up a warning dialog to let the user confirm the report deletion.
                            String warningText = "Really delete report \"" + report.getName() + "\"?";
                            Label label = new Label(YuiDialog.getContentId(), warningText);
                            label.setEscapeModelStrings(false);
                            YuiDialog dialog = new YuiDialog("dynamic.holder.content", "Confirm Generated Report Deletion", YuiDialog.Style.OK_CANCEL, label, new YuiDialog.OkCancelCallback() {
                                @Override
                                public void onAction( final YuiDialog dialog, AjaxRequestTarget target, YuiDialog.Button button) {
                                    if ( button == YuiDialog.Button.OK ) {
                                        if ( securityManager.hasPermission( new AttemptedDeleteSpecific(EntityType.ESM_STANDARD_REPORT, report) ) ) {
                                            try {
                                                reportManager.delete( report );
                                                target.addComponent( tableContainer );
                                            } catch (DeleteException e) {
                                                logger.log( Level.WARNING, "Error deleting report.", e );
                                            }
                                        } else {
                                            logger.log( Level.WARNING, "Report deletion not permitted." );
                                        }
                                    }
                                    dynamicDialogHolder.replace(new EmptyPanel("dynamic.holder.content"));
                                    target.addComponent(dynamicDialogHolder);
                                }
                            });

                            dynamicDialogHolder.replace(dialog);
                            ajaxRequestTarget.addComponent(dynamicDialogHolder);
                        } else {
                            logger.log( Level.FINE, "Report deletion request ignored for unknown report '"+reportIdentifier+"'." );
                        }
                    } catch ( FindException e ) {
                        logger.log( Level.WARNING, "Error deleting report.", e );
                    } catch ( NumberFormatException nfe ) {
                        logger.log( Level.INFO, "Report deletion request ignored for unknown report '"+reportIdentifier+"'." );
                    }
                }
            }
        };

        HiddenField hidden = new HiddenField("reportId", new Model(""));

        viewButton.setEnabled(false);
        downloadButton.setEnabled(false);

        pageForm.add( viewButton.setOutputMarkupId(true) );
        pageForm.add( downloadButton.setOutputMarkupId(true) );
        pageForm.add( deleteButton );
        pageForm.add( hidden.setOutputMarkupId(true) );

        List<PropertyColumn> columns = new ArrayList<PropertyColumn>();
        columns.add(new PropertyColumn(new Model("id"), "id"));
        columns.add(new PropertyColumn(new StringResourceModel("reporttable.column.name", this, null), "name", "name"));
        columns.add(new PropertyColumn(new StringResourceModel("reporttable.column.date", this, null), "statusTime", "statusTime"));
        columns.add(new PropertyColumn(new StringResourceModel("reporttable.column.clusterName", this, null), "clusterName", "clusterName"));
        columns.add(new PropertyColumn(new StringResourceModel("reporttable.column.status", this, null), "status", "status"));
        columns.add(new PropertyColumn(new StringResourceModel("reporttable.column.statusMessage", this, null), "statusMessage", "statusMessage"));

        YuiDataTable table = new YuiDataTable("reportTable", columns, "statusTime", false,  new ReportDataProvider("statusTime", false), hidden, "id", true, new Button[]{ deleteButton, viewButton, downloadButton }){
            @Override
            @SuppressWarnings({"UnusedDeclaration"})
            protected void onSelect(final AjaxRequestTarget ajaxRequestTarget, final String value) {
                boolean enable = false;
                try {
                    StandardReport report = reportManager.findByPrimaryKey( Long.parseLong(value) );
                    if ( "COMPLETED".equals(report.getStatus()) ) {
                        enable = true;
                    }
                } catch (FindException e) {
                    // disable buttons
                } catch (NumberFormatException e) {
                    // disable buttons
                }

                viewButton.setEnabled(enable);
                downloadButton.setEnabled(enable);

                ajaxRequestTarget.addComponent(downloadButton);
                ajaxRequestTarget.addComponent(viewButton);
            }
        };
        tableContainer.add( table );

        // Create a dynamic holder just in case for displaying a warning dialog.
        initDynamicDialogHolder();
        add(dynamicDialogHolder);
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( StandardReportsManagementPanel.class.getName() );

    @SpringBean
    private StandardReportManager reportManager;

    @SpringBean
    private EsmSecurityManager securityManager;

    private WebMarkupContainer dynamicDialogHolder;

    /**
     * Initialize the dynamic dialog holder/container.
     */
    private void initDynamicDialogHolder() {
        dynamicDialogHolder = new WebMarkupContainer("dynamic.holder");
        dynamicDialogHolder.add(new EmptyPanel("dynamic.holder.content"));
        dynamicDialogHolder.setOutputMarkupId(true);
    }

   private static final class ReportModel implements Serializable {
        private final String id;
        private final String name;
        private final Date date;
        private final String clusterName;
        private final String status;
        private final String statusMessage;

        ReportModel( final StandardReport report ) {
            this( report.getId(),
                  report.getName(),
                  new Date(report.getStatusTime()),
                  report.getSsgCluster().getName(),
                  report.getStatus(),
                  report.getStatusMessage() );
        }

        ReportModel( final String id,
                     final String name,
                     final Date date,
                     final String clusterName,
                     final String status,
                     final String statusMessage ) {
            this.id = id;
            this.name = name;
            this.date = date;
            this.clusterName = clusterName;
            this.status = status;
            this.statusMessage = statusMessage == null ? "" : statusMessage;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public Date getStatusTime() {
            return date;
        }

        public String getClusterName() {
            return clusterName;
        }

        public String getStatus() {
            return status;
        }

       public String getStatusMessage() {
           return statusMessage;
       }
    }

    private class ReportDataProvider extends SortableDataProvider {
        private final User user;

        public ReportDataProvider( final String sort, final boolean asc ) {
            User user = null;

            if ( !securityManager.hasPermission( new AttemptedReadAll( EntityType.ESM_STANDARD_REPORT ) ) ) {
                user = securityManager.getLoginInfo( ((WebRequest)RequestCycle.get().getRequest()).getHttpServletRequest().getSession(true) ).getUser();
            }
            this.user = user;

            setSort( sort, asc );
        }

        @Override
        public Iterator iterator( final int first, final int count ) {
            try {
                Iterator<StandardReport> iter = reportManager.findPage(user, getSort().getProperty(), getSort().isAscending(), first, count).iterator();
                return Functions.map( iter, new Functions.Unary<ReportModel, StandardReport>(){
                    @Override
                    public ReportModel call(final StandardReport standardReport) {
                        return new ReportModel( standardReport );
                    }
                });
            } catch (FindException fe) {
                logger.log( Level.WARNING, "Error finding audit records", fe );
                return Collections.emptyList().iterator();
            }
        }

        @Override
        public int size() {
            try {
                return reportManager.findCount(user);
            } catch (FindException fe) {
                logger.log( Level.WARNING, "Error getting audit record count", fe );
                return 0;
            }
        }

        @Override
        public IModel model(final Object reportObject) {
             return new AbstractReadOnlyModel() {
                @Override
                public Object getObject() {
                    return reportObject;
                }
            };
        }
    }
}
