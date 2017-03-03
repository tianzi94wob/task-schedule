package cn.com.citycloud.frame.task.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;

import cn.com.citycloud.frame.task.ConsoleManager;
import cn.com.citycloud.frame.task.core.TaskDefine;

@WebServlet(name="schedule",urlPatterns="/task/schedule")
public class ManagerServlet extends HttpServlet{

	/**
	 * 
	 */
	private static final long serialVersionUID = 8160082230341182715L;
	
	private static final String HEAD = 
		    "<!DOCTYPE html>\n"+
		    "<html>\n"+
		    "<head>\n"+
		    "<meta charset=\"utf-8\"/>\n"+
		    "\t  <title>Uncode-Schedule管理</title>\n"+
		    "\t  <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">\n"+
		    "\t  <meta name=\"viewport\" content=\"width=device-width\"/>\n"+
		    "\t  <meta name=\"keywords\" content=\"uncode,冶卫军\"/>\n"+
		    "\t  <meta name=\"description\" content=\"Uncode-Schedule管理\"/>\n"+
		    "\t  <link rel=\"stylesheet\"  href=\"http://cdn.bootcss.com/bootstrap/3.3.4/css/bootstrap.min.css\">\n"+
		    "\t  <script type=\"text/javascript\" src=\"http://apps.bdimg.com/libs/jquery/2.1.4/jquery.min.js\"></script>\n"+
		    "\t  <script type=\"text/javascript\" src=\"http://cdn.bootcss.com/bootstrap/3.3.4/js/bootstrap.min.js\"></script>\n"+
			"</head>\n";
	
	private static final String SCRIPT = 
			"\t	<script type=\"text/javascript\">\n"+
			"\t		$(document).ready(function(){\n"+
			"\t			$(\"#myModal\").on('show.bs.modal', function(event){\n"+
			"\t		    var button = $(event.relatedTarget); \n"+
			"\t			var titleData = button.data('title'); \n"+
			"\t		    var modal = $(this)\n"+
			"\t	       	modal.find('.modal-title').text(titleData + '定时任务');\n"+
			"\t	  		});\n"+
			"\t		});\n"+
			"\t		function formSubmit(){\n"+
			"\t			document.getElementById(\"addform\").submit();\n"+
			"\t		}\n"+
			"\t	</script>";
	
	private static final String PAGE = 
			"\t <body>\n"+
			"\t <div class=\"container\">\n"+
			"\t     <div class=\"navbar-right\">\n"+
			"\t     	<button type=\"button\" class=\"btn btn-primary\"  data-toggle=\"modal\" data-target=\"#myModal\" data-title=\"新增\">新增</button>\n"+
			"\t     </div>\n"+
			"\t     <div id=\"myModal\" class=\"modal fade\">\n"+
			"\t         <div class=\"modal-dialog\">\n"+
			"\t             <div class=\"modal-content\">\n"+
			"\t                 <div class=\"modal-header\">\n"+
			"\t                     <button type=\"button\" class=\"close\" data-dismiss=\"modal\" aria-hidden=\"true\">&times;</button>\n"+
			"\t                     <h4 class=\"modal-title\">Modal Window</h4>\n"+
			"\t                 </div>\n"+
			"\t                 <div class=\"modal-body\">\n"+
			"\t 					<div class=\"container\">\n"+
			"\t 						<form id=\"addform\" method=\"post\" action=\"%s\" class=\"form-horizontal\">\n"+
			"\t 						<div class=\"row\">\n"+
			"\t 							<div class=\"col-md-6\">\n"+
			"\t 									<div class=\"form-group\">\n"+
			"\t 										<label class=\"col-sm-4 control-label\" for=\"bean\">bean名称<span style=\"color:red\">*</span></label>\n"+
			"\t 										<div class=\"col-sm-6\">\n"+
			"\t 											<input id=\"bean\" name=\"bean\" type=\"text\" class=\"form-control\" required>\n"+
			"\t 										</div>\n"+
			"\t 									</div>\n"+
			"\t 									<div class=\"form-group\">\n"+
			"\t 										<label class=\"col-sm-4 control-label\" for=\"method\">方法名称<span style=\"color:red\">*</span></label>\n"+
			"\t 										<div class=\"col-sm-6\">\n"+
			"\t 											<input id=\"method\" name=\"method\" type=\"text\" class=\"form-control\" required>\n"+
			"\t 										</div>\n"+
			"\t 									</div>\n"+
			"\t 									<div class=\"form-group\">\n"+
			"\t 										<label class=\"col-sm-4 control-label\" for=\"cronExpression\">corn表达式</label>\n"+
			"\t 										<div class=\"col-sm-6\">\n"+
			"\t 											<input id=\"cronExpression\" name=\"cronExpression\" type=\"text\" class=\"form-control\" required>\n"+
			"\t 										</div>\n"+
			"\t 									</div>\n"+
			"\t 									<div class=\"form-group\">\n"+
			"\t 										<label class=\"col-sm-4 control-label\" for=\"period\">周期（秒）</label>\n"+
			"\t 										<div class=\"col-sm-6\">\n"+
			"\t 											<input id=\"period\" name=\"period\" type=\"text\" class=\"form-control\" required>\n"+
			"\t 										</div>\n"+
			"\t 									</div>\n"+
			"\t 									<div class=\"form-group\">\n"+
			"\t 										<label class=\"col-sm-4 control-label\" for=\"startTime\">开始时间</label>\n"+
			"\t 										<div class=\"col-sm-6\">\n"+
			"\t 											<input id=\"startTime\" name=\"startTime\" type=\"text\" class=\"form-control\" required>\n"+
			"\t 										</div>\n"+
			"\t 									</div>\n"+
			"\t 									<div class=\"form-group\">\n"+
			"\t 										<label class=\"col-sm-4 control-label\" for=\"param\">参数(字符串)</label>\n"+
			"\t 										<div class=\"col-sm-6\">\n"+
			"\t 											<input id=\"param\" name=\"param\" type=\"text\" class=\"form-control\" required>\n"+
			"\t 										</div>\n"+
			"\t 									</div>\n"+
			"\t              		   				<div class=\"modal-footer\">\n"+
			"\t               		      				<button type=\"button\" class=\"btn btn-default\" data-dismiss=\"modal\">取消</button>\n"+
			"\t               		      				<button type=\"button\" onclick=\"formSubmit()\" class=\"btn btn-primary\">保存</button>\n"+
			"\t             	    				</div>\n"+
			"\t                 				</div>\n"+
			"\t                 			</div>\n"+

