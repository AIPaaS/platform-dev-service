package com.aic.paas.dev.provider.svc.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.aic.paas.dev.provider.bean.CPcBuildDef;
import com.aic.paas.dev.provider.bean.CPcImageDef;
import com.aic.paas.dev.provider.bean.CPcProduct;
import com.aic.paas.dev.provider.bean.CPcProject;
import com.aic.paas.dev.provider.bean.PcBuildDef;
import com.aic.paas.dev.provider.bean.PcBuildTask;
import com.aic.paas.dev.provider.bean.PcImageDef;
import com.aic.paas.dev.provider.bean.PcProduct;
import com.aic.paas.dev.provider.bean.PcProject;
import com.aic.paas.dev.provider.db.PcBuildDefDao;
import com.aic.paas.dev.provider.db.PcBuildTaskDao;
import com.aic.paas.dev.provider.db.PcImageDefDao;
import com.aic.paas.dev.provider.db.PcProductDao;
import com.aic.paas.dev.provider.db.PcProjectDao;
import com.aic.paas.dev.provider.svc.PcBuildSvc;
import com.aic.paas.dev.provider.svc.bean.PcBuildDefInfo;
import com.aic.paas.dev.provider.util.HttpClientUtil;
import com.aic.paas.dev.provider.util.bean.PcBuildTaskCallBack;
import com.binary.core.util.BinaryUtils;
import com.binary.framework.exception.ServiceException;
import com.binary.jdbc.Page;
import com.binary.json.JSON;

public class PcBuildSvcImpl implements PcBuildSvc {
	static final Logger logger = LoggerFactory.getLogger(PcBuildSvcImpl.class);
	
	@Autowired
	PcBuildDefDao buildDefDao;
	
	
	@Autowired
	PcProductDao productDao;
	
	
	@Autowired
	PcProjectDao projectDao;
	
	
	@Autowired
	PcImageDefDao imageDefDao;
	
	@Autowired
	PcBuildTaskDao buildTaskDao;
	
	private String paasTaskUrl;

	
	
	public void setPaasTaskUrl(String paasTaskUrl) {
		if(paasTaskUrl != null) {
			this.paasTaskUrl = paasTaskUrl.trim();
		}
	}

	
	@Override
	public Page<PcBuildDef> queryDefPage(Integer pageNum, Integer pageSize, CPcBuildDef cdt, String orders) {
		return buildDefDao.selectPage(pageNum, pageSize, cdt, orders);
	}

	
	
	@Override
	public List<PcBuildDef> queryDefList(CPcBuildDef cdt, String orders) {
		return buildDefDao.selectList(cdt, orders);
	}
	
	

	@Override
	public PcBuildDef queryDefById(Long id) {
		return buildDefDao.selectById(id);
	}
	
	
	
	private List<PcBuildDefInfo> fillDefInfo(List<PcBuildDef> ls) {
		List<PcBuildDefInfo> infos = new ArrayList<PcBuildDefInfo>();
		if(ls!=null && ls.size()>0) {
			Long[] defIds = new Long[ls.size()];
			Set<Long> productIds = new HashSet<Long>();
			Set<Long> projectIds = new HashSet<Long>();
			Set<Long> imageDefIds = new HashSet<Long>();
			
			//key=defIds
			Map<Long, PcBuildDefInfo> infomap = new HashMap<Long, PcBuildDefInfo>();
			
			for(int i=0; i<ls.size(); i++) {
				PcBuildDef def = ls.get(i);
				PcBuildDefInfo info = new PcBuildDefInfo();
				info.setDef(def);
				infos.add(info);
				defIds[i] = def.getId();
				infomap.put(defIds[i], info);
				
				Long productId = def.getProductId();
				Long projectId = def.getProjectId();
				Long imageDefId = def.getImageDefId();
				
				if(productId != null) productIds.add(productId);
				if(projectId != null) projectIds.add(projectId);
				if(imageDefId != null) imageDefIds.add(imageDefId);
			}
			
			if(productIds.size() > 0) {
				CPcProduct cdt = new CPcProduct();
				cdt.setIds(productIds.toArray(new Long[0]));
				List<PcProduct> pls = productDao.selectList(cdt, null);
				if(pls.size() > 0) {
					Map<Long, PcProduct> map = BinaryUtils.toObjectMap(pls, "ID");
					for(int i=0; i<infos.size(); i++) {
						PcBuildDefInfo info = infos.get(i);
						Long productId = info.getDef().getProductId();
						if(productId != null) {
							info.setProduct(map.get(productId));
						}
					}
				}
			}
			
			if(projectIds.size() > 0) {
				CPcProject cdt = new CPcProject();
				cdt.setIds(projectIds.toArray(new Long[0]));
				List<PcProject> pls = projectDao.selectList(cdt, null);
				if(pls.size() > 0) {
					Map<Long, PcProject> map = BinaryUtils.toObjectMap(pls, "ID");
					for(int i=0; i<infos.size(); i++) {
						PcBuildDefInfo info = infos.get(i);
						Long projectId = info.getDef().getProjectId();
						if(projectId != null) {
							info.setProject(map.get(projectId));
						}
					}
				}
			}
			
			if(imageDefIds.size() > 0) {
				CPcImageDef cdt = new CPcImageDef();
				cdt.setIds(imageDefIds.toArray(new Long[0]));
				List<PcImageDef> pls = imageDefDao.selectList(cdt, null);
				if(pls.size() > 0) {
					Map<Long, PcImageDef> map = BinaryUtils.toObjectMap(pls, "ID");
					for(int i=0; i<infos.size(); i++) {
						PcBuildDefInfo info = infos.get(i);
						Long imageDefId = info.getDef().getImageDefId();
						if(imageDefId != null) {
							info.setImageDef(map.get(imageDefId));
						}
					}
				}
			}
			
			
			List<PcBuildTask> taskls = buildTaskDao.selectLastList(defIds);
			for(int i=0; i<taskls.size(); i++) {
				PcBuildTask task = taskls.get(i);
				infomap.get(task.getBuildDefId()).setLastBuildTask(task);
			}
		}
		
		return infos;		
	}
	
	
	
