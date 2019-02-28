<%@ page language="java" import="java.util.*" pageEncoding="utf-8"%>
<%@page import="inc.tech.persistent.entity.KjPatentFeeConfig"%>
<%@page import="inc.tech.persistent.entity.KjDictionary"%>
<%@page import="inc.tech.persistent.entity.KjUser"%>
<%@page import="inc.tech.persistent.entity.KjPatent"%>
<%@ page import="inc.tech.sys.user.UserBean"%>
<%@ page import="inc.tech.sys.group.dao.MemberDAO"%>
<%@ page import="inc.tech.patent.dao.KjPatentDao"%>
<%@ page import="inc.tech.fund.fundCard.dao.FundCardDao"%>
<%@ page import="inc.tech.patent.util.FeeList"%>
<%@page import="inc.tech.sys.group.dao.KjGroupDAO"%>
<%@page import="inc.tech.persistent.entity.KjDictIdentity"%>
<%@page import="inc.tech.util.ParamUtil"%>
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean"%>
<%@ taglib uri="http://struts.apache.org/tags-html" prefix="html"%>
<%@ taglib uri="http://struts.apache.org/tags-logic" prefix="logic"%>

<%
	String path = request.getContextPath();
	String basePath = request.getScheme() + "://"
			+ request.getServerName() + ":" + request.getServerPort()
			+ path + "/";

	List<KjPatentFeeConfig> feeConfigList = (List<KjPatentFeeConfig>) request.getAttribute("feeConfigList");

	List<KjDictionary> feeTypeList = (List<KjDictionary>) request.getAttribute("feeTypeList");

	String patentId = (String) request.getAttribute("patentId");
	String cardId = "";
	String cardNo = "";
	String fundPeopleName = "";
	String fundPeopleId = "";
	String fundPeopleCollege = "";
	String valCode = "";
	KjPatent kjPatent = null;
	if (patentId != null && patentId.length() > 0) {
		kjPatent = KjPatentDao.getInstance().findByPk(
		Long.parseLong(patentId));
		cardId = kjPatent.getFundCardNo();
		if (cardId != null && cardId.length() > 0) {
			cardNo = FundCardDao.getInstance().findByPk(
			Long.parseLong(cardId)).getFundCardNo();
		}
	}
	KjUser thisUser = (KjUser) request.getAttribute("fundsPeople");
	if (thisUser != null) {
		fundPeopleId = thisUser.getStaffId();
		fundPeopleName = thisUser.getName();
		if (thisUser.getKjCollege() != null
		&& thisUser.getKjCollege() > 0) {
			fundPeopleCollege = KjGroupDAO.getInstance().findByPk(
			thisUser.getKjCollege()).getGroupName();
		}
	}
	String type = (String) request.getAttribute("type");

	//获取当前用户身份  用户id 是否为专利管理组和数据维护组
	UserBean userbean = (UserBean) session.getAttribute("UserBean");
	String sign = ((KjDictIdentity) session.getAttribute("identity"))
			.getSign();
	String canWrite = "";
	//校级用户写权限的判断
	if (MemberDAO.getInstance().GroupHaveMember(
			MemberDAO.PATENT_MAINTAIN_GROUP, userbean.getUid())
			|| MemberDAO.getInstance().GroupHaveMember(
			MemberDAO.MAINTAIN_GROUP, userbean.getUid())) {
		canWrite = "true";
	}
	if((sign.equalsIgnoreCase("Agent") || sign.equalsIgnoreCase("Firm")) 
			&& kjPatent!=null 
			&& kjPatent.getState()!=null 
			&& kjPatent.getState().equals("33"))
	{
		canWrite = "true";
	}
%>
<!DOCTYPE HTML>
<html>
	<head>
		<base href="<%=basePath%>patent/">

		<title>专利费用清单</title>
		<meta http-equiv="pragma" content="no-cache">
		<meta http-equiv="cache-control" content="no-cache">
		<meta http-equiv="expires" content="0">
		<meta http-equiv="keywords" content="keyword1,keyword2,keyword3">
		<meta http-equiv="description" content="This is my page">
		<!--Css+Jquery//-->
		<link rel="StyleSheet" href="<%=basePath%>css/style.css" type="text/css" />
		<link href="<%=basePath%>css/patent/bootstrap.css" rel="stylesheet" type="text/css">
		<script language="JavaScript" type="text/javascript" src="<%=basePath%>js/jquery.pack.js"></script>
		<script language="JavaScript" type="text/javascript" src="<%=basePath%>js/patent/admin.js"></script>
		<script language="JavaScript" type="text/javascript" src="<%=basePath%>js/common.js"></script>
		<script language="javascript" type="text/javascript" src="<%=basePath%>js/DatePicker/WdatePicker.js"></script>
		<style type="text/css">
