package com.cliinstall.TUI;

import com.cliinstall.cliinstallmod;
import net.minecraft.network.chat.Component;

import java.net.InetSocketAddress;
import java.net.Socket;

public class PingWeb {
    /**
     * 检测网络连接并显示结果到聊天框和日志
     */
    public void webcuo() {
        try (Socket socket = new Socket()) {
            // 使用 TCP 连接端口 80，不需要 root 权限
            socket.connect(new InetSocketAddress("www.baidu.com", 80), 2000);
            cliinstallmod.LOGGER.info("网络可以使用");
            Component.literal("网络可以使用");

        } catch (Exception e) {
            cliinstallmod.LOGGER.info("网络无法使用: {}", e.getMessage());
            Component.literal("网络不可用");
        }
    }

}
