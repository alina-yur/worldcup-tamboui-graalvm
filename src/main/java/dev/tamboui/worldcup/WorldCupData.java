package dev.tamboui.worldcup;

import java.time.LocalDate;
import java.util.List;

public final class WorldCupData {
    public static final TournamentSnapshot SNAPSHOT = new TournamentSnapshot(
        "2026 FIFA World Cup",
        LocalDate.of(2026, 6, 29),
        standings(),
        fixtures()
    );

    private WorldCupData() {
    }

    public record TournamentSnapshot(
        String title,
        LocalDate snapshotDate,
        List<GroupTable> groups,
        List<Fixture> fixtures
    ) {
    }

    public record GroupTable(String code, List<TeamStanding> teams) {
    }

    public record TeamStanding(
        int rank,
        String emoji,
        String name,
        int played,
        int wins,
        int draws,
        int losses,
        int goalsFor,
        int goalsAgainst,
        int points
    ) {
        int goalDifference() {
            return goalsFor - goalsAgainst;
        }

        String record() {
            return WorldCupFormatText.record(wins, draws, losses);
        }
    }

    public record Fixture(
        LocalDate date,
        String phase,
        String leftSeed,
        String leftEmoji,
        String leftTeam,
        String rightSeed,
        String rightEmoji,
        String rightTeam,
        String status,
        String score
    ) {
    }

    public static GroupTable group(String code) {
        for (GroupTable group : SNAPSHOT.groups()) {
            if (group.code().equalsIgnoreCase(code)) {
                return group;
            }
        }
        return SNAPSHOT.groups().get(0);
    }

    static Class<?>[] reflectedTypes() {
        return new Class<?>[] {
            TournamentSnapshot.class,
            GroupTable.class,
            TeamStanding.class,
            Fixture.class
        };
    }

    private static List<GroupTable> standings() {
        return List.of(
            group("A",
                team(1, "🇲🇽", "Mexico", 3, 3, 0, 0, 6, 0, 9),
                team(2, "🇿🇦", "South Africa", 3, 1, 1, 1, 2, 3, 4),
                team(3, "🇰🇷", "South Korea", 3, 1, 0, 2, 2, 3, 3),
                team(4, "🇨🇿", "Czechia", 3, 0, 1, 2, 2, 6, 1)
            ),
            group("B",
                team(1, "🇨🇭", "Switzerland", 3, 2, 1, 0, 7, 3, 7),
                team(2, "🇨🇦", "Canada", 3, 1, 1, 1, 8, 3, 4),
                team(3, "🇧🇦", "Bosnia and Herzegovina", 3, 1, 1, 1, 5, 6, 4),
                team(4, "🇶🇦", "Qatar", 3, 0, 1, 2, 2, 10, 1)
            ),
            group("C",
                team(1, "🇧🇷", "Brazil", 3, 2, 1, 0, 7, 1, 7),
                team(2, "🇲🇦", "Morocco", 3, 2, 1, 0, 6, 3, 7),
                team(3, "🏴󠁧󠁢󠁳󠁣󠁴󠁿", "Scotland", 3, 1, 0, 2, 1, 4, 3),
                team(4, "🇭🇹", "Haiti", 3, 0, 0, 3, 2, 8, 0)
            ),
            group("D",
                team(1, "🇺🇸", "United States", 3, 2, 0, 1, 8, 4, 6),
                team(2, "🇦🇺", "Australia", 3, 1, 1, 1, 2, 2, 4),
                team(3, "🇵🇾", "Paraguay", 3, 1, 1, 1, 2, 4, 4),
                team(4, "🇹🇷", "Türkiye", 3, 1, 0, 2, 3, 5, 3)
            ),
            group("E",
                team(1, "🇩🇪", "Germany", 3, 2, 0, 1, 10, 4, 6),
                team(2, "🇨🇮", "Ivory Coast", 3, 2, 0, 1, 4, 2, 6),
                team(3, "🇪🇨", "Ecuador", 3, 1, 1, 1, 2, 2, 4),
                team(4, "🇨🇼", "Curacao", 3, 0, 1, 2, 1, 9, 1)
            ),
            group("F",
                team(1, "🇳🇱", "Netherlands", 3, 2, 1, 0, 10, 4, 7),
                team(2, "🇯🇵", "Japan", 3, 1, 2, 0, 7, 3, 5),
                team(3, "🇸🇪", "Sweden", 3, 1, 1, 1, 7, 7, 4),
                team(4, "🇹🇳", "Tunisia", 3, 0, 0, 3, 2, 12, 0)
            ),
            group("G",
                team(1, "🇧🇪", "Belgium", 3, 1, 2, 0, 6, 2, 5),
                team(2, "🇪🇬", "Egypt", 3, 1, 2, 0, 5, 3, 5),
                team(3, "🇮🇷", "Iran", 3, 0, 3, 0, 3, 3, 3),
                team(4, "🇳🇿", "New Zealand", 3, 0, 1, 2, 4, 10, 1)
            ),
            group("H",
                team(1, "🇪🇸", "Spain", 3, 2, 1, 0, 5, 0, 7),
                team(2, "🇨🇻", "Cape Verde", 3, 0, 3, 0, 2, 2, 3),
                team(3, "🇺🇾", "Uruguay", 3, 0, 2, 1, 3, 4, 2),
                team(4, "🇸🇦", "Saudi Arabia", 3, 0, 2, 1, 1, 5, 2)
            ),
            group("I",
                team(1, "🇫🇷", "France", 3, 3, 0, 0, 10, 2, 9),
                team(2, "🇳🇴", "Norway", 3, 2, 0, 1, 8, 7, 6),
                team(3, "🇸🇳", "Senegal", 3, 1, 0, 2, 8, 6, 3),
                team(4, "🇮🇶", "Iraq", 3, 0, 0, 3, 1, 12, 0)
            ),
            group("J",
                team(1, "🇦🇷", "Argentina", 3, 3, 0, 0, 8, 1, 9),
                team(2, "🇦🇹", "Austria", 3, 1, 1, 1, 6, 6, 4),
                team(3, "🇩🇿", "Algeria", 3, 1, 1, 1, 5, 7, 4),
                team(4, "🇯🇴", "Jordan", 3, 0, 0, 3, 3, 8, 0)
            ),
            group("K",
                team(1, "🇨🇴", "Colombia", 3, 2, 1, 0, 4, 1, 7),
                team(2, "🇵🇹", "Portugal", 3, 1, 2, 0, 6, 1, 5),
                team(3, "🇨🇩", "DR Congo", 3, 1, 1, 1, 4, 3, 4),
                team(4, "🇺🇿", "Uzbekistan", 3, 0, 0, 3, 2, 11, 0)
            ),
            group("L",
                team(1, "🏴", "England", 3, 2, 1, 0, 6, 2, 7),
                team(2, "🇭🇷", "Croatia", 3, 2, 0, 1, 5, 5, 6),
                team(3, "🇬🇭", "Ghana", 3, 1, 1, 1, 2, 2, 4),
                team(4, "🇵🇦", "Panama", 3, 0, 0, 3, 0, 4, 0)
            )
        );
    }

