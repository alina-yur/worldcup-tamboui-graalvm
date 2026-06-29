package dev.tamboui.worldcup;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.List;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.export.ExportRequest;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Layout;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Backend;
import dev.tamboui.terminal.BackendFactory;
import dev.tamboui.terminal.Frame;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.text.Text;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.block.Borders;
import dev.tamboui.widgets.block.Title;
import dev.tamboui.widgets.input.TextInput;
import dev.tamboui.widgets.input.TextInputState;
import dev.tamboui.widgets.paragraph.Paragraph;
import dev.tamboui.widgets.tabs.Tabs;
import dev.tamboui.widgets.tabs.TabsState;
import dev.tamboui.widgets.table.Cell;
import dev.tamboui.widgets.table.Row;
import dev.tamboui.widgets.table.Table;
import dev.tamboui.widgets.table.TableState;

public final class WorldCupStandingsApp {
    private static final String[] SPINNER = { "◐", "◓", "◑", "◒" };

    private final TabsState groupTabsState = new TabsState();
    private final TableState standingsState = new TableState();
    private final TableState fixturesState = new TableState();
    private final TextInputState searchInput = new TextInputState();
    private final List<ClickRegion> clickRegions = new ArrayList<>();
    private List<WorldCupData.TeamStanding> visibleTeams = List.of();
    private List<WorldCupData.Fixture> visibleFixtures = List.of();
    private Focus focus = Focus.STANDINGS;
    private int groupIndex;
    private int selectedStanding;
    private int selectedFixture;
    private int animationFrame;
    private boolean searchMode;
    private boolean running = true;

    private WorldCupStandingsApp(String groupCode) {
        this.groupIndex = groupIndex(groupCode);
        groupTabsState.select(groupIndex);
        standingsState.select(0);
        fixturesState.select(0);
    }

    public static void main(String[] args) throws Exception {
        Options options = Options.parse(args);
        if (options.help()) {
            printHelp();
            return;
        }

        var app = new WorldCupStandingsApp(options.group());
        if (!options.search().isBlank()) {
            app.searchInput.setText(options.search());
            app.syncSearchGroup();
        }
        if (options.snapshot()) {
            System.out.print(app.snapshot(options.width(), options.height()));
            return;
        }

        app.run();
    }

    private void run() throws Exception {
        try (Backend backend = BackendFactory.create()) {
            backend.enableRawMode();
            backend.enterAlternateScreen();
            backend.enableMouseCapture();
            backend.hideCursor();

            try {
                var terminal = new dev.tamboui.terminal.Terminal<>(backend);
                backend.onResize(() -> terminal.draw(this::ui));

                while (running) {
                    terminal.draw(this::ui);
                    handleInput(backend.read(250), backend);
                }
            } finally {
                backend.disableMouseCapture();
                backend.showCursor();
                backend.leaveAlternateScreen();
                backend.disableRawMode();
            }
        }
    }

    private String snapshot(int width, int height) {
        Buffer buffer = Buffer.empty(Rect.of(width, height));
        ui(Frame.forTesting(buffer));
        return ExportRequest.export(buffer).text().options(options -> options.styles(false)).toString();
    }

    private void handleInput(int key, Backend backend) throws Exception {
        if (key < 0) {
            return;
        }

        if (key == 3) {
            running = false;
            return;
        }

        if (searchMode) {
            handleSearchInput(key, backend);
            return;
        }

        if (key == 'q' || key == 'Q') {
            running = false;
            return;
        }

        if (key == '/' || key == 's' || key == 'S') {
            searchMode = true;
            backend.showCursor();
            return;
        }

        if (key == '\t') {
            focus = focus == Focus.STANDINGS ? Focus.FIXTURES : Focus.STANDINGS;
            return;
        }

        if (key == '\n' || key == '\r') {
            openSelected();
            return;
        }

        if (key == 'n' || key == 'l') {
            nextGroup();
            return;
        }

        if (key == 'p' || key == 'h') {
            previousGroup();
            return;
        }

        if (key == 27) {
            handleEscape(backend);
            return;
        }

        if (key == 'j') {
            selectNext();
        } else if (key == 'k') {
            selectPrevious();
        }
    }

