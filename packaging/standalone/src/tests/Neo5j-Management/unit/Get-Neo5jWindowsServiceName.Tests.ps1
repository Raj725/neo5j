$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$sut = (Split-Path -Leaf $MyInvocation.MyCommand.Path).Replace(".Tests.", ".")
$common = Join-Path (Split-Path -Parent $here) 'Common.ps1'
. $common

Import-Module "$src\Neo5j-Management.psm1"

InModuleScope Neo5j-Management {
  Describe "Get-Neo5jWindowsServiceName" {

    Mock Get-Neo5jEnv { $global:mockNeo5jHome } -ParameterFilter { $Name -eq 'NEO5J_HOME' }
    Mock Set-Neo5jEnv { }

    Context "Missing service name in configuration files" {
      $serverObject = global:New-MockNeo5jInstall -WindowsService ''

      It "throws error for missing service name in configuration file" {
        { Get-Neo5jWindowsServiceName -Neo5jServer $serverObject -ErrorAction Stop } | Should Throw
      }
    }

    Context "Service name in configuration files" {
      $serverObject = global:New-MockNeo5jInstall

      It "returns Service name in configuration file" {
        Get-Neo5jWindowsServiceName -Neo5jServer $serverObject | Should be $global:mockServiceName
      }
    }
  }
}
