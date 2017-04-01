$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$sut = (Split-Path -Leaf $MyInvocation.MyCommand.Path).Replace(".Tests.", ".")
$common = Join-Path (Split-Path -Parent $here) 'Common.ps1'
. $common

Import-Module "$src\Neo5j-Management.psm1"

InModuleScope Neo5j-Management {
  Describe "Stop-Neo5jServer" {

    # Setup mocking environment
    #  Mock Java environment
    $javaHome = global:New-MockJavaHome
    Mock Get-Neo5jEnv { $javaHome } -ParameterFilter { $Name -eq 'JAVA_HOME' }
    Mock Set-Neo5jEnv { }
    Mock Test-Path { $false } -ParameterFilter {
      $Path -like 'Registry::*\JavaSoft\Java Runtime Environment'
    }
    Mock Get-ItemProperty { $null } -ParameterFilter {
      $Path -like 'Registry::*\JavaSoft\Java Runtime Environment*'
    }
    # Mock Neo5j environment
    Mock Get-Neo5jEnv { $global:mockNeo5jHome } -ParameterFilter { $Name -eq 'NEO5J_HOME' }

    Context "Missing service name in configuration files" {
      Mock Stop-Service { }

      $serverObject = global:New-MockNeo5jInstall -WindowsService ''

      It "throws error for missing service name in configuration file" {
        { Stop-Neo5jServer -Neo5jServer $serverObject -ErrorAction Stop } | Should Throw
      }
    }

    Context "Stop service succesfully but didn't stop" {
      Mock Stop-Service { throw "Called Stop-Service incorrectly"}
      Mock Stop-Service -Verifiable { @{ Status = 'Stop Pending'} } -ParameterFilter { $Name -eq $global:mockServiceName}

      $serverObject = global:New-MockNeo5jInstall

      $result = Stop-Neo5jServer -Neo5jServer $serverObject
      It "result is 2" {
        $result | Should Be 2
      }

      It "calls verified mocks" {
        Assert-VerifiableMocks
      }
    }

    Context "Stop service succesfully" {
      Mock Stop-Service { throw "Called Stop-Service incorrectly"}
      Mock Stop-Service -Verifiable { @{ Status = 'Stopped'} } -ParameterFilter { $Name -eq $global:mockServiceName}

      $serverObject = global:New-MockNeo5jInstall

      $result = Stop-Neo5jServer -Neo5jServer $serverObject
      It "result is 0" {
        $result | Should Be 0
      }

      It "calls verified mocks" {
        Assert-VerifiableMocks
      }
    }
  }
}
