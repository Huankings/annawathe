package dev.annawathe.client.task;

import dev.annawathe.api.task.MoodTaskPointApi;
import dev.doctor4t.wathe.block.SmallDoorBlock;
import dev.doctor4t.wathe.block_entity.SmallDoorBlockEntity;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.index.WatheItems;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.OptionalDouble;
import java.util.Set;

/** 穿墙线框任务点；存活玩家只看当前任务相关点，钥匙门由手持钥匙独立控制。 */
public final class TaskPointOverlayRenderer {
    private static final RenderLayer LINES=RenderLayer.of("annawathe_task_points",VertexFormats.LINES,VertexFormat.DrawMode.LINES,256,false,false,
            RenderLayer.MultiPhaseParameters.builder().program(RenderPhase.LINES_PROGRAM).lineWidth(new RenderPhase.LineWidth(OptionalDouble.of(4D))).layering(RenderPhase.VIEW_OFFSET_Z_LAYERING).transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY).target(RenderPhase.ITEM_ENTITY_TARGET).writeMaskState(RenderPhase.COLOR_MASK).cull(RenderPhase.DISABLE_CULLING).depthTest(RenderPhase.ALWAYS_DEPTH_TEST).build(false));
    private TaskPointOverlayRenderer(){}
    public static void render(WorldRenderContext context){MinecraftClient client=MinecraftClient.getInstance();if(client.world==null||client.player==null||!TaskPointClientState.isEnabled()||WatheClient.gameComponent==null||!WatheClient.gameComponent.isRunning()||context.matrixStack()==null||context.consumers()==null)return;
        LinkedHashSet<Identifier> allowed=WatheClient.isPlayerAliveAndInSurvival()?new LinkedHashSet<>(TaskPointClientState.visibleTaskTypes(client.player)):new LinkedHashSet<>(MoodTaskPointApi.getRegisteredIds());allowed.remove(MoodTaskPointApi.KEYED_DOOR);String keyName=heldKeyName(client.player.getMainHandStack());
        for(var entry:TaskPointClientState.snapshot().entrySet()){LinkedHashSet<Identifier> visible=new LinkedHashSet<>(entry.getValue());visible.retainAll(allowed);if(entry.getValue().contains(MoodTaskPointApi.KEYED_DOOR)&&matchesDoor(client,entry.getKey(),keyName))visible.add(MoodTaskPointApi.KEYED_DOOR);if(!visible.isEmpty())renderPoint(context,entry.getKey(),visible);}}
    private static void renderPoint(WorldRenderContext context,BlockPos pos,Set<Identifier> types){MinecraftClient client=MinecraftClient.getInstance();BlockState state=client.world.getBlockState(pos);Box box=combinedBox(client.world,pos,state);int color=blend(types);VertexConsumer consumer=context.consumers().getBuffer(LINES);MatrixStack matrices=context.matrixStack();Vec3d camera=context.camera().getPos();matrices.push();matrices.translate(pos.getX()-camera.x,pos.getY()-camera.y,pos.getZ()-camera.z);WorldRenderer.drawBox(matrices,consumer,box,((color>>16)&255)/255F,((color>>8)&255)/255F,(color&255)/255F,1F);matrices.pop();if(camera.squaredDistanceTo(pos.toCenterPos())<=9D)renderLabel(context,pos,box,label(client,pos,types),color);}
    private static void renderLabel(WorldRenderContext context,BlockPos pos,Box box,Text text,int color){MinecraftClient client=MinecraftClient.getInstance();MatrixStack matrices=context.matrixStack();Vec3d camera=context.camera().getPos();TextRenderer renderer=client.textRenderer;matrices.push();matrices.translate(pos.getX()+(box.minX+box.maxX)/2-camera.x,pos.getY()+box.maxY+.35-camera.y,pos.getZ()+(box.minZ+box.maxZ)/2-camera.z);matrices.multiply(context.camera().getRotation());matrices.scale(-.025F,-.025F,.025F);VertexConsumerProvider.Immediate consumers=client.getBufferBuilders().getEntityVertexConsumers();renderer.draw(text,-renderer.getWidth(text)/2F,0,0xFF000000|color,false,matrices.peek().getPositionMatrix(),consumers,TextRenderer.TextLayerType.SEE_THROUGH,0,15728880);consumers.draw();matrices.pop();}
    private static Text label(MinecraftClient client,BlockPos pos,Set<Identifier> types){if(types.size()==1&&types.contains(MoodTaskPointApi.KEYED_DOOR)&&client.world.getBlockEntity(pos) instanceof SmallDoorBlockEntity door&&!door.getKeyName().isEmpty())return Text.translatable(MoodTaskPointApi.getTranslationKey(MoodTaskPointApi.KEYED_DOOR)).copy().append(": ").append(door.getKeyName());MutableText result=Text.empty();ArrayList<Identifier> sorted=new ArrayList<>(types);sorted.sort(java.util.Comparator.comparing(Identifier::toString));for(int i=0;i<sorted.size();i++){if(i>0)result.append(" / ");result.append(Text.translatable(MoodTaskPointApi.getTranslationKey(sorted.get(i))));}return result;}
    private static int blend(Set<Identifier> types){int r=0,g=0,b=0;for(Identifier id:types){int c=MoodTaskPointApi.getColor(id);r+=(c>>16)&255;g+=(c>>8)&255;b+=c&255;}int n=Math.max(1,types.size());return r/n<<16|g/n<<8|b/n;}
    private static Box combinedBox(net.minecraft.world.BlockView world,BlockPos pos,BlockState state){VoxelShape shape=state.getCollisionShape(world,pos,ShapeContext.absent());if(shape.isEmpty())shape=state.getOutlineShape(world,pos,ShapeContext.absent());Box box=shape.isEmpty()?new Box(0,0,0,1,1,1):shape.getBoundingBox();if(state.contains(BedBlock.PART)&&state.contains(BedBlock.FACING)){Direction dir=state.get(BedBlock.PART)==BedPart.FOOT?state.get(BedBlock.FACING):state.get(BedBlock.FACING).getOpposite();return extend(clamp(box),dir);}if(state.getBlock() instanceof SmallDoorBlock&&state.contains(SmallDoorBlock.HALF)){Box c=clamp(box);return state.get(SmallDoorBlock.HALF)==DoubleBlockHalf.LOWER?new Box(c.minX,0,c.minZ,c.maxX,2,c.maxZ):new Box(c.minX,-1,c.minZ,c.maxX,1,c.maxZ);}return box;}
    private static Box clamp(Box b){return new Box(unit(b.minX),unit(b.minY),unit(b.minZ),unit(b.maxX),unit(b.maxY),unit(b.maxZ));}private static double unit(double v){return Math.max(0,Math.min(1,v));}
    private static Box extend(Box b,Direction d){int x=d.getOffsetX(),z=d.getOffsetZ();return new Box(b.minX+Math.min(0,x),b.minY,b.minZ+Math.min(0,z),b.maxX+Math.max(0,x),b.maxY,b.maxZ+Math.max(0,z));}
    private static String heldKeyName(ItemStack stack){if(!stack.isOf(WatheItems.KEY))return null;LoreComponent lore=stack.get(DataComponentTypes.LORE);return lore==null||lore.lines().isEmpty()?null:lore.lines().getFirst().getString();}
    private static boolean matchesDoor(MinecraftClient client,BlockPos pos,String name){return name!=null&&!name.isEmpty()&&client.world.getBlockEntity(pos) instanceof SmallDoorBlockEntity door&&name.equals(door.getKeyName());}
}
