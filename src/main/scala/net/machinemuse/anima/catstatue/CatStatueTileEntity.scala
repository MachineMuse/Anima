package net.machinemuse.anima
package catstatue

import catstatue.CatStatueTileEntity.BOILAWAY_EVERY
import registration.RegistryHelpers.regTE
import util.VanillaClassEnrichers.RichBlockState

import net.minecraft.block.BlockState
import net.minecraft.inventory.IClearable
import net.minecraft.nbt.CompoundNBT
import net.minecraft.particles.ParticleTypes
import net.minecraft.tileentity._
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent
import org.apache.logging.log4j.scala.Logging

import scala.util.Random

/**
 * Created by MachineMuse on 2/11/2021.
 */
object CatStatueTileEntity extends Logging {

  private val BOILAWAY_EVERY = 5.minutesInTicks

  @SubscribeEvent def onConstructMod(e: FMLConstructModEvent) = {}

  val TYPE = regTE[CatStatueTileEntity]("cat_statue", () => new CatStatueTileEntity, CatStatueBlock.CAT_STATUE_BLOCK)
}

@EventBusSubscriber(modid = Anima.MODID, bus = Bus.MOD)
class CatStatueTileEntity extends TileEntity(CatStatueTileEntity.TYPE.get) with IClearable with ITickableTileEntity with Logging {
  private var BURNTIME = 0
  private var BOILTIME = 0

  override def clear(): Unit = {
    BURNTIME = 0
    BOILTIME = 0
  }

  def addFuel(burnTime: Int) = {
    BURNTIME += burnTime
    world.setBlockState(pos, getBlockState.updated(CatStatueBlock.LIT, true))
  }

  override def tick(): Unit = {
    world.onServer{ serverWorld =>
      if(BURNTIME > 0) {
        BURNTIME -= 1
        val myChunk = world.getChunkAt(pos)
        val capability = CatStatueTrackingCapability.CAT_STATUE_TRACKING_CAPABILITY.getOrElse {
          logger.error("Cat statue capability injection failed")
          ???
        }
        if(getBlockState.get(CatStatueBlock.WATERLEVEL) > 0) {
          val myCap = myChunk.getCapability(capability).resolve().get()
          myCap.putCatStatue(pos)

          BOILTIME += 1
          if (BOILTIME > BOILAWAY_EVERY) {
            val newWaterLevel = getBlockState.get(CatStatueBlock.WATERLEVEL) - 1
            world.setBlockState(pos, getBlockState.updated(CatStatueBlock.WATERLEVEL, newWaterLevel))
            BOILTIME = 0
            if(newWaterLevel <= 0) {
              myCap.removeCatStatue(pos)
            }
          }
        }
        if(BURNTIME == 0) {
          val myCap = myChunk.getCapability(capability).resolve().get()
          myCap.removeCatStatue(pos)
        }
        this.markDirty()
      } else {
        world.setBlockState(pos, getBlockState.updated(CatStatueBlock.LIT, false))
        this.markDirty()
      }

    }
    world.onClient {clientworld =>
      val state = world.getBlockState(pos)
      if(state.get(CatStatueBlock.LIT)) {
        val direction = state.get(CatStatueBlock.FACING).getDirectionVec
        val x = pos.getX + 0.5 + direction.getX * 0.5
        val y = pos.getY + 1.0 + direction.getY * 0.5
        val z = pos.getZ + 0.5 + direction.getZ * 0.5
        clientworld.addParticle(ParticleTypes.BUBBLE, x, y, z, Random.nextDouble()*0.2 - 0.1, Random.nextDouble()*0.2 - 0.1, Random.nextDouble()*0.2 - 0.1)
      }
    }
  }


  override def remove(): Unit = {
    val myChunk = world.getChunkAt(pos)
    val capability = CatStatueTrackingCapability.CAT_STATUE_TRACKING_CAPABILITY.getOrElse {
      logger.error("Cat statue capability injection failed")
      ???
    }
    val myCap = myChunk.getCapability(capability).resolve().get()
    myCap.removeCatStatue(pos)
    super.remove()
  }

  override def read(state: BlockState, nbt: CompoundNBT): Unit = {
    BURNTIME = nbt.getInt("burntime")
    super.read(state, nbt)
  }

  override def write(compound: CompoundNBT): CompoundNBT = {
    super.write(compound.andDo(_.putInt("burntime", BURNTIME)))
  }
}
