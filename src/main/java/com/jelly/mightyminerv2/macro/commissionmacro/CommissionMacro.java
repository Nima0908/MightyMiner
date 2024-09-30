package com.jelly.mightyminerv2.macro.commissionmacro;

import com.jelly.mightyminerv2.config.MightyMinerConfig;
import com.jelly.mightyminerv2.event.UpdateTablistEvent;
import com.jelly.mightyminerv2.failsafe.AbstractFailsafe.Failsafe;
import com.jelly.mightyminerv2.failsafe.FailsafeManager;
import com.jelly.mightyminerv2.feature.FeatureManager;
import com.jelly.mightyminerv2.feature.impl.AutoCommissionClaim;
import com.jelly.mightyminerv2.feature.impl.AutoInventory;
import com.jelly.mightyminerv2.feature.impl.AutoMobKiller;
import com.jelly.mightyminerv2.feature.impl.AutoWarp;
import com.jelly.mightyminerv2.feature.impl.MithrilMiner;
import com.jelly.mightyminerv2.feature.impl.RouteNavigator;
import com.jelly.mightyminerv2.handler.GameStateHandler;
import com.jelly.mightyminerv2.handler.GraphHandler;
import com.jelly.mightyminerv2.macro.AbstractMacro;
import com.jelly.mightyminerv2.macro.commissionmacro.helper.Commission;
import com.jelly.mightyminerv2.util.CommissionUtil;
import com.jelly.mightyminerv2.util.InventoryUtil;
import com.jelly.mightyminerv2.util.PlayerUtil;
import com.jelly.mightyminerv2.util.helper.location.Location;
import com.jelly.mightyminerv2.util.helper.location.SubLocation;
import com.jelly.mightyminerv2.util.helper.route.Route;
import com.jelly.mightyminerv2.util.helper.route.RouteWaypoint;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;

public class CommissionMacro extends AbstractMacro {

  private static CommissionMacro instance = new CommissionMacro();

  public static CommissionMacro getInstance() {
    return instance;
  }

  @Override
  public String getName() {
    return "Commission Macro";
  }

  @Override
  public void onEnable() {
    this.mainState = MainState.MACRO;
    log("CommMacro::onEnable");
  }

  @Override
  public void onDisable() {
    this.mainState = MainState.NONE;
    this.warpState = WarpState.STARTING;
    this.itemState = ItemState.STARTING;
    this.macroState = MacroState.STARTING;

    this.warpRetries = 0;
    this.itemRetries = 0;
    this.macroRetries = 0;

    this.miningSpeed = this.miningSpeedBoost = 0;
    this.curr = this.last = null;

    this.checkForCommissionChange = false;
    log("CommMacro::onDisable");
  }

  @Override
  public void onPause() {
    FeatureManager.getInstance().pauseAll();
    log("CommMacro::onPause");
  }

  @Override
  public void onResume() {
    FeatureManager.getInstance().resumeAll();
    log("CommMacro::onResume");
  }

  @Override
  public List<String> getNecessaryItems() {
    return Arrays.asList(MightyMinerConfig.commMiningTool);
  }

  public void stopActiveFeatures(){
    MithrilMiner.getInstance().stop();
    AutoMobKiller.getInstance().stop();
  }

  private void changeMainState(MainState to, int timeToWait) {
    this.mainState = to;
    this.timer.schedule(timeToWait);
  }

  private void changeMainState(MainState to) {
    this.mainState = to;
  }

  private MainState mainState = MainState.NONE;

  public void onTick(ClientTickEvent event) {
    if (!this.isEnabled()) {
      return;
    }

    if (this.isTimerRunning()) {
      return;
    }

    if (this.mainState == MainState.MACRO) {
      if (GameStateHandler.getInstance().getCurrentLocation() != Location.DWARVEN_MINES) {
        this.changeMainState(MainState.WARP, 0);
      } else if (!InventoryUtil.areItemsInInventory(this.getNecessaryItems()) && !FailsafeManager.getInstance()
          .isFailsafeActive(Failsafe.ITEM_CHANGE)) {
        this.changeMainState(MainState.NONE, 0);
      } else if (!InventoryUtil.areItemsInHotbar(this.getNecessaryItems()) && !FailsafeManager.getInstance().isFailsafeActive(Failsafe.ITEM_CHANGE)) {
        this.changeMainState(MainState.ITEMS, 0);
      }
    }

    switch (this.mainState) {
      case NONE:
        this.disable();
        break;
      case WARP:
        this.handleWarp();
        break;
      case ITEMS:
        this.handleItems();
        break;
      case MACRO:
        this.handleMacro();
        break;
    }
  }

