package me.ghosttypes.reaper.util.world;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class IBlock {
    public static Block getBlock(BlockPos blockPos) {
        return getState(blockPos).getBlock();
    }

    public static BlockState getState(BlockPos blockPos) {
        return mc.world.getBlockState(blockPos);
    }

    public static boolean isOf(BlockPos blockPos, Block block) {
        return getState(blockPos).isOf(block);
    }

    public static boolean isOf(BlockPos blockPos, instance block) {
        return switch (block) {
            case BedBlock -> getBlock(blockPos) instanceof BedBlock;
        };
    }

    public static double getResistance(BlockPos blockPos) {
        return getBlock(blockPos).getBlastResistance();
    }

    public static boolean isReplaceable(BlockPos blockPos) {
        return getState(blockPos).getMaterial().isReplaceable();
    }

    public static double getHardness(BlockPos blockPos) {
        return getState(blockPos).getHardness(mc.world, blockPos);
    }

    public static void renderBox(Render3DEvent event, BlockPos blockPos, Color sideColor, Color lineColor, ShapeMode shapeMode) {
        if (blockPos == null) return;
        if (IBlock.getState(blockPos).getOutlineShape(mc.world, blockPos).isEmpty()) return;

        render(event, blockPos, BlockHelper.getState(blockPos).getOutlineShape(mc.world, blockPos).getBoundingBox(), sideColor, lineColor, shapeMode);
    }

    private static void render(Render3DEvent event, BlockPos bp, Box box, Color sideColor, Color lineColor, ShapeMode shapeMode) {
        event.renderer.box(bp.getX() + box.minX, bp.getY() + box.minY, bp.getZ() + box.minZ, bp.getX() + box.maxX, bp.getY() + box.maxY, bp.getZ() + box.maxZ, sideColor, lineColor, shapeMode, 0);
    }

    public enum instance {
        BedBlock
    }
}
