package dev.annawathe.api.win;
import net.minecraft.entity.player.PlayerEntity; import net.minecraft.nbt.*; import net.minecraft.util.Identifier; import java.util.*;
/** 独立胜利分组数据，供结算同步和客户端布局使用。 */
public record CustomVictoryGroup(String titleTranslationKey,String fallbackTitle,int color,List<UUID> playerUuids){
 public CustomVictoryGroup{playerUuids=List.copyOf(playerUuids);}
 public boolean contains(UUID id){return id!=null&&playerUuids.contains(id);}
 public NbtCompound writeToNbt(){NbtCompound t=new NbtCompound();t.putString("title",titleTranslationKey);t.putString("fallback",fallbackTitle);t.putInt("color",color);NbtList l=new NbtList();for(UUID u:playerUuids)l.add(NbtHelper.fromUuid(u));t.put("players",l);return t;}
 public static CustomVictoryGroup fromNbt(NbtCompound t){List<UUID> l=new ArrayList<>();for(NbtElement e:t.getList("players",NbtElement.INT_ARRAY_TYPE))l.add(NbtHelper.toUuid(e));return new CustomVictoryGroup(t.getString("title"),t.getString("fallback"),t.getInt("color"),l);}
 static List<UUID> uuids(Collection<? extends PlayerEntity> p){return p.stream().filter(Objects::nonNull).map(PlayerEntity::getUuid).toList();}
 static String pretty(String p){return Arrays.stream(p.split("_")).filter(s->!s.isEmpty()).map(s->Character.toUpperCase(s.charAt(0))+s.substring(1)).reduce((a,b)->a+" "+b).orElse(p);}
}
