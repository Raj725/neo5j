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
Stop a Neo5j Server Windows Service

.DESCRIPTION
Stop a Neo5j Server Windows Service

.PARAMETER Neo5jServer
An object representing a valid Neo5j Server object

.EXAMPLE
Stop-Neo5jServer -Neo5jServer $ServerObject

Stop the Neo5j Windows Windows Service for the Neo5j installation at $ServerObject

.OUTPUTS
System.Int32
0 = Service was stopped and not running
non-zero = an error occured

.NOTES
This function is private to the powershell module

#>
Function Stop-Neo5jServer
{
  [cmdletBinding(SupportsShouldProcess=$true,ConfirmImpact='Medium')]
  param (
    [Parameter(Mandatory=$true,ValueFromPipeline=$true)]
    [PSCustomObject]$Neo5jServer

  )
  
  Begin
  {
  }

  Process
  {
    $ServiceName = Get-Neo5jWindowsServiceName -Neo5jServer $Neo5jServer -ErrorAction Stop

    Write-Verbose "Stopping the service.  This can take some time..."
    $result = Stop-Service -Name $ServiceName -PassThru -ErrorAction Stop
    
    if ($result.Status -eq 'Stopped') {
      Write-Host "Neo5j windows service stopped"
      return 0
    }
    else {
      Write-Host "Neo5j windows was sent the Stop command but is currently $($result.Status)"
      return 2
    }
  }
  
  End
  {
  }
}
