package com.l7tech.server.wsdm;

import com.l7tech.xml.soap.SoapUtil;

/**
 * Namespaces used by the ESM subsystem.
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Nov 5, 2007<br/>
 */
public class Namespaces {
    public static final String ESMSM	= "http://metadata.dod.mil/mdr/ns/netops/esm/esmsm";
    public static final String QOSM	    = "http://metadata.dod.mil/mdr/ns/netops/esm/qosm";
    public static final String QOSMW	= "http://metadata.dod.mil/mdr/ns/netops/esm/qosmw";
    public static final String MUWS1	= "http://docs.oasis-open.org/wsdm/muws1-2.xsd";
    public static final String MUWS2	= "http://docs.oasis-open.org/wsdm/muws2-2.xsd";
    public static final String MUWS_EV	= "http://docs.oasis-open.org/wsdm/muwse2-2.xml";
    public static final String MOWS	    = "http://docs.oasis-open.org/wsdm/mows-2.xsd";
    public static final String MOWSSE	= "http://docs.oasis-open.org/wsdm/mowse-2.xsd";
    public static final String MOWS2W	= "http://docs.oasis-open.org/wsdm/mows-2.wsdl";
    public static final String WSRF_RP	= "http://docs.oasis-open.org/wsrf/rp-2";
    public static final String WSRF_RPW	= "http://docs.oasis-open.org/wsrf/rpw-2";
    public static final String WSRF_RW	= "http://docs.oasis-open.org/wsrf/rw-2";
    public static final String WSRF_BF	= "http://docs.oasis-open.org/wsrf/bf-2";
    public static final String WSRF_R	= "http://docs.oasis-open.org/wsrf/r-2";
    public static final String WSNT	    = "http://docs.oasis-open.org/wsn/b-2";
    public static final String WSNTW	= "http://docs.oasis-open.org/wsn/bw-2";
    public static final String WSTOP	= "http://docs.oasis-open.org/wsn/t-1";
    public static final String RMD	    = "http://docs.oasis-open.org/wsrf/rmd-1";

    public static final String WSA = "http://www.w3.org/2005/08/addressing";
    public static final String MOWS_XS = "http://docs.oasis-open.org/wsdm/2004/12/mows/wsdm-mows.xsd";
    public static final String MUWS_P1_XS = "http://docs.oasis-open.org/wsdm/2004/12/muws/wsdm-muws-part1.xsd";
    //public static final String WSRF_RP  = "http://docs.oasis-open.org/wsrf/2004/06/wsrf-WS-ResourceProperties-1.2-draft-01.xsd";
    //public static final String MUWS_EV = "http://docs.oasis-open.org/wsdm/2004/12/muws/wsdm-muws-part2-events.xml";
    public static final String MOWSE = "http://docs.oasis-open.org/wsdm/mowse-2.xml";

    public static final String[] ALL_WSA = {WSA, SoapUtil.WSA_NAMESPACE, SoapUtil.WSA_NAMESPACE2};
}
