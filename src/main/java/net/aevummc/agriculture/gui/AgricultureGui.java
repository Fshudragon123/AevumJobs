package net.aevummc.agriculture.gui;

import net.aevummc.agriculture.AevumAgriculture;
import net.aevummc.agriculture.data.AgricultureDatabase;
import net.aevummc.agriculture.model.AgricultureRegistry;
import net.aevummc.agriculture.service.ItemFactory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.*;

/** Imperial Agrarian Ledger menus: deliberately empty slot wells let the custom pack artwork show through. */
public final class AgricultureGui implements Listener {
    private static final TextColor GOLD=TextColor.fromHexString("#D4972C");
    private static final TextColor PALE=TextColor.fromHexString("#F3EBDD");
    private static final TextColor MUTED=TextColor.fromHexString("#A8A095");
    private static final TextColor GREEN=TextColor.fromHexString("#75C78D");
    private static final TextColor RED=TextColor.fromHexString("#E05252");
    private static final int[] CONTENT={10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31,32,33,34,37,38,39,40,41,42,43};
    private final AevumAgriculture plugin;
    private final NamespacedKey actionKey;

    public AgricultureGui(AevumAgriculture plugin){this.plugin=plugin;this.actionKey=new NamespacedKey(plugin,"gui_action");}

    private static final class Holder implements InventoryHolder {
        private final String id; private Inventory inventory;
        private Holder(String id){this.id=id;}
        public String id(){return id;}
        @Override public Inventory getInventory(){return inventory;}
        void inventory(Inventory inventory){this.inventory=inventory;}
    }

    private Inventory create(String id,String title){Holder holder=new Holder(id);Inventory inv=Bukkit.createInventory(holder,54,Component.text(title,GOLD));holder.inventory(inv);return inv;}
    private Component line(String text,TextColor color){return Component.text(text,color);}
    private ItemStack button(String icon,String name,String action,String... lore){return plugin.items().gui(icon,name,action,Arrays.asList(lore));}
    private void open(Player player,Inventory inv){player.openInventory(inv);player.playSound(player.getLocation(),Sound.UI_BUTTON_CLICK,0.35f,1.12f);}

    public void openMain(Player player){
        AgricultureDatabase.Profile profile=plugin.database().profile(player.getUniqueId(),player.getName());
        AgricultureDatabase.Business business=plugin.database().business(player.getUniqueId());
        Inventory inv=create("main","AEVUM  /  IMPERIAL AGRARIAN LEDGER");
        inv.setItem(4,button("ledger","THE AGRARIAN LEDGER",null,"A record of cultivation, harvest and trade.","Farmer: "+player.getName(),"Agriculture Level: "+profile.level()+" / 50","Lifetime harvests: "+profile.harvests(),"XP: "+profile.xp()));
        inv.setItem(20,button("seed","THE IMPERIAL SEED EXCHANGE","menu:shop","Browse and purchase all enabled crop seeds.","119 crop varieties in the current catalogue."));
        inv.setItem(22,button("crops","THE CROP CODEX","menu:crops","Inspect crops, growing times, origins and harvest yields."));
        inv.setItem(24,button("recipes","THE CULINARY COMPENDIUM","menu:recipes","Explore 78 production and cooking recipes.","Discover mills, presses, ovens and kitchens."));
        inv.setItem(29,button("market","IMPERIAL COMMODITY EXCHANGE","menu:market","Sell harvested produce and finished goods.","Prices respond to recent market supply."));
        inv.setItem(31,button("business","AGRICULTURAL BUSINESSES","menu:business","Register a farmstead, bakery, mill, orchard, kitchen or grocer.",business==null?"No business registered yet.":"Your business: "+business.name()));
        inv.setItem(33,button("stations","PRODUCTION STATIONS","menu:stations","Inspect and purchase the eight specialist production stations."));
        inv.setItem(38,button("business","BUSINESS DIRECTORY","menu:directory","Discover player-run farmsteads, bakeries, mills, orchards and kitchens."));
        inv.setItem(40,button("profile","FARMER'S RECORD","menu:stats","Your farming level, harvests, recipes and sales."));
        inv.setItem(44,button("laurel","TOP FARMERS","menu:top","The most accomplished cultivators and producers."));
        inv.setItem(49,button("help","HOW TO FARM", "menu:help","Plant seeds on farmland.","Right-click your crop to harvest when mature.","Use BONE MEAL as the only growth booster.","Use stations to make ingredients and food."));
        inv.setItem(53,button("close","CLOSE",null,"Close the Agrarian Ledger."));
        open(player,inv);
    }

