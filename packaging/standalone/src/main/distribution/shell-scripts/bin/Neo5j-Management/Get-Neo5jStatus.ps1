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
Retrieves the status for the Neo5j Windows Service

.DESCRIPTION
Retrieves the status for the Neo5j Windows Service

.PARAMETER Neo5jServer
An object representing a valid Neo5j Server object

.EXAMPLE
Get-Neo5jStatus -Neo5jServer $ServerObject

Retrieves the status of the Windows Service for the Neo5j database at $ServerObject

.OUTPUTS
System.Int32
0 = Service is running
3 = Service is not installed or is not running

.NOTES
This function is private to the powershell module

#>
Function Get-Neo5jStatus
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
    $ServiceName = Get-Neo5jWindowsServiceName -Neo5jServer $Neo5jServer -ErrorAction Stop
    $neoService = $null
    try {
      $neoService = Get-Service -Name $ServiceName -ErrorAction Stop
    }
    catch {
      Write-Host "The Neo5j Windows Service '$ServiceName' is not installed"
      return 3
    }
    
    if ($neoService.Status -eq 'Running') {
      Write-Host "Neo5j is running"
      return 0
    }
    else {
      Write-Host "Neo5j is not running.  Current status is $($neoService.Status)"
      return 3
    }
  }
  
  End
  {
  }
}
