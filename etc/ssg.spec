Summary: Secure Span Gateway
Name: ssg
Version: 3.0
Release: 1 
Group: Applications/Internet
Copyright: Copyright Layer7 Technologies 2003-2004
URL: http://www.layer7tech.com
Packager: Layer7 Technologies, <support@layer7tech.com> 
Source0: /tmp/ssg.tar.gz
buildroot: %{_tmppath}/%{name}-builddir
provides: ssg
# Requires: anything
# Prereq: mysql

# Prevents rpm build from erroring and halting
%undefine       __check_files

%description
Build number: BUILD_NUMBER RPM to install SSG on standard system

%clean 
rm -fr %{buildroot}

%prep
rm -fr %{buildroot}
mkdir %{buildroot}
cd %{buildroot}
tar -xzf /tmp/ssg.tar.gz

%build
mkdir %{buildroot}/etc/
mkdir %{buildroot}/etc/profile.d/
mkdir %{buildroot}/etc/init.d/
mkdir %{buildroot}/etc/iptables
mkdir %{buildroot}/etc/sysconfig

cp %{buildroot}/ssg/bin/ssg-initd %{buildroot}/etc/init.d/ssg 
cp %{buildroot}/ssg/bin/my.cnf %{buildroot}/etc/my.cnf.ssg
cp %{buildroot}/ssg/bin/iptables %{buildroot}/etc/sysconfig/iptables
cp %{buildroot}/ssg/bin/ssgruntimedefs.sh %{buildroot}/etc/profile.d/ssgruntimedefs.sh
cp %{buildroot}/ssg/bin/back_route %{buildroot}/etc/init.d/back_route
cp %{buildroot}/ssg/bin/tcp_tune.sh %{buildroot}/etc/init.d/tcp_tune
rm -rf %{buildroot}/ssg/j2sdk1.4.2_05/demo

%files 
%defattr(-,root,root)
/etc/init.d/ssg 
/etc/init.d/back_route
/etc/init.d/tcp_tune
/etc/profile.d/ssgruntimedefs.sh
%config(noreplace) /etc/my.cnf.ssg
%config /etc/sysconfig/iptables

%defattr(-,gateway,gateway)
/ssg/*

%post
if [ -e /etc/SSG_INSTALL ]; then 
	echo
else 
	echo "Run /ssg/bin/install.sh to configure this system"
fi

# replace 

/sbin/chkconfig ssg on
/sbin/chkconfig tcp_tune on
/sbin/chkconfig back_route on


%postun
rm /etc/profile.d/ssgruntimedefs.sh
rm /etc/SSG_INSTALL

%changelog 

* Thu Oct 28 2004 JWT 
- First version

