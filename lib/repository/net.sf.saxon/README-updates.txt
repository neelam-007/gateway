The version of the saxon9ee jarfile in this directory has been modified by mlyons@layer7.com to strip the (expired)
jar signature in order to prevent the manage applet from locking up due to an expired signature warning dialog during
applet classloading (Bug #12893).

When updating Saxon to a newer version, please use jarsigner -verify -verbose to check whether the new version
includes an expired signature as well.  If so, please strip the signature before committing the new version
to avoid a regression of Bug #12893.
