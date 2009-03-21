package com.l7tech.server.ems.ui.pages;

import com.l7tech.server.ems.ui.LogResource;
import com.l7tech.server.ems.ui.NavigationPage;
import com.l7tech.gateway.common.security.rbac.AttemptedReadAny;
import com.l7tech.gateway.common.security.rbac.RequiredPermissionSet;
import com.l7tech.gateway.common.security.rbac.RequiredPermission;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.util.SizeUnit;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.HiddenField;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.value.ValueMap;

import java.io.File;
import java.io.Serializable;
import java.util.*;

/**
 * Log management
 */
@RequiredPermissionSet(
    requiredPermissions=@RequiredPermission(entityType=EntityType.LOG_RECORD, operationType= OperationType.READ)
)
@NavigationPage(page="Logs",pageIndex=200,section="Tools",sectionIndex=100,sectionPage="Audits",pageUrl="Logs.html")
public class Logs extends EsmStandardWebPage {

    public Logs() {
        WebMarkupContainer secured = new SecureWebMarkupContainer( "secured", new AttemptedReadAny(EntityType.LOG_RECORD) );
        
        final Form pageForm = new Form("form");
        secured.add ( pageForm );

        final WebMarkupContainer detailsContainer = new WebMarkupContainer("log.details");
        Button viewButton = new YuiAjaxButton("viewLogButton") {
            @Override
            protected void onSubmit(AjaxRequestTarget ajaxRequestTarget, Form form) {
                String logIdentifier = (String)form.get("logId").getModel().getObject();
                if ( logIdentifier != null && logIdentifier.length() > 0 ) {
                    File file = getLogFile(logIdentifier);
                    if ( file != null ) {
                        detailsContainer.removeAll();
                        detailsContainer.add( new LogDetailPanel("details", file) );
                        ajaxRequestTarget.addComponent(detailsContainer);
                    }
                }
            }
        };

        Button downloadButton = new YuiAjaxButton("downloadLogButton") {
            @Override
            protected void onSubmit(AjaxRequestTarget ajaxRequestTarget, Form form) {
                String logIdentifier = (String)form.get("logId").getModel().getObject();
                if ( logIdentifier != null && logIdentifier.length() > 0 ) {
                    ValueMap vm = new ValueMap();
                    vm.add("id", logIdentifier);
                    vm.add("disposition", "attachment");
                    ResourceReference logReference = new ResourceReference("logResource");
                    ajaxRequestTarget.appendJavascript("window.location = '" + RequestCycle.get().urlFor(logReference, vm).toString() + "';");
                }
            }
        };

        HiddenField hidden = new HiddenField("logId", new Model(""));

        pageForm.add( viewButton );
        pageForm.add( downloadButton );
        pageForm.add( hidden.setOutputMarkupId(true) );

        List<PropertyColumn> columns = new ArrayList<PropertyColumn>();
        columns.add(new PropertyColumn(new StringResourceModel("logtable.column.name", this, null), "name", "name"));
        columns.add(new PropertyColumn(new StringResourceModel("logtable.column.date", this, null), "date", "date"));
        columns.add(new PropertyColumn(new StringResourceModel("logtable.column.size", this, null), "size", "size"));

        YuiDataTable table = new YuiDataTable("logtable", columns, "name", true,  new LogDataProvider(), hidden, "name", false, new Button[]{ viewButton, downloadButton });
        pageForm.add( table );

        detailsContainer.add( new WebMarkupContainer("details") );

        secured.add(detailsContainer.setOutputMarkupId(true));

        add(secured);
    }

    private static File getLogFile( String name ) {
        return LogResource.getLogFileIfValid( name );
    }

    private static class LogDataProvider extends SortableDataProvider {
        final List<FileModel> files = listFiles();

        public LogDataProvider() {
            setSort("name", true);
        }

        @Override
        public Iterator iterator(int first, int count) {
            return newLogIter( files, first, first+count, getSort().getProperty(), getSort().isAscending() );
        }

        @Override
        public int size() {
            return files.size();
        }

        @Override
        public IModel model(final Object auditObject) {
             return new AbstractReadOnlyModel() {
                @Override
                public Object getObject() {
                    return auditObject;
                }
            };
        }

        @Override
        public void detach() {
        }

        private List<FileModel> listFiles() {
            List<FileModel> files = new ArrayList<FileModel>();

            for ( File file : LogResource.listLogFiles() ) {
                files.add( new FileModel(file) );                
            }

            return files;
        }

        private Iterator<FileModel> newLogIter( List<FileModel> files, int start, int end, final String sortBy, final boolean asc) {
            List<FileModel> list = new ArrayList<FileModel>( files );

            Collections.sort(list, new Comparator<FileModel>(){
                @Override
                @SuppressWarnings({"unchecked"})
                public int compare(FileModel file1, FileModel file2) {
                    Comparable v1 = file1.name.toLowerCase();
                    Comparable v2 = file2.name.toLowerCase();

                    if ( "date".equals(sortBy) ) {
                        v1 = file1.date;
                        v2 = file2.date;
                    } else if ( "size".equals(sortBy) ) {
                        v1 = file1.size;
                        v2 = file2.size;
                    }

                    return asc ?
                        v1.compareTo(v2) :
                        v2.compareTo(v1) ;
                }

            });

            if ( start < 0 ) start = 0;
            else if ( start > list.size() ) start = list.size();

            if ( end < start ) end = start;
            else if ( end > list.size() ) end = list.size();

            return list.subList(start, end).iterator();
        }
    }

    private static final class FileModel implements Serializable {
        private final String name;
        private final Date date;
        private final long size;

        FileModel( final File file ) {
            this( file.getName(), new Date(file.lastModified()), file.length() );
        }

        FileModel( final String name, final Date date, final long size ) {
            this.name = name;
            this.date = date;
            this.size = size;
        }

        public String getName() {
            return name;
        }

        public String getSize() {
            return SizeUnit.format(size);
        }

        public Date getDate() {
            return date;
        }
    }
}
