#version 150

uniform sampler2D DiffuseSampler;
uniform float Radius;
uniform float DarknessScale;

in vec2 texCoord;

out vec4 fragColor;

float random(vec2 st) {
    return fract(sin(dot(st.xy, vec2(127.1, 311.7))) * 43758.5453123);
}

void main() {
    vec4 color = texture(DiffuseSampler, texCoord);

    // Vignette vom Bildrand her (0=Mitte, 1=Rand)
    vec2 uv = texCoord * 2.0 - 1.0;
    float dist = length(uv);

    float darkness = clamp(Radius * DarknessScale, 0.0, 1.0);

    // Lila Infinite Void Farbe
    vec3 voidColor = vec3(0.04, 0.0, 0.12);

    // Vignette staerker zum Rand
    float vignette = smoothstep(0.2, 1.1, dist) * darkness;

    // Sterne am Rand
    float starNoise = random(floor(texCoord * 200.0));
    float star = step(0.985, starNoise) * smoothstep(0.5, 1.0, dist) * darkness;
    vec3 starColor = vec3(0.5, 0.0, 1.0) * star * 2.0;

    // Ergebnis
    vec3 result = mix(color.rgb, voidColor, vignette * 0.9) + starColor;

    fragColor = vec4(result, color.a);
}
