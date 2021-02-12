package net.machinemuse.anima
package bowl

import registration.RegistryHelpers.{regExtendedItem, regNamedBlockItem}
import registration.SimpleItems
import salt.SaltLine
import util.BlockStateFlags
import util.DatagenHelpers._

import net.minecraft.block.{Blocks, FlowingFluidBlock}
import net.minecraft.fluid.Fluids
import net.minecraft.item._
import net.minecraft.util.DrinkHelper
import net.minecraft.util.math.{RayTraceContext, RayTraceResult}
import net.minecraftforge.api.distmarker.{Dist, OnlyIn}
import net.minecraftforge.client.event.ColorHandlerEvent
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus
import net.minecraftforge.fml.event.lifecycle.{FMLConstructModEvent, GatherDataEvent}
import org.apache.logging.log4j.scala.Logging

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
                event.setCanceled(true)
                player.setHeldItem(event.getHand, result)
            case _ =>
          }
          case RayTraceResult.Type.ENTITY =>
          case RayTraceResult.Type.MISS =>
        }
      case BOWL_OF_WATER =>
        val raytraceresult = rayTrace(event.getWorld, player, RayTraceContext.FluidMode.ANY)
        raytraceresult.getType match {
          case RayTraceResult.Type.BLOCK =>
            val direction = raytraceresult.getFace
            val blockPos = raytraceresult.getPos.offset(direction)
            val blockState = event.getWorld.getBlockState(blockPos)
            if (event.getWorld.isBlockModifiable(player, blockPos) &&
              player.canPlayerEdit(blockPos, direction, itemInUse) &&
              blockState.isReplaceable(Fluids.FLOWING_WATER)
            ) {
              event.getWorld.setBlockState(blockPos, Blocks.WATER.getDefaultState.`with`(FlowingFluidBlock.LEVEL, Integer.valueOf(1)), BlockStateFlags.STANDARD_CLIENT_UPDATE)
              val emptyBowl = new ItemStack(Items.BOWL)
              val result = DrinkHelper.fill(itemInUse, player, emptyBowl, false)
              event.setCanceled(true)
              player.setHeldItem(event.getHand, result)
            }

          case RayTraceResult.Type.ENTITY =>
          case RayTraceResult.Type.MISS =>
        }
//      case BOWL_OF_SALT =>
//        val raytraceresult = rayTrace(event.getWorld, player, RayTraceContext.FluidMode.ANY)
      case _ =>

    }
  }
  @OnlyIn(Dist.CLIENT) @SubscribeEvent def onItemColorEvent(event: ColorHandlerEvent.Item) = {
    event.getItemColors.register(BowlContentsColourer, BowlWithContents.BOWL_OF_WATER.get)
  }

  val properties = new Item.Properties().maxStackSize(16).group(SimpleItems.AnimaCreativeGroup).containerItem(Items.BOWL)
  val BOWL_OF_WATER = regExtendedItem("bowl_of_water", () => new BowlWithContents(properties, Fluids.WATER.getAttributes.getColor))
  val BOWL_OF_SALT = regNamedBlockItem("bowl_of_salt", SaltLine.SALT_LINE_BLOCK.registryObject)

  @SubscribeEvent def onGatherData(implicit event: GatherDataEvent): Unit = {
    mkRecipeProvider({ consumer =>
          CampfireRecipeBuilder.campfireRecipe(BOWL_OF_WATER.get, BOWL_OF_SALT.get, 0.1F)
            .buildProperly(consumer, "salt_from_water_campfire")
        })
    mkLanguageProvider("en_us")({ lang =>
          lang.addItem(BOWL_OF_WATER.registryObject, "Bowl of Water")
          lang.addItem(BOWL_OF_SALT.registryObject, "Bowl of Salt")
        })
    mkLanguageProvider("fr_fr")({ lang =>
          lang.addItem(BOWL_OF_WATER.registryObject, "Bol d'Eau")
          lang.addItem(BOWL_OF_SALT.registryObject, "Bol de Sel")
        })
  }
}

@EventBusSubscriber(modid = Anima.MODID, bus = Bus.MOD)
class BowlWithContents(properties: Item.Properties, val contentsColour: Int) extends Item(properties) with Logging {
}
