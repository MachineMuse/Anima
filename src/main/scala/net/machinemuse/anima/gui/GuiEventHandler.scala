package net.machinemuse.anima
package gui

import item.ModeChangingItem

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.InputEvent.MouseScrollEvent
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus
import org.apache.logging.log4j.scala.Logging

import scala.annotation.nowarn

/**
 * Created by MachineMuse on 1/31/2021.
 */
@EventBusSubscriber(modid = Anima.MODID, value = Array(Dist.CLIENT), bus = Bus.FORGE)
object GuiEventHandler extends Logging {
  @SubscribeEvent
  def onScrollWheel(event: MouseScrollEvent): Unit = {

    val mc = Minecraft.getInstance()
    val player = mc.player
    val currentItemStack = player.inventory.getCurrentItem
    if(mc.gameSettings.keyBindsHotbar(player.inventory.currentItem).isKeyDown || player.isSneaking) {
      currentItemStack.getItem.optionallyDoAs[ModeChangingItem[_]] { mci =>
        mci.tryChangingModes(event.getScrollDelta.sign.toInt, currentItemStack)
        Network.CHANNEL.sendToServer(ModeChangingItem.ModeChangePacket(event.getScrollDelta.sign.toInt))
        event.setCanceled(true)
      }
    }
  }

  @SubscribeEvent
  def onDrawGui(event: RenderGameOverlayEvent.Post): Unit = {
    import RenderGameOverlayEvent.ElementType._
    event.getType match {
      case HOTBAR =>
        // TODO: Fancy selector menu thingy when button is pressed
//        val mc = Minecraft.getInstance
//        val gui = mc.ingameGUI
//        val partialTicks = event.getPartialTicks
//        val matrixStack = event.getMatrixStack
//        if (Minecraft.getInstance.playerController.getCurrentGameType != GameType.SPECTATOR) {
//          val playerentity = mc.player
//          val WIDGETS_TEX_PATH = new ResourceLocation("textures/gui/widgets.png")
//          if (playerentity != null) {
//            RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F)
//            mc.getTextureManager.bindTexture(WIDGETS_TEX_PATH)
//            val itemstack = playerentity.getHeldItemOffhand
//            val handside = playerentity.getPrimaryHand.opposite
//            val screenMiddleX = event.getWindow.getScaledWidth / 2
//            val scaledHeight = event.getWindow.getScaledHeight
//            val guiBlitOffset = gui.getBlitOffset
//            val hotbarWidth = 182
//            val halfHotbarWidth = 91
//            gui.setBlitOffset(-90)
//            gui.blit(matrixStack, screenMiddleX - halfHotbarWidth, scaledHeight - 22, 0, 0, 182, 22)
//            gui.blit(matrixStack, screenMiddleX - halfHotbarWidth - 1 + playerentity.inventory.currentItem * 20, scaledHeight - 22 - 1, 0, 22, 24, 22)
//            if (!itemstack.isEmpty) {
//              handside match {
//                case HandSide.LEFT => gui.blit(matrixStack, screenMiddleX - halfHotbarWidth - 29, scaledHeight - 23, 24, 22, 29, 24)
//                case HandSide.RIGHT => gui.blit(matrixStack, screenMiddleX + halfHotbarWidth, scaledHeight - 23, 53, 22, 29, 24)
//              }
//            }
//            gui.setBlitOffset(guiBlitOffset)
//            RenderSystem.enableRescaleNormal()
//            RenderSystem.enableBlend()
//            RenderSystem.defaultBlendFunc()
//            for (i1 <- 0 until 9) {
//              val j1 = screenMiddleX - 90 + i1 * 20 + 2
//              val k1 = scaledHeight - 16 - 3
//              renderHotbarItem(j1, k1, partialTicks, playerentity, playerentity.inventory.mainInventory.get(i1))
//            }
//            if (!itemstack.isEmpty) {
//              val i2 = scaledHeight - 16 - 3
//              if (handside eq HandSide.LEFT) renderHotbarItem(screenMiddleX - 91 - 26, i2, partialTicks, playerentity, itemstack)
//              else renderHotbarItem(screenMiddleX + 91 + 10, i2, partialTicks, playerentity, itemstack)
//            }
//            if (mc.gameSettings.attackIndicator eq AttackIndicatorStatus.HOTBAR) {
//              val attackCooldown = mc.player.getCooledAttackStrength(0.0F)
//              if (attackCooldown < 1.0F) {
//                val offHandItemX = handside match {
//                  case HandSide.LEFT => screenMiddleX + 91 + 6
//                  case HandSide.RIGHT => screenMiddleX - 91 - 22
//                }
//                mc.getTextureManager.bindTexture(AbstractGui.GUI_ICONS_LOCATION)
//                val l1 = (attackCooldown * 19.0F).toInt
//                RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F)
//                gui.blit(matrixStack, offHandItemX, scaledHeight - 20, 0, 94, 18, 18)
//                gui.blit(matrixStack, offHandItemX, scaledHeight - 20 + 18 - l1, 18, 112 - l1, 18, l1)
//              }
//            }
//            RenderSystem.disableRescaleNormal()
//            RenderSystem.disableBlend()
//          }
//        }
      case _ =>
    }
  }
  private def renderHotbarItem(x: Int, y: Int, partialTicks: Float, player: PlayerEntity, stack: ItemStack): Unit = {
    val mc = Minecraft.getInstance()
    if (!stack.isEmpty) {
      val f = stack.getAnimationsToGo.toFloat - partialTicks
      if (f > 0.0F) {
        RenderSystem.pushMatrix(): @nowarn
        val f1 = 1.0F + f / 5.0F
        RenderSystem.translatef((x + 8).toFloat, (y + 12).toFloat, 0.0F): @nowarn
        RenderSystem.scalef(1.0F / f1, (f1 + 1.0F) / 2.0F, 1.0F): @nowarn
        RenderSystem.translatef((-(x + 8)).toFloat, (-(y + 12)).toFloat, 0.0F): @nowarn
      }
      mc.getItemRenderer.renderItemAndEffectIntoGUI(player, stack, x, y)
      if (f > 0.0F) RenderSystem.popMatrix(): @nowarn
      mc.getItemRenderer.renderItemOverlays(mc.fontRenderer, stack, x, y)
    }
  }
}