    public void openShop(Player player,String category,int requestedPage){
        String filter=category==null?"all":category.toLowerCase(Locale.ROOT);
        List<AgricultureRegistry.Crop> list=plugin.registry().cropsByCategory(filter);
        int pageSize=CONTENT.length;int pages=Math.max(1,(list.size()+pageSize-1)/pageSize);int page=Math.max(0,Math.min(requestedPage,pages-1));
        Inventory inv=create("shop:"+filter+":"+page,"SEED EXCHANGE  /  "+filter.toUpperCase(Locale.ROOT));
        inv.setItem(4,button("seed","THE IMPERIAL SEED EXCHANGE",null,"Every enabled crop seed is available here.","Left-click: buy 1  •  Right-click: buy 64","Shift-click: buy 16","Payments use the server's Vault economy."));
        int start=page*pageSize;
        for(int i=0;i<pageSize&&start+i<list.size();i++){
            AgricultureRegistry.Crop crop=list.get(start+i);int slot=CONTENT[i];
            ItemStack item=plugin.items().seed(crop,1);ItemMeta meta=item.getItemMeta();
            List<Component> lore=new ArrayList<>(meta.lore()==null?List.of():meta.lore());
            lore.add(Component.empty());lore.add(line("PRICE  /  "+ItemFactory.money(crop.seedPrice()),GREEN));
            lore.add(line("YIELD  /  "+crop.minYield()+"–"+crop.maxYield(),MUTED));
            lore.add(line("Click to purchase this seed.",PALE));meta.lore(lore);meta.getPersistentDataContainer().set(actionKey,PersistentDataType.STRING,"buyseed:"+crop.id());item.setItemMeta(meta);inv.setItem(slot,item);
        }
        inv.setItem(45,button("all","ALL SEEDS","shopcat:all","Show every available seed."));
        inv.setItem(46,button("grain","GRAINS & LEGUMES","shopcat:grains","Browse grain and legume seeds."));
        inv.setItem(47,button("fruit","FRUIT & ORCHARDS","shopcat:tree_fruits","Browse trees, vines and fruit."));
        inv.setItem(48,button("vegetable","VEGETABLES","shopcat:vegetables","Browse field and root vegetables."));
        inv.setItem(49,button("herb","HERBS & SPICES","shopcat:herbs","Browse herbs, spices and aromatic crops."));
        if(page>0)inv.setItem(50,button("back","PREVIOUS PAGE","shoppage:"+filter+":"+(page-1)));
        inv.setItem(51,button("ledger","PAGE "+(page+1)+" / "+pages,null,"Available crops: "+list.size()));
        if(page<pages-1)inv.setItem(52,button("next","NEXT PAGE","shoppage:"+filter+":"+(page+1)));
        inv.setItem(53,button("home","RETURN TO LEDGER","menu:main"));
        open(player,inv);
    }

