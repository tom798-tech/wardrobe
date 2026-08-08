package com.tom.wardrobe.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tom.wardrobe.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT id, user_name AS userName, password, phone, address, role FROM t_user WHERE user_name = #{info} AND password = #{password}")
    User loginByUserName(@Param("info") String info, @Param("password") String password);

    @Select("SELECT id, user_name AS userName, password, phone, address, role FROM t_user WHERE phone = #{info} AND password = #{password}")
    User loginByPhone(@Param("info") String info, @Param("password") String password);

    @Select("SELECT id, user_name AS userName, password, phone, address, role FROM t_user WHERE user_name = #{userName}")
    User findByUserName(@Param("userName") String userName);

    @Select("SELECT id, user_name AS userName, password, phone, address, role FROM t_user WHERE phone = #{phone}")
    User findByPhone(@Param("phone") String phone);

    @Select("SELECT id, user_name AS userName, password, phone, address, role FROM t_user WHERE role = 2 AND (user_name LIKE CONCAT('%', #{queryStr}, '%') OR phone LIKE CONCAT('%', #{queryStr}, '%'))")
    List<User> getUserByParam(@Param("queryStr") String queryStr);

    @Select("SELECT id, user_name AS userName, password, phone, address, role FROM t_user WHERE role = 2")
    List<User> getAllUsers();
}