package com.l7tech.external.assertions.wsmanagement.server;

import com.sun.ws.management.server.HandlerContext;
import com.sun.ws.management.server.EnumerationSupport;
import com.sun.ws.management.server.IteratorFactory;
import com.sun.ws.management.server.EnumerationIterator;
import com.sun.ws.management.server.EnumerationItem;
import com.sun.ws.management.Management;
import com.sun.ws.management.InternalErrorFault;
import com.sun.ws.management.InvalidSelectorsFault;
import com.sun.ws.management.soap.FaultException;
import com.sun.ws.management.enumeration.Enumeration;
import com.sun.ws.management.transfer.TransferExtensions;
import com.sun.ws.management.framework.handlers.DefaultHandler;
import com.sun.ws.management.framework.Utilities;
import com.sun.ws.management.framework.transfer.TransferSupport;
import com.sun.ws.management.framework.enumeration.Enumeratable;
import com.sun.ws.management.framework.enumeration.EnumerationHandler;
import com.l7tech.external.assertions.wsmanagement.server.model.Service;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.util.ExceptionUtils;
import org.dmtf.schemas.wbem.wsman._1.wsman.SelectorType;
import org.xmlsoap.schemas.ws._2004._08.addressing.EndpointReferenceType;

import javax.xml.parsers.DocumentBuilder;
import java.util.Set;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;

/**
 * 
 */
public class ResourceHandler extends DefaultHandler implements Enumeratable {

    //- PUBLIC

    public ResourceHandler() {
        try {
            EnumerationSupport.registerIteratorFactory( "http://www.layer7tech.com/management/services", new ResourceIteratorFactory() );
        } catch ( Exception e ) {
            throw ExceptionUtils.wrap( e );
        }
    }

    @Override
    public void get( final HandlerContext context,
                     final Management request,
                     final Management response ) {

        final String oid = getSelector( request, "serviceoid" );
        if ( oid == null ) {
            throw new InvalidSelectorsFault(InvalidSelectorsFault.Detail.INSUFFICIENT_SELECTORS);
        }

        final ServiceManager manager = (ServiceManager) context.getRequestProperties().get("com.l7tech.serviceManager");
        try {
            TransferExtensions xferRequest = new TransferExtensions(request);
            TransferExtensions xferResponse = new TransferExtensions(response);

            PublishedService service = manager.findByPrimaryKey( Long.parseLong( oid ));

            Service test = new Service();
            test.setEnabled( !service.isDisabled() );
            test.setType( service.isSoap() ? "soap" : "xml" );
            test.setName( service.getName() );
            test.setOperationValidation( !service.isLaxResolution() );

            xferResponse.setFragmentGetResponse(  xferRequest.getFragmentHeader(), test );
        } catch (Exception e) {
            throw new InternalErrorFault(e);
        }
    }

    @Override
    public void release( final HandlerContext context, final Enumeration enuRequest, final Enumeration enuResponse ) {
        enumHandler.release( context, enuRequest, enuResponse );
    }

    @Override
    public void pull( final HandlerContext context, final Enumeration enuRequest, final Enumeration enuResponse ) {
        enumHandler.pull( context, enuRequest, enuResponse );
    }

    @Override
    public void enumerate( final HandlerContext context, final Enumeration enuRequest, final Enumeration enuResponse ) {
        enumHandler.enumerate( context, enuRequest, enuResponse );
    }

    @Override
    public void getStatus( final HandlerContext context, final Enumeration enuRequest, final Enumeration enuResponse ) {
        enumHandler.getStatus( context, enuRequest, enuResponse );
    }

    @Override
    public void renew( final HandlerContext context, final Enumeration enuRequest, final Enumeration enuResponse ) {
        enumHandler.renew( context, enuRequest, enuResponse );
    }

    //- PRIVATE

    private final ResourceEnumerationHandler enumHandler = new ResourceEnumerationHandler();

    private static String getSelector( final Management request,
                                       final String name )
                            throws InternalErrorFault {
        Set<SelectorType> selectors;
        try {
            selectors = request.getSelectors();
        } catch ( Exception e ) {
            throw new InternalErrorFault(e);
        }

        if ( Utilities.getSelectorByName( name, selectors) == null ) {
            return null;
        }

        return (String) Utilities.getSelectorByName(name, selectors).getContent().get(0);
    }

    private final static class ResourceEnumerationHandler extends EnumerationHandler {
    }

    private final static class ResourceIteratorFactory implements IteratorFactory {
        @Override
        public EnumerationIterator newIterator( final HandlerContext context,
                                                final Enumeration request,
                                                final DocumentBuilder db,
                                                final boolean includeItem,
                                                final boolean includeEPR ) throws FaultException {
            final ServiceManager manager = (ServiceManager) context.getRequestProperties().get("com.l7tech.serviceManager");
            return new ResourceEnumerationIterator( manager, context.getURL(), includeEPR );
        }
    }

    private final static class ResourceEnumerationIterator implements EnumerationIterator {
        private final Collection<ServiceHeader> services;
        private final Iterator<ServiceHeader> serviceIterator;
        private final String address;
        private final boolean includeEPR;

        public ResourceEnumerationIterator( final ServiceManager manager,
                                            final String address,
                                            final boolean includeEPR ) {
            try {
                services = manager.findAllHeaders( false );
                serviceIterator = services.iterator();
            } catch ( Exception e ) {
                throw new InternalErrorFault(e);
            }

            this.address = address;
            this.includeEPR = includeEPR;
        }

        @Override
        public int estimateTotalItems() {
            return services.size();
        }

        @Override
        public boolean isFiltered() {
            return false;
        }

        @Override
        public boolean hasNext() {
            return serviceIterator.hasNext();
        }

        @Override
        public EnumerationItem next() {
            ServiceHeader header = serviceIterator.next();

            Service test = new Service();
            test.setEnabled( !header.isDisabled() );
            test.setType( header.isSoap() ? "soap" : "xml" );
            test.setName( header.getName() );
            test.setOperationValidation( false );

            final EndpointReferenceType epr;
            if ( includeEPR ) {
                Map<String, String> selectors = new HashMap<String, String>();
                selectors.put("serviceoid", Long.toString(header.getOid()));

                try {
                    epr = TransferSupport.createEpr(
                            address,
                            "http://www.layer7tech.com/management/services",
                            selectors );
                } catch ( Exception e ) {
                    throw new InternalErrorFault(e);
                }
            } else {
                epr = null;
            }

            return new EnumerationItem( test, epr );
        }

        @Override
        public void release() {
        }
    }
}
