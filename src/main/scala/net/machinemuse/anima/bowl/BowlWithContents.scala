package net.machinemuse.anima
package bowl

import net.minecraft.advancements.CriteriaTriggers
import net.minecraft.block._
import net.minecraft.entity.item.ItemEntity
import net.minecraft.entity.player.ServerPlayerEntity
import net.minecraft.fluid.Fluids
import net.minecraft.item._
import net.minecraft.util._
import net.minecraft.util.math.shapes.ISelectionContext
import net.minecraft.util.math.{RayTraceContext, RayTraceResult}
import net.minecraftforge.api.distmarker.{Dist, OnlyIn}
import net.minecraftforge.client.event.ColorHandlerEvent
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus
import net.minecraftforge.fml.event.lifecycle.{FMLConstructModEvent, GatherDataEvent}

import bowl.BowlWithContents.{BOWL_OF_SALT, BOWL_OF_WATER}
import constants.BlockStateFlags
import registration.RegistryHelpers.regExtendedItem
import registration.SimpleItems
import salt.SaltLine
import util.DatagenHelpers._
import util.Logging
import util.VanillaClassEnrichers.RichBlockState


/**
 * Created by MachineMuse on 2/9/2021.
 */
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
            val fluidState = event.getWorld.getBlockState(raytraceresult.getPos).getFluidState
            fluidState.getFluid match {
              case Fluids.WATER | Fluids.FLOWING_WATER =>
                val bowlOfWater = new ItemStack(BOWL_OF_WATER.get)
                val result = DrinkHelper.fill(itemInUse, player, bowlOfWater, false)
                event.getPlayer.swing(event.getHand, true)
                event.setCanceled(true)
                player.setHeldItem(event.getHand, result)
            case _ =>
          }
          case RayTraceResult.Type.ENTITY =>
          case RayTraceResult.Type.MISS =>
        }
      case _ =>

    }
  }
  @OnlyIn(Dist.CLIENT) @SubscribeEvent def onItemColorEvent(event: ColorHandlerEvent.Item) = {
    event.getItemColors.register(BowlContentsColourer, BowlWithContents.BOWL_OF_WATER.get)
  }

  final private val properties = new Item.Properties().maxStackSize(16).group(SimpleItems.AnimaCreativeGroup).containerItem(Items.BOWL)
  final val BOWL_OF_WATER = regExtendedItem("bowl_of_water", () => new BowlWithContents(properties, Fluids.WATER.getAttributes.getColor))
  final val BOWL_OF_SALT = regExtendedItem("bowl_of_salt", () => new BowlWithContents(properties, 0xFFFFFFFF))

  @SubscribeEvent def onGatherData(implicit event: GatherDataEvent): Unit = {
    mkRecipeProvider{ consumer =>
        CampfireRecipeBuilder.campfireRecipe(BOWL_OF_WATER.get, BOWL_OF_SALT.get, 0.1F)
          .buildProperly(consumer, "salt_from_water_campfire")
      }
    mkLanguageProvider("en_us"){ lang =>
        lang.addItem(BOWL_OF_WATER, "Bowl of Water")
        lang.addItem(BOWL_OF_SALT, "Bowl of Salt")
      }
    mkLanguageProvider("fr_fr"){ lang =>
        lang.addItem(BOWL_OF_WATER, "Bol d'Eau")
        lang.addItem(BOWL_OF_SALT, "Bol de Sel")
      }
  }
}

@EventBusSubscriber(modid = Anima.MODID, bus = Bus.MOD)
class BowlWithContents(properties: Item.Properties, val contentsColour: Int) extends Item(properties) with Logging {

  override def onItemUse(context: ItemUseContext): ActionResultType =
    this match {
      case BOWL_OF_WATER(item) =>
        val bcontext = new BlockItemUseContext(context)
        val blockstate = Blocks.WATER.getStateForPlacement(bcontext).updated(FlowingFluidBlock.LEVEL, 1)
        item.tryPlace(bcontext, blockstate)
      case BOWL_OF_SALT(item) =>
        val bcontext = new BlockItemUseContext(context)
        val blockstate = SaltLine.SALT_LINE_BLOCK.get.getStateForPlacement(bcontext)
        item.tryPlace(bcontext, blockstate)
      case _ =>
        ActionResultType.PASS
    }


  def tryPlace(context: BlockItemUseContext, blockstate: BlockState): ActionResultType = {
    if (!context.canPlace || context == null || blockstate == null || !this.canPlace(context, blockstate)) {
      ActionResultType.FAIL
    } else {
      if (!context.getWorld.setBlockState(context.getPos, blockstate, BlockStateFlags.STANDARD_CLIENT_UPDATE)) {
        ActionResultType.FAIL
      } else {
        val blockpos = context.getPos
        val world = context.getWorld
        val player = context.getPlayer
        val itemstack = context.getItem
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
}
