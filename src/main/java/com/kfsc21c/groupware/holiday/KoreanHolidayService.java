package com.kfsc21c.groupware.holiday;

import com.github.usingsky.calendar.KoreanLunarCalendar;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 대한민국 공휴일(고정일 + 음력 기반 + 대체공휴일)을 계산한다.
 *
 * 음력->양력 변환은 usingsky/KoreanLunarCalendar(MIT 라이선스, 한국천문연구원(KASA)
 * 기준, 1000~2050년 지원)를 그대로 갖다 쓴다 - 예전엔 이 날짜들을 프론트엔드 JS에
 * 연도별로 손으로 하드코딩해뒀었는데, 매년 사람이 새로 찾아 넣어야 하는 게 번거로워서
 * 서버에서 계산하는 걸로 바꿨다.
 *
 * 대체공휴일 판단 기준(관공서의 공휴일에 관한 규정 제3조):
 * - 설날/추석 연휴(3일): 연휴 중 "일요일"과 겹치는 날이 있을 때만 대체공휴일 발생
 *   (토요일과 겹치는 건 해당 없음 - 예: 2026년 추석은 9/26(토)까지지만 대체공휴일 없음).
 * - 삼일절/어린이날/광복절/개천절/한글날/부처님오신날: 토요일 또는 일요일과 겹치면 발생.
 * - 신정/현충일/근로자의 날: 대체공휴일 적용 안 됨.
 * 대체공휴일 날짜는 해당 연휴(또는 해당일) 다음날부터 훑어서, 토·일도 아니고 이미
 * 다른 공휴일도 아닌 첫 번째 날로 정한다. 이 로직은 2026년 실제 공휴일 달력과
 * 대조해서(3.1절->3/2, 광복절->8/17, 개천절->10/5, 부처님오신날->5/25 대체,
 * 설날·추석·어린이날·한글날은 대체 없음) 전부 일치하는 걸 확인했다.
 */
@Service
public class KoreanHolidayService {

    private final Map<Integer, List<HolidayEntry>> cache = new HashMap<>();

    public synchronized List<HolidayEntry> forYear(int year) {
        return cache.computeIfAbsent(year, this::compute);
    }

    private List<HolidayEntry> compute(int year) {
        Map<LocalDate, HolidayEntry> holidays = new LinkedHashMap<>();
        List<Runnable> substituteChecks = new ArrayList<>();

        // ---- 고정일, 대체공휴일 없음 ----
        put(holidays, LocalDate.of(year, 1, 1), "신정", true);
        put(holidays, LocalDate.of(year, 5, 1), "근로자의 날", true);
        put(holidays, LocalDate.of(year, 6, 6), "현충일", true);
        put(holidays, LocalDate.of(year, 12, 25), "기독탄신일", true);

        // ---- 고정일 + 토/일 대체공휴일 ----
        addSatSunEligible(holidays, substituteChecks, LocalDate.of(year, 3, 1), "삼일절");
        addSatSunEligible(holidays, substituteChecks, LocalDate.of(year, 5, 5), "어린이날");
        addSatSunEligible(holidays, substituteChecks, LocalDate.of(year, 8, 15), "광복절");
        addSatSunEligible(holidays, substituteChecks, LocalDate.of(year, 10, 3), "개천절");
        addSatSunEligible(holidays, substituteChecks, LocalDate.of(year, 10, 9), "한글날");
        addSatSunEligible(holidays, substituteChecks, lunarToSolar(year, 4, 8), "부처님오신날");

        // ---- 음력 3일 연휴(설날/추석) + 일요일 겹침 대체공휴일 ----
        addLunarCluster(holidays, substituteChecks, lunarToSolar(year, 1, 1), "설날");
        addLunarCluster(holidays, substituteChecks, lunarToSolar(year, 8, 15), "추석");

        substituteChecks.forEach(Runnable::run);

        List<HolidayEntry> result = new ArrayList<>(holidays.values());
        result.sort(Comparator.comparing(HolidayEntry::date));
        return result;
    }

    private void put(Map<LocalDate, HolidayEntry> holidays, LocalDate date, String name, boolean label) {
        holidays.put(date, new HolidayEntry(date, name, label));
    }

    private boolean isWeekend(LocalDate d) {
        return d.getDayOfWeek() == DayOfWeek.SATURDAY || d.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    private void addSatSunEligible(Map<LocalDate, HolidayEntry> holidays, List<Runnable> substituteChecks,
                                    LocalDate date, String name) {
        put(holidays, date, name, true);
        substituteChecks.add(() -> {
            if (isWeekend(date)) {
                assignSubstitute(holidays, date);
            }
        });
    }

    private void addLunarCluster(Map<LocalDate, HolidayEntry> holidays, List<Runnable> substituteChecks,
                                  LocalDate middle, String name) {
        LocalDate before = middle.minusDays(1);
        LocalDate after = middle.plusDays(1);
        put(holidays, before, name + " 연휴", false);
        put(holidays, middle, name, true);
        put(holidays, after, name + " 연휴", false);
        substituteChecks.add(() -> {
            boolean anySunday = before.getDayOfWeek() == DayOfWeek.SUNDAY
                    || middle.getDayOfWeek() == DayOfWeek.SUNDAY
                    || after.getDayOfWeek() == DayOfWeek.SUNDAY;
            if (anySunday) {
                assignSubstitute(holidays, after);
            }
        });
    }

    private void assignSubstitute(Map<LocalDate, HolidayEntry> holidays, LocalDate searchAfter) {
        LocalDate d = searchAfter.plusDays(1);
        while (isWeekend(d) || holidays.containsKey(d)) {
            d = d.plusDays(1);
        }
        put(holidays, d, "대체공휴일", true);
    }

    private LocalDate lunarToSolar(int lunarYear, int lunarMonth, int lunarDay) {
        KoreanLunarCalendar cal = KoreanLunarCalendar.getInstance();
        cal.setLunarDate(lunarYear, lunarMonth, lunarDay, false);
        return LocalDate.parse(cal.getSolarIsoFormat());
    }
}
