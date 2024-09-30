package com.jelly.mightyminerv2.command;

import static java.lang.Math.ceil;

import cc.polyfrost.oneconfig.utils.Multithreading;
import cc.polyfrost.oneconfig.utils.commands.annotations.Command;
import cc.polyfrost.oneconfig.utils.commands.annotations.Main;
import cc.polyfrost.oneconfig.utils.commands.annotations.SubCommand;
import com.jelly.mightyminerv2.config.MightyMinerConfig;
import com.jelly.mightyminerv2.event.PacketEvent;
import com.jelly.mightyminerv2.feature.impl.AutoCommissionClaim;
import com.jelly.mightyminerv2.feature.impl.AutoInventory;
import com.jelly.mightyminerv2.feature.impl.AutoMobKiller;
import com.jelly.mightyminerv2.feature.impl.Pathfinder;
import com.jelly.mightyminerv2.feature.impl.RouteNavigator;
import com.jelly.mightyminerv2.handler.GraphHandler;
import com.jelly.mightyminerv2.handler.RotationHandler;
import com.jelly.mightyminerv2.handler.RouteHandler;
import com.jelly.mightyminerv2.MightyMiner;
import com.jelly.mightyminerv2.pathfinder.goal.Goal;
import com.jelly.mightyminerv2.util.AngleUtil;
import com.jelly.mightyminerv2.util.CommissionUtil;
import com.jelly.mightyminerv2.util.Logger;
import com.jelly.mightyminerv2.util.PlayerUtil;
import com.jelly.mightyminerv2.util.RenderUtil;
import com.jelly.mightyminerv2.util.helper.Angle;
import com.jelly.mightyminerv2.util.helper.Clock;
import com.jelly.mightyminerv2.util.helper.RotationConfiguration;
import com.jelly.mightyminerv2.util.helper.RotationConfiguration.RotationType;
import com.jelly.mightyminerv2.util.helper.route.Route;
import com.jelly.mightyminerv2.util.helper.route.RouteWaypoint;
import com.jelly.mightyminerv2.util.helper.route.TransportMethod;
import com.jelly.mightyminerv2.pathfinder.calculate.PathNode;
import com.jelly.mightyminerv2.pathfinder.calculate.path.AStarPathFinder;
import com.jelly.mightyminerv2.pathfinder.movement.CalculationContext;
import com.jelly.mightyminerv2.pathfinder.movement.Movement;
import com.jelly.mightyminerv2.pathfinder.movement.MovementResult;
import com.jelly.mightyminerv2.pathfinder.movement.Moves;
import com.jelly.mightyminerv2.pathfinder.movement.movements.MovementAscend;
import com.jelly.mightyminerv2.pathfinder.movement.movements.MovementDescend;
import com.jelly.mightyminerv2.pathfinder.movement.movements.MovementDiagonal;
import com.jelly.mightyminerv2.pathfinder.movement.movements.MovementTraverse;
import com.jelly.mightyminerv2.pathfinder.util.BlockUtil;
import com.jelly.mightyminerv2.pathfinder.calculate.Path;
import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import kotlin.Pair;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.network.play.server.S2FPacketSetSlot;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Command(value = "set")
public class OsamaTestCommandNobodyTouchPleaseLoveYou {

  @Getter
  private static OsamaTestCommandNobodyTouchPleaseLoveYou instance = new OsamaTestCommandNobodyTouchPleaseLoveYou();
  private final Minecraft mc = Minecraft.getMinecraft();
  RouteWaypoint first;
  RouteWaypoint second;

  Entity entTodraw = null;
  BlockPos block = null;
  List<BlockPos> blockToDraw = new CopyOnWriteArrayList<>();
  List<Vec3> points = new ArrayList<>();
  int tick = 0;
  Path path;
  AStarPathFinder pathfinder;
  PathNode curr;
  List<Pair<EntityPlayer, Pair<Double, Double>>> mobs = new ArrayList<>();
  List<Pair<String, Entity>> ents = new ArrayList<>();
  public boolean allowed = false;
  boolean allowedd = false;
  Clock timer = new Clock();

  private String mightyMinerV2$cleanSB(String scoreboard) {
    char[] arr = scoreboard.toCharArray();
    StringBuilder cleaned = new StringBuilder();
    for (int i = 0; i < arr.length; i++) {
      char c = arr[i];
      if (c >= 32 && c < 127) {
        cleaned.append(c);
      }
      if (c == 167) {
        i++;
      }
    }
    return cleaned.toString();
  }

