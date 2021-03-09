package net.machinemuse.anima
package bowl

import net.minecraft.advancements.CriteriaTriggers
import net.minecraft.block._
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.entity.item.ItemEntity
import net.minecraft.entity.player.ServerPlayerEntity
import net.minecraft.fluid.Fluids
import net.minecraft.item._
import net.minecraft.util.math.shapes.ISelectionContext
import net.minecraft.util.math.{RayTraceContext, RayTraceResult}
import net.minecraft.util.registry.Registry
import net.minecraft.util.text._
import net.minecraft.util.{Unit => _, _}
import net.minecraft.world.World
import net.minecraftforge.api.distmarker.{Dist, OnlyIn}
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent

import java.{util => ju}

import bowl.BowlContents.{BlockContents, FluidContents}
import constants.BlockStateFlags
import registration.RegistryHelpers.regExtendedItem
import registration.SimpleItems
import salt.SaltLine
import util.Logging
import util.VanillaClassEnrichers.RichBlockState


/**
 * Created by MachineMuse on 2/9/2021.
 */
@EventBusSubscriber(modid = Anima.MODID, bus = Bus.MOD)
object BowlWithContents extends Logging {
  @SubscribeEvent
  def onConstructMod(event: FMLConstructModEvent) = addForgeListeners(onRightClickBlock)

  def onRightClickBlock(event: PlayerInteractEvent.RightClickBlock): Unit = {
    val player = event.getPlayer
    val itemInUse = player.getHeldItem(event.getHand)
    itemInUse.getItem match {
      case Items.BOWL =>
        val raytraceresult = rayTrace(event.getWorld, player, RayTraceContext.FluidMode.ANY)
        raytraceresult.getType match {
          case RayTraceResult.Type.BLOCK =>
            if(player.isSneaking) {
              val ctx = new BlockItemUseContext(new ItemUseContext(player, event.getHand, raytraceresult))
              val state = BowlInWorld.BLOCK.get.getStateForPlacement(ctx)
              event.getWorld.setBlockState(ctx.getPos, state, BlockStateFlags.STANDARD_CLIENT_UPDATE)
              ()
            } else {
              val fluidState = event.getWorld.getBlockState(raytraceresult.getPos).getFluidState
              if(!fluidState.isEmpty) {
                val bowlOfWater = new ItemStack(BOWL_WITH_CONTENTS.get)
                BowlContents.setContents(bowlOfWater, FluidContents(fluidState.getFluid.getDefaultState.getFluid))
                val result = DrinkHelper.fill(itemInUse, player, bowlOfWater, false)
                event.getPlayer.swing(event.getHand, true)
                event.setCanceled(true)
                player.setHeldItem(event.getHand, result)
              }
            }
          case RayTraceResult.Type.ENTITY =>
          case RayTraceResult.Type.MISS =>
        }
      case _ =>
    }
  }

  final private val properties = new Item.Properties().maxStackSize(16).group(SimpleItems.AnimaCreativeGroup).containerItem(Items.BOWL).setISTER(() => BowlRenderers.mkISTER)
  final val BOWL_WITH_CONTENTS = regExtendedItem("bowl_with_contents", () => new BowlWithContents(properties))
  lazy val BOWL_OF_WATER = BowlContents.mkBowlWith(Fluids.WATER)
  lazy val BOWL_OF_SALT = BowlContents.mkBowlWith(SaltLine.SALT_LINE_BLOCK.get)



