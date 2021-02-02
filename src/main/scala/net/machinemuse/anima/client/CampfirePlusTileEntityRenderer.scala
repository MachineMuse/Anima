package net.machinemuse.anima
package client

import client.CampfirePlusTileEntityRenderer.MODEL_LOCATION
import item.campfire.CampfirePlusTileEntity
import util.Colour
import util.RenderingShorthand.withPushedMatrix

import com.mojang.blaze3d.matrix.MatrixStack
import com.mojang.blaze3d.vertex.IVertexBuilder
import net.minecraft.block.CampfireBlock
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.IRenderTypeBuffer
import net.minecraft.client.renderer.model.{BakedQuad, ItemCameraTransforms}
import net.minecraft.client.renderer.tileentity.{TileEntityRenderer, TileEntityRendererDispatcher}
import net.minecraft.item.{DyeColor, ItemStack}
import net.minecraft.util.math.vector.Vector3f
import net.minecraft.util.{Unit => _, _}
import net.minecraftforge.client.model.data.EmptyModelData
import org.apache.logging.log4j.scala.Logging

import java.util.Random
import scala.jdk.CollectionConverters._

/**
 * Created by MachineMuse on 1/24/2021.
 */
object CampfirePlusTileEntityRenderer extends Logging {
  val MODEL_LOCATION = new ResourceLocation(Anima.MODID, "block/campfireplus_flames")
}
class CampfirePlusTileEntityRenderer(dispatcher: TileEntityRendererDispatcher) extends TileEntityRenderer[CampfirePlusTileEntity](dispatcher) {

  override def render (tileEntity: CampfirePlusTileEntity, partialTicks: Float, matrixStack: MatrixStack, buffer: IRenderTypeBuffer, combinedLight: Int, combinedOverlay: Int): Unit = {
    val blockstate = tileEntity.getBlockState
    val direction: Direction = blockstate.get (CampfireBlock.FACING)
    val nonnulllist: NonNullList[ItemStack] = tileEntity.getInventory
    val minecraft = Minecraft.getInstance()
    val itemRenderer = minecraft.getItemRenderer

    // Copied from regular campfire's special renderer
    for (i <- 0 until nonnulllist.size) {
      val itemstack: ItemStack = nonnulllist.get (i)
      if (itemstack ne ItemStack.EMPTY) {
        matrixStack.push()
        matrixStack.translate (0.5D, 0.44921875D, 0.5D)
        val direction1: Direction = Direction.byHorizontalIndex ((i + direction.getHorizontalIndex) % 4)
        val f: Float = - direction1.getHorizontalAngle
        matrixStack.rotate (Vector3f.YP.rotationDegrees (f) )
        matrixStack.rotate (Vector3f.XP.rotationDegrees (90.0F) )
        matrixStack.translate (- 0.3125D, - 0.3125D, 0.0D)
        matrixStack.scale (0.375F, 0.375F, 0.375F)
        itemRenderer.renderItem (itemstack, ItemCameraTransforms.TransformType.FIXED, combinedLight, combinedOverlay, matrixStack, buffer)
        matrixStack.pop()
      }
    }

    withPushedMatrix (matrixStack) { matrixEntry =>
      val flamesModel = minecraft.getModelManager.getModel(MODEL_LOCATION)
      val vertexBuffer: IVertexBuilder = buffer.getBuffer(ClientSetup.getBetterTranslucentState) // ClientSetup.getBetterTranslucentState

      val rgb1 = Colour.toFloatArray(Colour.mixColours(tileEntity.colour1, DyeColor.BLACK.getTextColor, 4.0F/1.0F))
      val rgb2 = Colour.toFloatArray(tileEntity.colour2)

      val listQuads = flamesModel.getQuads(blockstate, null, new Random(), EmptyModelData.INSTANCE)
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
