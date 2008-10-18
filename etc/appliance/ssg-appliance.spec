Summary: SecureSpan Gateway, Copyright Layer 7 Technologies 2003-2008
Name: ssg-appliance
Version: 0.0
Release: 0
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
%ifarch i386
#set innodb data file values for i386 / VM builds
sed -i -e "s/^\(innodb_data_file_path=ibdata\):[^:]*:/\1:100M:/" %{buildroot}/etc/my.cnf.ssg
sed -i -e "s/:autoextend:max:.*$/:autoextend:max:3072M/" %{buildroot}/etc/my.cnf.ssg
%endif

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
%attr(0555,layer7,layer7) /opt/SecureSpan/Gateway/runtime/etc/profile.d/*.sh

#System Config Wizard
%defattr(0444,layer7,layer7,0775)
%dir /opt/SecureSpan/Appliance/sysconfig
/opt/SecureSpan/Appliance/sysconfig/configfiles
/opt/SecureSpan/Appliance/sysconfig/lib
/opt/SecureSpan/Appliance/sysconfig/*.jar
# this script does not need to be executable
/opt/SecureSpan/Appliance/sysconfig/ssg_sys_config.pl
%attr(0755,layer7,layer7) /opt/SecureSpan/Appliance/sysconfig/*.sh
%defattr(0644,layer7,layer7,0775)
/opt/SecureSpan/Appliance/sysconfig/*.properties

# SSG config user files
%attr(0664,ssgconfig,ssgconfig) /home/ssgconfig/.bash_profile

# Appliance migration configuration
%attr(0644,layer7,layer7) /opt/SecureSpan/Gateway/config/migration/cfg/grandmaster_flash

%pre

grep -q ^gateway: /etc/group || groupadd gateway
grep -q ^pkcs11: /etc/group || groupadd pkcs11

# If user gateway already exists ensure group membership is ok, if it doesn't exist add it
grep -qvL ^gateway: /etc/passwd || usermod -g gateway -G 'pkcs11' gateway
grep -q   ^gateway: /etc/passwd || useradd -g gateway -G 'pkcs11' gateway

# If user layer7 already exists ensure group membership is ok, if it doesn't exist add it
grep -qvL ^layer7: /etc/passwd || usermod -g layer7 -G 'pkcs11' layer7
grep -q   ^layer7: /etc/passwd || useradd -g layer7 -G 'pkcs11' layer7

grep -q ^ssgconfig: /etc/group || groupadd ssgconfig
if [ -n "`grep ^ssgconfig: /etc/passwd`" ]; then
    #user ssgconfig already exists, but needs it's group membership modified
    usermod -G '' -g ssgconfig ssgconfig
else
    useradd -g ssgconfig ssgconfig

    # GEN001880
    # Update the permissions on ssgconfig's initialization files
    if [ -e /home/ssgconfig/.bash_logout ]; then
      chmod 740 /home/ssgconfig/.bash_logout
    fi
    if [ -e /home/ssgconfig/.bash_profile ]; then
      chmod 740 /home/ssgconfig/.bash_profile
    fi
    if [ -e /home/ssgconfig/.bashrc ]; then
      chmod 740 /home/ssgconfig/.bashrc
    fi
fi

SSGCONFIGENTRY=`grep ^ssgconfig /etc/sudoers`
if [ -n "${SSGCONFIGENTRY}" ]; then
    #user already exists in the sudoers file but since the paths may have changed we'll remove everything and reset
    perl -pi.bak -e 's/^ssgconfig.*$//gs' /etc/sudoers
fi

SSPANENTRY=`grep ^layer7 /etc/sudoers`
if [ -n "${SSPANENTRY}" ]; then
    #user already exists in the sudoers file but since the paths may have changed we'll remove everything and reset
    perl -pi.bak -e 's/^layer7.*$//gs' /etc/sudoers
fi

GATEWAYCONFIGENTRY=`grep ^gateway /etc/sudoers`
if [ -n "${GATEWAYCONFIGENTRY}" ]; then
    #user already exists in the sudoers file but since the paths have changed we'll remove everything and reset
    perl -pi.bak -e 's/^gateway.*$//gs' /etc/sudoers
fi

# The ssgconfig user is allowed to reboot the system, even when not at the console
# The ssgconfig user can run system and software configuration as layer7 user
echo "ssgconfig ALL = NOPASSWD: /sbin/reboot" >> /etc/sudoers
echo "ssgconfig ALL = (layer7) NOPASSWD: /opt/SecureSpan/Appliance/sysconfig/systemconfig.sh" >> /etc/sudoers
echo "ssgconfig ALL = (layer7) NOPASSWD: /opt/SecureSpan/Gateway/config/ssgconfig.sh" >> /etc/sudoers
echo "ssgconfig ALL = NOPASSWD: /opt/SecureSpan/Appliance/libexec/masterkey-manage.pl" >> /etc/sudoers
echo "ssgconfig ALL = NOPASSWD: /sbin/chkconfig ssem on, /sbin/chkconfig ssem off" >> /etc/sudoers

# The layer7 user is allowed to run the sca stuff without having to enter a password
echo "layer7 ALL = NOPASSWD: /opt/sun/sca6000/bin/scakiod_load" >> /etc/sudoers
echo "layer7 ALL = NOPASSWD: /opt/sun/sca6000/sbin/scadiag" >> /etc/sudoers
echo "layer7 ALL = NOPASSWD: /opt/SecureSpan/Appliance/libexec/" >> /etc/sudoers
echo "layer7 ALL = (gateway) NOPASSWD: /opt/SecureSpan/Appliance/libexec/" >> /etc/sudoers

# The gateway user is allowed to run the sca stuff without having to enter a password
# The gateway user is allowed to update the firewall configuration
echo "gateway ALL = NOPASSWD: /opt/sun/sca6000/bin/scakiod_load" >> /etc/sudoers
echo "gateway ALL = NOPASSWD: /opt/SecureSpan/Appliance/libexec/update_firewall" >> /etc/sudoers

REBOOTPARAM=`grep kernel.panic /etc/sysctl.conf`
if [ "${REBOOTPARAM}" ]; then
	echo -n ""
	# its got the panic time in there already"
else
	echo "# kernel panic will reboot in 10 seconds " >> /etc/sysctl.conf
	echo "kernel.panic = 10" >> /etc/sysctl.conf
fi

# fix file limits
LIMITS=`egrep -e \^\*\.\*soft\.\*nofile\.\*4096\$ /etc/security/limits.conf`
if [ -z "${LIMITS}" ]; then
	echo "# Layer 7 Limits"  >> /etc/security/limits.conf
	echo "*               soft    nproc   2047"  >> /etc/security/limits.conf
	echo "*               hard    nproc   16384"  >> /etc/security/limits.conf
	echo "*               soft    nofile  4096"  >> /etc/security/limits.conf
	echo "*               hard    nofile  63536"  >> /etc/security/limits.conf
	# 4096 files open and stuff
fi

# fix the getty

GETTYS=`grep ^s0:2345:respawn:/sbin/agetty /etc/inittab`
if [ -z "${GETTYS}" ]; then
	echo 	's0:2345:respawn:/sbin/agetty -L 9600 ttyS0 vt100' >> /etc/inittab
	echo 	'ttyS0' >> /etc/securetty
fi

CONNTRACK=`grep "options ip_conntrack" /etc/modprobe.conf`
if [ -z "${CONNTRACK}" ]; then
	# add in larger hash size. final conntrack size will be 8* hashsize
	# This allows larger number of in-flight connections
	echo "options ip_conntrack hashsize=65536" >> /etc/modprobe.conf
fi


%post

# After above item has executed, on first install only
# we need to set password for ssgconfig and pre-expire it
# $1 equals what for first install?

if [ "$1" = "1" ] ; then
  # $1 is 1 on first install, not for upgrade
  echo "7layer" | passwd ssgconfig --stdin >/dev/null
  chage -M 365 ssgconfig
  chage -d 0   ssgconfig
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

    SSPANENTRY=`grep ^layer7 /etc/sudoers`
    if [ -n "${SSPANENTRY}" ]; then
        #remove the sudoers entry for layer7
        perl -pi.bak -e 's/^layer7.*$//gs' /etc/sudoers
    fi

    GATEWAYENTRY=`grep ^gateway /etc/sudoers`
    if [ -n "${GATEWAYENTRY}" ]; then
        #remove the sudoers entry for gateway
        perl -pi.bak -e 's/^gateway.*$//gs' /etc/sudoers
    fi

    GETTYS=`grep ^s0:2345:respawn:/sbin/agetty /etc/inittab`
	if [ -n "${GETTYS}" ]; then
		perl -pi.bak -e 's/^s0.*agetty.*//' /etc/inittab
		perl -pi.bak -e 's/ttyS0//' /etc/securetty
	fi

    chkconfig --del ssg-dbstatus
    chkconfig --del ssgsysconfig
    chkconfig --del ssg
fi
