package jp.co.sss.lms.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jp.co.sss.lms.dto.LoginUserDto;
import jp.co.sss.lms.dto.PlaceDto;
import jp.co.sss.lms.mapper.MPlaceMapper;
import jp.co.sss.lms.util.Constants;

@Service
public class PlaceService {
	@Autowired
	private LoginUserDto loginUserDto;
	@Autowired
	private MPlaceMapper mPlaceMapper;

	public PlaceDto getPlaceDto() {
		PlaceDto placeDto = mPlaceMapper.getPlaceDtoByPlaceId(loginUserDto.getPlaceId(), Constants.DB_FLG_FALSE);
		return placeDto;
	}
}
