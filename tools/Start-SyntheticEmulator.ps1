param(
    [ValidateSet(26, 35, 36)][int]$Api = 35,
    [int]$Port = 5562,
    [string]$Sdk = "$env:LOCALAPPDATA\Android\Sdk"
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$avdRoot = Join-Path $projectRoot 'build\isolated-avds'
$avdName = "SkryApi$Api"
$avdDirectory = Join-Path $avdRoot "$avdName.avd"
$image = switch ($Api) {
    26 { 'system-images\android-26\google_apis_playstore\x86\' }
    35 { 'system-images\android-35\google_apis_playstore_tablet\x86_64\' }
    36 { 'system-images\android-36.1\google_apis_playstore\x86_64\' }
}
if (-not (Test-Path -LiteralPath (Join-Path $Sdk $image))) { throw "Missing SDK image: $image" }
$adb = Join-Path $Sdk 'platform-tools\adb.exe'
$devices = & $adb devices
if ($devices -match "emulator-$Port\s") { throw "Port $Port already has an emulator; refusing to reuse it." }
New-Item -ItemType Directory -Force -Path $avdDirectory | Out-Null
$architecture = if ($Api -eq 26) { 'x86' } else { 'x86_64' }
$config = @"
AvdId=$avdName
avd.ini.displayname=Skry isolated API $Api
avd.ini.encoding=UTF-8
abi.type=$architecture
hw.cpu.arch=$architecture
hw.cpu.ncore=2
hw.ramSize=1536
hw.lcd.width=720
hw.lcd.height=1280
hw.lcd.density=320
hw.keyboard=yes
hw.mainKeys=no
hw.gpu.enabled=yes
hw.gpu.mode=swiftshader
hw.audioInput=no
hw.camera.back=none
hw.camera.front=none
disk.dataPartition.size=2G
image.sysdir.1=$image
tag.id=google_apis_playstore
target=android-$Api
fastboot.forceColdBoot=yes
fastboot.forceFastBoot=no
"@
if (-not (Test-Path -LiteralPath (Join-Path $avdDirectory 'config.ini'))) {
    [IO.File]::WriteAllText((Join-Path $avdDirectory 'config.ini'), $config)
}
[IO.File]::WriteAllText((Join-Path $avdRoot "$avdName.ini"), "avd.ini.encoding=UTF-8`npath=$avdDirectory`ntarget=android-$Api`n")
$env:ANDROID_AVD_HOME = $avdRoot
$env:ANDROID_USER_HOME = Join-Path $projectRoot 'build\emulator-home'
$env:ANDROID_HOME = $Sdk
$emulatorProcess = Start-Process -FilePath (Join-Path $Sdk 'emulator\emulator.exe') `
    -ArgumentList "-avd $avdName -port $Port -no-window -no-snapshot -no-audio -no-boot-anim -gpu swiftshader -camera-back none -camera-front none" `
    -WindowStyle Hidden -PassThru `
    -RedirectStandardOutput (Join-Path $projectRoot "build\emulator-api$Api.log") `
    -RedirectStandardError (Join-Path $projectRoot "build\emulator-api$Api-error.log")
Write-Output "Started $avdName, emulator-$Port, process $($emulatorProcess.Id). All AVD data is under $avdRoot."
