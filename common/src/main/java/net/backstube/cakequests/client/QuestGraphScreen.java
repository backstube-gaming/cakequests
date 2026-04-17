package net.backstube.cakequests.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.backstube.cakequests.CakeQuests;
import net.backstube.cakequests.data.*;
import net.backstube.cakequests.quest.QuestNodeState;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class QuestGraphScreen extends Screen {
    private static final int TAB_WIDTH = 112;
    private static final int NODE_SIZE = 26;
    private static final int DETAILS_MARGIN = 10;
    private static final int DETAILS_IMAGE_MAX_HEIGHT = 140;
    private static final int DETAILS_SCROLL_STEP = 18;
    private static final ResourceLocation GOAL_FRAME_OBTAINED = CakeQuests.id("textures/gui/quest_nodes/goal_frame_obtained.png");
    private static final ResourceLocation GOAL_FRAME_UNOBTAINED = CakeQuests.id("textures/gui/quest_nodes/goal_frame_unobtained.png");
    private static final ResourceLocation TASK_FRAME_OBTAINED = CakeQuests.id("textures/gui/quest_nodes/task_frame_obtained.png");
    private static final ResourceLocation TASK_FRAME_UNOBTAINED = CakeQuests.id("textures/gui/quest_nodes/task_frame_unobtained.png");
    private int selectedTab;
    private QuestNodeDefinition selectedNode;
    private double panX = 80;
    private double panY = 80;
    private double zoom = 1.0D;
    private final Map<ResourceLocation, ImageDimensions> imageDimensions = new HashMap<>();
    private double detailsScroll;
    private boolean draggingGraph;
    private int detailsContentHeight;

    public QuestGraphScreen() {
        super(new TextComponent("Cake Quests"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        renderBackground(poseStack);
        QuestBookDefinition book = ClientQuestGraphStore.activeBook();
        GuiComponent.fill(poseStack, 0, 0, width, height, 0xEE191B22);
        if (book.tabs().isEmpty()) {
            renderTabs(poseStack, book);
            drawCenteredString(poseStack, font, "No quest graphs loaded", width / 2, height / 2 - 4, 0xFFE5E7EB);
            super.render(poseStack, mouseX, mouseY, partialTick);
            return;
        }
        QuestTabDefinition tab = book.tabs().get(Math.max(0, Math.min(selectedTab, book.tabs().size() - 1)));
        QuestNodeDefinition hover = renderGraph(poseStack, tab, mouseX, mouseY);
        renderTabs(poseStack, book);
        renderDetails(poseStack, tab);
        if (hover != null && !draggingGraph && !isInsideDetails(mouseX, mouseY)) {
            renderTooltip(poseStack, hover.title().component(), mouseX, mouseY);
        }
        super.render(poseStack, mouseX, mouseY, partialTick);
    }

    private void renderTabs(PoseStack poseStack, QuestBookDefinition book) {
        poseStack.pushPose();
        poseStack.translate(0.0D, 0.0D, 250.0D);
        GuiComponent.fill(poseStack, 0, 0, TAB_WIDTH, height, 0xF0262932);
        font.draw(poseStack, ClientQuestGraphStore.titleLabel(), 12, 12, 0xFFFFFFFF);
        font.draw(poseStack, ClientQuestGraphStore.subtitle(), 12, 25, 0xFF9CA3AF);
        int y = 48;
        if (ClientQuestGraphStore.fallbackMode()) {
            font.draw(poseStack, "(client only)", 12, 37, 0xFF8B92A1);
            y = 60;
        }
        for (int i = 0; i < book.tabs().size(); i++) {
            QuestTabDefinition tab = book.tabs().get(i);
            int color = i == selectedTab ? tab.tabColor() : 0xFF343844;
            GuiComponent.fill(poseStack, 8, y, TAB_WIDTH - 8, y + 24, color);
            font.draw(poseStack, tab.title().component(), 16, y + 8, 0xFFFFFFFF);
            y += 28;
        }
        poseStack.popPose();
    }

    private QuestNodeDefinition renderGraph(PoseStack poseStack, QuestTabDefinition tab, int mouseX, int mouseY) {
        int left = TAB_WIDTH;
        GuiComponent.fill(poseStack, left, 0, width, height, 0xFF101218);
        poseStack.pushPose();
        poseStack.translate(left + panX, panY, 0);
        poseStack.scale((float) zoom, (float) zoom, 1.0F);
        for (QuestNodeDefinition node : tab.nodes()) {
            for (String parentId : node.parents()) {
                QuestNodeDefinition parent = tab.nodes().stream().filter(candidate -> candidate.id().equals(parentId)).findFirst().orElse(null);
                if (parent != null) {
                    drawEdge(poseStack, parent.x(), parent.y(), node.x(), node.y(), stateColor(ClientAdvancementBridge.state(tab, node), 0xFF687080));
                }
            }
        }
        QuestNodeDefinition hover = null;
        for (QuestNodeDefinition node : tab.nodes()) {
            renderNodeFrame(poseStack, tab, node);
            if (hitNode(mouseX, mouseY, node)) {
                hover = node;
            }
        }
        poseStack.popPose();
        renderScaledNodeIcons(poseStack, tab);
        return hover;
    }

    private void drawEdge(PoseStack poseStack, int x1, int y1, int x2, int y2, int color) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1)) / 6 + 1;
        for (int i = 0; i <= steps; i++) {
            double t = steps == 0 ? 0.0D : (double) i / (double) steps;
            int x = (int) (x1 + (x2 - x1) * t);
            int y = (int) (y1 + (y2 - y1) * t);
            GuiComponent.fill(poseStack, x - 1, y - 1, x + 2, y + 2, color);
        }
    }

    private void renderNodeFrame(PoseStack poseStack, QuestTabDefinition tab, QuestNodeDefinition node) {
        QuestNodeState state = ClientAdvancementBridge.state(tab, node);
        int x = node.x() - NODE_SIZE / 2;
        int y = node.y() - NODE_SIZE / 2;
        renderNodeFrameTexture(poseStack, x, y, nodeFrameTexture(node, state));
    }

    private void renderNodeFrameTexture(PoseStack poseStack, int x, int y, ResourceLocation texture) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, texture);
        GuiComponent.blit(poseStack, x, y, 0.0F, 0.0F, NODE_SIZE, NODE_SIZE, NODE_SIZE, NODE_SIZE);
    }

    private ResourceLocation nodeFrameTexture(QuestNodeDefinition node, QuestNodeState state) {
        boolean obtained = state == QuestNodeState.COMPLETE;
        if (node.shape() == QuestNodeShape.DIAMOND) {
            return obtained ? GOAL_FRAME_OBTAINED : GOAL_FRAME_UNOBTAINED;
        }
        return obtained ? TASK_FRAME_OBTAINED : TASK_FRAME_UNOBTAINED;
    }

    private void renderScaledNodeIcons(PoseStack poseStack, QuestTabDefinition tab) {
        int left = TAB_WIDTH;
        PoseStack modelView = RenderSystem.getModelViewStack();
        modelView.pushPose();
        modelView.translate(left + panX, panY, 0.0D);
        modelView.scale((float) zoom, (float) zoom, 1.0F);
        RenderSystem.applyModelViewMatrix();
        for (QuestNodeDefinition node : tab.nodes()) {
            renderNodeItem(node);
        }
        modelView.popPose();
        RenderSystem.applyModelViewMatrix();

        poseStack.pushPose();
        poseStack.translate(left + panX, panY, 250.0D);
        poseStack.scale((float) zoom, (float) zoom, 1.0F);
        for (QuestNodeDefinition node : tab.nodes()) {
            renderNodeStatus(poseStack, tab, node);
        }
        poseStack.popPose();
    }

    private void renderNodeItem(QuestNodeDefinition node) {
        int x = node.x() - 8;
        int y = node.y() - 8;
        minecraft.getItemRenderer().renderAndDecorateItem(node.icon().stack(), x, y);
    }

    private void renderNodeStatus(PoseStack poseStack, QuestTabDefinition tab, QuestNodeDefinition node) {
        QuestNodeState state = ClientAdvancementBridge.state(tab, node);
        int x = node.x() - 8;
        int y = node.y() - 8;
        if (state == QuestNodeState.COMPLETE) {
            renderCheckIcon(poseStack, x + 13, y + 13);
        } else if (state == QuestNodeState.LOCKED) {
            GuiComponent.fill(poseStack, x, y, x + 16, y + 16, 0x88000000);
            renderSmallLockIcon(poseStack, x + 13, y + 13);
        }
    }

    private int stateColor(QuestNodeState state, int base) {
        return switch (state) {
            case COMPLETE -> 0xFF4FB870;
            case LOCKED -> 0xFF555B66;
            case AVAILABLE -> base;
        };
    }

    private void renderDetails(PoseStack poseStack, QuestTabDefinition tab) {
        if (selectedNode == null) {
            return;
        }
        int panelWidth = detailsWidth();
        int left = detailsLeft(panelWidth);
        int top = DETAILS_MARGIN;
        int right = width - DETAILS_MARGIN;
        int bottom = height - DETAILS_MARGIN;
        int textLeft = left + 42;
        int contentLeft = left + 14;
        int contentTop = top + 62;
        int contentBottom = bottom - 12;
        int contentWidth = right - left - 28;
        int viewportHeight = Math.max(0, contentBottom - contentTop);
        QuestNodeState state = ClientAdvancementBridge.state(tab, selectedNode);
        detailsContentHeight = measureDescriptionHeight(selectedNode, contentWidth);
        detailsScroll = clampDetailsScroll(detailsScroll, viewportHeight);
        poseStack.pushPose();
        poseStack.translate(0.0D, 0.0D, 300.0D);
        GuiComponent.fill(poseStack, left, top, right, bottom, 0xF02A2E38);
        GuiComponent.fill(poseStack, left, top, right, top + 42, 0xF0363B47);
        GuiComponent.fill(poseStack, width - 30, top + 8, width - 18, top + 20, 0xFF3D4350);
        drawCenteredString(poseStack, font, "x", width - 24, top + 10, 0xFFFFFFFF);
        renderNodeFrameTexture(poseStack, left + 10, top + 8, nodeFrameTexture(selectedNode, state));
        minecraft.getItemRenderer().renderAndDecorateItem(selectedNode.icon().stack(), left + 15, top + 13);
        if (state == QuestNodeState.LOCKED) {
            renderLockIcon(poseStack, right - 50, top + 14, 0xFFC3C8D0, 0xFF2A2E38);
        }
        font.draw(poseStack, selectedNode.title().component(), textLeft, top + 10, 0xFFFFFFFF);
        font.draw(poseStack, selectedNode.subtitle().component(), textLeft, top + 24, 0xFFB6BCC8);
        GuiComponent.fill(poseStack, left + 12, top + 50, right - 12, top + 51, 0xFF454B58);
        enableScissor(contentLeft, contentTop, contentWidth, viewportHeight);
        poseStack.pushPose();
        poseStack.translate(0.0D, -detailsScroll, 0.0D);
        renderDescription(poseStack, selectedNode, contentLeft, contentTop, contentWidth);
        poseStack.popPose();
        RenderSystem.disableScissor();
        renderDetailsScrollBar(poseStack, right, contentTop, viewportHeight);
        poseStack.popPose();
    }

    private int measureDescriptionHeight(QuestNodeDefinition node, int contentWidth) {
        int height = 0;
        MutableComponent paragraph = new TextComponent("");
        boolean hasText = false;
        for (QuestDescriptionElement element : node.description()) {
            if (element.kind() == QuestDescriptionElement.Kind.IMAGE) {
                height = appendMeasuredParagraph(height, paragraph, hasText, contentWidth);
                paragraph = new TextComponent("");
                hasText = false;
                height += imageHeight(element.image(), contentWidth) + 8;
            } else {
                paragraph.append(element.text().component());
                hasText = true;
            }
        }
        return appendMeasuredParagraph(height, paragraph, hasText, contentWidth);
    }

    private int appendMeasuredParagraph(int height, MutableComponent paragraph, boolean hasText, int contentWidth) {
        if (!hasText) {
            return height;
        }
        return height + font.split(paragraph, contentWidth).size() * 11 + 5;
    }

    private int renderDescription(PoseStack poseStack, QuestNodeDefinition node, int x, int y, int contentWidth) {
        MutableComponent paragraph = new TextComponent("");
        boolean hasText = false;
        int cursorY = y;
        for (QuestDescriptionElement element : node.description()) {
            if (element.kind() == QuestDescriptionElement.Kind.IMAGE) {
                cursorY = renderDescriptionParagraph(poseStack, paragraph, hasText, x, cursorY, contentWidth);
                paragraph = new TextComponent("");
                hasText = false;
                int imageHeight = imageHeight(element.image(), contentWidth);
                renderDescriptionImage(poseStack, element.image(), x, cursorY, contentWidth, imageHeight);
                cursorY += imageHeight + 8;
            } else {
                paragraph.append(element.text().component());
                hasText = true;
            }
        }
        return renderDescriptionParagraph(poseStack, paragraph, hasText, x, cursorY, contentWidth);
    }

    private int renderDescriptionParagraph(PoseStack poseStack, MutableComponent paragraph, boolean hasText, int x, int y, int contentWidth) {
        if (!hasText) {
            return y;
        }
        for (var wrapped : font.split(paragraph, contentWidth)) {
            font.draw(poseStack, wrapped, x, y, 0xFFE5E7EB);
            y += 11;
        }
        return y + 5;
    }

    private void renderDescriptionImage(PoseStack poseStack, ResourceLocation image, int x, int y, int contentWidth, int imageHeight) {
        ImageDimensions dimensions = imageDimensions(image);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, image);
        GuiComponent.blit(poseStack, x, y, contentWidth, imageHeight, 0.0F, 0.0F, dimensions.width(), dimensions.height(), dimensions.width(), dimensions.height());
    }

    private int imageHeight(ResourceLocation image, int contentWidth) {
        ImageDimensions dimensions = imageDimensions(image);
        if (dimensions.width() <= 0) {
            return Math.min(contentWidth, DETAILS_IMAGE_MAX_HEIGHT);
        }
        int scaledHeight = Math.max(1, (int) Math.round(contentWidth * (dimensions.height() / (double) dimensions.width())));
        return Math.min(scaledHeight, DETAILS_IMAGE_MAX_HEIGHT);
    }

    private ImageDimensions imageDimensions(ResourceLocation image) {
        return imageDimensions.computeIfAbsent(image, location -> {
            try (Resource resource = minecraft.getResourceManager().getResource(location);
                 InputStream stream = resource.getInputStream();
                 NativeImage nativeImage = NativeImage.read(stream)) {
                return new ImageDimensions(nativeImage.getWidth(), nativeImage.getHeight());
            } catch (IOException | RuntimeException ex) {
                CakeQuests.LOGGER.warn("Unable to read quest description image {}", location, ex);
                return new ImageDimensions(16, 16);
            }
        });
    }

    private void renderDetailsScrollBar(PoseStack poseStack, int right, int top, int viewportHeight) {
        if (detailsContentHeight <= viewportHeight || viewportHeight <= 0) {
            return;
        }
        int trackX = right - 8;
        GuiComponent.fill(poseStack, trackX, top, trackX + 4, top + viewportHeight, 0x803D4350);
        int thumbHeight = Math.max(18, viewportHeight * viewportHeight / detailsContentHeight);
        int maxThumbTravel = viewportHeight - thumbHeight;
        int thumbY = top + (int) Math.round(maxThumbTravel * (detailsScroll / maxDetailsScroll(viewportHeight)));
        GuiComponent.fill(poseStack, trackX, thumbY, trackX + 4, thumbY + thumbHeight, 0xFF9CA3AF);
    }

    private void enableScissor(int x, int y, int width, int height) {
        double scale = minecraft.getWindow().getGuiScale();
        int scissorX = (int) Math.round(x * scale);
        int scissorY = (int) Math.round((minecraft.getWindow().getGuiScaledHeight() - y - height) * scale);
        int scissorWidth = (int) Math.round(width * scale);
        int scissorHeight = (int) Math.round(height * scale);
        RenderSystem.enableScissor(scissorX, scissorY, scissorWidth, scissorHeight);
    }

    private void renderLockIcon(PoseStack poseStack, int x, int y, int color, int keyholeColor) {
        GuiComponent.fill(poseStack, x + 3, y + 7, x + 13, y + 15, color);
        GuiComponent.fill(poseStack, x + 5, y + 3, x + 11, y + 5, color);
        GuiComponent.fill(poseStack, x + 4, y + 5, x + 6, y + 8, color);
        GuiComponent.fill(poseStack, x + 10, y + 5, x + 12, y + 8, color);
        GuiComponent.fill(poseStack, x + 7, y + 10, x + 9, y + 13, keyholeColor);
    }

    private void renderSmallLockIcon(PoseStack poseStack, int x, int y) {
        GuiComponent.fill(poseStack, x, y, x + 10, y + 10, 0xFF223040);
        GuiComponent.fill(poseStack, x + 1, y + 5, x + 9, y + 10, 0xFFE5E7EB);
        GuiComponent.fill(poseStack, x + 3, y + 2, x + 7, y + 4, 0xFFE5E7EB);
        GuiComponent.fill(poseStack, x + 2, y + 4, x + 4, y + 6, 0xFFE5E7EB);
        GuiComponent.fill(poseStack, x + 6, y + 4, x + 8, y + 6, 0xFFE5E7EB);
        GuiComponent.fill(poseStack, x + 4, y + 7, x + 6, y + 9, 0xFF20242D);
    }

    private void renderCheckIcon(PoseStack poseStack, int x, int y) {
        GuiComponent.fill(poseStack, x, y, x + 10, y + 10, 0xFF22382B);
        GuiComponent.fill(poseStack, x + 1, y + 5, x + 3, y + 7, 0xFF62D26F);
        GuiComponent.fill(poseStack, x + 2, y + 6, x + 4, y + 8, 0xFF62D26F);
        GuiComponent.fill(poseStack, x + 3, y + 7, x + 5, y + 9, 0xFF62D26F);
        GuiComponent.fill(poseStack, x + 4, y + 6, x + 6, y + 8, 0xFF62D26F);
        GuiComponent.fill(poseStack, x + 5, y + 5, x + 7, y + 7, 0xFF62D26F);
        GuiComponent.fill(poseStack, x + 6, y + 4, x + 8, y + 6, 0xFF62D26F);
        GuiComponent.fill(poseStack, x + 7, y + 3, x + 9, y + 5, 0xFF62D26F);
        GuiComponent.fill(poseStack, x + 8, y + 2, x + 10, y + 4, 0xFF62D26F);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        QuestBookDefinition book = ClientQuestGraphStore.activeBook();
        if (mouseX < TAB_WIDTH) {
            int tab = ((int) mouseY - 48) / 28;
            if (tab >= 0 && tab < book.tabs().size()) {
                selectedTab = tab;
                selectedNode = null;
                detailsScroll = 0.0D;
                return true;
            }
        } else if (!book.tabs().isEmpty()) {
            if (selectedNode != null && isCloseDetails(mouseX, mouseY)) {
                selectedNode = null;
                detailsScroll = 0.0D;
                return true;
            }
            if (isInsideDetails(mouseX, mouseY)) {
                return true;
            }
            QuestTabDefinition tab = book.tabs().get(selectedTab);
            for (QuestNodeDefinition node : tab.nodes()) {
                if (hitNode(mouseX, mouseY, node)) {
                    selectedNode = node;
                    detailsScroll = 0.0D;
                    return true;
                }
            }
            selectedNode = null;
            detailsScroll = 0.0D;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && mouseX > TAB_WIDTH && !isInsideDetails(mouseX, mouseY)) {
            draggingGraph = true;
            panX += dragX;
            panY += dragY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            draggingGraph = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (isInsideDetails(mouseX, mouseY)) {
            int viewportHeight = Math.max(0, height - DETAILS_MARGIN * 2 - 74);
            if (detailsContentHeight > viewportHeight) {
                detailsScroll = clampDetailsScroll(detailsScroll - delta * DETAILS_SCROLL_STEP, viewportHeight);
                return true;
            }
        }
        if (mouseX > TAB_WIDTH && !isInsideDetails(mouseX, mouseY)) {
            zoom = Math.max(0.5D, Math.min(2.0D, zoom + delta * 0.1D));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (selectedNode != null && keyCode == 256) {
            selectedNode = null;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private boolean hitNode(double mouseX, double mouseY, QuestNodeDefinition node) {
        double graphX = (mouseX - TAB_WIDTH - panX) / zoom;
        double graphY = (mouseY - panY) / zoom;
        return graphX >= node.x() - NODE_SIZE / 2.0D && graphX <= node.x() + NODE_SIZE / 2.0D
                && graphY >= node.y() - NODE_SIZE / 2.0D && graphY <= node.y() + NODE_SIZE / 2.0D;
    }

    private int screenX(int graphX) {
        return (int) Math.round(TAB_WIDTH + panX + graphX * zoom);
    }

    private int screenY(int graphY) {
        return (int) Math.round(panY + graphY * zoom);
    }

    private int detailsWidth() {
        return Math.max(160, Math.min(260, width - TAB_WIDTH - DETAILS_MARGIN * 2));
    }

    private double clampDetailsScroll(double value, int viewportHeight) {
        return Math.max(0.0D, Math.min(maxDetailsScroll(viewportHeight), value));
    }

    private double maxDetailsScroll(int viewportHeight) {
        return Math.max(0.0D, detailsContentHeight - viewportHeight);
    }

    private int detailsLeft(int panelWidth) {
        return width - panelWidth - DETAILS_MARGIN;
    }

    private boolean isInsideDetails(double mouseX, double mouseY) {
        if (selectedNode == null) {
            return false;
        }
        int panelWidth = detailsWidth();
        return mouseX >= detailsLeft(panelWidth) && mouseX <= width - DETAILS_MARGIN
                && mouseY >= DETAILS_MARGIN && mouseY <= height - DETAILS_MARGIN;
    }

    private boolean isCloseDetails(double mouseX, double mouseY) {
        return mouseX >= width - 30 && mouseX <= width - 18
                && mouseY >= DETAILS_MARGIN + 8 && mouseY <= DETAILS_MARGIN + 20;
    }

    private record ImageDimensions(int width, int height) {
    }
}
