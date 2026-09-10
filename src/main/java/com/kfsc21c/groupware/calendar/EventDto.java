package com.kfsc21c.groupware.calendar;

/**
 * FullCalendar가 기대하는 필드명(id/title/start/end)에 맞춘 응답 DTO.
 * User 엔티티(비밀번호 해시 포함)를 그대로 직렬화하지 않기 위해 별도로 둔다.
 * description은 FullCalendar가 인식하는 필드가 아니라서 event.extendedProps.description
 * 으로 자동으로 들어간다(프론트에서 그렇게 꺼내 씀).
 */
public record EventDto(Long id, String title, String start, String end, String description, String creatorName) {

    public static EventDto from(CalendarEvent e) {
        return new EventDto(
                e.getId(),
                e.getTitle(),
                e.getStartDateTime().toString(),
                e.getEndDateTime().toString(),
                e.getDescription(),
                e.getCreator().getDisplayName()
        );
    }
}
