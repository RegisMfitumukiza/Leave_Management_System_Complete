package com.daking.leave.security;

// import com.daking.leave.client.UserInfoClient;
// import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UserSecurity {
    // @Autowired
    // private UserInfoClient userInfoClient;

    public boolean isSelf(Long userId, String username) {
        // username is actually userId as string (see JwtAuthenticationFilter)
        if (userId == null || username == null)
            return false;
        try {
            return userId.equals(Long.valueOf(username));
        } catch (NumberFormatException e) {
            return false;
        }
    }
}