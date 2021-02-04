package net.machinemuse.anima
package client

import util.Colour
import util.VanillaClassEnrichers.RichItemStack

import com.mojang.blaze3d.matrix.MatrixStack
import net.minecraft.client.entity.player.AbstractClientPlayerEntity
import net.minecraft.client.renderer.entity.IEntityRenderer
import net.minecraft.client.renderer.entity.layers.{BipedArmorLayer, LayerRenderer}
import net.minecraft.client.renderer.entity.model.{BipedModel, PlayerModel}
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.client.renderer.{IRenderTypeBuffer, ItemRenderer}
import net.minecraft.entity.LivingEntity
import net.minecraft.inventory.EquipmentSlotType
import net.minecraft.item.{ArmorItem, IDyeableArmorItem}
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.MathHelper
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.RenderPlayerEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import org.apache.logging.log4j.scala.Logging


/**
 * Created by MachineMuse on 2/2/2021.
 */
object BipedArmorLayerPlus extends Logging {
  @SubscribeEvent def onPrePlayerRender(event: RenderPlayerEvent.Pre): Unit = {
    val layerRenderers = event.getRenderer.layerRenderers
    for(i <- 0 until layerRenderers.size()) {
      val layerUntyped = layerRenderers.get(i)
      type T = AbstractClientPlayerEntity
      type M = PlayerModel[T]
      layerUntyped.optionallyDoAs[BipedArmorLayer[T, M, M]] { layer =>
        if(!layer.isInstanceOf[BipedArmorLayerPlus[_,_,_]]) {
          layerRenderers.set(i, new BipedArmorLayerPlus(layer).asInstanceOf[LayerRenderer[T, M]])
          logger.info("Successfully inserted BipedArmorLayer hook")
        }
      }
    }

  }
}

@Mod.EventBusSubscriber(modid = Anima.MODID, value = Array(Dist.CLIENT), bus = Mod.EventBusSubscriber.Bus.FORGE)
class BipedArmorLayerPlus[T <: LivingEntity, M <: BipedModel[T], A <: BipedModel[T]](entity: IEntityRenderer[T, M], modelLeggings: A, modelArmor: A)
  extends BipedArmorLayer(entity, modelLeggings, modelArmor) with Logging {
  def this(old: BipedArmorLayer[T, M, A]) = {
    this(old.entityRenderer, old.modelLeggings, old.modelArmor)
  }

  override def render(matrixStackIn: MatrixStack, bufferIn: IRenderTypeBuffer, packedLightIn: Int, entitylivingbaseIn: T, limbSwing: Float, limbSwingAmount: Float, partialTicks: Float, ageInTicks: Float, netHeadYaw: Float, headPitch: Float): Unit = {
    this.renderArmorSlot(matrixStackIn, bufferIn, entitylivingbaseIn, EquipmentSlotType.CHEST, packedLightIn, modelArmor)
    this.renderArmorSlot(matrixStackIn, bufferIn, entitylivingbaseIn, EquipmentSlotType.LEGS, packedLightIn, modelLeggings)
    this.renderArmorSlot(matrixStackIn, bufferIn, entitylivingbaseIn, EquipmentSlotType.FEET, packedLightIn, modelArmor)
    this.renderArmorSlot(matrixStackIn, bufferIn, entitylivingbaseIn, EquipmentSlotType.HEAD, packedLightIn, modelArmor)
  }

  private def renderArmorSlot(matrixStack : MatrixStack, renderTypeBuffer : IRenderTypeBuffer, playerEntity : T, slotType : EquipmentSlotType, packedLight : Int, playerModelIn : A): Unit = {
    val itemstack = playerEntity.getItemStackFromSlot(slotType)
    itemstack.getItem match {
      case armoritem: ArmorItem =>
        if (armoritem.getEquipmentSlot eq slotType) {
          val playerModel = getArmorModelHook(playerEntity, itemstack, slotType, playerModelIn)
          this.getEntityModel.setModelAttributes(playerModel)
          this.setModelSlotVisible(playerModel, slotType)
          val glint = itemstack.hasEffect
          val alpha = 1 - MathHelper.clamp(itemstack.getTransparency, 0.0F, 1.0F)
          val glintAlpha = 1.0F - (1.0F-alpha)/.75F
          armoritem match {
            case item: IDyeableArmorItem =>
              val color = item.getColor(itemstack)
              val red = Colour.redFromInt(color)
              val green = Colour.blueFromInt(color)
              val blue = Colour.greenFromInt(color)
              this.renderArmorLayer(matrixStack, renderTypeBuffer, packedLight, glint, playerModel, red, green, blue, alpha, this.getArmorResource(playerEntity, itemstack, slotType, null))
              this.renderArmorLayer(matrixStack, renderTypeBuffer, packedLight, glint, playerModel, 1.0F, 1.0F, 1.0F, alpha, this.getArmorResource(playerEntity, itemstack, slotType, "overlay"))
            case _ =>
              this.renderArmorLayer(matrixStack, renderTypeBuffer, packedLight, glint, playerModel, 1.0F, 1.0F, 1.0F, alpha, this.getArmorResource(playerEntity, itemstack, slotType, null))
          }
        }
      case _ => // Not an armor item in that slot, so don't do anything for this layer
    }
  }

  private def renderArmorLayer(matrixStack : MatrixStack, renderTypeBuffer : IRenderTypeBuffer, packedLight : Int, glint : Boolean, playerModel : A, red : Float, green : Float, blue : Float, alpha: Float, armorResource: ResourceLocation): Unit = {
    val ivertexbuilder = ItemRenderer.getArmorVertexBuilder(renderTypeBuffer, ClientSetup.getBetterArmorState(armorResource), false, glint)
    playerModel.render(matrixStack, ivertexbuilder, packedLight, OverlayTexture.NO_OVERLAY, red, green, blue, alpha)
  }

}
