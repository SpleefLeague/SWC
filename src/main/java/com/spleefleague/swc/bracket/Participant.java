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
import com.spleefleague.core.io.TypeConverter;
import java.util.HashSet;
import java.util.UUID;

/**
 *
 * @author Jonas
 */
public class Participant extends DBEntity implements DBLoadable, DBSaveable {

    @DBLoad(fieldName = "pid", typeConverter = TypeConverter.UUIDStringConverter.class)
    @DBSave(fieldName = "pid", typeConverter = TypeConverter.UUIDStringConverter.class)
    private UUID pid;
    @DBLoad(fieldName = "mcid", typeConverter = TypeConverter.UUIDStringConverter.class)
    @DBSave(fieldName = "mcid", typeConverter = TypeConverter.UUIDStringConverter.class)
    private UUID mcid;

    public Participant(UUID pid, UUID mcid) {
        this.pid = pid;
        this.mcid = mcid;
    }

    public UUID getPID() {
        return pid;
    }

    public UUID getMCID() {
        return mcid;
    }

    @Override
    public void done() {
        participants.add(this);
    }

    private static final HashSet<Participant> participants = new HashSet<>();

    public static Participant getByPID(UUID id) {
        for (Participant participant : participants) {
            if (participant.getPID().equals(id)) {
                return participant;
            }
        }
        return null;
    }

    public static Participant getByMCID(UUID id) {
        for (Participant participant : participants) {
            if (participant.getMCID().equals(id)) {
                return participant;
            }
        }
        return null;
    }
}
