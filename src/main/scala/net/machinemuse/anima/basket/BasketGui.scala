package net.machinemuse.anima
package basket

import com.mojang.blaze3d.matrix.MatrixStack
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.gui.screen.inventory.ContainerScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.util.ResourceLocation
import net.minecraft.util.text.ITextComponent

import scala.annotation.nowarn

/**
 * Created by MachineMuse on 1/22/2021.
 */
class BasketGui(screenContainer: BasketContainer, inv: PlayerInventory, titleIn: ITextComponent) extends ContainerScreen[BasketContainer](screenContainer, inv, titleIn) {
  /** The ResourceLocation containing the chest GUI texture. */
  private val DISPENSER_GUI_TEXTURES = new ResourceLocation("textures/gui/container/dispenser.png")

  override def init(): Unit = {
    super.init()
    this.titleX = (this.xSize - this.font.getStringPropertyWidth(this.title)) / 2
  }

  override def render(matrixStack: MatrixStack, mouseX: Int, mouseY: Int, partialTicks: Float): Unit = {
    this.renderBackground(matrixStack)
    super.render(matrixStack, mouseX, mouseY, partialTicks)
    this.renderHoveredTooltip(matrixStack, mouseX, mouseY)
  }

  override protected def drawGuiContainerBackgroundLayer(matrixStack: MatrixStack, partialTicks: Float, x: Int, y: Int): Unit = {
    RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F): @nowarn
    this.minecraft.getTextureManager.bindTexture(DISPENSER_GUI_TEXTURES)
    val i = (this.width - this.xSize) / 2
    val j = (this.height - this.ySize) / 2
    this.blit(matrixStack, i, j, 0, 0, this.xSize, this.ySize)
  }
}
