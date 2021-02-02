package net.machinemuse.anima
package client

import entity.EntityLightSpirit

import com.sun.istack.internal.Nullable
import net.minecraft.client.renderer.entity.{EntityRenderer, EntityRendererManager}
import net.minecraft.util.ResourceLocation

/**
 * Created by MachineMuse on 1/25/2021.
 */
class LightSpiritRenderer(manager: EntityRendererManager) extends EntityRenderer[EntityLightSpirit](manager) {

  private val TEXTURE = new ResourceLocation(Anima.MODID, "textures/entity/sparkle.png")

  @Nullable def getEntityTexture(entity: EntityLightSpirit): ResourceLocation = TEXTURE
}
