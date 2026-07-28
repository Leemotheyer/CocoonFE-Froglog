# Capture Cocoon (Froglog) crash log via SDK platform-tools adb (not BlueStacks HD-Adb).
$SdkAdb = Join-Path $PSScriptRoot "..\android-sdk\platform-tools\adb.exe" | Resolve-Path -ErrorAction Stop
$env:Path = "$(Split-Path $SdkAdb);$env:Path"
Get-Process HD-Adb -ErrorAction SilentlyContinue | Stop-Process -Force

$serial = if ($env:ADB_SERIAL) { $env:ADB_SERIAL } else { "emulator-5554" }
$pkg = "rip.moth.cocoonshell.froglog"

adb start-server
adb -s $serial wait-for-device
adb -s $serial logcat -c
adb -s $serial shell am force-stop $pkg
adb -s $serial shell am start -n "$pkg/rip.moth.cocoonshell.MainActivity"
Start-Sleep -Seconds 50
$out = Join-Path $PSScriptRoot "..\android\dist\crash-logcat.txt" | Resolve-Path -ErrorAction SilentlyContinue
if (-not $out) { $out = Join-Path $PSScriptRoot "..\android\dist\crash-logcat.txt" }
adb -s $serial logcat -d | Out-File -FilePath $out -Encoding utf8
Write-Host "Saved: $out"
Select-String -Path $out -Pattern "FATAL EXCEPTION|VerifyError|AndroidRuntime|Caused by:" | Select-Object -Last 35
