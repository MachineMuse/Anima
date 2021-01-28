package net.machinemuse.anima.client

import net.machinemuse.anima.Anima
import net.machinemuse.anima.gui.BasketGui
import net.machinemuse.anima.item.basket.BasketISTER
import net.machinemuse.anima.registration.{AnimaRegistry, KeyBindings}
import net.minecraft.client.gui.ScreenManager
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.client.renderer.{RenderState, RenderType, RenderTypeLookup}
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.ModelRegistryEvent
import net.minecraftforge.client.model.ModelLoader
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.client.registry.{ClientRegistry, RenderingRegistry}
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent
import org.apache.logging.log4j.LogManager
import org.lwjgl.opengl.GL11

/**
 * Created by MachineMuse on 1/22/2021.
 */
@Mod.EventBusSubscriber(modid = Anima.MODID, value = Array(Dist.CLIENT), bus = Mod.EventBusSubscriber.Bus.MOD)
//@Mod.EventBusSubscriber(modid = "anima", value = Array(Dist.CLIENT), bus = Mod.EventBusSubscriber.Bus.FORGE)
object ClientSetup {
  private val LOGGER = LogManager.getLogger

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
    LOGGER.info("setting up client, event triggered")
    ScreenManager.registerFactory(AnimaRegistry.BASKET_CONTAINER.get(), new BasketGui(_,_,_))
    RenderTypeLookup.setRenderLayer(AnimaRegistry.CAMPFIREPLUS_BLOCK.get, RenderType.getCutout)
    ClientRegistry.bindTileEntityRenderer(AnimaRegistry.CAMPFIREPLUS_TE.get, new CampfirePlusTileEntityRenderer(_))
    RenderingRegistry.registerEntityRenderingHandler(AnimaRegistry.ENTITY_LIGHT_SPIRIT.get(), new LightSpiritRenderer(_))
    for( key <- KeyBindings.keybinds) {
      ClientRegistry.registerKeyBinding(key)
    }
//    minecraft.getBlockColors.register(new CampfirePlusFlameColour, AnimaRegistry.CAMPFIREPLUS_BLOCK.get())
  }

  @SubscribeEvent
  def setupModels(event: ModelRegistryEvent): Unit = {
    LOGGER.info("setting up models, event triggered")
    ModelLoader.addSpecialModel(CampfirePlusTileEntityRenderer.MODEL_LOCATION)
    ModelLoader.addSpecialModel(BasketISTER.MODEL_LOCATION)
  }

}
