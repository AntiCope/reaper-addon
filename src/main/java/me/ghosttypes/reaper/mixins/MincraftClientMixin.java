package me.ghosttypes.reaper.mixins;

import me.ghosttypes.reaper.Reaper;
import me.ghosttypes.reaper.events.InteractEvent;
import me.ghosttypes.reaper.modules.misc.MultiTask;
import me.ghosttypes.reaper.util.player.Interactions;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.mixininterface.IMinecraftClient;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.io.InputStream;

@Mixin(
    value = {MinecraftClient.class},
    priority = 1001
)
public abstract class MincraftClientMixin implements IMinecraftClient {

    @Inject(method = "getWindowTitle", at = @At("HEAD"), cancellable = true)
    public void getWindowTitle(CallbackInfoReturnable<String> ci){
        String title = "Reaper " + Reaper.VERSION;
        if (Interactions.isDeveloper()) title = "Reaper " + Reaper.VERSION + " [Developer Edition]";
        if (Interactions.isBetaUser()) title = "Reaper " + Reaper.VERSION + " [Beta Edition]";
        ci.setReturnValue(title);
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/Window;setIcon(Ljava/io/InputStream;Ljava/io/InputStream;)V"))
    public void setAlternativeWindowIcon(Window window, InputStream inputStream1, InputStream inputStream2) throws IOException {
        window.setIcon(
            Reaper.class.getResourceAsStream("/assets/reaper/16.png"),
            Reaper.class.getResourceAsStream("/assets/reaper/32.png")
        );
    }

    @Redirect(
        method = {"handleBlockBreaking"},
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z"
        )
    )
    public boolean breakBlockCheck(ClientPlayerEntity clientPlayerEntity) {
        return !Modules.get().isActive(MultiTask.class) && ((InteractEvent) MeteorClient.EVENT_BUS.post((Object) InteractEvent.get(clientPlayerEntity.isUsingItem()))).usingItem;
    }

    @Redirect(
        method = {"doItemUse"},
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;isBreakingBlock()Z"
        )
    )
    public boolean useItemBreakCheck(ClientPlayerInteractionManager clientPlayerInteractionManager) {
        return !Modules.get().isActive(MultiTask.class) && ((InteractEvent) MeteorClient.EVENT_BUS.post((Object) InteractEvent.get(clientPlayerInteractionManager.isBreakingBlock()))).usingItem;
    }
}
