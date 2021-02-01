package net.machinemuse.anima
package item
package campfire

import registration.RegistryHelpers._

import net.minecraft.block._
import net.minecraft.block.material.{Material, MaterialColor}
import net.minecraft.data.BlockTagsProvider
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.DyeItem
import net.minecraft.state.Property
import net.minecraft.state.properties.BlockStateProperties
import net.minecraft.tags.BlockTags
import net.minecraft.util.math.{BlockPos, BlockRayTraceResult}
import net.minecraft.util.{Unit => _, _}
import net.minecraft.world.{IBlockReader, World}
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.event.lifecycle.{FMLConstructModEvent, GatherDataEvent}

import scala.jdk.OptionConverters._

/**
 * Created by MachineMuse on 1/24/2021.
 */

object CampfirePlus {
  // Init (Required for registration to work)
  // Use event.enqueueWork{ () => doStuff() } if you need to run any actual mutating code in here
  @SubscribeEvent def init(e: FMLConstructModEvent) = {}

  // Add CampfirePlus to the list of valid Campfires so e.g. flint & steel can light them if doused
  @SubscribeEvent def gatherData(event: GatherDataEvent): Unit = {
    event.getGenerator.addProvider(
      new BlockTagsProvider(event.getGenerator, Anima.MODID, null) {
        override def registerTags(): Unit = {
          this.getOrCreateBuilder(BlockTags.CAMPFIRES).add(CampfirePlus.getBlock)
        }
      }
    )
  }

  private val DATA_NAME = "campfireplus"
  // Block and BlockItem registration
  private val CAMPFIREPLUS_BLOCK = regBlock(DATA_NAME, () => new CampfirePlus(
    false, 1,
    AbstractBlock.Properties.create(Material.WOOD, MaterialColor.OBSIDIAN)
      .hardnessAndResistance(2.0F)
      .sound(SoundType.WOOD)
      .setLightLevel((state: BlockState) => if (state.get(BlockStateProperties.LIT)) 15 else 0)
      .notSolid
  ))
  def getBlock = CAMPFIREPLUS_BLOCK.get()

  private val CAMPFIREPLUS_ITEM = regSimpleBlockItem(DATA_NAME, CampfirePlus.CAMPFIREPLUS_BLOCK)
  def getBlockItem = CAMPFIREPLUS_ITEM.get()
}

@Mod.EventBusSubscriber(modid = Anima.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
class CampfirePlus(smokey: Boolean, fireDamage: Int, properties: AbstractBlock.Properties) extends CampfireBlock(smokey, fireDamage, properties) {

  override def createNewTileEntity(worldIn: IBlockReader) = new CampfirePlusTileEntity


  def getCopiedState(oldState: BlockState): BlockState = {
    // this function gets the property from the old state, and if it exists, overwrites the new state with the old property.
    def apply[T <: Comparable[T]](prop: Property[T]): BlockState => BlockState = oldState.func_235903_d_(prop).toScala.foldId(_.`with`(prop, _))

    val f = apply(BlockStateProperties.WATERLOGGED) andThen apply(BlockStateProperties.SIGNAL_FIRE) andThen apply(BlockStateProperties.LIT) andThen apply(BlockStateProperties.HORIZONTAL_FACING)
    f(this.getDefaultState)
  }

  override def onBlockActivated(blockstate : BlockState, world : World, blockpos : BlockPos, playerentity : PlayerEntity, hand : Hand, blockRayTraceResult : BlockRayTraceResult): ActionResultType = {
    val heldItem = playerentity.getHeldItem(hand)
    heldItem.getItem.optionallyAs[DyeItem].fold {
      // Item is not dye
      super.onBlockActivated(blockstate, world, blockpos, playerentity, hand, blockRayTraceResult)
    } { dyeItem => // Item is dye!
      world.getTileEntity(blockpos).optionallyDoAs[CampfirePlusTileEntity] { campfire =>
        campfire.colour1 = campfire.colour2
        campfire.colour2 = dyeItem.getDyeColor.getTextColor // Colour.mixColours(dyeItem.getDyeColor.getTextColor, campfire.colour, 1.0F/1.0F)
      }
      ActionResultType.SUCCESS
    }
  }
}