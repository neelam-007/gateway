package com.l7tech.server.ems.pages;

import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.HiddenField;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.ajax.AjaxRequestTarget;

import java.util.Iterator;
import java.util.List;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Date;
import java.util.Arrays;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.Serializable;

import com.l7tech.gateway.common.audit.AuditSearchCriteria;
import com.l7tech.gateway.common.audit.AuditRecord;
import com.l7tech.gateway.common.audit.AdminAuditRecord;
import com.l7tech.gateway.common.audit.SystemAuditRecord;
import com.l7tech.server.audit.AuditRecordManager;
import com.l7tech.server.ems.NavigationPage;
import com.l7tech.objectmodel.FindException;
import com.l7tech.util.Functions;
import com.l7tech.util.TimeUnit;

/**
 * 
 */
@NavigationPage(page="Audits",section="Tools",sectionIndex=100,pageUrl="Audits.html")
public class Audits extends EmsPage {
    
    //- PUBLIC    
    
    public Audits() {
        final ModalWindow modal = new ModalWindow("audit.modal");
        modal.setCssClassName(ModalWindow.CSS_CLASS_GRAY);

        final Form pageForm = new Form("form");
        final WebMarkupContainer detailsContainer = new WebMarkupContainer("audit.details");
        detailsContainer.add( new WebMarkupContainer("details") );
        detailsContainer.setOutputMarkupId(true);

        Button downloadButton = new AjaxButton("downloadAuditsButton") {
            protected void onSubmit(AjaxRequestTarget ajaxRequestTarget, Form form) {
                AuditDownloadPanel download = new AuditDownloadPanel( modal.getContentId(), modal );
                modal.setTitle( "Audit Download" );
                modal.setContent( download );
                modal.show( ajaxRequestTarget );
            }
        };

        final HiddenField hidden = new HiddenField("auditId", new Model(""));
        hidden.setOutputMarkupId( true );

        final List<PropertyColumn> columns = new ArrayList<PropertyColumn>();
        columns.add(new PropertyColumn(new Model("id"), "id"));
        columns.add(new PropertyColumn(new StringResourceModel("audittable.column.time", this, null), "TIME", "time"));
        columns.add(new PropertyColumn(new StringResourceModel("audittable.column.level", this, null), "LEVEL", "level"));
        columns.add(new PropertyColumn(new StringResourceModel("audittable.column.message", this, null), "MESSAGE", "message"));

        final Model dateStartModel = new Model(new Date(System.currentTimeMillis()- TimeUnit.DAYS.toMillis(7)));
        final Model dateEndModel = new Model(new Date());
        final Model typeModel = new Model(values[0]);
        final WebMarkupContainer tableContainer = new WebMarkupContainer("audittable.container");
        tableContainer.setOutputMarkupId(true);
        Form auditSelectionForm = new Form("auditselectform");
        auditSelectionForm.add( new YuiDateSelector("auditstart", dateStartModel ) );
        auditSelectionForm.add( new YuiDateSelector("auditend", dateEndModel ) );
        auditSelectionForm.add( new DropDownChoice( "audittype", typeModel, Arrays.asList(values), new IChoiceRenderer(){
            public Object getDisplayValue( final Object key ) {
                return new StringResourceModel( "audit.type."+key, Audits.this, null ).getString();
            }
            public String getIdValue( final Object value, final int index ) {
                return values[index];
            }
        } ) );
        auditSelectionForm.add( new AjaxButton("audit.select.button"){
            protected void onSubmit( final AjaxRequestTarget ajaxRequestTarget, final Form form ) {
                // Rebuild the data table
                tableContainer.removeAll();
                tableContainer.add( buildDataTable( (String)typeModel.getObject(), (Date)dateStartModel.getObject(), (Date)dateEndModel.getObject(), columns, hidden, detailsContainer ) );

                // Clear the display of any details
                detailsContainer.removeAll();
                detailsContainer.add( new WebMarkupContainer("details") );

                ajaxRequestTarget.addComponent(tableContainer);
                ajaxRequestTarget.addComponent(detailsContainer);
            }
        } );

        tableContainer.add( buildDataTable( (String)typeModel.getObject(), (Date)dateStartModel.getObject(), (Date)dateEndModel.getObject(), columns, hidden, detailsContainer ) );

        pageForm.add( downloadButton );
        pageForm.add( hidden );

        add( pageForm );
        add( auditSelectionForm );
        add( tableContainer );
        add( modal );
        add( detailsContainer );
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( Audits.class.getName() );

    private static final String AUDIT_TYPE_ADMIN = "admin";
    private static final String AUDIT_TYPE_SYSTEM = "system";
    private static final String[] values = new String[]{"any", AUDIT_TYPE_ADMIN, AUDIT_TYPE_SYSTEM};

    @SuppressWarnings({"UnusedDeclaration"})
    @SpringBean
    private AuditRecordManager auditRecordManager;

    private YuiDataTable buildDataTable( final String type,
                                         final Date startDate,
                                         final Date endDate,
                                         final List<PropertyColumn> columns,
                                         final HiddenField hidden,
                                         final WebMarkupContainer detailsContainer ) {
        return new YuiDataTable("audittable", columns, "TIME", false,  new AuditDataProvider(type, startDate, endDate, "TIME", false), hidden, "id", true, null ){
            @Override
            protected void onSelect( final AjaxRequestTarget ajaxRequestTarget, final String auditIdentifier ) {
                logger.info("Processing selection callback for audit '"+auditIdentifier+"'.");
                if ( ajaxRequestTarget != null &&
                    auditIdentifier != null ) {
                    try {
                        AuditRecord record = auditRecordManager.findByPrimaryKey( Long.parseLong(auditIdentifier) );

                        AuditDetailPanel details = new AuditDetailPanel( "details", record );

                        detailsContainer.removeAll();
                        detailsContainer.add(details);

                        ajaxRequestTarget.addComponent(detailsContainer);
                    } catch ( NumberFormatException nfe ) {
                        logger.log( Level.WARNING, "Could not process audit id '"+auditIdentifier+"'." );
                    } catch ( FindException fe ) {
                        logger.log( Level.WARNING, "Error accessing audit details for audit record '"+auditIdentifier+"'.", fe );
                    }
                }
            }
        };
    }

    private class AuditModel implements Serializable {
        private final AuditRecord record;

        AuditModel( final AuditRecord record ) {
            this.record = record;
        }

        public String getId() {
            return record.getId();
        }

        public Date getTime() {
            return new Date(record.getMillis());
        }

        public String getLevel() {
            return record.getStrLvl();
        }

        public String getMessage() {
            return record.getMessage();
        }
    }

    private class AuditDataProvider extends SortableDataProvider {
        private final AuditSearchCriteria auditSearchCriteria;

        public AuditDataProvider(final String type, final Date start, final Date end,  final String sort, final boolean asc) {
            auditSearchCriteria = new AuditSearchCriteria(start, end, null, null, getClassForType(type), null, -1, -1, -1);
            setSort( sort, asc );
        }

        public Iterator iterator(int first, int count) {
            try {
                AuditRecordManager.SortProperty sort = AuditRecordManager.SortProperty.valueOf(getSort().getProperty());
                Iterator<AuditRecord> iter = auditRecordManager.findPage(sort, getSort().isAscending(), first, count, auditSearchCriteria).iterator();
                return Functions.map( iter, new Functions.Unary<AuditModel, AuditRecord>(){
                    public AuditModel call(AuditRecord auditRecord) {
                        return new AuditModel( auditRecord );
                    }
                });
            } catch (FindException fe) {
                logger.log( Level.WARNING, "Error finding audit records", fe );
                return Collections.emptyList().iterator();
            }
        }

        public int size() {
            try {
                return auditRecordManager.findCount(auditSearchCriteria);
            } catch (FindException fe) {
                logger.log( Level.WARNING, "Error getting audit record count", fe );
                return 0;
            }
        }

        public IModel model(final Object auditObject) {
             return new AbstractReadOnlyModel() {
                public Object getObject() {
                    return auditObject;
                }
            };
        }

        private Class getClassForType( final String type ) {
            Class auditClass = null;

            if ( AUDIT_TYPE_ADMIN.equals(type) ) {
                auditClass = AdminAuditRecord.class;
            } else if ( AUDIT_TYPE_SYSTEM.equals(type) ) {
                auditClass = SystemAuditRecord.class;
            }

            return auditClass;
        }
    }
}
