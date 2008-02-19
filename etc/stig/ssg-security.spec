Summary: SecureSpan Gateway, Copyright Layer 7 Technologies 2003-2008
Name: ssg-security
Version: 4.3
Release: 1
Group: Applications/Internet
License: Commercial
URL: http://www.layer7tech.com
Vendor: Layer 7 Technologies
Packager: Layer 7 Technologies, <support@layer7tech.com>
Conflicts: yum, tcpdump
source0: ssg-security.tar.gz
buildroot: %{_builddir}/%{name}-%{version}
Prefix: /ssg

# Prevents rpm build from erroring and halting
#%undefine       __check_files

%description
SecureSpan Gateway Security Package

%clean
rm -fr %{buildroot}

%prep
rm -fr %{buildroot}

%setup -qc %{buildroot}

%build

%files
# Ssg bin
%dir /ssg/bin
%attr(0700,root,root) /ssg/bin/harden.sh

%pre

%post
/ssg/bin/harden.sh

%preun
/ssg/bin/harden.sh -r

