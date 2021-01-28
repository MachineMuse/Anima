package net.machinemuse.anima.item.basket

import com.mojang.blaze3d.matrix.MatrixStack
import net.machinemuse.anima.Anima
import net.machinemuse.anima.client.ClientSetup
import net.machinemuse.anima.registration.AnimaRegistry
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.model.ItemCameraTransforms.TransformType
import net.minecraft.client.renderer.tileentity.ItemStackTileEntityRenderer
import net.minecraft.client.renderer.{IRenderTypeBuffer, ItemRenderer}
import net.minecraft.item.ItemStack
import net.minecraft.util.ResourceLocation
import org.apache.logging.log4j.LogManager

import java.util.Random

/**
 * Created by MachineMuse on 1/21/2021.
 */
object BasketISTER extends ItemStackTileEntityRenderer {
  private val LOGGER = LogManager.getLogger
  private val random = new Random()

  var translateX = 0.0F
  var translateY = 0.25F
  var translateZ = 0.0625F
  var scaleX = 0.75F
  var scaleY = 0.75F
  var scaleZ = 1.0F

  val MODEL_LOCATION = new ResourceLocation(Anima.MODID, "item/basket_underlay")


  // the actual render method
  override def func_239207_a_(bag: ItemStack,
                              transformType: TransformType,
                              matrixStack: MatrixStack,
                              buffer: IRenderTypeBuffer,
                              combinedLight: Int,
                              combinedOverlay: Int
                             ): Unit = {
    val minecraft = Minecraft.getInstance()
    val itemRenderer = minecraft.getItemRenderer
    matrixStack.push()
    transformType match {
      case TransformType.GUI =>
        val basketItem = AnimaRegistry.BASKET_ITEM.get()
        val firstInBasket = basketItem.getContentsAt(bag, 0)
        doRenderModel(bag, transformType, matrixStack, buffer, combinedLight, combinedOverlay)

        if(!firstInBasket.isEmpty) {
          matrixStack.push()
//          val scale = 16.0F/16.0F
//          val translateX = 0.0F/16.0F
//          val translateY = 0.0F/16.0F
//          val translateZ = 0.0F/16.0F

          matrixStack.translate(translateX, translateY, translateZ)
          matrixStack.scale(scaleX, scaleY, scaleZ)
          val stackModel = itemRenderer.getItemModelWithOverrides(firstInBasket, minecraft.world, minecraft.player)
          val vertexBuilder = ItemRenderer.getEntityGlintVertexBuilder(buffer, ClientSetup.getBetterTranslucentState, true, firstInBasket.hasEffect);
          if(stackModel.isBuiltInRenderer) {
            firstInBasket.getItem.getItemStackTileEntityRenderer.func_239207_a_(firstInBasket, transformType, matrixStack, buffer, combinedLight, combinedOverlay)
          } else {
            minecraft.getItemRenderer.renderModel(stackModel, firstInBasket, combinedLight, combinedOverlay, matrixStack, vertexBuilder)
          }
          matrixStack.pop()
        }
      case _ if(transformType.isFirstPerson) =>
//        matrixStack.getLast.getMatrix.mul(0.75F)
        doRenderModel(bag, transformType, matrixStack, buffer, combinedLight, combinedOverlay)
      case _ =>
        doRenderModel(bag, transformType, matrixStack, buffer, combinedLight, combinedOverlay)
    }
    matrixStack.pop()
  }

  def doRenderModel(bag: ItemStack,
                    transformType: TransformType,
                    matrixStack: MatrixStack,
                    buffer: IRenderTypeBuffer,
                    combinedLight: Int,
                    combinedOverlay: Int): Unit = {
    matrixStack.push()
    val bakedmodel = Minecraft.getInstance().getModelManager.getModel(MODEL_LOCATION)
    val vertexBuilder = ItemRenderer.getEntityGlintVertexBuilder(buffer, ClientSetup.getBetterTranslucentState, true, bag.hasEffect);
//    val transformedModel = ForgeHooksClient.handleCameraTransforms(matrixStack, bakedmodel, transformType, transformType.isLeftHand);
    Minecraft.getInstance().getItemRenderer.renderModel(bakedmodel, bag, combinedLight, combinedOverlay, matrixStack, vertexBuilder)
    matrixStack.pop()
  }
}
