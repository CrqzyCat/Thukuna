#version 150

uniform sampler2D InSampler;
uniform float Radius;
uniform float DarknessScale;

in vec2 texCoord;

out vec4 fragColor;

float random(vec2 st) {
    return fract(sin(dot(st.xy, vec2(127.1, 311.7))) * 43758.5453123);
}

void main() {
    vec4 color = texture(InSampler, texCoord);

    vec2 uv = texCoord * 2.0 - 1.0;
    float dist = length(uv);

    // Darkness Staerke - auch ohne Radius/DarknessScale sichtbar machen
    float darkness = max(Radius * DarknessScale, 0.5);

    // Lila Infinite Void Farbe
    vec3 voidColor = vec3(0.04, 0.0, 0.18);

    // Starke Vignette
    float vignette = smoothstep(0.1, 0.9, dist) * darkness;

    // Lila Nebel ueber alles
    float fog = darkness * 0.3;

    // Sterne
    float starNoise = random(floor(texCoord * 300.0));
    float star = step(0.982, starNoise) * smoothstep(0.3, 1.0, dist) * darkness;
    vec3 starColor = vec3(0.6, 0.1, 1.0) * star * 3.0;

    // Ergebnis: lila Schleier + Vignette + Sterne
    vec3 result = mix(color.rgb, voidColor, vignette * 0.95 + fog) + starColor;

    fragColor = vec4(result, color.a);
}
