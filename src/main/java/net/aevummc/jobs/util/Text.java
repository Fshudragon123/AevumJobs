package net.aevummc.jobs.util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
public final class Text { private Text(){} public static final TextColor GOLD=TextColor.fromHexString("#FFC847"); public static final TextColor BRONZE=TextColor.fromHexString("#C98B24"); public static Component c(String s){return Component.text(s,GOLD);} }