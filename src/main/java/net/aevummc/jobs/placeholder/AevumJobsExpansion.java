package net.aevummc.jobs.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.aevummc.jobs.AevumJobs;
import net.aevummc.jobs.job.JobType;
import net.aevummc.jobs.service.JobService;
import net.aevummc.jobs.service.PlayerJobs;
import org.bukkit.OfflinePlayer;

import java.util.Locale;

public final class AevumJobsExpansion extends PlaceholderExpansion {
    private final AevumJobs plugin; private final JobService service;
    public AevumJobsExpansion(AevumJobs plugin, JobService service){this.plugin=plugin;this.service=service;}
    @Override public String getIdentifier(){return "aevumjobs";} @Override public String getAuthor(){return "AevumMC";} @Override public String getVersion(){return plugin.getDescription().getVersion();} @Override public boolean persist(){return true;}
    @Override public String onRequest(OfflinePlayer player,String params){
        if(player==null)return "";
        PlayerJobs d=service.get(player.getUniqueId()); if(d==null)return "0";
        String[] p=params.toLowerCase(Locale.ROOT).split("_",2);
        if(p.length==1)return switch(p[0]){case "active_count"->String.valueOf(d.activeCount());case "highest_level"->String.valueOf(d.all().stream().mapToInt(x->x.level()).max().orElse(0));case "total_xp"->String.valueOf(d.all().stream().mapToLong(x->x.totalXp()).sum());default->null;};
        JobType job=JobType.fromString(p[0]); if(job==null)return null; var j=d.get(job);
        return switch(p[1]){case "level"->String.valueOf(j.level());case "xp"->String.valueOf(Math.round(j.xp()));case "next_xp"->String.valueOf(Math.round(plugin.getConfigManager().requiredXp(j.level())));case "progress"->String.format("%.1f",progress(j.level(),j.xp()));case "total_earned"->String.format("%.2f",j.totalEarned());case "actions"->String.valueOf(j.actions());default->null;};
    }
    private double progress(int level,double xp){if(level>=50)return 100;double req=plugin.getConfigManager().requiredXp(level);return req<=0?0:Math.min(100,xp/req*100);}
}