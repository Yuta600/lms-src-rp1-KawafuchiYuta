package jp.co.sss.lms.service;

import java.text.ParseException;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import jp.co.sss.lms.dto.AttendanceManagementDto;
import jp.co.sss.lms.dto.CompanyDto;
import jp.co.sss.lms.dto.CourseDto;
import jp.co.sss.lms.dto.LoginUserDto;
import jp.co.sss.lms.dto.PlaceDto;
//import jp.co.sss.lms.dto.UserAttendanceDto;
import jp.co.sss.lms.dto.UserDetailDto;
import jp.co.sss.lms.entity.TStudentAttendance;
import jp.co.sss.lms.enums.AttendanceStatusEnum;
import jp.co.sss.lms.form.AttendanceForm;
import jp.co.sss.lms.form.AttendanceSearchForm;
//import jp.co.sss.lms.form.BulkRegistForm;
import jp.co.sss.lms.form.DailyAttendanceForm;
import jp.co.sss.lms.mapper.MLmsUserMapper;
//import jp.co.sss.lms.mapper.MPlaceMapper;
import jp.co.sss.lms.mapper.TStudentAttendanceMapper;
import jp.co.sss.lms.mapper.TUserPlaceMapper;
import jp.co.sss.lms.util.AttendanceUtil;
import jp.co.sss.lms.util.Constants;
import jp.co.sss.lms.util.DateUtil;
import jp.co.sss.lms.util.LoginUserUtil;
import jp.co.sss.lms.util.MessageUtil;
import jp.co.sss.lms.util.TrainingTime;

/**
 * 勤怠情報（受講生入力）サービス
 * 
 * @author 東京ITスクール
 */
/**
 * 
 */
@Service
public class StudentAttendanceService {

	@Autowired
	private DateUtil dateUtil;
	@Autowired
	private AttendanceUtil attendanceUtil;
	@Autowired
	private MessageUtil messageUtil;
	@Autowired
	private LoginUserUtil loginUserUtil;
	@Autowired
	private LoginUserDto loginUserDto;
	@Autowired
	private TStudentAttendanceMapper tStudentAttendanceMapper;
	@Autowired
	private CourseService courseService;
	@Autowired
	private PlaceService placeService;
	@Autowired
	private CompanyService companyService;
	@Autowired
	private MLmsUserMapper mLmsUserMapper;
	@Autowired
	private TUserPlaceMapper tUserPlaceMapper;
//	@Autowired
//	private MPlaceMapper mPlaceMapper;
//	@Autowired
//	private TrainingTime trainingTime;

	/**
	 * 勤怠一覧情報取得
	 * 
	 * @param courseId
	 * @param lmsUserId
	 * @return 勤怠管理画面用DTOリスト
	 */
	public List<AttendanceManagementDto> getAttendanceManagement(Integer courseId,
			Integer lmsUserId) {

		// 勤怠管理リストの取得
		List<AttendanceManagementDto> attendanceManagementDtoList = tStudentAttendanceMapper
				.getAttendanceManagement(courseId, lmsUserId, Constants.DB_FLG_FALSE);
		for (AttendanceManagementDto dto : attendanceManagementDtoList) {
			// 中抜け時間を設定
			if (dto.getBlankTime() != null) {
				TrainingTime blankTime = attendanceUtil.calcBlankTime(dto.getBlankTime());
				dto.setBlankTimeValue(String.valueOf(blankTime));
			}
			// 遅刻早退区分判定
			AttendanceStatusEnum statusEnum = AttendanceStatusEnum.getEnum(dto.getStatus());
			if (statusEnum != null) {
				dto.setStatusDispName(statusEnum.name);
			}
		}

		return attendanceManagementDtoList;
	}

