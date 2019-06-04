package com.brainsci.service;

import com.brainsci.utils.MailUtils;
import com.brainsci.security.util.GsonPlus;
import com.brainsci.utils.FileHandleUtils;
import com.brainsci.utils.ZipUtils;
import com.brainsci.websocket.form.WebSocketMessageForm;
import com.brainsci.websocket.server.WebSocketServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

@Service
public class PythonService {
    private final String filedir;
    private final MailUtils mailUtils;
    private HashMap<String, String> pyScripyPath = new HashMap<String, String>(){{
        this.put("fmri", "/home/cdj/brainnet/python_pipeline/fmri_pipeline_parameter.py");
        this.put("smri", "/home/cdj/brainnet/python_pipeline/T1_surface/freesurfer_recon.py");
        this.put("smriParallel", "/home/cdj/brainnet/python_pipeline/T1_surface/freesurfer_recon_parallel.py");
        this.put("dti", "/home/cdj/brainnet/python_pipeline/dwi_dti/dwi_pipeline_parameter.py");
    }};

    @Autowired
    public PythonService(@Value("${filesys.dir}") String filedir, MailUtils mailUtils) {
        this.filedir = filedir;
        this.mailUtils = mailUtils;
    }

    private String resetParaJsonPath(String paraJson){
        File fmripub = new File(filedir+"./public/fmri/");
        File dtipub = new File(filedir+"./public/DTI/");
        return paraJson
                .replaceAll("/public/fmri",fmripub.getAbsolutePath())// getAbsolutePath方法获得的路径中最右边没有'/'
                .replaceAll("/public/DTI",dtipub.getAbsolutePath())
                .replaceAll("/./","/");
    }

