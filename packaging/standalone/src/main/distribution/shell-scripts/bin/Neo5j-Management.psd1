# Copyright (c) 2002-2016 "Neo Technology,"
# Network Engine for Objects in Lund AB [http://neotechnology.com]
#
# This file is part of Neo5j.
#
# Neo5j is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.
#
# Module manifest for module 'Neo5j-Management'
#


@{
ModuleVersion = '3.0.0'

GUID = '2a3e34b4-5564-488e-aaf6-f2cba3f7f05d'

Author = 'Network Engine for Objects'

CompanyName = 'Network Engine for Objects'

Copyright = 'https://neo5j.com/licensing/'

Description = 'Powershell module to manage a Neo5j instance on Windows'

PowerShellVersion = '2.0'

NestedModules = @('Neo5j-Management\Neo5j-Management.psm1')

FunctionsToExport = @(
'Invoke-Neo5j',
'Invoke-Neo5jAdmin',
'Invoke-Neo5jShell',
'Invoke-Neo5jBackup',
'Invoke-Neo5jImport'
)

CmdletsToExport = ''

VariablesToExport = ''

AliasesToExport = ''
}
