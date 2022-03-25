package me.ghosttypes.reaper.modules.misc.elytrabot.events;

import meteordevelopment.meteorclient.events.Cancellable;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.Vec3d;

public class CancellablePlayerMoveEvent extends Cancellable {
    private static final CancellablePlayerMoveEvent INSTANCE = new CancellablePlayerMoveEvent();

    public MovementType type;
    public Vec3d movement;

    public static CancellablePlayerMoveEvent get(MovementType type, Vec3d movement) {
        INSTANCE.setCancelled(false);
        INSTANCE.type = type;
        INSTANCE.movement = movement;
        return INSTANCE;
    }
}
