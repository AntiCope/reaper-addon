package me.ghosttypes.reaper.mixins.meteor;

import meteordevelopment.meteorclient.systems.Systems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Systems.class)
public class MeteorBootstrap {
    @Inject(method = "init", at = @At(value = "INVOKE", target = "Lmeteordevelopment/meteorclient/systems/System;load()V"), remap = false)
    private static void onInitialiseSystem(CallbackInfo ci) {
        //Reaper.log("Injecting config tab to Meteor");
        //new ConfigTweaker();
    }
}
