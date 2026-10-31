package net.aevummc.agriculture.service;

import net.aevummc.agriculture.AevumAgriculture;
import net.aevummc.agriculture.data.AgricultureDatabase;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;
import java.util.UUID;

public final class BusinessService {
    private final AevumAgriculture plugin;
    public BusinessService(AevumAgriculture plugin) { this.plugin=plugin; }

    public boolean register(Player player,String type,String name) {
        if(!player.hasPermission("aevumagriculture.business")) return false;
        if(plugin.economy()==null){player.sendMessage(plugin.message("A Vault economy provider is required to register a business."));return false;}
        if(plugin.database().business(player.getUniqueId())!=null){player.sendMessage(plugin.message("You already own an agricultural business."));return false;}
        double cost=plugin.getConfig().getDouble("economy.business-registration-cost",250.0);
        Economy econ=plugin.economy();
        if(!econ.has(player,cost)){player.sendMessage(plugin.message("You need "+ItemFactory.money(cost)+" to register a business."));return false;}
        String safeType=type.toLowerCase(Locale.ROOT);
        if(!java.util.Set.of("farmstead","bakery","mill","orchard","kitchen","grocer").contains(safeType)){player.sendMessage(plugin.message("Business types: farmstead, bakery, mill, orchard, kitchen, grocer."));return false;}
        if(name.length()<3||name.length()>32){player.sendMessage(plugin.message("Business names must be 3–32 characters."));return false;}
        econ.withdrawPlayer(player,cost);
        String town=plugin.getConfig().getBoolean("integrations.towny.enabled",true)?plugin.towny().townName(player):"";
        if(!plugin.database().createBusiness(player.getUniqueId(),player.getName(),name,safeType,town)){
            econ.depositPlayer(player,cost);player.sendMessage(plugin.message("That business name is already registered or could not be saved."));return false;
        }
        player.sendMessage(plugin.message("Registered "+name+" as a "+safeType+". Your business ledger is open."));
        plugin.gui().openBusiness(player);return true;
    }

    public boolean deposit(Player player,String itemId,int requested) {
        AgricultureDatabase.Business business=plugin.database().business(player.getUniqueId());
        if(business==null){player.sendMessage(plugin.message("Register a business first: /agri business register <type> <name>"));return false;}
        if(plugin.registry().basePrice(itemId)<=0||itemId.startsWith("seed_")){player.sendMessage(plugin.message("Only harvested produce, ingredients and finished foods can be stocked."));return false;}
        int found=count(player,itemId);int amount=Math.min(found,requested<=0?found:requested);
        if(amount<=0){player.sendMessage(plugin.message("You do not have any "+plugin.registry().itemName(itemId)+" to stock."));return false;}
        int max=plugin.getConfig().getInt("economy.business-max-stock-per-item",100000);
        AgricultureDatabase.Stock current=plugin.database().stock(player.getUniqueId(),itemId);
        if(current!=null)amount=Math.min(amount,Math.max(0,max-current.amount()));
        if(amount<=0){player.sendMessage(plugin.message("That business stock is already at its configured limit."));return false;}
        remove(player,itemId,amount);plugin.database().depositStock(player.getUniqueId(),itemId,amount);
        player.sendMessage(plugin.message("Deposited "+amount+" × "+plugin.registry().itemName(itemId)+" into "+business.name()+"."));return true;
    }

    public boolean setPrice(Player player,String itemId,double price) {
        if(plugin.database().business(player.getUniqueId())==null){player.sendMessage(plugin.message("You do not own a business."));return false;}
        if(plugin.registry().basePrice(itemId)<=0||itemId.startsWith("seed_")){player.sendMessage(plugin.message("That is not a tradeable agricultural product."));return false;}
        if(!Double.isFinite(price)||price<=0||price>1000000){player.sendMessage(plugin.message("Enter a valid price greater than zero."));return false;}
        plugin.database().setStockPrice(player.getUniqueId(),itemId,price);
        player.sendMessage(plugin.message("Set "+plugin.registry().itemName(itemId)+" to "+ItemFactory.money(price)+" each."));return true;
    }

    public boolean buy(Player buyer,UUID owner,String itemId,int requested) {
        if(plugin.economy()==null){buyer.sendMessage(plugin.message("The Vault economy is unavailable."));return false;}
        AgricultureDatabase.Business business=plugin.database().business(owner);
        if(business==null||!business.open()){buyer.sendMessage(plugin.message("That business is closed or unavailable."));return false;}
        if(owner.equals(buyer.getUniqueId())){buyer.sendMessage(plugin.message("You cannot purchase from your own business."));return false;}
        AgricultureDatabase.Stock stock=plugin.database().stock(owner,itemId);
        if(stock==null||stock.price()<=0||stock.amount()<=0){buyer.sendMessage(plugin.message("That product is not currently for sale."));return false;}
        int amount=Math.max(1,Math.min(Math.min(requested,stock.amount()),64));
        double total=Math.round(stock.price()*amount*100.0)/100.0;
        if(!plugin.economy().has(buyer,total)){buyer.sendMessage(plugin.message("You need "+ItemFactory.money(total)+" for that purchase."));return false;}
        if(!plugin.database().takeStock(owner,itemId,amount)){buyer.sendMessage(plugin.message("The stock changed before your purchase could complete. Please try again."));return false;}
        Economy econ=plugin.economy();econ.withdrawPlayer(buyer,total);econ.depositPlayer(plugin.getServer().getOfflinePlayer(owner),total);
        ItemStack stack=plugin.items().createById(itemId,amount);if(stack!=null){var leftovers=buyer.getInventory().addItem(stack);leftovers.values().forEach(i->buyer.getWorld().dropItemNaturally(buyer.getLocation(),i));}
        plugin.database().recordSale(itemId,amount,stock.price(),owner,"business");plugin.database().addBusinessReputation(owner,amount);
        plugin.database().addActivity(owner,business.ownerName(),plugin.getConfig().getLong("progression.sale-xp",2),0,0,1);
        buyer.sendMessage(plugin.message("Purchased "+amount+" × "+plugin.registry().itemName(itemId)+" from "+business.name()+" for "+ItemFactory.money(total)+"."));return true;
    }

    private int count(Player player,String id){int n=0;for(ItemStack item:player.getInventory().getContents())if(id.equals(plugin.items().itemId(item)))n+=item.getAmount();return n;}
    private void remove(Player player,String id,int amount){int left=amount;ItemStack[] items=player.getInventory().getContents();for(int i=0;i<items.length&&left>0;i++){ItemStack item=items[i];if(!id.equals(plugin.items().itemId(item)))continue;int take=Math.min(left,item.getAmount());item.setAmount(item.getAmount()-take);if(item.getAmount()<=0)items[i]=null;left-=take;}player.getInventory().setContents(items);}
}
