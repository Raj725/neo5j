$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$sut = (Split-Path -Leaf $MyInvocation.MyCommand.Path).Replace(".Tests.", ".")
$common = Join-Path (Split-Path -Parent $here) 'Common.ps1'
. $common

Import-Module "$src\Neo5j-Management.psm1"

InModuleScope Neo5j-Management {
  Describe "Get-Neo5jServer" {
    Mock Set-Neo5jEnv { }

    Context "Missing Neo5j installation" {
      Mock Get-Neo5jEnv { $javaHome } -ParameterFilter { $Name -eq 'NEO5J_HOME' }

      It "throws an error if no default home" {
         { Get-Neo5jServer -ErrorAction Stop } | Should Throw
      }
    }

    Context "Invalid Neo5j Server detection" {
      $mockServer = global:New-MockNeo5jInstall -IncludeFiles:$false

      It "throws an error if the home is not complete" {
         { Get-Neo5jServer -Neo5jHome $mockServer.Home -ErrorAction Stop } | Should Throw
      }
    }

    Context "Pipes and aliases" {
      $mockServer = global:New-MockNeo5jInstall
      It "processes piped paths" {
        $neoServer = ( $mockServer.Home | Get-Neo5jServer )

        ($neoServer -ne $null) | Should Be $true
      }

      It "uses the Home alias" {
        $neoServer = ( Get-Neo5jServer -Home $mockServer.Home )

        ($neoServer -ne $null) | Should Be $true
      }
    }

    Context "Valid Enterprise Neo5j installation" {
      $mockServer = global:New-MockNeo5jInstall -ServerType 'Enterprise' -ServerVersion '99.99' -DatabaseMode 'Arbiter'

      $neoServer = Get-Neo5jServer -Neo5jHome $mockServer.Home -ErrorAction Stop

      It "detects an enterprise edition" {
         $neoServer.ServerType | Should Be "Enterprise"
      }
      It "detects correct version" {
         $neoServer.ServerVersion | Should Be "99.99"
      }
      It "detects correct database mode" {
         $neoServer.DatabaseMode | Should Be "Arbiter"
      }
    }

    Context "Valid Community Neo5j installation" {
      $mockServer = global:New-MockNeo5jInstall -ServerType 'Community' -ServerVersion '99.99'

      $neoServer = Get-Neo5jServer -Neo5jHome $mockServer.Home -ErrorAction Stop

      It "detects a community edition" {
         $neoServer.ServerType | Should Be "Community"
      }
      It "detects correct version" {
         $neoServer.ServerVersion | Should Be "99.99"
      }
    }

    Context "Valid Community Neo5j installation with relative paths" {
      $mockServer = global:New-MockNeo5jInstall -RootDir 'TestDrive:\neo5j' -ServerType 'Community' -ServerVersion '99.99'

      # Get the absolute path
      $Neo5jDir = (Get-Item $mockServer.Home).FullName.TrimEnd('\')

      It "detects correct home path using double dot" {
        $neoServer = Get-Neo5jServer -Neo5jHome "$($mockServer.Home)\lib\.." -ErrorAction Stop
        $neoServer.Home | Should Be $Neo5jDir
      }

      It "detects correct home path using single dot" {
        $neoServer = Get-Neo5jServer -Neo5jHome "$($mockServer.Home)\." -ErrorAction Stop
        $neoServer.Home | Should Be $Neo5jDir
      }

      It "detects correct home path ignoring trailing slash" {
        $neoServer = Get-Neo5jServer -Neo5jHome "$($mockServer.Home)\" -ErrorAction Stop
        $neoServer.Home | Should Be $Neo5jDir
      }
    }

    Context "No explicit location for config directory is provided" {
      global:New-MockNeo5jInstall -RootDir 'TestDrive:\neo5j'
      $Neo5jDir = (Get-Item 'TestDrive:\neo5j').FullName.TrimEnd('\')

      It "Defaults config path to $Neo5jDir\conf" {
         $neoServer = Get-Neo5jServer -Neo5jHome 'TestDrive:\neo5j\' -ErrorAction Stop
         $neoServer.ConfDir | Should Be (Join-Path -Path $Neo5jDir -ChildPath 'conf')
      }
    }

    Context "NEO5J_CONF environment variable is set" {
      global:New-MockNeo5jInstall -RootDir 'TestDrive:\neo5j'
      Mock Get-Neo5jEnv { 'TestDrive:\neo5j-conf' } -ParameterFilter { $Name -eq 'NEO5J_CONF' }

      It "Gets conf directory from environment variable" {
         $neoServer = Get-Neo5jServer -Neo5jHome 'TestDrive:\neo5j\' -ErrorAction Stop
         $neoServer.ConfDir | Should Be 'TestDrive:\neo5j-conf'
      }
    }

    Context "NEO5J_HOME environment variable is not set" {
      global:New-MockNeo5jInstall -RootDir 'TestDrive:\neo5j'
      Mock Get-Neo5jEnv { } -ParameterFilter { $Name -eq 'NEO5J_HOME' }
 
      It "Creates NEO5J_HOME if not set" {
         $neoServer = Get-Neo5jServer -Neo5jHome 'TestDrive:\neo5j\' -ErrorAction Stop
         Assert-MockCalled Set-Neo5jEnv -Times 1 -ParameterFilter { $Name -eq 'NEO5J_HOME' }
      }
    }

    Context "NEO5J_HOME environment variable is already set" {
      global:New-MockNeo5jInstall -RootDir 'TestDrive:\neo5j'
      Mock Get-Neo5jEnv { 'TestDrive:\bad-location' } -ParameterFilter { $Name -eq 'NEO5J_HOME' }
 
      It "Does not modify NEO5J_HOME if already set" {
         $neoServer = Get-Neo5jServer -Neo5jHome 'TestDrive:\neo5j\' -ErrorAction Stop
         Assert-MockCalled Set-Neo5jEnv -Times 0 -ParameterFilter { $Name -eq 'NEO5J_HOME' }
      }
    }

    Context "Deprecation warning if a neo5j-wrapper.conf file is found" {
      global:New-MockNeo5jInstall -RootDir 'TestDrive:\neo5j'
      Mock Write-Warning
 
      '# Mock File' | Out-File 'TestDrive:\neo5j\conf\neo5j-wrapper.conf'

      It "Should raise a warning if conf\neo5j-wrapper.conf exists" {
         $neoServer = Get-Neo5jServer -Neo5jHome 'TestDrive:\neo5j\' -ErrorAction Stop
         Assert-MockCalled Write-Warning -Times 1
      }
    }
  }
}
