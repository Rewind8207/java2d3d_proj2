#version 330

in vec3 position;
in vec3 normal;
out vec4 fragmentColor;
const vec3 grey = vec3(0.5, 0.5, 0.5);

void main() {
    vec3 lightDir = normalize(vec3(.0, -2.0, 1.0));

    vec3 n = normalize(normal);
    // vec3 l = normalize(lightPosition - position);
    vec3 l = lightDir;
    vec3 e = normalize(position);
    vec3 r = reflect(l, n);
    float ambient = 0.2;
    float diffuse = 0.8 * clamp(0, dot(n, l), 1);
    float specular = 0.8 * pow(clamp(0, dot(e, r), 1), 15.0);
    fragmentColor = vec4(grey * (ambient + diffuse + specular), 1.0);
}