    public void openCropBook(Player player,int requestedPage){
        List<AgricultureRegistry.Crop> list=plugin.registry().crops().stream().sorted(Comparator.comparing(AgricultureRegistry.Crop::name)).toList();
        int pages=Math.max(1,(list.size()+CONTENT.length-1)/CONTENT.length);int page=Math.max(0,Math.min(requestedPage,pages-1));
        Inventory inv=create("crops:"+page,"THE IMPERIAL CROP CODEX");
        inv.setItem(4,button("crops","THE CROP CODEX",null,"A worldwide agricultural catalogue.","Each crop has unique growth timing, seed price and harvest yield."));
        int start=page*CONTENT.length;
        for(int i=0;i<CONTENT.length&&start+i<list.size();i++){
            AgricultureRegistry.Crop crop=list.get(start+i);ItemStack item=plugin.items().seed(crop,1);ItemMeta meta=item.getItemMeta();
            meta.displayName(line(crop.name(),PALE));meta.lore(List.of(line(crop.region()+"  •  "+crop.category().replace('_',' '),MUTED),line("Growth: "+ItemFactory.formatTime(crop.growthSeconds()),MUTED),line("Harvest: "+crop.minYield()+"–"+crop.maxYield(),MUTED),line(crop.description(),MUTED),line("Seed price: "+ItemFactory.money(crop.seedPrice()),GOLD)));item.setItemMeta(meta);inv.setItem(CONTENT[i],item);
        }
        if(page>0)inv.setItem(45,button("back","PREVIOUS PAGE","croppage:"+(page-1)));
        inv.setItem(49,button("ledger","PAGE "+(page+1)+" / "+pages,null,"Registered crop varieties: "+list.size()));
        if(page<pages-1)inv.setItem(52,button("next","NEXT PAGE","croppage:"+(page+1)));
        inv.setItem(53,button("home","RETURN TO LEDGER","menu:main"));open(player,inv);
    }

    public void openRecipeBook(Player player,int requestedPage){
        List<AgricultureRegistry.Recipe> list=plugin.registry().recipes().stream().sorted(Comparator.comparing(AgricultureRegistry.Recipe::name)).toList();
        int pages=Math.max(1,(list.size()+CONTENT.length-1)/CONTENT.length);int page=Math.max(0,Math.min(requestedPage,pages-1));
        Inventory inv=create("recipes:"+page,"THE CULINARY COMPENDIUM");
        inv.setItem(4,button("recipes","THE CULINARY COMPENDIUM",null,"78 documented production and cooking recipes.","Visit the matching production station to craft each recipe."));
        int start=page*CONTENT.length;
        for(int i=0;i<CONTENT.length&&start+i<list.size();i++){
            AgricultureRegistry.Recipe recipe=list.get(start+i);ItemStack item=button("recipe",recipe.name(),"recipeview:"+recipe.id(),"Station: "+recipe.station().replace('_',' '),"Produces: "+recipe.amount()+" × "+plugin.registry().itemName(recipe.result()),"Ingredients:");
            ItemMeta meta=item.getItemMeta();List<Component> lore=new ArrayList<>(meta.lore());
            recipe.ingredients().forEach((id,qty)->lore.add(line("  "+qty+" × "+plugin.registry().itemName(id),MUTED)));
            lore.add(line(recipe.description(),MUTED));meta.lore(lore);item.setItemMeta(meta);inv.setItem(CONTENT[i],item);
        }
        if(page>0)inv.setItem(45,button("back","PREVIOUS PAGE","recipepage:"+(page-1)));
        inv.setItem(49,button("ledger","PAGE "+(page+1)+" / "+pages,null,"Recipes: "+list.size()));
        if(page<pages-1)inv.setItem(52,button("next","NEXT PAGE","recipepage:"+(page+1)));
        inv.setItem(53,button("home","RETURN TO LEDGER","menu:main"));open(player,inv);
    }