    private void ui(Frame frame) {
        animationFrame++;
        clickRegions.clear();
        groupTabsState.select(groupIndex);

        Rect area = frame.area();
        var rows = Layout.vertical()
            .constraints(Constraint.length(7), Constraint.fill(), Constraint.length(3))
            .split(area);

        renderHeader(frame, rows.get(0));
        renderBody(frame, rows.get(1));
        renderFooter(frame, rows.get(2));
    }

    private void renderBody(Frame frame, Rect area) {
        if (area.width() < 100) {
            var rows = Layout.vertical()
                .constraints(Constraint.length(12), Constraint.fill(), Constraint.length(9))
                .split(area);
            renderStandings(frame, rows.get(0));
            renderFixtures(frame, rows.get(1));
            renderDetailsAndNativeImageNotes(frame, rows.get(2));
            return;
        }

        var columns = Layout.horizontal()
            .constraints(Constraint.percentage(55), Constraint.percentage(45))
            .split(area);
        var rightRows = Layout.vertical()
            .constraints(Constraint.fill(), Constraint.length(9))
            .split(columns.get(1));

        renderStandings(frame, columns.get(0));
        renderFixtures(frame, rightRows.get(0));
        renderDetailsAndNativeImageNotes(frame, rightRows.get(1));
    }

