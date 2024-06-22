package com.jelly.MightyMinerV2.Handler;

import baritone.api.BaritoneAPI;
import baritone.api.event.events.PathEvent;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.pathing.goals.GoalNear;
import baritone.api.process.PathingCommand;
import baritone.api.process.PathingCommandType;
import com.jelly.MightyMinerV2.Config.MightyMinerConfig;
import com.jelly.MightyMinerV2.Event.BaritoneEventListener;
import com.jelly.MightyMinerV2.Util.LogUtil;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BaritoneHandler {
    private static final Minecraft mc = Minecraft.getMinecraft();
    @Getter
    public static boolean pathing = false;

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    static {
        BaritoneAPI.getSettings().chatDebug.value = true; // Enable chat debug
        BaritoneAPI.getSettings().allowDiagonalAscend.value = true;
        BaritoneAPI.getSettings().pathingMapDefaultSize.value = 2048;
        BaritoneAPI.getSettings().maxFallHeightNoWater.value = 50;
        List<Block> blacklist = Arrays.asList(Blocks.oak_fence, Blocks.oak_fence_gate);
        BaritoneAPI.getSettings().blocksToAvoid.value = blacklist;
        BaritoneAPI.getSettings().costHeuristic.value = 50.0;
        BaritoneAPI.getSettings().yawSmoothingFactor.value = (float) MightyMinerConfig.yawsmoothingfactor;
        BaritoneAPI.getSettings().pitchSmoothingFactor.value = (float) MightyMinerConfig.pitchsmoothingfactor;
    }

    public static boolean isWalkingToGoalBlock() {
        return isWalkingToGoalBlock(0.75);
    }

    public static boolean isWalkingToGoalBlock(double nearGoalDistance) {
        if (pathing) {
            if (!mc.thePlayer.onGround) return true;
            double distance;
            Goal goal = BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().getGoal();
            if (goal instanceof GoalBlock) {
                GoalBlock goal1 = (GoalBlock) BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().getGoal();
                distance = mc.thePlayer.getDistance(goal1.getGoalPos().getX() + 0.5f, mc.thePlayer.posY, goal1.getGoalPos().getZ() + 0.5);
            } else if (goal instanceof GoalNear) {
                GoalNear goal1 = (GoalNear) BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().getGoal();
                distance = mc.thePlayer.getDistance(goal1.getGoalPos().getX() + 0.5f, mc.thePlayer.posY, goal1.getGoalPos().getZ() + 0.5);
            } else {
                distance = goal.isInGoal(mc.thePlayer.getPosition()) ? 0 : goal.heuristic();
            }

            if (distance <= nearGoalDistance || BaritoneEventListener.pathEvent == PathEvent.AT_GOAL) {
                BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
                pathing = false;
                return false;
            }
            return BaritoneEventListener.pathEvent != PathEvent.CANCELED;
        }
        return false;
    }

    public static boolean hasFailed() {
        if (BaritoneEventListener.pathEvent == PathEvent.CALC_FAILED
                || BaritoneEventListener.pathEvent == PathEvent.NEXT_CALC_FAILED) {
            BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
            pathing = false;
            return true;
        }
        return false;
    }

    public static void walkToBlockPos(BlockPos blockPos) {
        PathingCommand pathingCommand = new PathingCommand(new GoalBlock(blockPos), PathingCommandType.REVALIDATE_GOAL_AND_PATH);
        BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().secretInternalSetGoalAndPath(pathingCommand);
        pathing = true;
    }

    public static void walkCloserToBlockPos(BlockPos blockPos, int range) {
        PathingCommand pathingCommand = new PathingCommand(new GoalNear(blockPos, range), PathingCommandType.REVALIDATE_GOAL_AND_PATH);
        BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().secretInternalSetGoalAndPath(pathingCommand);
        pathing = true;
    }

    public static void walkThroughWaypoints(List<BlockPos> waypoints) {
        // Print the list of waypoints in the chat
        StringBuilder waypointsString = new StringBuilder("Waypoints: ");
        for (BlockPos waypoint : waypoints) {
            waypointsString.append(waypoint.toString()).append(" -> ");
        }
        waypointsString.setLength(waypointsString.length() - 4); // Remove the last " -> "
        LogUtil.send(waypointsString.toString(), LogUtil.ELogType.SUCCESS);

        // Move through the waypoints
        for (int i = 0; i < waypoints.size(); i++) {
            BlockPos waypoint = waypoints.get(i);
            walkToBlockPos(waypoint);

            // Calculate the next path if available and if there are more than one waypoint left in the list
            if (i < waypoints.size() - 2) {
                BlockPos nextWaypoint = waypoints.get(i + 1);
                PathingCommand nextPathingCommand = new PathingCommand(new GoalBlock(nextWaypoint), PathingCommandType.REVALIDATE_GOAL_AND_PATH);
                BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().secretInternalSetGoalAndPath(nextPathingCommand);
            }
        }
    }



    public static void stopPathing() {
        BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
        pathing = false;
    }
}
