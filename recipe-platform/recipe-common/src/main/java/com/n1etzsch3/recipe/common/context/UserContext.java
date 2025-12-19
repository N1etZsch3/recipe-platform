package com.n1etzsch3.recipe.common.context;

import com.n1etzsch3.recipe.common.core.domain.LoginUser;

public class UserContext {
    private static final ThreadLocal<LoginUser> USER_HOLDER = new ThreadLocal<>();

    public static void set(LoginUser user) {
        USER_HOLDER.set(user);
    }

    public static LoginUser get() {
        return USER_HOLDER.get();
    }

    public static Long getUserId() {
        LoginUser user = get();
        return user != null ? user.getId() : null;
    }

    public static void remove() {
        USER_HOLDER.remove();
    }
}
