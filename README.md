# RecapFlowAI-Android
External FFmpegAndroid implementation.

## Build on Windows (VS Code, PowerShell, or Command Prompt)

From the repository root, run the Windows build helper:

```powershell
.\scripts\build_debug.bat
```

It enables FFmpeg and uses a project-local Java socket directory to avoid
`Unable to establish loopback connection` when Windows redirects the default
temporary directory. The environment change applies only during the build.

The `.bat` helper runs directly without depending on PowerShell script execution
policy. In VS Code, open this repository folder and run it in the integrated
PowerShell or Command Prompt terminal. The filename is `build_debug.bat`, with
no backslash before the underscore. The existing `build_debug.ps1` helper is
also available when PowerShell scripts are permitted.

For the active feature branch's unit-test and build gate with Crossfade enabled:

```powershell
.\scripts\build_debug.bat :app:testDebugUnitTest "-Precapflow.crossfade.runtime.enabled=true"
```

The helper forwards additional Gradle tasks/options and returns Gradle's exit
code. FFmpeg stays enabled; Crossfade is enabled only when explicitly requested.

To invoke Gradle directly in an environment without that Java socket issue:

```powershell
.\gradlew.bat :app:assembleDebug "-Precapflow.ffmpeg.enabled=true" --no-daemon --max-workers=2
```

Quote the entire `-P` argument so PowerShell passes the dotted property name to
Gradle as one argument. Otherwise, Gradle can report
`Task '.ffmpeg.enabled=true' not found`. Use `gradlew.bat` without backslashes
before the dots in the filename or property name.

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.