    @Async
    public void runPyScript(HashMap<String,String>map, String script, String task, String token, String paraJson){
        String userHomeDir = map.get("userHomeDir");
        File path = new File(String.format(filedir + userHomeDir + "/%s/%s",script,task));
        File outputpath = new File(String.format(filedir + userHomeDir + "/%s/%s/working",script,task));
//        File zipTag = new File(outputpath.getAbsolutePath() + ".zip");
        if (outputpath.exists())FileHandleUtils.deleteFold(outputpath);// 判断文件夹是否存在，如果存在就删除
        if(!path.exists()) path.mkdirs();
//        zipTag.delete();// 删除原有压缩文件
        paraJson = resetParaJsonPath(paraJson);
        System.out.println(paraJson);
        String[] cmd = {"python", pyScripyPath.get(script),path.getAbsolutePath(), paraJson};
        WebSocketServer.sendMessage(GsonPlus.GSON.toJson(new WebSocketMessageForm(script+"State", "running")),token);
        try{
            Process p = Runtime.getRuntime().exec(cmd);
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = br.readLine();
            while(p.isAlive())
                if (line != null){
                    WebSocketServer.sendMessage(GsonPlus.GSON.toJson(new WebSocketMessageForm(script, line)),token);
                    System.out.println(line);
                    line = br.readLine();
                }
            BufferedReader err = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            line = err.readLine();
            while(line != null){
                WebSocketServer.sendMessage(GsonPlus.GSON.toJson(new WebSocketMessageForm(script, line)),token);
                System.out.println(line);
                line = err.readLine();
            }
            System.out.println("exit("+p.exitValue()+")");
            if (p.exitValue()!=0){
                WebSocketServer.sendMessage(GsonPlus.GSON.toJson(new WebSocketMessageForm(script, "error")),token);
                WebSocketServer.sendMessage(GsonPlus.GSON.toJson(new WebSocketMessageForm(script+"State", "error")),token);
                System.out.println("error");
            } else /*if (outputpath.exists())*/ {
//                WebSocketServer.sendMessage(GsonPlus.GSON.toJson(new WebSocketMessageForm(script, " compress...")),token);
//                System.out.println("compress...");
//                FileOutputStream fos = new FileOutputStream(zipTag);
//                ZipUtils.toZip(outputpath.getAbsolutePath(), fos, true);
                WebSocketServer.sendMessage(GsonPlus.GSON.toJson(new WebSocketMessageForm(script, "finish")),token);
                WebSocketServer.sendMessage(GsonPlus.GSON.toJson(new WebSocketMessageForm(script+"State", "finish")),token);
                mailUtils.sendCompleteMail(map.get("email"),map.get("username"),script,task,true);
                System.out.println("finish");
            }/*else {
                System.out.println("compress fail");
                WebSocketServer.sendMessage(GsonPlus.GSON.toJson(new WebSocketMessageForm(script, "compress fail")),token);
                WebSocketServer.sendMessage(GsonPlus.GSON.toJson(new WebSocketMessageForm(script+"State", "error")),token);
            }*/
            br.close();
            err.close();
            p.destroy();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
//    @Async
//    public void cpac(HashMap<String,String> map, String task, String token, String paraJson){
//        String userHomeDir = map.get("userHomeDir");
//        File path = new File(filedir + userHomeDir + "/fmri/"+task);
//        File working = new File(filedir + userHomeDir + "/fmri/"+task+"/working");
//        File zipTag = new File(working.getAbsolutePath() + ".zip");
//        if (working.exists())FileHandleUtils.deleteFold(working);// 判断文件夹是否存在，如果存在就删除
//        zipTag.delete();// 删除原有压缩文件
//        paraJson = resetParaJsonPath(paraJson);
//        System.out.println(paraJson);
//        String[] cmd = {"python",pyScripyPath.get("fmri"),path.getAbsolutePath(), paraJson};
//        WebSocketServer.sendMessage(GsonPlus.GSON.toJson(new WebSocketMessageForm("fmriState", "running")),token);
//        try{
//            Process p = Runtime.getRuntime().exec(cmd);
//            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
//            String line = br.readLine();
//            while(p.isAlive())
//                if (line != null){
//                    WebSocketServer.sendMessage(GsonPlus.GSON.toJson(new WebSocketMessageForm("fmri", line)),token);
//                    System.out.println(line);
//                    line = br.readLine();
//                }
//            BufferedReader err = new BufferedReader(new InputStreamReader(p.getErrorStream()));
//            line = err.readLine();
//            while(line != null){
//                WebSocketServer.sendMessage(GsonPlus.GSON.toJson(new WebSocketMessageForm("fmri", line)),token);
//                System.out.println(line);
//                line = err.readLine();
//            }
//            System.out.println("exit("+p.exitValue()+")");
//            if (p.exitValue()!=0){
//                WebSocketServer.sendMessage(GsonPlus.GSON.toJson(new WebSocketMessageForm("fmri", "error")),token);
//                WebSocketServer.sendMessage(GsonPlus.GSON.toJson(new WebSocketMessageForm("fmriState", "error")),token);
//                System.out.println("error");
//            } else if (working.exists()) {
//                WebSocketServer.sendMessage(GsonPlus.GSON.toJson(new WebSocketMessageForm("fmri", " compress...")),token);
//                System.out.println("compress...");
//                FileOutputStream fos = new FileOutputStream(zipTag);
//                ZipUtils.toZip(working.getAbsolutePath(), fos, true);
//                WebSocketServer.sendMessage(GsonPlus.GSON.toJson(new WebSocketMessageForm("fmri", "finish")),token);
//                WebSocketServer.sendMessage(GsonPlus.GSON.toJson(new WebSocketMessageForm("fmriState", "finish")),token);
//                mailUtils.sendCompleteMail(map.get("email"),map.get("username"),"fMRI",task,true);
//                System.out.println("finish");
//            }else {
//                System.out.println("compress fail");
//                WebSocketServer.sendMessage(GsonPlus.GSON.toJson(new WebSocketMessageForm("fmri", "compress fail")),token);
//                WebSocketServer.sendMessage(GsonPlus.GSON.toJson(new WebSocketMessageForm("fmriState", "error")),token);
//            }
//            br.close();
//            err.close();
//            p.destroy();
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//    }
//    @Async
//    public void sMRI(HashMap<String,String>map, String task, String token, String paraJson){
//        String userHomeDir = map.get("userHomeDir");
//        File path = new File(filedir + userHomeDir + "/smri/"+task);
//        File working = new File(filedir + userHomeDir + "/smri/"+task+"/working");
//        File zipTag = new File(working.getAbsolutePath() + ".zip");
//        if (working.exists())FileHandleUtils.deleteFold(working);// 判断文件夹是否存在，如果存在就删除
//        zipTag.delete();// 删除原有压缩文件
//        paraJson = resetParaJsonPath(paraJson);
//        System.out.println(paraJson);
//        String[] cmd = {"python",pyScripyPath.get("smri"),path.getAbsolutePath(), paraJson};
//        WebSocketServer.sendMessage(GsonPlus.GSON.toJson(new WebSocketMessageForm("smriState", "running")),token);
//        try{
//            Process p = Runtime.getRuntime().exec(cmd);
//            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
//            String line = br.readLine();
//            while(p.isAlive())
//                if (line != null){
//                    WebSocketServer.sendMessage(GsonPlus.GSON.toJson(new WebSocketMessageForm("smri", line)),token);
//                    System.out.println(line);
//                    line = br.readLine();
//                }
//            BufferedReader err = new BufferedReader(new InputStreamReader(p.getErrorStream()));
//            line = err.readLine();
//            while(line != null){
//                WebSocketServer.sendMessage(GsonPlus.GSON.toJson(new WebSocketMessageForm("smri", line)),token);
//                System.out.println(line);
//                line = err.readLine();
//            }
//            System.out.println("exit("+p.exitValue()+")");
//            if (p.exitValue()!=0){
//                WebSocketServer.sendMessage(GsonPlus.GSON.toJson(new WebSocketMessageForm("smri", "error")),token);
//                WebSocketServer.sendMessage(GsonPlus.GSON.toJson(new WebSocketMessageForm("smriState", "error")),token);
//                System.out.println("error");
//            } else if (working.exists()) {
//                WebSocketServer.sendMessage(GsonPlus.GSON.toJson(new WebSocketMessageForm("smri", " compress...")),token);
//                System.out.println("compress...");
//                FileOutputStream fos = new FileOutputStream(zipTag);
//                ZipUtils.toZip(working.getAbsolutePath(), fos, true);
//                WebSocketServer.sendMessage(GsonPlus.GSON.toJson(new WebSocketMessageForm("smri", "finish")),token);
//                WebSocketServer.sendMessage(GsonPlus.GSON.toJson(new WebSocketMessageForm("smriState", "finish")),token);
//                mailUtils.sendCompleteMail(map.get("email"),map.get("username"),"sMRI",task,true);
//                System.out.println("finish");
//            }else {
//                System.out.println("compress fail");
//                WebSocketServer.sendMessage(GsonPlus.GSON.toJson(new WebSocketMessageForm("smri", "compress fail")),token);
//                WebSocketServer.sendMessage(GsonPlus.GSON.toJson(new WebSocketMessageForm("smriState", "error")),token);
//            }
//            br.close();
//            err.close();
//            p.destroy();
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//    }
//    @Async
//    public void smriParallel(HashMap<String,String>map, String task, String token, String paraJson){
//        String userHomeDir = map.get("userHomeDir");
//        File path = new File(filedir + userHomeDir + "/smri/"+task);
//        File working = new File(filedir + userHomeDir + "/smri/"+task+"/working");
//        File zipTag = new File(working.getAbsolutePath() + ".zip");
//        if (working.exists())FileHandleUtils.deleteFold(working);// 判断文件夹是否存在，如果存在就删除
//        zipTag.delete();// 删除原有压缩文件
//        paraJson = resetParaJsonPath(paraJson);
//        System.out.println(paraJson);
//        String[] cmd = {"python",pyScripyPath.get("smriParallel"),path.getAbsolutePath(), paraJson};
//        WebSocketServer.sendMessage(GsonPlus.GSON.toJson(new WebSocketMessageForm("smriState", "running")),token);
//        try{
//            Process p = Runtime.getRuntime().exec(cmd);
//            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
//            String line = br.readLine();
//            while(p.isAlive())
//                if (line != null){
//                    WebSocketServer.sendMessage(GsonPlus.GSON.toJson(new WebSocketMessageForm("smri", line)),token);
//                    System.out.println(line);
//                    line = br.readLine();
//                }
//            BufferedReader err = new BufferedReader(new InputStreamReader(p.getErrorStream()));
//            line = err.readLine();
//            while(line != null){
//                WebSocketServer.sendMessage(GsonPlus.GSON.toJson(new WebSocketMessageForm("smri", line)),token);
//                System.out.println(line);
//                line = err.readLine();
//            }
//            System.out.println("exit("+p.exitValue()+")");
//            if (p.exitValue()!=0){
//                WebSocketServer.sendMessage(GsonPlus.GSON.toJson(new WebSocketMessageForm("smri", "error")),token);
//                WebSocketServer.sendMessage(GsonPlus.GSON.toJson(new WebSocketMessageForm("smriState", "error")),token);
//                System.out.println("error");
//            } else if (working.exists()) {
//                WebSocketServer.sendMessage(GsonPlus.GSON.toJson(new WebSocketMessageForm("smri", " compress...")),token);
//                System.out.println("compress...");
//                FileOutputStream fos = new FileOutputStream(zipTag);
//                ZipUtils.toZip(working.getAbsolutePath(), fos, true);
//                WebSocketServer.sendMessage(GsonPlus.GSON.toJson(new WebSocketMessageForm("smri", "finish")),token);
//                WebSocketServer.sendMessage(GsonPlus.GSON.toJson(new WebSocketMessageForm("smriState", "finish")),token);
//                mailUtils.sendCompleteMail(map.get("email"),map.get("username"),"sMRI",task,true);
//                System.out.println("finish");
//            }else {
//                System.out.println("compress fail");
//                WebSocketServer.sendMessage(GsonPlus.GSON.toJson(new WebSocketMessageForm("smri", "compress fail")),token);
//                WebSocketServer.sendMessage(GsonPlus.GSON.toJson(new WebSocketMessageForm("smriState", "error")),token);
//            }
//            br.close();
//            err.close();
//            p.destroy();
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//    }
//    @Async
//    public void dti(HashMap<String,String>map, String task, String token, String paraJson){
//        String userHomeDir = map.get("userHomeDir");
//        File path = new File(filedir + userHomeDir + "/dti/"+task);
////        File working = new File(filedir + userHomeDir + "/dti/"+task+"/working_dwi");
////        File zipTag = new File(working.getAbsolutePath() + ".zip");
////        if (working.exists())FileHandleUtils.deleteFold(working);// 判断文件夹是否存在，如果存在就删除
//        if(!path.exists()) path.mkdirs();
////        zipTag.delete();// 删除原有压缩文件
//        paraJson = resetParaJsonPath(paraJson);
//        System.out.println(paraJson);
//        String[] cmd = {"python",pyScripyPath.get("dti"),path.getAbsolutePath(), paraJson};
//        WebSocketServer.sendMessage(GsonPlus.GSON.toJson(new WebSocketMessageForm("dtiState", "running")),token);
//        try{
//            Process p = Runtime.getRuntime().exec(cmd);
//            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
//            String line = br.readLine();
//            while(p.isAlive())
//                if (line != null){
//                    WebSocketServer.sendMessage(GsonPlus.GSON.toJson(new WebSocketMessageForm("dti", line)),token);
//                    System.out.println(line);
//                    line = br.readLine();
//                }
//            BufferedReader err = new BufferedReader(new InputStreamReader(p.getErrorStream()));
//            line = err.readLine();
//            while(line != null){
//                WebSocketServer.sendMessage(GsonPlus.GSON.toJson(new WebSocketMessageForm("dti", line)),token);
//                System.out.println(line);
//                line = err.readLine();
//            }
//            System.out.println("exit("+p.exitValue()+")");
//            if (p.exitValue()!=0){
//                WebSocketServer.sendMessage(GsonPlus.GSON.toJson(new WebSocketMessageForm("dti", "error")),token);
//                WebSocketServer.sendMessage(GsonPlus.GSON.toJson(new WebSocketMessageForm("dtiState", "error")),token);
//                System.out.println("error");
//            } else/* if (working.exists())*/ {
////                WebSocketServer.sendMessage(GsonPlus.GSON.toJson(new WebSocketMessageForm("dti", " compress...")),token);
////                System.out.println("compress...");
////                FileOutputStream fos = new FileOutputStream(zipTag);
////                ZipUtils.toZip(working.getAbsolutePath(), fos, true);
//                WebSocketServer.sendMessage(GsonPlus.GSON.toJson(new WebSocketMessageForm("dti", "finish")),token);
//                WebSocketServer.sendMessage(GsonPlus.GSON.toJson(new WebSocketMessageForm("dtiState", "finish")),token);
//                mailUtils.sendCompleteMail(map.get("email"),map.get("username"),"dti",task,true);
//                System.out.println("finish");
//            }/*else {
//                System.out.println("compress fail");
//                WebSocketServer.sendMessage(GsonPlus.GSON.toJson(new WebSocketMessageForm("dti", "compress fail")),token);
//                WebSocketServer.sendMessage(GsonPlus.GSON.toJson(new WebSocketMessageForm("dtiState", "error")),token);
//            }*/
//            br.close();
//            err.close();
//            p.destroy();
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//    }
}