  public void onChat(String message) {
    if (!this.isEnabled() || this.mainState != MainState.MACRO) {
      return;
    }

    if (message.equalsIgnoreCase(this.curr.getName() + " Commission Complete! Visit the King to claim your rewards!")) {
      send("Commission Complete Detected");
      this.stopActiveFeatures();
      this.last = this.curr;
      this.curr = Commission.COMMISSION_CLAIM;
      this.changeMacroState(MacroState.PATHING);
    }
  }

  public void onTablistUpdate(UpdateTablistEvent event) {
    if (!this.isEnabled() || this.mainState != MainState.MACRO || !this.checkForCommissionChange) {
      return;
    }

    if (CommissionUtil.getCurrentCommission() != this.curr) {
      MithrilMiner.getInstance().stop();
      AutoMobKiller.getInstance().stop();
      this.checkForCommissionChange = false;
      this.changeMacroState(MacroState.STARTING);
    }
  }

  // <editor-fold desc="Handle Macro">
  private MacroState macroState = MacroState.STARTING;
  private boolean checkForCommissionChange = false;
  private int miningSpeed = 0;
  private int miningSpeedBoost = 0;
  private int macroRetries = 0;
  private Commission last;
  private Commission curr;

  private final int[] mithrilPriority = {9, 6, 4, 1};
  private final int[] titaniumPriority = {5, 3, 1, 10};

  private void changeMacroState(MacroState to, int timeToWait) {
    this.macroState = to;
    this.timer.schedule(timeToWait);
  }

  private void changeMacroState(MacroState to) {
    this.macroState = to;
  }

