$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$sut = (Split-Path -Leaf $MyInvocation.MyCommand.Path).Replace(".Tests.", ".")
$common = Join-Path (Split-Path -Parent $here) 'Common.ps1'
. $common

Import-Module "$src\Neo5j-Management.psm1"

InModuleScope Neo5j-Management {
  Describe "Get-Neo5jStatus" {

    $mockServerObject = global:New-MockNeo5jInstall
    Mock Set-Neo5jEnv { }
    Mock Get-Neo5jEnv { $mockServerObject.Home } -ParameterFilter { $Name -eq 'NEO5J_HOME' }

    Context "Missing service name in configuration files" {
      Mock -Verifiable Get-Neo5jWindowsServiceName { throw "Missing service name" }

      It "throws error for missing service name in configuration file" {
        { Get-Neo5jStatus -Neo5jServer $mockServerObject -ErrorAction Stop } | Should Throw
      }

      It "calls verified mocks" {
        Assert-VerifiableMocks
      }
    }

    Context "Service not installed" {
      Mock Get-Service -Verifiable { throw "Missing Service"}

      $result = Get-Neo5jStatus -Neo5jServer $mockServerObject
      It "result is 3" {
        $result | Should Be 3
      }

      It "calls verified mocks" {
        Assert-VerifiableMocks
      }
    }

    Context "Service not installed but not running" {
      Mock Get-Service -Verifiable { @{ Status = 'Stopped' }}

      $result = Get-Neo5jStatus -Neo5jServer $mockServerObject
      It "result is 3" {
        $result | Should Be 3
      }

      It "calls verified mocks" {
        Assert-VerifiableMocks
      }
    }

    Context "Service is running" {
      Mock Get-Service -Verifiable { @{ Status = 'Running' }}

      $result = Get-Neo5jStatus -Neo5jServer $mockServerObject
      It "result is 0" {
        $result | Should Be 0
      }

      It "calls verified mocks" {
        Assert-VerifiableMocks
      }
    }
  }
}
