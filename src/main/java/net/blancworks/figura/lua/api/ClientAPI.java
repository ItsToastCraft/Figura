package net.blancworks.figura.lua.api;

import net.blancworks.figura.access.InGameHudAccess;
import net.blancworks.figura.avatar.AvatarDataManager;
import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.api.math.LuaVector;
import net.blancworks.figura.utils.TextUtils;
import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.Version;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.util.Window;
import net.minecraft.util.Identifier;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

public class ClientAPI {

    public static Identifier getID() {
        return new Identifier("default", "client");
    }

    public static LuaTable getForScript(CustomScript script) {
        MinecraftClient client = MinecraftClient.getInstance();
        final boolean isHost = script.avatarData == AvatarDataManager.localPlayer;

        return new LuaTable() {{
            set("getOpenScreen", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    //always return nil when not the local player
                    if (client.currentScreen == null)
                        return NIL;

                    //get the current screen
                    String screenTitle = client.currentScreen.getTitle().getString();
                    if (screenTitle.equals(""))
                        screenTitle = client.currentScreen.getClass().getSimpleName();

                    return LuaValue.valueOf(screenTitle);
                }
            });

            set("getFPS", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(client.fpsDebugString);
                }
            });

            set("isPaused", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(client.isPaused());
                }
            });

            set("getVersion", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(SharedConstants.getGameVersion().getName());
                }
            });

            set("getVersionType", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(client.getVersionType());
                }
            });

            set("getServerBrand", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(client.getServer() == null ? client.player.getServerBrand() : "Integrated");
                }
            });

            set("getChunksCount", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(client.worldRenderer.getChunksDebugString());
                }
            });

            set("getEntityCount", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(client.worldRenderer.getEntitiesDebugString());
                }
            });

            set("getParticleCount", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(client.particleManager.getDebugString());
                }
            });

            set("getSoundCount", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(client.getSoundManager().getDebugString());
                }
            });

            set("getActiveShader", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    if (client.gameRenderer.getShader() == null)
                        return NIL;

                    return LuaValue.valueOf(client.gameRenderer.getShader().getName());
                }
            });

            set("getJavaVersion", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(System.getProperty("java.version"));
                }
            });

            set("getMemoryInUse", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
                }
            });

            set("getMaxMemory", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(Runtime.getRuntime().maxMemory());
                }
            });

            set("getAllocatedMemory", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(Runtime.getRuntime().totalMemory());
                }
            });

            set("isWindowFocused", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(client.isWindowFocused());
                }
            });

            set("isHudEnabled", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(MinecraftClient.isHudEnabled());
                }
            });

            set("getWindowSize", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    Window window = MinecraftClient.getInstance().getWindow();
                    return new LuaVector(window.getWidth(), window.getHeight());
                }
            });

            set("getGUIScale", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(MinecraftClient.getInstance().options.getGuiScale().getValue());
                }
            });

            set("getFov", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(MinecraftClient.getInstance().options.getFov().getValue());
                }
            });

            set("setCrosshairPos", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    script.crossHairPos = LuaVector.checkOrNew(arg).asV2f();
                    return NIL;
                }
            });

            set("getCrosshairPos", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaVector.of(script.crossHairPos);
                }
            });

            set("setCrosshairEnabled", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    script.crossHairEnabled = arg.checkboolean();
                    return NIL;
                }
            });

            set("getCrosshairEnabled", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(script.crossHairEnabled);
                }
            });

            set("isHost", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(script.avatarData == AvatarDataManager.localPlayer);
                }
            });

            set("getSystemTime", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(System.currentTimeMillis());
                }
            });

            set("getMousePos", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    Mouse mouse = MinecraftClient.getInstance().mouse;
                    return new LuaVector((float) mouse.getX(), (float) mouse.getY());
                }
            });

            set("getScaledWindowSize", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    Window window = MinecraftClient.getInstance().getWindow();
                    return new LuaVector(window.getScaledWidth(), window.getScaledHeight());
                }
            });

            set("getScaleFactor", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(MinecraftClient.getInstance().getWindow().getScaleFactor());
                }
            });

            set("setTitleTimes", new ThreeArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1, LuaValue arg2, LuaValue arg3) {
                    client.inGameHud.setTitleTicks(arg1.checkint(), arg2.checkint(), arg3.checkint());
                    return NIL;
                }
            });

            set("clearTitle", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    client.inGameHud.clearTitle();
                    return NIL;
                }
            });

            set("setTitle", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    if (isHost) client.inGameHud.setTitle(TextUtils.tryParseJson(arg1.checkjstring()));
                    return NIL;
                }
            });

            set("getTitle", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(((InGameHudAccess) client.inGameHud).getTitle().asString());
                }
            });

            set("setSubtitle", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    if (isHost) client.inGameHud.setSubtitle(TextUtils.tryParseJson(arg1.checkjstring()));
                    return NIL;
                }
            });

            set("getSubtitle", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(((InGameHudAccess) client.inGameHud).getSubtitle().asString());
                }
            });

            set("getActionbar", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(((InGameHudAccess) client.inGameHud).getOverlayMessage().asString());
                }
            });

            set("setActionbar", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    if (isHost && client.player != null)
                        client.player.sendMessage(TextUtils.tryParseJson(arg1.checkjstring()), true);

                    return NIL;
                }
            });

            set("setMouseUnlocked", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    script.unlockCursor = arg.checkboolean();
                    return NIL;
                }
            });

            set("getIrisShadersEnabled", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(RenderLayerAPI.areIrisShadersEnabled());
                }
            });

            set("checkVersion", new TwoArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1, LuaValue arg2) {
                    try {
                        SemanticVersion latest = SemanticVersion.parse(arg1.checkjstring());
                        SemanticVersion current = SemanticVersion.parse(arg2.checkjstring());
                        return LuaValue.valueOf(latest.compareTo((Version) current));
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw new LuaError("Failed to compare versions!");
                    }
                }
            });

            set("getMouseScroll", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    LuaValue ret = LuaValue.valueOf(CustomScript.mouseScroll);
                    CustomScript.mouseScroll = 0d;
                    return ret;
                }
            });
        }};
    }
}
