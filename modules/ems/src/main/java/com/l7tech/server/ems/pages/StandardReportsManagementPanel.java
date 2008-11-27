package com.l7tech.server.ems.pages;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.HiddenField;
import org.apache.wicket.markup.html.WebMarkupContainer;
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
import com.l7tech.server.ems.EmsSecurityManager;
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

        Button viewButton = new YuiAjaxButton("viewReportButton") {
            @Override
            protected void onSubmit( final AjaxRequestTarget ajaxRequestTarget, final Form form ) {
                String reportIdentifier = (String)form.get("reportId").getModel().getObject();
                if ( reportIdentifier != null && reportIdentifier.length() > 0 ) {
                    ValueMap vm = new ValueMap();
                    vm.add("reportId", reportIdentifier);
                    vm.add("type", "application/pdf");
                    ResourceReference logReference = new ResourceReference("reportResource");
                    ajaxRequestTarget.appendJavascript("document.location = '" + RequestCycle.get().urlFor(logReference, vm).toString() + "';");
                }
            }
        };

        Button deleteButton = new YuiAjaxButton("deleteReportButton") {
            @Override
            protected void onSubmit( final AjaxRequestTarget ajaxRequestTarget, final Form form ) {
                String reportIdentifier = (String)form.get("reportId").getModel().getObject();
                if ( reportIdentifier != null && reportIdentifier.length() > 0 ) {
                    try {
                        StandardReport report = reportManager.findByPrimaryKey( Long.parseLong(reportIdentifier) );
                        if ( report != null ) {
                            if ( securityManager.hasPermission( new AttemptedDeleteSpecific(EntityType.ESM_STANDARD_REPORT, report) ) ) {
                                reportManager.delete( report );
                            } else {
                                logger.log( Level.WARNING, "Report deletion not permitted." );
                            }
                        } else {
                            logger.log( Level.INFO, "Report deletion request ignored for unknown report '"+reportIdentifier+"'." );
                        }
                    } catch ( DeleteException e ) {
                        logger.log( Level.WARNING, "Error deleting report.", e );
                    } catch ( FindException e ) {
                        logger.log( Level.WARNING, "Error deleting report.", e );
                    } catch ( NumberFormatException nfe ) {
                        logger.log( Level.INFO, "Report deletion request ignored for unknown report '"+reportIdentifier+"'." );
                    }

                    ajaxRequestTarget.addComponent( tableContainer );
                }
            }
        };

        HiddenField hidden = new HiddenField("reportId", new Model(""));

        pageForm.add( viewButton );
        pageForm.add( deleteButton );
        pageForm.add( hidden.setOutputMarkupId(true) );

        List<PropertyColumn> columns = new ArrayList<PropertyColumn>();
        columns.add(new PropertyColumn(new Model("id"), "id"));
        columns.add(new PropertyColumn(new StringResourceModel("reporttable.column.name", this, null), "name", "name"));
        columns.add(new PropertyColumn(new StringResourceModel("reporttable.column.date", this, null), "date", "date"));
        columns.add(new PropertyColumn(new StringResourceModel("reporttable.column.clusterName", this, null), "clusterName", "clusterName"));
        columns.add(new PropertyColumn(new StringResourceModel("reporttable.column.status", this, null), "status", "status"));

        YuiDataTable table = new YuiDataTable("reportTable", columns, "date", true,  new ReportDataProvider("date", false), hidden, "id", true, new Button[]{ viewButton, deleteButton });
        tableContainer.add( table );
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( StandardReportsManagementPanel.class.getName() );

    @SpringBean
    private StandardReportManager reportManager;

    @SpringBean
    private EmsSecurityManager securityManager;

   private static final class ReportModel implements Serializable {
        private final String id;
        private final String name;
        private final Date date;
        private final String clusterName;
        private final String status;

        ReportModel( final StandardReport report ) {
            this( report.getId(),
                  report.getName(),
                  new Date(report.getStatusTime()),
                  report.getSsgCluster().getName(),
                  report.getStatus() );
        }

        ReportModel( final String id,
                     final String name,
                     final Date date,
                     final String clusterName,
                     final String status ) {
            this.id = id;
            this.name = name;
            this.date = date;
            this.clusterName = clusterName;
            this.status = status;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public Date getDate() {
            return date;
        }

        public String getClusterName() {
            return clusterName;
        }

        public String getStatus() {
            return status;
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
