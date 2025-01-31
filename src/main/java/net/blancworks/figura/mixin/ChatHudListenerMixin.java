package net.blancworks.figura.mixin;

import net.blancworks.figura.avatar.AvatarData;
import net.blancworks.figura.avatar.AvatarDataManager;
import net.blancworks.figura.config.ConfigManager.Config;
import net.blancworks.figura.lua.api.nameplate.NamePlateAPI;
import net.blancworks.figura.lua.api.nameplate.NamePlateCustomization;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHudListener;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.network.MessageType;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(ChatHudListener.class)
public class ChatHudListenerMixin {

    @Shadow @Final private MinecraftClient client;

    @Inject(method = "onChatMessage", at = @At("HEAD"))
    private void onChatMessage(MessageType type, Text message, UUID uuid, CallbackInfo ci) {
        if (!(boolean) Config.CHAT_MODIFICATIONS.value)
            return;

        String playerName = "";

        //get player profile
        PlayerListEntry playerEntry = this.client.player == null ? null : this.client.player.networkHandler.getPlayerListEntry(uuid);

        if (playerEntry == null) {
            String textString = message.getString();
            for (String part : textString.split("(§.)|[^\\w]")) {
                if (part.isEmpty())
                    continue;

                PlayerListEntry entry = this.client.player.networkHandler.getPlayerListEntry(part);
                if (entry != null) {
                    playerName = entry.getProfile().getName();
                    uuid = entry.getProfile().getId();
                    break;
                }
            }
        } else {
            playerName = playerEntry.getProfile().getName();
        }

        //player not found
        if (playerName.equals(""))
            return;

        //get player data
        AvatarData currentData = AvatarDataManager.getDataForPlayer(uuid);
        if (currentData == null)
            return;

        //apply formatting
        NamePlateCustomization nameplateData = currentData.script == null ? null : currentData.script.nameplateCustomizations.get(NamePlateAPI.CHAT);

        try {
            if (message instanceof TranslatableText) {
                Object[] args = ((TranslatableText) message).getArgs();

                for (Object arg : args) {
                    if (arg instanceof TranslatableText || !(arg instanceof Text))
                        continue;

                    if (NamePlateAPI.applyFormattingRecursive((LiteralText) arg, playerName, nameplateData, currentData))
                        break;
                }
            } else if (message instanceof LiteralText literal) {
                NamePlateAPI.applyFormattingRecursive(literal, playerName, nameplateData, currentData);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
