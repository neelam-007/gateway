Summary: Secure Span Gateway
Name: ssg
Version: 3.1
Release: rc1
Group: Applications/Internet
Copyright: Copyright Layer7 Technologies 2003-2005
URL: http://www.layer7tech.com
Packager: Layer7 Technologies, <support@layer7tech.com> 
Source0: /tmp/ssg.tar.gz
buildroot: %{_builddir}/%{name}-%{version}
provides: ssg

# Prevents rpm build from erroring and halting
%undefine       __check_files

%description
SSG software distribution on standard system
Does: ssg, network config, profiles
Modifies startup config to run only expected services

%clean 
rm -fr %{buildroot}

%prep
rm -fr %{buildroot}
mkdir %{buildroot}
cd %{buildroot}
tar -xzf /tmp/ssg.tar.gz

%build
mkdir %{buildroot}/etc/
mkdir %{buildroot}/etc/snmp/
mkdir %{buildroot}/etc/profile.d/
mkdir %{buildroot}/etc/init.d/
mkdir %{buildroot}/etc/iptables
mkdir %{buildroot}/etc/sysconfig

mv %{buildroot}/ssg/bin/ssg-initd %{buildroot}/etc/init.d/ssg 
mv %{buildroot}/ssg/bin/tarari-initd %{buildroot}/etc/init.d/tarari
mv %{buildroot}/ssg/bin/my.cnf %{buildroot}/etc/my.cnf.ssg
mv %{buildroot}/ssg/bin/iptables %{buildroot}/etc/sysconfig/iptables
mv %{buildroot}/ssg/bin/ssgruntimedefs.sh %{buildroot}/etc/profile.d/ssgruntimedefs.sh
mv %{buildroot}/ssg/bin/tarari.sh %{buildroot}/etc/profile.d/tarari.sh
mv %{buildroot}/ssg/bin/back_route %{buildroot}/etc/init.d/back_route
mv %{buildroot}/ssg/bin/tcp_tune.sh %{buildroot}/etc/init.d/tcp_tune
mv %{buildroot}/ssg/bin/snmpd.conf %{buildroot}/etc/snmp/snmpd.conf_example

%files 
%defattr(-,root,root)
/etc/init.d/ssg 
/etc/init.d/tarari
/etc/init.d/back_route
/etc/init.d/tcp_tune
/etc/snmp/snmpd.conf_example
/etc/profile.d/ssgruntimedefs.sh
/etc/profile.d/tarari.sh
%config(noreplace) /etc/my.cnf.ssg
%config(noreplace) /etc/sysconfig/iptables
%defattr(-,gateway,gateway)
%config(noreplace) /ssg/etc/conf/*
%config(noreplace) /ssg/tomcat/conf/*
/ssg/


%pre
if [ `grep ^gateway: /etc/passwd` ]; then
  echo "user/group gateway already exists"
else
  adduser gateway
fi

%post
# Check for existence of install crumbs left by install.pl
if [ -e /etc/SSG_INSTALL ]; then 
	echo "** Run upgrade script: /ssg/bin/upgrade.sh doUpgrade **"
else 
	# Enable required services
	echo " First Time Install: Modifying startup configuration"
	echo " Examine results with /sbin/chkconfig --list"

	# turn no matter what is on off
	cd /etc/init.d/
	for x in *; do /sbin/chkconfig $x off; done
	cd 

	# enable only what we want and expect to be on
	/sbin/chkconfig network on
	/sbin/chkconfig anacron on
	/sbin/chkconfig kudzu on
	/sbin/chkconfig smartd on
	/sbin/chkconfig crond on
	/sbin/chkconfig ntpd on
	/sbin/chkconfig sshd on
	/sbin/chkconfig rhnsd on
	/sbin/chkconfig acpid on
	/sbin/chkconfig cpuspeed on
	/sbin/chkconfig iptables on
	/sbin/chkconfig syslog on
	/sbin/chkconfig lm_sensors on
	/sbin/chkconfig apmd on
	/sbin/chkconfig mysql on
	/sbin/chkconfig ssg on
	/sbin/chkconfig tcp_tune on
	/sbin/chkconfig back_route on
	/sbin/chkconfig tarari on
	echo "** Run interactive /ssg/bin/install.pl to configure this system **"
fi

echo "Layer 7 SecureSpan(tm) Gateway v3.1\nKernel \r on an \m\n" >/etc/issue
echo "Layer 7 SecureSpan(tm) Gateway v3.1\nKernel \r on an \m\n" >/etc/issue.net

%postun

%changelog 

* Thu Oct 28 2004 JWT 
- Build 3079. Does not yet overwrite mysql configuration files. Provides relatively small version of install.pl
* Mon May 02 2005 JWT
- Build 3133. Now modifies startup to run only needed services. Modifies Issue files to show SSG id 
