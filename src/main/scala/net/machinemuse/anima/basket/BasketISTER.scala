package net.machinemuse.anima
package basket

import com.mojang.blaze3d.matrix.MatrixStack
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer._
import net.minecraft.client.renderer.model.ItemCameraTransforms.TransformType
import net.minecraft.client.renderer.tileentity.ItemStackTileEntityRenderer
import net.minecraft.item.ItemStack
import net.minecraft.util.ResourceLocation
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.ModelRegistryEvent
import net.minecraftforge.client.model.ModelLoader
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import org.apache.logging.log4j.scala.Logging

import java.util.concurrent.Callable
import scala.annotation.nowarn

import render.RenderStates
import render.RenderingShorthand.withPushedMatrix

/**
 * Created by MachineMuse on 1/21/2021.
 */

object BasketISTER extends Logging {
  @SubscribeEvent
  def setupModels(event: ModelRegistryEvent): Unit = {
    logger.debug("adding special model for BasketISTER")
    ModelLoader.addSpecialModel(MODEL_LOCATION)
  }

  private val MODEL_LOCATION = new ResourceLocation(implicitly[MODID], "item/basket_underlay")

  def mkISTER: Callable[ItemStackTileEntityRenderer] = () => new BasketISTER
}
@Mod.EventBusSubscriber(modid = Anima.MODID, value = Array(Dist.CLIENT), bus = Mod.EventBusSubscriber.Bus.MOD)
class BasketISTER extends ItemStackTileEntityRenderer with Logging {



  // the actual render method
  override def func_239207_a_(bag: ItemStack,
                              transformType: TransformType,
                              matrixStack: MatrixStack,
                              buffer: IRenderTypeBuffer,
                              combinedLight: Int,
                              combinedOverlay: Int
                             ): Unit = {
    transformType match {
      case TransformType.GUI =>
        val selectedInBasket = Basket.BASKET_ITEM.get.getStackInSelectedSlot(bag)
        doRenderModel(bag, transformType, matrixStack, buffer, combinedLight, combinedOverlay)

        if(!selectedInBasket.isEmpty) {
          doRenderOverlay(selectedInBasket, transformType, matrixStack, buffer, combinedLight, combinedOverlay)
          doRenderText(selectedInBasket, matrixStack)
        }
//      case _ if(transformType.isFirstPerson) =>
//        doRenderModel(bag, transformType, matrixStack, buffer, combinedLight, combinedOverlay)
      case _ =>
        doRenderModel(bag, transformType, matrixStack, buffer, combinedLight, combinedOverlay)
    }
  }
  def doRenderOverlay(contents: ItemStack,
                      transformType: TransformType,
                      matrixStack: MatrixStack,
                      buffer: IRenderTypeBuffer,
                      combinedLight: Int,
                      combinedOverlay: Int): Unit = {
    withPushedMatrix(matrixStack) { matEl =>
      matrixStack.translate(0.0F, 0.25F, 0.0625F)
      matrixStack.scale(0.75F, 0.75F, 1.0F)
      val stackModel = Minecraft.getInstance.getItemRenderer.getItemModelWithOverrides(contents, Minecraft.getInstance.world, Minecraft.getInstance.player)
      val vertexBuilder = ItemRenderer.getEntityGlintVertexBuilder(buffer, RenderStates.getBetterTranslucentState, true, contents.hasEffect)
      if(stackModel.isBuiltInRenderer) {
        contents.getItem.getItemStackTileEntityRenderer.func_239207_a_(contents, transformType, matrixStack, buffer, combinedLight, combinedOverlay)
      } else {
        Minecraft.getInstance.getItemRenderer.renderModel(stackModel, contents, combinedLight, combinedOverlay, matrixStack, vertexBuilder)
      }
    }
  }

  def doRenderModel(bag: ItemStack,
                    transformType: TransformType,
                    matrixStack: MatrixStack,
                    buffer: IRenderTypeBuffer,
                    combinedLight: Int,
                    combinedOverlay: Int): Unit = {
    withPushedMatrix(matrixStack) { matEl =>
      val bakedmodel = Minecraft.getInstance().getModelManager.getModel(BasketISTER.MODEL_LOCATION)
      val vertexBuilder = ItemRenderer.getEntityGlintVertexBuilder(buffer, RenderStates.getBetterTranslucentState, true, bag.hasEffect)
      //    val transformedModel = ForgeHooksClient.handleCameraTransforms(matrixStack, bakedmodel, transformType, transformType.isLeftHand);
      Minecraft.getInstance().getItemRenderer.renderModel(bakedmodel, bag, combinedLight, combinedOverlay, matrixStack, vertexBuilder)
    }
  }

  def doRenderText(stack: ItemStack, matrixStack: MatrixStack) =
    withPushedMatrix(matrixStack) { matEl =>
      val fontScale = 1.0F / 16.0F
      RenderSystem.pushMatrix(): @nowarn
      RenderSystem.scalef(fontScale, fontScale, fontScale): @nowarn
      RenderSystem.scalef(1.0F, -1.0F, 1.0F): @nowarn
      RenderSystem.translatef(-8.0F, -8.0F, 0.0F): @nowarn
      RenderSystem.translatef(0, 0, 50.0F): @nowarn

      matrixStack.translate(0.0F, 0.25F, 0.0625F)
      matrixStack.scale(0.75F, 0.75F, 1.0F)
      if (stack.getCount != 1) {
        val s = String.valueOf(stack.getCount)
        val buffer = IRenderTypeBuffer.getImpl(Tessellator.getInstance.getBuffer)
        val fr = Minecraft.getInstance().fontRenderer
        val xPosition = (19 - 2 - fr.getStringWidth(s)).toFloat
        val yPosition = (6 + 3).toFloat
        fr.renderString(s, xPosition, yPosition, 16777215, true, matrixStack.getLast.getMatrix, buffer, false, 0, 15728880)
        buffer.finish()
      }
      RenderSystem.popMatrix(): @nowarn
    }

}
