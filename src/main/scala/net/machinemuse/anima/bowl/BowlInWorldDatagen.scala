package net.machinemuse.anima
package bowl

import com.google.gson.JsonObject
import net.minecraft.advancements.Advancement.Builder
import net.minecraft.data.IFinishedRecipe
import net.minecraft.data.loot.BlockLootTables.dropping
import net.minecraft.item.Items
import net.minecraft.item.crafting._
import net.minecraft.util.ResourceLocation
import net.minecraftforge.common.crafting.NBTIngredient
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus
import net.minecraftforge.fml.event.lifecycle.GatherDataEvent

import bowl.BowlWithContents._
import util.DatagenHelpers.{SimplerBlockLootTable, existingModModelFile, mkLanguageProvider, mkRecipeProvider, mkSimpleBlockState, provideBlockLootTable, serializeItemStackForRecipe}
import util.{DatagenHelpers, Logging}

/**
 * Created by MachineMuse on 2/25/2021.
 */
@EventBusSubscriber(modid = Anima.MODID, bus = Bus.MOD)
object BowlInWorldDatagen extends Logging {


  @SubscribeEvent def gatherData(implicit event: GatherDataEvent): Unit = {
    mkLanguageProvider("en_us"){ lang =>
      lang.addBlock(BowlInWorld.BLOCK, "Bowl")
      lang.addItem(BOWL_WITH_CONTENTS, "Bowl")
    }
    mkLanguageProvider("fr_fr"){ lang =>
      lang.addBlock(BowlInWorld.BLOCK, "Bol")
      lang.addItem(BOWL_WITH_CONTENTS, "Bol")
    }



    mkRecipeProvider{ consumer =>
      val ingredient: Ingredient = new NBTIngredient(BOWL_OF_WATER) {}
      import util.VanillaCodecs._

      consumer.accept(new IFinishedRecipe {
        val advancementBuilder = Builder.builder.withCriterion("has_ingredient", DatagenHelpers.hasItem(BOWL_OF_WATER))
        override def serialize(jsonObject: JsonObject): Unit = {
          jsonObject.add("ingredient", ingredient.serialize())

          jsonObject.add("result", serializeItemStackForRecipe(BOWL_OF_SALT))
          jsonObject.addProperty("experience", 0.1f)
          jsonObject.addProperty("cookingtime", 600)
        }

        override def getID: ResourceLocation = modLoc("salt_from_water_campfire")

        override def getSerializer: IRecipeSerializer[_] = IRecipeSerializer.CAMPFIRE_COOKING

        override def getAdvancementJson: JsonObject = advancementBuilder.serialize()

        override def getAdvancementID: ResourceLocation = modLoc("recipes/anima/salt_from_water_campfire")
      })
    }
    provideBlockLootTable {
      new SimplerBlockLootTable {
        /*_*/
        add(BowlInWorld.BLOCK.get, dropping(Items.BOWL))
        /*_*/
      }
    }
    mkSimpleBlockState(BowlInWorld.BLOCK.get, existingModModelFile("block/bowl"))
  }
}
