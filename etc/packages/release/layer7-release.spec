# Automatically inserts the version into the etc files. For future releases
# just change the Version header, create a tarball from this that has the
# version in it (e.g. 'tar cvzf layer7-release-3.6.tar.gz layer7-release')
# then use 'rpmbuild -tb layer7-release-3.6.tar.gz' to build the package.
Summary: Layer 7 release files
Name: layer7-release
Version: 3.7.0
Release: 1
Group: Applications/Internet
License: Copyright Layer7 Technologies
URL: http://www.layer7tech.com
Packager: Layer7 Technologies, <support@layer7tech.com>
Source0: /tmp/%{name}-%{version}.tar.gz
buildroot: %{_builddir}/%{name}-%{version}
provides: system-logos redhat-release
ExclusiveArch: noarch

# Prevents rpm build from erroring and halting
%undefine       __check_files

%description
Sets /etc/redhat-release and the issue files to reflect Layer7 Suite
and provides a system logo for the grub screen.

%clean
rm -fr %{buildroot}

%prep
mkdir -p %{buildroot}
cd %{buildroot}
tar -xzvf /tmp/%{name}-%{version}.tar.gz

%build
cat etc/*
sed -i 's/_VER_/%{version}/g' etc/issue
sed -i 's/_VER_/%{version}/g' etc/issue.net
cat etc/*

%files
%defattr(-,root,root)
/boot
/etc

%pre

%post

%preun

%changelog
* Fri Apr 27 2007 JWT
- CVS control and Makefile
* Mon Dec 11 2006 JAM
- Build First version
