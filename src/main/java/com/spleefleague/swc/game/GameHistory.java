/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.spleefleague.swc.game;

import com.spleefleague.core.io.DBEntity;
import com.spleefleague.core.io.DBSave;
import com.spleefleague.core.io.DBSaveable;
import com.spleefleague.core.io.TypeConverter;
import com.spleefleague.swc.player.SWCPlayer;
import java.util.Collection;
import java.util.Date;
import java.util.UUID;

/**
 *
 * @author Jonas
 */
public class GameHistory extends DBEntity implements DBSaveable {

    @DBSave(fieldName = "players")
    private final PlayerData[] players;
    @DBSave(fieldName = "date")
    private final Date startDate;
    @DBSave(fieldName = "duration")
    private final int duration; //In ticks
    @DBSave(fieldName = "cancelled")
    private final boolean cancelled;
    @DBSave(fieldName = "arena")
    private final String arena;
    @DBSave(fieldName = "battleid", typeConverter = TypeConverter.UUIDStringConverter.class)
    private final UUID battleid;

    protected GameHistory(Battle battle, SWCPlayer winner, UUID battleid) {
        this.cancelled = winner == null;
        this.battleid = battleid;
        players = new PlayerData[battle.getPlayers().size()];
        Collection<SWCPlayer> activePlayers = battle.getActivePlayers();
        int i = 0;
        for (SWCPlayer sp : battle.getPlayers()) {
            players[i++] = new PlayerData(sp.getUniqueId(), battle.getData(sp).getPoints(), sp == winner, !activePlayers.contains(sp));
        }
        this.duration = battle.getDuration();
        startDate = new Date(System.currentTimeMillis() - this.duration * 50);
        this.arena = battle.getArena().getName();
    }

    public static class PlayerData extends DBEntity implements DBSaveable {

        @DBSave(fieldName = "uuid", typeConverter = TypeConverter.UUIDStringConverter.class)
        private final UUID uuid;
        @DBSave(fieldName = "points")
        private final int points;
        @DBSave(fieldName = "winner")
        private final Boolean winner;
        @DBSave(fieldName = "surrendered")
        private final Boolean surrendered;

        public PlayerData(UUID uuid, int points, boolean winner, boolean surrendered) {
            this.uuid = uuid;
            this.points = points;
            this.winner = winner;
            this.surrendered = surrendered;
        }
    }
}
