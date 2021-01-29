package net.machinemuse.anima
package item
package campfire

import net.machinemuse.anima.entity.EntityLightSpirit
import net.machinemuse.anima.registration.AnimaRegistry.{AnimaCreativeGroup, CAMPFIREPLUS_BLOCK, CAMPFIREPLUS_TE, ENTITY_LIGHT_SPIRIT}
import net.machinemuse.anima.util.BlockStateFlags
import net.minecraft.block.CampfireBlock
import net.minecraft.entity.SpawnReason
import net.minecraft.item._
import net.minecraft.tileentity.CampfireTileEntity
import net.minecraft.util.ActionResultType
import org.apache.logging.log4j.scala.Logging

/**
 * Created by MachineMuse on 1/24/2021.
 */
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
        val newTileEntity = CAMPFIREPLUS_TE.get.create()
        val newState = CAMPFIREPLUS_BLOCK.get.getCopiedState(oldState)

        val oldTileEntity = world.getTileEntity(pos)
        world.removeTileEntity(pos)

        world.setBlockState(context.getPos, newState, BlockStateFlags.STANDARD_CLIENT_UPDATE)
        world.setTileEntity(context.getPos, newTileEntity)

        oldTileEntity.optionallyDoAs[CampfireTileEntity] { oldte =>
          newTileEntity.init(newState, oldte)
        }

        ActionResultType.SUCCESS
      }
    } else {
      world.onServer { serverWorld =>
        val blockAbove = pos.up()
        val itemstack = context.getItem
        val newEnt = ENTITY_LIGHT_SPIRIT.get().spawn(serverWorld, itemstack, context.getPlayer, blockAbove, SpawnReason.SPAWN_EGG, true, true).asInstanceOf[EntityLightSpirit]
        if(newEnt != null) {
          newEnt.homeblock.set(blockAbove)
          itemstack.shrink(1)
        }
        logger.trace("new entity " + newEnt + " created")
      }
      ActionResultType.SUCCESS
//      super.onItemUse(context)
    }
  }
}
