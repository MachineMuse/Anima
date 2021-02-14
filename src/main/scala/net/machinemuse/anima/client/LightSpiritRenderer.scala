package net.machinemuse.anima
package client

import com.sun.istack.internal.Nullable
import net.minecraft.client.renderer.entity.{EntityRenderer, EntityRendererManager}
import net.minecraft.util.ResourceLocation
import net.minecraftforge.api.distmarker.{Dist, OnlyIn}

import entity.EntityLightSpirit

/**
 * Created by MachineMuse on 1/25/2021.
 */
@OnlyIn(Dist.CLIENT)
class LightSpiritRenderer(manager: EntityRendererManager) extends EntityRenderer[EntityLightSpirit](manager) {
  private val TEXTURE = new ResourceLocation(implicitly[MODID], "textures/entity/sparkle.png")
  @Nullable def getEntityTexture(entity: EntityLightSpirit): ResourceLocation = TEXTURE
}
