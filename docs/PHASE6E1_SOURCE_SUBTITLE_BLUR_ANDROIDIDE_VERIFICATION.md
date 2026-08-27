# Phase 6E.1 Source Subtitle Blur — AndroidIDE / Device Verification

## Build

From the project root:

```bash
bash scripts/verify_phase6e1_source.sh
./gradlew :app:compileDebugKotlin --stacktrace
./gradlew :app:testDebugUnitTest --stacktrace
./gradlew :app:assembleDebug -Precapflow.ffmpeg.enabled=true --stacktrace
```

Confirm current hotfix identity `RecapFlowAI_Phase6E1_5` and version
`1.0-phase6e1.5` before
installing the debug APK.

The fixed source must contain both files below and must not pass `R.raw` IDs to
`GlProgram`:

```text
app/src/main/assets/shaders/vertex_shader_source_subtitle_blur_es2.glsl
app/src/main/assets/shaders/fragment_shader_source_subtitle_blur_es2.glsl
```

## Off states and editor interaction

1. Open Editor and confirm tab order Clips → Transform → Audio → Overlay.
2. With Overlay Off, render a short baseline and confirm no blur is present.
3. Turn Overlay On but leave Source subtitle blur Off; preview/export must remain unchanged.
4. Turn Source subtitle blur On. Confirm the lower default guide appears only in Overlay tab;
   direct guide touch is temporarily unavailable and geometry is controlled by the sliders.
5. Drag the region to each corner and resize it from small to large; it must stay in bounds.
6. Use Horizontal, Vertical, Width, Height, and Blur strength sliders and compare the guide.
7. Collapse/expand controls. Values and active preview effect must remain unchanged.
8. Reset. Confirm lower caption-safe geometry, default strength 14, and current Trim time.

## Realtime localized effect

Use a source with visible baked-in subtitles and detailed pixels just outside the subtitle area.

- Play, pause, seek, and adjust every control; the paused frame must refresh without rendering.
- Confirm subtitle pixels inside the rectangle blur and pixels immediately outside stay sharp.
- Test strengths 4, 14, and 32 on portrait and landscape sources.
- Set a short time range and seek just before start, inside the range, and at/after end.
- Confirm the guide is an editor affordance only and is never burned into output.
- If the device cannot initialize the shader, confirm ordinary source preview is restored with
  a visible fallback message while the typed render plan remains available for device testing.

## Combined edit matrix

- Custom Crop + 9:16 Fit/Fill: the guide must correspond to the displayed output frame.
- Mirror, Color, Zoom, Speed, and Visual Fade: blur stays on the chosen output area.
- Intro Freeze: if Trim-start time is inside the blur range, the still frame is blurred; otherwise
  it stays sharp.
- Adaptive candidate and full sequence preview: blur activates only at matching source times.
- Adaptive Apply export: every retained range uses correct source-time mapping.
- Audio Keep/Mute/Replace/Mix must not alter blur behavior or sync.

## Export and safety

- Render 720p, play it, then render 1080p. Compare rectangle, feather, strength, and timing.
- Probe H.264/AAC policy, dimensions, duration, and audio sync as in the Phase 6 baseline.
- During render confirm all effect-changing Overlay controls and the guide are locked/hidden.
- Cancel at an intermediate percentage; incomplete output is removed and source is preserved.
- Rotate/recreate and background/foreground. Confirm tab, switches, collapse, geometry, strength,
  and time range restore.
- Choose another source. Confirm geometry/strength/time reset safely for the new video.
- Repeat render twice and check for shader errors, stale output, crashes, or memory growth.

Record device/API/GPU, source dimensions/rotation/duration, selected rectangle/strength/time,
enabled combined effects, output preset/codecs/duration, observed parity, and any fallback.
