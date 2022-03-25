package me.ghosttypes.reaper.util.render;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.util.math.BlockPos;

public class RenderHelper {
    public static void block(Render3DEvent event, BlockPos pos, ShapeMode mode, Color lineColor, Color lineColor2, Color sideColor, Color sideColor2, double lineSize) {
        double low = lineSize;
        double high = 1 - low;

        if (mode == ShapeMode.Lines || mode == ShapeMode.Both) {
            // Sides
            event.renderer.gradientQuadVertical(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY() + 1, pos.getZ() + low, lineColor, lineColor2);
            event.renderer.gradientQuadVertical(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + low, pos.getY() + 1, pos.getZ(), lineColor, lineColor2);
            event.renderer.gradientQuadVertical(pos.getX() + 1, pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + low, lineColor, lineColor2);
            event.renderer.gradientQuadVertical(pos.getX() + 1, pos.getY(), pos.getZ(), pos.getX() + high, pos.getY() + 1, pos.getZ(), lineColor, lineColor2);
            event.renderer.gradientQuadVertical(pos.getX(), pos.getY(), pos.getZ() + 1, pos.getX(), pos.getY() + 1, pos.getZ() + high, lineColor, lineColor2);
            event.renderer.gradientQuadVertical(pos.getX(), pos.getY(), pos.getZ() + 1, pos.getX() + low, pos.getY() + 1, pos.getZ() + 1, lineColor, lineColor2);
            event.renderer.gradientQuadVertical(pos.getX() + 1, pos.getY(), pos.getZ() + 1, pos.getX() + 1, pos.getY() + 1, pos.getZ() + high, lineColor, lineColor2);
            event.renderer.gradientQuadVertical(pos.getX() + 1, pos.getY(), pos.getZ() + 1, pos.getX() + high, pos.getY() + 1, pos.getZ() + 1, lineColor, lineColor2);

            // Up
            event.renderer.gradientQuadVertical(pos.getX(), pos.getY() + 1, pos.getZ(), pos.getX() + 1, pos.getY() + high, pos.getZ(), lineColor, lineColor);
            event.renderer.quadHorizontal(pos.getX(), pos.getY() + 1, pos.getZ(), pos.getX() + 1, pos.getZ() + low, lineColor);
            event.renderer.gradientQuadVertical(pos.getX(), pos.getY() + 1, pos.getZ(), pos.getX(), pos.getY() + high, pos.getZ() + 1, lineColor, lineColor);
            event.renderer.quadHorizontal(pos.getX(), pos.getY() + 1, pos.getZ(), pos.getX() + low, pos.getZ() + 1, lineColor);
            event.renderer.gradientQuadVertical(pos.getX(), pos.getY() + 1, pos.getZ() + 1, pos.getX() + 1, pos.getY() + high, pos.getZ() + 1, lineColor, lineColor);
            event.renderer.quadHorizontal(pos.getX(), pos.getY() + 1, pos.getZ() + 1, pos.getX() + 1, pos.getZ() + high, lineColor);
            event.renderer.gradientQuadVertical(pos.getX() + 1, pos.getY() + 1, pos.getZ(), pos.getX() + 1, pos.getY() + high, pos.getZ() + 1, lineColor, lineColor);
            event.renderer.quadHorizontal(pos.getX() + 1, pos.getY() + 1, pos.getZ(), pos.getX() + high, pos.getZ() + 1, lineColor);

            // Down
            event.renderer.gradientQuadVertical(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + low, pos.getZ(), lineColor2, lineColor2);
            event.renderer.quadHorizontal(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getZ() + low, lineColor2);
            event.renderer.gradientQuadVertical(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY() + low, pos.getZ() + 1, lineColor2, lineColor2);
            event.renderer.quadHorizontal(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + low, pos.getZ() + 1, lineColor2);
            event.renderer.gradientQuadVertical(pos.getX(), pos.getY(), pos.getZ() + 1, pos.getX() + 1, pos.getY() + low, pos.getZ() + 1, lineColor2, lineColor2);
            event.renderer.quadHorizontal(pos.getX(), pos.getY(), pos.getZ() + 1, pos.getX() + 1, pos.getZ() + high, lineColor2);
            event.renderer.gradientQuadVertical(pos.getX() + 1, pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + low, pos.getZ() + 1, lineColor2, lineColor2);
            event.renderer.quadHorizontal(pos.getX() + 1, pos.getY(), pos.getZ(), pos.getX() + high, pos.getZ() + 1, lineColor2);
        }

        if (mode == ShapeMode.Sides || mode == ShapeMode.Both) {
            event.renderer.gradientQuadVertical(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ(), sideColor, sideColor2);
            event.renderer.gradientQuadVertical(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY() + 1, pos.getZ() + 1, sideColor, sideColor2);
            event.renderer.gradientQuadVertical(pos.getX() + 1, pos.getY(), pos.getZ() + 1, pos.getX() + 1, pos.getY() + 1, pos.getZ(), sideColor, sideColor2);
            event.renderer.gradientQuadVertical(pos.getX() + 1, pos.getY(), pos.getZ() + 1, pos.getX(), pos.getY() + 1, pos.getZ() + 1, sideColor, sideColor2);
            event.renderer.quadHorizontal(pos.getX(), pos.getY() + 1, pos.getZ(), pos.getX() + 1, pos.getZ() + 1, sideColor);
            event.renderer.quadHorizontal(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getZ() + 1, sideColor2);
        }
    }

    public static void sideUp(Render3DEvent event, BlockPos pos, ShapeMode mode, Color lineColor, Color sideColor, double lineSize) {
        double low = lineSize;
        double high = 1 - low;

        if (mode == ShapeMode.Lines || mode == ShapeMode.Both) {
            event.renderer.gradientQuadVertical(pos.getX(), pos.getY() + 1, pos.getZ(), pos.getX() + 1, pos.getY() + high, pos.getZ(), lineColor, lineColor);
            event.renderer.quadHorizontal(pos.getX(), pos.getY() + 1, pos.getZ(), pos.getX() + 1, pos.getZ() + low, lineColor);
            event.renderer.gradientQuadVertical(pos.getX(), pos.getY() + 1, pos.getZ(), pos.getX(), pos.getY() + high, pos.getZ() + 1, lineColor, lineColor);
            event.renderer.quadHorizontal(pos.getX(), pos.getY() + 1, pos.getZ(), pos.getX() + low, pos.getZ() + 1, lineColor);
            event.renderer.gradientQuadVertical(pos.getX(), pos.getY() + 1, pos.getZ() + 1, pos.getX() + 1, pos.getY() + high, pos.getZ() + 1, lineColor, lineColor);
            event.renderer.quadHorizontal(pos.getX(), pos.getY() + 1, pos.getZ() + 1, pos.getX() + 1, pos.getZ() + high, lineColor);
            event.renderer.gradientQuadVertical(pos.getX() + 1, pos.getY() + 1, pos.getZ(), pos.getX() + 1, pos.getY() + high, pos.getZ() + 1, lineColor, lineColor);
            event.renderer.quadHorizontal(pos.getX() + 1, pos.getY() + 1, pos.getZ(), pos.getX() + high, pos.getZ() + 1, lineColor);
        }

        if (mode == ShapeMode.Sides || mode == ShapeMode.Both) {
            event.renderer.quadHorizontal(pos.getX(), pos.getY() + 1, pos.getZ(), pos.getX() + 1, pos.getZ() + 1, sideColor);
        }
    }

}
