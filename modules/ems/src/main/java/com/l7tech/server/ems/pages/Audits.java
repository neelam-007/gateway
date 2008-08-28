package com.l7tech.server.ems.pages;

import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

import java.util.Iterator;
import java.util.List;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.Serializable;

import com.l7tech.gateway.common.audit.AuditSearchCriteria;
import com.l7tech.gateway.common.audit.AuditRecord;
import com.l7tech.server.audit.AuditRecordManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.util.Functions;

/**
 * 
 */
public class Audits extends EmsPage {
    
    //- PUBLIC    
    
    public Audits() {
        List<PropertyColumn> columns = new ArrayList<PropertyColumn>();
        columns.add(new PropertyColumn(new StringResourceModel("audittable.column.time", this, null), "TIME", "time"));
        columns.add(new PropertyColumn(new StringResourceModel("audittable.column.level", this, null), "LEVEL", "level"));
        columns.add(new PropertyColumn(new StringResourceModel("audittable.column.message", this, null), "MESSAGE", "message"));

        YuiDataTable table = new YuiDataTable("audittable", columns, "TIME", false,  new AuditDataProvider("TIME", false));
        add(table);
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( Audits.class.getName() );

    @SuppressWarnings({"UnusedDeclaration"})
    @SpringBean
    private AuditRecordManager auditRecordManager;

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

        public AuditDataProvider(final String sort, final boolean asc) {
            auditSearchCriteria = new AuditSearchCriteria(null, new Date(), null, null, null, null, -1, -1, -1);
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
    }
}
