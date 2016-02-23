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
import com.spleefleague.swc.bracket.Reference.RootReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;
import org.bson.Document;

/**
 *
 * @author Jonas
 */
public class Score extends DBEntity implements DBLoadable, DBSaveable {
    
    @DBLoad(fieldName = "scores", typeConverter = HashMapReferenceScoreConverter.class)
    @DBSave(fieldName = "scores", typeConverter = HashMapReferenceScoreConverter.class)
    private final HashMap<Reference, Integer> scores = new HashMap<>();
    @DBSave(fieldName = "isOver")
    @DBLoad(fieldName = "isOver")
    private boolean isOver = false;
    
    public void end(){
        isOver = true;
    }
    
    protected void reset() {
        isOver = false;
        scores.clear();
    }
    
    public void setScore(int score, Participant p) {
        for(Reference r : scores.keySet()) {
            if(r.getReferenced() == p) {
                scores.put(r, score);
                return;
            }
        }
        setScore(score, new RootReference(p.getPID()));
    }
    
    public void setScore(int score, Reference r) {
        if(!isOver){
            if(scores.containsKey(r))
                scores.remove(r);
            scores.put(r, score);
        }
    }
    
    public int getScore(Participant p) {
        for(Reference r : scores.keySet()) {
            if(r.getReferenced() == p) {
                return scores.get(r);
            }
        }
        return -1;
    }
    
    public Reference getWinner() {
        Reference winner = null;
        if(isOver){
            for(Reference r : scores.keySet()) {
                if(winner == null) {
                    winner = r;
                }
                else {
                    if(scores.get(r) > scores.get(winner)) {
                        winner = r;
                    }
                }
            }
        }
        return winner;
    }
    
    public Reference getLoser() {
        Reference loser = null;
        if(isOver){
            for(Reference r : scores.keySet()) {
                if(loser == null) {
                    loser = r;
                }
                else {
                    if(scores.get(r) <= scores.get(loser)) {
                        loser = r;
                    }
                }
            }
        }
        return loser;
    }
    
    public boolean isOver() {
        return isOver;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Score{");
        for(Reference reference : scores.keySet()) {
            sb.append(reference.getReferenced()).append(":").append(scores.get(reference)).append(" | ");
        }
        if(scores.size() > 0)
            sb.replace(sb.length() - 3, sb.length(), "");
        sb.append("}");
        return sb.toString();
    }
    
    public static class HashMapReferenceScoreConverter extends TypeConverter<List<Document>, HashMap<Reference, Integer>> {

        @Override
        public HashMap<Reference, Integer> convertLoad(List<Document> t) {
            HashMap<Reference, Integer> map = new HashMap<>();
            for(Document document : t) {
                Document refdoc = document.get("reference", Document.class);
                Reference ref;
                if(refdoc.containsKey("player")) {
                    ref = EntityBuilder.load(refdoc, RootReference.class);
                }
                else {
                    ref = EntityBuilder.load(refdoc, Reference.class);
                }
                map.put(ref, document.get("score", Integer.class));
            }
            return map;
        }

        @Override
        public List<Document> convertSave(HashMap<Reference, Integer> v) {
            List<Document> list = new ArrayList<>();
            for(Entry<Reference, Integer> e : v.entrySet()) {
                Document doc = new Document("score", e.getValue());
                Document ref = EntityBuilder.serialize(e.getKey()).get("$set", Document.class);
                doc.put("reference", ref);
                list.add(doc);
            }
            return list;
        }
    }
}