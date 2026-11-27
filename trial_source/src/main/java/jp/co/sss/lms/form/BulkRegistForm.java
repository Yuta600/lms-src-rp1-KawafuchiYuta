package jp.co.sss.lms.form;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BulkRegistForm {
	// 日次の勤怠フォームリスト
	private List<DailyAttendanceForm> dailyAttendanceFormList;
	// 会場ID
	private Integer placeId;
	// 会場名
	private String placeName;
	// 検索期間from
	@NotBlank
	private String searchPeriodFrom;
	// 検索期間to
	@NotBlank
	private String searchPeriodTo;
	// 対象インデックス
	private String targetIndex;
}
