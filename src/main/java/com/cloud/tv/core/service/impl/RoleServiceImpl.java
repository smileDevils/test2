package com.cloud.tv.core.service.impl;

import com.cloud.tv.core.mapper.RoleMapper;
import com.cloud.tv.core.service.IRoleGroupService;
import com.cloud.tv.core.service.IRoleResService;
import com.cloud.tv.entity.Res;
import com.cloud.tv.entity.Role;
import com.cloud.tv.entity.RoleGroup;
import com.cloud.tv.entity.RoleRes;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.cloud.tv.core.service.IResService;
import com.cloud.tv.core.service.IRoleService;
import com.cloud.tv.dto.RoleDto;
import com.cloud.tv.entity.*;
import com.cloud.tv.vo.RoleVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;


@Service("roleService")
@Transactional
public class RoleServiceImpl implements IRoleService {

    @Autowired
    private RoleMapper roleMapper;
    @Autowired
    private IRoleGroupService roleGroupService;
    @Autowired
    private IResService resService;
    @Autowired
    private IRoleResService roleResService;

    @Override
    public Role findRoleById(Long id) {
        return this.roleMapper.findRoleById(id);
    }

    @Override
    public List<Role> findRoleByType(String type) {
        return this.roleMapper.findRoleByType(type);
    }

    @Override
    public boolean countBy(String name) {
        int count = this.roleMapper.countBy(name);
        return count != 0;
    }

    @Override
    public List<Role> findRoleByUserId(Long user_id) {
        return this.roleMapper.findRoleByUserId(user_id);
    }

    @Override
    public List<Role> findRoleByRoleGroupId(Long role_group_id) {
        return this.roleMapper.findRoleByRoleGroupId(role_group_id);
    }

    @Override
    public List<Role> findRoleByResId(Long res_id) {
        return this.roleMapper.findRoleByResId(res_id);
    }

    @Override
    public List<Role> findRoleByIdList(List<Integer> list) {
        return this.roleMapper.findRoleByIdList(list);
    }

    @Override
    public Role selectByPrimaryUpdae(Long id) {
        return this.roleMapper.selectByPrimaryUpdae(id);
    }

    @Override
    public boolean save(RoleDto instance) {
        Role role = null;
        if(instance.getId() == null){
            role = new Role();
            role.setAddTime(new Date());
        }else{
            role = this.roleMapper.findRoleById(instance.getId());
        }
        if(instance.getRg_id() != null){
            RoleGroup roleGroup = this.roleGroupService.getObjById(instance.getRg_id());
            role.setRoleGroup(roleGroup);
        }

        BeanUtils.copyProperties(instance, role);
        role.setType("ADMIN");
        if(role.getId() == null){
            try {
                this.roleMapper.insert(role);
                // ??????????????????
                if(instance.getRes_id() != null && instance.getRes_id().length > 0){
                    List<Integer> idList = Arrays.asList(instance.getRes_id());
                    List<Res> resList = this.resService.findResByResIds(idList);
                    List<RoleRes> roleResList = new ArrayList<RoleRes>();
                    for(Res res : resList){
                        RoleRes roleRes = new RoleRes();
                        roleRes.setRes_id(res.getId());
                        roleRes.setRole_id(role.getId());
                        roleResList.add(roleRes);
                    }
                    this.roleResService.batchAddRoleRes(roleResList);
                }
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }else{
            try {
                if(instance.getRes_id() != null){
                    // ????????????????????????
                    this.roleResService.deleteRoleResByRoleId(role.getId());
                    // ??????????????????
                    List<Integer> idList = Arrays.asList(instance.getRes_id());
                    if(idList.size() > 0){
                        List<Res> resList = this.resService.findResByResIds(idList);
                        List<RoleRes> roleResList = new ArrayList<RoleRes>();
                        for(Res res : resList){
                            RoleRes roleRes = new RoleRes();
                            roleRes.setRes_id(res.getId());
                            roleRes.setRole_id(role.getId());
                            roleResList.add(roleRes);
                        }
                        this.roleResService.batchAddRoleRes(roleResList);
                    }
                }
                this.roleMapper.update(role);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    @Override
    public Object update(RoleDto instance) {
        return null;
    }

    @Override
    public List<Role> query(Map params) {

//         Page<Role> page = PageHelper.startPage((Integer)params.get("startRow"), (Integer)params.get("pageSize"));
//        return page;
        return this.roleMapper.query(params);

    }

    @Override
    public List<RoleVo> queryRole(Map params) {
       Page<RoleVo> page = PageHelper.startPage((Integer)params.get("currentPage"), (Integer)params.get("pageSize"));
         this.roleMapper.queryVo(params);
       return page;
    }

    @Override
    public boolean delete(Long id) {
        int flag = this.roleMapper.delete(id);
        return flag != 0;
    }

    @Override
    public int batchUpdateRoleGroupId(Map map) {
        return this.roleMapper.batchUpdateRoleGroupId(map);

    }

}
