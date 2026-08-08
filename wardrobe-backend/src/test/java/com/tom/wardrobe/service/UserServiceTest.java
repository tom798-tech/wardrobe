package com.tom.wardrobe.service;

import com.tom.wardrobe.entity.User;
import com.tom.wardrobe.mapper.UserMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UserService 单元测试。
 */
@SpringBootTest
class UserServiceTest {

    private static final String TEST_USER_NAME = "test_user";
    private static final String TEST_PHONE = "13800138000";
    private static final String NEW_USER_NAME = "new_user";
    private static final String NEW_PHONE = "13900139000";

    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;

    @BeforeEach
    void setUp() {
        cleanupTestUsers();

        testUser = new User();
        testUser.setUserName(TEST_USER_NAME);
        testUser.setPassword(passwordEncoder.encode("Test@123456"));
        testUser.setPhone(TEST_PHONE);
        testUser.setRole(0);
        userMapper.insert(testUser);
    }

    @AfterEach
    void tearDown() {
        cleanupTestUsers();
    }

    @Test
    @DisplayName("测试用户登录（成功）")
    void testLoginSuccess() {
        User result = userService.login(TEST_USER_NAME, "Test@123456");
        assertNotNull(result);
        assertEquals(TEST_USER_NAME, result.getUserName());
    }

    @Test
    @DisplayName("测试用户登录（密码错误）")
    void testLoginWrongPassword() {
        User result = userService.login(TEST_USER_NAME, "WrongPassword");
        assertNull(result);
    }

    @Test
    @DisplayName("测试用户登录（用户不存在）")
    void testLoginUserNotFound() {
        User result = userService.login("non_exist_user", "Test@123456");
        assertNull(result);
    }

    @Test
    @DisplayName("测试用户登录（手机号登录）")
    void testLoginByPhone() {
        User result = userService.login(TEST_PHONE, "Test@123456");
        assertNotNull(result);
        assertEquals(TEST_USER_NAME, result.getUserName());
    }

    @Test
    @DisplayName("测试用户注册（成功）")
    void testRegisterSuccess() {
        User user = new User();
        user.setUserName(NEW_USER_NAME);
        user.setPassword("New@123456");
        user.setPhone(NEW_PHONE);

        String result = userService.register(user);
        assertEquals("注册成功，请登录！", result);

        User createdUser = userMapper.findByUserName(NEW_USER_NAME);
        assertNotNull(createdUser);
        assertTrue(passwordEncoder.matches("New@123456", createdUser.getPassword()));
    }

    @Test
    @DisplayName("测试用户注册（用户名已存在）")
    void testRegisterUserNameExists() {
        User user = new User();
        user.setUserName(TEST_USER_NAME);
        user.setPassword("Test@123456");
        user.setPhone(NEW_PHONE);

        String result = userService.register(user);
        assertEquals("用户名已存在，请更换！", result);
    }

    @Test
    @DisplayName("测试删除用户（成功）")
    void testDeleteUserSuccess() {
        String result = userService.delUser(testUser.getId());
        assertEquals("删除成功！", result);

        User deletedUser = userMapper.selectById(testUser.getId());
        assertNull(deletedUser);
    }

    @Test
    @DisplayName("测试删除用户（用户不存在）")
    void testDeleteUserNotFound() {
        String result = userService.delUser(99999);
        assertEquals("删除失败！", result);
    }

    @Test
    @DisplayName("测试获取当前用户")
    void testGetCurrentUser() {
        User user = userService.getCurrentUser(testUser.getId());
        assertNotNull(user);
        assertEquals(TEST_USER_NAME, user.getUserName());
    }

    @Test
    @DisplayName("测试获取当前用户（用户不存在）")
    void testGetCurrentUserNotFound() {
        User user = userService.getCurrentUser(99999);
        assertNull(user);
    }

    @Test
    @DisplayName("测试密码加密")
    void testPasswordEncoding() {
        String rawPassword = "Test@123456";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        assertNotEquals(rawPassword, encodedPassword);
        assertTrue(passwordEncoder.matches(rawPassword, encodedPassword));
        assertFalse(passwordEncoder.matches("WrongPassword", encodedPassword));
    }

    private void cleanupTestUsers() {
        deleteByUserName(TEST_USER_NAME);
        deleteByUserName(NEW_USER_NAME);
        deleteByPhone(TEST_PHONE);
        deleteByPhone(NEW_PHONE);
    }

    private void deleteByUserName(String userName) {
        User existing = userMapper.findByUserName(userName);
        if (existing != null) {
            userMapper.deleteById(existing.getId());
        }
    }

    private void deleteByPhone(String phone) {
        User existing = userMapper.findByPhone(phone);
        if (existing != null) {
            userMapper.deleteById(existing.getId());
        }
    }
}
