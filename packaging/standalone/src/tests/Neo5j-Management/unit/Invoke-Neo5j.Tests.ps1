$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$sut = (Split-Path -Leaf $MyInvocation.MyCommand.Path).Replace(".Tests.", ".")
$common = Join-Path (Split-Path -Parent $here) 'Common.ps1'
. $common

Import-Module "$src\Neo5j-Management.psm1"

InModuleScope Neo5j-Management {
  Describe "Invoke-Neo5j" {

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
    $mockNeo5jHome = global:New-MockNeo5jInstall
    Mock Get-Neo5jEnv { $global:mockNeo5jHome } -ParameterFilter { $Name -eq 'NEO5J_HOME' }
    Mock Start-Process { throw "Should not call Start-Process mock" }
    # Mock helper functions
    Mock Start-Neo5jServer { 2 } -ParameterFilter { $Console -eq $true }
    Mock Start-Neo5jServer { 3 } -ParameterFilter { $Service -eq $true }
    Mock Stop-Neo5jServer { 4 }
    Mock Get-Neo5jStatus { 6 }
    Mock Install-Neo5jServer { 7 }
    Mock Uninstall-Neo5jServer { 8 }

    Context "No arguments" {
      $result = Invoke-Neo5j

      It "returns 1 if no arguments" {
        $result | Should Be 1
      }
    }

    # Helper functions - error
    Context "Helper function throws an error" {
      Mock Get-Neo5jStatus { throw "error" }

      It "returns non zero exit code on error" {
        Invoke-Neo5j 'status' -ErrorAction SilentlyContinue | Should Be 1
      }

      It "throws error when terminating error" {
        { Invoke-Neo5j 'status' -ErrorAction Stop } | Should Throw
      }
    }


    # Helper functions
    Context "Helper functions" {
      It "returns exitcode from console command" {
        Invoke-Neo5j 'console' | Should Be 2
      }

      It "returns exitcode from start command" {
        Invoke-Neo5j 'start' | Should Be 3
      }

      It "returns exitcode from stop command" {
        Invoke-Neo5j 'stop' | Should Be 4
      }

      It "returns exitcode from restart command" {
        Mock Start-Neo5jServer { 5 } -ParameterFilter { $Service -eq $true }
        Mock Stop-Neo5jServer { 0 }

        Invoke-Neo5j 'restart' | Should Be 5
      }

      It "returns exitcode from status command" {
        Invoke-Neo5j 'status' | Should Be 6
      }

      It "returns exitcode from install-service command" {
        Invoke-Neo5j 'install-service' | Should Be 7
      }

      It "returns exitcode from uninstall-service command" {
        Invoke-Neo5j 'uninstall-service' | Should Be 8
      }
    }

  }
}
