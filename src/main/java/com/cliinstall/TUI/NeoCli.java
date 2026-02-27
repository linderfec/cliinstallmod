package com.cliinstall.TUI;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Random;

public class NeoCli {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("neo")
            // 无参数执行
            .executes(context -> {
                sendClientMessage("§e欢迎使用 Neo 使用 /neo -H 查看帮助");
                return 1;
            })
            // 帮助子命令
            .then(Commands.literal("-H")
                .executes(context -> {
                    sendClientMessage(
                        "§6=== Neo 帮助 ===§r\n" +
                                "§e-S <项目ID>§r - 下载模组\n" +
                                "§e-Q <模组名>§r - 搜索模组\n" +
                                "§e-L§r - 列出所有已安装的模组\n" +
                                "§e-U <URL>§r - 通过链接下载模组"
                    );
                    return 1;
                })
            )
            // -S 安装下载模组
            .then(Commands.literal("-S")
                .then(Commands.argument("projectId", StringArgumentType.word())
                    .executes(context -> {
                        String projectId = StringArgumentType.getString(context, "projectId");

                        new Thread(() -> {
                            try {
                                sendClientMessage("§e正在获取模组信息...");
                                String result = downloadModrinthMod(projectId);
                                sendClientMessage(result);
                            } catch (Exception e) {
                                sendClientMessage("§c下载失败: " + e.getMessage());
                            }
                        }).start();

                        return 1;
                    })
                )
            )
            // -Q 搜索模组
            .then(Commands.literal("-Q")
                .then(Commands.argument("modname", StringArgumentType.greedyString())
                    .executes(context -> {
                        String modName = StringArgumentType.getString(context, "modname");

                        new Thread(() -> {
                            try {
                                sendClientMessage("§e正在搜索模组: " + modName);
                                String searchResult = searchModrinthMods(modName);
                                sendClientMessage(searchResult);
                            } catch (Exception e) {
                                sendClientMessage("§c搜索失败: " + e.getMessage());
                            }
                        }).start();

                        return 1;
                    })
                )
            )
            // -L 列出所有已安装的模组
            .then(Commands.literal("-L")
                .executes(context -> {
                    String modsPath = getModsPath();
                    String result = listInstalledMods(modsPath);
                    sendClientMessage(result);
                    return 1;
                })
            )
            // -U 通过链接下载模组
            .then(Commands.literal("-U")
                .then(Commands.argument("url", StringArgumentType.greedyString())
                    .executes(context -> {
                        String fileUrl = StringArgumentType.getString(context, "url");

                        // 获取文件名
                        String fileName = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
                        if (fileName.isEmpty() || !fileName.contains(".")) {
                            Random random = new Random();
                            int sixdirgit = 100000 + random.nextInt(900000);
                            fileName = sixdirgit + ".jar";
                        }

                        // 获取mods文件夹路径
                        String modsPath = getModsPath() + "/" + fileName;
                        String finalFileName = fileName;

                        // 在新线程中下载，避免阻塞主线程
                        new Thread(() -> {
                            try {
                                sendClientMessage("正在下载: " + fileUrl);
                                download(fileUrl, modsPath);
                                sendClientMessage("下载完成: " + finalFileName);
                            } catch (IOException e) {
                                sendClientMessage("§c下载失败: " + e.getMessage());
                            }
                        }).start();

                        return 1;
                    })
                )
            )
        );
    }

    /**
     * 向客户端玩家发送消息
     */
    private static void sendClientMessage(String message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null) {
            mc.execute(() -> {
                LocalPlayer player = mc.player;
                if (player != null) {
                    player.displayClientMessage(Component.literal(message), false);
                }
            });
        }
    }

    /**
     * 获取 mods 文件夹路径（客户端）
     */
    private static String getModsPath() {
        File gameDir = Minecraft.getInstance().gameDirectory;
        return new File(gameDir, "mods").getAbsolutePath();
    }

    public static void download(String fileURL, String savePath) throws IOException {
        URL url = new URL(fileURL);
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        httpConn.setConnectTimeout(10000);
        httpConn.setReadTimeout(60000);
        int responseCode = httpConn.getResponseCode();

        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (InputStream inputStream = httpConn.getInputStream();
                 FileOutputStream outputStream = new FileOutputStream(savePath)) {

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
        } else {
            throw new IOException("HTTP响应码: " + responseCode);
        }
        httpConn.disconnect();
    }

    private static final String MODRINTH_API = "https://api.modrinth.com/v2";

    /**
     * 列出已安装的模组
     */
    public static String listInstalledMods(String modsPath) {
        File modsDir = new File(modsPath);
        if (!modsDir.exists() || !modsDir.isDirectory()) {
            return "§cmods文件夹不存在";
        }

        File[] files = modsDir.listFiles((dir, name) -> name.endsWith(".jar"));
        if (files == null || files.length == 0) {
            return "§e没有已安装的模组";
        }

        StringBuilder result = new StringBuilder();
        result.append("§6=== 已安装模组 ===§r\n");
        result.append("§7共 ").append(files.length).append(" 个模组§r\n\n");

        for (int i = 0; i < files.length; i++) {
            String fileName = files[i].getName();
            long fileSize = files[i].length();
            String sizeStr = formatFileSize(fileSize);
            result.append(String.format("§e[%d]§r %s §7(%s)§r\n", i + 1, fileName, sizeStr));
        }

        return result.toString();
    }

    /**
     * 格式化文件大小
     */
    private static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    /**
     * 从 Modrinth 搜索模组
     */
    public static String searchModrinthMods(String query) throws IOException {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        // 搜索 NeoForge 模组
        String urlStr = MODRINTH_API + "/search?query=" + encodedQuery 
                + "&facets=[[%22project_type:mod%22],[%22categories:neoforge%22]]"
                + "&limit=10";

        String response = httpRequest(urlStr);
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
        JsonArray hits = json.getAsJsonArray("hits");

        if (hits.size() == 0) {
            return "§c未找到匹配的模组";
        }

        StringBuilder result = new StringBuilder();
        result.append("§6=== 搜索结果 ===§r\n");
        
        for (int i = 0; i < hits.size(); i++) {
            JsonObject mod = hits.get(i).getAsJsonObject();
            String projectId = mod.get("project_id").getAsString();
            String title = mod.get("title").getAsString();
            String description = mod.get("description").getAsString();
            int downloads = mod.get("downloads").getAsInt();
            String author = mod.get("author").getAsString();

            result.append(String.format("§e[%d]§r §a%s§r §7(%s)§r\n", i + 1, title, projectId));
            result.append(String.format("    §7作者: %s | 下载量: %,d§r\n", author, downloads));
            result.append(String.format("    §8%s§r\n", truncate(description, 50)));
        }

        result.append("\n§7使用 /neo -S <项目ID> 下载模组§r");
        return result.toString();
    }

    /**
     * 获取当前 Minecraft 版本
     */
    private static String getMcVersion() {
        // 使用 SharedConstants 获取真实的 MC 版本号，格式如 "1.21.8"
        return net.minecraft.SharedConstants.getCurrentVersion().id();
    }

    /**
     * 检查版本是否匹配当前 MC 版本
     * 支持精确匹配和前缀匹配（如 "1.21.8" 匹配 "1.21.x" 或 "1.21"）
     */
    private static boolean matchesMcVersion(String gameVersion, String currentVersion) {
        if (gameVersion.equals(currentVersion)) {
            return true;
        }
        // 支持模糊版本匹配，如 "1.21.x" 匹配 "1.21.8"
        if (gameVersion.endsWith(".x") || gameVersion.endsWith("-")) {
            String prefix = gameVersion.replaceAll("[.x-]+$", ".");
            return currentVersion.startsWith(prefix);
        }
        return false;
    }

    /**
     * 从 Modrinth 下载模组
     */
    public static String downloadModrinthMod(String projectId) throws IOException {
        // 获取当前 MC 版本
        String mcVersion = getMcVersion();
        sendClientMessage("§7当前 MC 版本: " + mcVersion);

        // 获取项目版本列表
        String urlStr = MODRINTH_API + "/project/" + projectId + "/version";
        String response = httpRequest(urlStr);
        JsonArray versions = JsonParser.parseString(response).getAsJsonArray();

        if (versions.size() == 0) {
            return "§c未找到可用版本";
        }

        // 查找适合当前 MC 版本和 NeoForge 的版本
        JsonObject targetVersion = null;
        for (int i = 0; i < versions.size(); i++) {
            JsonObject version = versions.get(i).getAsJsonObject();
            JsonArray loaders = version.getAsJsonArray("loaders");
            JsonArray gameVersions = version.getAsJsonArray("game_versions");
            
            // 检查是否为 NeoForge 加载器
            boolean isNeoForge = false;
            for (int j = 0; j < loaders.size(); j++) {
                if ("neoforge".equals(loaders.get(j).getAsString())) {
                    isNeoForge = true;
                    break;
                }
            }
            
            if (!isNeoForge) continue;
            
            // 检查是否匹配当前 MC 版本
            boolean matchesVersion = false;
            for (int j = 0; j < gameVersions.size(); j++) {
                String gv = gameVersions.get(j).getAsString();
                if (matchesMcVersion(gv, mcVersion)) {
                    matchesVersion = true;
                    break;
                }
            }
            
            if (matchesVersion) {
                targetVersion = version;
                break;
            }
        }

        // 如果没找到匹配版本，提示用户
        if (targetVersion == null) {
            return "§c未找到适合 MC " + mcVersion + " 和 NeoForge 的版本§r\n§7该模组可能不支持当前版本，请检查模组页面§r";
        }

        // 获取下载信息
        JsonArray files = targetVersion.getAsJsonArray("files");
        if (files.size() == 0) {
            return "§c未找到下载文件";
        }

        JsonObject file = files.get(0).getAsJsonObject();
        String downloadUrl = file.get("url").getAsString();
        String fileName = file.get("filename").getAsString();
        String versionNumber = targetVersion.get("version_number").getAsString();

        // 获取 mods 文件夹路径
        String modsPath = getModsPath() + "/" + fileName;

        // 执行下载
        sendClientMessage("§e正在下载: " + fileName + " (版本: " + versionNumber + ")");
        download(downloadUrl, modsPath);

        return "§a下载完成: " + fileName + "§r\n§7请重启游戏以加载模组§r";
    }

    /**
     * 发送 HTTP GET 请求并返回响应内容
     */
    private static String httpRequest(String urlStr) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("User-Agent", "NeoCli/1.0 (Minecraft Mod)");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);

        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IOException("HTTP 错误: " + responseCode);
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        conn.disconnect();
        return response.toString();
    }

    /**
     * 截断字符串
     */
    private static String truncate(String str, int maxLength) {
        if (str == null) return "";
        if (str.length() <= maxLength) return str;
        return str.substring(0, maxLength) + "...";
    }
}
