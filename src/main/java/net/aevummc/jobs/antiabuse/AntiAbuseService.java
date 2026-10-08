package net.aevummc.jobs.antiabuse;

import net.aevummc.jobs.config.ConfigManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class AntiAbuseService {
    private final ConfigManager config;
    private final Map<UUID,Map<UUID,KillState>> kills=new ConcurrentHashMap<>();
    public AntiAbuseService(ConfigManager config,net.aevummc.jobs.database.Database ignored){this.config=config;}

    public synchronized double warriorMultiplier(UUID killer,UUID victim,long now){
        if(killer.equals(victim))return 0;
        long cooldown=config.config().getLong("anti-abuse.pvp.cooldown-seconds",120)*1000L;
        long reciprocalWindow=config.config().getLong("anti-abuse.pvp.reciprocal-window-seconds",20)*1000L;
        int maxSame=Math.max(1,config.config().getInt("anti-abuse.pvp.max-xp-from-same-victim",1));
        Map<UUID,KillState> killerMap=kills.computeIfAbsent(killer,k->new HashMap<>());
        Map<UUID,KillState> victimMap=kills.computeIfAbsent(victim,k->new HashMap<>());
        KillState prior=killerMap.get(victim);
        KillState reciprocal=victimMap.get(killer);
        if(reciprocal!=null&&now-reciprocal.lastKill<=reciprocalWindow)return 0.0;
        if(prior==null||now-prior.lastKill>=cooldown){killerMap.put(victim,new KillState(now,1));return 1.0;}
        if(prior.count>=maxSame)return 0.0;
        prior.count++;prior.lastKill=now;
        return config.config().getDouble("anti-abuse.pvp.repeated-kill-xp-multiplier",0.0);
    }

    public boolean validHunterEntity(org.bukkit.entity.Entity entity){
        if(entity instanceof org.bukkit.entity.Player)return false;
        if(entity.hasMetadata("NPC")||entity.hasMetadata("npc")||entity.hasMetadata("CitizensNPC")||entity.hasMetadata("AevumFakeEntity"))return false;
        String type=entity.getType().name();
        Set<String> invalid=new HashSet<>(config.config().getStringList("anti-abuse.hunter.invalid-entities"));
        return !invalid.contains(type);
    }
    private static final class KillState{long lastKill;int count;KillState(long lastKill,int count){this.lastKill=lastKill;this.count=count;}}
}