    public void openStation(Player player,String stationId){
        List<AgricultureRegistry.Recipe> recipes=plugin.registry().recipesByStation(stationId);
        Inventory inv=create("station:"+stationId,"PRODUCTION  /  "+stationId.toUpperCase(Locale.ROOT));
        inv.setItem(4,button("stations",stationName(stationId),null,"Select a recipe to process your ingredients.","Required ingredients are taken from your inventory.","Unused station slots remain empty by design."));
        for(int i=0;i<CONTENT.length&&i<recipes.size();i++){
            AgricultureRegistry.Recipe recipe=recipes.get(i);ItemStack item=button("recipe",recipe.name(),"craft:"+recipe.id()+":"+stationId,"Produces: "+recipe.amount()+" × "+plugin.registry().itemName(recipe.result()),"Ingredients:");
            ItemMeta meta=item.getItemMeta();List<Component> lore=new ArrayList<>(meta.lore());
            recipe.ingredients().forEach((id,qty)->lore.add(line("  "+qty+" × "+plugin.registry().itemName(id),MUTED)));
            lore.add(line("Click to craft.",GREEN));meta.lore(lore);item.setItemMeta(meta);inv.setItem(CONTENT[i],item);
        }
        inv.setItem(49,button("ledger","RECIPES: "+recipes.size(),null,"Your station's available production recipes."));
        inv.setItem(53,button("home","RETURN TO LEDGER","menu:main"));open(player,inv);
    }

    public void openMarket(Player player,int requestedPage){
        List<String> ids=new ArrayList<>();for(AgricultureRegistry.Crop crop:plugin.registry().crops())ids.add("produce_"+crop.id());for(AgricultureRegistry.Good good:plugin.registry().goods())if(good.marketPrice()>0)ids.add(good.id());
        ids.sort(Comparator.comparing(plugin.registry()::itemName));
        int pages=Math.max(1,(ids.size()+CONTENT.length-1)/CONTENT.length);int page=Math.max(0,Math.min(requestedPage,pages-1));
        Inventory inv=create("market:"+page,"IMPERIAL COMMODITY EXCHANGE");
        inv.setItem(4,button("market","IMPERIAL COMMODITY EXCHANGE",null,"Sell produce, ingredients and crafted food.","Market values move with recent sales and supply.","Click an item to sell all matching stock up to the daily demand limit."));
        int start=page*CONTENT.length;
        for(int i=0;i<CONTENT.length&&start+i<ids.size();i++){
            String id=ids.get(start+i);ItemStack item=plugin.items().createById(id,1);if(item==null)continue;
            ItemMeta meta=item.getItemMeta();List<Component> lore=new ArrayList<>(meta.lore()==null?List.of():meta.lore());
            lore.add(line("MARKET PRICE  /  "+ItemFactory.money(plugin.market().price(id)),GREEN));lore.add(line("In your inventory: sell all available stock.",PALE));
            meta.lore(lore);meta.getPersistentDataContainer().set(actionKey,PersistentDataType.STRING,"marketsell:"+id);item.setItemMeta(meta);inv.setItem(CONTENT[i],item);
        }
        if(page>0)inv.setItem(45,button("back","PREVIOUS PAGE","marketpage:"+(page-1)));
        inv.setItem(49,button("ledger","PAGE "+(page+1)+" / "+pages,null,"Tradeable products: "+ids.size()));
        if(page<pages-1)inv.setItem(52,button("next","NEXT PAGE","marketpage:"+(page+1)));
        inv.setItem(53,button("home","RETURN TO LEDGER","menu:main"));open(player,inv);
    }

