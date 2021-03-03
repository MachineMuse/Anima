package net.machinemuse.anima
package ghostdust

import net.minecraft.data.ShapelessRecipeBuilder
import net.minecraft.item.Items
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus
import net.minecraftforge.fml.event.lifecycle.GatherDataEvent

import bowl.BowlWithContents.BOWL_OF_SALT
import ghostdust.GhostDustingRecipe._
import util.DatagenHelpers.{FancyShapelessRecipeBuilder, mkLanguageProvider, mkRecipeProvider, mkSimpleItemModel}
import util.Logging

/**
 * Created by MachineMuse on 2/22/2021.
 */
@EventBusSubscriber(modid = Anima.MODID, bus = Bus.MOD)
object GhostDustDatagen extends Logging {

  @SubscribeEvent
  def gatherData(implicit event: GatherDataEvent): Unit = {
    mkRecipeProvider{ consumer =>
      val defaultRecipe = GhostDustingRecipe(
        List(
          GhostDustingIngredient(GHOSTDUST_REMOVER_ITEM.get, -1.0F, 1, true),
          GhostDustingIngredient(GHOSTDUST_ITEM.get, 0.125F, 0, false)
        )
      )
      consumer.accept(defaultRecipe)

      ShapelessRecipeBuilder
        .shapelessRecipe(GHOSTDUST_ITEM.get, 2)
        .addIngredientAsCriterion("gunpowder", Items.GUNPOWDER)
        .addIngredientAsCriterion("bonemeal", Items.BONE_MEAL)
        .addIngredientAsCriterion("bowl_of_salt", BOWL_OF_SALT)
        .setGroup("ghost_dust")
        .buildProperly(consumer, "ghost_dust_from_bonemeal")

      ShapelessRecipeBuilder
        .shapelessRecipe(GHOSTDUST_ITEM.get, 2)
        .addIngredientAsCriterion("gunpowder", Items.GUNPOWDER)
        .addIngredientAsCriterion("white_dye", Items.WHITE_DYE)
        .addIngredientAsCriterion("bowl_of_salt", BOWL_OF_SALT)
        .setGroup("ghost_dust")
        .buildProperly(consumer, "ghost_dust_from_white_dye")

      ShapelessRecipeBuilder
        .shapelessRecipe(GHOSTDUST_REMOVER_ITEM.get, 1)
        .addIngredientAsCriterion("gunpowder", Items.GUNPOWDER)
        .addIngredientAsCriterion("ink_sac", Items.INK_SAC)
        .addIngredientAsCriterion("bowl_of_salt", BOWL_OF_SALT)
        .setGroup("ghost_dust_remover")
        .buildProperly(consumer, "ghost_dust_remover_from_ink_sac")

      ShapelessRecipeBuilder
        .shapelessRecipe(GHOSTDUST_REMOVER_ITEM.get, 1)
        .addIngredientAsCriterion("gunpowder", Items.GUNPOWDER)
        .addIngredientAsCriterion("black_dye", Items.BLACK_DYE)
        .addIngredientAsCriterion("bowl_of_salt", BOWL_OF_SALT)
        .setGroup("ghost_dust_remover")
        .buildProperly(consumer, "ghost_dust_remover_from_black_dye")

    }
    mkLanguageProvider("en_us"){lang =>
      lang.addTooltip("transparency", "Transparency: %s%%")

      lang.addItem(GHOSTDUST_ITEM.supplier, "Ghost Dust")
      lang.addItem(GHOSTDUST_REMOVER_ITEM.supplier, "Ghost Dust Remover")
    }
    mkLanguageProvider("fr_fr"){lang =>
      lang.addTooltip("transparency", "Transparence: %s%%")

      lang.addItem(GHOSTDUST_ITEM.supplier, "Poussière de Fantôme")
      lang.addItem(GHOSTDUST_REMOVER_ITEM.supplier, "Détachant de Fantôme")
    }

    mkSimpleItemModel(GHOSTDUST_ITEM.get)
    mkSimpleItemModel(GHOSTDUST_REMOVER_ITEM.get)
  }
}
