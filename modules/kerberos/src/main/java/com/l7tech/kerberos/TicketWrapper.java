package com.l7tech.kerberos;


import sun.security.krb5.internal.Ticket;



import java.util.Arrays;

/**
 * sun.security.krb5.internal.Ticket does not have equals and hashCode methods implemented so it can't be used as a key for the ticket cache
 * This class wraps  sun.security.krb5.internal.Ticket and provides equals and hashCode methods
 *
 * Copyright: CA Technologies, 2015
 * User: ymoiseyenko
 * Date: 2/19/15
 */
public class TicketWrapper  {
    final Ticket ticket;
    public TicketWrapper(Ticket ticket) {
        this.ticket = ticket;
    }

    public Ticket getTicket() {
        return ticket;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TicketWrapper that = (TicketWrapper) o;

        if (ticket.tkt_vno != that.ticket.tkt_vno) return false;
        if (ticket.encPart != null ? !Arrays.equals(ticket.encPart.getBytes(), that.ticket.encPart.getBytes()) : that.ticket.encPart != null) return false;
        if (ticket.sname != null ? !ticket.sname.equals(that.ticket.sname) : that.ticket.sname != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = ticket.tkt_vno;
        result = 31 * result + (ticket.sname != null ? ticket.sname.hashCode() : 0);
        result = 31 * result + (ticket.encPart != null ? Arrays.hashCode(ticket.encPart.getBytes()):0);
        return result;
    }

}
