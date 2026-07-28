# Drive onboarding on BlueStacks, then verify main home stays up without FATAL.
$ErrorActionPreference = "Continue"
$SdkAdb = Join-Path $PSScriptRoot "..\android-sdk\platform-tools\adb.exe" | Resolve-Path -ErrorAction Stop
$env:Path = "$(Split-Path $SdkAdb);$env:Path"
$serial = if ($env:ADB_SERIAL) { $env:ADB_SERIAL } else { "emulator-5554" }
$pkg = "rip.moth.cocoonshell.froglog"
$apk = Join-Path $PSScriptRoot "..\android\dist\cocoon-froglog.apk" | Resolve-Path -ErrorAction Stop

function Get-UiDump {
  adb -s $serial shell uiautomator dump /sdcard/ui.xml 2>$null | Out-Null
  adb -s $serial shell cat /sdcard/ui.xml 2>$null
}

function Tap-Text {
  param([string[]]$Patterns)
  $xml = Get-UiDump
  if (-not $xml) { return $false }
  foreach ($pat in $Patterns) {
    if ($xml -match 'text="' + [regex]::Escape($pat) + '"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"') {
      $x = ([int]$Matches[1] + [int]$Matches[3]) / 2
      $y = ([int]$Matches[2] + [int]$Matches[4]) / 2
      adb -s $serial shell input tap ([int]$x) ([int]$y)
      return $true
    }
    if ($xml -match 'content-desc="' + [regex]::Escape($pat) + '"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"') {
      $x = ([int]$Matches[1] + [int]$Matches[3]) / 2
      $y = ([int]$Matches[2] + [int]$Matches[4]) / 2
      adb -s $serial shell input tap ([int]$x) ([int]$y)
      return $true
    }
  }
  return $false
}

function Tap-AnyClickable {
  $xml = Get-UiDump
  if ($xml -match 'clickable="true"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"') {
    $x = ([int]$Matches[1] + [int]$Matches[3]) / 2
    $y = ([int]$Matches[2] + [int]$Matches[4]) / 2
    if ($y -gt 200) {
      adb -s $serial shell input tap ([int]$x) ([int]$y)
      return $true
    }
  }
  return $false
}

Write-Host "Installing $apk ..."
adb -s $serial install -r -g $apk | Out-Null
adb -s $serial logcat -c
adb -s $serial shell am force-stop $pkg
adb -s $serial shell am start -n "$pkg/rip.moth.cocoonshell.MainActivity" | Out-Null
Start-Sleep -Seconds 8

$labels = @(
  "Continue", "Get started", "Get Started", "Next", "Done", "Allow", "ALLOW",
  "Skip", "Not now", "Accept", "I agree", "Agree", "Start", "Finish", "Go",
  "Enable", "OK", "Got it", "Let's go", "Proceed", "Confirm"
)

for ($i = 0; $i -lt 40; $i++) {
  $resumed = adb -s $serial shell "dumpsys activity activities" 2>$null
  if ($resumed -match "mResumedActivity.*MainActivity" -and $resumed -notmatch "Onboarding") {
    Write-Host "Reached main activity after $i onboarding steps"
    break
  }
  $tapped = $false
  foreach ($l in $labels) {
    if (Tap-Text @($l)) { $tapped = $true; Write-Host "Tapped: $l"; break }
  }
  if (-not $tapped) {
    if (Tap-Text @("While using the app", "Only this time")) { $tapped = $true }
  }
  if (-not $tapped) { Tap-AnyClickable | Out-Null }
  Start-Sleep -Seconds 3
}

Write-Host "Waiting 45s on home ..."
Start-Sleep -Seconds 45
$pid = adb -s $serial shell pidof $pkg
$resumed2 = adb -s $serial shell "dumpsys activity activities" 2>$null | Select-String "mResumedActivity"
$fatals = adb -s $serial logcat -d | Select-String -Pattern "FATAL EXCEPTION|VerifyError|NoClassDefFoundError.*RuntimeShader|Process: $pkg" | Select-Object -Last 20
Write-Host "pid=$pid"
Write-Host $resumed2
if ($fatals) {
  Write-Host "=== CRASHES ==="
  $fatals
  exit 1
}
if (-not $pid) {
  Write-Host "Process not running"
  exit 1
}
Write-Host "OK: app alive after onboarding + home wait"
exit 0