  @Main
  public void main() {
    BlockPos targ = new BlockPos(mc.thePlayer.getPositionVector().add(AngleUtil.getVectorForRotation(AngleUtil.get360RotationYaw())));
    this.blockToDraw.clear();
    this.blockToDraw.add(targ);
    this.blockToDraw.add(PlayerUtil.getBlockStandingOn());
    Logger.sendLog("ShouldJump: " + com.jelly.mightyminerv2.util.BlockUtil.canWalkBetween(new CalculationContext(), PlayerUtil.getBlockStandingOn(), targ));
  }

  @SubCommand
  public void p() {
    mc.theWorld.loadedEntityList.forEach(it -> {
      if (it instanceof EntityArmorStand) {
        if (it.hasCustomName()) {
          System.out.println("ARMORSTAND");
          System.out.println("Name: " + it.getCustomNameTag() + ", PositionVec: " + it.getPositionVector() + ", Height: " + it.height);
          System.out.println("Casted Pos(x: " + (int) it.posX + ", realY: " + it.posY + ", z: " + (int) it.posZ + ")");
          System.out.println("Exp Pos(x: " + (int) it.posX + ", y: " + (((int) it.posY) - 1) + ", z: " + (int) it.posZ + ")");
          System.out.println();
        }
      } else if (it instanceof EntityLivingBase) {
        System.out.println("ENTITY");
        System.out.println("Name: " + it.getName() + ", Height: " + it.height +  ", PositionVector: " + it.getPositionVector());
        System.out.println("Casted Pos(x: " + (int) it.posX + ", realY: " + it.posY + ", z: " + (int) it.posZ + ")");
        System.out.println("ExpectedCastPos Pos(x: " + (int) it.posX + ", y: " + (int) (it.posY + it.height) + ", z: " + (int) it.posZ + ")");
        System.out.println();
      }
    });
    Logger.sendNote("Done");
  }

  public String getEntityNameFromArmorStand(String armorstandName) {
    char[] carr = armorstandName.toCharArray();
    if (carr[carr.length - 1] != '❤') {
      return "";
    }
    StringBuilder builder = new StringBuilder();
    boolean foundSpace = false;
    byte charCounter = 0;
    for (int i = carr.length - 1; i >= 0; i--) {
      char curr = carr[i];
      if (!foundSpace) {
        if (curr == ' ') {
          foundSpace = true;
        }
      } else {
        if (curr == '§') {
          charCounter++;
        }
        if (charCounter == 2) {
          builder.deleteCharAt(builder.length() - 1);
          break;
        }
        builder.append(curr);
      }
    }
    return builder.reverse().toString();
  }

  @SubCommand
  public void calc() {
    BlockPos pos = new BlockPos(mc.thePlayer.posX, ceil(mc.thePlayer.posY) - 1, mc.thePlayer.posZ);
    MovementResult res = new MovementResult();
    double walkSpeed = mc.thePlayer.getAIMoveSpeed();
    CalculationContext ctx = new CalculationContext(walkSpeed * 1.3, walkSpeed, walkSpeed * 0.3);
    for (Moves move : Moves.getEntries()) {
      res.reset();
      move.calculate(ctx, pos.getX(), pos.getY(), pos.getZ(), res);
      double cost = res.getCost();
      if (cost >= 1e6) {
        continue;
      }
      Logger.sendMessage("Name: " + move.name() + ", Movement to: " + res.getDest() + ", Cost: " + cost);
      this.blockToDraw.add(res.getDest());
    }
  }

  private boolean canStandOn(final BlockPos pos) {
    return mc.theWorld.isBlockFullCube(pos)
        && mc.theWorld.isAirBlock(pos.add(0, 1, 0))
        && mc.theWorld.isAirBlock(pos.add(0, 2, 0));
  }

  @SubCommand
  public void f() {
    BlockPos playerPos = PlayerUtil.getBlockStandingOn();
    first = new RouteWaypoint(playerPos, TransportMethod.WALK);
  }

  @SubCommand
  public void s() {
    BlockPos playerPos = PlayerUtil.getBlockStandingOn();
    second = new RouteWaypoint(playerPos, TransportMethod.WALK);
  }

  @SubCommand
  public void graph() {
    GraphHandler.getInstance().toggleEdit();
  }

  @SubCommand
  public void findg() {
//    GraphHandler.getInstance().stop();
    List<RouteWaypoint> path = GraphHandler.getInstance().findPath(first, second);
    Route route = new Route();
    path.forEach(k -> route.insert(k));
    blockToDraw.clear();
//    path.forEach(i -> blockToDraw.add(new BlockPos(i.toVec3())));
    RouteNavigator.getInstance().start(route);
  }

