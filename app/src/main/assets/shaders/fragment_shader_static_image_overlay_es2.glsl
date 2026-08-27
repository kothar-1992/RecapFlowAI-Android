precision mediump float;

uniform sampler2D uTexSampler;
uniform sampler2D uOverlaySampler;
uniform float uOverlayLeft;
uniform float uOverlayTop;
uniform float uOverlayRight;
uniform float uOverlayBottom;
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
    if (
        vTexSamplingCoord.x < uOverlayLeft ||
        vTexSamplingCoord.x > uOverlayRight ||
        topCoordinate < uOverlayTop ||
        topCoordinate > uOverlayBottom
    ) {
        gl_FragColor = original;
        return;
    }

    vec2 overlayCoordinate = vec2(
        (vTexSamplingCoord.x - uOverlayLeft) / (uOverlayRight - uOverlayLeft),
        (topCoordinate - uOverlayTop) / (uOverlayBottom - uOverlayTop)
    );
    vec4 overlay = texture2D(uOverlaySampler, overlayCoordinate);
    float alpha = clamp(overlay.a * uOverlayOpacity, 0.0, 1.0);
    gl_FragColor = vec4(mix(original.rgb, overlay.rgb, alpha), original.a);
}
