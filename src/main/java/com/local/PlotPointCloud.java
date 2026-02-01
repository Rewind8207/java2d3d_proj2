package com.local;

import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import com.local.PointBuffer;
import com.local.VertexReader3D;

public class PlotPointCloud {

    // Draw a triangle with vertices in NDC
    public void show(String resourceName) {

        // Create GL context and GLFW window
        GLFWErrorCallback errorCallback = GLFWErrorCallback.createPrint(System.err);
        GLFW.glfwSetErrorCallback(errorCallback);
        GLFW.glfwInit();
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);
        long window = GLFW.glfwCreateWindow(800 /* width */, 800 /* height */, "HelloGL", 0, 0);
        GLFW.glfwMakeContextCurrent(window);
        GLFW.glfwSwapInterval(1);
        GLFW.glfwShowWindow(window);
      
      
        // Set up OpenGL
        GL.createCapabilities();
        GL11.glClearColor(1.0f, 1.0f, 1.0f, 0.0f);
        GL11.glClearDepth(1.0f);
      
        // Vertex shader
        var vertex_shader = loadResourceShader("shaders/pointCloud_vertShader.glsl");

        // Fragment shader
        var fragment_shader = loadResourceShader("shaders/pointCloud_fragShader.glsl");


        // Compile and link shaders
        int vs = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        GL20.glShaderSource(vs, vertex_shader);
        GL20.glCompileShader(vs);

        int fs = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
        GL20.glShaderSource(fs, fragment_shader);
        GL20.glCompileShader(fs);

        int program = GL20.glCreateProgram();
        GL20.glAttachShader(program, vs);
        GL20.glAttachShader(program, fs);
        GL20.glLinkProgram(program);
        GL20.glUseProgram(program);
      
        var cloudData = VertexReader3D.readPointCloudData(resourceName);
        var vertexBuffer = cloudData.get(0);
        int pointCount = vertexBuffer.size();
        float[] vert_coords = new float[pointCount*3];
        for (int i = 0; i < pointCount*3; i++) {
            vert_coords[i] = (float) vertexBuffer.getRawArray()[i];
        }
        FloatBuffer fbo = BufferUtils.createFloatBuffer(vert_coords.length);
        fbo.put(vert_coords);
        fbo.flip();
      
        int vbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, fbo, GL15.GL_STATIC_DRAW);
      
        int vao = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vao);
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 0, 0);
        
        int locAngle = GL20.glGetUniformLocation(program, "angle");
      
        // Loop and render
        while (!GLFW.glfwWindowShouldClose(window)) {
            GLFW.glfwPollEvents();
            
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
            GL30.glBindVertexArray(vao);
            
            double time = GLFW.glfwGetTime(); 
            // compute current angle
            float currentAngle = (float) (-time * 0.5); 
            // pass angle to shader
            GL20.glUniform1f(locAngle, currentAngle);

            GL11.glPointSize(3.0f); 
            GL11.glDrawArrays(GL11.GL_POINTS, 0, pointCount);
            
            GLFW.glfwSwapBuffers(window);
        }
      
      
        // free resources
        GL15.glDeleteBuffers(vbo);
        GL30.glDeleteVertexArrays(vao);
        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();
        GLFW.glfwSetErrorCallback(null).free();
    }

    private String loadResourceShader(String shaderPath) {
        String shader = "";
        try{
          InputStream is = PlotPointCloud.class.getClassLoader().getResourceAsStream(shaderPath);
          shader = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
      
        return shader;
    }

    public static void main(String[] args) {
        new PlotPointCloud().show("bunny.xyz");
    }
}