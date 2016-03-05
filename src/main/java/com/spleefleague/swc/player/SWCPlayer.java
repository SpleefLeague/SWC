/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.spleefleague.swc.player;

import com.spleefleague.core.player.GeneralPlayer;
import com.spleefleague.swc.SWC;
import com.spleefleague.swc.bracket.Participant;
import com.spleefleague.swc.game.Battle;
import org.bukkit.Location;

/**
 *
 * @author Jonas
 */
public class SWCPlayer extends GeneralPlayer {

    private Location disconnectLocation;
    private boolean ingame, frozen, requestingReset, requestingEndgame, ready;
    
    public Participant getTournamentParticipant() {
        return Participant.getByMCID(this.getUniqueId());
    }
    
    public void setIngame(boolean ingame) {
        this.ingame = ingame;
    }
    
    public boolean isIngame() {
        return ingame;
    }
    
    public void setFrozen(boolean frozen) {
        this.frozen = frozen;
    }
    
    public boolean isFrozen() {
        return frozen;
    }
    
    public void setRequestingReset(boolean requestingReset) {
        this.requestingReset = requestingReset;
    }
    
    public boolean isRequestingReset() {
        return requestingReset;
    }
    
    public void setRequestingEndgame(boolean requestingEndgame) {
        this.requestingEndgame = requestingEndgame;
    }
    
    public boolean isRequestingEndgame() {
        return requestingEndgame;
    }
    
    public void setReady(boolean ready) {
        this.ready = ready;
    }

    public boolean isReady() {
        return ready;
    }
    
    public Battle getCurrentBattle() {
        if(SWC.getInstance().getBattleManager().isIngame(this)) {  
            return SWC.getInstance().getBattleManager().getBattle(this);
        }
        return null;
    }

    public Location getDisconnectLocation() {
        return disconnectLocation;
    }

    public void setDisconnectLocation(Location disconnectLocation) {
        this.disconnectLocation = disconnectLocation;
    }

    @Override
    public void setDefaults() {
        super.setDefaults();
        this.frozen = false;
        this.ingame = false;
        this.ready = false;
        this.requestingEndgame = false;
        this.requestingReset = false;
        this.disconnectLocation = null;
    }
}