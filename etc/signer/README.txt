This folder contains:

1. Server Module .aar or .jar files and Solution Kit Archive .skar files signing certs for our 4 teams:
** gatewayKeyStore.p12       -- Gateway         (type: PKCS#12; pass: 7layer)
** magKeyStore.p12           -- MAG             (type: PKCS#12; pass: 7layer)
** matterhornKeyStore.p12    -- Matterhorn      (type: PKCS#12; pass: 7layer)
** portalKeyStore.p12        -- Hybrid Portal   (type: PKCS#12; pass: 7layer)

2. The Gateway trusted signers keystore file, holding the public key of all trusted signers above:
** trusted_signers  -- type: JKS, pass: changeit

3. Batch and Shell scripts for running the SkarSigner.jar (for usage help see SkarSigner page in wiki):
** skar_signer.bat  -- Windows
** skar_signer.sh   -- Mac/Linux/Unix