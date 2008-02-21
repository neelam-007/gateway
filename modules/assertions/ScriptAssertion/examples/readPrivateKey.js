var keyAlias = "myclientcertalias";
var newKeystorePass = new Packages.java.lang.String("7layer").toCharArray();

var ksm = appContext.getBean("ssgKeyStoreManager");
var entry = ksm.lookupKeyByKeyAlias(keyAlias, -1);
var chain = entry.getCertificateChain();
var privateKey = entry.getPrivateKey();

var ks = Packages.java.security.KeyStore.getInstance("PKCS12");
ks.load(null, newKeystorePass);
ks.setKeyEntry(keyAlias, privateKey, newKeystorePass, chain);
var baos = new Packages.java.io.ByteArrayOutputStream();
ks.store(baos, newKeystorePass);
var bais = new Packages.java.io.ByteArrayInputStream(baos.toByteArray());

var response = policyContext.getResponse();
var sm = new Packages.com.l7tech.common.mime.ByteArrayStashManager();
var ctype = Packages.com.l7tech.common.mime.ContentTypeHeader.parseValue("application/x-pkcs12");
response.initialize(sm, ctype, bais);

true;