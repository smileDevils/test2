package com.cloud.tv.core.service.impl;

import com.cloud.tv.core.manager.admin.tools.ShiroUserHolder;
import com.cloud.tv.core.mapper.BuyerMapper;
import com.cloud.tv.core.mapper.LiveRoomMapper;
import com.cloud.tv.core.service.*;
import com.cloud.tv.core.utils.CommUtils;
import com.cloud.tv.core.utils.FileUtil;
import com.cloud.tv.dto.UserDto;
import com.cloud.tv.entity.*;
import com.cloud.tv.vo.UserVo;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.cloud.tv.core.service.*;
import com.cloud.tv.entity.*;
import com.cloud.tv.core.service.*;
import com.cloud.tv.entity.*;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@Transactional
public class UserServiceImpl implements IUserService {

    @Autowired
    private BuyerMapper buyerMapper;
    @Autowired
    private IUserRoleService userRoleService;
    @Autowired
    private IRoleService roleService;
    @Autowired
    private ISysConfigService sysConfigService;
    @Autowired
    private ILiveRoomService liveRoomService;
    @Autowired
    private LiveRoomMapper liveRoomMapper;

    @Override
    public User findByUserName(String username) {
        return this.buyerMapper.findByUserName(username);
    }

    @Override
    public User findRolesByUserName(String username) {
        return null;
    }

    @Override
    public UserVo findUserUpdate(Long id) {
        return this.buyerMapper.findUserUpdate(id);
    }

    @Override
    public User findObjById(Long id) {
        return this.buyerMapper.selectPrimaryKey(id);
    }

    @Override
    public Page<UserVo> query(Map params) {
        Page<UserVo> page = PageHelper.startPage((Integer) params.get("currentPage"), (Integer) params.get("pageSize"));
        List<UserVo> users = this.buyerMapper.selectAll(params);
        return page;
    }

    @Override
    public boolean save(UserDto dto) {
        User user = null;
        if(dto.getId() == null){
            user = new User();
            dto.setAddTime(new Date());
        }else{
            user = this.buyerMapper.selectPrimaryKey(dto.getId());
        }
        BeanUtils.copyProperties(dto, user);
        if(dto.getType() != null){
            if(dto.getType().equals("?????????")){
                user.setUserRole("?????????");
            }else{
                user.setUserRole("????????????");
            }
        }
        if(dto.getId() == null){
            try {
                this.buyerMapper.insert(user);
                // ???????????????
                Map params = new HashMap();
                params.put("currentPage", 0);
                params.put("pageSize", 1);
                params.put("userId", user.getId());
                List<LiveRoom> liveRoomList = this.liveRoomService.findObjByMap(params);
               if(liveRoomList.size() < 1){
                   LiveRoom instance = new LiveRoom();
                   instance.setDeleteStatus(0);
                   instance.setAddTime(new Date());
                   instance.setTitle(user.getUsername() + "????????????");
                   Date date = new Date();
                   SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
                   String df = sdf.format(date);
                   String bindCode = df + CommUtils.randomString(6);// ?????????
                   instance.setBindCode(bindCode);

                   instance.setUser(user);
                   instance.setUserId(user.getId());
                   instance.setUsername(user.getUsername());
                   if(user.getUserRole().equals("ADMIN")){
                       instance.setType(1);
                   }
                   instance.setIsEnable(1);
                   if(instance.getManager() == null || instance.getManager().equals("")){
                       instance.setManager(user.getUsername());
                   }
                   //rtmp://lk.soarmall.com:1935/hls
                   SysConfig SysConfig = this.sysConfigService.findSysConfigList();
                   String rtmp = CommUtils.getRtmp(SysConfig.getIp(), bindCode);
                   String obsRtmp = CommUtils.getObsRtmp(SysConfig.getIp());
                   String path =  SysConfig.getPath() + File.separator + bindCode;
                   instance.setRtmp(rtmp);
                   instance.setObsRtmp(obsRtmp);
                   try {
                       FileUtil.storeFile(path);
                       FileUtil.possessor(path);
                   } catch (IOException e) {
                       e.printStackTrace();
                   }
                   this.liveRoomMapper.save(instance);
               }

                // ??????????????????????????????
                if(dto.getRole_id() != null && dto.getRole_id().length > 0){
                    List<Integer> idList = Arrays.asList(dto.getRole_id());
                    List<Role> roleList = this.roleService.findRoleByIdList(idList);
                    List<UserRole> userRoles = new ArrayList<UserRole>();
                    for(Role role : roleList){
                        UserRole userRole = new UserRole();
                        userRole.setUser_id(user.getId());
                        userRole.setRole_id(role.getId());
                        userRoles.add(userRole);
                    }
                    try {
                        this.userRoleService.batchAddUserRole(userRoles);
                        return true;
                    } catch (Exception e) {
                        e.printStackTrace();
                        return false;
                    }
                }
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }else{
            // ????????????????????????
            try {
                // ??????????????????????????????
                if(dto.getRole_id() != null && dto.getRole_id().length > 0){
                    this.userRoleService.deleteUserByRoleId(user.getId());
                    List<Integer> idList = Arrays.asList(dto.getRole_id());
                    List<Role> roleList = this.roleService.findRoleByIdList(idList);
                    List<UserRole> userRoles = new ArrayList<UserRole>();
                    for(Role role : roleList){
                        UserRole userRole = new UserRole();
                        userRole.setUser_id(user.getId());
                        userRole.setRole_id(role.getId());
                        userRoles.add(userRole);
                    }
                    this.userRoleService.batchAddUserRole(userRoles);
                }
                this.buyerMapper.update(user);
                User currentUser = ShiroUserHolder.currentUser();


                // ??????????????? ????????????????????????
                // ??????????????????????????????????????????????????????????????????
                if(dto.isFlag() && currentUser.getId().equals(user.getId())){
                     SecurityUtils.getSubject().logout();
                    // ????????????????????????????????????Subject???????????????
                    Subject subject = SecurityUtils.getSubject();
                    PrincipalCollection principalCollection = subject.getPrincipals();
                    String realmName = principalCollection.getRealmNames().iterator().next();
                    User userInfo = this.buyerMapper.findByUserName(user.getUsername());// ??????????????????????????????Subject???
                    PrincipalCollection newPrincipalCollection =
                            new SimplePrincipalCollection(userInfo, realmName);
                    subject.runAs(newPrincipalCollection);
                }

                //??????????????? ???????????????????????????

                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    @Override
    public boolean update(User user) {
        try {
            this.buyerMapper.update(user);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean delete(User user) {
        // ??????????????????
        try {
            this.userRoleService.deleteUserByRoleId(user.getId());
            this.buyerMapper.delete(user.getId());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


}
