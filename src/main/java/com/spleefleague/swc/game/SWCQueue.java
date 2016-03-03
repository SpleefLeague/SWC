/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.spleefleague.swc.game;

import com.spleefleague.core.events.BattleStartEvent;
import com.spleefleague.core.player.PlayerManager;
import com.spleefleague.swc.SWC;
import com.spleefleague.swc.bracket.Participant;
import com.spleefleague.swc.player.SWCPlayer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

/**
 *
 * @author Jonas
 */
public class SWCQueue {

    private final Random random;
    private final BukkitTask tickTask;
    
    public SWCQueue() {
        this.random = new Random();
        tickTask = Bukkit.getScheduler().runTaskTimer(SWC.getInstance(), this::doTick, 0, 5 * 20);
    }
    
    private void doTick() {
        List<com.spleefleague.swc.bracket.Battle> battles = Arrays.asList(SWC.getInstance().getBracket().getPlayableBattles());
        PlayerManager<SWCPlayer> pm = SWC.getInstance().getPlayerManager();
        List<Arena> free = new ArrayList<>();
        for(Arena arena : Arena.getAll()) {
            if(!arena.isOccupied() && !arena.isPaused()) {
                free.add(arena);
            }
        }
        for(com.spleefleague.swc.bracket.Battle battle : battles) {
            List<SWCPlayer> participants = new ArrayList<>();
            boolean complete = true;
            for(Participant p : battle.getParticipants()) {
                SWCPlayer swcp = pm.get(p.getMCID());
                if(swcp != null && swcp.isOnline() && swcp.isReady() && !swcp.isIngame()) {
                    participants.add(swcp);
                }
                else {
                    complete = false;
                    break;
                }
            }
            if(complete) {
                Arena arena = free.get(random.nextInt(free.size()));
                arena.startBattle(participants, BattleStartEvent.StartReason.QUEUE, battle);
            }
        }
    }
}
