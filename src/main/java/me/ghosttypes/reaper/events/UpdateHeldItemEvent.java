package me.ghosttypes.reaper.events;

import net.minecraft.client.render.item.HeldItemRenderer;

import meteordevelopment.meteorclient.events.Cancellable;

public class UpdateHeldItemEvent extends Cancellable {
    public static final UpdateHeldItemEvent INSTANCE = new UpdateHeldItemEvent();

    public HeldItemRenderer renderer;

    public static UpdateHeldItemEvent get(HeldItemRenderer renderer) {
        INSTANCE.renderer = renderer;
        INSTANCE.setCancelled(false);
        return INSTANCE;
    }
}
