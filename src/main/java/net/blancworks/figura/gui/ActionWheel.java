package net.blancworks.figura.gui;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import net.blancworks.figura.avatar.AvatarData;
import net.blancworks.figura.avatar.AvatarDataManager;
import net.blancworks.figura.config.ConfigManager.Config;
import net.blancworks.figura.lua.api.actionwheel.ActionWheelCustomization;
import net.blancworks.figura.models.FiguraTexture;
import net.blancworks.figura.utils.MathUtils;
import net.blancworks.figura.utils.TextUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.texture.MissingSprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3f;

import java.util.List;
import java.util.Objects;

public class ActionWheel extends DrawableHelper {

    private static final Identifier ACTION_WHEEL = new Identifier("figura", "textures/gui/action_wheel.png");
    private static final Identifier ACTION_WHEEL_SELECTED = new Identifier("figura", "textures/gui/action_wheel_selected.png");
    private static final Vec3f ERROR_COLOR = new Vec3f(1f, 0.28f, 0.28f);
    private static final List<Text> NO_FUNCTION_MESSAGE = ImmutableList.of(new TranslatableText("figura.actionwheel.nofunction"));

    public static int selectedSlot = -1;
    public static boolean enabled = false;

    public static void render(MatrixStack matrices) {
        MinecraftClient client = MinecraftClient.getInstance();

        //screen
        Vec2f screenSize = new Vec2f(client.getWindow().getWidth() / 2f, client.getWindow().getHeight() / 2f);
        float screenScale = (float) client.getWindow().getScaleFactor();

        //mouse
        Vec2f mousePos = new Vec2f((float) client.mouse.getX() - screenSize.x, (float) client.mouse.getY() - screenSize.y);
        float angle = getAngle(mousePos.x, mousePos.y);
        float distance = MathHelper.sqrt(mousePos.x * mousePos.x + mousePos.y * mousePos.y);

        //wheel
        Vec2f wheelPos = new Vec2f(screenSize.x / screenScale, screenSize.y / screenScale);
        int wheelSize = 180;

        //item
        Vec2f itemOffset = new Vec2f((wheelPos.x * 2f / 3f) - 8f, (wheelPos.y * 2f / 3f) - 8f);
        int itemRadius = 39;

        //script data
        AvatarData data = AvatarDataManager.localPlayer;

        //render
        RenderSystem.enableBlend();

        if (data != null && data.script != null) {
            int leftSegments = data.script.actionWheelLeftSize;
            int rightSegments = data.script.actionWheelRightSize;

            //set selected slot
            if (distance > 28 * screenScale) {
                if (angle < 180) {
                    selectedSlot = MathHelper.floor((rightSegments / 180f) * angle);
                } else {
                    selectedSlot = MathHelper.floor((leftSegments / 180f) * (angle - 180)) + rightSegments;
                }
            } else {
                selectedSlot = -1;
            }

            //render wheel
            renderWheel(matrices, wheelPos, wheelSize, leftSegments, rightSegments);

            //render overlay
            for (int i = 0; i < leftSegments + rightSegments; i++) {
                renderOverlay(matrices, wheelPos, wheelSize, leftSegments, rightSegments, data, i);
            }

            //render textures
            renderTextures(matrices, leftSegments, rightSegments, itemOffset, itemRadius, data);

            //render text
            if (selectedSlot != -1) {
                renderText(matrices, wheelPos, wheelSize, screenScale, data, client);
            }

            //render items
            renderItems(leftSegments, rightSegments, itemOffset, itemRadius, data, client);
        }
        else {
            //draw default wheel
            renderWheel(matrices, wheelPos, wheelSize, 4, 4);

            //draw warning texts
            drawCenteredTextWithShadow(
                    matrices, MinecraftClient.getInstance().textRenderer,
                    new TranslatableText("figura.actionwheel.warning").formatted(Formatting.UNDERLINE).asOrderedText(),
                    (int) wheelPos.x, (int) wheelPos.y - 4,
                    Formatting.RED.getColorValue()
            );
            drawCenteredTextWithShadow(
                    matrices, MinecraftClient.getInstance().textRenderer,
                    new TranslatableText("figura.actionwheel.warninginfo").asOrderedText(),
                    (int) wheelPos.x, (int) Math.max(wheelPos.y - wheelSize / 2f - 10, 4),
                    Formatting.RED.getColorValue()
            );
        }

        RenderSystem.disableBlend();
        enabled = true;
    }

    public static float getAngle(float x, float y) {
        float ang = (float) Math.toDegrees(MathHelper.atan2(x, -y));
        return ang < 0 ? 360 + ang : ang;
    }

