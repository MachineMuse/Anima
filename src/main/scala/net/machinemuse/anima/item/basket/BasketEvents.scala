package net.machinemuse.anima.item.basket

import net.machinemuse.anima.util.OptionCast._
import net.minecraftforge.event.entity.player.EntityItemPickupEvent
import net.minecraftforge.eventbus.api.Event.Result
import net.minecraftforge.eventbus.api.SubscribeEvent
import org.apache.logging.log4j.LogManager

import scala.jdk.CollectionConverters._

/**
 * Created by MachineMuse on 1/23/2021.
 */
class BasketEvents {
  private val LOGGER = LogManager.getLogger
  @SubscribeEvent
  def onEntityItemPickup(event: EntityItemPickupEvent): Unit = {
    LOGGER.info("item picked up")
    for { slotStack <- event.getPlayer.inventory.mainInventory.asScala ++ event.getPlayer.inventory.offHandInventory.asScala
          basket <- slotStack.getItem.optionallyAsList[Basket]
          } {
        val remainder = basket.insertItem(slotStack, event.getItem.getItem)
        event.getItem.setItem(remainder)
    }
    val remainder = event.getItem.getItem
    if(remainder.isEmpty) { // Whole stack was consumed in adding to basket
      event.setResult(Result.ALLOW) // process achievements etc. but skip adding the item to main inventory
    }
  }

}
