package net.machinemuse.anima
package campfire

import net.minecraft.block.CampfireBlock
import net.minecraft.entity.SpawnReason
import net.minecraft.item._
import net.minecraft.tileentity.CampfireTileEntity
import net.minecraft.util.math.vector.Vector3d
import net.minecraft.util.text.TranslationTextComponent
import net.minecraft.util.{ActionResultType, Direction}
import net.minecraft.world.server.ServerWorld
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus
import net.minecraftforge.fml.event.lifecycle.{FMLConstructModEvent, GatherDataEvent}
import org.apache.logging.log4j.scala.Logging

import scala.util.Random

import constants.BlockStateFlags
import entity.EntityLightSpirit
import registration.ParticlesGlobal.AnimaParticleData
import registration.RegistryHelpers._
import registration.SimpleItems._
import util.Colour
import util.DatagenHelpers.mkLanguageProvider

/**
 * Created by MachineMuse on 1/24/2021.
 */

object DustForCampfire {
  @SubscribeEvent def onConstructMod(event: FMLConstructModEvent) = {} // Ensures the class gets initialized when the mod is constructed

  final val CAMPFIRE_DUST_ITEM = regExtendedItem("campfiredust", () => new DustForCampfire)


  @SubscribeEvent def gatherData(implicit event: GatherDataEvent): Unit = {
    mkLanguageProvider("en_us") { lang =>
      lang.addItem(CAMPFIRE_DUST_ITEM, "Campfire Dust")
    }
    mkLanguageProvider("fr_fr") { lang =>
      lang.addItem(CAMPFIRE_DUST_ITEM, "Poussière pour Feu de Camp")
    }
  }
}

// counterintuitively, this will autosubscribe all the methods annotated with @SubscribeEvent in the companion object above.
@EventBusSubscriber(modid = Anima.MODID, bus = Bus.MOD)
class DustForCampfire extends Item(new Item.Properties().group(AnimaCreativeGroup)) with Logging {
  // Called when this item is used when targeting a Block
  override def onItemUse(context: ItemUseContext): ActionResultType = {
    val pos = context.getPos
    val world = context.getWorld
    val oldState = world.getBlockState(pos)
    if(oldState.getBlock.isInstanceOf[CampfireBlock]) {
      if(oldState.getBlock.isInstanceOf[CampfirePlus]) {
        world.onServer { serverWorld =>
          doSpawnParticles(context, serverWorld)
          serverWorld.getTileEntity(pos).optionallyDoAs[campfire.CampfirePlusTileEntity]{ te =>
            te.activeDusts = te.activeDusts ++ CampfirePlusTileEntity.dustInfoFromItemStack(context.getItem).toList
          }
        }
        ActionResultType.SUCCESS
      } else {
        world.onServer { serverWorld =>
          val newTileEntity = CampfirePlusTileEntity.getType.create()
          val newState = CampfirePlus.getBlock.getCopiedState(oldState)

          val oldTileEntity = world.getTileEntity(pos)
          world.removeTileEntity(pos)

          world.setBlockState(context.getPos, newState, BlockStateFlags.STANDARD_CLIENT_UPDATE)
          world.setTileEntity(context.getPos, newTileEntity)

          oldTileEntity.optionallyDoAs[CampfireTileEntity] { oldte =>
            newTileEntity.copyOldTE(newState, oldte)
          }
          doSpawnParticles(context, serverWorld)
          newTileEntity.activeDusts = CampfirePlusTileEntity.dustInfoFromItemStack(context.getItem).toList
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

  private def doSpawnParticles(context: ItemUseContext, serverWorld: ServerWorld) = {
    val colour1 = if(context.getItem.hasTag && context.getItem.getTag.contains("colour1")) context.getItem.getTag.getInt("colour1") else DyeColor.WHITE.getColorValue
    val colour2 = if(context.getItem.hasTag && context.getItem.getTag.contains("colour2")) context.getItem.getTag.getInt("colour2") else DyeColor.WHITE.getColorValue
    val target = Vector3d.copy(context.getPos).add(0.5, 0.2, 0.5)
    val origin = target.add(0, 1.0, 0)// context.getPlayer.getEyePosition(1.0F).subtract(0, 0.1, 0)
    for (i <- 0 to 100) {
      val colour = Colour.mixColoursByWeight(colour1, colour2, Random.nextFloat(), Random.nextFloat())
      val skew = new Vector3d(Random.nextDouble() - 0.5, Random.nextDouble() - 0.5, Random.nextDouble() - 0.5) //.mul(1.5, 1.5, 1.5)
      val originSkew = new Vector3d(Random.nextDouble() - 0.5, Random.nextDouble() - 0.5, Random.nextDouble() - 0.5).mul(0.2, 0.2, 0.2)
      val skewedOrigin = origin.add(originSkew)
      val skewedTarget = target.add(skew)
      val offset = skewedTarget.subtract(skewedOrigin)
      val (offX, offY, offZ) = (offset.getX, offset.getY, offset.getZ)
      val (stX, stY, stZ) = (skewedOrigin.getX, skewedOrigin.getY, skewedOrigin.getZ)

      val data = AnimaParticleData(colour = colour, lifeticks = 10 + Random.nextInt(20), gravity = 1.0f, spin = 1.0f, doCollision = true, size = 0.02)
      //particle count 0 uses the offset as a direction vector for speed, otherwise it just spews them in a random direction O.o
      serverWorld.spawnParticle(data, stX, stY, stZ, 0, offX, offY, offZ, Random.nextDouble())
    }
  }
}