	@Override
	public Page<PcBuildDefInfo> queryDefInfoPage(Integer pageNum, Integer pageSize, CPcBuildDef cdt, String orders) {
		Page<PcBuildDef> page = queryDefPage(pageNum, pageSize, cdt, orders);
		List<PcBuildDef> ls = page.getData();
		List<PcBuildDefInfo> infols = fillDefInfo(ls);
		return new Page<PcBuildDefInfo>(page.getPageNum(), page.getPageSize(), page.getTotalRows(), page.getTotalPages(), infols);
	}


	

	@Override
	public List<PcBuildDefInfo> queryDefInfoList(CPcBuildDef cdt, String orders) {
		List<PcBuildDef> ls = queryDefList(cdt, orders);
		return fillDefInfo(ls);
	}



	@Override
	public PcBuildDefInfo queryByDefInfoId(Long id) {
		PcBuildDef def = queryDefById(id);
		if(def == null) return null;
		
		List<PcBuildDef> ls = new ArrayList<PcBuildDef>();
		ls.add(def);
		return fillDefInfo(ls).get(0);
	}

	
	
	
	
	@Override
	public Long saveOrUpdateDef(PcBuildDef record,String userCode,String mntCode) {
		BinaryUtils.checkEmpty(record, "record");
		BinaryUtils.checkEmpty(record.getMntId(), "record.mntId");
		
		boolean isadd = record.getId() == null;
		if(isadd) {
			BinaryUtils.checkEmpty(record.getBuildName(), "record.buildName");
			BinaryUtils.checkEmpty(record.getIsExternal(), "record.isExternal");
			if(record.getIsExternal().intValue() == 0) {
				BinaryUtils.checkEmpty(record.getProductId(), "record.productId");
				BinaryUtils.checkEmpty(record.getProjectId(), "record.projectId");
			}
			BinaryUtils.checkEmpty(record.getRespType(), "record.respType");
			BinaryUtils.checkEmpty(record.getRespUrl(), "record.respUrl");
			BinaryUtils.checkEmpty(record.getRespUser(), "record.respUser");
			BinaryUtils.checkEmpty(record.getRespPwd(), "record.respPwd");
			
			BinaryUtils.checkEmpty(record.getRespBranch(), "record.respBranch");
			BinaryUtils.checkEmpty(record.getDepTag(), "record.depTag");
			BinaryUtils.checkEmpty(record.getImageDefId(), "record.imageDefId");
			BinaryUtils.checkEmpty(record.getDockerFilePath(), "record.dockerFilePath");
		}else {
			if(record.getBuildName() != null) BinaryUtils.checkEmpty(record.getBuildName(), "record.buildName");
			if(record.getIsExternal() != null) BinaryUtils.checkEmpty(record.getIsExternal(), "record.isExternal");
			if(record.getProductId() != null) BinaryUtils.checkEmpty(record.getProductId(), "record.productId");
			if(record.getProjectId() != null) BinaryUtils.checkEmpty(record.getProjectId(), "record.projectId");
			if(record.getRespType() != null) BinaryUtils.checkEmpty(record.getRespType(), "record.respType");
			if(record.getRespUrl() != null) BinaryUtils.checkEmpty(record.getRespUrl(), "record.respUrl");
			if(record.getRespUser() != null) BinaryUtils.checkEmpty(record.getRespUser(), "record.respUser");
			if(record.getRespPwd() != null) BinaryUtils.checkEmpty(record.getRespPwd(), "record.respPwd");
			if(record.getImageDefId() != null) BinaryUtils.checkEmpty(record.getImageDefId(), "record.imageDefId");
			if(record.getDockerFilePath() != null) BinaryUtils.checkEmpty(record.getDockerFilePath(), "record.dockerFilePath");
		}
		
		if(record.getBuildName() != null) {
			String name = record.getBuildName().trim();
			record.setBuildName(name);
		}
		Map<String,Object> paramMap = new HashMap<String,Object>();
		Map<String,Object> buildConfigMap = new HashMap<String,Object>();
		Map<String,Object> tagConfigsMap = new HashMap<String,Object>();
		PcImageDef pcImageDef = imageDefDao.selectById(record.getImageDefId());
		
		if(pcImageDef != null){
			tagConfigsMap.put("code_repo_type", "branch");
			tagConfigsMap.put("code_repo_type_value", "master");
			tagConfigsMap.put("docker_repo_tag", record.getDepTag());
			tagConfigsMap.put("dockerfile_location", record.getDockerFilePath().trim());
			tagConfigsMap.put("is_active", "true");
			if(record.getOpenCache()!=null&&record.getOpenCache()==1){
				tagConfigsMap.put("build_cache_enabled","true");
			}else{
				tagConfigsMap.put("build_cache_enabled","false");
			}
			
			buildConfigMap.put("tag_configs", tagConfigsMap);
			buildConfigMap.put("code_repo_client", "Gitlab");
			buildConfigMap.put("code_repo_clone_url",record.getRespUrl().trim());
			
			paramMap.put("namespace", mntCode+"_____"+userCode);
			paramMap.put("repo_name", record.getBuildName().substring(1).trim());
			paramMap.put("image_name", pcImageDef.getImageFullName().substring(1).trim());
			paramMap.put("description", pcImageDef.getImageFullName().substring(1).trim());
			paramMap.put("is_public", "false");
			if(record.getOpenEmail()==1){
				paramMap.put("email_enabled", "true");
			}else{
				paramMap.put("email_enabled", "false");
			}
			paramMap.put("email", "");
			paramMap.put("build_config", buildConfigMap);
			
		}
		String result=null;
		String param = JSON.toString(paramMap);
		if(record.getId()!=null){
			result = HttpClientUtil.sendPostRequest(paasTaskUrl+"/dev/buildDefMvc/updateBuildDefApi", param);
		}else{
			result = HttpClientUtil.sendPostRequest(paasTaskUrl+"/dev/buildDefMvc/buildDefApi", param);
		}
		if(result!=null &&!"".equals(result)&&"success".equals(result)){
			return buildDefDao.save(record);
		}else{
			 throw new ServiceException("调用构建定义接口失败! ");
		}
		
	}
	
	
	
	

