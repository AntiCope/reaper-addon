package me.ghosttypes.reaper.modules.misc;

import me.ghosttypes.reaper.events.InteractEvent;
import me.ghosttypes.reaper.modules.ML;
import me.ghosttypes.reaper.util.misc.ReaperModule;
import meteordevelopment.orbit.EventHandler;

public class MultiTask extends ReaperModule {
    public MultiTask() {
        super(ML.M, "multi-task", "Allows you to eat while mining a block.");
    }

    @EventHandler
    public void onInteractEvent(InteractEvent event) {
        event.usingItem = false;
    }
}
