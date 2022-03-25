package me.ghosttypes.reaper.util.world;

import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.GoalBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;

public class BaritoneManager {

    // Path Control

    public static void pathToBlockPos(BlockPos pos) {
        BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalBlock(pos));
    }

    public static void followPlayer(PlayerEntity player) {
        BaritoneAPI.getProvider().getPrimaryBaritone().getMineProcess().cancel();
        BaritoneAPI.getProvider().getPrimaryBaritone().getFollowProcess().follow(entity -> entity.getEntityName().equalsIgnoreCase(player.getEntityName()));
    }

    public static void forceStopPathing() {
        BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().forceCancel();
    }

    public static void stopPathing() {
        BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
    }

    public static void stopFollowing() {
        BaritoneAPI.getProvider().getPrimaryBaritone().getFollowProcess().cancel();
    }


    // Bools

    public static boolean hasGoal() {
        return BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().getGoal() != null;
    }

    public static boolean isAtGoal(BlockPos pos) {
        return BlockHelper.distanceTo(pos) <= 0.5;
    }

    public static boolean hasPath() {
        return BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().hasPath();
    }

    public static boolean isFollowing() {
        return BaritoneAPI.getProvider().getPrimaryBaritone().getFollowProcess().isActive();
    }
}