    public void openBusiness(Player player){
        AgricultureDatabase.Business business=plugin.database().business(player.getUniqueId());
        Inventory inv=create("business","AGRICULTURAL BUSINESS REGISTRY");
        inv.setItem(4,button("business","AGRICULTURAL BUSINESSES",null,"Player-owned farms, bakeries, mills, orchards, kitchens and grocers.","Register: /agri business register <type> <name>"));
        if(business==null){
            inv.setItem(22,button("business","REGISTER YOUR BUSINESS",null,"Registration: "+ItemFactory.money(plugin.getConfig().getDouble("economy.business-registration-cost",250.0)),"Types: farmstead, bakery, mill, orchard, kitchen, grocer","Use /agri business register <type> <name> to begin."));
            inv.setItem(31,button("market","BROWSE PLAYER STORES","menu:directory","Visit open agricultural businesses and buy their stock."));
        }else{
            inv.setItem(20,button("business","YOUR BUSINESS  /  "+business.name(),null,"Speciality: "+business.type(),"Town: "+(business.town().isBlank()?"Independent":business.town()),"Status: "+(business.open()?"OPEN":"CLOSED"),"Reputation: "+business.reputation()));
            List<AgricultureDatabase.Stock> stock=plugin.database().stock(player.getUniqueId());
            for(int i=0;i<Math.min(CONTENT.length,stock.size());i++){
                AgricultureDatabase.Stock entry=stock.get(i);ItemStack item=plugin.items().createById(entry.itemId(),Math.max(1,Math.min(64,entry.amount())));if(item==null)continue;
                ItemMeta meta=item.getItemMeta();meta.lore(List.of(line("STOCK  /  "+entry.amount(),MUTED),line("PRICE  /  "+(entry.price()>0?ItemFactory.money(entry.price()):"Not listed"),GREEN),line("Manage: /agri business price "+entry.itemId()+" <price>",MUTED)));item.setItemMeta(meta);inv.setItem(CONTENT[i],item);
            }
            inv.setItem(45,button(business.open()?"closed":"open",business.open()?"CLOSE BUSINESS":"OPEN BUSINESS",business.open()?"business:close":"business:open","Change your storefront status."));
            inv.setItem(46,button("stock","STOCK GUIDE",null,"Hold a custom item and run /agri business stock <item-id> [amount]."));
            inv.setItem(49,button("market","BROWSE PLAYER STORES","menu:directory","Visit other open businesses and buy their goods."));
        }
        inv.setItem(53,button("home","RETURN TO LEDGER","menu:main"));open(player,inv);
    }

    public void openBusinessDirectory(Player player){
        List<AgricultureDatabase.Business> stores=plugin.database().openBusinesses(28);
        Inventory inv=create("directory","THE IMPERIAL BUSINESS DIRECTORY");
        inv.setItem(4,button("business","OPEN AGRICULTURAL STORES",null,"Browse player-run businesses across Aevum.","Visit a store to purchase its listed stock."));
        if(stores.isEmpty()){
            inv.setItem(22,button("business","NO STORES OPEN",null,"Be the first merchant to register a farmstead, bakery, mill, orchard, kitchen or grocer.","Register with /agri business register <type> <name>."));
        }else{
            for(int i=0;i<stores.size()&&i<CONTENT.length;i++){
                AgricultureDatabase.Business store=stores.get(i);
                inv.setItem(CONTENT[i],button("business",store.name(),"businessvisit:"+store.owner(),
                        "OWNER  /  "+store.ownerName(),"SPECIALITY  /  "+store.type(),
                        "TOWN  /  "+(store.town().isBlank()?"Independent":store.town()),
                        "REPUTATION  /  "+store.reputation(),"Click to visit storefront."));
            }
        }
        inv.setItem(53,button("home","RETURN TO LEDGER","menu:main"));open(player,inv);
    }

