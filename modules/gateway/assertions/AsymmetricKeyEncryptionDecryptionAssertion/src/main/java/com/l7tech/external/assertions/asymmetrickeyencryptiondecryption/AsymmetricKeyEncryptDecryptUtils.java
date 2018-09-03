package com.l7tech.external.assertions.asymmetrickeyencryptiondecryption;

import com.l7tech.common.io.CertUtils;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.util.HexUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class AsymmetricKeyEncryptDecryptUtils {

	private AsymmetricKeyEncryptDecryptUtils() { }

	private static final String LINE_FEED_CHARACTER = "\n";
	private static final String CARRIAGE_RETURN_CHARACTER = "\r";
	private static final String X509_CERT_BEGIN_MARKER = "-----BEGIN CERTIFICATE-----";
	private static final String X509_CERT_END_MARKER = "-----END CERTIFICATE-----";
	private static final String PUBLIC_KEY_BEGIN_MARKER = "-----BEGIN PUBLIC KEY-----";
	private static final String PUBLIC_KEY_END_MARKER = "-----END PUBLIC KEY-----";
	private static final String RSA_PUBLIC_KEY_BEGIN_MARKER = "-----BEGIN RSA PUBLIC KEY-----";
	private static final String RSA_PUBLIC_KEY_END_MARKER = "-----END RSA PUBLIC KEY-----";
	private static final String PRIVATE_KEY_BEGIN_MARKER = "-----BEGIN PRIVATE KEY-----";
	private static final String PRIVATE_KEY_END_MARKER = "-----END PRIVATE KEY-----";
	private static final String RSA_PRIVATE_KEY_BEGIN_MARKER = "-----BEGIN RSA PRIVATE KEY-----";
	private static final String RSA_PRIVATE_KEY_END_MARKER = "-----END RSA PRIVATE KEY-----";


	public static PublicKey parsePublicKeyOrCertificate(String keyString) throws CertificateException,
			InvalidKeySpecException, NoSuchAlgorithmException, IOException {
		keyString = removePublicKeyBeginAndEndMarker(keyString);
		try {
			final String x509CertificateString = addX509BeginAndEndMarker(keyString);
			CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
			InputStream certificateInputStream = new ByteArrayInputStream(x509CertificateString.getBytes());
			X509Certificate x509Certificate = (X509Certificate) certFactory.generateCertificate(certificateInputStream);
			return x509Certificate.getPublicKey();
		} catch (CertificateException ce) {
			// try parsing as a encoded PublicKey
			final KeyFactory keyFactory = JceProvider.getInstance().getKeyFactory("RSA");
			final X509EncodedKeySpec keySpec = new X509EncodedKeySpec(HexUtils.decodeBase64(keyString));
			return keyFactory.generatePublic(keySpec);
		}
	}

	public static PrivateKey parsePrivateKey(String keyString) throws CertificateException, InvalidKeySpecException,
			NoSuchAlgorithmException, IOException {
		try {
			final String pkcs8KeyString = removePrivateKeyBeginAndEndMarker(keyString);
			// Try PKCS8 parsing
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(HexUtils.decodeBase64(pkcs8KeyString));
			return keyFactory.generatePrivate(spec);
		} catch (InvalidKeySpecException kse) {
			// try PKCS1 parsing
			return CertUtils.decodeKeyFromPEM(keyString);
		}
	}

	private static String removePrivateKeyBeginAndEndMarker(String keyString) {
		keyString = keyString.replace(LINE_FEED_CHARACTER, "");
		keyString = keyString.replace(CARRIAGE_RETURN_CHARACTER, "");
		keyString = keyString.replace(PRIVATE_KEY_BEGIN_MARKER, "");
		keyString = keyString.replace(PRIVATE_KEY_END_MARKER, "");
		keyString = keyString.replace(RSA_PRIVATE_KEY_BEGIN_MARKER, "");
		keyString = keyString.replace(RSA_PRIVATE_KEY_END_MARKER, "");
		return keyString;
	}

	private static String removePublicKeyBeginAndEndMarker(String keyString) {
		keyString = keyString.replace(LINE_FEED_CHARACTER, "");
		keyString = keyString.replace(CARRIAGE_RETURN_CHARACTER, "");
		keyString = keyString.replace(X509_CERT_BEGIN_MARKER, "");
		keyString = keyString.replace(X509_CERT_END_MARKER, "");
		keyString = keyString.replace(PUBLIC_KEY_BEGIN_MARKER, "");
		keyString = keyString.replace(PUBLIC_KEY_END_MARKER, "");
		keyString = keyString.replace(RSA_PUBLIC_KEY_BEGIN_MARKER, "");
		keyString = keyString.replace(RSA_PUBLIC_KEY_END_MARKER, "");
		return keyString;
	}

	private static String addX509BeginAndEndMarker(final String keyString) {
		final StringBuilder sb = new StringBuilder(X509_CERT_BEGIN_MARKER);
		sb.append(LINE_FEED_CHARACTER);
		sb.append(keyString);
		sb.append(LINE_FEED_CHARACTER);
		sb.append(X509_CERT_END_MARKER);
		return sb.toString();
	}
}
