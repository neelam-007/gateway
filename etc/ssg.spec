Summary: Secure Span Gateway
Name: ssg
Version: 3.1
Release: m3d
Group: Applications/Internet
Copyright: Copyright Layer7 Technologies 2003-2004
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
Does not: overwrite mysql config, set up db clustering, failover

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
/ssg/bin/upgrade.sh getFromRelease
if [ `grep ^gateway: /etc/passwd` ]
then
  echo "user/group gateway already existed"
else
  adduser gateway
fi

%post
# Check for existence of install crumbs left by install.pl
if [ -e /etc/SSG_INSTALL ]; then 
	echo "Running upgrade script"
	/ssg/bin/upgrade.sh doUpgrade
else 
	echo "**Run interactive /ssg/bin/install.pl to configure this system**"
fi
# Enable required services
/sbin/chkconfig ssg on
/sbin/chkconfig tcp_tune on
/sbin/chkconfig back_route on
/sbin/chkconfig tarari on

echo "Layer 7 SecureSpan(tm) Gateway v3.1\nKernel \r on an \m\n" >/etc/issue
echo "Layer 7 SecureSpan(tm) Gateway v3.1\nKernel \r on an \m\n" >/etc/issue.net

%postun
if [ -e /etc/SSG_INSTALL ]; then
	rm /etc/SSG_INSTALL
fi

%changelog 

* Thu Oct 28 2004 JWT 
- Build 3079. Does not yet overwrite mysql configuration files. Provides relatively small version of install.pl
