package net.aevummc.agriculture.resourcepack;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import net.aevummc.agriculture.AevumAgriculture;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

/** Sends the agriculture pack as an additional stacked pack; it never clears AevumReligion's pack. */
public final class ResourcePackManager implements Listener {
    private static final UUID PACK_ID = UUID.nameUUIDFromBytes("AevumAgriculture-Imperial-Agrarian-Ledger".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    private final AevumAgriculture plugin;
    private byte[] packBytes;
    private byte[] sha1;
    private String downloadUrl;
    private HttpServer server;
    private ExecutorService executor;

    public ResourcePackManager(AevumAgriculture plugin) { this.plugin = plugin; }

    public void start() {
        stop();
        if (!plugin.getConfig().getBoolean("resource-pack.enabled", true)) return;
        try {
            File pack = new File(plugin.getDataFolder(), "aevum-agriculture.zip");
            try (InputStream in = plugin.getResource("aevum-agriculture-pack.zip")) {
                if (in == null) throw new FileNotFoundException("Embedded aevum-agriculture-pack.zip is missing from the JAR");
                Files.copy(in, pack.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            packBytes = Files.readAllBytes(pack.toPath());
            sha1 = MessageDigest.getInstance("SHA-1").digest(packBytes);
            downloadUrl = plugin.getConfig().getString("resource-pack.url", "").trim();
            if (plugin.getConfig().getBoolean("resource-pack.host.enabled", false)) {
                startHost();
                if (downloadUrl.isBlank()) {
                    String base = plugin.getConfig().getString("resource-pack.host.public-url", "").trim();
                    if (!base.isBlank()) downloadUrl = stripSlash(base) + "/aevum-agriculture.zip";
                }
            }
            plugin.getLogger().info("Agriculture resource pack prepared: " + pack.length() + " bytes, SHA-1 " + HexFormat.of().formatHex(sha1));
            if (downloadUrl.isBlank()) plugin.getLogger().warning("Set resource-pack.url or configure the built-in public host; clients cannot download a local server file.");
        } catch (Exception ex) {
            plugin.getLogger().log(Level.SEVERE, "Could not prepare AevumAgriculture resource pack.", ex);
        }
    }

    private void startHost() throws IOException {
        String bind = plugin.getConfig().getString("resource-pack.host.bind-address", "0.0.0.0");
        int port = plugin.getConfig().getInt("resource-pack.host.port", 25822);
        server = HttpServer.create(new InetSocketAddress(bind, port), 0);
        server.createContext("/aevum-agriculture.zip", this::serve);
        executor = Executors.newFixedThreadPool(2, runnable -> { Thread t = new Thread(runnable, "AevumAgriculture-PackHTTP"); t.setDaemon(true); return t; });
        server.setExecutor(executor); server.start();
        plugin.getLogger().info("Agriculture resource-pack host listening on " + bind + ":" + port);
    }

    private void serve(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("GET") && !exchange.getRequestMethod().equalsIgnoreCase("HEAD")) {
            exchange.getResponseHeaders().set("Allow", "GET, HEAD"); exchange.sendResponseHeaders(405, -1); exchange.close(); return;
        }
        exchange.getResponseHeaders().set("Content-Type", "application/zip");
        exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"aevum-agriculture.zip\"");
        exchange.getResponseHeaders().set("Cache-Control", "public, max-age=300");
        if (exchange.getRequestMethod().equalsIgnoreCase("HEAD")) { exchange.getResponseHeaders().set("Content-Length", Integer.toString(packBytes.length)); exchange.sendResponseHeaders(200, -1); }
        else { exchange.sendResponseHeaders(200, packBytes.length); try (OutputStream out = exchange.getResponseBody()) { out.write(packBytes); } }
        exchange.close();
    }

    public void send(Player player) {
        if (!plugin.getConfig().getBoolean("resource-pack.enabled", true) || !plugin.getConfig().getBoolean("resource-pack.auto-send-on-join", true)) return;
        if (downloadUrl == null || downloadUrl.isBlank() || sha1 == null) return;
        // addResourcePack stacks this pack on top of AevumReligion rather than replacing it.
        player.addResourcePack(PACK_ID, downloadUrl, sha1,
                plugin.getConfig().getString("resource-pack.prompt", "AevumAgriculture custom items and crops."),
                plugin.getConfig().getBoolean("resource-pack.force", false));
    }

    @EventHandler
    public void onPackStatus(PlayerResourcePackStatusEvent event) {
        if (event.getID() != null && event.getID().equals(PACK_ID)) {
            plugin.getLogger().fine("Resource-pack status for " + event.getPlayer().getName() + ": " + event.getStatus());
        }
    }

    public void stop() {
        if (server != null) { server.stop(0); server = null; }
        if (executor != null) { executor.shutdownNow(); executor = null; }
        packBytes = null; sha1 = null;
    }
    private String stripSlash(String value) { while (value.endsWith("/")) value = value.substring(0,value.length()-1); return value; }
}