    public void openBusinessStore(Player viewer,UUID owner){
        AgricultureDatabase.Business business=plugin.database().business(owner);
        if(business==null||!business.open()){viewer.sendMessage(plugin.message("That business is currently closed."));return;}
        Inventory inv=create("store:"+owner,"MARKET STALL  /  "+business.name());
        inv.setItem(4,button("business",business.name(),null,"Owned by "+business.ownerName(),"Speciality: "+business.type(),"Town: "+(business.town().isBlank()?"Independent":business.town()),"Click stock to purchase."));
        List<AgricultureDatabase.Stock> stock=plugin.database().stock(owner);
        for(int i=0;i<CONTENT.length&&i<stock.size();i++){
            AgricultureDatabase.Stock entry=stock.get(i);if(entry.price()<=0)continue;
            ItemStack item=plugin.items().createById(entry.itemId(),1);if(item==null)continue;ItemMeta meta=item.getItemMeta();
            meta.lore(List.of(line("AVAILABLE  /  "+entry.amount(),MUTED),line("PRICE  /  "+ItemFactory.money(entry.price()),GREEN),line("Left-click: buy 1  •  Shift-click: buy 16",PALE)));
            meta.getPersistentDataContainer().set(actionKey,PersistentDataType.STRING,"businessbuy:"+owner+":"+entry.itemId());item.setItemMeta(meta);inv.setItem(CONTENT[i],item);
        }
        inv.setItem(53,button("home","RETURN TO LEDGER","menu:main"));open(viewer,inv);
    }

    public void openStats(Player player){
        AgricultureDatabase.Profile p=plugin.database().profile(player.getUniqueId(),player.getName());
        AgricultureDatabase.Business b=plugin.database().business(player.getUniqueId());
        Inventory inv=create("stats","THE FARMER'S LIFETIME LEDGER");
        inv.setItem(4,button("profile","THE FARMER'S RECORD",null,"A UUID-linked agricultural record."));
        inv.setItem(20,button("harvest","HARVESTS",null,"Lifetime harvests: "+p.harvests()));
        inv.setItem(22,button("level","AGRICULTURE LEVEL",null,"Level: "+p.level()+" / 50","Total XP: "+p.xp()));
        inv.setItem(24,button("recipe","PRODUCTION",null,"Recipes crafted: "+p.crafts()));
        inv.setItem(31,button("market","TRADE",null,"Recorded market sales: "+p.sales()));
        inv.setItem(40,button("business","BUSINESS",null,b==null?"No business registered.":b.name()+"  •  "+b.type()));
        inv.setItem(53,button("home","RETURN TO LEDGER","menu:main"));open(player,inv);
    }

    public void openTop(Player player){
        List<AgricultureDatabase.Farmer> top=plugin.database().topFarmers(28);
        Inventory inv=create("top","THE IMPERIAL FARMER RANKINGS");inv.setItem(4,button("laurel","HALL OF AGRICULTURAL HONOUR",null,"Ranked by Agriculture XP."));
        for(int i=0;i<top.size()&&i<CONTENT.length;i++){var farmer=top.get(i);inv.setItem(CONTENT[i],button("profile","#"+(i+1)+"  "+farmer.name(),null,"Agriculture XP: "+farmer.xp(),"Harvests: "+farmer.harvests(),"Recipes crafted: "+farmer.crafts()));}
        inv.setItem(53,button("home","RETURN TO LEDGER","menu:main"));open(player,inv);
    }

    public void openStationDirectory(Player player){
        Inventory inv=create("stations","THE IMPERIAL WORKSHOPS");inv.setItem(4,button("stations","PRODUCTION STATIONS",null,"Eight specialist workshops power the food economy."));
        String[][] stations={{"mill","STONE MILL"},{"press","OLIVE PRESS"},{"oven","BAKER'S OVEN"},{"kitchen","COOKING HEARTH"},{"drying_rack","DRYING RACK"},{"fermenter","FERMENTATION STAND"},{"preparation","PREPARATION TABLE"},{"mortar","MORTAR & PESTLE"}};
        int[] slots={20,21,22,23,24,29,30,31};
        for(int i=0;i<stations.length;i++){
            String id=stations[i][0];
            int count=plugin.registry().recipesByStation(id).size();
            double price=plugin.getConfig().getDouble("stations."+id+".price",150.0);
            String material=plugin.getConfig().getString("stations."+id+".material","BARREL");
            inv.setItem(slots[i],button("stations",stations[i][1],"buystation:"+id,
                    "Equipment: "+stations[i][1],"Placed block: "+material,"Recipes available: "+count,
                    "PURCHASE KIT  /  "+ItemFactory.money(price),"Click to buy a placeable station kit."));
        }
        inv.setItem(53,button("home","RETURN TO LEDGER","menu:main"));open(player,inv);
    }

