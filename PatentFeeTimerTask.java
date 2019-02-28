package inc.tech.patent.util;

import inc.tech.patent.action.*;
import inc.tech.patent.dao.KjPatentDao;
import inc.tech.patent.dao.KjPatentFeeDao;
import inc.tech.patent.dao.KjProjectStaffDao;
import inc.tech.persistent.DAOException;
import inc.tech.persistent.entity.KjAuditLog;
import inc.tech.persistent.entity.KjPatent;
import inc.tech.persistent.entity.KjPatentFee;
import inc.tech.persistent.entity.KjProjectstaff;
import inc.tech.persistent.entity.KjTableTemplate;
import inc.tech.persistent.entity.KjUser;
import inc.tech.sys.chop.dao.KjAuditLogDAO;
import inc.tech.sys.init.TechSystem;
import inc.tech.sys.mail.dao.KjMailDAO;
import inc.tech.sys.mail.form.KjMailForm;
import inc.tech.sys.table.dao.KjTableTemplateDao;
import inc.tech.user.dao.KjUserDAO;
import inc.tech.util.Clawer;
import inc.tech.util.DateUtil;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TimerTask;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.jsoup.nodes.Document;
import org.xml.sax.SAXException;

import com.gargoylesoftware.htmlunit.WebClient;
 
public class PatentFeeTimerTask extends TimerTask {
	Clawer patentClawer = new Clawer();
	WebClient webClient = null;
	
