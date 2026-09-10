/**
 * 캘린더 위젯 공용 스크립트. /calendar 전체화면과 홈 화면 미니 캘린더가 이 파일을
 * 그대로 같이 쓴다(로직 중복 방지) - 두 페이지 모두 동일한 id의 모달 마크업
 * (dayModalOverlay/eventDetailOverlay/newEventOverlay)을 갖고 있다고 가정한다.
 */

// 대한민국 공휴일/명절(고정일 + 음력 기반 + 대체공휴일)은 이제 서버(/api/holidays?year=)가
// 계산해서 내려준다(KoreanHolidayService, usingsky/KoreanLunarCalendar 라이브러리 사용,
// 2025~2050년 지원) - 예전엔 여기(JS)에 연도별로 손으로 날짜를 하드코딩했었지만,
// 음력 변환을 서버가 대신 해주니 더 이상 매년 사람이 갱신할 필요가 없다.
// 연도별로 한 번 받아오면 holidayCacheByYear에 캐싱해두고, 화면에 보이는 연도가
// 바뀔 때만(예: 12월<->1월 넘어갈 때) 새로 요청한다.
var holidayCacheByYear = {};
var KR_HOLIDAYS = {};

function fetchHolidayYear(year) {
    if (holidayCacheByYear[year]) return holidayCacheByYear[year];
    var promise = fetch('/api/holidays?year=' + year)
        .then(function (res) { return res.json(); })
        .then(function (list) {
            var map = {};
            list.forEach(function (h) { map[h.date] = { name: h.name, label: h.label }; });
            return map;
        })
        .catch(function () { return {}; });
    holidayCacheByYear[year] = promise;
    return promise;
}

