package net.aevummc.agriculture.command;

import net.aevummc.agriculture.AevumAgriculture;
import net.aevummc.agriculture.model.AgricultureRegistry;
import net.aevummc.agriculture.service.ItemFactory;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

public final class AgricultureCommand implements CommandExecutor, TabCompleter {
    private final AevumAgriculture plugin;
    public AgricultureCommand(AevumAgriculture plugin){this.plugin=plugin;}

    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args){
        if(args.length==0){if(sender instanceof Player player)plugin.gui().openMain(player);else help(sender);return true;}
        String sub=args[0].toLowerCase(Locale.ROOT);
        if(sub.equals("help")){help(sender);return true;}
        if(sub.equals("admin")){admin(sender,args);return true;}
        if(!(sender instanceof Player player)){sender.sendMessage("This command requires a player unless using /agri admin.");return true;}
        switch(sub){
            case "menu" -> plugin.gui().openMain(player);
            case "shop", "seeds" -> plugin.gui().openShop(player,"all",0);
            case "crops", "codex" -> plugin.gui().openCropBook(player,0);
            case "recipes", "food" -> plugin.gui().openRecipeBook(player,0);
            case "market", "exchange" -> plugin.gui().openMarket(player,0);
            case "stats", "profile" -> plugin.gui().openStats(player);
            case "top", "leaderboard" -> plugin.gui().openTop(player);
            case "stations" -> plugin.gui().openStationDirectory(player);
            case "directory", "stores" -> plugin.gui().openBusinessDirectory(player);
            case "business" -> business(player,args);
            case "give" -> {if(player.hasPermission("aevumagriculture.admin.give"))admin(sender,new String[]{"admin","give",player.getName(),args.length>1?args[1]:"",args.length>2?args[2]:"1"});else player.sendMessage(plugin.message("No permission."));}
            default -> help(sender);
        }
        return true;
    }

    private void business(Player player,String[] args){
        if(args.length==1){plugin.gui().openBusiness(player);return;}
        String action=args[1].toLowerCase(Locale.ROOT);
        switch(action){
            case "register" -> {
                if(args.length<4){player.sendMessage(plugin.message("Usage: /agri business register <farmstead|bakery|mill|orchard|kitchen|grocer> <business name>"));return;}
                String name=String.join(" ",Arrays.copyOfRange(args,3,args.length));plugin.businesses().register(player,args[2],name);
            }
            case "stock" -> {
                if(args.length<3){player.sendMessage(plugin.message("Usage: /agri business stock <item-id> [amount]"));return;}
                int amount=0;if(args.length>3)try{amount=Integer.parseInt(args[3]);}catch(NumberFormatException ignored){}
                plugin.businesses().deposit(player,args[2],amount);
            }
            case "price" -> {
                if(args.length<4){player.sendMessage(plugin.message("Usage: /agri business price <item-id> <price>"));return;}
                try{plugin.businesses().setPrice(player,args[2],Double.parseDouble(args[3]));}catch(NumberFormatException ex){player.sendMessage(plugin.message("Enter a valid numeric price."));}
            }
            case "open" -> {var b=plugin.database().business(player.getUniqueId());if(b==null)player.sendMessage(plugin.message("You do not own a business."));else{plugin.database().setBusinessOpen(player.getUniqueId(),true);player.sendMessage(plugin.message("Your business is now open."));plugin.gui().openBusiness(player);}}
            case "close" -> {var b=plugin.database().business(player.getUniqueId());if(b==null)player.sendMessage(plugin.message("You do not own a business."));else{plugin.database().setBusinessOpen(player.getUniqueId(),false);player.sendMessage(plugin.message("Your business is now closed."));plugin.gui().openBusiness(player);}}
            case "visit" -> {
                if(args.length<3){player.sendMessage(plugin.message("Usage: /agri business visit <player>"));return;}
                OfflinePlayer target=Bukkit.getOfflinePlayer(args[2]);var business=plugin.database().business(target.getUniqueId());
                if(business==null){player.sendMessage(plugin.message("No agricultural business belongs to that player."));return;}
                plugin.gui().openBusinessStore(player,business.owner());
            }
            default -> {player.sendMessage(plugin.message("Business commands: register, stock, price, open, close, visit."));plugin.gui().openBusiness(player);}
        }
    }

    private void admin(CommandSender sender,String[] args){
        if(args.length<2){sender.sendMessage("/agri admin <reload|give|station|stats|xp>");return;}
        String action=args[1].toLowerCase(Locale.ROOT);
        switch(action){
            case "reload" -> {if(!sender.hasPermission("aevumagriculture.admin.reload")){sender.sendMessage(plugin.message("No permission."));return;}plugin.reloadAgriculture();sender.sendMessage(plugin.message("Agriculture catalogues reloaded."));}
            case "stats" -> {
                if(!sender.hasPermission("aevumagriculture.admin.stats")){sender.sendMessage(plugin.message("No permission."));return;}
                sender.sendMessage(plugin.message("Catalogue: "+plugin.registry().crops().size()+" crops, "+plugin.registry().goods().size()+" goods, "+plugin.registry().recipes().size()+" recipes."));
                sender.sendMessage(plugin.message("Economy: "+(plugin.economy()==null?"offline":"Vault connected")+"; crop growth is timestamp-based."));
            }
            case "give" -> {
                if(!sender.hasPermission("aevumagriculture.admin.give")){sender.sendMessage(plugin.message("No permission."));return;}
                if(args.length<4){sender.sendMessage("/agri admin give <player> <item-id> [amount]");return;}
                Player target=Bukkit.getPlayerExact(args[2]);if(target==null){sender.sendMessage(plugin.message("That player must be online."));return;}
                int amount=1;if(args.length>4)try{amount=Math.max(1,Math.min(64,Integer.parseInt(args[4])));}catch(NumberFormatException ignored){}
                String id=args[3];ItemStack item;
                if(id.startsWith("seed_")){var crop=plugin.registry().crop(id.substring(5));item=crop==null?null:plugin.items().seed(crop,amount);}
                else if(id.startsWith("produce_")){var crop=plugin.registry().crop(id.substring(8));item=crop==null?null:plugin.items().produce(crop,amount);}
                else item=plugin.items().createById(id,amount);
                if(item==null){sender.sendMessage(plugin.message("Unknown item ID: "+id));return;}
                target.getInventory().addItem(item).values().forEach(left->target.getWorld().dropItemNaturally(target.getLocation(),left));
                sender.sendMessage(plugin.message("Gave "+amount+" × "+id+" to "+target.getName()+"."));
            }
            case "station" -> {
                if(!sender.hasPermission("aevumagriculture.admin.station")){sender.sendMessage(plugin.message("No permission."));return;}
                if(args.length<5||!args[2].equalsIgnoreCase("give")){sender.sendMessage("/agri admin station give <player> <station-id>");return;}
                Player target=Bukkit.getPlayerExact(args[3]);String id=args[4];if(target==null){sender.sendMessage(plugin.message("That player must be online."));return;}
                String material=plugin.getConfig().getString("stations."+id+".material");if(material==null){sender.sendMessage(plugin.message("Unknown station ID."));return;}
                String name=plugin.getConfig().getString("stations."+id+".name",id);
                ItemStack item=plugin.items().station(id,name,material,1);target.getInventory().addItem(item).values().forEach(left->target.getWorld().dropItemNaturally(target.getLocation(),left));
                sender.sendMessage(plugin.message("Gave a "+name+" kit to "+target.getName()+"."));
            }
            case "xp" -> {
                if(!sender.hasPermission("aevumagriculture.admin.xp")){sender.sendMessage(plugin.message("No permission."));return;}
                if(args.length<4){sender.sendMessage("/agri admin xp <player> <amount>");return;}
                Player target=Bukkit.getPlayerExact(args[2]);if(target==null){sender.sendMessage(plugin.message("That player must be online."));return;}
                try{long xp=Math.max(0,Long.parseLong(args[3]));var profile=plugin.database().addActivity(target.getUniqueId(),target.getName(),xp,0,0,0);sender.sendMessage(plugin.message("Awarded "+xp+" Agriculture XP. New level: "+profile.level()+"."));}catch(NumberFormatException ex){sender.sendMessage(plugin.message("Enter a valid XP amount."));}
            }
            default -> sender.sendMessage("/agri admin <reload|give|station|stats|xp>");
        }
    }

    private void help(CommandSender sender){
        sender.sendMessage(plugin.message("Player commands: /agri, /agri shop, /agri crops, /agri recipes, /agri market, /agri business, /agri directory, /agri stats, /agri top."));
        sender.sendMessage(plugin.message("Business: /agri business register <type> <name>, stock <item-id> [amount], price <item-id> <price>, open, close, visit <player>."));
        sender.sendMessage(plugin.message("Plant custom seeds on farmland. Right-click a crop to harvest it when mature. Bone meal is the only growth booster."));
    }

    @Override public List<String> onTabComplete(CommandSender sender,Command command,String alias,String[] args){
        if(args.length==1)return filter(List.of("menu","shop","seeds","crops","recipes","market","business","stats","top","stations","directory","stores","help","admin"),args[0]);
        if(args[0].equalsIgnoreCase("business")){
            if(args.length==2)return filter(List.of("register","stock","price","open","close","visit"),args[1]);
            if(args.length==3&&args[1].equalsIgnoreCase("register"))return filter(List.of("farmstead","bakery","mill","orchard","kitchen","grocer"),args[2]);
            if(args.length==3&&args[1].equalsIgnoreCase("stock"))return filter(itemIds(),args[2]);
            if(args.length==3&&args[1].equalsIgnoreCase("price"))return filter(itemIds(),args[2]);
            if(args.length==3&&args[1].equalsIgnoreCase("visit"))return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(),args[2]);
        }
        if(args[0].equalsIgnoreCase("admin")){
            if(args.length==2)return filter(List.of("reload","give","station","stats","xp"),args[1]);
            if(args.length==3&&args[1].equalsIgnoreCase("give"))return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(),args[2]);
            if(args.length==4&&args[1].equalsIgnoreCase("give"))return filter(itemIds(),args[3]);
            if(args.length==3&&args[1].equalsIgnoreCase("station"))return filter(List.of("give"),args[2]);
            if(args.length==4&&args[1].equalsIgnoreCase("station"))return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(),args[3]);
            if(args.length==5&&args[1].equalsIgnoreCase("station"))return filter(plugin.getConfig().getConfigurationSection("stations").getKeys(false).stream().toList(),args[4]);
        }
        return List.of();
    }
    private List<String> itemIds(){List<String> ids=new ArrayList<>();plugin.registry().crops().forEach(c->{ids.add("seed_"+c.id());ids.add("produce_"+c.id());});plugin.registry().goods().forEach(g->ids.add(g.id()));return ids;}
    private List<String> filter(List<String> values,String prefix){return values.stream().filter(v->v.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT))).sorted().collect(Collectors.toList());}
}
