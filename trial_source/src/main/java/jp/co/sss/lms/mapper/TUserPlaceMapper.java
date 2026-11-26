package jp.co.sss.lms.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * ユーザー・会場紐づけマッパー
 * 
 * @author 川渕佑太 - Task.57
 */
@Mapper
public interface TUserPlaceMapper {
	
	/**
	 * 会場ID取得
	 * 
	 * @param lmsUserId
	 * @param deleteFlg
	 * @return 会場IDリスト
	 */
	List<Integer> getPlaceId(@Param("lmsUserId") Integer lmsUserId,
			@Param("deleteFlg") Short deleteFlg);
}
