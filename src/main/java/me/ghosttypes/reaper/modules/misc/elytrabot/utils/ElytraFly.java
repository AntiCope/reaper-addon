package me.ghosttypes.reaper.modules.misc.elytrabot.utils;

import me.ghosttypes.reaper.modules.misc.elytrabot.ElytraBotThreaded;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.util.math.BlockPos;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class ElytraFly {
    public static boolean toggled;

    public static void toggle(boolean on) {
        toggled = on;
    }

    public static void setMotion(BlockPos pos, BlockPos next, BlockPos previous) {
        if (!toggled) return;

        double x = 0, y = 0, z = 0;
        double xDiff = (pos.getX() + 0.5) - mc.player.getX();
        double yDiff = (pos.getY() + 0.4) - mc.player.getY();
        double zDiff = (pos.getZ() + 0.5) - mc.player.getZ();

        double speed = Modules.get().get(ElytraBotThreaded.class).flySpeed.get();

        int amount = 0;
        try {
            if (Math.abs(next.getX() - previous.getX()) > 0) amount++;
            if (Math.abs(next.getY() - previous.getY()) > 0) amount++;
            if (Math.abs(next.getZ() - previous.getZ()) > 0) amount++;
            if (amount > 1) {
                speed = Modules.get().get(ElytraBotThreaded.class).maneuverSpeed.get();

                //If the previous and next is both diagonal then use real speed
                if (next.getX() - previous.getX() == next.getZ() - previous.getZ() && next.getY() - previous.getY() == 0) {
                    if (xDiff >= 1 && zDiff >= 1 || xDiff <= -1 && zDiff <= -1) {
                        speed = Modules.get().get(ElytraBotThreaded.class).flySpeed.get();
                    }
                }
            }
        } catch (Exception nullPointerProbablyIdk) {
            speed = Modules.get().get(ElytraBotThreaded.class).maneuverSpeed.get();
        }

        if ((int) xDiff > 0) {
            x = speed;
        } else if ((int) xDiff < 0) {
            x = -speed;
        }

        if ((int) yDiff > 0) {
            y = Modules.get().get(ElytraBotThreaded.class).maneuverSpeed.get();
        } else if ((int) yDiff < 0) {
            y = -Modules.get().get(ElytraBotThreaded.class).maneuverSpeed.get();
        }

        if ((int) zDiff > 0) {
            z = speed;
        } else if ((int) zDiff < 0) {
            z = -speed;
        }

        mc.player.setVelocity(x, y, z);

        double centerSpeed = 0.2;
        double centerCheck = 0.1;

        mc.player.setVelocity((x == 0 ? xDiff > centerCheck ? centerSpeed : xDiff < -centerCheck ? -centerSpeed : 0 : mc.player.getVelocity().x), (y == 0 ? yDiff > centerCheck ? centerSpeed : yDiff < -centerCheck ? -centerSpeed : 0 : mc.player.getVelocity().y), (z == 0 ? zDiff > centerCheck ? centerSpeed : zDiff < -centerCheck ? -centerSpeed : 0 : mc.player.getVelocity().z));
    }
}
