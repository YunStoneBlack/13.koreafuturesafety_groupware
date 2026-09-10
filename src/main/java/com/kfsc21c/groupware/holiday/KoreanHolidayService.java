package com.kfsc21c.groupware.holiday;

import com.github.usingsky.calendar.KoreanLunarCalendar;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
 * - 서로 다른 공휴일끼리 같은 날짜에 겹쳐도 발생한다(신정/현충일/근로자의 날 제외) -
 *   실제로 2025년 5/5(월) 어린이날과 부처님오신날이 겹쳐서 5/6(화)이 대체공휴일로
 *   지정됐는데, 이 케이스로 검증했다.
 * - 신정/현충일/근로자의 날: 대체공휴일 적용 안 됨.
 * 대체공휴일 날짜는 해당 연휴(또는 해당일) 다음날부터 훑어서, 토·일도 아니고 이미
 * 다른 공휴일도 아닌 첫 번째 날로 정한다. 이 로직은 2026년 실제 공휴일(3.1절->3/2,
 * 광복절->8/17, 개천절->10/5, 부처님오신날->5/25 대체, 설날·추석·어린이날·한글날은
 * 대체 없음)과 2025년 어린이날/부처님오신날 겹침 케이스(->5/6 대체) 전부와 대조해서
 * 일치함을 확인했다.
 */
@Service
public class KoreanHolidayService {

    private final Map<Integer, List<HolidayEntry>> cache = new HashMap<>();

    public synchronized List<HolidayEntry> forYear(int year) {
        return cache.computeIfAbsent(year, this::compute);
    }

    private List<HolidayEntry> compute(int year) {
        Map<LocalDate, HolidayEntry> holidays = new LinkedHashMap<>();
        Map<LocalDate, Integer> hitCount = new HashMap<>();
        Set<LocalDate> substitutedSearchPoints = new HashSet<>();
        List<Runnable> substituteChecks = new ArrayList<>();

        // ---- 고정일, 대체공휴일 없음 ----
        put(holidays, hitCount, LocalDate.of(year, 1, 1), "신정", true);
        put(holidays, hitCount, LocalDate.of(year, 5, 1), "근로자의 날", true);
        put(holidays, hitCount, LocalDate.of(year, 6, 6), "현충일", true);
        put(holidays, hitCount, LocalDate.of(year, 12, 25), "기독탄신일", true);

        // ---- 고정일 + 토/일(또는 다른 공휴일과 겹침) 대체공휴일 ----
        addSatSunEligible(holidays, hitCount, substitutedSearchPoints, substituteChecks, LocalDate.of(year, 3, 1), "삼일절");
        addSatSunEligible(holidays, hitCount, substitutedSearchPoints, substituteChecks, LocalDate.of(year, 5, 5), "어린이날");
        addSatSunEligible(holidays, hitCount, substitutedSearchPoints, substituteChecks, LocalDate.of(year, 8, 15), "광복절");
        addSatSunEligible(holidays, hitCount, substitutedSearchPoints, substituteChecks, LocalDate.of(year, 10, 3), "개천절");
        addSatSunEligible(holidays, hitCount, substitutedSearchPoints, substituteChecks, LocalDate.of(year, 10, 9), "한글날");
        addSatSunEligible(holidays, hitCount, substitutedSearchPoints, substituteChecks, lunarToSolar(year, 4, 8), "부처님오신날");

        // ---- 음력 3일 연휴(설날/추석) + 일요일(또는 다른 공휴일과 겹침) 대체공휴일 ----
        addLunarCluster(holidays, hitCount, substitutedSearchPoints, substituteChecks, lunarToSolar(year, 1, 1), "설날");
        addLunarCluster(holidays, hitCount, substitutedSearchPoints, substituteChecks, lunarToSolar(year, 8, 15), "추석");

        substituteChecks.forEach(Runnable::run);

        List<HolidayEntry> result = new ArrayList<>(holidays.values());
        result.sort(Comparator.comparing(HolidayEntry::date));
        return result;
    }

    /** 같은 날짜에 이미 다른 공휴일이 있으면 이름을 합쳐서 둘 다 남긴다(어린이날+부처님오신날 겹침 같은 경우). */
    private void put(Map<LocalDate, HolidayEntry> holidays, Map<LocalDate, Integer> hitCount,
                      LocalDate date, String name, boolean label) {
        hitCount.merge(date, 1, Integer::sum);
        HolidayEntry existing = holidays.get(date);
        if (existing != null) {
            holidays.put(date, new HolidayEntry(date, existing.name() + "・" + name, existing.label() || label));
        } else {
            holidays.put(date, new HolidayEntry(date, name, label));
        }
    }

    private boolean isWeekend(LocalDate d) {
        return d.getDayOfWeek() == DayOfWeek.SATURDAY || d.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    private void addSatSunEligible(Map<LocalDate, HolidayEntry> holidays, Map<LocalDate, Integer> hitCount,
                                    Set<LocalDate> substitutedSearchPoints, List<Runnable> substituteChecks,
                                    LocalDate date, String name) {
        put(holidays, hitCount, date, name, true);
        substituteChecks.add(() -> {
            if (isWeekend(date) || hitCount.getOrDefault(date, 0) > 1) {
                assignSubstitute(holidays, substitutedSearchPoints, date);
            }
        });
    }

    private void addLunarCluster(Map<LocalDate, HolidayEntry> holidays, Map<LocalDate, Integer> hitCount,
                                  Set<LocalDate> substitutedSearchPoints, List<Runnable> substituteChecks,
                                  LocalDate middle, String name) {
        LocalDate before = middle.minusDays(1);
        LocalDate after = middle.plusDays(1);
        put(holidays, hitCount, before, name + " 연휴", false);
        put(holidays, hitCount, middle, name, true);
        put(holidays, hitCount, after, name + " 연휴", false);
        substituteChecks.add(() -> {
            boolean sundayOrOverlap = before.getDayOfWeek() == DayOfWeek.SUNDAY
                    || middle.getDayOfWeek() == DayOfWeek.SUNDAY
                    || after.getDayOfWeek() == DayOfWeek.SUNDAY
                    || hitCount.getOrDefault(before, 0) > 1
                    || hitCount.getOrDefault(middle, 0) > 1
                    || hitCount.getOrDefault(after, 0) > 1;
            if (sundayOrOverlap) {
                assignSubstitute(holidays, substitutedSearchPoints, after);
            }
        });
    }

    /**
     * searchAfter(트리거가 된 날짜/연휴 끝날) 기준으로 대체공휴일을 하나만 배정한다.
     * 같은 searchAfter에 대해 두 번째로 불리면(예: 어린이날/부처님오신날처럼 서로 다른
     * 공휴일이 같은 날짜를 트리거로 공유하는 경우) 조용히 무시해서 중복 배정을 막는다.
     */
    private void assignSubstitute(Map<LocalDate, HolidayEntry> holidays, Set<LocalDate> substitutedSearchPoints,
                                   LocalDate searchAfter) {
        if (!substitutedSearchPoints.add(searchAfter)) {
            return;
        }
        LocalDate d = searchAfter.plusDays(1);
        while (isWeekend(d) || holidays.containsKey(d)) {
            d = d.plusDays(1);
        }
        holidays.put(d, new HolidayEntry(d, "대체공휴일", true));
    }

    private LocalDate lunarToSolar(int lunarYear, int lunarMonth, int lunarDay) {
        KoreanLunarCalendar cal = KoreanLunarCalendar.getInstance();
        cal.setLunarDate(lunarYear, lunarMonth, lunarDay, false);
        return LocalDate.parse(cal.getSolarIsoFormat());
    }
}
