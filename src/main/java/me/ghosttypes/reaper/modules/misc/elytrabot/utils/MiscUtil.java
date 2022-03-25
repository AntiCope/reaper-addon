package me.ghosttypes.reaper.modules.misc.elytrabot.utils;

import me.ghosttypes.reaper.modules.misc.elytrabot.ElytraBotThreaded;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.PickaxeItem;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class MiscUtil {
    public static void toggle(Thread thread) {
        suspend(thread);
        Module m = Modules.get().get(ElytraBotThreaded.class);
        if (m.isActive()) m.toggle();
    }

    public static void suspend(Thread thread) {
        if (thread != null) thread.suspend();
    }

    public static void eat(int slot) {
        InvUtils.swap(slot, false);
        mc.options.useKey.setPressed(true);
        Utils.rightClick();
    }

    public static boolean shouldEatItem(Item item) {
        if (item == Items.ENCHANTED_GOLDEN_APPLE || item == Items.GOLDEN_APPLE && !Modules.get().get(ElytraBotThreaded.class).allowGaps.get()) return false;
        else return (item != Items.CHORUS_FRUIT && item != Items.POISONOUS_POTATO && item != Items.PUFFERFISH &&
            item != Items.CHICKEN && item != Items.ROTTEN_FLESH && item != Items.SPIDER_EYE && item != Items.SUSPICIOUS_STEW);
    }

    public static void mine(BlockPos blockPos) {
        if (!(MeteorClient.mc.player.getMainHandStack().getItem() instanceof PickaxeItem)) {
            FindItemResult result = InvUtils.findInHotbar(Items.DIAMOND_PICKAXE, Items.NETHERITE_PICKAXE, Items.IRON_PICKAXE);
            if (result.slot() != -1) {
                InvUtils.swap(result.slot(), false);
            }
        }

        Rotations.rotate(Rotations.getYaw(blockPos), Rotations.getPitch(blockPos));

        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, Direction.UP));
        mc.player.swingHand(Hand.MAIN_HAND);
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, Direction.UP));

        ElytraBotThreaded.sleepUntil(() -> !isSolid(blockPos), 15000);
    }

    public static boolean isSolid(BlockPos pos) {
        try {
            return mc.world.getBlockState(pos).getMaterial().isSolid();
        } catch (NullPointerException e) {
            return false;
        }
    }

    public static boolean isInRenderDistance(BlockPos pos) {
        int chunkX = (pos.getX() / 16);
        int chunkZ = (pos.getZ() / 16);
        return (mc.world.getChunkManager().isChunkLoaded(chunkX, chunkZ));
    }

    public static Block getBlock(BlockPos pos) {
        try {
            return mc.world.getBlockState(pos).getBlock();
        } catch (NullPointerException e) {
            return null;
        }
    }

    public static double getSpeed() {
        return new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ()).distanceTo(new Vec3d(mc.player.prevX, mc.player.prevY, mc.player.prevZ));
    }

    public static void useItem() {
        mc.options.useKey.setPressed(true);
        if (!mc.player.isUsingItem()) Utils.rightClick();
        mc.options.useKey.setPressed(false);
    }

    public static int distance(BlockPos first, BlockPos second) {
        return Math.abs(first.getX() - second.getX()) + Math.abs(first.getY() - second.getY()) + Math.abs(first.getZ() - second.getZ());
    }
}
