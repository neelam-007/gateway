/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.audit;

import com.l7tech.objectmodel.imp.EntityImp;

import java.util.logging.Level;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class AuditRecord extends EntityImp {
    /** @deprecated to be called only for serialization and persistence purposes! */
    protected AuditRecord() {
    }

    public AuditRecord( Level level, String nodeId, String message ) {
        this.level = level;
        this.nodeId = nodeId;
        this.message = message;
        this.time = System.currentTimeMillis();
    }

    public long getTime() {
        return time;
    }

    public Level getLevel() {
        return level;
    }

    public String getMessage() {
        return message;
    }

    public String getNodeId() {
        return nodeId;
    }

    /** @deprecated to be called only for serialization and persistence purposes! */
    public String getStrLvl() {
        if (level == null) return null;
        return getLevel().getName();
    }

    /** @deprecated to be called only for serialization and persistence purposes! */
    public void setStrLvl(String arg) {
        if (arg == null) return;
        setLevel(Level.parse(arg));
    }

    /** @deprecated to be called only for serialization and persistence purposes! */
    public void setOid( long oid ) {
        super.setOid( oid );
    }

    /** @deprecated to be called only for serialization and persistence purposes! */
    public void setTime( long time ) {
        this.time = time;
    }

    /** @deprecated to be called only for serialization and persistence purposes! */
    public void setVersion( int version ) {
        super.setVersion( version );
    }

    /** @deprecated to be called only for serialization and persistence purposes! */
    public void setLevel( Level level ) {
        this.level = level;
    }

    /** @deprecated to be called only for serialization and persistence purposes! */
    public void setMessage( String message ) {
        this.message = message;
    }

    /** @deprecated to be called only for serialization and persistence purposes! */
    public void setNodeId( String nodeId ) {
        this.nodeId = nodeId;
    }

    public boolean equals( Object o ) {
        if ( this == o ) return true;
        if ( !(o instanceof AuditRecord) ) return false;

        final AuditRecord auditRecord = (AuditRecord) o;

        if ( _oid != auditRecord._oid) return false;
        if ( time != auditRecord.time ) return false;
        if ( level != null ? !level.equals( auditRecord.level ) : auditRecord.level != null ) return false;
        if ( message != null ? !message.equals( auditRecord.message ) : auditRecord.message != null ) return false;
        if ( nodeId != null ? !nodeId.equals( auditRecord.nodeId ) : auditRecord.nodeId != null ) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (int) (_oid ^ (_oid >>> 64));
        result = (int) (time ^ (time >>> 32));
        result = 29 * result + (level != null ? level.hashCode() : 0);
        result = 29 * result + (message != null ? message.hashCode() : 0);
        result = 29 * result + (nodeId != null ? nodeId.hashCode() : 0);
        return result;
    }

    protected long time;
    protected Level level;
    protected String message;
    protected String nodeId;
}
