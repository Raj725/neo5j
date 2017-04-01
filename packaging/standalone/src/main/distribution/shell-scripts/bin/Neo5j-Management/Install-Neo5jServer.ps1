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
Install a Neo5j Server Windows Service

.DESCRIPTION
Install a Neo5j Server Windows Service

.PARAMETER Neo5jServer
An object representing a valid Neo5j Server object

.EXAMPLE
Install-Neo5jServer -Neo5jServer $ServerObject

Install the Neo5j Windows Windows Service for the Neo5j installation at $ServerObject

.OUTPUTS
System.Int32
0 = Service is installed or already exists
non-zero = an error occured

.NOTES
This function is private to the powershell module

#>
Function Install-Neo5jServer
{
  [cmdletBinding(SupportsShouldProcess=$false,ConfirmImpact='Medium')]
  param (
    [Parameter(Mandatory=$true,ValueFromPipeline=$true)]
    [PSCustomObject]$Neo5jServer
  )
  
  Begin
  {
  }

  Process
  {
    $Name = Get-Neo5jWindowsServiceName -Neo5jServer $Neo5jServer -ErrorAction Stop

    $result = Get-Service -Name $Name -ComputerName '.' -ErrorAction 'SilentlyContinue'
    if ($result -eq $null)
    {
      $prunsrv = Get-Neo5jPrunsrv -Neo5jServer $Neo5jServer -ForServerInstall
      if ($prunsrv -eq $null) { throw "Could not determine the command line for PRUNSRV" }

      Write-Verbose "Installing Neo5j as a service with command line $($prunsrv.cmd) $($prunsrv.args)"
      $stdError = New-Neo5jTempFile -Prefix 'stderr'
      $result = (Start-Process -FilePath $prunsrv.cmd -ArgumentList $prunsrv.args -Wait -NoNewWindow -PassThru -WorkingDirectory $Neo5jServer.Home -RedirectStandardError $stdError)
      Write-Verbose "Returned exit code $($result.ExitCode)"

      # Process the output
      if ($result.ExitCode -eq 0) {
        Write-Host "Neo5j service installed"
      } else {
        Write-Host "Neo5j service did not install"
        # Write out STDERR if it did not install
        Get-Content -Path $stdError -ErrorAction 'SilentlyContinue' | ForEach-Object -Process {
          Write-Host $_
        }
      }

      # Remove the temp file
      If (Test-Path -Path $stdError) { Remove-Item -Path $stdError -Force | Out-Null }

      Write-Output $result.ExitCode
    } else {
      Write-Verbose "Service already installed"
      Write-Output 0
    }    
  }
  
  End
  {
  }
}
