package me.ghosttypes.reaper.util.world;

import meteordevelopment.meteorclient.utils.player.FindItemResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class BlockBuilder {

    public enum SurroundDesign {
        Single,
        Double,
        Extra,
        Russian
    }

    public static boolean isVecListComplete(ArrayList<Vec3d> vlist) {
        if (vlist == null || vlist.isEmpty()) return false;
        BlockPos ppos = mc.player.getBlockPos();
        for (Vec3d b: vlist) if (BlockHelper.canPlace(ppos.add(b.x, b.y, b.z))) return false;
        return true;
    }

    public static boolean isBlockListComplete(ArrayList<BlockPos> blist) {
        if (blist == null || blist.isEmpty()) return false;
        for (BlockPos b : blist) if (BlockHelper.canPlace(b)) return false;
        return true;
    }

    public static ArrayList<BlockPos> vecToPos(BlockPos center, ArrayList<Vec3d> vlist) {
        ArrayList<BlockPos> blocks = new ArrayList<>();
        for (Vec3d v : vlist) {
            BlockPos p = center.add(v.x, v.y, v.z);
            blocks.add(p);
        }
        return blocks;
    }


    public static int build(ArrayList<BlockPos> blocks, FindItemResult item, boolean rotate, int max, boolean strict, boolean packet) {
        int bp = 0;
        for (BlockPos p : blocks) {
            BlockHelper.place(p, item, rotate, packet);
            if (strict && BlockHelper.isSolid(p)) bp++;
            else if (!strict) bp++;
            if (bp > max) break;
        }
        return bp;
    }


    public static ArrayList<BlockPos> addLegacyPositions(List<BlockPos> blocks) {
        ArrayList<BlockPos> posi = new ArrayList<>();
        blocks.forEach(blockPos -> {
            BlockPos down = blockPos.down();
            if (!BlockHelper.isSolid(down)) posi.add(down);
        });
        return posi;
    }



    public static ArrayList<BlockPos> getSurroundDesign(SurroundDesign design, boolean legacy) {
        ArrayList<BlockPos> posi = new ArrayList<>();
        BlockPos pp = mc.player.getBlockPos();
        switch (design) {
            case Single -> {
                posi.add(pp.north());
                posi.add(pp.east());
                posi.add(pp.south());
                posi.add(pp.west());
            }
            case Double -> {
                posi.add(pp.north());
                posi.add(pp.east());
                posi.add(pp.south());
                posi.add(pp.west());
                for (BlockPos p : posi) posi.add(p.up());
            }
            case Extra -> {
                posi.add(pp.north(2));
                posi.add(pp.south(2));
                posi.add(pp.east(2));
                posi.add(pp.west(2));
            }
            case Russian -> {
                posi.add(pp.north().east());
                posi.add(pp.east().south());
                posi.add(pp.south().west());
                posi.add(pp.west().north());
            }
        }
        if (legacy) for (BlockPos p : posi) posi.add(p.down());
        return posi;
    }
}
