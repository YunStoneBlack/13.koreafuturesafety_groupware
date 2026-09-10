package com.kfsc21c.groupware.holiday;

import java.time.LocalDate;

/**
 * label=false는 "~연휴"(예: 추석 연휴)라서 날짜 칸에 이름 글자는 안 띄우고
 * 빨간 숫자색만 적용한다는 뜻 - static/js/calendar-widget.js의 KR_HOLIDAYS와
 * 같은 구조를 서버에서 계산해서 그대로 내려준다.
 */
public record HolidayEntry(LocalDate date, String name, boolean label) {
}
