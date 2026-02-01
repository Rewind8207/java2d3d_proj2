#version 330 core
layout (location = 0) in vec3 aPos;

void main() {
    float scale = 0.7;
    vec3 pos = aPos * scale;
    pos.y -= 0.1;
    
    float angle = 0.0 * 3.14159;
    float s = sin(angle);
    float c = cos(angle);
    
    float newX = pos.x * c - pos.z * s;
    float newZ = pos.x * s + pos.z * c;
    float newY = pos.y;

    gl_Position = vec4(newX, newY, newZ, 1.0);
}