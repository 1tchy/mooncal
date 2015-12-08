package models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

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

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class EventRequest {
		public String name;
		public String include;
	}

}
