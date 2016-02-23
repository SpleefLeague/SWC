/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.spleefleague.swc.listener;

import com.spleefleague.core.SpleefLeague;
import com.spleefleague.core.listeners.FakeBlockHandler;
import com.spleefleague.core.player.PlayerState;
import com.spleefleague.core.player.SLPlayer;
import com.spleefleague.core.utils.PlayerUtil;
import com.spleefleague.swc.SWC;
import com.spleefleague.swc.game.Arena;
import com.spleefleague.swc.player.SWCPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;



/**
 *
 * @author Jonas
 */
public class EnvironmentListener implements Listener {
    
    private static Listener instance;
    
    public static void init() {
        if(instance == null) {
            instance = new EnvironmentListener();
            Bukkit.getPluginManager().registerEvents(instance, SWC.getInstance());
        }
    }
    
    private EnvironmentListener() {
        
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onJoin(PlayerJoinEvent event) {
        for(Arena arena : Arena.getAll()) {
            FakeBlockHandler.addArea(arena.getDefaultSnow(), false, event.getPlayer());
        }
    }
}
