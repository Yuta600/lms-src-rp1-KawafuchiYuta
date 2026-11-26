package jp.co.sss.lms.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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
}
