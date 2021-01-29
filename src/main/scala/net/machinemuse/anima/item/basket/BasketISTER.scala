package net.machinemuse.anima
package item.basket

import com.mojang.blaze3d.matrix.MatrixStack
import net.machinemuse.anima.client.ClientSetup
import net.machinemuse.anima.registration.AnimaRegistry
import net.machinemuse.anima.util.RenderingShorthand.withPushedMatrix
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.model.ItemCameraTransforms.TransformType
import net.minecraft.client.renderer.tileentity.ItemStackTileEntityRenderer
import net.minecraft.client.renderer.{IRenderTypeBuffer, ItemRenderer}
import net.minecraft.item.ItemStack
import net.minecraft.util.ResourceLocation
import org.apache.logging.log4j.scala.Logging

import java.util.Random

/**
 * Created by MachineMuse on 1/21/2021.
 */
object BasketISTER extends ItemStackTileEntityRenderer with Logging {
  private val random = new Random()

  val MODEL_LOCATION = new ResourceLocation(Anima.MODID, "item/basket_underlay")


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
        val basketItem = AnimaRegistry.BASKET_ITEM.get()
        val firstInBasket = basketItem.getContentsAt(bag, 0)
        doRenderModel(bag, transformType, matrixStack, buffer, combinedLight, combinedOverlay)

        if(!firstInBasket.isEmpty) {
          doRenderOverlay(firstInBasket, transformType, matrixStack, buffer, combinedLight, combinedOverlay)
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
      val vertexBuilder = ItemRenderer.getEntityGlintVertexBuilder(buffer, ClientSetup.getBetterTranslucentState, true, contents.hasEffect);
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
      val bakedmodel = Minecraft.getInstance().getModelManager.getModel(MODEL_LOCATION)
      val vertexBuilder = ItemRenderer.getEntityGlintVertexBuilder(buffer, ClientSetup.getBetterTranslucentState, true, bag.hasEffect);
      //    val transformedModel = ForgeHooksClient.handleCameraTransforms(matrixStack, bakedmodel, transformType, transformType.isLeftHand);
      Minecraft.getInstance().getItemRenderer.renderModel(bakedmodel, bag, combinedLight, combinedOverlay, matrixStack, vertexBuilder)
    }
  }
}
