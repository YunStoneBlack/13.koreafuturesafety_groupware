/**
 * 캘린더 위젯 공용 스크립트. /calendar 전체화면과 홈 화면 미니 캘린더가 이 파일을
 * 그대로 같이 쓴다(로직 중복 방지) - 두 페이지 모두 동일한 id의 모달 마크업
 * (dayModalOverlay/eventDetailOverlay/newEventOverlay)을 갖고 있다고 가정한다.
 */
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
        var editStartTime = document.getElementById('editEventStartTime');
        var editEndDate = document.getElementById('editEventEndDate');
        var editEndTime = document.getElementById('editEventEndTime');
        var editDeleteBtn = document.getElementById('editEventDeleteBtn');
        var currentEditEventId = null;

        var newOverlay = document.getElementById('newEventOverlay');
        var newForm = document.getElementById('newEventForm');
        var newTitle = document.getElementById('newEventTitle');
        var newDesc = document.getElementById('newEventDescription');
        var newStartDate = document.getElementById('newEventStartDate');
        var newStartTime = document.getElementById('newEventStartTime');
        var newEndDate = document.getElementById('newEventEndDate');
        var newEndTime = document.getElementById('newEventEndTime');

        function pad(n) { return n < 10 ? '0' + n : '' + n; }
        function toDateStr(d) { return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate()); }
        function timeKorean(hhmm) {
            var h = parseInt(hhmm.slice(0, 2), 10);
            var m = parseInt(hhmm.slice(3, 5), 10);
            return m === 0 ? (h + '시') : (h + '시 ' + m + '분');
        }
        function timeRangeKorean(startIso, endIso) {
            return timeKorean(startIso.slice(11, 16)) + ' ~ ' + timeKorean(endIso.slice(11, 16));
        }

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
            editStartTime.value = ev.start.slice(11, 16);
            editEndDate.value = ev.end.slice(0, 10);
            editEndTime.value = ev.end.slice(11, 16);
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
            params.set('startTime', editStartTime.value);
            params.set('endDate', editEndDate.value);
            params.set('endTime', editEndTime.value);
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
            newStartTime.value = '09:00';
            newEndDate.value = dateStr;
            newEndTime.value = '10:00';
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
            params.set('startTime', newStartTime.value);
            params.set('endDate', newEndDate.value);
            params.set('endTime', newEndTime.value);
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

        // ---------- FullCalendar ----------
        var baseOptions = {
            initialView: 'dayGridMonth',
            locale: 'ko',
            eventColor: style.getPropertyValue('--blue').trim(),
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
    });
}