			"\t 							</form>\n"+
			"\t         				</div>\n"+
			"\t         			</div>\n"+
			"\t         		</div>\n"+
			"\t     	</div>\n"+
		    "\t 	</div>\n"+
		    "\t </div>\n"+
			"\t <h1>Uncode-Schedule管理页面</h1>\n"+
			"\t <div class=\"container-fluid\">\n"+
			"\t 	<div class=\"row-fluid\">\n"+
			"\t 		<div class=\"span12\">\n"+
			"\t 			<h3>集群节点</h3>\n"+
			"\t 			<table class=\"table\">\n"+
			"\t 				<thead>\n"+
			"\t 					<tr>\n"+
			"\t 						<th width=\"100px\">序号</th>\n"+
			"\t 						<th>名称</th>\n"+
			"\t 						<th>调度节点</th>\n"+
			"\t 					</tr>\n"+
			"\t 				</thead>\n"+
			"\t 				<tbody>\n"+
			"\t 					%s \n"+
			"\t 				</tbody>\n"+
			"\t 			</table>\n"+
			"\t 		</div>\n"+
			"\t 		<div class=\"span12\">\n"+
			"\t 			<h3>定时任务列表</h3>\n"+
			"\t 			<table class=\"table\">\n"+
			"\t 				<thead>\n"+
			"\t 					<tr>\n"+
			"\t 						<th>序号</th>\n"+
			"\t 						<th>目标bean</th>\n"+
			"\t 						<th>目标方法</th>\n"+
			"\t 						<th>类型</th>\n"+
			"\t 						<th>cron表达式</th>\n"+
			"\t 						<th>开始时间</th>\n"+
			"\t 						<th>周期（秒）</th>\n"+
			"\t 						<th>执行节点</th>\n"+
			"\t 						<th>执行次数</th>\n"+
			"\t 						<th>最近执行时间</th>\n"+
			"\t 						<th>操作</th>\n"+
			"\t 					</tr>\n"+
			"\t 				</thead>\n"+
			"\t 				<tbody>\n"+
			"\t 					%s\n "+
			"\t 				</tbody>\n"+
			"\t 			</table>\n"+
			"\t 		</div>\n"+
			"\t 	</div>\n"+
			"\t </div>\n"+
			"\t </body>";
	
	
	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String del = request.getParameter("del");
		String bean = request.getParameter("bean");
		String method = request.getParameter("method");
		if(StringUtils.isNotEmpty(del)){
			TaskDefine taskDefine = new TaskDefine();
			String[] dels = del.split("_");
			taskDefine.setTargetBean(dels[0]);
			taskDefine.setTargetMethod(dels[1]);
			ConsoleManager.delScheduleTask(taskDefine);
			response.sendRedirect(request.getSession().getServletContext().getContextPath()+"/uncode/schedule");
		}else if(StringUtils.isNotEmpty(bean) && StringUtils.isNotEmpty(method)){
			TaskDefine taskDefine = new TaskDefine();
			taskDefine.setTargetBean(bean);
			taskDefine.setTargetMethod(method);
			String cronExpression = request.getParameter("cronExpression");
			if(StringUtils.isNotEmpty(cronExpression)){
				taskDefine.setCronExpression(cronExpression);
			}
			String period = request.getParameter("period");
			if(StringUtils.isNotEmpty(period)){
				taskDefine.setPeriod(Long.valueOf(period));
			}
			String startTime = request.getParameter("startTime");
			if(StringUtils.isNotEmpty(startTime)){
				taskDefine.setStartTime(new Date());
			}
			String param = request.getParameter("param");
			if(StringUtils.isNotEmpty(param)){
				taskDefine.setParams(param);
			}
			if(StringUtils.isNotEmpty(cronExpression) || StringUtils.isNotEmpty(period)){
				ConsoleManager.addScheduleTask(taskDefine);
			}
			response.sendRedirect(request.getSession().getServletContext().getContextPath()+"/uncode/schedule");
		}
		try {
			List<String> servers = ConsoleManager.getScheduleManager().getScheduleDataManager().loadScheduleServerNames();
			if(servers != null){
				response.setContentType("text/html;charset=UTF-8");
		        PrintWriter out = response.getWriter();  
		        StringBuffer sb = new StringBuffer();
	    		for(int i=0; i< servers.size();i++){
	    			String ser = servers.get(i);
	    			sb.append("<tr class=\"info\">")
	    			  .append("<td>").append(i+1).append("</td>")
	    			  .append("<td>").append(ser).append("</td>");
					if( ConsoleManager.getScheduleManager().getScheduleDataManager().isLeader(ser, servers)){
						sb.append("<td>").append("是").append("</td>");
					}else{
						sb.append("<td>").append("否").append("</td>");
					}
	    			sb.append("</tr>");
	    		}
	    		
	    		List<TaskDefine> tasks = ConsoleManager.queryScheduleTask();
	    		StringBuffer sbTask = new StringBuffer();
	    		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	    		for(int i=0; i< tasks.size();i++){
	    			TaskDefine taskDefine = tasks.get(i);
	    			sbTask.append("<tr class=\"info\">")
	    			  .append("<td>").append(i+1).append("</td>")
	    			  .append("<td>").append(taskDefine.getTargetBean()).append("</td>")
	    			  .append("<td>").append(taskDefine.getTargetMethod()).append("</td>")
	    			  .append("<td>").append(taskDefine.getType()).append("</td>")
	    			  .append("<td>").append(taskDefine.getCronExpression()).append("</td>")
	    			  .append("<td>").append(taskDefine.getStartTime()).append("</td>")
	    			  .append("<td>").append(taskDefine.getPeriod()).append("</td>")
	    			  .append("<td>").append(taskDefine.getCurrentServer()).append("</td>")
	    			  .append("<td>").append(taskDefine.getRunTimes()).append("</td>");
	    			if(taskDefine.getLastRunningTime() > 0){
	    				Date date = new Date(taskDefine.getLastRunningTime());
		    			sbTask.append("<td>").append(sdf.format(date)).append("</td>");
	    			}else{
	    				sbTask.append("<td>").append("-").append("</td>");
	    			}
	    			sbTask.append("<td>").append("<a href=\"").append(request.getSession().getServletContext().getContextPath())
	    			  				 .append("/uncode/schedule?del=")
	    			                 .append(taskDefine.getTargetBean())
	    			                 .append("_")
	    			                 .append(taskDefine.getTargetMethod())
	    			                 .append("\" >删除</a>")
	    			                 .append("</td>");
					sbTask.append("</tr>");
	    		}
	    		 out.write(HEAD);
	    		 out.write(SCRIPT);
	    		 out.write(String.format(PAGE, request.getSession().getServletContext().getContextPath()+"/uncode/schedule", 
	    				sb.toString(), sbTask.toString()));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

}
