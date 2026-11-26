package jp.co.sss.lms.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jp.co.sss.lms.dto.LoginUserDto;
import jp.co.sss.lms.dto.PlaceDto;
import jp.co.sss.lms.entity.MPlace;
import jp.co.sss.lms.mapper.MPlaceMapper;
import jp.co.sss.lms.util.Constants;

/**
 * 会場情報サービス
 * 
 * @author 川渕佑太 - Task.57
 */
@Service
public class PlaceService {
	@Autowired
	private LoginUserDto loginUserDto;
	@Autowired
	private MPlaceMapper mPlaceMapper;

	/**
	 * 会場情報DTO取得
	 * 
	 * @author 川渕佑太 - Task57
	 * @return 会場情報DTO
	 */
	public PlaceDto getPlaceDto() {
		MPlace place = mPlaceMapper.findByPlaceId(loginUserDto.getPlaceId(),Constants.DB_HIDDEN_FLG_FALSE, Constants.DB_FLG_FALSE);
		PlaceDto placeDto = new PlaceDto();
		
		if (place != null) {
			// placeエンティティをplaceDtoに詰め替え
			placeDto.setPlaceId(place.getPlaceId());
			placeDto.setPlaceName(place.getPlaceName());
			placeDto.setPlaceDescription(place.getPlaceDescription());
			placeDto.setPlaceNote(place.getPlaceNote());
			placeDto.setSupportAvailable(place.getSupportAvailable());
			placeDto.setSeatingCapacity(place.getSeatingCapacity());
			placeDto.setHiddenFlg(place.getHiddenFlg());
		}
		
		return placeDto;
	}
	
	
}
