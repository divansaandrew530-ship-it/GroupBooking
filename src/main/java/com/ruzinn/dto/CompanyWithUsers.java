package com.ruzinn.dto;

import java.util.List;

public record CompanyWithUsers(CompanyView company, List<UserView> users) {}
