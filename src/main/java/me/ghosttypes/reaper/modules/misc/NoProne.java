package me.ghosttypes.reaper.modules.misc;

import me.ghosttypes.reaper.modules.ML;
import me.ghosttypes.reaper.util.misc.ReaperModule;
import me.ghosttypes.reaper.util.player.Interactions;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.EntityPose;

public class NoProne extends ReaperModule {

    public NoProne() {
        super(ML.M, "no-prone", "Prevents you from going into the prone position.");
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        if (Interactions.isInElytra()) return;
        mc.player.setPose(EntityPose.STANDING);
    }
}
