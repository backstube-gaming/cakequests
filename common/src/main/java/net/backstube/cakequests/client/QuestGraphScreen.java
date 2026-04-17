package net.backstube.cakequests.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.backstube.cakequests.data.QuestBookDefinition;
import net.backstube.cakequests.data.QuestNodeDefinition;
import net.backstube.cakequests.data.QuestTabDefinition;
import net.backstube.cakequests.quest.QuestNodeState;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;

public class QuestGraphScreen extends Screen {
    private static final int TAB_WIDTH = 112;
    private static final int NODE_SIZE = 28;
    private int selectedTab;
    private QuestNodeDefinition selectedNode;
    private double panX = 80;
    private double panY = 80;
    private double zoom = 1.0D;

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
        renderTabs(poseStack, book);
        if (book.tabs().isEmpty()) {
            drawCenteredString(poseStack, font, "No quest graphs loaded", width / 2, height / 2 - 4, 0xFFE5E7EB);
            super.render(poseStack, mouseX, mouseY, partialTick);
            return;
        }
        QuestTabDefinition tab = book.tabs().get(Math.max(0, Math.min(selectedTab, book.tabs().size() - 1)));
        renderGraph(poseStack, tab, mouseX, mouseY);
        renderDetails(poseStack, tab);
        super.render(poseStack, mouseX, mouseY, partialTick);
    }

    private void renderTabs(PoseStack poseStack, QuestBookDefinition book) {
        GuiComponent.fill(poseStack, 0, 0, TAB_WIDTH, height, 0xF0262932);
        font.draw(poseStack, title, 12, 12, 0xFFFFFFFF);
        font.draw(poseStack, ClientQuestGraphStore.fallbackMode() ? "Fallback" : "Server", 12, 25, 0xFF9CA3AF);
        int y = 48;
        for (int i = 0; i < book.tabs().size(); i++) {
            QuestTabDefinition tab = book.tabs().get(i);
            int color = i == selectedTab ? tab.tabColor() : 0xFF343844;
            GuiComponent.fill(poseStack, 8, y, TAB_WIDTH - 8, y + 24, color);
            font.draw(poseStack, tab.title().component(), 16, y + 8, 0xFFFFFFFF);
            y += 28;
        }
    }

    private void renderGraph(PoseStack poseStack, QuestTabDefinition tab, int mouseX, int mouseY) {
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
            renderNode(poseStack, tab, node);
            if (hitNode(mouseX, mouseY, node)) {
                hover = node;
            }
        }
        poseStack.popPose();
        if (hover != null) {
            renderTooltip(poseStack, hover.title().component(), mouseX, mouseY);
        }
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

    private void renderNode(PoseStack poseStack, QuestTabDefinition tab, QuestNodeDefinition node) {
        QuestNodeState state = ClientAdvancementBridge.state(tab, node);
        int color = stateColor(state, node.color());
        int x = node.x() - NODE_SIZE / 2;
        int y = node.y() - NODE_SIZE / 2;
        if (node.shape().name().equals("DIAMOND")) {
            GuiComponent.fill(poseStack, x + 10, y, x + 18, y + 4, color);
            GuiComponent.fill(poseStack, x + 5, y + 4, x + 23, y + 10, color);
            GuiComponent.fill(poseStack, x, y + 10, x + 28, y + 18, color);
            GuiComponent.fill(poseStack, x + 5, y + 18, x + 23, y + 24, color);
            GuiComponent.fill(poseStack, x + 10, y + 24, x + 18, y + 28, color);
        } else {
            GuiComponent.fill(poseStack, x, y, x + NODE_SIZE, y + NODE_SIZE, color);
        }
        GuiComponent.fill(poseStack, x + 3, y + 3, x + NODE_SIZE - 3, y + NODE_SIZE - 3, 0xFF20242D);
        minecraft.getItemRenderer().renderAndDecorateItem(node.icon().stack(), x + 6, y + 6);
        if (state == QuestNodeState.COMPLETE) {
            GuiComponent.fill(poseStack, x + 20, y + 20, x + 27, y + 27, 0xFF62D26F);
        } else if (state == QuestNodeState.LOCKED) {
            GuiComponent.fill(poseStack, x + 20, y + 20, x + 27, y + 27, 0xFF6B7280);
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
        int panelWidth = Math.min(260, width - TAB_WIDTH - 20);
        int left = width - panelWidth - 10;
        int top = 10;
        GuiComponent.fill(poseStack, left, top, width - 10, height - 10, 0xF02A2E38);
        font.draw(poseStack, selectedNode.title().component(), left + 14, top + 14, 0xFFFFFFFF);
        font.draw(poseStack, selectedNode.subtitle().component(), left + 14, top + 29, 0xFFB6BCC8);
        font.draw(poseStack, ClientAdvancementBridge.state(tab, selectedNode).name(), left + 14, top + 48, 0xFF9CA3AF);
        font.draw(poseStack, selectedNode.advancement().toString(), left + 14, top + 62, 0xFF9CA3AF);
        int y = top + 86;
        for (var line : selectedNode.description()) {
            font.draw(poseStack, line.component(), left + 14, y, 0xFFE5E7EB);
            y += 12;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        QuestBookDefinition book = ClientQuestGraphStore.activeBook();
        if (mouseX < TAB_WIDTH) {
            int tab = ((int) mouseY - 48) / 28;
            if (tab >= 0 && tab < book.tabs().size()) {
                selectedTab = tab;
                selectedNode = null;
                return true;
            }
        } else if (!book.tabs().isEmpty()) {
            QuestTabDefinition tab = book.tabs().get(selectedTab);
            for (QuestNodeDefinition node : tab.nodes()) {
                if (hitNode(mouseX, mouseY, node)) {
                    selectedNode = node;
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && mouseX > TAB_WIDTH) {
            panX += dragX;
            panY += dragY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX > TAB_WIDTH) {
            zoom = Math.max(0.5D, Math.min(2.0D, zoom + delta * 0.1D));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private boolean hitNode(double mouseX, double mouseY, QuestNodeDefinition node) {
        double graphX = (mouseX - TAB_WIDTH - panX) / zoom;
        double graphY = (mouseY - panY) / zoom;
        return graphX >= node.x() - NODE_SIZE / 2.0D && graphX <= node.x() + NODE_SIZE / 2.0D
                && graphY >= node.y() - NODE_SIZE / 2.0D && graphY <= node.y() + NODE_SIZE / 2.0D;
    }
}
