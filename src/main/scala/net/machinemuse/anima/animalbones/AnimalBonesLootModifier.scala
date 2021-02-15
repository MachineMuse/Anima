package net.machinemuse.anima
package animalbones

import net.minecraft.advancements.criterion.EntityPredicate
import net.minecraft.data.ShapelessRecipeBuilder
import net.minecraft.entity.EntityType
import net.minecraft.item._
import net.minecraft.loot.LootContext
import net.minecraft.loot.conditions.{EntityHasProperty, ILootCondition}
import net.minecraftforge.common.data.GlobalLootModifierProvider
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus
import net.minecraftforge.fml.event.lifecycle.{FMLConstructModEvent, GatherDataEvent}
import org.apache.logging.log4j.scala.Logging

import animalbones.AnimalBonesLootModifier.AnimalLootModifierData
import util.DatagenHelpers.{FancyShapelessRecipeBuilder, mkLanguageProvider, mkLootModifierProvider, mkRecipeProvider}
import util.GenCodecsByName._
import util.VanillaCodecs._
import java.util
import scala.util.Random

import registration.RegistryHelpers._

/**
 * Created by MachineMuse on 2/14/2021.
 */
object AnimalBonesLootModifier extends Logging {
  @SubscribeEvent def onConstructMod(event: FMLConstructModEvent) = {}

  val BIRDBONES_ITEM = regSimpleItem("birdbones")

  val ANIMALBONES_ITEM = regSimpleItem("animalbones")

  case class AnimalLootModifierData(itemToAdd: Item,
                                    minCount: Int,
                                    maxCount: Int) extends CodecByName

  /*_*/
  private val SERIALIZER = regLootModifierSerializer("animal_bones", new AnimalBonesLootModifier(_,_))
  /*_*/

  @SubscribeEvent def onGatherData(implicit event: GatherDataEvent): Unit = {
    mkRecipeProvider { consumer =>
      ShapelessRecipeBuilder
        .shapelessRecipe(Items.BONE_MEAL)
        .addIngredientAsCriterion("birdbones", BIRDBONES_ITEM.get)
        .setGroup("bonemeal")
        .buildProperly(consumer, "bonemeal_from_birdbones")

      ShapelessRecipeBuilder
        .shapelessRecipe(Items.BONE_MEAL, 2)
        .addIngredientAsCriterion("animalbones", ANIMALBONES_ITEM.get)
        .setGroup("bonemeal")
        .buildProperly(consumer, "bonemeal_from_animalbones")
    }

    mkLanguageProvider("en_us") { lang =>
      lang.addItem(BIRDBONES_ITEM.supplier, "Bird Bones")
      lang.addItem(ANIMALBONES_ITEM.supplier, "Animal Bones")
    }
    mkLanguageProvider("fr_fr") { lang =>
      lang.addItem(BIRDBONES_ITEM.supplier, "Os d'Oiseau")
      lang.addItem(ANIMALBONES_ITEM.supplier, "Os d'Animal")
    }
    def mkLM(provider: GlobalLootModifierProvider, name: String, item: Item, min: Int, max: Int, entityType: EntityType[_]) = {
      provider.add(name, SERIALIZER.get,
        new AnimalBonesLootModifier(AnimalLootModifierData(item, min, max),
          Array(EntityHasProperty.builder(LootContext.EntityTarget.THIS, EntityPredicate.Builder.create().`type`(entityType)).build())
        )
      )
    }

    mkLootModifierProvider{provider =>
      mkLM(provider, "chicken_bones", BIRDBONES_ITEM.get, 1, 2, EntityType.CHICKEN)
      mkLM(provider, "cow_bones", ANIMALBONES_ITEM.get, 1, 4, EntityType.COW)
      mkLM(provider, "pig_bones", ANIMALBONES_ITEM.get, 1, 3, EntityType.PIG)
      mkLM(provider, "sheep_bones", ANIMALBONES_ITEM.get, 1, 2, EntityType.SHEEP)
    }
  }
}

@EventBusSubscriber(modid = Anima.MODID, bus = Bus.MOD)
class AnimalBonesLootModifier(data: AnimalLootModifierData, conditions: Array[ILootCondition])
  extends SimpleLootModifier[AnimalLootModifierData](data, conditions) with Logging {

  override def doApply(generatedLoot: util.List[ItemStack], context: LootContext): util.List[ItemStack] = {
    val numBones = Random.nextInt(data.maxCount + 1 - data.minCount) + data.minCount
    generatedLoot.add(new ItemStack(data.itemToAdd, numBones))
    generatedLoot
  }

}
