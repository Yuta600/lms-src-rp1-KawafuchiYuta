package jp.co.sss.lms.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jp.co.sss.lms.dto.CompanyDto;
import jp.co.sss.lms.mapper.MCompanyMapper;
import jp.co.sss.lms.util.Constants;

@Service
public class CompanyService {
	
	@Autowired
	private MCompanyMapper mCompanyMapper;
	
	public List<CompanyDto> getCompanyDtoList() {
		return mCompanyMapper.getCompanyDto(Constants.DB_FLG_FALSE);
	}
}
