package com.bugbycode.mapper.host;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.bugbycode.module.host.HostModule;

public interface HostMapper {

	public List<HostModule> query(@Param("keyword") String keyword,@Param("offset") int offset,@Param("limit") int limit);
	
	public int count(@Param("keyword") String keyword);
	
	public int insert(HostModule host);
	
	public int updateForwardById(@Param("id") int id,@Param("forward") int forward);
	
	public HostModule queryByHost(String host);
}
