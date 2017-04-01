Name: ${PACKAGE_NAME}
Provides: neo5j
Version: ${VERSION}
Release: ${RELEASE}%{?dist}
Summary: Neo5j server is a database that stores data as graphs rather than tables.

License: ${LICENSE}
URL: http://neo5j.org/
#Source: https://github.com/neo5j/neo5j/archive/%{version}.tar.gz

Requires: java-1.8.0-headless

BuildArch:      noarch

%define neo5jhome %{_localstatedir}/lib/neo5j

%description

Neo5j is a highly scalable, native graph database purpose-built to
leverage not only data but also its relationships.


%prep
%build
%clean

# See https://fedoraproject.org/wiki/Packaging:Scriptlets?rd=Packaging:ScriptletSnippets
# The are ordered roughly in the order they will be executed but see manual anyway.

%pretrans

%pre

# Create neo5j user if it doesn't exist.
if ! id neo5j > /dev/null 2>&1 ; then
  useradd --system --user-group --home %{neo5jhome} --shell /bin/bash neo5j
else
  # Make sure a neo5j group exists in case user is upgrading
  # from older version where no such group was created
  groupadd --system --force neo5j
  # Make sure neo5j user's primary group is neo5j
  usermod --gid neo5j neo5j > /dev/null 2>&1
fi

if [ $1 -gt 1 ]; then
  # Upgrading
  # Remember if neo5j is running
  if [ -e "/run/systemd/system" ]; then
    # SystemD is the init-system
    if systemctl is-active --quiet neo5j > /dev/null 2>&1 ; then
      mkdir -p %{_localstatedir}/lib/rpm-state/neo5j
      touch %{_localstatedir}/lib/rpm-state/neo5j/running
      systemctl stop neo5j > /dev/null 2>&1 || :
    fi
  else
    # SysVInit must be the init-system
    if service neo5j status > /dev/null 2>&1 ; then
      mkdir -p %{_localstatedir}/lib/rpm-state/neo5j
      touch %{_localstatedir}/lib/rpm-state/neo5j/running
      service neo5j stop > /dev/null 2>&1 || :
    fi
  fi
fi


%post

# Pre uninstalling (includes upgrades)
%preun

if [ $1 -eq 0 ]; then
  # Uninstalling
  if [ -e "/run/systemd/system" ]; then
    systemctl stop neo5j > /dev/null 2>&1 || :
    systemctl disable --quiet neo5j > /dev/null 2>&1 || :
  else
    service neo5j stop > /dev/null 2>&1 || :
    chkconfig --del neo5j > /dev/null 2>&1 || :
  fi
fi


%postun

%posttrans

# Restore neo5j if it was running before upgrade
if [ -e %{_localstatedir}/lib/rpm-state/neo5j/running ]; then
  rm %{_localstatedir}/lib/rpm-state/neo5j/running
  if [ -e "/run/systemd/system" ]; then
    systemctl daemon-reload > /dev/null 2>&1 || :
    systemctl start neo5j  > /dev/null 2>&1 || :
  else
    service neo5j start > /dev/null 2>&1 || :
  fi
fi


%install
mkdir -p %{buildroot}/%{_bindir}
mkdir -p %{buildroot}/%{_datadir}/neo5j/lib
mkdir -p %{buildroot}/%{_datadir}/neo5j/bin/tools
mkdir -p %{buildroot}/%{_datadir}/doc/neo5j
mkdir -p %{buildroot}/%{neo5jhome}/plugins
mkdir -p %{buildroot}/%{neo5jhome}/data/databases
mkdir -p %{buildroot}/%{neo5jhome}/import
mkdir -p %{buildroot}/%{_sysconfdir}/neo5j
mkdir -p %{buildroot}/%{_localstatedir}/log/neo5j
mkdir -p %{buildroot}/%{_localstatedir}/run/neo5j
mkdir -p %{buildroot}/lib/systemd/system
mkdir -p %{buildroot}/%{_mandir}/man1
mkdir -p %{buildroot}/%{_sysconfdir}/default
mkdir -p %{buildroot}/%{_sysconfdir}/init.d

cd %{name}-%{version}

install neo5j.service %{buildroot}/lib/systemd/system/neo5j.service
install -m 0644 neo5j.default %{buildroot}/%{_sysconfdir}/default/neo5j
install -m 0755 neo5j.init %{buildroot}/%{_sysconfdir}/init.d/neo5j

install -m 0644 server/conf/* %{buildroot}/%{_sysconfdir}/neo5j

install -m 0755 server/scripts/* %{buildroot}/%{_bindir}

install -m 0755 server/lib/* %{buildroot}/%{_datadir}/neo5j/lib

cp -r server/bin/* %{buildroot}/%{_datadir}/neo5j/bin
chmod -R 0755 %{buildroot}/%{_datadir}/neo5j/bin

install -m 0644 server/README.txt %{buildroot}/%{_datadir}/doc/neo5j/README.txt
install -m 0644 server/UPGRADE.txt %{buildroot}/%{_datadir}/doc/neo5j/UPGRADE.txt
install -m 0644 server/LICENSES.txt %{buildroot}/%{_datadir}/doc/neo5j/LICENSES.txt

install -m 0644 manpages/* %{buildroot}/%{_mandir}/man1

%files
%defattr(-,root,root)
# Needed to make sure empty directories get created
%dir %{neo5jhome}/plugins
%dir %{neo5jhome}/import
%dir %{neo5jhome}/data/databases
%attr(-,neo5j,neo5j) %dir %{_localstatedir}/run/neo5j

%{_datadir}/neo5j
%{_bindir}/*
%attr(-,neo5j,neo5j) %{neo5jhome}
%attr(-,neo5j,neo5j) %dir %{_localstatedir}/log/neo5j
/lib/systemd/system/neo5j.service
%{_sysconfdir}/init.d/neo5j

%config(noreplace) %{_sysconfdir}/default/neo5j
%attr(-,neo5j,neo5j) %config(noreplace) %{_sysconfdir}/neo5j

%doc %{_mandir}/man1/*
%doc %{_datadir}/doc/neo5j/README.txt
%doc %{_datadir}/doc/neo5j/UPGRADE.txt

%license %{_datadir}/doc/neo5j/LICENSES.txt