	/**
	 * 出退勤更新前のチェック
	 * 
	 * @param attendanceType
	 * @return エラーメッセージ
	 */
	public String punchCheck(Short attendanceType) {
		Date trainingDate = attendanceUtil.getTrainingDate();
		// 権限チェック
		if (!loginUserUtil.isStudent()) {
			return messageUtil.getMessage(Constants.VALID_KEY_AUTHORIZATION);
		}
		// 研修日チェック
		if (!attendanceUtil.isWorkDay(loginUserDto.getCourseId(), trainingDate)) {
			return messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_NOTWORKDAY);
		}
		// 登録情報チェック
		TStudentAttendance tStudentAttendance = tStudentAttendanceMapper
				.findByLmsUserIdAndTrainingDate(loginUserDto.getLmsUserId(), trainingDate,
						Constants.DB_FLG_FALSE);
		switch (attendanceType) {
		case Constants.CODE_VAL_ATWORK:
			if (tStudentAttendance != null
					&& !tStudentAttendance.getTrainingStartTime().equals("")) {
				// 本日の勤怠情報は既に入力されています。直接編集してください。
				return messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_PUNCHALREADYEXISTS);
			}
			break;
		case Constants.CODE_VAL_LEAVING:
			if (tStudentAttendance == null
					|| tStudentAttendance.getTrainingStartTime().equals("")) {
				// 出勤情報がないため退勤情報を入力出来ません。
				return messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_PUNCHINEMPTY);
			}
			if (!tStudentAttendance.getTrainingEndTime().equals("")) {
				// 本日の勤怠情報は既に入力されています。直接編集してください。
				return messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_PUNCHALREADYEXISTS);
			}
			TrainingTime trainingStartTime = new TrainingTime(
					tStudentAttendance.getTrainingStartTime());
			TrainingTime trainingEndTime = new TrainingTime();
			if (trainingStartTime.compareTo(trainingEndTime) > 0) {
				// 退勤時刻は出勤時刻より後でなければいけません。
				return messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_TRAININGTIMERANGE);
			}
			break;
		}
		return null;
	}

	/**
	 * 出勤ボタン処理
	 * 
	 * @return 完了メッセージ
	 */
	public String setPunchIn() {
		// 当日日付
		Date date = new Date();
		// 本日の研修日
		Date trainingDate = attendanceUtil.getTrainingDate();
		// 現在の研修時刻
		TrainingTime trainingStartTime = new TrainingTime();
		// 遅刻早退ステータス
		AttendanceStatusEnum attendanceStatusEnum = attendanceUtil.getStatus(trainingStartTime,
				null);
		// 研修日の勤怠情報取得
		TStudentAttendance tStudentAttendance = tStudentAttendanceMapper
				.findByLmsUserIdAndTrainingDate(loginUserDto.getLmsUserId(), trainingDate,
						Constants.DB_FLG_FALSE);
		if (tStudentAttendance == null) {
			// 登録処理
			tStudentAttendance = new TStudentAttendance();
			tStudentAttendance.setLmsUserId(loginUserDto.getLmsUserId());
			tStudentAttendance.setTrainingDate(trainingDate);
			tStudentAttendance.setTrainingStartTime(trainingStartTime.toString());
			tStudentAttendance.setTrainingEndTime("");
			tStudentAttendance.setStatus(attendanceStatusEnum.code);
			tStudentAttendance.setNote("");
			tStudentAttendance.setAccountId(loginUserDto.getAccountId());
			tStudentAttendance.setDeleteFlg(Constants.DB_FLG_FALSE);
			tStudentAttendance.setFirstCreateUser(loginUserDto.getLmsUserId());
			tStudentAttendance.setFirstCreateDate(date);
			tStudentAttendance.setLastModifiedUser(loginUserDto.getLmsUserId());
			tStudentAttendance.setLastModifiedDate(date);
			tStudentAttendance.setBlankTime(null);
			tStudentAttendanceMapper.insert(tStudentAttendance);
		} else {
			// 更新処理
			tStudentAttendance.setTrainingStartTime(trainingStartTime.toString());
			tStudentAttendance.setStatus(attendanceStatusEnum.code);
			tStudentAttendance.setDeleteFlg(Constants.DB_FLG_FALSE);
			tStudentAttendance.setLastModifiedUser(loginUserDto.getLmsUserId());
			tStudentAttendance.setLastModifiedDate(date);
			tStudentAttendanceMapper.update(tStudentAttendance);
		}
		// 完了メッセージ
		return messageUtil.getMessage(Constants.PROP_KEY_ATTENDANCE_UPDATE_NOTICE);
	}

	/**
	 * 退勤ボタン処理
	 * 
	 * @return 完了メッセージ
	 */
	public String setPunchOut() {
		// 当日日付
		Date date = new Date();
		// 本日の研修日
		Date trainingDate = attendanceUtil.getTrainingDate();
		// 研修日の勤怠情報取得
		TStudentAttendance tStudentAttendance = tStudentAttendanceMapper
				.findByLmsUserIdAndTrainingDate(loginUserDto.getLmsUserId(), trainingDate,
						Constants.DB_FLG_FALSE);
		// 出退勤時刻
		TrainingTime trainingStartTime = new TrainingTime(
				tStudentAttendance.getTrainingStartTime());
		TrainingTime trainingEndTime = new TrainingTime();
		// 遅刻早退ステータス
		AttendanceStatusEnum attendanceStatusEnum = attendanceUtil.getStatus(trainingStartTime,
				trainingEndTime);
		// 更新処理
		tStudentAttendance.setTrainingEndTime(trainingEndTime.toString());
		tStudentAttendance.setStatus(attendanceStatusEnum.code);
		tStudentAttendance.setDeleteFlg(Constants.DB_FLG_FALSE);
		tStudentAttendance.setLastModifiedUser(loginUserDto.getLmsUserId());
		tStudentAttendance.setLastModifiedDate(date);
		tStudentAttendanceMapper.update(tStudentAttendance);
		// 完了メッセージ
		return messageUtil.getMessage(Constants.PROP_KEY_ATTENDANCE_UPDATE_NOTICE);
	}

	/**
	 * 勤怠フォームへ設定
	 * 
	 * @param attendanceManagementDtoList
	 * @return 勤怠編集フォーム
	 */
	public AttendanceForm setAttendanceForm(
			List<AttendanceManagementDto> attendanceManagementDtoList) {

		AttendanceForm attendanceForm = new AttendanceForm();
		attendanceForm.setAttendanceList(new ArrayList<DailyAttendanceForm>());
		attendanceForm.setLmsUserId(loginUserDto.getLmsUserId());
		attendanceForm.setUserName(loginUserDto.getUserName());
		attendanceForm.setLeaveFlg(loginUserDto.getLeaveFlg());
		attendanceForm.setBlankTimes(attendanceUtil.setBlankTime());
		// 川渕佑太 Task.26
		attendanceForm.setHourMap(attendanceUtil.setHourMap());
		attendanceForm.setMinuteMap(attendanceUtil.setMinuteMap());
		

		// 途中退校している場合のみ設定
		if (loginUserDto.getLeaveDate() != null) {
			attendanceForm
					.setLeaveDate(dateUtil.dateToString(loginUserDto.getLeaveDate(), "yyyy-MM-dd"));
			attendanceForm.setDispLeaveDate(
					dateUtil.dateToString(loginUserDto.getLeaveDate(), "yyyy年M月d日"));
		}

		// 勤怠管理リストの件数分、日次の勤怠フォームに移し替え
		for (AttendanceManagementDto attendanceManagementDto : attendanceManagementDtoList) {
			DailyAttendanceForm dailyAttendanceForm = new DailyAttendanceForm();
			dailyAttendanceForm
					.setStudentAttendanceId(attendanceManagementDto.getStudentAttendanceId());
			dailyAttendanceForm
					.setTrainingDate(dateUtil.toString(attendanceManagementDto.getTrainingDate()));
			
			// 川渕佑太 Task.26
			// null,空文字チェック
			if (attendanceManagementDto.getTrainingStartTime() != null && !"".equals(attendanceManagementDto.getTrainingStartTime())) {
				// 勤務開始時刻を時、分に分けてフォームに設定
				dailyAttendanceForm
						.setTrainingStartTimeHour(attendanceUtil.getHour(attendanceManagementDto.getTrainingStartTime()));
				dailyAttendanceForm
						.setTrainingStartTimeMinute(attendanceUtil.getMinute(attendanceManagementDto.getTrainingStartTime()));
			}
			// null,空文字チェック
			if(attendanceManagementDto.getTrainingEndTime() != null && !"".equals(attendanceManagementDto.getTrainingEndTime())) {
				// 勤務終了時刻を時、分に分けてフォームに設定。
				dailyAttendanceForm
						.setTrainingEndTimeHour(attendanceUtil.getHour(attendanceManagementDto.getTrainingEndTime()));
				dailyAttendanceForm
						.setTrainingEndTimeMinute(attendanceUtil.getMinute(attendanceManagementDto.getTrainingEndTime()));
			}
			
			if (attendanceManagementDto.getBlankTime() != null) {
				dailyAttendanceForm.setBlankTime(attendanceManagementDto.getBlankTime());
				dailyAttendanceForm.setBlankTimeValue(String.valueOf(
						attendanceUtil.calcBlankTime(attendanceManagementDto.getBlankTime())));
			}
			dailyAttendanceForm.setStatus(String.valueOf(attendanceManagementDto.getStatus()));
			dailyAttendanceForm.setNote(attendanceManagementDto.getNote());
			dailyAttendanceForm.setSectionName(attendanceManagementDto.getSectionName());
			dailyAttendanceForm.setIsToday(attendanceManagementDto.getIsToday());
			dailyAttendanceForm.setDispTrainingDate(dateUtil
					.dateToString(attendanceManagementDto.getTrainingDate(), "yyyy年M月d日(E)"));
			dailyAttendanceForm.setStatusDispName(attendanceManagementDto.getStatusDispName());

			attendanceForm.getAttendanceList().add(dailyAttendanceForm);
		}

		return attendanceForm;
	}

	/**
	 * 勤怠登録・更新処理
	 * 
	 * @param attendanceForm
	 * @return 完了メッセージ
	 * @throws ParseException
	 */
	public String update(AttendanceForm attendanceForm) throws ParseException {

		Integer lmsUserId = loginUserUtil.isStudent() ? loginUserDto.getLmsUserId()
				: attendanceForm.getLmsUserId();

		// 現在の勤怠情報（受講生入力）リストを取得
		List<TStudentAttendance> tStudentAttendanceList = tStudentAttendanceMapper
				.findByLmsUserId(lmsUserId, Constants.DB_FLG_FALSE);

		// 入力された情報を更新用のエンティティに移し替え
		Date date = new Date();
		for (DailyAttendanceForm dailyAttendanceForm : attendanceForm.getAttendanceList()) {

			// 更新用エンティティ作成
			TStudentAttendance tStudentAttendance = new TStudentAttendance();
			// 日次勤怠フォームから更新用のエンティティにコピー
			BeanUtils.copyProperties(dailyAttendanceForm, tStudentAttendance);
			// 研修日付
			tStudentAttendance
					.setTrainingDate(dateUtil.parse(dailyAttendanceForm.getTrainingDate()));
			// 現在の勤怠情報リストのうち、研修日が同じものを更新用エンティティで上書き
			for (TStudentAttendance entity : tStudentAttendanceList) {
				if (entity.getTrainingDate().equals(tStudentAttendance.getTrainingDate())) {
					tStudentAttendance = entity;
					break;
				}
			}
			tStudentAttendance.setLmsUserId(lmsUserId);
			tStudentAttendance.setAccountId(loginUserDto.getAccountId());
			
			// 出勤時刻整形
			TrainingTime trainingStartTime = null;
			trainingStartTime = new TrainingTime(dailyAttendanceForm.getTrainingStartTimeRaw());
			tStudentAttendance.setTrainingStartTime(trainingStartTime.getFormattedString());
			// 退勤時刻整形
			TrainingTime trainingEndTime = null;
			trainingEndTime = new TrainingTime(dailyAttendanceForm.getTrainingEndTimeRaw());
			tStudentAttendance.setTrainingEndTime(trainingEndTime.getFormattedString());
				
			// 中抜け時間
			tStudentAttendance.setBlankTime(dailyAttendanceForm.getBlankTime());
			// 遅刻早退ステータス
			if ((trainingStartTime != null || trainingEndTime != null)
					&& !dailyAttendanceForm.getStatusDispName().equals("欠席")) {
				AttendanceStatusEnum attendanceStatusEnum = attendanceUtil
						.getStatus(trainingStartTime, trainingEndTime);
				tStudentAttendance.setStatus(attendanceStatusEnum.code);
			}
		
			// 備考
			tStudentAttendance.setNote(dailyAttendanceForm.getNote());
			// 更新者と更新日時
			tStudentAttendance.setLastModifiedUser(loginUserDto.getLmsUserId());
			tStudentAttendance.setLastModifiedDate(date);
			// 削除フラグ
			tStudentAttendance.setDeleteFlg(Constants.DB_FLG_FALSE);
			// 登録用Listへ追加
			tStudentAttendanceList.add(tStudentAttendance);
		}
		// 登録・更新処理
		for (TStudentAttendance tStudentAttendance : tStudentAttendanceList) {
			if (tStudentAttendance.getStudentAttendanceId() == null) {
				tStudentAttendance.setFirstCreateUser(loginUserDto.getLmsUserId());
				tStudentAttendance.setFirstCreateDate(date);
				tStudentAttendanceMapper.insert(tStudentAttendance);
			} else {
				tStudentAttendanceMapper.update(tStudentAttendance);
			}
		}
		// 完了メッセージ
		return messageUtil.getMessage(Constants.PROP_KEY_ATTENDANCE_UPDATE_NOTICE);
	}
	
	
	/**
	 * 過去日未入力チェック処理
	 * 
	 * @author 川渕佑太  – Task.25
	 * @return 過去日未入力有無判定結果（true:有、false:無）
	 * @throws ParseException
	 */
	public Boolean notEnterCheck() throws ParseException {
		
		// 過去日の勤怠が未入力の件数を取得
		int notEnterCount = tStudentAttendanceMapper.notEnterCount(loginUserDto.getLmsUserId(),
				Constants.DB_FLG_FALSE, attendanceUtil.getTrainingDate());

		// 未入力が1件以上存在する場合、trueを返却する。
		return notEnterCount > 0;
	}
	
	/**
	 * 入力された出退勤の時、分をhh:mm形式に変換し、attendanceFormに設定する。
	 * 
	 * @author 川渕佑太  – Task.26
	 * @param attendanceForm
	 */
	public void formatConversion(AttendanceForm attendanceForm) {
		
		for (DailyAttendanceForm dailyAttendanceForm : attendanceForm.getAttendanceList()) {
			Integer trainingStartTimeHour = dailyAttendanceForm.getTrainingStartTimeHour();
			Integer trainingStartTimeMinute = dailyAttendanceForm.getTrainingStartTimeMinute();
			// 出勤時刻を結合
			if (trainingStartTimeHour != null && trainingStartTimeMinute != null) {
				dailyAttendanceForm.setTrainingStartTimeRaw(String.format(Constants.TIME_FORMAT, trainingStartTimeHour)
						+ ":" + String.format(Constants.TIME_FORMAT, trainingStartTimeMinute));
			}
			
			Integer trainingEndTimeHour = dailyAttendanceForm.getTrainingEndTimeHour();
			Integer trainingEndTimeMinute = dailyAttendanceForm.getTrainingEndTimeMinute();
			// 退勤時刻を結合
			if (trainingEndTimeHour != null && trainingEndTimeMinute != null) {
				dailyAttendanceForm.setTrainingEndTimeRaw(String.format(Constants.TIME_FORMAT, trainingEndTimeHour) + ":" + String.format(Constants.TIME_FORMAT, trainingEndTimeMinute));
			}
		}
	}
	
	
	/**
	 * 更新時バリデーション
	 * 
	 * @author 川渕佑太 – Task.27
	 * @param attendanceForm
	 * @param result
	 */
	public void updateInputCheck(AttendanceForm attendanceForm, BindingResult result) {
		List<DailyAttendanceForm> attendance = attendanceForm.getAttendanceList();
		for (int i = 0; i < attendance.size(); i++) {
			// 備考
			String note = attendance.get(i).getNote();
			// 出勤時間(時)
			Integer trainingStartTimeHour = attendance.get(i).getTrainingStartTimeHour();
			// 出勤時間(分)
			Integer trainingStartTimeMinute = attendance.get(i).getTrainingStartTimeMinute();
			// 退勤時間(時)
			Integer trainingEndTimeHour = attendance.get(i).getTrainingEndTimeHour();
			// 退勤時間(分)
			Integer trainingEndTimeMinute = attendance.get(i).getTrainingEndTimeMinute();
			// 出勤時間(HH:mm)
			String trainingStartTime = attendance.get(i).getTrainingStartTimeRaw();
			// 退勤時間(HH:mm)
			String trainingEndTime = attendance.get(i).getTrainingEndTimeRaw();
			// 中抜け時間
			Integer blankTime = attendance.get(i).getBlankTime();

			// 備考フィールドの文字数 > 100 の場合
			if ("" != note && note.length() > 100) {
				result.addError(new FieldError(result.getObjectName(), "attendanceList[" + i + "].note",
						messageUtil.getMessage(Constants.VALID_KEY_MAXLENGTH, new String[] { "備考", "100" })));
			}

			// 出退勤時間関連バリデーション
			// 出勤時間（時）、出勤時間（分）の一方が入力有り、もう一方が入力なしの場合
			if ((trainingStartTimeHour != null) ^ (trainingStartTimeMinute != null)) {
				// 
				String nullField = trainingStartTimeHour == null ? "trainingStartTimeHour" : "trainingStartTimeMinute";
				result.addError(new FieldError(
						result.getObjectName(), 
						"attendanceList[" + i + "]." + nullField,
						messageUtil.getMessage(Constants.INPUT_INVALID, new String[] { "出勤時間" })));
			}

			// 退勤時間（時）、退勤時間（分）の一方が入力有り、もう一方が入力なしの場合
			if ((trainingEndTimeHour != null ^ trainingEndTimeMinute != null)) {
				String nullField = trainingEndTimeHour == null ? "trainingEndTimeHour" : "trainingEndTimeMinute";
				result.addError(new FieldError(
						result.getObjectName(), 
						"attendanceList[" + i + "]." + nullField,
						messageUtil.getMessage(Constants.INPUT_INVALID, new String[] { "退勤時間" })));
			}

			// 出勤時間に入力なし、退勤時間に入力ありの場合、
			if (trainingStartTime == null && trainingEndTime != null) {
				result.addError(new FieldError(
						result.getObjectName(), 
						"attendanceList[" + i + "].trainingStartTimeHour",
						messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_PUNCHINEMPTY)));
				result.addError(new FieldError(
						result.getObjectName(), 
						"attendanceList[" + i + "].trainingStartTimeMinute",
						messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_PUNCHINEMPTY)));
			}

			if (result.hasErrors()) {
				continue;
			}

			// 出勤時間および退勤時間の両方に入力があり、出勤時間 > 退勤時間の場合
			if ((trainingStartTime != null && trainingEndTime != null)
					&& trainingStartTime.compareTo(trainingEndTime) > 0) {
				result.addError(new FieldError(
						result.getObjectName(), 
						"attendanceList[" + i + "].trainingEndTimeHour",
						messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_TRAININGTIMERANGE,
								new String[] { String.valueOf(i) })));
				result.addError(new FieldError(
						result.getObjectName(), 
						"attendanceList[" + i + "].trainingEndTimeMinute",
						messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_TRAININGTIMERANGE,
								new String[] { String.valueOf(i) })));
			}

			// 中抜け時間関連バリデーション
			// 中抜け時間に入力があり、出勤時間および退勤時間が未入力の場合
			if (blankTime != null) {
				if (trainingStartTime == null) {
					result.addError(new FieldError(
							result.getObjectName(), 
							"attendanceList[" + i + "].trainingStartTimeHour",
							messageUtil.getMessage(Constants.INPUT_INVALID, new String[] { "出勤時間" })));
					result.addError(new FieldError(
							result.getObjectName(), 
							"attendanceList[" + i + "].trainingStartTimeMinute",
							messageUtil.getMessage(Constants.INPUT_INVALID, new String[] { "出勤時間" })));
				}
				if (trainingEndTime == null) {
					result.addError(new FieldError(
							result.getObjectName(), 
							"attendanceList[" + i + "].trainingEndTimeHour",
							messageUtil.getMessage(Constants.INPUT_INVALID, new String[] { "退勤時間" })));
					result.addError(new FieldError(
							result.getObjectName(), 
							"attendanceList[" + i + "].trainingEndTimeMinute",
							messageUtil.getMessage(Constants.INPUT_INVALID, new String[] { "退勤時間" })));
				}
			}

			// 中抜け時間が勤務時間（出勤時間～退勤時間までの時間）を超える場合
			if (blankTime != null && trainingStartTime != null && trainingEndTime != null) {
				Duration duration = Duration.between(LocalTime.parse(trainingStartTime),
						LocalTime.parse(trainingEndTime));
				if (blankTime > duration.toMinutes()) {
					result.addError(new FieldError(
							result.getObjectName(), 
							"attendanceList[" + i + "].blankTime",
							messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_BLANKTIMEERROR)));
				}
			}
		}
	}
	
	/**
	 * 勤怠検索フォーム情報取得
	 * 
	 * @author 川渕佑太 - Task.57
	 * @param model
	 */
	public void getAttendanceSearchForm(Model model) {
		// 講師権限の場合
		if(loginUserUtil.isTeacher()) {
			// コースリストを取得、フォームに設定
			List<CourseDto> courseDtoList = courseService.getCourseDtoList();
			model.addAttribute("courseList", courseDtoList);
			// ログインユーザーの担当会場を取得、フォームに設定
			PlaceDto placeDto = placeService.getPlaceDto();
			model.addAttribute("place", placeDto);
			// 企業リストを取得、フォームに設定
			List<CompanyDto> companyDtoList = companyService.getCompanyDtoList();
			model.addAttribute("companyList", companyDtoList);
		}
	}
	
	/**
	 * 勤怠詳細DTOリスト取得
	 * 
	 * @author 川渕佑太 - Task.57
	 * @param attendanceSearchForm
	 * @return 勤怠詳細DTOリスト
	 * @throws ParseException
	 */
	public List<UserDetailDto> getAttendanceDetailDtoList(AttendanceSearchForm attendanceSearchForm) throws ParseException {
		
		List<Integer> placeIdList = null;
		// 講師権限の場合
		if (loginUserUtil.isTeacher()) {
			// ログインユーザーの担当会場を取得
			placeIdList = tUserPlaceMapper.getPlaceId(loginUserDto.getLmsUserId(), Constants.DB_FLG_FALSE);
		}
		
		String companyName;
		// 企業担当者権限の場合
		if (loginUserUtil.isCompany()) {
			// ログインユーザーの企業名を取得
			companyName = loginUserDto.getCompanyName();
		} else {
			// 検索フォームに入力された企業名を取得
			companyName = attendanceSearchForm.getCompanyName();
		}
		// フォームからコース名、ユーザー名を取得
		String courseName = attendanceSearchForm.getCourseName();
		String userName = attendanceSearchForm.getUserName();
		
		// 検索条件に一致するユーザーの詳細情報を取得
		List<UserDetailDto> userDetailList = mLmsUserMapper.getUserDetailForSearch(courseName, companyName, userName,
				placeIdList, Constants.CODE_VAL_ROLL_STUDENT, new Date(), Constants.DB_FLG_FALSE);
		
		return userDetailList;
	}
	
