attribute vec4 aFramePosition;

varying vec2 vTexSamplingCoord;

void main() {
    gl_Position = aFramePosition;
    vTexSamplingCoord = (aFramePosition.xy + vec2(1.0)) * 0.5;
}