    public void openRecipeDetail(Player player,String id){
        AgricultureRegistry.Recipe recipe=plugin.registry().recipe(id);if(recipe==null){openRecipeBook(player,0);return;}
        Inventory inv=create("recipe:"+id,"RECIPE  /  "+recipe.name().toUpperCase(Locale.ROOT));
        inv.setItem(4,button("recipe",recipe.name(),null,recipe.description(),"Production station: "+recipe.station()));
        inv.setItem(22,button("recipe","REQUIRED INGREDIENTS",null,recipe.ingredients().entrySet().stream().map(e->e.getValue()+" × "+plugin.registry().itemName(e.getKey())).toArray(String[]::new)));
        inv.setItem(31,button("food","OUTPUT  /  "+recipe.amount()+" × "+plugin.registry().itemName(recipe.result()),null,"Agriculture XP: "+recipe.xp()));
        inv.setItem(49,button("stations","FIND THE "+recipe.station().toUpperCase(Locale.ROOT),"menu:stations","Purchase and place the matching production station."));
        inv.setItem(53,button("back","BACK TO RECIPES","menu:recipes"));open(player,inv);
    }

    @EventHandler public void onClick(InventoryClickEvent event){
        if(!(event.getView().getTopInventory().getHolder(false) instanceof Holder holder))return;
        event.setCancelled(true);if(!(event.getWhoClicked() instanceof Player player))return;
        int raw=event.getRawSlot();if(raw<0||raw>=event.getView().getTopInventory().getSize())return;
        ItemStack clicked=event.getCurrentItem();String action=plugin.items().action(clicked);if(action==null)return;
        player.playSound(player.getLocation(),Sound.UI_BUTTON_CLICK,0.25f,1.3f);
        if(action.equals("menu:main")){openMain(player);return;}
        if(action.equals("menu:shop")){openShop(player,"all",0);return;}
        if(action.startsWith("shopcat:")){openShop(player,action.substring(8),0);return;}
        if(action.startsWith("shoppage:")){String[] p=action.split(":");openShop(player,p[1],Integer.parseInt(p[2]));return;}
        if(action.equals("menu:crops")){openCropBook(player,0);return;}
        if(action.startsWith("croppage:")){openCropBook(player,Integer.parseInt(action.substring(9)));return;}
        if(action.equals("menu:recipes")){openRecipeBook(player,0);return;}
        if(action.startsWith("recipepage:")){openRecipeBook(player,Integer.parseInt(action.substring(11)));return;}
        if(action.equals("menu:market")){openMarket(player,0);return;}
        if(action.startsWith("marketpage:")){openMarket(player,Integer.parseInt(action.substring(12)));return;}
        if(action.equals("menu:business")){openBusiness(player);return;}
        if(action.equals("menu:directory")){openBusinessDirectory(player);return;}
        if(action.equals("menu:stations")){openStationDirectory(player);return;}
        if(action.equals("menu:stats")){openStats(player);return;}
        if(action.equals("menu:top")){openTop(player);return;}
        if(action.equals("menu:help")){player.sendMessage(plugin.message("Plant a custom seed on farmland. Wait for it to mature, then right-click its crop. Use bone meal to accelerate growth. Craft at specialist stations and sell goods at the Imperial Commodity Exchange."));return;}
        if(action.startsWith("buyseed:")){buySeed(player,action.substring(8),event);return;}
        if(action.startsWith("marketsell:")){plugin.market().sellAll(player,action.substring(11));openMarket(player,0);return;}
        if(action.startsWith("buystation:")){buyStation(player,action.substring(11));return;}
        if(action.startsWith("craft:")){String[] parts=action.split(":",3);if(parts.length==3)plugin.crops().craft(player,parts[1],parts[2]);return;}
        if(action.startsWith("recipeview:")){openRecipeDetail(player,action.substring(11));return;}
        if(action.startsWith("businessvisit:")){openBusinessStore(player,UUID.fromString(action.substring(14)));return;}
        if(action.startsWith("businessbuy:")){String[] parts=action.split(":",3);if(parts.length==3){int amount=event.isShiftClick()?16:1;plugin.businesses().buy(player,UUID.fromString(parts[1]),parts[2],amount);openBusinessStore(player,UUID.fromString(parts[1]));}return;}
        if(action.equals("business:open")||action.equals("business:close")){var b=plugin.database().business(player.getUniqueId());if(b!=null){plugin.database().setBusinessOpen(player.getUniqueId(),action.endsWith("open"));openBusiness(player);}return;}
        if(action.startsWith("menu:"))return;
    }