//	実装途中のためコメントアウト
//	/**
//	 * @param bulkRegistForm
//	 */
//	public void setBulkRegistForm(BulkRegistForm bulkRegistForm) {
//		PlaceDto placeDto = placeService.getPlaceDto();
//		bulkRegistForm.setPlaceName(placeDto.getPlaceName() + placeDto.getPlaceNote());
//	}
//	
//	public void searchInputCheck(BulkRegistForm bulkRegistForm, BindingResult result) throws ParseException{
//		if (result.hasErrors()) return;
//		Date searchPeriodFrom = dateUtil.parse(bulkRegistForm.getSearchPeriodFrom());
//		Date searchPeriodTo = dateUtil.parse(bulkRegistForm.getSearchPeriodTo());
//		if (searchPeriodTo.compareTo(attendanceUtil.getTrainingDate()) < 0) {
//			result.addError(new FieldError(result.getObjectName(), "searchPeriodFrom",
//					messageUtil.getMessage(Constants.VALID_KEY_SEARCHTORANGEERROR,
//							new String[] { messageUtil.getMessage("searchPeriodTo") })));
//			return;
//		}
//		if (searchPeriodFrom.compareTo(searchPeriodTo) < 0) {
//			result.addError(new FieldError(result.getObjectName(), "searchPeriodFrom",
//					messageUtil.getMessage(Constants.VALID_KEY_SEARCHPERIODCOMPAREERROR,
//							new String[] { messageUtil.getMessage("searchPeriodTo"), messageUtil.getMessage("searchPeriodFrom")  })));
//			return;
//		}
//		if (dateUtil.differenceDays(searchPeriodFrom, searchPeriodTo) > 30) {
//			result.addError(new FieldError(result.getObjectName(), "searchPeriodFrom",
//					messageUtil.getMessage(Constants.VALID_KEY_SEARCHSETTINGOVER,
//							new String[] { messageUtil.getMessage("searchPeriodTo"), "30" })));
//			return;
//		}
//		
//		List<UserAttendanceDto> userAttendanceDtoList = mPlaceMapper.getUserAttendanceDto(bulkRegistForm.getPlaceId(),
//				searchPeriodFrom, searchPeriodTo, Constants.DB_FLG_FALSE);
//		List<DailyAttendanceForm> dailyAttendanceFormList = bulkRegistForm.getDailyAttendanceFormList();
//		for(int i = 0; i < userAttendanceDtoList.size(); i++) {
//			UserAttendanceDto userAttendanceDto = userAttendanceDtoList.get(i);
//			DailyAttendanceForm dailyAttendanceForm = dailyAttendanceFormList.get(i);
//			
//			dailyAttendanceForm.setDispTrainingDate(userAttendanceDto.getTrainingDate().toString());
//			dailyAttendanceForm.setTrainingStartTimeRaw(null == userAttendanceDto.getTrainingStartTime() ? "未入力" : userAttendanceDto.getTrainingStartTime());
//			dailyAttendanceForm.setTrainingEndTimeRaw(null == userAttendanceDto.getTrainingEndTime() ? "未入力" : userAttendanceDto.getTrainingEndTime());
//			TrainingTime workStartTime = new TrainingTime(9, 0);
//			TrainingTime trainingStartTime = new TrainingTime(userAttendanceDto.getTrainingStartTime()).roundUp();
//			TrainingTime trainingStartTimeRounded = new TrainingTime().max(workStartTime, trainingStartTime);
//			dailyAttendanceForm.setTrainingStartTimeRounded(trainingStartTimeRounded.toString());
//			TrainingTime workEndTime = new TrainingTime(18, 0);
//			TrainingTime trainingEndTime = new TrainingTime(userAttendanceDto.getTrainingEndTime()).roundDown();
//			TrainingTime trainingEndTimeRounded = new TrainingTime().min(workEndTime, trainingEndTime);
//			dailyAttendanceForm.setTrainingEndTimeRounded(trainingEndTimeRounded.toString());
//			dailyAttendanceForm.setBlankTimeValue(userAttendanceDto.getBlankTime().toString());
//			dailyAttendanceForm.setStatusDispName(userAttendanceDto.getStatus());
//			
//			
//		}
//	}
//	
//	public String bulkUpdate(BulkRegistForm bulkRegistForm) throws ParseException {
//		for (DailyAttendanceForm dailyAttendanceForm : bulkRegistForm.getDailyAttendanceFormList()) {
//			
//		}
//		return "";
//	}
}
