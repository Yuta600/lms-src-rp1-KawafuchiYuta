package jp.co.sss.lms.form;

import lombok.Data;

/**
 * 勤怠検索フォーム
 * 
 * @author 川渕佑太 - Task.57
 */
@Data
public class AttendanceSearchForm {
	// 企業名
	private String companyName;
	// コース名
	private String courseName;
	// LMSユーザーID
	private Integer lmsUserId;
	// 過去の受講生検索フラグ
	private Short pastFlg;
	// 過去の受講生検索期間
	private String pastTimeLabel;
	// 会場ID
	private Integer placeId;
	// 会場名
	private String placeName;
	// ユーザー名
	private String userName;
}
