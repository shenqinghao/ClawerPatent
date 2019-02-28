package inc.tech.sys.chop.dao;

import inc.tech.persistent.AbstractDao;
import inc.tech.persistent.entity.KjAuditLog;
import inc.tech.persistent.entity.KjDictObjtype;
import inc.tech.student.dao.KjStudentDao;
import inc.tech.sys.user.UserBean;
import inc.tech.user.dao.KjUserDAO;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Expression;

public class KjAuditLogDAO extends AbstractDao<KjAuditLog, Long> {
	private static KjAuditLogDAO dao;
    
	public static Long[] OBJECT_TYPE={1L,2L,3L,4L};
	
	
	private KjAuditLogDAO() {
	}

	public static KjAuditLogDAO getInstance() {
		if (dao == null)
			dao = new KjAuditLogDAO();
		return dao;
	}
	public KjAuditLog getAuditLog( Long objtypeId,Long objId){
		KjDictObjtype type = new KjDictObjtype();
		type.setObjtypeId(objtypeId);
		Criterion crit1 = Expression.eq("kjDictObjtype",type);
		Criterion crit2 = Expression.eq("objId", objId);
		
		List<KjAuditLog> k =super.findByCriteria(crit1,crit2);
		KjAuditLog l =null;
		if(k.size()>0) l = k.get(0);
		return l;
	}	@SuppressWarnings("unchecked")
	public KjAuditLog getChopLog( Long objtypeId,Long objId){
	
		String sql = "from KjAuditLog as log where log.kjDictObjtype='"+objtypeId+"' and log.objId='"+objId+"' order by log.logId desc";
		
		List<KjAuditLog> k =getSession().createQuery(sql).list();
		KjAuditLog l =null;
		if(k.size()>0) l = k.get(0);
		return l;
	}
	
	
	public Long save(KjAuditLog auditLog,HttpServletRequest request){
		
		// 对 request 中的userBean中的授权信息进行处理
		HttpSession session =request.getSession();
		UserBean userbean = (UserBean) session.getAttribute("UserBean");
		String logInfo = "";
		if(userbean.getIsSelf() == 0){
			if(userbean.getAuthUserId().length() > 6 ){
				logInfo = KjStudentDao.getInstance().getStudentLog( userbean.getAuthUserId());
			}else{
				logInfo = KjUserDAO.getInstance().getAuthUserLog( userbean.getAuthUserId());
			}
			if(auditLog.getOperMsg()!=null){
				auditLog.setOperMsg(auditLog.getOperMsg() + logInfo);
			}else{
				auditLog.setOperMsg(logInfo);
			}
		}
		save(auditLog);
		return auditLog.getLogId();
	}
	
	public List<KjAuditLog> getAuditLogList( Long objtypeId,Long objId){
		KjDictObjtype type = new KjDictObjtype();
		type.setObjtypeId(objtypeId);
		Criterion crit1 = Expression.eq("kjDictObjtype",type);
		Criterion crit2 = Expression.eq("objId", objId);
		
		List<KjAuditLog> k =super.findByCriteria(crit1,crit2);
		if(k!=null && k.size()>0)return k;
		else return null;
	}
	
	public String getLatestLogDate( Long objtypeId,Long objId,String likeString){
		
		String sql = "from KjAuditLog as log where log.kjDictObjtype='"+objtypeId+"' and log.objId='"+objId+"' and log.operMsg like '%"+likeString+"%' order by log.logId desc";
		
		List<KjAuditLog> k =getSession().createQuery(sql).list();
		KjAuditLog l =null;
		if(k.size()>0)
		{
			return k.get(0).getOperTime();
		}
		
		return "";
	}
	public boolean getBatchSynRecordToday(String dateToday){
		String sql = "from KjAuditLog as log where log.oper='批量同步数据' and log.operTime='"+dateToday+"'";
		List<KjAuditLog> k =getSession().createQuery(sql).list();
		if(k.size()>0){
			return false;
		}else{
			return true;
		}
	}
}
