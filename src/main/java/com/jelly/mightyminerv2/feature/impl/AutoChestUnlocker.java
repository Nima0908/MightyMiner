package com.jelly.mightyminerv2.feature.impl;

import com.jelly.mightyminerv2.event.BlockChangeEvent;
import com.jelly.mightyminerv2.event.SpawnParticleEvent;
import com.jelly.mightyminerv2.feature.AbstractFeature;
import com.jelly.mightyminerv2.handler.RotationHandler;
import com.jelly.mightyminerv2.util.BlockUtil;
import com.jelly.mightyminerv2.util.InventoryUtil;
import com.jelly.mightyminerv2.util.KeyBindUtil;
import com.jelly.mightyminerv2.util.PlayerUtil;
import com.jelly.mightyminerv2.util.RenderUtil;
import com.jelly.mightyminerv2.util.helper.RotationConfiguration;
import com.jelly.mightyminerv2.util.helper.Target;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Stack;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockChest;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;

public class AutoChestUnlocker extends AbstractFeature {

  private static AutoChestUnlocker instance = new AutoChestUnlocker();

  public static AutoChestUnlocker getInstance() {
    return instance;
  }

  public static Stack<BlockPos> chestQueue = new Stack<>();
  private State state = State.STARTING;
  private Vec3 particlePos = null;
  private boolean particleSpawned = true;
  private boolean chestSolved = false;
  private BlockPos chestSolving = null;
  private List<BlockPos> walkableBlocks = new ArrayList<>();
  private boolean clickChest = false;
  public ChestFailure chestFailure = ChestFailure.NONE;

  @Override
  public String getName() {
    return "TreasureChestUnlocker";
  }

  public void start(boolean clickChest) {
    this.start("", clickChest);
  }

  public void start(String itemToHold, boolean clickChest) {
    if (!itemToHold.isEmpty()) {
      // doesnt matter if i can hold it or not
      InventoryUtil.holdItem(itemToHold);
    }
    this.chestFailure = ChestFailure.NONE;
    this.clickChest = clickChest;
    this.enabled = true;
    note("Started");
  }

  @Override
  public void stop() {
    this.enabled = false;
    this.particlePos = null;
    this.chestSolving = null;
    this.walkableBlocks.clear();
    this.clickChest = false;
    this.resetStatesAfterStop();
    note("Stopped");
  }

  @Override
  public void resetStatesAfterStop() {
    this.state = State.STARTING;
  }

  private void stop(ChestFailure failure) {
    this.chestFailure = failure;
    this.stop();
  }

  private void changeState(State to, int time) {
    this.state = to;
    if (time == 0) {
      this.timer.reset();
    } else {
      this.timer.schedule(time);
    }
  }

  @SubscribeEvent
  protected void onTick(ClientTickEvent event) {
    if (!this.enabled) {
      return;
    }

    switch (this.state) {
      case STARTING:
        if (chestQueue.isEmpty()) {
          log("Chestqueue is empty");
          this.changeState(State.ENDING, 0);
          break;
        }
        this.chestSolving = chestQueue.pop();
        if (PlayerUtil.getPlayerEyePos().distanceTo(new Vec3(this.chestSolving).addVector(0.5, 0.5, 0.5)) > 4) {
          this.changeState(State.FINDING_WALKABLE_BLOCKS, 0);
        } else {
          this.changeState(State.LOOKING, 0);
        }
        break;
      case FINDING_WALKABLE_BLOCKS:
        List<BlockPos> pos = new ArrayList<>();
        for (int y = -4; y < 0; y++) {
          for (int x = -1; x < 2; x++) {
            for (int z = -1; z < 2; z++) {
              BlockPos newPos = this.chestSolving.add(x, y, z);
              if (BlockUtil.canStandOn(newPos)) {
                pos.add(newPos);
              }
            }
          }
        }
        pos.sort(Comparator.comparing(mc.thePlayer::getDistanceSqToCenter));
        this.walkableBlocks = pos;
        this.changeState(State.WALKING, 0);
        log("Found block list size: " + pos.size());
        break;
      case WALKING:
        if (this.walkableBlocks.isEmpty()) {
          error("no walkable blocks around this chest. ignoring");
          this.changeState(State.STARTING, 0);
          break;
        }
        BlockPos target = this.walkableBlocks.remove(0);
        log("Walking to " + target);
        Pathfinder.getInstance().queue(PlayerUtil.getBlockStandingOn(), target);
        Pathfinder.getInstance().start();
        this.changeState(State.WAITING, 0);
        break;
      case WAITING:
        if (Pathfinder.getInstance().isRunning()) {
          if (PlayerUtil.getPlayerEyePos().distanceTo(new Vec3(this.chestSolving).addVector(0.5, 0.5, 0.5)) < 3) {
            log("distance < 4");
            Pathfinder.getInstance().stop();
            this.changeState(State.LOOKING, 5000);
          }
        } else {
          if (Pathfinder.getInstance().succeeded()) {
            log("Pathfinder succeeded");
            this.changeState(State.LOOKING, 5000);
          } else {
            error("failed walking to block. retrying");
            this.changeState(State.WALKING, 0);
          }
        }
        break;
      case LOOKING:
        if (!this.chestSolving.equals(BlockUtil.getBlockLookingAt()) && !RotationHandler.getInstance().isEnabled()) {
          RotationHandler.getInstance().easeTo(new RotationConfiguration(new Target(this.chestSolving), 400, null));
        }
        if (this.clickChest) {
          this.changeState(State.CLICKING, 3000);
          break;
        }

        if (this.hasTimerEnded()) {
          error("no particle spawned in over 2 seconds.");
          this.changeState(State.STARTING, 0);
          break;
        }

        if (this.chestSolved) {
          log("chest solved. stopping rotation and going to next");
          RotationHandler.getInstance().stop();
          this.changeState(State.STARTING, 0);
          this.chestSolved = false;
          break;
        }

        if (this.particleSpawned && this.particlePos != null) {
          RotationHandler.getInstance().stopFollowingTarget();
          RotationHandler.getInstance().easeTo(new RotationConfiguration(new Target(this.particlePos), 400, null).followTarget(true));
          this.timer.schedule(5000);
          this.particleSpawned = false;
        }
        break;
      case CLICKING:
        if (this.hasTimerEnded()) {
          error("Couldn't look at chest.");
          this.changeState(State.STARTING, 0);
        }
        if (!RotationHandler.getInstance().isEnabled() || this.chestSolving.equals(BlockUtil.getBlockLookingAt())) {
          RotationHandler.getInstance().stop();
          KeyBindUtil.rightClick();
          this.changeState(State.STARTING, 0);
        }
        break;
      case ENDING:
        log("Ended. Stopping");
        this.stop();
        break;
    }

  }

