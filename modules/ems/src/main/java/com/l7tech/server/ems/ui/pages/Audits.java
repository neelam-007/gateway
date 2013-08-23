package com.l7tech.server.ems.ui.pages;

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
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.audit.AuditRecordManager;
import com.l7tech.server.ems.ui.NavigationPage;
import com.l7tech.util.Functions;
import com.l7tech.util.TimeUnit;
import com.l7tech.identity.User;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.markup.html.form.validation.IFormValidator;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.CompoundPropertyModel;
import javax.inject.Inject;

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
public class Audits extends EsmStandardWebPage {
    
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
                final Model<String> downloadModel = new Model<String>();
                AuditDownloadPanel download = new AuditDownloadPanel( YuiDialog.getContentId(), downloadModel );
                YuiDialog dialog = new YuiDialog("audit.holder.content", "Audit Download", YuiDialog.Style.OK_CANCEL, download, new YuiDialog.OkCancelCallback(){
                    @Override
                    public void onAction(YuiDialog dialog, AjaxRequestTarget target, YuiDialog.Button button) {
                        if ( button == YuiDialog.Button.OK ) {
                            String url = downloadModel.getObject();
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

        final HiddenField<String> hidden = new HiddenField<String>("auditId", new Model<String>(""));
        hidden.setOutputMarkupId( true );

        final List<PropertyColumn<?>> columns = new ArrayList<PropertyColumn<?>>();
        columns.add(new PropertyColumn<String>(new Model<String>("id"), "id"));
        columns.add(new PropertyColumn<Date>(new StringResourceModel("audittable.column.time", this, null), "TIME", "time"));
        columns.add(new PropertyColumn<String>(new StringResourceModel("audittable.column.level", this, null), "LEVEL", "level"));
        columns.add(new PropertyColumn<String>(new StringResourceModel("audittable.column.message", this, null), "MESSAGE", "message"));

        final FeedbackPanel feedback = new FeedbackPanel("feedback");
        Date now = new Date();
        Date last7thDay = new Date(now.getTime() - TimeUnit.DAYS.toMillis(7L));

        final DataProviderOptions options = new DataProviderOptions( values[0], last7thDay, now);
        final WebMarkupContainer tableContainer = new WebMarkupContainer("audittable.container");
        tableContainer.setOutputMarkupId(true);
        CompoundPropertyModel<DataProviderOptions> formModel = new CompoundPropertyModel<DataProviderOptions>(options);
        Form auditSelectionForm = new Form<DataProviderOptions>("auditselectform", formModel);
        final YuiDateSelector startDate = new YuiDateSelector("auditstart", formModel.<Date>bind("auditstart"), null, now );
        final YuiDateSelector endDate = new YuiDateSelector("auditend", formModel.<Date>bind("auditend"), last7thDay, now );

        startDate.addInteractionWithOtherDateSelector(endDate, false, new YuiDateSelector.InteractionTasker());
        endDate.addInteractionWithOtherDateSelector(startDate, true, new YuiDateSelector.InteractionTasker());

        final AuditDataProvider provider = new AuditDataProvider( options, "time", false );
        auditSelectionForm.add( startDate );
        auditSelectionForm.add( endDate );
        auditSelectionForm.add( new DropDownChoice<String>( "audittype", null, Arrays.asList(values), new IChoiceRenderer<String>(){
            @Override
            public Object getDisplayValue( final String key ) {
                return new StringResourceModel( "audit.type."+key, Audits.this, null ).getString();
            }
            @Override
            public String getIdValue( final String value, final int index ) {
                return values[index];
            }
        } ) );
        auditSelectionForm.add( new YuiAjaxButton("audit.select.button"){
            @Override
            protected void onSubmit( final AjaxRequestTarget ajaxRequestTarget, final Form form ) {
                // Update provide settings
                provider.setOptions( options );

                // Clear the display of any details
                detailsContainer.removeAll();
                detailsContainer.add( new WebMarkupContainer("details") );

                ajaxRequestTarget.addComponent( tableContainer );
                ajaxRequestTarget.addComponent( detailsContainer );
                ajaxRequestTarget.addComponent( feedback );
            }

            @Override
            protected void onError( final AjaxRequestTarget ajaxRequestTarget, final Form form) {
                ajaxRequestTarget.addComponent( feedback );
            }
        } );
        auditSelectionForm.add( new IFormValidator(){
            @Override
            public FormComponent[] getDependentFormComponents() {
                return new FormComponent[]{ startDate.getDateTextField(), endDate.getDateTextField() };
            }
            @Override
            public void validate( final Form form ) {
                Date start = startDate.getDateTextField().getConvertedInput();
                Date end = endDate.getDateTextField().getConvertedInput();
                if ( end.before(start) ) {
                    form.error( new StringResourceModel("message.daterange", Audits.this, null).getString() );
                }
            }
        } );

        tableContainer.add( buildDataTable( provider, columns, hidden, detailsContainer ) );

        pageForm.add( downloadButton );
        pageForm.add( deleteButton );
        pageForm.add( hidden );

        add( pageForm );
        add( feedback.setOutputMarkupId(true) );
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

    @Inject
    private AuditRecordManager auditRecordManager;

    private Date startOfDay( final Date date ) {
        Calendar calendar = Calendar.getInstance();
        if ( getSession().getTimeZoneId() != null ) {
            calendar.setTimeZone( TimeZone.getTimeZone( getSession().getTimeZoneId() ) );
        }
        calendar.setTime(date);

        calendar.set(Calendar.HOUR, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar.getTime();
    }
    
    private YuiDataTable buildDataTable( final AuditDataProvider provider,
                                         final List<PropertyColumn<?>> columns,
                                         final HiddenField hidden,
                                         final WebMarkupContainer detailsContainer ) {
        return new YuiDataTable("audittable", columns, "time", false,  provider, hidden, "id", true, null ){
            @Override
            protected void onSelect( final AjaxRequestTarget ajaxRequestTarget, final String auditIdentifier ) {
                logger.finer("Processing selection callback for audit '"+auditIdentifier+"'.");
                if ( ajaxRequestTarget != null &&
                    auditIdentifier != null ) {
                    try {
                        AuditRecord record = auditRecordManager.findByPrimaryKey( Goid.parseGoid(auditIdentifier) );

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

    private class DataProviderOptions implements Serializable {
        private String audittype;
        private Date auditstart;
        private Date auditend;

        public DataProviderOptions( final String type, final Date start, final Date end ) {
            this.audittype = type;
            this.auditstart = start;
            this.auditend = end;
        }

        public Date getAuditend() {
            return auditend;
        }

        public void setAuditend(Date auditend) {
            this.auditend = auditend;
        }

        public Date getAuditstart() {
            return auditstart;
        }

        public void setAuditstart(Date auditstart) {
            this.auditstart = auditstart;
        }

        public String getAudittype() {
            return audittype;
        }

        public void setAudittype(String audittype) {
            this.audittype = audittype;
        }

        public Date getStartTime() {
            return startOfDay(auditstart);
        }

        public Date getEndTime() {
            return new Date(startOfDay(auditend).getTime() + TimeUnit.DAYS.toMillis(1L));
        }
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

    private class AuditDataProvider extends SortableDataProvider<AuditModel> {
        private final User user;
        private AuditSearchCriteria auditSearchCriteria;

        public AuditDataProvider( final DataProviderOptions options,  final String sort, final boolean asc ) {
            User user = null;
            if ( !securityManager.hasPermission( new AttemptedReadAll( EntityType.AUDIT_RECORD ) ) ) {
                user = getUser();
            }
            this.user = user;

            auditSearchCriteria = buildCriteria( options );

            setSort( sort, asc );
        }

        public void setOptions( final DataProviderOptions options ) {
            auditSearchCriteria = buildCriteria( options );
        }

        private AuditSearchCriteria buildCriteria( final DataProviderOptions options ) {
            return new AuditSearchCriteria.Builder().fromTime(options.getStartTime()).
                    toTime(options.getEndTime()).
                    recordClass(getClassForType(options.getAudittype())).
                    maxRecords(-1).
                    user(user)
                    .build();
        }

        @Override
        public Iterator<AuditModel> iterator(int first, int count) {
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
                return Collections.<AuditModel>emptyList().iterator();
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
        public IModel<AuditModel> model(final AuditModel auditObject) {
             return new AbstractReadOnlyModel<AuditModel>() {
                @Override
                public AuditModel getObject() {
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
