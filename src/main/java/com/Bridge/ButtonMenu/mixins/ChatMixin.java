package com.Bridge.ButtonMenu.mixins;

import com.Bridge.ButtonMenu.SimpleButtonMod;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatComponent.class)
public class ChatMixin {

    // Hook into the simple addMessage(Component) overload to avoid signature drift between versions
    @ModifyVariable(
        method = "addMessage(Lnet/minecraft/network/chat/Component;)V",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 0
    )
    private Component modifyMessage(Component message) {
        if (message == null) {
            return message;
        }

        String unformatted = message.getString();
        Component processed = SimpleButtonMod.processChatMessage(message, unformatted);

        // If message is blocked, return empty component
        if (processed == null) {
            return Component.empty();
        }

        return processed;
    }

    @Inject(
        method = "addMessage(Lnet/minecraft/network/chat/Component;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void cancelEmptyMessages(Component message, CallbackInfo ci) {
        // Cancel empty messages (blocked messages)
        if (message != null && message.getString().isEmpty() && message.getSiblings().isEmpty()) {
            ci.cancel();
        }
    }
}