    private void renderHeader(Frame frame, Rect area) {
        var rows = Layout.vertical()
            .constraints(Constraint.length(4), Constraint.length(3))
            .split(area);
        var snapshot = WorldCupData.SNAPSHOT;
        String snapshotDate = fixedDate(snapshot.snapshotDate());
        String totalTeams = Integer.toString(totalTeams());
        String totalFixtures = Integer.toString(snapshot.fixtures().size());

        Line title = Line.from(
            Span.raw(" 🏆 ").yellow(),
            Span.raw(snapshot.title()).bold().cyan(),
            Span.raw("  "),
            Span.raw("snapshot ").dim(),
            Span.raw(snapshotDate).yellow()
        );
        Line subtitle = Line.from(
            Span.raw(WorldCupFormatText.headerCounts(totalTeams, totalFixtures)).green()
        );

        var paragraph = Paragraph.builder()
            .text(Text.from(title.centered(), subtitle.centered()))
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.CYAN))
                .title(Title.from(Line.from(Span.raw(" TamboUI + GraalVM Native Image ").bold().yellow())).centered())
                .build())
            .build();
        frame.renderWidget(paragraph, rows.get(0));
        renderGroupTabs(frame, rows.get(1));
    }

    private void renderGroupTabs(Frame frame, Rect area) {
        List<Line> titles = new ArrayList<>();
        for (WorldCupData.GroupTable group : WorldCupData.SNAPSHOT.groups()) {
            titles.add(Line.from(Span.raw(group.code()).bold()));
        }

        var tabs = Tabs.builder()
            .titles(titles)
            .padding(" ", " ")
            .divider(Span.raw(" "))
            .highlightStyle(Style.EMPTY.bold().reversed().yellow())
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.BLUE))
                .title(Title.from(Line.from(Span.raw(" Groups ").blue())))
                .build())
            .build();

        frame.renderStatefulWidget(tabs, area, groupTabsState);
        registerGroupClicks(area);
    }

    private void renderStandings(Frame frame, Rect area) {
        WorldCupData.GroupTable group = WorldCupData.SNAPSHOT.groups().get(groupIndex);
        visibleTeams = filteredTeams(group);
        clampSelections();

        List<Row> rows = new ArrayList<>();
        for (WorldCupData.TeamStanding team : visibleTeams) {
            Style style = team.rank() <= 2
                ? Style.EMPTY.green()
                : team.rank() == 3 ? Style.EMPTY.yellow() : Style.EMPTY.gray();
            rows.add(Row.from(
                cell(WorldCupFormatText.rank(team.rank()), style),
                cell(WorldCupFormatText.teamLabel(team.emoji(), team.name()), style),
                cell(String.valueOf(team.played()), Style.EMPTY.white()),
                cell(team.record(), Style.EMPTY.white()),
                cell(String.valueOf(team.goalsFor()), Style.EMPTY.white()),
                cell(String.valueOf(team.goalsAgainst()), Style.EMPTY.white()),
                cell(WorldCupFormatText.goalDifference(team.goalDifference()), gdStyle(team.goalDifference())),
                cell(String.valueOf(team.points()), Style.EMPTY.bold().yellow())
            ));
        }
        if (rows.isEmpty()) {
            rows.add(Row.from("", "No team matches", "", "", "", "", "", "").style(Style.EMPTY.gray()));
            standingsState.clearSelection();
        } else {
            standingsState.select(selectedStanding);
        }

        var table = Table.builder()
            .header(Row.from("RK", "TEAM", "MP", "W-D-L", "GF", "GA", "GD", "PTS").style(Style.EMPTY.bold().cyan()))
            .rows(rows)
            .widths(
                Constraint.length(3),
                Constraint.fill(),
                Constraint.length(3),
                Constraint.length(7),
                Constraint.length(3),
                Constraint.length(3),
                Constraint.length(4),
                Constraint.length(4)
            )
            .columnSpacing(1)
            .highlightStyle(focus == Focus.STANDINGS ? Style.EMPTY.reversed().bold() : Style.EMPTY)
            .highlightSymbol(focus == Focus.STANDINGS ? "› " : "  ")
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.GREEN))
                .title(Title.from(Line.from(
                    Span.raw(" Group ").green(),
                    Span.raw(group.code()).bold().yellow(),
                    Span.raw(" standings ").green()
                )))
                .build())
            .build();

        frame.renderStatefulWidget(table, area, standingsState);
        registerRowClicks(area, ClickAction.STANDING, visibleTeams.size());
    }

    private void renderFixtures(Frame frame, Rect area) {
        visibleFixtures = filteredFixtures();
        clampSelections();

        List<Row> rows = new ArrayList<>();
        for (WorldCupData.Fixture fixture : visibleFixtures) {
            Style statusStyle = switch (fixture.status()) {
                case "TODAY" -> animationFrame % 2 == 0
                    ? Style.EMPTY.bold().yellow()
                    : Style.EMPTY.bold().magenta();
                case "FINAL" -> Style.EMPTY.green();
                case "NEXT" -> Style.EMPTY.cyan();
                default -> Style.EMPTY.gray();
            };
            rows.add(Row.from(
                cell(shortDate(fixture), statusStyle),
                cell(statusLabel(fixture), statusStyle),
                cell(WorldCupFormatText.fixtureTeam(fixture.leftSeed(), fixture.leftEmoji(), fixture.leftTeam()), Style.EMPTY.white()),
                cell(fixture.score(), Style.EMPTY.bold().yellow()),
                cell(WorldCupFormatText.fixtureTeam(fixture.rightSeed(), fixture.rightEmoji(), fixture.rightTeam()), Style.EMPTY.white())
            ));
        }
        if (rows.isEmpty()) {
            rows.add(Row.from("", "No fixture matches", "", "", "").style(Style.EMPTY.gray()));
            fixturesState.clearSelection();
        } else {
            fixturesState.select(selectedFixture);
        }

        var table = Table.builder()
            .header(Row.from("DATE", "STATE", "LEFT", "SCORE", "RIGHT").style(Style.EMPTY.bold().cyan()))
            .rows(rows)
            .widths(
                Constraint.length(7),
                Constraint.length(8),
                Constraint.fill(),
                Constraint.length(5),
                Constraint.fill()
            )
            .columnSpacing(1)
            .highlightStyle(focus == Focus.FIXTURES ? Style.EMPTY.reversed().bold() : Style.EMPTY)
            .highlightSymbol(focus == Focus.FIXTURES ? "› " : "  ")
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.MAGENTA))
                .title(Title.from(Line.from(Span.raw(" Who plays whom ").magenta(), Span.raw("⚽").yellow())))
                .build())
            .build();

        frame.renderStatefulWidget(table, area, fixturesState);
        registerRowClicks(area, ClickAction.FIXTURE, visibleFixtures.size());
    }

    private void renderDetailsAndNativeImageNotes(Frame frame, Rect area) {
        MetadataDigest digest = MetadataDigest.read();
        Line selectionLine = selectedDetailLine();
        Line searchLine = Line.from(
            Span.raw("filter: ").dim(),
            Span.raw(query().isEmpty() ? "none" : query()).bold().cyan(),
            Span.raw("  focus: ").dim(),
            Span.raw(focus == Focus.STANDINGS ? "standings" : "fixtures").yellow()
        );
        String metadataLine = WorldCupFormatText.metadataLine(
            digest.types(),
            digest.recordComponents(),
            digest.methods()
        );
        String heapLine = WorldCupFormatText.heapLine(
            WorldCupData.SNAPSHOT.groups().size(),
            totalTeams()
        );
        String formatLine = WorldCupFormatText.formatLine(
            totalTeams() + WorldCupData.SNAPSHOT.fixtures().size()
        );

        var paragraph = Paragraph.builder()
            .text(Text.from(
                selectionLine,
                searchLine,
                Line.from(Span.raw(metadataLine).cyan()),
                Line.from(Span.raw(heapLine).green()),
                Line.from(Span.raw(formatLine).yellow()),
                Line.from(Span.raw(WorldCupFormatText.probeLine()).magenta())
            ))
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.YELLOW))
                .title(Title.from(Line.from(Span.raw(" Native Image size hooks ").yellow())))
                .build())
            .build();
        frame.renderWidget(paragraph, area);
    }

    private void renderFooter(Frame frame, Rect area) {
        registerSearchClick(area);

        if (searchMode) {
            var input = TextInput.builder()
                .placeholder("Search teams, fixtures, groups, status...")
                .style(Style.EMPTY.cyan())
                .cursorStyle(Style.EMPTY.reversed().bold())
                .placeholderStyle(Style.EMPTY.gray())
                .block(Block.builder()
                    .borders(Borders.ALL)
                    .borderType(BorderType.ROUNDED)
                    .borderStyle(Style.EMPTY.fg(Color.CYAN))
                    .title(Title.from(Line.from(Span.raw(" Search ").cyan(), Span.raw("Enter keeps filter, Esc clears/exits").dim())))
                    .build())
                .build();
            input.renderWithCursor(area, frame.buffer(), searchInput, frame);
            return;
        }

        Line footer = Line.from(
            Span.raw(" / search ").cyan(),
            Span.raw(" ↑/↓ select ").cyan(),
            Span.raw(" tab focus ").cyan(),
            Span.raw(" enter open ").cyan(),
            Span.raw(" click groups/rows ").cyan(),
            Span.raw(" q quit ").yellow(),
            Span.raw(query().isEmpty() ? "" : "  filter: " + query()).dim()
        );
        var paragraph = Paragraph.builder()
            .text(Text.from(footer.centered()))
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.DARK_GRAY))
                .build())
            .build();
        frame.renderWidget(paragraph, area);
    }

    private void nextGroup() {
        setGroupIndex((groupIndex + 1) % WorldCupData.SNAPSHOT.groups().size());
    }

    private void previousGroup() {
        setGroupIndex(Math.floorMod(groupIndex - 1, WorldCupData.SNAPSHOT.groups().size()));
    }

    private void setGroupIndex(int index) {
        groupIndex = Math.floorMod(index, WorldCupData.SNAPSHOT.groups().size());
        groupTabsState.select(groupIndex);
        selectedStanding = 0;
        standingsState.select(0);
    }

    private void selectNext() {
        if (focus == Focus.STANDINGS) {
            selectedStanding = Math.min(selectedStanding + 1, Math.max(0, visibleTeams.size() - 1));
            standingsState.select(selectedStanding);
        } else {
            selectedFixture = Math.min(selectedFixture + 1, Math.max(0, visibleFixtures.size() - 1));
            fixturesState.select(selectedFixture);
        }
    }

    private void selectPrevious() {
        if (focus == Focus.STANDINGS) {
            selectedStanding = Math.max(0, selectedStanding - 1);
            standingsState.select(selectedStanding);
        } else {
            selectedFixture = Math.max(0, selectedFixture - 1);
            fixturesState.select(selectedFixture);
        }
    }

    private void openSelected() {
        if (focus == Focus.FIXTURES && selectedFixture >= 0 && selectedFixture < visibleFixtures.size()) {
            WorldCupData.Fixture fixture = visibleFixtures.get(selectedFixture);
            int target = groupIndex(fixture.leftSeed().substring(0, 1));
            setGroupIndex(target);
            focus = Focus.STANDINGS;
        }
    }

    private static int groupIndex(String groupCode) {
        if (groupCode == null || groupCode.isBlank()) {
            return 0;
        }
        for (int i = 0; i < WorldCupData.SNAPSHOT.groups().size(); i++) {
            if (WorldCupData.SNAPSHOT.groups().get(i).code().equalsIgnoreCase(groupCode)) {
                return i;
            }
        }
        return 0;
    }

    private void handleSearchInput(int key, Backend backend) throws Exception {
        if (key == '\n' || key == '\r') {
            searchMode = false;
            backend.hideCursor();
            return;
        }

        if (key == 27) {
            int next = backend.read(1);
            if (next == '[') {
                int direction = backend.read(1);
                if (direction == 'C') {
                    searchInput.moveCursorRight();
                } else if (direction == 'D') {
                    searchInput.moveCursorLeft();
                }
                return;
            }

            if (searchInput.length() > 0) {
                searchInput.clear();
                selectedStanding = 0;
                selectedFixture = 0;
            } else {
                searchMode = false;
                backend.hideCursor();
            }
            return;
        }

        if (key == 127 || key == 8) {
            searchInput.deleteBackward();
            syncSearchGroup();
            return;
        }

        if (key >= 32 && key < 127) {
            searchInput.insert((char) key);
            syncSearchGroup();
        }
    }

    private void handleEscape(Backend backend) throws Exception {
        int bracket = backend.read(1);
        if (bracket != '[') {
            return;
        }

        int direction = backend.read(1);
        if (direction == '<') {
            handleSgrMouse(backend);
            return;
        }
        if (direction == 'M') {
            handleX10Mouse(backend);
            return;
        }

        if (direction == 'A') {
            selectPrevious();
        } else if (direction == 'B') {
            selectNext();
        } else if (direction == 'C') {
            nextGroup();
        } else if (direction == 'D') {
            previousGroup();
        }
    }

    private void handleSgrMouse(Backend backend) throws Exception {
        StringBuilder sequence = new StringBuilder();
        int end;
        while ((end = backend.read(5)) >= 0) {
            if (end == 'M' || end == 'm') {
                if (end == 'M') {
                    String[] parts = sequence.toString().split(";");
                    if (parts.length == 3) {
                        int button = Integer.parseInt(parts[0]);
                        int x = Integer.parseInt(parts[1]) - 1;
                        int y = Integer.parseInt(parts[2]) - 1;
                        if ((button & 3) == 0) {
                            handleClick(x, y, backend);
                        }
                    }
                }
                return;
            }
            sequence.append((char) end);
        }
    }

    private void handleX10Mouse(Backend backend) throws Exception {
        int button = backend.read(1);
        int x = backend.read(1);
        int y = backend.read(1);
        if (button >= 0 && x >= 33 && y >= 33 && ((button - 32) & 3) == 0) {
            handleClick(x - 33, y - 33, backend);
        }
    }

    private void handleClick(int x, int y, Backend backend) throws Exception {
        for (ClickRegion region : clickRegions) {
            if (!region.rect().contains(x, y)) {
                continue;
            }

            if (region.action() == ClickAction.GROUP) {
                setGroupIndex(region.index());
                focus = Focus.STANDINGS;
            } else if (region.action() == ClickAction.STANDING) {
                focus = Focus.STANDINGS;
                selectedStanding = region.index();
                standingsState.select(selectedStanding);
            } else if (region.action() == ClickAction.FIXTURE) {
                focus = Focus.FIXTURES;
                selectedFixture = region.index();
                fixturesState.select(selectedFixture);
            } else if (region.action() == ClickAction.SEARCH) {
                searchMode = true;
                backend.showCursor();
            }
            return;
        }
    }

    private void registerGroupClicks(Rect area) {
        int groups = WorldCupData.SNAPSHOT.groups().size();
        int innerLeft = area.x() + 1;
        int innerWidth = Math.max(1, area.width() - 2);
        int chunk = Math.max(1, innerWidth / groups);
        for (int i = 0; i < groups; i++) {
            int left = innerLeft + i * chunk;
            int right = i == groups - 1 ? innerLeft + innerWidth : left + chunk;
            clickRegions.add(new ClickRegion(new Rect(left, area.y() + 1, Math.max(1, right - left), 1), ClickAction.GROUP, i));
        }
    }

    private void registerRowClicks(Rect area, ClickAction action, int count) {
        int dataTop = area.y() + 2;
        int maxRows = Math.max(0, area.height() - 3);
        int rows = Math.min(count, maxRows);
        for (int i = 0; i < rows; i++) {
            clickRegions.add(new ClickRegion(new Rect(area.x(), dataTop + i, area.width(), 1), action, i));
        }
    }

    private void registerSearchClick(Rect area) {
        clickRegions.add(new ClickRegion(area, ClickAction.SEARCH, 0));
    }

    private List<WorldCupData.TeamStanding> filteredTeams(WorldCupData.GroupTable group) {
        String query = query();
        if (query.isEmpty()) {
            return group.teams();
        }

        List<WorldCupData.TeamStanding> matches = new ArrayList<>();
        for (WorldCupData.TeamStanding team : group.teams()) {
            if (matches(query, group.code(), team.name(), team.record(), String.valueOf(team.points()))) {
                matches.add(team);
            }
        }
        return matches;
    }

    private List<WorldCupData.Fixture> filteredFixtures() {
        String query = query();
        if (query.isEmpty()) {
            return WorldCupData.SNAPSHOT.fixtures();
        }

        List<WorldCupData.Fixture> matches = new ArrayList<>();
        for (WorldCupData.Fixture fixture : WorldCupData.SNAPSHOT.fixtures()) {
            if (matches(
                query,
                fixture.status(),
                fixture.leftSeed(),
                fixture.leftTeam(),
                fixture.rightSeed(),
                fixture.rightTeam(),
                shortDate(fixture)
            )) {
                matches.add(fixture);
            }
        }
        return matches;
    }

    private void syncSearchGroup() {
        selectedStanding = 0;
        selectedFixture = 0;
        String query = query();
        if (query.isEmpty()) {
            return;
        }

        WorldCupData.GroupTable current = WorldCupData.SNAPSHOT.groups().get(groupIndex);
        if (!filteredTeams(current).isEmpty()) {
            return;
        }

        for (int i = 0; i < WorldCupData.SNAPSHOT.groups().size(); i++) {
            WorldCupData.GroupTable group = WorldCupData.SNAPSHOT.groups().get(i);
            for (WorldCupData.TeamStanding team : group.teams()) {
                if (matches(query, group.code(), team.name(), team.record(), String.valueOf(team.points()))) {
                    setGroupIndex(i);
                    return;
                }
            }
        }
    }

    private void clampSelections() {
        selectedStanding = Math.min(selectedStanding, Math.max(0, visibleTeams.size() - 1));
        selectedFixture = Math.min(selectedFixture, Math.max(0, visibleFixtures.size() - 1));
    }

    private Line selectedDetailLine() {
        if (focus == Focus.STANDINGS && selectedStanding >= 0 && selectedStanding < visibleTeams.size()) {
            WorldCupData.TeamStanding team = visibleTeams.get(selectedStanding);
            return Line.from(
                Span.raw("selected: ").dim(),
                Span.raw(team.emoji() + " " + team.name()).bold().yellow(),
                Span.raw(WorldCupFormatText.teamDetail(team.points(), team.goalDifference(), team.record())).white()
            );
        }

        if (focus == Focus.FIXTURES && selectedFixture >= 0 && selectedFixture < visibleFixtures.size()) {
            WorldCupData.Fixture fixture = visibleFixtures.get(selectedFixture);
            return Line.from(
                Span.raw("selected: ").dim(),
                Span.raw(fixture.leftEmoji() + " " + fixture.leftTeam()).bold().yellow(),
                Span.raw(" vs ").dim(),
                Span.raw(fixture.rightEmoji() + " " + fixture.rightTeam()).bold().cyan(),
                Span.raw("  " + shortDate(fixture) + " " + fixture.status()).white()
            );
        }

        return Line.from(Span.raw("selected: no match").gray());
    }

    private String statusLabel(WorldCupData.Fixture fixture) {
        if ("TODAY".equals(fixture.status())) {
            return SPINNER[animationFrame % SPINNER.length] + " TODAY";
        }
        if ("FINAL".equals(fixture.status())) {
            return "✓ FINAL";
        }
        return fixture.status();
    }

    private String query() {
        return asciiLower(searchInput.text().trim());
    }

    private static boolean matches(String query, String... values) {
        for (String value : values) {
            if (value != null && asciiLower(value).contains(query)) {
                return true;
            }
        }
        return false;
    }

    private static Cell cell(String value, Style style) {
        return Cell.from(value).style(style);
    }

    private static Style gdStyle(int goalDifference) {
        if (goalDifference > 0) {
            return Style.EMPTY.green();
        }
        if (goalDifference < 0) {
            return Style.EMPTY.red();
        }
        return Style.EMPTY.white();
    }

    private static String shortDate(WorldCupData.Fixture fixture) {
        return fixture.date().getMonth().name().substring(0, 3) + " " + fixture.date().getDayOfMonth();
    }

    private static String fixedDate(java.time.LocalDate date) {
        String[] months = {
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
        };
        return months[date.getMonthValue() - 1] + " " + date.getDayOfMonth() + ", " + date.getYear();
    }

    private static String asciiLower(String text) {
        StringBuilder result = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                result.append((char) (c + ('a' - 'A')));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    private static int totalTeams() {
        int total = 0;
        for (WorldCupData.GroupTable group : WorldCupData.SNAPSHOT.groups()) {
            total += group.teams().size();
        }
        return total;
    }

    private static void printHelp() {
        System.out.println("World Cup TamboUI GraalVM Demo");
        System.out.println();
        System.out.println("Usage:");
        System.out.println("  mvn -q exec:java");
        System.out.println("  target/worldcup-standings");
        System.out.println("  target/worldcup-standings --snapshot --group=C");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --snapshot        Render once as plain text");
        System.out.println("  --group=<A-L>     Initial group");
        System.out.println("  --search=<text>   Start with a filter");
        System.out.println("  --width=<cols>    Snapshot width, default 120");
        System.out.println("  --height=<rows>   Snapshot height, default 42");
        System.out.println();
        System.out.println("Controls:");
        System.out.println("  / or s            Search");
        System.out.println("  Tab               Switch focus between standings and fixtures");
        System.out.println("  Up/Down or k/j    Select rows");
        System.out.println("  Left/Right or p/n Switch groups");
        System.out.println("  Enter             Open selected fixture's left-side group");
        System.out.println("  Mouse             Click group tabs, rows, or search footer");
        System.out.println("  q                 Quit");
    }

    private record Options(boolean snapshot, boolean help, String group, String search, int width, int height) {
        static Options parse(String[] args) {
            boolean snapshot = false;
            boolean help = false;
            String group = "A";
            String search = "";
            int width = 120;
            int height = 42;

            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if ("--snapshot".equals(arg)) {
                    snapshot = true;
                } else if ("--help".equals(arg) || "-h".equals(arg)) {
                    help = true;
                } else if (arg.startsWith("--group=")) {
                    group = arg.substring("--group=".length());
                } else if ("--group".equals(arg) && i + 1 < args.length) {
                    group = args[++i];
                } else if (arg.startsWith("--search=")) {
                    search = arg.substring("--search=".length());
                } else if ("--search".equals(arg) && i + 1 < args.length) {
                    search = args[++i];
                } else if (arg.startsWith("--width=")) {
                    width = Integer.parseInt(arg.substring("--width=".length()));
                } else if (arg.startsWith("--height=")) {
                    height = Integer.parseInt(arg.substring("--height=".length()));
                }
            }

            return new Options(snapshot, help, group, search, width, height);
        }
    }

    private record MetadataDigest(int types, int recordComponents, int methods) {
        static MetadataDigest read() {
            int types = 0;
            int recordComponents = 0;
            int methods = 0;

            for (Class<?> type : WorldCupData.reflectedTypes()) {
                types++;
                RecordComponent[] components = type.getRecordComponents();
                if (components != null) {
                    recordComponents += components.length;
                }
                methods += type.getDeclaredMethods().length;
            }

            return new MetadataDigest(types, recordComponents, methods);
        }
    }

    @SuppressWarnings("unused")
    private static String stackTraceForMetadata(Throwable throwable) {
        StringWriter writer = new StringWriter();
        throwable.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }

    private enum Focus {
        STANDINGS,
        FIXTURES
    }

    private enum ClickAction {
        GROUP,
        STANDING,
        FIXTURE,
        SEARCH
    }

    private record ClickRegion(Rect rect, ClickAction action, int index) {
    }
}
