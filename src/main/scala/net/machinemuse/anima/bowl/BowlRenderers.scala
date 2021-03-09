package net.machinemuse.anima
package bowl

import com.mojang.blaze3d.matrix.MatrixStack
import com.mojang.blaze3d.vertex.IVertexBuilder
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer._
import net.minecraft.client.renderer.model.ItemCameraTransforms.TransformType
import net.minecraft.client.renderer.tileentity._
import net.minecraft.inventory.container.PlayerContainer
import net.minecraft.item.ItemStack
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.ModelRegistryEvent
import net.minecraftforge.client.model.ModelLoader
import net.minecraftforge.client.model.data.EmptyModelData
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.client.registry.ClientRegistry
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent

import java.util.concurrent.Callable

import render.RenderStates
import render.RenderingShorthand.withPushedMatrix
import util.{Colour, Logging}

/**
 * Created by MachineMuse on 3/2/2021.
 */
@EventBusSubscriber(modid = Anima.MODID, value = Array(Dist.CLIENT), bus = Bus.MOD)
object BowlRenderers extends Logging {

  @SubscribeEvent def onClientSetup(event: FMLClientSetupEvent) = {
    ClientRegistry.bindTileEntityRenderer(BowlInWorldTileEntity.getType, new BowlWithContentsTER(_))
  }

  @SubscribeEvent
  def setupModels(event: ModelRegistryEvent): Unit = {
    logger.debug("adding special model for Bowl ISTER")
    ModelLoader.addSpecialModel(MODEL_LOCATION)
  }

  class HalfBakedQuad(buffer: IVertexBuilder, stackEntry: MatrixStack.Entry,
                      r: Float, g: Float, b: Float, a: Float,
                      overlay: Int, light: Int,
                      nx: Float, ny: Float, nz: Float) {
    def vertex(x: Float, y: Float, z: Float, u: Float, v: Float): Unit = {
      buffer.pos(stackEntry.getMatrix, x, y, z)
        .color(r, g, b, a)
        .tex(u, v)
        .lightmap(light)
//        .overlay(overlay)
        .normal(nx, ny, nz)
        .endVertex()
    }
    def doFaceUpQuad(minx: Float, maxx: Float, minz: Float, maxz: Float,
               minu: Float, maxu: Float, minv: Float, maxv: Float): Unit = {
      vertex(minx, 0.0F, maxz, minu, maxv)
      vertex(maxx, 0.0F, maxz, maxu, maxv)
      vertex(maxx, 0.0F, minz, maxu, minv)
      vertex(minx, 0.0F, minz, minu, minv)
    }
  }

  lazy val MODEL_LOCATION = new ResourceLocation(implicitly[MODID], "block/bowl")

  def mkISTER: Callable[ItemStackTileEntityRenderer] = () => new BowlWithContentsISTER

  def doRenderContents(contents: BowlContents.BowlContents, world: World, pos: BlockPos, partialTicks: Float, matrixStack: MatrixStack, typeBuffer: IRenderTypeBuffer, combinedLight: Int, combinedOverlay: Int): Unit = {

    contents match {
      case BowlContents.NoContents =>
      case BowlContents.FluidContents(fluid) =>
        val fluidStill = fluid.getAttributes.getStillTexture(world, pos)
        val sprite = Minecraft.getInstance().getAtlasSpriteGetter(PlayerContainer.LOCATION_BLOCKS_TEXTURE).apply(fluidStill)
        val vertexBuffer = typeBuffer.getBuffer(RenderStates.getBetterTranslucentState)
//        val rotation = Vector3f.YP.rotationDegrees(0)
        val color = fluid.getAttributes.getColor()
        val (r,g,b) = Colour.toTuple(color)
        val alpha = 1.0f
        withPushedMatrix(matrixStack) { stackEntry =>
          matrixStack.translate(.5, 0, .5)
          matrixStack.scale(6.0f/16.0f, 0.25f, 6.0f/16.0f)
//          matrixStack.rotate(rotation)
          matrixStack.translate(-.5, 1.0-0.0625, -.5)
          val quad = new HalfBakedQuad(vertexBuffer, stackEntry, r, g, b, alpha, combinedOverlay, combinedLight, 1.0f, 0.0f, 0.0f)
          quad.doFaceUpQuad(0.0f, 1.0f, 0.0f, 1.0f, sprite.getMinU, sprite.getMaxU, sprite.getMinV, sprite.getMaxV)
        }


      case BowlContents.BlockContents(block) =>
//        val rotation = Vector3f.YP.rotationDegrees(0)
        val color = block.getMaterialColor.colorValue
        val (r,g,b) = Colour.toTuple(color)
        withPushedMatrix(matrixStack) { stackEntry =>
          matrixStack.translate(.5, 1.125f/16.0f, .5)
          matrixStack.scale(95f / 256f, 63f/256f, 95f / 256f)
//          matrixStack.rotate(rotation)
          matrixStack.translate(-.5, 0, -.5)
          val vertexBuffer = typeBuffer.getBuffer(RenderStates.getBetterTranslucentState)
          val blockrendererdispatcher: BlockRendererDispatcher = Minecraft.getInstance().getBlockRendererDispatcher
          blockrendererdispatcher.getBlockModelRenderer.renderModel(stackEntry, vertexBuffer, block.getDefaultState,
            blockrendererdispatcher.getModelForState(block.getDefaultState), r, g, b, combinedLight, combinedOverlay, EmptyModelData.INSTANCE)
        }
      case BowlContents.ItemContents(itemStack) =>
    }

  }

  class BowlWithContentsISTER extends ItemStackTileEntityRenderer {

    override def func_239207_a_(stack: ItemStack, transformType : TransformType, matrixStack: MatrixStack, buffer: IRenderTypeBuffer, combinedLight: Int, combinedOverlay: Int): Unit = {
      val contents = BowlContents.getContents(stack)
      val bakedmodel = Minecraft.getInstance().getModelManager.getModel(MODEL_LOCATION)
      val vertexBuilder = ItemRenderer.getEntityGlintVertexBuilder(buffer, RenderStates.getBetterTranslucentState, true, stack.hasEffect)
      Minecraft.getInstance().getBlockRendererDispatcher.getBlockModelRenderer.renderModel(matrixStack.getLast, vertexBuilder, null, bakedmodel, 1.0f, 1.0f, 1.0f, combinedLight, combinedOverlay, EmptyModelData.INSTANCE)
      doRenderContents(contents, null, null, 0, matrixStack, buffer, combinedLight, combinedOverlay)
    }

  }

  class BowlWithContentsTER(dispatcher: TileEntityRendererDispatcher) extends TileEntityRenderer[BowlInWorldTileEntity](dispatcher){
    override def render(tileEntity: BowlInWorldTileEntity, partialTicks: Float, matrixStack: MatrixStack, typeBuffer: IRenderTypeBuffer, combinedLight: Int, combinedOverlay: Int): Unit ={
      doRenderContents(tileEntity.contents, tileEntity.getWorld, tileEntity.getPos, partialTicks, matrixStack, typeBuffer, combinedLight, combinedOverlay)
    }

  }
}