  @SubCommand
  public void k() {
    AutoMobKiller.getInstance().start(Arrays.asList(MightyMinerConfig.devMKillerMob), "");
//    this.c = !this.c;
  }

  @SubCommand
  public void stop() {
    RouteNavigator.getInstance().stop();
    AutoMobKiller.getInstance().stop();
    Pathfinder.getInstance().stop();
    RotationHandler.getInstance().stop();
  }

  @SubCommand
  public void b() {
    this.block = PlayerUtil.getBlockStandingOn();
  }

  @SubCommand
  public void between() {
    if (this.block != null) {
      this.blockToDraw.clear();
      boolean bs = BlockUtil.INSTANCE.bresenham(new CalculationContext(), PlayerUtil.getBlockStandingOn(), this.block);
      Logger.sendMessage("Walkable: " + bs);
    }
  }

  @SubCommand
  public void rot() {
    RotationConfiguration conf = new RotationConfiguration(new Angle(0f, 0f), 400, null);
//    conf.followTarget(true);
    RotationHandler.getInstance().easeTo(conf);
  }

  @SubCommand
  public void claim() {
//    if (!AutoCommissionClaim.getInstance().isRunning()) {
//      AutoCommissionClaim.getInstance().start();
//    } else {
//      AutoCommissionClaim.getInstance().stop();
//    }
    Multithreading.schedule(() -> {
      Logger.sendNote("Comm: " + CommissionUtil.getCommissionFromContainer((ContainerChest) mc.thePlayer.openContainer));
    }, 800, TimeUnit.MILLISECONDS);
  }

  @SubCommand
  public void move() {
    AutoInventory.getInstance().moveItems(Arrays.asList("Pickonimbus 2000", "Aspect of the Void"));
  }

  @SubCommand
  public void clear() {
    blockToDraw.clear();
    entTodraw = null;
    block = null;
    path = null;
    first = null;
    second = null;
    pathfinder = null;
    curr = null;
    btd.clear();
    ents.clear();
  }

  @SubCommand
  public void aotv() {
    if (RouteHandler.getInstance().getSelectedRoute().isEmpty()) {
      Logger.sendMessage("Selected Route is empty.");
      return;
    }
    RouteNavigator.getInstance().queueRoute(RouteHandler.getInstance().getSelectedRoute());
    RouteNavigator.getInstance().goTo(36);
  }

  @SubCommand
  public void c() {
    BlockPos pp = PlayerUtil.getBlockStandingOn();
//    this.curr = this.pathfinder.getClosedSet().get(PathNode.Companion.longHash(pp.getX(), pp.getY(), pp.getZ()));
    Logger.sendMessage("Curr: " + curr);
  }

  @SubCommand
  public void go(int go) {
//    if (first == null || second == null) {
//      LogUtil.error("First or sec is null");
//      return;
//    }
//    Multithreading.schedule(() -> {
//      double walkSpeed = mc.thePlayer.getAIMoveSpeed();
//      CalculationContext ctx = new CalculationContext(walkSpeed * 1.3, walkSpeed, walkSpeed * 0.3);
//      BlockPos first = PlayerUtil.getBlockStandingOn();
//      BlockPos second = this.block;
//      AStarPathFinder finder = new AStarPathFinder(
//          first.getX(), first.getY(), first.getZ(),
//          new Goal(second.getX(), second.getY(), second.getZ(), ctx),
//          ctx
//      );
//      Path path = finder.calculatePath();
//      if (path == null) {
//        Logger.sendMessage("No path found");
//      } else {
//        Logger.sendMessage("path found");
//        blockToDraw.clear();
//        if (go == 0) {
//          blockToDraw.addAll(path.getSmoothedPath());
//          Pathfinder.getInstance().queue();
//        } else {
//          blockToDraw.addAll(path.getPath());
//        }
//      }
//    }, 0, TimeUnit.MILLISECONDS);
    Pathfinder.getInstance().queue(PlayerUtil.getBlockStandingOn(), this.block);
    Pathfinder.getInstance().setInterpolationState(go == 0);
    Pathfinder.getInstance().start();
//    Pathfinder.getInstance().queue(new BlockPos(first.toVec3()), new BlockPos(second.toVec3()));

  }

  //    @SubscribeEvent
//  public void onTick(ClientTickEvent event) {
//    if (!allowed) {
//      return;
//    }
//    mobs = CommissionUtil.getMobListDebug("Goblin", new HashSet<>());
//  }
//
  List<Pair<BlockPos, List<Float>>> btd = new ArrayList<>();