  public void handleMacro() {
    switch (this.macroState) {
      case STARTING:
        this.changeMacroState(MacroState.CHECKING_COMMISSION);
        if (this.miningSpeed == 0 && this.miningSpeedBoost == 0) {
          if (!InventoryUtil.holdItem(MightyMinerConfig.commMiningTool)) {
            error("Something went wrong. Cannot hold mining tool");
            this.changeMainState(MainState.NONE);
            return;
          }
          this.changeMacroState(MacroState.CHECKING_STATS, 500);
        }
        break;
      case CHECKING_STATS:
        AutoInventory.getInstance().retrieveSpeedBoost();
        this.changeMacroState(MacroState.GETTING_STATS);
        break;
      case GETTING_STATS:
        if (AutoInventory.getInstance().isRunning()) {
          return;
        }

        if (AutoInventory.getInstance().sbSucceeded()) {
          int[] sb = AutoInventory.getInstance().getSpeedBoostValues();
          this.miningSpeed = sb[0];
          this.miningSpeedBoost = sb[1];
          this.macroRetries = 0;
          this.changeMacroState(MacroState.STARTING);
          log("MiningSpeed: " + miningSpeed + ", MiningSpeedBoost: " + miningSpeedBoost);
          return;
        }

        switch (AutoInventory.getInstance().getSbError()) {
          case NONE:
            throw new IllegalStateException("AutoInventory#getSbError failed but returned NONE");
          case CANNOT_OPEN_INV:
            if (++this.macroRetries > 3) {
              this.changeMainState(MainState.NONE);
              error("Tried 3 times to open inv but failed. Stopping");
            } else {
              this.changeMacroState(MacroState.STARTING);
              log("Failed to open inventory. Retrying");
            }
            break;
          case CANNOT_GET_VALUE:
            this.changeMainState(MainState.NONE);
            error("Failed To Get Value. Follow Previous Instruction (If Any) or contact the developer.");
            break;
        }
        break;
      case CHECKING_COMMISSION:
        this.last = this.curr;
        this.curr = CommissionUtil.getCurrentCommission();
        this.changeMacroState(MacroState.PATHING);

        if (this.curr != null) {
          this.macroRetries = 0;
          return;
        }

        if (++this.macroRetries > 3) {
          error("Tried > 3 times to retrieve current commission but failed. Disabling");
          this.changeMainState(MainState.NONE);
        } else {
          log("Could not find commission. Retrying");
          this.changeMacroState(MacroState.STARTING, 300);
        }
        break;
      case PATHING:
        RouteWaypoint end;
        if (this.curr.getName().equals("Claim Commission")) {
          end = this.curr.closestWaypointTo(CommissionUtil.getClosestEmissaryPosition());
        } else {
          end = this.curr.getWaypoint();
        }

        List<RouteWaypoint> nodes = GraphHandler.getInstance().findPath(PlayerUtil.getBlockStandingOn(), end);
        if (nodes.isEmpty()) {
          error("Could not find a path to target. Stopping");
          this.changeMainState(MainState.NONE);
          return;
        }

        RouteNavigator.getInstance().start(new Route(nodes));
        this.changeMacroState(MacroState.PATHING_VERIFY);
        break;
      case PATHING_VERIFY:
        if (RouteNavigator.getInstance().isRunning()) {
          return;
        }

        if (RouteNavigator.getInstance().succeeded()) {
          this.changeMacroState(MacroState.TOGGLE_MACRO);
          this.macroRetries = 0;
          return;
        }

        if (++this.macroRetries > 3) {
          this.changeMainState(MainState.NONE);
          error("Could Not Reach Vein Properly. Disabling");
          return;
        }

        switch (RouteNavigator.getInstance().getNavError()) {
          case NONE:
            error("RouteNavigator Failed But NavError is NONE.");
            this.changeMainState(MainState.NONE);
            break;
          case TIME_FAIL:
          case PATHFIND_FAILED:
            this.changeMainState(MainState.WARP);
            this.changeMacroState(MacroState.PATHING);
            break;
        }
        break;
      case TOGGLE_MACRO:
        String commName = this.curr.getName();
        if (commName.contains("Claim")) {
          this.changeMacroState(MacroState.CLAIMING_COMMISSION);
        } else if (commName.contains("Titanium") || commName.contains("Mithril")) {
          this.changeMacroState(MacroState.START_MINING);
        } else {
          this.changeMacroState(MacroState.ENABLE_MOBKILLER);
        }
        break;
      case START_MINING:
        MithrilMiner.getInstance().start(
            miningSpeed,
            miningSpeedBoost,
            this.curr.getName().contains("Titanium") ? titaniumPriority : mithrilPriority,
            MightyMinerConfig.commMiningTool
        );
        this.changeMacroState(MacroState.MINING_VERIFY);
        break;
      case MINING_VERIFY:
        if (MithrilMiner.getInstance().isRunning()) {
          return;
        }
        if (MithrilMiner.getInstance().hasSucceeded()) {
          return;
        }

        if (++this.macroRetries > 3) {
          this.changeMainState(MainState.NONE);
          error("MithrilMiner Crashed More Than 3 Times");
          break;
        }

        switch (MithrilMiner.getInstance().getMithrilError()) {
          case NONE:
            error("MithrilMacro Failed but error is NONE.");
            this.changeMainState(MainState.NONE);
            return;
          case NOT_ENOUGH_BLOCKS:
            this.changeMainState(MainState.WARP);
            this.changeMacroState(MacroState.STARTING);
            break;
        }
        break;
      case ENABLE_MOBKILLER:
        Set<String> mobName = CommissionUtil.getMobForCommission(this.curr);
        if (mobName == null) {
          error("Was Supposed to Start MobKiller but current comm is " + this.curr.getName());
          this.changeMainState(MainState.NONE);
          return;
        }
        AutoMobKiller.getInstance().start(mobName, this.curr.getName().startsWith("Glacite") ? MightyMinerConfig.commMiningTool : MightyMinerConfig.commSlayerWeapon);
        this.changeMacroState(MacroState.MOBKILLER_VERIFY);
        break;
      case MOBKILLER_VERIFY:
        if (AutoMobKiller.getInstance().isRunning()) {
          return;
        }
        if (AutoMobKiller.getInstance().succeeded()) {
          return;
        }

        if (++this.macroRetries > 3) {
          this.changeMainState(MainState.NONE);
          error("Tried AutoMobKiller more than 3 times but it didnt work. Disabling");
          break;
        }

        switch (AutoMobKiller.getInstance().getMkError()) {
          case NONE:
            error("AutoMobKiller Failed But MKError is NONE.");
            this.changeMainState(MainState.NONE);
            break;
          case NO_ENTITIES:
            log("Restarting");
            this.changeMainState(MainState.WARP);
            this.changeMacroState(MacroState.STARTING);
            break;
        }
        break;
      case CLAIMING_COMMISSION:
        AutoCommissionClaim.getInstance().start();
        this.changeMacroState(MacroState.CLAIM_VERIFY);
        break;
      case CLAIM_VERIFY:
        if (AutoCommissionClaim.getInstance().isRunning()) {
          return;
        }

        if (AutoCommissionClaim.getInstance().succeeded()) {
          Commission nextComm = AutoCommissionClaim.getInstance().getNextComm();
          if (nextComm == null) {
            error("Couldn't retrieve next comm from NPC gui. Waiting for Tablist to update.");
            this.changeMacroState(MacroState.WAITING, 3000);
            this.checkForCommissionChange = true;
          } else {
            this.curr = nextComm;
            this.changeMacroState(MacroState.PATHING);
          }
          return;
        }

        if (++this.macroRetries > 3) {
          error("Tried three time but kept getting timed out. Disabling");
          this.changeMainState(MainState.NONE);
          break;
        }

        switch (AutoCommissionClaim.getInstance().claimError()) {
          case NONE:
            error("AutoCommissionClaim Failed but ClaimError is NONE.");
            this.changeMainState(MainState.NONE);
            break;
          case INACCESSIBLE_NPC:
            error("Inaccessible NPC. Retrying");
            this.changeMainState(MainState.WARP);
            this.changeMacroState(MacroState.STARTING);
            break;
          case TIMEOUT:
            log("Retrying claim");
            this.changeMacroState(MacroState.CLAIMING_COMMISSION);
            break;
        }
        break;
      case WAITING:
        if (this.hasTimerEnded()) {
          error("Timer ended but couldnt exit WAITING state. Disabling");
          this.changeMainState(MainState.NONE);
        }
        break;
    }
  }
  // </editor-fold>

