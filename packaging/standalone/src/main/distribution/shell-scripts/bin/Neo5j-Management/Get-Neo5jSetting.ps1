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
TODO UPDATE HELPTEXT
Retrieves properties about a Neo5j installation

.DESCRIPTION
Retrieves properties about a Neo5j installation

.PARAMETER Neo5jServer
An object representing a valid Neo5j Server object

.PARAMETER ConfigurationFile
The name of the configuration file or files to parse.  If not specified the default set of all configuration files are used.  Do not use the full path, just the filename, the path is relative to '[Neo5jHome]\conf'

.PARAMETER Name
The name of the property to retrieve.  If not specified, all properties are returned.

.EXAMPLE
Get-Neo5jSetting -Neo5jServer $ServerObject | Format-Table

Retrieves all settings for the Neo5j installation at $ServerObject

.EXAMPLE
Get-Neo5jSetting -Neo5jServer $ServerObject -Name 'dbms.active_database'

Retrieves all settings with the name 'dbms.active_database' from the Neo5j installation at $ServerObject

.EXAMPLE
Get-Neo5jSetting -Neo5jServer $ServerObject -Name 'dbms.active_database' -ConfigurationFile 'neo5j.conf'

Retrieves all settings with the name 'dbms.active_database' from the Neo5j installation at $ServerObject in 'neo5j.conf'

.OUTPUTS
System.Management.Automation.PSCustomObject
This is a Neo5j Setting Object
Properties;
'Name' : Name of the property
'Value' : Value of the property.  Multivalue properties are string arrays (string[])
'ConfigurationFile' : Name of the configuration file where the setting is defined
'IsDefault' : Whether this setting is a default value (Reserved for future use)
'Neo5jHome' : Path to the Neo5j installation

.LINK
Get-Neo5jServer 

.NOTES
This function is private to the powershell module

#>
Function Get-Neo5jSetting
{
  [cmdletBinding(SupportsShouldProcess=$false,ConfirmImpact='Low')]
  param (
    [Parameter(Mandatory=$true,ValueFromPipeline=$true)]
    [PSCustomObject]$Neo5jServer

    ,[Parameter(Mandatory=$false)]
    [string[]]$ConfigurationFile = $null

    ,[Parameter(Mandatory=$false)]
    [string]$Name = ''
  )
  
  Begin
  {
  }

  Process
  {
    # Get the Neo5j Server information
    if ($Neo5jServer -eq $null) { return }

    # Set the default list of configuration files    
    if ($ConfigurationFile -eq $null)
    {
      $ConfigurationFile = ('neo5j.conf','neo5j-wrapper.conf')
    }
   
    $ConfigurationFile | ForEach-Object -Process `
    {
      $filename = $_
      $filePath = Join-Path -Path $Neo5jServer.ConfDir -ChildPath $filename
      if (Test-Path -Path $filePath)
      {
        $keyPairsFromFile = Get-KeyValuePairsFromConfFile -filename $filePath        
      }
      else
      {
        $keyPairsFromFile = $null
      }
      
      if ($keyPairsFromFile -ne $null)
      {
        $keyPairsFromFile.GetEnumerator() | Where-Object { ($Name -eq '') -or ($_.Name -eq $Name) } | ForEach-Object -Process `
        {
          $properties = @{
            'Name' = $_.Name;
            'Value' = $_.Value;
            'ConfigurationFile' = $filename;
            'IsDefault' = $false;
            'Neo5jHome' = $Neo5jServer.Home;
          }

          Write-Output (New-Object -TypeName PSCustomObject -Property $properties)
        }
      }
    }
  }
  
  End
  {
  }
}
