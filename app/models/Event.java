package models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class Event implements Comparable<Event> {

	@NotNull
	private ZonedDateTime dateTime;

	@NotNull
	private String title;

	@Nullable
	private String description;

	public Event(@NotNull ZonedDateTime dateTime, @NotNull String title, @Nullable String description) {
		this.dateTime = dateTime;
		this.title = title;
		this.description = description;
	}

	@NotNull
	@JsonIgnore
	public ZonedDateTime getDateTime() {
		return dateTime;
	}

	@JsonProperty("date")
	public String getDateString() {
		return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
	}

	@NotNull
	public String getTitle() {
		return title;
	}

	@Nullable
	public String getDescription() {
		return description;
	}

	@Override
	public int compareTo(@NotNull Event other) {
		return this.dateTime.compareTo(other.dateTime);
	}

	@Override
	public String toString() {
		return title + "@" + dateTime;
	}
}
