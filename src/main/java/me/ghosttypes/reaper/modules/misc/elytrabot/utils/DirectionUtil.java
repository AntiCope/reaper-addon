package me.ghosttypes.reaper.modules.misc.elytrabot.utils;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public enum DirectionUtil {
    XP("X-Plus"),
    XM("X-Minus"),
    ZP("Z-Plus"),
    ZM("Z-Minus"),
    XP_ZP("X-Plus, Z-Plus"),
    XM_ZP("X-Minus, Z-Plus"),
    XM_ZM("X-Minus, Z-Minus"),
    XP_ZM("X-Plus, Z-Minus");

    public String name;
    private static net.minecraft.util.math.Direction mcDir;

    DirectionUtil(String name) {
        this.name = name;
    }

    public static DirectionUtil getDirection() {
        net.minecraft.util.math.Direction direction = mc.player.getHorizontalFacing();
        return (direction == mcDir.NORTH ? ZM : direction == mcDir.WEST ? XM : direction == mcDir.SOUTH ? ZP : XP);
    }

    public static DirectionUtil getDiagonalDirection() {
        net.minecraft.util.math.Direction direction = mc.player.getHorizontalFacing();

        if (direction.equals(mcDir.NORTH)) {
            double closest = getClosest(135, -135);
            return closest == -135 ? XP_ZM : XM_ZM;
        } else if (direction.equals(mcDir.WEST)) {
            double closest = getClosest(135, 45);
            return closest == 135 ? XM_ZM : XM_ZP;
        } else if (direction.equals(mcDir.EAST)) {
            double closest = getClosest(-45, -135);
            return closest == -135 ? XP_ZM : XP_ZP;
        } else {
            double closest = getClosest(45, -45);
            return closest == 45 ? XM_ZP : XP_ZP;
        }
    }

    private static double getClosest(double a, double b) {
        double yaw = mc.player.getYaw();
        yaw = yaw < -180 ? yaw += 360 : yaw > 180 ? yaw -= 360 : yaw;

        if (Math.abs(yaw - a) < Math.abs(yaw - b)) {
            return a;
        } else {
            return b;
        }
    }
}
