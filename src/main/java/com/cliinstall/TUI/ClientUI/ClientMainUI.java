package com.cliinstall.TUI.ClientUI;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ClientMainUI extends Screen {
    private EditBox searchBox; // 类字段，用于在其他方法中访问

    public ClientMainUI() {
        super(Component.literal("CLI Install Mod"));
    }

    public ClientMainUI(Component title) {
        super(title);
    }

    @Override
    protected void init() {
        // 添加搜索输入框
        searchBox = new EditBox(
                this.font,
                this.width / 2 - 460, this.height / 2 - 250,
                120, 20,
                Component.literal("")
        );
        searchBox.setMaxLength(Integer.MAX_VALUE); // 无字符上限
        searchBox.setHint(Component.literal("更线")); // 占位符提示文字
        this.addRenderableWidget(searchBox);

        // 添加搜索按钮
        this.addRenderableWidget(Button.builder(
                        Component.literal("搜索"),
                        (button) -> {
                            String searchText = searchBox.getValue(); // 获取输入内容
                            if (!searchText.isEmpty()) {
                                // TODO: 执行搜索逻辑
                                System.out.println("搜索内容: " + searchText);
                            }
                        })
                .bounds(this.width / 2 - 340, this.height / 2 - 250, 30, 20)
                .build());
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 只绘制简单的半透明背景，不调用父类方法
        guiGraphics.fillGradient(0, 0, this.width, this.height, -1072689136, -804253680);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 绘制背景
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        
        // 在屏幕左上角绘制一段文字
        guiGraphics.drawString(this.font, "模组下载", 10, 10, 0xFFFFFF);

        // 绘制分割线（白色，高度1像素）
        int lineY = 30; // 分割线Y坐标
        guiGraphics.fill(0, lineY, this.width, lineY + 1, 0xFFFFFFFF); // 白色水平线

        // 渲染所有已添加的组件（按钮、输入框等）
        for (var widget : this.renderables) {
            widget.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }
}