    public static void renderWheel(MatrixStack matrices, Vec2f pos, int size, int leftSegments, int rightSegments) {
        //texture
        RenderSystem.setShaderTexture(0, ACTION_WHEEL);

        //draw right side
        matrices.push();

        matrices.translate(Math.round(pos.x), Math.round(pos.y - size / 2f), 0f);
        drawTexture(matrices, 0, 0, size / 2, size, 8f * (rightSegments - 1), 0f, 8, 16, 32, 16);

        matrices.pop();

        //draw left side
        matrices.push();

        matrices.translate(Math.round(pos.x), Math.round(pos.y + size / 2f), 0f);
        Quaternion quaternion = Vec3f.POSITIVE_Z.getDegreesQuaternion(180);
        matrices.multiply(quaternion);

        drawTexture(matrices, 0, 0, size / 2, size, 8f * (leftSegments - 1), 0f, 8, 16, 32, 16);

        matrices.pop();
    }

    public static void renderOverlay(MatrixStack matrices, Vec2f pos, int size, int leftSegments, int rightSegments, AvatarData data, int i) {
        ActionWheelCustomization customization = data.script.getActionWheelCustomization("SLOT_" + (i + 1));

        //property variables
        boolean hasFunction = false;
        boolean hasColor = false;
        boolean hasHoverColor = false;
        boolean isSelected = selectedSlot == i;
        int slot = i < rightSegments ? i : i - rightSegments + 4;
        Vec3f overlayColor = MathUtils.Vec3f_ONE;

        if (customization != null) {
            hasFunction = customization.function != null;
            hasColor = customization.color != null;
            hasHoverColor = customization.hoverColor != null;
        }

        //set default color
        if (hasColor)
            overlayColor = customization.color;

        //if is selected, but has no function, set to error color
        //if it has function and has an hover color, set to the hover color
        if (isSelected) {
            if (!hasFunction) {
                overlayColor = ERROR_COLOR;
            } else if (hasHoverColor) {
                overlayColor = customization.hoverColor;
            }
        } else if (!hasColor) {
            return;
        }

        //modifiable variables
        int segments;
        int selected;

        if (slot < 4) {
            segments = rightSegments;
            selected = slot;
        } else {
            segments = leftSegments;
            selected = slot - 4 + leftSegments;
        }

        double y = pos.y;
        float angle = 0f;
        int height = size / 2;
        float u = 0f;
        float v = 0f;
        int regionHeight = 8;

        switch (segments) {
            case 1 -> {
                y = selected % 2 == 1 ? pos.y + size / 2f : pos.y - size / 2f;
                angle = 180f * selected;
                height = size;
                regionHeight = 16;
            }
            case 2 -> {
                angle = 90f * (selected - 1f);
                u = 8f;
            }
            case 3 -> {
                if (selected % 3 != 2) {
                    y += (selected < 3 ? -1 : 1) * size / 2f;

                    if (selected % 3 == 1) {
                        y += (selected < 3 ? 1 : -1) * size / 4f;
                        v = 8f;
                    }

                    u = 16f;
                }
                else {
                    u = 8f;
                    v = 8f;
                }

                angle = 180f * MathHelper.floor(selected / 3f);
            }
            case 4 -> {
                angle = 90f * (MathHelper.floor(selected / 2f) + 3f);
                u = 24f;
                v = selected % 2 == 1 ? 8f : 0f;
            }
        }

        //texture
        RenderSystem.setShaderTexture(0, ACTION_WHEEL_SELECTED);

        //draw
        matrices.push();

        matrices.translate(Math.round(pos.x), Math.round(y), 0f);
        Quaternion quaternion = Vec3f.POSITIVE_Z.getDegreesQuaternion(angle);
        matrices.multiply(quaternion);

        RenderSystem.setShaderColor(overlayColor.getX(), overlayColor.getY(), overlayColor.getZ(), 1f);
        drawTexture(matrices, 0, 0, size / 2, height, u, v, 8, regionHeight, 32, 16);

        matrices.pop();
    }

    public static void renderTextures(MatrixStack matrices, int leftSegments, int rightSegments, Vec2f offset, int radius, AvatarData data) {
        for (int i = 0; i < leftSegments + rightSegments; i++) {

            int index;
            float angle;
            if (i < rightSegments) {
                index = i;
                angle = (float) Math.toRadians(180f / rightSegments * (index - ((rightSegments - 1) * 0.5f)));
            } else {
                index = i - rightSegments + 4;
                angle = (float) Math.toRadians(180f / leftSegments * (index - 4 - ((leftSegments - 1) * 0.5f) + leftSegments));
            }

            //radius * cos/sin angle in rads + offset
            Vec2f pos = new Vec2f(radius * MathHelper.cos(angle) + offset.x, radius * MathHelper.sin(angle) + offset.y);

            //render textures
            ActionWheelCustomization cust = data.script.getActionWheelCustomization("SLOT_" + (i + 1));
            if (cust != null && cust.texture != ActionWheelCustomization.TextureType.None && cust.uvOffset != null && cust.uvSize != null && data.playerListEntry != null) {
                //texture
                Identifier textureId;
                switch (cust.texture) {
                    case Cape -> textureId = Objects.requireNonNullElse(data.playerListEntry.getCapeTexture(), FiguraTexture.DEFAULT_ID);
                    case Elytra -> textureId = Objects.requireNonNullElse(data.playerListEntry.getElytraTexture(), FiguraTexture.ELYTRA_ID);
                    case Resource -> textureId = MinecraftClient.getInstance().getResourceManager().getResource(cust.texturePath).isPresent() ? cust.texturePath : MissingSprite.getMissingSpriteId();
                    case Skin -> textureId = data.playerListEntry.getSkinTexture();
                    case Custom -> textureId = data.texture != null ? data.texture.id : MissingSprite.getMissingSpriteId();
                    default -> textureId = FiguraTexture.DEFAULT_ID;
                }

                RenderSystem.setShaderTexture(0, textureId);
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
                matrices.push();
                matrices.scale(1.5f, 1.5f, 1.5f);

                Vec2f off = cust.uvOffset;
                Vec2f len = cust.uvSize;
                Vec2f siz = cust.textureSize;
                Vec2f scl = cust.textureScale;

                drawTexture(matrices, (int) (pos.x + 8 - (siz.x * scl.x) / 2), (int) (pos.y + 8 - (siz.y * scl.y) / 2), (int) (siz.x * scl.x), (int) (siz.y * scl.y), off.x, off.y, (int) (len.x), (int) (len.y), (int) (siz.x), (int) (siz.y));

                matrices.pop();
            }
        }
    }

