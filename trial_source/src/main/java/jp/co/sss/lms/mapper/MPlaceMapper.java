package jp.co.sss.lms.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import jp.co.sss.lms.dto.PlaceDto;

@Mapper
public interface MPlaceMapper {
	PlaceDto getPlaceDtoByPlaceId(@Param("placeId") Integer placeId, @Param("deleteFlg") Short deleteFlg);
}
