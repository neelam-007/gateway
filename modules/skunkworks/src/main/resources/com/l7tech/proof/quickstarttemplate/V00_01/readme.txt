How to sign QuickStartTemplateAssertion.aar.
    prompt> cd <l7_workspace>/trunk/modules/skunkworks/src/main/resources/com/l7tech/proof/quickstarttemplate/V00_01
    prompt> SkarSigner-HEAD/skar_signer.sh  sign --storeFile "../../../../../../../../../../etc/signer/gatewayKeyStore.p12" --storePass "6fj1QDCbvjI.OBh1tzgR5MCLBQo72qH5gA" --keyPass "6fj1QDCbvjI.OBh1tzgR5MCLBQo72qH5gA" --fileToSign "../../../../../../../../../gateway/assertions/QuickStartTemplateAssertion/build/QuickStartTemplateAssertion.aar"

How to build quick start template proof of concept skar file (e.g. quickstarttemplate-proof-0.01.skar).
    prompt> cd <l7_workspace>/trunk/modules/skunkworks/src/main/resources/com/l7tech/proof/quickstarttemplate/V00_01
    prompt> ./build.sh