/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.spleefleague.swc.game;

import com.spleefleague.swc.player.SWCPlayer;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Jonas
 */
public class SWCBattleManager {

    private final Set<Battle> activeBattles;
    private final SWCQueue gameQueue;
    
    public SWCBattleManager() {
        activeBattles = new HashSet<>();
        gameQueue = new SWCQueue();
    }
    
    public void add(Battle battle) {
        activeBattles.add(battle);
    }
    
    public void remove(Battle battle) {
        activeBattles.remove(battle);
    }
    
    public Collection<Battle> getAll() {
        return activeBattles;
    }
    
    public Battle getBattle(SWCPlayer player) {
        for(Battle battle : activeBattles) {
            for(SWCPlayer p : battle.getActivePlayers()) {
                if(player == p) {
                    return battle;
                }
            }
        }
        return null;
    }
    
    public Battle getBattle(Arena arena) {
        for(Battle battle : activeBattles) {
            if(battle.getArena() == arena) {
                return battle;
            }
        }
        return null;
    }
    
    public boolean isIngame(SWCPlayer p) {
        return getBattle(p) != null;
    }
}
