package com.converter.controller;

import com.converter.config.CustomizeConfig;
import com.converter.pojo.ConvertInfo;
import com.converter.service.MainService;
import com.converter.utils.RedisUtils;
import com.converter.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 主Controller类
 *
 * @author Evan
 */
@Slf4j
@Controller
public class MainController {
    /**
     * 表明添加的是单个文件
     */
    private static final String TYPE_FILE = "file";
    /**
     * 表明添加的是文件夹
     */
    private static final String TYPE_DIR = "dir";
    private final MainService service;

    @Autowired
    public MainController(MainService service) {
        this.service = service;
    }

    /**
     * 跳转到首页
     */
    @GetMapping(value = {"/", "/index"})
    public String index() {
        return "admin/index";
    }

    /**
     * 跳转到监控添加页
     */
    @GetMapping("/watch")
    public String watch() {
        return "admin/watch";
    }

    /**
     * 跳转到设置页
     */
    @GetMapping("/setting")
    public String setting(HttpServletRequest request) {
        try {
            request.setAttribute("setting", service.getConfig());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "admin/setting";
    }

    /**
     * 获取任务列表信息, 返回json格式供前端使用
     */
    @GetMapping("/getInfo")
    @ResponseBody
    public String getInfo() {
        String info = null;
        try {
            info = service.getAllConvertInfoOfJson();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return info;
    }

    /**
     * 获取redis缓存中监控的文件/文件夹
     */
    @GetMapping("/getWatchedFiles")
    @ResponseBody
    public String getWatchedFiles() {
        String watchedFiles = null;
        try {
            watchedFiles = service.getWatchedFiles();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return watchedFiles;
    }

    /**
     * 接收前端参数, 用于添加任务
     */
    @PostMapping("/addMissions")
    @ResponseBody
    public String addMissions(HttpServletRequest request) {
        try {
            Map<String, String[]> map = request.getParameterMap();
            String sourcePath = map.get("sourcePath")[0].trim();
            String targetPath = map.get("targetPath")[0].trim();
            String type = map.get("type")[0].trim();
            if (TYPE_FILE.equals(type)) {
                if ("".equals(targetPath)) {
                    service.addMission(sourcePath);
                } else {
                    service.addMission(sourcePath, targetPath);
                }
            } else if (TYPE_DIR.equals(type)) {
                if ("".equals(targetPath)) {
                    service.addMissions(sourcePath);
                } else {
                    service.addMissions(sourcePath, targetPath);
                }
            } else {
                return "error file type, it should be file or dir";
            }
            return "success";
        } catch (Exception e) {
            log.error("MainController添加任务错误: {}", e.getMessage());
            return "error: " + e.getMessage();
        }
    }

    /**
     * 接收前端参数, 用于修改设置(运行时有效, 下次运行以配置文件为准)
     */
    @PostMapping("/setting")
    @ResponseBody
    public String setSettings(HttpServletRequest request) {
        try {
            String setting = request.getParameterMap().values().stream().map(s -> s[0]).collect(Collectors.joining(","));
            service.setConfig(setting);
            log.info("成功修改配置: {}", setting);
            return "success";
        } catch (Exception e) {
            log.error("MainController修改设置错误: {}", e.getMessage());
            return "error: " + e.getMessage();
        }
    }

    /**
     * 根据id取消任务
     */
    @PostMapping("/cancelById")
    @ResponseBody
    public String cancelById(HttpServletRequest request) {
        try {
            String id = request.getParameterMap().get("id")[0];
            service.cancelMissionById(Integer.valueOf(id));
            log.info("成功取消任务{}", id);
            return "success";
        } catch (Exception e) {
            log.error("MainController取消任务错误: {}", e.getMessage());
            return "error: " + e.getMessage();
        }
    }

    /**
     * 用于预览PDF文件
     *
     * @param filename 文件名
     */
    @RequestMapping("/preview")
    public void readPdf(@RequestParam String filename, @RequestParam String source, HttpServletResponse response) {
        try {
            if (verify(source, filename)) {
                log.debug("预览文件{}", filename);
                response.reset();
                response.setContentType("application/pdf");
                OutputStream output = response.getOutputStream();
                output.write(Files.readAllBytes(new File(filename).toPath()));
                output.flush();
                output.close();
                return;
            }
        } catch (Exception e) {
            log.error("MainController预览文件[{}]错误: {}", filename, e.toString());
        }
        try {
            response.sendRedirect("error/404");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 验证预览请求是否合理
     *
     * @param source 源文件路径
     * @param target 目的文件路径
     * @return true代表合理
     */
    private boolean verify(String source, String target) {
        Set<Object> objects = RedisUtils.sGet(CustomizeConfig.instance().getRedisInfoKey());
        if (objects != null) {
            for (Object object : objects) {
                ConvertInfo convertInfo = StringUtils.parseJsonString((String) object, ConvertInfo.class);
                if (convertInfo == null) {
                    continue;
                }
                if (convertInfo.getSourceFilePath().equals(source) && convertInfo.getTargetFilePath().equals(target)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 删除监控列表中文件或文件夹
     */
    @RequestMapping("/delWatchFiles")
    @ResponseBody
    public String delWatchFiles(HttpServletRequest request) {
        try {
            String path = request.getParameterMap().get("path")[0];
            String type = request.getParameterMap().get("type")[0];
            String key;
            log.debug("删除监控文件: {}", path);
            if (TYPE_FILE.equals(type)) {
                key = CustomizeConfig.instance().getRedisFileKey();
            } else if (TYPE_DIR.equals(type)) {
                key = CustomizeConfig.instance().getRedisDirKey();
            } else {
                return "error file type, it should be file or dir";
            }
            RedisUtils.setRemove(key, path);
            return "success";
        } catch (Exception e) {
            log.error("MainController删除监控文件错误: {}", e.getMessage());
            return "error: " + e.getMessage();
        }
    }
}
