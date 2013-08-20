package com.l7tech.server.ems.ui.pages;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import static com.l7tech.gateway.common.Component.fromId;
import com.l7tech.gateway.common.audit.AuditRecord;
import com.l7tech.gateway.common.audit.AdminAuditRecord;
import com.l7tech.gateway.common.audit.SystemAuditRecord;
import com.l7tech.gateway.common.audit.AuditDetail;
import com.l7tech.gateway.common.audit.AuditDetailMessage;
import com.l7tech.server.ems.ui.EsmSession;
import com.l7tech.server.ems.util.MessagesUtil;
import com.l7tech.util.Pair;

import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Collections;
import java.util.Comparator;
import java.util.Collection;
import java.util.logging.Level;
import java.io.Serializable;
import java.text.MessageFormat;

/**
 * Panel to display audit details.
 */
public class AuditDetailPanel extends Panel {

    //- PUBLIC

    /**
     *
     */
    public AuditDetailPanel( final String identifier, final AuditRecord record ) {
        super( identifier, new Model<AuditRecord>(record) );

        RepeatingView summary = new RepeatingView("auditsummary");
        int count=0;
        for ( Pair<String,String> data : buildLogDetails(record) ) {
            WebMarkupContainer container = new WebMarkupContainer( Integer.toString(count++) );
            if ( data.left.equals("") ) {
                container.add(new Label("auditsummary.name", "&nbsp;").setEscapeModelStrings(false));
                container.add(new Label("auditsummary.value", data.right));
            } else {
                container.add(new Label("auditsummary.name", data.left + ":"));
                container.add(new Label("auditsummary.value", data.right));
            }
            summary.add(container);
        }

        List<PropertyColumn<?>> columns = new ArrayList<PropertyColumn<?>>();
        columns.add(new PropertyColumn<Date>(new StringResourceModel("auditdetailtable.column.date", this, null), "DATE", "date"));
        columns.add(new PropertyColumn<Level>(new StringResourceModel("auditdetailtable.column.level", this, null), "LEVEL", "level"));
        columns.add(new PropertyColumn<String>(new StringResourceModel("auditdetailtable.column.message", this, null), "MESSAGE", "message"));
        YuiDataTable table = new YuiDataTable("auditdetailtable", columns, "date", false,  new AuditDetailDataProvider(record, "date", false));
        table.setVisible(!record.getDetails().isEmpty());
        
        add( summary );
        add( table );
    }

    /**
     *
     */
    public Collection<Pair<String,String>> buildLogDetails( final AuditRecord record ) {
        EsmSession session = (EsmSession) getSession();

        Collection<Pair<String,String>> details = new ArrayList<Pair<String,String>>();

        nonull(details, "Time", session.buildDateFormat().format( new Date(record.getMillis()) ));
        nonull(details, "Severity", record.getLevel());
        nonule(details, "Request Id", record.getReqId());
        nonull(details, "Message", record.getMessage());

        details.add( new Pair<String,String>("","") );

        if (record instanceof AdminAuditRecord) {
            AdminAuditRecord aarec = (AdminAuditRecord)record;
            add(details, "Event Type", "Manager Action");
            add(details, "Admin user", aarec.getUserName());
            add(details, "Admin IP", record.getIpAddress());
            add(details, "Action", fixAction(aarec.getAction()));
            if (AdminAuditRecord.ACTION_LOGIN!=aarec.getAction() &&
                AdminAuditRecord.ACTION_OTHER!=aarec.getAction()) {
                add(details, "Entity name", record.getName());
                add(details, "Entity id", aarec.getEntityGoid());
                add(details, "Entity type", fixType(aarec.getEntityClassname()));
            }
        } else if (record instanceof SystemAuditRecord) {
            SystemAuditRecord sys = (SystemAuditRecord)record;
            com.l7tech.gateway.common.Component component = fromId(sys.getComponentId());
            boolean isClient = component != null && component.isClientComponent();
            add(details, "Event Type", "System Message");
            if(isClient) {
                add(details, "Client IP", record.getIpAddress());
            }
            else {
                add(details, "Node IP", record.getIpAddress());
            }
            add(details, "Action", sys.getAction());
            add(details, "Component", fixComponent(sys.getComponentId()));
            if(isClient) {
                add(details, "User ID", fixUserId(record.getUserId()));
                add(details, "User Name", record.getUserName());
            }
            add(details, "Entity name", record.getName());
        } else {
            add(details, "Event Type", "Unknown");
            add(details, "Entity name", record.getName());
            add(details, "IP Address", record.getIpAddress());
        }

        return details;
    }

    //- PRIVATE

    private void add( final Collection<Pair<String,String>> details, final String s, final Object n ) {
        details.add( new Pair<String,String>( s, n==null ? "" : n.toString() ));            
    }

