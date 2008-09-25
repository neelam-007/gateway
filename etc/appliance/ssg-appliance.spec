Summary: SecureSpan Gateway, Copyright Layer 7 Technologies 2003-2008
Name: ssg-appliance
Version: 0.0
Release: 1
Group: Applications/Internet
License: Commercial
URL: http://www.layer7tech.com
Vendor: Layer 7 Technologies
Packager: Layer 7 Technologies, <support@layer7tech.com>
Source0: ssg-appliance.tar.gz
Source1: jdk.tar.gz
BuildRoot: %{_builddir}/%{name}-%{version}
Requires: ssg >= %{version}

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

[ ! -e %{buildroot}/jdk/db ] || rm -rf %{buildroot}/jdk/db
[ ! -e %{buildroot}/jdk/demo ] || rm -rf %{buildroot}/jdk/demo
[ ! -e %{buildroot}/jdk/sample ] || rm -rf %{buildroot}/jdk/sample
[ ! -e %{buildroot}/jdk/man ] || rm -rf %{buildroot}/jdk/man
[ ! -e %{buildroot}/jdk/jre/.systemPrefs ] || rm -rf %{buildroot}/jdk/jre/.systemPrefs
[ ! -e %{buildroot}/jdk/jre/javaws ] || rm -rf %{buildroot}/jdk/jre/javaws
[ ! -e %{buildroot}/jdk/jre/plugin ] || rm -rf %{buildroot}/jdk/jre/plugin
[ ! -e %{buildroot}/jdk/jre/CHANGES ] || rm -f %{buildroot}/jdk/jre/CHANGES
[ ! -e %{buildroot}/jdk/jre/lib/deploy ] || rm -rf %{buildroot}/jdk/jre/lib/deploy
[ ! -e %{buildroot}/jdk/jre/lib/desktop ] || rm -rf %{buildroot}/jdk/jre/lib/desktop
# Ensure that the libs are not executable, if they are then their dependencies are required by this rpm.
chmod -R '-x+X' %{buildroot}/jdk/jre/lib
mv %{buildroot}/jdk %{buildroot}/opt/SecureSpan/JDK

%build

%files
# Root owned OS components
%defattr(0755,root,root)
/etc/init.d/ssg
/etc/init.d/ssgsysconfig
/etc/init.d/ssg-dbstatus
/etc/init.d/tcp_tune

# Config components, owned by root
%defattr(0644,root,root)
%config(noreplace) /etc/sysconfig/iptables
%defattr(0644,root,sys)
/etc/snmp/snmpd.conf_example
/etc/my.cnf.ssg

# SecureSpan base
%defattr(0644,root,root,0755)
%dir /opt/SecureSpan

# SecureSpan JDK
%dir /opt/SecureSpan/JDK
/opt/SecureSpan/JDK/COPYRIGHT
/opt/SecureSpan/JDK/LICENSE
/opt/SecureSpan/JDK/README.html
/opt/SecureSpan/JDK/THIRDPARTYLICENSEREADME.txt
%attr(0755,root,root) /opt/SecureSpan/JDK/bin
/opt/SecureSpan/JDK/include
%dir /opt/SecureSpan/JDK/jre
/opt/SecureSpan/JDK/jre/COPYRIGHT
/opt/SecureSpan/JDK/jre/LICENSE
/opt/SecureSpan/JDK/jre/README
/opt/SecureSpan/JDK/jre/THIRDPARTYLICENSEREADME.txt
/opt/SecureSpan/JDK/jre/Welcome.html
%attr(0755,root,root) /opt/SecureSpan/JDK/jre/bin
/opt/SecureSpan/JDK/jre/lib
/opt/SecureSpan/JDK/lib

# Main appliance tree, owned by root
%defattr(0644,root,root,0755)
%dir /opt/SecureSpan/Appliance
/opt/SecureSpan/Appliance/etc
/opt/SecureSpan/Appliance/var
%attr(0755,root,root) /opt/SecureSpan/Appliance/bin
%attr(0755,root,root) /opt/SecureSpan/Appliance/libexec

# Extra ssg files
%attr(0775,gateway,gateway) %dir /opt/SecureSpan/Gateway/Nodes/default/etc/profile.d
%attr(0775,gateway,gateway) /opt/SecureSpan/Gateway/Nodes/default/etc/profile.d/*.sh
%attr(0644,gateway,gateway) /opt/SecureSpan/Gateway/Nodes/default/bin/samples/fix_banner.sh

#System Config Wizard
%defattr(0664,gateway,gateway,0775)
%dir /opt/SecureSpan/Appliance/sysconfigwizard
/opt/SecureSpan/Appliance/sysconfigwizard/configfiles
/opt/SecureSpan/Appliance/sysconfigwizard/lib
/opt/SecureSpan/Appliance/sysconfigwizard/*.jar
/opt/SecureSpan/Appliance/sysconfigwizard/*.properties
# this script does not need to be executable
/opt/SecureSpan/Appliance/sysconfigwizard/ssg_sys_config.pl
%attr(0755,gateway,gateway) /opt/SecureSpan/Appliance/sysconfigwizard/*.sh

%attr(0664,ssgconfig,gateway) /home/ssgconfig/.bash_profile
/opt/SecureSpan/Gateway/migration/cfg

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
echo "ssgconfig    ALL = NOPASSWD: /opt/SecureSpan/Appliance/libexec/" >> /etc/sudoers
echo "ssgconfig    ALL = NOPASSWD: /opt/sun/sca6000/sbin/scadiag" >> /etc/sudoers

GATEWAYCONFIGENTRY=`grep ^gateway /etc/sudoers`
if [ -n "${GATEWAYCONFIGENTRY}" ]; then
    #user already exists in the sudoers file but since the paths have changed we'll remove everything and reset
    perl -pi.bak -e 's/^gateway.*$//gs' /etc/sudoers
fi

#the gateway user is allowed to run the sca stuff without having to enter a password
echo "gateway    ALL = NOPASSWD: /opt/sun/sca6000/bin/scakiod_load" >> /etc/sudoers
echo "gateway    ALL = NOPASSWD: /opt/SecureSpan/Appliance/libexec/" >> /etc/sudoers

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
	echo 	's0:2345:respawn:/sbin/agetty -L 9600 ttyS0 vt100' >> /etc/inittab
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
cat > /opt/SecureSpan/Gateway/Nodes/default/etc/profile.d/java.sh <<-EOF
SSG_JAVA_HOME="/opt/SecureSpan/JDK"
export SSG_JAVA_HOME
EOF

if [ -e '/opt/SecureSpan/Gateway/Nodes/default/etc/profile.d/output_redirection.sh' ]; then
    sed -i -e 's/^export LOG_REDIRECTION_OPERATOR=">"/export LOG_REDIRECTION_OPERATOR="|"/' /opt/SecureSpan/Gateway/Nodes/default/etc/profile.d/output_redirection.sh
    sed -i -e 's/^export LOG_REDIRECTION_DEST="\/dev\/null"/export LOG_REDIRECTION_DEST="logger -t SSG-default_"/' /opt/SecureSpan/Gateway/Nodes/default/etc/profile.d/output_redirection.sh
fi

# Change issue. This may move to a layer7-release file

echo "Layer 7 SecureSpan(tm) Gateway v%{version}" >/etc/issue
echo "Kernel \r on an \m" >>/etc/issue
#add the ssg and the configuration service to chkconfig if they are not already there
/sbin/chkconfig --add ssg
/sbin/chkconfig --add ssgsysconfig
/sbin/chkconfig --add ssg-dbstatus
/sbin/chkconfig ssg on

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

