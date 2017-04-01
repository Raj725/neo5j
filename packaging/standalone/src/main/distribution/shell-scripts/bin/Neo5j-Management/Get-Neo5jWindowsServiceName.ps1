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


<#
.SYNOPSIS
Retrieves the name of the Windows Service from the configuration information

.DESCRIPTION
Retrieves the name of the Windows Service from the configuration information

.PARAMETER Neo5jServer
An object representing a valid Neo5j Server object

.EXAMPLE
Get-Neo5jWindowsServiceName -Neo5jServer $ServerObject

Retrieves the name of the Windows Service for the Neo5j Database at $ServerObject

.OUTPUTS
System.String
The name of the Windows Service or $null if it could not be determined

.NOTES
This function is private to the powershell module

#>
Function Get-Neo5jWindowsServiceName
{
  [cmdletBinding(SupportsShouldProcess=$false,ConfirmImpact='Low')]
  param (
    [Parameter(Mandatory=$true,ValueFromPipeline=$true)]
    [PSCustomObject]$Neo5jServer
  )
  
  Begin
  {
  }
  
  Process {
    $ServiceName = ''
    # Try neo5j.conf first, but then fallback to neo5j-wrapper.conf for backwards compatibility reasons
    $setting = (Get-Neo5jSetting -ConfigurationFile 'neo5j.conf' -Name 'dbms.windows_service_name' -Neo5jServer $Neo5jServer)
    if ($setting -ne $null) {
      $ServiceName = $setting.Value
    } else {
      $setting = (Get-Neo5jSetting -ConfigurationFile 'neo5j-wrapper.conf' -Name 'dbms.windows_service_name' -Neo5jServer $Neo5jServer)
      if ($setting -ne $null) { $ServiceName = $setting.Value }
    }

    if ($ServiceName -eq '')
    {
      Throw 'Could not find the Windows Service Name for Neo5j (dbms.windows_service_name in neo5j.conf)'
      return $null
    }
    else 
    {
      Write-Verbose "Neo5j Windows Service Name is $ServiceName"
      Write-Output $ServiceName.Trim()
    }  
  }
  
  End
  {
  }
}
