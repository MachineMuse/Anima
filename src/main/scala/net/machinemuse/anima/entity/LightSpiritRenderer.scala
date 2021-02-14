package net.machinemuse.anima
package entity

import com.sun.istack.internal.Nullable
import net.minecraft.client.renderer.entity.{EntityRenderer, EntityRendererManager}
import net.minecraft.util.ResourceLocation
import net.minecraftforge.api.distmarker.{Dist, OnlyIn}
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.client.registry.RenderingRegistry
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent

/**
 * Created by MachineMuse on 1/25/2021.
 */
object LightSpiritRenderer {

  @SubscribeEvent
  def onClientSetup(event: FMLClientSetupEvent) : Unit = {
    RenderingRegistry.registerEntityRenderingHandler(EntityLightSpirit.getType, new LightSpiritRenderer(_))
  }
}
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = Anima.MODID, value = Array(Dist.CLIENT), bus = Bus.MOD)
class LightSpiritRenderer(manager: EntityRendererManager) extends EntityRenderer[EntityLightSpirit](manager) {
  private val TEXTURE = new ResourceLocation(implicitly[MODID], "textures/entity/sparkle.png")
  @Nullable def getEntityTexture(entity: EntityLightSpirit): ResourceLocation = TEXTURE
}
