#!/bin/bash

#open cryptoki is built incorrectly and expects a file that it put somewhere else. Make the dir and symlink to the right place.
mkdir -p /usr/lib/pkcs11/methods/
ln -s /usr/sbin/pkcs11_startup /usr/lib/pkcs11/methods/pkcs11_startup

#link in the startup of pkcsslotd at runlevel 3
ln -s /etc/init.d/pkcsslotd /etc/rc.d/rc3.d/S04pkcsslotd

#/opt/sun/sca6000/sbin/pkcs11_startup doesn't exist.
ln -s /opt/sun/sca6000/sbin/pkcs11_startup.64 /opt/sun/sca6000/sbin/pkcs11_startup

#/usr/local/sbin/pkcsslotd doesn't exist
ln -s /usr/sbin/pkcsslotd /usr/local/sbin/pkcsslotd
