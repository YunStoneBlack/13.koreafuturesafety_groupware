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
 * - 신정/현충일(2025년까지의 근로자의 날 포함): 대체공휴일 적용 안 됨. 2026년부터 노동절·제헌절은 토/일 대체공휴일 적용.
 * 대체공휴일 날짜는 해당 연휴(또는 해당일) 다음날부터 훑어서, 토·일도 아니고 이미
 * 다른 공휴일도 아닌 첫 번째 날로 정한다. 이 로직은 2026년 실제 공휴일(3.1절->3/2,
 * 광복절->8/17, 개천절->10/5, 부처님오신날->5/25 대체, 설날·추석·어린이날·한글날은
 * 대체 없음)과 2025년 어린이날/부처님오신날 겹침 케이스(->5/6 대체) 전부와 대조해서
 * 일치함을 확인했다.
 */
@Service
public class KoreanHolidayService {

    private final Map<Integer, List<HolidayEntry>> cache = new HashMap<>();

    /**
     * 선거일·임시공휴일 — 계산이 아니라 날짜 목록(2026-10-01, 파이썬 holidays 패키지 0.105 목록과 같음 — 12-1 보고서 웹판 자동 배치도
     * 같은 날을 뺀다). 미래 선거일은 법정 날짜, 보궐·조기 선거나 새 임시공휴일이 정해지면 여기에 추가.
     */
    private static final Map<LocalDate, String> ELECTION_DAYS = Map.ofEntries(
            Map.entry(LocalDate.of(2023, 10, 2), "임시공휴일"),
            Map.entry(LocalDate.of(2024, 4, 10), "국회의원 선거일"),
            Map.entry(LocalDate.of(2024, 10, 1), "국군의 날(임시공휴일)"),
            Map.entry(LocalDate.of(2025, 1, 27), "임시공휴일"),
            Map.entry(LocalDate.of(2025, 6, 3), "대통령 선거일"),
            Map.entry(LocalDate.of(2026, 6, 3), "지방선거일"),
            Map.entry(LocalDate.of(2028, 4, 12), "국회의원 선거일"),
            Map.entry(LocalDate.of(2030, 4, 3), "대통령 선거일"),
            Map.entry(LocalDate.of(2030, 6, 12), "지방선거일"),
            Map.entry(LocalDate.of(2032, 4, 14), "국회의원 선거일"),
            Map.entry(LocalDate.of(2034, 6, 14), "지방선거일"),
            Map.entry(LocalDate.of(2035, 4, 4), "대통령 선거일"),
            Map.entry(LocalDate.of(2036, 4, 9), "국회의원 선거일"),
            Map.entry(LocalDate.of(2038, 6, 2), "지방선거일"),
            Map.entry(LocalDate.of(2040, 4, 4), "대통령 선거일"),
            Map.entry(LocalDate.of(2040, 4, 11), "국회의원 선거일"),
            Map.entry(LocalDate.of(2042, 6, 4), "지방선거일"),
            Map.entry(LocalDate.of(2044, 4, 13), "국회의원 선거일"),
            Map.entry(LocalDate.of(2045, 4, 5), "대통령 선거일"),
            Map.entry(LocalDate.of(2046, 6, 13), "지방선거일"),
            Map.entry(LocalDate.of(2048, 4, 8), "국회의원 선거일"),
            Map.entry(LocalDate.of(2050, 4, 6), "대통령 선거일"),
            Map.entry(LocalDate.of(2050, 6, 1), "지방선거일"));

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
        if (year < 2026) {
            put(holidays, hitCount, LocalDate.of(year, 5, 1), "근로자의 날", true);
        }
        put(holidays, hitCount, LocalDate.of(year, 6, 6), "현충일", true);
        if (year < 2023) {
            put(holidays, hitCount, LocalDate.of(year, 12, 25), "기독탄신일", true);
        }

        // ---- 고정일 + 토/일(또는 다른 공휴일과 겹침) 대체공휴일 ----
        addSatSunEligible(holidays, hitCount, substitutedSearchPoints, substituteChecks, LocalDate.of(year, 3, 1), "삼일절");
        addSatSunEligible(holidays, hitCount, substitutedSearchPoints, substituteChecks, LocalDate.of(year, 5, 5), "어린이날");
        addSatSunEligible(holidays, hitCount, substitutedSearchPoints, substituteChecks, LocalDate.of(year, 8, 15), "광복절");
        addSatSunEligible(holidays, hitCount, substitutedSearchPoints, substituteChecks, LocalDate.of(year, 10, 3), "개천절");
        addSatSunEligible(holidays, hitCount, substitutedSearchPoints, substituteChecks, LocalDate.of(year, 10, 9), "한글날");
        addSatSunEligible(holidays, hitCount, substitutedSearchPoints, substituteChecks, lunarToSolar(year, 4, 8), "부처님오신날");
        // 기독탄신일 — 2023년부터 토/일이면 대체공휴일(2027·2032년 12/25 토요일 → 12/27 월요일)
        if (year >= 2023) {
            addSatSunEligible(holidays, hitCount, substitutedSearchPoints, substituteChecks, LocalDate.of(year, 12, 25), "기독탄신일");
        }
        // 노동절(옛 근로자의 날) — 2026년부터 이름이 바뀌고 대체공휴일도 적용(2027년 5/1 토요일 → 5/3 월요일, 회사도 실제로 쉼 — 사용자 2026-10-01)
        if (year >= 2026) {
            addSatSunEligible(holidays, hitCount, substitutedSearchPoints, substituteChecks, LocalDate.of(year, 5, 1), "노동절");
        }
        // 제헌절 — 2008년부터 쉬지 않다가 2026년부터 다시 공휴일(대체공휴일도 적용 — 2027·2032년은 토요일이라 다음 월요일)
        if (year >= 2026) {
            addSatSunEligible(holidays, hitCount, substitutedSearchPoints, substituteChecks, LocalDate.of(year, 7, 17), "제헌절");
        }

        // ---- 선거일·임시공휴일(대체공휴일 없음) — ELECTION_DAYS 목록 ----
        ELECTION_DAYS.forEach((date, name) -> {
            if (date.getYear() == year) {
                put(holidays, hitCount, date, name, true);
            }
        });

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
                assignSubstitute(holidays, substitutedSearchPoints, date, date);
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
            // 연휴 중 일요일이거나 다른 공휴일과 겹친 날(= 잃은 날)마다 연휴 끝 다음 첫 평일로 하루씩
            for (LocalDate d : List.of(before, middle, after)) {
                if (d.getDayOfWeek() == DayOfWeek.SUNDAY || hitCount.getOrDefault(d, 0) > 1) {
                    assignSubstitute(holidays, substitutedSearchPoints, d, after);
                }
            }
        });
    }

    /**
     * 잃은 날(lostDay — 주말이거나 공휴일끼리 겹친 날) 하나당 대체공휴일 하나를 searchAfter 다음 첫 평일에 배정한다.
     * 같은 잃은 날로 두 번 불리면(어린이날+부처님오신날 2025-05-05, 개천절+추석 2028-10-03처럼 겹친 공휴일이 각자 부르는 경우)
     * 조용히 무시해 하루만 준다(2026-10-01 — 예전엔 연휴 끝날 기준으로 막아서 2028년에 10/5·10/6 이틀이 나왔음).
     */
    private void assignSubstitute(Map<LocalDate, HolidayEntry> holidays, Set<LocalDate> substitutedLostDays,
                                   LocalDate lostDay, LocalDate searchAfter) {
        if (!substitutedLostDays.add(lostDay)) {
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
