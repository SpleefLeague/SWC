/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.spleefleague.swc.game;

import com.mongodb.client.MongoCursor;
import com.spleefleague.core.SpleefLeague;
import com.spleefleague.core.events.BattleStartEvent.StartReason;
import com.spleefleague.core.io.DBEntity;
import com.spleefleague.core.io.DBLoad;
import com.spleefleague.core.io.DBLoadable;
import com.spleefleague.core.io.DBSave;
import com.spleefleague.core.io.DBSaveable;
import com.spleefleague.core.io.EntityBuilder;
import com.spleefleague.core.io.TypeConverter;
import com.spleefleague.core.listeners.FakeBlockHandler;
import com.spleefleague.core.player.SLPlayer;
import com.spleefleague.core.queue.QueueableArena;
import com.spleefleague.core.utils.Area;
import com.spleefleague.core.utils.FakeArea;
import com.spleefleague.core.utils.FakeBlock;
import com.spleefleague.core.utils.function.Dynamic;
import com.spleefleague.swc.SWC;
import com.spleefleague.swc.game.scoreboards.Scoreboard;
import com.spleefleague.swc.player.SWCPlayer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

/**
 *
 * @author Jonas
 */
public class Arena extends DBEntity implements DBLoadable, DBSaveable, QueueableArena<SWCPlayer> {
    
    @DBLoad(fieldName = "border")
    private Area border;
    private Area[] field;
    @DBLoad(fieldName = "spawns", typeConverter = TypeConverter.LocationConverter.class)
    private Location[] spawns;
    @DBLoad(fieldName = "creator")
    private String creator;
    @DBLoad(fieldName = "name")
    private String name;
    @DBLoad(fieldName = "queued")
    private boolean queued = true;
    @DBLoad(fieldName = "tpBackSpectators")
    private boolean tpBackSpectators = true;
    @DBLoad(fieldName = "scoreboards")
    private Scoreboard[] scoreboards;
    @DBLoad(fieldName = "paused")
    @DBSave(fieldName = "paused")
    private boolean paused = false;
    @DBLoad(fieldName = "spectatorSpawn", typeConverter = TypeConverter.LocationConverter.class)
    private Location spectatorSpawn; //null -> default world spawn
    @DBLoad(fieldName = "maxRating")
    private int maxRating = 5;
    @DBLoad(fieldName = "area")
    private Area area;
    private int runningGames = 0;
    private FakeArea defaultSnow;
    
    
    public Location[] getSpawns() {
        return spawns;
    }
    
    public Area getBorder() {
        return border;
    }
    
    public FakeArea getDefaultSnow() {
        return defaultSnow;
    }
    
    @DBLoad(fieldName = "field")
    public void setField(Area[] field) {
        this.field = field;
        defaultSnow = new FakeArea();
        for(Area area : field) {
            for(Block block : area.getBlocks()) {
                defaultSnow.addBlock(new FakeBlock(block.getLocation(), Material.SNOW_BLOCK));
            }
        }
        FakeBlockHandler.addArea(defaultSnow, false, Bukkit.getOnlinePlayers().toArray(new Player[0]));
    }
    
    public Area[] getField() {
        return field;
    }
    
    public Location getSpectatorSpawn() {
        return spectatorSpawn;
    }
    
    public String getCreator() {
        return creator;
    }
    
    public Scoreboard[] getScoreboards() {
        return scoreboards;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public boolean isOccupied() {
        return false;
    }
    
    public int getRunningGamesCount() {
        return runningGames;
    }
    
    public void registerGameStart() {
        runningGames++;
    }
    
    public void registerGameEnd() {
        runningGames--;
    }
    
    public Area getArea() {
        return area;
    }
    
    public boolean isRated() {
        return false;
    }
    
    public boolean isTpBackSpectators() {
        return tpBackSpectators;
    }
    
    @Override
    public boolean isPaused() {
        return paused;
    }
    
    public void setPaused(boolean paused) {
        this.paused = paused;
    }
    
    public int getMaxRating() {
        return maxRating;
    }
    
    @Override
    public int getSize() {
        return spawns.length;
    }
    
    public Dynamic<List<String>> getDynamicDescription() {
        return (SLPlayer slp) -> {
            List<String> description = new ArrayList<>();
            SWCPlayer sjp = SWC.getInstance().getPlayerManager().get(slp.getUniqueId());
            if(Arena.this.isAvailable(sjp)) {
                if(Arena.this.isPaused()) {
                    description.add(ChatColor.RED + "This arena is");
                    description.add(ChatColor.RED + "currently paused.");
                }
                else if(getRunningGamesCount() == 0) {
                    description.add(ChatColor.DARK_GRAY + "" + ChatColor.ITALIC + "Click to join the queue");
                }
            }
            else {
                description.add(ChatColor.RED + "You have not discovered");
                description.add(ChatColor.RED + "this arena yet.");
            }
            return description;
        };
    }

    @Override
    public boolean isAvailable(SWCPlayer sp) {
        return true;
    }
    
    @Override
    public boolean isQueued() {
        return queued;
    }
    
    public Battle startBattle(List<SWCPlayer> players, StartReason reason, com.spleefleague.swc.bracket.Battle bracketBattle) {
        if(!isOccupied()) { //Shouldn't be necessary
            Battle battle = new Battle(this, players, bracketBattle);
            battle.start(reason);
            return battle;
        }
        return null;
    }
    
    private static Map<String, Arena> arenas;
    
    public static Arena byName(String name) {
        Arena arena = arenas.get(name);
        if(arena == null) {
            for(Arena a : arenas.values()) {
                if(a.getName().equalsIgnoreCase(name)) {
                    arena = a;
                }
            }
        }
        return arena;
    }
    
    public static Collection<Arena> getAll() {
        return arenas.values();
    }
    
    public static void init(){
        arenas = new HashMap<>();
        MongoCursor<Document> dbc = SpleefLeague.getInstance().getMongo().getDatabase("SuperSpleef").getCollection("Arenas").find(new Document("swc", true)).iterator();
        while(dbc.hasNext()) {
            Arena arena = EntityBuilder.load(dbc.next(), Arena.class);
            arenas.put(arena.getName(), arena);
        }
        SWC.getInstance().log("Loaded " + arenas.size() + " arenas!");
    }
}
