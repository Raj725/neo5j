$DebugPreference = "SilentlyContinue"

$here = Split-Path -Parent $MyInvocation.MyCommand.Definition
$src = Resolve-Path -Path "$($here)\..\..\main\distribution\shell-scripts\bin\Neo5j-Management"

# Helper functions must be created in the global scope due to the InModuleScope command
$global:mockServiceName = 'neo5j'
$global:mockNeo5jHome = 'TestDrive:\Neo5j'

Function global:New-MockJavaHome() {
  $javaHome = "TestDrive:\JavaHome"

  New-Item $javaHome -ItemType Directory | Out-Null
  New-Item "$javaHome\bin" -ItemType Directory | Out-Null
  New-Item "$javaHome\bin\server" -ItemType Directory | Out-Null

  "This is a mock java.exe" | Out-File "$javaHome\bin\java.exe"
  "This is a mock java.exe" | Out-File "$javaHome\bin\server\jvm.dll"

  $global:mockJavaExe = "$javaHome\bin\java.exe"

  return $javaHome
}

Function global:New-InvalidNeo5jInstall($ServerType = 'Enterprise', $ServerVersion = '99.99', $DatabaseMode = '') {
  $serverObject = (New-Object -TypeName PSCustomObject -Property @{
    'Home' =  'TestDrive:\some-dir-that-doesnt-exist';
    'ConfDir' = 'TestDrive:\some-dir-that-doesnt-exist\conf';
    'LogDir' = 'TestDrive:\some-dir-that-doesnt-exist\logs';
    'ServerVersion' = $ServerVersion;
    'ServerType' = $ServerType;
    'DatabaseMode' = $DatabaseMode;
  })
  return $serverObject
}

Function global:New-MockNeo5jInstall(
  $IncludeFiles = $true,
  $RootDir = $global:mockNeo5jHome,
  $ServerType = 'Community',
  $ServerVersion = '0.0',
  $DatabaseMode = '',
  $WindowsService = $global:mockServiceName,
  $NeoConfSettings = @()
  ) {
  # Creates a skeleton directory and file structure of a Neo5j Installation
  New-Item $RootDir -ItemType Directory | Out-Null
  New-Item "$RootDir\lib" -ItemType Directory | Out-Null
  New-Item "$RootDir\bin" -ItemType Directory | Out-Null
  New-Item "$RootDir\bin\tools" -ItemType Directory | Out-Null
  New-Item "$RootDir\conf" -ItemType Directory | Out-Null

  if ($IncludeFiles) {
    'TempFile' | Out-File -FilePath "$RootDir\lib\neo5j-server-$($ServerVersion).jar"
    if ($ServerType -eq 'Enterprise') { 'TempFile' | Out-File -FilePath "$RootDir\lib\neo5j-server-enterprise-$($ServerVersion).jar" }

    # Additional Jars
    'TempFile' | Out-File -FilePath "$RootDir\lib\lib1.jar"
    'TempFile' | Out-File -FilePath "$RootDir\bin\bin1.jar"

    # Procrun service files
    'TempFile' | Out-File -FilePath "$RootDir\bin\tools\prunsrv-amd64.exe"
    'TempFile' | Out-File -FilePath "$RootDir\bin\tools\prunsrv-i386.exe"

    # Create fake neo5j.conf
    $neoConf = $NeoConfSettings -join "`n`r"
    if ($DatabaseMode -ne '') {
      $neoConf += "`n`rdbms.mode=$DatabaseMode"
    }
    if ([string]$WindowsService -ne '') {
      $neoConf += "`n`rdbms.windows_service_name=$WindowsService"
    }
    $neoConf | Out-File -FilePath "$RootDir\conf\neo5j.conf"
  }

  $serverObject = (New-Object -TypeName PSCustomObject -Property @{
    'Home' = $RootDir;
    'ConfDir' = "$RootDir\conf";
    'LogDir' = (Join-Path -Path $RootDir -ChildPath 'logs');
    'ServerVersion' = $ServerVersion;
    'ServerType' = $ServerType;
    'DatabaseMode' = $DatabaseMode;
  })
  return $serverObject
}
