package com.talenthub.recruitment.service;

import com.talenthub.recruitment.dto.UserRegisterDto;
import com.talenthub.recruitment.entity.User;

public interface UserService {
    User registerCandidate(UserRegisterDto dto);
}
