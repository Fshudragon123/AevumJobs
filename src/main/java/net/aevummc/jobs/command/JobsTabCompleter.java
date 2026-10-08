package net.aevummc.jobs.command;

import net.aevummc.jobs.job.JobType;
import org.bukkit.command.*;
import java.util.*;

public final class JobsTabCompleter implements TabCompleter {
    @Override public List<String> onTabComplete(CommandSender s,Command c,String l,String[] a){
        if(a.length==1)return partial(a[0],List.of("menu","stats","leaderboard","info","level","rewards","help","admin"));
        if(a.length==2&&List.of("info","level","rewards").contains(a[0].toLowerCase()))return partial(a[1],Arrays.stream(JobType.values()).map(JobType::key).toList());
        if(a.length==2&&a[0].equalsIgnoreCase("admin"))return partial(a[1],List.of("setlevel","addxp","removexp","reset","reload"));
        if(a.length==4&&a[0].equalsIgnoreCase("admin"))return Arrays.stream(JobType.values()).map(JobType::key).toList();
        return List.of();
    }
    private List<String> partial(String input,List<String> values){return values.stream().filter(x->x.toLowerCase().startsWith(input.toLowerCase())).toList();}
}