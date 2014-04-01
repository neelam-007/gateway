Summary: Layer 7 Gateway, Copyright Layer 7 Technologies 2003-2012
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
Requires(pre): /usr/sbin/useradd /usr/sbin/groupadd /usr/sbin/usermod
Requires(preun): /usr/sbin/userdel /usr/sbin/groupdel
Requires: ssg >= %{version}
Prefix: /opt/SecureSpan/Appliance

# Prevents rpm build from erroring and halting
#%undefine       __check_files

# Need these to build RHEL5 rpm from Fedora 11 / 12
%define _binary_filedigest_algorithm md5
%define _binary_payload w9.bzdio

%description
Layer 7 Gateway Appliance Add-On Package

%clean
rm -fr %{buildroot}

%prep
rm -fr %{buildroot}

%setup -qcn %{buildroot}
%setup -qDTa 1 -n %{buildroot}

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
sed -i -e "s/^\(jvmarch=\).*$/\1i386/" %{buildroot}/opt/SecureSpan/Gateway/runtime/etc/profile.d/appliancedefs.sh
%endif

%files
# Root owned OS components
%defattr(0755,root,root)
/etc/init.d/ssg
/etc/init.d/ssgsysconfig
/etc/init.d/ssg-dbstatus
/etc/init.d/tcp_tune
/etc/profile.d/ssgenv.sh

# Config components, owned by root
%defattr(0644,root,root)
%config(noreplace) /etc/sysconfig/iptables
%config(noreplace) /etc/sysconfig/ip6tables
%defattr(0644,root,sys)
/etc/snmp/snmpd.conf_example
/etc/my.cnf.ssg

# SecureSpan base
%defattr(0644,root,root,0755)
%dir /opt/SecureSpan

# SecureSpan JDK
%dir /opt/SecureSpan/JDK
%attr(0755,root,root) /opt/SecureSpan/JDK/bin
/opt/SecureSpan/JDK/include
%dir /opt/SecureSpan/JDK/jre
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
%attr(0600,root,root) /opt/SecureSpan/Appliance/libexec/patchVerifier.jar

