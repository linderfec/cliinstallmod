package com.cliinstall;

import com.cliinstall.TUI.ClientUI.ClientMainUI;
import com.cliinstall.TUI.PingWeb;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import net.neoforged.neoforge.common.util.Lazy;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.glfw.GLFW;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = cliinstallmod.MODID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = cliinstallmod.MODID, value = Dist.CLIENT)
public class cliinstallmodClient {

    // 定义按键绑定：Alt+M
    public static final Lazy<KeyMapping> OPEN_UI_KEY = Lazy.of(() ->
            new KeyMapping(
                    "key.cliinstallmod.open_ui", // 翻译键
                    KeyConflictContext.IN_GAME, // 只在游戏中生效
                    KeyModifier.ALT, // 需要 Alt 修饰键
                    InputConstants.Type.KEYSYM, // 按键类型
                    GLFW.GLFW_KEY_M, // 默认按键 M
                    "key.categories.cliinstallmod" // 按键分类
            ));

    public cliinstallmodClient(ModContainer container) {
        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        cliinstallmod.LOGGER.info("HELLO FROM CLIENT SETUP");
        cliinstallmod.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());

        // 执行网络检测
        PingWeb pingWeb = new PingWeb();
        pingWeb.webcuo();
    }

    @SubscribeEvent
    static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_UI_KEY.get());
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        while (OPEN_UI_KEY.get().consumeClick()) {
            Minecraft.getInstance().setScreen(new ClientMainUI());
        }
    }
}
