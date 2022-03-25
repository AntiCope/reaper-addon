package me.ghosttypes.reaper.modules.misc.elytrabot.utils;

import me.ghosttypes.reaper.modules.misc.elytrabot.events.CancellablePlayerMoveEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.Vec3d;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class PacketFly {
    private static final TimerUtil antiKickTimer = new TimerUtil();
    private static double startY;
    public static boolean toggled;

    public static void toggle(boolean on) {
        toggled = on;
        if (on) startY = mc.player.getY();
    }

    @EventHandler
    private void onMove(CancellablePlayerMoveEvent event) {
        if (!toggled) return;

        mc.player.setVelocity(0, 0, 0);
        event.cancel();

        float speedY = 0;
        if (mc.player.getY() < startY) {
            if (!antiKickTimer.hasPassed(3000)) {
                speedY = mc.player.age % 20 == 0 ? -0.1f : 0.031f;
            } else {
                antiKickTimer.reset();
                speedY = -0.1f;
            }
        } else if (mc.player.age % 4 == 0) {
            speedY = -0.1f;
        }

        Vec3d vel = mc.player.getVelocity();
        mc.player.setVelocity(vel.x, speedY, vel.z);
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(mc.player.getX() + vel.x, mc.player.getY() + vel.y, mc.player.getZ() + vel.z, mc.player.getYaw(), mc.player.getPitch(), mc.player.isOnGround()));

        double y = mc.player.getY() + vel.y;
        y += 1337;
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(mc.player.getX() + vel.x, y, mc.player.getZ() + vel.z, mc.player.getYaw(), mc.player.getPitch(), mc.player.isOnGround()));
    }

    @EventHandler
    private void onPacketRecieve(PacketEvent.Receive event) {
        if (!toggled) return;
        if (event.packet instanceof PlayerPositionLookS2CPacket packet && mc.currentScreen == null) {
            mc.getNetworkHandler().sendPacket(new TeleportConfirmC2SPacket(packet.getTeleportId()));
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(packet.getX(), packet.getY(), packet.getZ(), packet.getYaw(), packet.getPitch(), false));
            mc.player.setPosition(packet.getX(), packet.getY(), packet.getZ());
            event.cancel();
        }
    }
}