    private static List<Fixture> fixtures() {
        return List.of(
            fixture("2026-06-28", "Round of 32", "A2", "🇿🇦", "South Africa", "B2", "🇨🇦", "Canada", "FINAL", "0-1"),
            fixture("2026-06-29", "Round of 32", "C1", "🇧🇷", "Brazil", "F2", "🇯🇵", "Japan", "TODAY", "-"),
            fixture("2026-06-29", "Round of 32", "E1", "🇩🇪", "Germany", "D3", "🇵🇾", "Paraguay", "TODAY", "-"),
            fixture("2026-06-29", "Round of 32", "F1", "🇳🇱", "Netherlands", "C2", "🇲🇦", "Morocco", "TODAY", "-"),
            fixture("2026-06-30", "Round of 32", "E2", "🇨🇮", "Ivory Coast", "I2", "🇳🇴", "Norway", "NEXT", "-"),
            fixture("2026-06-30", "Round of 32", "I1", "🇫🇷", "France", "F3", "🇸🇪", "Sweden", "NEXT", "-"),
            fixture("2026-06-30", "Round of 32", "A1", "🇲🇽", "Mexico", "E3", "🇪🇨", "Ecuador", "NEXT", "-"),
            fixture("2026-07-01", "Round of 32", "L1", "🏴", "England", "K3", "🇨🇩", "DR Congo", "UPCOMING", "-"),
            fixture("2026-07-01", "Round of 32", "G1", "🇧🇪", "Belgium", "I3", "🇸🇳", "Senegal", "UPCOMING", "-"),
            fixture("2026-07-01", "Round of 32", "D1", "🇺🇸", "United States", "B3", "🇧🇦", "Bosnia and Herzegovina", "UPCOMING", "-"),
            fixture("2026-07-02", "Round of 32", "H1", "🇪🇸", "Spain", "J2", "🇦🇹", "Austria", "UPCOMING", "-"),
            fixture("2026-07-02", "Round of 32", "K2", "🇵🇹", "Portugal", "L2", "🇭🇷", "Croatia", "UPCOMING", "-"),
            fixture("2026-07-02", "Round of 32", "B1", "🇨🇭", "Switzerland", "J3", "🇩🇿", "Algeria", "UPCOMING", "-"),
            fixture("2026-07-03", "Round of 32", "D2", "🇦🇺", "Australia", "G2", "🇪🇬", "Egypt", "UPCOMING", "-"),
            fixture("2026-07-03", "Round of 32", "J1", "🇦🇷", "Argentina", "H2", "🇨🇻", "Cape Verde", "UPCOMING", "-"),
            fixture("2026-07-03", "Round of 32", "K1", "🇨🇴", "Colombia", "L3", "🇬🇭", "Ghana", "UPCOMING", "-")
        );
    }

    private static GroupTable group(String code, TeamStanding... teams) {
        return new GroupTable(code, List.of(teams));
    }

    private static TeamStanding team(
        int rank,
        String emoji,
        String name,
        int played,
        int wins,
        int draws,
        int losses,
        int goalsFor,
        int goalsAgainst,
        int points
    ) {
        return new TeamStanding(rank, emoji, name, played, wins, draws, losses, goalsFor, goalsAgainst, points);
    }

    private static Fixture fixture(
        String date,
        String phase,
        String leftSeed,
        String leftEmoji,
        String leftTeam,
        String rightSeed,
        String rightEmoji,
        String rightTeam,
        String status,
        String score
    ) {
        return new Fixture(LocalDate.parse(date), phase, leftSeed, leftEmoji, leftTeam, rightSeed, rightEmoji, rightTeam, status, score);
    }

}
