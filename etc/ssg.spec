Summary: Layer 7 Gateway, Copyright Layer 7 Technologies 2003-2012
Name: ssg
Version: 0.0
Release: 0
Group: Applications/Internet
License: Commercial
URL: http://www.layer7tech.com
Vendor: Layer 7 Technologies
Packager: Layer 7 Technologies, <support@layer7tech.com>
Source0: ssg.tar.gz
BuildRoot: %{_builddir}/%{name}-%{version}
Prefix: /opt/SecureSpan/Gateway

# Prevents rpm build from erroring and halting
#%undefine       __check_files

# Need these to build RHEL5 rpm from Fedora 11 / 12
%define _binary_filedigest_algorithm md5
%define _binary_payload w9.bzdio

%description
Layer 7 Gateway Software Package

%clean
rm -fr %{buildroot}

%prep
rm -fr %{buildroot}

%setup -qcn %{buildroot}

%build

%files
# Main tree, owned by root
%defattr(0644,root,root,0755)
%dir /opt/SecureSpan/Gateway
%dir /opt/SecureSpan/Controller

# Group writable config files
%defattr(0640,layer7,gateway,0750)
%dir /opt/SecureSpan/Gateway/node
%dir /opt/SecureSpan/Gateway/node/default
%dir /opt/SecureSpan/Gateway/node/default/etc
# runtime and runtime/etc must be world-searchable since runtime/etc/profile is underneath it
%defattr(0644,layer7,gateway,0755)
%dir /opt/SecureSpan/Gateway/runtime
%dir /opt/SecureSpan/Gateway/runtime/etc
# Conf dir must be sgid so config files created by layer7 are readable by gateway group
%defattr(0640,layer7,gateway,2750)
%dir /opt/SecureSpan/Gateway/node/default/etc/conf
%defattr(0640,layer7,gateway,0750)
%config(noreplace) /opt/SecureSpan/Gateway/node/default/etc/conf/*

# Group directories and files
%defattr(0444,layer7,layer7,0755)
/opt/SecureSpan/Gateway/runtime/etc/profile
/opt/SecureSpan/Gateway/runtime/etc/ssg.policy
/opt/SecureSpan/Gateway/runtime/modules

# The main Gateway jar
%defattr(0444,layer7,layer7,0755)
/opt/SecureSpan/Gateway/runtime/Gateway.jar

# Ssg bin
%defattr(0555,layer7,layer7,0755)
%dir /opt/SecureSpan/Gateway/runtime/bin
/opt/SecureSpan/Gateway/runtime/bin/*
%dir /opt/SecureSpan/Gateway/runtime/etc/profile.d
/opt/SecureSpan/Gateway/runtime/etc/profile.d/ssgnodedefs.sh
%config(noreplace) /opt/SecureSpan/Gateway/runtime/etc/profile.d/ssgruntimedefs.sh
/opt/SecureSpan/Gateway/runtime/etc/profile.d/ssg-utilities.sh
/opt/SecureSpan/Gateway/runtime/etc/profile.d/siteminder-env.sh
%config(noreplace) %attr(755,layer7,layer7) /opt/SecureSpan/Gateway/runtime/etc/profile.d/xlocaldefs.sh

# Other stuff
%defattr(0444,layer7,layer7,0755)
/opt/SecureSpan/Gateway/config/etc
/opt/SecureSpan/Gateway/runtime/web
/opt/SecureSpan/Gateway/runtime/lib

#var directory needs to be writable by layer7 group as well as gateway
%defattr(0644,gateway,layer7,0775)
/opt/SecureSpan/Gateway/node/default/var

#Configuration
%defattr(0644,layer7,layer7,0755)
%dir %attr(0755,root,root) /opt/SecureSpan/Gateway/config
%dir %attr(0755,layer7,layer7) /opt/SecureSpan/Gateway/config/logs
/opt/SecureSpan/Gateway/config/*.properties
%defattr(0444,layer7,layer7,0755)
/opt/SecureSpan/Gateway/config/*.jar
%defattr(0555,layer7,layer7,0755)
/opt/SecureSpan/Gateway/config/*.sh

%defattr(0644,layer7,layer7,0755)
%dir %attr(0755,root,root) /opt/SecureSpan/Gateway/config/backup
%attr(0755,root,root) /opt/SecureSpan/Gateway/config/backup/*.sh
/opt/SecureSpan/Gateway/config/backup/cfg/
/opt/SecureSpan/Gateway/config/backup/*.properties
/opt/SecureSpan/Gateway/config/backup/logs/
/opt/SecureSpan/Gateway/config/backup/images/
%defattr(0444,layer7,layer7,0755)
/opt/SecureSpan/Gateway/config/backup/*.jar

# Gateway process controller
%defattr(0640,layer7,layer7,0755)
%dir /opt/SecureSpan/Controller/var

%defattr(0444,layer7,layer7,0755)
/opt/SecureSpan/Controller/Controller.jar
%attr(0555,root,root) /opt/SecureSpan/Controller/bin
/opt/SecureSpan/Controller/lib
%dir %attr(0775,layer7,gateway) /opt/SecureSpan/Controller/etc
%config %attr(0770,layer7,gateway) /opt/SecureSpan/Controller/etc/conf
%attr(0770,layer7,gateway) /opt/SecureSpan/Controller/var/run
%attr(0770,layer7,gateway) /opt/SecureSpan/Controller/var/logs
%attr(0770,layer7,gateway) /opt/SecureSpan/Controller/var/patches

%pre
grep -q ^gateway: /etc/group || groupadd gateway
grep -q ^layer7: /etc/group || groupadd layer7

# nfast may already be present but add it just in case
grep -q ^nfast: /etc/group || groupadd nfast

# If gateway and layer7 users already exist then ensure their group membership is ok. If they don't exist add them.
if grep -q ^gateway: /etc/passwd; then
  usermod -g gateway -a -G 'nfast' gateway
else
  useradd -g gateway -G 'nfast' gateway
fi

if grep -q ^layer7: /etc/passwd; then
  usermod -g layer7 -G 'gateway,nfast' layer7
else
  useradd -g layer7 -G 'gateway,nfast' layer7
fi

# Chown any files that have been left behind by a previous installation
[ ! -d "${RPM_INSTALL_PREFIX0}/config" ] || chown -R layer7.layer7 "${RPM_INSTALL_PREFIX0}/config"
[ ! -d "${RPM_INSTALL_PREFIX0}/node/default/etc/conf" ] || chown -R layer7.gateway "${RPM_INSTALL_PREFIX0}/node/default/etc/conf"
[ ! -d "${RPM_INSTALL_PREFIX0}/node/default/var" ] || chown -R gateway.gateway "${RPM_INSTALL_PREFIX0}/node/default/var"

# hack to turn back ownership/permissions for what's needed in software
chown gateway:gateway /opt/SecureSpan/Controller/etc/host.properties 2>/dev/null
chmod 660 /opt/SecureSpan/Controller/etc/host.properties 2>/dev/null
chown gateway:gateway /opt/SecureSpan/Controller/etc/*.p12 2>/dev/null
chmod 660 /opt/SecureSpan/Controller/etc/*.p12 2>/dev/null

prev_gateway_uid=`find /opt/SecureSpan/Controller/ -nouser -printf "%u\n" 2>/dev/null | sort -n | head -1`
if [ -n "$prev_gateway_uid" ]; then
    find /opt/SecureSpan/Controller/ -user $prev_gateway_uid -exec chown gateway '{}' \;
    prev_layer7_uid=`find /opt/SecureSpan/Controller/ -nouser -printf "%u\n" | sort -n | grep -v $prev_gateway_uid | head -1`
    if [ -n "$prev_layer7_uid" ]; then
        find /opt/SecureSpan/Controller/ -user $prev_layer7_uid -exec chown layer7 '{}' \;
    fi
fi
prev_gateway_gid=`find /opt/SecureSpan/Controller/ -nogroup -printf "%g\n" 2>/dev/null | sort -n | head -1`
if [ -n "$prev_gateway_gid" ]; then
    find /opt/SecureSpan/Controller/ -group $prev_gateway_gid -exec chgrp gateway '{}' \;
    prev_layer7_gid=`find /opt/SecureSpan/Controller/ -nogroup -printf "%g\n" | sort -n | grep -v $prev_gateway_gid | head -1`
    if [ -n "$prev_layer7_gid" ]; then
        find /opt/SecureSpan/Controller/ -group $prev_layer7_gid -exec chgrp layer7 '{}' \;
    fi
fi

%post
if [ -d "/ssg" ] ; then
   sh "${RPM_INSTALL_PREFIX0}/runtime/bin/upgrade.sh" >> "${RPM_INSTALL_PREFIX0}/config/upgrade.log" 2>&1
fi

if [ -f /opt/SecureSpan/Gateway/config/config.log ]; then
 mv /opt/SecureSpan/Gateway/config/config.log /opt/SecureSpan/Gateway/config/logs/config.log 2>/dev/null
fi

find /opt/SecureSpan/Controller/var/logs/ -name "patch_cli_*" -not -user layer7 -exec chown layer7:layer7 '{}' \;

%preun
# Modifications to handle upgrades properly
if [ "$1" = "0" ] ; then
    # $1 is 0 on last uninstall, ie, package remove, not upgrade

    if grep -q '^gateway:' /etc/passwd; then userdel -r gateway; fi
    if grep -q '^layer7:' /etc/passwd; then userdel -r layer7; fi

    if grep -q ^gateway: /etc/group; then groupdel gateway; fi
    if grep -q ^layer7: /etc/group; then groupdel layer7; fi
fi
