package com.l7tech.server.management.api.node;

import org.junit.Test;
import org.junit.Assert;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.StringWriter;
import java.io.StringReader;

import com.l7tech.objectmodel.EntityHeaderSet;
import com.l7tech.objectmodel.EntityHeaderSetType;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;

/**
 *
 */
public class MigrationApiTest {

    @SuppressWarnings({"unchecked"})
    @Test
    public void testEntityHeaderSetSerialization() throws Exception {
        EntityHeaderSet set = new EntityHeaderSet();
        roundTrip( new Holder(set) );

        EntityHeaderSet seta = new EntityHeaderSet( new EntityHeader( "id", EntityType.EMAIL_LISTENER, "name", "description" ) );
        seta.setMaxExceeded(100);
        roundTrip( new Holder(seta) );

        EntityHeaderSet set2 = new EntityHeaderSet( new EntityHeader( "id", EntityType.EMAIL_LISTENER, "name", "description" ) );
        roundTrip( new Holder(set2) );

        EntityHeaderSet set3 = new EntityHeaderSet(
                new EntityHeader( "id1", EntityType.EMAIL_LISTENER, "name", "description" ),
                new EntityHeader( "id2", EntityType.EMAIL_LISTENER, "name", "description" ));
        roundTrip( new Holder(set3) );

    }

    private Object roundTrip( Object object ) throws Exception {
        StringWriter out = new StringWriter();
        JAXBContext context = JAXBContext.newInstance( Holder.class, EntityHeaderSet.class, EntityHeaderSetType.class, EntityHeaderSetType.EntityHeaderSetTypeAdapter.class );
        Marshaller marshaller = context.createMarshaller();
        marshaller.setEventHandler( new ValidationEventHandler(){
            @Override
            public boolean handleEvent( final ValidationEvent event ) {
                System.out.println( event );
                return false;
            }
        } );
        marshaller.setListener( new Marshaller.Listener(){
            @Override
            public void beforeMarshal(Object source) {
                //System.out.println( "Marshalling: " + source );
            }
        } );
        marshaller.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE );
        marshaller.marshal( object, out );
        System.out.println( out );

        Unmarshaller unmarshaller = context.createUnmarshaller();
        Object objectRedux = unmarshaller.unmarshal( new StringReader( out.toString() ) );

        Assert.assertTrue( "Roundtrip JAXB equality failed.", object.equals(objectRedux) );

        return objectRedux;
    }

    /**
     * Helper class to allow for non root content.
     */
    @XmlRootElement
    public static class Holder {
        private EntityHeaderSet data;

        public Holder() {
        }

        public Holder( EntityHeaderSet data ) {
            this.data = data;
        }

        public EntityHeaderSet getData() {
            return data;
        }

        public void setData(EntityHeaderSet data) {
            this.data = data;
        }

        @Override
        @SuppressWarnings({"RedundantIfStatement"})
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Holder holder = (Holder) o;

            if (data != null ? !data.equals(holder.data) : holder.data != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return (data != null ? data.hashCode() : 0);
        }
    }

}
