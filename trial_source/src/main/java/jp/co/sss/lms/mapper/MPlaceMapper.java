package jp.co.sss.lms.mapper;

import java.util.Date;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import jp.co.sss.lms.dto.UserAttendanceDto;
import jp.co.sss.lms.entity.MPlace;

/**
 * 会場マスタマッパー
 * 
 * @author 川渕佑太 - Task.57
 */
@Mapper
public interface MPlaceMapper {
	/**
	 * 会場エンティティ取得
	 * 
	 * @author user
	 * @param placeId
	 * @param deleteFlg
	 * @return 会場エンティティ
	 */
	MPlace findByPlaceId(@Param("placeId") Integer placeId, @Param("hiddenFlg") Short hiddenFlg,
			@Param("deleteFlg") Short deleteFlg);
	
	List<UserAttendanceDto> getUserAttendanceDto(@Param("placeId") Integer placeId, @Param("from") Date from, @Param("to") Date to, @Param("deleteFlg") Short deleteFlg);
}