  @SubscribeEvent
  protected void onParticleSpawn(SpawnParticleEvent event) {
    if (this.state != State.LOOKING) {
      return;
    }
    if (event.getParticleTypes() == EnumParticleTypes.CRIT && mc.thePlayer.getPositionVector().squareDistanceTo(event.getPos()) < 64) {
      IBlockState state = mc.theWorld.getBlockState(this.chestSolving);
      if (state.getBlock() instanceof BlockChest && this.chestSolving.offset(state.getValue(BlockChest.FACING))
          .equals(new BlockPos(event.getPos()))) {
        this.particlePos = event.getPos();
        this.particleSpawned = true;
      }
    }
  }

  @SubscribeEvent
  protected void onRender(RenderWorldLastEvent event) {
    chestQueue.forEach(it -> {
      RenderUtil.drawBlock(it, new Color(0, 255, 255, 100));
    });

    if (!this.enabled) {
      return;
    }

    if (this.chestSolving != null) {
      RenderUtil.drawBlock(this.chestSolving, new Color(0, 255, 0, 100));
    }

    if (this.particlePos != null) {
      RenderUtil.drawPoint(this.particlePos, new Color(255, 0, 0, 100));
    }
  }

  // maybe move this into powdermacro class
  @SubscribeEvent
  protected void onBlockChange(BlockChangeEvent event) {
    if(mc.thePlayer == null) return;
    BlockPos eventPos = event.pos;
    if (mc.thePlayer.getDistanceSq(eventPos) > 64) {
      return;
    }
    Block newBlock = event.update.getBlock();
    Block oldBlock = event.old.getBlock();

    if (newBlock instanceof BlockChest) {
      TileEntity entity = mc.theWorld.getTileEntity(eventPos);
      if (!(entity instanceof TileEntityChest)) {
        return;
      }

      TileEntityChest chest = (TileEntityChest) entity;
      if (chest.numPlayersUsing == 0) {
        note("Chest Spawned. Last: " + event.old.getBlock());
        chestQueue.add(eventPos);
      } else if (eventPos.equals(this.chestSolving)) {
        note("Chest solved");
        this.chestSolved = true;
      } else {
        note("Chest despawned");
        chestQueue.remove(eventPos);
      }
      return;
    }

    if (newBlock instanceof BlockAir && oldBlock instanceof BlockChest) {
      if (eventPos.equals(this.chestSolving)) {
        log("Chest removed; solved");
        this.chestSolved = true;
      } else {
        log("Chest despawned");
        chestQueue.remove(eventPos);
      }
    }
  }

  @SubscribeEvent
  protected void onWorldUnload(WorldEvent.Unload event) {
    chestQueue.clear();
  }

  @SubscribeEvent
  protected void onChat(ClientChatReceivedEvent event) {
    if (!this.enabled) {
      return;
    }
    String message = event.message.getUnformattedText();
    if (message.equals("CHEST LOCKPICKED")) {
      this.chestSolved = true;
    }
  }

  enum State {
    STARTING, FINDING_WALKABLE_BLOCKS, WALKING, WAITING, LOOKING, CLICKING, ENDING
  }

  enum ChestFailure {
    NONE, NO_TOOL_IN
  }
}