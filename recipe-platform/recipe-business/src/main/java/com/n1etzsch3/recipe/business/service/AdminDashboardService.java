package com.n1etzsch3.recipe.business.service;

import com.n1etzsch3.recipe.business.domain.dto.DashboardDTO;
import com.n1etzsch3.recipe.common.core.domain.Result;

public interface AdminDashboardService {

    Result<DashboardDTO> getDashboard();
}