  // <editor-fold desc="Handle Items">
  private ItemState itemState = ItemState.STARTING;
  private int itemRetries = 0;

  private void changeItemState(ItemState to, int time) {
    this.itemState = to;
    this.timer.schedule(time);
  }

  private void changeItemState(ItemState to) {
    this.itemState = to;
  }

  public void handleItems() {
    switch (this.itemState) {
      case STARTING:
        this.changeItemState(ItemState.TRIGGERING_AUTO_INV);
        break;
      case TRIGGERING_AUTO_INV:
        AutoInventory.getInstance().moveItems(this.getNecessaryItems());
        this.changeItemState(ItemState.WAITING_FOR_AUTO_INV);
        break;
      case WAITING_FOR_AUTO_INV:
        if (AutoInventory.getInstance().isRunning()) {
          return;
        }

        if (!AutoInventory.getInstance().moveFailed()) {
          log("AutoInventory Completed");
          this.changeMainState(MainState.MACRO);
          return;
        }

        if (++this.itemRetries > 3) {
          this.changeMainState(MainState.NONE);
          error("tried to swap items three times but failed");
        } else {
          this.changeItemState(ItemState.HANDLING_ERRORS);
          log("Attempting to handle errors");
        }
        break;
      case HANDLING_ERRORS:
        switch (AutoInventory.getInstance().getMoveError()) {
          case NONE:
            throw new IllegalStateException("AutoInventory Failed but MoveError is NONE.");
          case NOT_ENOUGH_HOTBAR_SPACE:
            this.changeMainState(MainState.NONE);
            error("Not enough space in hotbar. This should never happen.");
            break;
        }
        break;
    }
  }

  //</editor-fold>

  // <editor-fold desc="Handle Warp">
  private WarpState warpState = WarpState.STARTING;
  private int warpRetries = 0;

  private void changeWarpState(WarpState to, int timeToWait) {
    this.warpState = to;
    this.timer.schedule(timeToWait);
  }

  private void changeWarpState(WarpState to) {
    this.warpState = to;
  }

  public void handleWarp() {
    switch (this.warpState) {
      case STARTING:
        this.changeWarpState(WarpState.TRIGGERING_AUTOWARP);
        break;

      case TRIGGERING_AUTOWARP:
        AutoWarp.getInstance().start(null, SubLocation.THE_FORGE);
        this.changeWarpState(WarpState.WAITING_FOR_AUTOWARP);
        break;

      case WAITING_FOR_AUTOWARP:
        if (AutoWarp.getInstance().isRunning()) {
          return;
        }

        log("AutoWarp Ended");

        if (AutoWarp.getInstance().hasSucceeded()) {
          log("AutoWarp Completed");
          this.changeMainState(MainState.MACRO);
          return;
        }

        if (++this.warpRetries > 3) {
          this.changeMainState(MainState.NONE);
          error("Tried to warp 3 times but didn't reach destination. Disabling.");
        } else {
          log("Something went wrong while warping. Trying to fix!");
          this.changeWarpState(WarpState.HANDLING_ERRORS);
        }
        break;

      case HANDLING_ERRORS:
        switch (AutoWarp.getInstance().getFailReason()) {
          case NONE:
            throw new IllegalStateException("AutoWarp Failed But FailReason is NONE.");
          case FAILED_TO_WARP:
            log("Retrying AutoWarp");
            this.changeWarpState(WarpState.STARTING);
            break;
          case NO_SCROLL:
            log("No Warp Scroll. Disabling");
            this.changeMainState(MainState.NONE);
            break;
        }
    }
  }
  // </editor-fold>

  enum MainState {
    NONE, WARP, ITEMS, MACRO,
  }

  enum WarpState {
    STARTING, TRIGGERING_AUTOWARP, WAITING_FOR_AUTOWARP, HANDLING_ERRORS
  }

  enum ItemState {
    STARTING, TRIGGERING_AUTO_INV, WAITING_FOR_AUTO_INV, HANDLING_ERRORS
  }

  enum MacroState {
    STARTING, CHECKING_STATS, GETTING_STATS, CHECKING_COMMISSION, PATHING, PATHING_VERIFY, TOGGLE_MACRO, START_MINING, MINING_VERIFY, ENABLE_MOBKILLER, MOBKILLER_VERIFY, CLAIMING_COMMISSION, CLAIM_VERIFY, WAITING;
  }
}