package net.blancworks.figura.mixin;

import net.blancworks.figura.avatar.AvatarData;
import net.blancworks.figura.access.ElytraEntityModelAccess;
import net.blancworks.figura.access.MatrixStackAccess;
import net.blancworks.figura.lua.api.model.ElytraModelAPI;
import net.blancworks.figura.lua.api.model.VanillaModelPartCustomization;
import net.blancworks.figura.models.CustomModelPart;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.AnimalModel;
import net.minecraft.client.render.entity.model.ElytraEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ElytraEntityModel.class)
public class ElytraEntityModelMixin<T extends LivingEntity> extends AnimalModel<T> implements ElytraEntityModelAccess {

    @Shadow
    @Final
    private ModelPart leftWing;
    @Shadow
    @Final
    private ModelPart rightWing;

    @Override
    @Shadow
    protected Iterable<ModelPart> getHeadParts() {
        return null;
    }

    @Override
    @Shadow
    protected Iterable<ModelPart> getBodyParts() {
        return null;
    }

    @Override
    @Shadow
    public void setAngles(T entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {

    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha) {
        AvatarData data = AvatarData.currentRenderingData;
        
        try {
            if (data != null && data.model != null) {
                VanillaModelPartCustomization originModification;

                //Left wing
                originModification = data.model.originModifications.get(ElytraModelAPI.VANILLA_LEFT_WING_ID);

                if (originModification != null && originModification.stackReference != null) {
                    if (originModification.visible != null && originModification.visible) {
                        originModification.visible = null;
                        MatrixStackAccess msa = (MatrixStackAccess) new MatrixStack();
                        msa.pushEntry(originModification.stackReference);
                        getLeftWing().render((MatrixStack) msa, vertices, light, overlay, red, green, blue, alpha);
                    }

                    getLeftWing().visible = false;
                }

                //Right wing
                originModification = data.model.originModifications.get(ElytraModelAPI.VANILLA_RIGHT_WING_ID);

                if (originModification != null && originModification.stackReference != null) {
                    if (originModification.visible != null && originModification.visible) {
                        originModification.visible = null;
                        MatrixStackAccess msa = (MatrixStackAccess) new MatrixStack();
                        msa.pushEntry(originModification.stackReference);
                        getRightWing().render((MatrixStack) msa, vertices, light, overlay, red, green, blue, alpha);
                    }

                    getRightWing().visible = false;
                }
            }

            super.render(matrices, vertices, light, overlay, red, green, blue, alpha);
        } catch (Exception e){
            //e.printStackTrace();

            super.render(matrices, vertices, light, overlay, red, green, blue, alpha);
        }

        try {
            getLeftWing().visible = true;
            getRightWing().visible = true;

            if (data != null && data.model != null) {
                figura$renderExtraElytraPartsWithTexture(data, matrices, light, overlay, alpha);
            }
        } catch (Exception e){
            //e.printStackTrace();
        }
    }

    public ModelPart getLeftWing() {
        return leftWing;
    }

    public ModelPart getRightWing() {
        return rightWing;
    }

    public void figura$renderExtraElytraPartsWithTexture(AvatarData data, MatrixStack matrices, int light, int overlay, float alpha) {
        //Render left parts.
        matrices.push();

        ModelPart leftWing = getLeftWing();
        leftWing.rotate(matrices);
        matrices.translate(-leftWing.pivotX / 16f, 0f, 0f);

        synchronized (data.model.specialParts) {
            for (CustomModelPart modelPart : data.model.getSpecialParts(CustomModelPart.ParentType.LeftElytra)) {
                data.model.leftToRender = modelPart.render(data, matrices, new MatrixStack(), data.vertexConsumerProvider, light, overlay, alpha);

                if (data.model.leftToRender == 0)
                    break;
            }
        }

        matrices.pop();

        //Render right parts.
        matrices.push();

        ModelPart rightWing = getRightWing();
        rightWing.rotate(matrices);
        matrices.translate(-rightWing.pivotX / 16f, 0f, 0f);

        synchronized (data.model.specialParts) {
            for (CustomModelPart modelPart : data.model.getSpecialParts(CustomModelPart.ParentType.RightElytra)) {
                data.model.leftToRender = modelPart.render(data, matrices, new MatrixStack(), data.vertexConsumerProvider, light, overlay, alpha);

                if (data.model.leftToRender == 0)
                    break;
            }
        }

        matrices.pop();
    }
}
