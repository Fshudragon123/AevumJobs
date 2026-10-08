package net.aevummc.jobs.command;

import net.aevummc.jobs.AevumJobs;
import net.aevummc.jobs.config.ConfigManager;
import net.aevummc.jobs.gui.GuiManager;
import net.aevummc.jobs.job.JobType;
import net.aevummc.jobs.reward.RewardService;
import net.aevummc.jobs.service.JobService;
import net.aevummc.jobs.service.PlayerJobs;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public final class JobsCommand implements CommandExecutor {
    private final AevumJobs plugin;private final JobService jobs;private final ConfigManager config;private final GuiManager gui;private final RewardService rewards;
    public JobsCommand(AevumJobs p,JobService j,ConfigManager c,GuiManager g,RewardService r){plugin=p;jobs=j;config=c;gui=g;rewards=r;}
    @Override public boolean onCommand(CommandSender s,Command cmd,String label,String[] a){
        if(a.length==0||a[0].equalsIgnoreCase("menu")){if(!(s instanceof Player p)){s.sendMessage("Players only.");return true;}jobs.load(p.getUniqueId()).thenAccept(d->Bukkit.getScheduler().runTask(plugin,()->gui.openMain(p,d)));return true;}
        switch(a[0].toLowerCase()){
            case "help"->help(s);
            case "stats"->playerMenu(s,gui::openStats);
            case "leaderboard","top"->playerMenu(s,gui::openLeaderboard);
            case "info","level","rewards"->{if(a.length<2){s.sendMessage("§cUsage: /jobs "+a[0]+" <job>");return true;}JobType j=JobType.fromString(a[1]);if(j==null){s.sendMessage("§cUnknown profession. Use /jobs help.");return true;}playerMenu(s,(p,d)->{if(a[0].equalsIgnoreCase("rewards"))gui.openRewards(p,d,j,0);else gui.openProgression(p,d,j);});}
            case "admin"->admin(s,a);
            default->help(s);
        }return true;
    }
    private void playerMenu(CommandSender s,java.util.function.BiConsumer<Player,PlayerJobs> action){if(!(s instanceof Player p)){s.sendMessage("Players only.");return;}jobs.load(p.getUniqueId()).thenAccept(d->Bukkit.getScheduler().runTask(plugin,()->action.accept(p,d)));}
    private void admin(CommandSender s,String[] a){
        if(!s.hasPermission("aevumjobs.admin")){s.sendMessage("§cNo permission.");return;}if(a.length<2){help(s);return;}
        switch(a[1].toLowerCase()){
            case "reload"->{if(!s.hasPermission("aevumjobs.admin.reload")){s.sendMessage("§cNo permission.");return;}config.reload();s.sendMessage("§6§lAevumJobs §7» §aConfiguration reloaded safely.");}
            case "setlevel","addxp","removexp","reset"->{
                String node="aevumjobs.admin."+a[1].toLowerCase();if(!s.hasPermission(node)){s.sendMessage("§cNo permission.");return;}
                int needed=a[1].equalsIgnoreCase("reset")?4:5;if(a.length<needed){s.sendMessage("§cInvalid arguments.");return;}
                OfflinePlayer target=Bukkit.getOfflinePlayer(a[2]);JobType j=JobType.fromString(a[3]);if(j==null){s.sendMessage("§cUnknown profession.");return;}
                jobs.load(target.getUniqueId()).thenAccept(d->{try{
                    switch(a[1].toLowerCase()){case "setlevel"->d.get(j).setLevel(Math.max(0,Math.min(50,Integer.parseInt(a[4]))));case "addxp"->d.get(j).addXp(Double.parseDouble(a[4]));case "removexp"->d.get(j).setXp(Math.max(0,d.get(j).xp()-Double.parseDouble(a[4])));case "reset"->{d.get(j).setLevel(0);d.get(j).setXp(0);d.get(j).setTotalXp(0);d.get(j).setTotalEarned(0);d.get(j).setActions(0);d.get(j).setLevel50Rewarded(false);}}
                    jobs.save(d);s.sendMessage("§6§lAevumJobs §7» §aUpdated "+target.getName()+" / "+j.getDisplayName()+".");
                }catch(NumberFormatException ex){s.sendMessage("§cAmount/level must be a valid number.");}});
            }
            default->help(s);
        }
    }
    private void help(CommandSender s){s.sendMessage("§6§l══════════ AEVUMJOBS ══════════");s.sendMessage("§e/jobs §7Open professions");s.sendMessage("§e/jobs stats §7View statistics");s.sendMessage("§e/jobs leaderboard §7View leaderboards");s.sendMessage("§e/jobs info <job> §7View profession");s.sendMessage("§e/jobs level <job> §7View progression");s.sendMessage("§e/jobs rewards <job> §7View rewards");s.sendMessage("§e/jobs help §7Show this help");if(s.hasPermission("aevumjobs.admin")){s.sendMessage("§6/jobs admin setlevel <player> <job> <level>");s.sendMessage("§6/jobs admin addxp <player> <job> <amount>");s.sendMessage("§6/jobs admin removexp <player> <job> <amount>");s.sendMessage("§6/jobs admin reset <player> <job>");s.sendMessage("§6/jobs admin reload");}s.sendMessage("§6§l═══════════════════════════════");}
}