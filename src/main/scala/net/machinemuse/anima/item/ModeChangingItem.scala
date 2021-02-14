package net.machinemuse.anima
package item

import net.minecraft.item.ItemStack
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent
import net.minecraftforge.fml.network.NetworkEvent
import org.apache.logging.log4j.scala.Logging

import Network._

/**
 * Created by MachineMuse on 1/31/2021.
 */

object ModeChangingItem extends Logging{
  @SubscribeEvent
  def onConstructMod(event: FMLConstructModEvent) = event.doOnMainThread{ () =>
    Network.registerCaseMessage[ModeChangePacket](CaseNetCodec1(ModeChangePacket.apply, ModeChangePacket.unapply))
  }
  case class ModeChangePacket(delta: Int) extends CasePacket {
    def handle(context: NetworkEvent.Context) = {
      val player = context.getSender
      val item = player.inventory.getCurrentItem
      item.getItem.optionallyDoAs[ModeChangingItem[_]]{mci =>
        logger.trace(s"Mode change packet received: delta=$delta, item=$item")
        mci.tryChangingModes(delta, item)
      }
    }
  }
}

@EventBusSubscriber(modid = Anima.MODID, bus = Bus.MOD)
trait ModeChangingItem[T] extends Logging {

  def getDefaultMode(stack: ItemStack): T
  def getValidModes(stack: ItemStack): Seq[T]
  def accessor: NBTTagAccessor[T]

  def getCurrentMode(stack: ItemStack): T = stack.getOrCreateTag().getT("selected") (accessor)// returns 0 if tag doesn't exist
  def setCurrentMode(stack: ItemStack, i: T): Unit = stack.getOrCreateTag().putT("selected", i)(accessor)

  def tryChangingModes(delta: Int, bag: ItemStack) = {
    val modes = getValidModes(bag)
    val indices = modes.zipWithIndex
    logger.trace("indices: " + indices)
    if(delta > 0) {
      val nextIx = modes.indexOf(getCurrentMode(bag)) + delta
      val loopix = if(nextIx.isFromUntil(0, modes.size)) nextIx else 0
      val closestixFound: Option[T] = indices.find(_._2 >= loopix).map(_._1)
      val closestix = closestixFound.getOrElse(getDefaultMode(bag))
      setCurrentMode(bag, closestix)
    } else if(delta < 0) {
      val nextIx = modes.indexOf(getCurrentMode(bag)) + delta
      val loopix = if(nextIx.isFromUntil(0, modes.size)) nextIx else modes.size - 1
      val closestixFound: Option[T] = indices.reverse.find(_._2 <= loopix).map(_._1)
      val closestix = closestixFound.getOrElse(getDefaultMode(bag))
      setCurrentMode(bag, closestix)
    }

  }
}