    public static void renderText(MatrixStack matrices, Vec2f pos, int size, float scale, AvatarData data, MinecraftClient client) {
        //customization
        ActionWheelCustomization customization = data.script.getActionWheelCustomization("SLOT_" + (selectedSlot + 1));

        List<Text> lines = NO_FUNCTION_MESSAGE;
        int textColor = Formatting.RED.getColorValue();

        if (customization != null && customization.function != null) {
            if (customization.title == null)
                return;

            Text text = TextUtils.tryParseJson(customization.title);
            lines = TextUtils.splitText(text, "\n");

            textColor = Formatting.WHITE.getColorValue();
        }

        //text pos
        Vec2f textPos;
        int titleLen = client.textRenderer.getWidth(lines.get(0)) / 2;

        switch ((int) Config.ACTION_WHEEL_TITLE_POS.value) {
            //top
            case 1 -> textPos = new Vec2f(pos.x - titleLen, Math.max(pos.y - size / 2f - 10, 4));
            //bottom
            case 2 -> textPos = new Vec2f(pos.x - titleLen, Math.min(pos.y + size / 2f + 4, client.getWindow().getHeight() - 12));
            //center
            case 3 -> textPos = new Vec2f(pos.x - titleLen, pos.y - 4);
            //default mouse
            default -> textPos = new Vec2f((float) client.mouse.getX() / scale, (float) client.mouse.getY() / scale - 10);
        }

        //draw
        matrices.push();
        matrices.translate(0, 0, 599);
        int i = 0;
        for (Text text : lines) {
            drawTextWithShadow(matrices, client.textRenderer, text, (int) textPos.x, (int) textPos.y + (i - lines.size() + 1) * 9, textColor);
            i++;
        }
        matrices.pop();
    }

    public static void renderItems(int leftSegments, int rightSegments, Vec2f offset, int radius, AvatarData data, MinecraftClient client) {
        for (int i = 0; i < leftSegments + rightSegments; i++) {

            int index;
            float angle;
            if (i < rightSegments) {
                index = i;
                angle = (float) Math.toRadians(180f / rightSegments * (index - ((rightSegments - 1) * 0.5f)));
            } else {
                index = i - rightSegments + 4;
                angle = (float) Math.toRadians(180f / leftSegments * (index - 4 - ((leftSegments - 1) * 0.5f) + leftSegments));
            }

            //radius * cos/sin angle in rads + offset
            Vec2f pos = new Vec2f(radius * MathHelper.cos(angle) + offset.x, radius * MathHelper.sin(angle) + offset.y);

            //get item - defaults to air
            ItemStack item = Items.AIR.getDefaultStack();

            ActionWheelCustomization cust = data.script.getActionWheelCustomization("SLOT_" + (i + 1));
            if (cust != null) {
                if (cust.texture != ActionWheelCustomization.TextureType.None && cust.uvOffset != null && cust.uvSize != null)
                    continue;

                if (selectedSlot == index && cust.hoverItem != null) {
                    item = cust.hoverItem;
                } else if (cust.item != null) {
                    item = cust.item;
                }
            }

            //render
            MatrixStack matrixStack = RenderSystem.getModelViewStack();
            matrixStack.push();
            matrixStack.scale(1.5f, 1.5f, 1.5f);

            client.getItemRenderer().renderGuiItemIcon(item, (int) pos.x, (int) pos.y);

            matrixStack.pop();
            RenderSystem.applyModelViewMatrix();
        }
    }

    public static void play() {
        if (selectedSlot == -1)
            return;

        AvatarData currentData = AvatarDataManager.localPlayer;
        if (currentData != null && currentData.script != null) {
            ActionWheelCustomization customization = currentData.script.getActionWheelCustomization("SLOT_" + (selectedSlot + 1));

            if (customization != null && customization.function != null) {
                currentData.script.runActionWheelFunction(customization.function, customization.arg);
            }
        }

        selectedSlot = -1;
    }
}
