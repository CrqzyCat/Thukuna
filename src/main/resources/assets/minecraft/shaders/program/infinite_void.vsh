#version 150

in vec4 Position;
in vec2 UV;

uniform mat4 ProjMat;
uniform vec2 InSize;

out vec2 texCoord;
out vec2 oneTexel;

void main() {
    gl_Position = ProjMat * Position;
    texCoord = UV;
    oneTexel = 1.0 / InSize;
}
