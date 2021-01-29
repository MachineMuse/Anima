package net.machinemuse.anima.registration

import net.machinemuse.anima.Anima
import net.machinemuse.anima.entity.{AirLightBlock, EntityLightSpirit}
import net.machinemuse.anima.gui.BasketContainer
import net.machinemuse.anima.item.basket.Basket
import net.machinemuse.anima.item.campfire.{CampfirePlus, CampfirePlusTileEntity, DustForCampfire}
import net.minecraft.entity.EntityClassification
import net.minecraft.item._

/**
 * Created by MachineMuse on 1/21/2021.
 */
object AnimaRegistry {
  import net.machinemuse.anima.registration.RegistryHelpers._

  object AnimaCreativeGroup extends ItemGroup(Anima.MODID + ".creativetab") {
    override def createIcon(): ItemStack = {
      AnimaRegistry.SPIRITFIRE_ITEM.get.getDefaultInstance
    }
  }

  val KINDLING_ITEM: RegO[Item] = regSimpleItem("kindling")

  val BIRDBONES_ITEM: RegO[Item] = regSimpleItem("birdbones")

  val ANIMALBONES_ITEM: RegO[Item] = regSimpleItem("animalbones")

  val SPIRITFIRE_ITEM: RegO[Item] = regSimpleItem("spiritfire", creativeGroup = Some(null))

  val BASKET_ITEM: RegO[Basket] = regExtendedItem("basket", () => new Basket)

  val BASKET_CONTAINER: RegCT[BasketContainer] = regContainer("basket", (id, inv, buf) => new BasketContainer(id, inv))

  val DUSTFORCAMPFIRE_ITEM: RegO[DustForCampfire] = regExtendedItem("campfiredust", () => new DustForCampfire)

  val CAMPFIREPLUS_ITEM: RegO[BlockItem] = regExtendedItem("campfireplus", () => new BlockItem(CAMPFIREPLUS_BLOCK.get, new Item.Properties().group(AnimaCreativeGroup)))

  val CAMPFIREPLUS_BLOCK: RegO[CampfirePlus] = regBlock("campfireplus", () => new CampfirePlus)

  val CAMPFIREPLUS_TE: RegTE[CampfirePlusTileEntity] = regTE("campfireplus", () => new CampfirePlusTileEntity, CAMPFIREPLUS_BLOCK)

  val AIRLIGHT_BLOCK: RegO[AirLightBlock] = regBlock("airlight", () => new AirLightBlock)

  val ENTITY_LIGHT_SPIRIT: RegE[EntityLightSpirit] = regEntity("lightspirit", () => EntityLightSpirit, new EntityLightSpirit(_,_), EntityClassification.MISC)

}