    @EventHandler public void onDrag(InventoryDragEvent event){if(event.getView().getTopInventory().getHolder(false) instanceof Holder)event.setCancelled(true);}

    private void buyStation(Player player,String id){
        if(!player.hasPermission("aevumagriculture.business")){player.sendMessage(plugin.message("You do not have permission to purchase production equipment."));return;}
        if(plugin.economy()==null){player.sendMessage(plugin.message("Purchasing station kits requires Vault and an economy provider."));return;}
        String path="stations."+id;
        if(!plugin.getConfig().contains(path+".material")){player.sendMessage(plugin.message("That production station is not available."));return;}
        double price=plugin.getConfig().getDouble(path+".price",150.0);
        if(!Double.isFinite(price)||price<0){player.sendMessage(plugin.message("This station has an invalid configured price. Contact an administrator."));return;}
        if(!plugin.economy().has(player,price)){player.sendMessage(plugin.message("You need "+ItemFactory.money(price)+" to buy that station kit."));return;}
        String name=plugin.getConfig().getString(path+".name",id);
        String material=plugin.getConfig().getString(path+".material","BARREL");
        org.bukkit.inventory.ItemStack kit=plugin.items().station(id,name,material,1);
        plugin.economy().withdrawPlayer(player,price);
        var leftovers=player.getInventory().addItem(kit);
        leftovers.values().forEach(item->player.getWorld().dropItemNaturally(player.getLocation(),item));
        player.sendMessage(plugin.message("Purchased a "+name+" kit for "+ItemFactory.money(price)+". Place it and right-click to use it."));
        openStationDirectory(player);
    }

    private void buySeed(Player player,String id,InventoryClickEvent event){
        if(!plugin.getConfig().getBoolean("economy.seed-shop-enabled",true)){player.sendMessage(plugin.message("The seed exchange is disabled."));return;}
        if(plugin.economy()==null){player.sendMessage(plugin.message("The seed exchange requires Vault and an economy provider."));return;}
        AgricultureRegistry.Crop crop=plugin.registry().crop(id);if(crop==null)return;
        int amount=event.isShiftClick()?16:event.isRightClick()?64:1;double cost=Math.round(crop.seedPrice()*amount*100.0)/100.0;
        if(!plugin.economy().has(player,cost)){player.sendMessage(plugin.message("You need "+ItemFactory.money(cost)+" for "+amount+" × "+crop.name()+" seeds."));return;}
        plugin.economy().withdrawPlayer(player,cost);ItemStack seeds=plugin.items().seed(crop,amount);var leftovers=player.getInventory().addItem(seeds);leftovers.values().forEach(item->player.getWorld().dropItemNaturally(player.getLocation(),item));
        player.sendMessage(plugin.message("Purchased "+amount+" × "+crop.name()+" seeds for "+ItemFactory.money(cost)+"."));
    }
}
