package com.converter.controller;

import com.converter.config.CustomizeConfig;
import com.converter.core.ConvertManager;
import com.converter.pojo.ConvertInfo;
import com.converter.service.MainService;
import com.converter.utils.FileUtils;
import com.converter.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    /**
     * service对象
     */
    private final MainService service;

    @Autowired
    public MainController(@Qualifier("mainService") final MainService service) {
        this.service = service;
    }

    /**
     * 跳转到登录页
     */
    @GetMapping("login")
    public String login() {
        return "admin/login";
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
     * 跳转到线程信息页
     */
    @GetMapping("/thread")
    public String thread() {
        return "admin/thread";
    }

    /**
     * 跳转到日志页
     */
    @GetMapping("/log")
    public String log() {
        return "admin/log";
    }

    /**
     * 跳转到设置页
     */
    @GetMapping("/setting")
    public String setting(final HttpServletRequest request) {
        try {
            request.setAttribute("setting", service.getConfig());
        } catch (Exception e) {
            log.error("获取配置失败", e);
        }
        return "admin/setting";
    }

    /**
     * 获取任务列表信息, 返回json格式供前端使用
     */
    @GetMapping("/getInfo")
    @ResponseBody
    public String getInfo(final @RequestParam boolean cache) {
        String info = null;
        try {
            info = service.getAllConvertInfoOfJson(cache);
        } catch (Exception e) {
            log.error("获取信息失败", e);
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
            log.error("获取监控列表失败", e);
        }
        return watchedFiles;
    }

    /**
     * 获取线程池信息
     */
    @GetMapping("/getThreadsInfo")
    @ResponseBody
    public String getThreadsInfo() {
        String threadsInfo = null;
        try {
            threadsInfo = service.getThreadsInfo();
        } catch (Exception e) {
            log.error("获取线程池信息失败", e);
        }
        return threadsInfo;
    }

    /**
     * 登录
     */
    @PostMapping("/login")
    public String login(final HttpServletRequest request) {
        String[] username = request.getParameterMap().get("username");
        String[] password = request.getParameterMap().get("password");
        String[] remember = request.getParameterMap().get("rememberMe");
        if (username == null || password == null) {
            request.setAttribute("msg", "用户名和密码不能为空");
            return "admin/login";
        }

        UsernamePasswordToken token = new UsernamePasswordToken(username[0], password[0]);
        token.setRememberMe(remember != null);
        Subject subject = SecurityUtils.getSubject();
        try {
            subject.login(token);
            log.debug("登录成功, 用户名: {}", username[0]);
            return "redirect:index";
        } catch (AuthenticationException e) {
            log.error("登录错误: {}", e.getMessage());
            token.clear();
            request.setAttribute("username", username[0]);
            request.setAttribute("msg", "用户名或密码错误");
            return "admin/login";
        }
    }

    /**
     * 接收前端参数, 用于添加任务
     */
    @PostMapping("/addMissions")
    @ResponseBody
    public String addMissions(final @RequestParam("file") MultipartFile file,
                              final HttpServletRequest request) {
        try {
            Map<String, String[]> map = request.getParameterMap();
            String sourcePath = map.get("sourcePath")[0].trim();
            String targetPath = map.get("targetPath")[0].trim();
            String type = map.get("type")[0].trim();
            boolean useDefaultTargetDir = "".equals(targetPath);
            // 如果上传文件非空, 则使用上传文件
            if (!file.isEmpty()) {
                String fileName = System.currentTimeMillis() + ConvertManager.UPLOAD + file.getOriginalFilename();
                String filePath = FileUtils.dealWithDir(CustomizeConfig.instance().getUploadPath()) + fileName;
                file.transferTo(new File(filePath));
                if (useDefaultTargetDir) {
                    service.addMission(filePath);
                } else {
                    service.addMission(filePath, targetPath);
                }
                return "success";
            }
            // 添加文件
            if (TYPE_FILE.equals(type)) {
                if (useDefaultTargetDir) {
                    service.addMission(sourcePath);
                } else {
                    service.addMission(sourcePath, targetPath);
                }
            }
            // 添加文件夹
            else if (TYPE_DIR.equals(type)) {
                if (useDefaultTargetDir) {
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
    public String setSettings(final HttpServletRequest request) {
        try {
            Map<String, String> map = new HashMap<>(16);
            Set<Map.Entry<String, String[]>> entries = request.getParameterMap().entrySet();
            for (Map.Entry<String, String[]> entry : entries) {
                map.put(entry.getKey(), entry.getValue()[0]);
            }
            service.setConfig(map);
            log.info("成功修改配置: {}", map.toString());
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
    public String cancelById(final HttpServletRequest request) {
        try {
            String id = request.getParameterMap().get("id")[0];
            service.cancelMissionById(Integer.valueOf(id));
            log.debug("成功取消任务{}", id);
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
    public void readPdf(final @RequestParam String filename,
                        final @RequestParam String source,
                        final HttpServletResponse response) {
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
        // 如果验证失败, 则跳转至404页面
        try {
            response.sendRedirect("error/404");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    /**
     * 验证预览请求是否合理
     *
     * @param source 源文件路径
     * @param target 目的文件路径
     * @return true代表合理
     */
    private boolean verify(final String source,
                           final String target) {
        List<ConvertInfo> finishedInfo = ConvertManager.getFinishedInfo();
        for (ConvertInfo convertInfo : finishedInfo) {
            if (convertInfo.getSourceFilePath().equals(source)
                    && convertInfo.getTargetFilePath().equals(target)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 删除监控列表中文件或文件夹
     */
    @RequestMapping("/delWatchFiles")
    @ResponseBody
    public String delWatchFiles(final HttpServletRequest request) {
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
