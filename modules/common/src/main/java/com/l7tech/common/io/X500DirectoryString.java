package com.l7tech.common.io;

import org.apache.harmony.security.asn1.ASN1Choice;
import org.apache.harmony.security.asn1.ASN1StringType;
import org.apache.harmony.security.asn1.ASN1Type;
import org.apache.harmony.security.asn1.BerInputStream;
import org.apache.harmony.security.asn1.BerOutputStream;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Access X.500 DirectoryString contents.
 *
 * <pre>
 * DirectoryString ::= CHOICE {
 *       teletexString           TeletexString (SIZE (1..MAX)),
 *       printableString         PrintableString (SIZE (1..MAX)),
 *       universalString         UniversalString (SIZE (1..MAX)),
 *       utf8String              UTF8String (SIZE (1.. MAX)),
 *       bmpString               BMPString (SIZE (1..MAX))
 * }
 * </pre>
 *
 * <p>NOTE: This class also permits IA5String and GeneralString.</p>
 *
 * <p>See http://harmony.apache.org/subcomponents/classlibrary/asn1_framework.html
 * (Harmony ASN.1 framework)</p> 
 */
public class X500DirectoryString {

    //- PUBLIC

    public X500DirectoryString( final byte[] derDirectoryString ) throws IOException {
        contents = (String) directoryString.decode( derDirectoryString );
    }

    public String getContents() {
        return contents;
    }

    public String toString() {
        return getContents();
    }

    //- PRIVATE

    private static final ASN1StringType UTF8STRING = new ASN1EncodedStringType(12, "UTF8");
    private static final ASN1StringType PRINTABLESTRING = new ASN1EncodedStringType(19, "ASCII");
    private static final ASN1StringType TELETEXSTRING = new ASN1EncodedStringType(20, "ISO-8859-1");
    private static final ASN1StringType IA5STRING = new ASN1EncodedStringType(22, "ASCII");
    private static final ASN1StringType GENERALSTRING = new ASN1EncodedStringType(27, "ASCII");
    // Not sure which character set to use for UniversalString, it appears to be UCS-4 / ISO 10646
    // since Java's DerValue class doesn't support it, it presumably isn't used much.
    // private static final ASN1StringType UNIVERSALSTRING = new ASN1EncodedStringType(28, "UTF-32");
    private static final ASN1StringType BMPSTRING = new ASN1EncodedStringType(30, "UnicodeBigUnmarked");

    private static final ASN1Type directoryString = buildDirectoryString();

    private final String contents;


    private static ASN1Type buildDirectoryString() {
        return new ASN1Choice(new ASN1Type[]{
                    TELETEXSTRING,
                    PRINTABLESTRING,
                    UTF8STRING,
                    BMPSTRING,
                    IA5STRING,
                    GENERALSTRING
            }){
                @Override
                public int getIndex( final Object o ) {
                    return 0;
                }

                @Override
                public Object getObjectToEncode( final Object o ) {
                    return null;
                }
            };
    }

    /**
     * Harmony's built in classes don't encode the strings correctly
     */
    private static final class ASN1EncodedStringType extends ASN1StringType {
        private final Charset encoding;

        ASN1EncodedStringType( final int tag, final String encoding ) {
            super( tag );
            this.encoding = Charset.forName( encoding );
        }

        @Override
        public Object getDecodedObject( final BerInputStream berInputStream ) throws IOException {
            return new String(
                    berInputStream.getBuffer(),
                    berInputStream.getContentOffset(),
                    berInputStream.getLength(),
                    encoding);
        }

        @Override
        public void encodeContent( final BerOutputStream berOutputStream ) {
            byte bytes[] = ((String)berOutputStream.content).getBytes( encoding );
            berOutputStream.content = bytes;
            berOutputStream.length = bytes.length;
        }
    }
}