  boolean test = false;

  //  @SubscribeEvent
  public void onPack(PacketEvent.Received event) {
    if (!this.allowed) {
      return;
    }
    if (this.timer.isScheduled() && this.timer.passed()) {
      this.timer.reset();
      this.test = false;
      this.allowed = false;
    }
//    if (!this.allowed || !(event.packet instanceof S2FPacketSetSlot)) {
//      return;
//    }
    if (event.packet instanceof S2FPacketSetSlot) {
      if (!this.timer.isScheduled()) {
        this.timer.schedule(5000);
        this.test = true;
      }
      S2FPacketSetSlot pack = (S2FPacketSetSlot) event.packet;
      Logger.sendLog("Real: " + pack.func_149173_d() + ", +36: " + (pack.func_149173_d() + 36) + ", Item: " + (pack.func_149174_e() != null
          ? pack.func_149174_e().stackSize : "null"));
    }
    if (!this.test) {
      return;
    }
    Logger.sendLog(event.packet.toString());
  }

  @SubscribeEvent
  public void onRender(RenderWorldLastEvent event) {
    if (entTodraw != null) {
      RenderUtil.drawBox(entTodraw.getEntityBoundingBox(), new Color(123, 214, 44, 150));
      RenderUtil.drawBlockBox(new BlockPos(entTodraw.posX, Math.ceil(entTodraw.posY) - 1, entTodraw.posZ), new Color(123, 214, 44, 150));
    }

    if (!blockToDraw.isEmpty()) {
      blockToDraw.forEach(b -> RenderUtil.drawBlockBox(b, new Color(255, 0, 0, 200)));
    }

    if (this.block != null) {
      RenderUtil.drawBlockBox(this.block, new Color(255, 0, 0, 50));
    }

    if (this.first != null) {
      RenderUtil.drawBlockBox(new BlockPos(this.first.toVec3()), new Color(0, 0, 0, 200));
    }

    if (this.second != null) {
      RenderUtil.drawBlockBox(new BlockPos(this.second.toVec3()), new Color(0, 0, 0, 200));
    }

//    if (pathfinder != null) {
//      pathfinder.getClosedSet().values().forEach(it -> RenderUtil.drawBlockBox(it.getBlock(), new Color(213, 124, 124, 100)));
//    }

    if (path != null) {
      path.getPath().forEach(tit -> RenderUtil.drawBlockBox(tit, new Color(255, 0, 0, 200)));
    }

    if (curr != null) {
      RenderUtil.drawBlockBox(curr.getBlock(), new Color(0, 255, 0, 255));
      if (curr.getParentNode() != null) {
        RenderUtil.drawBlockBox(curr.getParentNode().getBlock(), new Color(0, 0, 255, 255));
      }
    }

    if (!ents.isEmpty()) {
      ents.forEach(ent -> {
        Vec3 pos = ent.getSecond().getPositionVector();
        RenderUtil.drawText(ent.getFirst(), pos.xCoord, pos.yCoord + 0.5, pos.zCoord, 1f);
      });
    }

    if (!mobs.isEmpty()) {
      Pair<EntityPlayer, Pair<Double, Double>> best = mobs.get(0);
      Vec3 pos = best.getFirst().getPositionVector();
      RenderUtil.drawBox(new AxisAlignedBB(pos.xCoord - 0.5, pos.yCoord, pos.zCoord - 0.5, pos.xCoord + 0.5, pos.yCoord + 2, pos.zCoord + 0.5),
          new Color(255, 0, 241, 150));
      RenderUtil.drawText(String.format("Dist: %.2f, Angle: %.2f", best.getSecond().getFirst(), best.getSecond().getSecond()), pos.xCoord,
          pos.yCoord + 2.2, pos.zCoord, 1);

      for (int i = 1; i < mobs.size(); i++) {
        best = mobs.get(i);
        pos = best.getFirst().getPositionVector();
        RenderUtil.drawBox(new AxisAlignedBB(pos.xCoord - 0.5, pos.yCoord, pos.zCoord - 0.5, pos.xCoord + 0.5, pos.yCoord + 2, pos.zCoord + 0.5),
            new Color(123, 0, 234, 150));
        RenderUtil.drawText(String.format("Dist: %.2f, Angle: %.2f", best.getSecond().getFirst(), best.getSecond().getSecond()), pos.xCoord,
            pos.yCoord + 2.2, pos.zCoord, 1);
      }
    }

    if (!btd.isEmpty()) {
      Pair<BlockPos, List<Float>> best = btd.get(0);
      BlockPos pos = best.getFirst();
      RenderUtil.drawBlockBox(best.getFirst(), new Color(123, 0, 234, 150));
      if (com.jelly.mightyminerv2.util.BlockUtil.getBlockLookingAt().equals(pos)) {
        RenderUtil.drawText(
            String.format("Hardness: %.2f, Angle: %.2f, Dist: %.2f", best.getSecond().get(0), best.getSecond().get(1), best.getSecond().get(2)),
            pos.getX() + 0.5,
            pos.getY() + 1.2, pos.getZ() + 0.5, 1);
      }

      for (int i = 1; i < btd.size(); i++) {
        best = btd.get(i);
        pos = best.getFirst();
        RenderUtil.drawBlockBox(best.getFirst(), new Color(255, 0, 241, 50));
        if (com.jelly.mightyminerv2.util.BlockUtil.getBlockLookingAt().equals(pos)) {
          RenderUtil.drawText(
              String.format("Hardness: %.2f, Angle: %.2f, Dist: %.2f", best.getSecond().get(0), best.getSecond().get(1), best.getSecond().get(2)),
              pos.getX() + 0.5,
              pos.getY() + 1.2, pos.getZ() + 0.5, 1);
        }
      }


      if(this.allowedd) {
        BlockPos poss = new BlockPos(mc.thePlayer.getPositionVector().add(AngleUtil.getVectorForRotation(AngleUtil.normalizeAngle(mc.thePlayer.rotationYaw))));
        if (!mc.theWorld.isAirBlock(poss)) {
          RenderUtil.drawBlockBox(poss, Color.GREEN);
        }
      }
    }
  }

