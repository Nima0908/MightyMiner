package com.jelly.MightyMinerV2.Util;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import java.util.ArrayList;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class TablistUtil {

  public static final Ordering<NetworkPlayerInfo> playerOrdering = Ordering.from(
      new PlayerComparator());

  @Getter
  private static final CopyOnWriteArrayList<String> cachedTablist = new CopyOnWriteArrayList<>();
  @Getter
  private static final CopyOnWriteArrayList<String> cachedTablistFooter = new CopyOnWriteArrayList<>();

  public static void setCachedTablist(List<String> tablist) {
    cachedTablist.clear();
    cachedTablist.addAll(tablist);
  }

  public static void setCachedTabListFooter(List<String> tabListFooter) {
    cachedTablistFooter.clear();
    cachedTablistFooter.addAll(tabListFooter);
  }

  @SideOnly(Side.CLIENT)
  static class PlayerComparator implements Comparator<NetworkPlayerInfo> {

    private PlayerComparator() {
    }

    public int compare(NetworkPlayerInfo o1, NetworkPlayerInfo o2) {
      ScorePlayerTeam team1 = o1.getPlayerTeam();
      ScorePlayerTeam team2 = o2.getPlayerTeam();
      return ComparisonChain.start().compareTrueFirst(
              o1.getGameType() != WorldSettings.GameType.SPECTATOR,
              o2.getGameType() != WorldSettings.GameType.SPECTATOR
          )
          .compare(
              team1 != null ? team1.getRegisteredName() : "",
              team2 != null ? team2.getRegisteredName() : ""
          )
          .compare(o1.getGameProfile().getName(), o2.getGameProfile().getName()).result();
    }
  }

  public static List<String> getTabListPlayersUnprocessed() {
    try {
      List<NetworkPlayerInfo> players = playerOrdering.sortedCopy(
          Minecraft.getMinecraft().thePlayer.sendQueue.getPlayerInfoMap()
      );

      List<String> result = new ArrayList<>();

      for (NetworkPlayerInfo info : players) {
        String name = Minecraft.getMinecraft().ingameGUI.getTabList().getPlayerName(info);
        result.add(name);
      }
      return result;
    } catch (Exception e) {
      return new ArrayList<>();
    }
  }

  public static List<String> getTabListPlayersSkyblock() {
    try {
      List<String> tabListPlayersFormatted = getTabListPlayersUnprocessed();
      List<String> playerList = new ArrayList<>();
      tabListPlayersFormatted.remove(0); // remove "Players (x)"
      String firstPlayer = null;
      for (String s : tabListPlayersFormatted) {
        int a = s.indexOf("]");
        if (a == -1) {
          continue;
        }
        if (s.length() < a + 2) {
          continue; // if the player name is too short (e.g. "§c[§f]"
        }

        s = s.substring(a + 2).replaceAll("§([0-9]|[a-z])", "").replace("♲", "").trim();
        if (firstPlayer == null) {
          firstPlayer = s;
        } else if (s.equals(firstPlayer)) // it returns two copy of the player list for some reason
        {
          break;
        }
        playerList.add(s);
      }
      return playerList;
    } catch (Exception e) {
      return new ArrayList<>();
    }
  }
}
