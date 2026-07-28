# Install/test Cocoon (Froglog) via project android-sdk platform-tools adb.
param(
  [string]$Apk = "$PSScriptRoot\..\android\dist\cocoon-froglog.apk"
)
$ErrorActionPreference = "Stop"
$SdkAdb = Join-Path $PSScriptRoot "..\android-sdk\platform-tools\adb.exe" | Resolve-Path -ErrorAction Stop
$env:Path = "$(Split-Path $SdkAdb);$env:Path"
Get-Process HD-Adb -ErrorAction SilentlyContinue | Stop-Process -Force

$serial = if ($env:ADB_SERIAL) { $env:ADB_SERIAL } else { "emulator-5554" }
$pkg = "rip.moth.cocoonshell.froglog"

adb start-server
adb -s $serial wait-for-device
adb -s $serial uninstall $pkg 2>$null | Out-Null
Write-Host "Installing $Apk ..."
adb -s $serial install -r -g $Apk
adb -s $serial logcat -c
adb -s $serial shell am force-stop $pkg
adb -s $serial shell am start -W -n "$pkg/rip.moth.cocoonshell.MainActivity"
Start-Sleep -Seconds 35
Select-String -InputObject (adb -s $serial logcat -d) -Pattern "FATAL EXCEPTION|VerifyError|AndroidRuntime" | Select-Object -Last 25
