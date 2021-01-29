package net.machinemuse.anima
package registration

import net.machinemuse.anima.entity.{AirLightBlock, EntityLightSpirit}
import net.machinemuse.anima.gui.BasketContainer
import net.machinemuse.anima.item.basket.Basket
import net.machinemuse.anima.item.campfire.{CampfirePlus, CampfirePlusTileEntity, DustForCampfire}
import net.minecraft.entity.EntityClassification
import net.minecraft.item._
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent

/**
 * Created by MachineMuse on 1/21/2021.
 */
@Mod.EventBusSubscriber(modid = Anima.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
object AnimaRegistry {
  import net.machinemuse.anima.registration.RegistryHelpers._

  @SubscribeEvent
  def init(e: FMLConstructModEvent) = this

  val AnimaCreativeGroup = regCreativeTab(() => SPIRITFIRE_ITEM)

  val KINDLING_ITEM: RegO[Item] = regSimpleItem("kindling")

  val BIRDBONES_ITEM: RegO[Item] = regSimpleItem("birdbones")

  val ANIMALBONES_ITEM: RegO[Item] = regSimpleItem("animalbones")

  val SPIRITFIRE_ITEM: RegO[Item] = regSimpleItem("spiritfire", ItemProperties(creativeGroup = Some(null)).some)

  val BASKET_ITEM: RegO[Basket] = regExtendedItem("basket", () => new Basket)

  val BASKET_CONTAINER: RegCT[BasketContainer] = regContainer("basket", (id, inv, buf) => new BasketContainer(id, inv))

  val DUSTFORCAMPFIRE_ITEM: RegO[DustForCampfire] = regExtendedItem("campfiredust", () => new DustForCampfire)

  val CAMPFIREPLUS_ITEM: RegO[BlockItem] = regExtendedItem("campfireplus", () => new BlockItem(CAMPFIREPLUS_BLOCK.get, new Item.Properties().group(AnimaCreativeGroup)))

  val CAMPFIREPLUS_BLOCK: RegO[CampfirePlus] = regBlock("campfireplus", () => new CampfirePlus)

  val CAMPFIREPLUS_TE: RegTE[CampfirePlusTileEntity] = regTE("campfireplus", () => new CampfirePlusTileEntity, CAMPFIREPLUS_BLOCK)

  val AIRLIGHT_BLOCK: RegO[AirLightBlock] = regBlock("airlight", () => new AirLightBlock)

  val ENTITY_LIGHT_SPIRIT: RegE[EntityLightSpirit] = regEntity("lightspirit", () => EntityLightSpirit, new EntityLightSpirit(_,_), EntityClassification.MISC)



//  // You can do it this way instead
//  @Mod.EventBusSubscriber(modid = Anima.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
//  object RegistryEvents {
//    @SubscribeEvent def onBlocksRegistry(blockRegistryEvent: RegistryEvent.Register[Block]): Unit = { // register a new block here
//      LOGGER.info("HELLO from Register Block")
//    }
//
//    @SubscribeEvent def onItemsRegistry(itemRegistryEvent: RegistryEvent.Register[Item]): Unit = {
//      LOGGER.info("HELLO from Register Item")
//    }
//  }
}