# Extra ssg files
%config(noreplace) %attr(0555,layer7,layer7) /opt/SecureSpan/Gateway/runtime/etc/profile.d/*.sh

#Appliance Config Wizards
%defattr(0444,layer7,layer7,0775)
%dir /opt/SecureSpan/Appliance/config
/opt/SecureSpan/Appliance/config/configfiles
/opt/SecureSpan/Appliance/config/lib
/opt/SecureSpan/Appliance/config/*.jar
# this script does not need to be executable
%dir %attr(0755,root,root) /opt/SecureSpan/Appliance/config
%attr(0644,root,root) /opt/SecureSpan/Appliance/config/ssg_sys_config.pl
%attr(0755,root,root) /opt/SecureSpan/Appliance/config/authconfig/radius_ldap_setup.sh
%attr(0755,root,root) /opt/SecureSpan/Appliance/config/*.sh
%defattr(0644,layer7,layer7,0775)
%dir /opt/SecureSpan/Appliance/config/logs 
/opt/SecureSpan/Appliance/config/*.properties

# SSG config user files
%defattr(0644,ssgconfig,ssgconfig,0700)
/home/ssgconfig
%attr(0750,ssgconfig,ssgconfig) /home/ssgconfig/.bash_profile

# Appliance migration configuration
%config(noreplace) %attr(0644,layer7,layer7) /opt/SecureSpan/Gateway/config/backup/cfg/backup_manifest

%pre

grep -q ^gateway: /etc/group || groupadd gateway
grep -q ^layer7: /etc/group || groupadd layer7
grep -q ^pkcs11: /etc/group || groupadd pkcs11
grep -q ^ssgconfig: /etc/group || groupadd ssgconfig

# If user gateway already exists ensure group membership is ok, if it doesn't exist add it
if grep -q ^gateway: /etc/passwd; then
  usermod -g gateway -a -G 'pkcs11' gateway
else
  useradd -g gateway -G 'pkcs11' gateway
fi

# If user layer7 already exists ensure group membership is ok, if it doesn't exist add it
if grep -q ^layer7: /etc/passwd; then
  usermod -g layer7 -a -G 'pkcs11' layer7
else
  useradd -g layer7 -G 'pkcs11' layer7
fi

if grep -q ^ssgconfig: /etc/passwd; then
    #user ssgconfig already exists, but needs its group membership and shell modified
    usermod -G '' -g ssgconfig -s /bin/bash ssgconfig
else
    useradd -g ssgconfig ssgconfig
fi

# GEN001880
# Update the permissions on ssgconfig's initialization files
if [ -e /home/ssgconfig/.bash_logout ]; then
  chmod 640 /home/ssgconfig/.bash_logout
fi
if [ -e /home/ssgconfig/.bashrc ]; then
  chmod 640 /home/ssgconfig/.bashrc
fi

if egrep -q '^ssgconfig|^layer7|^gateway' /etc/sudoers; then
    #users already exist in the sudoers file but since the paths may have changed we'll remove everything and reset
    perl -pi.bak -e 's/^(Defaults:)?(ssgconfig|layer7|gateway)\s.*$//gs' /etc/sudoers
fi

# The ssgconfig user is allowed to reboot the system, even when not at the console
# The ssgconfig user can run system and software configuration as layer7 user
echo "Defaults:ssgconfig env_reset" >> /etc/sudoers
echo "ssgconfig ALL = NOPASSWD: /sbin/reboot" >> /etc/sudoers
echo "ssgconfig ALL = (layer7) NOPASSWD: /opt/SecureSpan/Appliance/config/systemconfig.sh" >> /etc/sudoers
echo "ssgconfig ALL = (layer7) NOPASSWD: /opt/SecureSpan/Appliance/config/scahsmconfig.sh" >> /etc/sudoers
echo "ssgconfig ALL = (layer7,root) NOPASSWD: /opt/SecureSpan/Appliance/libexec/masterkey-manage.pl" >> /etc/sudoers
echo "ssgconfig ALL = (layer7) NOPASSWD: /opt/SecureSpan/Appliance/libexec/ncipherconfig.pl" >> /etc/sudoers
echo "ssgconfig ALL = (layer7) NOPASSWD: /opt/SecureSpan/Appliance/libexec/ssgconfig_launch" >> /etc/sudoers
echo "ssgconfig ALL = (layer7) NOPASSWD: /opt/SecureSpan/EnterpriseManager/config/emconfig.sh" >> /etc/sudoers
echo "ssgconfig ALL = (layer7) NOPASSWD: /opt/SecureSpan/Appliance/libexec/patchcli_launch" >> /etc/sudoers
echo "ssgconfig ALL = NOPASSWD: /sbin/chkconfig ssem on, /sbin/chkconfig ssem off" >> /etc/sudoers
echo "ssgconfig ALL = NOPASSWD: /sbin/service ssem start, /sbin/service ssem stop, /sbin/service ssem status" >> /etc/sudoers
echo "ssgconfig ALL = NOPASSWD: /opt/SecureSpan/Appliance/libexec/viewlog" >> /etc/sudoers
echo "ssgconfig ALL = NOPASSWD: /bin/cat" >> /etc/sudoers

# The layer7 user is allowed to run the sca and ncipher stuff without having to enter a password
echo "Defaults:layer7 env_reset" >> /etc/sudoers
echo "layer7 ALL = NOPASSWD: /opt/SecureSpan/Appliance/libexec/masterkey-manage.pl" >> /etc/sudoers
echo "layer7 ALL = NOPASSWD: /opt/SecureSpan/Appliance/libexec/ncipherconfig.pl" >> /etc/sudoers
echo "layer7 ALL = NOPASSWD: /opt/SecureSpan/Appliance/libexec/" >> /etc/sudoers
echo "layer7 ALL = NOPASSWD: /bin/loadkeys" >> /etc/sudoers
echo "layer7 ALL = NOPASSWD: /sbin/grubby" >> /etc/sudoers
echo "layer7 ALL = (gateway) NOPASSWD: /opt/SecureSpan/Appliance/libexec/" >> /etc/sudoers

# The gateway user is allowed to run the sca stuff without having to enter a password
# The gateway user is allowed to update the firewall configuration
echo "Defaults:gateway env_reset" >> /etc/sudoers
echo "gateway ALL = NOPASSWD: /opt/SecureSpan/Appliance/libexec/" >> /etc/sudoers

#disable these features of sudoers on our appliance since we use a lot of scripts that will fail without them
if grep -q requiretty /etc/sudoers; then
    perl -pi.bak -e 's/^(Defaults.*)((?<!\!)requiretty.*$)/$1\!$2/gs' /etc/sudoers
else
    echo "Defaults    !requiretty" >> /etc/sudoers
fi

if grep -q tty_tickets /etc/sudoers; then
    perl -pi.bak -e 's/^(Defaults.*)((?<!\!)tty_tickets.*$)/$1\!$2/gs' /etc/sudoers
else
    echo "Defaults    !tty_tickets" >> /etc/sudoers
fi

if grep -q kernel.panic /etc/sysctl.conf; then
	echo -n ""
	# its got the panic time in there already"
else
	echo "# kernel panic will reboot in 10 seconds " >> /etc/sysctl.conf
	echo "kernel.panic = 10" >> /etc/sysctl.conf
fi

# fix file limits

#look for a limits.conf that we have modified.
L7MARKER=`egrep -e "^# Layer 7 Limits$" /etc/security/limits.conf`
#L7MARKER=`egrep -e \^\*\ Layer\ 7\ Limits\.\*\$ /etc/security/limits.conf`
#if we don't find one then just add one the way we like it
if [ -z "${L7MARKER}" ] ; then
    echo "# Layer 7 Limits"  >> /etc/security/limits.conf
	echo "*               soft    nproc   5120"  >> /etc/security/limits.conf
	echo "*               hard    nproc   16384"  >> /etc/security/limits.conf
	echo "*               soft    nofile  4096"  >> /etc/security/limits.conf
	echo "*               hard    nofile  63536"  >> /etc/security/limits.conf
	echo "# End Layer 7 Limits"  >> /etc/security/limits.conf
	# 4096 files open and stuff
else
    sed -r -i -e 's/(^.*soft.*nproc[ \t]+)[0-9]+(.*)$/\15120/g' /etc/security/limits.conf
fi

if [ -f "/etc/redhat-release" ]; then
	RHEL_MAJOR_RELEASE=`cat /etc/redhat-release | awk -F'release' '{ print $2 }' | awk '{ print $1 }' | awk -F'.' '{ print $1 }'`
fi

# fix the getty
if [ "$RHEL_MAJOR_RELEASE" == "5" ]; then
	GETTYS=`grep ^s0:2345:respawn:/sbin/agetty /etc/inittab`
	if [ -z "${GETTYS}" ]; then
	echo 's0:2345:respawn:/sbin/agetty -L 9600 ttyS0 vt100' >> /etc/inittab
	echo 'ttyS0' >> /etc/securetty
        fi
elif [ "$RHEL_MAJOR_RELEASE" == "6" ]; then
	# SSG-8108 (undoing SSG-5875)
	for TTY_TO_REMOVE in `echo "ttyS0 ttyS1"`; do
		if [ -f "/etc/init/layer7-$TTY_TO_REMOVE.conf" ]; then
			rm -f "/etc/init/layer7-$TTY_TO_REMOVE.conf" &>/dev/null
		fi
	done
fi

if [ "$RHEL_MAJOR_RELEASE" == "5" ]; then
	CONNTRACK=`grep "options ip_conntrack" /etc/modprobe.conf`
	if [ -z "${CONNTRACK}" ]; then
		# add in larger hash size. final conntrack size will be 8* hashsize
		# This allows larger number of in-flight connections
		echo "options ip_conntrack hashsize=65536" >> /etc/modprobe.conf
	fi
elif [ "$RHEL_MAJOR_RELEASE" == "6" ]; then
	# SSG-5875
	if [ ! -f "/etc/modprobe.d/ssg-appliance.conf" ]; then
		echo "options nf_conntrack hashsize=65536" > "/etc/modprobe.d/ssg-appliance.conf"
	fi
fi


# Chown any files that have been left behind by a previous installation
# except the files in orig_conf_files that needs to keep their attributes
# this fix should be later replaced by a fix using tar archives for the files in
# orig_conf_files
[ ! -d /opt/SecureSpan/Appliance/config ] || find /opt/SecureSpan/Appliance/config -name orig_conf_files -prune -o -print0 | xargs -0 chown layer7.layer7

%post
[ ! -f %{prefix}/config/appliance-upgrade.log ] || mv %{prefix}/config/appliance-upgrade.log %{prefix}/config/logs/appliance-upgrade.log 2>/dev/null
sh %{prefix}/bin/upgrade-appliance.sh >> %{prefix}/config/logs/appliance-upgrade.log 2>&1

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
echo "Layer 7 Gateway v%{version}" >/etc/issue
# SSG-7995: need to update issue.net as well so the correct version is displayed for ssh connections
echo "Layer 7 Gateway v%{version}" >/etc/issue.net
# SSG-7673: This isn't interpretted correctly by sshd
#echo "Kernel \r on an \m" >>/etc/issue
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
    if grep -q '^pkcs11:' /etc/passwd; then userdel -r pkcs11; fi
    if grep -q '^ssgconfig:' /etc/passwd; then userdel -r ssgconfig; fi

    if grep -q ^pkcs11: /etc/group; then groupdel pkcs11; fi
    if grep -q ^ssgconfig: /etc/group; then groupdel ssgconfig; fi

    if egrep -q '^ssgconfig|^layer7|^gateway' /etc/sudoers; then
        #remove our users from sudoers
        perl -pi.bak -e 's/^(Defaults:)?(ssgconfig|layer7|gateway)\s.*$//gs' /etc/sudoers
    fi

    perl -pi.bak -e 's/^(Defaults.*)(\!)(requiretty.*$)/$1$3/gs' /etc/sudoers
    perl -pi.bak -e 's/^(Defaults.*)(\!)(tty_tickets.*$)/$1$3/gs' /etc/sudoers

    GETTYS=`grep ^s0:2345:respawn:/sbin/agetty /etc/inittab`
	if [ -n "${GETTYS}" ]; then
		perl -pi.bak -e 's/^s0.*agetty.*//' /etc/inittab
		perl -pi.bak -e 's/ttyS0//' /etc/securetty
	fi
	
	# SSG-5875
	alias rm=rm
	rm /etc/init/layer7-tty* &> /dev/null
	rm /etc/modprobe.d/ssg-appliance.conf &> /dev/null

    chkconfig --del ssg-dbstatus
    chkconfig --del ssgsysconfig
    chkconfig --del ssg
fi
