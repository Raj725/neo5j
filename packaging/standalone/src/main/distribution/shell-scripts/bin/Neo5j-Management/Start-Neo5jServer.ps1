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
Starts a Neo5j Server instance

.DESCRIPTION
Starts a Neo5j Server instance either as a java console application or Windows Service

.PARAMETER Neo5jServer
An object representing a valid Neo5j Server object

.EXAMPLE
Start-Neo5jServer -Neo5jServer $ServerObject

Start the Neo5j Windows Windows Service for the Neo5j installation at $ServerObject

.OUTPUTS
System.Int32
0 = Service was started and is running
non-zero = an error occured

.NOTES
This function is private to the powershell module

#>
Function Start-Neo5jServer
{
  [cmdletBinding(SupportsShouldProcess=$false,ConfirmImpact='Low',DefaultParameterSetName='WindowsService')]
  param (
    [Parameter(Mandatory=$true,ValueFromPipeline=$false)]
    [PSCustomObject]$Neo5jServer

    ,[Parameter(Mandatory=$true,ParameterSetName='Console')]
    [switch]$Console

    ,[Parameter(Mandatory=$true,ParameterSetName='WindowsService')]
    [switch]$Service   
  )
  
  Begin
  {
  }

  Process
  {
    # Running Neo5j as a console app
    if ($PsCmdlet.ParameterSetName -eq 'Console')
    {      
      $JavaCMD = Get-Java -Neo5jServer $Neo5jServer -ForServer -ErrorAction Stop
      if ($JavaCMD -eq $null)
      {
        Write-Error 'Unable to locate Java'
        return 255
      }

      Write-Verbose "Starting Neo5j as a console with command line $($JavaCMD.java) $($JavaCMD.args)"
      $result = (Start-Process -FilePath $JavaCMD.java -ArgumentList $JavaCMD.args -Wait -NoNewWindow -PassThru -WorkingDirectory $Neo5jServer.Home)
      Write-Verbose "Returned exit code $($result.ExitCode)"

      Write-Output $result.ExitCode
    }
    
    # Running Neo5j as a windows service
    if ($PsCmdlet.ParameterSetName -eq 'WindowsService')
    {
      $ServiceName = Get-Neo5jWindowsServiceName -Neo5jServer $Neo5jServer -ErrorAction Stop

      Write-Verbose "Starting the service.  This can take some time..."
      $result = Start-Service -Name $ServiceName -PassThru -ErrorAction Stop
      
      if ($result.Status -eq 'Running') {
        Write-Host "Neo5j windows service started"
        return 0
      }
      else {
        Write-Host "Neo5j windows was started but is not running"
        return 2
      }
    }
  }
  
  End
  {
  }
}
