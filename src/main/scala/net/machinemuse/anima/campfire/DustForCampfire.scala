package net.machinemuse.anima
package campfire

import entity.EntityLightSpirit
import registration.RegistryHelpers._
import registration.SimpleItems._
import util.BlockStateFlags

import net.minecraft.block.CampfireBlock
import net.minecraft.entity.SpawnReason
import net.minecraft.item._
import net.minecraft.tileentity.CampfireTileEntity
import net.minecraft.util.text.TranslationTextComponent
import net.minecraft.util.{ActionResultType, Direction}
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent
import org.apache.logging.log4j.scala.Logging

/**
 * Created by MachineMuse on 1/24/2021.
 */

object DustForCampfire {
  @SubscribeEvent def onConstructMod(event: FMLConstructModEvent) = {} // Ensures the class gets initialized when the mod is constructed

  final val instance = regExtendedItem("campfiredust", () => new DustForCampfire)
  def getInstance = instance.get()
}

// counterintuitively, this will autosubscribe all the methods annotated with @SubscribeEvent in the companion object above.
@Mod.EventBusSubscriber(modid = Anima.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
class DustForCampfire extends Item(new Item.Properties().group(AnimaCreativeGroup)) with Logging {
  // Called when this item is used when targeting a Block
  override def onItemUse(context: ItemUseContext): ActionResultType = {
    val pos = context.getPos
    val world = context.getWorld
    val oldState = world.getBlockState(pos)
    if(oldState.getBlock.isInstanceOf[CampfireBlock]) {
      if(oldState.getBlock.isInstanceOf[CampfirePlus]) {
        ActionResultType.SUCCESS
      } else {
        val newTileEntity = CampfirePlusTileEntity.getType.create()
        val newState = CampfirePlus.getBlock.getCopiedState(oldState)

        val oldTileEntity = world.getTileEntity(pos)
        world.removeTileEntity(pos)

        world.setBlockState(context.getPos, newState, BlockStateFlags.STANDARD_CLIENT_UPDATE)
        world.setTileEntity(context.getPos, newTileEntity)

        oldTileEntity.optionallyDoAs[CampfireTileEntity] { oldte =>
          newTileEntity.copyOldTE(newState, oldte)
        }

        ActionResultType.SUCCESS
      }
    } else {
      world.onServer { serverWorld =>
        val blockAbove = pos.offset(Direction.UP)
        val itemstack = context.getItem
        val newEnt = EntityLightSpirit.getType.spawn(serverWorld, itemstack.getTag, new TranslationTextComponent("lightspirit"), context.getPlayer, blockAbove, SpawnReason.SPAWN_EGG, true, true)
        if(newEnt != null) {
          newEnt.homeblock := blockAbove
          itemstack.shrink(1)
        }
        logger.debug("new entity " + newEnt + " created")
      }
      ActionResultType.SUCCESS
//      super.onItemUse(context)
    }
  }
}