    private void nonull( final Collection<Pair<String,String>> details, final String s, final Object n ) {
        if ( n != null ) {
            details.add( new Pair<String,String>( s, n.toString() ));            
        }
    }

    private void nonule( final Collection<Pair<String,String>> details, final String s, final Object n ) {
        String value = n == null ? null : n.toString();
        if ( value != null && value.length() > 0 ) {
            details.add( new Pair<String,String>( s, value ));
        }
    }

    private String fixComponent( final int componentId ) {
        com.l7tech.gateway.common.Component c = fromId(componentId);
        if (c == null) return "Unknown Component #" + componentId;
        StringBuffer ret = new StringBuffer(c.getName());
        while (c.getParent() != null && c.getParent() != c) {
            ret.insert(0, ": ");
            ret.insert(0, c.getParent().getName());
            c = c.getParent();
        }
        return ret.toString();
    }

    /** Strip the "com.l7tech." from the start of a class name. */
    private String fixType( final String entityClassname ) {
        final String coml7tech = "com.l7tech.";
        if (entityClassname == null) {
            return "<unknown>";
        } else if (entityClassname.startsWith(coml7tech))
            return entityClassname.substring(coml7tech.length());
        return entityClassname;
    }

    private String fixUserId(String id) {
        return (id!=null ? id : "<No ID>");
    }

    /** Convert a single-character action into a human-readable String. */
    private String fixAction( final char action ) {
        switch (action) {
            case AdminAuditRecord.ACTION_CREATED:
                return "Object Created";
            case AdminAuditRecord.ACTION_UPDATED:
                return "Object Changed";
            case AdminAuditRecord.ACTION_DELETED:
                return "Object Deleted";
            case AdminAuditRecord.ACTION_LOGIN:
                return "Admin Login";
            case AdminAuditRecord.ACTION_OTHER:
                return "Other";
            default:
                return "Unknown Action '" + action + "'";
        }
    }

    private class AuditDetailDataProvider extends SortableDataProvider<AuditDetailModel> {
        private final List<AuditDetailModel> details;

        public AuditDetailDataProvider( final AuditRecord auditRecord,
                                        final String sort,
                                        final boolean asc ) {
            this.details = listFiles( auditRecord );
            this.setSort( sort, asc );
        }

        private List<AuditDetailModel> listFiles( final AuditRecord record ) {
            List<AuditDetailModel> files = new ArrayList<AuditDetailModel>();

            for ( AuditDetail detail : record.getDetails() ) {
                files.add( new AuditDetailModel(detail) );
            }

            return files;
        }

        @Override
        public Iterator<AuditDetailModel> iterator( int first, int count ) {
            return newAuditIter( details, first, first+count, getSort().getProperty(), getSort().isAscending() );
        }

        @Override
        public int size() {
            return details.size();
        }

        @Override
        public IModel<AuditDetailModel> model(final AuditDetailModel auditObject) {
             return new AbstractReadOnlyModel<AuditDetailModel>() {
                @Override
                public AuditDetailModel getObject() {
                    return auditObject;
                }
            };
        }

        @Override
        public void detach() {
        }

        private Iterator<AuditDetailModel> newAuditIter( List<AuditDetailModel> audits, int start, int end, final String sortBy, final boolean asc ) {
            List<AuditDetailModel> list = new ArrayList<AuditDetailModel>( audits );

            Collections.sort(list, new Comparator<AuditDetailModel>(){
                @Override
                @SuppressWarnings({"unchecked"})
                public int compare(AuditDetailModel detail1, AuditDetailModel detail2) {
                    Comparable v1 = detail1.getDate();
                    Comparable v2 = detail2.getDate();

                    if ( "level".equals(sortBy) ) {
                        v1 = detail1.getLevel().intValue();
                        v2 = detail2.getLevel().intValue();
                    } else if ( "message".equals(sortBy) ) {
                        v1 = detail1.getMessage();
                        v2 = detail2.getMessage();
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

    private static final class AuditDetailModel implements Serializable {
        private final Date date;
        private final Level level;
        private final String message;

        AuditDetailModel( final AuditDetail auditDetail ) {
            this( new Date(auditDetail.getTime()),
                  MessagesUtil.getAuditDetailMessageById(auditDetail.getMessageId()),
                  auditDetail.getParams() );
        }

        AuditDetailModel( final Date date, final AuditDetailMessage message, final String[] params ) {
            this( date, message.getLevel(), MessageFormat.format(message.getMessage(), (Object[])params) );
        }

        AuditDetailModel( final Date date, final Level level, final String message ) {
            this.date = date;
            this.level = level;
            this.message = message;
        }

        public Date getDate() {
            return date;
        }

        public Level getLevel() {
            return level;
        }

        public String getMessage() {
            return message;
        }
    }

}