function initGroupwareCalendar(elId, fcOptions) {
    document.addEventListener('DOMContentLoaded', function () {
        var calendarEl = document.getElementById(elId);
        if (!calendarEl) return;

        var style = getComputedStyle(document.documentElement);
        var csrfToken = document.querySelector('meta[name="_csrf"]').content;
        var csrfParam = document.querySelector('meta[name="_csrf_parameter"]').content;

        var dayOverlay = document.getElementById('dayModalOverlay');
        var dayTitleEl = document.getElementById('dayModalTitle');
        var dayListEl = document.getElementById('dayModalList');
        var dayAddBtn = document.getElementById('dayModalAddBtn');

        var detailOverlay = document.getElementById('eventDetailOverlay');
        var detailTitleEl = document.getElementById('eventDetailTitle');
        var detailTimeEl = document.getElementById('eventDetailTime');
        var detailDescEl = document.getElementById('eventDetailDescription');
        var detailEditBtn = document.getElementById('eventDetailEditBtn');
        var currentDetailEvent = null;

        var editOverlay = document.getElementById('editEventOverlay');
        var editForm = document.getElementById('editEventForm');
        var editTitle = document.getElementById('editEventTitle');
        var editDesc = document.getElementById('editEventDescription');
        var editStartDate = document.getElementById('editEventStartDate');
        var editStartHour = document.getElementById('editEventStartHour');
        var editStartMinute = document.getElementById('editEventStartMinute');
        var editEndDate = document.getElementById('editEventEndDate');
        var editEndHour = document.getElementById('editEventEndHour');
        var editEndMinute = document.getElementById('editEventEndMinute');
        var editDeleteBtn = document.getElementById('editEventDeleteBtn');
        var currentEditEventId = null;

        var newOverlay = document.getElementById('newEventOverlay');
        var newForm = document.getElementById('newEventForm');
        var newTitle = document.getElementById('newEventTitle');
        var newDesc = document.getElementById('newEventDescription');
        var newStartDate = document.getElementById('newEventStartDate');
        var newStartHour = document.getElementById('newEventStartHour');
        var newStartMinute = document.getElementById('newEventStartMinute');
        var newEndDate = document.getElementById('newEventEndDate');
        var newEndHour = document.getElementById('newEventEndHour');
        var newEndMinute = document.getElementById('newEventEndMinute');

        function pad(n) { return n < 10 ? '0' + n : '' + n; }
        function toDateStr(d) { return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate()); }
        function timeKorean(hhmm) {
            if (!hhmm || hhmm.length < 5) return '';
            var h = parseInt(hhmm.slice(0, 2), 10);
            var m = parseInt(hhmm.slice(3, 5), 10);
            if (isNaN(h) || isNaN(m)) return '';
            return m === 0 ? (h + '시') : (h + '시 ' + m + '분');
        }
        function timeRangeKorean(startIso, endIso) {
            var startText = timeKorean(startIso ? startIso.slice(11, 16) : '');
            var endText = endIso ? timeKorean(endIso.slice(11, 16)) : '';
            return endText ? (startText + ' ~ ' + endText) : startText;
        }

        // ---------- 시/분 드롭다운(00~23시, 10분 단위) ----------
        function populateHourMinute(hourEl, minuteEl) {
            for (var h = 0; h < 24; h++) {
                var opt = document.createElement('option');
                opt.value = pad(h); opt.textContent = pad(h);
                hourEl.appendChild(opt);
            }
            [0, 10, 20, 30, 40, 50].forEach(function (m) {
                var opt = document.createElement('option');
                opt.value = pad(m); opt.textContent = pad(m);
                minuteEl.appendChild(opt);
            });
        }
        [[newStartHour, newStartMinute], [newEndHour, newEndMinute],
         [editStartHour, editStartMinute], [editEndHour, editEndMinute]].forEach(function (pair) {
            populateHourMinute(pair[0], pair[1]);
        });

        function snapMinute(m) { return pad(Math.round(m / 10) * 10 % 60); }

        // 시작 시간이 바뀌어 종료 시간보다 같거나 늦어지면, 종료를 시작+1시간으로 자동으로
        // 채워준다(그 뒤에 사용자가 종료 시간을 직접 다시 고치는 건 자유).
        function wireAutoBumpEnd(startDateEl, startHourEl, startMinuteEl, endDateEl, endHourEl, endMinuteEl) {
            function bump() {
                if (!startDateEl.value || !endDateEl.value) return;
                var start = new Date(startDateEl.value + 'T' + startHourEl.value + ':' + startMinuteEl.value + ':00');
                var end = new Date(endDateEl.value + 'T' + endHourEl.value + ':' + endMinuteEl.value + ':00');
                if (end <= start) {
                    var bumped = new Date(start.getTime() + 60 * 60000);
                    endDateEl.value = toDateStr(bumped);
                    endHourEl.value = pad(bumped.getHours());
                    endMinuteEl.value = pad(bumped.getMinutes());
                }
            }
            [startDateEl, startHourEl, startMinuteEl].forEach(function (el) {
                el.addEventListener('change', bump);
            });
        }
        wireAutoBumpEnd(newStartDate, newStartHour, newStartMinute, newEndDate, newEndHour, newEndMinute);
        wireAutoBumpEnd(editStartDate, editStartHour, editStartMinute, editEndDate, editEndHour, editEndMinute);

        // ---------- 모달 드래그 이동 ----------
        function makeDraggable(panelEl) {
            var head = panelEl.querySelector('.day-modal-head');
            var dragging = false, startX = 0, startY = 0, origX = 0, origY = 0;
            head.addEventListener('mousedown', function (e) {
                if (e.target.closest('.day-modal-close')) return;
                dragging = true;
                startX = e.clientX; startY = e.clientY;
                var m = new DOMMatrixReadOnly(getComputedStyle(panelEl).transform);
                origX = m.m41; origY = m.m42;
                document.body.style.userSelect = 'none';
                e.preventDefault();
            });
            document.addEventListener('mousemove', function (e) {
                if (!dragging) return;
                var dx = e.clientX - startX, dy = e.clientY - startY;
                panelEl.style.transform = 'translate(' + (origX + dx) + 'px,' + (origY + dy) + 'px)';
            });
            document.addEventListener('mouseup', function () {
                dragging = false;
                document.body.style.userSelect = '';
            });
        }
        [dayOverlay, detailOverlay, newOverlay, editOverlay].forEach(function (overlay) {
            if (overlay) makeDraggable(overlay.querySelector('.day-modal'));
        });
        function resetPosition(overlay) { overlay.querySelector('.day-modal').style.transform = ''; }

        // ---------- 일정 상세 모달 ----------
        function openEventDetail(ev) {
            currentDetailEvent = ev;
            detailTitleEl.textContent = ev.title;
            var startParts = ev.start.split('-');
            var dateLabel = parseInt(startParts[1], 10) + '월 ' + parseInt(startParts[2].slice(0, 2), 10) + '일';
            detailTimeEl.textContent = dateLabel + ' · ' + timeRangeKorean(ev.start, ev.end);
            detailDescEl.textContent = ev.description ? ev.description : '설명이 없습니다.';
            resetPosition(detailOverlay);
            detailOverlay.classList.add('open');
        }
        document.getElementById('eventDetailClose').addEventListener('click', function () { detailOverlay.classList.remove('open'); });
        detailOverlay.addEventListener('click', function (e) { if (e.target === detailOverlay) detailOverlay.classList.remove('open'); });

        // ---------- 일정 수정 모달 ----------
        function openEditEventModal(ev) {
            currentEditEventId = ev.id;
            editTitle.value = ev.title;
            editDesc.value = ev.description || '';
            editStartDate.value = ev.start.slice(0, 10);
            editStartHour.value = ev.start.slice(11, 13);
            editStartMinute.value = snapMinute(parseInt(ev.start.slice(14, 16), 10));
            if (ev.end && ev.end.length >= 16) {
                editEndDate.value = ev.end.slice(0, 10);
                editEndHour.value = ev.end.slice(11, 13);
                editEndMinute.value = snapMinute(parseInt(ev.end.slice(14, 16), 10));
            } else {
                // 저장된 종료 시각이 없거나 시작보다 빠른(데이터 오류) 경우:
                // 화면이 비어있지 않도록 일단 시작+1시간으로 채워두고, 사용자가 직접 고치게 한다.
                var bumped = new Date(ev.start.replace(' ', 'T'));
                bumped.setHours(bumped.getHours() + 1);
                editEndDate.value = toDateStr(bumped);
                editEndHour.value = pad(bumped.getHours());
                editEndMinute.value = pad(bumped.getMinutes());
            }
            detailOverlay.classList.remove('open');
            resetPosition(editOverlay);
            editOverlay.classList.add('open');
        }
        detailEditBtn.addEventListener('click', function () { if (currentDetailEvent) openEditEventModal(currentDetailEvent); });
        document.getElementById('editEventClose').addEventListener('click', function () { editOverlay.classList.remove('open'); });
        editOverlay.addEventListener('click', function (e) { if (e.target === editOverlay) editOverlay.classList.remove('open'); });

        editForm.addEventListener('submit', function (e) {
            e.preventDefault();
            var params = new URLSearchParams();
            params.set('title', editTitle.value);
            params.set('description', editDesc.value);
            params.set('startDate', editStartDate.value);
            params.set('startTime', editStartHour.value + ':' + editStartMinute.value);
            params.set('endDate', editEndDate.value);
            params.set('endTime', editEndHour.value + ':' + editEndMinute.value);
            params.set(csrfParam, csrfToken);

            fetch('/calendar/' + currentEditEventId, { method: 'POST', body: params })
                .then(function (res) {
                    if (res.ok) {
                        editOverlay.classList.remove('open');
                        dayOverlay.classList.remove('open');
                        calendar.refetchEvents();
                    } else {
                        alert('저장에 실패했습니다. 입력값을 확인해주세요.');
                    }
                })
                .catch(function () { alert('저장 중 오류가 발생했습니다.'); });
        });

        editDeleteBtn.addEventListener('click', function () {
            if (!currentEditEventId) return;
            if (!confirm('삭제하시겠습니까?')) return;
            var params = new URLSearchParams();
            params.set(csrfParam, csrfToken);
            fetch('/calendar/' + currentEditEventId + '/delete', { method: 'POST', body: params })
                .then(function (res) {
                    if (res.ok) {
                        editOverlay.classList.remove('open');
                        dayOverlay.classList.remove('open');
                        calendar.refetchEvents();
                    } else {
                        alert('삭제에 실패했습니다.');
                    }
                })
                .catch(function () { alert('삭제 중 오류가 발생했습니다.'); });
        });

        // ---------- 새 일정 추가 모달 ----------
        function openNewEventModal(dateStr) {
            newForm.reset();
            newStartDate.value = dateStr;
            newStartHour.value = '09';
            newStartMinute.value = '00';
            newEndDate.value = dateStr;
            newEndHour.value = '10';
            newEndMinute.value = '00';
            resetPosition(newOverlay);
            newOverlay.classList.add('open');
        }
        document.getElementById('newEventClose').addEventListener('click', function () { newOverlay.classList.remove('open'); });
        newOverlay.addEventListener('click', function (e) { if (e.target === newOverlay) newOverlay.classList.remove('open'); });

        newForm.addEventListener('submit', function (e) {
            e.preventDefault();
            var params = new URLSearchParams();
            params.set('title', newTitle.value);
            params.set('description', newDesc.value);
            params.set('startDate', newStartDate.value);
            params.set('startTime', newStartHour.value + ':' + newStartMinute.value);
            params.set('endDate', newEndDate.value);
            params.set('endTime', newEndHour.value + ':' + newEndMinute.value);
            params.set(csrfParam, csrfToken);

            fetch('/calendar', { method: 'POST', body: params })
                .then(function (res) {
                    if (res.ok) {
                        newOverlay.classList.remove('open');
                        dayOverlay.classList.remove('open');
                        calendar.refetchEvents();
                    } else {
                        alert('저장에 실패했습니다. 입력값을 확인해주세요.');
                    }
                })
                .catch(function () { alert('저장 중 오류가 발생했습니다.'); });
        });

        // ---------- 하루 일정 목록 모달 ----------
        function openDayModal(dateStr) {
            var dayEvents = calendar.getEvents()
                .filter(function (ev) { return toDateStr(ev.start) === dateStr; })
                .map(function (ev) {
                    return {
                        id: ev.id, title: ev.title, start: ev.startStr, end: ev.endStr,
                        description: ev.extendedProps.description
                    };
                })
                .sort(function (a, b) { return a.start.localeCompare(b.start); });

            var parts = dateStr.split('-');
            dayTitleEl.textContent = parseInt(parts[1], 10) + '월 ' + parseInt(parts[2], 10) + '일 일정';

            dayListEl.innerHTML = '';
            if (dayEvents.length === 0) {
                var empty = document.createElement('div');
                empty.className = 'empty-note';
                empty.textContent = '등록된 일정이 없습니다.';
                dayListEl.appendChild(empty);
            } else {
                dayEvents.forEach(function (ev) {
                    var row = document.createElement('button');
                    row.type = 'button';
                    row.className = 'row';
                    row.style.width = '100%';
                    row.style.textAlign = 'left';
                    row.style.border = 'none';
                    row.style.background = 'none';
                    row.style.cursor = 'pointer';
                    row.style.font = 'inherit';
                    var titleSpan = document.createElement('span');
                    titleSpan.className = 'title';
                    titleSpan.textContent = ev.title;
                    var timeSpan = document.createElement('span');
                    timeSpan.className = 'meta num';
                    timeSpan.textContent = timeRangeKorean(ev.start, ev.end);
                    row.appendChild(titleSpan);
                    row.appendChild(timeSpan);
                    row.addEventListener('click', function () { openEventDetail(ev); });
                    dayListEl.appendChild(row);
                });
            }
            dayAddBtn.onclick = function () { openNewEventModal(dateStr); };
            resetPosition(dayOverlay);
            dayOverlay.classList.add('open');
        }
        document.getElementById('dayModalClose').addEventListener('click', function () { dayOverlay.classList.remove('open'); });
        dayOverlay.addEventListener('click', function (e) { if (e.target === dayOverlay) dayOverlay.classList.remove('open'); });

        // ---------- 공휴일 표시 ----------
        // dayCellDidMount는 "이 칸이 처음 DOM에 붙을 때" 딱 한 번만 불린다.
        // FullCalendar가 월 이동 시 일부 날짜 칸(특히 이전/다음 달과 겹치는 줄)의
        // DOM을 재사용하는 경우가 있어서, didMount에만 의존하면 예를 들어
        // "9월 화면에 흐리게 보이던 10월 1~3일"이 "10월 화면의 진짜 10월 1~3일"이
        // 될 때 다시 안 불려서 공휴일 표시가 누락되는 버그가 있었다(9월→10월은
        // 안 보이는데 9월→11월→10월로 가면 보이는 증상이 바로 이거). 그래서 월이
        // 바뀔 때마다("datesSet") 보이는 칸을 전부 다시 스캔해서 매번 새로 칠한다.
        function applyHolidayStyling() {
            calendarEl.querySelectorAll('.fc-daygrid-day').forEach(function (cell) {
                var dateStr = cell.getAttribute('data-date');
                var isOther = cell.classList.contains('fc-day-other');
                var holiday = !isOther ? KR_HOLIDAYS[dateStr] : null;

                cell.classList.toggle('holiday', !!holiday);
                cell.title = holiday ? holiday.name : '';

                var topEl = cell.querySelector('.fc-daygrid-day-top');
                var existingLabel = topEl ? topEl.querySelector('.holiday-label') : null;
                if (existingLabel) existingLabel.remove();
                if (holiday && holiday.label && topEl) {
                    var labelEl = document.createElement('div');
                    labelEl.className = 'holiday-label';
                    labelEl.textContent = holiday.name;
                    // .fc-daygrid-day-top은 FullCalendar 기본 CSS가 row-reverse라
                    // 날짜 숫자(<a>) 뒤에 넣어야 화면에는 숫자 반대편(왼쪽)에 뜬다.
                    topEl.appendChild(labelEl);
                }
            });
        }

        // ---------- FullCalendar ----------
        var baseOptions = {
            initialView: 'dayGridMonth',
            locale: 'ko',
            eventColor: style.getPropertyValue('--blue').trim(),
            datesSet: function (arg) {
                // 달력 그리드에 보이는 범위가 걸친 연도(1~2개, 예: 12월↔1월 경계)의
                // 공휴일을 미리 받아 KR_HOLIDAYS로 합친 뒤에 칠한다.
                var startYear = arg.start.getFullYear();
                var endYear = new Date(arg.end.getTime() - 86400000).getFullYear();
                var years = startYear === endYear ? [startYear] : [startYear, endYear];
                Promise.all(years.map(fetchHolidayYear)).then(function (maps) {
                    KR_HOLIDAYS = Object.assign.apply(Object, [{}].concat(maps));
                    applyHolidayStyling();
                });
            },
            eventContent: function (arg) {
                var dot = document.createElement('div');
                dot.className = 'fc-daygrid-event-dot';
                dot.style.borderColor = arg.event.backgroundColor || style.getPropertyValue('--blue').trim();
                var timeEl = document.createElement('div');
                timeEl.className = 'fc-event-time';
                timeEl.textContent = timeKorean(arg.event.startStr.slice(11, 16));
                var titleEl = document.createElement('div');
                titleEl.className = 'fc-event-title';
                titleEl.textContent = arg.event.title;
                return { domNodes: [dot, timeEl, titleEl] };
            },
            events: function (fetchInfo, successCallback, failureCallback) {
                var start = fetchInfo.start.toISOString().slice(0, 10);
                var end = fetchInfo.end.toISOString().slice(0, 10);
                fetch('/api/calendar/events?start=' + start + '&end=' + end)
                    .then(function (res) { return res.json(); })
                    .then(function (data) {
                        successCallback(data.map(function (e) {
                            return { id: e.id, title: e.title, start: e.start, end: e.end, description: e.description };
                        }));
                    })
                    .catch(failureCallback);
            },
            dateClick: function (info) { openDayModal(info.dateStr); },
            eventClick: function (info) { openDayModal(toDateStr(info.event.start)); }
        };
        var calendar = new FullCalendar.Calendar(calendarEl, Object.assign(baseOptions, fcOptions || {}));
        calendar.render();

        // 캘린더 바깥의 다른 버튼에서도 이 모달들을 띄울 수 있게 열어둔다
        // (예: home.html의 "오늘 일정"/"다가오는 일정" 카드, calendar/index.html
        // 상단의 "+ 일정 추가" 버튼).
        window.groupwareCalendar = {
            openDayModal: openDayModal,
            openNewEventModal: openNewEventModal
        };
    });
}
