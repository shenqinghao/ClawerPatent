<%@ page language="java" import="java.util.*" pageEncoding="utf-8"%>
<%@page import="inc.tech.persistent.entity.KjPatentFee"%>
<%@page import="inc.tech.sys.chop.dao.KjAuditLogDAO"%>
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean"%>
<%@ taglib uri="http://struts.apache.org/tags-html" prefix="html"%>
<%@ taglib uri="http://struts.apache.org/tags-logic" prefix="logic"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@page import="inc.tech.persistent.entity.*"%>
<%@page import="inc.tech.fund.fundCard.dao.FundCardDao"%>
<%@page import="inc.tech.patent.dao.KjPatentFeeDao"%>
<%@page import="inc.tech.patent.dao.KjPatentDao"%>
<%@ page import="inc.tech.patent.dao.KjProjectStaffDao"%>
<%@ page import="inc.tech.sys.user.UserBean"%>
<%@ page import="inc.tech.sys.group.dao.MemberDAO"%>
<%@page import="inc.tech.sys.group.dao.EmployeeDAO"%>
<%@page import="inc.tech.sys.group.dao.KjGroupDAO"%>
<%@page import="inc.tech.sys.mail.dao.KjMailDAO"%>
<%@page import="inc.tech.util.ParamUtil"%>
<%@ taglib uri="/tags/tech" prefix="tech" %>
<%
	String path = request.getContextPath();
	String basePath = request.getScheme() + "://"
			+ request.getServerName() + ":" + request.getServerPort()
			+ path + "/";
	String sign = ((KjDictIdentity)session.getAttribute("identity")).getSign();
	UserBean userbean = (UserBean) session.getAttribute("UserBean");
	List<KjGroup1> kjGroup =  KjGroupDAO.getInstance().getChildGroup(98l);
	request.setAttribute("kjGroup",kjGroup);
	
	String canWrite = "";
	//数据维护组写权限的判断
	if(MemberDAO.getInstance().GroupHaveMember(MemberDAO.PATENT_MAINTAIN_GROUP, userbean.getUid()) ||
	   MemberDAO.getInstance().GroupHaveMember(MemberDAO.MAINTAIN_GROUP, userbean.getUid())){
		canWrite = "true";
	}  
	List allAmount=(List)request.getAttribute("allAmount");
	List<KjPatentFee> allRecover=(List<KjPatentFee>)request.getAttribute("allRecover");
	String fromOffice=(String)request.getAttribute("fromOffice");
	String towards="showRemind";
	if(fromOffice!=null&&fromOffice.equals("true")){
		towards="showOffice";
	}
	
	String display0 = "none", display1 = "none", display2 = "none", display3 = "none";
	
	
	//lyb 2015-01-04 使用新行 显示意见
	int AdminColumn    = 18 - 3;//前3列跨行
	int ManagerColumn  = 14 - 3;//前3列跨行
	int userColumn     = 15 - 2;//前2列跨行
	
	int thisColmn     = 0;
	if(sign.equalsIgnoreCase("Admin")){
		thisColmn = AdminColumn;
	}else if(sign.equalsIgnoreCase("Manager")){
		thisColmn = ManagerColumn;
	}else{
		thisColmn = userColumn;
	}
	

%>

