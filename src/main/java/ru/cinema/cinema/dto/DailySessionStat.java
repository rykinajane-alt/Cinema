
package ru.cinema.cinema.dto;

public class DailySessionStat {

    private final String dateLabel;
    private final long count;
    private final int widthPercent;

    public DailySessionStat(String dateLabel, long count, int widthPercent) {
        this.dateLabel = dateLabel;
        this.count = count;
        this.widthPercent = widthPercent;
    }

    public String getDateLabel() {
        return dateLabel;
    }

    public long getCount() {
        return count;
    }

    public int getWidthPercent() {
        return widthPercent;
    }
}
