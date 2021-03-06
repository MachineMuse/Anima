package net.machinemuse.anima
package campfire

import com.mojang.blaze3d.matrix.MatrixStack
import com.mojang.blaze3d.vertex.IVertexBuilder
import net.minecraft.block.CampfireBlock
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer._
import net.minecraft.client.renderer.model.BakedQuad
import net.minecraft.client.renderer.tileentity.{TileEntityRenderer, TileEntityRendererDispatcher}
import net.minecraft.tileentity.CampfireTileEntity
import net.minecraft.util.{ResourceLocation, Unit => _}
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.ModelRegistryEvent
import net.minecraftforge.client.model.ModelLoader
import net.minecraftforge.client.model.data.EmptyModelData
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.client.registry.ClientRegistry
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus
import net.minecraftforge.fml.event.lifecycle.{FMLClientSetupEvent, GatherDataEvent}

import java.util.Random
import scala.jdk.CollectionConverters.CollectionHasAsScala

import render.RenderStates
import render.RenderingShorthand.withPushedMatrix
import util.{Colour, Logging}

/**
 * Created by MachineMuse on 1/24/2021.
 */
object CampfirePlusTileEntityRenderer extends Logging {
  val FLAMES_MODEL_LOCATION = new ResourceLocation(implicitly[MODID], "block/campfireplus_flames")

  @SubscribeEvent def onClientSetup(event: FMLClientSetupEvent) = {
    RenderTypeLookup.setRenderLayer(CampfirePlus.getBlock, RenderType.getCutout)
    ClientRegistry.bindTileEntityRenderer(CampfirePlusTileEntity.getType, new CampfirePlusTileEntityRenderer(_))
  }

  @SubscribeEvent def setupModels(event: ModelRegistryEvent): Unit = ModelLoader.addSpecialModel(FLAMES_MODEL_LOCATION)

  @SubscribeEvent def gatherData(implicit event: GatherDataEvent): Unit = {

  }

  lazy val campfireTER = TileEntityRendererDispatcher.instance.getRenderer(new CampfireTileEntity)
}
@EventBusSubscriber(modid = Anima.MODID, value = Array(Dist.CLIENT), bus = Bus.MOD)
class CampfirePlusTileEntityRenderer(dispatcher: TileEntityRendererDispatcher) extends TileEntityRenderer[CampfirePlusTileEntity](dispatcher) {
  import campfire.CampfirePlusTileEntityRenderer._

  // Can't extend CampfireTileEntityRenderer because it locks in the type argument
  override def render (tileEntity: CampfirePlusTileEntity, partialTicks: Float, matrixStack: MatrixStack, buffer: IRenderTypeBuffer, combinedLight: Int, combinedOverlay: Int): Unit = {
    // Render items that are on the fire
    campfireTER.render(tileEntity, partialTicks, matrixStack, buffer, combinedLight, combinedOverlay)

    // Render the flames
    if(CampfireBlock.isLit(tileEntity.getBlockState))
      withPushedMatrix (matrixStack) { matrixEntry =>
        val flamesModel = Minecraft.getInstance().getModelManager.getModel(FLAMES_MODEL_LOCATION)
        val vertexBuffer: IVertexBuilder = buffer.getBuffer(RenderStates.getBetterTranslucentState) // ClientSetup.getBetterTranslucentState


        val (outer, inner) = {
          if(tileEntity.activeDusts.isEmpty) {
            (CampfirePlusTileEntity.defaultOuterColour, CampfirePlusTileEntity.defaultInnerColour)
          } else {
            var outerFound = 0.0F
            var outerColour = 0
            var innerFound = 0.0F
            var innerColour = 0
            for(dust <- tileEntity.activeDusts) {
              innerColour = Colour.mixColoursByWeight(dust.innerColour, innerColour, 1, innerFound)
              innerFound += 1.0F
              outerColour = Colour.mixColoursByWeight(dust.outerColour, outerColour, 1, outerFound)
              outerFound += 1.0F
            }
            (outerColour, innerColour)
          }
        }
        val rgb1 = Colour.toFloatArray(outer)
        val rgb2 = Colour.toFloatArray(inner)

        val listQuads = flamesModel.getQuads(tileEntity.getBlockState, null, new Random(), EmptyModelData.INSTANCE)
        val danceMod = tileEntity.dance_enhancement.toFloat
        matrixStack.translate(0.5f, 0.0625f, 0.5f)
        matrixStack.scale(1.0f + danceMod/10.0f, 1.0f + danceMod, 1.0f + danceMod/10.0f)
        matrixStack.translate(-0.5f, -0.0625f, -0.5f)
        for (bakedquad: BakedQuad <- listQuads.asScala) {
          bakedquad.getTintIndex match {
            case 0 => vertexBuffer.addQuad(matrixEntry, bakedquad, rgb1(0), rgb1(1), rgb1(2), combinedLight, combinedOverlay)
            case 1 => vertexBuffer.addQuad(matrixEntry, bakedquad, rgb2(0), rgb2(1), rgb2(2), combinedLight, combinedOverlay)
            case _ => vertexBuffer.addQuad(matrixEntry, bakedquad, 1.0F, 1.0F, 1.0F, combinedLight, combinedOverlay)
          }
        }
      }

  }
}
