#version 150

uniform sampler2D DiffuseSampler;
uniform float Radius;
uniform float DarknessScale;

in vec2 texCoord;
in vec2 oneTexel;

out vec4 fragColor;

void main() {
    vec4 color = texture(DiffuseSampler, texCoord);
    
    // Vignette berechnen (0 = Mitte, 1 = Rand)
    vec2 uv = texCoord * 2.0 - 1.0;
    float dist = length(uv);
    
    // Darkness-Staerke (kommt von Minecraft, 0.0 - 1.0)
    float darkness = clamp(Radius * DarknessScale, 0.0, 1.0);
    
    // Lila Infinite Void Farbe
    vec3 voidColor = vec3(0.05, 0.0, 0.15);
    
    // Vignette: staerker zum Rand hin
    float vignette = smoothstep(0.3, 1.2, dist) * darkness;
    
    // Sterne: zufaellige helle lila Punkte am Rand
    float starSeed = fract(sin(dot(texCoord * 100.0, vec2(127.1, 311.7))) * 43758.5453);
    float star = step(0.98, starSeed) * vignette * 1.5;
    vec3 starColor = vec3(0.6, 0.0, 1.0) * star;
    
    // Mischen: Originalfarbe + lila Vignette + Sterne
    vec3 result = mix(color.rgb, voidColor, vignette * 0.85) + starColor;
    
    fragColor = vec4(result, color.a);
}
