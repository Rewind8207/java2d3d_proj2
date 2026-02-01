#version 330 core


layout (location = 0) in vec3 vPosition;
layout (location = 1) in vec3 vNormal;

uniform float angle;

out vec3 position;
out vec3 normal;

void main() {
    float scale = 0.8;
    vec3 pos = vPosition * scale;

    // float angle = 1.0 * 3.14159;
    float s = sin(angle);
    float c = cos(angle);
    
    float newX = pos.x * c - pos.z * s;
    float newZ = pos.x * s + pos.z * c;
    float newY = pos.y;

    float newNormX = vNormal.x * c - vNormal.z * s;
    float newNormZ = vNormal.x * s + vNormal.z * c;
    float newNormY = vNormal.y;
    vec3 newNormal = vec3(newNormX, newNormY, newNormZ);

    gl_Position = vec4(newX, newY, newZ, 1.0);

    position = vec3(newX, newY, newZ);
    normal = normalize(newNormal);

}
