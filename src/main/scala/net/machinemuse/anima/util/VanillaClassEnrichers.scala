package net.machinemuse.anima
package util

import util.VanillaCodecs.ConvenientCodec

import net.minecraft.client.renderer.model.ItemCameraTransforms.TransformType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.util.{Unit => _, _}
import org.apache.logging.log4j.scala.Logging

/**
 * Created by MachineMuse on 1/27/2021.
 */
object VanillaClassEnrichers extends Logging {
  implicit class RichTransformType(transformType: TransformType) {
    def isLeftHand = transformType == TransformType.FIRST_PERSON_LEFT_HAND || transformType == TransformType.FIRST_PERSON_LEFT_HAND
  }

  implicit class RichPlayerEntity(player: PlayerEntity) {
    def itemInHand: Hand => ItemStack = {
      case Hand.MAIN_HAND => player.inventory.mainInventory.get(player.inventory.currentItem)
      case Hand.OFF_HAND => player.inventory.offHandInventory.get(0)
    }
  }

  implicit class RichItemStack(stack: ItemStack) {
    private val tagName = "transparency"
    def hasTransparency: Boolean = {
      stack.hasTag && stack.getTag.contains(tagName)
    }
    def getTransparency: Float = {
      if(stack.hasTransparency) {
        stack.getTag.getFloat(tagName)
      } else {
        0.0F
      }
    }
    def setTransparency(f: Float): Unit = {
      stack.getOrCreateTag().putFloat(tagName, f)
    }

    def removeTransparency(): Unit = {
      stack.getOrCreateTag().remove(tagName)
    }
  }

}
