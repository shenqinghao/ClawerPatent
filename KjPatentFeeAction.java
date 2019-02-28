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
	  static  int AnnelFeeConfigId=11;//���ñ���Ѷ�Ӧ�ķ������ñ������
	  static  int overDueFeeConfigId=12;//���ñ����ɽ��Ӧ�ķ������ñ������
	  static  int recoverFeeConfigId=14;//���ñ�ָ��Ѷ�Ӧ�ķ������ñ������
	  
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
		
		//����Ϊϵͳ�Զ��������
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
					auditlog.setOper("����ά���û��󶨾��ѿ�,���ţ�"+cardNo);// һ����ָ���ݶ��� ��ʲô״̬ ת��ʲô״̬ 3
					auditlog.setOperTime(DateUtil.getDate(new Date()));
					auditlog.setOperMsg(KjUserDAO.getInstance().findByPk(userId).getName() + "�󶨾��ѿ�");// һ��ָ˭������ʲô���� �� ��xx��ˡ���xx�޸ġ�xxxɾ�� 4
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
	
	//���
	public ActionForward showRemind(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)throws DAOException  {    
		HttpSession session=request.getSession();
		UserBean userbean = (UserBean) session.getAttribute("UserBean");
		String userId = userbean.getUid();
		//��ȡ�û������
		String sign = ((KjDictIdentity)session.getAttribute("identity")).getSign();
		String confirm=ParamUtil.getParameterFrom(request, "confirm");  //�ж��Ƿ���ͨ��ȷ���ѽɷѵĲ���
				
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
			//��ȡ��������Ϣ
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
		
		//���˵�����ķ��ü�¼
		Map patentIdMap = new HashMap();			 //�жϵ�ǰpatentId�Ƿ��Ѿ����ڵ�map
		
		Long time1 = System.currentTimeMillis();
//		System.out.println(" ƴװ��sql  " + (time1 - time0)/1000);
		
		List<KjPatentFee> patentFeeListAll = dao.getList(sqlWhere);
//		List<Long> patentFeeListAll_id = null;
//		String sql = " select distinct kj.patentId.patentId,kj.expiryDay  from KjPatentFee " + sqlWhere;
//		System.out.println(sql);
//		Query  q = KjPatentFeeDao.getInstance().getSession().createQuery(sql);
//		patentFeeListAll_id = q.list();
		
		Long time2 = System.currentTimeMillis();
//		System.out.println("��ѯ��sql  " + (time2 - time1));//�����ʱ5��
		
		List<KjPatentFee> patentFeeListAll_filter = new ArrayList<KjPatentFee>();
		List<KjPatentFee> patentFeeList = new ArrayList<KjPatentFee>();
//		System.out.println(sqlWhere);
//		System.out.println("s:"+patentFeeListAll.size());
	
		
		if(patentFeeListAll!=null){
			for(int i=0; i<patentFeeListAll.size(); i++){       
				if(i==0){
					
					patentFeeListAll_filter.add(patentFeeListAll.get(0));
					patentIdMap.put(patentFeeListAll.get(0).getPatentId().getPatentId(), 1);    //��patentId����map,���ں�����У��
				}else{
					if(!patentIdMap.containsKey(patentFeeListAll.get(i).getPatentId().getPatentId())){   //���û�н�patentId��Ӧ�ķ��ü���list,���ڴ˼���
						patentFeeListAll_filter.add(patentFeeListAll.get(i));
						patentIdMap.put(patentFeeListAll.get(i).getPatentId().getPatentId(), 1);    //��patentId����map,���ں�����У��
					}
				}
			}
			//
			
			//ƴװ���β�ѯ����ʼλ�� ���Ҽ��㵱ǰ�ķ�����ʾ��ֵ���  <����ǰ�����һ���ֹ�����з������>
//			System.out.println(currentCount+"  "+pageSize);
			if(currentCount>=patentFeeListAll_filter.size())currentCount = (patentFeeListAll_filter.size()/pageSize)*(pageSize);
			if(currentCount==patentFeeListAll_filter.size())currentCount = patentFeeListAll_filter.size()-pageSize;
			if(currentCount<0)currentCount=0;
			for(int j=currentCount; j<=currentCount+pageSize-1 && j<patentFeeListAll_filter.size(); j++){
				patentFeeList.add(patentFeeListAll_filter.get(j));
			}
		}
		
		Long time3 = System.currentTimeMillis();
//		System.out.println("������list  " + (time3 - time2)/1000);
		
		int sumCount = patentFeeListAll_filter.size();
		PaginalBean paginalbean = new PaginalBean(currentCount, sumCount, pageSize);
		
		for(int i=0; i<patentFeeList.size(); i++){
			KjPatent patentObj = patentFeeList.get(i).getPatentId();
			if(patentObj.getIsGiveup()!=null && patentObj.getIsGiveup()==3L){   //����ר��������Ҳ����ʾ�������
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
		//��ȡ�û������
		String sign = ((KjDictIdentity)session.getAttribute("identity")).getSign();
		String confirm=ParamUtil.getParameterFrom(request, "confirm"); ; //�ж��Ƿ���ͨ��ȷ���ѽɷѵĲ���
		
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
			//��ȡ��������Ϣ
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
		
		//���˵�����ķ��ü�¼
		Map patentIdMap = new HashMap();			 //�жϵ�ǰpatentId�Ƿ��Ѿ����ڵ�map
		List<KjPatentFee> patentFeeListAll = dao.getList(sqlWhere);
		List<KjPatentFee> patentFeeListAll_filter = new ArrayList<KjPatentFee>();
		List<KjPatentFee> patentFeeList = new ArrayList<KjPatentFee>();
//		System.out.println(sqlWhere);
//		System.out.println("s:"+patentFeeListAll.size());
		if(patentFeeListAll!=null){
			for(int i=0; i<patentFeeListAll.size(); i++){       
				if(i==0){
					patentFeeListAll_filter.add(patentFeeListAll.get(0));
					patentIdMap.put(patentFeeListAll.get(0).getPatentId().getPatentId(), 1);    //��patentId����map,���ں�����У��
				}else{
					if(!patentIdMap.containsKey(patentFeeListAll.get(i).getPatentId().getPatentId())){   //���û�н�patentId��Ӧ�ķ��ü���list,���ڴ˼���
						patentFeeListAll_filter.add(patentFeeListAll.get(i));
						patentIdMap.put(patentFeeListAll.get(i).getPatentId().getPatentId(), 1);    //��patentId����map,���ں�����У��
					}
				}
			}
			//
			
			//ƴװ���β�ѯ����ʼλ�� ���Ҽ��㵱ǰ�ķ�����ʾ��ֵ���  <����ǰ�����һ���ֹ�����з������>
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
			if(patentObj.getIsGiveup()!=null && patentObj.getIsGiveup()==3L){   //����ר��������Ҳ����ʾ�������
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
		//��ȡ�û������
		String sign = ((KjDictIdentity)session.getAttribute("identity")).getSign();
		
		String sqlWhereYear = "";//������ѵ��б�
		String sqlWhereOverdue = "";//�������ɷѵ��б�
		String sqlWhereRecover = "";//���лָ��ѵ��б�
		String sqlWhereAll = "";//���ظ�ר���Ļָ�+����+���
		
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
		HSSFSheet sheet = wb.createSheet("����嵥");
	    
	    //����
		HSSFRow rowHead = sheet.createRow(0);
		HSSFCell cellHead = rowHead.createCell((short) 0);
	    cellHead.setCellValue("ר����");
	    cellHead = rowHead.createCell((short)1);
	    cellHead.setCellValue("ר������");
	    cellHead = rowHead.createCell((short)2);
	    cellHead.setCellValue("ר������");
	    cellHead = rowHead.createCell((short)3);
	    cellHead.setCellValue("��һ������");
	    cellHead = rowHead.createCell((short)4);
	    cellHead.setCellValue("����ѧԺ");
	    
	    cellHead = rowHead.createCell((short)5);
	    cellHead.setCellValue("�������ƣ���ѣ�");    
	    cellHead = rowHead.createCell((short)6);
	    cellHead.setCellValue("��������");
	    cellHead = rowHead.createCell((short)7);
	    cellHead.setCellValue("Ӧ�ɽ��");
	    
	    cellHead = rowHead.createCell((short)8);
	    cellHead.setCellValue("���ɽ�");	    
	    cellHead = rowHead.createCell((short)9);
	    cellHead.setCellValue("��������");
	    cellHead = rowHead.createCell((short)10);
	    cellHead.setCellValue("Ӧ�ɽ��");
	    
	    cellHead = rowHead.createCell((short)11);
	    cellHead.setCellValue("�ָ���");	    
	    cellHead = rowHead.createCell((short)12);
	    cellHead.setCellValue("��������");
	    cellHead = rowHead.createCell((short)13);
	    cellHead.setCellValue("Ӧ�ɽ��");
	    
	    cellHead = rowHead.createCell((short)14);
	    cellHead.setCellValue("����������");
	    cellHead = rowHead.createCell((short)15);
	    cellHead.setCellValue("����������ѧԺ");
	    cellHead = rowHead.createCell((short)16);
	    cellHead.setCellValue("�����˹��ʺ�");
	    cellHead = rowHead.createCell((short)17);
	    cellHead.setCellValue("���ѿ���");
	    
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
	    	cell.setCellValue("��"+pt.getYear()+"�����");
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
			    		cell.setCellValue("���ɷ�");
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
			    		cell.setCellValue("�ָ���");
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
    		cell.setCellValue("���ɷ�");
 	    	if(pt.getExpiryDay()!=null){
	 	    	cell = row.createCell((short)9);  
			    cell.setCellValue(sdf.format(pt.getExpiryDay()));
 	    	}
		    cell = row.createCell((short)10);
		    cell.setCellValue(pt.getAmount());
    	    
		   if(kp.getFundsPeople()!=null){
			    cell = row.createCell((short)14);
			    cell.setCellValue(kp.getFundsPeople().getName());   //���ѿ�������
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
    		cell.setCellValue("�ָ���");
 	    	if(pt.getExpiryDay()!=null){
	 	    	cell = row.createCell((short)12);  
			    cell.setCellValue(sdf.format(pt.getExpiryDay()));
 	    	}
		    cell = row.createCell((short)13);
		    cell.setCellValue(pt.getAmount());
    	    
		    
		    if(kp.getFundsPeople()!=null){
			    cell = row.createCell((short)14);
			    cell.setCellValue(kp.getFundsPeople().getName());   //���ѿ�������
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

	    //���
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
					auditlog.setOper("����ר��״̬�ӣ�"+KjPatentStateDao.getInstance().searchByCode(oldState).getName()+"  ����"
							+KjPatentStateDao.getInstance().searchByCode(kps.getPatentId().getState()).getName());// һ����ָ���ݶ��� ��ʲô״̬ ת��ʲô״̬ 3
					auditlog.setOperTime(DateUtil.getDate(new Date()));
					auditlog.setOperMsg(KjUserDAO.getInstance().findByPk(userId).getName() + "ȷ�Ͻɷ�");// һ��ָ˭������ʲô���� �� ��xx��ˡ���xx�޸ġ�xxxɾ�� 4
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
						oper = "ȷ�Ͻ��ɵ�"+kps.getYear()+"����ѣ�";
					}
					else if(kps.getConfigId().getId().intValue() == overDueFeeConfigId)
					{
						oper = "ȷ�Ͻ������ɽ�";
					}
					else if(kps.getConfigId().getId().intValue() == recoverFeeConfigId)
					{
						oper = "ȷ�Ͻ��ɻָ���";
					}
				}
				
				auditlog.setOper(oper);// һ����ָ���ݶ��� ��ʲô״̬ ת��ʲô״̬ 3
				auditlog.setOperTime(DateUtil.getDate(new Date()));
				auditlog.setOperMsg(KjUserDAO.getInstance().findByPk(userId).getName() + "ȷ�Ͻɷ�");// һ��ָ˭������ʲô���� �� ��xx��ˡ���xx�޸ġ�xxxɾ�� 4
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
		//��ȡ�û������
		String sign = ((KjDictIdentity)session.getAttribute("identity")).getSign();
		
		String sqlWhere = "";//���йٷѵ��б�
		sqlWhere=" as kj where kj.isPaid is not null and ( kj.configId.type='office') and kj.expiryDay is not null and kj.patentId.deleteMark <1 ";
		sqlWhere=QuickSearch.feeForm(sqlWhere, form, "isPaid,expiryDay");
		session.setAttribute("sqlWhere",sqlWhere);
			
		KjPatentFeeDao dao = KjPatentFeeDao.getInstance();
		List<KjPatentFee> kpfs=dao.findByHQL("from KjPatentFee "+sqlWhere);
				
		HSSFWorkbook wb = new HSSFWorkbook();           // or new XSSFWorkbook();
		HSSFSheet sheet = wb.createSheet("�ٷ��嵥");
	    
	    //����
		HSSFRow rowHead = sheet.createRow(0);
		HSSFCell cellHead = rowHead.createCell((short) 0);
	    cellHead.setCellValue("ר����");
	    cellHead = rowHead.createCell((short)1);
	    cellHead.setCellValue("ר������");
	    cellHead = rowHead.createCell((short)2);
	    cellHead.setCellValue("ר������");
	    cellHead = rowHead.createCell((short)3);
	    cellHead.setCellValue("��һ������");
	    cellHead = rowHead.createCell((short)4);
	    cellHead.setCellValue("����ѧԺ");
	    cellHead = rowHead.createCell((short)5);
	    cellHead.setCellValue("ר��״̬");
	    cellHead = rowHead.createCell((short)6);
	    cellHead.setCellValue("��������");    
	    cellHead = rowHead.createCell((short)7);
	    cellHead.setCellValue("��������");
	    cellHead = rowHead.createCell((short)8);
	    cellHead.setCellValue("Ӧ�ɽ��");

	    
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
	    //��� 
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
					oper = "ȷ�Ͻ���"+kps.getConfigId().getName()+"��";
					
				}
				
				auditlog.setOper(oper);// һ����ָ���ݶ��� ��ʲô״̬ ת��ʲô״̬ 3
				auditlog.setOperTime(DateUtil.getDate(new Date()));
				auditlog.setOperMsg(KjUserDAO.getInstance().findByPk(userId).getName() + "ȷ�Ͻɷ�");// һ��ָ˭������ʲô���� �� ��xx��ˡ���xx�޸ġ�xxxɾ�� 4
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
		//��ȡ�û������
		String sign = ((KjDictIdentity)session.getAttribute("identity")).getSign();
		String confirm=ParamUtil.getParameterFrom(request, "confirm");  //�ж��Ƿ���ͨ��ȷ���ѽɷѵĲ���
				
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
			//��ȡ��������Ϣ
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
		
		//���˵�����ķ��ü�¼
		Map patentIdMap = new HashMap();			 //�жϵ�ǰpatentId�Ƿ��Ѿ����ڵ�map
		List<KjPatentFee> patentFeeListAll = dao.getList(sqlWhere);
		List<KjPatentFee> patentFeeListAll_filter = new ArrayList<KjPatentFee>();
		List<KjPatentFee> patentFeeList = new ArrayList<KjPatentFee>();
//		System.out.println(sqlWhere);
//		System.out.println("s:"+patentFeeListAll.size());
		if(patentFeeListAll!=null){
			for(int i=0; i<patentFeeListAll.size(); i++){       
				if(i==0){
					patentFeeListAll_filter.add(patentFeeListAll.get(0));
					patentIdMap.put(patentFeeListAll.get(0).getPatentId().getPatentId(), 1);    //��patentId����map,���ں�����У��
				}else{
					if(!patentIdMap.containsKey(patentFeeListAll.get(i).getPatentId().getPatentId())){   //���û�н�patentId��Ӧ�ķ��ü���list,���ڴ˼���
						patentFeeListAll_filter.add(patentFeeListAll.get(i));
						patentIdMap.put(patentFeeListAll.get(i).getPatentId().getPatentId(), 1);    //��patentId����map,���ں�����У��
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
			String confirm=ParamUtil.getParameterFrom(request, "confirm");  //�ж��Ƿ���ͨ��ȷ���ѽɷѵĲ���
					
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
				//��ȡ��������Ϣ
				String kjCollege = userbean.getCollege();
				KjPatentFeeForm Form = (KjPatentFeeForm) form;
				Form.setCollegeId(kjCollege);
				sqlWhere=QuickSearch.feeForm(sqlWhere, Form, "expiryDay");
				session.setAttribute("sqlWhere",sqlWhere);
			}
			KjPatentFeeDao dao = KjPatentFeeDao.getInstance();
			int currentCount = SysConfig.START_COUNT;
			int pageSize = SysConfig.PAGE_SIZE;

			//���˵�����ķ��ü�¼
			Map patentIdMap = new HashMap();			 //�жϵ�ǰpatentId�Ƿ��Ѿ����ڵ�map
			List<KjPatentFee> patentFeeListAll = dao.getList(sqlWhere);
			List<KjPatentFee> patentFeeListAll_filter = new ArrayList<KjPatentFee>();
			List<KjPatentFee> patentFeeList = new ArrayList<KjPatentFee>();
//			System.out.println(sqlWhere);
//			System.out.println("s:"+patentFeeListAll.size());
			if(patentFeeListAll!=null){
				for(int i=0; i<patentFeeListAll.size(); i++){       
					if(i==0){
						patentFeeListAll_filter.add(patentFeeListAll.get(0));
						patentIdMap.put(patentFeeListAll.get(0).getPatentId().getPatentId(), 1);    //��patentId����map,���ں�����У��
					}else{
						if(!patentIdMap.containsKey(patentFeeListAll.get(i).getPatentId().getPatentId())){   //���û�н�patentId��Ӧ�ķ��ü���list,���ڴ˼���
							patentFeeListAll_filter.add(patentFeeListAll.get(i));
							patentIdMap.put(patentFeeListAll.get(i).getPatentId().getPatentId(), 1);    //��patentId����map,���ں�����У��
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
		String confirm=ParamUtil.getParameterFrom(request, "confirm");  //�ж��Ƿ���ͨ��ȷ���ѽɷѵĲ���
				
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
			//��ȡ��������Ϣ
			String kjCollege = userbean.getCollege();
			KjPatentFeeForm Form = (KjPatentFeeForm) form;
			Form.setCollegeId(kjCollege);
			sqlWhere=QuickSearch.feeForm(sqlWhere, Form, "expiryDay");
			session.setAttribute("sqlWhere",sqlWhere);
		}
		KjPatentFeeDao dao = KjPatentFeeDao.getInstance();
		int currentCount = SysConfig.START_COUNT;
		int pageSize = SysConfig.PAGE_SIZE;

		//���˵�����ķ��ü�¼
		Map patentIdMap = new HashMap();			 //�жϵ�ǰpatentId�Ƿ��Ѿ����ڵ�map
		List<KjPatentFee> patentFeeListAll = dao.getList(sqlWhere);
		List<KjPatentFee> patentFeeListAll_filter = new ArrayList<KjPatentFee>();
		List<KjPatentFee> patentFeeList = new ArrayList<KjPatentFee>();
//		System.out.println(sqlWhere);
//		System.out.println("s:"+patentFeeListAll.size());
		if(patentFeeListAll!=null){
			for(int i=0; i<patentFeeListAll.size(); i++){       
				if(i==0){
					patentFeeListAll_filter.add(patentFeeListAll.get(0));
					patentIdMap.put(patentFeeListAll.get(0).getPatentId().getPatentId(), 1);    //��patentId����map,���ں�����У��
				}else{
					if(!patentIdMap.containsKey(patentFeeListAll.get(i).getPatentId().getPatentId())){   //���û�н�patentId��Ӧ�ķ��ü���list,���ڴ˼���
						patentFeeListAll_filter.add(patentFeeListAll.get(i));
						patentIdMap.put(patentFeeListAll.get(i).getPatentId().getPatentId(), 1);    //��patentId����map,���ں�����У��
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
		
		
		
		String sqlWhere = "";//���йٷѵ��б�
		sqlWhere=" as kj where kj.isPaid is not null and ( kj.configId <=10 or kj.configId=13 or kj.configId=15) and kj.expiryDay is not null";
		sqlWhere=QuickSearch.feeForm(sqlWhere, form, "isPaid,expiryDay");
		session.setAttribute("sqlWhere",sqlWhere);
			
		KjPatentFeeDao dao = KjPatentFeeDao.getInstance();
		List<KjPatentFee> kpfs=dao.findByHQL("from KjPatentFee "+sqlWhere);
				
		HSSFWorkbook wb = new HSSFWorkbook();           // or new XSSFWorkbook();
		HSSFSheet sheet = wb.createSheet("�ٷ��嵥");
	    
	    //����
		HSSFRow rowHead = sheet.createRow(0);
		HSSFCell cellHead = rowHead.createCell((short) 0);
	    cellHead.setCellValue("ר����");
	    cellHead = rowHead.createCell((short)1);
	    cellHead.setCellValue("ר������");
	    cellHead = rowHead.createCell((short)2);
	    cellHead.setCellValue("ר������");
	    cellHead = rowHead.createCell((short)3);
	    cellHead.setCellValue("��һ������");
	    cellHead = rowHead.createCell((short)4);
	    cellHead.setCellValue("����ѧԺ");
	    cellHead = rowHead.createCell((short)5);
	    cellHead.setCellValue("ר��״̬");
	    cellHead = rowHead.createCell((short)6);
	    cellHead.setCellValue("��������");    
	    cellHead = rowHead.createCell((short)7);
	    cellHead.setCellValue("��������");
	    cellHead = rowHead.createCell((short)8);
	    cellHead.setCellValue("Ӧ�ɽ��");

	    
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
	    //��� 
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
					oper = "ȷ�Ͻ���"+kps.getConfigId().getName()+"��";
					
				}
				
				auditlog.setOper(oper);// һ����ָ���ݶ��� ��ʲô״̬ ת��ʲô״̬ 3
				auditlog.setOperTime(DateUtil.getDate(new Date()));
				auditlog.setOperMsg(KjUserDAO.getInstance().findByPk(userId).getName() + "ȷ�Ͻɷ�");// һ��ָ˭������ʲô���� �� ��xx��ˡ���xx�޸ġ�xxxɾ�� 4
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
		//��ȡ�û������
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
				auditlog.setOper("����ר��״̬�ӣ�"+KjPatentStateDao.getInstance().searchByCode(oldState).getName()+"  ����"
						+KjPatentStateDao.getInstance().searchByCode(kps.getPatentId().getState()).getName());// һ����ָ���ݶ��� ��ʲô״̬ ת��ʲô״̬ 3
				auditlog.setOperTime(DateUtil.getDate(new Date()));
				auditlog.setOperMsg(KjUserDAO.getInstance().findByPk(userId).getName() + "ȷ�Ͻɷ�");// һ��ָ˭������ʲô���� �� ��xx��ˡ���xx�޸ġ�xxxɾ�� 4
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
			auditlog.setOper("ȷ�Ͻ��ɵ�"+kps.getYear()+"����ѣ�");// һ����ָ���ݶ��� ��ʲô״̬ ת��ʲô״̬ 3
			auditlog.setOperTime(DateUtil.getDate(new Date()));
			auditlog.setOperMsg(KjUserDAO.getInstance().findByPk(userId).getName() + "ȷ�Ͻɷ�");// һ��ָ˭������ʲô���� �� ��xx��ˡ���xx�޸ġ�xxxɾ�� 4
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
//			loginPage = webClient.getCookieManager().getCookie("cm").getName(); //�鿴�ɷ���ҳ�Ƿ�����Ϣ
//			System.out.println(loginPage+"+++++++++++++");
//		}
		   
		if(webClient == null){                                      //��webclientδ����
			webClient = patentClawer.preLoginCponline();	
		}else if(patentClawer.getDocOfCponline(webClient, "2012102816763")!=null){  
			response.setContentType("text/html;charset=utf-8");      //��webclient�ѿ���
			PrintWriter out=response.getWriter();
			out.println("������ʼͬ��");
			out.flush();
			out.close();	
		}else{
			webClient = patentClawer.preLoginCponline();         //��ֹֻ�Ǵ���web����δ��¼
		}

		return null;
	}
	public ActionForward getBatchFeeSynchronization(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response) throws DAOException, IOException,SAXException ,InterruptedException , ParseException{
//		String valCode = ParamUtil.getParameter(request, "valCode");
//		webClient = patentClawer.newLoginCponline(valCode);
//		PatentFeeTimerTask task = new PatentFeeTimerTask();
//		task.run();
//		String dateToday = DateUtil.getDate(new Date());      //���Է����⣬���ǲ��Ե�ʱ��̫�ã�ϵͳ�жϽ�����¹���Ͳ���¼�ˣ�һֱ���ظ�������֤��
//		KjAuditLogDAO kjAuditdao = KjAuditLogDAO.getInstance();
//		if(kjAuditdao.getBatchSynRecordToday(dateToday)){
			
	
		List<Long> updatedPatentId = new ArrayList<Long>();//for email
		long startTime=System.currentTimeMillis();   //��ȡ��ʼʱ��  
		
//		if (TechSystem.getInstance().getTechConfig().getPatenFeeGetOn()) 
//		{
//		System.out.println(" PatentFeeTimerTask  ר�������û�ȡ ����.... "  );
		
		
			//TODO 1 ȡ���� �Ƽ�ϵͳ�й���״̬Ϊ31�Ժ�����ݡ� ��ʹ�ù���ʱ�����򡢷��顣
		KjPatentDao patentDao = KjPatentDao.getInstance();//ר��
//		String sqlWhere = "from KjPatent as kj where kj.deleteMark < 1 and kj.patentNo in ('2015107021978','2011103261546','2011103318309','2010105221873')"
//				+" and kj.patentNo is not null and (kj.patentForeignType is null or kj.patentForeignType<'1') order by patentId desc ";
		String sqlWhere = "from KjPatent as kj where kj.deleteMark < 1 and kj.state in (select s.code from KjPatentState s where ((s.id >= 31 and s.id<=50) or s.id >= 90) )"
		+" and kj.patentNo is not null and (kj.patentForeignType is null or kj.patentForeignType<'1') order by patentId desc ";
		List<KjPatent> kjpl = patentDao.findByHQL(sqlWhere);
		//Clawer patentClawer = new Clawer();
		KjPatentFeeDao patentFeedao = KjPatentFeeDao.getInstance();
		
		if(kjpl!=null)
		{
			System.out.println("��ʧЧר�� - " + kjpl.size());
			
			//��½ר���� 
			//WebClient webClient = null;
			try{
				long startLoginTime=System.currentTimeMillis();   //��ȡ��ʼʱ��
			
				//webClient = patentClawer.loginCponline();
				String valCode = ParamUtil.getParameter(request, "valCode");
				//System.out.print(valCode);
				//System.out.print("+++++++++++++++++++++++++++++++++++++++"+valCode.equals("0"));
				if(!valCode.equals("0")){
					webClient = patentClawer.newLoginCponline(valCode);
				}
				
				String dateToday = DateUtil.getDate(new Date());
				KjAuditLogDAO kjAuditdao = KjAuditLogDAO.getInstance();
				if(kjAuditdao.getBatchSynRecordToday(dateToday)){                       //�ж���־�����Ƿ�����ͬ����
					
				if(webClient!=null){
					long endLoginTime=System.currentTimeMillis(); //��ȡ����ʱ��  
//					System.out.println("����ʱ�䣺 "+(endLoginTime-startLoginTime)+"ms");
					response.setContentType("text/html;charset=utf-8");
					PrintWriter out=response.getWriter();
//					for(int i =0;i<1000;i++)
					for(int i =0;i<kjpl.size() 
//					&& i<=40 
					;i++)
//					for(int i =2200;i<kjpl.size() && i<2250 ;i++)
					{
						System.out.println(kjpl.get(i).getPatentNo());
//						responseSend(response,"�˴�ͬ���漰���ݹ�"+kjpl.size()+"��,���ڸ��µ�"+i+"��");
						//if(i%2==0){
							out.println("�˴�ͬ�����ݹ�"+kjpl.size()+"��,���ڸ��µ�"+(i+1)+"��,���Ե�...");
							out.flush();
						//}
						
						//Thread.currentThread().sleep(1000); 						//out.close();
						//TODO 2 ��ÿ��ר�����ж���ʷ��־�������Ƿ��ֵ�������
						if(i%50 == 0) System.out.println(i);
						//��ȡ��ר������ѣ�������ʣ��ʱ��
						boolean isUpdate = false;
						
						Map resultMap = KjPatentFeeDao.getInstance().getRemainDayAndExpireDayByPatentId(kjpl.get(i).getPatentId());
						Long remainDays = 10000L; // ��ǰר����ʣ������,����Сʣ������Ϊ׼
						Date expireDay = new Date(); // ר���뵱ǰ��Сʣ��������Ӧ�Ľ�ֹ����(�����ҵ��ý�ֹ������Ҫ���ѵ����з��ü�¼)
						remainDays = (Long) resultMap.get("remainDays");
						expireDay = (Date) resultMap.get("expireDay");
						
						if(remainDays<=120)
						{
							long daysFromLastLog = PatentFeeTimerTask.daysFromLastUpdateLogDate(kjpl.get(i));   //����PatentFeeTimerTask
							if(daysFromLastLog!=-1){// ��ʾ����־�Ҽ���ɹ�
//								System.out.println("��ʾ����־�Ҽ���ɹ�" + daysFromLastLog);
								List<KjPatentFee> kpfs=patentFeedao.findByPIdAndConfig(Long.valueOf(kjpl.get(i).getPatentId()),11L);
								if (kpfs != null && kpfs.size() > 0) {// ��ר����Ѽ�¼����ʣ��ʱ�䣿
									if (remainDays < 7l) {// ʣ��ʱ�� С�� 7�� 1�����һ�� ������ʣ��ʱ�� С�� 0������
										isUpdate = true;
									} else if (remainDays >= 7l && remainDays < 15l) {// ����7�� С��15��� 2�����һ��
										if (daysFromLastLog >= 7) isUpdate = true;
									} else if (remainDays >= 15l && remainDays < 30l) {// ����15��  С��30��� 7�����һ��
										if (daysFromLastLog >= 7l) isUpdate = true;
									} else if (remainDays >= 30l && remainDays < 60l) {// ����30�� С��60��� 15�����һ��
										if (daysFromLastLog >= 15l) isUpdate = true;
									} else {// ����60�� 30�����һ��
										if (daysFromLastLog >= 30l) isUpdate = true;
									}
								}else{
									// TODO û��ר����� ��
									isUpdate=true;
								}
							}else{//��ʾ��ר��û����־ ���� ��־ û�С�ͬ��ר�������á�������������Ϊ��һ��ִ�У�ȫ������
								isUpdate=true;
//								System.out.println("��ʾ��ר��û����־ ���� ��־ û�� ͬ��ר�������� ������");
							}
//							System.out.println(kjpl.get(i) .getPatentNo() + " : isUpdate-" + isUpdate);
						}
						
						//TODO 3 �����Ը��µ�ר�� ���и��� getFeeSynchronization��˫��̥���� ������¼��־
						if(isUpdate){
							
//							System.out.println("�����Ը��µ�ר�� ���и��� getFeeSynchronization��˫��̥���� ������¼��־");
							
							//if(!kjpl.get(i).getState().equals("55") && !kjpl.get(i).getState().equals("56")){
//							System.out.println(kjpl.get(i).getPatentNo()+" state:"+kjpl.get(i).getState());
							String patentNo = kjpl.get(i).getPatentNo().toUpperCase().replaceAll("ZL", "").replaceAll(" ", "");//ר����
							if(patentNo.contains("."))patentNo=patentNo.replace(".", "");// ר������ ���� "." �ַ� ���⴦��
							Document doc =  patentClawer.getDocOfCponline(webClient, patentNo);
							if(doc!=null)
							{
								HttpSession session=request.getSession();
								UserBean userbean = (UserBean) session.getAttribute("UserBean");
								boolean isSucceed = patentClawer.synPatentAndFee(doc,kjpl.get(i),userbean,1);  //1��������ͬ��д����־
								if(isSucceed){//������³ɹ�
									updatedPatentId.add(kjpl.get(i).getPatentId());
									System.out.println("patentNo:"+patentNo+" syn succeed");
//									out.println("�˴�ͬ�����ݹ�"+kjpl.size()+"��,���ڸ��µ�"+(i+1)+"��,���Ե�...");
//									out.flush();
								}else{
									System.out.println("patentNo:"+patentNo+" syn error");
								}
							}else{
								System.out.println("patentNo:"+patentNo+" ��ȡר��������ʧ�ܣ���ַ��http://www.cponline.gov.cn/��");
							}
							//}
						}
					}// end for
				}// end if(webClient!=null){
				else{
					System.out.println("ר������http://www.cponline.gov.cn/������ʧ�� ----------- connect failure��  ");
				}
				}else{
					System.out.println("��������ͬ����ִ��,��ֹ�ٴ�ִ��");
					response.setContentType("text/html;charset=utf-8");
					PrintWriter out=response.getWriter();
					out.println("111");             //��ֹ�ٴε��������ť������ǰ̨
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
			System.out.println("������");
		}
		//KjPatent kjp = patentDao.findByPk(Long.valueOf(patentId));
			//Ϊ�˷����������ִ�е�Ч�ʣ����ڿ�����ʡ��1 2��ֱ��ִ��3������ǰ20����N�����ж�����Ч��
			
//		} else {
//			System.out.println("ר����ݶ��ȡ��ͬ������ �ѽ��� ----------- PatentFeeTimerTask not open�� ");
//		}
//		for(int i=0;i<updatedPatentId.size();i++)
//		{
//			System.out.println(updatedPatentId.get(i));
//		}
		long endTime=System.currentTimeMillis(); //��ȡ����ʱ��  
		System.out.println("ͬ����������ʱ�䣺 -----------  PatentFeeTimerTask cost time�� "+(endTime-startTime)+"ms");
		String sendType = "";
//		sendType = TechSystem.getInstance().getTechConfig().getPatenFeeMailSendType();
		//TODO sendType �к����ַ�1��������ר�����ʼ�������2����ʵ�����ͷ�������3���������Ʒ���
//		List<Long> sendMailPatentList = getListForSendMail(updatedPatentId,sendType); //����patentIdList��sendType���ж��Ƿ���Ϸ����ʼ������������б�
//		System.out.println("����Ҫ��ļ�¼���� ----------- toSend mail �� "+sendMailPatentList.size());
		
		System.out.println(" ----  senMail function will open in coming days ...");
		
		//��patentId�����������Ա�����ʼ�
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
		//���Է����ʼ�����
//		Long id = 2283L;
//		try {
//			sendMailById(id);
//		} catch (ParseException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}	
		
		System.out.println("����ʱϵͳʱ�� ----------- end PatentFeeTimerTask�� "+new Date(Calendar.getInstance().getTimeInMillis()));
//		}else{
//			System.out.println("��������ͬ����ִ��,��ֹ�ٴ�ִ��");
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
			System.out.println("ר�����ͬ�����ԣ�patentId-->"+patentId);
			
			String patentNo=ParamUtil.getParameterFrom(request, "patentNo");
			if(patentNo.contains("."))patentNo=patentNo.replace(".", "");
			
			
			
			long startTime=System.currentTimeMillis();   //��ȡ��ʼʱ��  
			long tic,tid;
			
			
			
			//String sqlWhere = "";//������ѵ��б�
			//sqlWhere=" as kj where 1=1 and kj.patentId.patentId="+patentId+" asc ";
			KjPatentFeeDao patentFeedao = KjPatentFeeDao.getInstance();
			List<KjPatentFee> kpfs=patentFeedao.findByPIdAndConfig(Long.valueOf(patentId),11L);
			
			KjPatentDao patentDao = KjPatentDao.getInstance();//ר��
			KjPatent kjp = patentDao.findByPk(Long.valueOf(patentId));
			if(patentNo.length()==0)
			{
				patentNo = kjp.getPatentNo();
			}
			System.out.println("ר�����ͬ�����ԣ�patentNo-->"+patentNo);
			
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
			
			if(webClient!=null){//��½�ɹ�
//				Document patentDoc = patentClawer.quickGetXmlByPatentId(patentNo);
				Document patentDoc =  patentClawer.getDocOfCponline(webClient, patentNo);
				if(patentDoc!=null)
				{
					tid = System.currentTimeMillis(); 
					System.out.println("��ȡ����ʱ�䣺 "+(tid-tic)+"ms");
					tic = System.currentTimeMillis(); 
					boolean isSucceed = patentClawer.synPatentAndFee(patentDoc,kjp,userbean,0); //0������ͬ��
					if(isSucceed)System.out.println("patentNo:"+patentNo+" syn succeed");
		
					tid = System.currentTimeMillis(); 
					System.out.println("ͬ������ʱ�䣺 "+(tid-tic)+"ms");
					long endTime=System.currentTimeMillis(); //��ȡ����ʱ��  
					System.out.println("��������ʱ�䣺 "+(endTime-startTime)+"ms");
					System.out.println("done��������");
				}else{
					System.out.println("��ȡ����ʧ��");
					return null;
				}
			}else{
				System.out.println("��½ʧ��");
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
//		//out.close();//�˴������Ƿ�sendһ����¼�͹ر�
//	}
	
}
