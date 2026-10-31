package net.aevummc.agriculture.hook;

import org.bukkit.entity.Player;

import java.lang.reflect.Method;

/** Towny is deliberately optional; this bridge only reads a player's current town. */
public final class TownyHook {
    public String townName(Player player) {
        try {
            Class<?> apiClass = Class.forName("com.palmergames.bukkit.towny.TownyAPI");
            Object api = apiClass.getMethod("getInstance").invoke(null);
            Object resident = invokeLookup(api, "getResident", player);
            if (resident == null) return "";
            Object town = invokeNoArgs(resident, "getTownOrNull", "getTown");
            if (town == null) return "";
            Object name = invokeNoArgs(town, "getName", "getFormattedName");
            return name == null ? "" : name.toString();
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return "";
        }
    }

    private Object invokeLookup(Object target, String name, Player player) throws ReflectiveOperationException {
        for (Method method : target.getClass().getMethods()) {
            if (!method.getName().equals(name) || method.getParameterCount() != 1) continue;
            Class<?> type = method.getParameterTypes()[0];
            if (type.isAssignableFrom(Player.class)) return method.invoke(target, player);
            if (type.isAssignableFrom(java.util.UUID.class)) return method.invoke(target, player.getUniqueId());
        }
        return null;
    }
    private Object invokeNoArgs(Object target, String... names) throws ReflectiveOperationException {
        for (String name : names) {
            try { return target.getClass().getMethod(name).invoke(target); }
            catch (NoSuchMethodException ignored) { }
        }
        return null;
    }
}
