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
import com.spleefleague.swc.bracket.Reference.ReferenceConverter;
import java.util.HashSet;
import java.util.UUID;

/**
 *
 * @author Jonas
 */
public class Battle extends DBEntity implements DBLoadable, DBSaveable{
    
    @DBLoad(fieldName = "uuid", typeConverter = UUIDStringConverter.class)
    @DBSave(fieldName = "uuid", typeConverter = UUIDStringConverter.class)
    private UUID uuid;
    @DBLoad(fieldName = "participants", typeConverter = ReferenceConverter.class)
    @DBSave(fieldName = "participants")
    private Reference[] participants;
    @DBLoad(fieldName = "score")
    @DBSave(fieldName = "score")
    private Score score;
    private boolean isRunning;
    @DBLoad(fieldName = "reported")
    @DBSave(fieldName = "reported")
    private boolean reported = false;
    
    public UUID getUUID() {
        return uuid;
    }
    
    public boolean isReported() {
        return reported;
    }
    
    public void setReported(boolean reported) {
        this.reported = reported;
    }
    
    public Participant getFirst() {
        if(isOver()) {
            return score.getFirst();
            
        }
        return null;
    }
    
    public Participant getSecond() {
        if(isOver()) {
            return score.getSecond();
        }
        return null;
    }
    
    public Participant[] getParticipants() {
        Participant[] participants = new Participant[this.participants.length];
        for(int i = 0; i < participants.length; i++) {
            participants[i] = this.participants[i].getReferenced();
        }
        return participants;
    }
    
    public Score getScore() {
        if(score == null) {
            score = new Score();
        }
        return score;
    }
    
    public boolean isOver() {
        return getScore().isOver();
    }
    
    public void end(Participant winner) {
        this.isRunning = false;
        getScore().end(winner);
    }
    
    public boolean isRunning() {
        return this.isRunning;
    }
    
    public void start() {
        this.isRunning = true;
        this.getScore().reset();
    }
    
    public boolean isOpen() {
        if(!isOver() && !isRunning()) {
            for(Participant participant : getParticipants()) {
                if(participant == null) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
    
    public void reset() {
        score = null;
        isRunning = false;
    }
    
    public Reference[] getReferences() {
        return participants;
    }
    
    @Override
    public void done() {
        battles.add(this);
    }
    
    protected static class BattleReference extends DBEntity implements DBLoadable, DBSaveable {
        
        @DBLoad(fieldName = "uuid", typeConverter = UUIDStringConverter.class)
        @DBSave(fieldName = "uuid", typeConverter = UUIDStringConverter.class)
        private UUID uuid;
        private Battle battle;
        
        public BattleReference() {
            
        }
        
        public BattleReference(UUID uuid) {
            this.uuid = uuid;
        }
        
        public BattleReference(Battle battle) {
            this.battle = battle;
        }
        
        private void retrieve() {
            battle = Battle.getByUUID(uuid);
        }
        
        public Battle getBattle() {
            if(battle == null) {
                retrieve();
            }
            return battle;
        }
    }
    
    private static final HashSet<Battle> battles = new HashSet<>();
    
    public static Battle getByUUID(UUID id) {
        for(Battle battle : battles) {
            if(battle.getUUID().equals(id)) {
                return battle;
            }
        }
        return null;
    }
}