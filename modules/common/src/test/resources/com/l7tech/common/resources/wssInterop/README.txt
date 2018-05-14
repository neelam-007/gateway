This README contains notes on how the wssInterop keys and certs were generated

Keystore password: password

=================================
OASIS Root
=================================
Create self-signed root keystore
keytool -genkeypair -keystore root.pfx -validity 36500 -alias oasis_root -keysize 2048 -keyalg RSA -sigalg SHA1withRSA -storetype pkcs12 -ext KeyUsage=keyCertSign,cRLSign -ext BasicConstraints=ca:true -dname "CN=OASIS Interop Test Root, O=OASIS"

Export root cert in PEM format
openssl pkcs12 -in root.pfx -out root.crt.pem -clcerts -nokeys

Export root private key in PEM format
openssl pkcs12 -in root.pfx -out root.key.pem -nocerts -nodes

Convert root cert from PEM to DER
openssl x509 -outform der -in root.crt.pem -out exported_root.crt
=================================

=================================
OASIS CA
=================================
Create self-signed ca keystore
keytool -genkeypair -keystore ca.pfx -validity 36500 -alias oasis_ca -keysize 2048 -keyalg RSA -sigalg SHA1withRSA -storetype pkcs12 -ext KeyUsage=keyCertSign,cRLSign -ext BasicConstraints=ca:true,PathLen:0 -dname "CN=OASIS Interop Test CA, O=OASIS"

Create CSR
keytool -certreq -keyalg RSA -alias oasis_ca -file ca_csr.pem -keystore ca.pfx

Generate signed ca cert using root private key
openssl x509 -req -CA root.crt.pem -CAkey root.key.pem -in ca_csr.pem -out ca_chain.crt.pem -days 36500 -CAcreateserial -sha1 -extfile  /cygdrive/c/OpenSSL-Win64/bin/cnf/openssl.cnf -extensions oasis_ca

Import root cert to keystore
keytool -import -keystore ca.pfx -file root.crt.pem -alias oasis_root

Import signed ca cert to keystore
keytool -import -keystore ca.pfx -file ca_chain.crt.pem -alias oasis_ca

Export ca cert in PEM format
openssl pkcs12 -in ca.pfx -out ca.crt.pem -clcerts -nokeys

Export ca private key in PEM format
openssl pkcs12 -in ca.pfx -out ca.key.pem -nocerts -nodes

Convert ca cert from PEM to DER
openssl x509 -outform der -in ca.crt.pem -out exported_ca.crt
=================================

=================================
Bob
=================================
Create self-signed bob keystore
keytool -genkeypair -keystore bob.pfx -validity 36500 -alias bob -keysize 1024 -keyalg RSA -sigalg SHA1withRSA -storetype pkcs12 -ext KeyUsage=digitalSignature,keyEncipherment,dataEncipherment -dname "CN=Bob, OU=OASIS Interop Test Cert, O=OASIS"

Create CSR
keytool -certreq -keyalg RSA -alias bob -file bob_csr.pem -keystore bob.pfx

Generate signed bob cert using ca private key
openssl x509 -req -CA ca.crt.pem -CAkey ca.key.pem -in bob_csr.pem -out bob_chain.crt.pem -days 36500 -CAcreateserial -sha1 -extfile  /cygdrive/c/OpenSSL-Win64/bin/cnf/openssl.cnf -extensions oasis_user

Import root cert to keystore
keytool -import -keystore bob.pfx -file root.crt.pem -alias oasis_root

Import signed ca cert to keystore
keytool -import -keystore bob.pfx -file ca_chain.crt.pem -alias oasis_ca

Import signed bob cert to keystore
keytool -import -keystore bob.pfx -file bob_chain.crt.pem -alias bob

Export bob cert in PEM format
openssl pkcs12 -in bob.pfx -out bob.crt.pem -clcerts -nokeys

Convert bob cert from PEM to DER
openssl x509 -outform der -in bob.crt.pem -out exported_bob.crt
=================================

=================================
Franco (Bob copy but with tomcat alias)
=================================
Change bob alias in keystore to tomcat
keytool -changealias -keystore franco.ks -alias bob -destalias tomcat

=================================
Alice
=================================
Create self-signed alice keystore
keytool -genkeypair -keystore alice.pfx -validity 36500 -alias alice -keysize 1024 -keyalg RSA -sigalg SHA1withRSA -storetype pkcs12 -ext KeyUsage=digitalSignature,keyEncipherment,dataEncipherment -dname "CN=Alice, OU=OASIS Interop Test Cert, O=OASIS"

Create CSR
keytool -certreq -keyalg RSA -alias alice -file alice_csr.pem -keystore alice.pfx

Generate signed alice cert using ca private key
openssl x509 -req -CA ca.crt.pem -CAkey ca.key.pem -in alice_csr.pem -out alice_chain.crt.pem -days 36500 -CAcreateserial -sha1 -extfile  /cygdrive/c/OpenSSL-Win64/bin/cnf/openssl.cnf -extensions oasis_user

Import root cert to keystore
keytool -import -keystore alice.pfx -file root.crt.pem -alias oasis_root

Import signed ca cert to keystore
keytool -import -keystore alice.pfx -file ca_chain.crt.pem -alias oasis_ca

Import signed alice cert to keystore
keytool -import -keystore alice.pfx -file alice_chain.crt.pem -alias alice

Export alice cert in PEM format
openssl pkcs12 -in alice.pfx -out alice.crt.pem -clcerts -nokeys

Convert alice cert from PEM to DER
openssl x509 -outform der -in alice.crt.pem -out exported_alice.crt
=================================

=================================
WssIP
=================================
Create self-signed wssip keystore
keytool -genkeypair -keystore wssip.pfx -validity 36500 -alias wssip -keysize 1024 -keyalg RSA -sigalg SHA1withRSA -storetype pkcs12 -ext KeyUsage=digitalSignature,keyEncipherment,dataEncipherment -dname "CN=WssIP, OU=OASIS Interop Test Cert, O=OASIS"

Create CSR
keytool -certreq -keyalg RSA -alias wssip -file wssip_csr.pem -keystore wssip.pfx

Generate signed wssip cert using ca private key
openssl x509 -req -CA ca.crt.pem -CAkey ca.key.pem -in wssip_csr.pem -out wssip_chain.crt.pem -days 36500 -CAcreateserial -sha1 -extfile  /cygdrive/c/OpenSSL-Win64/bin/cnf/openssl.cnf -extensions oasis_user

Import root cert to keystore
keytool -import -keystore wssip.pfx -file root.crt.pem -alias oasis_root

Import signed ca cert to keystore
keytool -import -keystore wssip.pfx -file ca_chain.crt.pem -alias oasis_ca

Import signed wssip cert to keystore
keytool -import -keystore wssip.pfx -file wssip_chain.crt.pem -alias wssip

Export wssip cert in PEM format
openssl pkcs12 -in wssip.pfx -out wssip.crt.pem -clcerts -nokeys

Convert wssip cert from PEM to DER
openssl x509 -outform der -in wssip.crt.pem -out exported_wssip.crt
=================================

=================================
OpenSSL openssl.conf settings
=================================
The extensions parameter used in the generated signed cert step requires the following sections to be added to openssl.conf

[ oasis_ca ]
basicConstraints = CA:TRUE, pathlen:0
keyUsage = keyCertSign, cRLSign

[ oasis_user ]
keyUsage = digitalSignature, keyEncipherment, dataEncipherment