package com.brainsci.utils;

import com.brainsci.form.BsciProcessor;
import com.brainsci.security.entity.UserBaseEntity;
import com.brainsci.security.util.GsonPlus;
import com.brainsci.websocket.form.WebSocketMessageForm;
import com.brainsci.websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

public class BsciProcessorHandler {
    Map<String, BsciProcessor> handlers = new HashMap<String,BsciProcessor>();
    private static BsciProcessorHandler processorHandler = null;
    private String filedir;
    private List<ProcessWrapper> processWrappers = new ArrayList<>();
    private final String LOCK = "LOCK";
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private Thread pollThread = new Thread(){// 轮询线程
        @Override
        public void run() {
            synchronized (LOCK){
                while(true){
                    Iterator<ProcessWrapper> it = processWrappers.iterator();
                    while (it.hasNext()) {
                        ProcessWrapper p = it.next();
                        if (p.isAlive()) {
//                            logger.info("Call");
                            p.call();
                        } else {
                            p.beforeExit();
                            it.remove();
                        }
                    }
                    try{
                        if(processWrappers.isEmpty()) {
                            logger.info("Hang");
                            LOCK.wait();// 在语句块中释放锁不会被本语句块重新获取, 阻塞在这里
                            logger.info("Awake");
                        }
                        // 详情请看线程状态图
                        Thread.sleep(1000);
                    }catch (InterruptedException e){
                        e.printStackTrace();
                    }
                }
            }//释放锁后LOCK，先执行到哪个线程的同步块就执行它
        }
    };
    private Thread exitThread = new Thread() {
        @Override
        public void run(){
            logger.info("清理子进程");
            Iterator<ProcessWrapper> it = processWrappers.iterator();
            while (it.hasNext()) {
                ProcessWrapper p = it.next();
                p.destroy();
            }
        }
    };

    private BsciProcessorHandler() {
        pollThread.start();
        Runtime.getRuntime().addShutdownHook(exitThread);
    }

    private void add(ProcessWrapper pw){
        synchronized (LOCK){
            processWrappers.add(pw);
            logger.info("Add new process");
            LOCK.notify();// 若有线程用了wait, 使用notify唤醒
        }
    }

    public boolean create(String procId, String task, String token, UserBaseEntity userInfo, String paraJson){
        BsciProcessor proc = handlers.get(procId);
        if(proc==null) return false;
        File path = new File(String.format(filedir + userInfo.getHomeDirectory() + "/%s/%s",procId,task));
        String pathStr = path.getAbsolutePath().replaceAll("/./","/");
        if(!path.exists()) path.mkdirs();
        String deleteTag = proc.getDeleteTaget();
        String outputTag = proc.getOutputTaget();
        if (deleteTag!=null) {// 需要删除操作
            File deletepath;
            if(deleteTag.charAt(0)=='/')deletepath = new File(deleteTag);
            else deletepath = new File(pathStr+"/"+deleteTag);
            if(deletepath.exists()) FileHandleUtils.deleteFold(deletepath);// 判断文件夹是否存在，如果存在就删除
        }
        String[] params = proc.getParams();
        String param;
        String[] replAll = proc.getReplaceAll();
        if(replAll!=null) paraJson = resetParaJsonPath(paraJson,replAll);
        paraJson = paraJson.replaceAll("/./","/");
//        System.out.println(paraJson);
        int len = params.length;
        String[] cmdArray = new String[len+1];
        cmdArray[0] = proc.getBoot();
        for(int i = 0;i < len;i++){
            param = params[i];
            if(param.matches("<jsonparam>")){
                param = param.replaceAll("<jsonparam>",paraJson);
            }
            if (param.matches("<basepath>")){
                param = param.replaceAll("<basepath>",pathStr);
            }
            if (param.matches("<outputpath>")){
                File outputpath = new File(pathStr+"/"+outputTag);
                param = param.replaceAll("<outputpath>",outputpath.getAbsolutePath().replaceAll("/./","/"));
            }
            cmdArray[i+1] = param;
        }
        System.out.println(Arrays.toString(cmdArray));
        try {
//            Process p = Runtime.getRuntime().exec(cmdArray);
            Process p=new ProcessBuilder(cmdArray)
                    .directory(path)
                    .redirectOutput(new File(pathStr+"/"+procId+".log"))
                    .redirectError(new File(pathStr+"/"+procId+".error"))
                    .start();
            logger.info("Created");
            ProcessWrapper pw = new ProcessWrapper(procId,token,task,pathStr,userInfo);
            pw.setProcess(p);
            add(pw);
        }catch (IOException ioe){
            logger.error(ioe.getMessage());
        }
        return true;
    }

