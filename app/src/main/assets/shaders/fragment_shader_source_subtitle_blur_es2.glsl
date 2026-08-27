precision mediump float;

uniform sampler2D uTexSampler;
uniform float uBlurLeft;
uniform float uBlurTop;
uniform float uBlurRight;
uniform float uBlurBottom;
uniform float uHorizontalStep;
uniform float uVerticalStep;
uniform float uHorizontalFeather;
uniform float uVerticalFeather;
uniform float uBlurEnabled;

varying vec2 vTexSamplingCoord;

void main() {
    vec4 original = texture2D(uTexSampler, vTexSamplingCoord);
    if (uBlurEnabled < 0.5) {
        gl_FragColor = original;
        return;
    }

    float topCoordinate = 1.0 - vTexSamplingCoord.y;
    float horizontalEdge = min(
        vTexSamplingCoord.x - uBlurLeft,
        uBlurRight - vTexSamplingCoord.x
    );
    float verticalEdge = min(
        topCoordinate - uBlurTop,
        uBlurBottom - topCoordinate
    );
    float insideDistance = min(horizontalEdge, verticalEdge);
    if (insideDistance <= 0.0) {
        gl_FragColor = original;
        return;
    }

    // DENSE_9X9_NORMALIZED_KERNEL: the old sparse taps placed readable copies at full-radius
    // offsets. Dense samples cover the complete area and normalize once, producing a real blur.
    // REGION_CLAMP_PREVENTS_TILE_GHOSTS: never let texture wrap/mirror repeat subtitle pixels.
    vec2 regionMinimum = vec2(uBlurLeft, 1.0 - uBlurBottom);
    vec2 regionMaximum = vec2(uBlurRight, 1.0 - uBlurTop);
    vec4 blurred = vec4(0.0);
    for (int sampleY = -4; sampleY <= 4; sampleY++) {
        for (int sampleX = -4; sampleX <= 4; sampleX++) {
            vec2 sampleOffset = vec2(
                float(sampleX) * uHorizontalStep,
                float(sampleY) * uVerticalStep
            );
            vec2 sampleCoordinate = clamp(
                vTexSamplingCoord + sampleOffset,
                regionMinimum,
                regionMaximum
            );
            blurred += texture2D(uTexSampler, sampleCoordinate);
        }
    }
    blurred *= 1.0 / 81.0;

    float horizontalMix = smoothstep(0.0, uHorizontalFeather, horizontalEdge);
    float verticalMix = smoothstep(0.0, uVerticalFeather, verticalEdge);
    float blurMix = min(horizontalMix, verticalMix);
    gl_FragColor = mix(original, blurred, blurMix);
}
