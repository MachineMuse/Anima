package net.machinemuse.anima
package catstatue

import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraft.world.chunk.Chunk
import net.minecraftforge.common.capabilities._
import net.minecraftforge.event.AttachCapabilitiesEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus

import scala.collection.mutable

import catstatue.CatStatueTrackingCapability.TrackingInterface
import registration.RegistryHelpers._
import util.Logging
import util.VanillaCodecs._

/**
 * Created by MachineMuse on 2/12/2021.
 */
object CatStatueTrackingCapability extends Logging {
//  @SubscribeEvent def onConstructMod(e: FMLConstructModEvent) = {
//    addGenericListener
//  }

  @SubscribeEvent def attachCapabilities(event: AttachCapabilitiesEvent[Chunk]) = {
    event.addCapability(
      modLoc("cat_statue_tracking"),
      mkCapabilityProviderWithSaveData[TrackingInterface](getCapability, () => new CatStatueTrackingCapability)
    )
  }


  @CapabilityInject(classOf[TrackingInterface])
  private var CAPABILITY: Capability[TrackingInterface] = null
  def getCapability = Option(CAPABILITY).getOrElse {
    logger.error("Cat statue capability injection failed")
    ???
  }

  trait TrackingInterface extends SavedData[Set[BlockPos]]{
    def getData: Set[BlockPos]
    def getScaryCatStatues(world: World): Set[BlockPos]
    def putCatStatue(blockPos: BlockPos): Unit
    def putData(data: Set[BlockPos]): Unit
    def removeCatStatue(blockPos: BlockPos): Unit
  }


  regCapWithStorage[Set[BlockPos], TrackingInterface](() => new CatStatueTrackingCapability)
}

@EventBusSubscriber(modid = Anima.MODID, bus = Bus.FORGE)
class CatStatueTrackingCapability extends TrackingInterface {

  private val catStatueSet: mutable.HashSet[BlockPos] = mutable.HashSet.empty

  override def getData: Set[BlockPos] = catStatueSet.toSet

  override def getScaryCatStatues(world: World): Set[BlockPos] = {
    catStatueSet.filter{pos =>
      val blockState = world.getBlockState(pos)
      blockState.get(CatStatue.WATERLEVEL) > 0 && blockState.get(CatStatue.LIT)
    }.toSet
  }

  override def putCatStatue(blockPos: BlockPos): Unit = catStatueSet.add(blockPos)

  override def removeCatStatue(blockPos: BlockPos): Unit = catStatueSet.remove(blockPos)

  override def putData(data: Set[BlockPos]): Unit = catStatueSet.addAll(data)
}
