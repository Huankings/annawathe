package dev.annawathe.client.psychosis;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import java.util.*;
/**
 * 每个观察者独立保存的幻觉物品/手臂姿势缓存。
 * 该缓存是客户端临时状态，不写入 CCA NBT；目标死亡、停局或断线时必须清理。
 */
public final class AnnaPsychosisVisualState {
 private static final Map<UUID,Map<Key,ItemStack>> ITEMS=new HashMap<>(); private static final Map<UUID,Map<Key,BipedEntityModel.ArmPose>> POSES=new HashMap<>(); private AnnaPsychosisVisualState(){}
 public static void put(UUID viewer,UUID target,Hand hand,ItemStack stack,BipedEntityModel.ArmPose pose){ITEMS.computeIfAbsent(viewer,k->new HashMap<>()).put(new Key(target,hand),stack.copy());if(pose!=null)POSES.computeIfAbsent(viewer,k->new HashMap<>()).put(new Key(target,hand),pose);}
 public static ItemStack get(UUID viewer,UUID target,Hand hand){ItemStack s=ITEMS.getOrDefault(viewer,Map.of()).get(new Key(target,hand));return s==null?null:s.copy();}
 public static BipedEntityModel.ArmPose getPose(UUID viewer,UUID target,Hand hand){return POSES.getOrDefault(viewer,Map.of()).get(new Key(target,hand));}
 public static void remove(UUID viewer,UUID target,Hand hand){Map<Key,ItemStack> items=ITEMS.get(viewer);if(items!=null)items.remove(new Key(target,hand));Map<Key,BipedEntityModel.ArmPose> poses=POSES.get(viewer);if(poses!=null)poses.remove(new Key(target,hand));}
 public static void clear(UUID viewer){ITEMS.remove(viewer);POSES.remove(viewer);} public static void clearAll(){ITEMS.clear();POSES.clear();}
 private record Key(UUID target,Hand hand){}
}
