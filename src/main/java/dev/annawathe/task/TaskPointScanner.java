package dev.annawathe.task;

import dev.annawathe.api.task.MoodTaskPointApi;
import dev.annawathe.api.task.TaskPointScanContext;
import dev.doctor4t.wathe.block.MountableBlock;
import dev.doctor4t.wathe.block_entity.BeveragePlateBlockEntity;
import dev.doctor4t.wathe.block_entity.SmallDoorBlockEntity;
import dev.doctor4t.wathe.cca.MapVariablesWorldComponent;
import dev.doctor4t.wathe.item.CocktailItem;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** 只扫描当前列车复制区域且位于 playArea 内的任务点。 */
public final class TaskPointScanner {
    private TaskPointScanner() {}
    public static Map<BlockPos, Set<Identifier>> scan(ServerWorld world) {
        MapVariablesWorldComponent areas = MapVariablesWorldComponent.KEY.get(world);
        BlockPos templateMin=BlockPos.ofFloored(areas.getResetTemplateArea().getMinPos());
        BlockPos templateMax=BlockPos.ofFloored(areas.getResetTemplateArea().getMaxPos());
        // 直接平移两个端点，避免用 inclusive dimensions 计算时多扫描一格边界。
        BlockBox box=BlockBox.create(templateMin.add(areas.getResetPasteOffset()),templateMax.add(areas.getResetPasteOffset()));
        HashMap<BlockPos,Set<Identifier>> result=new HashMap<>();
        for(int x=box.getMinX();x<=box.getMaxX();x++)for(int y=box.getMinY();y<=box.getMaxY();y++)for(int z=box.getMinZ();z<=box.getMaxZ();z++){
            BlockPos pos=new BlockPos(x,y,z);if(!areas.getPlayArea().contains(pos.toCenterPos()))continue;
            BlockState state=world.getBlockState(pos);BlockEntity blockEntity=world.getBlockEntity(pos);
            if(state.isIn(BlockTags.BEDS))add(result,canonicalBed(pos,state),MoodTaskPointApi.BED);
            if(state.getBlock() instanceof MountableBlock)add(result,pos,MoodTaskPointApi.SEAT);
            if(blockEntity instanceof BeveragePlateBlockEntity plate)scanPlate(result,pos,plate);
            if(blockEntity instanceof SmallDoorBlockEntity door&&!door.getKeyName().isEmpty())add(result,pos,MoodTaskPointApi.KEYED_DOOR);
            MoodTaskPointApi.scanExtraTaskPoints(new TaskPointScanContext(world,areas,pos,state,blockEntity,id->add(result,pos,id)));
        }
        return result;
    }
    private static void scanPlate(Map<BlockPos,Set<Identifier>> map,BlockPos pos,BeveragePlateBlockEntity plate){boolean food=false,drink=false;for(ItemStack stack:plate.getStoredItems()){
        if(stack.isEmpty())continue;if(stack.get(DataComponentTypes.FOOD)!=null&&!(stack.getItem() instanceof CocktailItem))food=true;if(stack.getItem() instanceof CocktailItem)drink=true;}
        if(food)add(map,pos,MoodTaskPointApi.FOOD_TRAY);if(drink)add(map,pos,MoodTaskPointApi.COCKTAIL_TRAY);}
    private static BlockPos canonicalBed(BlockPos pos,BlockState state){return state.contains(BedBlock.PART)&&state.contains(BedBlock.FACING)&&state.get(BedBlock.PART)==net.minecraft.block.enums.BedPart.HEAD?pos.offset(state.get(BedBlock.FACING).getOpposite()):pos;}
    private static void add(Map<BlockPos,Set<Identifier>> map,BlockPos pos,Identifier id){map.computeIfAbsent(pos.toImmutable(),p->new LinkedHashSet<>()).add(id);}
}
