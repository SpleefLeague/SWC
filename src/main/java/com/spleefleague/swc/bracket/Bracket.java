/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.spleefleague.swc.bracket;

import com.spleefleague.core.io.DBEntity;
import com.spleefleague.core.io.DBLoad;
import com.spleefleague.core.io.DBLoadable;
import com.spleefleague.core.io.DBSave;
import com.spleefleague.core.io.DBSaveable;
import com.spleefleague.core.io.TypeConverter.UUIDStringConverter;
import com.spleefleague.swc.SWC;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

/**
 *
 * @author Jonas
 */
public class Bracket extends DBEntity implements DBLoadable, DBSaveable{
    
    @DBLoad(fieldName = "bracketID", typeConverter = UUIDStringConverter.class)
    @DBSave(fieldName = "bracketID", typeConverter = UUIDStringConverter.class)
    private UUID uuid;
    @DBLoad(fieldName = "hasStarted")
    @DBSave(fieldName = "hasStarted")
    private boolean hasStarted = false;
    @DBLoad(fieldName = "participants")
    @DBSave(fieldName = "participants")
    private ArrayList<Participant> participants;
    @DBLoad(fieldName = "battles")
    @DBSave(fieldName = "battles")
    private Battle[] battles;
    
    public ArrayList<Participant> getParticipants() {
        return participants;
    }
    
    public Battle[] getBattles() {
        return battles;
    }
    
    public Battle[] getOpenBattles() {
        Collection<Battle> open = new ArrayList<>();
        for(Battle battle : battles) {
            if(battle.isOpen()) {
                open.add(battle);
            }
        }
        return open.toArray(new Battle[open.size()]);
    }
    
    public Battle[] getPlayableBattles() {
        Collection<Battle> open = new ArrayList<>();
btls:   for(Battle battle : battles) {
            if(battle.isOpen()) {
                for(Participant p : battle.getParticipants()) {
                    if(SWC.getInstance().getPlayerManager().get(p.getMCID()) == null) {
                        continue btls;//Forgive me
                    }
                }
                open.add(battle);
            }
        }
        return open.toArray(new Battle[open.size()]);
    }
}