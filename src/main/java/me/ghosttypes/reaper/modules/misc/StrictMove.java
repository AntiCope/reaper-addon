package me.ghosttypes.reaper.modules.misc;

import me.ghosttypes.reaper.modules.ML;
import me.ghosttypes.reaper.util.misc.MathUtil;
import me.ghosttypes.reaper.util.misc.ModuleHelper;
import me.ghosttypes.reaper.util.misc.ReaperModule;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.BlockPos;

public class StrictMove extends ReaperModule {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> alert = sgGeneral.add(new BoolSetting.Builder().name("alert").description("Alerts you when lag-back is detected.").defaultValue(false).build());
    private final Setting<Boolean> toggleCombat = sgGeneral.add(new BoolSetting.Builder().name("toggle-combat").description("Disable combat modules when lag-back is detected.").defaultValue(false).build());
    private final Setting<Boolean> toggleMovement = sgGeneral.add(new BoolSetting.Builder().name("toggle-movement").description("Disable movement modules when lag-back is detected.").defaultValue(false).build());

    private long lastPearl, lastChorus;

    public StrictMove() {
        super(ML.M, "strict-move", "Mitigate lag-back from modules and/or strict anti-cheat");
    }

    @Override
    public void onActivate() {
        lastPearl = MathUtil.now() - 5000;
        lastChorus = MathUtil.now() - 5000;
        PacketFly pfly = Modules.get().get(PacketFly.class);
        if (pfly.isActive()) {
            error("Cannot use StrictMove and PacketFly at the same time!");
            toggle();
        }
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (event.packet instanceof PlayerPositionLookS2CPacket lookPacket) {
            //BlockPos pos = new BlockPos(lookPacket.getX(), lookPacket.getY(), lookPacket.getZ());
            if (MathUtil.msPassed(lastPearl) < 500 || MathUtil.msPassed(lastChorus) < 150) return;
            if (alert.get()) warning("Lag-back detected!");
            if (toggleCombat.get()) ModuleHelper.disableCombat();
            if (toggleMovement.get()) ModuleHelper.disableMovement();
        }
    }

    @EventHandler
    private void onPacketSend(PacketEvent.Send event) {
        if (event.packet instanceof PlayerInteractItemC2SPacket packet) {
            if (mc.player.getStackInHand(packet.getHand()).getItem().equals(Items.ENDER_PEARL)) {
                lastPearl = MathUtil.now();
            } else if (mc.player.getStackInHand(packet.getHand()).getItem().equals(Items.CHORUS_FRUIT))  {
                lastChorus = MathUtil.now();
            }
        }
    }
}
