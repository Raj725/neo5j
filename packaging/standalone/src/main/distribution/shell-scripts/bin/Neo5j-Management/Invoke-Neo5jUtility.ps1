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
Invokes various Neo5j Utilites

.DESCRIPTION
Invokes various Neo5j Utilites.  This is a generic utility function called by the external functions e.g. Shell, Import

.PARAMETER Command
A string of the command to run.

.PARAMETER CommandArgs
Command line arguments to pass to the utility

.OUTPUTS
System.Int32
0 = Success
non-zero = an error occured

.NOTES
Only supported on version 3.x Neo5j Community and Enterprise Edition databases

.NOTES
This function is private to the powershell module

#>
Function Invoke-Neo5jUtility
{
  [cmdletBinding(SupportsShouldProcess=$false,ConfirmImpact='Low')]
  param (
    [Parameter(Mandatory=$false,ValueFromPipeline=$false,Position=0)]
    [string]$Command = ''

    ,[parameter(Mandatory=$false,ValueFromRemainingArguments=$true)]
    [object[]]$CommandArgs = @()
  )

  Begin
  {
  }

  Process
  {
    # Determine the Neo5j Home Directory.  Uses the NEO5J_HOME enironment variable or a parent directory of this script
    $Neo5jHome = Get-Neo5jEnv 'NEO5J_HOME'
    if ( ($Neo5jHome -eq $null) -or (-not (Test-Path -Path $Neo5jHome)) ) {
      $Neo5jHome = Split-Path -Path (Split-Path -Path $PSScriptRoot -Parent) -Parent
    }
    if ($Neo5jHome -eq $null) { throw "Could not determine the Neo5j home Directory.  Set the NEO5J_HOME environment variable and retry" }
    Write-Verbose "Neo5j Root is '$Neo5jHome'"

    $thisServer = Get-Neo5jServer -Neo5jHome $Neo5jHome -ErrorAction Stop
    if ($thisServer -eq $null) { throw "Unable to determine the Neo5j Server installation information" }
    Write-Verbose "Neo5j Server Type is '$($thisServer.ServerType)'"
    Write-Verbose "Neo5j Version is '$($thisServer.ServerVersion)'"
    Write-Verbose "Neo5j Database Mode is '$($thisServer.DatabaseMode)'"

    # Check if we have administrative rights; If the current user's token contains the Administrators Group SID (S-1-5-32-544)
    if (-not [bool](([System.Security.Principal.WindowsIdentity]::GetCurrent()).groups -match "S-1-5-32-544")) {
      Write-Warning "This command does not appear to be running with administrative rights.  Some commands may fail e.g. Start/Stop"
    }

    $GetJavaParams = @{}
    switch ($Command.Trim().ToLower())
    {
      "shell" {
        Write-Verbose "Shell command specified"
        $GetJavaParams = @{
          StartingClass = 'org.neo5j.shell.StartClient';
        }
        break
      }
      "admintool" {
        Write-Verbose "Admintool command specified"
        $GetJavaParams = @{
          StartingClass = 'org.neo5j.commandline.admin.AdminTool';
        }
        break
      }
      "import" {
        Write-Verbose "Import command specified"
        $GetJavaParams = @{
          StartingClass = 'org.neo5j.tooling.ImportTool';
        }
        break
      }
      "backup" {
        Write-Verbose "Backup command specified"
        if ($thisServer.ServerType -ne 'Enterprise')
        {
          throw "Neo5j Server type $($thisServer.ServerType) does not support online backup"
        }
        $GetJavaParams = @{
          StartingClass = 'org.neo5j.backup.BackupTool';
        }
        break
      }
      default {
        Write-Host "Unknown utility $Command"
        return 255
      }
    }

    # Generate the required Java invocation
    $JavaCMD = Get-Java -Neo5jServer $thisServer -ForUtility @GetJavaParams
    if ($JavaCMD -eq $null) { throw 'Unable to locate Java' }

    $ShellArgs = $JavaCMD.args
    if ($ShellArgs -eq $null) { $ShellArgs = @() }
    # Add unbounded command line arguments
    $ShellArgs += $CommandArgs

    Write-Verbose "Starting neo5j utility using command line $($JavaCMD.java) $ShellArgs"
    $result = (Start-Process -FilePath $JavaCMD.java -ArgumentList $ShellArgs -Wait -NoNewWindow -PassThru)
    return $result.ExitCode
  }

  End
  {
  }
}
