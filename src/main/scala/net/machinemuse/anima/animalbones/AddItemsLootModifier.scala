package net.machinemuse.anima
package animalbones

import net.minecraft.item._
import net.minecraft.loot.conditions.ILootCondition
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent
import org.apache.logging.log4j.scala.Logging

import scala.util.Random

import registration.RegistryHelpers._
import util.GenCodecsByName._
import util.VanillaCodecs._

/**
 * Created by MachineMuse on 2/14/2021.
 */
@EventBusSubscriber(modid = Anima.MODID, bus = Bus.MOD)
object AddItemsLootModifier extends Logging {
  @SubscribeEvent def onConstructMod(event: FMLConstructModEvent) = {}

  case class AddItemsLootData(itemToAdd: Item,
                              minCount: Int,
                              maxCount: Int) extends CodecByName

  /*_*/
  private val SERIALIZER = regLootModifierSerializer("add_items", mkLootModifier)
  def getSerializer = SERIALIZER.get
  /*_*/

  def mkLootModifier(data: AddItemsLootData, conditions: Array[ILootCondition]) = mkSimpleLootModifier(data, conditions) {
    case (generatedLoot, context) =>
      generatedLoot.andDo(_.add(new ItemStack(data.itemToAdd, Random.between(data.minCount, data.maxCount + 1))))
  }
}