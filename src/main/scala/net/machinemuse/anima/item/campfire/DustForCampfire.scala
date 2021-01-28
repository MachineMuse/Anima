package net.machinemuse.anima.item.campfire

import net.machinemuse.anima.entity.EntityLightSpirit
import net.machinemuse.anima.registration.AnimaRegistry.{AnimaCreativeGroup, CAMPFIREPLUS_BLOCK, CAMPFIREPLUS_TE, ENTITY_LIGHT_SPIRIT}
import net.machinemuse.anima.util.{BlockStateFlags, OptionCast}
import net.minecraft.block.CampfireBlock
import net.minecraft.entity.SpawnReason
import net.minecraft.item._
import net.minecraft.tileentity.CampfireTileEntity
import net.minecraft.util.ActionResultType
import net.minecraft.world.server.ServerWorld
import org.apache.logging.log4j.LogManager

/**
 * Created by MachineMuse on 1/24/2021.
 */
class DustForCampfire extends Item(new Item.Properties().group(AnimaCreativeGroup)) {
  private val LOGGER = LogManager.getLogger

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

        OptionCast[CampfireTileEntity](oldTileEntity).foreach(oldte =>
          newTileEntity.init(newState, oldte)
        )

        ActionResultType.SUCCESS
      }
    } else {
      if(!world.isRemote) {
        OptionCast[ServerWorld](world).foreach{sw =>
          val blockAbove = pos.up()
          val itemstack = context.getItem
          val newEnt = ENTITY_LIGHT_SPIRIT.get().spawn(sw, itemstack, context.getPlayer, blockAbove, SpawnReason.SPAWN_EGG, true, true).asInstanceOf[EntityLightSpirit]
          if(newEnt != null) {
            newEnt.homeblock.set(blockAbove)
            itemstack.shrink(1)
          }
          LOGGER.info("new entity " + newEnt + " created")
        }
      }
      ActionResultType.SUCCESS
//      super.onItemUse(context)
    }
  }
}
