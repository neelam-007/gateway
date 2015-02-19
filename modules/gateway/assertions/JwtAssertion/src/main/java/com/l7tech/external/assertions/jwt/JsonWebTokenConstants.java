package com.l7tech.external.assertions.jwt;


import com.google.common.collect.*;
import com.l7tech.util.Pair;
import org.jose4j.jwk.Use;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class JsonWebTokenConstants {
    public static final String KEY_TYPE_PRIVATE_KEY = "Private Key";
    public static final String KEY_TYPE_CERTIFICATE = "Certificate";
    public static final String KEY_TYPE_JWK = "JSON Web Key";
    public static final String KEY_TYPE_JWKS = "JSON Web Key Set";

    public static final Set<String> UNSUPPORTED_ALGORITHMS = ImmutableSet.of(
            //jws
            "PS256",
            "PS384",
            "PS512",
            //jwe
            "RSA1_5",
            "RSA-OAEP-256",
            "A192KW",
            "ECDH-ES+A192KW",
            "A128GCMKW",
            "A192GCMKW",
            "A256GCMKW",
            "PBES2-HS256+A128KW",
            "PBES2-HS384+A192KW",
            "PBES2-HS512+A256KW",
            //cek
            "A192CBC-HS384",
            "A128GCM",
            "A192GCM",
            "A256GCM"
    );

    public static final BiMap<String, String> SIGNATURE_ALGORITHMS = ImmutableBiMap.<String, String>builder()
            .put("HS256", "HMAC Using SHA-256")
            .put("HS384", "HMAC Using SHA-384")
            .put("HS512", "HMAC Using SHA-512")
            .put("RS256", "RSASSA-PKCS-v1_5 using SHA-256")
            .put("RS384", "RSASSA-PKCS-v1_5 using SHA-384")
            .put("RS512", "RSASSA-PKCS-v1_5 using SHA-512")
            .put("ES256", "ECDSA using P-256 and SHA-256")
            .put("ES384", "ECDSA using P-256 and SHA-384")
            .put("ES512", "ECDSA using P-256 and SHA-512")
            .put("PS256", "RSASSA-PSS using SHA-256 and MGF1 with SHA-256")
            .put("PS384", "RSASSA-PSS using SHA-384 and MGF1 with SHA-384")
            .put("PS512", "RSASSA-PSS using SHA-512 and MGF1 with SHA-512")
            .build();

    public static final BiMap<String, String> KEY_MANAGEMENT_ALGORITHMS = ImmutableBiMap.<String, String>builder()
            .put("RSA1_5", "RSAES-PKCS1-V1_5")
            .put("RSA-OAEP", "RSAES OAEP using default parameters")
            .put("RSA-OAEP-256", "RSAES OAEP using SHA-256 and MGF1 with SHA-256")
            .put("A128KW", "AES Key Wrap with default initial value using 128 bit key")
            .put("A192KW", "AES Key Wrap with default initial value using 192 bit key")
            .put("A256KW", "AES Key Wrap with default initial value using 256 bit key")
            .put("dir", "Direct use of a shared symmetric key as the Content Encryption Key (CEK)")
            .put("ECDH-ES", "Elliptic Curve Diffie-Hellman Ephemeral Static key agreement using Concat KDF")
            .put("ECDH-ES+A128KW", "ECDH-ES using Concat KDF and CEK wrapped with A128KW")
            .put("ECDH-ES+A192KW", "ECDH-ES using Concat KDF and CEK wrapped with A192KW")
            .put("ECDH-ES+A256KW", "ECDH-ES using Concat KDF and CEK wrapped with A256KW")
            .put("A128GCMKW", "Key wrapping with AES GCM using 128 bit key")
            .put("A192GCMKW", "Key wrapping with AES GCM using 192 bit key")
            .put("A256GCMKW", "Key wrapping with AES GCM using 256 bit key")
            .put("PBES2-HS256+A128KW", "PBES2 with HMAC SHA-256 and A128KW wrapping")
            .put("PBES2-HS384+A192KW", "PBES2 with HMAC SHA-384 and A192KW wrapping")
            .put("PBES2-HS512+A256KW", "PBES2 with HMAC SHA-512 and A256KW wrapping")
            .build();

    public static final BiMap<String, String> CONTENT_ENCRYPTION_ALGORITHMS = ImmutableBiMap.<String, String>builder()
            .put("A128CBC-HS256", "AES_128_CBC_HMAC_SHA_256 authenticated encryption algorithm")
            .put("A192CBC-HS384", "AES_192_CBC_HMAC_SHA_384 authenticated encryption algorithm")
            .put("A256CBC-HS512", "AES_256_CBC_HMAC_SHA_512 authenticated encryption algorithm")
            .put("A128GCM", "AES GCM using 128 bit key")
            .put("A192GCM", "AES GCM using 192 bit key")
            .put("A256GCM", "AES GCM using 256 bit key")
            .build();

    public static final BiMap<String, String> PUBLIC_KEY_USE = ImmutableBiMap.<String, String>builder()
            .put(Use.ENCRYPTION, "Encryption")
            .put(Use.SIGNATURE, "Signature")
            .build();

    public static final List<String> SIGNATURE_KEY_TYPES = ImmutableList.of("JSON Web Key", "JSON Web Key Set");

    public static final List<String> ENCRYPTION_KEY_TYPES = ImmutableList.of("Certificate", "JSON Web Key", "JSON Web Key Set");

    public static final String HEADERS_USE_DEFAULT = "Use Generated Header";
    public static final String HEADERS_MERGE = "Merge to Generated Header";
    public static final String HEADERS_REPLACE = "Replace Generated Header";
    public static final List<String> HEADER_ACTION = ImmutableList.of(HEADERS_USE_DEFAULT, HEADERS_MERGE, HEADERS_REPLACE);

    public static final String VALIDATION_NONE = "None";
    public static final String VALIDATION_USING_SECRET = "Using Secret";
    public static final String VALIDATION_USING_PK = "Using Recipient Key From List";
    public static final String VALIDATION_USING_CV = "Using Recipient Key From Context Variable";

    public static final List<String> VALIDATION_TYPE = ImmutableList.of(VALIDATION_NONE, VALIDATION_USING_SECRET, VALIDATION_USING_PK, VALIDATION_USING_CV);

    public static final int SOURCE_SECRET = 0;
    public static final int SOURCE_PK = 1;
    public static final int SOURCE_CV = 2;
}
