package inc.tech.patent.action;

import inc.tech.apply.dao.AuditLogDao;
import inc.tech.fund.fundCard.dao.FundCardDao;
import inc.tech.patent.dao.KjPatentDao;
import inc.tech.patent.dao.KjPatentFeeConfigDao;
import inc.tech.patent.dao.KjPatentFeeDao;
import inc.tech.patent.dao.KjPatentStateDao;
import inc.tech.patent.form.KjPatentFeeForm;
import inc.tech.patent.util.CaculateAnnualFee;
import inc.tech.patent.util.CaculateCostDate;
import inc.tech.patent.util.FeeList;
import inc.tech.patent.util.PatentFeeTimerTask;
import inc.tech.patent.util.QuickSearch;
import inc.tech.persistent.DAOException;
import inc.tech.persistent.entity.KjAuditLog;
import inc.tech.persistent.entity.KjDictIdentity;
import inc.tech.persistent.entity.KjDictionary;
import inc.tech.persistent.entity.KjPatent;
import inc.tech.persistent.entity.KjPatentFee;
import inc.tech.persistent.entity.KjPatentFeeConfig;
import inc.tech.persistent.entity.KjUser;
import inc.tech.sys.chop.dao.KjAuditLogDAO;
import inc.tech.sys.common.dao.KjDictObjtypeDAO;
import inc.tech.sys.dict.dao.KjDictionaryDao;
import inc.tech.sys.group.dao.KjGroupDAO;
import inc.tech.sys.init.TechSystem;
import inc.tech.sys.user.UserBean;
import inc.tech.user.dao.KjUserDAO;
import inc.tech.util.Clawer;
import inc.tech.util.DateUtil;
import inc.tech.util.FileUtil;
import inc.tech.util.PaginalBean;
import inc.tech.util.ParamUtil;
import inc.tech.util.SysConfig;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.actions.DispatchAction;
import org.apache.struts.upload.FormFile;
import org.jsoup.nodes.Document;
import org.xml.sax.SAXException;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class KjPatentFeeAction extends DispatchAction {
	  static  int AnnelFeeConfigId=11;//费用表年费对应的费用配置表的主键
	  static  int overDueFeeConfigId=12;//费用表滞纳金对应的费用配置表的主键
	  static  int recoverFeeConfigId=14;//费用表恢复费对应的费用配置表的主键
	  
	  Clawer patentClawer = new Clawer();
	  WebClient webClient = null;
	
	
	public ActionForward showAllFee(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)throws DAOException, IOException  {
		
		String patentId=ParamUtil.getParameterFrom(request, "patentId"); 
		String type=ParamUtil.getParameterFrom(request, "type");
		
		List<KjPatentFeeConfig> feeConfigList = KjPatentFeeConfigDao.getInstance().findAll();
		List<KjPatentFee> thisFees=KjPatentFeeDao.getInstance().findByPatentId(Long.parseLong(patentId));
				
		List<KjDictionary> feeTypeList = KjDictionaryDao.getInstance().getKjDictionaryByType("DICT.PATENT.FEE.CONFIGTYPE");

		for(int i=0;i<thisFees.size();i++){
			KjPatentFee thisFee = thisFees.get(i);
			if(thisFee.getConfigId().getId()!=AnnelFeeConfigId){
				if(thisFee.getAmount()!=null){
					request.setAttribute("fee_"+thisFee.getConfigId().getId(), thisFee.getAmount());
				}
				else{
					request.setAttribute("fee_"+thisFee.getConfigId().getId(),"");
				}
				if(thisFee.getExpiryDay()!=null){
					SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd");   
					String str=sdf.format(thisFee.getExpiryDay());  
					request.setAttribute("date_"+thisFee.getConfigId().getId(), str);
				}
				else{
					request.setAttribute("date_"+thisFee.getConfigId().getId(),"");
				}		
				if(thisFee.getIsPaid()!=null){
					request.setAttribute("pay_"+thisFee.getConfigId().getId(), thisFee.getIsPaid());
				}
				else{
					
				}
			}
			else{
				if(thisFee.getAmount()!=null){
					request.setAttribute("annelFee_"+thisFee.getYear(), thisFee.getAmount());
				}
				else{
					request.setAttribute("annelFee_"+thisFee.getYear(),"");
				}
				if(thisFee.getExpiryDay()!=null){
					SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd");   
					String str=sdf.format(thisFee.getExpiryDay());  
					request.setAttribute("annelDate_"+thisFee.getYear(), str);
				}
				else{
					request.setAttribute("annelDate_"+thisFee.getYear(),"");
				}
				if(thisFee.getIsPaid()!=null){
					request.setAttribute("annelPay_"+thisFee.getYear(), thisFee.getIsPaid());
				}
				else{
					
				}
				
			}			
		}		
		
		//以下为系统自动计算年费
		KjPatent kp = KjPatentDao.getInstance().findByPk(Long.parseLong(patentId));
		if(kp.getAcceptDate()!=null&&kp.getAnnouncementDate()!=null){
			CaculateCostDate ccd=new CaculateCostDate();
			Long beginYear = ccd.judgeYear(kp.getAcceptDate(), kp.getAnnouncementDate());
			String caculateType = ccd.judgeType(Long.parseLong(patentId), kp.getType());
			List<FeeList> feeList= CaculateAnnualFee.feeList(kp.getAcceptDate(), Short.parseShort(caculateType), beginYear);
			request.setAttribute("feeList", feeList);
		}	
		
		request.setAttribute("fundsPeople", KjPatentDao.getInstance().findByPk(Long.parseLong(patentId)).getFundsPeople());
		request.setAttribute("feeConfigList", feeConfigList);
		request.setAttribute("feeTypeList", feeTypeList);
		request.setAttribute("patentId", patentId);
		request.setAttribute("type", type);
		request.setAttribute("state", KjPatentDao.getInstance().findByPk(Long.parseLong(patentId)).getState());
		request.setAttribute("patentStateList",KjPatentStateDao.getInstance().findAll());
		return new ActionForward("/fee/allFees.jsp");		
	}
	
	public ActionForward saveOrUpdate(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)throws DAOException, IOException, NumberFormatException, ParseException  {
		HttpSession session = request.getSession();
		UserBean userbean = (UserBean) session.getAttribute("UserBean");
		String userId = userbean.getUid();
		
		String fundPeopleId = ParamUtil.getParameter(request, "fundPeopleId");
		String cardNo = ParamUtil.getParameter(request, "cardNo");
		String patentId=ParamUtil.getParameterFrom(request, "patentId"); 
		KjPatent thisPatent = KjPatentDao.getInstance().findByPk(Long.parseLong(patentId));
		if(thisPatent!=null){
			if(fundPeopleId!=null && cardNo!=null && fundPeopleId.length()>0 && cardNo.length()>0){
				String cardId = FundCardDao.getInstance().findByCardNo(cardNo);
				KjUser thisUser = KjUserDAO.getInstance().findByPk(fundPeopleId);
				if(cardId!=null && thisUser!=null){
					thisPatent.setFundsPeople(thisUser);
					thisPatent.setFundCardNo(cardId);
					KjPatentDao.getInstance().update(thisPatent);
					
					KjAuditLog auditlog = new KjAuditLog();
					auditlog.setKjDictObjtype(KjDictObjtypeDAO.getInstance().findByPk(15l));//1
					auditlog.setObjId(Long.parseLong(patentId));//2
					auditlog.setOper("数据维护用户绑定经费卡,卡号："+cardNo);// 一般是指数据对象 从什么状态 转到什么状态 3
					auditlog.setOperTime(DateUtil.getDate(new Date()));
					auditlog.setOperMsg(KjUserDAO.getInstance().findByPk(userId).getName() + "绑定经费卡");// 一般指谁进行了什么操作 如 宁xx审核、段xx修改、xxx删除 4
					auditlog.setOperUser(userId);
					KjAuditLogDAO.getInstance().merge(auditlog);
				}
					
			}
		}
		String type=ParamUtil.getParameterFrom(request, "type");
		KjPatentFeeDao.getInstance().saveOrUpdate(Long.parseLong(patentId), request);
		String state = ParamUtil.getParameterFrom(request, "state");
		KjPatentDao.getInstance().changeState(Long.parseLong(patentId), state);
		return new ActionForward("/fee.do?method=showAllFee&patentId="+patentId+"&type="+type);		
	}
	
	//年费
	public ActionForward showRemind(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)throws DAOException  {    
		HttpSession session=request.getSession();
		UserBean userbean = (UserBean) session.getAttribute("UserBean");
		String userId = userbean.getUid();
		//获取用户的身份
		String sign = ((KjDictIdentity)session.getAttribute("identity")).getSign();
		String confirm=ParamUtil.getParameterFrom(request, "confirm");  //判断是否是通过确认已缴费的操作
				
		String sqlWhere = "";
		KjPatentFeeForm Form = (KjPatentFeeForm) form;
		if(Form.getType()==null || Form.getType().length==0)
		{
			Form.setType(new String[]{"1","",""});
		}
		
		Long time0 = System.currentTimeMillis();
		
		if(sign.equals("Admin")){
			
//			sqlWhere=" as kj where kj.isPaid=0 and kj.configId.id in ("+AnnelFeeConfigId+","+overDueFeeConfigId+","+recoverFeeConfigId+")  and kj.expiryDay is not null";
			
			sqlWhere=" as kj where kj.isPaid=0 "
				  +" and (kj.configId.type='year' or kj.configId.type='other' or kj.configId.type='auth') "
				  +" and kj.expiryDay is not null  "
				  +" and kj.patentId.patentId is not null "
				  +" and kj.patentId.deleteMark<1  "
				  +" and kj.patentId.state!='54' and kj.patentId.state!='55' and kj.patentId.state!='56' "
				  ;
			sqlWhere=QuickSearch.feeForm(sqlWhere, form, "kj.expiryDay");
//			sqlWhere=QuickSearch.feeForm(sqlWhere, form, "");
			session.setAttribute("sqlWhere",sqlWhere);
		}
		else if(sign.equals("User")){
			sqlWhere=" as kj where kj.isPaid=0 and (kj.configId.type='year' or kj.configId.type='other' or kj.configId.type='auth') and kj.expiryDay is not null "+
			"and kj.patentId.patentId is not null and kj.patentId.deleteMark<1 and (kj.patentId.kjFirstInventorId.staffId='"+userId+"'or kj.patentId.patentId in (select kjPatent.patentId from KjProjectstaff where kjUser.staffId='"+userId+"')) "
			+" and kj.patentId.state!='54' and kj.patentId.state!='55' and kj.patentId.state!='56' ";
			sqlWhere=QuickSearch.feeForm(sqlWhere, form, "kj.expiryDay");
			session.setAttribute("sqlWhere",sqlWhere);
		}
		else if(sign.equals("Manager")){
			
			sqlWhere = " as kj where 1=1 ";
			if(userbean.getOtherManagerCollege()!=null && userbean.getOtherManagerCollege().length()>0)
			{
				sqlWhere = sqlWhere + " and (kj.patentId.deptId='"+userbean.getCollege()+"' or kj.patentId.deptId='"+userbean.getOtherManagerCollege()+"'  )  ";
			}
			else
			{
				sqlWhere = sqlWhere + " and kj.patentId.deptId='"+userbean.getCollege()+"' ";
			}
			
			sqlWhere = sqlWhere + " and kj.isPaid=0 and (kj.configId.type='year' or kj.configId.type='other' or kj.configId.type='auth') and kj.expiryDay is not null  and kj.patentId.patentId is not null and kj.patentId.deleteMark<1  "
//			+" and kj.patentId.deptId='"+userbean.getCollege()+"'"
			+" and kj.patentId.state!='54' and kj.patentId.state!='55' and kj.patentId.state!='56' ";
			//获取科研组信息
			String kjCollege = userbean.getCollege();
			
			Form.setCollegeId(kjCollege);
			sqlWhere=QuickSearch.feeForm(sqlWhere, Form, "kj.expiryDay");
			session.setAttribute("sqlWhere",sqlWhere);
		}
		
		KjPatentFeeDao dao = KjPatentFeeDao.getInstance();
		int currentCount = SysConfig.START_COUNT;
		int pageSize = SysConfig.PAGE_SIZE;
		if (request.getParameter("page") != null){
			currentCount = Integer.parseInt((request.getParameter("page")));
		}
		if (request.getParameter("pageSize") != null){
			pageSize = Integer.parseInt((request.getParameter("pageSize")));
		}
		
		//过滤掉多余的费用记录
		Map patentIdMap = new HashMap();			 //判断当前patentId是否已经存在的map
		
		Long time1 = System.currentTimeMillis();
//		System.out.println(" 拼装完sql  " + (time1 - time0)/1000);
		
		List<KjPatentFee> patentFeeListAll = dao.getList(sqlWhere);
//		List<Long> patentFeeListAll_id = null;
//		String sql = " select distinct kj.patentId.patentId,kj.expiryDay  from KjPatentFee " + sqlWhere;
//		System.out.println(sql);
//		Query  q = KjPatentFeeDao.getInstance().getSession().createQuery(sql);
//		patentFeeListAll_id = q.list();
		
		Long time2 = System.currentTimeMillis();
//		System.out.println("查询完sql  " + (time2 - time1));//这里耗时5秒
		
		List<KjPatentFee> patentFeeListAll_filter = new ArrayList<KjPatentFee>();
		List<KjPatentFee> patentFeeList = new ArrayList<KjPatentFee>();
//		System.out.println(sqlWhere);
//		System.out.println("s:"+patentFeeListAll.size());
	
		
		if(patentFeeListAll!=null){
			for(int i=0; i<patentFeeListAll.size(); i++){       
				if(i==0){
					
					patentFeeListAll_filter.add(patentFeeListAll.get(0));
					patentIdMap.put(patentFeeListAll.get(0).getPatentId().getPatentId(), 1);    //讲patentId存入map,用于后续的校验
				}else{
					if(!patentIdMap.containsKey(patentFeeListAll.get(i).getPatentId().getPatentId())){   //如果没有将patentId对应的费用加入list,则在此加入
						patentFeeListAll_filter.add(patentFeeListAll.get(i));
						patentIdMap.put(patentFeeListAll.get(i).getPatentId().getPatentId(), 1);    //讲patentId存入map,用于后续的校验
					}
				}
			}
			//
			
			//拼装当次查询的起始位置 并且计算当前的费用提示数值情况  <即当前最近的一天截止的所有费用求和>
//			System.out.println(currentCount+"  "+pageSize);
			if(currentCount>=patentFeeListAll_filter.size())currentCount = (patentFeeListAll_filter.size()/pageSize)*(pageSize);
			if(currentCount==patentFeeListAll_filter.size())currentCount = patentFeeListAll_filter.size()-pageSize;
			if(currentCount<0)currentCount=0;
			for(int j=currentCount; j<=currentCount+pageSize-1 && j<patentFeeListAll_filter.size(); j++){
				patentFeeList.add(patentFeeListAll_filter.get(j));
			}
		}
		
		Long time3 = System.currentTimeMillis();
//		System.out.println("过滤完list  " + (time3 - time2)/1000);
		
		int sumCount = patentFeeListAll_filter.size();
		PaginalBean paginalbean = new PaginalBean(currentCount, sumCount, pageSize);
		
		for(int i=0; i<patentFeeList.size(); i++){
			KjPatent patentObj = patentFeeList.get(i).getPatentId();
			if(patentObj.getIsGiveup()!=null && patentObj.getIsGiveup()==3L){   //放弃专利被驳回也会显示驳回意见
				List<KjAuditLog> logList = AuditLogDao.getInstance().getAuditLogList(15L, patentObj.getPatentId());
				if(logList!=null && logList.size()>0){
					request.setAttribute("auditInfo_"+patentObj.getPatentId(), logList.get(0).getOperMsg());
				}
			}
		}
		
		List<String> selectedList = new ArrayList<String>();
		String selectedItems = ParamUtil.getParameter(request, "selectedItems");
		String unSelectedItems = ParamUtil.getParameter(request, "unSelectedItems");
		String tableIds = ParamUtil.getParameter(request, "tableIds");
		if(selectedItems!=null && unSelectedItems!=null && tableIds != null)
			selectedList = dealIds(tableIds, selectedItems, unSelectedItems);
		
		tableIds = "";
		for(int i=0; i<selectedList.size(); i++)
		{
			if(i > 0)
				tableIds += ",";
			tableIds += selectedList.get(i);
		}
		
		
		request.setAttribute("selectedItems",selectedItems);
		request.setAttribute("unSelectedItems",unSelectedItems);
		request.setAttribute("tableIds",tableIds);
		request.setAttribute("selectedList", selectedList);
		
		
		request.setAttribute("PaginalBean", paginalbean);
		request.setAttribute("pageSize", pageSize);
		request.setAttribute("updates", request.getAttribute("updates"));
		request.setAttribute("notUpdate", request.getAttribute("notUpdate"));
		request.setAttribute("confirm", confirm);
		request.setAttribute("allAmount", KjPatentFeeDao.getInstance().getAllAmount());
		request.setAttribute("allRecover", KjPatentFeeDao.getInstance().getAllRecover());
		
		request.setAttribute("tableIds", tableIds);
		request.setAttribute("selectedItems", selectedItems);
		request.setAttribute("unSelectedItems", unSelectedItems);
		
//		System.out.println("t:"+tableIds+"  s:"+selectedItems+"  u:"+unSelectedItems);
		
		request.setAttribute("patentList", patentFeeList);
		request.setAttribute("patentStateList",KjPatentStateDao.getInstance().findAll());
		return new ActionForward("/fee/authorFees.jsp");	
	}
	
	private List<String> dealIds(String ids, String selectedItems, String unSelectedItems){
		if(ids.length()==0)
		{
			ids = selectedItems;
		}
		List<String> selectedList = new ArrayList<String>();
		String[] selectedArray = selectedItems.split(",");
		String[] unSelectedArray = unSelectedItems.split(",");
		String[] idsArray = ids.split(",");
		if(ids.length()>0)
		{
			for(int i=0; i<idsArray.length; i++)
				selectedList.add(idsArray[i]);
		}
		for(int i=0; i<selectedArray.length; i++)
		{
			if(!selectedList.contains(selectedArray[i]))
			{
				selectedList.add(selectedArray[i]);
			}
		}
		for(int i=0; i<unSelectedArray.length; i++)
		{
			if(selectedList.contains(unSelectedArray[i]))
				selectedList.remove(unSelectedArray[i]);
		}
		return selectedList;
	}
	
	public ActionForward showOffice(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)throws DAOException  {
		HttpSession session=request.getSession();
		UserBean userbean = (UserBean) session.getAttribute("UserBean");
		String userId = userbean.getUid();
		//获取用户的身份
		String sign = ((KjDictIdentity)session.getAttribute("identity")).getSign();
		String confirm=ParamUtil.getParameterFrom(request, "confirm"); ; //判断是否是通过确认已缴费的操作
		
		String sqlWhere = "";
		if(sign.equals("Admin")){
			
//			sqlWhere=" as kj where kj.isPaid=0 and kj.configId.id in ("+AnnelFeeConfigId+","+overDueFeeConfigId+","+recoverFeeConfigId+")  and kj.expiryDay is not null";
			
			sqlWhere=" as kj where kj.isPaid=0 and (kj.configId.type='office') and kj.expiryDay is not null  and kj.patentId.patentId is not null and kj.patentId.deleteMark<1  ";
			sqlWhere=QuickSearch.feeForm(sqlWhere, form, "expiryDay");
			session.setAttribute("sqlWhere",sqlWhere);
		}
		else if(sign.equals("User")){
			sqlWhere=" as kj where kj.isPaid=0 and (kj.configId.type='office' ) and kj.expiryDay is not null "+
			"and kj.patentId.patentId is not null and kj.patentId.deleteMark<1 and (kj.patentId.kjFirstInventorId.staffId='"+userId+"'or kj.patentId.patentId in (select kjPatent.patentId from KjProjectstaff where kjUser.staffId='"+userId+"')) ";
			sqlWhere=QuickSearch.feeForm(sqlWhere, form, "expiryDay");
			session.setAttribute("sqlWhere",sqlWhere);
		}
		else if(sign.equals("Manager")){
			
			sqlWhere = " as kj where 1=1 ";
			if(userbean.getOtherManagerCollege()!=null && userbean.getOtherManagerCollege().length()>0)
			{
				sqlWhere = sqlWhere + " and (kj.patentId.deptId='"+userbean.getCollege()+"' or kj.patentId.deptId='"+userbean.getOtherManagerCollege()+"'  )  ";
			}
			else
			{
				sqlWhere = sqlWhere + " and kj.patentId.deptId='"+userbean.getCollege()+"' ";
			}
			
			sqlWhere = sqlWhere + " and kj.isPaid=0 and (kj.configId.type='office' ) and kj.expiryDay is not null  and kj.patentId.patentId is not null and kj.patentId.deleteMark<1  "
//			+" and kj.patentId.deptId='"+userbean.getCollege()+"'"
			;
			//获取科研组信息
			String kjCollege = userbean.getCollege();
			KjPatentFeeForm Form = (KjPatentFeeForm) form;
			Form.setCollegeId(kjCollege);
			sqlWhere=QuickSearch.feeForm(sqlWhere, Form, "expiryDay");
			session.setAttribute("sqlWhere",sqlWhere);
		}
		KjPatentFeeDao dao = KjPatentFeeDao.getInstance();
		int currentCount = SysConfig.START_COUNT;
		int pageSize = SysConfig.PAGE_SIZE;
		if (request.getParameter("page") != null){
			currentCount = Integer.parseInt((request.getParameter("page")));
		}
		if (request.getParameter("pageSize") != null){
			pageSize = Integer.parseInt((request.getParameter("pageSize")));
		}
		
		//过滤掉多余的费用记录
		Map patentIdMap = new HashMap();			 //判断当前patentId是否已经存在的map
		List<KjPatentFee> patentFeeListAll = dao.getList(sqlWhere);
		List<KjPatentFee> patentFeeListAll_filter = new ArrayList<KjPatentFee>();
		List<KjPatentFee> patentFeeList = new ArrayList<KjPatentFee>();
//		System.out.println(sqlWhere);
//		System.out.println("s:"+patentFeeListAll.size());
		if(patentFeeListAll!=null){
			for(int i=0; i<patentFeeListAll.size(); i++){       
				if(i==0){
					patentFeeListAll_filter.add(patentFeeListAll.get(0));
					patentIdMap.put(patentFeeListAll.get(0).getPatentId().getPatentId(), 1);    //讲patentId存入map,用于后续的校验
				}else{
					if(!patentIdMap.containsKey(patentFeeListAll.get(i).getPatentId().getPatentId())){   //如果没有将patentId对应的费用加入list,则在此加入
						patentFeeListAll_filter.add(patentFeeListAll.get(i));
						patentIdMap.put(patentFeeListAll.get(i).getPatentId().getPatentId(), 1);    //讲patentId存入map,用于后续的校验
					}
				}
			}
			//
			
			//拼装当次查询的起始位置 并且计算当前的费用提示数值情况  <即当前最近的一天截止的所有费用求和>
//			System.out.println(currentCount+"  "+pageSize);
			if(currentCount>=patentFeeListAll_filter.size())currentCount = (patentFeeListAll_filter.size()/pageSize)*(pageSize);
			if(currentCount==patentFeeListAll_filter.size())currentCount = patentFeeListAll_filter.size()-pageSize;
			if(currentCount<0)currentCount=0;
			for(int j=currentCount; j<=currentCount+pageSize-1 && j<patentFeeListAll_filter.size(); j++){
				patentFeeList.add(patentFeeListAll_filter.get(j));
			}
		}
		
		int sumCount = patentFeeListAll_filter.size();
		PaginalBean paginalbean = new PaginalBean(currentCount, sumCount, pageSize);
		
		for(int i=0; i<patentFeeList.size(); i++){
			KjPatent patentObj = patentFeeList.get(i).getPatentId();
			if(patentObj.getIsGiveup()!=null && patentObj.getIsGiveup()==3L){   //放弃专利被驳回也会显示驳回意见
				List<KjAuditLog> logList = AuditLogDao.getInstance().getAuditLogList(15L, patentObj.getPatentId());
				if(logList!=null && logList.size()>0){
					request.setAttribute("auditInfo_"+patentObj.getPatentId(), logList.get(0).getOperMsg());
				}
//				System.out.println("log:"+logList.size());
			}
		}
		request.setAttribute("PaginalBean", paginalbean);
		request.setAttribute("pageSize", pageSize);
		request.setAttribute("updates", request.getAttribute("updates"));
		request.setAttribute("notUpdate", request.getAttribute("notUpdate"));
		request.setAttribute("confirm", confirm);
		request.setAttribute("allAmount", KjPatentFeeDao.getInstance().getAllAmount());
		request.setAttribute("allRecover", KjPatentFeeDao.getInstance().getAllRecover());
		
		request.setAttribute("patentList", patentFeeList);
		request.setAttribute("fromOffice", "true");
		request.setAttribute("patentStateList",KjPatentStateDao.getInstance().findAll());
		return new ActionForward("/fee/authorFees.jsp");	
	}
	
	public void confirm(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)throws DAOException, IOException  {
		HttpSession session=request.getSession();
		UserBean userbean = (UserBean) session.getAttribute("UserBean");
		String userId = userbean.getUid();
		//获取用户的身份
		String sign = ((KjDictIdentity)session.getAttribute("identity")).getSign();
		
		String sqlWhereYear = "";//所有年费的列表
		String sqlWhereOverdue = "";//所有滞纳费的列表
		String sqlWhereRecover = "";//所有恢复费的列表
		String sqlWhereAll = "";//不重复专利的恢复+滞纳+年费
		
		if(sign.equals("Admin")){ 
			sqlWhereYear=" as kj where kj.isPaid=0 and (kj.configId.id="+AnnelFeeConfigId+") and kj.expiryDay is not null and kj.patentId.deleteMark <1 ";
			sqlWhereYear=QuickSearch.feeForm(sqlWhereYear, form, "expiryDay");
			session.setAttribute("sqlWhere",sqlWhereYear);
			
			sqlWhereOverdue=" as kj where kj.isPaid=0 and (kj.configId.id="+overDueFeeConfigId+") and kj.expiryDay is not null and kj.patentId.deleteMark <1 ";
			sqlWhereOverdue=QuickSearch.feeForm(sqlWhereOverdue, form, "expiryDay");
			
			sqlWhereRecover=" as kj where kj.isPaid=0 and (kj.configId.id="+recoverFeeConfigId+") and kj.expiryDay is not null and kj.patentId.deleteMark <1 ";
			sqlWhereRecover=QuickSearch.feeForm(sqlWhereRecover, form, "expiryDay");
			
		}
		/*else if(sign.equals("User")){
			sqlWhere=" as kj where kj.deleteMark=0L and kj.paied=0 and (kj.patentId.state='1' or (kj.patentId.state >='12' and kj.patentId.state <='20')) and (kj.patentId.kjFirstInventorId.staffId='"+userId+"'or kj.patentId.patentId in (select kjPatent.patentId from KjProjectstaff where kjUser.staffId='"+userId+"')) ";
			sqlWhere=QuickSearch.costForm(sqlWhere, form, "payDate");
			session.setAttribute("sqlWhere",sqlWhere);
		}
		else if(sign.equals("Manager")){
			sqlWhere=" as kj where kj.deleteMark=0L and kj.paied=0 and (kj.patentId.state='1' or (kj.patentId.state >='12' and kj.patentId.state <='20')) and kj.patentId.deptId='"+userbean.getCollege()+"'";
			sqlWhere=QuickSearch.costForm(sqlWhere, form, "payDate");
			session.setAttribute("sqlWhere",sqlWhere);
		}*/
		KjPatentFeeDao dao = KjPatentFeeDao.getInstance();
		List<KjPatentFee> kpfsYear=dao.findByHQL("from KjPatentFee "+sqlWhereYear);
		List<KjPatentFee> kpfsOverdue=dao.findByHQL("from KjPatentFee "+sqlWhereOverdue);
		List<KjPatentFee> kpfsRecover=dao.findByHQL("from KjPatentFee "+sqlWhereRecover);
		
		
		HSSFWorkbook wb = new HSSFWorkbook();           // or new XSSFWorkbook();
		HSSFSheet sheet = wb.createSheet("年费清单");
	    
	    //标题
		HSSFRow rowHead = sheet.createRow(0);
		HSSFCell cellHead = rowHead.createCell((short) 0);
	    cellHead.setCellValue("专利号");
	    cellHead = rowHead.createCell((short)1);
	    cellHead.setCellValue("专利名称");
	    cellHead = rowHead.createCell((short)2);
	    cellHead.setCellValue("专利类型");
	    cellHead = rowHead.createCell((short)3);
	    cellHead.setCellValue("第一发明人");
	    cellHead = rowHead.createCell((short)4);
	    cellHead.setCellValue("所属学院");
	    
	    cellHead = rowHead.createCell((short)5);
	    cellHead.setCellValue("费用名称（年费）");    
	    cellHead = rowHead.createCell((short)6);
	    cellHead.setCellValue("到期日期");
	    cellHead = rowHead.createCell((short)7);
	    cellHead.setCellValue("应缴金额");
	    
	    cellHead = rowHead.createCell((short)8);
	    cellHead.setCellValue("滞纳金");	    
	    cellHead = rowHead.createCell((short)9);
	    cellHead.setCellValue("到期日期");
	    cellHead = rowHead.createCell((short)10);
	    cellHead.setCellValue("应缴金额");
	    
	    cellHead = rowHead.createCell((short)11);
	    cellHead.setCellValue("恢复费");	    
	    cellHead = rowHead.createCell((short)12);
	    cellHead.setCellValue("到期日期");
	    cellHead = rowHead.createCell((short)13);
	    cellHead.setCellValue("应缴金额");
	    
	    cellHead = rowHead.createCell((short)14);
	    cellHead.setCellValue("经费人姓名");
	    cellHead = rowHead.createCell((short)15);
	    cellHead.setCellValue("经费人所属学院");
	    cellHead = rowHead.createCell((short)16);
	    cellHead.setCellValue("经费人工资号");
	    cellHead = rowHead.createCell((short)17);
	    cellHead.setCellValue("经费卡号");
	    
	    int count = 1;
	    Map feeMap = new HashMap();
	    
	    for (KjPatentFee pt:kpfsYear){
	    	
	    	if(pt.getPatentId()==null) continue;
	    	if(KjPatentDao.getInstance().findByPk(pt.getPatentId().getPatentId())==null)  continue;
	    	
	    	if(feeMap.get(pt.getId())==null)
	    	{
	    		feeMap.put(pt.getId(), "true");
	    	}
	    	else  continue;
	    	
	    	if(pt.getExpiryDay()==null) continue;
	    	
	    	
	    	KjPatent kp= pt.getPatentId();
	    	HSSFRow row = sheet.createRow(count++);
	    	HSSFCell cell = row.createCell((short)0);
	    	cell.setCellValue(pt.getPatentId().getPatentNo());
	    	cell = row.createCell((short)1);
	    	cell.setCellValue(pt.getPatentId().getName());
	    	cell = row.createCell((short)2);
	    	cell.setCellValue(KjDictionaryDao.getInstance().findByHQL(
	    			"from KjDictionary as kj where kj.kjDictionaryPK.domainId="
	    			+pt.getPatentId().getType()+ "and kj.kjDictionaryPK.typeId='DIC.PATENT.TYPE'").get(0).getDomainValue());
	    	cell = row.createCell((short)3);
	    	if(pt.getPatentId().getKjFirstInventorId()!=null){
	    		cell.setCellValue(pt.getPatentId().getKjFirstInventorId().getName());
	    	}else{
	    		cell.setCellValue("");
	    	}
	    	
    	    if(pt.getPatentId().getDeptId()!=null)
		    {
    		 cell = row.createCell((short)4);
 		     cell.setCellValue(KjGroupDAO.getInstance().findByPk(Long.parseLong(pt.getPatentId().getDeptId())).getGroupName());
		    }
		    else{
		    	cell = row.createCell((short)4);
		    	cell.setCellValue((pt.getPatentId().getDeptName()));
		    }
    	    SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd");   
    	    
    	    cell = row.createCell((short)5);
	    	cell.setCellValue("第"+pt.getYear()+"年年费");
    	    cell = row.createCell((short)6);
		    cell.setCellValue(sdf.format(pt.getExpiryDay()));
		    cell = row.createCell((short)7);
		    cell.setCellValue(pt.getAmount());
		    
	    	 for(int j =0;j<kpfsOverdue.size();j++)
	    	 {
	    		 if(feeMap.get(kpfsOverdue.get(j).getPatentId())!=null) continue;
	    		 
	    		 if(kpfsOverdue.get(j).getPatentId()!=null&&kpfsOverdue.get(j).getPatentId().getPatentId().toString().equals(pt.getPatentId().getPatentId().toString()))
	    		 {
			    		cell = row.createCell((short)8);
			    		cell.setCellValue("滞纳费");
			 	    	if(kpfsOverdue.get(j).getExpiryDay()!=null){
				 	    	cell = row.createCell((short)9);  
						    cell.setCellValue(sdf.format(kpfsOverdue.get(j).getExpiryDay()));
			 	    	}
					    cell = row.createCell((short)10);
					    cell.setCellValue(kpfsOverdue.get(j).getAmount());
					    feeMap.put(kpfsOverdue.get(j).getId(), "true");
			    		break;
			    	}
			    }
    	    
	   	     for(int j =0;j<kpfsRecover.size();j++)
	   	     {
	   	    	if(feeMap.get(kpfsRecover.get(j).getPatentId())!=null) continue;
	   	    	
			    	if(kpfsRecover.get(j).getPatentId()!=null&&kpfsRecover.get(j).getPatentId().getPatentId().toString().equals(pt.getPatentId().getPatentId().toString())){
			    		cell = row.createCell((short)11);
			    		cell.setCellValue("恢复费");
			 	    	if(kpfsRecover.get(j).getExpiryDay()!=null){
				 	    	cell = row.createCell((short)12);  
						    cell.setCellValue(sdf.format(kpfsRecover.get(j).getExpiryDay()));
			 	    	}
					    cell = row.createCell((short)13);
					    cell.setCellValue(kpfsRecover.get(j).getAmount());
					    feeMap.put(kpfsRecover.get(j).getId(), "true");
			    		break;
			    	}
			    	
			    }
		    
		   if(kp.getFundsPeople()!=null){
		    cell = row.createCell((short)14);
		    cell.setCellValue(kp.getFundsPeople().getName());
		    cell = row.createCell((short)15);
		    if(kp.getFundsPeople().getCollegeId()!=null && KjGroupDAO.getInstance().findByPk(Long.parseLong(kp.getFundsPeople().getCollegeId()))!=null)
		    {
		    	cell.setCellValue(KjGroupDAO.getInstance().findByPk(Long.parseLong(kp.getFundsPeople().getCollegeId())).getGroupName());
		    }
		    else
		    {
		    	cell.setCellValue("");
		    }
		    
		    
		    cell = row.createCell((short)16);
		    cell.setCellValue(kp.getFundsPeople().getStaffId());
		    cell = row.createCell((short)17);
		    cell.setCellValue(kp.getFundsPeople().getFundsCardId());
		   }
		    
	    }
	    
	    //zz 2014-10-30 (1)
	    for (KjPatentFee pt:kpfsOverdue){
	    	
	    	if(pt.getPatentId()==null) continue;
	    	if(KjPatentDao.getInstance().findByPk(pt.getPatentId().getPatentId())==null)  continue;
	    	
	    	if(feeMap.get(pt.getId())==null)
	    	{
	    		feeMap.put(pt.getId(), "true");
	    	}
	    	else  continue;
	    	if(pt.getExpiryDay()==null) continue;
	    	
	    	
	    	KjPatent kp= pt.getPatentId();
	    	HSSFRow row = sheet.createRow(count++);
	    	HSSFCell cell = row.createCell((short)0);
	    	cell.setCellValue(pt.getPatentId().getPatentNo());
	    	cell = row.createCell((short)1);
	    	cell.setCellValue(pt.getPatentId().getName());
	    	cell = row.createCell((short)2);
	    	cell.setCellValue(KjDictionaryDao.getInstance().findByHQL(
	    			"from KjDictionary as kj where kj.kjDictionaryPK.domainId="
	    			+pt.getPatentId().getType()+ "and kj.kjDictionaryPK.typeId='DIC.PATENT.TYPE'").get(0).getDomainValue());
	    	cell = row.createCell((short)3);
	    	if(pt.getPatentId().getKjFirstInventorId()!=null){
	    		cell.setCellValue(pt.getPatentId().getKjFirstInventorId().getName());
	    	}else{
	    		cell.setCellValue("");
	    	}
	    	
    	    if(pt.getPatentId().getDeptId()!=null)
		    {
    		 cell = row.createCell((short)4);
 		     cell.setCellValue(KjGroupDAO.getInstance().findByPk(Long.parseLong(pt.getPatentId().getDeptId())).getGroupName());
		    }
		    else{
		    	cell = row.createCell((short)4);
		    	cell.setCellValue((pt.getPatentId().getDeptName()));
		    }
    	    SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd");   
    	    
    		cell = row.createCell((short)8);
    		cell.setCellValue("滞纳费");
 	    	if(pt.getExpiryDay()!=null){
	 	    	cell = row.createCell((short)9);  
			    cell.setCellValue(sdf.format(pt.getExpiryDay()));
 	    	}
		    cell = row.createCell((short)10);
		    cell.setCellValue(pt.getAmount());
    	    
		   if(kp.getFundsPeople()!=null){
			    cell = row.createCell((short)14);
			    cell.setCellValue(kp.getFundsPeople().getName());   //经费卡持有人
			    if(kp.getFundsPeople().getCollegeId()!=null){
			    	cell = row.createCell((short)15);
			    	cell.setCellValue(KjGroupDAO.getInstance().findByPk(Long.parseLong(kp.getFundsPeople().getCollegeId())).getGroupName());
			    }else{
			    	cell = row.createCell((short)15);
			    	cell.setCellValue("");
			    }	
		    	cell = row.createCell((short)16);
			    cell.setCellValue(kp.getFundsPeople().getStaffId());
			    if(kp.getFundCardNo()!=null && kp.getFundCardNo().length()>0){
			    	cell = row.createCell((short)17);
				    cell.setCellValue(FundCardDao.getInstance().findByPk(Long.parseLong(kp.getFundCardNo())).getFundCardNo());
			    }
			    System.out.println("f:"+FundCardDao.getInstance().findByPk(Long.parseLong(kp.getFundCardNo())).getFundCardNo());
		   }
	    }
	    
	  //zz 2014-10-30 (2)
	    for (KjPatentFee pt:kpfsRecover){
	    	
	    	if(pt.getPatentId()==null) continue;
	    	if(KjPatentDao.getInstance().findByPk(pt.getPatentId().getPatentId())==null)  continue;
	    	
	    	if(feeMap.get(pt.getId())==null)
	    	{
	    		feeMap.put(pt.getId(), "true");
	    	}
	    	else  continue;
	    	if(pt.getExpiryDay()==null) continue;
	    	
	    	
	    	KjPatent kp= pt.getPatentId();
	    	HSSFRow row = sheet.createRow(count++);
	    	HSSFCell cell = row.createCell((short)0);
	    	cell.setCellValue(pt.getPatentId().getPatentNo());
	    	cell = row.createCell((short)1);
	    	cell.setCellValue(pt.getPatentId().getName());
	    	cell = row.createCell((short)2);
	    	cell.setCellValue(KjDictionaryDao.getInstance().findByHQL(
	    			"from KjDictionary as kj where kj.kjDictionaryPK.domainId="
	    			+pt.getPatentId().getType()+ "and kj.kjDictionaryPK.typeId='DIC.PATENT.TYPE'").get(0).getDomainValue());
	    	cell = row.createCell((short)3);
	    	if(pt.getPatentId().getKjFirstInventorId()!=null){
	    		cell.setCellValue(pt.getPatentId().getKjFirstInventorId().getName());
	    	}else{
	    		cell.setCellValue("");
	    	}
	    	
    	    if(pt.getPatentId().getDeptId()!=null)
		    {
    		 cell = row.createCell((short)4);
 		     cell.setCellValue(KjGroupDAO.getInstance().findByPk(Long.parseLong(pt.getPatentId().getDeptId())).getGroupName());
		    }
		    else{
		    	cell = row.createCell((short)4);
		    	cell.setCellValue((pt.getPatentId().getDeptName()));
		    }
    	    SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd");   
    	    
    		cell = row.createCell((short)11);
    		cell.setCellValue("恢复费");
 	    	if(pt.getExpiryDay()!=null){
	 	    	cell = row.createCell((short)12);  
			    cell.setCellValue(sdf.format(pt.getExpiryDay()));
 	    	}
		    cell = row.createCell((short)13);
		    cell.setCellValue(pt.getAmount());
    	    
		    
		    if(kp.getFundsPeople()!=null){
			    cell = row.createCell((short)14);
			    cell.setCellValue(kp.getFundsPeople().getName());   //经费卡持有人
			    if(kp.getFundsPeople().getCollegeId()!=null){
			    	cell = row.createCell((short)15);
			    	cell.setCellValue(KjGroupDAO.getInstance().findByPk(Long.parseLong(kp.getFundsPeople().getCollegeId())).getGroupName());
			    }else{
			    	cell = row.createCell((short)15);
			    	cell.setCellValue("");
			    }	
		    	cell = row.createCell((short)16);
			    cell.setCellValue(kp.getFundsPeople().getStaffId());
			    if(kp.getFundCardNo()!=null && kp.getFundCardNo().length()>0){
			    	cell = row.createCell((short)17);
				    cell.setCellValue(FundCardDao.getInstance().findByPk(Long.parseLong(kp.getFundCardNo())).getFundCardNo());
			    }
			    System.out.println("f:"+FundCardDao.getInstance().findByPk(Long.parseLong(kp.getFundCardNo())).getFundCardNo());
		   }
		    
	    }

	    //输出
	    SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd");   
		response.addHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode("annelFeeList"+sdf.format(new Date())+".xls", "UTF-8"));  
        response.setContentType("application/vnd.msexcel;charset=UTF-8");  
        OutputStream out = response.getOutputStream();  
		wb.write(out);
		
		out.flush();
		out.close();
		
		String doUpdate = ParamUtil.getParameter(request, "doUpdate");
		if(doUpdate.equals("1"))
		{
			Object[] feeIds = feeMap.keySet().toArray();
			
			for(int i = 0;i<feeIds.length;i++){
				KjPatentFee kps = KjPatentFeeDao.getInstance().findByPk((Long)feeIds[i]);
				String oldState=kps.getPatentId().getState();
				kps.setIsPaid(1L);
				if(oldState.equals("91") || oldState.equals("92") ){
					KjPatent kp=KjPatentDao.getInstance().findByPk(kps.getPatentId().getPatentId());
					kp.setState("33");
					
					KjAuditLog auditlog = new KjAuditLog();
					auditlog.setKjDictObjtype(KjDictObjtypeDAO.getInstance().findByPk(15l));//1
					auditlog.setObjId(kps.getPatentId().getPatentId());//2
					auditlog.setOper("更改专利状态从："+KjPatentStateDao.getInstance().searchByCode(oldState).getName()+"  到："
							+KjPatentStateDao.getInstance().searchByCode(kps.getPatentId().getState()).getName());// 一般是指数据对象 从什么状态 转到什么状态 3
					auditlog.setOperTime(DateUtil.getDate(new Date()));
					auditlog.setOperMsg(KjUserDAO.getInstance().findByPk(userId).getName() + "确认缴费");// 一般指谁进行了什么操作 如 宁xx审核、段xx修改、xxx删除 4
					auditlog.setOperUser(userId);
					KjAuditLogDAO.getInstance().save(auditlog);
					
					KjPatentDao.getInstance().update(kp);
				}
				KjPatentFeeDao.getInstance().update(kps);
				
				KjAuditLog auditlog = new KjAuditLog();
				auditlog.setKjDictObjtype(KjDictObjtypeDAO.getInstance().findByPk(15l));//1
				auditlog.setObjId(kps.getPatentId().getPatentId());//2
				
				String oper = "";
				if(kps.getConfigId()!=null)
				{
					if(kps.getConfigId().getId().intValue() == AnnelFeeConfigId)
					{
						oper = "确认缴纳第"+kps.getYear()+"年年费：";
					}
					else if(kps.getConfigId().getId().intValue() == overDueFeeConfigId)
					{
						oper = "确认缴纳滞纳金";
					}
					else if(kps.getConfigId().getId().intValue() == recoverFeeConfigId)
					{
						oper = "确认缴纳恢复金";
					}
				}
				
				auditlog.setOper(oper);// 一般是指数据对象 从什么状态 转到什么状态 3
				auditlog.setOperTime(DateUtil.getDate(new Date()));
				auditlog.setOperMsg(KjUserDAO.getInstance().findByPk(userId).getName() + "确认缴费");// 一般指谁进行了什么操作 如 宁xx审核、段xx修改、xxx删除 4
				auditlog.setOperUser(userId);
				KjAuditLogDAO.getInstance().save(auditlog);
			}
		}
		
		
		
	}

	public void confirmOffice(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)throws DAOException, IOException  {
		HttpSession session=request.getSession();
		UserBean userbean = (UserBean) session.getAttribute("UserBean");
		String userId = userbean.getUid();
		//获取用户的身份
		String sign = ((KjDictIdentity)session.getAttribute("identity")).getSign();
		
		String sqlWhere = "";//所有官费的列表
		sqlWhere=" as kj where kj.isPaid is not null and ( kj.configId.type='office') and kj.expiryDay is not null and kj.patentId.deleteMark <1 ";
		sqlWhere=QuickSearch.feeForm(sqlWhere, form, "isPaid,expiryDay");
		session.setAttribute("sqlWhere",sqlWhere);
			
		KjPatentFeeDao dao = KjPatentFeeDao.getInstance();
		List<KjPatentFee> kpfs=dao.findByHQL("from KjPatentFee "+sqlWhere);
				
		HSSFWorkbook wb = new HSSFWorkbook();           // or new XSSFWorkbook();
		HSSFSheet sheet = wb.createSheet("官费清单");
	    
	    //标题
		HSSFRow rowHead = sheet.createRow(0);
		HSSFCell cellHead = rowHead.createCell((short) 0);
	    cellHead.setCellValue("专利号");
	    cellHead = rowHead.createCell((short)1);
	    cellHead.setCellValue("专利名称");
	    cellHead = rowHead.createCell((short)2);
	    cellHead.setCellValue("专利类型");
	    cellHead = rowHead.createCell((short)3);
	    cellHead.setCellValue("第一发明人");
	    cellHead = rowHead.createCell((short)4);
	    cellHead.setCellValue("所属学院");
	    cellHead = rowHead.createCell((short)5);
	    cellHead.setCellValue("专利状态");
	    cellHead = rowHead.createCell((short)6);
	    cellHead.setCellValue("费用名称");    
	    cellHead = rowHead.createCell((short)7);
	    cellHead.setCellValue("到期日期");
	    cellHead = rowHead.createCell((short)8);
	    cellHead.setCellValue("应缴金额");

	    
	    int count = 1;	    
	    for (KjPatentFee pt:kpfs){
	    	
	    	if(pt.getPatentId()==null) continue;
	    	if(KjPatentDao.getInstance().findByPk(pt.getPatentId().getPatentId())==null)  continue;	    		    	
	    	if(pt.getExpiryDay()==null) continue;
	    	    	
	    	KjPatent kp= pt.getPatentId();
	    	HSSFRow row = sheet.createRow(count++);
	    	HSSFCell cell = row.createCell((short)0);
	    	cell.setCellValue(pt.getPatentId().getPatentNo());
	    	cell = row.createCell((short)1);
	    	cell.setCellValue(pt.getPatentId().getName());
	    	cell = row.createCell((short)2);
	    	cell.setCellValue(KjDictionaryDao.getInstance().findByHQL(
	    			"from KjDictionary as kj where kj.kjDictionaryPK.domainId="
	    			+pt.getPatentId().getType()+ "and kj.kjDictionaryPK.typeId='DIC.PATENT.TYPE'").get(0).getDomainValue());
	    	cell = row.createCell((short)3);
	    	if(pt.getPatentId().getKjFirstInventorId()!=null){
	    		cell.setCellValue(pt.getPatentId().getKjFirstInventorId().getName());
	    	}else{
	    		cell.setCellValue("");
	    	}
	    	
    	    if(pt.getPatentId().getDeptId()!=null)
		    {
    		 cell = row.createCell((short)4);
 		     cell.setCellValue(KjGroupDAO.getInstance().findByPk(Long.parseLong(pt.getPatentId().getDeptId())).getGroupName());
		    }
		    else{
		    	cell = row.createCell((short)4);
		    	cell.setCellValue((pt.getPatentId().getDeptName()));
		    }
    	    SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd");   
    	    
    	    cell = row.createCell((short)5);
	    	cell.setCellValue(pt.getPatentId().getStateName());
    	    
    	    cell = row.createCell((short)6);
	    	cell.setCellValue(pt.getConfigId().getName());
    	    cell = row.createCell((short)7);
		    cell.setCellValue(sdf.format(pt.getExpiryDay()));
		    cell = row.createCell((short)8);
		    cell.setCellValue(pt.getAmount());	
	    }
	    SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd");   
	    //输出 
		response.addHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode("officeFeeList"+sdf.format(new Date())+".xls", "UTF-8"));  
        response.setContentType("application/vnd.msexcel;charset=UTF-8");  
        OutputStream out = response.getOutputStream();  
		wb.write(out);
		
		
		out.flush();
		out.close();
		
		String doUpdate = ParamUtil.getParameter(request, "doUpdate");
		if(doUpdate.equals("1"))
		{		
			for(int i = 0;i<kpfs.size();i++){
				KjPatentFee kps = kpfs.get(i);
				String oldState=kps.getPatentId().getState();
				kps.setIsPaid(1L);
				KjPatentFeeDao.getInstance().update(kps);
				
				KjAuditLog auditlog = new KjAuditLog();
				auditlog.setKjDictObjtype(KjDictObjtypeDAO.getInstance().findByPk(15l));//1
				auditlog.setObjId(kps.getPatentId().getPatentId());//2
				
				String oper = "";
				if(kps.getConfigId()!=null)
				{
					oper = "确认缴纳"+kps.getConfigId().getName()+"：";
					
				}
				
				auditlog.setOper(oper);// 一般是指数据对象 从什么状态 转到什么状态 3
				auditlog.setOperTime(DateUtil.getDate(new Date()));
				auditlog.setOperMsg(KjUserDAO.getInstance().findByPk(userId).getName() + "确认缴费");// 一般指谁进行了什么操作 如 宁xx审核、段xx修改、xxx删除 4
				auditlog.setOperUser(userId);
				KjAuditLogDAO.getInstance().save(auditlog);
			}
		}
	}
	/*
	public void commonFeeExport(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)throws DAOException, IOException  {
		HttpSession session=request.getSession();
		UserBean userbean = (UserBean) session.getAttribute("UserBean");
		String userId = userbean.getUid();
		//获取用户的身份
		String sign = ((KjDictIdentity)session.getAttribute("identity")).getSign();
		String confirm=ParamUtil.getParameterFrom(request, "confirm");  //判断是否是通过确认已缴费的操作
				
		String sqlWhere = "";
		if(sign.equals("Admin")){
			
//			sqlWhere=" as kj where kj.isPaid=0 and kj.configId.id in ("+AnnelFeeConfigId+","+overDueFeeConfigId+","+recoverFeeConfigId+")  and kj.expiryDay is not null";
			
			sqlWhere=" as kj where kj.isPaid=0 and (kj.configId.type='year' or kj.configId.type='other' or kj.configId.type='auth') and kj.expiryDay is not null  and kj.patentId.patentId is not null and kj.patentId.deleteMark<1  ";
			sqlWhere=QuickSearch.feeForm(sqlWhere, form, "expiryDay");
			session.setAttribute("sqlWhere",sqlWhere);
		}
		else if(sign.equals("User")){
			sqlWhere=" as kj where kj.isPaid=0 and (kj.configId.type='year' or kj.configId.type='other' or kj.configId.type='auth') and kj.expiryDay is not null "+
			"and kj.patentId.patentId is not null and kj.patentId.deleteMark<1 and (kj.patentId.kjFirstInventorId.staffId='"+userId+"'or kj.patentId.patentId in (select kjPatent.patentId from KjProjectstaff where kjUser.staffId='"+userId+"')) ";
			sqlWhere=QuickSearch.feeForm(sqlWhere, form, "expiryDay");
			session.setAttribute("sqlWhere",sqlWhere);
		}
		else if(sign.equals("Manager")){
			sqlWhere=" as kj where kj.isPaid=0 and (kj.configId.type='year' or kj.configId.type='other' or kj.configId.type='auth') and kj.expiryDay is not null  and kj.patentId.patentId is not null and kj.patentId.deleteMark<1  "+
			" and kj.patentId.deptId='"+userbean.getCollege()+"'";
			//获取科研组信息
			String kjCollege = userbean.getCollege();
			KjPatentFeeForm Form = (KjPatentFeeForm) form;
			Form.setCollegeId(kjCollege);
			sqlWhere=QuickSearch.feeForm(sqlWhere, Form, "expiryDay");
			session.setAttribute("sqlWhere",sqlWhere);
		}
		KjPatentFeeDao dao = KjPatentFeeDao.getInstance();
		int currentCount = SysConfig.START_COUNT;
		int pageSize = SysConfig.PAGE_SIZE;
		if (request.getParameter("page") != null){
			currentCount = Integer.parseInt((request.getParameter("page")));
		}
		if (request.getParameter("pageSize") != null){
			pageSize = Integer.parseInt((request.getParameter("pageSize")));
		}
		
		//过滤掉多余的费用记录
		Map patentIdMap = new HashMap();			 //判断当前patentId是否已经存在的map
		List<KjPatentFee> patentFeeListAll = dao.getList(sqlWhere);
		List<KjPatentFee> patentFeeListAll_filter = new ArrayList<KjPatentFee>();
		List<KjPatentFee> patentFeeList = new ArrayList<KjPatentFee>();
//		System.out.println(sqlWhere);
//		System.out.println("s:"+patentFeeListAll.size());
		if(patentFeeListAll!=null){
			for(int i=0; i<patentFeeListAll.size(); i++){       
				if(i==0){
					patentFeeListAll_filter.add(patentFeeListAll.get(0));
					patentIdMap.put(patentFeeListAll.get(0).getPatentId().getPatentId(), 1);    //讲patentId存入map,用于后续的校验
				}else{
					if(!patentIdMap.containsKey(patentFeeListAll.get(i).getPatentId().getPatentId())){   //如果没有将patentId对应的费用加入list,则在此加入
						patentFeeListAll_filter.add(patentFeeListAll.get(i));
						patentIdMap.put(patentFeeListAll.get(i).getPatentId().getPatentId(), 1);    //讲patentId存入map,用于后续的校验
					}
				}
			}
		}
		
		
	}
	*/
	public ActionForward previewRemindUpdate(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)throws DAOException, IOException  {
			HttpSession session=request.getSession();
			UserBean userbean = (UserBean) session.getAttribute("UserBean");
			String userId = userbean.getUid();
			
			String sign = ((KjDictIdentity)session.getAttribute("identity")).getSign();
			String confirm=ParamUtil.getParameterFrom(request, "confirm");  //判断是否是通过确认已缴费的操作
					
			String sqlWhere = "";
			if(sign.equals("Admin")){
				
//				sqlWhere=" as kj where kj.isPaid=0 and kj.configId.id in ("+AnnelFeeConfigId+","+overDueFeeConfigId+","+recoverFeeConfigId+")  and kj.expiryDay is not null";
				
				sqlWhere=" as kj where kj.isPaid=0 and (kj.configId.type='remind' or kj.configId.type='other' or kj.configId.type='auth') and kj.expiryDay is not null  and kj.patentId.patentId is not null and kj.patentId.deleteMark<1  ";
				sqlWhere=QuickSearch.feeForm(sqlWhere, form, "expiryDay");
				session.setAttribute("sqlWhere",sqlWhere);
			}
			else if(sign.equals("User")){
				sqlWhere=" as kj where kj.isPaid=0 and (kj.configId.type='remind' or kj.configId.type='other' or kj.configId.type='auth') and kj.expiryDay is not null "+
				"and kj.patentId.patentId is not null and kj.patentId.deleteMark<1 and (kj.patentId.kjFirstInventorId.staffId='"+userId+"'or kj.patentId.patentId in (select kjPatent.patentId from KjProjectstaff where kjUser.staffId='"+userId+"')) ";
				sqlWhere=QuickSearch.feeForm(sqlWhere, form, "expiryDay");
				session.setAttribute("sqlWhere",sqlWhere);
			}
			else if(sign.equals("Manager")){
				
				sqlWhere = " as kj where 1=1 ";
				if(userbean.getOtherManagerCollege()!=null && userbean.getOtherManagerCollege().length()>0)
				{
					sqlWhere = sqlWhere + " and (kj.patentId.deptId='"+userbean.getCollege()+"' or kj.patentId.deptId='"+userbean.getOtherManagerCollege()+"'  )  ";
				}
				else
				{
					sqlWhere = sqlWhere + " and kj.patentId.deptId='"+userbean.getCollege()+"' ";
				}
				
				sqlWhere = sqlWhere + " and kj.isPaid=0 and (kj.configId.type='remind' or kj.configId.type='other' or kj.configId.type='auth') and kj.expiryDay is not null  and kj.patentId.patentId is not null and kj.patentId.deleteMark<1  "
//				+" and kj.patentId.deptId='"+userbean.getCollege()+"'"
				;
				//获取科研组信息
				String kjCollege = userbean.getCollege();
				KjPatentFeeForm Form = (KjPatentFeeForm) form;
				Form.setCollegeId(kjCollege);
				sqlWhere=QuickSearch.feeForm(sqlWhere, Form, "expiryDay");
				session.setAttribute("sqlWhere",sqlWhere);
			}
			KjPatentFeeDao dao = KjPatentFeeDao.getInstance();
			int currentCount = SysConfig.START_COUNT;
			int pageSize = SysConfig.PAGE_SIZE;

			//过滤掉多余的费用记录
			Map patentIdMap = new HashMap();			 //判断当前patentId是否已经存在的map
			List<KjPatentFee> patentFeeListAll = dao.getList(sqlWhere);
			List<KjPatentFee> patentFeeListAll_filter = new ArrayList<KjPatentFee>();
			List<KjPatentFee> patentFeeList = new ArrayList<KjPatentFee>();
//			System.out.println(sqlWhere);
//			System.out.println("s:"+patentFeeListAll.size());
			if(patentFeeListAll!=null){
				for(int i=0; i<patentFeeListAll.size(); i++){       
					if(i==0){
						patentFeeListAll_filter.add(patentFeeListAll.get(0));
						patentIdMap.put(patentFeeListAll.get(0).getPatentId().getPatentId(), 1);    //讲patentId存入map,用于后续的校验
					}else{
						if(!patentIdMap.containsKey(patentFeeListAll.get(i).getPatentId().getPatentId())){   //如果没有将patentId对应的费用加入list,则在此加入
							patentFeeListAll_filter.add(patentFeeListAll.get(i));
							patentIdMap.put(patentFeeListAll.get(i).getPatentId().getPatentId(), 1);    //讲patentId存入map,用于后续的校验
						}
					}
				}
			}
			request.setAttribute("patentFeeList", patentFeeListAll_filter);
			return new ActionForward("/fee/previewYearUpdate.jsp");	
		}
	
	public ActionForward previewYearUpdate(ActionMapping mapping, ActionForm form,
		HttpServletRequest request, HttpServletResponse response)throws DAOException, IOException  {
		HttpSession session=request.getSession();
		UserBean userbean = (UserBean) session.getAttribute("UserBean");
		String userId = userbean.getUid();
		
		String sign = ((KjDictIdentity)session.getAttribute("identity")).getSign();
		String confirm=ParamUtil.getParameterFrom(request, "confirm");  //判断是否是通过确认已缴费的操作
				
		String sqlWhere = "";
		if(sign.equals("Admin")){
			
//			sqlWhere=" as kj where kj.isPaid=0 and kj.configId.id in ("+AnnelFeeConfigId+","+overDueFeeConfigId+","+recoverFeeConfigId+")  and kj.expiryDay is not null";
			
			sqlWhere=" as kj where kj.isPaid=0 and (kj.configId.type='year' or kj.configId.type='other' or kj.configId.type='auth') and kj.expiryDay is not null  and kj.patentId.patentId is not null and kj.patentId.deleteMark<1  ";
			sqlWhere=QuickSearch.feeForm(sqlWhere, form, "expiryDay");
			session.setAttribute("sqlWhere",sqlWhere);
		}
		else if(sign.equals("User")){
			sqlWhere=" as kj where kj.isPaid=0 and (kj.configId.type='year' or kj.configId.type='other' or kj.configId.type='auth') and kj.expiryDay is not null "+
			"and kj.patentId.patentId is not null and kj.patentId.deleteMark<1 and (kj.patentId.kjFirstInventorId.staffId='"+userId+"'or kj.patentId.patentId in (select kjPatent.patentId from KjProjectstaff where kjUser.staffId='"+userId+"')) ";
			sqlWhere=QuickSearch.feeForm(sqlWhere, form, "expiryDay");
			session.setAttribute("sqlWhere",sqlWhere);
		}
		else if(sign.equals("Manager")){
			
			sqlWhere = " as kj where 1=1 ";
			if(userbean.getOtherManagerCollege()!=null && userbean.getOtherManagerCollege().length()>0)
			{
				sqlWhere = sqlWhere + " and (kj.patentId.deptId='"+userbean.getCollege()+"' or kj.patentId.deptId='"+userbean.getOtherManagerCollege()+"'  )  ";
			}
			else
			{
				sqlWhere = sqlWhere + " and kj.patentId.deptId='"+userbean.getCollege()+"' ";
			}
			
			sqlWhere = sqlWhere + " and kj.isPaid=0 and (kj.configId.type='year' or kj.configId.type='other' or kj.configId.type='auth') and kj.expiryDay is not null  and kj.patentId.patentId is not null and kj.patentId.deleteMark<1  "
//			+" and kj.patentId.deptId='"+userbean.getCollege()+"'"
			;
			//获取科研组信息
			String kjCollege = userbean.getCollege();
			KjPatentFeeForm Form = (KjPatentFeeForm) form;
			Form.setCollegeId(kjCollege);
			sqlWhere=QuickSearch.feeForm(sqlWhere, Form, "expiryDay");
			session.setAttribute("sqlWhere",sqlWhere);
		}
		KjPatentFeeDao dao = KjPatentFeeDao.getInstance();
		int currentCount = SysConfig.START_COUNT;
		int pageSize = SysConfig.PAGE_SIZE;

		//过滤掉多余的费用记录
		Map patentIdMap = new HashMap();			 //判断当前patentId是否已经存在的map
		List<KjPatentFee> patentFeeListAll = dao.getList(sqlWhere);
		List<KjPatentFee> patentFeeListAll_filter = new ArrayList<KjPatentFee>();
		List<KjPatentFee> patentFeeList = new ArrayList<KjPatentFee>();
//		System.out.println(sqlWhere);
//		System.out.println("s:"+patentFeeListAll.size());
		if(patentFeeListAll!=null){
			for(int i=0; i<patentFeeListAll.size(); i++){       
				if(i==0){
					patentFeeListAll_filter.add(patentFeeListAll.get(0));
					patentIdMap.put(patentFeeListAll.get(0).getPatentId().getPatentId(), 1);    //讲patentId存入map,用于后续的校验
				}else{
					if(!patentIdMap.containsKey(patentFeeListAll.get(i).getPatentId().getPatentId())){   //如果没有将patentId对应的费用加入list,则在此加入
						patentFeeListAll_filter.add(patentFeeListAll.get(i));
						patentIdMap.put(patentFeeListAll.get(i).getPatentId().getPatentId(), 1);    //讲patentId存入map,用于后续的校验
					}
				}
			}
		}
		request.setAttribute("patentFeeList", patentFeeListAll_filter);
		return new ActionForward("/fee/previewYearUpdate.jsp");	
	}
	
	public void outputYear(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)throws DAOException, IOException  {
		HttpSession session=request.getSession();
		UserBean userbean = (UserBean) session.getAttribute("UserBean");
		String userId = userbean.getUid();
		
		
		
		String sqlWhere = "";//所有官费的列表
		sqlWhere=" as kj where kj.isPaid is not null and ( kj.configId <=10 or kj.configId=13 or kj.configId=15) and kj.expiryDay is not null";
		sqlWhere=QuickSearch.feeForm(sqlWhere, form, "isPaid,expiryDay");
		session.setAttribute("sqlWhere",sqlWhere);
			
		KjPatentFeeDao dao = KjPatentFeeDao.getInstance();
		List<KjPatentFee> kpfs=dao.findByHQL("from KjPatentFee "+sqlWhere);
				
		HSSFWorkbook wb = new HSSFWorkbook();           // or new XSSFWorkbook();
		HSSFSheet sheet = wb.createSheet("官费清单");
	    
	    //标题
		HSSFRow rowHead = sheet.createRow(0);
		HSSFCell cellHead = rowHead.createCell((short) 0);
	    cellHead.setCellValue("专利号");
	    cellHead = rowHead.createCell((short)1);
	    cellHead.setCellValue("专利名称");
	    cellHead = rowHead.createCell((short)2);
	    cellHead.setCellValue("专利类型");
	    cellHead = rowHead.createCell((short)3);
	    cellHead.setCellValue("第一发明人");
	    cellHead = rowHead.createCell((short)4);
	    cellHead.setCellValue("所属学院");
	    cellHead = rowHead.createCell((short)5);
	    cellHead.setCellValue("专利状态");
	    cellHead = rowHead.createCell((short)6);
	    cellHead.setCellValue("费用名称");    
	    cellHead = rowHead.createCell((short)7);
	    cellHead.setCellValue("到期日期");
	    cellHead = rowHead.createCell((short)8);
	    cellHead.setCellValue("应缴金额");

	    
	    int count = 1;	    
	    for (KjPatentFee pt:kpfs){
	    	
	    	if(pt.getPatentId()==null) continue;
	    	if(KjPatentDao.getInstance().findByPk(pt.getPatentId().getPatentId())==null)  continue;	    		    	
	    	if(pt.getExpiryDay()==null) continue;
	    	    	
	    	KjPatent kp= pt.getPatentId();
	    	HSSFRow row = sheet.createRow(count++);
	    	HSSFCell cell = row.createCell((short)0);
	    	cell.setCellValue(pt.getPatentId().getPatentNo());
	    	cell = row.createCell((short)1);
	    	cell.setCellValue(pt.getPatentId().getName());
	    	cell = row.createCell((short)2);
	    	cell.setCellValue(KjDictionaryDao.getInstance().findByHQL(
	    			"from KjDictionary as kj where kj.kjDictionaryPK.domainId="
	    			+pt.getPatentId().getType()+ "and kj.kjDictionaryPK.typeId='DIC.PATENT.TYPE'").get(0).getDomainValue());
	    	cell = row.createCell((short)3);
	    	if(pt.getPatentId().getKjFirstInventorId()!=null){
	    		cell.setCellValue(pt.getPatentId().getKjFirstInventorId().getName());
	    	}else{
	    		cell.setCellValue("");
	    	}
	    	
    	    if(pt.getPatentId().getDeptId()!=null)
		    {
    		 cell = row.createCell((short)4);
 		     cell.setCellValue(KjGroupDAO.getInstance().findByPk(Long.parseLong(pt.getPatentId().getDeptId())).getGroupName());
		    }
		    else{
		    	cell = row.createCell((short)4);
		    	cell.setCellValue((pt.getPatentId().getDeptName()));
		    }
    	    SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd");   
    	    
    	    cell = row.createCell((short)5);
	    	cell.setCellValue(pt.getPatentId().getStateName());
    	    
    	    cell = row.createCell((short)6);
	    	cell.setCellValue(pt.getConfigId().getName());
    	    cell = row.createCell((short)7);
		    cell.setCellValue(sdf.format(pt.getExpiryDay()));
		    cell = row.createCell((short)8);
		    cell.setCellValue(pt.getAmount());	
	    }
	    SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd");   
	    //输出 
		response.addHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode("officeFeeList"+sdf.format(new Date())+".xls", "UTF-8"));  
        response.setContentType("application/vnd.msexcel;charset=UTF-8");  
        OutputStream out = response.getOutputStream();  
		wb.write(out);
		
		
		out.flush();
		out.close();
		
		String doUpdate = ParamUtil.getParameter(request, "doUpdate");
		if(doUpdate.equals("1"))
		{		
			for(int i = 0;i<kpfs.size();i++){
				KjPatentFee kps = kpfs.get(i);
				String oldState=kps.getPatentId().getState();
				kps.setIsPaid(1L);
				KjPatentFeeDao.getInstance().update(kps);
				
				KjAuditLog auditlog = new KjAuditLog();
				auditlog.setKjDictObjtype(KjDictObjtypeDAO.getInstance().findByPk(15l));//1
				auditlog.setObjId(kps.getPatentId().getPatentId());//2
				
				String oper = "";
				if(kps.getConfigId()!=null)
				{
					oper = "确认缴纳"+kps.getConfigId().getName()+"：";
					
				}
				
				auditlog.setOper(oper);// 一般是指数据对象 从什么状态 转到什么状态 3
				auditlog.setOperTime(DateUtil.getDate(new Date()));
				auditlog.setOperMsg(KjUserDAO.getInstance().findByPk(userId).getName() + "确认缴费");// 一般指谁进行了什么操作 如 宁xx审核、段xx修改、xxx删除 4
				auditlog.setOperUser(userId);
				KjAuditLogDAO.getInstance().save(auditlog);
			}
		}
		
	}
	/*public ActionForward confirm(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)throws DAOException, IOException, NumberFormatException, ParseException  {
		
		HttpSession session=request.getSession();
		UserBean userbean = (UserBean) session.getAttribute("UserBean");
		String userId = userbean.getUid();
		//获取用户的身份
		String sign = ((KjDictIdentity)session.getAttribute("identity")).getSign();
		String sqlWhere = "";
		
		sqlWhere=" as kj where kj.isPaid=0 and kj.configId.id="+AnnelFeeConfigId;
		sqlWhere=QuickSearch.feeForm(sqlWhere, form, "expiryDay");
		session.setAttribute("sqlWhere",sqlWhere);
		KjPatentFeeDao dao = KjPatentFeeDao.getInstance();
		List<KjPatentFee> kpfs=dao.findByHQL("from KjPatentFee "+sqlWhere);
		int updates=0;
		int notUpdate=0;
		for(int i = 0;i<kpfs.size();i++){
			KjPatentFee kps=kpfs.get(i);
			String oldState=kps.getPatentId().getState();
			kps.setIsPaid(1L);
			if(kps.getPatentId().getState().equals("91")){
				KjPatent kp=KjPatentDao.getInstance().findByPk(kps.getPatentId().getPatentId());
				kp.setState("33");
				
				KjAuditLog auditlog = new KjAuditLog();
				auditlog.setKjDictObjtype(KjDictObjtypeDAO.getInstance().findByPk(15l));//1
				auditlog.setObjId(kps.getPatentId().getPatentId());//2
				auditlog.setOper("更改专利状态从："+KjPatentStateDao.getInstance().searchByCode(oldState).getName()+"  到："
						+KjPatentStateDao.getInstance().searchByCode(kps.getPatentId().getState()).getName());// 一般是指数据对象 从什么状态 转到什么状态 3
				auditlog.setOperTime(DateUtil.getDate(new Date()));
				auditlog.setOperMsg(KjUserDAO.getInstance().findByPk(userId).getName() + "确认缴费");// 一般指谁进行了什么操作 如 宁xx审核、段xx修改、xxx删除 4
				auditlog.setOperUser(userId);
				KjAuditLogDAO.getInstance().save(auditlog);
				
				updates++;
				KjPatentDao.getInstance().update(kp);
			}
			else{
				notUpdate++;
			}
			KjPatentFeeDao.getInstance().update(kps);
			
			KjAuditLog auditlog = new KjAuditLog();
			auditlog.setKjDictObjtype(KjDictObjtypeDAO.getInstance().findByPk(15l));//1
			auditlog.setObjId(kps.getPatentId().getPatentId());//2
			auditlog.setOper("确认缴纳第"+kps.getYear()+"年年费：");// 一般是指数据对象 从什么状态 转到什么状态 3
			auditlog.setOperTime(DateUtil.getDate(new Date()));
			auditlog.setOperMsg(KjUserDAO.getInstance().findByPk(userId).getName() + "确认缴费");// 一般指谁进行了什么操作 如 宁xx审核、段xx修改、xxx删除 4
			auditlog.setOperUser(userId);
			KjAuditLogDAO.getInstance().save(auditlog);
		}
		request.setAttribute("updates", updates);
		request.setAttribute("notUpdate", notUpdate);
		return new ActionForward("/fee.do?method=exportRemind&confirm=true");		
	}*/
	
	public ActionForward importFees(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response) throws DAOException, IOException {
		
		KjPatentFeeForm pfForm =(KjPatentFeeForm) form;
		List<FormFile> files=pfForm.getFiles();
		String file1="";
		String file2="";
		String file3="";
		for(int i=0;i<files.size();i++)
		{
			System.out.println(files.get(i).getFileName());
			String fileName=files.get(i).getFileName();
			String filePath=SysConfig.UPLOAD_PATH+SysConfig.getNow("yyyy-MM-dd");
			try {
				FileUtil.write(files.get(i), filePath, fileName);
			} catch (Exception e) {
				e.printStackTrace();
			}
			if(i==0){
				file1=filePath+"/"+fileName;
			}else if(i==1){
				file2=filePath+"/"+fileName;
			}else if(i==2){
				file3=filePath+"/"+fileName;
			}
			
		}

		KjPatentDao.getInstance().importFees(file2, file3, file1, request);
		return new ActionForward("/fee/importFees.jsp");	
	}
	
	public ActionForward getPreLogin(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response) throws IOException, SAXException, InterruptedException{
		 //HtmlPage loginPage = null;
//		String loginPage = "";
//		if(webClient != null){
//			loginPage = webClient.getCookieManager().getCookie("cm").getName(); //查看缴费网页是否有信息
//			System.out.println(loginPage+"+++++++++++++");
//		}
		   
		if(webClient == null){                                      //若webclient未开启
			webClient = patentClawer.preLoginCponline();	
		}else if(patentClawer.getDocOfCponline(webClient, "2012102816763")!=null){  
			response.setContentType("text/html;charset=utf-8");      //若webclient已开启
			PrintWriter out=response.getWriter();
			out.println("请点击开始同步");
			out.flush();
			out.close();	
		}else{
			webClient = patentClawer.preLoginCponline();         //防止只是打开了web，但未登录
		}

		return null;
	}
	public ActionForward getBatchFeeSynchronization(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response) throws DAOException, IOException,SAXException ,InterruptedException , ParseException{
//		String valCode = ParamUtil.getParameter(request, "valCode");
//		webClient = patentClawer.newLoginCponline(valCode);
//		PatentFeeTimerTask task = new PatentFeeTimerTask();
//		task.run();
//		String dateToday = DateUtil.getDate(new Date());      //可以放在这，但是测试的时候不太好，系统判断今天更新过后就不登录了，一直在重复输入验证码
//		KjAuditLogDAO kjAuditdao = KjAuditLogDAO.getInstance();
//		if(kjAuditdao.getBatchSynRecordToday(dateToday)){
			
	
		List<Long> updatedPatentId = new ArrayList<Long>();//for email
		long startTime=System.currentTimeMillis();   //获取开始时间  
		
//		if (TechSystem.getInstance().getTechConfig().getPatenFeeGetOn()) 
//		{
//		System.out.println(" PatentFeeTimerTask  专利网费用获取 启动.... "  );
		
		
			//TODO 1 取所有 科技系统中过程状态为31以后的数据。 并使用过期时间排序、分组。
		KjPatentDao patentDao = KjPatentDao.getInstance();//专利
//		String sqlWhere = "from KjPatent as kj where kj.deleteMark < 1 and kj.patentNo in ('2015107021978','2011103261546','2011103318309','2010105221873')"
//				+" and kj.patentNo is not null and (kj.patentForeignType is null or kj.patentForeignType<'1') order by patentId desc ";
		String sqlWhere = "from KjPatent as kj where kj.deleteMark < 1 and kj.state in (select s.code from KjPatentState s where ((s.id >= 31 and s.id<=50) or s.id >= 90) )"
		+" and kj.patentNo is not null and (kj.patentForeignType is null or kj.patentForeignType<'1') order by patentId desc ";
		List<KjPatent> kjpl = patentDao.findByHQL(sqlWhere);
		//Clawer patentClawer = new Clawer();
		KjPatentFeeDao patentFeedao = KjPatentFeeDao.getInstance();
		
		if(kjpl!=null)
		{
			System.out.println("非失效专利 - " + kjpl.size());
			
			//登陆专利网 
			//WebClient webClient = null;
			try{
				long startLoginTime=System.currentTimeMillis();   //获取开始时间
			
				//webClient = patentClawer.loginCponline();
				String valCode = ParamUtil.getParameter(request, "valCode");
				//System.out.print(valCode);
				//System.out.print("+++++++++++++++++++++++++++++++++++++++"+valCode.equals("0"));
				if(!valCode.equals("0")){
					webClient = patentClawer.newLoginCponline(valCode);
				}
				
				String dateToday = DateUtil.getDate(new Date());
				KjAuditLogDAO kjAuditdao = KjAuditLogDAO.getInstance();
				if(kjAuditdao.getBatchSynRecordToday(dateToday)){                       //判断日志今天是否批量同步过
					
				if(webClient!=null){
					long endLoginTime=System.currentTimeMillis(); //获取结束时间  
//					System.out.println("连接时间： "+(endLoginTime-startLoginTime)+"ms");
					response.setContentType("text/html;charset=utf-8");
					PrintWriter out=response.getWriter();
//					for(int i =0;i<1000;i++)
					for(int i =0;i<kjpl.size() 
//					&& i<=40 
					;i++)
//					for(int i =2200;i<kjpl.size() && i<2250 ;i++)
					{
						System.out.println(kjpl.get(i).getPatentNo());
//						responseSend(response,"此次同步涉及数据共"+kjpl.size()+"条,正在更新第"+i+"条");
						//if(i%2==0){
							out.println("此次同步数据共"+kjpl.size()+"条,正在更新第"+(i+1)+"条,请稍等...");
							out.flush();
						//}
						
						//Thread.currentThread().sleep(1000); 						//out.close();
						//TODO 2 对每组专利，判断历史日志，今天是否轮到更新它
						if(i%50 == 0) System.out.println(i);
						//获取该专利的年费，并计算剩余时间
						boolean isUpdate = false;
						
						Map resultMap = KjPatentFeeDao.getInstance().getRemainDayAndExpireDayByPatentId(kjpl.get(i).getPatentId());
						Long remainDays = 10000L; // 当前专利的剩余天数,以最小剩余天数为准
						Date expireDay = new Date(); // 专利离当前最小剩余天数对应的截止日期(用于找到该截止日期需要提醒的所有费用记录)
						remainDays = (Long) resultMap.get("remainDays");
						expireDay = (Date) resultMap.get("expireDay");
						
						if(remainDays<=120)
						{
							long daysFromLastLog = PatentFeeTimerTask.daysFromLastUpdateLogDate(kjpl.get(i));   //引入PatentFeeTimerTask
							if(daysFromLastLog!=-1){// 表示有日志且计算成功
//								System.out.println("表示有日志且计算成功" + daysFromLastLog);
								List<KjPatentFee> kpfs=patentFeedao.findByPIdAndConfig(Long.valueOf(kjpl.get(i).getPatentId()),11L);
								if (kpfs != null && kpfs.size() > 0) {// 有专利年费记录才有剩余时间？
									if (remainDays < 7l) {// 剩余时间 小于 7天 1天更新一次 （包过剩余时间 小于 0？？）
										isUpdate = true;
									} else if (remainDays >= 7l && remainDays < 15l) {// 大于7天 小于15天的 2天更新一次
										if (daysFromLastLog >= 7) isUpdate = true;
									} else if (remainDays >= 15l && remainDays < 30l) {// 大于15天  小于30天的 7天更新一次
										if (daysFromLastLog >= 7l) isUpdate = true;
									} else if (remainDays >= 30l && remainDays < 60l) {// 大于30天 小于60天的 15天更新一次
										if (daysFromLastLog >= 15l) isUpdate = true;
									} else {// 大于60天 30天更新一次
										if (daysFromLastLog >= 30l) isUpdate = true;
									}
								}else{
									// TODO 没有专利年费 ？
									isUpdate=true;
								}
							}else{//表示该专利没有日志 或者 日志 没有“同步专利网费用”的字样，即认为第一次执行，全部更新
								isUpdate=true;
//								System.out.println("表示该专利没有日志 或者 日志 没有 同步专利网费用 的字样");
							}
//							System.out.println(kjpl.get(i) .getPatentNo() + " : isUpdate-" + isUpdate);
						}
						
						//TODO 3 给可以更新的专利 进行更新 getFeeSynchronization的双胞胎方法 ，并记录日志
						if(isUpdate){
							
//							System.out.println("给可以更新的专利 进行更新 getFeeSynchronization的双胞胎方法 ，并记录日志");
							
							//if(!kjpl.get(i).getState().equals("55") && !kjpl.get(i).getState().equals("56")){
//							System.out.println(kjpl.get(i).getPatentNo()+" state:"+kjpl.get(i).getState());
							String patentNo = kjpl.get(i).getPatentNo().toUpperCase().replaceAll("ZL", "").replaceAll(" ", "");//专利号
							if(patentNo.contains("."))patentNo=patentNo.replace(".", "");// 专利号中 含有 "." 字符 特殊处理。
							Document doc =  patentClawer.getDocOfCponline(webClient, patentNo);
							if(doc!=null)
							{
								HttpSession session=request.getSession();
								UserBean userbean = (UserBean) session.getAttribute("UserBean");
								boolean isSucceed = patentClawer.synPatentAndFee(doc,kjpl.get(i),userbean,1);  //1代表批量同步写入日志
								if(isSucceed){//如果更新成功
									updatedPatentId.add(kjpl.get(i).getPatentId());
									System.out.println("patentNo:"+patentNo+" syn succeed");
//									out.println("此次同步数据共"+kjpl.size()+"条,正在更新第"+(i+1)+"条,请稍等...");
//									out.flush();
								}else{
									System.out.println("patentNo:"+patentNo+" syn error");
								}
							}else{
								System.out.println("patentNo:"+patentNo+" 获取专利网数据失败，网址（http://www.cponline.gov.cn/）");
							}
							//}
						}
					}// end for
				}// end if(webClient!=null){
				else{
					System.out.println("专利网（http://www.cponline.gov.cn/）连接失败 ----------- connect failure：  ");
				}
				}else{
					System.out.println("今日批量同步已执行,禁止再次执行");
					response.setContentType("text/html;charset=utf-8");
					PrintWriter out=response.getWriter();
					out.println("111");             //禁止再次点击批量按钮，传入前台
					out.flush();
					out.close();
				}
			}catch(InterruptedException e ){//SAXException, InterruptedException
				System.out.println(e.getMessage());
			}catch(SAXException e)
			{
				System.out.println(e.getMessage());
			}catch(IOException e)
			{
				System.out.println(e.getMessage());
			}catch(ParseException e)
			{
				System.out.println(e.getMessage());
			}catch(DAOException e)
			{
				System.out.println(e.getMessage());
			}
			
		}
		else
		{
			System.out.println("无数据");
		}
		//KjPatent kjp = patentDao.findByPk(Long.valueOf(patentId));
			//为了方便测试批量执行的效率，现在可以先省掉1 2，直接执行3，更新前20条或N条，判断批量效率
			
//		} else {
//			System.out.println("专利年份额获取及同步程序 已禁用 ----------- PatentFeeTimerTask not open： ");
//		}
//		for(int i=0;i<updatedPatentId.size();i++)
//		{
//			System.out.println(updatedPatentId.get(i));
//		}
		long endTime=System.currentTimeMillis(); //获取结束时间  
		System.out.println("同步所用所有时间： -----------  PatentFeeTimerTask cost time： "+(endTime-startTime)+"ms");
		String sendType = "";
//		sendType = TechSystem.getInstance().getTechConfig().getPatenFeeMailSendType();
		//TODO sendType 中含有字符1，给发明专利发邮件，含有2，给实用新型发，含有3，给外观设计发。
//		List<Long> sendMailPatentList = getListForSendMail(updatedPatentId,sendType); //根据patentIdList和sendType，判断是否符合发送邮件条件，返回列表
//		System.out.println("符合要求的记录数： ----------- toSend mail ： "+sendMailPatentList.size());
		
		System.out.println(" ----  senMail function will open in coming days ...");
		
		//给patentId里面的所有人员发送邮件
//		for(int i=0;i<sendMailPatentList.size();i++)
//		{
//			try {
//				sendMailById(sendMailPatentList.get(i));
//			} catch (ParseException e) {
//				e.printStackTrace();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}
//		
		//测试发送邮件功能
//		Long id = 2283L;
//		try {
//			sendMailById(id);
//		} catch (ParseException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}	
		
		System.out.println("结束时系统时间 ----------- end PatentFeeTimerTask： "+new Date(Calendar.getInstance().getTimeInMillis()));
//		}else{
//			System.out.println("今日批量同步已执行,禁止再次执行");
//			response.setContentType("text/html;charset=utf-8");
//			PrintWriter out=response.getWriter();
//			out.println("111");
//			out.flush();
//			out.close();
//		}
		return null;
	}
	
	public ActionForward getFeeSynchronization(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response) throws DAOException, IOException,SAXException ,InterruptedException , ParseException{
			HttpSession session=request.getSession();
			UserBean userbean = (UserBean) session.getAttribute("UserBean");
			String userId = userbean.getUid();
			
			String sign = ((KjDictIdentity)session.getAttribute("identity")).getSign();
			String patentId=ParamUtil.getParameterFrom(request, "patentId");  
			if(patentId.length()==0)
			{
				patentId = ParamUtil.getAttribute(request, "patentId");
			}
			System.out.println("专利年费同步测试：patentId-->"+patentId);
			
			String patentNo=ParamUtil.getParameterFrom(request, "patentNo");
			if(patentNo.contains("."))patentNo=patentNo.replace(".", "");
			
			
			
			long startTime=System.currentTimeMillis();   //获取开始时间  
			long tic,tid;
			
			
			
			//String sqlWhere = "";//所有年费的列表
			//sqlWhere=" as kj where 1=1 and kj.patentId.patentId="+patentId+" asc ";
			KjPatentFeeDao patentFeedao = KjPatentFeeDao.getInstance();
			List<KjPatentFee> kpfs=patentFeedao.findByPIdAndConfig(Long.valueOf(patentId),11L);
			
			KjPatentDao patentDao = KjPatentDao.getInstance();//专利
			KjPatent kjp = patentDao.findByPk(Long.valueOf(patentId));
			if(patentNo.length()==0)
			{
				patentNo = kjp.getPatentNo();
			}
			System.out.println("专利年费同步测试：patentNo-->"+patentNo);
			
			tic = System.currentTimeMillis();   
//			Clawer patentClawer = new Clawer();
//			WebClient webClient = null;
//			webClient = patentClawer.preLoginCponline();

			String valCode = ParamUtil.getParameter(request, "valCode");
//			System.out.println(valCode);
//			System.out.print("+++++++++++++++++++++++++++++++++++++++"+valCode.equals("0"));
			if(!valCode.equals("0")){
				webClient = patentClawer.newLoginCponline(valCode);
			}
			
			if(webClient!=null){//登陆成功
//				Document patentDoc = patentClawer.quickGetXmlByPatentId(patentNo);
				Document patentDoc =  patentClawer.getDocOfCponline(webClient, patentNo);
				if(patentDoc!=null)
				{
					tid = System.currentTimeMillis(); 
					System.out.println("获取数据时间： "+(tid-tic)+"ms");
					tic = System.currentTimeMillis(); 
					boolean isSucceed = patentClawer.synPatentAndFee(patentDoc,kjp,userbean,0); //0代表单个同步
					if(isSucceed)System.out.println("patentNo:"+patentNo+" syn succeed");
		
					tid = System.currentTimeMillis(); 
					System.out.println("同步数据时间： "+(tid-tic)+"ms");
					long endTime=System.currentTimeMillis(); //获取结束时间  
					System.out.println("程序运行时间： "+(endTime-startTime)+"ms");
					System.out.println("done！！！！");
				}else{
					System.out.println("获取数据失败");
					return null;
				}
			}else{
				System.out.println("登陆失败");
				return null;
			}
			
			//return new ActionForward("/fee/previewYearUpdate.jsp");	
			return null;
		}
//	public void responseSend(HttpServletResponse response,Object o) throws IOException{
//		response.setContentType("text/html;charset=utf-8");
//		PrintWriter out=response.getWriter();
//		out.println(o.toString());
//		out.flush();
//		//out.close();//此处控制是否send一条记录就关闭
//	}
	
}