	@Override
	public int removeDefById(Long id) {
		return buildDefDao.deleteById(id);
	}



	@Override
	public int checkBuildFullName(PcBuildDef record) {
		Long id = record.getId();
		if(record.getBuildName() != null) {
			String name = record.getBuildName().trim();
			record.setBuildName(name);
			
			CPcBuildDef cdt = new CPcBuildDef();
			cdt.setMntId(record.getMntId());
			List<PcBuildDef> ls = buildDefDao.selectListByFullBuildName(name, cdt, null);
			if(ls.size()>0 && (id==null || ls.size()>1 || ls.get(0).getId().longValue()!=id.longValue())) {
				return 0;
			}else{
				return 1;
			}
		}	
		return 0;
	}
	
	
	@Override
	public String queryCompRoomIdByCallBack(PcBuildTaskCallBack pbtc) {
		String  queryResult = "error";
		//所属机房
		String mntId = pbtc.getMnt_id();
		String buildName = pbtc.getRepo_name();
		String depTag =pbtc.getTag();
		
		//2.根据租户id [MNT_ID]和repo_name[BUILD_NAME]和tag[DEP_TAG]获取一条 部署定义记录
		CPcBuildDef cbd = new CPcBuildDef();
		cbd.setMntId(Long.parseLong(mntId));
		cbd.setBuildName(buildName);
		
		cbd.setDepTagEqual(depTag);
		cbd.setDataStatus(1);
		List<PcBuildDef> cbdlist = buildDefDao.selectList(cbd, null);
		PcBuildDef pbd = new PcBuildDef();
		
		String compRoomId = "";
		logger.info("========paas-provider-dev:PcBuildSvcImpl:queryCompRoomIdByCallBack:cbdlist.size() = "+cbdlist.size());
		if(cbdlist!=null && cbdlist.size()>0){
			pbd =cbdlist.get(0);
			if(pbd.getProductId()!=null){
				PcProduct pp = new PcProduct();
				pp = productDao.selectById(pbd.getProductId());
				if(pp.getCompRoomId()!=null)compRoomId = pp.getCompRoomId().toString();
			}
		}else{
			logger.info("查询不到构建定义记录记录！");
			return queryResult;
		}
		if("".equals(compRoomId)){
			logger.info("查询不到机房Id！");
			return queryResult;
		}
		return compRoomId;
	}

	
}






