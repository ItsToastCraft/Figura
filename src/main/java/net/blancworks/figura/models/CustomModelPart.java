package net.blancworks.figura.models;

import com.google.common.collect.ImmutableMap;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.avatar.AvatarData;
import net.blancworks.figura.lua.api.model.*;
import net.blancworks.figura.models.shaders.FiguraRenderLayer;
import net.blancworks.figura.models.shaders.FiguraVertexConsumerProvider;
import net.blancworks.figura.trust.TrustContainer;
import net.blancworks.figura.utils.MathUtils;
import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.texture.MissingSprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public class CustomModelPart {
    public String name = "NULL";
    public CustomModel model;

    //Transform data
    public Vec3f pivot = Vec3f.ZERO.copy();
    public Vec3f pos = Vec3f.ZERO.copy();
    public Vec3f rot = Vec3f.ZERO.copy();
    public Vec3f scale = MathUtils.Vec3f_ONE.copy();
    public Vec3f color = MathUtils.Vec3f_ONE.copy();

    //uv stuff
    public Map<UV, uvData> UVCustomizations = new HashMap<>();
    public Vec2f texSize = new Vec2f(64f, 64f);
    public Vec2f uvOffset = new Vec2f(0f, 0f);

    //model properties
    public boolean visible = true;

    public ParentType parentType = ParentType.Model;
    public boolean isMimicMode = false;

    public ShaderType shaderType = ShaderType.None;

    public FiguraRenderLayer customLayer = null;

    public TextureType textureType = TextureType.Custom;
    public Identifier textureVanilla = FiguraTexture.DEFAULT_ID;

    public boolean extraTex = true;
    public boolean cull = false;

    public float alpha = 1f;
    public Vec2f light = null;
    public Vec2f overlay = null;

    //All the vertex data is stored here! :D
    public FloatList vertexData = new FloatArrayList();
    public int vertexCount = 0;

    public Matrix4f lastModelMatrix = new Matrix4f();
    public Matrix3f lastNormalMatrix = new Matrix3f();

    public Matrix4f lastModelMatrixInverse = new Matrix4f();
    public Matrix3f lastNormalMatrixInverse = new Matrix3f();

    //Extra special rendering for this part
    public final HashMap<String, RenderTaskAPI.RenderTaskTable> renderTasks = new LinkedHashMap<>();

    public static boolean canRenderHitBox = false;
    public static boolean canRenderTasks = true;

    private static final Vector4f FULL_VERT = new Vector4f();
    private static final Vec3f NORMAL_VERT = new Vec3f();

    //Renders a model part (and all sub-parts) using the textures provided by a PlayerData instance.
    public int render(AvatarData data, MatrixStack matrices, MatrixStack transformStack, VertexConsumerProvider vcp, int light, int overlay, float alpha) {
        //no model to render
        if (data.model == null || data.vanillaModel == null || vcp == null || !data.isAvatarLoaded() || data.model.leftToRender <= 0)
            return 0;

        //lets render boys!!
        boolean applyHiddenTransforms = data.model.applyHiddenTransforms;
        ParentType renderOnly = data.model.renderOnly;
        int ret = data.model.leftToRender;
        data.model.renderOnly = null;

        //main texture
        Function<Identifier, RenderLayer> layerFunction = RenderLayer::getEntityTranslucent;
        ret = renderTextures(data, ret, matrices, transformStack, vcp, null, light, overlay, 0, 0, MathUtils.Vec3f_ONE, alpha, false, getTexture(data), layerFunction, false, applyHiddenTransforms, renderOnly);

        //extra textures
        for (FiguraTexture figuraTexture : data.extraTextures) {
            Function<Identifier, RenderLayer> renderLayerGetter = FiguraTexture.EXTRA_TEXTURE_TO_RENDER_LAYER.get(figuraTexture.type);

            if (renderLayerGetter != null) {
                renderTextures(data, ret, matrices, transformStack, vcp, null, light, overlay, 0, 0, MathUtils.Vec3f_ONE, alpha, false, figuraTexture.id, renderLayerGetter, true, applyHiddenTransforms, renderOnly);
            }
        }
        draw(vcp);

        //shaders
        ret = renderShaders(data, ret, matrices, vcp, light, overlay, 0, 0, MathUtils.Vec3f_ONE, alpha, false, (byte) 0, applyHiddenTransforms, renderOnly);
        draw(vcp);

        //extra stuff and hitboxes
        ret = renderExtraParts(data, ret, matrices, vcp, light, overlay, false, applyHiddenTransforms, renderOnly);
        draw(vcp);

        return ret;
    }

    //Renders this custom model part and all its children.
    //Returns the cuboids left to render after this one, and only renders until leftToRender is zero.
    public int renderTextures(AvatarData data, int leftToRender, MatrixStack matrices, MatrixStack transformStack, VertexConsumerProvider vcp, RenderLayer layer, int light, int overlay, float u, float v, Vec3f prevColor, float alpha, boolean canRender, Identifier texture, Function<Identifier, RenderLayer> layerFunction, boolean isExtraTex, boolean applyHiddenTransforms, ParentType renderOnly) {
        //do not render invisible parts
        if (!this.visible || (isExtraTex && !this.extraTex))
            return leftToRender;

        matrices.push();
        transformStack.push();

        if (applyHiddenTransforms) {
            applyVanillaTransforms(data, matrices, transformStack);

            applyTransforms(matrices);
            applyTransforms(transformStack);

            updateModelMatrices(transformStack);
        } else if (canRender) {
            applyTransforms(matrices);
            applyTransforms(transformStack);
        }

        if (renderOnly == null || this.parentType == renderOnly)
            canRender = true;

        //uv -> color -> alpha -> cull
        u += this.uvOffset.x;
        v += this.uvOffset.y;

        Vec3f color = this.color.copy();
        color.multiplyComponentwise(prevColor.getX(), prevColor.getY(), prevColor.getZ());

        alpha = this.alpha * alpha;

        if (this.light != null)
            light = LightmapTextureManager.pack((int) this.light.x, (int) this.light.y);
        if (this.overlay != null)
            overlay = OverlayTexture.packUv((int) this.overlay.x, (int) this.overlay.y);

        if (!isExtraTex && this.cull)
            layerFunction = RenderLayer::getEntityTranslucentCull;

        //texture
        if (this.textureType != TextureType.Custom)
            texture = getTexture(data);

        //render!
        if (canRender) {
            //get vertex consumer
            VertexConsumer consumer;

            if (customLayer != null && data.getTrustContainer().getTrust(TrustContainer.Trust.CUSTOM_RENDER_LAYER) == 1) {
                consumer = vcp.getBuffer(customLayer);
                layer = customLayer;
            } else if (layer instanceof FiguraRenderLayer) {
                consumer = vcp.getBuffer(layer);
            } else {
                consumer = vcp.getBuffer(layerFunction.apply(texture));
            }

            //render
            leftToRender = renderCube(leftToRender, matrices, consumer, light, overlay, u, v, color, alpha);
        }

        if (this instanceof CustomModelPartGroup group) {
            for (CustomModelPart child : group.children) {
                if (leftToRender <= 0)
                    break;

                //Don't render special parts.
                if (child.isSpecial())
                    continue;

                //render part
                leftToRender = child.renderTextures(data, leftToRender, matrices, transformStack, vcp, layer, light, overlay, u, v, color, alpha, canRender, texture, layerFunction, isExtraTex, applyHiddenTransforms, renderOnly);
            }
        }

        matrices.pop();
        transformStack.pop();

        return leftToRender;
    }

    public int renderShaders(AvatarData data, int leftToRender, MatrixStack matrices, VertexConsumerProvider vcp, int light, int overlay, float u, float v, Vec3f prevColor, float alpha, boolean canRender, byte shadersToRender, boolean applyHiddenTransforms, ParentType renderOnly) {
        //do not render invisible parts
        if (!this.visible)
            return leftToRender;

        matrices.push();

        if (applyHiddenTransforms) {
            applyVanillaTransforms(data, matrices, new MatrixStack());
            applyTransforms(matrices);
        } else if (canRender) {
            applyTransforms(matrices);
        }

        if (renderOnly == null || this.parentType == renderOnly)
            canRender = true;

        //uv -> color -> alpha -> shaders
        u += this.uvOffset.x;
        v += this.uvOffset.y;

        Vec3f color = this.color.copy();
        color.multiplyComponentwise(prevColor.getX(), prevColor.getY(), prevColor.getZ());

        alpha = this.alpha * alpha;

        if (this.light != null)
            light = LightmapTextureManager.pack((int) this.light.x, (int) this.light.y);
        if (this.overlay != null)
            overlay = OverlayTexture.packUv((int) this.overlay.x, (int) this.overlay.y);

        byte shaders = shadersToRender;
        if (this.shaderType != ShaderType.None)
            shaders = (byte) (shaders | this.shaderType.id);

        //render!
        if (canRender) {
            if (ShaderType.EndPortal.isShader(shaders))
                leftToRender = renderCube(leftToRender, matrices, vcp.getBuffer(RenderLayer.getEndGateway()), light, overlay, u, v, color, alpha);
            if (ShaderType.Glint.isShader(shaders))
                leftToRender = renderCube(leftToRender, matrices, vcp.getBuffer(RenderLayer.getDirectEntityGlint()), light, overlay, u, v, color, alpha);
        }

        if (this instanceof CustomModelPartGroup group) {
            for (CustomModelPart child : group.children) {
                if (leftToRender <= 0)
                    break;

                //Don't render special parts.
                if (child.isSpecial())
                    continue;

                //render part
                leftToRender = child.renderShaders(data, leftToRender, matrices, vcp, light, overlay, u, v, color, alpha, canRender, shaders, applyHiddenTransforms, renderOnly);
            }
        }

        matrices.pop();

        return leftToRender;
    }

    public int renderExtraParts(AvatarData data, int leftToRender, MatrixStack matrices, VertexConsumerProvider vcp, int light, int overlay, boolean canRender, boolean applyHiddenTransforms, ParentType renderOnly) {
        //do not render invisible parts
        if (!this.visible)
            return leftToRender;

        matrices.push();

        if (applyHiddenTransforms) {
            applyVanillaTransforms(data, matrices, new MatrixStack());
            applyTransforms(matrices);
        } else if (canRender) {
            applyTransforms(matrices);
        }

        if (renderOnly == null || this.parentType == renderOnly)
            canRender = true;

        if (this.light != null)
            light = LightmapTextureManager.pack((int) this.light.x, (int) this.light.y);
        if (this.overlay != null)
            overlay = OverlayTexture.packUv((int) this.overlay.x, (int) this.overlay.y);

        //render!
        if (canRender) {
            //render tasks
            if (canRenderTasks) leftToRender = renderExtras(leftToRender, data, matrices, vcp, light, overlay);

            //render hit box
            if (canRenderHitBox) renderHitBox(matrices, vcp.getBuffer(RenderLayer.LINES));
        }

        if (this instanceof CustomModelPartGroup group) {
            for (CustomModelPart child : group.children) {
                if (leftToRender <= 0)
                    break;

                //Don't render special parts.
                if (child.isSpecial())
                    continue;

                //render part
                leftToRender = child.renderExtraParts(data, leftToRender, matrices, vcp, light, overlay, canRender, applyHiddenTransforms, renderOnly);
            }
        }

        matrices.pop();

        return leftToRender;
    }

    public void draw(VertexConsumerProvider vcp) {
        if (vcp instanceof FiguraVertexConsumerProvider customVCP) customVCP.draw();
        else if (vcp instanceof VertexConsumerProvider.Immediate immediate) immediate.draw();
        else if (vcp instanceof OutlineVertexConsumerProvider outline) outline.draw();
    }

    public Identifier getTexture(AvatarData data) {
        Identifier textureId = null;

        if (textureType == TextureType.Resource) {
            textureId = MinecraftClient.getInstance().getResourceManager().getResource(textureVanilla).isPresent() ? textureVanilla : MissingSprite.getMissingSpriteId();
        } else if (textureType == TextureType.Elytra) {
            if (data.playerListEntry != null)
                textureId = data.playerListEntry.getElytraTexture();

            if (textureId == null)
                textureId = FiguraTexture.ELYTRA_ID;
        } else if (data.playerListEntry != null && textureType != TextureType.Custom) {
            textureId = this.textureType == TextureType.Cape ? data.playerListEntry.getCapeTexture() : data.playerListEntry.getSkinTexture();
        } else if (data.texture != null) {
            textureId = data.texture.id;
        } else if (data.lastEntity != null) {
            textureId = MinecraftClient.getInstance().getEntityRenderDispatcher().getRenderer(data.lastEntity).getTexture(data.lastEntity);
        }

        return textureId == null ? FiguraTexture.DEFAULT_ID : textureId;
    }

    public int renderCube(int leftToRender, MatrixStack matrices, VertexConsumer vertices, int light, int overlay, float u, float v, Vec3f color, float alpha) {
        Matrix4f modelMatrix = matrices.peek().getPositionMatrix();
        Matrix3f normalMatrix = matrices.peek().getNormalMatrix();

        for (int i = 1; i <= this.vertexCount; i++) {
            int startIndex = (i - 1) * 8;

            //Get vertex.
            FULL_VERT.set(
                    this.vertexData.getFloat(startIndex++),
                    this.vertexData.getFloat(startIndex++),
                    this.vertexData.getFloat(startIndex++),
                    1
            );

            float vertU = this.vertexData.getFloat(startIndex++);
            float vertV = this.vertexData.getFloat(startIndex++);

            NORMAL_VERT.set(
                    this.vertexData.getFloat(startIndex++),
                    this.vertexData.getFloat(startIndex++),
                    this.vertexData.getFloat(startIndex)
            );

            FULL_VERT.transform(modelMatrix);
            NORMAL_VERT.transform(normalMatrix);

            //Push vertex.
            vertices.vertex(
                    FULL_VERT.getX(), FULL_VERT.getY(), FULL_VERT.getZ(),
                    color.getX(), color.getY(), color.getZ(), alpha,
                    vertU + u, vertV + v,
                    overlay, light,
                    NORMAL_VERT.getX(), NORMAL_VERT.getY(), NORMAL_VERT.getZ()
            );

            //Every 4 verts (1 face)
            if (i % 4 == 0) {
                leftToRender -= 4;

                if (leftToRender <= 0)
                    break;
            }
        }

        return leftToRender;
    }

    public int renderExtras(int leftToRender, AvatarData data, MatrixStack matrices, VertexConsumerProvider vcp, int light, int overlay) {
        //render extra parts
        synchronized (this.renderTasks) {
            for (RenderTaskAPI.RenderTaskTable tbl : this.renderTasks.values()) {
                if (tbl.task.enabled) {
                    leftToRender -= tbl.task.render(data, matrices, vcp, light, overlay);
                    if (leftToRender <= 0) break;
                }
            }
        }

        return leftToRender;
    }

    public void renderHitBox(MatrixStack matrices, VertexConsumer vertices) {
        Vec3f color;
        float boxSize;
        if (this.getPartType() == PartType.CUBE) {
            color = FiguraMod.FRAN_PINK;
            boxSize = 1 / 48f;
        }
        else {
            color = FiguraMod.ACE_BLUE;
            boxSize = 1 / 24f;
        }

        //render the box
        WorldRenderer.drawBox(matrices, vertices, -boxSize, -boxSize, -boxSize, boxSize, boxSize, boxSize, color.getX(), color.getY(), color.getZ(), 1f);
    }

    //clear all extra render tasks
    public void clearExtraRendering() {
        this.renderTasks.clear();
    }

    public int getComplexity() {
        //don't render invisible parts
        return this.visible ? this.vertexCount : 0;
    }

    public void updateModelMatrices(MatrixStack stack) {
        lastModelMatrix = stack.peek().getPositionMatrix().copy();
        lastNormalMatrix = stack.peek().getNormalMatrix().copy();

        lastModelMatrixInverse = lastModelMatrix.copy();
        lastModelMatrixInverse.invert();
        lastNormalMatrixInverse = lastNormalMatrix.copy();
        lastNormalMatrixInverse.invert();
    }

    public void applyVanillaTransforms(AvatarData data, MatrixStack matrices, MatrixStack transformStack) {
        if (parentType == ParentType.Model)
            return;

        try {
            ModelPart part = null;
            if (data.vanillaModel instanceof PlayerEntityModel model)
                part = getModelPart(model, this.parentType);

            if (part != null) {
                //mimic rotations
                if (this.isMimicMode) {
                    this.rot = new Vec3f(part.pitch, part.yaw, part.roll);
                    this.rot.scale(MathHelper.DEGREES_PER_RADIAN);
                }
                //vanilla rotations
                else {
                    part.rotate(matrices);
                    part.rotate(transformStack);
                }
            }
            //camera
            else if (this.parentType == ParentType.Camera) {
                Quaternion rot = MinecraftClient.getInstance().getEntityRenderDispatcher().getRotation().copy();
                Vec3f euler = MathUtils.quaternionToEulerXYZ(rot);
                rotate(matrices, euler);
                rotate(transformStack, euler);
            }
            //custom parts
            else {
                HashMap<Identifier, VanillaModelPartCustomization> oriModifications = data.model.originModifications;
                VanillaModelPartCustomization cust = new VanillaModelPartCustomization() {{
                    matrices.push();
                    if (parentType == ParentType.LeftItemOrigin || parentType == ParentType.RightItemOrigin) {
                        applyTransformsAsItem(matrices);
                        applyTransformsAsItem(transformStack);
                    } else {
                        applyOriginTransforms(matrices);
                        applyOriginTransforms(transformStack);
                    }
                    stackReference = matrices.peek();
                    part = CustomModelPart.this;
                    visible = true;
                    matrices.pop();
                }};

                switch (this.parentType) {
                    case LeftItemOrigin -> oriModifications.put(ItemModelAPI.VANILLA_LEFT_HAND_ID, cust);
                    case RightItemOrigin -> oriModifications.put(ItemModelAPI.VANILLA_RIGHT_HAND_ID, cust);
                    case LeftElytraOrigin -> oriModifications.put(ElytraModelAPI.VANILLA_LEFT_WING_ID, cust);
                    case RightElytraOrigin -> oriModifications.put(ElytraModelAPI.VANILLA_RIGHT_WING_ID, cust);
                    case LeftParrotOrigin -> oriModifications.put(ParrotModelAPI.VANILLA_LEFT_PARROT_ID, cust);
                    case RightParrotOrigin -> oriModifications.put(ParrotModelAPI.VANILLA_RIGHT_PARROT_ID, cust);
                    case LeftSpyglass -> oriModifications.put(SpyglassModelAPI.VANILLA_LEFT_SPYGLASS_ID, cust);
                    case RightSpyglass -> oriModifications.put(SpyglassModelAPI.VANILLA_RIGHT_SPYGLASS_ID, cust);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static ModelPart getModelPart(PlayerEntityModel<?> model, ParentType parent) {
        ModelPart part;
        switch (parent) {
            case Head -> part = model.head;
            case Torso -> part = model.body;
            case LeftArm -> part = model.leftArm;
            case LeftLeg -> part = model.leftLeg;
            case RightArm -> part = model.rightArm;
            case RightLeg -> part = model.rightLeg;
            default -> part = null;
        }
        return part;
    }

    public void applyTransforms(MatrixStack stack) {
        stack.translate(this.pos.getX() / 16f, this.pos.getY() / 16f, this.pos.getZ() / 16f);
        stack.translate(-this.pivot.getX() / 16f, -this.pivot.getY() / 16f, -this.pivot.getZ() / 16f);

        if (this.isMimicMode) vanillaRotate(stack, this.rot);
        else rotate(stack, this.rot);

        stack.scale(this.scale.getX(), this.scale.getY(), this.scale.getZ());
        stack.translate(this.pivot.getX() / 16f, this.pivot.getY() / 16f, this.pivot.getZ() / 16f);
    }

    //TODO move these to the mixins, probably.
    public void applyTransformsAsItem(MatrixStack stack) {
        stack.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(-90f));
        stack.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(180f));
        //stack.translate(0, 0.125D, -0.625D);
        stack.translate(pivot.getX() / 16f, pivot.getZ() / 16f, pivot.getY() / 16f);
        rotate(stack, this.rot);
        stack.translate(this.pos.getX() / 16f, this.pos.getY() / 16f, this.pos.getZ() / 16f);
    }

    public void applyOriginTransforms(MatrixStack stack) {
        stack.translate(-pivot.getX() / 16f, -pivot.getY() / 16f, -pivot.getZ() / 16f);
        rotate(stack, this.rot);
        stack.translate(this.pos.getX() / 16f, this.pos.getY() / 16f, this.pos.getZ() / 16f);
    }

    //Re-builds the mesh data for a custom model part.
    public void rebuild(Vec2f newTexSize) {
        this.texSize = newTexSize;
    }

    public void rotate(MatrixStack stack, Vec3f rot) {
        stack.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(rot.getZ()));
        stack.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(-rot.getY()));
        stack.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(-rot.getX()));
    }

    public void vanillaRotate(MatrixStack stack, Vec3f rot) {
        stack.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(rot.getZ()));
        stack.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(rot.getY()));
        stack.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(rot.getX()));
    }

    public void addVertex(Vec3f vert, float u, float v, Vec3f normal, FloatList vertexData) {
        vertexData.add(vert.getX() / 16f);
        vertexData.add(vert.getY() / 16f);
        vertexData.add(vert.getZ() / 16f);
        vertexData.add(u);
        vertexData.add(v);
        vertexData.add(-normal.getX());
        vertexData.add(-normal.getY());
        vertexData.add(-normal.getZ());
    }

    public void readNbt(NbtCompound partNbt) {
        if (partNbt.contains("nm"))
            this.name = partNbt.getString("nm");
        else
            this.name = "NULL";

        if (partNbt.contains("vb"))
            this.visible = partNbt.getBoolean("vb");

        if (partNbt.contains("pos")) {
            NbtList list = (NbtList) partNbt.get("pos");
            this.pos = vec3fFromNbt(list);
        }
        if (partNbt.contains("rot")) {
            NbtList list = (NbtList) partNbt.get("rot");
            this.rot = vec3fFromNbt(list);
        }
        if (partNbt.contains("scl")) {
            NbtList list = (NbtList) partNbt.get("scl");
            this.scale = vec3fFromNbt(list);
        }
        if (partNbt.contains("piv")) {
            NbtList list = (NbtList) partNbt.get("piv");
            this.pivot = vec3fFromNbt(list);
        }

        if (partNbt.contains("stype")) {
            try {
                this.shaderType = ShaderType.valueOf(partNbt.getString("stype"));
            } catch (Exception ignored) {
                this.shaderType = ShaderType.None;
            }
        }
    }

    public PartType getPartType() {
        return null;
    }

    public enum ParentType {
        Model,
        Head,
        LeftArm,
        RightArm,
        LeftLeg,
        RightLeg,
        Torso,
        WORLD(true),
        LeftItemOrigin, //Origin position of the held item in the left hand
        RightItemOrigin, //Origin position of the held item
        LeftElytraOrigin, //Left origin position of the elytra
        RightElytraOrigin, //Right origin position of the elytra
        LeftParrotOrigin, //Left origin position of the shoulder parrot
        RightParrotOrigin, //Right origin position of the shoulder parrot
        LeftElytra(true), //Left position of the elytra model
        RightElytra(true), //Right position of the elytra model
        LeftSpyglass, //Left position of the spyglass model
        RightSpyglass, //Right position of the spyglass model
        Camera, //paparazzi
        Skull(true), //A replacement for the "Head" type, but only rendered in the tab list and player head item/blocks
        Hud(true); //hud rendering

        private final boolean special;
        ParentType(boolean special) {
            this.special = special;
        }
        ParentType() {
            this.special = false;
        }
    }

    public boolean isSpecial() {
        return this.parentType.special;
    }

    public enum ShaderType {
        None(0),
        EndPortal(1),
        Glint(2);

        public final int id;
        ShaderType(int id) {
            this.id = id;
        }

        public boolean isShader(int shader) {
            return (id & shader) == id;
        }
    }

    public enum TextureType {
        Custom,
        Skin,
        Cape,
        Elytra,
        Resource
    }

    public enum UV {
        ALL,
        NORTH,
        SOUTH,
        WEST,
        EAST,
        UP,
        DOWN
    }

    public enum PartType {
        GROUP("na"),
        CUBE("cub"),
        MESH("msh");

        public final String val;
        PartType(String value) {
            this.val = value;
        }
    }

    public static class uvData {
        public Vec2f uvOffset, uvSize;

        public void setUVOffset(Vec2f uvOffset) {
            this.uvOffset = uvOffset;
        }

        public void setUVSize(Vec2f uvSize) {
            this.uvSize = uvSize;
        }
    }

    public void applyUVMods(Vec2f v) {
        rebuild(v);
    }

    //---------MODEL PART TYPES---------

    public static final Map<String, Supplier<CustomModelPart>> MODEL_PART_TYPES =
            new ImmutableMap.Builder<String, Supplier<CustomModelPart>>()
                    .put("na", CustomModelPartGroup::new)
                    .put("cub", CustomModelPartCuboid::new)
                    .put("msh", CustomModelPartMesh::new)
                    .build();

    /**
     * Gets a CustomModelPart from NBT, automatically reading the type from that NBT.
     */
    public static CustomModelPart fromNbt(NbtCompound nbt, CustomModel model) {
        NbtElement pt = nbt.get("pt");
        String partType = pt == null ? "na" : pt.asString();

        if (!MODEL_PART_TYPES.containsKey(partType))
            return null;

        Supplier<CustomModelPart> sup = MODEL_PART_TYPES.get(partType);
        CustomModelPart part = sup.get();

        part.model = model;
        part.readNbt(nbt);
        return part;
    }

    public static Vec3f vec3fFromNbt(@Nullable NbtList nbt) {
        if (nbt == null || nbt.getHeldType() != NbtType.FLOAT)
            return new Vec3f(0f, 0f, 0f);
        return new Vec3f(nbt.getFloat(0), nbt.getFloat(1), nbt.getFloat(2));
    }

    public static Vector4f v4fFromNbtList(NbtList list) {
        return new Vector4f(list.getFloat(0), list.getFloat(1), list.getFloat(2), list.getFloat(3));
    }

    public static Vec2f v2fFromNbtList(NbtList list) {
        return new Vec2f(list.getFloat(0), list.getFloat(1));
    }
}
