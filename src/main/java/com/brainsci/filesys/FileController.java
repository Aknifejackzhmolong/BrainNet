package com.brainsci.filesys;

import com.brainsci.filesys.repository.FileRepository;
import com.brainsci.form.CommonResultForm;
import com.brainsci.security.repository.UserBaseRepository;
import com.brainsci.utils.FileHandleUtils;
import com.brainsci.utils.ZipUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@RestController
public class FileController {
    @Autowired
    private Environment env;
    @Autowired
    private FileRepository fileRepository;
    @Autowired
    private UserBaseRepository userBaseRepository;
//    @Value("${filesys.public-dir}")
//    private String publicDir;
    private FileHandleUtils fileHandleUtils;

    @GetMapping({"/getfilelist/**","/getdirlist/**"})
    public CommonResultForm getFileList(HttpServletRequest request, HttpSession httpSession) throws UnsupportedEncodingException{
        String fileDir = env.getProperty("filesys.dir");
        String flag = request.getRequestURI().substring(4,7);
        final String uri = flag.equals("dir")?URLDecoder.decode(request.getRequestURI().substring(12), "UTF-8"):URLDecoder.decode(request.getRequestURI().substring(13), "UTF-8");
        String userHomeDir = userBaseRepository.getOne((String) httpSession.getAttribute("username")).getHomeDirectory().substring(2);
        File[] fileArr = new File(fileDir + userHomeDir + "/" + uri).listFiles(flag.equals("dir")?
                new FileFilter() {
                    @Override
                    public boolean accept(File f){
                        return f.isDirectory();
                    }
                }:new FileFilter() {
                    @Override
                    public boolean accept(File f){
                        return f.isFile();
                    }
                });
        List<Object> usrlicList = new ArrayList<Object>();
        if (fileArr != null){
            for (File f : fileArr){
                usrlicList.add(new HashMap<String, Object>(){{
                    this.put("name", f.getName());
                    String path = f.getPath();
                    path = f.getPath().substring(path.indexOf(uri)).replace("\\","/");
                    this.put("uri", "/MyFile/"+path);
                }});
            }
        }
        return CommonResultForm.of200("success", usrlicList );
    }
    @DeleteMapping(value = "/MyFile/**")
    public CommonResultForm singleFileDelete(HttpServletRequest request, HttpSession httpSession) throws IOException{
        String uri = request.getRequestURI().substring(8);
        uri = URLDecoder.decode(uri, "UTF-8");
        String userHomeDir = userBaseRepository.getOne((String) httpSession.getAttribute("username")).getHomeDirectory();
        File tag = new File(env.getProperty("filesys.dir") + userHomeDir + "/" + uri);
        if (tag.exists()) {
            if (tag.isDirectory()) {
                FileHandleUtils.deleteFold(tag);
                if (!tag.exists()) return CommonResultForm.of204("delete success");
            } else if (tag.isFile() && tag.delete()) return CommonResultForm.of204("delete success");
        }
        return CommonResultForm.of400("delete fail");
    }
    @GetMapping(value = "/MyFile/**")
    public void singleFileGet(HttpServletRequest request, HttpServletResponse response, HttpSession httpSession) throws IOException{
        String uri = request.getRequestURI().substring(8);
        uri = URLDecoder.decode(uri, "UTF-8");
        String username = (String) httpSession.getAttribute("username");
        String fileDir = env.getProperty("filesys.dir");
        String userHomeDir = userBaseRepository.getOne(username).getHomeDirectory();
        File file = new File(fileDir + (userHomeDir + "/" + uri).replaceAll("\\./",""));
        if (!file.exists()) {
            response.sendError(402);
            return;
        }
//        return "forward:/"+(userHomeDir + "/" + uri).replaceAll("\\./","");
        String fileName = uri.substring(uri.lastIndexOf('/')+1);
        if (file.isDirectory()) fileName = fileName + ".zip";
        response.setContentType("application/octet-stream");
        response.addHeader("Content-Disposition", "attachment;fileName=" + fileName);
        FileInputStream fis = null;
        ServletOutputStream sos = null;
        try {
            sos = response.getOutputStream();
            if (file.isDirectory()){
                ZipUtils.toZip(file.getAbsolutePath(),sos, true);
            }else {
                fis = new FileInputStream(file);
                response.addHeader("Content-Length", String.valueOf(file.length()));
                System.out.println(file.getAbsolutePath()+" size:"+String.valueOf(file.length()));
                byte[] buffer = new byte[1048576];// 一次读取1M
                int i = fis.read(buffer);
                while (i != -1) {
                    sos.write(buffer, 0, i);
                    i = fis.read(buffer);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (sos != null) {
                try {
                    sos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    @PostMapping(value = "/deleteFile")
    public CommonResultForm multiFileDelete(@RequestBody ArrayList<String> deletefiles,HttpServletRequest request, HttpSession httpSession) throws IOException{
        String userHomeDir = userBaseRepository.getOne((String) httpSession.getAttribute("username")).getHomeDirectory();
        if (deletefiles == null) return CommonResultForm.of400("delete fail");
        List<String> res = new ArrayList<>();
        for(String uri : deletefiles){
            File tag = new File(env.getProperty("filesys.dir") + userHomeDir + uri);
            if (tag.delete()) res.add(uri+".log -delete success");
            else res.add(uri+".log -delete fail");
        }
        return CommonResultForm.of200("delete fail", res);
    }
    @PostMapping(value = "/MyFile")
    public CommonResultForm postForDownload(@RequestBody ArrayList<String> files,HttpServletResponse response, HttpSession httpSession) throws IOException{
        String userHomeDir = userBaseRepository.getOne((String) httpSession.getAttribute("username")).getHomeDirectory();
        String fileDir = env.getProperty("filesys.dir");
        List<File> tag = new ArrayList<>();
        for(String uri : files){
            tag.add(new File(fileDir + (userHomeDir + uri).replaceAll("\\./","")));
        }
        httpSession.setAttribute("downloadOption",tag);
        httpSession.setAttribute("downloadTagPath",files);
        return CommonResultForm.of204("success");
    }
    @GetMapping(value = "/downloadzip")
    public void compressDownload(HttpServletResponse response, HttpSession httpSession) throws IOException{
        List<File> tag = (List<File>)httpSession.getAttribute("downloadOption");
        List<String> paths = (List<String>)httpSession.getAttribute("downloadTagPath");
        if (tag == null || paths == null) return;
        httpSession.removeAttribute("downloadOption");
        httpSession.removeAttribute("downloadTagPath");
        response.setContentType("application/octet-stream");
        response.addHeader("Content-Disposition", "attachment;fileName=working.zip");
        try {
            ZipUtils.toZip(tag, paths, response.getOutputStream(), true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @PostMapping(value = "/find")
    public CommonResultForm multiFileFind(@RequestBody ArrayList<String> findfiles,HttpServletRequest request, HttpSession httpSession) throws IOException{
        String userHomeDir = userBaseRepository.getOne((String) httpSession.getAttribute("username")).getHomeDirectory();
        if (findfiles == null) return CommonResultForm.of400("find fail");
        List<String> res = new ArrayList<>();
        for(String uri : findfiles){
            File tag = new File(env.getProperty("filesys.dir") + userHomeDir + uri);
            if (tag.exists()) res.add(uri+".log -find success");
            else res.add(uri+".log -find fail");
        }
        return CommonResultForm.of200("find fail", res);
    }
    @PostMapping("/uploadsinglefile")
    public CommonResultForm singleFileUpload(MultipartFile file, HttpServletRequest request, HttpSession httpSession) throws IOException {
//        String ip = RemoteAddrUtils.getRemoteAddrUtils().getRemoteIP(request);
        String userHomeDir = userBaseRepository.getOne((String) httpSession.getAttribute("username")).getHomeDirectory();
        String uploadFolder = env.getProperty("filesys.dir") + userHomeDir + "/matrix/";
        try {
            byte[] bytes = file.getBytes();
//            String content = new String(bytes, "GBK");
            Path path = Paths.get(uploadFolder + file.getOriginalFilename());
            //如果没有files文件夹，则创建
            if (!Files.isWritable(path)) {
                Files.createDirectories(Paths.get(uploadFolder));
            }
            //文件写入指定路径
            Files.write(path, bytes);
            return CommonResultForm.of200("success", new HashMap<String, String>(){{
                this.put("uri", "/MyFile/" + file.getOriginalFilename());
            }});
        } catch (IOException e) {
            return CommonResultForm.of400(e.getMessage());
        }
    }
    @PostMapping("/uploadfiles/{part}")
    public CommonResultForm multiFileUpload(@PathVariable(value="part")String part, @RequestParam("uploadfile")MultipartFile file, HttpServletRequest request, HttpSession httpSession) throws IOException {
//        String ip = RemoteAddrUtils.getRemoteAddrUtils().getRemoteIP(request);
        String userHomeDir = userBaseRepository.getOne((String) httpSession.getAttribute("username")).getHomeDirectory();
        String uploadFolder = env.getProperty("filesys.dir") + userHomeDir + "/"+part+"/";
        try {
            byte[] bytes = file.getBytes();
//            String content = new String(bytes, "GBK");
            if(file.getOriginalFilename() == null) return CommonResultForm.of400("file name was null");
            System.out.println("upload file: "+file.getName()+", Original File Name: "+file.getOriginalFilename());
            String filepath = uploadFolder + URLDecoder.decode(file.getOriginalFilename(), "UTF-8");
            Path path = Paths.get(filepath);
            //如果没有files文件夹，则创建
            if (!Files.isWritable(path)) {
                Files.createDirectories(Paths.get(filepath.substring(0,filepath.lastIndexOf('/'))));
            }
            //文件写入指定路径
            Files.write(path, bytes);
            return CommonResultForm.of200("success", new HashMap<String, String>(){{
                this.put("uri", "/MyFile/" + file.getOriginalFilename());
            }});
        } catch (IOException e) {
            return CommonResultForm.of400(e.getMessage());
        }
    }

}