package com.jelly.mightyminerv2.Macro;

import cc.polyfrost.oneconfig.events.event.ReceivePacketEvent;
import com.jelly.mightyminerv2.Util.LogUtil;
import com.jelly.mightyminerv2.Util.helper.Clock;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;

public abstract class AbstractMacro {

  private static final Minecraft mc = Minecraft.getMinecraft();
  private boolean enabled = false;
  public Clock timer = new Clock();

  public abstract String getName();

  public boolean isEnabled() {
    return this.enabled;
  }

  public void enable() {
    onEnable();
    this.enabled = true;
  }

  public void disable() {
    this.enabled = false;
    onDisable();
  }

  public void toggle() {
    if (this.enabled) {
      this.disable();
    } else {
      this.enable();
    }
  }

  public boolean hasTimerEnded(){
    return this.timer.isScheduled() && this.timer.passed();
  }

  public abstract List<String> getNecessaryItems();

  public void onEnable() {
  }

  public void onDisable() {
  }

  public void onTick(ClientTickEvent event) {
  }

  public void onWorldRender(RenderWorldLastEvent event) {
  }

  public void onChat(ClientChatReceivedEvent event) {
  }

  public void onOverlayRender(RenderGameOverlayEvent event) {
  }

  public void onReceivePacket(ReceivePacketEvent event) {
  }

  public void log(String message) {
    LogUtil.log(getMessage(message));
  }

  public void send(String message) {
    LogUtil.send(getMessage(message));
  }

  public void error(String message) {
    LogUtil.error(getMessage(message));
  }

  public void warn(String message) {
    LogUtil.warn(getMessage(message));
  }

  public String getMessage(String message) {
    return "[" + this.getName() + "] " + message;
  }
}