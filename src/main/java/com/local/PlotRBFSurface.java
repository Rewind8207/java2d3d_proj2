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
    private void show(String resourceName) {

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
        var vertex_shader = loadResourceShader("shaders/simpleRBF_vertShader.glsl");
        // Fragment shader
        var fragment_shader = loadResourceShader("shaders/simpleRBF_fragShader.glsl");

        // String vertex_shader = "#version 330 core\n" +
        //         "layout (location = 0) in vec3 aPos;\n" +
        //         "void main() {\n" +
        //         "    gl_Position = vec4(aPos, 1.0);\n" + 
        //         "}";
                
        // String fragment_shader = "#version 330 core\n" +
        //         "out vec4 FragColor;\n" +
        //         "void main() {\n" +
        //         "    FragColor = vec4(1.0, 0.5, 0.2, 1.0);\n" +
        //         "}";




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
        
        // RFV surface reconstruction
        RBFReconstructor rbf = new RBFReconstructor();
        rbf.setDownSamplingStep(3);
        rbf.computeWeights(cloudData);

        // Marching Cubes mesh generation
        MarchingCubesGenerator mc = new MarchingCubesGenerator(rbf);
        mc.generateMesh(60, 60, 60);
        var meshVerts = mc.getVertices();
        int vertexCount = meshVerts.size();
        if (vertexCount == 0) {
            System.err.println("No surface generated!");
            return;
        }

        FloatBuffer fbo = BufferUtils.createFloatBuffer(vertexCount * 3);
        for(int i=0; i<vertexCount; i++) {
             fbo.put((float)meshVerts.get(i, 0));
             fbo.put((float)meshVerts.get(i, 1));
             fbo.put((float)meshVerts.get(i, 2));
        }
        fbo.flip();
      
        int vbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, fbo, GL15.GL_STATIC_DRAW);
      
        int vao = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vao);
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 0, 0);
        
        // Set background color
        GL11.glClearColor(0.1f, 0.1f, 0.15f, 1.0f);
        
        // Turn on wireframe mode
        GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);
      
        // Loop and render
        while (!GLFW.glfwWindowShouldClose(window)) {
            GLFW.glfwPollEvents();
            
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
            GL30.glBindVertexArray(vao);

            // Draw the reconstructed triangle mesh
            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, vertexCount);
            
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

    /**
     * Create shader program from vertex shader and fragment shader code
     * @param vsCode vertex shader code
     * @param fsCode fragment shader code
     * @return
     */
    private int createShader(String vsCode, String fsCode) {
        int vs = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        GL20.glShaderSource(vs, vsCode);
        GL20.glCompileShader(vs);
        int fs = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
        GL20.glShaderSource(fs, fsCode);
        GL20.glCompileShader(fs);
        int prog = GL20.glCreateProgram();
        GL20.glAttachShader(prog, vs);
        GL20.glAttachShader(prog, fs);
        GL20.glLinkProgram(prog);
        return prog;
    }

    public static void main(String[] args) {
        new PlotRBFSurface().show("bunny.xyz");
    }
}