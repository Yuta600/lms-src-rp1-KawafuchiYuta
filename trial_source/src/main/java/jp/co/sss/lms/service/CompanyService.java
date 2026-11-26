package jp.co.sss.lms.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jp.co.sss.lms.dto.CompanyDto;
import jp.co.sss.lms.mapper.MCompanyMapper;
import jp.co.sss.lms.util.Constants;

/**
 * 企業情報サービス
 * 
 * @author 川渕佑太 - Task.57
 */
@Service
public class CompanyService {
	
	@Autowired
	private MCompanyMapper mCompanyMapper;
	
	/**
	 * 企業リスト取得
	 * 
	 * @author 川渕佑太 - Task.57
	 * @return 企業DTOリスト
	 */
	public List<CompanyDto> getCompanyDtoList() {
		return mCompanyMapper.getCompanyDto(Constants.DB_FLG_FALSE);
	}
}
