package models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.jetbrains.annotations.NotNull;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RequestForm {
	public List<EventRequest> phases;
	public List<EventRequest> events;
	public ZonedDateTime from;
	public ZonedDateTime to;

	public void setFrom(String from) {
		this.from = parseDate(from);
	}

	public void setTo(String to) {
		this.to = parseDate(to);
	}

	private ZonedDateTime parseDate(String date) {
		return ZonedDateTime.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSX"));
	}

	public boolean includePhase(@NotNull String name) {
		return includeEventRequest(name, phases);
	}

	public boolean includeEvent(@NotNull String name) {
		return includeEventRequest(name, events);
	}

	private static boolean includeEventRequest(@NotNull String name, List<EventRequest> eventRequests) {
		for (EventRequest phase : eventRequests) {
			if (name.equals(phase.name)) {
				return phase.include;
			}
		}
		return false;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class EventRequest {
		public EventRequest() {
		}

		public EventRequest(String name, boolean include) {
			this.name = name;
			this.include = include;
		}

		public String name;
		public boolean include;
	}

}