.toUpdate {
	width: 120px;
	height: 30px;
	background: #00b5e5;
	border: none;
	padding: 5px;
	font-size: 14px;
	font-weight: bold;
}

.update {
	width: 120px;
	height: 30px;
	background: #00b5e5;
	border: none;
	padding: 5px;
	font-size: 14px;
	font-weight: bold;
}


.sync {
	width: 120px;
	height: 30px;
	background: #00b5e5;
	border: none;
	padding: 5px;
	font-size: 14px;
	font-weight: bold;
}
.syncNew {
	width: 120px;
	height: 30px;
	background: #00b5e5;
	border: none;
	padding: 5px;
	font-size: 14px;
	font-weight: bold;
}

 #box_relative
        {
            position: relative;
            left: 10px;
            top: 10px; background-color: gray;z-index:-1;
        }
        
.stop{
	display:inline-block;
	width:100px;
	color:white;
	background:#e8543f;
	text-align:center; 
	left: 10px;}
</style>
		<!--Css+Jquery//-->
	</head>
	<script language="JavaScript">
		var xmlHttp = false;
		function createXMLHttpRequest() {
			if (window.ActiveXObject) {
				xmlHttp = new ActiveXObject("Microsoft.XMLHTTP");
			} else if (window.XMLHttpRequest) {
				xmlHttp = new XMLHttpRequest();
			}
		}
		
		
		if ('<html:errors property="infor"/>'!="")
			alert('<html:errors property="infor"/>');
			
			
		function  submitForm()
		{
			$('.feeForm').submit();
			document.all("submitButton").disabled=true;
		}
		
		function isDigit(s){ 
			var patrn=/^[0-9]{1,6}(\.[0-9]{0,2})?$/;
			if (!patrn.exec(s)){
		 		return "";
		 	}
		 	var chkObjs = document.getElementsByName("pay_12");
               for(var i=0;i<chkObjs.length;i++){
                   if(chkObjs[i].checked){
                       if(chkObjs[i].value=="0"){
                       		document.getElementById("state").value = "91";
                       		alert("有未缴费的滞纳金,专利自动变为滞纳状态!");
                       }
                   }
            }
			return s;
		}
		
		function zhina(name){
			if(document.getElementById("fee_12") && document.getElementById("fee_12").value.length>0){
				document.getElementById("state").value = "91";
				alert("有未缴费的滞纳金,专利自动变为滞纳状态!");
			}
		}
		function gotoUpdate(){			
			//var myTr= document.getElementById("selectPeole"); myTr.style.display="block";
			$('.modify').removeAttr("disabled");
			$('.isPay').removeAttr("disabled");
			$('.update').show();
			$('.toUpdate').hide();
		
			
		}
		function gotoCancel(){
			window.location="<%=path%>/patent/fee.do?method=showAllFee&patentId="+<%=patentId%>+"&type="+<%=type%>;
		}
		function update(){
			var radio = false;
			var input = false;
			var chkObjs = document.getElementsByName("pay_12");
            for(var i=0;i<chkObjs.length;i++){
                   if(chkObjs[i].checked){
                       if(chkObjs[i].value=="0"){
                       		radio = true;
                       }
                   }
            }
            if(document.getElementById("fee_12") && document.getElementById("fee_12").value.length>0){
            	input = true;
            }
            var state = document.getElementById("state").value;
            if(radio && input && state!="91"){
            	alert("有未缴费的滞纳金,请更改专利过程状态!");
            }else{
				$('.feeForm').submit();
			}
		}
		
		function cancelCard(){
				document.getElementById("fundPeopleName").value = "<%=fundPeopleName %>";
				document.getElementById("fundPeopleId").value = "<%=fundPeopleId %>";
				document.getElementById("fundPeopleCollege").value = "<%=fundPeopleCollege %>";
				document.getElementById("cardNo").options.length = 0;
				document.getElementById("cardNo").options.add(new Option("<%=cardNo %>", "<%=cardNo %>"));
		}
		
		function editFundCardAjax(){
			createXMLHttpRequest();
			var staffId = document.getElementById("leaderId").value;
			if(staffId!=null && staffId.length>0){
				var url = '/tech/Fund/fundCard.do?method=EditFundCardAjax&staffId='+staffId+'&time='+(new Date());
				//alert(url);
				xmlHttp.open("POST", url, true);
				xmlHttp.onreadystatechange = editCallBack;
				xmlHttp.send(null);
			}
		}
		
		function editCallBack(){
			if (xmlHttp.readyState == 4) {
				if (xmlHttp.status == 200) {
					var num = xmlHttp.responseXML.getElementsByTagName("num")[0].firstChild.data;
					if(num>0){
						var fundPeopleName = xmlHttp.responseXML.getElementsByTagName("fundPeopleName")[0].firstChild.data;
						var fundPeopleId = xmlHttp.responseXML.getElementsByTagName("fundPeopleId")[0].firstChild.data;
						var fundPeopleCollege = xmlHttp.responseXML.getElementsByTagName("fundPeopleCollege")[0].firstChild.data;
						if(fundPeopleName!=null){
							document.getElementById("fundPeopleName").value = fundPeopleName;
						}
						if(fundPeopleId!=null){
							document.getElementById("fundPeopleId").value = fundPeopleId;
						}
						if(fundPeopleCollege!=null){
							document.getElementById("fundPeopleCollege").value = fundPeopleCollege;
						}
						
						var cardList = xmlHttp.responseXML.getElementsByTagName("cardList")[0].firstChild.data;
						var cardArray = cardList.split(";");
						//首先remove当前经费卡
						var len =  document.getElementById("cardNo").options.length;
						if(len==1){
							removeOption();
						}
						for(var i=0; i<cardArray.length-1; i++){
							document.getElementById("cardNo").options.add(new Option(cardArray[i], cardArray[i]));
						}
					}else{
						alert("当前选定用户无可用经费卡!");
					}
					
					//window.location.reload();			
				}
			}
		}
		
		function addOption(name){
				
				document.getElementById("agentFirmId").options.add(new Option(name+"(未审核)","0"));
				document.getElementById("newAgentPersonFirm").options.add(new Option(name+"(未审核)","0"));
				document.getElementById("agentFirmId").value = "0";
				document.getElementById("newAgentPersonFirm").value = "0";
				document.getElementById("agentPersonId").options.length = 0;
				$('#firmIntro').html("&nbsp;&nbsp;&nbsp;&nbsp;");
		}
		
		function removeOption(){
			var len =  document.getElementById("cardNo").options.length;
			document.getElementById("cardNo").options.remove(len-1);
			//document.getElementById("agentFirmId").value = "";
		}
		
		function refMoney(money, id){
			document.getElementById(id).value=money;
		}
		
		$(document).ready(function() {
			$(".modify").each(function(){
				if($(this).val()==null||$(this).val()=="null"){
					$(this).val("");
				}
			});
			$('#leaderButton').click(function(){
　　 			var openre=window.open ('<%=basePath%>sys/managerall.do?method=searchKjUser&fromFee=true', 'openre', 'top=0, left=0, toolbar=no, menubar=no, scrollbars=yes, resizable=yes,location=n o, status=no') //这句要写成一行
			});
		});
		
		
