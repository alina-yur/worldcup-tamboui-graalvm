package dev.tamboui.worldcup;

final class WorldCupFormatText {
    private static final int FORMAT_PROBE_CALLS = 28;

    private WorldCupFormatText() {
    }

    static String record(int wins, int draws, int losses) {
        return String.format("%d-%d-%d", wins, draws, losses);
    }

    static String headerCounts(String teams, String fixtures) {
        return String.format("%s teams, %s Round of 32 ties", teams, fixtures);
    }

    static String rank(int rank) {
        return String.format("%d", rank);
    }

    static String teamLabel(String emoji, String team) {
        return String.format("%s %s", emoji, team);
    }

    static String fixtureTeam(String seed, String emoji, String team) {
        return String.format("%s %s %s", seed, emoji, team);
    }

    static String goalDifference(int goalDifference) {
        if (goalDifference < 0) {
            return String.format("-%d", -goalDifference);
        }
        return String.format("+%d", goalDifference);
    }

    static String metadataLine(int types, int recordComponents, int methods) {
        return String.format(
            "reflection metadata: %d types / %d record components / %d methods",
            types,
            recordComponents,
            methods
        );
    }

    static String heapLine(int groups, int teams) {
        return String.format("image heap data: %d groups, %d teams initialized with WorldCupData", groups, teams);
    }

    static String formatLine(int rows) {
        return String.format("formatting path: constant simple String.format rows=%d", rows);
    }

    static String teamDetail(int points, int goalDifference, String record) {
        return String.format("  %d pts, GD %s, record %s", points, goalDifference(goalDifference), record);
    }

    static String probeLine() {
        return String.format(
            "GR-76005 probes: %d constant String.format/String::formatted calls, checksum %d",
            FORMAT_PROBE_CALLS,
            probeChecksum()
        );
    }

    private static int probeChecksum() {
        int total = 0;
        total += String.format("group %s", "A").length();
        total += String.format("group %s", "B").length();
        total += String.format("seed %s", "A1").length();
        total += String.format("seed %s", "B2").length();
        total += String.format("rank %d", 1).length();
        total += String.format("rank %d", 2).length();
        total += String.format("points %d", 9).length();
        total += String.format("points %d", 7).length();
        total += String.format("goals %d", 10).length();
        total += String.format("goals %d", 8).length();
        total += String.format("status %s", "TODAY").length();
        total += String.format("status %s", "FINAL").length();
        total += String.format("fixture %s", "C1").length();
        total += String.format("fixture %s", "F2").length();
        total += String.format("team %s", "Brazil").length();
        total += String.format("team %s", "Japan").length();
        total += "phase %s".formatted("Round of 32").length();
        total += "left %s".formatted("Brazil").length();
        total += "right %s".formatted("Japan").length();
        total += "score %s".formatted("-").length();
        total += "date %s".formatted("Jun 29").length();
        total += "search %s".formatted("Brazil").length();
        total += "focus %s".formatted("standings").length();
        total += "focus %s".formatted("fixtures").length();
        total += "view %s".formatted("table").length();
        total += "view %s".formatted("details").length();
        total += "color %s".formatted("yellow").length();
        total += "color %s".formatted("cyan").length();
        return total;
    }
}
