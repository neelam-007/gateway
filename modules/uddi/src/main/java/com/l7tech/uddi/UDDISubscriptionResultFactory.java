package com.l7tech.uddi;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import com.l7tech.common.uddi.guddiv3.SubscriptionResultsList;
import com.l7tech.common.uddi.guddiv3.ServiceList;
import com.l7tech.common.uddi.guddiv3.ServiceInfos;
import com.l7tech.common.uddi.guddiv3.ServiceInfo;
import com.l7tech.common.uddi.guddiv3.KeyBag;
import com.l7tech.common.uddi.guddiv3.Subscription;
import com.l7tech.common.uddi.guddiv3.CoveragePeriod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.util.ExceptionUtils;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.List;

/**
 *
 */
public class UDDISubscriptionResultFactory {

    //- PUBLIC

    public static UDDISubscriptionResults buildResults( final String message ) throws UDDIException {
        try {
            return buildResults( XmlUtil.parse( message ) );
        } catch (SAXException e) {
            throw new UDDIException( "Error parsing message '"+ExceptionUtils.getMessage( e )+"'.", e );
        }
    }

    public static UDDISubscriptionResults buildResults( final Document message ) throws UDDIException {
        try {
            NodeList nodes = message.getElementsByTagNameNS( "urn:uddi-org:sub_v3", "subscriptionResultsList" );
            if ( nodes.getLength()!=1 ) {
                throw new UDDIException( "Invalid subscription results message." );                
            }

            JAXBContext context = JAXBContext.newInstance( SubscriptionResultsList.class.getPackage().getName() );
            JAXBElement<SubscriptionResultsList> resultsJAXB =
                    context.createUnmarshaller().unmarshal( new DOMSource(nodes.item( 0 )), SubscriptionResultsList.class );
            SubscriptionResultsList results = resultsJAXB.getValue();
            Subscription subscription = results.getSubscription();
            if ( subscription == null || subscription.getSubscriptionKey()==null ) {
                throw new UDDIException( "Missing subscription key in results." );                
            }
            CoveragePeriod coveragePeriod = results.getCoveragePeriod();
            if ( coveragePeriod == null ) {
                throw new UDDIException( "Missing coverage period in results." );                
            }

            UDDISubscriptionResultsImpl uddiResults = new UDDISubscriptionResultsImpl(
                    subscription.getSubscriptionKey(),
                    toTimeInMillis(coveragePeriod.getStartPoint(), 0),
                    toTimeInMillis(coveragePeriod.getEndPoint(), System.currentTimeMillis()) );

            // Long format results
            ServiceList serviceList = results.getServiceList();
            if ( serviceList != null ) {
                ServiceInfos serviceInfos = serviceList.getServiceInfos();
                if ( serviceInfos != null ) {
                    List<ServiceInfo> serviceInfoList = serviceInfos.getServiceInfo();
                    if ( serviceInfoList != null ) {
                        for ( ServiceInfo serviceInfo : serviceInfoList ) {
                            uddiResults.add( new UDDISubscriptionResultsImpl.ResultImpl( serviceInfo.getServiceKey(), false ) );
                        }
                    }
                }
            }

            // Brief format/deleted entity results
            List<KeyBag> bags = results.getKeyBag();
            if ( bags != null ) {
                for ( KeyBag bag : bags ) {
                    boolean deleted = bag.isDeleted();
                    List<String> serviceKeys = bag.getServiceKey();
                    if ( serviceKeys != null ) {
                        for ( String servicekey : serviceKeys ) {
                            uddiResults.add( new UDDISubscriptionResultsImpl.ResultImpl( servicekey, deleted ) );
                        }
                    }
                }
            }

            return uddiResults;
        } catch (JAXBException e) {
            throw new UDDIException( "Error unmarshalling results: " + ExceptionUtils.getMessage( e ), e );
        }
    }

    //- PRIVATE

    private static long toTimeInMillis( final XMLGregorianCalendar xmlGregorianCalendar, final long defaultValue ) {
        long time = defaultValue;

        if ( xmlGregorianCalendar != null ) {
            time = xmlGregorianCalendar.toGregorianCalendar().getTimeInMillis();
        }

        return time;
    }
}
