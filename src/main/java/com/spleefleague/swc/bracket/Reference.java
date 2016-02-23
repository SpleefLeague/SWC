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
import com.spleefleague.core.io.EntityBuilder;
import com.spleefleague.core.io.TypeConverter;
import com.spleefleague.core.io.TypeConverter.UUIDStringConverter;
import com.spleefleague.swc.bracket.Battle.BattleReference;
import java.util.UUID;
import org.bson.Document;

/**
 *
 * @author Jonas
 */
public class Reference extends DBEntity implements DBLoadable, DBSaveable {
    
    @DBLoad(fieldName = "winner")
    @DBSave(fieldName = "winner")
    public Boolean winner;
    @DBLoad(fieldName = "battle")
    @DBSave(fieldName = "battle")
    public BattleReference battle;
    
    public boolean isWinner() {
        return winner;
    }
    
    public Participant getReferenced() {
        return (winner) ? battle.getBattle().getWinner() : battle.getBattle().getLoser();
    }
    
    public static class RootReference extends Reference {
        
        @DBLoad(fieldName = "player", typeConverter = UUIDStringConverter.class)
        @DBSave(fieldName = "player", typeConverter = UUIDStringConverter.class)
        private UUID referenced;
        
        public RootReference() {
            
        }
        
        public RootReference(UUID referenced) {
            this.referenced = referenced;
        }
        
        @Override
        public Participant getReferenced() {
            return Participant.getByPID(referenced);
        }
    }
    
    public static class ReferenceConverter extends TypeConverter<Document, Reference> {

        @Override
        public Reference convertLoad(Document t) {
            if(t.containsKey("player")) {
                return EntityBuilder.deserialize(t, RootReference.class);
            }
            else {
                return EntityBuilder.deserialize(t, Reference.class);
            }
        }

        @Override
        public Document convertSave(Reference v) {
            return EntityBuilder.serialize(v);
        }
    }
}