//同步专利年费数据---------------------我是华丽的分割线---------------------------------
	//add by xyj 2015/10/26
	//var xmlSynHttp = false;
	
	//用于放弃和恢复专利的功能
	
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
							var str = "<input type='text' id='imgCode'>"+"<img src='<%=basePath%>images/yzm.jpg' >"+
							"<input type='button' id='zhixing' class='syncNew' name='' value='执行' onclick='javascript:getFeeSynchronization(<%=patentId%>,<%=kjPatent.getPatentNo() %>);'>";
							yzm.innerHTML = str;
						}else{
							var yzm = document.getElementById('yzm');
							var str = "<input type='button' id='zhixing'  name='' class='syncNew' value='开始执行' onclick='javascript:getFeeSynchronizationNoImgCode(<%=patentId%>,<%=kjPatent.getPatentNo() %>);'>";
							yzm.innerHTML = str;
						}
					}
			}
		}
		document.getElementById('yzm').style.display='block';
		document.getElementById('syn').style.display='none';
		xmlPreSynHttp.send(null);
	}
	 
	 function createXMLxSynHttpRequest() {	
		if (window.ActiveXObject) {
			xmlSynHttp = new ActiveXObject("Microsoft.XMLHTTP");
		} else if (window.XMLHttpRequest) {
			xmlSynHttp = new XMLHttpRequest();
		}
	 }
	 
	 	function getFeeSynchronizationNoImgCode(patentId,patentNo){	
	    document.getElementById('zhixing').disabled = true;
			createXMLxSynHttpRequest();
			var valCode = 0;
			var url = "/tech/patent/fee.do?method=getFeeSynchronization&patentId="+patentId+"&patentNo="+patentNo+"&valCode="+valCode;
			xmlSynHttp.open("GET", url, true);		
			xmlSynHttp.onreadystatechange = function feeSynchronizationCallback(){
				if (xmlSynHttp.readyState == 4) {
					if (xmlSynHttp.status == 200) {
						alert("同步成功");
						location.reload(true);
					}else{
						alert("同步失败");
					}
				}
			};
			xmlSynHttp.send(null);

	}
	function getFeeSynchronization(patentId,patentNo){	
	    document.getElementById('zhixing').disabled = true;
	    var valCode = document.getElementById('imgCode').value;
	    if(valCode.length!=4){
	    	alert("验证码输入长度有误！");
	    	return;
	    }
			createXMLxSynHttpRequest();
			var url = "/tech/patent/fee.do?method=getFeeSynchronization&patentId="+patentId+"&patentNo="+patentNo+"&valCode="+valCode;
			xmlSynHttp.open("GET", url, true);		
			xmlSynHttp.onreadystatechange = function feeSynchronizationCallback(){
				if (xmlSynHttp.readyState == 4) {
					if (xmlSynHttp.status == 200) {
						alert("同步成功");
						location.reload(true);
					}else{
						alert("同步失败");
					}
				}
			};
			xmlSynHttp.send(null);

	}
	

	
	
	
	</script>

	<body>
		<html:form method="POST" action="fee.do?method=saveOrUpdate"
			target="_self" styleId="feeForm" styleClass="feeForm">
			<input type="hidden" name="patentId" value="<%=patentId%>">
			<input type="hidden" name="type" value="<%=type%>">
			<table width="70%" border="0" cellspacing="0" cellpadding="0"
				align="center" bgcolor="#FFFFFF" class="table_gray_all">
				<%
				if (canWrite.equals("true")) {
				%>
				<tr valign="middle">
					<td width="50%" class="formbody2" align="right"style="height: 50px;">
							<input type="button" class="toUpdate" name="" value="新建或修改"
							onclick="gotoUpdate();">
						&nbsp;&nbsp;
						<input type="button" class="update" style="display: none;" name=""
							value="提交修改" onclick="update();">
						&nbsp;&nbsp;
						<input type="button" class="update" style="display: none;" name=""
							value="取消修改" onclick="gotoCancel();">
					&nbsp;&nbsp;
					</td>
					<td width="50%" class="formbody2" align="left">
				<%
				if(userbean.getIsPatentB() || userbean.getIsMaintainB())
				{
				%>
				
				
				<input id="syn" type="button" class="sync" name=""
							value="同步" onclick="javascript:getPreLogin();">
				<%
				}
				%>
				<span id="yzm" style= "display:none"></span>
				</td>
				</tr>
				<%
					}
				%>
			</table>

			<table width="70%" border="0" cellspacing="0" cellpadding="0"
				align="center" bgcolor="#FFFFFF" class="table_gray_all">
				
				<tr>
					<td colspan="4" align="center" class="formlabel_nolength">&nbsp;
					<%
					if(kjPatent!=null)
					{
						out.print("专利 ： "+kjPatent.getName()+" 的所有费用");
					}
					%>
					</td>
				</tr>
				<%
				if (canWrite.equals("true") && false) {
				%>
				<tr bgcolor="#FFFFFF">
					<td height="30" width="25%"
						background="<%=basePath%>images/top_BG.jpg" align="center">
						&nbsp;&nbsp;
						<span class="font_big_menu">经费人姓名</span>
					</td>
					<td height="30" width="25%"
						background="<%=basePath%>images/top_BG.jpg" align="center">
						&nbsp;&nbsp;
						<span class="font_big_menu">学院</span>
					</td>
					<td height="30" width="15%"
						background="<%=basePath%>images/top_BG.jpg" align="center">
						&nbsp;&nbsp;
						<span class="font_big_menu">工资号</span>
					</td>
					<td height="30" width="40%"
						background="<%=basePath%>images/top_BG.jpg" align="center">
						&nbsp;&nbsp;
						<span class="font_big_menu">经费卡号</span>
					</td>
				</tr>
				<tr>
					<td align="center" class="formlabel">
						<input type="text" name="fundPeopleName"
							value="<%=fundPeopleName%>" readonly="readonly">
					</td>
					<td align="center" class="formlabel">
						<input type="text" name="fundPeopleCollege"
							value="<%=fundPeopleCollege%>" readonly="readonly">
					</td>
					<td align="center" class="formlabel">
						<input type="text" name="fundPeopleId" value="<%=fundPeopleId%>"
							readonly="readonly">
					</td>
					<td align="center" class="formlabel">
						<select name="cardNo" id="cardNo" value="<%=cardNo%>">
							<option value="<%=cardNo%>"><%=cardNo%></option>
						</select>
					</td>
				</tr>
				<tr id="selectPeole" style="display: none;">

					<td align="left">
						<input name="leaderButton" type="button" id="leaderButton"
							value="选择持卡人"  />
						<input name="clearButton" type="button" id="clearButton"
							value="取消" onclick="cancelCard();"/>
						<input type="text" name="leaderId" id="leaderId"
							style="display: none">
					</td>
				</tr>
				<%
				}
				%>
				<%
						if (feeTypeList != null && feeTypeList.size() > 0) {
						for (int i = 0; i < feeTypeList.size(); i++) {
							KjDictionary thisFeeType = feeTypeList.get(i);
							if (thisFeeType.getDomainValue().equals("年费")) {
				%>
				<table width="70%" border="0" cellspacing="0" cellpadding="0"
					align="center" bgcolor="#FFFFFF" class="table_gray_all">
					<tr bgcolor="#FFFFFF">
						<td height="30" width="10%"
							background="<%=basePath%>images/top_BG.jpg" align="center">
							&nbsp;&nbsp;
							<span class="font_big_menu"><%=thisFeeType.getDomainValue()%></span>
						</td>
						<td height="30" width="10%"
							background="<%=basePath%>images/top_BG.jpg" align="center">
							&nbsp;&nbsp;
							<span class="font_big_menu">专利年度</span>
						</td>
						<td colspan="2" height="30" width="30%"
							background="<%=basePath%>images/top_BG.jpg" align="center">
							&nbsp;&nbsp;
							<span class="font_big_menu">金额</span>
						</td>
						<td height="30" width="25%"
							background="<%=basePath%>images/top_BG.jpg" align="center">
							&nbsp;&nbsp;
							<span class="font_big_menu">截止日期</span>
						</td>
						<td height="30" width="16%"
							background="<%=basePath%>images/top_BG.jpg" align="center">
							&nbsp;&nbsp;
							<span class="font_big_menu">是否已缴费</span>
						</td>
					</tr>
					<%
					} else {
					%>
					<table width="70%" border="0" cellspacing="0" cellpadding="0"
						align="center" bgcolor="#FFFFFF" class="table_gray_all">
						<tr bgcolor="#FFFFFF">
							<td height="30" width="20%"
								background="<%=basePath%>images/top_BG.jpg" align="center">
								&nbsp;&nbsp;
								<span class="font_big_menu"><%=thisFeeType.getDomainValue()%></span>
							</td>
							<td colspan="2" width="30%"
								background="<%=basePath%>images/top_BG.jpg" align="center">
								&nbsp;&nbsp;
								<span class="font_big_menu">金额</span>
							</td>
							<td height="30" width="25%"
								background="<%=basePath%>images/top_BG.jpg" align="center">
								&nbsp;&nbsp;
								<span class="font_big_menu">截止日期</span>
							<td height="30" width="16%"
								background="<%=basePath%>images/top_BG.jpg" align="center">
								&nbsp;&nbsp;
								<span class="font_big_menu">是否已缴费</span>
							</td>
							</td>
						</tr>
						<%
					  }
					  if (feeConfigList != null && feeConfigList.size() > 0){
						  for (int j = 0; j < feeConfigList.size(); j++) {
							  if (feeConfigList.get(j).getType().equals(thisFeeType.getDomainValueEn())) {
									KjPatentFeeConfig thisConfigObj = feeConfigList.get(j);
									if (thisFeeType.getDomainValue().equals("年费")) {
									int m = 0;
									if (type.equals("1")) {
										m = 20;
									} else {
										m = 10;
									}
									for (int k = 1; k <= m; k++) {
						%>

						<tr>
							<td>
								
							</td>
							<td align="center" class="formlabel"><%=k%></td>
							
							<td colspan="2" align="center" class="formlabel_nolength">
										<input type="text" style="float:left" class="modify" 
										disabled="disabled" id="annelFee_<%=k%>" name="annelFee_<%=k%>" 
										onkeyup="this.value=isDigit(this.value);" 
										size="8"
										value="<%=request.getAttribute("annelFee_"+ k)%>">&nbsp;
									<%
										double money = 0.0;
										if(m==20){
											int level = (int)Math.floor((k-1)/3)+1;    // 1-3(1) 4-6(2) 7-9(3) 10-12(4) 13-15(5) 16-20(6 7)
											switch(level){
												case 1: money=900; break;
												case 2: money=1200; break;
												case 3: money=2000; break;
												case 4: money=4000; break;
												case 5: money=6000; break;
												case 6: money=8000; break;
												case 7: money=8000; break;
											}
										}else if(m==10){
											switch(k){
												case 1: money=600; break;
												case 2: money=600; break;
												case 3: money=600; break;
												case 4: money=900; break;
												case 5: money=900; break;
												case 6: money=1200; break;
												case 7: money=1200; break;
												case 8: money=1200; break;
												case 9: money=2000; break;
												case 10:money=2000; break;
											}
										}
									%>
										<a href="javascript:refMoney('<%=money %>', 'annelFee_<%=k%>');"  class="stop" style="text-decoration:none;" >&nbsp;参考值(<%=money%>)</a>
							</td>
							
							<td valign="center" class="formlabel_nolength">&nbsp;&nbsp;
							<input type="text" class="modify Wdate" disabled="disabled" name="annelDate_<%=k%>" size="" onclick="WdatePicker()" value="<%=request.getAttribute("annelDate_"+ k)%>">
							</td>
							<td align="center" class="formlabel">
								<%
									if (request.getAttribute("annelPay_" + k) == null) {
								%>
								<input type="radio" class="isPay" disabled="disabled"
									name="annelPay_<%=k%>" value="0" />
								未缴费
								<input type="radio" class="isPay" disabled="disabled"
									name="annelPay_<%=k%>" value="1" />
								已缴费
								<%
									} else if (request.getAttribute("annelPay_" + k).toString().equals("0")) {
								%>
								<input type="radio" class="isPay" disabled="disabled"
									name="annelPay_<%=k%>" checked="checked" value="0" />
								未缴费
								<input type="radio" class="isPay" disabled="disabled"
									name="annelPay_<%=k%>" value="1" />
								已缴费
								<%
									} else if (request.getAttribute("annelPay_" + k).toString().equals("1")) {
								%>
								<input type="radio" class="isPay" disabled="disabled"
									name="annelPay_<%=k%>" value="0" />
								未缴费
								<input type="radio" class="isPay" disabled="disabled"
									name="annelPay_<%=k%>" checked="checked" value="1" />
								已缴费
								<%
								}
								%>
							</td>


						</tr>

						<%
										}
								}else {
						%>
						<tr>

							<td align="center" class="formlabel" width="30%">
								&nbsp;<%=thisConfigObj.getName()%></td>
							<td colspan="2" lign="center" class="formlabel" width="30%">
								<input type="text" class="modify" disabled="disabled" name="fee_<%=thisConfigObj.getId()%>" onkeyup="this.value=isDigit(this.value);" value="<%=request.getAttribute("fee_"+ thisConfigObj.getId())%>">
							</td>
							<td valign="center" class="formlabel_nolength" >&nbsp;&nbsp;
							<input type="text" class="modify Wdate" disabled="disabled" name="date_<%=thisConfigObj.getId()%>" size="" onclick="WdatePicker()" value="<%=request.getAttribute("date_" + thisConfigObj.getId())%>">
							</td>
							<td align="center" class="formlabel" width="30%">
								<%
									if (request.getAttribute("pay_"	+ thisConfigObj.getId()) == null) {
								%>
								<input type="radio" class="isPay" disabled="disabled"
									name="pay_<%=thisConfigObj.getId()%>" onclick="javascript:zhina(this.value);" value="0" />
								未缴费
								<input type="radio" class="isPay" disabled="disabled"
									name="pay_<%=thisConfigObj.getId()%>" value="1" />
								已缴费
								<%
									} else if (request.getAttribute("pay_" + thisConfigObj.getId()).toString().equals("0")) {
								%>
								<input type="radio" class="isPay" disabled="disabled"
									name="pay_<%=thisConfigObj.getId()%>" checked="checked"
									value="0" />
								未缴费
								<input type="radio" class="isPay" disabled="disabled"
									name="pay_<%=thisConfigObj.getId()%>" value="1" />
								已缴费
								<%
									} else if (request.getAttribute("pay_" + thisConfigObj.getId()).toString().equals("1")) {
								%>
								<input type="radio" class="isPay" disabled="disabled"
									name="pay_<%=thisConfigObj.getId()%>" value="0" />
								未缴费
								<input type="radio" class="isPay" disabled="disabled"
									name="pay_<%=thisConfigObj.getId()%>" checked="checked"
									value="1" />
								已缴费
								<%
								}
								%>

							</td>
						</tr>
						
						<%
						}
						%>

						<%
								}
							}
						}
						%>
					</table>
					<%
						}
						}
					%>
					
					<table width="70%" border="0" cellspacing="0" cellpadding="0" align="center" bgcolor="#FFFFFF" class="table_gray_all">
						<tr>
							<td height="30" width="32%"
									background="<%=basePath%>images/top_BG.jpg" align="left" colspan="4">
									&nbsp;&nbsp;
									<span class="font_big_menu">&nbsp;&nbsp;&nbsp;&nbsp;专利过程状态</span>
							</td>
						</tr>
						<tr bgcolor="#FFFFFF">
							<td align="center" class="formlabel" width="20%" >
								&nbsp;过程状态</td>
							<td  align="center" class="formlabel" width="30%">
						<%
						String thisState = ParamUtil.getAttribute(request,"state");
						%>
								<html:select property="state" styleId="state" value="<%=thisState%>">
									<option value="" selected="selected">--请选择--</option>
								    <logic:iterate id="patentState" name="patentStateList">
								    	<html:option  value="${patentState.code}">${patentState.name}</html:option>
									</logic:iterate>
								</html:select>
							</td>
							<td width="20%">&nbsp;</td>
							<td width="30%">&nbsp;</td>
						</tr>
					</table>
					
					<table width="70%" border="0" cellspacing="0" cellpadding="0"
				align="center" bgcolor="#FFFFFF" class="table_gray_all">
				<%
				if (canWrite.equals("true")) {
				%>
				<tr valign="middle">
					<td colspan="4" class="formbody2" align="center"
						style="height: 50px;">
						<input type="button" class="toUpdate" name="" value="新建或修改"
							onclick="gotoUpdate();">
						&nbsp;&nbsp;
						<input type="button" class="update" style="display: none;" name=""
							value="提交修改" onclick="update();">
						&nbsp;&nbsp;
						<input type="button" class="update" style="display: none;" name=""
							value="取消修改" onclick="gotoCancel();">
						&nbsp;&nbsp;
						
					</td>
				</tr>
				<%
					}
				%>
			</table>

					</html:form>
	</body>
</html>



