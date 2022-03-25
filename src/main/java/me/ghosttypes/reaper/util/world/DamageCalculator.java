package me.ghosttypes.reaper.util.world;

import me.ghosttypes.reaper.util.world.BlockHelper.BlockPosExtended;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.player.DamageUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.meteorclient.utils.world.CardinalDirection;
import net.minecraft.block.entity.BedBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.IntStream;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class DamageCalculator {


    public enum BlockSortMode {CloseToSelf, CloseToTarget}

    public static PlayerEntity cachedTarget = null;

    public static class AnchorPlacement {
        private BlockPos pos;
        private Vec3d vec;
        private BlockHitResult hitResult;

        public AnchorPlacement() {}
        public AnchorPlacement(BlockPos anchorPos) {
            this.set(anchorPos);
        }

        public void set(BlockPos anchorPos) {
            Vec3d v = BlockHelper.vec3d(anchorPos);
            this.pos = anchorPos;
            this.vec = v;
            this.hitResult = new BlockHitResult(BlockHelper.bestHitPos(anchorPos), Direction.UP, anchorPos, true);
        }

        public double getSelfDMG() {
            return DamageUtils.bedDamage(mc.player, this.vec);
        }
        public double getTargetDMG(PlayerEntity target) {
            return DamageUtils.bedDamage(target, this.vec);
        }
        public BlockPos getPos() {return this.pos;}
        public BlockHitResult getHitResult() {return this.hitResult;}
    }

    public static class BedPlacement {
        private BlockPos pos, offset;
        private Vec3d vec;
        private double damage;
        private BlockHitResult hitResult;
        private CardinalDirection rotationOffset;


        public BedPlacement() {}
        public BedPlacement(BlockPos bed, CardinalDirection dir, double dmg) {this.set(bed, dir, dmg);}

        public void set(BlockPos center, CardinalDirection dirO, double dmg) {
            this.pos = center;
            this.offset = center.offset(dirO.toDirection());
            this.vec = BlockHelper.vec3d(center);
            this.damage = dmg;
            this.rotationOffset = dirO;
            this.hitResult = new BlockHitResult(BlockHelper.bestHitPos(center), Direction.UP, center, true);
        }

        public void recalculate(PlayerEntity target) { // recalculate damage to target without having to recalculate other data
            if (target == null) return;
            this.damage = DamageUtils.bedDamage(target, this.vec);
            this.hitResult = new BlockHitResult(BlockHelper.bestHitPos(this.pos), Direction.UP, this.pos, true);
        }

        public boolean isStillValid(PlayerEntity target, double minDmg, double maxDmg) {
            this.recalculate(target);
            return this.damage >= minDmg && DamageUtils.bedDamage(mc.player, this.vec) < maxDmg;
        }

        public BlockPos getPos() {return this.pos;}
        public BlockPos getOffset() {return this.offset;}
        public Vec3d getVec() {return this.vec;}
        public double getDamage() {return this.damage;}
        public CardinalDirection getRotationOffset() {return this.rotationOffset;}
        public BlockHitResult getHitResult() {return this.hitResult;}
    }


    public static BlockPos getBreakPos(PlayerEntity target, double range, double maxDmg, boolean antiSuicide) {
        if (target == null) return null;
        double currentHP = PlayerUtils.getTotalHealth();
        for (BlockEntity be : Utils.blockEntities()) {
            if (be instanceof BedBlockEntity bed) {
                BlockPosExtended bedPos = new BlockPosExtended(bed.getPos());
                if (bedPos.getDistance() > range) continue;
                Vec3d vec = bedPos.asVec3d();
                double selfDMG = DamageUtils.bedDamage(mc.player, vec);
                if (selfDMG > maxDmg) continue;
                if (antiSuicide && (currentHP - selfDMG) <= 0) continue;
                return bedPos.getPos();
            }
        }
        return null;
    }


    public static BedPlacement getPlacePos(PlayerEntity target, int xRadius, int yRadius, double range, double minDmg, double maxDmg, boolean antiSuicide, boolean groundOnly, BlockSortMode sortMode) {
        if (target == null) return null;
        BedPlacement placement = new BedPlacement();

        BlockPos.Mutable bedPos = new BlockPos.Mutable();
        BlockPos tPos = target.getBlockPos();
        CardinalDirection bedDir = CardinalDirection.North;
        double bestDamage = 0;
        double currentHP = PlayerUtils.getTotalHealth();
        //double targetHP = EntityUtils.getTotalHealth(target);

        ArrayList<BlockPos> toCheck = new ArrayList<>();
        if (CombatHelper.isInHole(target)) {
            IntStream.rangeClosed(1, 4).forEach(i -> {
                BlockPos b = tPos.up(i);
                for (CardinalDirection cd : CardinalDirection.values()) toCheck.add(b.offset(cd.toDirection()));
            });
        } else {
            toCheck.addAll(getSphere(target, xRadius, yRadius, sortMode));
        }

        for (BlockPos p : toCheck) {
            if (p.equals(tPos) || groundOnly && !BlockHelper.isSolid(p.down())) continue;
            BlockPosExtended pos = new BlockPosExtended(p);
            if (!pos.canPlace() || pos.getDistance() > range) continue;
            for (CardinalDirection d : CardinalDirection.values()) {
                BlockPosExtended offset = new BlockPosExtended(p.offset(d.toDirection()));
                if (!offset.canPlace() || groundOnly && !BlockHelper.isSolid(offset.getPos().down())) continue;
                Vec3d bed = offset.asVec3d();
                double targetDMG = DamageUtils.bedDamage(target, bed);
                if (targetDMG < minDmg || targetDMG < bestDamage) continue;
                double selfDMG = DamageUtils.bedDamage(mc.player, bed);
                double postHP = currentHP - selfDMG;
                if (antiSuicide && postHP <= 0 ||  selfDMG > maxDmg) continue;
                bestDamage = targetDMG;
                bedDir = d;
                bedPos.set(pos.getPos());
            }
        }
        placement.set(bedPos, bedDir, bestDamage);
        return placement;

    }


    public static ArrayList<BlockPos> getSphere(PlayerEntity target, int xRadius, int yRadius, BlockSortMode sortMode) {
        if (target == null) return null;
        cachedTarget = target;

        ArrayList<BlockPos> ar = new ArrayList<>();
        BlockPos tPos = target.getBlockPos();
        BlockPos.Mutable p = new BlockPos.Mutable();

        for (int x = -xRadius; x <= xRadius; x++) {
            for (int y = -yRadius; y <= yRadius; y++) {
                for (int z = -xRadius; z <= xRadius; z++) {
                    p.set(tPos).move(x, y, z);
                    BlockPos p2 = p.toImmutable();
                    if (BlockHelper.isSolid(p2)) continue; // skip solid blocks
                    if (MathHelper.sqrt((float) ((tPos.getX() - p2.getX()) * (tPos.getX() - p2.getX()) + (tPos.getZ() - p2.getZ()) * (tPos.getZ() - p2.getZ()))) <= xRadius && MathHelper.sqrt((float) ((tPos.getY() - p2.getY()) * (tPos.getY() - p2.getY()))) <= yRadius)
                    {if (!ar.contains(p2)) ar.add(p2);}
                }
            }
        }

        switch (sortMode) {
            case CloseToSelf -> ar.sort(Comparator.comparingDouble(PlayerUtils::distanceTo));
            case CloseToTarget -> ar.sort(Comparator.comparingDouble(DamageCalculator::distanceTo));
        }

        return ar;
    }

    public static double distanceTo(Entity entity) {
        return distanceTo(entity.getX(), entity.getY(), entity.getZ());
    }
    public static double distanceTo(BlockPos blockPos) {return distanceTo(blockPos.getX(), blockPos.getY(), blockPos.getZ());}
    public static double distanceTo(Vec3d vec3d) {
        return distanceTo(vec3d.getX(), vec3d.getY(), vec3d.getZ());
    }

    public static double distanceTo(double x, double y, double z) {
        if (cachedTarget == null) return 100;
        float f = (float) (cachedTarget.getX() - x);
        float g = (float) (cachedTarget.getY() - y);
        float h = (float) (cachedTarget.getZ() - z);
        return MathHelper.sqrt(f * f + g * g + h * h);
    }


}
