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
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuestGraphScreen extends Screen {
    private static final int TAB_WIDTH = 112;
    private static final int NODE_SIZE = 26;
    private static final int DETAILS_MARGIN = 10;
    private static final int DETAILS_IMAGE_MAX_HEIGHT = 140;
    private static final int DETAILS_SCROLL_STEP = 18;
    private static final int TAB_SCROLL_STEP = 28;
    private static final int TAB_TOP = 48;
    private static final int TAB_HEIGHT = 24;
    private static final int TAB_GAP = 4;
    private static final int CHECK_BUTTON_SIZE = 22;
    private static final int CHECK_BUTTON_MARGIN = 10;
    private static final int EDGE_OUTLINE_COLOR = 0xFF000000;
    private static final int EDGE_AVAILABLE_COLOR = 0xFFFFF1A8;
    private static final int EDGE_LOCKED_COLOR = 0xFF555B66;
    private static final double DEFAULT_PAN_X = 80.0D;
    private static final double DEFAULT_PAN_Y = 80.0D;
    private static final double DEFAULT_ZOOM = 1.0D;
    private static final double MIN_ZOOM = 0.5D;
    private static final double MAX_ZOOM = 2.0D;
    private static final ResourceLocation GOAL_FRAME_OBTAINED = CakeQuests.id("textures/gui/quest_nodes/goal_frame_obtained.png");
    private static final ResourceLocation GOAL_FRAME_UNOBTAINED = CakeQuests.id("textures/gui/quest_nodes/goal_frame_unobtained.png");
    private static final ResourceLocation TASK_FRAME_OBTAINED = CakeQuests.id("textures/gui/quest_nodes/task_frame_obtained.png");
    private static final ResourceLocation TASK_FRAME_UNOBTAINED = CakeQuests.id("textures/gui/quest_nodes/task_frame_unobtained.png");
    private static final ResourceLocation CHALLENGE_FRAME_OBTAINED = CakeQuests.id("textures/gui/quest_nodes/challenge_frame_obtained.png");
    private static final ResourceLocation CHALLENGE_FRAME_UNOBTAINED = CakeQuests.id("textures/gui/quest_nodes/challenge_frame_unobtained.png");
    private int selectedTab;
    private QuestNodeDefinition selectedNode;
    private double panX = DEFAULT_PAN_X;
    private double panY = DEFAULT_PAN_Y;
    private double zoom = DEFAULT_ZOOM;
    private final Map<ResourceLocation, ImageDimensions> imageDimensions = new HashMap<>();
    private double detailsScroll;
    private boolean draggingGraph;
    private int detailsContentHeight;
    private final List<DescriptionLine> descriptionLines = new ArrayList<>();
    private double tabScroll;
    private int checkButtonLeft = -1;
    private int checkButtonTop = -1;
    private String viewGraphHash = "";

    public QuestGraphScreen() {
        super(new TextComponent("Cake Quests"));
        syncViewMemory();
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
        syncViewMemory();
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
        tabScroll = clampTabScroll(tabScroll, book);
        enableScissor(0, TAB_TOP, TAB_WIDTH, Math.max(0, height - TAB_TOP));
        int y = TAB_TOP - (int) Math.round(tabScroll);
        for (int i = 0; i < book.tabs().size(); i++) {
            QuestTabDefinition tab = book.tabs().get(i);
            int color = i == selectedTab ? tab.tabColor() : 0xFF343844;
            GuiComponent.fill(poseStack, 8, y, TAB_WIDTH - 8, y + TAB_HEIGHT, color);
            font.draw(poseStack, tab.title().component(), 16, y + 8, 0xFFFFFFFF);
            y += TAB_HEIGHT + TAB_GAP;
        }
        RenderSystem.disableScissor();
        renderTabScrollBar(poseStack, book);
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
                    drawEdge(poseStack, parent.x(), parent.y(), node.x(), node.y(), edgeColor(ClientQuestProgressStore.state(tab, node)));
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
        int midX = x1 + (x2 - x1) / 2;
        drawEdgeSegments(poseStack, x1, y1, midX, y2, x2, EDGE_OUTLINE_COLOR, 3);
        drawEdgeSegments(poseStack, x1, y1, midX, y2, x2, color, 1);
    }

    private void drawEdgeSegments(PoseStack poseStack, int x1, int y1, int midX, int y2, int x2, int color, int thickness) {
        drawHorizontalEdgeSegment(poseStack, x1, midX, y1, color, thickness);
        drawVerticalEdgeSegment(poseStack, midX, y1, y2, color, thickness);
        drawHorizontalEdgeSegment(poseStack, midX, x2, y2, color, thickness);
    }

    private void drawHorizontalEdgeSegment(PoseStack poseStack, int x1, int x2, int y, int color, int thickness) {
        int left = Math.min(x1, x2);
        int right = Math.max(x1, x2);
        int halfThickness = thickness / 2;
        GuiComponent.fill(poseStack, left, y - halfThickness, right + 1, y + halfThickness + 1, color);
    }

    private void drawVerticalEdgeSegment(PoseStack poseStack, int x, int y1, int y2, int color, int thickness) {
        int top = Math.min(y1, y2);
        int bottom = Math.max(y1, y2);
        int halfThickness = thickness / 2;
        GuiComponent.fill(poseStack, x - halfThickness, top, x + halfThickness + 1, bottom + 1, color);
    }

    private void renderNodeFrame(PoseStack poseStack, QuestTabDefinition tab, QuestNodeDefinition node) {
        QuestNodeState state = ClientQuestProgressStore.state(tab, node);
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
        if (node.shape() == QuestNodeShape.CHALLENGE) {
            return obtained ? CHALLENGE_FRAME_OBTAINED : CHALLENGE_FRAME_UNOBTAINED;
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
        QuestNodeState state = ClientQuestProgressStore.state(tab, node);
        int x = node.x() - 8;
        int y = node.y() - 8;
        if (state == QuestNodeState.COMPLETE) {
            renderCheckIcon(poseStack, x + 13, y + 13);
        } else if (state == QuestNodeState.LOCKED) {
            GuiComponent.fill(poseStack, x, y, x + 16, y + 16, 0x88000000);
            renderSmallLockIcon(poseStack, x + 13, y + 13);
        }
    }

    private int edgeColor(QuestNodeState state) {
        return switch (state) {
            case LOCKED -> EDGE_LOCKED_COLOR;
            case COMPLETE, AVAILABLE -> EDGE_AVAILABLE_COLOR;
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
        QuestNodeState state = ClientQuestProgressStore.state(tab, selectedNode);
        detailsContentHeight = measureProgressHeight(selectedNode) + measureDescriptionHeight(selectedNode, contentWidth) + measureCheckButtonHeight(selectedNode);
        detailsScroll = clampDetailsScroll(detailsScroll, viewportHeight);
        descriptionLines.clear();
        checkButtonLeft = -1;
        checkButtonTop = -1;
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
        int descriptionTop = renderProgress(poseStack, tab, selectedNode, contentLeft, contentTop);
        int checkTop = renderDescription(poseStack, selectedNode, contentLeft, descriptionTop, contentWidth);
        renderCheckButton(poseStack, tab, selectedNode, state, contentLeft, checkTop, contentWidth);
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

    private int measureProgressHeight(QuestNodeDefinition node) {
        int rows = 0;
        for (QuestEventRequirement event : node.events()) {
            if (event.type() != QuestEventType.CHECK) {
                rows++;
            }
        }
        return rows == 0 ? 0 : rows * 12 + 4;
    }

    private int measureCheckButtonHeight(QuestNodeDefinition node) {
        return hasCheckRequirement(node) ? CHECK_BUTTON_SIZE + CHECK_BUTTON_MARGIN : 0;
    }

    private int renderProgress(PoseStack poseStack, QuestTabDefinition tab, QuestNodeDefinition node, int x, int y) {
        if (!hasVisibleProgress(node)) {
            return y;
        }
        int cursorY = y;
        for (QuestEventRequirement event : node.events()) {
            if (event.type() == QuestEventType.CHECK) {
                continue;
            }
            long count = ClientQuestProgressStore.count(tab.id(), node.id(), event.id());
            MutableComponent label = targetDisplayName(event.target()).copy()
                    .append(" " + Math.min(count, event.count()) + "/" + event.count());
            font.draw(poseStack, label, x, cursorY, count >= event.count() ? 0xFF7DDA8A : 0xFFE5E7EB);
            cursorY += 12;
        }
        return cursorY + 4;
    }

    private boolean hasVisibleProgress(QuestNodeDefinition node) {
        for (QuestEventRequirement event : node.events()) {
            if (event.type() != QuestEventType.CHECK) {
                return true;
            }
        }
        return false;
    }

    private Component targetDisplayName(QuestItemTarget target) {
        if (target.tag()) {
            TagDisplayName configured = ClientQuestGraphStore.tagName(target.id());
            if (configured != null) {
                if (!configured.translate().isBlank() && I18n.exists(configured.translate())) {
                    return new TranslatableComponent(configured.translate());
                }
                if (!configured.fallback().isBlank()) {
                    return new TextComponent(configured.fallback());
                }
            }
            String translationKey = tagTranslationKey(target.id());
            if (translationKey != null) {
                return new TranslatableComponent(translationKey);
            }
            return new TextComponent("Any #" + target.id());
        }
        return Registry.ITEM.getOptional(target.id())
                .map(ItemStack::new)
                .map(ItemStack::getHoverName)
                .orElseGet(() -> new TextComponent(target.id().toString()));
    }

    private String tagTranslationKey(ResourceLocation tag) {
        String path = tag.getPath().replace('/', '.');
        String[] keys = {
                "tag.item." + tag.getNamespace() + "." + path,
                "tag." + tag.getNamespace() + "." + path
        };
        for (String key : keys) {
            if (I18n.exists(key)) {
                return key;
            }
        }
        return null;
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
            descriptionLines.add(new DescriptionLine(x, y - (int) Math.round(detailsScroll), contentWidth, wrapped));
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

    private void renderCheckButton(PoseStack poseStack, QuestTabDefinition tab, QuestNodeDefinition node, QuestNodeState state, int x, int y, int contentWidth) {
        if (!hasCheckRequirement(node)) {
            return;
        }
        int left = x + (contentWidth - CHECK_BUTTON_SIZE) / 2;
        int top = y + CHECK_BUTTON_MARGIN;
        boolean enabled = state == QuestNodeState.AVAILABLE;
        int background = state == QuestNodeState.COMPLETE ? 0xFF22382B : enabled ? 0xFF3D4350 : 0xFF252932;
        int border = enabled ? 0xFF9CA3AF : 0xFF4B5563;
        GuiComponent.fill(poseStack, left, top, left + CHECK_BUTTON_SIZE, top + CHECK_BUTTON_SIZE, border);
        GuiComponent.fill(poseStack, left + 1, top + 1, left + CHECK_BUTTON_SIZE - 1, top + CHECK_BUTTON_SIZE - 1, background);
        renderCheckIcon(poseStack, left + 6, top + 6);
        checkButtonLeft = left;
        checkButtonTop = top - (int) Math.round(detailsScroll);
    }

    private boolean hasCheckRequirement(QuestNodeDefinition node) {
        for (QuestEventRequirement event : node.events()) {
            if (event.type() == QuestEventType.CHECK) {
                return true;
            }
        }
        return false;
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

    private void renderTabScrollBar(PoseStack poseStack, QuestBookDefinition book) {
        int viewportHeight = Math.max(0, height - TAB_TOP);
        int contentHeight = tabContentHeight(book);
        if (contentHeight <= viewportHeight || viewportHeight <= 0) {
            return;
        }
        int trackX = TAB_WIDTH - 6;
        GuiComponent.fill(poseStack, trackX, TAB_TOP, trackX + 3, height, 0x803D4350);
        int thumbHeight = Math.max(18, viewportHeight * viewportHeight / contentHeight);
        int maxThumbTravel = viewportHeight - thumbHeight;
        int thumbY = TAB_TOP + (int) Math.round(maxThumbTravel * (tabScroll / maxTabScroll(book)));
        GuiComponent.fill(poseStack, trackX, thumbY, trackX + 3, thumbY + thumbHeight, 0xFF9CA3AF);
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
            int tab = (int) Math.floor((mouseY - TAB_TOP + tabScroll) / (TAB_HEIGHT + TAB_GAP));
            if (tab >= 0 && tab < book.tabs().size()) {
                selectedTab = tab;
                selectedNode = null;
                detailsScroll = 0.0D;
                rememberView();
                return true;
            }
        } else if (!book.tabs().isEmpty()) {
            if (selectedNode != null && isCloseDetails(mouseX, mouseY)) {
                selectedNode = null;
                detailsScroll = 0.0D;
                return true;
            }
            if (isInsideDetails(mouseX, mouseY)) {
                QuestTabDefinition tab = book.tabs().get(selectedTab);
                if (button == 0 && handleCheckButtonClick(tab, mouseX, mouseY)) {
                    return true;
                }
                if (button == 0 && handleDescriptionLinkClick(mouseX, mouseY)) {
                    return true;
                }
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
            rememberView();
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
        if (mouseX < TAB_WIDTH) {
            QuestBookDefinition book = ClientQuestGraphStore.activeBook();
            double oldScroll = tabScroll;
            tabScroll = clampTabScroll(tabScroll - delta * TAB_SCROLL_STEP, book);
            return oldScroll != tabScroll;
        }
        if (isInsideDetails(mouseX, mouseY)) {
            int viewportHeight = Math.max(0, height - DETAILS_MARGIN * 2 - 74);
            if (detailsContentHeight > viewportHeight) {
                detailsScroll = clampDetailsScroll(detailsScroll - delta * DETAILS_SCROLL_STEP, viewportHeight);
                return true;
            }
        }
        if (mouseX > TAB_WIDTH && !isInsideDetails(mouseX, mouseY)) {
            zoom = clampZoom(zoom + delta * 0.1D);
            rememberView();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void removed() {
        rememberView();
        super.removed();
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

    private void syncViewMemory() {
        QuestBookDefinition book = ClientQuestGraphStore.activeBook();
        if (viewGraphHash.equals(book.hash())) {
            selectedTab = Math.max(0, Math.min(selectedTab, Math.max(0, book.tabs().size() - 1)));
            return;
        }
        viewGraphHash = book.hash();
        selectedTab = 0;
        selectedNode = null;
        detailsScroll = 0.0D;
        tabScroll = 0.0D;
        panX = DEFAULT_PAN_X;
        panY = DEFAULT_PAN_Y;
        zoom = DEFAULT_ZOOM;
        ClientQuestViewMemory.Snapshot snapshot = ClientQuestViewMemory.snapshotFor(book);
        if (snapshot == null) {
            return;
        }
        selectedTab = tabIndex(book, snapshot.tabId());
        panX = snapshot.panX();
        panY = snapshot.panY();
        zoom = clampZoom(snapshot.zoom());
    }

    private void rememberView() {
        QuestBookDefinition book = ClientQuestGraphStore.activeBook();
        if (book.tabs().isEmpty()) {
            return;
        }
        selectedTab = Math.max(0, Math.min(selectedTab, book.tabs().size() - 1));
        ClientQuestViewMemory.remember(book, book.tabs().get(selectedTab).id(), panX, panY, zoom);
    }

    private int tabIndex(QuestBookDefinition book, String tabId) {
        for (int i = 0; i < book.tabs().size(); i++) {
            if (book.tabs().get(i).id().equals(tabId)) {
                return i;
            }
        }
        return 0;
    }

    private double clampZoom(double value) {
        return Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, value));
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

    private double clampTabScroll(double value, QuestBookDefinition book) {
        return Math.max(0.0D, Math.min(maxTabScroll(book), value));
    }

    private double maxTabScroll(QuestBookDefinition book) {
        return Math.max(0.0D, tabContentHeight(book) - Math.max(0, height - TAB_TOP));
    }

    private int tabContentHeight(QuestBookDefinition book) {
        return book.tabs().isEmpty() ? 0 : book.tabs().size() * (TAB_HEIGHT + TAB_GAP) - TAB_GAP;
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

    private boolean handleCheckButtonClick(QuestTabDefinition tab, double mouseX, double mouseY) {
        if (selectedNode == null || checkButtonLeft < 0 || checkButtonTop < 0 || ClientQuestProgressStore.state(tab, selectedNode) != QuestNodeState.AVAILABLE) {
            return false;
        }
        if (mouseX < checkButtonLeft || mouseX > checkButtonLeft + CHECK_BUTTON_SIZE
                || mouseY < checkButtonTop || mouseY > checkButtonTop + CHECK_BUTTON_SIZE) {
            return false;
        }
        sendCheckCompletion(tab.id(), selectedNode.id());
        return true;
    }

    private boolean handleDescriptionLinkClick(double mouseX, double mouseY) {
        for (DescriptionLine line : descriptionLines) {
            if (mouseY < line.y() || mouseY > line.y() + 10 || mouseX < line.x() || mouseX > line.x() + line.width()) {
                continue;
            }
            Style style = font.getSplitter().componentStyleAtWidth(line.text(), (int) Math.round(mouseX - line.x()));
            if (style != null && style.getClickEvent() != null && openJeiLink(style.getClickEvent())) {
                return true;
            }
        }
        return false;
    }

    private boolean openJeiLink(ClickEvent event) {
        if (event.getAction() != ClickEvent.Action.CHANGE_PAGE || !event.getValue().startsWith("cakequests:jei:")) {
            return false;
        }
        String itemId = event.getValue().substring("cakequests:jei:".length());
        try {
            Class<?> helper = Class.forName("net.backstube.cakequests.forge.jei.CakeQuestsJeiHelper");
            Method method = helper.getMethod("showItemRecipes", String.class);
            method.invoke(null, itemId);
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ex) {
            return false;
        }
    }

    private void sendCheckCompletion(String tabId, String nodeId) {
        try {
            Class<?> network = Class.forName("net.backstube.cakequests.forge.CakeQuestsForgeNetwork");
            Method method = network.getMethod("sendCheckCompletion", String.class, String.class, String.class);
            method.invoke(null, ClientQuestProgressStore.graphHash(), tabId, nodeId);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // The common screen can run without the Forge networking hook in non-Forge test contexts.
        }
    }

    private record ImageDimensions(int width, int height) {
    }

    private record DescriptionLine(int x, int y, int width, FormattedCharSequence text) {
    }
}
