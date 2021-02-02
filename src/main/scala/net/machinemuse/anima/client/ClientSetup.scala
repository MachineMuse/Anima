package net.machinemuse.anima
package client

import entity.EntityLightSpirit
import gui.{BasketContainer, BasketGui}
import item.basket.BasketISTER
import item.campfire.{CampfirePlus, CampfirePlusTileEntity}
import registration.KeyBindings

import net.minecraft.client.gui.ScreenManager
import net.minecraft.client.renderer._
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.ModelRegistryEvent
import net.minecraftforge.client.model.ModelLoader
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.client.registry.{ClientRegistry, RenderingRegistry}
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent
import org.apache.logging.log4j.scala.Logging
import org.lwjgl.opengl.GL11

/**
 * Created by MachineMuse on 1/22/2021.
 */
@Mod.EventBusSubscriber(modid = Anima.MODID, value = Array(Dist.CLIENT), bus = Mod.EventBusSubscriber.Bus.MOD)
//@Mod.EventBusSubscriber(modid = "anima", value = Array(Dist.CLIENT), bus = Mod.EventBusSubscriber.Bus.FORGE)
object ClientSetup extends Logging {

  val getBetterTranslucentState = {
    import RenderState._
    val renderState = RenderType.State.getBuilder
                                .shadeModel(SHADE_ENABLED)
                                .lightmap(LIGHTMAP_ENABLED)
                                .texture(BLOCK_SHEET_MIPPED)
                                .transparency(TRANSLUCENT_TRANSPARENCY)
                                .alpha(DEFAULT_ALPHA)
                                .build(true)
    val INITIAL_BUFFER_SIZE = 262144
    RenderType.makeType("anima_campfire_translucent", DefaultVertexFormats.BLOCK, GL11.GL_QUADS, INITIAL_BUFFER_SIZE, true, true, renderState)
  }

  val customCutoutState = {
    import RenderState._
    val renderState = RenderType.State.getBuilder.shadeModel(SHADE_ENABLED).lightmap(LIGHTMAP_ENABLED).texture(BLOCK_SHEET).alpha(HALF_ALPHA).build(true)
    val INITIAL_BUFFER_SIZE = 131072
    RenderType.makeType("anima_campfire_cutout", DefaultVertexFormats.BLOCK, GL11.GL_QUADS, INITIAL_BUFFER_SIZE, true, false, renderState)
  }

  @SubscribeEvent
  def init(event: FMLClientSetupEvent) : Unit = {
    val minecraft = event.getMinecraftSupplier.get()
    logger.debug("setting up client, event triggered")
    ScreenManager.registerFactory[BasketContainer, BasketGui](BasketContainer.getType, new BasketGui(_, _, _)) // type annotations added for IntelliJ's sake
    RenderTypeLookup.setRenderLayer(CampfirePlus.getBlock, RenderType.getCutout)
    ClientRegistry.bindTileEntityRenderer(CampfirePlusTileEntity.getType, new CampfirePlusTileEntityRenderer(_))
    RenderingRegistry.registerEntityRenderingHandler(EntityLightSpirit.getType, new LightSpiritRenderer(_))
    for( key <- KeyBindings.keybinds) {
      ClientRegistry.registerKeyBinding(key)
    }
//    minecraft.getBlockColors.register(new CampfirePlusFlameColour, AnimaRegistry.CAMPFIREPLUS_BLOCK.get())
  }

  @SubscribeEvent
  def setupModels(event: ModelRegistryEvent): Unit = {
    logger.debug("setting up models, event triggered")
    ModelLoader.addSpecialModel(CampfirePlusTileEntityRenderer.MODEL_LOCATION)
    ModelLoader.addSpecialModel(BasketISTER.MODEL_LOCATION)
  }

}
