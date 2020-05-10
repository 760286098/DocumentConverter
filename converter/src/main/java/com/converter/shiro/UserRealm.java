package com.converter.shiro;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.beans.factory.annotation.Value;

/**
 * 自定义Realm
 *
 * @author Evan
 */
public class UserRealm extends AuthorizingRealm {
    @Value("${customize.profile.username}")
    private String username;

    @Value("${customize.profile.password}")
    private String password;

    /**
     * 授权
     */
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(final PrincipalCollection principals) {
        SimpleAuthorizationInfo authorizationInfo = new SimpleAuthorizationInfo();
        authorizationInfo.addRole("admin");
        authorizationInfo.addStringPermission("all");
        return authorizationInfo;
    }

    /**
     * 认证, 验证当前登录的Subject, 执行Subject.login()时, 执行此方法
     */
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(final AuthenticationToken authcToken) {
        String username = (String) authcToken.getPrincipal();
        String password = new String((char[]) authcToken.getCredentials());

        if (!match(username, password)) {
            throw new AuthenticationException("用户名或密码错误");
        }

        //交给AuthenticatingRealm使用CredentialsMatcher进行密码匹配
        return new SimpleAuthenticationInfo(
                username,
                password,
                this.getName()
        );
    }

    /**
     * 判断用户名密码是否匹配
     *
     * @param username 用户名
     * @param password 密码
     * @return true代表匹配
     */
    private boolean match(final String username,
                          final String password) {
        return this.username.equals(username) && this.password.equals(password);
    }
}
