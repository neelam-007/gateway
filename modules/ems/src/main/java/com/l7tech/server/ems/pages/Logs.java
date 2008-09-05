package com.l7tech.server.ems.pages;

import com.l7tech.server.ems.NavigationPage;
import com.l7tech.server.ems.LogResource;

import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.io.File;
import java.io.Serializable;

import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.HiddenField;
import org.apache.wicket.request.target.basic.RedirectRequestTarget;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.util.value.ValueMap;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.ajax.AjaxRequestTarget;

/**
 * Log management
 */
@NavigationPage(page="Logs",section="Tools",sectionIndex=100,pageUrl="Logs.html")
public class Logs extends EmsPage {

    public Logs() {
        final ModalWindow modal = new ModalWindow("log.modal");
        modal.setCssClassName(ModalWindow.CSS_CLASS_GRAY);

        final Form pageForm = new Form("form");
        add ( pageForm );

        Button viewButton = new AjaxButton("viewLogButton") {
            protected void onSubmit(AjaxRequestTarget ajaxRequestTarget, Form form) {
                String logIdentifier = (String)form.get("logId").getModel().getObject();
                if ( logIdentifier != null && logIdentifier.length() > 0 ) {
                    File file = getLogFile(logIdentifier);
                    if ( file != null ) {
                        LogDetailPanel details = new LogDetailPanel(modal.getContentId(), file, modal);

                        modal.setTitle( new StringResourceModel("page.Logs.label", this, null).getString() + " : " + logIdentifier);
                        modal.setContent(details);
                        modal.show(ajaxRequestTarget);
                    }
                }
            }
        };

        Button downloadButton = new AjaxButton("downloadLogButton") {
            protected void onSubmit(AjaxRequestTarget ajaxRequestTarget, Form form) {
                String logIdentifier = (String)form.get("logId").getModel().getObject();
                if ( logIdentifier != null && logIdentifier.length() > 0 ) {
                    ValueMap vm = new ValueMap();
                    vm.add("id", logIdentifier);
                    vm.add("disposition", "attachment");
                    ResourceReference logReference = new ResourceReference("logResource");
                    RequestCycle.get().setRequestTarget(new RedirectRequestTarget(RequestCycle.get().urlFor(logReference, vm).toString()));
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

        YuiDataTable table = new YuiDataTable("logtable", columns, "name", true,  new LogDataProvider(), hidden, "name", new Button[]{ viewButton, downloadButton });
        pageForm.add( table );

        final WebMarkupContainer detailsContainer = new WebMarkupContainer("log.details");
        detailsContainer.add( new WebMarkupContainer("details") );

        add(modal);
        add(detailsContainer.setOutputMarkupId(true));
    }

    private static File getLogFile( String name ) {
        return LogResource.getLogFileIfValid( name );
    }

    private static class LogDataProvider extends SortableDataProvider {
        final List<FileModel> files = listFiles();

        public LogDataProvider() {
            setSort("name", true);
        }

        public Iterator iterator(int first, int count) {
            return newLogIter( files, first, first+count, getSort().getProperty(), getSort().isAscending() );
        }

        public int size() {
            return files.size();
        }

        public IModel model(final Object auditObject) {
             return new AbstractReadOnlyModel() {
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
            return Long.toString(size);
        }

        public Date getDate() {
            return date;
        }
    }
}
