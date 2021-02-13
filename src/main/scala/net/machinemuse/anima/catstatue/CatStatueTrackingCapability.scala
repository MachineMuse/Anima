package net.machinemuse.anima
package catstatue

import catstatue.CatStatueTrackingCapability.TrackingInterface
import util.VanillaCodecs._

import com.mojang.serialization.Codec
import net.minecraft.nbt.{INBT, ListNBT}
import net.minecraft.util.Direction
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraft.world.chunk.Chunk
import net.minecraftforge.common.capabilities._
import net.minecraftforge.common.util.{INBTSerializable, LazyOptional}
import net.minecraftforge.event.AttachCapabilitiesEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus
import org.apache.logging.log4j.scala.Logging

import scala.collection.mutable

/**
 * Created by MachineMuse on 2/12/2021.
 */
object CatStatueTrackingCapability extends Logging {
//  @SubscribeEvent def onConstructMod(e: FMLConstructModEvent) = {
//    addGenericListener
//  }

  class Provider extends ICapabilityProvider with INBTSerializable[INBT] {
    private val localCapInst: TrackingInterface = new CatStatueTrackingCapability

    override def serializeNBT(): INBT = {
      CAT_STATUE_TRACKING_CAPABILITY.fold[INBT](new ListNBT())(cap => cap.writeNBT(localCapInst, Direction.DOWN))
    }

    override def deserializeNBT(nbt: INBT): Unit = {
      CAT_STATUE_TRACKING_CAPABILITY.foreach(cap => cap.readNBT(localCapInst, Direction.DOWN, nbt))
    }

    override def getCapability[T](cap: Capability[T], side: Direction): LazyOptional[T] = {
      CAT_STATUE_TRACKING_CAPABILITY.fold[LazyOptional[T]](LazyOptional.empty()) {truecap =>
        truecap.orEmpty[T](cap, LazyOptional.of[TrackingInterface](() => localCapInst))
      }
    }
  }

  @SubscribeEvent def attachCapabilities(event: AttachCapabilitiesEvent[Chunk]) = {
    event.addCapability(modLoc("cat_statue_tracking"), new Provider)
  }

  var CAT_STATUE_TRACKING_CAPABILITY: Option[Capability[TrackingInterface]] = None

  @CapabilityInject(classOf[TrackingInterface])
  def setCapability(capability: Capability[TrackingInterface]) = {
    logger.info(s"Capability injected: $capability")
    CAT_STATUE_TRACKING_CAPABILITY = capability.some
  }

  trait TrackingInterface {
    def getCatStatues: List[BlockPos]
    def getScaryCatStatues(world: World): List[BlockPos]
    def putCatStatue(blockPos: BlockPos): Unit
    def removeCatStatue(blockPos: BlockPos): Unit
  }

  val DATACODEC = implicitly[Codec[List[BlockPos]]]

  CapabilityManager.INSTANCE.register(classOf[TrackingInterface], new Capability.IStorage[TrackingInterface]{
    override def writeNBT(capability: Capability[TrackingInterface], instance: TrackingInterface, side: Direction): INBT = {
      DATACODEC.writeINBT(instance.getCatStatues)
    }

    override def readNBT(capability: Capability[TrackingInterface], instance: TrackingInterface, side: Direction, nbt: INBT): Unit = {
      val data = DATACODEC.parseINBT(nbt)
      data.foreach(list => list.foreach(instance.putCatStatue))
    }
  }, () => new CatStatueTrackingCapability)
}

@EventBusSubscriber(modid = Anima.MODID, bus = Bus.FORGE)
class CatStatueTrackingCapability extends TrackingInterface with Logging {

  private val catStatueList: mutable.HashSet[BlockPos] = mutable.HashSet.empty

  override def getCatStatues: List[BlockPos] = catStatueList.toList

  override def getScaryCatStatues(world: World): List[BlockPos] = {
    catStatueList.filter{pos =>
      val blockState = world.getBlockState(pos)
      blockState.get(CatStatueBlock.WATERLEVEL) > 0 && blockState.get(CatStatueBlock.LIT)
    }.toList
  }

  override def putCatStatue(blockPos: BlockPos): Unit = catStatueList.add(blockPos)

  override def removeCatStatue(blockPos: BlockPos): Unit = catStatueList.remove(blockPos)

}
