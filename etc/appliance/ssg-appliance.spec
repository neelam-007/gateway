Summary: SecureSpan Gateway, Copyright Layer 7 Technologies 2003-2007
Name: ssg-appliance
Version: 4.3
Release: 1
Group: Applications/Internet
License: Commercial
URL: http://www.layer7tech.com
Vendor: Layer 7 Technologies
Packager: Layer 7 Technologies, <support@layer7tech.com>
source0: ssg-appliance.tar.gz
source1: jdk.tar.gz
buildroot: %{_builddir}/%{name}-%{version}
requires: ssg >= 4.3

# Prevents rpm build from erroring and halting
#%undefine       __check_files

%description
SecureSpan Gateway Appliance Add-On Package

%clean
rm -fr %{buildroot}

%prep
rm -fr %{buildroot}

%setup -qc %{buildroot}
%setup -qDTa 1 %{buildroot}
rm -rf %{buildroot}/ssg/jdk
mv %{buildroot}/jdk %{buildroot}/ssg/
[ ! -e %{buildroot}/ssg/jdk/db ] || rm -rf %{buildroot}/ssg/jdk/db
[ ! -e %{buildroot}/ssg/jdk/demo ] || rm -rf %{buildroot}/ssg/jdk/demo
[ ! -e %{buildroot}/ssg/jdk/sample ] || rm -rf %{buildroot}/ssg/jdk/sample
[ ! -e %{buildroot}/ssg/jdk/man ] || rm -rf %{buildroot}/ssg/jdk/man
[ ! -e %{buildroot}/ssg/jdk/jre/.systemPrefs ] || rm -rf %{buildroot}/ssg/jdk/jre/.systemPrefs
[ ! -e %{buildroot}/ssg/jdk/jre/javaws ] || rm -rf %{buildroot}/ssg/jdk/jre/javaws
[ ! -e %{buildroot}/ssg/jdk/jre/plugin ] || rm -rf %{buildroot}/ssg/jdk/jre/plugin
[ ! -e %{buildroot}/ssg/jdk/jre/CHANGES ] || rm -f %{buildroot}/ssg/jdk/jre/CHANGES
[ ! -e %{buildroot}/ssg/jdk/jre/lib/deploy ] || rm -rf %{buildroot}/ssg/jdk/jre/lib/deploy
[ ! -e %{buildroot}/ssg/jdk/jre/lib/desktop ] || rm -rf %{buildroot}/ssg/jdk/jre/lib/desktop
# Ensure that the libs are not executable, if they are then their dependencies are required by this rpm.
chmod -R '-x+X' %{buildroot}/ssg/jdk/jre/lib

%build
mkdir %{buildroot}/etc/
mkdir %{buildroot}/etc/snmp/
mkdir %{buildroot}/etc/profile.d/
mkdir %{buildroot}/etc/init.d/
mkdir %{buildroot}/etc/sysconfig
mkdir %{buildroot}/etc/logrotate.d/
mkdir -p %{buildroot}/home/ssgconfig/
mkdir -p %{buildroot}/ssg/appliance/libexec/
mkdir -p %{buildroot}/ssg/appliance/bin
mkdir -p %{buildroot}/ssg/etc/profile.d/
mkdir -p %{buildroot}/ssg/migration/cfg/