	public void run(){
		
		List<Long> updatedPatentId = new ArrayList<Long>();//for email
		long startTime=System.currentTimeMillis();   //��ȡ��ʼʱ��  
		
//		if (TechSystem.getInstance().getTechConfig().getPatenFeeGetOn())             ��ע�͵���  
		if (true)
		{
		System.out.println(" PatentFeeTimerTask  ר�������û�ȡ ����.... "  );
		
		
			//TODO 1 ȡ���� �Ƽ�ϵͳ�й���״̬Ϊ31�Ժ�����ݡ� ��ʹ�ù���ʱ�����򡢷��顣
		KjPatentDao patentDao = KjPatentDao.getInstance();//ר��
//		String sqlWhere = "from KjPatent as kj where kj.deleteMark < 1 and kj.state in (select s.code from KjPatentState s where s.id >= 31 )"
//				+" and kj.patentNo is not null and (kj.patentForeignType is null or kj.patentForeignType<'1') order by patentId desc ";
		String sqlWhere = "from KjPatent as kj where kj.deleteMark < 1 and kj.patentNo in ('2015107021978','2011103261546','2011103318309','2010105221873')"
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
				if(webClient!=null){
					long endLoginTime=System.currentTimeMillis(); //��ȡ����ʱ��  
//					System.out.println("����ʱ�䣺 "+(endLoginTime-startLoginTime)+"ms");
					
//					for(int i =0;i<1000;i++)
					for(int i =0;i<kjpl.size() ;i++)
//					for(int i =2200;i<kjpl.size() && i<2250 ;i++)
					{
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
							long daysFromLastLog = daysFromLastUpdateLogDate(kjpl.get(i));
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
								boolean isSucceed = patentClawer.synPatentAndFee(doc,kjpl.get(i),null);
								if(isSucceed){//������³ɹ�
									updatedPatentId.add(kjpl.get(i).getPatentId());
									System.out.println("patentNo:"+patentNo+" syn succeed");
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
			
		} else {
			System.out.println("ר����ݶ��ȡ��ͬ������ �ѽ��� ----------- PatentFeeTimerTask not open�� ");
		}
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
	}
	public static long daysFromLastUpdateLogDate(KjPatent kjp) throws ParseException{
		KjAuditLogDAO kjAuditdao = KjAuditLogDAO.getInstance();
		List<KjAuditLog> kjAuditLogList = kjAuditdao.getAuditLogList(15l, kjp.getPatentId());
		if(kjAuditLogList!=null && kjAuditLogList.size()>0)
		{
			//�Ի�ȡ����־����
			Collections.sort(kjAuditLogList, new Comparator<KjAuditLog>() {//��־�ӽ���������������Խ����Խǰ��
	            public int compare(KjAuditLog arg0, KjAuditLog arg1) {
	                return arg1.getOperTime().compareTo(arg0.getOperTime());
	            }
	        });
			//��ȡ�ϴ���־ʱ��
			String lastUpdateLogTime ="";
			for(int i=0;i<kjAuditLogList.size();i++){//ͬ��ר������������
				if(kjAuditLogList.get(i).getOperMsg()!=null 
						&& kjAuditLogList.get(i).getOperMsg().contains("ͬ��ר��������")){
					lastUpdateLogTime= kjAuditLogList.get(i).getOperTime();
					break;
				}
			}
			if(!"".equals(lastUpdateLogTime)){//˵��֮ǰ��ͬ���ļ�¼
				//������������ʱ��
				SimpleDateFormat df=new SimpleDateFormat("yyyy-MM-dd");
			    Date todayDate=new java.util.Date();
			    Date lastDate=df.parse(lastUpdateLogTime);
			    long lastTime=lastDate.getTime();
			    long todayTime=todayDate.getTime();
//			    System.out.println((todayTime-lastTime)/(1000*60*60*24));
			    long intervalDays = (todayTime-lastTime)/(1000*60*60*24);
			    return intervalDays;
			}else{//˵��֮ǰû��ͬ���ļ�¼   ��������
				return -1;
			}
			
		    
		}else{
			return -1;
		}
		
	}
	
	/*
	 * yzw 2015-11-15 ����ר��id�б����ʼ�
	 */
	public void sendMailById(Long id)throws ParseException, IOException
	{
		
		KjUser kjUser = null;
		KjUserDAO kjUserDao = KjUserDAO.getInstance();
		KjPatent kjPatent =new KjPatent();
		KjMailForm kjMailForm = new KjMailForm(); //��ʼ���ʼ�Form
		StringBuffer contentStrBuf = new StringBuffer(""); //�ʼ����ݻ���
		String mailType = "patentFee";    //�ʼ�����
		String firstObjName = "";  //�����ʼ�����
		
		kjMailForm.setTitle("����ר����");
		kjMailForm.setMailType(mailType);
		
		//��ȡ�ʼ�����
		kjPatent = KjPatentDao.getInstance().findByPk(id);
	
		kjUser = kjPatent.getKjFirstInventorId();
	    firstObjName = "��"+kjPatent.getName()+"��" + " �辡��������";
	    
	    KjTableTemplate kjTableTemplate = KjTableTemplateDao.getInstance().searchTable("ר������", 15l);
	    String thisContent = kjTableTemplate.getContent();
	    thisContent = thisContent.replaceAll("param_patentName", kjPatent.getName()==null?"":kjPatent.getName());
	    thisContent = thisContent.replaceAll("param_patentNo", kjPatent.getPatentNo()==null?"":kjPatent.getPatentNo());
	    thisContent = thisContent.replaceAll("param_patentName", kjPatent.getKjFirstInventorId().getName());
	    String remindStr = KjPatentFeeDao.getInstance().getRemindStringByPatentId(kjPatent.getPatentId());
	    thisContent = thisContent.replaceAll("param_feeNameAndAmount", remindStr.substring(remindStr.indexOf("����")));
	    List<KjProjectstaff> newProjectStaffList = KjProjectStaffDao.getInstance().searchProjectStaff(kjPatent.getPatentId(), 2L, "0");
	    List<KjProjectstaff> oldProjectStaffList = KjProjectStaffDao.getInstance().searchProjectStaff(kjPatent.getPatentId(), 0L, "0");
	    if(newProjectStaffList==null || newProjectStaffList.size()==0) newProjectStaffList = oldProjectStaffList;
	    	String allUsername = "";
	    if(newProjectStaffList!=null)
	    {
	    	for (int j = 0; j < newProjectStaffList.size(); j++) 
	    	{
	    		if(j==0) allUsername = newProjectStaffList.get(j).getKjUser().getName();
	    		else allUsername = allUsername + "," + newProjectStaffList.get(j).getKjUser().getName();
			}
	    }
	    thisContent = thisContent.replaceAll("param_allInventors", allUsername==null?"":allUsername);
	    String acceptDate = DateUtil.convertDateToString(kjPatent.getAcceptDate());
	    thisContent = thisContent.replaceAll("param_applyDate", acceptDate==null?"":acceptDate);
	    			
	    contentStrBuf.append(thisContent);
	    			
		 kjMailForm.setTitle(kjMailForm.getTitle() + firstObjName + " ������");
		 kjMailForm.setContent(contentStrBuf.toString());
		 
		 String content = contentStrBuf.toString();
		 String title = kjMailForm.getTitle();
		 String sendMailStatus = "";
		 String sendSmsStatus = "δ�ύ��������";
		 
		 //��ȡ������Ա
		 List<String> toUserId = new ArrayList<String>();
		 /*/(ֻ��ȡ��һ������id��ֻ�Ե�һ�����˷����ʼ�)
		 if(newProjectStaffList!=null && newProjectStaffList.size()>0)
		 {
			 if(newProjectStaffList.get(0).getStaffId()!=null)
				 toUserId.add(newProjectStaffList.get(0).getStaffId()); 
		 }*/
		 //��ȡ��ʦ�����ˣ���ȡ��һ�����ˣ�
		 for(int i=0;i<newProjectStaffList.size();i++)
		 {
			String sendMailUserIdTemp = newProjectStaffList.get(i).getStaffId();
			if(sendMailUserIdTemp !=null && sendMailUserIdTemp.length() == 6)  //staffIdλ����6λ����ʾ��ʦ
			{
				toUserId.add(sendMailUserIdTemp);
				break;
			}
		 }
		 /*KjMailAction kjMailAction = new KjMailAction();
		 List<String> thisUserIds =  kjMailAction.getUserId(id.toString(), mailType);
		 //KjPatent thisPatent = KjPatentDao.getInstance().findByPk(id);
		 if(thisUserIds!=null && thisUserIds.size()>0){
			for (int j = 0; j < thisUserIds.size(); j++){
				String thisUserId = thisUserIds.get(j);
				//KjUser thisUser = KjUserDAO.getInstance().findByPk(thisUserId);
				if(thisUserId.length()>0 && !toUserId.contains(thisUserId)){
					toUserId.add(thisUserId);
				}
			}
		 }*/
		 //start - �����ʼ�
		 String toAddressList = "";
		 String sendMailStatusItem = "δ�ύ��������";
		 //send mails
		 if(toUserId.size()>0){                         
				for (int j = 0; j < toUserId.size(); j++){
					Boolean isSend = false;
					String thisUserId = toUserId.get(j);
					kjUser = kjUserDao.findByPk(thisUserId);
					//System.out.println("emailAdd"+kjUser.getEmail());
						if (kjUser != null && kjUser.getEmail() != null && kjUser.getEmail().length() > 0){
							try{
								Properties p = new Properties(); // Properties p =
																	// System.getProperties();
								p.put("mail.smtp.auth", "true");
								//System.out.println(TechSystem.getInstance().getTechConfig().getFromServer());
								p.put("mail.transport.protocol", "smtp");
								p.put("mail.smtp.host", TechSystem.getInstance().getTechConfig().getFromServer());
								p.put("mail.smtp.port", "25");
								// �����Ự
								Session session2 = Session.getInstance(p);
								Message msg = new MimeMessage(session2); // ������Ϣ
								
								// zz 2012-03-26
								String fromMail = MimeUtility.encodeText( "kj.tju.edu.cn", "gb2312", "B");
		
								msg.setFrom(new InternetAddress( TechSystem.getInstance().getTechConfig().getFromAddress(), fromMail));
		
								msg.addRecipient(Message.RecipientType.TO, new InternetAddress(kjUser.getEmail()));
							
								// msg.setSentDate(); // ��������
								if (title != null && title.length() > 0)
									msg.setSubject(title); // ����
								msg.setContent(content, "text/html;charset = gbk");  //zz 215-04-30
								
								if(toAddressList.equals("")){
									toAddressList = kjUser.getEmail();
								}
								else{
									toAddressList = toAddressList + "," + kjUser.getEmail();
								}
								
								// �ʼ�������������֤,���ص��Ե�ʱ��Ҫ����
								Transport tran = session2.getTransport("smtp");
								tran.connect(TechSystem.getInstance().getTechConfig() .getFromServer(),
											TechSystem.getInstance() .getTechConfig().getFromAddress(), 
											TechSystem .getInstance().getTechConfig() .getFromPassword() );
								tran.sendMessage(msg, msg.getAllRecipients()); // ����
								
								isSend = true;
							} 
							catch (AddressException e) {
								//sendMap.put(kjUser.getEmail(), "�ռ���ַ����");
								sendMailStatusItem = "�ռ���ַ����";
							}
							catch (MessagingException e) {
								//sendMap.put(kjUser.getEmail(), "�ʼ����ͳ���");
								sendMailStatusItem = "�ʼ����ͳ���";
							} // ������
						}
						else{
							//sendMap.put(thisUserId, "�ռ��˻��ַ��Ϣ������");
							sendMailStatusItem = "�ռ��˻��ַ��Ϣ������";
						}
						if(isSend){
							sendMailStatusItem = "�ʼ����ͳɹ�";
						}
						else{
							sendMailStatusItem = "�ʼ�����ʧ��";
						}
					if(j == 0){
						sendMailStatus = sendMailStatusItem;
					}else{
						sendMailStatus = sendMailStatus + "," +sendMailStatusItem;
			        }
		        }
		}
		//���ͼ�¼�������ݿ�
		kjMailForm.setTableIds(id.toString());
		kjMailForm.setMailType(mailType);
		kjMailForm.setFromAddress(TechSystem.getInstance() .getTechConfig().getFromAddress());
		kjMailForm.setCreateDate(null);
			
		kjMailForm.setToAddress(toAddressList);
		kjMailForm.setContent(content);
		kjMailForm.setTitle(title);
			
		kjMailForm.setSendMailStatus(sendMailStatus);
		kjMailForm.setSendSmsStatus(sendSmsStatus);
		
		KjUser sendUser = KjUserDAO.getInstance().findByPk("k19122");
	    KjMailDAO.getInstance().saveKjMail(sendUser,kjMailForm);
	    
	    /*System.out.println("�ʼ����⣺"+title);
	    System.out.println("�ʼ����ݣ�"+content);
	    System.out.println("����״̬��"+sendMailStatus);
	    System.out.println("�ʼ����ͣ�"+mailType);
	    System.out.println("���Ͷ���"+toAddressList);
	    System.out.println("�����ߣ�"+kjMailForm.getFromAddress());
	    */
	}
	
	/*
	 * yzw 2015-11-15 ����ϵͳ���µ�ר��id�б��жϷ��Ϸ��ʼ�������ר��
	 */
	public List<Long> getListForSendMail(List<Long> idList, String sendType)
	{
		//����list
		/*idList.add(553L);
		idList.add(555L);
		idList.add(587L);
		idList.add(663L);
		idList.add(87L);
		idList.add(88L);
		idList.add(93L);*/
		List<Long> patentList = new ArrayList<Long>();
		//System.out.println("�ж��б�");
		
		for(int i=0;i<idList.size();i++)
		{
			Long patentId = idList.get(i);
			KjPatent kjPatent = KjPatentDao.getInstance().findByPk(patentId);
			String type = kjPatent.getType();
			Long giveUp = kjPatent.getIsGiveup();
			if(sendType.contains(type))
			{ 
				 if(giveUp != null && giveUp == 1)
					 continue;
			     Long remainDays = 10000L; // ��ǰר����ʣ������,����Сʣ������Ϊ׼
				 Date expireDay = new Date(); // ר���뵱ǰ��Сʣ��������Ӧ�Ľ�ֹ����(�����ҵ��ý�ֹ������Ҫ���ѵ����з��ü�¼)
				 Map resultMap = KjPatentFeeDao.getInstance().getRemainDayAndExpireDayByPatentId(patentId);
				 remainDays = (Long)resultMap.get("remainDays");
				 expireDay = (Date)resultMap.get("expireDay");
				 
				 Long between_days = 10000L;
				 if(KjMailDAO.getInstance().getMailBetweenDays(type, patentId, expireDay)!= null)
					 between_days =  KjMailDAO.getInstance().getMailBetweenDays(type, patentId, expireDay); //��ȡ���һ�η��ʼ������ں�ר��������֮�������
				 
				 //System.out.println("��ֹ����"+expireDay);
				 //System.out.println("ʣ������"+remainDays);
				 //System.out.println("�ϴη��ʼ��ൽ���յ�����"+between_days);
				 if(remainDays<=15)
				 {
					 if(between_days>15)
						 patentList.add(patentId);
				 }
				 else if(remainDays>15 && remainDays<=29)
				 {
					 if(between_days>29)
						 patentList.add(patentId);
				 }
				 else if(remainDays>29 && remainDays<=60)
				 {
					 if(between_days>60)
						 patentList.add(patentId);
				 }
			}
		}
		return patentList;
	}
	
	public static void main(String[] args)
	{
		PatentFeeTimerTask task = new PatentFeeTimerTask();
		task.run();
	}
	
}
