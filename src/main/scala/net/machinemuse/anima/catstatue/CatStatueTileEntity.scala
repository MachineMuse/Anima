package net.machinemuse.anima
package catstatue

import net.minecraft.block.BlockState
import net.minecraft.inventory.IClearable
import net.minecraft.nbt.CompoundNBT
import net.minecraft.particles.ParticleTypes
import net.minecraft.tileentity._
import net.minecraft.util.{SoundCategory, SoundEvents}
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent

import scala.util.Random

import catstatue.CatStatueTileEntity.BOILAWAY_EVERY
import registration.RegistryHelpers.regTE
import util.Logging
import util.VanillaClassEnrichers.RichBlockState

/**
 * Created by MachineMuse on 2/11/2021.
 */
object CatStatueTileEntity extends Logging {

  private val BOILAWAY_EVERY = 5.minutesInTicks // TODO: Configurable

  @SubscribeEvent def onConstructMod(e: FMLConstructModEvent) = {}

  val TYPE = regTE[CatStatueTileEntity]("cat_statue", () => new CatStatueTileEntity, CatStatue.BLOCK)
}

@EventBusSubscriber(modid = Anima.MODID, bus = Bus.MOD)
class CatStatueTileEntity extends TileEntity(CatStatueTileEntity.TYPE.get) with IClearable with ITickableTileEntity {
  private var BURNTIME = 0
  private var BOILTIME = 0

  private var DRIPPING = 0

  override def clear(): Unit = {
    BURNTIME = 0
    BOILTIME = 0
  }

  def addFuel(burnTime: Int) = {
    BURNTIME += burnTime
    world.setBlockState(pos, getBlockState.updated(CatStatue.LIT, true))
  }

  override def tick(): Unit = {
    world.onServer{ serverWorld =>
      if(BURNTIME > 0) {
        BURNTIME -= 1
        val myChunk = world.getChunkAt(pos)
        val capability = CatStatueTrackingCapability.getCapability
        if(getBlockState.get(CatStatue.WATERLEVEL) > 0) {
          val myCap = myChunk.getCapability(capability).resolve().get()
          myCap.putCatStatue(pos)

          BOILTIME += 1
          if (BOILTIME > BOILAWAY_EVERY) {
            val newWaterLevel = getBlockState.get(CatStatue.WATERLEVEL) - 1
            world.setBlockState(pos, getBlockState.updated(CatStatue.WATERLEVEL, newWaterLevel))
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
        world.setBlockState(pos, getBlockState.updated(CatStatue.LIT, false))
        this.markDirty()
      }

    }
    world.onClient {clientworld =>
      val state = world.getBlockState(pos)
      val direction = state.get(CatStatue.FACING).getDirectionVec
      val lit = state.get(CatStatue.LIT)
      val hasWater = state.get(CatStatue.WATERLEVEL) > 0
      if(lit && Random.nextInt(20) == 0) {
        val x = pos.getX + 0.5 + direction.getX * 6.0/16.0
        val y = pos.getY + (0.25/16.0) + direction.getY * 0.5
        val z = pos.getZ + 0.5 + direction.getZ * 6.0/16.0
        val vx = Random.nextDouble()*0.02 - 0.01
        val vy = Random.nextDouble()*0.02
        val vz = Random.nextDouble()*0.02 - 0.01
        clientworld.addParticle(ParticleTypes.FLAME, x, y, z, vx, vy, vz)
      }
      if(DRIPPING != 0) {
        DRIPPING -= 1
        if(DRIPPING == 0){
          val x = pos.getX + 0.5 + direction.getX * 6.0/16.0
          val y = pos.getY + (0.25/16.0) + direction.getY * 0.5
          val z = pos.getZ + 0.5 + direction.getZ * 6.0/16.0

          val soundEvent = SoundEvents.BLOCK_LAVA_EXTINGUISH
          val category = SoundCategory.BLOCKS
          clientworld.playSound(x, y, z, soundEvent, category, 0.5F, 2.0F, false)

          val vx = Random.nextDouble()*0.02 - 0.01
          val vy = Random.nextDouble()*0.02
          val vz = Random.nextDouble()*0.02 - 0.01
          clientworld.addParticle(ParticleTypes.SMOKE, x, y, z, vx, vy, vz)
        }
      }
      if(hasWater && lit && DRIPPING == 0 && Random.nextInt(50) == 0) {
        val x = pos.getX + 0.5 + direction.getX * 5.5/16.0
        val y = pos.getY + 9.0/16.0
        val z = pos.getZ + 0.5 + direction.getZ * 5.5/16.0
        val vx = Random.nextDouble()*0.02 - 0.01
        val vy = -0.01
        val vz = Random.nextDouble()*0.02 - 0.01
        clientworld.addParticle(ParticleTypes.DRIPPING_WATER, x, y, z, vx, vy, vz)
        DRIPPING = 50
      }

    }
  }

  override def remove(): Unit = {
    val myChunk = world.getChunkAt(pos)
    val capability = CatStatueTrackingCapability.getCapability
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
