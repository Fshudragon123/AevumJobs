package net.aevummc.jobs.service;

import net.aevummc.jobs.AevumJobs;
import net.aevummc.jobs.antiabuse.AntiAbuseService;
import net.aevummc.jobs.config.ConfigManager;
import net.aevummc.jobs.job.JobData;
import net.aevummc.jobs.job.JobType;
import net.aevummc.jobs.reward.RewardService;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

public final class JobService {
    private final AevumJobs plugin; private final ConfigManager config; private final RewardService rewards; private final AntiAbuseService antiAbuse;
    private final Map<UUID, PlayerJobs> cache = new ConcurrentHashMap<>();
    public JobService(AevumJobs plugin, ConfigManager config, RewardService rewards, AntiAbuseService antiAbuse){this.plugin=plugin;this.config=config;this.rewards=rewards;this.antiAbuse=antiAbuse;}
    public CompletableFuture<PlayerJobs> load(UUID uuid){ PlayerJobs cached=cache.get(uuid); if(cached!=null)return CompletableFuture.completedFuture(cached); return plugin.getDatabase().load(uuid).thenApply(d->{cache.put(uuid,d);return d;}); }
    public PlayerJobs get(UUID uuid){return cache.get(uuid);} public void cache(PlayerJobs d){cache.put(d.uuid(),d);}
    public void unload(UUID uuid){PlayerJobs d=cache.remove(uuid); if(d!=null) plugin.getDatabase().save(d);}
    public void save(PlayerJobs d){plugin.getDatabase().save(d);}
    public boolean join(PlayerJobs d, JobType job){boolean ok=d.activate(job,config.activeLimit()); if(ok)save(d);return ok;}
    public boolean leave(PlayerJobs d, JobType job){boolean ok=d.leave(job); if(ok && !config.preserveOnLeave()){JobData x=d.get(job);x.setLevel(0);x.setXp(0);x.setTotalXp(0);x.setTotalEarned(0);x.setActions(0);x.setLevel50Rewarded(false);} if(ok)save(d);return ok;}
    public void addXp(Player player, JobType job, double amount){
        if(amount<=0)return; PlayerJobs d=cache.get(player.getUniqueId()); if(d==null || !d.isActive(job))return;
        JobData jd=d.get(job); synchronized(jd){
            jd.addXp(amount); jd.addActions(1);
            while(jd.level()<50 && jd.xp()>=config.requiredXp(jd.level())){
                double req=config.requiredXp(jd.level()); jd.setXp(jd.xp()-req); jd.setLevel(jd.level()+1);
                rewards.handleLevelReward(player,d,job,jd.level());
            }
        }
        save(d);
    }
    public void addXp(Player player, JobType job, double amount, Object ignored){addXp(player,job,amount);}
    public double warriorMultiplier(UUID killer, UUID victim){return antiAbuse.warriorMultiplier(killer,victim,System.currentTimeMillis());}
}