  def tryPlace(context: BlockItemUseContext, blockstate: BlockState): ActionResultType = {
    if (!context.canPlace || context == null || blockstate == null || !canPlace(context, blockstate)) {
      ActionResultType.FAIL
    } else {
      if (!context.getWorld.setBlockState(context.getPos, blockstate, BlockStateFlags.STANDARD_CLIENT_UPDATE)) {
        ActionResultType.FAIL
      } else {
        val blockpos = context.getPos
        val world = context.getWorld
        val player = context.getPlayer
        val itemstack = context.getItem
        context.getWorld.getTileEntity(context.getPos).optionallyDoAs[BowlInWorldTileEntity](
          _.contents = BowlContents.getContents(itemstack)
        )
        player.optionallyDoAs[ServerPlayerEntity](serverPlayer =>
          CriteriaTriggers.PLACED_BLOCK.trigger(serverPlayer, blockpos, itemstack))
        val soundtype = blockstate.getSoundType(world, blockpos, context.getPlayer)
        world.playSound(player, blockpos, blockstate.getSoundType(world, blockpos, player).getPlaceSound,
          SoundCategory.BLOCKS, (soundtype.getVolume + 1.0F) / 2.0F, soundtype.getPitch * 0.8F)
        if (player == null) { // a dispenser or something
          itemstack.shrink(1)
          world.onServer {serverWorld =>
            val replacement = new ItemStack(Items.BOWL)
            val itementity = new ItemEntity(serverWorld, blockpos.getX, blockpos.getY + 1.0, blockpos.getZ, replacement)
            itementity.setDefaultPickupDelay()
            serverWorld.addEntity(itementity)
          }
        } else if(!player.isCreative){
          val replacement = new ItemStack(Items.BOWL)
          val result = DrinkHelper.fill(itemstack, player, replacement, false)
          player.setHeldItem(context.getHand, result)
        }
        // Success or Consume depending on the side
        ActionResultType.func_233537_a_(world.isRemote)
      }
    }
  }

  def canPlace(context: BlockItemUseContext, blockstate: BlockState) = {

  val playerentity = context.getPlayer
    val selectioncontext = if (playerentity == null) ISelectionContext.dummy else ISelectionContext.forEntity(playerentity)
    blockstate.isValidPosition(context.getWorld, context.getPos) &&
      context.getWorld.placedBlockCollides(blockstate, context.getPos, selectioncontext)
  }

  class BowlWithContents(properties: Item.Properties) extends Item(properties) {

    @OnlyIn(Dist.CLIENT)
    override def addInformation(stack : ItemStack, world : World, tip : ju.List[ITextComponent], flag : ITooltipFlag): Unit ={
      val tipLine = /*new StringTextComponent("Contains: ") append */BowlContents.getContents(stack).getDisplayName
      tip.add(tipLine.mergeStyle(TextFormatting.GRAY, TextFormatting.ITALIC))
    }

    override def fillItemGroup(group : ItemGroup, list : NonNullList[ItemStack]): Unit = {
      if(isInGroup(group)) {
        Registry.FLUID.forEach { fluid =>
          if (fluid.isSource(fluid.getDefaultState)) {
            list.add(BowlContents.mkBowlWith(FluidContents(fluid)))
          }
        }
        list.add(BowlContents.mkBowlWith(BlockContents(SaltLine.SALT_LINE_BLOCK.get)))
      }
    }

    override def onItemUse(context: ItemUseContext): ActionResultType = {
      if(context.getPlayer.isSneaking) {
        val bcontext = new BlockItemUseContext(context)
        val state = BowlInWorld.BLOCK.get.getStateForPlacement(bcontext)
        val result = tryPlace(bcontext, state)
        result
      } else {
        BowlContents.getContents(context.getItem) match {
          case BowlContents.NoContents =>
            ActionResultType.PASS
          case BowlContents.FluidContents(fluid) =>
            val bcontext = new BlockItemUseContext(context)

            val blockstate = fluid.getDefaultState.getBlockState
            tryPlace(bcontext, blockstate).andDo {
              case ActionResultType.SUCCESS =>
                val newBlockState = context.getWorld.getBlockState(context.getPos)
                if(newBlockState.hasProperty(FlowingFluidBlock.LEVEL)) {
                  context.getWorld.setBlockState(context.getPos, newBlockState.updated(FlowingFluidBlock.LEVEL, 2))
                }
              case ActionResultType.CONSUME =>
              case ActionResultType.PASS =>
              case ActionResultType.FAIL =>
            }
          case BowlContents.ItemContents(itemStack) =>
            ActionResultType.PASS
          case BowlContents.BlockContents(block) =>
            val bcontext = new BlockItemUseContext(context)
            val blockstate = block.getStateForPlacement(bcontext)
            tryPlace(bcontext, blockstate)
          case _ =>
            ActionResultType.PASS
        }
      }
    }

  }

}