  @SubCommand
  public void trav() {
    double walkSpeed = mc.thePlayer.getAIMoveSpeed();
    CalculationContext ctx = new CalculationContext(walkSpeed * 1.3, walkSpeed, walkSpeed * 0.3);
    BlockPos pp = PlayerUtil.getBlockStandingOn();
    EnumFacing d = mc.thePlayer.getHorizontalFacing();
    MovementResult res = new MovementResult();
    Movement trav = new MovementTraverse(MightyMiner.instance, pp, pp.add(d.getFrontOffsetX(), d.getFrontOffsetY(), d.getFrontOffsetZ()));
    trav.calculateCost(ctx, res);
    Logger.sendMessage("Movement cost: " + res.getCost());
    this.block = res.getDest();
  }

  @SubCommand
  public void asc() {
    CalculationContext ctx = new CalculationContext();
    BlockPos pp = PlayerUtil.getBlockStandingOn();
    EnumFacing d = mc.thePlayer.getHorizontalFacing();
    MovementResult res = new MovementResult();
    Movement trav = new MovementAscend(MightyMiner.instance, pp, pp.add(d.getFrontOffsetX(), d.getFrontOffsetY() + 1, d.getFrontOffsetZ()));
    trav.calculateCost(ctx, res);
    Logger.sendMessage("Movement cost: " + res.getCost());
    this.block = res.getDest();
  }

  @SubCommand
  public void desc() {
    double walkSpeed = mc.thePlayer.getAIMoveSpeed();
    CalculationContext ctx = new CalculationContext(walkSpeed * 1.3, walkSpeed, walkSpeed * 0.3);
    BlockPos pp = PlayerUtil.getBlockStandingOn();
    EnumFacing d = mc.thePlayer.getHorizontalFacing();
    MovementResult res = new MovementResult();
    Movement trav = new MovementDescend(MightyMiner.instance, pp, pp.add(d.getFrontOffsetX(), d.getFrontOffsetY() - 1, d.getFrontOffsetZ()));
    trav.calculateCost(ctx, res);
    Logger.sendMessage("Movement cost: " + res.getCost());
    this.block = res.getDest();
  }

  @SubCommand
  public void diag() {
    double walkSpeed = mc.thePlayer.getAIMoveSpeed();
    CalculationContext ctx = new CalculationContext(walkSpeed * 1.3, walkSpeed, walkSpeed * 0.3);
    BlockPos pp = PlayerUtil.getBlockStandingOn();
    MovementResult res = new MovementResult();
    Movement diag = new MovementDiagonal(MightyMiner.instance, pp, pp.add(1, 0, 1));
    diag.calculateCost(ctx, res);
    Logger.sendMessage("Movement cost: " + res.getCost());
    this.block = res.getDest();
  }
}