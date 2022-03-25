package me.ghosttypes.reaper.modules.misc;


import me.ghosttypes.reaper.modules.ML;
import me.ghosttypes.reaper.util.misc.ReaperModule;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.Vec3d;

public class ChorusPredict extends ReaperModule {
    private final SettingGroup sgGeneral = settings.createGroup("Render");

    public final Setting<Boolean> confirm = sgGeneral.add(new BoolSetting.Builder().name("debug").defaultValue(false).build());
    private final Setting<ShapeMode> shapeMode = sgGeneral.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").defaultValue(ShapeMode.Both).build());
    private final Setting<SettingColor> sideColor = sgGeneral.add(new ColorSetting.Builder().name("side-color").defaultValue(new SettingColor(0, 205, 255, 15)).build());
    private final Setting<SettingColor> lineColor = sgGeneral.add(new ColorSetting.Builder().name("line-color").defaultValue(new SettingColor(0, 205, 255,255)).build());

    private int tel;
    private Vec3d vec;

    public ChorusPredict() {
        super(ML.M, "chorus-predict", "Modifies usage of chorus fruit.");
    }

    @Override
    public void onActivate() {
        tel = -1;
        vec = null;
    }

    @Override
    public void onDeactivate() {
        tel = -1;
        vec = null;
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (!(event.packet instanceof PlayerPositionLookS2CPacket packet)) return;

        if (mc.player.getMainHandStack().getItem() == Items.CHORUS_FRUIT) {
            tel = packet.getTeleportId();
            vec = new Vec3d(packet.getX(), packet.getY(), packet.getZ());
            event.cancel();
        }
    }

    @EventHandler
    private void onSentPacket(PacketEvent.Sent event) {
        if (!(event.packet instanceof TeleportConfirmC2SPacket packet)) return;

        if (packet.getTeleportId() == tel) {
            tel = -1;
            event.cancel();
        } else vec = null;
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (vec != null) {
            event.renderer.box(vec.x - 0.25, vec.y, vec.z - 0.25, vec.x + 0.25, vec.y + 0.50, vec.z + 0.25, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            event.renderer.line(RenderUtils.center.x, RenderUtils.center.y, RenderUtils.center.z, vec.getX(), vec.getY() + 0.25, vec.getZ(), lineColor.get());
        }
    }
}
