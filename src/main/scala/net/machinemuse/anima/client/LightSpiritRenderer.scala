package net.machinemuse.anima.client

import com.sun.istack.internal.Nullable
import net.machinemuse.anima.Anima
import net.machinemuse.anima.entity.EntityLightSpirit
import net.minecraft.client.renderer.entity.{EntityRenderer, EntityRendererManager}
import net.minecraft.util.ResourceLocation

/**
 * Created by MachineMuse on 1/25/2021.
 */
class LightSpiritRenderer(manager: EntityRendererManager) extends EntityRenderer[EntityLightSpirit](manager) {

  private val TEXTURE = new ResourceLocation(Anima.MODID, "textures/entity/sparkle.png")

  @Nullable def getEntityTexture(entity: EntityLightSpirit): ResourceLocation = TEXTURE
}
