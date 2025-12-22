package com.n1etzsch3.recipe.business.service;

import com.n1etzsch3.recipe.common.core.domain.Result;

import java.util.Map;

public interface AdminAuthService {

    Result<Map<String, Object>> adminLogin(String username, String password);
}
