package com.l7tech.server.ems.pages;

import com.l7tech.gateway.common.audit.AdminAuditRecord;
import com.l7tech.gateway.common.audit.AuditRecord;
import com.l7tech.gateway.common.audit.AuditSearchCriteria;
import com.l7tech.gateway.common.audit.SystemAuditRecord;
import com.l7tech.gateway.common.security.rbac.AttemptedReadAll;
import com.l7tech.gateway.common.security.rbac.AttemptedDeleteAll;
import com.l7tech.gateway.common.security.rbac.RequiredPermissionSet;
import com.l7tech.gateway.common.security.rbac.RequiredPermission;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.server.audit.AuditRecordManager;
import com.l7tech.server.ems.NavigationPage;
import com.l7tech.server.ems.EmsSecurityManager;
import com.l7tech.util.Functions;
import com.l7tech.util.TimeUnit;
import com.l7tech.identity.User;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.validation.validator.DateValidator;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.protocol.http.WebRequest;

import java.io.Serializable;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 */
@RequiredPermissionSet(
    requiredPermissions=@RequiredPermission(entityType=EntityType.AUDIT_RECORD, operationType= OperationType.READ)
)
@NavigationPage(page="Audits",pageIndex=100,section="Tools",sectionIndex=100,sectionPage="Audits",pageUrl="Audits.html")
public class Audits extends EmsPage {
    
    //- PUBLIC    
    
