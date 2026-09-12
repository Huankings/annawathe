package dev.annawathe.api.client.inventory;
import net.fabricmc.api.EnvType; import net.fabricmc.api.Environment; import net.minecraft.util.Identifier; import java.util.*;
@Environment(EnvType.CLIENT) public final class InventoryPageState { private static final Map<Identifier,Integer> PAGES=new HashMap<>(); private InventoryPageState(){} public static int getPage(Identifier id){return PAGES.getOrDefault(id,0);} public static void setPage(Identifier id,int page){PAGES.put(id,Math.max(0,page));} public static void reset(){PAGES.clear();} }