    private String resetParaJsonPath(String paraJson, String[] replAll){
        int len = replAll.length;
        if((len&1)==1) len = len - 1;
        for(int i = 0;i < len;i+=2){
            paraJson = paraJson.replaceAll(replAll[i],replAll[i+1]);
        }
        return paraJson;
    }

    public static BsciProcessorHandler getProcessorHandler() {
        if(processorHandler == null) processorHandler = new BsciProcessorHandler();
        return processorHandler;
    }

    public BsciProcessorHandler setHandlers(List<BsciProcessor> handlersList) {
        for(BsciProcessor bp:handlersList){
            this.handlers.put(bp.getId(),bp);
        }
        return this;
    }

    public BsciProcessorHandler setFiledir(String filedir) {
        this.filedir = filedir;
        return this;
    }
}
class ProcessWrapper{
    private String procId;
    private String token;
    private String task;
    private String pathStr;
    private UserBaseEntity userInfo;
    private Process process;
    private long position = 0;
    private long lastModify = 0;

    ProcessWrapper(String procId, String token, String task, String pathStr, UserBaseEntity userInfo) {
        this.procId = procId;
        this.token = token;
        this.task = task;
        this.pathStr = pathStr;
        this.userInfo = userInfo;
    }

    void setProcess(Process process) {
        this.process = process;
    }

    boolean isAlive(){
        return process.isAlive();
    }

    void call(){
        File log = new File(pathStr+"/"+procId+".log");
        if(lastModify == log.lastModified()) return;
        else lastModify = log.lastModified();
        try {
            FileInputStream fis = new FileInputStream(log);
            if(fis.available()<position){
                position = 0;
            }else {
                fis.skip(position);
                position = fis.available();
            }
            String cntent = new String(fis.readAllBytes());
            WebSocketServer.sendMessage(GsonPlus.GSON.toJson(new WebSocketMessageForm(procId, cntent)),token);
            System.out.println(cntent);
            fis.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    void beforeExit(){
        BsciProcessor proc = BsciProcessorHandler.getProcessorHandler().handlers.get(procId);
        try{
            BufferedReader err = new BufferedReader(new FileReader(pathStr+"/"+procId+".error"));
            String line = err.readLine();
            while(line != null){
                WebSocketServer.sendMessage(GsonPlus.GSON.toJson(new WebSocketMessageForm(procId, line)),token);
                System.out.println(line);
                line = err.readLine();
            }
            System.out.println("exit("+process.exitValue()+")");
            if (process.exitValue()!=0){
                WebSocketServer.sendMessage(GsonPlus.GSON.toJson(new WebSocketMessageForm(procId, "error")),token);
                System.out.println("error");
            } else {
                WebSocketServer.sendMessage(GsonPlus.GSON.toJson(new WebSocketMessageForm(procId, "finish")),token);
                if(proc.isEnableMail()) MailUtils.currentMail.sendCompleteMail(userInfo.getEmail(),userInfo.getUsername(),procId,task,true);
                System.out.println("finish");
            }
            err.close();
            destroy();
        }catch (IOException e){
            e.printStackTrace();
        }
    }
    void destroy(){
        process.toHandle().descendants().forEach(ProcessHandle::destroyForcibly);
        process.toHandle().destroyForcibly();
    }
}