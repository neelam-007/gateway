package com.l7tech.security.prov.rsa;

import com.rsa.jsafe.provider.JsafeJCE;
import sun.misc.BASE64Decoder;

import java.lang.reflect.Method;
import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.Security;
import java.security.spec.*;


/**
 * Test AlgorithmParameters.EC ability to lookup a named curve's ECGenParameterSpec from its ECParameterSpec.
 * <p/>
 * Inability to do this prevents the JDK 8 version of SunJSSE from supporting any ECDHE cipher suite
 * when using Crypto-J's EC implementation (which customers who a FIPS provider may want to do).
 * The symptom is that the ServerKeyExchange always indicates the ephemeral key uses curveId 65535 which is not valid.
 * <p/>
 * The root cause of the regression appears to be a change in JDK 8 SunJSSE to use AlgorithmParameters.EC to look up
 * the curve name from the parameters instead of its previous (JDK 7) sort-of hacky approach of using
 * sun.security.ec.ECParameters#getNamedCurve which iterates all sun.security.ec.NamedCurve#knownECParameterSpecs
 */
public class RsaReproLookupCurveNameFromParameters {
    public static void main( String[] args ) throws Exception {
        // Install Crypto-J 6.2.0.1 as most-preferred provider
        // Comment out this line to allow code to use SunEC's AlgorithmParameters.EC impl instead, which lets tests pass
        Security.insertProviderAt( new JsafeJCE(), 1 );

        // Build an ECParameterSpec for P-256, looking like it does when SunJSSE tries to use it
        BigInteger p = new BigInteger( "115792089210356248762697446949407573530086143415290314195533631308867097853951" );
        ECFieldFp field = new ECFieldFp( p );
        BigInteger a = new BigInteger( "115792089210356248762697446949407573530086143415290314195533631308867097853948" );
        BigInteger b = new BigInteger( "41058363725152142129326129780047268409114441015993725554835256314039467401291" );
        byte[] seed = new BASE64Decoder().decodeBuffer( "xJ02CIbnBJNqZnjhE50mt4GffpA=" );
        EllipticCurve ec = new EllipticCurve( field, a, b, seed );
        BigInteger x = new BigInteger( "48439561293906451759052585252797914202762949526041747995844080717082404635286" );
        BigInteger y = new BigInteger( "36134250956749795798585127919587881956611106672985015071877198253568414405109" );
        ECPoint point = new ECPoint( x, y );
        BigInteger n = new BigInteger( "115792089210356248762697446949407573529996955224135760342422259061068512044369" );
        ECParameterSpec spec = new ECParameterSpec( ec, point, n, 1 );


        // Test #1: Emulate SunJSSE lookup of EC curve index (-1 means not found / no way to express curve in handshake)
        Method getCurveIndex = Class.forName( "sun.security.ssl.SupportedEllipticCurvesExtension" ).
                getDeclaredMethod( "getCurveIndex", ECParameterSpec.class );
        getCurveIndex.setAccessible( true );
        int index = (int)getCurveIndex.invoke( null, spec );
        System.out.println( "index = " + index + " (expect 23)" );
        // expected: 23  (RFC4492 NamedCurve enum index of secp256r1)
        // actual: -1 (meaning unable to figure out which RFC4492 named curve these parameters describe)
        // Passes with SunEC provider, and with Crypto-J provider under JDK 7 (tested CryptoJ version 6.1.2)
        // Fails with Crypto-J provider under JDK 8 (tested Crypto-J versions 6.2.0.1 and 6.1.2)


        // Test #2: Try to look up curve name using method used by ECUtil.getCurveName() and, indirectly,
        // by SupportedEllipticCurvesExtension.getCurveIndex()
        AlgorithmParameters parameters = AlgorithmParameters.getInstance( "EC" );
        parameters.init( spec );
        ECGenParameterSpec genSpec = parameters.getParameterSpec( ECGenParameterSpec.class ); // throws here
        System.out.println( "curve name = " + genSpec.getName() + " (expect 1.2.840.10045.3.1.7)" );
        // expected: "1.2.840.10045.3.1.7" (though "secp256r1", "P-256", or "prime256v1" would probably work also)
        // actual: InvalidParameterSpecException: ECGenParameterSpec cannot be created for non-named curve.
        // Passes with SunEC provider
        // Fails with Crypto-J provider under any JDK version (tested Crypto-J versions 6.2.0.1 and 6.1.2)
    }
}
