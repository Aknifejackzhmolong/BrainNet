package com.brainsci.brainnet;

import com.brainsci.form.CommonResultForm;
import com.brainsci.form.NetAnalysisOption;
import com.brainsci.security.entity.UserBaseEntity;
import com.brainsci.security.repository.UserBaseRepository;
import com.brainsci.security.repository.UserRepository;
import com.brainsci.security.util.GsonPlus;
import com.brainsci.service.MatlabService;
import com.brainsci.utils.BsciProcessorHandler;
import com.brainsci.utils.MatlabUtils;
import com.brainsci.websocket.form.WebSocketMessageForm;
import com.brainsci.websocket.server.WebSocketServer;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Map;

@RestController
public class BrainNetController {

    private final MatlabService matlabService;
    private final UserBaseRepository userBaseRepository;
    private final UserRepository userRepository;

    @Autowired
    public BrainNetController(MatlabService matlabService, UserBaseRepository userBaseRepository, UserRepository userRepository) {
        this.matlabService = matlabService;
        this.userBaseRepository = userBaseRepository;
        this.userRepository = userRepository;
    }
    @ApiOperation(value = "网络分析")
    @PostMapping(value = "/gretna/{task}/{token}")
    public CommonResultForm gretnaNetworkAnalysis(@PathVariable("task") String task,@RequestBody NetAnalysisOption para, @PathVariable("token") String token, HttpServletRequest request, HttpSession httpSession){
        String userHomeDir = userBaseRepository.getOne((String) httpSession.getAttribute("username")).getHomeDirectory();
        if (MatlabUtils.state.containsKey(userHomeDir)) return CommonResultForm.of204("Tasks are "+MatlabUtils.state.get(userHomeDir)+", please wait");
        MatlabUtils.state.put(userHomeDir, "submitted");
        WebSocketServer.sendMessage(GsonPlus.GSON.toJson(new WebSocketMessageForm("gretnaState", "submitted")),token);
        matlabService.networkAnalysis(userHomeDir,task, token, para);
        return CommonResultForm.of204("Tasks are queuing");
    }
    @ApiOperation(value = "Python脚本调用模块")
    @PostMapping(value = "/proc/{procid}/{task}/{token}")
    public CommonResultForm test(@PathVariable("procid") String procid,
                                 @PathVariable("task") String task,
                                 @PathVariable("token") String token,
                                 @RequestBody Map<String, String> map, HttpSession httpSession, HttpServletResponse httpServletResponse){
        String str = map.get("jsonstr");
        String username = (String) httpSession.getAttribute("username");
        UserBaseEntity userInfo = userBaseRepository.getOne(username);
        boolean success = BsciProcessorHandler.getProcessorHandler().create(procid,task,token,userInfo,str);
        if(!success) httpServletResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        return CommonResultForm.of204("success");
    }
    @RequestMapping(value = "/")
    public void index(HttpServletResponse response) throws IOException{
        response.sendRedirect("/bsci/index.html");
    }
}