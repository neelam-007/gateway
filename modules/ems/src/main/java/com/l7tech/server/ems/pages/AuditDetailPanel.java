package com.l7tech.server.ems.pages;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import com.l7tech.gateway.common.audit.AuditRecord;
import com.l7tech.gateway.common.audit.AdminAuditRecord;
import com.l7tech.gateway.common.audit.SystemAuditRecord;
import com.l7tech.gateway.common.audit.AuditDetail;
import com.l7tech.gateway.common.audit.Messages;
import com.l7tech.gateway.common.audit.AuditDetailMessage;
import static com.l7tech.gateway.common.Component.fromId;
import com.l7tech.server.ems.EmsSession;

import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Collections;
import java.util.Comparator;
import java.util.logging.Level;
import java.io.Serializable;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;

/**
 * Panel to display audit details.
 */
public class AuditDetailPanel extends Panel {

    //- PUBLIC

    /**
     *
     */
    public AuditDetailPanel( final String identifier, final AuditRecord record ) {
        super( identifier, new Model(record) );

        Label summary = new Label("auditsummary", new Model(buildLogDetails(record)));

        List<PropertyColumn> columns = new ArrayList<PropertyColumn>();
        columns.add(new PropertyColumn(new StringResourceModel("auditdetailtable.column.date", this, null), "DATE", "date"));
        columns.add(new PropertyColumn(new StringResourceModel("auditdetailtable.column.level", this, null), "LEVEL", "level"));
        columns.add(new PropertyColumn(new StringResourceModel("auditdetailtable.column.message", this, null), "MESSAGE", "message"));
        YuiDataTable table = new YuiDataTable("auditdetailtable", columns, "TIME", false,  new AuditDetailDataProvider(record, "DATE", false));

        add( summary );
        add( table );
    }

    /**
     *
     */
    public String buildLogDetails( final AuditRecord record ) {
        EmsSession session = (EmsSession) getSession();

        String msg = "";
        msg += nonull("Time       : ", new SimpleDateFormat(session.getDateTimeFormatPattern()).format( new Date(record.getMillis()) ));
        msg += nonull("Severity   : ", record.getLevel());
        msg += nonule("Request Id : ", record.getReqId());
        msg += nonull("Class      : ", record.getSourceClassName());
        msg += nonull("Method     : ", record.getSourceMethodName());
        msg += nonull("Message    : ", record.getMessage());

        msg += "\n";

        if (record instanceof AdminAuditRecord) {
            AdminAuditRecord aarec = (AdminAuditRecord)record;
            msg += "Event Type : Manager Action" + "\n";
            msg += "Admin user : " + aarec.getUserName() + "\n";
            msg += "Admin IP   : " + record.getIpAddress() + "\n";
            msg += "Action     : " + fixAction(aarec.getAction()) + "\n";
            if (AdminAuditRecord.ACTION_LOGIN!=aarec.getAction() &&
                AdminAuditRecord.ACTION_OTHER!=aarec.getAction()) {
                msg += "Entity name: " + record.getName() + "\n";
                msg += "Entity id  : " + aarec.getEntityOid() + "\n";
                msg += "Entity type: " + fixType(aarec.getEntityClassname()) + "\n";
            }
        } else if (record instanceof SystemAuditRecord) {
            SystemAuditRecord sys = (SystemAuditRecord)record;
            com.l7tech.gateway.common.Component component = fromId(sys.getComponentId());
            boolean isClient = component != null && component.isClientComponent();
            msg += "Event Type : System Message" + "\n";
            if(isClient) {
                msg += "Client IP  : " + record.getIpAddress() + "\n";
            }
            else {
                msg += "Node IP    : " + record.getIpAddress() + "\n";
            }
            msg += "Action     : " + sys.getAction() + "\n";
            msg += "Component  : " + fixComponent(sys.getComponentId()) + "\n";
            if(isClient) {
                msg += "User ID    : " + fixUserId(record.getUserId()) + "\n";
                msg += "User Name  : " + record.getUserName() + "\n";
            }
            msg += "Entity name: " + record.getName() + "\n";
        } else {
            msg += "Event Type : Unknown" + "\n";
            msg += "Entity name: " + record.getName() + "\n";
            msg += "IP Address : " + record.getIpAddress() + "\n";
        }

        return msg;
    }

    //- PRIVATE

    private String nonull( final String s, final Object n ) {
        return n == null ? "" : (s + n + "\n");
    }

    private String nonule( final String s, final Object n ) {
        String result = "";
        String value = n == null ? null : n.toString();

        if ( value != null && value.length() > 0 ) {
            result = s + n + "\n";
        }

        return result;
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

    private class AuditDetailDataProvider extends SortableDataProvider {
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

        public Iterator iterator( int first, int count ) {
            return newAuditIter( details, first, first+count, getSort().getProperty(), getSort().isAscending() );
        }

        public int size() {
            return details.size();
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

        private Iterator<AuditDetailModel> newAuditIter( List<AuditDetailModel> audits, int start, int end, final String sortBy, final boolean asc ) {
            List<AuditDetailModel> list = new ArrayList<AuditDetailModel>( audits );

            Collections.sort(list, new Comparator<AuditDetailModel>(){
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
                  Messages.getAuditDetailMessageById(auditDetail.getMessageId()),
                  auditDetail.getParams() );
        }

        AuditDetailModel( final Date date, final AuditDetailMessage message, final String[] params ) {
            this( date, message.getLevel(), MessageFormat.format(message.getMessage(), params) );
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
