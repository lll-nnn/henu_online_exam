package com.lee.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lee.mapper.IpBindMapper;
import com.lee.pojo.IpBind;
import com.lee.service.IpBindService;
import org.springframework.stereotype.Service;

@Service
public class IpBindServiceImpl extends ServiceImpl<IpBindMapper, IpBind> implements IpBindService {
}
