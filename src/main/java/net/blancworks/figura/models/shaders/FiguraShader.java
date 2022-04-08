package net.blancworks.figura.models.shaders;

import com.google.common.collect.ImmutableList;
import net.blancworks.figura.lua.api.math.LuaVector;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.Program;
import net.minecraft.client.render.Shader;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceFactory;
import net.minecraft.util.Identifier;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;

public class FiguraShader extends Shader {

    public FiguraShader(ResourceFactory factory, String name, VertexFormat format) throws IOException {
        super(factory, name, format);
        Program.Type.VERTEX.getProgramCache().remove(name);
        Program.Type.FRAGMENT.getProgramCache().remove(name);
    }

    public boolean hasUniform(String name) {
        return getUniform(name) != null;
    }

    /**
     * Attempts to set the value of the specified uniform to the specified value.
     * @param name The name of the targeted uniform
     * @param value The value to be passed to the uniform
     * @throws LuaError When the arguments are not valid
     */
    public void setUniformFromLua(LuaValue name, LuaValue value) throws LuaError {
        try {
            GlUniform uniform = getUniform(name.checkjstring());
            if (uniform == null) {
                throw new LuaError("No uniform with name " + name.checkjstring() + " exists!");
            } else {
                try {
                    value.checknotnil();
                    switch (uniform.getDataType()) {
                        case 0, 1, 2, 3 -> { //int
                            if (value.isnumber()) {
                                uniform.set(value.checkint());
                            } else {
                                LuaVector v = LuaVector.checkOrNew(value);
                                uniform.setForDataType((int) v.x(), (int) v.y(), (int) v.z(), (int) v.w());
                            }
                        }
                        case 4, 5, 6, 7 -> { //float
                            if (value.isnumber()) {
                                uniform.set(value.tofloat());
                            } else {
                                LuaVector v = LuaVector.checkOrNew(value);
                                uniform.setForDataType(v.x(), v.y(), v.z(), v.w());
                            }

                        }
                        case 8 -> { //2x2 matrix
                            value.checktable();
                            uniform.set(value.get(1).tofloat(), value.get(2).tofloat(),
                                    value.get(3).tofloat(), value.get(4).tofloat());
                        }
                        case 9 -> { //3x3 matrix
                            value.checktable();
                            uniform.set(value.get(1).tofloat(), value.get(2).tofloat(), value.get(3).tofloat(),
                                    value.get(4).tofloat(), value.get(5).tofloat(), value.get(6).tofloat(),
                                    value.get(7).tofloat(), value.get(8).tofloat(), value.get(9).tofloat());
                        }
                        case 10 -> { //4x4 matrix
                            value.checktable();
                            uniform.set(value.get(1).tofloat(), value.get(2).tofloat(), value.get(3).tofloat(), value.get(4).tofloat(),
                                    value.get(5).tofloat(), value.get(6).tofloat(), value.get(7).tofloat(), value.get(8).tofloat(),
                                    value.get(9).tofloat(), value.get(10).tofloat(), value.get(11).tofloat(), value.get(12).tofloat(),
                                    value.get(13).tofloat(), value.get(14).tofloat(), value.get(15).tofloat(), value.get(16).tofloat());
                        }
                    }
                } catch (LuaError err) {
                    throw new LuaError("Invalid arguments for setUniform(). Value should either be a number or a table of numbers, depending on the uniform type.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static int shadersMade = 0;
    private static final List<String> defaultUniformNames = Arrays.asList("ModelViewMat", "ProjMat", "TextureMat", "ScreenSize", "ColorModulator", "Light0_Direction", "Light1_Direction", "FogStart", "FogEnd", "FogColor", "LineWidth", "GameTime");
    private static final List<String> defaultUniformTypes = Arrays.asList("mat4", "mat4", "mat4", "vec2", "vec4", "vec3", "vec3", "float", "float", "vec4", "float", "float");
    private static final Map<String, String> typeMap = new HashMap<>() {{
        put("mat4", "matrix4x4");
        put("mat3", "matrix3x3");
        put("mat2", "matrix2x2");
        put("float", "float");
        put("vec2", "float");
        put("vec3", "float");
        put("vec4", "float");
        put("int", "int");
        put("ivec2", "int");
        put("ivec3", "int");
        put("ivec4", "int");
    }};
    private static final Map<String, Integer> countMap = new HashMap<>() {{
        put("mat4", 16);
        put("mat3", 9);
        put("mat2", 4);
        put("float", 1);
        put("vec2", 2);
        put("vec3", 3);
        put("vec4", 4);
        put("int", 1);
        put("ivec2", 2);
        put("ivec3", 3);
        put("ivec4", 4);
    }};

    public static FiguraShader create(VertexFormat vertexFormat, String vertexSource, String fragmentSource, int numSamplers, List<String> uniformNames, List<String> uniformTypes) throws IOException {
        String name = "shader"+shadersMade++;

        //Generate json string from provided info
        StringBuilder json = new StringBuilder("{\"vertex\":\"" + name + "\",\"fragment\":\"" + name + "\",\"attributes\": [");
        //Attributes
        ImmutableList<String> attributes = vertexFormat.getShaderAttributes();
        for (String attribute : attributes) {
            if (!(attribute.equals("Padding")))
                json.append("\"").append(attribute).append("\",");
        }
        json = new StringBuilder(json.substring(0, json.length() - 1));
        //Samplers
        json.append("],\"samplers\":[");
        for (int i = 0; i < numSamplers; i++)
            json.append("{\"name\":\"Sampler").append(i).append("\"},");
        json = new StringBuilder(json.substring(0, json.length() - 1));
        //Uniforms
        json.append("],\"uniforms\":[");
        for (int i = 0; i < defaultUniformNames.size(); i++) {
            String uniformName = defaultUniformNames.get(i);
            String uniformType = defaultUniformTypes.get(i);
            String type = typeMap.get(uniformType);
            int count = countMap.get(uniformType);
            json.append("{\"name\":\"").append(uniformName).append("\",\"type\":\"").append(type).append("\",\"count\":").append(count).append(",\"values\":[");
            for (int j = 0; j < count; j++)
                json.append(uniformType.startsWith("mat") && j % (Math.sqrt(count) + 1) == 0 ? "1.0" : "0.0").append(",");
            json = new StringBuilder(json.substring(0, json.length() - 1) + "]},");
        }
        for (int i = 0; i < uniformNames.size(); i++) {
            String uniformName = uniformNames.get(i);
            String uniformType = uniformTypes.get(i);
            if (uniformName.contains(" "))
                throw new LuaError("Uniform names cannot contain spaces.");
            if (!typeMap.containsKey(uniformType))
                throw new LuaError("Invalid uniform type: " + uniformType);
            String type = typeMap.get(uniformType);
            int count = countMap.get(uniformType);
            json.append("{\"name\":\"").append(uniformName).append("\",\"type\":\"").append(type).append("\",\"count\":").append(count).append(",\"values\":[");
            for (int j = 0; j < count; j++)
                json.append(uniformType.startsWith("mat") && j % (Math.sqrt(count) + 1) == 0 ? "1.0" : "0.0").append(",");
            json = new StringBuilder(json.substring(0, json.length() - 1) + "]},");
        }
        json = new StringBuilder(json.substring(0, json.length() - 1) + "]}");

        //System.out.println(json);

        //Pass all 3 strings to create a special factory that just returns them back as InputStreams
        ResourceFactory factory = new FiguraShaderFactory(json.toString(), vertexSource, fragmentSource);
        return new FiguraShader(factory, name, vertexFormat);
    }

    private static class FiguraShaderFactory implements ResourceFactory {

        private final String[] sources;
        private int i = 0;

        public FiguraShaderFactory(String json, String vsh, String fsh) {
            sources = new String[] {json, vsh, fsh};
        }

        @Override
        public Optional<Resource> getResource(Identifier id) {
            return Optional.of(new Resource("Custom Figura Render Layers", () -> new ByteArrayInputStream(sources[i++].getBytes())));
        }
    }

}
