package logics;

import com.google.common.io.LineReader;
import models.Event;
import models.RequestForm;

import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.TreeMap;
import java.util.TreeSet;

public class MoonEventCalculation implements Calculation {

	TreeMap<ZonedDateTime, Event> lunarEclipses = new TreeMap<>();

	public MoonEventCalculation() {
		try {
			final LineReader csvFile = new LineReader(new FileReader(this.getClass().getResource("lunar-eclipses.csv").getFile()));
			csvFile.readLine(); //skip header
			String line;
			while ((line = csvFile.readLine()) != null) {
				final String[] row = line.split("\\t");
				final ZonedDateTime date = LocalDateTime.parse(row[0], DateTimeFormatter.ofPattern("d.M.u'T'H:m:s")).atZone(ZoneOffset.UTC);
				String eclipseTypeName = getEclipseName(row[1]);
				lunarEclipses.put(date, new Event(date, eclipseTypeName, null));
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static String getEclipseName(String shortcut) {
		switch (shortcut) {
			case "T":
				return "Totale Mondfinsternis";
			case "P":
				return "Partielle Mondfinsternis";
			default:
				throw new RuntimeException(shortcut + " is no known eclipse type");
		}
	}

	@Override
	public Collection<Event> calculate(RequestForm requestForm) {
		final Collection<Event> eventCollection = new TreeSet<>();
		calculate(requestForm, eventCollection);
		return eventCollection;
	}

	@Override
	public void calculate(RequestForm requestForm, Collection<Event> eventCollection) {
		if (requestForm.includeEvent("Mondfinsternis")) {
			for (Event lunarEclipseEvent : lunarEclipses.tailMap(requestForm.from).values()) {
				if (lunarEclipseEvent.getDateTime().isBefore(requestForm.to)) {
					eventCollection.add(lunarEclipseEvent);
				} else {
					break;
				}
			}
		}
	}

}