mv %{buildroot}/ssg/bin/ssg-initd %{buildroot}/etc/init.d/ssg
mv %{buildroot}/ssg/bin/sysconfigscript-initd %{buildroot}/etc/init.d/ssgsysconfig
mv %{buildroot}/ssg/bin/ssg-dbstatus-initd %{buildroot}/etc/init.d/ssg-dbstatus
mv %{buildroot}/ssg/bin/my.cnf %{buildroot}/etc/my.cnf.ssg
mv %{buildroot}/ssg/bin/iptables %{buildroot}/etc/sysconfig/iptables
mv %{buildroot}/ssg/bin/appliancedefs.sh %{buildroot}/ssg/etc/profile.d/
mv %{buildroot}/ssg/bin/tarari.sh %{buildroot}/ssg/etc/profile.d/
mv %{buildroot}/ssg/bin/tcp_tune.sh %{buildroot}/etc/init.d/tcp_tune
mv %{buildroot}/ssg/bin/snmpd.conf %{buildroot}/etc/snmp/snmpd.conf_example
mv %{buildroot}/ssg/bin/configuser_bashrc %{buildroot}/home/ssgconfig/.bashrc
mv %{buildroot}/ssg/bin/pkcs11_linux.cfg %{buildroot}/ssg/appliance/pkcs11.cfg
mv %{buildroot}/ssg/bin/* %{buildroot}/ssg/appliance/bin/
mv %{buildroot}/ssg/libexec/* %{buildroot}/ssg/appliance/libexec
mv %{buildroot}/ssg/appliance/bin/grandmaster_flash %{buildroot}/ssg/migration/cfg/

# Root war is redundant
rm -f %{buildroot}/ssg/dist/*
# so is the windows mysql config
rm -f %{buildroot}/ssg/bin/my.ini
# tarari rpm has this
rm -f %{buildroot}/ssg/bin/tarari-initd

chmod 755 %{buildroot}/etc/init.d/*
chmod 755 %{buildroot}/ssg/appliance/libexec
chmod 711 %{buildroot}/ssg/appliance/libexec/*

%files
# Root owned OS components
%defattr(0755,root,root)
/etc/init.d/ssg
/etc/init.d/ssgsysconfig
/etc/init.d/ssg-dbstatus
/etc/init.d/tcp_tune

%config(noreplace) /etc/sysconfig/iptables
%defattr(0644,root,root)
/etc/snmp/snmpd.conf_example
# Config components, owned by root
%config(noreplace) /etc/my.cnf.ssg

# Main tree, owned by gateway
%defattr(0644,gateway,gateway,0755)
%dir /ssg
/ssg/appliance
/ssg/appliance/pkcs11.cfg
%attr(0755,root,root) /ssg/appliance/libexec
%attr(0755,root,root) /ssg/appliance/libexec/*

# Ssg bin
%dir /ssg/appliance/bin
%attr(0755,gateway,gateway) /ssg/appliance/bin/*.pl
%attr(0755,gateway,gateway) /ssg/appliance/bin/*.sh

%attr(0775,gateway,gateway) %dir /ssg/etc/profile.d
%attr(0775,gateway,gateway) /ssg/etc/profile.d/appliancedefs.sh
%attr(0775,gateway,gateway) /ssg/etc/profile.d/tarari.sh

# JDK
%dir /ssg/jdk
/ssg/jdk/COPYRIGHT
/ssg/jdk/LICENSE
/ssg/jdk/README.html
/ssg/jdk/THIRDPARTYLICENSEREADME.txt
%attr(0755,gateway,gateway) /ssg/jdk/bin
/ssg/jdk/include
%dir /ssg/jdk/jre
/ssg/jdk/jre/COPYRIGHT
/ssg/jdk/jre/LICENSE
/ssg/jdk/jre/README
/ssg/jdk/jre/THIRDPARTYLICENSEREADME.txt
/ssg/jdk/jre/Welcome.html
%attr(0755,gateway,gateway) /ssg/jdk/jre/bin
/ssg/jdk/jre/lib
%config(noreplace) /ssg/jdk/jre/lib/security/java.security
%attr(0775,gateway,gateway) /ssg/jdk/jre/lib/security/

/ssg/jdk/lib

#System Config Wizard
%defattr(0755,gateway,gateway,0664)
%dir /ssg/sysconfigwizard
%dir /ssg/sysconfigwizard/configfiles
/ssg/sysconfigwizard/lib
/ssg/sysconfigwizard/*.jar
/ssg/sysconfigwizard/*.properties
# this script does not need to be executable
/ssg/sysconfigwizard/ssg_sys_config.pl
%attr(0775,gateway,gateway) /ssg/sysconfigwizard/
%attr(0775,gateway,gateway) /ssg/sysconfigwizard/configfiles
%attr(0755,gateway,gateway) /ssg/sysconfigwizard/*.sh

%attr(0664,ssgconfig,gateway) /home/ssgconfig/.bashrc
%attr(0775,gateway,gateway) /ssg/migration/cfg/grandmaster_flash

%pre
if [ `grep ^gateway: /etc/group` ]; then
	echo -n ""
else
	groupadd gateway
fi

if [ `grep ^pkcs11: /etc/group` ]; then
	echo -n ""
else
	groupadd pkcs11
fi

if [ `grep ^gateway: /etc/passwd` ]; then
    #user gateway already exists, but needs it's group membership modified
    usermod -G gateway,pkcs11 gateway
else
    useradd -G gateway,pkcs11 -g gateway gateway
fi

if [ `grep ^ssgconfig: /etc/passwd` ]; then
    #user ssgconfig already exists, but needs it's group membership modified
    usermod -G gateway,pkcs11 ssgconfig
else
    useradd -G gateway,pkcs11 -g gateway ssgconfig
fi

SSGCONFIGENTRY=`grep ^ssgconfig /etc/sudoers`
if [ -n "${SSGCONFIGENTRY}" ]; then
    #user already exists in the sudoers file but since the paths have changed we'll remove everything and reset
    perl -pi.bak -e 's/^ssgconfig.*$//gs' /etc/sudoers
fi

#the ssgconfig user is allowed to reboot the system, even when not at the console
echo "ssgconfig  ALL = NOPASSWD: /sbin/reboot" >> /etc/sudoers
#the ssgconfig user is allowed to run the sca stuff without having to enter a password
echo "ssgconfig    ALL = NOPASSWD: /opt/sun/sca6000/bin/scakiod_load" >> /etc/sudoers
echo "ssgconfig    ALL = NOPASSWD: /ssg/appliance/libexec/" >> /etc/sudoers
echo "ssgconfig    ALL = NOPASSWD: /opt/sun/sca6000/sbin/scadiag" >> /etc/sudoers

GATEWAYCONFIGENTRY=`grep ^gateway /etc/sudoers`
if [ -n "${GATEWAYCONFIGENTRY}" ]; then
    #user already exists in the sudoers file but since the paths have changed we'll remove everything and reset
    perl -pi.bak -e 's/^gateway.*$//gs' /etc/sudoers
fi

#the gateway user is allowed to run the sca stuff without having to enter a password
echo "gateway    ALL = NOPASSWD: /opt/sun/sca6000/bin/scakiod_load" >> /etc/sudoers
echo "gateway    ALL = NOPASSWD: /ssg/appliance/libexec/" >> /etc/sudoers

rebootparam=`grep kernel.panic /etc/sysctl.conf`

if [ "$rebootparam" ]; then
	echo -n ""
	# its got the panic time in there already"
else
	echo "# kernel panic will reboot in 10 seconds " >> /etc/sysctl.conf
	echo "kernel.panic = 10" >> /etc/sysctl.conf
fi

# fix file limits

limits=`egrep -e \^\*\.\*soft\.\*nofile\.\*4096\$ /etc/security/limits.conf`

if [ "$limits" ]; then
	echo -n ""
	# already installed
else
	echo "# Layer 7 Limits"  >> /etc/security/limits.conf
	echo "*               soft    nproc   2047"  >> /etc/security/limits.conf
	echo "*               hard    nproc   16384"  >> /etc/security/limits.conf
	echo "*               soft    nofile  4096"  >> /etc/security/limits.conf
	echo "*               hard    nofile  63536"  >> /etc/security/limits.conf
	# 4096 files open and stuff
fi

# fix the getty

gettys=`grep ^s0:2345:respawn:/sbin/agetty /etc/inittab`

if [ "$gettys" ]; then
	echo -n ""
	# serial line agetty exists
else
	echo 	's0:2345:respawn:/sbin/agetty -L 115200 ttyS0 vt100' >> /etc/inittab
	echo 	'ttyS0' >> /etc/securetty
fi

connt=`grep "options ip_conntrack" /etc/modprobe.conf`

if [ "$connt" ]; then
	echo -n ""
	# connection tracking already set
else
	echo "options ip_conntrack hashsize=65536" >> /etc/modprobe.conf
	# add in larger hash size. final conntrack size will be 8* hashsize
	# This allows larger number of in-flight connections
fi


%post

#modify java.sh to use the appliance jdk
cat > /ssg/etc/profile.d/java.sh <<-EOF
SSG_JAVA_HOME="/ssg/jdk"
export SSG_JAVA_HOME
EOF

/ssg/configwizard/ssgconfig.sh -partitionMigrate &> /dev/null
# Change issue. This may move to a layer7-release file

echo "Layer 7 SecureSpan(tm) Gateway v4.3" >/etc/issue
echo "Kernel \r on an \m" >>/etc/issue
#add the ssg and the configuration service to chkconfig if they are not already there
/sbin/chkconfig --list ssg &>/dev/null
if [ ${?} -ne 0 ]; then
    # add service if not found
    /sbin/chkconfig --add ssg
fi

/sbin/chkconfig --list ssgsysconfig &>/dev/null
if [ ${?} -ne 0 ]; then
    # add service if not found
    /sbin/chkconfig --add ssgsysconfig
fi

/sbin/chkconfig --list ssg-dbstatus &>/dev/null
if [ ${?} -ne 0 ]; then
    # add service if not found
    /sbin/chkconfig --add ssg-dbstatus
fi

# After above item has executed, on first install only
# we need to set password for ssgconfig and pre-expire it
# $1 equals what for first install?

%preun
# Modifications to handle upgrades properly
if [ "$1" = "0" ] ; then
    SSGCONFIGENTRY=`grep ^ssgconfig /etc/sudoers`
    if [ -n "${SSGCONFIGENTRY}" ]; then
        #remove the sudoers entry for ssgconfig
        perl -pi.bak -e 's/^ssgconfig.*$//gs' /etc/sudoers
    fi

    GATEWAYENTRY=`grep ^gateway /etc/sudoers`
    if [ -n "${GATEWAYENTRY}" ]; then
        #remove the sudoers entry for gateway
        perl -pi.bak -e 's/^gateway.*$//gs' /etc/sudoers
    fi

    gettys=`grep ^s0:2345:respawn:/sbin/agetty /etc/inittab`

	if [ "$gettys" ]; then
		perl -pi.bak -e 's/^s0.*agetty.*//' /etc/inittab
		perl -pi.bak -e 's/ttyS0//' /etc/securetty
	fi

    chkconfig --del ssg-dbstatus
    chkconfig --del ssgsysconfig
    chkconfig --del ssg
fi

