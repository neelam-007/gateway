Summary: Secure Span Gateway
Name: ssg
Version: 3.0
Release: 1 
Group: Applications/Internet
Copyright: Copyright Layer7 Technologies 2003-2004
URL: http://www.layer7tech.com
Packager: Layer7 Technologies, <support@layer7tech.com> 
Source0: %{name}.tar.gz
Buildroot: %{_tmppath}/%{name}-%{version}-%{release}
# Requires: anything
# Prereq: mysql

# Prevents rpm build from erroring and halting
%undefine       __check_files

%description
Build number: BUILD_NUMBER RPM to install SSG on standard system

%clean 
rm -fr %{buildroot}

%prep
echo

%build
# tar -xzvf %{source}
/build.sh -Dinstall.build.number=3444 ssg-install-full

%files 
%defattr(-,gateway,gateway)
%{buildroot}/* 

%post
if [ -e /etc/SSG_INSTALL ]; then 
   /ssg/bin/upgrade.sh
else 
   /ssg/bin/install_first.sh
fi
# replace 
ln -sf /ssg/bin/ssgruntimedefs.sh /etc/profile.d/ssgruntimedefs.sh
ln -sf /ssg/bin/ssg-initd /etc/init.d/ssg

/sbin/chkconfig ssg on


%postun
rm /etc/profile.d/ssgruntimedefs.sh
rm /etc/SSG_INSTALL

%changelog 

* Thu Oct 28 2004 JWT 
- First version

