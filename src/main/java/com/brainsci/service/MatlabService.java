package com.brainsci.service;

import com.brainsci.form.CommonResultForm;
import com.brainsci.form.NetAnalysisOption;
import com.brainsci.security.util.GsonPlus;
import com.brainsci.utils.FileHandleUtils;
import com.brainsci.utils.MatlabUtils;
import com.brainsci.utils.ZipUtils;
import com.brainsci.websocket.form.WebSocketMessageForm;
import com.brainsci.websocket.server.WebSocketServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

@Service
public class MatlabService {
    private final String filedir;
    public MatlabService(@Value("${filesys.dir}") String filedir, @Value("${filesys.path-matlab}") String matlabPath) {
        this.filedir = filedir;
        MatlabUtils.matlabApplication = new File(matlabPath);
    }
    @Async
    public void networkAnalysis(String userHomeDir, String task, String token, NetAnalysisOption para){
        MatlabUtils.state.put(userHomeDir, "running");
        WebSocketServer.sendMessage(GsonPlus.GSON.toJson(new WebSocketMessageForm("gretnaState", "running")),token);
        File path = new File(filedir + userHomeDir + "/netAnalysis/"+task+"/result");
        if (path.exists())FileHandleUtils.deleteFold(path);// 判断文件夹是否存在，如果存在就删除
        path.mkdirs();
        int SType = para.getSignType();
        int TType = para.getThresMethod();
        int NType = para.getNetType();
        List<Double> Thres = para.getThresSequence();
        List<String> fileList = para.getRealNetURI().stream().map(s->"'" + new File((filedir + s.replaceAll("/MyFile",userHomeDir.substring(2)))).getAbsolutePath() + "'").collect(Collectors.toList());
        List<Integer> SelPreInd = para.getSelPreInd();
        List<Integer> SelPostInd = para.getSelPostInd();
        List<Double> CIndex = para.getcIndex();
        String InputMatCell = fileList.toString().replaceAll(",",";");
        String cmd =
                "handles.InputMatCell={" + InputMatCell.substring(1, InputMatCell.length()-1) + "};" +
                "handles.SelPreInd=" + SelPreInd + "';" +
                "handles.SelPostInd=" + SelPostInd + "';" +
                "handles.OutputDir=['" + path.getAbsolutePath() + "'];" +
                "Para.RandNum={" + para.getRandNetworkNum() + "};" +
                "Para.NetSign={" + SType + "};" +
                "Para.ThresType={" + TType + "};" +
                "Para.Thres={" + Thres + "};" +
                "Para.NetType={" + NType + "};" +
                "Para.ClustAlgor={" + para.getClustAlgor() + "};" +
                "Para.ModulAlgor={" + para.getModulAlgor() + "};" +
                "Para.DDPcFlag={" + para.getdDPcFlag() + "};" +
                "Para.CIndex={" + CIndex + "};" +
                "gretna_pipeline(handles,Para);";
        System.out.println(cmd);
        MatlabUtils.getMatlabUtils().run(cmd,token, path);
        WebSocketServer.sendMessage(GsonPlus.GSON.toJson(new WebSocketMessageForm("gretna", "finish!!")),token);
        WebSocketServer.sendMessage(GsonPlus.GSON.toJson(new WebSocketMessageForm("gretnaState", "finish")),token);
        MatlabUtils.state.remove(userHomeDir);
    }
//
//    public CommonResultForm gretnaAnalysis(String userIP, Para para){
//        File temp = new File(filedir + "/" + userIP);
//        if (!temp.exists()) {
//            temp.mkdirs();// 判断文件夹是否存在，如果不存在就创建
//        }
//        int SType = para.getSignType();
//        int TType = para.getThresMethod();
//        int NType = para.getNetType();
//        List<Double> Thres = para.getThresSequence();
//        int randNetworkNum = para.getRandNetworkNum();
//        int ClustAlgor = 1;
//        double AUCInterval = Thres.get(1) - Thres.get(0);
//        File realNetFile = new File(filedir + "/" + para.getRealNetURI());
//        List<String> label = para.getToDoList();
//        GretnaUtils gretnaUtils = GretnaUtils.getGretnaUtils();
//        gretnaUtils.genNetwork(temp, realNetFile, SType, TType, Thres, NType, randNetworkNum);
//        if (label.contains("Global - Small-World"))gretnaUtils.smallWorld(temp, NType, ClustAlgor, AUCInterval);
//        if (label.contains("Global - Efficiency"))gretnaUtils.globalEfficiency(temp, NType, AUCInterval);
//        if (label.contains("Global - Rich-Club"))gretnaUtils.richClub(temp, NType);
//        if (label.contains("Global - Assortativity"))gretnaUtils.assortativity(temp, NType, AUCInterval);
//        if (label.contains("Global - Synchronization"))gretnaUtils.synchronization(temp, NType, AUCInterval);
//        if (label.contains("Global - Hierarchy"))gretnaUtils.hierarchy(temp, NType, AUCInterval);
//        if (label.contains("Nodal - Shortest Path Length"))gretnaUtils.shortestPathLength(temp, NType, AUCInterval);
//        if (label.contains("Nodal - Efficiency"))gretnaUtils.nodalEfficiency(temp, NType, AUCInterval);
//        if (label.contains("Nodal - Local Efficiency"))gretnaUtils.localEfficiency(temp, NType, AUCInterval);
//        if (label.contains("Nodal - Degree Centrality"))gretnaUtils.degreeCentrality(temp, NType, AUCInterval);
//        if (label.contains("Nodal - Betweenness Centrality"))gretnaUtils.betweennessCentrality(temp, NType, AUCInterval);
////        if (label.contains("Nodal - Community Index"))gretnaUtils.communityIndex(NType, AUCInterval);
////        if (label.contains("Nodal - Participant Coefficient"))gretnaUtils.participantCoefficient(NType, AUCInterval);
////        if (label.contains("Modular - Interaction"))gretnaUtils.interaction(NType, AUCInterval);
//        return CommonResultForm.of204("sucess");
//    }
}