    public Audits() {
        final WebMarkupContainer auditHolder = new WebMarkupContainer("audit.holder");
        auditHolder.add( new EmptyPanel("audit.holder.content") );
        auditHolder.setOutputMarkupId(true);

        final Form pageForm = new Form("form");
        final WebMarkupContainer detailsContainer = new WebMarkupContainer("audit.details");
        detailsContainer.add( new WebMarkupContainer("details") );
        detailsContainer.setOutputMarkupId(true);

        Button downloadButton = new YuiAjaxButton("downloadAuditsButton") {
            @Override
            protected void onSubmit(AjaxRequestTarget ajaxRequestTarget, Form form) {
                final Model downloadModel = new Model();
                AuditDownloadPanel download = new AuditDownloadPanel( YuiDialog.getContentId(), downloadModel );
                YuiDialog dialog = new YuiDialog("audit.holder.content", "Audit Download", YuiDialog.Style.OK_CANCEL, download, new YuiDialog.OkCancelCallback(){
                    @Override
                    public void onAction(YuiDialog dialog, AjaxRequestTarget target, YuiDialog.Button button) {
                        if ( button == YuiDialog.Button.OK ) {
                            String url = (String)downloadModel.getObject();
                            target.appendJavascript( "window.setTimeout(function() { window.location = '" + url + "'; }, 0)" );
                        }
                    }
                });
                auditHolder.replace(dialog);
                ajaxRequestTarget.addComponent(auditHolder);
            }
        }.add( new AttemptedReadAll(EntityType.AUDIT_RECORD) );

        Button deleteButton = new YuiAjaxButton("deleteAuditsButton") {
            @Override
            protected void onSubmit(AjaxRequestTarget ajaxRequestTarget, Form form) {
                AuditDeletePanel delete = new AuditDeletePanel( YuiDialog.getContentId() );
                YuiDialog dialog = new YuiDialog("audit.holder.content", "Audit Deletion", YuiDialog.Style.OK_CANCEL, delete);
                auditHolder.replace(dialog);
                ajaxRequestTarget.addComponent(auditHolder);
            }
        }.add( new AttemptedDeleteAll(EntityType.AUDIT_RECORD) );

        final HiddenField hidden = new HiddenField("auditId", new Model(""));
        hidden.setOutputMarkupId( true );

        final List<PropertyColumn> columns = new ArrayList<PropertyColumn>();
        columns.add(new PropertyColumn(new Model("id"), "id"));
        columns.add(new PropertyColumn(new StringResourceModel("audittable.column.time", this, null), "TIME", "time"));
        columns.add(new PropertyColumn(new StringResourceModel("audittable.column.level", this, null), "LEVEL", "level"));
        columns.add(new PropertyColumn(new StringResourceModel("audittable.column.message", this, null), "MESSAGE", "message"));

        final Model dateStartModel = new Model(new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)));
        final Model dateEndModel = new Model(new Date());
        final Model typeModel = new Model(values[0]);
        final WebMarkupContainer tableContainer = new WebMarkupContainer("audittable.container");
        tableContainer.setOutputMarkupId(true);
        Form auditSelectionForm = new Form("auditselectform");
        YuiDateSelector startDate = new YuiDateSelector("auditstart", dateStartModel );
        YuiDateSelector endDate = new YuiDateSelector("auditend", dateEndModel );
        startDate.getDateTextField().add(DateValidator.maximum(new Date()));
        auditSelectionForm.add( startDate );
        auditSelectionForm.add( endDate );
        auditSelectionForm.add( new DropDownChoice( "audittype", typeModel, Arrays.asList(values), new IChoiceRenderer(){
            @Override
            public Object getDisplayValue( final Object key ) {
                return new StringResourceModel( "audit.type."+key, Audits.this, null ).getString();
            }
            @Override
            public String getIdValue( final Object value, final int index ) {
                return values[index];
            }
        } ) );
        auditSelectionForm.add( new YuiAjaxButton("audit.select.button"){
            @Override
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
        pageForm.add( deleteButton );
        pageForm.add( hidden );

        add( pageForm );
        add( auditSelectionForm );
        add( tableContainer );
        add( detailsContainer );
        add( auditHolder );
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( Audits.class.getName() );

    private static final String AUDIT_TYPE_ADMIN = "admin";
    private static final String AUDIT_TYPE_SYSTEM = "system";
    private static final String[] values = new String[]{"any", AUDIT_TYPE_ADMIN, AUDIT_TYPE_SYSTEM};

    @SpringBean
    private AuditRecordManager auditRecordManager;

    @SpringBean
    private EmsSecurityManager securityManager;

    private Date startOfDay( final Date date ) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        calendar.set(Calendar.HOUR, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar.getTime();
    }
    
    private YuiDataTable buildDataTable( final String type,
                                         final Date startDate,
                                         final Date endDate,
                                         final List<PropertyColumn> columns,
                                         final HiddenField hidden,
                                         final WebMarkupContainer detailsContainer ) {
        Date start = startOfDay(startDate);
        Date end = new Date(startOfDay(endDate).getTime() + TimeUnit.DAYS.toMillis(1));

        return new YuiDataTable("audittable", columns, "time", false,  new AuditDataProvider(type, start, end, "time", false), hidden, "id", true, null ){
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
            User user = null;
            if ( !securityManager.hasPermission( new AttemptedReadAll( EntityType.AUDIT_RECORD ) ) ) {
                user = securityManager.getLoginInfo( ((WebRequest)RequestCycle.get().getRequest()).getHttpServletRequest().getSession(true) ).getUser();
            }

            auditSearchCriteria = new AuditSearchCriteria.Builder().fromTime(start).
                    toTime(end).
                    recordClass(getClassForType(type)).
                    startMessageNumber(-1).
                    endMessageNumber(-1).
                    maxRecords(-1).
                    user(user)
                    .build();

            setSort( sort, asc );
        }

        @Override
        public Iterator iterator(int first, int count) {
            try {
                AuditRecordManager.SortProperty sort = AuditRecordManager.SortProperty.valueOf(getSort().getProperty());
                Iterator<AuditRecord> iter = auditRecordManager.findPage(sort, getSort().isAscending(), first, count, auditSearchCriteria).iterator();
                return Functions.map( iter, new Functions.Unary<AuditModel, AuditRecord>(){
                    @Override
                    public AuditModel call(AuditRecord auditRecord) {
                        return new AuditModel( auditRecord );
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
                return auditRecordManager.findCount(auditSearchCriteria);
            } catch (FindException fe) {
                logger.log( Level.WARNING, "Error getting audit record count", fe );
                return 0;
            }
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
