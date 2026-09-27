$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$socketDirectory = Join-Path $projectRoot '.gradle\java-sockets'
$previousJavaToolOptions = $env:JAVA_TOOL_OPTIONS
$buildExitCode = 1

try {
    New-Item -ItemType Directory -Force -Path $socketDirectory | Out-Null
    # Windows packaged-app temp redirection can break Java's Unix-domain
    # loopback sockets. Use a real project-local directory for all build JVMs.
    $env:JAVA_TOOL_OPTIONS = (
        $previousJavaToolOptions + ' "-Djdk.net.unixdomain.tmpdir=' + $socketDirectory + '"'
    ).Trim()

    Push-Location $projectRoot
    try {
        & .\gradlew.bat :app:assembleDebug '-Precapflow.ffmpeg.enabled=true' --no-daemon --max-workers=2
        $buildExitCode = $LASTEXITCODE
    }
    finally {
        Pop-Location
    }
}
finally {
    $env:JAVA_TOOL_OPTIONS = $previousJavaToolOptions
}

exit $buildExitCode
