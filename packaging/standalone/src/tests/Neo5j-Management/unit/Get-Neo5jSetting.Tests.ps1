$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$sut = (Split-Path -Leaf $MyInvocation.MyCommand.Path).Replace(".Tests.", ".")
$common = Join-Path (Split-Path -Parent $here) 'Common.ps1'
. $common

Import-Module "$src\Neo5j-Management.psm1"

InModuleScope Neo5j-Management {
  Describe "Get-Neo5jSetting" {

    Context "Invalid or missing specified neo5j installation" {
      $serverObject = global:New-InvalidNeo5jInstall

      $result = Get-Neo5jSetting -Neo5jServer $serverObject

      It "return null if invalid directory" {
        $result | Should BeNullOrEmpty
      }
    }

    Context "Missing configuration file is ignored" {
      $serverObject = global:New-MockNeo5jInstall

      "setting=value" | Out-File -FilePath "$($serverObject.Home)\conf\neo5j.conf"
      # Remove the neo5j-wrapper
      $wrapperFile = "$($serverObject.Home)\conf\neo5j-wrapper.conf"
      if (Test-Path -Path $wrapperFile) { Remove-Item -Path $wrapperFile | Out-Null }

      $result = Get-Neo5jSetting -Neo5jServer $serverObject

      It "ignore the missing file" {
        $result.Name | Should Be "setting"
        $result.Value | Should Be "value"
      }
    }

    Context "Simple configuration settings" {
      $serverObject = global:New-MockNeo5jInstall

      "setting1=value1" | Out-File -FilePath "$($serverObject.Home)\conf\neo5j.conf"
      "setting2=value2" | Out-File -FilePath "$($serverObject.Home)\conf\neo5j-wrapper.conf"

      $result = Get-Neo5jSetting -Neo5jServer $serverObject

      It "one setting per file" {
        $result.Count | Should Be 2
      }

      # Parse the results and make sure the expected results are there
      $unknownSetting = $false
      $neo5jProperties = $false
      $neo5jServerProperties = $false
      $neo5jWrapper = $false
      $result | ForEach-Object -Process {
        $setting = $_
        switch ($setting.Name) {
          'setting1' { $neo5jServerProperties = ($setting.ConfigurationFile -eq 'neo5j.conf') -and ($setting.IsDefault -eq $false) -and ($setting.Value -eq 'value1') }
          'setting2' { $neo5jWrapper =          ($setting.ConfigurationFile -eq 'neo5j-wrapper.conf') -and ($setting.IsDefault -eq $false) -and ($setting.Value -eq 'value2') }
          default { $unknownSetting = $true}
        }
      }

      It "returns settings for file neo5j.conf" {
        $neo5jServerProperties | Should Be $true
      }
      It "returns settings for file neo5j-wrapper.conf" {
        $neo5jWrapper | Should Be $true
      }

      It "returns no unknown settings" {
        $unknownSetting | Should Be $false
      }
    }

    Context "Configuration settings with multiple values" {
      $serverObject = global:New-MockNeo5jInstall

      "setting1=value1`n`rsetting2=value2`n`rsetting2=value3`n`rsetting2=value4" | Out-File -FilePath "$($serverObject.Home)\conf\neo5j.conf"
      "" | Out-File -FilePath "$($serverObject.Home)\conf\neo5j-wrapper.conf"

      $result = Get-Neo5jSetting -Neo5jServer $serverObject

      # Parse the results and make sure the expected results are there
      $singleSetting = $null
      $multiSetting = $null
      $result | ForEach-Object -Process {
        $setting = $_
        switch ($setting.Name) {
          'setting1' { $singleSetting = $setting }
          'setting2' { $multiSetting = $setting }
        }
      }

      It "returns single settings" {
        ($singleSetting -ne $null) | Should Be $true
      }
      It "returns a string for single settings" {
        $singleSetting.Value.GetType().ToString() | Should Be "System.String"
      }

      It "returns multiple settings" {
        ($multiSetting -ne $null) | Should Be $true
      }
      It "returns an object array for multiple settings" {
        $multiSetting.Value.GetType().ToString() | Should Be "System.Object[]"
      }
      It "returns an object array for multiple settings with the correct size" {
        $multiSetting.Value.Count | Should Be 3
      }
    }
  }
}
