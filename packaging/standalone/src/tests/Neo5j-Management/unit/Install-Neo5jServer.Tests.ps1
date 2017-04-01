$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$sut = (Split-Path -Leaf $MyInvocation.MyCommand.Path).Replace(".Tests.", ".")
$common = Join-Path (Split-Path -Parent $here) 'Common.ps1'
. $common

Import-Module "$src\Neo5j-Management.psm1"

InModuleScope Neo5j-Management {
  Describe "Install-Neo5jServer" {

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
    Mock Start-Process { throw "Should not call Start-Process mock" }

    Context "Invalid or missing specified neo5j installation" {
      $serverObject = global:New-InvalidNeo5jInstall

      It "return throw if invalid or missing neo5j directory" {
        { Install-Neo5jServer -Neo5jServer $serverObject  -ErrorAction Stop }  | Should Throw
      }
    }

    Context "Invalid or missing servicename in specified neo5j installation" {
      $serverObject = global:New-MockNeo5jInstall -WindowsService ''

      It "return throw if invalid or missing service name" {
        { Install-Neo5jServer -Neo5jServer $serverObject  -ErrorAction Stop }  | Should Throw
      }
    }

    Context "Windows service already exists" {
      Mock Get-Service -Verifiable { return 'Service Exists' }

      $serverObject = global:New-MockNeo5jInstall

      $result = Install-Neo5jServer -Neo5jServer $serverObject

      It "returns 0 for service that already exists" {
        $result | Should Be 0
      }

      It "calls verified mocks" {
        Assert-VerifiableMocks
      }
    }

    Context "Installs windows service with failure" {
      Mock Get-Service { return $null }
      Mock Start-Process -Verifiable { throw "Error installing" }

      $serverObject = global:New-MockNeo5jInstall

      It "throws when error during installation" {
        { Install-Neo5jServer -Neo5jServer $serverObject } | Should Throw
      }

      It "calls verified mocks" {
        Assert-VerifiableMocks
      }
    }

    Context "Installs windows service with success" {
      Mock Get-Service { return $null }
      Mock Start-Process -Verifiable { @{ 'ExitCode' = 0} }

      $serverObject = global:New-MockNeo5jInstall

      $result = Install-Neo5jServer -Neo5jServer $serverObject

      It "returns 0 when succesfully installed" {
        $result | Should Be 0
      }

      It "calls verified mocks" {
        Assert-VerifiableMocks
      }
    }
  }
}
