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
import com.local.RBFReconstructor;
import com.local.MarchingCubesGenerator;


public class PlotRBFSurface {

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
        var vertex_shader = loadResourceShader("shaders/Phong_vertex_shader.glsl");
        // Fragment shader
        var fragment_shader = loadResourceShader("shaders/Phong_fragment_shader.glsl");

        // Compile and link shaders
        int vs = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        GL20.glShaderSource(vs, vertex_shader);
        GL20.glCompileShader(vs);
        // Check for compile errors
        if (GL20.glGetShaderi(vs, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            System.err.println("Vertex Shader Error: " + GL20.glGetShaderInfoLog(vs));
        }

        int fs = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
        GL20.glShaderSource(fs, fragment_shader);
        GL20.glCompileShader(fs);
        // Check for compile errors
        if (GL20.glGetShaderi(fs, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            System.err.println("Fragment Shader Error: " + GL20.glGetShaderInfoLog(fs));
        }

        int program = GL20.glCreateProgram();
        GL20.glAttachShader(program, vs);
        GL20.glAttachShader(program, fs);
        GL20.glLinkProgram(program);
        GL20.glUseProgram(program);
      
        // var cloudData = VertexReader3D.readPointCloudData(resourceName);
        
        // RFV surface reconstruction
        RBFReconstructor rbf = new RBFReconstructor();
        rbf.setDownSamplingStep(3);
        rbf.computeWeights(resourceName);

        // Marching Cubes mesh generation
        MarchingCubesGenerator mc = new MarchingCubesGenerator(rbf);
        mc.generateMesh(60, 60, 60);
        var meshVerts = mc.getVertices();
        var meshNormals = mc.getNormals();
        int vertexCount = meshVerts.size();
        if (vertexCount == 0) {
            System.err.println("No surface generated!");
            return;
        }

        FloatBuffer vertBuffer = BufferUtils.createFloatBuffer(vertexCount * 3);
        FloatBuffer normalsBuffer = BufferUtils.createFloatBuffer(vertexCount * 3);
        for(int i=0; i<vertexCount; i++) {
             vertBuffer.put((float)meshVerts.get(i, 0));
             vertBuffer.put((float)meshVerts.get(i, 1));
             vertBuffer.put((float)meshVerts.get(i, 2));
             normalsBuffer.put((float)meshNormals.get(i, 0));
             normalsBuffer.put((float)meshNormals.get(i, 1));
             normalsBuffer.put((float)meshNormals.get(i, 2));
        }
        vertBuffer.flip();
        normalsBuffer.flip();

        int vao = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vao);
      
        int vboPos = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboPos);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertBuffer, GL15.GL_STATIC_DRAW);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 0, 0);
        GL20.glEnableVertexAttribArray(0);
      
        int vboNorm = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboNorm);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, normalsBuffer, GL15.GL_STATIC_DRAW);
        GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, 0, 0);
        GL20.glEnableVertexAttribArray(1);
        
        // Set background color
        GL11.glClearColor(.9f, .9f, .9f, 1.0f);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
        // GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);

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

            // Draw the reconstructed triangle mesh
            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, vertexCount);
            
            GLFW.glfwSwapBuffers(window);
        }
      
      
        // free resources
        GL15.glDeleteBuffers(vboPos);
        // GL15.glDeleteBuffers(vboNorm);
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
        new PlotRBFSurface().show("bunny.xyz");
    }
}