/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.spleefleague.swc.listener;

import com.comphenix.packetwrapper.WrapperPlayServerPlayerInfo;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.spleefleague.core.SpleefLeague;
import com.spleefleague.core.events.GeneralPlayerLoadedEvent;
import com.spleefleague.core.player.SLPlayer;
import com.spleefleague.swc.SWC;
import com.spleefleague.swc.game.Battle;
import com.spleefleague.swc.player.SWCPlayer;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 *
 * @author Jonas
 */
public class ConnectionListener implements Listener {

    private static Listener instance;

    public static void init() {
        if (instance == null) {
            instance = new ConnectionListener();
            Bukkit.getPluginManager().registerEvents(instance, SWC.getInstance());
        }
    }

    private ConnectionListener() {

    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onQuit(PlayerQuitEvent event) {
        SWCPlayer sp = SWC.getInstance().getPlayerManager().get(event.getPlayer());
        if (sp.isIngame()) {
            SWC.getInstance().getBattleManager().getBattle(sp).playerQuit(sp);
        }
    }

    @EventHandler
    public void onJoin(GeneralPlayerLoadedEvent e) {
        if (e.getGeneralPlayer() instanceof SLPlayer) {
            SWCPlayer swcPlayer = SWC.getInstance().getPlayerManager().get(e.getPlayer());
            for (Battle battle : SWC.getInstance().getBattleManager().getAll()) {
                for (SWCPlayer player : battle.getDisconnectPlayers()) {
                    if (player != null && swcPlayer != null && player.getUniqueId().equals(swcPlayer.getUniqueId())) {
                        battle.rejoin(swcPlayer, (SLPlayer) e.getGeneralPlayer(), player.getDisconnectLocation());
                        return;
                    }
                }
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        List<Player> ingamePlayers = new ArrayList<>();
        List<Battle> toCancel = new ArrayList<>();//Workaround
        for (Battle battle : SWC.getInstance().getBattleManager().getAll()) {
            for (SWCPlayer p : battle.getActivePlayers()) {
                if (p.getPlayer() != null) {
                    event.getPlayer().hidePlayer(p.getPlayer());
                    p.getPlayer().hidePlayer(event.getPlayer());
                    ingamePlayers.add(p.getPlayer());
                } else {
                    toCancel.add(battle);
                    break;
                }
            }
        }
        for (Battle battle : toCancel) {
            for (SWCPlayer p : battle.getActivePlayers()) {
                if (p.getPlayer() != null) {
                    p.kickPlayer("An error has occured. Please reconnect");
                }
            }
            SWC.getInstance().getBattleManager().remove(battle);
        }
        Bukkit.getScheduler().runTaskLater(SWC.getInstance(), () -> {
            List<PlayerInfoData> list = new ArrayList<>();
            SpleefLeague.getInstance().getPlayerManager().getAll().forEach((SLPlayer slPlayer) -> list.add(new PlayerInfoData(WrappedGameProfile.fromPlayer(slPlayer.getPlayer()), ((CraftPlayer) slPlayer.getPlayer()).getHandle().ping, EnumWrappers.NativeGameMode.SURVIVAL, WrappedChatComponent.fromText(slPlayer.getRank().getColor() + slPlayer.getName()))));
            WrapperPlayServerPlayerInfo packet = new WrapperPlayServerPlayerInfo();
            packet.setAction(EnumWrappers.PlayerInfoAction.ADD_PLAYER);
            packet.setData(list);
            ingamePlayers.forEach((Player p) -> packet.sendPacket(p));

            list.clear();
            ingamePlayers.forEach((Player p) -> {
                SLPlayer generalPlayer = SpleefLeague.getInstance().getPlayerManager().get(p);
                list.add(new PlayerInfoData(WrappedGameProfile.fromPlayer(p), ((CraftPlayer) p).getHandle().ping, EnumWrappers.NativeGameMode.SURVIVAL, WrappedChatComponent.fromText(generalPlayer.getRank().getColor() + generalPlayer.getName())));
            });
            packet.setData(list);
            packet.sendPacket(event.getPlayer());
        }, 10);
    }
}