<!DOCTYPE HTML>
<html>
	<head>
		<base href="<%=basePath%>patent/">

		<title>费用提醒</title>
		<meta http-equiv="pragma" content="no-cache">
		<meta http-equiv="cache-control" content="no-cache">
		<meta http-equiv="expires" content="0">
		<meta http-equiv="keywords" content="keyword1,keyword2,keyword3">
		<meta http-equiv="description" content="This is my page">
		<!--Css+Jquery//-->
		<link rel="StyleSheet" href="<%=basePath%>css/style.css" type="text/css" />
		<link rel="stylesheet" href="http://www.baidufe.com/fe/component/static/widget/jDialog/jDialog.css">
		<link href="<%=basePath%>css/patent/bootstrap.css" rel="stylesheet" type="text/css">
		<link href="<%=basePath%>css/jDialog.css" rel="stylesheet" type="text/css">
		<script language="JavaScript" type="text/javascript" src="<%=basePath%>js/patent/jquery.js"></script>
		<script type="text/javascript" src="<%=basePath%>js/patent/bootstrap-modal.js"></script>
		<script language="JavaScript" type="text/javascript" src="<%=basePath%>js/patent/admin.js"></script>
		<script language="JavaScript" type="text/javascript" src="<%=basePath%>js/page_checkbox.js"></script>
		<script language="JavaScript" type="text/javascript" src="<%=basePath%>js/jDialog.js"></script>
		<script language="JavaScript" type="text/javascript" src="<%=basePath%>js/jquery.dialog.js"></script>
		<script language="JavaScript" type="text/javascript" src="<%=basePath%>js/jquery.drag.js"></script>
		<script language="JavaScript" type="text/javascript" src="<%=basePath%>js/jquery.mask.js"></script>
		<script language="javascript" type="text/javascript" src="<%=basePath%>/js/DatePicker/WdatePicker.js"></script>				
		<style>
		
		.less15{color:white;background:red;text-align:center;}
		.m7l30{color:white;background:#4B0091;text-align:center;}
		.m30l60{color:white;background:#0000FF;text-align:center;}
		.m60l90{color:white;background:#006600;text-align:center;}
		.m90{color:white;text-align:center;}
		
     </style>
	 <style type="text/css">
		.b-r {
				background: none repeat scroll 0 0 #7ABD54;
				border-radius: 1px 1px 1px 1px;
				color: #FFF;
				font-weight: 600;
				height: 20px;
				line-height: 20px;
				width: 60px;
				border: 0 none;
				cursor: pointer;
				display: inline-block;
				margin: 0;
				padding: 0;
				text-align: center;
					}
		.b-r a:hover {
				background: none repeat scroll 0 0 red;
					}
		.b-r-short {
				background: none repeat scroll 0 0 #7ABD54;
				border-radius: 1px 1px 1px 1px;
				color: #FFF;
				font-weight: 600;
				height: 20px;
				line-height: 20px;
				width: 38px;
				border: 0 none;
				cursor: pointer;
				display: inline-block;
				margin: 0;
				padding: 0;
				text-align: center;
					}
		.b-r-short a:hover {
				background: none repeat scroll 0 0 red;
					}
		#myModal{display:none;}
		
		.stop{display:inline-block;width:50px;color:white;background:#e8543f;text-align:center;}
		a.stop:hover{color:white}
		.open{display:inline-block;width:50px;color:white;background:#8fcf00;text-align:center;}
		a.open:hover{color:white}

		</style>		
		<!--Css+Jquery//-->
	</head>
	<script language="JavaScript">
		if ('<html:errors property="infor"/>'!="")
			alert('<html:errors property="infor"/>');
			
			$(document).ready(function(){
			<%if (request.getAttribute("confirm")!=null&&request.getAttribute("confirm")!=""){%>
				var updates=<%=request.getAttribute("updates")%>;
				var notUpdate=<%=request.getAttribute("notUpdate")%>;
				alert("此次所有确认缴费的专利中，有"+updates+"条专利状态从滞纳改为了授权；有"+notUpdate+"条专利状态没有更改。");
			<%}%>
			});
			
	//--批量同年年费--
	
	function createXMLxPreSynHttpRequest() {
		if (window.ActiveXObject) {
			xmlPreSynHttp = new ActiveXObject("Microsoft.XMLHTTP");
		} else if (window.XMLHttpRequest) {
			xmlPreSynHttp = new XMLHttpRequest();
		}
	 }
	 
	 function getPreLogin(){
		createXMLxPreSynHttpRequest();
		var url = "/tech/patent/fee.do?method=getPreLogin";
		xmlPreSynHttp.open("GET", url, true);
		xmlPreSynHttp.onreadystatechange = function preLoginCallback(){
			if (xmlPreSynHttp.readyState == 4) {
					if (xmlPreSynHttp.status == 200) {
						var result = xmlPreSynHttp.responseText;
						if(result.length == 0){
							var yzm = document.getElementById('yzm');
							var str = "<input type='text' id='imgCode' style='width:50px'>"+"<img src='<%=basePath%>images/yzm.jpg' >"+
							"<input type='button' id='zhixing' name='' value='开始同步' onclick='javascript:getBatchFeeSynchronization();'>";
							yzm.innerHTML = str;
							document.getElementById('hint').style.display='block';
						}else{
							var yzm = document.getElementById('yzm');
							var str = "<input type='button' id='zhixing' name='' value='开始同步' onclick='javascript:getBatchFeeSynchronizationNoImgCode();'>";
							yzm.innerHTML = str;
						}
					}
			}
		}
		
		document.getElementById('yzm').style.display='block';
		document.getElementById('batch-syn').style.display='none';
		xmlPreSynHttp.send(null);
	}
	
	function createXMLxBatchSynHttpRequest() {	
		if (window.ActiveXObject) {
			xmlBatchSynHttp = new ActiveXObject("Microsoft.XMLHTTP");
		} else if (window.XMLHttpRequest) {
			xmlBatchSynHttp = new XMLHttpRequest();
		}
	 }
	 
	 function getBatchFeeSynchronizationNoImgCode(){
	 	document.getElementById('zhixing').disabled = true;
		createXMLxBatchSynHttpRequest();
		var valCode = 0;
		var url = "/tech/patent/fee.do?method=getBatchFeeSynchronization&valCode="+valCode;
		xmlBatchSynHttp.open("GET", url, true);	
		var showProgress = document.getElementById("showProgress");
		var oldSize = 0;
		xmlBatchSynHttp.onreadystatechange = function batchFeeSynchronizationCallback(){
			if(xmlBatchSynHttp.readyState > 2) {
						var result = xmlBatchSynHttp.responseText.substring(oldSize);
						oldSize = xmlBatchSynHttp.responseText.length;
						var words = new Array();
						words = result.split('...');
						// console.log(words);
						
						if(result!='undefined'&&result!=''){
							showProgress.innerHTML = words[0];
							}
						document.getElementById('yzm').style.display='none';
						document.getElementById('hint').style.display='none';
						document.getElementById('showProgress').style.display='block';
					}
			if (xmlBatchSynHttp.readyState == 4) {
					if (xmlBatchSynHttp.status == 200) {
						alert("批量同步成功");
						document.getElementById('batch-syn').style.display='block';
						document.getElementById('showProgress').style.display='none';
						for (i = 1; i <= 300; i++) {
                            window.setTimeout("update( " + i + ") ", i * 1000);
                            setCookie();
                        }
					}else{
						alert("批量同步失败");
						document.getElementById('batch-syn').style.display='block';
					}
					}
		};
		xmlBatchSynHttp.send(null);
	 }
	 		
	 function getBatchFeeSynchronization(){	
	    document.getElementById('zhixing').disabled = true;
	    var valCode = document.getElementById('imgCode').value;
	    if(valCode.length != 4){
	    alert("验证码输入长度有误！");
	    return;
	    }
		createXMLxBatchSynHttpRequest();
		var url = "/tech/patent/fee.do?method=getBatchFeeSynchronization&valCode="+valCode;
		xmlBatchSynHttp.open("GET", url, true);	
		var showProgress = document.getElementById("showProgress");
		var oldSize = 0;
		xmlBatchSynHttp.onreadystatechange = function batchFeeSynchronizationCallback(){
			if(xmlBatchSynHttp.readyState > 2) {
						var result = xmlBatchSynHttp.responseText.substring(oldSize);
						oldSize = xmlBatchSynHttp.responseText.length;
						showProgress.innerHTML = result;
						var words = new Array();
						words = result.split('...');
						if(result!='undefined'&&result!=''){
							showProgress.innerHTML = words[0];
							}
						
						document.getElementById('yzm').style.display='none';
						document.getElementById('hint').style.display='none';
						document.getElementById('showProgress').style.display='block';
					}
			if (xmlBatchSynHttp.readyState == 4) {
					if (xmlBatchSynHttp.status == 200) {
						alert("批量同步成功");
						document.getElementById('batch-syn').style.display='block';
						document.getElementById('showProgress').style.display='none';
						
					}else{
						alert("批量同步失败");
						document.getElementById('batch-syn').style.display='block';
					}
					}
					
 
     
		};
		xmlBatchSynHttp.send(null);
		
		    
	}

	
     function update(num) {
            var secs = 300;
            if (num >= secs) {
                $("#batch-syn").attr("disabled", false);
            }
            else {
                printnr = secs - num;
                $("#batch-syn").val("还剩" + printnr + "秒");
                $("#batch-syn").attr("disabled", true);
            }
            }
            //页面加载时判断发送按钮5分钟倒计时是否已到
        $(document).ready(function () {
            var date = new Date();
            var msNow = date.getTime();
            var msBefore = getCookie("batch-synTime");
            console.log(msBefore);
            if (msBefore != null) {
                var msMul = msBefore - msNow;
                if (msMul > 0) {
                    for (var i = 1, j = parseInt(300 - msMul / 1000); j <= 60 * 5; i++, j++) {
                        window.setTimeout("update( " + j + ") ", i * 1000);
                    }
                }
            }
        });
        //获取cookie
        function getCookie(name) {
            var prefix = name + "="
            var start = document.cookie.indexOf(prefix)
            if (start == -1) {
                return null;
            }
            var end = document.cookie.indexOf(";", start + prefix.length)
            if (end == -1) {
                end = document.cookie.length;
            }
            var value = document.cookie.substring(start + prefix.length, end)
            return unescape(value);
        }
        //设置cookie
        function setCookie(){
            if (!navigator.cookieEnabled) {
                alert('不Cookie项!');
            }
            else {
                var date = new Date();
                date.setTime(date.getTime() + 60000 * 5);//5分钟过期
                document.cookie = 'batch-synTime=' + escape(date.getTime()) + ';expires=' + date.toGMTString() + ';path=/' + ';domaim=null' + ':secure';
            }
        }   

		

	

	</script>
	
	<body>
	<html:form method="POST" styleId="feeForm"  action="fee.do" target="_self" >
	<input type="hidden" name="method" value="<%=towards %>"/>
						<table width="100%" border="0" cellspacing="0" cellpadding="4">
					  		<tr>
						  		<td class="font_title14" height="32" background="<%=basePath%>images/top_BG.jpg">&nbsp;&nbsp;&nbsp;&nbsp;
									    年费查询
								</td>
							</tr>
						</table>
						
						<table width="100%" border="0" cellspacing="0" cellpadding="4">
							<tr>
							    <td>
											<table border="0" cellspacing="0" cellpadding="4">
											  <tr>
											  	<td align="left" valign="middle">
											    	模糊查询:	
											    </td>
											    <td align="left" valign="middle">
											    	<html:text property="name" styleId="fuzzyQuery" styleClass="input"  style="width:120px" onchange="document.getElementById('searchSubmit').click();" onkeypress="if(event.keyCode==13) {document.getElementById('searchSubmit').click();return false;}"/>
												</td>											
												<td align="left" valign="middle">
											    	&nbsp;&nbsp;剩余时间:
											    	从<html:text property="belowDays" styleClass="input"  style="width:20px" onchange="document.getElementById('searchSubmit').click();" />天&nbsp; <strong>到</strong>&nbsp;
											    	<html:text property="aboveDays" styleClass="input"  style="width:20px" onchange="document.getElementById('searchSubmit').click();" />天
												</td>	
												
												<%
												 	 if(sign.equals("Admin")){
												%>
												<td align="left" valign="middle">&nbsp;&nbsp;科研组:&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
													<html:select property="collegeId" styleClass="iselectS" style="width:100px" onchange="document.getElementById('searchSubmit').click()">
													     <html:option value="">请选择</html:option> 
													     <html:option value="-1">**无科研组**</html:option> 
													     <html:options collection="kjGroup"
													                      property="groupId"
													                      labelProperty="groupName" />
													  </html:select> 
												</td>
												<%} %>
												
												<td align="left" valign="middle">&nbsp;&nbsp;放弃专利：
													<html:select property="isGiveUp" styleClass="iselectS" style="width:100px" onchange="document.getElementById('searchSubmit').click()">
													     <html:option value="">全部</html:option> 
													     <html:option value="0">未放弃</html:option> 
													     <html:option value="1 or 4">已放弃</html:option> 
													     <html:option value="1">已放弃（有确认书）</html:option> 
													     <html:option value="3">驳回放弃</html:option> 
													     <html:option value="4">已放弃（无确认书）</html:option> 
													</html:select>
												</td>
												
												<% if(fromOffice!=null&&fromOffice.equals("true")){
												%>
												<td align="left" valign="middle">&nbsp;&nbsp;是否已缴费
													<html:select property="isPaid" styleClass="iselectS" style="width:100px" onchange="document.getElementById('searchSubmit').click()">
													     <html:option value="">全部</html:option> 
													     <html:option value="0">未缴费</html:option> 
													      <html:option value="1">已缴费</html:option> 
													</html:select>
												</td>
												<%} %>
												
												<td align="left" valign="middle" >
													&nbsp;&nbsp;&nbsp;专利类型:&nbsp;
													<html:multibox property="type" value="1" ></html:multibox>发明&nbsp;&nbsp;
													<html:multibox property="type" value="2" ></html:multibox>实用新型&nbsp;&nbsp;
													<html:multibox property="type" value="3" ></html:multibox>外观设计
												</td>
												
												</tr>
												
												<tr>
												 <td align="center" valign="middle">
											    	截止日期：
											     </td>
											    <td align="center" valign="middle" >
													<html:text property="expiryDay" style="float:left;width:110px"  onclick="WdatePicker()" readonly="readonly"  styleClass="Wdate" />												    
												</td>
												<td align="left" valign="middle" >&nbsp;&nbsp;到&nbsp;&nbsp;<html:text property="expiryDayTo" style="width:110px"  onclick="WdatePicker()" readonly="readonly"  styleClass="Wdate" />
												</td>
												
												
												
												<td align="left" valign="middle">
											    	&nbsp;&nbsp;过程状态：
											  		<html:select property="state" styleId="state" styleClass="iselectS" style="width:100px"  onchange="document.getElementById('searchSubmit').click()">
														<html:option value="">全部</html:option> 
													    <logic:iterate id="patentState" name="patentStateList">
													    		<html:option  value="${patentState.code}">${patentState.name}</html:option>
														</logic:iterate>
													</html:select>
												</td>
												
												<td align="left" valign="middle">&nbsp;&nbsp;经费卡绑定：
													<html:select property="cardBinding" styleClass="iselectS" style="width:100px" onchange="document.getElementById('searchSubmit').click()">
													     <html:option value="">全部</html:option> 
													     <html:option value="0">未绑定</html:option> 
													     <html:option value="1">绑定中</html:option> 
													     <html:option value="2">已绑定</html:option> 
													</html:select>
												</td>
												
												<td  align="left" colpan="2">&nbsp;&nbsp;
											   		<input type="submit" id="searchSubmit" value="查询" class="ButtonHeightWidth">
											   		<input type="button" value="重置" class="ButtonHeightWidth" onclick="resetForm()">
											   		<%
												 	 //if(sign.equals("User")||sign.equals("Manager")){
												 	 %>
											   		<!-- <input type="submit" id="export-submit" value="导出" class="ButtonHeightWidth"> -->
											   		 <%//}
												 	 if(sign.equals("Admin") && (userbean.getIsMaintainB() || userbean.getIsPatentB())){
												 	 %>
											   		<input type="button" id="confirm-submit" value="导出缴费" class="ButtonHeightWidth">
											   		&nbsp;<input type="button" id="giveupbutton" value="强制放弃" class="ButtonHeightWidth" onclick="javascript:showSelect('<%=basePath%>sys/mail.do?method=gotoSendList&doGiveup=true');">
											   		<%} %>
											   	</td>
											  </tr>
											  <tr>
											    <td align="left" valign="middle" >	
											        自
											    </td>
											    <td>
											    <script language="javascript" type="text/javascript" src="<%=basePath%>/js/DatePicker/WdatePicker.js"></script>					
					                            <html:text property="sendMailDate" style="float:left" onclick="WdatePicker()" readonly="readonly"  styleClass="Wdate"/>
											    </td>
											    <td>
											        至今日&nbsp;&nbsp;&nbsp;&nbsp;
												至少发送邮件数：<html:text property="sendMailNum" styleId="sendMailNum" styleClass="input"  style="width:50px" />
											    </td>
											    <td id="hint" style= "display:none">
											    <div style="text-align:right;">请输入验证码：</div>
											    </td>
											    <td>
											    	<div id="yzm" style= "display:none" width="150px"> </div>
											    	<div id="showProgress" style= "display:none"> </div>
											    	<input type="button" id="batch-syn" value="批量同步年费" class="ButtonHeightWidth" onclick="javascript:getPreLogin();">
											    </td>
											  </tr>
											</table>
						</td>
						
				  	 <td>
				 	<%
				 	if((sign.equals("Admin")||sign.equals("Manager"))&&(fromOffice==null||fromOffice=="")){
				 	 %>
					<fieldset width="100%">
					<table border="0" cellspacing="4" height="34" cellpadding="1" background="<%=basePath%>images/main_smallmenu_BG.jpg">
					  <tr>
					  	<td width="20" align="right"><a href='javascript:showSelect("<%=basePath%>sys/mail.do?method=gotoSendList");' target="_blank">
					  	<img src="<%=basePath%>images/kj_icon_document_create.jpg" border="0"/></a></td>
					    <td width="50" align="left"><a href='javascript:sendMails();'>发消息</a></td>
					  </tr>
					</table>
					<table border="0" cellspacing="4" height="34" cellpadding="1" background="<%=basePath%>images/main_smallmenu_BG.jpg">
					  <tr>
					    <td width="20" align="right"><a href="<%=path %>/patent/fee/auditLog.jsp" target="_blank">
					    <img src="<%=basePath%>images/kj_icon_document_delete.jpg" border="0" height="20px" width="20px"/></a></td>
    					<td width="50" align="left"><a href="<%=path %>/patent/fee/auditLog.jsp" target="_blank" >无效日志</a></td>
					  </tr>
					</table>
					</fieldset>
					<%} %>
				       </td>
		  
				  </tr>
			   </table>
		   </html:form> 
	
		   <table width="100%" border="0" cellspacing="0" cellpadding="0">
			  <tr bgcolor="#FFFFFF">
			    <td class="font_title14" height="32" background="<%=basePath%>images/top_BG.jpg">&nbsp;&nbsp;&nbsp;&nbsp;
			    	<%if(towards.equals("showRemind")){ %>
			    		专利年费列表
			    	<%}else{ %>
			    		专利官费列表
			    	<%} %>
			    </td>
			  </tr>
		   </table>
			<input type="hidden" id="selectedItems" name="selectedItems" value="${selectedItems }"/>
			<input type="hidden" id="unSelectedItems" name="unSelectedItems" value="${unSelectedItems }"/>
			<input type="hidden" id="tableIds" name="tableIds" value="${tableIds}"/>
			<input type="hidden" id="module" name="module" value="patentFee"/>
		   <logic:notEmpty name="patentList">
			  <table class="defaulettable" width="100%"  border="0" cellpadding="4" cellspacing="1" bgcolor="#E8E8E8">
			    <thead>
			      <tr bgcolor="#FFFFFF">
			      <%
				 	if((sign.equals("Admin")||sign.equals("Manager"))&&(fromOffice==null||fromOffice=="")){
				 	 %>
			        <th width="2%" class="table_head" align="center">
					<font style="font-weight: bolder;">邮件</font><br>
					<input type='checkbox' onclick="javascript:selectAll(${PaginalBean.pageSize});" id="checkBoxAll"></th>
					<%}else if(fromOffice!=null&&fromOffice.equals("true")){
					 %>
					 <th align="center" width="5%"><b>专利编号<b></th>
					 <%} %>
			        <th align="center" width="7%"><b>专利号<b></th>
			        <th align="center" width="14%"><b>专利名称</b></th>
			        <th  style="width: 5%; ">专利类型</th>
					<logic:equal name="identity" value="Admin" property="sign" scope="session">
					<th  style="width: 5%; ">第一<br>发明人</th>
					<th  style="width: 5%; ">所属学院</th>
					</logic:equal>
					<th align="center" width="7%"><b>专利状态</b></th>
					<% if(fromOffice!=null&&fromOffice.equals("true")){
					%>
					<th align="center" width="7%"><b>费用类型</b></th>
					<%} %>
					<th align="center" width="4%"><b>剩余<br>天数</b></th>
			      	<th align="center" width="6%"><b>到期日期</b></th>
			      	<th align="center" width="5%"><b>应缴金额</b></th>
			      	<th align="center" width="6%"><b>费用</b></th>
			      	<th align="center" width="5%"><b>同步</b></th>
			      	<th align="center" width="4%"><b>日志</b></th>
			      	<!-- 
			      	<th align="center" width="7%"><b>经费卡</b></th> -->
			      	<%if(towards.equals("showRemind")){ %>
			        <!--<th align="center" width="7%"><b>绑定经费卡</b></th> -->
			        <th align="center" width="7%"><b>联系人电话</b></th>
			        <%} %>
			        <%if((canWrite.equals("true") || "User".equals(sign)) && (towards.equals("showRemind"))){ %> 
			      		<th align="center" width="5%"><b>放弃</b></th>
			      	<%} %>
			      	<th align="center" width="7%"><b>邮件</b><br><font style="font-size: 10px">(邮件封数)</font></th>
			        <th align="center" width="5%"><b>消息</b></th>
			       	<%if("User".equals(sign) || ("Admin".equals(sign) && canWrite.equals("true"))){%>   
			        	<th align="center" width="5%"><b>操作</b></th>
			        <%}%>
			      </tr>
			    </thead>
			    
			    <tbody>
			      <logic:iterate id="thisPatent" name="patentList" indexId="iNum">
			       <%
			          KjPatentFee thisPatentFeeObj = (KjPatentFee)pageContext.getAttribute("thisPatent");
			          KjPatent    patentObj = thisPatentFeeObj.getPatentId();
			          
			          //获取发明人列表
			          List<KjProjectstaff> newProjectStaffList = KjProjectStaffDao.getInstance().searchProjectStaff(patentObj.getPatentId(), 2L, "0");
	   				  List<KjProjectstaff> oldProjectStaffList = KjProjectStaffDao.getInstance().searchProjectStaff(patentObj.getPatentId(), 0L, "0");
	    			  if(newProjectStaffList==null || newProjectStaffList.size()==0) newProjectStaffList = oldProjectStaffList;
				      //获取当前审核状态
				      Long status = patentObj.getKjDictStatus().getStatusId(); 
				      int thisRowSpan = 1;
				      String auditInfo="";
					  if(status==7 || status==9 || (patentObj.getIsGiveup()!=null && patentObj.getIsGiveup()==3L)){     //校级或者院级审核驳回,增加一行显示驳回意见
		          			auditInfo = ParamUtil.getAttribute(request,"auditInfo_"+patentObj.getPatentId());
          			        if(auditInfo.length()>0 && !sign.equals("Firm")){    //如果有驳回意见并且不是事务所用户则加一行显示驳回意见
          			        	thisRowSpan = 2;
          			        }
		          	  }
		          	 %>
			       <%
			      	KjPatentFee thisPatentObj = (KjPatentFee)pageContext.getAttribute("thisPatent");		  
			      	%>
				        <tr bgcolor="#FFFFFF">
				       		<%
				 	if((sign.equals("Admin")||sign.equals("Manager"))&&(fromOffice==null||fromOffice=="")){
				 	 %>
				        	  <td class="table_td2" align="center" rowspan="<%=thisRowSpan %>">
								<input type='checkbox' name="checkbox" onclick="setIds(${PaginalBean.pageSize});" id="checkbox_<%=iNum %>" value="${thisPatent.patentId.patentId}">
							  </td><%}else if(fromOffice!=null&&fromOffice.equals("true")){ %>
							  <td align="center" rowspan="<%=thisRowSpan %>">${thisPatent.patentId.schoolNo}</td>
							  <%} %>
					          <td align="center" class="fuzzySearch" rowspan="<%=thisRowSpan %>">					    
					          ${thisPatent.patentId.patentNo}
					          </td>
					         <td align="center" rowspan="<%=thisRowSpan %>" ><a id="checkName_${thisPatent.patentId.patentId}" href="<%=path %>/patent/patent.do?method=detailPatent&patentId=${thisPatent.patentId.patentId}&newDetail=true" target="_blank" class="fuzzySearch">
									<%if(thisPatentObj.getPatentId().getNewName() != null && thisPatentObj.getPatentId().getNewName()!=""){%>
											  ${thisPatent.patentId.newName}
									<%}else{%>${thisPatent.patentId.name}
									<%} %></a>
							</td>
					        <td align="center"><tech:dicTranslate key="${thisPatent.patentId.type}" dicType="DIC.PATENT.TYPE"/></td>	
								<logic:equal name="identity" value="Admin" property="sign" scope="session">				
								
								<td align="center">			          
		                          <%
		                            String firstInventorId = "";
		                            String firstInventorName = "";
		                          	if(newProjectStaffList!=null && newProjectStaffList.size()>0)
									{
										firstInventorId = newProjectStaffList.get(0).getStaffId();
										firstInventorName = newProjectStaffList.get(0).getStaffName();
										//判断是否外校人员 
									    if(firstInventorId.length()==6 && EmployeeDAO.getInstance().findByPk(firstInventorId)!=null)
									        ;
									    else firstInventorName = firstInventorName + "(外校)";
								    }
								    
								     
								    %>						  
								   <a href="<%=basePath%>//user/kjUser.do?method=showUser&staffId=<%=firstInventorId%>" target="_blank" class="fuzzySearch">
								   <%=firstInventorName %></a>
								  
					            </td>
								
                                <td align="center">                              
                                  <%
								    if(thisPatentObj.getPatentId().getDeptId()!=null)
								    {
								    	out.print(KjGroupDAO.getInstance().findByPk(Long.parseLong(thisPatentObj.getPatentId().getDeptId())).getGroupName());
								    }
								    else{
								    	out.print(thisPatentObj.getPatentId().getDeptName());
								    }
								  %>
                                </td>    
                                </logic:equal>   
							 <td align="center">${thisPatent.patentId.stateName}</td>
							 <% if(fromOffice!=null&&fromOffice.equals("true")){
							 %>		
							 <td align="center">${thisPatent.configId.name}</td>
							 <%} %>
							 <%if(thisPatentObj.getIsPaid()==1){ %>
							 <td align="center" >已缴费</td>
							 <%}else{ %>			
							  	<%if(thisPatentObj.getRemainDays()<=90&&thisPatentObj.getRemainDays()>60){%>
							  	<td align="center" ><b class="m60l90">
							  	<%=thisPatentObj.getRemainDays() %></b>
							  	</td><%} %>
							  	<%if(thisPatentObj.getRemainDays()<=60&&thisPatentObj.getRemainDays()>30){%>
							  	<td align="center" ><b class="m30l60">
							  	<%=thisPatentObj.getRemainDays() %></b>
							  	</td><%} %>
							  	<%if(thisPatentObj.getRemainDays()<=30&&thisPatentObj.getRemainDays()>15){%>
							  	<td align="center" ><b class="m7l30">
							  	<%=thisPatentObj.getRemainDays() %></b>
							  	</td><%} %>
							  	<%if(thisPatentObj.getRemainDays()<=15){%>
							  	<td align="center" ><b class="less15">
							  	<%=thisPatentObj.getRemainDays() %></b>
							  	</td><%} %>
							  	<%if(thisPatentObj.getRemainDays()>90){%>
							  	<td align="center" >
							  	<%=thisPatentObj.getRemainDays() %>
							  	</td><%}}%>					  								  	
								<td align="center">								
					          	  <bean:write name="thisPatent" property="expiryDay" scope="page" format="yyyy-MM-dd"/>
					            </td>
					           
					            <td align="center">		
					         <% if(fromOffice!=null&&fromOffice.equals("true")){
							 %>	 
							 		${thisPatent.amount}    
							 <%}else{ 
									Double money = KjPatentFeeDao.getInstance().findMoneyNoPayByDate(thisPatentObj.getPatentId().getPatentId(), thisPatentObj.getExpiryDay());
									out.print(money);
					            } %>
					            </td>
					        
							  	<td align="center"><a href="fee.do?method=showAllFee&patentId=${thisPatent.patentId.patentId}&type=${thisPatent.patentId.type}" class=" btn btn-info" target="_blank">费用</a>
							  	<br><br>
							  	</td>	
							  	<%
						          	patentObj = thisPatentObj.getPatentId();
						        %>  	
						        <td><a href="javascript:getFeeSynchronization('${thisPatent.patentId.patentId}','${thisPatent.patentId.patentNo}');" class=" btn btn-info" >同步</a>
						        <br><br>
						        <font style="font-size: 10px"><%
						        //String syncDate =  KjAuditLogDAO.getInstance().getLatestLogDate(15l,patentObj.getPatentId(),"同步");
						        //if(syncDate==null || syncDate.length()==0) syncDate = "--";
						        //out.print(syncDate);
						        %></font>
						        </td>
							  	<td align="center">
							  		<a href="<%=path %>/apply/audit.do?method=showRejectList&objtype=15&objId=${thisPatent.patentId.patentId}" target="_blank">日志</a>
							  	</td>
							  	<%if(towards.equals("showRemind")){ %>
							  	<!--  <td align="center">-->
						          	<%
						          	//if(patentObj.getFundCardNo()!=null && patentObj.getFundCardNo().length()>0){ %>
						          		<!--已绑定-->
						          	<%//}else if(patentObj.getFundsPeople()!=null){ %>
						          		<!--绑定中-->
						          	<%//}else{ %>
						          		<!--未绑定-->
						          	<%//} %>
					          		<!--<a href="patent.do?method=bindFundCard&patentId=${thisPatent.patentId.patentId}" class=" btn btn-info" target="_blank">绑定</a>
								  </td>-->
								  <%
								      String contactPhone = "";
								      List<KjMail>  mailList = KjMailDAO.getInstance().searchMailByTypeAndId("patentFee",patentObj.getPatentId().toString());patentObj.getContactPhone();
								      //获取一个联系手机号码（按联系人、第一发明人、第二发明人依次查询，直到找到一个为止）
								      contactPhone = patentObj.getContactPhone();
								      if(contactPhone == null || contactPhone.length()<8)
								      {
	    									String tempPhone = "";
											if(newProjectStaffList!=null)
											{
											    for (int j = 0; j < newProjectStaffList.size(); j++) 
											    {
											    	tempPhone = newProjectStaffList.get(j).getKjUser().getCellPhone();
											    	if(tempPhone != null && tempPhone.length()>8)
											    	{
											    		contactPhone = tempPhone;
											    		break;
											    	}
												}
											 }
								      }
								      if(contactPhone == null) contactPhone = "";
								      
								      Map resultMap = KjPatentFeeDao.getInstance().getRemainDayAndExpireDayByPatentId(patentObj.getPatentId());
								      Long remainDays = (Long) resultMap.get("remainDays");
								      if(mailList.size()>=2 && remainDays<=60)
								      {
								  %>
								  <td align="center" >
								      <a name="blinkObj" ><%=contactPhone%> </a>   
					        	  </td>
                                  <%}else{ %>
                                   <td align="center" >
								     <%=contactPhone%>   
					        	  </td>							
								 <%}} %>
							<!-- 专利放弃和恢复 -->	  
							<%
							
							String giveUpChn = "";
							if((canWrite.equals("true") || "User".equals(sign)) && (towards.equals("showRemind"))){ %>
					          <td align="center"><%=patentObj.getIsGiveupShortChn() %> </td>	  
						  	  <%} %>
						  	  
						  	  <td align="center"><a href="patent.do?method=showMail&patentId=${thisPatent.patentId.patentId}&type=patentFee" target="_blank">
						  	  	<%
						  	  	Long lastMailDate = KjMailDAO.getInstance().getLastMailDate("patentFee", patentObj.getPatentId().toString());
						  	  	if(lastMailDate!=null){ %><strong>
						  	  	<%=lastMailDate%></strong>天前<%}else{ %>无<%} %></a>
						  	  	<br>
						  	  	 
				          	 <%
				          	 try{
					 		  	  int mailNum = KjPatentFeeDao.getInstance().getThisYearFeeMailCount(patentObj);
					 		  	  if(mailNum!=0){
					 		  	  request.setAttribute("mailNum",mailNum);
					 		  	  }else{
					 		  	  request.setAttribute("mailNum0",mailNum);  
					 		  	  }
					 		  	  }catch(Exception e){
					 		  		out.print("0");
					 		  	  }
					 		  	  %>
				          	  <a href="patent.do?method=showMail&patentId=${thisPatent.patentId.patentId}&type=patentFee&date=${datePayTemp }" target="_blank">
				 		  	  <%
				 		  	  
				 		  	  if(KjMailDAO.getInstance().searchMailByTypeAndId("patentFee",patentObj.getPatentId().toString())!=null)
				 		  	  { 
				 		  	  %>
				 		  	  <strong>${mailNum }</strong>  </a> 封
				 		  	  
				 		  	  <%
				 		  	  }
				 		  	  else{ %>0 封
				 		  	  <%
				 		  	  }
				 		  	  %>
						  	  </td>
						  	  
							  <td align="center"><a href="patent.do?method=showNews&patentId=${thisPatent.patentId.patentId}" target="_blank">进入</a></td>
							  
							  <td align="center">
							  <%if(("Admin".equals(sign) && canWrite.equals("true")  )){  //校级用户进行修改的情形%>
							  
								  	<a href="patent.do?method=updatePatent&patentId=${thisPatent.patentId.patentId}" target="_blank"><img border="0" src="../images/kj_icon_document_update.gif"/></a> 
									<a href="patent.do?method=deletePatent&patentId=${thisPatent.patentId.patentId}&page=${PaginalBean.currentPage}&pageSize=${PaginalBean.pageSize}&fromFee=true" class="delete" target="_self"><img border="0" src="../images/kj_icon_document_delete.jpg"/></a>
							  </td>
							  <%}else if("Manager".equals(sign) || "Admin".equals(sign)||"Firm".equals(sign)){//科研秘书不能修改,且不显示td
							  %>
							  &nbsp;
							  <%}else{   //其他情况只显示空白的td%>                   
								  &nbsp;
							  <%}%>
							 
							  
				        </tr>
				        
				        <%		
				        	if(status==7 || status==9 || (patentObj.getIsGiveup()!=null && patentObj.getIsGiveup()==3L)){ 
				        		if(auditInfo.length()==0){
				        			auditInfo="无意见";
				        		}
				        %>
				        	<tr>
								<td colspan="<%=thisColmn %>" align="left">
									<b>&nbsp;&nbsp;&nbsp;&nbsp;
									<% if(status==7 || status==9){%>审核意见：<%}else{%>放弃专利申请被驳回:<%} %></b> <font color="red" ><%=auditInfo %></font>
								</td>
							</tr>
				        <%		
				        	}
				        %>
				        
			      </logic:iterate>
			    </tbody>
			  </table>
			  
		</logic:notEmpty>
	  	
	  <%if(fromOffice!=null&&fromOffice.equals("true")){ %>	
	    <form target='_self' id="pageSize" method="GET" 
				action="<%=basePath%>patent/fee.do?method=showOffice">
				<input type="hidden" name="method" value="showOffice">
				<%}else{ %>
				<form target='_self' id="pageSize" method="GET" 
				action="<%=basePath%>patent/fee.do?method=showRemind">
				<input type="hidden" name="method" value="showRemind">
				<%} %>
				<input type="hidden" name="Who" value="Who">
				<table width="100%" border="0">
					<tr bgcolor="#FFFFFF">
						<td><br></td>
						<td align="right">
							<b> 共<span class="rednum">${PaginalBean.sumCount}</span>条&nbsp;&nbsp;&nbsp;&nbsp;
								共<span class="rednum">${PaginalBean.pageCount}</span>页&nbsp;&nbsp;&nbsp;&nbsp;
								当前是第<span class="rednum">${PaginalBean.currentPage}</span>页&nbsp;&nbsp;&nbsp;&nbsp;
								<a href="javascript:gotoNext(0,'${PaginalBean.pageSize}');" target='_self'>首页</a>&nbsp;&nbsp;&nbsp;&nbsp; 
								<a href="javascript:gotoNext('${PaginalBean.prePage}','${PaginalBean.pageSize}');" target='_self'>上一页</a>&nbsp;&nbsp;&nbsp;&nbsp;
								<a href="javascript:gotoNext('${PaginalBean.nextPage}','${PaginalBean.pageSize}');" target='_self'>下一页</a>&nbsp;&nbsp;&nbsp;&nbsp; 
								<a onclick="javascript:finalPage('${PaginalBean.pageCount}','${PaginalBean.pageSize}');return false;"
								href=#>尾页</a>&nbsp;&nbsp;&nbsp;&nbsp; </b>
						</td>
						<td width="60" align="center">
							<b>跳转至</b>
						</td>
						<td width="50" align="center">
							<input type="text" size="2" name="toPage" class="toPage" value="">
						</td>
						<td width="50" align="center">
							<input type="button" value="跳转"
								onclick="javascript:goToPage('${PaginalBean.pageSize}');">
						</td>
						<td width="60" align="center">
							<b>每页显示</b>
						</td>
						<td width="50" align="center">
							<input type="text" size="2" name="pageSize" id="pageSize1" class="pageSize"
								value="${pageSize}">
						</td>
						<td width="50" align="center">
							<input type="button" value="提交" onclick="document.getElementById('searchSubmit').click()">
						</td>
						<td width="60" align="center">
							&nbsp;
						</td>
					</tr>
					<tr>
						<td colspan="9" height="100px"> &nbsp;
						</td>
					</tr>
				</table>
		</form>
		
			
	</body>
</html>


<script language="JavaScript" type="text/javascript">
	<%
	List<String> selectedList = (List<String>)request.getAttribute("selectedList");
	List<KjPatentFee> patentList = (List<KjPatentFee>)request.getAttribute("patentList");
	//System.out.println(selectedList.size()+"  "+patentList.size());
	if(patentList!=null && selectedList!=null){
		for(int i=0; i<patentList.size();i++){
			KjPatentFee   kjPatentFee = (KjPatentFee)patentList.get(i);
			if(selectedList.contains(kjPatentFee.getPatentId().getPatentId().toString())){
				//System.out.println("kjPatentFee.getId() " + kjPatentFee.getId());	
		%>
				if(document.getElementById("checkbox_<%=i%>")){
					document.getElementById("checkbox_<%=i%>").checked = true;
				}
		<%
			}
		}
	} %>
	        var xmlHttp = false;
	
			//用于放弃和恢复专利的功能
			 
			 function createXMLHttpRequest() {
				if (window.ActiveXObject) {
					xmlHttp = new ActiveXObject("Microsoft.XMLHTTP");
				} else if (window.XMLHttpRequest) {
					xmlHttp = new XMLHttpRequest();
				}
			 }
			 
		     function giveUpAjax(patentId, state){
				var reason = document.getElementById("reason"+state+patentId).value;
				if(reason!=null && reason.length>0){
					reason = encodeURIComponent(encodeURIComponent(reason));
				}else{
					reason = "";
				}
				
				var isGiveUp = 0;
				var obj = document.getElementById('isGiveUp'+patentId);
				if(obj.options[0].selected == true)
				{	
					alert('请选择放弃类型');
					return ;
				}
				else if(obj.options[1].selected == true)
				{	
					isGiveUp = 4;
				}
				else if(obj.options[2].selected == true)
				{	
					isGiveUp = 1;
				}
				
				createXMLHttpRequest();
				var url = "/tech/patent/patent.do?method=giveUpAjax&patentId="+patentId+"&reason="+reason+"&giveUp=1&time="+(new Date())+"&isGiveUp="+isGiveUp;
				//alert(url);
				xmlHttp.open("GET", url, true);
				xmlHttp.onreadystatechange = giveUpCallback;
				xmlHttp.send(null);
			}
			
			function refuseAjax(patentId, state){
				var reason = document.getElementById("reason"+state+patentId).value;
				if(reason!=null && reason.length>0){
					reason = encodeURIComponent(encodeURIComponent(reason));
				}else{
					reason = "";
				}
				createXMLHttpRequest();
				var url = "/tech/patent/patent.do?method=giveUpAjax&patentId="+patentId+"&reason="+reason+"&giveUp=3&time="+(new Date());
				//alert(url);
				xmlHttp.open("GET", url, true);
				xmlHttp.onreadystatechange = giveUpCallback;
				xmlHttp.send(null);
			}
			
			function resumeAjax(patentId){
				createXMLHttpRequest();
				//alert(patentId);
				var url = "/tech/patent/patent.do?method=giveUpAjax&patentId="+patentId+"&giveUp=0&time="+(new Date());
				//alert(url);
				xmlHttp.open("GET", url, true);
				xmlHttp.onreadystatechange = giveUpCallback;
				xmlHttp.send(null);
			}
			
			function giveUpCallback(){
				if (xmlHttp.readyState == 4) {
					if (xmlHttp.status == 200) {
						var status = xmlHttp.responseXML.getElementsByTagName("status")[0].firstChild.data;
						var reason = xmlHttp.responseXML.getElementsByTagName("reason")[0].firstChild.data;
						var patentId = xmlHttp.responseXML.getElementsByTagName("patentId")[0].firstChild.data;
						//alert(reason);
						if(status == 0){
							$('.giveUp'+patentId).show();
							$('.resume1'+patentId).hide();
							$('.resume2'+patentId).hide();
							$('.resume3'+patentId).hide();
							$('.text'+patentId).hide();
						}else if(status==1){
							$('.giveUp'+patentId).hide();
							$('.resume1'+patentId).show();
							$('.resume2'+patentId).hide();
							$('.resume3'+patentId).hide();
							$('.reasonToShow1'+patentId).html("已放弃(有确认书),"+reason);
						}else if(status==4){
							$('.giveUp'+patentId).hide();
							$('.resume1'+patentId).show();
							$('.resume2'+patentId).hide();
							$('.resume3'+patentId).hide();
							$('.reasonToShow1'+patentId).html("已放弃(无确认书),"+reason);
						}else if(status==2){
							$('.giveUp'+patentId).hide();
							$('.resume1'+patentId).hide();
							$('.resume2'+patentId).show();
							$('.resume3'+patentId).hide();
							$('.reasonToShow2'+patentId).html("申请放弃,"+reason);
						}else if(status==3){
							$('.giveUp'+patentId).hide();
							$('.resume1'+patentId).hide();
							$('.resume2'+patentId).hide();
							$('.resume3'+patentId).show();
							$('.reasonToShow3'+patentId).html("已驳回,"+reason);
						}else if(status==10){
							alert("专利已经放弃,无法恢复!");
						}
					}
				}
			}
			
			function giveUpPatent(patentId){
				$('.text'+patentId).show();
			}
			function abortGiveUp(patentId){
				$('.text'+patentId).hide();
			}
			function refuseGiveUp(patentId){
				$('.text4'+patentId).show();
			}
			function abortRefuse(patentId){
				$('.text4'+patentId).hide();
			}
			
			function setValue(value, patentId){
				$('.input'+patentId).val(value);
			}
			
			function setValue4(value, patentId){
				$('.input4'+patentId).val(value);
			}
			
	
			function showSelect(url){
				 var selectedItems = document.getElementById("selectedItems").value;
				 var unSelectedItems = document.getElementById("unSelectedItems").value;
				 var tableIds = document.getElementById("tableIds").value;
				 var module = document.getElementById("module").value;
				 var state = "";
				 url = url + "&selectedItems=" + selectedItems + "&unSelectedItems=" + unSelectedItems+ "&tableIds=" + tableIds +"&module=" + module+"&kjDictStatusId=" + state;
				 window.open (url);
			}
			
			function sendMails(){
				 var selectedItems = document.getElementById("selectedItems").value;
				 var unSelectedItems = document.getElementById("unSelectedItems").value;
				 var tableIds = document.getElementById("tableIds").value;
				 var module = document.getElementById("module").value;
				 var state = "";
				 var url = "";
				 var info = "确认给专利";
				 var Ids = selectedItems.split(",");
				 var name = '';
				 for(var i=0;i<Ids.length;i++)
				 {
				     if(i==0)
				        name = "《" + document.getElementById('checkName_'+Ids[i]).innerText + "》";
				     else 
				        name = name + ",《" + document.getElementById('checkName_'+Ids[i]).innerText + "》";
				  }
				  info = info + name +"的所有人员发送邮件吗？";        
				 
				 
			     if(selectedItems.indexOf(",")>0)
				 {
				     if(window.confirm(info))
		             {
				         url = "<%=basePath%>sys/mail.do?method=sendPatentMailBatch";
				         url = url + "&selectedItems=" + selectedItems + "&unSelectedItems=" + unSelectedItems+ "&tableIds=" + tableIds +"&module=" + module+"&kjDictStatusId=" + state;
				         window.open (url);
				         setTimeout('document.location.reload()',2000);
				      }
				  } 
				 else {url = "<%=basePath%>sys/mail.do?method=gotoSendList";
				       url = url + "&selectedItems=" + selectedItems + "&unSelectedItems=" + unSelectedItems+ "&tableIds=" + tableIds +"&module=" + module+"&kjDictStatusId=" + state;
				       window.open (url);  
		         }
				 
			}
			
			function goToPage(pageSize){
				var toPage = document.all("toPage").value;
				//Integer pageNum = Integer.valueOf(toPage)*Integer.valueOf(pageSize);
				var pageNum = (parseInt(toPage)-1)*parseInt(pageSize);	
				if(!pageNum){
					return;
				}	
				<%if(fromOffice!=null&&fromOffice.equals("true")){ %>	
				var url = "<%=path%>/patent/fee.do?method=showOffice&Who=Who&page="+pageNum+"&pageSize="+pageSize;		
				<%}else{%>	
				var url = "<%=path%>/patent/fee.do?method=showRemind&Who=Who&page="+pageNum+"&pageSize="+pageSize;
				<%}%>
				document.forms[0].action = url;	
				document.forms[0].submit();
			}	
			function turnPageTo( url){
				window.location.href = url;
			}
			
			function gotoNext(pageNum,pageSize){
				var selectedItems = document.getElementById("selectedItems").value;
				var unSelectedItems = document.getElementById("unSelectedItems").value;
				var tableIds = document.getElementById("tableIds").value;
				var module = document.getElementById("module").value;
				
				<%if(fromOffice!=null&&fromOffice.equals("true")){ %>	
					var url = "<%=path%>/patent/fee.do?method=showOffice&Who=Who&page="+pageNum+"&pageSize="+pageSize;		
				<%}else{%>	
					var url = "<%=path%>/patent/fee.do?method=showRemind&Who=Who&page="+pageNum+"&pageSize="+pageSize+"&selectedItems="+
						selectedItems+"&unSelectedItems="+unSelectedItems+"&tableIds="+tableIds;
				<%}%>
				document.forms[0].action=url;
			  	document.forms[0].submit();
			}
			
			function finalPage(pageCount,pageSize){
				var selectedItems = document.getElementById("selectedItems").value;
				var unSelectedItems = document.getElementById("unSelectedItems").value;
				var tableIds = document.getElementById("tableIds").value;
				var module = document.getElementById("module").value;
				
				var pageNum = (parseInt(pageCount)-1)*parseInt(pageSize);
				
				<%if(fromOffice!=null&&fromOffice.equals("true")){ %>	
					document.forms[0].action = "<%=path%>/patent/fee.do?method=showOffice&Who=Who&page="+pageNum+"&pageSize="+pageSize;		
				<%}else{%>
					document.forms[0].action = "<%=path%>/patent/fee.do?method=showRemind&Who=Who&page="+pageNum+"&pageSize="+pageSize+"&selectedItems="+
						selectedItems+"&unSelectedItems="+unSelectedItems+"&tableIds="+tableIds;	
				<%}%>				
				document.forms[0].submit();
			}

			function resetForm()
			{
				document.forms[0].reset();
			}
			//模糊检索的字段高亮显示
			$(document).ready(function() {
				var fuzzyQuery = $("#fuzzyQuery").val();
				$(".fuzzySearch").each(function(){
					//if($(this)[0].innerHTML.indexof(fuzzyQuery) >=0){
						
						var inH= $(this)[0].innerHTML;
						var newInH="<span style='color:Red'>"+fuzzyQuery+"</span>";
						inH=inH.replace(fuzzyQuery,newInH);
						$(this)[0].innerHTML=inH;
					//}
				});
			});
		$("#export-submit").click(function(){	
			$("#feeForm").attr('action',"fee.do?method=exportRemind");
			$("#feeForm").submit();
		});
		$("#searchSubmit").click(function(){
		var pageSize=document.getElementById("pageSize1").value;
			
		<% if(fromOffice!=null&&fromOffice.equals("true")){ %>
		$("#feeForm").attr('action',"fee.do?method=showOffice&pageSize="+pageSize);
		<%}else{ %>
			$("#feeForm").attr('action',"fee.do?method=showRemind&pageSize="+pageSize);
			<%}%>
			$("#feeForm").submit();
		});	

		$("#show-jsp").click(function(){	
			$("#feeForm").attr('action',"fee.do?method=confirmOffice&doUpdate=1");
			$("#feeForm").submit();
		});
		
		$("#confirm-submit").click(function(){
			var dialog = jDialog.dialog({
	    	title : '提示：',
	    	buttonAlign   : 'center',
	    	autoClose   : 0,
	    	content : '确认把'+${PaginalBean.sumCount}+'条数据改为已缴费?',
	    	buttons : [
	        {
	            type : 'highlight',
	            text : '导出并确认缴费',
	            handler : function(){
	            
	           //if(sign.equals("showRemind")){ 
			    		$("#feeForm").attr('action',"fee.do?method=previewYearUpdate");
						$("#feeForm").submit();
			   /*}else{ 
			    		$("#feeForm").attr('action',"fee.do?method=previewYearUpdate");
						$("#feeForm").submit();
			   } */
	            }
	        },
	        {
	            type : 'normal',
	            text : '仅导出',
	            handler : function(button,dialog){
				$("#feeForm").attr('action',"fee.do?method=outputYear");
				$("#feeForm").submit();
	            }
	        }
	        ]
			});
					
		});
		
	//设置文字高亮
	var fn = (function() {
        var blink = document.getElementsByName('blinkObj');
        return function(){   
	        for( var i=0;i<blink.length;i++)
	        {	                   
		       blink[i].style.color = blink[i].style.color=="red"?"black":"red";
		    } 
        }
      })()
      setInterval(fn, 500);
		
		
		//同步专利年费数据---------------------我是华丽的分割线---------------------------------
	//add by xyj 2015/10/26
	var xmlSynHttp = false;
	
	//用于放弃和恢复专利的功能
	 
	 function createXMLxSynHttpRequest() {
		if (window.ActiveXObject) {
			xmlSynHttp = new ActiveXObject("Microsoft.XMLHTTP");
		} else if (window.XMLHttpRequest) {
			xmlSynHttp = new XMLHttpRequest();
		}
	 }
	var canClick = true;
	function getFeeSynchronization(patentId,patentNo){
		if(canClick){
		canClick=false;
		createXMLxSynHttpRequest();
		var url = "/tech/patent/fee.do?method=getFeeSynchronization&patentId="+patentId+"&patentNo="+patentNo;
		//alert(url);
		xmlSynHttp.open("GET", url, true);
		xmlSynHttp.onreadystatechange = feeSynchronizationCallback;
		xmlSynHttp.send(null);
		}
	}
	function feeSynchronizationCallback(){
				if (xmlSynHttp.readyState == 4) {
					if (xmlSynHttp.status == 200) {
						
						alert("同步成功");
					}else{
						alert("同步失败");
					}
					canClick=true;
				}
			}
	//xyj 华丽的分割线-------------------------------------------------------------	
</script>
