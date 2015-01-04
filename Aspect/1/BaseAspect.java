
/**  
 * @Title: BaseAspect.java
 * @Package com.haier.openplatform.ppm.common.aop
 * @Description: 切面处理基类文件
 * @author code0   
 * @date 2014-1-23 下午2:21:36
 * @version V1.0  
 */
 
package com.haier.openplatform.alm.log.webapp.aop;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.struts2.ServletActionContext;
import org.aspectj.lang.JoinPoint;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.haier.openplatform.alm.log.domain.UserLog;
import com.haier.openplatform.alm.log.service.IUserLogService;
import com.haier.openplatform.security.SessionSecurityConstants;
import com.haier.openplatform.util.IpUtil;
import com.opensymphony.xwork2.ActionContext;


/**  
 * @Title: BaseAspect.java
 * @Package com.haier.openplatform.ppm.common.aop
 * @Description: 切面处理基类
 * @author code0   
 * @date 2014-1-23 下午2:21:36 
 */
public class BaseAspect {
	
	/**
	 * @return the log
	 */
	public Logger getLog() {
		return log;
	}
	/**
	 * @param log the log to set
	 */
	public void setLog(Logger log) {
		this.log = log;
	}
	/**
	 * 是否记录方法的参数.true记录;false不记录
	 * @return
	 */
	public boolean isSaveArgs() {
		return saveArgs;
	}
	/**
	 * 设置是否记录方法的参数.默认true
	 * @param saveArgs
	 */
	public void setSaveArgs(boolean saveArgs) {
		this.saveArgs = saveArgs;
	}
	/**
	 * 是否记录方法的返回值.true记录;false不记录
	 * @return
	 */
	public boolean isSaveResults() {
		return saveResults;
	}
	/**
	 * 设置是否记录方法的返回值;默认false
	 * @param saveResults
	 */
	public void setSaveResults(boolean saveResults) {
		this.saveResults = saveResults;
	}

	public IUserLogService getUserLogService() {
		return userLogService;
	}
	
	public void setUserLogService(IUserLogService userLogService) {
		this.userLogService = userLogService;
	}

	/**日志中是否记录方法的参数-默认true*/
	private boolean saveArgs=true;
	/**日志中是否记录方法的返回值-默认false*/
	private boolean saveResults=false;
	/**log对象*/
	private Logger log=Logger.getLogger(BaseAspect.class);
	/**用户日志业务操作对象*/
	private IUserLogService userLogService;
	
	
	
	/**
	 * 封装-用户日志处理
	 * @param joinpoint 连接点
	 * @param UserLog   用户日志对象-可为空
	 * @param returnArg 返回值-可为空，如doBefor时
	 */
	public void packUserLog(JoinPoint joinpoint,UserLog userLog1,Object returnArg){
		UserLog userLog =userLog1;
		//用户名
		String userCode=null;
		//方法执行结果
		String status="success";
		//封装日志对象
		if(userLog==null){
			userLog=new UserLog();			
		}

		
		//连接点目标对象
		Object target=joinpoint.getTarget();
		//连接点传递的参数
		Object[] args=joinpoint.getArgs();
		
		//方法名
		String methodName=joinpoint.getSignature().getName();
		//类名
		String clazzName=target.getClass().getName();
		
		//设置用户执行动作时需要记录的一些属性.
		try{
			//获取request
			HttpServletRequest request=(HttpServletRequest)ActionContext.getContext().get(ServletActionContext.HTTP_REQUEST);
			//客户端请求的IP地址
			String ip=IpUtil.getIpAddress(request);
			userLog.setIp(ip);
			//获取session
			HttpSession session=request.getSession();
			//userCode赋值【只要userLog对象中的usercode不是null值，代表有过赋值操作，不再进行取值操作。】
			if(StringUtils.isEmpty(userLog.getUserCode())&&session!=null){
				userCode=(String)session.getAttribute(SessionSecurityConstants.KEY_USER_NAME);
				//如果取不到用户名，说明当前切入点为登陆方法，且登陆未成功，未执行到写session的语句，
				//则在request中取用户名信息
				if(!StringUtils.isEmpty(userCode)){
					//userCode=request.getParameter("username");//当前系统登录表单对应的name[目前是PPM的先注释]
					userLog.setUserCode(userCode);
				}
			}
			this.getLog().debug("IP："+ip+"用户名："+userLog.getUserCode());
		}catch (Exception e) {
			this.getLog().debug("提醒:request不存在,该动作为系统任务");
			userLog.setUserCode("系统操作");
		}
		
		//执行结果赋值【如果没有手动指定值，则设置为程序判断得到值】
		if(userLog.getStatus()==null){
			userLog.setStatus(status);
		}
		//参数值、参数描述赋值
		if(this.isSaveArgs()){
			if(args!=null&&args.length>0){
				//设置参数描述
				JSONArray jsonarray=new JSONArray();
				for(int i=0;i<args.length;i++){
					Object arg=args[i];
					String argType="null";
					if(arg!=null){
						argType=arg.getClass().getName();
					}
					JSONObject jo=new JSONObject();
					jo.put("参数类型", argType);
					jo.put("参数索引", i);
					jsonarray.add(jo);
				}
				userLog.setArgsDescription(JSONObject.toJSONString(jsonarray));//设置参数描述
				userLog.setArgsValue(JSONObject.toJSONString(args));//设置参数值
			}
		}
		//返回值。返回值描述赋值
		if(this.isSaveResults()){
			if(returnArg!=null){
				userLog.setReturnValue(JSONObject.toJSONString(returnArg));
				userLog.setReturnDescription(returnArg.getClass().getName());
			}
		}
		
		userLog.setCreatedDate(new Date());
		userLog.setOperation(methodName);
		userLog.setOperationDetail(clazzName+"."+methodName);
		this.getLog().debug("类："+clazzName+"方法："+methodName);
		//将日志对象写入数据库
		try {
			this.getUserLogService().addUserLog(userLog);
		} catch (Exception e) {
			this.getLog().error("写日志业务处理异常！"+"类："+clazzName+"方法："+methodName);
		}
	}

	
	
	/**
	 * 异常捕获切面处理
	 * @param joinpoint
	 * @param ex
	 */
	public void doThrowing(JoinPoint joinpoint,Throwable ex){
		this.getLog().error("执行切面doThrowing-用户日志切面异常");
		UserLog userLog=new UserLog();
		StringBuffer sb=new StringBuffer();
		sb.append("抛出异常:");
		sb.append(ex.toString());
		userLog.setMsg(sb.toString());
		userLog.setStatus("exception");
		this.packUserLog(joinpoint, userLog, null);
	}
	/**
	 * 后置切面处理
	 * @param joinpoint
	 * @param returnArg
	 */
	public void doAfter(JoinPoint joinpoint, Object returnArg){
		this.packUserLog(joinpoint, null, returnArg);
	}

	
}
