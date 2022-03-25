package me.ghosttypes.reaper.util.render;

import me.ghosttypes.reaper.util.misc.MathUtil;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.world.CardinalDirection;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class Renderers {

    // Bed render with fading and damage rendering
    public static class SimpleBedRender {
        private BlockPos center;
        private CardinalDirection offset;
        private final Color sideColor1;
        private final Color lineColor1;
        private final Color damageColor;
        private int renderTicks;
        private final int fadeFactor;
        private double damage;
        private String damageText;

        public SimpleBedRender(BlockPos bed, int renderTime, CardinalDirection dir, Color sideColor, Color lineColor, Color damageC, int fade, double dmg) {
            center = bed;
            offset = dir;
            sideColor1 = new Color(sideColor.r, sideColor.g, sideColor.b, sideColor.a);
            lineColor1 = new Color(lineColor.r, lineColor.g, lineColor.b, lineColor.a);
            damageColor = new Color(damageC.r, damageC.g, damageC.b, damageC.a);
            fadeFactor = fade;
            renderTicks = MathUtil.intToTicks(renderTime);
            damage = dmg;
            damageText = String.valueOf(Math.round(damage * 100.0) / 100.0);
        }

        public void tick() {
            renderTicks--;
            sideColor1.a -= fadeFactor;
            lineColor1.a -= fadeFactor;
            damageColor.a -= fadeFactor;
        }

        public boolean shouldRemove() {
            return renderTicks < 0;
        }
        public double getDamage() {return damage;}
        public String getDamageTxt() {return damageText;}
        public BlockPos getPos() {
            return center;
        }
        public CardinalDirection getDir() {return offset;}
        public Color getSideColor() {
            return sideColor1;
        }
        public Color getLineColor() {
            return lineColor1;
        }
        public Color getDamageColor(){return damageColor;}
    }

    // Anchor renderer with fading and damage rendering
    public static class SimpleAnchorRender {
        private final BlockPos pos;
        private final Color sideColor1;
        private final Color lineColor1;
        private final Color damageColor;
        private int renderTicks;
        private final int fadeFactor;
        private double damage;
        private String damageText;

        public SimpleAnchorRender(BlockPos p, int renderTime, Color sideColor, Color lineColor, Color damageC, int fade, double dmg) {
            pos = p;
            sideColor1 = new Color(sideColor.r, sideColor.g, sideColor.b, sideColor.a);
            lineColor1 = new Color(lineColor.r, lineColor.g, lineColor.b, lineColor.a);
            damageColor = new Color(damageC.r, damageC.g, damageC.b, damageC.a);
            fadeFactor = fade;
            renderTicks = MathUtil.intToTicks(renderTime);
            damage = dmg;
            damageText = String.valueOf(Math.round(damage * 100.0) / 100.0);
        }

        public void tick() {
            renderTicks--;
            sideColor1.a -= fadeFactor;
            lineColor1.a -= fadeFactor;
            damageColor.a -= fadeFactor;
        }


        public boolean shouldRemove() {
            return renderTicks < 0;
        }
        public double getDamage() {return damage;}
        public String getDamageTxt() {return damageText;}
        public BlockPos getPos() {
            return pos;
        }
        public Color getSideColor() {
            return sideColor1;
        }
        public Color getLineColor() {
            return lineColor1;
        }
        public Color getDamageColor(){return damageColor;}
    }


    // Standard block rendering (with fading in/out)
    public static class SimpleBlockRender {
        private final BlockPos pos;
        private final Color sideColor1;
        private final Color lineColor1;
        private int renderTicks;
        private final int fadeFactor;

        public SimpleBlockRender(BlockPos p, int renderTime, Color sideColor, Color lineColor, int fade) {
            pos = p;
            sideColor1 = new Color(sideColor.r, sideColor.g, sideColor.b, sideColor.a);
            lineColor1 = new Color(lineColor.r, lineColor.g, lineColor.b, lineColor.a);
            fadeFactor = fade;
            renderTicks = MathUtil.intToTicks(renderTime);
        }

        public void tick() {
            renderTicks--;
            sideColor1.a -= fadeFactor;
            lineColor1.a -= fadeFactor;
        }


        public boolean shouldRemove() {
            return renderTicks < 0;
        }

        public BlockPos getPos() {
            return pos;
        }

        public Color getSideColor() {
            return sideColor1;
        }

        public Color getLineColor() {
            return lineColor1;
        }
    }

    public static class SimpleBlockFadeIn { // basically the same as SimpleBlockRender, but modified for a few modules to do fading-in block rendering
        private final BlockPos pos;
        private final Color sideColor1;
        private final Color lineColor1;
        private final Color ogSide;
        private final Color ogLine;
        private final int renderTicks;
        private final int fadeFactor;

        public SimpleBlockFadeIn(BlockPos p, int renderTime, Color sideColor, Color lineColor, int fade) {
            pos = p;
            ogSide = sideColor; // store 'original' color
            ogLine = lineColor;
            sideColor1 = new Color(sideColor.r, sideColor.g, sideColor.b, -sideColor.a); // reverse alpha for fading in
            lineColor1 = new Color(lineColor.r, lineColor.g, lineColor.b, -lineColor.a);
            fadeFactor = fade;
            renderTicks = MathUtil.intToTicks(renderTime);
        }

        public void tick() {
            if (ogLine.a > lineColor1.a) lineColor1.a += fadeFactor; // increase the alpha until its = to the 'original' color
            if (ogSide.a > sideColor1.a) sideColor1.a += fadeFactor;
        }

        public BlockPos getPos() { return pos; }

        public Color getSideColor() { return sideColor1; }
        public Color getLineColor() { return lineColor1; }
    }
}
