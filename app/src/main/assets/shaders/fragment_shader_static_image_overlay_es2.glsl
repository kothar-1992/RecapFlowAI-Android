precision mediump float;

uniform sampler2D uTexSampler;
uniform sampler2D uOverlaySampler;
uniform float uOverlayLeft;
uniform float uOverlayTop;
uniform float uOverlayRight;
uniform float uOverlayBottom;
uniform float uOverlayCenterX;
uniform float uOverlayCenterY;
uniform float uOverlayHalfWidth;
uniform float uOverlayHalfHeight;
uniform float uAnimationScale;
uniform float uAnimationRotationRadians;
uniform float uAnimationEnabled;
uniform float uOverlayOpacity;
uniform float uOverlayEnabled;

varying vec2 vTexSamplingCoord;

void main() {
    vec4 original = texture2D(uTexSampler, vTexSamplingCoord);
    if (uOverlayEnabled < 0.5) {
        gl_FragColor = original;
        return;
    }

    float topCoordinate = 1.0 - vTexSamplingCoord.y;
    vec2 overlayCoordinate;

    if (uAnimationEnabled < 0.5) {
        // Keep the previously verified NONE/static mapping byte-for-byte in behavior: the same
        // axis-aligned bounds and texture-coordinate equation are used when animation is disabled.
        if (
            vTexSamplingCoord.x < uOverlayLeft ||
            vTexSamplingCoord.x > uOverlayRight ||
            topCoordinate < uOverlayTop ||
            topCoordinate > uOverlayBottom
        ) {
            gl_FragColor = original;
            return;
        }

        overlayCoordinate = vec2(
            (vTexSamplingCoord.x - uOverlayLeft) / (uOverlayRight - uOverlayLeft),
            (topCoordinate - uOverlayTop) / (uOverlayBottom - uOverlayTop)
        );
    } else {
        // Inverse-map the output pixel through the animated center/scale/rotation into the source
        // logo texture. Geometry is pre-clamped on the Kotlin side so the full transformed logo
        // remains inside the normalized output frame after aspect conversion.
        vec2 delta = vec2(
            vTexSamplingCoord.x - uOverlayCenterX,
            topCoordinate - uOverlayCenterY
        );
        float c = cos(uAnimationRotationRadians);
        float s = sin(uAnimationRotationRadians);
        vec2 local = vec2(
            c * delta.x + s * delta.y,
            -s * delta.x + c * delta.y
        );
        float halfWidth = max(uOverlayHalfWidth * uAnimationScale, 0.000001);
        float halfHeight = max(uOverlayHalfHeight * uAnimationScale, 0.000001);
        overlayCoordinate = vec2(
            local.x / (2.0 * halfWidth) + 0.5,
            local.y / (2.0 * halfHeight) + 0.5
        );
        if (
            overlayCoordinate.x < 0.0 ||
            overlayCoordinate.x > 1.0 ||
            overlayCoordinate.y < 0.0 ||
            overlayCoordinate.y > 1.0
        ) {
            gl_FragColor = original;
            return;
        }
    }

    vec4 overlay = texture2D(uOverlaySampler, overlayCoordinate);
    float alpha = clamp(overlay.a * uOverlayOpacity, 0.0, 1.0);
    gl_FragColor = vec4(mix(original.rgb, overlay.rgb, alpha), original.a);
}
