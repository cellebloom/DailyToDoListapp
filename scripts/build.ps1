# Build helper for DailyToDoListapp.
#
# Why this exists: the JBR bundled with Android Studio on this machine is broken
# (it is missing lib/jvm.cfg, so java.exe cannot start). Gradle therefore needs an
# explicit JDK 21. Using this script keeps every build invocation consistent.
#
# Usage:
#   .\scripts\build.ps1 test      # compile + run unit tests
#   .\scripts\build.ps1 build     # assembleDebug
#   .\scripts\build.ps1 install   # assembleDebug + install on the running emulator
#   .\scripts\build.ps1 all       # test + build

param(
    [ValidateSet('test', 'build', 'install', 'all')]
    [string]$Task = 'build'
)

$ErrorActionPreference = 'Stop'

$jdk = 'C:\Program Files\Android\openjdk\jdk-21.0.8'
if (-not (Test-Path (Join-Path $jdk 'bin\java.exe'))) {
    throw "JDK 21 not found at $jdk. Update `$jdk in this script."
}
$env:JAVA_HOME = $jdk

$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

# The wrapper writes .lck files into ~/.gradle, which the default sandbox blocks.
# Prefer the already-extracted distribution when the wrapper cannot start.
$gradleBat = Join-Path $env:USERPROFILE '.gradle\wrapper\dists\gradle-9.6.0-bin\42k10rwplmzkhuboz9kdazi7s\gradle-9.6.0\bin\gradle.bat'
if (-not (Test-Path $gradleBat)) {
    throw "Gradle 9.6.0 distribution not found. Run .\gradlew.bat once with wider sandbox access to unpack it."
}

$adb = 'C:\Users\celle\AppData\Local\Android\Sdk\platform-tools\adb.exe'

switch ($Task) {
    'test' { & $gradleBat --offline :app:testDebugUnitTest --console=plain }
    'build' { & $gradleBat --offline :app:assembleDebug --console=plain }
    'install' {
        & $gradleBat --offline :app:assembleDebug --console=plain
        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
        & $adb install -r 'app\build\outputs\apk\debug\app-debug.apk'
    }
    'all' {
        & $gradleBat --offline :app:testDebugUnitTest :app:assembleDebug --console=plain
    }
}

exit $LASTEXITCODE
