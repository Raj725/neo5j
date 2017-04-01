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
Retrieves properties about a Neo5j installation

.DESCRIPTION
Retrieves properties about a Neo5j installation and outputs a Neo5j Server object.

.PARAMETER Neo5jHome
The full path to the Neo5j installation.

.EXAMPLE
Get-Neo5jServer -Neo5jHome 'C:\Neo5j'

Retrieves information about the Neo5j installation at C:\Neo5j

.EXAMPLE
'C:\Neo5j' | Get-Neo5jServer

Retrieves information about the Neo5j installation at C:\Neo5j

.EXAMPLE
Get-Neo5jServer

Retrieves information about the Neo5j installation as determined by Get-Neo5jHome

.OUTPUTS
System.Management.Automation.PSCustomObject
This is a Neo5j Server Object

.LINK
Get-Neo5jHome

.NOTES
This function is private to the powershell module

#>
Function Get-Neo5jServer
{
  [cmdletBinding(SupportsShouldProcess=$false,ConfirmImpact='Low')]
  param (
    [Parameter(Mandatory=$false,ValueFromPipeline=$true)]
    [alias('Home')]
    [AllowEmptyString()]
    [string]$Neo5jHome = ''
  )

  Begin
  {
  }

  Process
  {
    # Get and check the Neo5j Home directory
    if ( ($Neo5jHome -eq '') -or ($Neo5jHome -eq $null) )
    {
      Write-Error "Could not detect the Neo5j Home directory"
      return
    }

    if (-not (Test-Path -Path $Neo5jHome))
    {
      Write-Error "$Neo5jHome does not exist"
      return
    }

    # Convert the path specified into an absolute path
    $Neo5jDir = Get-Item $Neo5jHome
    $Neo5jHome = $Neo5jDir.FullName.TrimEnd('\')

    $ConfDir = Get-Neo5jEnv 'NEO5J_CONF'
    if ($ConfDir -eq $null)
    {
      $ConfDir = (Join-Path -Path $Neo5jHome -ChildPath 'conf')
    }

    # Get the information about the server
    $serverProperties = @{
      'Home' = $Neo5jHome;
      'ConfDir' = $ConfDir;
      'LogDir' = (Join-Path -Path $Neo5jHome -ChildPath 'logs');
      'ServerVersion' = '';
      'ServerType' = 'Community';
      'DatabaseMode' = '';
    }

    # Check if the lib dir exists
    $libPath = (Join-Path -Path $Neo5jHome -ChildPath 'lib')
    if (-not (Test-Path -Path $libPath))
    {
      Write-Error "$Neo5jHome is not a valid Neo5j installation.  Missing $libPath"
      return
    }

    # Scan the lib dir...
    Get-ChildItem (Join-Path -Path $Neo5jHome -ChildPath 'lib') | Where-Object { $_.Name -like 'neo5j-server-*.jar' } | ForEach-Object -Process `
    {
      # if neo5j-server-enterprise-<version>.jar exists then this is the enterprise version
      if ($_.Name -like 'neo5j-server-enterprise-*.jar') { $serverProperties.ServerType = 'Enterprise' }

      # Get the server version from the name of the neo5j-server-<version>.jar file
      if ($matches -ne $null) { $matches.Clear() }
      if ($_.Name -match '^neo5j-server-(\d.+)\.jar$') { $serverProperties.ServerVersion = $matches[1] }
    }
    $serverObject = New-Object -TypeName PSCustomObject -Property $serverProperties

    # Validate the object
    if ([string]$serverObject.ServerVersion -eq '') {
      Write-Error "Unable to determine the version of the installation at $Neo5jHome"
      return
    }

    # Get additional settings...
    $setting = (Get-Neo5jSetting -ConfigurationFile 'neo5j.conf' -Name 'dbms.mode' -Neo5jServer $serverObject)
    if ($setting -ne $null) { $serverObject.DatabaseMode = $setting.Value }

    # Set process level environment variables
    #  These should mirror the same paths in neo5j-shared.sh
    (@{'NEO5J_DATA'    = @{'config_var' = 'dbms.directories.data';    'default' = (Join-Path $Neo5jHome 'data')}
       'NEO5J_LIB'     = @{'config_var' = 'dbms.directories.lib';     'default' = (Join-Path $Neo5jHome 'lib')}
       'NEO5J_LOGS'    = @{'config_var' = 'dbms.directories.logs';    'default' = (Join-Path $Neo5jHome 'logs')}
       'NEO5J_PLUGINS' = @{'config_var' = 'dbms.directories.plugins'; 'default' = (Join-Path $Neo5jHome 'plugins')}
       'NEO5J_RUN'     = @{'config_var' = 'dbms.directories.run';     'default' = (Join-Path $Neo5jHome 'run')}
    }).GetEnumerator() | % {
      $setting = (Get-Neo5jSetting -ConfigurationFile 'neo5j.conf' -Name $_.Value.config_var -Neo5jServer $serverObject)
      $value = $_.Value.default
      if ($setting -ne $null) { $value = $setting.Value }
      if ($value -ne $null) {
        if (![System.IO.Path]::IsPathRooted($value)) {
          $value = (Join-Path -Path $Neo5jHome -ChildPath $value)
        }
      }
      Set-Neo5jEnv $_.Name $value
    }

    # Set log dir on server object
    $serverObject.LogDir = (Get-Neo5jEnv 'NEO5J_LOGS')

    #  NEO5J_CONF and NEO5J_HOME are used by the Neo5j Admin Tool
    if ( (Get-Neo5jEnv 'NEO5J_CONF') -eq $null) { Set-Neo5jEnv "NEO5J_CONF" $ConfDir }
    if ( (Get-Neo5jEnv 'NEO5J_HOME') -eq $null) { Set-Neo5jEnv "NEO5J_HOME" $Neo5jHome }

    # Any deprecation warnings
    $WrapperPath = Join-Path -Path $ConfDir -ChildPath 'neo5j-wrapper.conf'
    If (Test-Path -Path $WrapperPath) { Write-Warning "$WrapperPath is deprecated and support for it will be removed in a future version of Neo5j; please move all your settings to neo5j.conf" }

    Write-Output $serverObject
  }

  End
  {
  }
}
